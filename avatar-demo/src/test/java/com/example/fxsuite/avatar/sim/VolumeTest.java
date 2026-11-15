package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Shapes in the way more closely than boxes: an outline stood up (any outline, a notch in it open);
 * one laid on its side (its top sloping as its profile does, and only what of it is between a
 * body's feet and its head in its way); two of those crossed (a pyramid: its top the lower of
 * theirs); each struck by rays where it truly is, and gone with its piece.
 */
class VolumeTest {

    private static final double FRAME = 1.0 / 60;

    /** An L: 4 m square with its front right quarter cut away, 3 m high. */
    private static final Prism ELL = new Prism(List.of(new Vec2(-200, -200), new Vec2(200, -200), new Vec2(200, 0),
            new Vec2(0, 0), new Vec2(0, 200), new Vec2(-200, 200)), 0, 300);

    /** A gable 10 m across, its ridge 5 m up, 20 m long along z. */
    private static final Extrusion GABLE = new Extrusion(0, 0, 0, 1000,
            List.of(new Vec2(-500, 0), new Vec2(500, 0), new Vec2(0, 500)));

    /** A pyramid 20 m square, 10 m high (45°). */
    private static final Crossing PYRAMID = new Crossing(
            new Extrusion(0, 0, 0, 1000, List.of(new Vec2(-1000, 0), new Vec2(1000, 0), new Vec2(0, 1000))),
            new Extrusion(0, 0, 90, 1000, List.of(new Vec2(-1000, 0), new Vec2(1000, 0), new Vec2(0, 1000))));

    private static Obstacles world(final Volume... volumes) {
        return new Obstacles(List.of()).with(List.of(volumes), java.util.stream.IntStream.range(0, volumes.length)
                .boxed().toList());
    }

    @Test
    void anOutlineStoodUpIsInTheWayWhereItIsAndOpenInItsNotch() {
        assertTrue(ELL.covers(-100, -100, 1));
        assertFalse(ELL.covers(100, 100, 1), "the notch is open");
        assertTrue(ELL.covers(100, 100, 150), "but a wide circle reaches its edge");
        // pushed out of the nearest side
        final Vec2 push = ELL.push(150, -10, 32, 0, 200);
        assertNotNull(push);
        assertEquals(0, push.x(), 1e-6);
        assertEquals(42, push.z(), 1e-6, "out of the notch's side, into the notch");
        assertNull(ELL.push(-100, -100, 32, 300, 500), "over it, nothing");
        // a ray along x at 1 m up meets its left side; one from above, its top
        final Hit side = ELL.ray(new Vec3(-1000, -100, -100), new Vec3(1, 0, 0), 5000);
        assertEquals(800, side.distance(), 1e-6);
        assertEquals(new Vec3(-1, 0, 0), side.normal());
        final Hit top = ELL.ray(new Vec3(-100, -1000, -100), new Vec3(0, 1, 0), 5000);
        assertEquals(700, top.distance(), 1e-6);
        assertNull(ELL.ray(new Vec3(100, -1000, 100), new Vec3(0, 1, 0), 5000), "down through the notch");
    }

    @Test
    void anOutlineLaidOnItsSideSlopesAsItsProfileDoes() {
        assertEquals(500, GABLE.topAt(0, 0, 1), 1, "its ridge");
        assertEquals(250, GABLE.topAt(250 + 1, 300, 1), 1.5, "half way down a slope");
        assertEquals(Double.NEGATIVE_INFINITY, GABLE.topAt(0, 1200, 1), "past its end");
        // between 4 and 6 m, only the ridge's stretch is in the way: a body at 3 m across is not
        assertNull(GABLE.push(300, 0, 32, 400, 600));
        assertNotNull(GABLE.push(100, 0, 32, 400, 600));
        // a ray straight down onto a slope: its normal up and out
        final Hit hit = GABLE.ray(new Vec3(250, -2000, 0), new Vec3(0, 1, 0), 5000);
        assertEquals(2000 - 250, hit.distance(), 1e-6);
        assertEquals(Math.sqrt(0.5), hit.normal().x(), 1e-9);
        assertEquals(-Math.sqrt(0.5), hit.normal().y(), 1e-9);
    }

