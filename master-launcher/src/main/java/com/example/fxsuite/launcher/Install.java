package com.example.fxsuite.launcher;

import java.nio.file.Path;

/** Resolves the launcher's install layout, relative to its own jar. */
public final class Install {

    private Install() {}

    /** Install root = the directory containing the launcher jar. */
    public static Path root() throws LaunchException {
        Path jar = ProtocolRegistrar.ownJarPath();
        if (jar == null || jar.getParent() == null) {
            throw new LaunchException("Could not locate the launcher install directory.");
        }
        return jar.getParent();
    }

    /** The shared JavaFX runtime jar. */
    public static Path sharedJavafxJar() throws LaunchException {
        return root().resolve("lib/fxsuite-javafx.jar");
    }

    /** Per-user cache root for downloaded app jars. */
    public static Path cacheRoot() {
        String base = System.getenv("LOCALAPPDATA");
        if (base == null || base.isBlank()) {
            base = System.getProperty("java.io.tmpdir");
        }
        return Path.of(base, "fxsuite", "cache");
    }
}
