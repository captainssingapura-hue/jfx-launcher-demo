package com.example.fxsuite.launcher.env;

/**
 * An app an environment build carries directly.
 *
 * <p>Its classes are shaded into the environment jar, so launching it needs no download and
 * no separate jar — the launcher spawns it from its own classpath, which also supplies
 * JavaFX.</p>
 *
 * <p>Declare one with {@link #of(String, Class, String)} so the entry point is a compile-time
 * reference: a renamed or missing main class breaks the build rather than the launch.</p>
 */
public record BundledApp(String appId, String mainClassName, String version) {

    public static BundledApp of(String appId, Class<?> mainClass, String version) {
        return new BundledApp(appId, mainClass.getName(), version);
    }
}
