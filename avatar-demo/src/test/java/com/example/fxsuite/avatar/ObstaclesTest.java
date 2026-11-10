package com.example.fxsuite.avatar;

import javafx.geometry.Point2D;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObstaclesTest {

    /** A 200 × 200 block, 100 tall, at the origin. */
    private static final Block CRATE = Block.standing(0, 0, 200, 200, 100, 0);

    @Test
    void pushesACircleStraightOutOfTheNearestFace() {
        final Point2D push = CRATE.push(-120, 0, 32);
        assertEquals(-12, push.getX(), 1e-9);
        assertEquals(0, push.getY(), 1e-9);
        assertNull(CRATE.push(-140, 0, 32), "clear of it");
    }

    @Test
    void pushesOutOfACornerDiagonally() {
        final Point2D push = CRATE.push(110, 110, 32);
        assertTrue(push.getX() > 0 && push.getY() > 0);
        assertEquals(push.getX(), push.getY(), 1e-9);
    }

    @Test
    void pushesACentreThatIsInsideOutThroughTheNearestSide() {
        final Point2D push = CRATE.push(90, 0, 32);
        assertEquals(10 + 32, push.getX(), 1e-9);
        assertEquals(0, push.getY(), 1e-9);
    }

    @Test
    void aTurnedBlockIsMetInItsOwnFrame() {
        // 200 long and 20 thick, turned 90°: Rotate(90, Y) takes its length from x onto z
        final Block wall = new Block(0, 0, 100, 10, 90, 0, 300);
        final Point2D push = wall.push(20, 80, 32);
        assertEquals(22, push.getX(), 1e-9, "out across its thickness, now along x");
        assertEquals(0, push.getY(), 1e-9);
        assertNull(wall.push(20, 140, 32), "past its end, now along z");
    }

    @Test
    void theFloorIsTheHighestTopWithinReach() {
        final Obstacles world = new Obstacles(List.of(CRATE, Block.standing(0, 0, 50, 50, 400, 0)));
        assertEquals(100, world.floor(80, 0, 16, 120), "the crate; the pillar is out of reach");
        assertEquals(400, world.floor(0, 0, 16, 500));
        assertEquals(0, world.floor(500, 0, 16, 500), "the ground, away from both");
    }

    @Test
    void theCeilingIsTheLowestUndersideAbove() {
        final Obstacles world = new Obstacles(List.of(new Block(0, 0, 100, 100, 0, 400, 450)));
        assertEquals(400, world.ceiling(0, 0, 32, 200));
        assertEquals(Double.POSITIVE_INFINITY, world.ceiling(0, 0, 32, 450), "already above it");
    }

    @Test
    void slidingStopsTheVelocityIntoAWallAndKeepsTheRest() {
        final Obstacles world = new Obstacles(List.of(CRATE));
        final Obstacles.Contact head_on = world.slide(-120, 0, 300, 0, 32, 0, 200);
        assertTrue(head_on.blocked());
        assertEquals(-132, head_on.x(), 1e-9);
        assertEquals(0, head_on.vx(), 1e-9);
        final Obstacles.Contact glancing = world.slide(-120, 0, 300, 200, 32, 0, 200);
        assertEquals(0, glancing.vx(), 1e-9);
        assertEquals(200, glancing.vz(), 1e-9, "along the wall, kept");
        final Obstacles.Contact leaving = world.slide(-120, 0, -300, 0, 32, 0, 200);
        assertEquals(-300, leaving.vx(), 1e-9, "moving away is never held back");
    }

    @Test
    void slidingIgnoresWhatIsBelowTheFeetOrAboveTheHead() {
        final Obstacles world = new Obstacles(List.of(CRATE));
        assertFalse(world.slide(-120, 0, 300, 0, 32, 100, 300).blocked(), "standing level with its top");
        assertFalse(world.slide(-120, 0, 300, 0, 32, -300, -100).blocked(), "wholly below it");
    }
}
