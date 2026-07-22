package com.example.fxsuite.launcher.setup;

import com.example.fxsuite.launcher.EnvConfig;
import com.example.fxsuite.launcher.Install;
import com.example.fxsuite.launcher.env.EnvSpecs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Plans, applies and reverses the per-user protocol-handler registration.
 *
 * <p>Everything is a two-step: build a {@link RegistryPlan} (which touches nothing)
 * so it can be shown and consented to, then {@link #apply} it. Installing records
 * whatever command was registered beforehand, so uninstalling can put it back rather
 * than merely deleting — that is what makes the process genuinely reversible.</p>
 *
 * <p>All writes go to {@code HKEY_CURRENT_USER}, so no administrator rights are
 * needed, and the only process ever launched is {@code reg.exe} with an argument
 * vector — never a shell.</p>
 */
public final class RegistrySetup {

    private static final String CLASSES = "HKCU\\Software\\Classes\\";
    /** Marker meaning "nothing was registered before we installed". */
    private static final String NONE = "";

    private RegistrySetup() {}

    // --- planning --------------------------------------------------------

    /**
     * The exact command Windows will run for this environment.
     *
     * <p>A singleton build knows its own environment, so its command needs no {@code --env}.
     * A multiplexed build must be told which of its family this registration is for, so its
     * command supplies {@code --env} (and an optional {@code --base}) — the "optional args
     * for the multiplexed env". The jar itself is asked which it is.</p>
     */
    public static String commandFor(String envId, Path launcherJar, String base) {
        boolean needsEnvArg = EnvSpecs.ofJar(launcherJar, envId)
                .map(EnvSpecs.JarEnv::requiresEnvArgument)
                .orElse(true);          // unknown build: be explicit rather than assume
        StringBuilder cmd = new StringBuilder()
                .append('"').append(javawExe()).append('"')
                .append(" -jar ").append('"').append(launcherJar).append('"');
        if (needsEnvArg) {
            cmd.append(" --env=").append(envId);
            if (base != null && !base.isBlank()) cmd.append(" --base=").append(base.trim());
        }
        return cmd.append(" \"%1\"").toString();
    }

    /** What installing this environment would change. Writes nothing. */
    public static RegistryPlan installPlan(String envId, Path launcherJar, String base) {
        String scheme = EnvConfig.scheme(envId);
        String key = CLASSES + scheme;
        String cmdKey = key + "\\shell\\open\\command";

        if (launcherJar == null || !Files.isRegularFile(launcherJar)) {
            return new RegistryPlan(RegistryPlan.Action.INSTALL, envId, scheme, List.of(),
                    "launcher jar not found: " + launcherJar);
        }

        // Ask the jar whether it may serve this environment at all. Registering the dev build
        // as fxsuite-prod:// used to succeed and fail later at launch; now it never installs.
        var jarEnv = EnvSpecs.ofJar(launcherJar, envId).orElse(null);
        if (jarEnv != null && !jarEnv.accepts()) {
            return new RegistryPlan(RegistryPlan.Action.INSTALL, envId, scheme, List.of(),
                    "this is the " + jarEnv.displayName() + " launcher; it serves "
                            + jarEnv.membership() + ", not '" + envId + "'");
        }

        String command = commandFor(envId, launcherJar, base);
        String current = currentCommand(envId);
        if (command.equals(current)) {
            return new RegistryPlan(RegistryPlan.Action.INSTALL, envId, scheme, List.of(), null);
        }

        List<RegistryOp> ops = List.of(
                RegistryOp.set(key, "(Default)", "URL:FxSuite (" + envId + ")", null),
                RegistryOp.set(key, "URL Protocol", "", null),
                RegistryOp.set(cmdKey, "(Default)", command, current));
        return new RegistryPlan(RegistryPlan.Action.INSTALL, envId, scheme, ops, null);
    }

    /** What removing this environment would change — restoring any prior handler. */
    public static RegistryPlan uninstallPlan(String envId) {
        String scheme = EnvConfig.scheme(envId);
        String key = CLASSES + scheme;
        String current = currentCommand(envId);

        if (current == null) {
            return new RegistryPlan(RegistryPlan.Action.UNINSTALL, envId, scheme, List.of(), null);
        }
        String backup = backups().getProperty(scheme);
        List<RegistryOp> ops = (backup != null && !backup.equals(NONE))
                ? List.of(RegistryOp.restore(key + "\\shell\\open\\command", "(Default)", backup))
                : List.of(RegistryOp.deleteKey(key, current));
        return new RegistryPlan(RegistryPlan.Action.UNINSTALL, envId, scheme, ops, null);
    }

    // --- applying --------------------------------------------------------

    /** Execute a plan. Returns a human-readable result line. */
    public static String apply(RegistryPlan plan) {
        if (!plan.applicable()) {
            return plan.blocker() != null ? "refused: " + plan.blocker() : "nothing to do";
        }
        return switch (plan.action()) {
            case INSTALL -> applyInstall(plan);
            case UNINSTALL -> applyUninstall(plan);
        };
    }

    private static String applyInstall(RegistryPlan plan) {
        // Remember what was there first, so uninstall can put it back.
        String previous = plan.ops().stream()
                .filter(o -> o.kind() == RegistryOp.Kind.SET && o.previous() != null)
                .map(RegistryOp::previous).findFirst().orElse(NONE);
        rememberBackup(plan.scheme(), previous);

        String command = plan.ops().get(plan.ops().size() - 1).data();
        String reg = """
                Windows Registry Editor Version 5.00

                [HKEY_CURRENT_USER\\Software\\Classes\\%1$s]
                @="URL:FxSuite (%2$s)"
                "URL Protocol"=""

                [HKEY_CURRENT_USER\\Software\\Classes\\%1$s\\shell\\open\\command]
                @="%3$s"
                """.formatted(plan.scheme(), plan.envId(), escapeRegValue(command));
        return importReg(reg) ? "installed " + plan.scheme() + "://"
                              : "FAILED to install " + plan.scheme();
    }

    private static String applyUninstall(RegistryPlan plan) {
        RegistryOp op = plan.ops().get(0);
        if (op.kind() == RegistryOp.Kind.DELETE_KEY) {
            boolean ok = reg("delete", CLASSES + plan.scheme(), "/f");
            if (ok) forgetBackup(plan.scheme());
            return ok ? "removed " + plan.scheme() + "://" : "FAILED to remove " + plan.scheme();
        }
        // Restore the handler that existed before we installed.
        String reg = """
                Windows Registry Editor Version 5.00

                [HKEY_CURRENT_USER\\Software\\Classes\\%1$s\\shell\\open\\command]
                @="%2$s"
                """.formatted(plan.scheme(), escapeRegValue(op.data()));
        boolean ok = importReg(reg);
        if (ok) forgetBackup(plan.scheme());
        return ok ? "restored the previous handler for " + plan.scheme() + "://"
                  : "FAILED to restore " + plan.scheme();
    }

    // --- inspection ------------------------------------------------------

    /** The command currently registered for this environment, or null if none. */
    public static String currentCommand(String envId) {
        String out = regCapture("query", CLASSES + EnvConfig.scheme(envId) + "\\shell\\open\\command", "/ve");
        for (String line : out.split("\\R")) {
            int i = line.indexOf("REG_SZ");
            if (i >= 0) {
                String v = line.substring(i + "REG_SZ".length()).trim();
                return v.isEmpty() ? null : v;
            }
        }
        return null;
    }

    /** Every FxSuite environment currently registered for this user. */
    public static List<String> registeredEnvs() {
        List<String> envs = new ArrayList<>();
        String out = regCapture("query", CLASSES, "/f", "fxsuite-", "/k");
        String needle = "\\Software\\Classes\\fxsuite-";
        for (String line : out.split("\\R")) {
            int i = line.indexOf(needle);
            if (i < 0) continue;
            String tail = line.substring(i + needle.length()).trim();
            if (tail.isEmpty() || tail.contains("\\")) continue;   // only the top-level scheme key
            if (!envs.contains(tail)) envs.add(tail);
        }
        return envs;
    }

    // --- backup of any pre-existing handler -------------------------------

    private static Path backupFile() {
        return Install.envStateRoot("_setup").resolve("previous-handlers.properties");
    }

    private static Properties backups() {
        Properties p = new Properties();
        Path f = backupFile();
        if (Files.isRegularFile(f)) {
            try (InputStream in = Files.newInputStream(f)) { p.load(in); } catch (IOException ignored) {}
        }
        return p;
    }

    private static void rememberBackup(String scheme, String previousCommand) {
        Properties p = backups();
        p.setProperty(scheme, previousCommand == null ? NONE : previousCommand);
        store(p);
    }

    private static void forgetBackup(String scheme) {
        Properties p = backups();
        p.remove(scheme);
        store(p);
    }

    private static void store(Properties p) {
        try {
            Path f = backupFile();
            Files.createDirectories(f.getParent());
            try (OutputStream out = Files.newOutputStream(f)) {
                p.store(out, "Handlers that existed before FxSuite setup installed its own");
            }
        } catch (IOException ignored) {}
    }

    // --- reg.exe plumbing -------------------------------------------------

    private static String javawExe() {
        return Path.of(System.getProperty("java.home"), "bin", "javaw.exe").toString();
    }

    private static String escapeRegValue(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /** Import a .reg file — the command value's quotes cannot survive argv quoting. */
    private static boolean importReg(String regText) {
        Path tmp = null;
        try {
            tmp = Files.createTempFile("fxsuite-setup-", ".reg");
            String withBom = '﻿' + regText;   // .reg "Version 5.00" wants a UTF-16LE BOM
            Files.write(tmp, withBom.getBytes(StandardCharsets.UTF_16LE));
            return reg("import", tmp.toString());
        } catch (IOException e) {
            return false;
        } finally {
            if (tmp != null) { try { Files.deleteIfExists(tmp); } catch (IOException ignored) {} }
        }
    }

    private static boolean reg(String... args) {
        List<String> cmd = new ArrayList<>();
        cmd.add("reg");
        cmd.addAll(List.of(args));
        try {
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            p.getInputStream().readAllBytes();      // drain so the child never blocks
            return p.waitFor() == 0;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            return false;
        }
    }

    private static String regCapture(String... args) {
        List<String> cmd = new ArrayList<>();
        cmd.add("reg");
        cmd.addAll(List.of(args));
        try {
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            p.waitFor();
            return out;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            return "";
        }
    }
}
