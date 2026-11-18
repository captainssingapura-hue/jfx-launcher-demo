package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The demos offered: the town first, the playground, the track; the simple scenes kept for the tests
 * not offered. And a group of pieces laid out about the origin, moved into place together.
 */
class DemoTest {

    @Test
    void theTownFirstThenItsSiegeTheWallTowerThePlaygroundAndTheTrackAloneThenThoseToldOf() {
        assertEquals(List.of(Demos.TOWN, Demos.SIEGE, Demos.WALL_TOWER, Demos.PLAYGROUND, Demos.TRACK),
                Demos.playable().subList(0, 5));
        for (final Demo demo : Demos.values()) {
            assertEquals(Demos.playable().contains(demo), demo.offered(), demo.title());
            assertTrue(demo.recordable(), "a built-in found again by its name");
            assertEquals(demo, Demos.named(demo.name()));
        }
        // one told of: offered after the built-ins, found by its name
        final Demo told = new Laid("TOLD_OF", true, List.of());
        Demos.register(told);
        assertEquals(told, Demos.named("TOLD_OF"));
        assertTrue(Demos.playable().indexOf(told) >= 5);
        assertEquals(null, Demos.named("NONE_SUCH"));
    }

    /** A demo made on the fly: a few pieces, the avatar at the origin. */
    private record Laid(String name, boolean offered, List<Course.Piece> pieces) implements Demo {

        @Override
        public String title() {
            return "laid";
        }

        @Override
        public String description() {
            return "made on the fly";
        }

        @Override
        public double startYaw() {
            return 0;
        }

        @Override
        public void lay(final Course.Layout world) {
            world.start(new Vec3(0, 0, -1_000)).pieces(pieces).ruled(false);
        }
    }

    @Test
    void aDemoMadeOnTheFlyLaidIntoACourseAndNotRecorded() {
        final Course.Piece pillar = new Course.Piece(Block.standing(0, 1_000, 200, 100, 300, 0), Course.Kind.PILLAR, 0);
        final Demo made = new Laid("MADE", false, List.of(pillar));
        final Course course = new Course(made);
        assertEquals(List.of(pillar), course.pieces());
        assertEquals(new Vec3(0, 0, -1_000), course.start());
        assertFalse(made.recordable());
        assertThrows(IllegalArgumentException.class,
                () -> new Recording.Recorder(new java.io.StringWriter(), false, made));
    }

    @Test
    void piecesLaidOutAboutTheOriginAreMovedAndTurnedTogether() {
        // a pillar ahead of the origin, and a shaped one beside it; put 100 m east and 50 m on, turned to face east
        final Place at = new Place(10_000, 5_000, 0, 90);
        final Course.Piece box = new Course.Piece(Block.standing(0, 1_000, 200, 100, 300, 0), Course.Kind.PILLAR, 0);
        final Course.Piece moved = box.within(at);
        // ahead of it, turned east: east of where it is put
        assertEquals(11_000, moved.block().x(), 1e-9);
        assertEquals(5_000, moved.block().z(), 1e-9);
        assertEquals(90, moved.block().yaw(), 1e-9);
        assertTrue(moved.solid().getFirst().covers(11_000, 5_000, 10));
        assertFalse(moved.solid().getFirst().covers(0, 1_000, 10), "nothing left where it was");
        final Volume prism = new Prism(List.of(new Vec2(-500, 1_900), new Vec2(500, 1_900), new Vec2(500, 2_100),
                new Vec2(-500, 2_100)), 0, 400);
        final Course.Piece shaped = Course.Piece.shaped(Block.standing(0, 2_000, 1_000, 200, 400, 0), Course.Kind.BUILDING, 0,
                List.of(), List.of(prism), new Place(0, 2_000, 0, 0)).within(at);
        assertTrue(shaped.volumes().getFirst().covers(12_000, 5_000, 10), "its shape with it");
        assertEquals(new Place(12_000, 5_000, 0, 90).x(), shaped.place().x(), 1e-9);
        assertEquals(90, shaped.place().yaw(), 1e-9);
        // someone on the ground, with them
        final Course.Cast monk = new Course.Cast(Person.Role.SMALL_MONK, null, 0, 1_000, 180, null).within(at);
        assertEquals(11_000, monk.across(), 1e-9);
        assertEquals(5_000, monk.along(), 1e-9);
        assertEquals(-90, monk.facing(), 1e-9);
        // but not what moves of itself
        final Course.Piece car = Course.Piece.vehicle(Course.CAR_CHASSIS, null, new Place(0, 0, Course.CAR_CHASSIS.ride(), 0), 0);
        assertThrows(IllegalArgumentException.class, () -> car.within(at));
    }
}