    @Test
    void twoCrossedAreAPyramid() {
        assertEquals(1000, PYRAMID.topAt(0, 0, 0.01), 0.1, "its point");
        assertEquals(400, PYRAMID.topAt(600, 0, 0.01), 0.1);
        assertEquals(400, PYRAMID.topAt(0, -600, 0.01), 0.1);
        assertEquals(200, PYRAMID.topAt(800, 300, 0.01), 0.1, "the lower of the two slopes");
        assertFalse(PYRAMID.covers(1100, 0, 32));
        // a ray from the side at 9 m up passes over its 45° slope where a box round it would have stopped it
        assertNull(PYRAMID.ray(new Vec3(-3000, -900, 500), new Vec3(1, 0, 0), 6000), "over the shoulder");
        final Hit face = PYRAMID.ray(new Vec3(-3000, -500, 0), new Vec3(1, 0, 0), 6000);
        assertEquals(2500, face.distance(), 1e-6, "into its face at 5 m up, half way in");
        assertEquals(-Math.sqrt(0.5), face.normal().x(), 1e-9);
    }

    @Test
    void aBodyWalksUpAGentleSlopeButNotASteepOne() {
        // 25°: walked up; 60°: stopped at its foot
        final Crossing gentle = new Crossing(
                new Extrusion(0, 0, 0, 1000, List.of(new Vec2(-1000, 0), new Vec2(1000, 0), new Vec2(0, 466))),
                new Extrusion(0, 0, 90, 1000, List.of(new Vec2(-1000, 0), new Vec2(1000, 0), new Vec2(0, 466))));
        assertTrue(highest(world(gentle), 6) > 400, "up near its point");
        final Crossing steep = new Crossing(
                new Extrusion(0, 0, 0, 1000, List.of(new Vec2(-1000, 0), new Vec2(1000, 0), new Vec2(0, 1732))),
                new Extrusion(0, 0, 90, 1000, List.of(new Vec2(-1000, 0), new Vec2(1000, 0), new Vec2(0, 1732))));
        assertTrue(highest(world(steep), 6) < 40, "not up it: " + highest(world(steep), 6));
        final Body stopped = walk(world(steep), 6);
        assertTrue(stopped.position().x() < -950, "stopped at its foot: " + stopped.position().x());
    }

    /** How high a body walking east from 13 m west of the middle gets, in {@code seconds}. */
    private static double highest(final Obstacles world, final double seconds) {
        Body body = Body.standing(new Vec3(-1300, 0, 0), 90);
        double highest = 0;
        for (int i = 0; i < seconds / FRAME; i++) {
            body = body.step(new Controls(1, 0, 0, 0, true), FRAME, world);
            highest = Math.max(highest, body.height());
        }
        return highest;
    }

    /** A body walking east from 13 m west of the middle, for {@code seconds}. */
    private static Body walk(final Obstacles world, final double seconds) {
        Body body = Body.standing(new Vec3(-1300, 0, 0), 90);
        for (int i = 0; i < seconds / FRAME; i++) {
            body = body.step(new Controls(1, 0, 0, 0, true), FRAME, world);
        }
        return body;
    }

    @Test
    void aShellOverheadIsACeilingToWalkUnderAndGoesWithItsPiece() {
        // a slab 3 to 4 m up: walked under, its underside the ceiling
        final Extrusion shelf = new Extrusion(0, 0, 0, 500, List.of(new Vec2(-500, 300), new Vec2(500, 300),
                new Vec2(500, 400), new Vec2(-500, 400)));
        final Obstacles world = world(shelf, ELL);
        assertEquals(300, world.ceiling(0, 0, 32, 200), 1e-9);
        assertFalse(world.slide(-600, 0, 400, 0, 32, 20, 200).blocked(), "under it");
        assertEquals(0, world.floor(0, 0, 16, 20), 1e-9, "the ground under it");
        assertEquals(400, world.floor(0, 0, 16, 450), 1e-9, "its top, from over it");
        assertEquals(0, world.under(0, 0, 16, 400), "standing on piece 0");
        final Obstacles gone = world.without(0);
        assertEquals(Double.POSITIVE_INFINITY, gone.ceiling(0, 0, 32, 200), "gone with its piece");
        assertEquals(1, gone.volumes().size());
        assertEquals(List.of(1), gone.volumePieces());
    }

