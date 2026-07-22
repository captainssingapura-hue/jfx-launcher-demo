package com.example.fxsuite.launcher;

import com.example.fxsuite.launcher.env.BundledApp;
import com.example.fxsuite.launcher.env.EnvSpec;
import com.example.fxsuite.launcher.env.EnvSpecs;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Which environment this launcher instance serves, and where that environment's
 * artifacts live.
 *
 * <p>The environment comes from the {@link EnvSpec} the build carries, combined with the
 * {@code --env=} argument its registration supplies:</p>
 * <ul>
 *   <li>a {@link com.example.fxsuite.launcher.env.SingletonEnv} build (Prod, UAT) already
 *       knows its environment and rejects an argument naming another;</li>
 *   <li>a {@link com.example.fxsuite.launcher.env.MultiplexedEnv} build (dev) requires the
 *       argument and checks it belongs to the family.</li>
 * </ul>
 *
 * <p>The environment is therefore fixed by <b>the build and its registration</b>, never by
 * anything inside the launch URL. {@code repo.base} is the one value an operator may still
 * override without a rebuild — via {@code --base=} or a {@code launcher.properties} beside
 * the jar.</p>
 *
 * @param spec    the environment this build declares, or null when running the bare core
 * @param problem why no environment id could be resolved, ready to show the user
 */
public record EnvConfig(String envId, String repoBase, EnvSpec spec, String problem) {

    /** Environment ids: lowercase alphanumeric + hyphen (also used in paths and scheme names). */
    public static final Pattern ENV_ID = Pattern.compile("[a-z0-9][a-z0-9-]{0,31}");

    private static final String DEFAULT_BASE = "http://localhost:8087";

    public static EnvConfig load(String argEnv, String argBase) {
        EnvSpec spec = EnvSpecs.installed().orElse(null);
        String env = null, problem = null;

        if (spec != null) {
            try {
                env = spec.resolve(argEnv);
            } catch (LaunchException e) {
                problem = e.getMessage();
            }
        } else if (argEnv != null && !argEnv.isBlank()) {
            env = argEnv.trim();                     // bare core, driven entirely by arguments
        } else {
            problem = "This build declares no environment. Run one of the env/ builds "
                    + "(FxSuite-prod.jar, FxSuite-uat.jar, FxSuite-dev.jar) or pass --env=<id>.";
        }

        Properties ext = properties();
        String base = firstNonBlank(argBase, spec == null ? null : spec.repoBase(),
                ext.getProperty("repo.base"), DEFAULT_BASE).trim();
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);

        return new EnvConfig(env, base, spec, problem);
    }

    /** @throws LaunchException if this launcher has no usable environment id */
    public String requireEnvId() throws LaunchException {
        if (envId == null || !ENV_ID.matcher(envId).matches()) {
            throw new LaunchException(problem != null ? problem
                    : "No valid environment configured (found: " + envId + ")");
        }
        return envId;
    }

    /** The apps this build carries directly. */
    public List<BundledApp> bundledApps() {
        return spec == null ? List.of() : spec.bundledApps();
    }

    public Optional<BundledApp> bundledApp(String appId) {
        return spec == null ? Optional.empty() : spec.app(appId);
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
