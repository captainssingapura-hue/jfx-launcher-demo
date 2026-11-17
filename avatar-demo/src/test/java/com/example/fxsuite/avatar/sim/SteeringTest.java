package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How far and how fast a land vehicle's wheels turn, by its speed: full lock at a crawl; a full turn pulling no
 * more than a tyre holds, gentler the faster and faster so; turned in over longer the faster, let back twice as
 * fast, never further than the lock now is; and a car flat out on the motorway turned gently by a key held.
 */
class SteeringTest {

    private static final double KMH = 100 / 3.6;
    private static final Course.Chassis CAR = Course.CAR_CHASSIS;

    /** How hard a full turn going {@code speed} pulls across, cm/s². */
    private static double pulled(final Course.Chassis chassis, final double speed) {
        return speed * speed * Math.tan(Math.toRadians(Steering.lock(chassis, speed))) / chassis.wheelbase();
    }

    @Test
    void fullLockAtACrawl() {
        assertEquals(CAR.drive().steer(), Steering.lock(CAR, 0));
        assertEquals(CAR.drive().steer(), Steering.lock(CAR, 10 * KMH));
        assertEquals(CAR.drive().steer(), Steering.lock(CAR, -10 * KMH), "backing too");
    }

    @Test
    void aFullTurnPullsNoMoreThanATyreHoldsGentlerTheFasterAndFasterSo() {
        for (final Course.Chassis chassis : new Course.Chassis[] {CAR, Course.SPORT_BIKE, Course.TRUCK_CHASSIS}) {
            for (double kmh = 1; kmh <= 260; kmh++) {
                assertTrue(pulled(chassis, kmh * KMH) <= Steering.GRIP + 1e-6, chassis.name() + " at " + kmh + " km/h");
            }
        }
        // at its top, 60% of it; and each 20 km/h faster calms it more than the last
        assertEquals(0.6 * Steering.GRIP, pulled(CAR, CAR.drive().top()), 1);
        double calmed = 0;
        for (double kmh = 60; kmh <= 240; kmh += 20) {
            final double step = Steering.pull(CAR, (kmh - 20) * KMH) - Steering.pull(CAR, kmh * KMH);
            assertTrue(step > calmed, kmh + " km/h");
            calmed = step;
        }
        // as in the paper: 1.35° of lock at 108 km/h, a quarter of a degree at 216 km/h
        assertEquals(1.35, Steering.lock(CAR, 108 * KMH), 0.01);
        assertEquals(0.26, Steering.lock(CAR, 216 * KMH), 0.01);
    }

    @Test
    void turnedInOverLongerTheFasterLetBackTwiceAsFastNeverPastTheLock() {
        assertEquals(Steering.IN, Steering.turnIn(CAR, 0));
        assertEquals(Steering.IN + Steering.SLOWER, Steering.turnIn(CAR, CAR.drive().top()), 1e-12);
        final double v = 108 * KMH;
        final double lock = Steering.lock(CAR, v);
        // a key held: at lock over its time to turn in, not at once
        double steer = 0;
        final double frame = 1 / 60.0;
        final int frames = (int) Math.round(Steering.turnIn(CAR, v) / frame);
        for (int f = 0; f < frames / 2; f++) {
            steer = Steering.turned(CAR, steer, 1, v, frame);
        }
        assertEquals(lock / 2, steer, lock * 0.05, "half way there, half way through");
        for (int f = 0; f < frames; f++) {
            steer = Steering.turned(CAR, steer, 1, v, frame);
        }
        assertEquals(lock, steer, 1e-12);
        // let go: back twice as fast
        for (int f = 0; f < frames / 2; f++) {
            steer = Steering.turned(CAR, steer, 0, v, frame);
        }
        assertEquals(0, steer, lock * 0.05);
        // turned in hard at a crawl, then going fast: brought in with the lock
        assertEquals(Steering.lock(CAR, v), Steering.turned(CAR, 30, 1, v, frame), 1e-12);
    }

    @Test
    void aCarFlatOutOnTheMotorwayTurnedGentlyByAKeyHeld() {
        final Course.Chassis chassis = CAR;
        final double v = 216 * KMH;
        double steer = 0;
        double heading = 0;
        final double frame = 1 / 60.0;
        for (int f = 0; f < 60; f++) {
            steer = Steering.turned(chassis, steer, 1, v, frame);
            heading += Math.toDegrees(v * frame / chassis.wheelbase() * Math.tan(Math.toRadians(steer)));
        }
        // a key held a whole second: a few degrees, where it was a half turn
        assertTrue(heading > 2 && heading < 8, "turned " + heading + "°");
        // a tap, a tenth of a second: a fraction of one
        steer = 0;
        heading = 0;
        for (int f = 0; f < 60; f++) {
            steer = Steering.turned(chassis, steer, f < 6 ? 1 : 0, v, frame);
            heading += Math.toDegrees(v * frame / chassis.wheelbase() * Math.tan(Math.toRadians(steer)));
        }
        assertTrue(heading > 0.02 && heading < 1, "tapped " + heading + "°");
    }
}
