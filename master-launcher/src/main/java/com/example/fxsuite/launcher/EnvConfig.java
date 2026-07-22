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
 *   <li>the <b>baked-in</b> {@code /fxsuite/launcher-env.properties} resource — how a
 *       singleton (Prod, UAT) jar identifies itself, so it needs no companion file and
 *       no {@code --env} argument;</li>
 *   <li>{@code launcher.properties} next to the jar — an external override;</li>
 *   <li>built-in default for {@code repo.base}.</li>
 * </ol>
 *
 * <p>The environment is therefore fixed by <b>the jar and its registration</b>, never
 * by anything inside the launch URL.</p>
 */
public record EnvConfig(String envId, String repoBase) {

    /** Environment ids: lowercase alphanumeric + hyphen (also used in paths and scheme names). */
    public static final Pattern ENV_ID = Pattern.compile("[a-z0-9][a-z0-9-]{0,31}");

    private static final String DEFAULT_BASE = "http://localhost:8087";

    private static final String BAKED_RESOURCE = "/fxsuite/launcher-env.properties";

    public static EnvConfig load(String argEnv, String argBase) {
        Properties baked = baked();
        Properties ext = properties();
        String env = firstNonBlank(argEnv, baked.getProperty("env"), ext.getProperty("env"));
        String base = firstNonBlank(argBase, baked.getProperty("repo.base"),
                ext.getProperty("repo.base"), DEFAULT_BASE).trim();
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return new EnvConfig(env == null ? null : env.trim(), base);
    }

    /** The environment baked into this jar, if any (present in singleton jars, absent in the dev jar). */
    public static String bakedEnv() {
        String e = baked().getProperty("env");
        return e == null || e.isBlank() ? null : e.trim();
    }

    /** The environment baked into a specific jar file, or null — used when registering it. */
    public static String bakedEnvOf(Path jar) {
        try (java.util.jar.JarFile jf = new java.util.jar.JarFile(jar.toFile())) {
            var entry = jf.getJarEntry("fxsuite/launcher-env.properties");
            if (entry == null) return null;
            Properties p = new Properties();
            try (InputStream in = jf.getInputStream(entry)) { p.load(in); }
            String e = p.getProperty("env");
            return e == null || e.isBlank() ? null : e.trim();
        } catch (IOException e) {
            return null;
        }
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

    private static Properties baked() {
        Properties p = new Properties();
        try (InputStream in = EnvConfig.class.getResourceAsStream(BAKED_RESOURCE)) {
            if (in != null) p.load(in);
        } catch (IOException ignored) {}
        return p;
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
