package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The land: its lie with its features laid over it in turn; what a body stands on and what a ray meets
 * on it; a body walking down into a channel cut in it and up out of it, and turned aside by a slope too
 * steep to climb.
 */
class LandTest {

    private static final double FRAME = 1.0 / 60;

    /** A rise of so much over so far, beyond x = 0: a ramp of the land, eastward. */
    private record Rise(double from, double steep) implements Land.Layer {

        @Override
        public double shape(final double x, final double z, final double under) {
            return under + Math.max(0, x - from) * steep;
        }

        @Override
        public boolean touches(final double minX, final double minZ, final double maxX, final double maxZ) {
            return maxX > from;
        }

        @Override
        public double deepest() {
            return 0;
        }

        @Override
        public double tallest() {
            return Double.POSITIVE_INFINITY;
        }
    }

    private static Body walk(final Obstacles world, final Vec3 from, final double yaw, final double seconds,
                             final double[] lowest) {
        Body body = Body.standing(from, yaw);
        for (int i = 0; i < seconds / FRAME; i++) {
            body = body.step(new Controls(1, 0, 0, 0, false), FRAME, world);
            lowest[0] = Math.min(lowest[0], body.height());
        }
        return body;
    }

    @Test
    void itsLieWithItsFeaturesLaidOverIt() {
        assertTrue(Land.LEVEL.level());
        assertEquals(0, Land.LEVEL.height(123, -456));
        final Land land = new Land(null, List.of(ChannelTest.CHANNEL, new Rise(200_000, 0.5)));
        assertFalse(land.level());
        assertEquals(-150, land.height(-50_000, 0), 1e-9);
        assertEquals(500, land.height(201_000, 30_000), 1e-9);
        assertEquals(-150, land.lowest());
        assertTrue(land.featured(-60_000, -10, -59_000, 10));
        assertFalse(land.featured(-60_000, 20_000, -59_000, 30_000));
        assertEquals(0.5, land.rise(250_000, 0, 50).x(), 1e-9);
    }

    @Test
    void whatABodyStandsOnAndWhatARayMeets() {
        final Obstacles world = Obstacles.NONE.on(new Land(null, List.of(new Rise(0, 0.5))));
        assertEquals(500, world.floor(1_000, 0, 10, 0), 1e-9, "the land, however high");
        final Obstacles.Footing level = world.footing(-1_000, 0, 10, 100);
        assertEquals(0, level.height(), 1e-9);
        assertEquals(Surface.GROUND, level.surface());
        // a ray level with the ground's foot, east: it meets the rise 3 m up it, at x = 6 m
        final Hit hit = world.ray(new Vec3(-1_000, -300, 0), new Vec3(1, 0, 0), 10_000);
        assertNotNull(hit);
        assertEquals(600, hit.at().x(), 1);
        assertEquals(-300, hit.at().y(), 1);
        assertTrue(hit.normal().x() < 0 && hit.normal().y() < 0, "facing back up the ray: " + hit.normal());
        assertNull(world.ray(new Vec3(-1_000, -300, 0), new Vec3(-1, 0, 0), 10_000), "away from it, nothing");
        assertNull(world.ray(new Vec3(-1_000, -300, 0), new Vec3(1, 0, 0), 1_000), "not that far");
    }

    @Test
    void aBodyWalksDownIntoAChannelAndUpOutOfIt() {
        final Obstacles world = Obstacles.NONE.on(new Land(null, List.of(ChannelTest.CHANNEL)));
        final double[] lowest = {Double.POSITIVE_INFINITY};
        // from 20 m south of it, north across it
        final Body across = walk(world, new Vec3(-50_000, 0, -2_000), 0, 16, lowest);
        assertTrue(across.position().z() > 2_000, "across it and out: " + across.position());
        assertEquals(-150, lowest[0], 1, "down on its bed");
        assertEquals(0, across.height(), 1e-6, "on the ground beyond it");
    }

    @Test
    void aSlopeTooSteepTurnsItAside() {
        // 60° up, eastward from x = 0
        final Obstacles world = Obstacles.NONE.on(new Land(null, List.of(new Rise(0, Math.tan(Math.toRadians(60))))));
        final double[] lowest = {Double.POSITIVE_INFINITY};
        final Body stopped = walk(world, new Vec3(-1_000, 0, 0), 90, 4, lowest);
        assertTrue(stopped.position().x() < 50, "kept off it: " + stopped.position());
        assertTrue(stopped.height() < Body.STEP * 2, "not up it: " + stopped.height());
        // but a gentle one is walked up: 20°
        final Obstacles gentle = Obstacles.NONE.on(new Land(null, List.of(new Rise(0, Math.tan(Math.toRadians(20))))));
        final Body up = walk(gentle, new Vec3(-1_000, 0, 0), 90, 10, lowest);
        assertTrue(up.position().x() > 1_000, "up it: " + up.position());
        assertEquals(gentle.land().height(up.position().x(), up.position().z()), up.height(), 1e-6, "on it");
    }
}
