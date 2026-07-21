package com.example.fxsuite.launcher;

import java.nio.file.Path;

/** Resolves the launcher's install layout and per-environment local state. */
public final class Install {

    private Install() {}

    /** Install root = the directory containing the launcher jar (per environment for singletons). */
    public static Path root() throws LaunchException {
        Path jar = ProtocolRegistrar.ownJarPath();
        if (jar == null || jar.getParent() == null) {
            throw new LaunchException("Could not locate the launcher install directory.");
        }
        return jar.getParent();
    }

    /** The shared JavaFX runtime jar (may be shared across environments). */
    public static Path sharedJavafxJar() throws LaunchException {
        Path own = root().resolve("lib/fxsuite-javafx.jar");
        // Singleton installs sit one level below a shared lib/ directory; accept either.
        return java.nio.file.Files.isRegularFile(own)
                ? own
                : root().getParent() == null ? own : root().getParent().resolve("lib/fxsuite-javafx.jar");
    }

    /** This environment's trust anchor, if the install provides one. */
    public static Path verifyKeyFile() throws LaunchException {
        return root().resolve("verify-key.x509.b64");
    }

    /** Per-user, <b>per-environment</b> state root: cache and logs never mix across envs. */
    public static Path envStateRoot(String envId) {
        String base = System.getenv("LOCALAPPDATA");
        if (base == null || base.isBlank()) base = System.getProperty("java.io.tmpdir");
        return Path.of(base, "fxsuite", envId);
    }

    /** Per-environment cache of downloaded app jars. */
    public static Path cacheRoot(String envId) {
        return envStateRoot(envId).resolve("cache");
    }
}
