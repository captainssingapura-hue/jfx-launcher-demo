package com.example.fxsuite.launcher.setup;

import java.util.List;

/**
 * An ordered, reviewable set of registry changes for one environment.
 *
 * <p>Nothing is written when a plan is built — a plan is exactly what the setup app
 * shows the user before asking for consent, and it is also how the reverse operation
 * is presented.</p>
 *
 * @param action    what this plan does
 * @param envId     the environment it concerns
 * @param scheme    the URL scheme, e.g. {@code fxsuite-prod}
 * @param ops       the changes, in the order they will be applied
 * @param blocker   why the plan cannot be applied, or null when it can
 */
public record RegistryPlan(Action action, String envId, String scheme,
                           List<RegistryOp> ops, String blocker) {

    public enum Action { INSTALL, UNINSTALL }

    public boolean applicable() { return blocker == null && !ops.isEmpty(); }

    /** Multi-line rendering of every change, for display before consent. */
    public String describe() {
        if (blocker != null) return "Cannot proceed: " + blocker;
        if (ops.isEmpty()) return "Nothing to do — already in the requested state.";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ops.size(); i++) {
            sb.append(i + 1).append(". ").append(ops.get(i).describe()).append(System.lineSeparator());
        }
        return sb.toString();
    }
}
