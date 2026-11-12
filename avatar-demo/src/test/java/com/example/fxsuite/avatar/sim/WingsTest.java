package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The swing wings: their sweep scheduled by speed, and that they stop against what they meet. */
class WingsTest {

    private static final double FRAME = 1.0 / 60;

    /** Flying forward at {@code speed} from {@code at}, facing {@code yaw}. */
    private static Body flying(final Vec3 at, final double yaw, final double speed) {
        return new Body(at, yaw, yaw, speed, 0, 0, 0, true, false, 0, 0);
    }

    @Test
    void stowedUnlessFlying() {
        assertEquals(Wings.STOWED, Wings.sweepFor(true, Body.standing(Vec3.ZERO, 0)));
        assertEquals(Wings.STOWED, Wings.sweepFor(false, flying(Vec3.ZERO, 0, 0)), "no pack, no flight");
    }

    @Test
    void spreadToHoverOnTheJetsAndSweptBackAsItSpeedsUp() {
        assertEquals(Wings.SPREAD, Wings.sweepFor(true, flying(Vec3.ZERO, 0, 0)), "hovering, held up by thrust");
        assertEquals(Wings.SPREAD, Wings.sweepFor(true, flying(Vec3.ZERO, 0, Wings.SLOW)), "still spread, slow");
        assertEquals(Wings.SPREAD, Wings.sweepFor(true, flying(Vec3.ZERO, 0, -Body.FLY)), "and backing off");
        double was = Wings.SPREAD;
        for (double speed = Wings.SLOW + 100; speed <= Body.BOOST; speed += 100) {
            final double now = Wings.sweepFor(true, flying(Vec3.ZERO, 0, speed));
            assertTrue(now > was, "sweeps further back at " + speed);
            was = now;
        }
        assertEquals(Wings.SWEPT, Wings.sweepFor(true, flying(Vec3.ZERO, 0, Body.BOOST)), 1e-9);
        assertTrue(Wings.SWEPT < Wings.STOWED, "never as far back in flight as stowed");
    }

    @Test
    void theAvatarSwingsItsWingsTowardTheScheduleAFewFramesAtATime() {
        Avatar avatar = new Avatar(Body.standing(Vec3.ZERO, 0), true).toggledFlight();
        assertEquals(Wings.STOWED, avatar.sweep(), "just taken off: still stowed");
        avatar = avatar.step(Controls.NONE, FRAME, Obstacles.NONE).avatar();
        assertTrue(avatar.sweep() < Wings.STOWED && avatar.sweep() > Wings.SPREAD, "on its way: " + avatar.sweep());
        for (int i = 0; i < 60; i++) {
            avatar = avatar.step(Controls.NONE, FRAME, Obstacles.NONE).avatar();
        }
        assertEquals(Wings.SPREAD, avatar.sweep(), 0.5, "spread to hover after a second");
        assertEquals(Wings.STOWED, avatar.toggledPack().toggledPack().sweep(), "a pack put back on is stowed");
    }

    @Test
    void theWingsReachFurthestSpreadAndLowestSwept() {
        final Wings spread = new Wings(Wings.SPREAD);
        final Wings swept = new Wings(Wings.SWEPT);
        assertTrue(spread.reach() > swept.reach());
        assertTrue(swept.reach() > Body.RADIUS, "even fully swept, wider than the body");
        final Wings.Span left = spread.spans().get(0);
        final Wings.Span right = spread.spans().get(1);
        assertEquals(spread.reach() - Wings.REACH, right.to(), 1e-9, "out to the tip");
        assertEquals(-right.from(), left.from(), 1e-9, "the left wing mirrors the right");
        assertEquals(-right.to(), left.to(), 1e-9);
        assertTrue(swept.spans().get(1).bottom() < right.bottom(), "swept back, the tips hang lower");
        assertTrue(Wings.STEP < Wings.REACH, "a step is never more than a wing is thick");
    }

    @Test
    void aWingCannotPassThroughAGateBarThoughTheBodyWould() {
        final Course course = new Course();
        final Gate gate = course.gates().get(0);
        final List<Block> bars = course.pieces().stream()
                .filter(p -> p.kind() == Course.Kind.GATE).map(Course.Piece::block).toList().subList(0, 4);
        final Obstacles world = new Obstacles(bars, List.of(gate));
        // the body's middle clears the side bar by more than its radius; a wing reaches into it
        final double across = gate.span() / 2 - Body.RADIUS - 20;
        final Vec3 ahead = Num.heading(gate.yaw());
        final Vec3 start = new Vec3(gate.x(), -(gate.height() - Body.TALL / 2), gate.z())
                .subtract(ahead.multiply(400)).add(Num.right(gate.yaw()).multiply(across));

        final Flight bodyAlone = fly(new Avatar(flying(start, gate.yaw(), Body.FLY), false), world);
        assertEquals(1, bodyAlone.passed, "the body alone fits through");
        assertTrue(bodyAlone.deepest > 1, "and so its wings, were they there, would cut through the bar: "
                + bodyAlone.deepest);
        assertTrue(!bodyAlone.bumped, "touching nothing");

        final Flight withWings = fly(new Avatar(flying(start, gate.yaw(), Body.FLY), true), world);
        assertTrue(withWings.bumped, "the wing meets the bar, and is heard to");
        assertTrue(withWings.deepest < 1, "never into it: " + withWings.deepest);
    }

