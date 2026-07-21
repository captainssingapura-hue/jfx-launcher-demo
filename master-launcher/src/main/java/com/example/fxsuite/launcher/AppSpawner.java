package com.example.fxsuite.launcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Launches a resolved app jar in its OWN process, sharing the common JavaFX jar.
 *
 * <p>The spawned command is a direct {@code CreateProcess} of {@code javaw.exe}
 * with an argument vector — no shell, no {@code cmd.exe}:</p>
 * <pre>
 *   javaw --enable-native-access=ALL-UNNAMED
 *         -cp "&lt;app jar&gt;;&lt;fxsuite-javafx.jar&gt;" &lt;MainClass&gt; &lt;app args…&gt;
 * </pre>
 *
 * <p>The app jar is the cached, integrity-verified file from {@link AppFetcher};
 * its main class comes from that verified jar's manifest.</p>
 */
public final class AppSpawner {

    private AppSpawner() {}

    public static void spawn(String envId, Path appJar, String mainClass, List<String> appArgs)
            throws LaunchException {
        // The launcher's own (self-contained) jar provides JavaFX to the app.
        Path javafxProvider = Install.javafxProviderJar();
        if (!Files.isRegularFile(appJar)) {
            throw new LaunchException("App jar is missing: " + appJar);
        }

        String javaw = Path.of(System.getProperty("java.home"), "bin", "javaw.exe").toString();
        String classpath = appJar + File.pathSeparator + javafxProvider;

        List<String> cmd = new ArrayList<>();
        cmd.add(javaw);
        cmd.add("--enable-native-access=ALL-UNNAMED");   // silence JDK 25 native-access warning
        // Tell the app which environment it belongs to, so it can render unmistakable
        // environment chrome and connect to the right backend.
        cmd.add("-Dfxsuite.env=" + envId);
        cmd.add("-cp");
        cmd.add(classpath);
        cmd.add(mainClass);
        cmd.addAll(appArgs);

        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            // The launcher runs windowless (javaw), so fold the child's stdout
            // into the shared diagnostic log for the POC.
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(DiagLog.logFile().toFile()));
            Process p = pb.start();
            DiagLog.log("spawned " + mainClass + " pid=" + p.pid());
            DiagLog.log("  cp=" + classpath);
            // Do not waitFor(): the app is independent and must outlive the launcher.
        } catch (IOException e) {
            throw new LaunchException("Failed to start app process: " + e.getMessage());
        }
    }
}