    /** A gateway 11 m across, 7 m high, 5 m through along z; an arched passage 3.6 m wide through it, its crown at 5.2 m. */
    private static final Extrusion GATEWAY = new Extrusion(0, 0, 0, 250,
            List.of(new Vec2(-550, 0), new Vec2(550, 0), new Vec2(550, 700), new Vec2(-550, 700)), List.of(arch()));

    private static List<Vec2> arch() {
        final List<Vec2> arch = new java.util.ArrayList<>(List.of(new Vec2(-180, 0), new Vec2(180, 0)));
        for (int k = 0; k <= 16; k++) {
            arch.add(new Vec2(180 * Math.cos(Math.PI * k / 16), 340 + 180 * Math.sin(Math.PI * k / 16)));
        }
        return arch;
    }

    @Test
    void aPassageThroughAnExtrusionIsWalkedThroughUnderItsArch() {
        final Obstacles world = world(GATEWAY);
        // walked straight through along its passage; stopped by it beside the passage
        Body through = Body.standing(new Vec3(0, 0, -800), 0);
        Body beside = Body.standing(new Vec3(-350, 0, -800), 0);
        for (int i = 0; i < 4 / FRAME; i++) {
            through = through.step(new Controls(1, 0, 0, 0, true), FRAME, world);
            beside = beside.step(new Controls(1, 0, 0, 0, true), FRAME, world);
        }
        assertTrue(through.position().z() > 600, "through it: " + through.position());
        assertTrue(beside.position().z() < -250, "stopped at its face: " + beside.position());
        // in the passage: the ground under it, the arch over it; on top of it, its top
        assertEquals(0, world.floor(0, 0, 16, 20), 1e-9);
        assertEquals(340 + Math.sqrt(180 * 180 - 32 * 32), world.ceiling(0, 0, 32, 200), 1,
                "under the arch, lowest at the sides of the body");
        assertEquals(700, world.floor(0, 0, 16, 720), 1e-9, "on its top, over the passage");
        // a round down the passage flies on through; one beside it, or down onto its top, meets it
        assertNull(GATEWAY.ray(new Vec3(0, -150, -2000), new Vec3(0, 0, 1), 5000), "down the passage");
        assertEquals(1750, GATEWAY.ray(new Vec3(-400, -150, -2000), new Vec3(0, 0, 1), 5000).distance(), 1e-6);
        final Hit top = GATEWAY.ray(new Vec3(0, -1000, 0), new Vec3(0, 1, 0), 5000);
        assertEquals(300, top.distance(), 1e-6, "on its top, over the passage");
        // across the passage from inside it: out of the passage into its side
        final Hit side = GATEWAY.ray(new Vec3(0, -150, 0), new Vec3(1, 0, 0), 5000);
        assertEquals(180, side.distance(), 1e-6);
        assertEquals(-1, side.normal().x(), 1e-9, "its side faces into the passage");
    }

    @Test
    void aShapeIsPlacedAndScaledAsTheBuildingItIsPartOfIs() {
        final Prism local = new Prism(List.of(new Vec2(-100, 0), new Vec2(100, 0), new Vec2(100, 400),
                new Vec2(-100, 400)), 0, 300);
        // turned to face +x, at (1000, 2000), twice as big: its far end 8 m along +x
        final Volume placed = Volume.placed(local, new Place(1000, 2000, 0, 90), 2);
        assertTrue(placed.covers(1000 + 700, 2000, 1));
        assertFalse(placed.covers(1000, 2000 + 700, 1));
        assertEquals(600, placed.high(), 1e-9);
        final Volume roof = Volume.placed(GABLE, new Place(0, 0, 0, 90), 1);
        assertEquals(500, roof.topAt(0, 0, 1), 1);
        assertEquals(250, roof.topAt(0, 251, 1), 1.5, "its slope now across z");
    }
}
