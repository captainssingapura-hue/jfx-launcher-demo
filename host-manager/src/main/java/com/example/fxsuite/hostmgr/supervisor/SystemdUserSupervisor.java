package com.example.fxsuite.hostmgr.supervisor;

import com.example.fxsuite.hostmgr.model.LaunchSpec;
import com.example.fxsuite.hostmgr.model.UnitId;
import com.example.fxsuite.hostmgr.model.UnitState;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Lane A: the agent delegates supervision to the user's own systemd instance.
 *
 * <p>This is the production target on Linux, and it is deliberately low-privilege. It manages
 * <b>user</b> units ({@code systemctl --user}), which run entirely as the agent's unprivileged
 * account — no root to author a unit, reload, start, stop, or set a cgroup resource cap. The
 * only privileged step is a one-time {@code loginctl enable-linger <user>} at provisioning, so
 * the user manager persists across logout and reboot. After that, IT is out of the loop — the
 * same "no ongoing central involvement" property the desktop launcher gets from HKCU.</p>
 *
 * <p>An upgrade is a symlink swap plus a restart, so rollback is instant and needs no
 * re-download: {@code current -> versions/<ver>} is repointed and the unit restarted. A
 * templated unit {@code fxsuite-app@.service} is instantiated per environment
 * ({@code fxsuite-app@prod}, {@code @uat}), the server-side twin of the launcher's {@code --env}.</p>
 *
 * <p><b>Verification note:</b> this binding shells out to {@code systemctl}/{@code systemd-run},
 * which exist only on Linux. On the Windows build machine it compiles but is never selected; it
 * is exercised where systemd exists. Every method routes through {@link #run} so the exact
 * command line is logged and auditable.</p>
 */
public final class SystemdUserSupervisor implements Supervisor {

    private final Path appsRoot;          // <appsRoot>/<app>/versions/<ver>/app.jar, and .../current
    private final boolean verbose;

    public SystemdUserSupervisor(Path appsRoot, boolean verbose) {
        this.appsRoot = appsRoot;
        this.verbose = verbose;
    }

    @Override
    public String kind() {
        return "systemd-user";
    }

    @Override
    public void apply(LaunchSpec spec) throws Exception {
        // Point <app>/current at the verified version, then (re)start the unit that runs it.
        Path appDir = appsRoot.resolve(spec.id().appId());
        Path current = appDir.resolve("current");
        Path target = spec.jar().toAbsolutePath();
        repoint(current, target);

        String unit = spec.id().unitName() + ".service";
        if (unitFileExists(spec.id())) {
            // A pre-installed template unit already reads <app>/current — just bounce it.
            run("systemctl", "--user", "restart", unit);
        } else {
            // No pre-installed unit: run a transient one. Transient units need no file on disk
            // and no daemon-reload, which is what makes adding an app fully unprivileged.
            List<String> cmd = new ArrayList<>(List.of(
                    "systemd-run", "--user",
                    "--unit=" + spec.id().unitName(),
                    "--collect",
                    "--property=Restart=on-failure",
                    "--property=RestartSec=2"));
            if (spec.memoryMax() != null && !spec.memoryMax().isBlank()) {
                cmd.add("--property=MemoryMax=" + spec.memoryMax());
            }
            cmd.add("--setenv=FXSUITE_VERSION=" + spec.version());
            cmd.add(javaExe());
            cmd.add("-jar");
            cmd.add(current.toString());          // run through the symlink, so upgrades are a repoint
            cmd.addAll(spec.args());
            run(cmd.toArray(String[]::new));
        }
    }

    @Override
    public void stop(UnitId id) throws Exception {
        run("systemctl", "--user", "stop", id.unitName() + ".service");
        // --collect on the transient unit clears its state; a file-based unit would also be
        // disabled here in a fuller build.
    }

    @Override
    public List<UnitState> list() throws Exception {
        List<UnitState> out = new ArrayList<>();
        // List our instances; each line: "fxsuite-<app>@<env>.service  loaded active running ..."
        Result r = run("systemctl", "--user", "list-units", "--type=service",
                "--all", "--no-legend", "--plain", "fxsuite-*");
        for (String line : r.stdout().split("\n")) {
            String s = line.strip();
            if (s.isEmpty()) continue;
            String unit = s.split("\\s+")[0];
            if (!unit.endsWith(".service")) continue;
            UnitId id = parseUnit(unit);
            if (id == null) continue;
            out.add(readState(id));
        }
        return out;
    }

    private UnitState readState(UnitId id) throws Exception {
        // One show call yields the machine-readable facts we report upward.
        Result r = run("systemctl", "--user", "show", id.unitName() + ".service",
                "--property=ActiveState,MainPID,ExecMainStartTimestampMonotonic,Environment");
        String active = prop(r.stdout(), "ActiveState");
        long pid = parseLong(prop(r.stdout(), "MainPID"));
        String version = envVar(prop(r.stdout(), "Environment"), "FXSUITE_VERSION");
        return new UnitState(id, version, "active".equals(active), pid, 0);
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private boolean unitFileExists(UnitId id) {
        try {
            Result r = run("systemctl", "--user", "cat", id.unitName() + ".service");
            return r.exit() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static void repoint(Path link, Path target) throws IOException {
        Files.createDirectories(link.getParent());
        Path tmp = link.resolveSibling(link.getFileName() + ".new");
        Files.deleteIfExists(tmp);
        Files.createSymbolicLink(tmp, target);
        Files.move(tmp, link, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                java.nio.file.StandardCopyOption.ATOMIC_MOVE);
    }

    private static UnitId parseUnit(String unit) {
        // fxsuite-<app>@<env>.service
        String body = unit.substring(0, unit.length() - ".service".length());
        if (!body.startsWith("fxsuite-")) return null;
        body = body.substring("fxsuite-".length());
        int at = body.indexOf('@');
        if (at < 0) return null;
        return new UnitId(body.substring(0, at), body.substring(at + 1));
    }

    private static String prop(String show, String key) {
        for (String line : show.split("\n")) {
            int eq = line.indexOf('=');
            if (eq > 0 && line.substring(0, eq).equals(key)) return line.substring(eq + 1).strip();
        }
        return "";
    }

    private static String envVar(String environment, String name) {
        // Environment=FXSUITE_VERSION=1.2.0 OTHER=...
        for (String tok : environment.split("\\s+")) {
            if (tok.startsWith(name + "=")) return tok.substring(name.length() + 1);
        }
        return null;
    }

    private static long parseLong(String s) {
        try { return Long.parseLong(s.strip()); } catch (Exception e) { return 0; }
    }

    private static String javaExe() {
        return Path.of(System.getProperty("java.home"), "bin", "java").toString();
    }

    private record Result(int exit, String stdout) {}

    private Result run(String... cmd) throws IOException, InterruptedException {
        if (verbose) System.out.println("[systemd] " + String.join(" ", cmd));
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exit = p.waitFor();
        return new Result(exit, out);
    }
}
