package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Body;
import com.example.fxsuite.avatar.sim.Controls;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Hit;
import com.example.fxsuite.avatar.sim.Obstacles;
import com.example.fxsuite.avatar.sim.Vec3;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The pyramids, in the world: each one piece, in the way as it is shaped. The great one stops a
 * body at its foot and a round where its face is, not its box; the low one is walked up to its
 * point; the stepped one's stairway is walked up to its top tier.
 */
class PyramidsTest {

    private static final double FRAME = 1.0 / 60;

    /** The pyramids alone, laid out about their own origin: as they are put at the edge of the town, but nearer. */
    private static Obstacles world() {
        return Course.of(Pyramids.pieces()).obstacles();
    }

    /** A body walking (running) from {@code from} heading {@code yaw}, for {@code seconds}. */
    private static Body walk(final Obstacles world, final Vec3 from, final double yaw, final double seconds) {
        Body body = Body.standing(from, yaw);
        for (int i = 0; i < seconds / FRAME; i++) {
            body = body.step(new Controls(1, 0, 0, 0, true), FRAME, world);
        }
        return body;
    }

    /** How high a body walking from {@code from} heading {@code yaw} gets, in {@code seconds}. */
    private static double highest(final Obstacles world, final Vec3 from, final double yaw, final double seconds) {
        Body body = Body.standing(from, yaw);
        double highest = 0;
        for (int i = 0; i < seconds / FRAME; i++) {
            body = body.step(new Controls(1, 0, 0, 0, true), FRAME, world);
            highest = Math.max(highest, body.height());
        }
        return highest;
    }

    @Test
    void threePyramidsEachOnePieceShapedClosely() {
        final List<Course.Piece> pieces = Pyramids.pieces();
        assertEquals(3, pieces.size());
        for (int k = 0; k < 3; k++) {
            assertEquals(Course.Kind.BUILDING, pieces.get(k).kind());
            assertFalse(pieces.get(k).volumes().isEmpty(), "shaped closely");
            assertFalse(Sites.drawn(pieces.get(k).tint()).isEmpty());
            assertFalse(pieces.get(k).block().covers(0, 0, 200), "clear of the start");
        }
        assertEquals(1, pieces.get(2).solid().size(), "the stepped one's stairway, a ramp");
    }

    @Test
    void theGreatPyramidStopsABodyAtItsFootAndARoundAtItsFace() {
        final Obstacles world = world();
        // walked straight at it from the start: stopped at its foot, its face too steep
        final Body body = walk(world, Vec3.ZERO, 0, 30);
        final double foot = Pyramids.GREAT.z() - Pyramids.GREAT_HALF;
        assertTrue(body.position().z() < foot && body.position().z() > foot - 200, "at its foot: " + body.position());
        assertTrue(highest(world, Vec3.ZERO, 0, 30) < 60, "not up its face: " + highest(world, Vec3.ZERO, 0, 30));
        // a round aimed level at 30 m up flies past where its box would have stopped it, and on
        final double up = 3000;
        final Hit high = world.ray(new Vec3(0, -up, 0), new Vec3(0, 0, 1), 20_000);
        final double face = foot + Pyramids.GREAT_HALF * up / Pyramids.GREAT_HIGH;
        assertNotNull(high);
        assertEquals(face, high.at().z(), 1e-6, "on its face, not its box's");
        assertEquals(0, high.piece());
    }

    @Test
    void theLowPyramidIsWalkedUpToItsPoint() {
        final Obstacles world = world();
        // from 20 m off its middle, straight at it
        final Vec3 at = new Vec3(Pyramids.LOW.x(), 0, Pyramids.LOW.z() - 2000);
        assertTrue(highest(world, at, 0, 12) > Pyramids.LOW_HIGH - 60, "up near its point: " + highest(world, at, 0, 12));
    }

    @Test
    void theSteppedPyramidsStairwayIsWalkedUpToItsTop() {
        final Obstacles world = world();
        // from before the stairway's foot, toward the pyramid (its front faces the start: heading 0 to it)
        final double foot = Pyramids.TIERS[Pyramids.TIERS.length - 1]
                + Pyramids.TOP / Math.tan(Math.toRadians(Pyramids.STAIR_SLOPE));
        final Vec3 start = new Vec3(Pyramids.STEPPED.x(), 0, Pyramids.STEPPED.z() - foot - 500);
        final Body body = walk(world, start, 0, 20);
        assertEquals(Pyramids.TOP, body.height(), 1, "on its top tier, by its shrine");
        // and not by its sides: walked at its side, stopped at its first tier
        final Body side = walk(world, new Vec3(Pyramids.STEPPED.x() + 3500, 0, Pyramids.STEPPED.z()), -90, 10);
        assertTrue(side.height() < Pyramids.TIER, "stopped by its first tier: " + side.height());
    }
}
