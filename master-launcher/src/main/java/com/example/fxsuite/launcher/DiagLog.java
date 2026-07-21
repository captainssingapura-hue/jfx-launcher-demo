package com.example.fxsuite.launcher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Append-only diagnostic log at {@code %LOCALAPPDATA%\fxsuite\launch.log}.
 *
 * <p>When the OS invokes the protocol handler there is no console to watch (the
 * registered command uses {@code javaw.exe}). This file is how we prove the
 * hand-off happened and see any parse/launch errors during the POC.</p>
 */
public final class DiagLog {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /** Environment this process serves; scopes the log file so environments never share one. */
    private static volatile String env = "unscoped";

    private DiagLog() {}

    public static void setEnv(String envId) {
        if (envId != null && !envId.isBlank()) env = envId;
    }

    public static Path logFile() {
        return Install.envStateRoot(env).resolve("launch.log");
    }

    public static synchronized void log(String message) {
        String line = TS.format(LocalDateTime.now()) + "  " + message + System.lineSeparator();
        try {
            Path f = logFile();
            Files.createDirectories(f.getParent());
            Files.writeString(f, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
            // Diagnostics must never break the launch.
        }
        // Also echo to stdout in case a console IS attached (e.g. --register runs).
        System.out.println(message);
    }
}