    @Test
    void headOnIntoAGatePostTheWingStopsTheFlight() {
        final Course course = new Course();
        final Gate gate = course.gates().get(0);
        final List<Block> bars = course.pieces().stream()
                .filter(p -> p.kind() == Course.Kind.GATE).map(Course.Piece::block).toList().subList(0, 4);
        final Obstacles world = new Obstacles(bars, List.of(gate));
        // flying at the gate with the right wing's tip square on to the middle of a side post
        final Wings cruising = new Wings(Wings.sweepFor(true, flying(Vec3.ZERO, 0, Body.FLY)));
        final double across = (gate.span() / 2 + Course.GATE_BAR / 2) - (cruising.reach() - Wings.REACH);
        final Vec3 ahead = Num.heading(gate.yaw());
        final Vec3 start = new Vec3(gate.x(), -(gate.height() - Body.TALL / 2), gate.z())
                .subtract(ahead.multiply(400)).add(Num.right(gate.yaw()).multiply(across));
        final Flight flight = fly(new Avatar(flying(start, gate.yaw(), Body.FLY), true), world);
        assertTrue(flight.bumped);
        assertTrue(flight.deepest < 1, "the wing never cuts into the post: " + flight.deepest);
    }

    @Test
    void aWingAgainstAWallPushesTheWholeBodyClear() {
        // hovering with the wings spread, a wall just beyond the right wing's tip, and drifting into it
        final Wings spread = new Wings(Wings.SPREAD);
        final double wallAt = spread.reach() + 30;
        final Obstacles wall = new Obstacles(List.of(Block.standing(wallAt + 100, 0, 200, 2000, 1000, 0)));
        Avatar avatar = new Avatar(flying(new Vec3(0, -500, 0), 0, 0), true);
        for (int i = 0; i < 120; i++) {
            avatar = avatar.step(new Controls(0, 1, 0, 0, false), FRAME, wall).avatar();
        }
        final double gap = wallAt - avatar.body().position().x();
        assertTrue(gap >= spread.reach() - 1, "the tip stops at the wall: " + gap + " from it");
    }

    /** A second's flight: gates gone through, whether it bumped, and the deepest a wing went into a block. */
    private record Flight(long passed, boolean bumped, double deepest) {
    }

    /**
     * Flies ahead for a second. After every frame, measures how far the wings (as cruising
     * wings would be, worn or not) reach into any block.
     */
    private static Flight fly(Avatar avatar, final Obstacles world) {
        long passed = 0;
        boolean bumped = false;
        double deepest = 0;
        for (int f = 0; f < 60; f++) {
            final Avatar.Stepped stepped = avatar.step(new Controls(1, 0, 0, 0, false), FRAME, world);
            passed += stepped.events().stream().filter(e -> e instanceof Event.PassedGate).count();
            bumped |= stepped.events().stream().anyMatch(e -> e instanceof Event.Bumped);
            avatar = stepped.avatar();
            deepest = Math.max(deepest, intoBlocks(avatar, world));
        }
        return new Flight(passed, bumped, deepest);
    }

    /** How deep into any block either wing is (a little in is just touching), 0 if both are clear. */
    private static double intoBlocks(final Avatar avatar, final Obstacles world) {
        final Body body = avatar.body();
        final Wings wings = avatar.wearingPack() ? avatar.wings() : new Wings(Wings.sweepFor(true, body));
        final Vec3 side = Num.right(body.yaw());
        final Vec3 back = Num.heading(body.yaw()).multiply(-1);
        double deepest = 0;
        for (final Wings.Span span : wings.spans()) {
            final double bx = body.position().x() + back.x() * span.behind();
            final double bz = body.position().z() + back.z() * span.behind();
            for (final Block block : world.blocks()) {
                final boolean level = block.top() > body.height() + span.bottom()
                        && block.bottom() < body.height() + span.top();
                final Vec2 push = level ? block.push(bx + side.x() * span.from(), bz + side.z() * span.from(),
                        bx + side.x() * span.to(), bz + side.z() * span.to(), Wings.REACH) : null;
                if (push != null) {
                    deepest = Math.max(deepest, push.magnitude());
                }
            }
        }
        return deepest;
    }
}
