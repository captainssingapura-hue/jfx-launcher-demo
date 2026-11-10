package com.example.fxsuite.avatar;

import javafx.geometry.Point3D;
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
        Body body = Body.standing(Point3D.ZERO, Course.START_YAW);
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

    /**
     * Whether a body flying straight at the gate, square on, at {@code across} from its middle
     * and with its feet at {@code feet}, gets through to the other side.
     */
    private static boolean flies(final Obstacles gate, final Block bar, final double across, final double feet) {
        final double yaw = bar.yaw();
        final Point3D ahead = Body.heading(yaw);
        final Point3D side = Body.right(yaw);
        final Point3D start = new Point3D(bar.x(), -feet, bar.z()).subtract(ahead.multiply(400)).add(side.multiply(across));
        Body body = new Body(start, yaw, Body.FLY, 0, 0, 0, true, false, 0, 0);
        for (int i = 0; i < 60; i++) {
            body = body.step(new Controls(1, 0, 0, 0, false), FRAME, gate);
        }
        final double past = body.position().subtract(new Point3D(bar.x(), body.position().getY(), bar.z()))
                .dotProduct(ahead);
        return past > 300;
    }
}
