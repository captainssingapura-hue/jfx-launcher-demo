package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A platform among what is shaped more closely than a box: it will not move into a round tower's
 * curve, but may go into its box's empty corner; it settles on a shaped top; it may fly as high as
 * well over the tallest tower.
 */
class LiftShapeTest {

    /** A round tower 40 m across, 60 m high, its middle 30 m ahead; and a platform by the start. */
    private static Course world() {
        final List<Vec2> round = new ArrayList<>();
        for (int k = 0; k < 48; k++) {
            final double a = 2 * Math.PI * k / 48;
            round.add(new Vec2(2000 * Math.cos(a), 3000 + 2000 * Math.sin(a)));
        }
        final Course.Piece tower = Course.Piece.shaped(new Block(0, 3000, 2000, 2000, 0, 0, 6000), Course.Kind.BUILDING,
                0, List.of(), List.of(new Prism(round, 0, 6000)), new Place(0, 3000, 0, 0));
        final Course.Piece platform = Course.Piece.moving(Course.CARPET.shape(), new Place(0, 0, Course.LIFT_LOW, 0),
                Course.Kind.PLATFORM, 0, null);
        return Course.of(List.of(tower, platform));
    }

    private static Lift with(final Lift lift, final Place place) {
        return new Lift(lift.piece(), lift.craft(), place, 0, 0, 0, false, false, lift.bombs(), lift.ready(),
                lift.steer(), lift.tilt());
    }

    @Test
    void itWillNotMoveIntoACurveButMayIntoTheEmptyCornerOfItsBox() {
        final Damage damage = Damage.of(world());
        final Lift lift = Lift.of(damage).getFirst();
        final Obstacles world = damage.obstacles();
        assertFalse(lift.clear(new Place(0, 1000, 1000, 0), world, Set.of()), "into its curve");
        // in its box's corner, 19 m across and 19 m back from its middle: outside its 20 m circle
        assertTrue(lift.clear(new Place(-1750, 3000 - 1750, 1000, 0), world, Set.of()), "its box's empty corner");
        assertTrue(lift.clear(new Place(0, 1000, 6500, 0), world, Set.of()), "over it");
    }

    @Test
    void itSettlesOnAShapedTop() {
        final Damage damage = Damage.of(world());
        final Lift lift = Lift.of(damage).getFirst();
        final Lift over = with(lift, new Place(0, 3000, 8000, 0));
        final Lift.Support under = over.support(damage.obstacles(), Set.of(), List.of());
        assertEquals(6000, under.top(), 1e-6, "its roof");
        assertEquals(0, under.piece(), "the tower");
        // over its box's empty corner: the ground
        final Lift corner = with(lift, new Place(-1850, 3000 - 1850, 8000, 0));
        assertTrue(corner.support(damage.obstacles(), Set.of(), List.of()).top() < 6000, "not its roof, out there");
    }

    @Test
    void itFliesWellOverTheTallestTower() {
        assertTrue(Course.LIFT_HIGH >= 30_000, "300 m and more");
        assertTrue(Course.LIFT_HIGH + Body.TALL < Body.CEILING, "under the ceiling, its pilot and all");
        assertEquals(Course.LIFT_HIGH, Course.PLATFORM_HANDLING.high());
    }
}
