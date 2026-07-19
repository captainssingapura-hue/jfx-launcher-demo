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

    private DiagLog() {}

    public static Path logFile() {
        String base = System.getenv("LOCALAPPDATA");
        if (base == null || base.isBlank()) {
            base = System.getProperty("java.io.tmpdir");
        }
        return Path.of(base, "fxsuite", "launch.log");
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
