package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Curve;
import com.example.fxsuite.avatar.sim.Form;
import com.example.fxsuite.avatar.sim.Vec3;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A Chinese temple's main hall, composed: its platform and hall from plans, its bays widest in the
 * middle; bracket sets carrying its eave purlin; its hip roof, eaves swept up at the corners, rafters
 * and hips bowed down; all of it one piece, every joint meeting.
 */
class TempleTest {

    private static final Assembly.Result TEMPLE = Temple.assembled();
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
        for (final Tower stack : Temple.stacks()) {
            assertEquals(List.of(), stack.problems(), stack.name());
        }
        assertEquals(1, FORM.pieces(), "platform, hall, brackets, roof and steps all one");
        final Form.Bounds box = FORM.bounds();
        assertEquals(2 * 1640, box.width(), 1e-6, "its eaves' corners the widest of it");
        assertTrue(box.high() >= 1375 + 110 && box.high() < 1375 + 140, "the ornaments on its ridge the highest: " + box.high());
    }

    @Test
    void itsBaysWidenTowardTheMiddleItsColumnsOnThePlatform() {
        final Plan.Layout hall = Temple.plans().get("hall").layout();
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
}
