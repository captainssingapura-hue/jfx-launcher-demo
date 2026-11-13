package com.example.fxsuite.avatar.sound;

import com.example.fxsuite.avatar.sim.Course;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The game's gearboxes, the engine bench's vehicles': at any speed in any gear, the same revs as
 * the bench gives; up through every gear to the vehicle's own top speed, the throttle cut at each
 * change; down to idle as it slows; up early cruising, down flooring it; R going back; and a
 * scooter's CVT holding its revs as the speed builds.
 */
class GearboxTest {

    private static final double STEP = 1.0 / 60;
    private static final Vehicle CAR = Vehicle.CAR;
    private static final Gearbox SIX = new Gearbox(CAR);

    /** How fast {@code vehicle} goes flat out, on the bench, in metres a second. */
    private static double top(final Vehicle vehicle) {
        Driving driving = Driving.stopped(vehicle);
        double most = 0;
        for (int i = 0; i < 120 * 120; i++) {
            driving = driving.step(vehicle, 1, 0, 0, true, 1.0 / 120);
            most = Math.max(most, driving.speed());
        }
        return most;
    }

    @Test
    void atAnySpeedInAnyGearTheRevsAreTheBenchs() {
        for (final Course.Chassis chassis : Course.CHASSES) {
            final Vehicle vehicle = Vehicle.of(chassis);
            final Gearbox gearbox = new Gearbox(vehicle);
            if (gearbox.continuous()) {
                continue;
            }
            // the bench driving it flat out, and the gearbox following the same speed, in the same gear
            Driving driving = Driving.stopped(vehicle);
            int compared = 0;
            for (int i = 0; i < 30 * 120; i++) {
                driving = driving.step(vehicle, 1, 0, 0, true, 1.0 / 120);
                if (driving.clutch() == Driving.Clutch.IN) {
                    final Gearbox.Geared same = new Gearbox.Geared(driving.gear(), driving.rpm(), 0, driving.gear(),
                            Driving.Clutch.IN, 1, false);
                    final Gearbox.Geared next = gearbox.step(same, driving.speed(), 0.6, false, 0);
                    if (next.clutch() == Driving.Clutch.IN && next.gear() == driving.gear()) {
                        assertEquals(driving.rpm(), next.rpm(), 1e-6, chassis.name() + " at " + driving.kmh() + " km/h");
                        compared++;
                    }
                }
            }
            assertTrue(compared > 100, chassis.name() + " compared all the way up");
        }
    }

    @Test
    void gatheringSpeedItGoesUpThroughEveryGearToItsTopSpeedTheThrottleCutAtEachChange() {
        final double top = top(CAR);
        Gearbox.Geared geared = SIX.stopped();
        int highest = 1;
        int changes = 0;
        for (int i = 0; i < 30 / STEP; i++) {
            final double speed = Math.min(top, top * i * STEP / 26);
            final Gearbox.Geared next = SIX.step(geared, speed, 1, false, STEP);
            if (next.clutch() == Driving.Clutch.SHIFTING) {
                assertEquals(0, next.firing(), "the throttle cut, changing up");
                if (geared.clutch() != Driving.Clutch.SHIFTING) {
                    changes++;
                }
            }
            geared = next;
            highest = Math.max(highest, geared.gear());
            assertTrue(geared.rpm() <= CAR.engine().redline() * 1.02, "never past the red line: " + geared);
        }
        assertEquals(CAR.count(), highest);
        assertEquals(CAR.count() - 1, changes, "each gear once");
        assertEquals(CAR.rpm(CAR.count(), top), geared.rpm(), 1, "flat out in top, as on the bench");
    }

    @Test
    void slowingOffTheThrottleItComesDownThroughTheGearsToIdle() {
        final double top = top(CAR);
        Gearbox.Geared geared = new Gearbox.Geared(6, CAR.rpm(6, top), 0, 6, Driving.Clutch.IN, 0, false);
        int lowest = 6;
        for (int i = 0; i < 22 / STEP; i++) {
            geared = SIX.step(geared, Math.max(0, top * (1 - i * STEP / 20)), 0, false, STEP);
            lowest = Math.min(lowest, geared.gear());
        }
        assertEquals(1, lowest, "down to first");
        assertEquals(Driving.Clutch.OUT, geared.clutch(), "stopped, the clutch out");
        assertEquals(CAR.engine().idle(), geared.rpm(), 5, "idling");
    }

    @Test
    void cruisingItSitsInAHighGearAndFlooringItKicksDown() {
        // 90 km/h, cruising, after pulling up to it
        final double cruise = 25;
        Gearbox.Geared geared = SIX.stopped();
        for (int i = 0; i < 16 / STEP; i++) {
            geared = SIX.step(geared, Math.min(cruise, i * STEP * 3), i * STEP < 9 ? 1 : 0.3, false, STEP);
        }
        final int cruising = geared.gear();
        assertTrue(cruising >= 5, "cruising in a high gear: " + geared);
        for (int i = 0; i < 1 / STEP; i++) {
            geared = SIX.step(geared, cruise, 1, false, STEP);
        }
        assertTrue(geared.gear() < cruising, "floored, down a gear: " + geared);
    }

    @Test
    void goingBackItIsInItsFirstAndShowsR() {
        Gearbox.Geared geared = SIX.stopped();
        for (int i = 0; i < 2 / STEP; i++) {
            geared = SIX.step(geared, -3, 1, true, STEP);
        }
        assertEquals("R", geared.shown());
        assertEquals(1, geared.gear());
        assertEquals(CAR.rpm(1, 3), geared.rpm(), 1e-6, "in its first, going back");
    }

    @Test
    void aCvtHoldsItsRevsAsTheSpeedBuildsThenRisesWithIt() {
        final Vehicle scooter = Vehicle.SCOOTER_125;
        final Engine single = scooter.engine();
        final Gearbox cvt = new Gearbox(scooter);
        assertTrue(cvt.continuous());
        final double top = top(scooter);
        Gearbox.Geared geared = cvt.stopped();
        assertEquals("D", geared.shown());
        double least = Double.MAX_VALUE;
        double most = 0;
        for (int i = 0; i < 24 / STEP; i++) {
            final double speed = Math.min(top, top * i * STEP / 20);
            geared = cvt.step(geared, speed, 1, false, STEP);
            if (speed > 0.2 * top && speed < 0.5 * top) {
                least = Math.min(least, geared.rpm());
                most = Math.max(most, geared.rpm());
            }
        }
        assertEquals(scooter.cvt(), (least + most) / 2, 0.05 * single.redline(), "held flat out");
        assertTrue(most - least < 0.05 * single.redline(), "steady as the speed builds: " + least + " to " + most);
        assertEquals("D", geared.shown());
        for (int i = 0; i < 4 / STEP; i++) {
            geared = cvt.step(geared, 0, 0, false, STEP);
        }
        assertEquals(Driving.Clutch.OUT, geared.clutch(), "stopped, its clutch let go");
        assertEquals(single.idle(), geared.rpm(), 5);
    }
}
