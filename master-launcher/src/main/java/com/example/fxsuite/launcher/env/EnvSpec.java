package com.example.fxsuite.launcher.env;

import com.example.fxsuite.launcher.LaunchException;

import java.util.List;
import java.util.Optional;

/**
 * What an environment build declares about itself.
 *
 * <p>This replaces the old {@code launcher-env.properties} / {@code bundled-apps.properties}
 * pair. Those files could only describe an environment by what they <i>omitted</i>: a build
 * with {@code env=prod} was a singleton, and one with no {@code env=} at all was the shared
 * dev build. That distinction is the most important thing about an environment, so it is now
 * a type rather than a missing line:</p>
 *
 * <ul>
 *   <li>{@link SingletonEnv} — one build serves exactly one environment (Prod, UAT). It knows
 *       its own id, so its registration needs no {@code --env} and a stray one is refused.</li>
 *   <li>{@link MultiplexedEnv} — one build serves a family of environments (dev1 … devN). It
 *       cannot know which one it is, so {@code --env} is <b>required</b> and is checked for
 *       membership in the family.</li>
 * </ul>
 *
 * <p>The interface is sealed: there are two kinds of environment and adding a third is a
 * deliberate act, not an accident of configuration.</p>
 *
 * <p>An environment build supplies its spec through {@link java.util.ServiceLoader} — see
 * {@link EnvSpecs}. Because the spec is code, the apps an environment carries are named by
 * {@code SomeApp.class} rather than by a string, so a typo or a missing dependency fails the
 * build instead of the launch.</p>
 */
public sealed interface EnvSpec permits SingletonEnv, MultiplexedEnv {

    /** The family this build belongs to: {@code prod}, {@code uat}, {@code dev}. */
    String family();

    /** Human name for window titles and messages, e.g. "Production". */
    String displayName();

    /** Where this environment's artifacts are published (apps it does not carry itself). */
    String repoBase();

    /** The apps this build carries directly, launchable with no download. */
    List<BundledApp> bundledApps();

    /**
     * The environment id this run serves.
     *
     * @param argEnv the {@code --env=} argument, or null
     * @throws LaunchException if the argument contradicts a singleton build, or is missing
     *         or out of family for a multiplexed one
     */
    String resolve(String argEnv) throws LaunchException;

    /** Whether this build may serve {@code envId}. */
    boolean accepts(String envId);

    /** True when one build serves several environments and must be told which. */
    boolean multiplexed();

    /** Whether a registration for this build has to carry {@code --env=}. */
    boolean requiresEnvArgument();

    /** Which environments this build serves, for messages: {@code "prod"}, {@code "dev1 … dev99"}. */
    String membership();

    /** The id to show when nothing has been chosen yet. */
    String suggestedId();

    /** Accent colour for this environment's chrome — Prod should never look like dev. */
    default String accentColour() {
        return "#607d8b";
    }

    default Optional<BundledApp> app(String appId) {
        return bundledApps().stream().filter(a -> a.appId().equals(appId)).findFirst();
    }
}
