package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.sim.Vec3;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How finely the land is drawn: at rest, by distance, its levels today's steps; coarser the faster the camera
 * goes, and faster so; always finest right round it; never finer further off; a tile going finer again only
 * past a level's edge by a margin; and the camera's speed smoothed, taken in quickly, let go slowly, a jump none.
 */
class FinenessTest {

    private static final double KMH = 100_000 / 3_600.0;

    @Test
    void asInThePapersTable() {
        final double[] distances = {5_000, 10_000, 20_000, 50_000, 100_000, 300_000};
        final double[] speeds = {0, 108 * KMH, 162 * KMH, 216 * KMH};
        final double[][] fine = {
                {1, 1, 1, 1},
                {1, 0.90, 0.73, 0.49},
                {0.97, 0.72, 0.33, -0.24},
                {0.47, 0.22, -0.17, -0.74},
                {0.08, -0.12, -0.43, -1},
                {-0.53, -0.59, -0.70, -1},
        };
        final int[][] levels = {
                {0, 0, 0, 0},
                {0, 0, 1, 1},
                {0, 1, 1, 2},
                {1, 2, 2, 3},
                {2, 2, 3, 4},
                {3, 3, 3, 4},
        };
        for (int d = 0; d < distances.length; d++) {
            for (int v = 0; v < speeds.length; v++) {
                final double f = Fineness.of(distances[d], speeds[v]);
                assertEquals(fine[d][v], f, 0.01, distances[d] / 100 + " m at " + speeds[v] / KMH + " km/h");
                assertEquals(levels[d][v], Fineness.level(f, -1), distances[d] / 100 + " m at " + speeds[v] / KMH + " km/h");
            }
        }
    }

    @Test
    void atRestItsLevelsTodaysSteps() {
        assertEquals(0, Fineness.level(Fineness.of(0, 0), -1));
        assertEquals(0, Fineness.level(Fineness.of(29_900, 0), -1), "the finest within 300 m");
        assertEquals(1, Fineness.level(Fineness.of(30_100, 0), -1));
        assertEquals(1, Fineness.level(Fineness.of(73_000, 0), -1), "10 cm out to 740 m");
        assertEquals(2, Fineness.level(Fineness.of(75_000, 0), -1));
        assertEquals(3, Fineness.level(Fineness.of(200_000, 0), -1));
        assertEquals(4, Fineness.level(Fineness.of(451_000, 0), -1), "the roughest past 4.5 km");
        // on foot, nothing given up
        assertEquals(Fineness.of(50_000, 0), Fineness.of(50_000, Fineness.FREE));
    }

    @Test
    void coarserTheFasterAndFasterSoButAlwaysFinestRoundYou() {
        // at 300 m, each 10 km/h more costs more than the last
        double lost = 0;
        for (double kmh = 30; kmh <= 250; kmh += 10) {
            final double step = Fineness.of(30_000, (kmh - 10) * KMH) - Fineness.of(30_000, kmh * KMH);
            assertTrue(step >= lost - 1e-12, kmh + " km/h");
            lost = step;
        }
        assertTrue(lost > 0);
        // within 60 m, finest at any speed: even an aircraft's
        assertEquals(1, Fineness.of(6_000, 900 * KMH));
        assertEquals(0, Fineness.level(Fineness.of(6_000, 900 * KMH), -1));
    }

    @Test
    void neverFinerFurtherOffAndSpeedCountingLessFar() {
        for (double kmh = 0; kmh <= 400; kmh += 20) {
            double was = 1;
            for (double d = 0; d <= 1_000_000; d += 1_000) {
                final double f = Fineness.of(d, kmh * KMH);
                assertTrue(f <= was + 1e-12, d / 100 + " m at " + kmh + " km/h");
                was = f;
            }
        }
        // 108 km/h costs 300 m a quarter, 3 km much less
        final double near = Fineness.of(30_000, 0) - Fineness.of(30_000, 108 * KMH);
        final double far = Fineness.of(300_000, 0) - Fineness.of(300_000, 108 * KMH);
        assertEquals(0.25, near, 1e-9);
        assertTrue(far < near / 3, "far: " + far);
    }

    @Test
    void finerAgainOnlyPastALevelsEdgeByAMargin() {
        assertEquals(1, Fineness.level(0.8, 1), "just past the edge: stays as it was");
        assertEquals(0, Fineness.level(0.8, -1));
        assertEquals(0, Fineness.level(0.86, 1));
        assertEquals(1, Fineness.level(0.7, 0), "coarser as soon as it is past");
        assertEquals(4, Fineness.level(-1, 2));
    }

    @Test
    void theCamerasSpeedSmoothedQuickUpSlowDownAJumpNone() {
        final Fineness.Pace pace = new Fineness.Pace();
        final double frame = 1 / 60.0;
        final double fast = 3_000;
        Vec3 at = Vec3.ZERO;
        assertEquals(0, pace.at(at, frame), "nothing to go on yet");
        for (int f = 0; f < 30; f++) {
            at = new Vec3(at.x() + fast * frame, 0, 0);
            pace.at(at, frame);
        }
        assertEquals(fast * (1 - Math.exp(-Fineness.Pace.RISE * 0.5)), pace.speed(), 1, "most of it in half a second");
        for (int f = 0; f < 300; f++) {
            at = new Vec3(at.x() + fast * frame, 0, 0);
            pace.at(at, frame);
        }
        assertEquals(fast, pace.speed(), 1);
        // stopped: let go slowly
        for (int f = 0; f < 30; f++) {
            pace.at(at, frame);
        }
        assertEquals(fast * Math.exp(-Fineness.Pace.FALL * 0.5), pace.speed(), 1, "most of it kept after half a second");
        // a jump is no speed
        final double before = pace.speed();
        pace.at(new Vec3(at.x() + 100_000, 0, 0), frame);
        assertTrue(pace.speed() <= before);
    }
}
