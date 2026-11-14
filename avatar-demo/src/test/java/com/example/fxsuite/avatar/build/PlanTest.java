package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.build.Plan.Layout;
import com.example.fxsuite.avatar.build.Plans.Shape;
import com.example.fxsuite.avatar.build.Plans.Size;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Floor plans composed of rectangles of grid: their outlines traced, their corners shaped (cut,
 * rounded, an ellipse, fuller), their columns where their lines cross within them, their area as the
 * shape's; and what is wrong with one said.
 */
class PlanTest {

    private static Layout plan(final Shape shape, final double width, final double depth, final double extra) {
        return Plans.of(shape, new Size(width, depth, 900, extra)).layout();
    }

    @Test
    void aRectangleIsFourCornersItsColumnsAtItsBaysCrossings() {
        final Layout rect = plan(Shape.RECTANGLE, 4800, 3000, 0);
        assertEquals(List.of(), rect.problems());
        assertEquals(1, rect.square().size());
        assertEquals(4, rect.square().getFirst().size());
        assertEquals(4800 * 3000, rect.area(), 1e-6);
        assertEquals(2 * (4800 + 3000), rect.perimeter(), 1e-6);
        // bays as near 9 m as divide it: 5 across (9.6 m), 3 along (10 m); a column at every crossing
        assertEquals(6 * 4, rect.columns().size());
        assertTrue(rect.square().getFirst().stream().allMatch(p -> rect.inside(p)));
        assertTrue(Layout.signedArea(rect.square().getFirst()) > 0, "its outside anticlockwise");
        // and as a graph: its columns, beams between them, its outline along its edge beams
        final var graph = rect.graph();
        assertEquals(List.of(), graph.problems());
        assertEquals(1, graph.pieces());
        assertEquals(24, graph.vertices().size());
        assertEquals(5 * 4 + 6 * 3, graph.edges().size(), "its beams, the outline along the outer ones");
    }

    @Test
    void aCircleAndAnOvalAreAsTheirShapesAreInStraightPiecesNoWiderThanAPanel() {
        final Layout circle = plan(Shape.CIRCLE, 3600, 0, 0);
        assertEquals(List.of(), circle.problems());
        assertEquals(Math.PI * 1800 * 1800, circle.area(), Math.PI * 1800 * 1800 * 0.005, "within half a percent");
        final List<Plan.Pt> round = circle.shaped().getFirst();
        for (int k = 0; k < round.size(); k++) {
            assertEquals(1800, Math.hypot(round.get(k).x(), round.get(k).y()), 1e-6, "on the circle");
            assertTrue(round.get(k).distance(round.get((k + 1) % round.size())) <= Corner.PANEL + 1e-6);
        }
        // its grid's corners outside it: no columns there
        assertFalse(circle.columns().containsValue(new Plan.Pt(-1800, -1800)));
        assertTrue(circle.columns().containsValue(new Plan.Pt(0, 0)));
        final Layout oval = plan(Shape.OVAL, 4800, 3000, 2);
        assertEquals(Math.PI * 2400 * 1500, oval.area(), Math.PI * 2400 * 1500 * 0.005);
        // fuller ovals have more floor, toward the rectangle; a squareness of 1, a diamond
        final double fuller = plan(Shape.OVAL, 4800, 3000, 4).area();
        assertTrue(fuller > oval.area() && fuller < 4800 * 3000);
        assertEquals(4800 * 3000 / 2.0, plan(Shape.OVAL, 4800, 3000, 1).area(), 1e-6);
    }

    @Test
    void cornersCutOrRoundedTakeTheirShareOff() {
        final double r = 600;
        final Layout rounded = plan(Shape.ROUNDED, 4800, 3000, r);
        // less a sliver at each straight piece of the curves: within a fifth of a percent
        final double exact = 4800 * 3000 - 4 * (r * r - Math.PI * r * r / 4);
        assertEquals(exact, rounded.area(), exact * 0.002);
        assertTrue(rounded.area() < exact, "the curves drawn in chords, inside them");
        final Plan chamfered = Plan.of("chamfered", Grid.of("g", 0, 0, 3000, 3000, 1000).shaped(Corner.chamfer(500)));
        assertEquals(3000 * 3000 - 4 * 500 * 500 / 2.0, chamfered.layout().area(), 1e-6);
        assertEquals(8, chamfered.layout().shaped().getFirst().size(), "each corner cut straight, once");
    }

