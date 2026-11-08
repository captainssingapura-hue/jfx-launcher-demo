package com.example.fxsuite.hostmgr;

import com.example.fxsuite.hostmgr.model.AppSpec;
import com.example.fxsuite.hostmgr.model.DesiredState;
import com.example.fxsuite.hostmgr.model.UnitId;
import com.example.fxsuite.hostmgr.model.UnitState;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Convergence rules — the day-to-day operations behaviour, pinned. */
class ReconcilerTest {

    private static AppSpec spec(String app, String ver, String env, boolean enabled) {
        return new AppSpec(app, ver, "0".repeat(64), env, null, enabled);
    }

    private static UnitState running(String app, String env, String ver) {
        return new UnitState(new UnitId(app, env), ver, true, 100, 5);
    }

    private static DesiredState desired(AppSpec... apps) {
        return new DesiredState(1, "*", List.of(apps));
    }

    @Test
    void installsWhatIsDeclaredButNotRunning() {
        Reconciler.Plan p = Reconciler.plan(desired(spec("svc", "1.0.0", "prod", true)), List.of());
        assertEquals(1, p.toApply().size());
        assertEquals(Reconciler.Reason.INSTALL, p.toApply().get(0).reason());
        assertTrue(p.toStop().isEmpty());
    }

    @Test
    void upgradesWhenTheRunningVersionIsWrong() {
        Reconciler.Plan p = Reconciler.plan(
                desired(spec("svc", "1.1.0", "prod", true)),
                List.of(running("svc", "prod", "1.0.0")));
        assertEquals(1, p.toApply().size());
        assertEquals(Reconciler.Reason.UPGRADE, p.toApply().get(0).reason());
    }

    @Test
    void selfHealsWhenDeclaredAndRightVersionButDown() {
        UnitState crashed = new UnitState(new UnitId("svc", "prod"), "1.0.0", false, 0, 0);
        Reconciler.Plan p = Reconciler.plan(
                desired(spec("svc", "1.0.0", "prod", true)), List.of(crashed));
        assertEquals(1, p.toApply().size());
        assertEquals(Reconciler.Reason.HEAL, p.toApply().get(0).reason());
    }

    @Test
    void doesNothingWhenAlreadyConverged() {
        Reconciler.Plan p = Reconciler.plan(
                desired(spec("svc", "1.0.0", "prod", true)),
                List.of(running("svc", "prod", "1.0.0")));
        assertTrue(p.isEmpty());
    }

    @Test
    void decommissionsWhatIsNoLongerDeclared() {
        Reconciler.Plan p = Reconciler.plan(desired(), List.of(running("svc", "prod", "1.0.0")));
        assertEquals(List.of(new UnitId("svc", "prod")), p.toStop());
    }

    @Test
    void disabledSpecStopsTheRunningUnit() {
        Reconciler.Plan p = Reconciler.plan(
                desired(spec("svc", "1.0.0", "prod", false)),
                List.of(running("svc", "prod", "1.0.0")));
        assertTrue(p.toApply().isEmpty());
        assertEquals(List.of(new UnitId("svc", "prod")), p.toStop());
    }

    @Test
    void sameAppDifferentEnvsAreIndependentUnits() {
        Reconciler.Plan p = Reconciler.plan(
                desired(spec("svc", "1.0.0", "prod", true), spec("svc", "2.0.0", "uat", true)),
                List.of(running("svc", "prod", "1.0.0")));
        // prod is converged; uat must be installed
        assertEquals(1, p.toApply().size());
        assertEquals(new UnitId("svc", "uat"), p.toApply().get(0).spec().id());
        assertEquals(Reconciler.Reason.INSTALL, p.toApply().get(0).reason());
        assertTrue(p.toStop().isEmpty());
    }
}
