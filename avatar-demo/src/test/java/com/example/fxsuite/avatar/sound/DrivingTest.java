package com.example.fxsuite.avatar.sound;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Driving, as the engines' sounds need it: pulling away on a slipping clutch, up through every
 * gear at the top of the revs, the engine turning with the wheels in gear; lifting off, it holds
 * back; braking to a stop, it idles; a scooter's CVT holds its revs as it gathers speed.
 */
class DrivingTest {

    private static final double STEP = 1.0 / 120;

    @Test
    void fromAStandstillItPullsAwayAndGoesUpThroughEveryGear() {
        for (final Vehicle vehicle : Vehicle.ALL) {
            if (vehicle.cvt() > 0) {
                continue;
            }
            final Engine engine = vehicle.engine();
            Driving driving = Driving.stopped(vehicle);
            final Set<Driving.Clutch> seen = new HashSet<>();
            int highest = 1;
            for (int i = 0; i < 60 / STEP; i++) {
                // flat out for most of a minute, then easing off to cruise, where an overdrive top gear is for
                driving = driving.step(vehicle, i < 45 / STEP ? 1 : 0.4, 0, 0, true, STEP);
                seen.add(driving.clutch());
                highest = Math.max(highest, driving.gear());
                assertTrue(driving.rpm() <= engine.redline() * 1.02, vehicle + " never past the red line: " + driving);
                if (driving.clutch() == Driving.Clutch.SHIFTING) {
                    assertTrue(driving.next() > driving.gear(), vehicle + " changing up: " + driving);
                    assertEquals(0, driving.load(), "the throttle off as it changes");
                }
                if (driving.clutch() == Driving.Clutch.IN) {
                    final double wheels = driving.speed() / vehicle.wheel() * 60 / (2 * Math.PI);
                    assertEquals(wheels * vehicle.ratio(driving.gear()), driving.rpm(), 1e-6,
                            vehicle + ": in gear, the engine turns with the wheels");
                }
            }
            assertTrue(seen.contains(Driving.Clutch.SLIPPING), vehicle + " pulled away on the clutch");
            assertTrue(seen.contains(Driving.Clutch.SHIFTING), vehicle + " changed gear");
            assertEquals(vehicle.count(), highest, vehicle + " up into top: " + driving);
        }
    }

    @Test
    void offTheThrottleInGearItHoldsBackAndBrakedToAStopItIdles() {
        for (final Vehicle vehicle : Vehicle.ALL) {
            Driving driving = Driving.stopped(vehicle);
            for (int i = 0; i < 12 / STEP; i++) {
                driving = driving.step(vehicle, 1, 0, 0, true, STEP);
            }
            final double fast = driving.speed();
            Driving coasting = driving;
            for (int i = 0; i < 2 / STEP; i++) {
                coasting = coasting.step(vehicle, 0, 0, 0, false, STEP);
            }
            assertTrue(coasting.speed() < fast, vehicle + " slowing off the throttle");
            assertEquals(0, coasting.firing(), vehicle + " the throttle shut");
            Driving braked = coasting;
            for (int i = 0; i < 20 / STEP; i++) {
                braked = braked.step(vehicle, 0, 1, 0, true, STEP);
            }
            assertEquals(0, braked.speed(), vehicle + " stopped");
            assertEquals(Driving.Clutch.OUT, braked.clutch());
            assertEquals(vehicle.engine().idle(), braked.rpm(), 5, vehicle + " idling");
        }
    }

    @Test
    void changingUpByHandDropsTheRevsByTheGearsRatio() {
        final Vehicle vehicle = Vehicle.MUSCLE;
        Driving driving = Driving.stopped(vehicle);
        for (int i = 0; i < 2 / STEP; i++) {
            driving = driving.step(vehicle, 0.6, 0, 0, false, STEP);
        }
        assertEquals(1, driving.gear(), "held in first, by hand");
        assertEquals(Driving.Clutch.IN, driving.clutch());
        final double before = driving.rpm();
        driving = driving.step(vehicle, 0.6, 0, 1, false, STEP);
        assertEquals(Driving.Clutch.SHIFTING, driving.clutch());
        while (driving.clutch() == Driving.Clutch.SHIFTING) {
            driving = driving.step(vehicle, 0.6, 0, 0, false, STEP);
        }
        assertEquals(2, driving.gear());
        assertEquals(before * vehicle.gears()[1] / vehicle.gears()[0], driving.rpm(), before * 0.06,
                "down by the ratio, give or take what was lost changing");
    }

    @Test
    void aScootersCvtHoldsItsRevsAsItGathersSpeed() {
        for (final Vehicle vehicle : new Vehicle[] {Vehicle.SCOOTER_50, Vehicle.SCOOTER_125}) {
            Driving driving = Driving.stopped(vehicle);
            double least = Double.MAX_VALUE;
            double most = 0;
            for (int i = 0; i < 30 / STEP; i++) {
                driving = driving.step(vehicle, 1, 0, 0, false, STEP);
                if (driving.kmh() > 15 && driving.kmh() < 35) {
                    least = Math.min(least, driving.rpm());
                    most = Math.max(most, driving.rpm());
                }
                assertEquals("D", driving.shown());
            }
            assertTrue(driving.kmh() > 40 && driving.kmh() < 110, vehicle + " flat out at " + driving.kmh());
            assertEquals(vehicle.cvt(), (least + most) / 2, vehicle.cvt() * 0.05, vehicle + " held at its revs");
            assertTrue(most - least < vehicle.cvt() * 0.05, vehicle + " steady: " + least + " to " + most);
        }
    }

    @Test
    void theyGetUpToSpeedAsBriskelyAsSuchVehiclesDo() {
        assertBetween(Vehicle.HATCHBACK, 7, 16);
        assertBetween(Vehicle.RALLY, 4, 9);
        assertBetween(Vehicle.SALOON, 5, 11);
        assertBetween(Vehicle.MUSCLE, 4, 8);
        assertBetween(Vehicle.SUPERCAR, 2.5, 6);
        assertBetween(Vehicle.SPORT_BIKE, 2.5, 6);
    }

    /** From a standstill to 100 km/h, flat out, the gearbox left to it, in between {@code low} and {@code high} seconds. */
    private static void assertBetween(final Vehicle vehicle, final double low, final double high) {
        Driving driving = Driving.stopped(vehicle);
        double seconds = 0;
        while (driving.kmh() < 100 && seconds < 60) {
            driving = driving.step(vehicle, 1, 0, 0, true, STEP);
            seconds += STEP;
        }
        assertTrue(seconds > low && seconds < high, vehicle + " to 100 km/h in " + seconds + " s");
    }
}
