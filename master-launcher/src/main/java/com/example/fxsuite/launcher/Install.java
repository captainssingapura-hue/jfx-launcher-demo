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

    /**
     * The jar that provides JavaFX to spawned apps — the launcher's own (self-contained)
     * jar. Because the launcher bundles JavaFX (classes + native dlls), putting this jar
     * on an app's classpath is all the app needs. Resolved from the running jar's
     * location, so there is no relative lib/ path to get wrong.
     */
    public static Path javafxProviderJar() throws LaunchException {
        Path jar = ProtocolRegistrar.ownJarPath();
        if (jar == null || !jar.toString().toLowerCase().endsWith(".jar")) {
            throw new LaunchException("Could not locate the launcher jar to provide JavaFX from "
                    + "(are you running from the built -app jar?).");
        }
        return jar;
    }

    /**
     * This environment's trust anchor beside the jar, if the install provides one.
     *
     * <p>Env-specific first ({@code verify-key-<env>.x509.b64}) so several per-environment
     * launcher jars can sit in one folder and still each trust their own key; a plain
     * {@code verify-key.x509.b64} serves as a shared fallback.</p>
     */
    public static Path verifyKeyFile(String envId) throws LaunchException {
        Path envSpecific = root().resolve("verify-key-" + envId + ".x509.b64");
        return java.nio.file.Files.isRegularFile(envSpecific)
                ? envSpecific
                : root().resolve("verify-key.x509.b64");
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
