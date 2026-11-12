package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A block's own frame: where a mark on it is, so the mark goes with it when it moves. */
class FrameTest {

    private static void near(final Vec3 expected, final Vec3 actual, final double within) {
        assertEquals(expected.x(), actual.x(), within, "x of " + actual);
        assertEquals(expected.y(), actual.y(), within, "y of " + actual);
        assertEquals(expected.z(), actual.z(), within, "z of " + actual);
    }

    @Test
    void intoABlocksFrameAndBackIsWhereItWas() {
        final Block block = new Block(350, -1200, 80, 40, 37, 500, 900);
        final Vec3 point = new Vec3(410, -620, -1170);
        near(point, block.toWorld(block.toLocal(point)), 1e-9);
        near(new Vec3(0, 0, 0), block.toLocal(new Vec3(350, -700, -1200)), 1e-9);
        // a direction turns as the difference of two points does
        final Vec3 d = new Vec3(0.3, -0.5, 0.8);
        near(block.toLocal(point.add(d)).subtract(block.toLocal(point)), block.turnToLocal(d), 1e-9);
    }

    @Test
    void theFrameIsTheOneANodeTurnedWithTheBlockIsIn() {
        // Rotate(yaw, Y) turns the frame's +z to the block's heading, and its +x to its right
        final Block block = new Block(0, 0, 100, 50, 30, 0, 200);
        near(Num.heading(30).multiply(10).add(0, -100, 0), block.toWorld(new Vec3(0, 0, 10)), 1e-9);
        near(Num.right(30).multiply(10).add(0, -100, 0), block.toWorld(new Vec3(10, 0, 0)), 1e-9);
    }

    @Test
    void aMarkOnAnAirshipGoesWhereTheAirshipCarriesIt() {
        final Course course = new Course();
        final Course.Piece airship = course.pieces().stream().filter(Course.Piece::moves).findFirst().orElseThrow();
        final Course.Route route = airship.route();
        // struck on the side of the hull, twelve seconds in
        final Block then = airship.at(12).block();
        final Block side = Course.HULL.get(3).placed(route.at(12));
        final Vec3 struck = new Vec3(side.x(), -(side.bottom() + side.top()) / 2, side.z())
                .add(Num.right(route.at(12).yaw()).multiply(side.halfWidth()));
        final Vec3 onIt = then.toLocal(struck);
        // half a minute on, where it is drawn is where riding along would have carried it
        final Vec3 later = airship.at(42).block().toWorld(onIt);
        near(route.at(12).carry(struck, route.at(42)), later, 1e-6);
    }
}
