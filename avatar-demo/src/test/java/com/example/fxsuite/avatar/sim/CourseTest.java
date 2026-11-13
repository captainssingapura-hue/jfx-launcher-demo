package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourseTest {

    private static final double FRAME = 1.0 / 60;
    private final Course course = new Course();

    @Test
    void theStartIsClear() {
        final Obstacles world = course.obstacles();
        assertFalse(world.slide(0, 0, 0, 0, Body.RADIUS, 0, Body.TALL).blocked());
        assertEquals(0, world.floor(0, 0, Body.FOOTING, 1000));
    }

    @Test
    void theSameEveryRun() {
        assertEquals(course.pieces(), new Course().pieces());
    }

    @Test
    void eachStairIsAJumpUpFromTheLast() {
        final double reach = Body.JUMP * Body.JUMP / (2 * Body.GRAVITY) + Body.STEP;
        final List<Course.Piece> stairs = course.pieces().subList(0, Course.STEPS);
        double below = 0;
        for (final Course.Piece stair : stairs) {
            assertTrue(stair.block().top() - below < reach, "reachable from " + below);
            below = stair.block().top();
        }
    }

    @Test
    void theStaircaseCanBeClimbedFromTheStart() {
        // hold ahead and jump again whenever back on its feet, until standing on the top step
        final Obstacles world = course.obstacles();
        final double top = Course.STEPS * Course.STEP_RISE;
        Body body = Body.standing(Vec3.ZERO, Course.START_YAW);
        double took = 0;
        while (!(body.grounded() && body.height() == top) && took < 10) {
            if (body.grounded()) {
                body = body.jumped();
            }
            body = body.step(new Controls(1, 0, 0, 0, false), FRAME, world);
            took += FRAME;
        }
        assertEquals(top, body.height(), 1e-6, "after " + took + " s");
        assertTrue(took < 6, "a brisk climb, not a struggle: " + took + " s");
    }

    @Test
    void someCratesAreWithinAJumpAndSomeAreNot() {
        final double reach = Body.JUMP * Body.JUMP / (2 * Body.GRAVITY) + Body.STEP;
        final List<Block> crates = course.pieces().stream()
                .filter(p -> p.kind() == Course.Kind.CRATE).map(Course.Piece::block).toList();
        assertTrue(crates.stream().anyMatch(b -> b.top() < reach));
        assertTrue(crates.stream().anyMatch(b -> b.top() > reach));
        assertTrue(course.pieces().stream().filter(p -> p.kind() == Course.Kind.PILLAR)
                .allMatch(p -> p.block().top() > reach), "pillars are only for flying onto");
    }

    @Test
    void everyGateCanBeFlownThroughButNotItsFrame() {
        final List<Block> bars = course.pieces().stream()
                .filter(p -> p.kind() == Course.Kind.GATE).map(Course.Piece::block).toList();
        assertEquals(14 * 4, bars.size());
        for (int i = 0; i < bars.size(); i += 4) {
            final Obstacles gate = new Obstacles(bars.subList(i, i + 4));
            final Block top = bars.get(i);
            final double centre = top.bottom() - Course.GATE_SPAN / 2 + Course.GATE_BAR / 2;
            assertTrue(flies(gate, top, 0, centre - Body.TALL / 2), "through the middle of gate " + i / 4);
            assertFalse(flies(gate, top, Course.GATE_SPAN / 2, centre - Body.TALL / 2), "into a side bar");
            assertFalse(flies(gate, top, 0, centre + Course.GATE_SPAN / 2 - Body.TALL / 2), "into the top bar");
        }
    }

    @Test
    void goingThroughEveryGateIsHeardOnceAndFlyingIntoItsFrameIsNot() {
        final List<Block> bars = course.pieces().stream()
                .filter(p -> p.kind() == Course.Kind.GATE).map(Course.Piece::block).toList();
        final List<Gate> gates = course.gates();
        assertEquals(bars.size() / 4, gates.size());
        for (int i = 0; i < gates.size(); i++) {
            final Gate gate = gates.get(i);
            final Obstacles one = new Obstacles(bars.subList(4 * i, 4 * i + 4), List.of(gate));
            assertEquals(1, passes(one, gate, 0), "through the middle of gate " + i);
            assertEquals(0, passes(one, gate, Course.GATE_SPAN / 2), "into a side bar of gate " + i);
            assertEquals(0, passes(one, gate, Course.GATE_SPAN), "past gate " + i);
        }
        assertEquals(course.gates(), course.obstacles().gates());
    }

    /** How many times an avatar flying square at a gate, {@code across} from its middle, goes through it. */
    private static long passes(final Obstacles world, final Gate gate, final double across) {
        final Vec3 ahead = Num.heading(gate.yaw());
        final Vec3 start = new Vec3(gate.x(), -(gate.height() - Body.TALL / 2), gate.z())
                .subtract(ahead.multiply(400)).add(Num.right(gate.yaw()).multiply(across));
        Avatar avatar = new Avatar(new Body(start, gate.yaw(), gate.yaw(), Body.FLY, 0, 0, 0, true, false, 0, 0), true);
        long passed = 0;
        for (int f = 0; f < 60; f++) {
            final Avatar.Stepped stepped = avatar.step(new Controls(1, 0, 0, 0, false), FRAME, world);
            passed += stepped.events().stream().filter(e -> e instanceof Event.PassedGate).count();
            avatar = stepped.avatar();
        }
        return passed;
    }

    /**
     * Whether a body flying straight at the gate, square on, at {@code across} from its middle
     * and with its feet at {@code feet}, gets through to the other side.
     */
    private static boolean flies(final Obstacles gate, final Block bar, final double across, final double feet) {
        final double yaw = bar.yaw();
        final Vec3 ahead = Num.heading(yaw);
        final Vec3 side = Num.right(yaw);
        final Vec3 start = new Vec3(bar.x(), -feet, bar.z()).subtract(ahead.multiply(400)).add(side.multiply(across));
        Body body = new Body(start, yaw, yaw, Body.FLY, 0, 0, 0, true, false, 0, 0);
        for (int i = 0; i < 60; i++) {
            body = body.step(new Controls(1, 0, 0, 0, false), FRAME, gate);
        }
        final double past = body.position().subtract(new Vec3(bar.x(), body.position().y(), bar.z()))
                .dotProduct(ahead);
        return past > 300;
    }

    @Test
    void theTargetsStandInTheOpenEachClearOfEverythingElse() {
        final List<Course.Piece> pieces = course.pieces();
        // the targets standing about the course; the fruit, on the stall, has a test of its own
        final List<Course.Piece> targets = pieces.stream().filter(p -> p.kind().target() && !p.kind().loose()).toList();
        assertEquals(8, targets.stream().filter(p -> p.kind() == Course.Kind.DUMMY).count());
        assertEquals(Course.DRONES, targets.stream().filter(p -> p.kind() == Course.Kind.DRONE).count());
        for (final Course.Piece target : targets) {
            final Block t = target.block();
            // a circle round the whole footprint, with room to spare, touches nothing at its heights
            final double round = Math.hypot(t.halfWidth(), t.halfDepth()) + 40;
            for (final Course.Piece other : pieces) {
                final Block o = other.block();
                if (other == target || o.top() <= t.bottom() || o.bottom() >= t.top()) {
                    continue;
                }
                assertFalse(o.covers(t.x(), t.z(), round), target + " overlaps " + other);
            }
        }
    }

    @Test
    void dummiesFaceTheStartAndDronesHoverOutOfAJumpsReach() {
        final double reach = Body.JUMP * Body.JUMP / (2 * Body.GRAVITY) + Body.STEP;
        for (final Course.Piece piece : course.pieces()) {
            final Block b = piece.block();
            if (piece.kind() == Course.Kind.DUMMY) {
                assertEquals(0, b.bottom(), "standing on the ground");
                // its broad face square on to the start, give or take the row's spread
                final Vec3 facing = Num.heading(b.yaw());
                final Vec3 toward = new Vec3(-b.x(), 0, -b.z()).normalize();
                assertTrue(-facing.dotProduct(toward) > 0.9, piece + " faces the start");
            } else if (piece.kind() == Course.Kind.DRONE) {
                assertTrue(b.bottom() > reach, piece + " hovers out of a jump's reach");
            }
        }
    }

    @Test
    void onlyCratesTargetsLooseThingsAndPlatformsCanBreak() {
        for (final Course.Kind kind : Course.Kind.values()) {
            assertEquals(kind == Course.Kind.CRATE || kind == Course.Kind.PLATFORM || kind.target() || kind.loose(), kind.breakable(), kind.name());
        }
    }
}
