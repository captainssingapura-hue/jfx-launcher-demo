package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Curve;
import com.example.fxsuite.avatar.sim.Form;
import com.example.fxsuite.avatar.sim.Vec3;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The town church, composed: its nave, apse and tower from plans; its roofs, spire, cross and
 * arches drawn in 3D, curved where they curve; all of it put together, every joint meeting.
 */
class ChurchTest {

    private static final Assembly.Result CHURCH = Church.assembled();
    private static final Form FORM = CHURCH.form();

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
    void itsPartsMeetWhereTheyMeanToAndItIsSound() {
        assertEquals(List.of(), CHURCH.problems());
        assertEquals(List.of(), FORM.problems());
        for (final Tower stack : Church.stacks()) {
            assertEquals(List.of(), stack.problems(), stack.name());
        }
        // one piece, but for the windows and the belfry's openings, each standing apart in its wall
        assertEquals(1 + 12 + 4, FORM.pieces());
        final Form.Bounds box = FORM.bounds();
        assertEquals(1000, box.width(), 1e-6, "the nave's width: the tower and apse narrower");
        assertEquals(-1700, box.back(), 1e-6, "the apse out behind");
        assertEquals(1700, box.front(), 1e-6, "the tower out in front");
        assertEquals(3900, box.high(), 1e-6, "the cross's head the highest of it");
    }

    @Test
    void theRoofStandsOnTheNavesEavesItsFrontGableThroughTheTowersCorners() {
        // the roof's eaves are the nave's top corners: one vertex, the nave's column up to it
        final int eaves = at(-500, -1200, 900);
        // its corner column split where the apse's eaves meet it, at 7.5 m: ground to there, there to the roof
        final int apseEaves = at(-500, -1200, 750);
        assertTrue(eaves >= 0 && joined(eaves, apseEaves) && joined(apseEaves, at(-500, -1200, 0)),
                "the nave's corner column up to the roof's eaves, through the apse's");
        assertTrue(joined(eaves, at(0, -1200, 1500)), "the back gable up from it to the ridge");
        // the front gable: split where the tower's back corners cross it, the tower's stages meeting there
        final int crossing = at(-250, 1200, 1200);
        assertTrue(crossing >= 0);
        assertTrue(joined(crossing, at(-500, 1200, 900)) && joined(crossing, at(0, 1200, 1500)));
        assertTrue(joined(crossing, at(-250, 1200, 900)) && joined(crossing, at(-250, 1200, 1700)),
                "the tower's corner column on through it");
    }

    @Test
    void itsCurvesAreCurvedEdgesArchesCirclesTheSpireFlaredTheApseRound() {
        final long arcs = FORM.edges().stream().filter(e -> e.curved() && e.curve().kind() == Curve.Kind.ARC).count();
        assertEquals(2 * (12 + 4 + 1), arcs, "two arcs to each window, belfry opening and the doorway");
        // a window's left arc: round its circle, centred on its right springing, as wide as the window
        final int springLeft = FORM.vertex("openings, window, right, at -1000, spring left");
        final int point = FORM.vertex("openings, window, right, at -1000, point");
        final Form.Edge arc = FORM.edges().stream().filter(e -> e.from() == springLeft && e.to() == point)
                .findFirst().orElseThrow();
        final Vec3 centre = FORM.at(FORM.vertex("openings, window, right, at -1000, spring right"));
        for (final Vec3 p : FORM.points(arc, 5)) {
            assertEquals(120, p.distance(centre), 1e-6);
        }
        // the spire's edges bowed in toward its middle, their middles nearer it than the straight line's
        final List<Form.Edge> spire = FORM.edges().stream().filter(e -> e.curved() && e.curve().kind() == Curve.Kind.POWER)
                .toList();
        assertEquals(4, spire.size());
        for (final Form.Edge edge : spire) {
            final Vec3 mid = edge.curve().at(FORM.at(edge.from()), FORM.at(edge.to()), 0.5);
            final Vec3 chord = FORM.at(edge.from()).add(FORM.at(edge.to())).multiply(0.5);
            assertTrue(Math.hypot(mid.x(), mid.y() - 1450) < Math.hypot(chord.x(), chord.y() - 1450));
        }
        // the apse's curve: curved edges at its foot and its eaves, every point of them on its circle
        final List<Form.Edge> round = FORM.edges().stream().filter(e -> e.curved() && e.curve().kind() == Curve.Kind.CORNER)
                .toList();
        assertTrue(round.size() >= 4, round.size() + " pieces of its curve");
        for (final Form.Edge edge : round) {
            for (final Vec3 p : FORM.points(edge, 20)) {
                assertEquals(500, Math.hypot(p.x(), p.y() + 1200), 1e-6, "on the apse's circle");
            }
        }
        // and its roof's ribs from every point of its eaves to the apex on the back gable
        final int apex = at(0, -1200, 1250);
        assertTrue(FORM.edges().stream().filter(e -> e.from() == apex || e.to() == apex).count() >= 5);
    }

    @Test
    void theDoorwaysFeetAreJoinedToTheTowersFoot() {
        final int left = at(-90, 1700, 0);
        final int right = at(90, 1700, 0);
        assertTrue(left >= 0 && right >= 0);
        assertTrue(joined(left, at(-250, 1700, 0)), "the tower's front foot split at the doorway");
        assertTrue(joined(right, at(250, 1700, 0)));
    }

    @Test
    void itsStackedPartsPlans() {
        assertEquals(List.of("nave", "apse", "tower"), List.copyOf(Church.plans().keySet()));
        final Plan.Layout apse = Church.plans().get("apse").layout();
        assertEquals(List.of(), apse.problems());
        assertEquals(Math.PI * 500 * 500 / 2, apse.area(), apse.area() * 0.015,
                "a half circle, a little less: its plan's area its outline's straight pieces'");
    }
}
