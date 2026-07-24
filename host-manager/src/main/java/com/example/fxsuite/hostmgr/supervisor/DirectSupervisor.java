package com.example.fxsuite.hostmgr.supervisor;

import com.example.fxsuite.hostmgr.model.LaunchSpec;
import com.example.fxsuite.hostmgr.model.UnitId;
import com.example.fxsuite.hostmgr.model.UnitState;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lane C: the agent supervises processes itself.
 *
 * <p>It spawns each unit as a real child process and tracks it in a map. This needs no
 * privilege at all — if the agent is allowed to run, it is allowed to do this — and it works
 * on any OS, which is why it is also the one that runs on a Windows developer box.</p>
 *
 * <p>Its honest limitation is the flip side of holding state in memory: if the agent process
 * itself dies, it loses its handle on the children (and, being their parent, they will not
 * outlive it cleanly). That is exactly what {@link SystemdUserSupervisor} exists to fix —
 * systemd owns the processes, so they survive an agent restart and a host reboot. Keep-alive
 * across agent restarts is therefore the reason to prefer lane A in production; here, the
 * reconcile loop is the only healer, so a short reconcile interval covers crashes.</p>
 */
public final class DirectSupervisor implements Supervisor {

    private record Running(Process process, String version, long startMs) {}

    private final Map<UnitId, Running> running = new ConcurrentHashMap<>();
    private final Path logDir;
    private final String javaExe;

    public DirectSupervisor(Path workDir) {
        this.logDir = workDir.resolve("logs");
        this.javaExe = Path.of(System.getProperty("java.home"), "bin", "java").toString();
    }

    @Override
    public String kind() {
        return "direct";
    }

    @Override
    public List<UnitState> list() {
        // Deliberately do NOT forget an exited child: a unit that was asked to run and then
        // died must be reported as *down*, not as *absent*, or the reconciler cannot tell a
        // crash (heal it) from something never installed. An entry is dropped only on an
        // explicit stop(). This mirrors systemd keeping a failed unit listed as inactive.
        List<UnitState> out = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (var e : running.entrySet()) {
            Running r = e.getValue();
            boolean alive = r.process().isAlive();
            out.add(new UnitState(e.getKey(), r.version(), alive,
                    alive ? r.process().pid() : 0, alive ? (now - r.startMs()) / 1000 : 0));
        }
        return out;
    }

    @Override
    public void apply(LaunchSpec spec) throws Exception {
        Running existing = running.remove(spec.id());
        if (existing != null && existing.process().isAlive()) {
            existing.process().destroy();                          // replace: stop the old first
            existing.process().waitFor();
        }
        Files.createDirectories(logDir);

        List<String> cmd = new ArrayList<>();
        cmd.add(javaExe);
        cmd.add("-jar");
        cmd.add(spec.jar().toString());
        cmd.addAll(spec.args());

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(
                logDir.resolve(spec.id().unitName() + ".log").toFile()));
        // memoryMax is a systemd concern; the direct supervisor cannot enforce a cgroup cap,
        // so it is deliberately not applied here (documented, not silently ignored).
        Process p = pb.start();
        running.put(spec.id(), new Running(p, spec.version(), System.currentTimeMillis()));
    }

    @Override
    public void stop(UnitId id) throws Exception {
        Running r = running.remove(id);
        if (r != null && r.process().isAlive()) {
            r.process().destroy();
            r.process().waitFor();
        }
    }

    /** Path separator kept explicit for readers used to the multi-jar classpath elsewhere. */
    @SuppressWarnings("unused")
    private static final String CP_SEP = File.pathSeparator;
}
