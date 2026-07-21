package com.example.fxsuite.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Which environment this launcher instance serves, and where that environment's
 * artifacts live.
 *
 * <p>Resolution order (highest first):</p>
 * <ol>
 *   <li>command-line {@code --env=} / {@code --base=} — how multiplexed dev
 *       environments are distinguished (one shared binary, different args);</li>
 *   <li>{@code launcher.properties} next to the jar ({@code env=}, {@code repo.base=}) —
 *       how singleton environments (Prod, UAT) are configured, since each has its
 *       own dedicated install;</li>
 *   <li>built-in default for {@code repo.base}.</li>
 * </ol>
 *
 * <p>The environment is therefore fixed by <b>installation and registration</b>, never
 * by anything inside the launch URL.</p>
 */
public record EnvConfig(String envId, String repoBase) {

    /** Environment ids: lowercase alphanumeric + hyphen (also used in paths and scheme names). */
    public static final Pattern ENV_ID = Pattern.compile("[a-z0-9][a-z0-9-]{0,31}");

    private static final String DEFAULT_BASE = "http://localhost:8087";

    public static EnvConfig load(String argEnv, String argBase) {
        Properties p = properties();
        String env = firstNonBlank(argEnv, p.getProperty("env"));
        String base = firstNonBlank(argBase, p.getProperty("repo.base"), DEFAULT_BASE).trim();
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return new EnvConfig(env == null ? null : env.trim(), base);
    }

    /** @throws LaunchException if this launcher has no usable environment id */
    public String requireEnvId() throws LaunchException {
        if (envId == null || !ENV_ID.matcher(envId).matches()) {
            throw new LaunchException("No valid environment configured. Pass --env=<id> or set "
                    + "env= in launcher.properties (found: " + envId + ")");
        }
        return envId;
    }

    /** The URL scheme this launcher answers, e.g. {@code fxsuite-prod}. */
    public static String scheme(String envId) {
        return "fxsuite-" + envId;
    }

    /** {@code <base>/apps/<app>/<ver>/app-<app>-<ver>.jar} — fixed pattern, never from the URL. */
    public URI artifactUri(String app, String ver) {
        return URI.create(repoBase + "/apps/" + app + "/" + ver + "/app-" + app + "-" + ver + ".jar");
    }

    private static Properties properties() {
        Properties p = new Properties();
        try {
            Path f = Install.root().resolve("launcher.properties");
            if (Files.isRegularFile(f)) {
                try (InputStream in = Files.newInputStream(f)) { p.load(in); }
            }
        } catch (LaunchException | IOException e) {
            // Not resolvable (e.g. running from classes) — fall back to args/defaults.
        }
        return p;
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) if (v != null && !v.isBlank()) return v;
        return null;
    }
}
