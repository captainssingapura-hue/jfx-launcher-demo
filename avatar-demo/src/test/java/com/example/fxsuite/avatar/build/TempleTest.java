package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Curve;
import com.example.fxsuite.avatar.sim.Form;
import com.example.fxsuite.avatar.sim.Vec3;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A Chinese temple's main hall, composed: its platform and hall from plans, its bays widest in the
 * middle; bracket sets carrying its eave purlin; its hip roof, eaves swept up at the corners, rafters
 * and hips bowed down; all of it one piece, every joint meeting.
 */
class TempleTest {

    private static final Assembly.Result TEMPLE = Temple.standard().assembled();
    private static final Form FORM = TEMPLE.form();

    private static int at(final double x, final double y, final double z) {
        for (int v = 0; v < FORM.vertices().size(); v++) {
            if (FORM.at(v).distance(new Vec3(x, y, z)) < 0.5) {
                return v;
            }
        }
        return -1;
    }

    private static boolean joined(final int a, final int b) {
        return FORM.edges().stream().anyMatch(e -> e.from() == a && e.to() == b || e.from() == b && e.to() == a);
    }

    @Test
    void itsPartsAllMeetItIsSoundAndOnePiece() {
        assertEquals(List.of(), TEMPLE.problems());
        assertEquals(List.of(), FORM.problems());
        for (final Tower stack : Temple.standard().stacks()) {
            assertEquals(List.of(), stack.problems(), stack.name());
        }
        assertEquals(1, FORM.pieces(), "platform, hall, brackets, roof and steps all one");
        final Form.Bounds box = FORM.bounds();
        assertEquals(2 * 1640, box.width(), 1e-6, "its eaves' corners the widest of it");
        assertTrue(box.high() >= 1375 + 110 && box.high() < 1375 + 140, "the ornaments on its ridge the highest: " + box.high());
    }

    @Test
    void itsBaysWidenTowardTheMiddleItsColumnsOnThePlatform() {
        final Plan.Layout hall = Temple.standard().plans().get("hall").layout();
        assertEquals(List.of(-1320.0, -880.0, -320.0, 320.0, 880.0, 1320.0),
                java.util.Arrays.stream(hall.xs()).boxed().toList());
        assertEquals(24, hall.columns().size(), "six lines across by four along");
        // a column stands on the platform: from its top (1.2 m) to its head (5.7 m)
        assertTrue(joined(at(-320, 640, 120), at(-320, 640, 570)));
    }

    @Test
    void bracketSetsCarryTheEavePurlinTheirTipsOnIt() {
        // a set on the middle front column: its foot on the column's head, its tip on the purlin, 90 out and up
        final int foot = at(-320, 640, 570);
        final int tip = at(-320, 730, 660);
        assertTrue(foot >= 0 && tip >= 0);
        assertTrue(FORM.vertices().get(tip).name().startsWith("brackets") || FORM.vertices().get(tip).name().startsWith("roof"));
        // the purlin split at it: runs on from it both ways
        final long along = FORM.edges().stream().filter(e -> (e.from() == tip || e.to() == tip)
                && Math.abs(FORM.at(e.from()).z() - 660) < 1e-6 && Math.abs(FORM.at(e.to()).z() - 660) < 1e-6
                && Math.abs(FORM.at(e.from()).y() - 730) < 1e-6 && Math.abs(FORM.at(e.to()).y() - 730) < 1e-6).count();
        assertEquals(2, along, "the purlin runs on from the tip each way");
        // at a corner, the set turned to the diagonal, its tip on the purlin's corner
        assertTrue(at(1410, 730, 660) >= 0 && joined(at(1410, 730, 660), at(1640, 960, 670)), "the corner rafter out from it");
        // as many sets as columns round its edge and bays between them
        assertEquals(2 * (2 * 5 + 2 * 3), FORM.vertices().stream().filter(v -> v.name().endsWith(", out 3")).count(),
                "by their tips: their feet are the columns' heads, or on the beams between");
    }

    @Test
    void itsEavesSweepUpAtTheCornersItsRaftersAndHipsBowDown() {
        // the eaves' corners up and out of the line of their middles
        final int corner = at(1640, 960, 670);
        assertTrue(corner >= 0);
        final List<Form.Edge> eaves = FORM.edges().stream().filter(e -> e.curved() && e.curve().kind() == Curve.Kind.POWER
                && e.curve().from() != null).toList();
        assertTrue(eaves.size() >= 4 * 2);
        final Curve front = eaves.stream().map(Form.Edge::curve).filter(c -> c.from().y() > 0 && c.to().y() > 0)
                .findFirst().orElseThrow();
        final double middle = Curve.bowAt(front.control(), front.fullness(), front.from(), front.to(), 0.5).z();
        assertEquals(600, middle, 1e-6, "the front eave's middle 70 cm below its corners");
        // each piece of an eave on its one bow, its ends where the bow has them
        for (final Form.Edge edge : eaves) {
            final Curve c = edge.curve();
            assertEquals(0, Curve.bowAt(c.control(), c.fullness(), c.from(), c.to(), c.t0()).distance(FORM.at(edge.from())), 1e-6);
            assertEquals(0, Curve.bowAt(c.control(), c.fullness(), c.from(), c.to(), c.t1()).distance(FORM.at(edge.to())), 1e-6);
        }
        // rafters and hips bowed down: their middles under their straight lines
        final List<Form.Edge> bowed = FORM.edges().stream().filter(e -> e.curved() && e.curve().kind() == Curve.Kind.POWER
                && e.curve().from() == null).toList();
        assertEquals(2 * 4 + 2 + 4, bowed.size(), "four rafters to the ridge each long side, two to its ends, four hips");
        for (final Form.Edge edge : bowed) {
            final Vec3 mid = edge.curve().at(FORM.at(edge.from()), FORM.at(edge.to()), 0.5);
            assertTrue(mid.z() < (FORM.at(edge.from()).z() + FORM.at(edge.to()).z()) / 2, "concave");
        }
    }

