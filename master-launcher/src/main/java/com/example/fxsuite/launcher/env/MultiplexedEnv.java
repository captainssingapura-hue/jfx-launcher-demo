package com.example.fxsuite.launcher.env;

import com.example.fxsuite.launcher.LaunchException;

import java.util.regex.Pattern;

/**
 * One build serving a family of environments: dev1 … devN.
 *
 * <p>Developer environments come and go too fast to justify a build each, so a single jar is
 * installed once and registered once per environment, each registration passing
 * {@code --env=devN}. The build cannot know which environment it is, and that is stated here
 * rather than inferred from a missing property: {@code --env} is required, and the value has
 * to be a member of the family — {@code --env=prod} against the dev build is refused, so a
 * mis-registration fails loudly instead of producing a launcher that quietly claims to be
 * Production.</p>
 *
 * <pre>{@code
 * public final class DevEnv extends MultiplexedEnv {
 *     public DevEnv() { super("dev", "Development", Pattern.compile("dev[1-9][0-9]?"), "dev1 … dev99"); }
 *     @Override public String repoBase() { return "https://nexus.internal/fxsuite/dev"; }
 *     ...
 * }
 * }</pre>
 */
public abstract non-sealed class MultiplexedEnv implements EnvSpec {

    private final String family;
    private final String displayName;
    private final Pattern members;
    private final String membership;

    /**
     * @param members   which ids this build may serve
     * @param membership how to describe that set to a human, e.g. {@code "dev1 … dev99"}
     */
    protected MultiplexedEnv(String family, String displayName, Pattern members, String membership) {
        this.family = family;
        this.displayName = displayName;
        this.members = members;
        this.membership = membership;
    }

    @Override public final String family() { return family; }
    @Override public final String displayName() { return displayName; }
    @Override public final boolean multiplexed() { return true; }
    @Override public final boolean requiresEnvArgument() { return true; }
    @Override public final String membership() { return membership; }

    /** Offered in the UI before the user picks one. */
    @Override public String suggestedId() { return family + "1"; }

    @Override
    public final boolean accepts(String envId) {
        return envId != null && members.matcher(envId).matches();
    }

    @Override
    public final String resolve(String argEnv) throws LaunchException {
        if (argEnv == null || argEnv.isBlank()) {
            throw new LaunchException("The " + displayName + " launcher serves several environments ("
                    + membership + "); it must be told which one with --env=<id>.");
        }
        String id = argEnv.trim();
        if (!accepts(id)) {
            throw new LaunchException("'" + id + "' is not a " + displayName + " environment; "
                    + "this launcher serves " + membership + ".");
        }
        return id;
    }
}
