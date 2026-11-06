package com.example.fxsuite.launcher;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Finds a Java path that is safe to write into the registry.
 *
 * <p>{@code java.home} is a versioned folder such as {@code jdk-25.0.2}, which disappears at the
 * next JDK update, and a handler naming it breaks every web link with no way for FxSuite to
 * report it. Oracle's installer also keeps {@code Java\latest\jdk-25}, a link that follows
 * updates within that feature release and never crosses to one the jar was not built for.</p>
 *
 * <p>Other JDK distributions have no such link, so they fall back to {@code java.home}. The
 * launcher window then reports a handler whose Java has gone, rather than repairing it, because
 * registry changes are always shown before they are made.</p>
 */
public final class StableJava {

    private StableJava() {}

    public static Path javaw() {
        return javaw(Path.of(System.getProperty("java.home")), Runtime.version().feature());
    }

    static Path javaw(Path javaHome, int feature) {
        Path parent = javaHome.toAbsolutePath().getParent();
        if (parent != null) {
            Path stable = parent.resolve("latest").resolve("jdk-" + feature)
                    .resolve("bin").resolve("javaw.exe");
            if (Files.isRegularFile(stable)) return stable;
        }
        return javaHome.resolve("bin").resolve("javaw.exe");
    }
}
