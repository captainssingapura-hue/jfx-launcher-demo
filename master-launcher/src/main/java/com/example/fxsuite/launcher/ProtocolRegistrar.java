package com.example.fxsuite.launcher;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Installs / removes the per-user {@code fxsuite://} custom protocol handler.
 *
 * <p>Everything is written under {@code HKEY_CURRENT_USER\Software\Classes},
 * which a normal user may modify without Administrator rights — so no UAC
 * prompt and no IT involvement. The registered command re-invokes <i>this
 * very jar</i> with the clicked URL as its argument:</p>
 *
 * <pre>
 *   HKCU\Software\Classes\fxsuite\                 (default) = "URL:FxSuite Protocol"
 *                                                 "URL Protocol" = ""
 *   HKCU\Software\Classes\fxsuite\shell\open\command
 *        (default) = "&lt;javaw.exe&gt;" -jar "&lt;this.jar&gt;" "%1"
 * </pre>
 */
public final class ProtocolRegistrar {

    private static final String PROTOCOL = "fxsuite";
    private static final String ROOT = "HKCU\\Software\\Classes\\" + PROTOCOL;

    private ProtocolRegistrar() {}

    public static void register() {
        Path jar = ownJarPath();
        if (jar == null) {
            DiagLog.log("[register] ERROR: could not locate the running jar. "
                    + "Run this from the built jar: java -jar master-launcher.jar --register");
            return;
        }
        if (!jar.toString().toLowerCase().endsWith(".jar")) {
            DiagLog.log("[register] WARNING: not running from a .jar (found " + jar + "). "
                    + "The handler would point at a classes dir, not a runnable jar. "
                    + "Package first (mvn package) and register from the jar.");
        }

        String javaw = javawExe();
        // Exact command Windows runs on a click; %1 is substituted with the URL.
        String command = quote(javaw) + " -jar " + quote(jar.toString()) + " \"%1\"";

        // We import a .reg file rather than shelling out `reg add /d "<value>"`:
        // the command value contains both spaces and embedded quotes, which
        // cannot survive Windows argv quoting when passed to reg.exe. In a .reg
        // file the escaping rules are unambiguous (\\ and \").
        String reg = """
                Windows Registry Editor Version 5.00

                [HKEY_CURRENT_USER\\Software\\Classes\\%1$s]
                @="URL:FxSuite Protocol"
                "URL Protocol"=""

                [HKEY_CURRENT_USER\\Software\\Classes\\%1$s\\shell\\open\\command]
                @="%2$s"
                """.formatted(PROTOCOL, escapeRegValue(command));

        boolean ok = importReg(reg);
        if (ok) {
            DiagLog.log("[register] fxsuite:// registered for the current user.");
            DiagLog.log("[register] command = " + command);
            DiagLog.log("[register] Test it:  cmd /c start \"\" \"fxsuite://launch/hello\"");
        } else {
            DiagLog.log("[register] FAILED — see reg.exe output above.");
        }
    }

    /** Write the .reg text to a temp file and import it with reg.exe. */
    private static boolean importReg(String regText) {
        Path tmp = null;
        try {
            tmp = Files.createTempFile("fxsuite-register-", ".reg");
            // .reg files are UTF-16LE with a BOM; reg.exe expects that for the
            // "Version 5.00" header.
            String withBom = '﻿' + regText;   // .reg "Version 5.00" wants a UTF-16LE BOM
            Files.write(tmp, withBom.getBytes(StandardCharsets.UTF_16LE));
            return reg("import", tmp.toString());
        } catch (IOException e) {
            DiagLog.log("[register] could not write .reg file: " + e);
            return false;
        } finally {
            if (tmp != null) {
                try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
            }
        }
    }

    /** Escape a string for use inside a double-quoted .reg value: \ -> \\, " -> \". */
    private static String escapeRegValue(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public static void unregister() {
        boolean ok = reg("delete", ROOT, "/f");
        DiagLog.log(ok ? "[unregister] fxsuite:// handler removed."
                       : "[unregister] nothing removed (was it registered?).");
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

    /** The windowless Java launcher from the JDK that is currently running us. */
    private static String javawExe() {
        return Path.of(System.getProperty("java.home"), "bin", "javaw.exe").toString();
    }

    private static String quote(String s) {
        return "\"" + s + "\"";
    }

    private static boolean reg(String... args) {
        List<String> cmd = new ArrayList<>();
        cmd.add("reg");
        cmd.addAll(List.of(args));
        try {
            Process p = new ProcessBuilder(cmd)
                    .redirectErrorStream(true)
                    .inheritIO()
                    .start();
            int code = p.waitFor();
            if (code != 0) {
                DiagLog.log("[reg] exit " + code + " for: " + String.join(" ", cmd));
            }
            return code == 0;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            DiagLog.log("[reg] failed to run reg.exe: " + e);
            return false;
        }
    }
}