    @Test
    void anLIsOneGridItsCornerCutOutAlongItsOwnLines() {
        final Layout l = plan(Shape.L_SHAPE, 4800, 3600, 1800);
        assertEquals(List.of(), l.problems());
        assertEquals(6, l.square().getFirst().size(), "five outside corners and one inside");
        // its arms on its lines, as near 18 m as they come: two bays of 9.6 m across, two of 9 m along
        assertEquals(4800 * 3600 - (4800 - 1920) * (3600 - 1800), l.area(), 1e-6);
        assertEquals(6, l.xs().length, "no lines but its own: no columns crowding each other");
        assertEquals(5, l.ys().length);
        assertEquals(1, l.graph().pieces());
    }

    @Test
    void gridsPutTogetherShareTheirLinesWhereTheyAreNear() {
        // two grids whose lines miss each other by 60 cm and 1.2 m: the second's moved onto the first's
        final Plan plan = Plan.of("two", Grid.of("a", 0, 0, 4800, 1800, 960),
                new Grid("b", 0, 0, new double[] {900, 900}, Grid.even(3600, 900), java.util.Map.of()));
        final Layout layout = plan.layout();
        assertEquals(List.of(), layout.problems());
        assertEquals(List.of(0.0, 960.0, 1920.0, 2880.0, 3840.0, 4800.0),
                java.util.Arrays.stream(layout.xs()).boxed().toList(), "b's 900 and 1800 onto a's 960 and 1920");
        // and lines further than that apart are kept: a column line of its own
        final Layout apart = Plan.of("apart", Grid.of("a", 0, 0, 4800, 1800, 960),
                new Grid("b", 0, 0, new double[] {1400, 1400}, Grid.even(3600, 900), java.util.Map.of())).layout();
        assertTrue(java.util.Arrays.stream(apart.xs()).anyMatch(x -> x == 1400), "kept: 4.4 m and 4.8 m from a's");
    }

    @Test
    void theTwoTogetherAnEndRoundAndABowFront() {
        final Layout end = plan(Shape.ROUND_END, 4800, 3000, 0);
        assertEquals(List.of(), end.problems());
        assertEquals((4800 - 1500) * 3000 + Math.PI * 1500 * 1500 / 2, end.area(), end.area() * 0.002);
        final Layout bow = plan(Shape.BOW, 4800, 3000, 1200);
        assertEquals(List.of(), bow.problems());
        assertEquals(4800 * 3000 + Math.PI * 1200 * 1200 / 2, bow.area(), bow.area() * 0.002);
        assertEquals(1500 + 1200, bow.bounds()[3], 1e-6, "the bow out of the front by its radius");
        assertEquals(1, bow.graph().pieces());
    }

    @Test
    void aCourtyardCutOutAndWhatIsWrongSaid() {
        final Plan court = Plan.of("court", Grid.of("all", 0, 0, 3000, 3000, 1000))
                .cut(Grid.of("court", 1000, 1000, 1000, 1000, 1000));
        final Layout layout = court.layout();
        assertEquals(List.of(), layout.problems());
        assertEquals(2, layout.square().size(), "its outside and its courtyard");
        assertTrue(Layout.signedArea(layout.square().get(1)) < 0, "the courtyard clockwise");
        assertEquals(3000 * 3000 - 1000 * 1000, layout.area(), 1e-6);
        // a corner cut more than its edges have
        final Layout overrun = Plan.of("x", Grid.of("g", 0, 0, 1000, 1000, 1000).shaped(Corner.round(800))).layout();
        assertTrue(overrun.problems().stream().anyMatch(p -> p.contains("overrun")), overrun.problems().toString());
        // an inside corner shaped
        final Plan inside = Plan.of("l", Grid.of("all", 0, 0, 2000, 2000, 1000))
                .cut(Grid.of("notch", 1000, 1000, 1000, 1000, 1000).shaped(Grid.Quarter.SOUTH_WEST, Corner.round(300)));
        assertTrue(inside.layout().problems().stream().anyMatch(p -> p.contains("inside corner")),
                inside.layout().problems().toString());
        // two parts touching at a corner only
        final Layout touching = Plan.of("t", Grid.of("a", 0, 0, 1000, 1000, 1000), Grid.of("b", 1000, 1000, 1000, 1000, 1000))
                .layout();
        assertFalse(touching.problems().isEmpty());
    }

    @Test
    void everyShapeAtItsUsualSizeIsSound() {
        for (final Shape shape : Shape.values()) {
            final Layout layout = plan(shape, 4800, 3200, shape.usual());
            assertEquals(List.of(), layout.problems(), shape.title());
            assertTrue(layout.area() > 0, shape.title());
            assertEquals(List.of(), layout.graph().problems(), shape.title());
        }
    }
}
