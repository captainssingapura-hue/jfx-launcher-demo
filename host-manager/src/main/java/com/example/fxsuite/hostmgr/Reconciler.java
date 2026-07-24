package com.example.fxsuite.hostmgr;

import com.example.fxsuite.hostmgr.model.AppSpec;
import com.example.fxsuite.hostmgr.model.DesiredState;
import com.example.fxsuite.hostmgr.model.UnitId;
import com.example.fxsuite.hostmgr.model.UnitState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The heart of the host manager: given what the operator declared and what is actually
 * running, decide what to change. This is a <b>pure function</b> — no I/O, no processes — so
 * the convergence rules can be pinned by tests and reasoned about in isolation.
 *
 * <p>The rules are the whole day-to-day operations story:</p>
 * <ul>
 *   <li><b>install</b> — declared but not running → start it;</li>
 *   <li><b>upgrade</b> — running the wrong version → replace it;</li>
 *   <li><b>self-heal</b> — declared and right version but not active (it crashed, or the host
 *       rebooted) → start it again. This is what makes the manager low-touch: nobody is paged
 *       to restart a service that fell over;</li>
 *   <li><b>decommission</b> — running but no longer declared (or declared disabled) → stop it.</li>
 * </ul>
 *
 * <p>An app that is declared, at the right version, and active needs nothing — the common
 * case, so a steady host does no work each tick.</p>
 */
public final class Reconciler {

    /** Why a unit is in the apply list — for logs and the reason column in ops. */
    public enum Reason { INSTALL, UPGRADE, HEAL }

    public record Apply(AppSpec spec, Reason reason) {}

    /** The change set for one tick. Empty {@link #isEmpty()} means the host already matches. */
    public record Plan(List<Apply> toApply, List<UnitId> toStop) {
        public boolean isEmpty() {
            return toApply.isEmpty() && toStop.isEmpty();
        }
    }

    private Reconciler() {}

    public static Plan plan(DesiredState desired, List<UnitState> actual) {
        Map<UnitId, UnitState> running = new HashMap<>();
        for (UnitState u : actual) running.put(u.id(), u);

        List<Apply> toApply = new ArrayList<>();
        Set<UnitId> declared = new HashSet<>();

        for (AppSpec spec : desired.enabledApps()) {
            UnitId id = spec.id();
            declared.add(id);
            UnitState now = running.get(id);
            if (now == null || !now.active()) {
                // Not running at all — either never started or fell over. Same remedy.
                toApply.add(new Apply(spec, now == null ? Reason.INSTALL : Reason.HEAL));
            } else if (!spec.version().equals(now.version())) {
                toApply.add(new Apply(spec, Reason.UPGRADE));
            }
            // declared + active + right version → nothing to do
        }

        // Anything running that is no longer declared (or was disabled) is decommissioned.
        List<UnitId> toStop = new ArrayList<>();
        for (UnitState u : actual) {
            if (!declared.contains(u.id())) toStop.add(u.id());
        }

        return new Plan(toApply, toStop);
    }
}
