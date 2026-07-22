package com.example.fxsuite.launcher.env;

import com.example.fxsuite.launcher.DiagLog;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Finds the {@link EnvSpec} a build carries.
 *
 * <p>The launcher core is environment-agnostic: it ships no spec at all. Each module under
 * {@code env/} contributes exactly one through {@link ServiceLoader}, which is what makes an
 * otherwise identical jar the Production launcher or the dev launcher.</p>
 */
public final class EnvSpecs {

    private static final Optional<EnvSpec> INSTALLED = discover(EnvSpecs.class.getClassLoader());

    private EnvSpecs() {}

    /** The spec this running build carries, or empty when running the bare core (or tests). */
    public static Optional<EnvSpec> installed() {
        return INSTALLED;
    }

    /**
     * What another launcher jar says about itself — used when registering it, to decide
     * whether its command needs {@code --env} and whether it may serve {@code envId} at all.
     *
     * <p>The jar's spec is instantiated in a throwaway class loader and reduced to the
     * answers below, so nothing from that jar outlives this call.</p>
     */
    public static Optional<JarEnv> ofJar(Path jar, String envId) {
        if (jar == null || !Files.isRegularFile(jar)) return Optional.empty();
        try (URLClassLoader cl = new JarOnlyServices(jar.toUri().toURL())) {
            return discover(cl).map(s -> new JarEnv(s.family(), s.displayName(), s.multiplexed(),
                    s.requiresEnvArgument(), s.accepts(envId), s.membership()));
        } catch (Exception e) {
            DiagLog.log("could not read the environment spec of " + jar + ": " + e);
            return Optional.empty();
        }
    }

    /**
     * A launcher jar's own account of itself, flattened for one question about one
     * environment id.
     *
     * @param accepts whether that jar may serve the environment id it was asked about
     */
    public record JarEnv(String family, String displayName, boolean multiplexed,
                         boolean requiresEnvArgument, boolean accepts, String membership) {}

    /**
     * Loads classes parent-first — so a spec read from another jar implements <i>our</i>
     * {@link EnvSpec} — but resolves <b>resources from that jar alone</b>.
     *
     * <p>Without the second half, a running launcher inspecting another jar would find both
     * service files and conclude the jar declares two environments, which is how the dev jar
     * once slipped past the check for being registered as Production.</p>
     */
    private static final class JarOnlyServices extends URLClassLoader {
        JarOnlyServices(URL jar) {
            super(new URL[]{jar}, EnvSpecs.class.getClassLoader());
        }

        @Override
        public URL getResource(String name) {
            return findResource(name);
        }

        @Override
        public java.util.Enumeration<URL> getResources(String name) throws java.io.IOException {
            return findResources(name);
        }
    }

    private static Optional<EnvSpec> discover(ClassLoader cl) {
        List<EnvSpec> found = new ArrayList<>();
        try {
            for (EnvSpec s : ServiceLoader.load(EnvSpec.class, cl)) found.add(s);
        } catch (Throwable t) {
            DiagLog.log("environment spec lookup failed: " + t);
            return Optional.empty();
        }
        if (found.isEmpty()) return Optional.empty();
        if (found.size() > 1) {
            // A build serves one environment. Two specs means two environments were shaded
            // together, and guessing which one is meant is exactly the wrong thing to do.
            DiagLog.log("this build declares " + found.size() + " environments ("
                    + found.stream().map(EnvSpec::family).toList() + ") — it must declare one");
            return Optional.empty();
        }
        return Optional.of(found.get(0));
    }
}