    @Test
    void stepsDownFromThePlatformBeforeTheMiddleBay() {
        assertTrue(joined(at(-320, 880, 120), at(-320, 880, 90)), "down the first riser from the platform's edge");
        assertTrue(at(320, 880 + 4 * 35, 0) >= 0, "out onto the ground");
    }

    @Test
    void builtAnySizeItIsStillOnePieceItsBaysAndBracketsAsMany() {
        final List<Temple.Size> sizes = List.of(Temple.Size.STANDARD, new Temple.Size(3, 450, 3, 350, 60),
                new Temple.Size(9, 800, 5, 700, 240), new Temple.Size(11, 400, 3, 300, 300));
        for (final Temple.Size size : sizes) {
            final Temple temple = Temple.of(size);
            final Assembly.Result built = temple.assembled();
            assertEquals(List.of(), built.problems(), size.toString());
            assertEquals(List.of(), built.form().problems(), size.toString());
            temple.stacks().forEach(stack -> assertEquals(List.of(), stack.problems(), stack.name() + " " + size));
            assertEquals(1, built.form().pieces(), size.toString());
            final double halfWide = java.util.Arrays.stream(size.across()).sum() / 2;
            assertEquals(2 * (halfWide + 270 + 50), built.form().bounds().width(), 1e-6, "its eaves' corners the widest");
            assertEquals(2 * (2 * size.bays() + 2 * size.deep()), built.form().vertices().stream()
                    .filter(v -> v.name().endsWith(", out 3")).count(), "a set on each column round it, and between");
            assertEquals((size.bays() + 1) * (size.deep() + 1), temple.plans().get("hall").layout().columns().size());
            // its widest bay in the middle, narrower out each way, never under half
            final double[] across = size.across();
            for (int b = 0; b < across.length; b++) {
                assertEquals(across[b], across[across.length - 1 - b], 1e-9, "the same each side");
                assertTrue(across[b] <= size.middle() && across[b] >= size.middle() / 2);
            }
            // its steps down to the ground
            assertTrue(built.form().vertices().stream().anyMatch(v -> v.name().startsWith("steps") && Math.abs(v.up()) < 1e-6),
                    "down to the ground");
            final Building building = temple.building();
            assertEquals((size.bays() + 1) * (size.deep() + 1), building.dressed(building.own(), Temple.LACQUERED).stream()
                    .filter(s -> s.name().startsWith("column")).count());
        }
    }

    @Test
    void notBuiltASizeItCannotStandAt() {
        assertThrows(IllegalArgumentException.class, () -> new Temple.Size(4, 640, 3, 450, 120), "no middle bay");
        assertThrows(IllegalArgumentException.class, () -> new Temple.Size(3, 640, 5, 450, 120), "deeper than wide");
        assertThrows(IllegalArgumentException.class, () -> new Temple.Size(5, 640, 3, 1000, 120), "columns too tall");
    }

    @Test
    void inTheWayAsItIsShapedOpenUnderItsEaves() {
        final Temple temple = Temple.standard();
        final List<com.example.fxsuite.avatar.sim.Volume> shape = temple.shape();
        final com.example.fxsuite.avatar.sim.Obstacles world = new com.example.fxsuite.avatar.sim.Obstacles(List.of())
                .with(shape, shape.stream().map(v -> 0).toList());
        // on its platform, under its eaves, between its wall and their edge: the eaves over, room to stand
        final double z = 640 + 150;
        assertEquals(120, world.floor(0, z, 16, 200), 1e-9, "on its platform");
        final double over = world.ceiling(0, z, 32, 120 + 200);
        assertTrue(over > 120 + 200 && over < 700, "the eaves overhead: " + over);
        assertFalse(world.slide(0, z, 300, 0, 32, 140, 320).blocked(), "walked along under them");
        // its roof: on its ridge; and its hip end slopes down to its eaves
        assertEquals(1375, world.floor(0, 0, 1, 1400), 1);
        assertTrue(world.floor(1250, 0, 1, 1400) < 1375 - 100, "down its hip end");
    }
}
