package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.air.Bomber;
import com.example.fxsuite.avatar.air.Craft;
import com.example.fxsuite.avatar.air.Doors;
import com.example.fxsuite.avatar.air.Hinge;
import com.example.fxsuite.avatar.air.Paint;
import com.example.fxsuite.avatar.sim.Form;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A door ({@link Door}): a leaf, or a pair, each framed as a ledged and braced door is, turning on its
 * hinge at its jamb, out of its face; made into an aircraft's door on a hinge of its own
 * ({@link Doors}), and a temple's gate ({@link TempleYard#gateDoor}).
 */
class DoorTest {

    private static final Door SINGLE = Door.single("door", 90, 200, 5, 100);
    private static final Door GATE = Door.pair("gate", 360, 340, 12, 95);

    @Test
    void shutItIsWhereItIsDrawnOpenedItTurnsOutOnItsHinge() {
        final Point free = new Point(90, 0, 100);
        assertEquals(free, SINGLE.opened(0, free, 0));
        // a quarter round: its free edge out from its face, as far as the leaf is wide
        final Point out = SINGLE.opened(0, free, 90);
        assertEquals(0, out.across(), 1e-9);
        assertEquals(90, out.along(), 1e-9);
        assertEquals(100, out.up(), 1e-9, "turned about upright, as high as it was");
        // no further than it opens
        assertEquals(SINGLE.opened(0, free, 100), SINGLE.opened(0, free, 170));
    }

    @Test
    void aPairsLeavesHangAtEachJambMeetInTheMiddleAndBothSwingOut() {
        assertEquals(180, GATE.leaf(), 1e-9);
        assertEquals(0, GATE.hinge(0), 1e-9);
        assertEquals(360, GATE.hinge(1), 1e-9);
        for (int k = 0; k < 2; k++) {
            final List<Point> nodes = GATE.nodes(k);
            // each from its jamb toward the middle, its free stile a frame's half-width short of it
            assertEquals(180 - GATE.frame() / 2, Math.abs(nodes.get(1).across() - GATE.hinge(k)), 1e-9);
            for (final Point node : nodes) {
                assertTrue(GATE.opened(k, node, 60).along() >= -1e-9, "out of its face: " + node);
            }
        }
    }

    @Test
    void eachLeafIsFramedItsSkeletonWholeItsMembersBoxesTheRightWayRound() {
        for (final Door door : List.of(SINGLE, GATE)) {
            for (int k = 0; k < door.leaves(); k++) {
                assertEquals(9, door.members(k).size(), "two stiles in two, three rails, two braces");
                assertEquals(2, door.infills(k).size());
                for (final double angle : new double[] {0, 45, 90}) {
                    final Form skeleton = door.skeleton(k, angle);
                    assertTrue(skeleton.problems().isEmpty(), skeleton.problems().toString());
                    assertEquals(1, skeleton.pieces(), "all one frame");
                    assertEquals(6, skeleton.vertices().size());
                    for (final Door.Member member : door.members(k)) {
                        assertTrue(volume(door.corners(k, member, door.thick(), angle)) > 0, member.name());
                    }
                    for (final List<Point> infill : door.infills(k)) {
                        assertTrue(volume(door.corners(k, infill, Door.INFILL * door.thick(), angle)) > 0, "infill");
                    }
                }
            }
        }
        assertThrows(IllegalArgumentException.class, () -> new Door("three", 300, 200, 5, 3, 90));
    }

    /** How much a box's first corner's three edges span, its bits' way: positive, the right-hand way round. */
    private static double volume(final List<Point> c) {
        final Point a = c.get(1).plus(c.get(0).times(-1));
        final Point b = c.get(2).plus(c.get(0).times(-1));
        final Point d = c.get(4).plus(c.get(0).times(-1));
        return a.across() * (b.along() * d.up() - b.up() * d.along())
                - a.along() * (b.across() * d.up() - b.up() * d.across())
                + a.up() * (b.across() * d.along() - b.along() * d.across());
    }

    @Test
    void onAnAircraftEachLeafHangsOnAHingeOfItsOwnOneControlOpeningThem() {
        final Craft single = Doors.of(SINGLE, "door", Paint.TRIM, Paint.CAR);
        assertEquals(1, single.hinges().size());
        final Hinge hinge = single.hinges().getFirst();
        assertEquals(new Point(0, 0, 1), hinge.axis());
        assertEquals(1, hinge.sense(), 1e-9);
        assertEquals(11, hinge.part().pieces().size(), "its frame and its infill");
        assertEquals(1, hinge.part().skeletons().size());
        final Craft pair = Doors.of(GATE, "gate", Paint.TRIM, Paint.CAR);
        assertEquals(2, pair.hinges().size());
        assertEquals(-pair.hinges().get(0).sense(), pair.hinges().get(1).sense(), 1e-9, "turning opposite ways, both out");
        // the bomber's bomb bay: a pair of doors in its cabin's floor, on the one control, swinging down
        final Map<String, double[]> controls = Hinge.controls(Bomber.body(7000));
        assertTrue(controls.containsKey(Bomber.BAY));
        assertEquals(0, controls.get(Bomber.BAY)[0], 1e-9);
        assertEquals(Bomber.BAY_OPENS, controls.get(Bomber.BAY)[1], 1e-9);
        final List<Hinge> bay = Bomber.body(7000).hinges();
        assertEquals(2, bay.size(), "a pair");
        for (final Hinge leaf : bay) {
            assertEquals(1, Math.abs(leaf.axis().along()), 1e-9, "hung along the bay's long edges");
            // its leaf's free edge, turned the way it opens, goes down
            final Point free = leaf.part().pieces().stream().flatMap(p -> p.corners().stream())
                    .max(java.util.Comparator.comparingDouble(p -> Math.abs(p.across() - leaf.pivot().across()))).orElseThrow();
            final double a = Math.toRadians(30 * leaf.sense());
            final Point r = free.plus(leaf.pivot().times(-1));
            final Point k = leaf.axis();
            // turned about its axis the right-hand way (Rodrigues), as it is drawn
            final double dot = k.across() * r.across() + k.along() * r.along() + k.up() * r.up();
            final double up = r.up() * Math.cos(a) + (k.across() * r.along() - k.along() * r.across()) * Math.sin(a)
                    + k.up() * dot * (1 - Math.cos(a));
            assertTrue(up < r.up() - 1, "swinging down to face the ground");
        }
    }

    @Test
    void theTemplesGateFillsItsPassageAndStandsOpenAgainstItsSidesIntoTheYard() {
        final Door gate = TempleYard.gateDoor();
        assertEquals(2 * TempleYard.PASSAGE, gate.width(), 1e-9);
        assertEquals(TempleYard.SPRING, gate.height(), 1e-9, "up to where its arch springs");
        final double half = TempleYard.PASSAGE;
        for (int k = 0; k < 2; k++) {
            for (final Door.Member member : gate.members(k)) {
                for (final Point p : gate.corners(k, member, gate.thick(), TempleYard.GATE_OPEN)) {
                    final double across = p.across() - half;
                    assertTrue(Math.abs(across) >= half - gate.thick() / 2 - 1e-6, "against its side: " + p);
                    assertTrue(p.along() >= -1e-6 && p.along() <= TempleYard.GATE_DEEP, "into the yard, in its gatehouse");
                }
            }
        }
    }
}
