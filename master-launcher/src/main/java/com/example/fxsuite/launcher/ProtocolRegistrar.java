package com.example.fxsuite.launcher;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Installs / removes the per-user, <b>per-environment</b> protocol handlers.
 *
 * <p>Each environment gets its own URL scheme, so which scheme the browser invokes
 * decides which binary runs with which arguments — the environment can never be
 * influenced by anything inside the URL:</p>
 *
 * <pre>
 *   HKCU\Software\Classes\fxsuite-prod\shell\open\command
 *       = "javaw" -jar "…\prod\master-launcher.jar" --env=prod "%1"
 *   HKCU\Software\Classes\fxsuite-dev3\shell\open\command
 *       = "javaw" -jar "…\dev\master-launcher.jar" --env=dev3 --base=https://dev3… "%1"
 * </pre>
 *
 * <p>Singleton environments (Prod, UAT) point at their own dedicated install; multiplexed
 * dev environments all point at the one dev install, differing only by arguments.</p>
 */
public final class ProtocolRegistrar {

    private static final String CLASSES = "HKCU\\Software\\Classes\\";

    private ProtocolRegistrar() {}

    public static void register(String envId, String base) {
        Path jar = ownJarPath();
        if (jar == null) {
            DiagLog.log("[register] ERROR: could not locate the running jar.");
            return;
        }
        if (!jar.toString().toLowerCase().endsWith(".jar")) {
            DiagLog.log("[register] WARNING: not running from a .jar (" + jar + "). Package first.");
        }

        String scheme = EnvConfig.scheme(envId);
        String javaw = javawExe();
        StringBuilder cmd = new StringBuilder()
                .append(quote(javaw)).append(" -jar ").append(quote(jar.toString()))
                .append(" --env=").append(envId);
        if (base != null && !base.isBlank()) cmd.append(" --base=").append(base.trim());
        cmd.append(" \"%1\"");
        String command = cmd.toString();

        String reg = """
                Windows Registry Editor Version 5.00

                [HKEY_CURRENT_USER\\Software\\Classes\\%1$s]
                @="URL:FxSuite (%2$s)"
                "URL Protocol"=""

                [HKEY_CURRENT_USER\\Software\\Classes\\%1$s\\shell\\open\\command]
                @="%3$s"
                """.formatted(scheme, envId, escapeRegValue(command));

        if (importReg(reg)) {
            DiagLog.log("[register] " + scheme + ":// registered for environment '" + envId + "'.");
            DiagLog.log("[register] command = " + command);
            DiagLog.log("[register] Test:  cmd /c start \"\" \"" + scheme + "://launch/hello?tok=…\"");
        } else {
            DiagLog.log("[register] FAILED — see reg.exe output above.");
        }
    }

    public static void unregister(String envId) {
        String scheme = EnvConfig.scheme(envId);
        boolean ok = reg("delete", CLASSES + scheme, "/f");
        DiagLog.log(ok ? "[unregister] removed " + scheme + "://"
                       : "[unregister] nothing removed for " + scheme + " (was it registered?)");
    }

    /** All registered FxSuite environments on this user account. */
    public static List<String> registeredEnvs() {
        List<String> envs = new ArrayList<>();
        String out = regCapture("query", CLASSES, "/f", "fxsuite-", "/k");
        for (String line : out.split("\\R")) {
            String s = line.trim();
            int i = s.indexOf("\\Software\\Classes\\fxsuite-");
            if (i < 0) continue;
            String tail = s.substring(i + "\\Software\\Classes\\fxsuite-".length());
            if (tail.contains("\\") || tail.isEmpty()) continue;   // only the top-level scheme key
            if (!envs.contains(tail)) envs.add(tail);
        }
        return envs;
    }

    public static void list() {
        List<String> envs = registeredEnvs();
        if (envs.isEmpty()) { DiagLog.log("[list] no FxSuite environments registered."); return; }
        for (String env : envs) {
            DiagLog.log("[list] " + EnvConfig.scheme(env) + "://   -> " + commandOf(env));
        }
    }

    /**
     * Remove registrations whose launcher jar no longer exists — dev environments churn,
     * so their scheme registrations would otherwise accumulate.
     */
    public static void prune() {
        int removed = 0;
        for (String env : registeredEnvs()) {
            String command = commandOf(env);
            Path jar = jarFromCommand(command);
            if (jar != null && !Files.isRegularFile(jar)) {
                DiagLog.log("[prune] " + EnvConfig.scheme(env) + " -> missing " + jar);
                unregister(env);
                removed++;
            }
        }
        DiagLog.log("[prune] removed " + removed + " stale registration(s).");
    }

    private static String commandOf(String envId) {
        String out = regCapture("query", CLASSES + EnvConfig.scheme(envId) + "\\shell\\open\\command", "/ve");
        for (String line : out.split("\\R")) {
            int i = line.indexOf("REG_SZ");
            if (i >= 0) return line.substring(i + "REG_SZ".length()).trim();
        }
        return "";
    }

    /** Pull the {@code -jar "<path>"} argument out of a registered command string. */
    private static Path jarFromCommand(String command) {
        int i = command.indexOf("-jar ");
        if (i < 0) return null;
        String rest = command.substring(i + 5).trim();
        if (!rest.startsWith("\"")) return null;
        int end = rest.indexOf('"', 1);
        if (end < 0) return null;
        try { return Path.of(rest.substring(1, end)); } catch (Exception e) { return null; }
    }

    /** Absolute path to the jar (or classes dir) this class was loaded from. */
    static Path ownJarPath() {
        try {
            var src = ProtocolRegistrar.class.getProtectionDomain().getCodeSource();
            if (src == null || src.getLocation() == null) return null;
            return Path.of(src.getLocation().toURI()).toAbsolutePath();
        } catch (URISyntaxException | IllegalArgumentException e) {
            return null;
        }
    }

    private static String javawExe() {
        return Path.of(System.getProperty("java.home"), "bin", "javaw.exe").toString();
    }

    private static String quote(String s) { return "\"" + s + "\""; }

    private static String escapeRegValue(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /** Import a .reg file — the command value contains quotes that argv quoting cannot carry. */
    private static boolean importReg(String regText) {
        Path tmp = null;
        try {
            tmp = Files.createTempFile("fxsuite-register-", ".reg");
            String withBom = '﻿' + regText;   // .reg "Version 5.00" wants a UTF-16LE BOM
            Files.write(tmp, withBom.getBytes(StandardCharsets.UTF_16LE));
            return reg("import", tmp.toString());
        } catch (IOException e) {
            DiagLog.log("[register] could not write .reg file: " + e);
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
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).inheritIO().start();
            int code = p.waitFor();
            if (code != 0) DiagLog.log("[reg] exit " + code + " for: " + String.join(" ", cmd));
            return code == 0;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            DiagLog.log("[reg] failed to run reg.exe: " + e);
            return false;
        }
    }

    /** Same as {@link #reg} but returns stdout instead of inheriting it. */
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
