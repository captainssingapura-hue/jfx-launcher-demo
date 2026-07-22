package com.example.fxsuite.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * The apps this launcher carries directly, read from the bundled
 * {@code /fxsuite/bundled-apps.properties} resource.
 *
 * <p>A bundled app's classes are shaded into the launcher's own {@code -app} jar, so
 * launching it needs no download and no separate jar — the launcher spawns it from its
 * own classpath. This is the "managed apps, carried by the env launcher" model: the set
 * (and the version of each) is fixed by what the jar was built with.</p>
 */
public final class BundledApps {

    /** @param mainClass entry point of the bundled app; @param version the bundled version */
    public record Bundled(String appId, String mainClass, String version) {}

    private static final Map<String, Bundled> APPS = load();

    private BundledApps() {}

    public static Optional<Bundled> find(String appId) {
        return Optional.ofNullable(APPS.get(appId));
    }

    public static java.util.Collection<Bundled> all() {
        return APPS.values();
    }

    private static Map<String, Bundled> load() {
        Map<String, Bundled> out = new LinkedHashMap<>();
        try (InputStream in = BundledApps.class.getResourceAsStream("/fxsuite/bundled-apps.properties")) {
            if (in == null) return out;
            Properties p = new Properties();
            p.load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
            for (String appId : p.stringPropertyNames()) {
                // value = "<main class> ; <version>"
                String[] parts = p.getProperty(appId).split(";");
                String mainClass = parts[0].trim();
                String version = parts.length > 1 ? parts[1].trim() : "bundled";
                if (!mainClass.isEmpty()) out.put(appId.trim(), new Bundled(appId.trim(), mainClass, version));
            }
        } catch (IOException ignored) {
            // no bundled apps — the launcher falls back to the repo for everything
        }
        return out;
    }
}
