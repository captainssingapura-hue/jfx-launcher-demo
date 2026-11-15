package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Form;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Towers stacked from plans, and their skeletons: a level of each plan's graph at every floor, the
 * columns up between them, sections standing on each other's columns; curves ringed with columns;
 * and what is wrong with one said.
 */
class TowerTest {

    private static long at(final Form form, final double up) {
        return form.vertices().stream().filter(v -> Math.abs(v.up() - up) < 1e-6).count();
    }

    @Test
    void eachPresetIsSoundOnePieceAndAsTallAsItsFloors() {
        for (final Tower tower : Towers.all()) {
            assertEquals(List.of(), tower.problems(), tower.name());
            final Form skeleton = tower.skeleton();
            assertEquals(List.of(), skeleton.problems(), tower.name());
            assertEquals(1, skeleton.pieces(), tower.name() + " all one piece");
            assertEquals(tower.height(), skeleton.bounds().height(), 1e-6, tower.name());
        }
    }

    @Test
    void aSingleSectionIsItsPlanAtEveryLevelItsColumnsUpBetween() {
        final Plan plan = Plans.of(Plans.Shape.RECTANGLE, new Plans.Size(2400, 1600, 800, 0));
        final Tower tower = Tower.of("block", plan, 10, 350);
        final Form skeleton = tower.skeleton();
        final Form level = plan.layout().graph();
        // eleven levels of the plan's graph, its columns up through the ten floors between them
        assertEquals(11 * level.vertices().size(), skeleton.vertices().size());
        final long columns = plan.layout().allColumns().size();
        assertEquals(11 * level.edges().size() + 10 * columns, skeleton.edges().size());
        assertEquals(level.vertices().size(), at(skeleton, 3500));
        assertTrue(skeleton.vertices().stream().anyMatch(v -> v.name().equals("level 10, block, column x0 y0")));
        assertEquals(3500, tower.height(), 1e-9);
        assertEquals(10, tower.floors());
    }

    @Test
    void sectionsStandOnEachOtherSharingTheLevelBetween() {
        final Tower setback = Towers.setback();
        final Form skeleton = setback.skeleton();
        // the podium's roof is the shaft's first floor: both plans' vertices there, the shaft's on the podium's
        final double first = setback.base(1);
        assertEquals(1000, first, 1e-9);
        final Plan.Layout podium = setback.sections().get(0).plan().layout();
        assertEquals(podium.graph().vertices().size(), at(skeleton, first),
                "the shaft's columns among the podium's, none of its own");
        // every shaft column above that level stands on a column below: an upright edge down from it
        final double second = first + 400;
        for (final Form.Vertex v : skeleton.vertices()) {
            if (Math.abs(v.up() - second) < 1e-6 && v.name().contains("column")) {
                final int index = skeleton.vertices().indexOf(v);
                assertTrue(skeleton.edges().stream().anyMatch(e -> (e.from() == index || e.to() == index)
                        && Math.abs(skeleton.vertices().get(e.from() == index ? e.to() : e.from()).up() - first) < 1e-6),
                        v.name() + " stands on a column");
            }
        }
        assertEquals(15_400, setback.height(), 1e-9);
    }

    @Test
    void aCurveIsRingedWithColumnsTiedInToItsGrid() {
        final Plan.Layout circle = Towers.round().sections().getFirst().plan().layout();
        final var ring = circle.perimeterColumns();
        assertTrue(ring.size() >= Math.ceil(Math.PI * 4000 / Plan.Layout.PERIMETER), ring.size() + " round it");
        for (final Plan.Pt p : ring.values()) {
            assertEquals(2000, Math.hypot(p.x(), p.y()), 1e-6, "on its edge");
        }
        assertEquals(ring.size(), circle.ties().size(), "each tied in");
        assertEquals(1, circle.structures());
        // and a rectangle has none: its grid reaches its edges
        assertEquals(0, Plans.of(Plans.Shape.RECTANGLE, new Plans.Size(2400, 1600, 800, 0)).layout()
                .perimeterColumns().size());
    }

    @Test
    void whatIsWrongWithAStackIsSaid() {
        // a shaft on a grid offset from its podium's: its columns on nothing; and one wider than what it stands on
        final Plan podium = Plans.of(Plans.Shape.SQUARE, new Plans.Size(4800, 0, 800, 0));
        final Plan offset = Plans.of(Plans.Shape.SQUARE, new Plans.Size(4000, 0, 1000, 0));
        final Plan wider = Plans.of(Plans.Shape.SQUARE, new Plans.Size(5600, 0, 800, 0));
        final List<String> problems = new Tower("bad", List.of(new Tower.Section("podium", podium, 2, 500),
                new Tower.Section("shaft", offset, 10, 400), new Tower.Section("top", wider, 2, 400))).problems();
        assertTrue(problems.stream().anyMatch(p -> p.startsWith("shaft: ") && p.contains("stand on no column below")),
                problems.toString());
        assertTrue(problems.stream().anyMatch(p -> p.equals("top: it reaches beyond the section under it")),
                problems.toString());
    }
}
