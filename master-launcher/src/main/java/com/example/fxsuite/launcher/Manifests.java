package com.example.fxsuite.launcher;

import java.io.IOException;
import java.nio.file.Path;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/** Reads the entry point from a (already integrity-verified) app jar's manifest. */
public final class Manifests {

    private Manifests() {}

    /**
     * @return the jar's {@code Main-Class}
     * @throws LaunchException if the jar has no manifest / no Main-Class
     */
    public static String mainClass(Path jar) throws LaunchException {
        try (JarFile jf = new JarFile(jar.toFile())) {
            Manifest mf = jf.getManifest();
            String main = mf == null ? null : mf.getMainAttributes().getValue("Main-Class");
            if (main == null || main.isBlank()) {
                throw new LaunchException("App jar has no Main-Class: " + jar.getFileName());
            }
            return main.trim();
        } catch (IOException e) {
            throw new LaunchException("Could not read app jar manifest: " + e.getMessage());
        }
    }
}
