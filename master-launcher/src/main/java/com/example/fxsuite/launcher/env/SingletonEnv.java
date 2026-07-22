package com.example.fxsuite.launcher.env;

import com.example.fxsuite.launcher.LaunchException;

/**
 * An environment with a dedicated build: Production, UAT.
 *
 * <p>The build <i>is</i> the environment. It needs no argument to know what it serves, its
 * registered command carries no {@code --env}, and a {@code --env} naming anything else is
 * an error rather than something to quietly honour — a prod launcher must never be talked
 * into serving dev.</p>
 *
 * <pre>{@code
 * public final class ProdEnv extends SingletonEnv {
 *     public ProdEnv() { super("prod", "Production"); }
 *     @Override public String repoBase() { return "https://nexus.internal/fxsuite/prod"; }
 *     @Override public List<BundledApp> bundledApps() {
 *         return List.of(BundledApp.of("hello", HelloMain.class, "1.0.0"));
 *     }
 * }
 * }</pre>
 */
public abstract non-sealed class SingletonEnv implements EnvSpec {

    private final String id;
    private final String displayName;

    protected SingletonEnv(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    @Override public final String family() { return id; }
    @Override public final String displayName() { return displayName; }
    @Override public final boolean multiplexed() { return false; }
    @Override public final boolean requiresEnvArgument() { return false; }
    @Override public final String membership() { return id; }
    @Override public final String suggestedId() { return id; }

    @Override
    public final boolean accepts(String envId) {
        return id.equals(envId);
    }

    @Override
    public final String resolve(String argEnv) throws LaunchException {
        if (argEnv != null && !argEnv.isBlank() && !argEnv.trim().equals(id)) {
            throw new LaunchException("This is the " + displayName + " (" + id + ") launcher; "
                    + "it cannot serve environment '" + argEnv.trim() + "'.");
        }
        return id;
    }
}
