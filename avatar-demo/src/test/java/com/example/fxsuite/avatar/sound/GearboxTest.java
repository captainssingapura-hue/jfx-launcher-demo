package com.example.fxsuite.avatar.sound;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A gearbox following a vehicle driven by something else: up through its gears as it gathers
 * speed, the throttle cut at each change; down as it slows, to idle; up early cruising, down
 * flooring it; and in top, flat out, a little short of the red line.
 */
class GearboxTest {

    private static final double STEP = 1.0 / 60;
    private static final Engine V6 = Engine.V6;
    private static final Gearbox SIX = new Gearbox(6, 0.22, 0.85, 0.25);

    @Test
    void itsGearsGoFromAShortFirstToATopThatTakesItToTheTopSpeed() {
        assertEquals(0.22, SIX.reach(1), 1e-12);
        assertEquals(1 / 0.85, SIX.reach(6), 1e-12);
        for (int gear = 2; gear <= 6; gear++) {
            assertTrue(SIX.reach(gear) > SIX.reach(gear - 1));
        }
        assertEquals(0.85 * V6.redline(), SIX.rpm(V6, 6, 1), 1e-9, "flat out in top, short of the red line");
        assertThrows(IllegalArgumentException.class, () -> new Gearbox(-1, 0.2, 0.85, 0.2));
        assertThrows(IllegalArgumentException.class, () -> Gearbox.cvt(1.2, 0.85), "a CVT holds no further than the red line");
        assertThrows(IllegalArgumentException.class, () -> new Gearbox(5, 1.5, 0.85, 0.2));
    }

    @Test
    void gatheringSpeedItGoesUpThroughEveryGearTheThrottleCutAtEachChange() {
        Gearbox.Geared geared = SIX.stopped(V6);
        int highest = 1;
        int changes = 0;
        for (int i = 0; i < 20 / STEP; i++) {
            final double share = Math.min(1, i * STEP / 18);
            final Gearbox.Geared next = SIX.step(V6, geared, share, 1, false, STEP);
            if (next.clutch() == Driving.Clutch.SHIFTING) {
                assertEquals(0, next.firing(), "the throttle cut, changing up");
                if (geared.clutch() != Driving.Clutch.SHIFTING) {
                    changes++;
                }
            }
            if (geared.clutch() == Driving.Clutch.SHIFTING && next.clutch() != Driving.Clutch.SHIFTING) {
                assertTrue(next.rpm() < 0.85 * V6.redline(), "the revs down in the new gear: " + next);
            }
            geared = next;
            highest = Math.max(highest, geared.gear());
            assertTrue(geared.rpm() <= V6.redline() * 1.02, "never past the red line: " + geared);
        }
        assertEquals(6, highest);
        assertEquals(5, changes, "each gear once");
        assertEquals(0.85 * V6.redline(), geared.rpm(), 1, "flat out in top");
    }

    @Test
    void slowingOffTheThrottleItComesDownThroughTheGearsToIdle() {
        Gearbox.Geared geared = new Gearbox.Geared(6, SIX.rpm(V6, 6, 1), 0, 6, Driving.Clutch.IN, 0, false);
        int lowest = 6;
        for (int i = 0; i < 16 / STEP; i++) {
            final double share = Math.max(0, 1 - i * STEP / 14);
            geared = SIX.step(V6, geared, share, 0, false, STEP);
            lowest = Math.min(lowest, geared.gear());
        }
        assertEquals(1, lowest, "down to first");
        assertEquals(Driving.Clutch.OUT, geared.clutch(), "stopped, the clutch out");
        assertEquals(V6.idle(), geared.rpm(), 5, "idling");
    }

    @Test
    void cruisingItSitsInAHighGearAndFlooringItKicksDown() {
        Gearbox.Geared geared = SIX.stopped(V6);
        for (int i = 0; i < 12 / STEP; i++) {
            geared = SIX.step(V6, geared, Math.min(0.5, i * STEP / 8), i * STEP < 8 ? 1 : 0.3, false, STEP);
        }
        final int cruising = geared.gear();
        assertTrue(cruising >= 5, "cruising at half its top speed in a high gear: " + geared);
        for (int i = 0; i < 1 / STEP; i++) {
            geared = SIX.step(V6, geared, 0.5, 1, false, STEP);
        }
        assertTrue(geared.gear() < cruising, "floored, down a gear: " + geared);
    }

    @Test
    void goingBackItIsInItsFirstAndShowsR() {
        Gearbox.Geared geared = SIX.stopped(V6);
        for (int i = 0; i < 2 / STEP; i++) {
            geared = SIX.step(V6, geared, -0.1, 1, true, STEP);
        }
        assertEquals("R", geared.shown());
        assertEquals(1, geared.gear());
        assertEquals(SIX.rpm(V6, 1, 0.1), geared.rpm(), 1e-6, "in its first, going back");
    }

    @Test
    void aCvtHoldsItsRevsAsTheSpeedBuildsThenRisesWithIt() {
        final Engine single = Engine.SCOOTER;
        final Gearbox cvt = Gearbox.cvt(0.74, 0.85);
        assertTrue(cvt.continuous());
        Gearbox.Geared geared = cvt.stopped(single);
        assertEquals("D", geared.shown());
        double least = Double.MAX_VALUE;
        double most = 0;
        for (int i = 0; i < 20 / STEP; i++) {
            final double share = Math.min(1, i * STEP / 16);
            geared = cvt.step(single, geared, share, 1, false, STEP);
            if (share > 0.2 && share < 0.5) {
                least = Math.min(least, geared.rpm());
                most = Math.max(most, geared.rpm());
            }
        }
        assertEquals(0.74 * single.redline(), (least + most) / 2, 0.05 * single.redline(), "held flat out");
        assertTrue(most - least < 0.05 * single.redline(), "steady as the speed builds: " + least + " to " + most);
        assertEquals(0.85 * single.redline(), geared.rpm(), 0.02 * single.redline(), "out of ratio, at its top speed");
        assertEquals("D", geared.shown());
        for (int i = 0; i < 4 / STEP; i++) {
            geared = cvt.step(single, geared, 0, 0, false, STEP);
        }
        assertEquals(Driving.Clutch.OUT, geared.clutch(), "stopped, its clutch let go");
        assertEquals(single.idle(), geared.rpm(), 5);
    }
}
