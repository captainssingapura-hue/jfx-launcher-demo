package com.example.fxsuite.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * The <b>pinned</b> artifact repository the launcher downloads app jars from —
 * a Nexus/Artifactory (raw-hosted) style base URL.
 *
 * <p>This is trusted, operator-controlled configuration: it comes from
 * {@code launcher.properties} next to the launcher jar (or a built-in default),
 * <b>never</b> from the launch URL or the token. That is deliberate — the token
 * chooses <i>which app/version</i>, but it can never redirect the download to an
 * attacker-controlled host. The artifact path is a fixed pattern; {@code app}
 * and {@code ver} have already passed strict charset validation.</p>
 */
public record RepoConfig(String base) {

    private static final String DEFAULT_BASE = "http://localhost:8087";

    public static RepoConfig load() {
        String base = DEFAULT_BASE;
        try {
            Path props = Install.root().resolve("launcher.properties");
            if (Files.isRegularFile(props)) {
                Properties p = new Properties();
                try (InputStream in = Files.newInputStream(props)) {
                    p.load(in);
                }
                base = p.getProperty("repo.base", DEFAULT_BASE).trim();
            }
        } catch (LaunchException | IOException e) {
            // Install dir not resolvable (e.g. running from classes) — use default.
            DiagLog.log("[repo] using default repo base (" + DEFAULT_BASE + "): " + e.getMessage());
        }
        // strip a trailing slash for clean concatenation
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return new RepoConfig(base);
    }

    /**
     * Deterministic artifact location: {@code <base>/apps/<app>/<ver>/app-<app>-<ver>.jar}.
     */
    public URI artifactUri(String app, String ver) {
        return URI.create(base + "/apps/" + app + "/" + ver + "/app-" + app + "-" + ver + ".jar");
    }
}
