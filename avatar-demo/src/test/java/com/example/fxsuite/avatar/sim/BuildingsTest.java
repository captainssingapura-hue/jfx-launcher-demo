package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The typical buildings drawn whole, as skeletons (the town church is composed: {@code build.ChurchTest}):
 * each sound (nothing wrong with it), each standing on
 * the ground about its middle, each as big as it should be; the frame of each one piece, but for
 * the outlines of its openings, standing apart until faces hold them.
 */
class BuildingsTest {

    private static final Form FLATS = Buildings.apartments();
    private static final Form TOWER = Buildings.skyscraper();

    @Test
    void eachIsSoundAndStandsOnTheGroundAboutItsMiddle() {
        for (final Form building : Buildings.all()) {
            assertEquals(List.of(), building.problems(), building.name());
            final Form.Bounds box = building.bounds();
            assertEquals(0, box.low(), 1e-9, building.name() + " on the ground");
            assertEquals(0, box.leftmost() + box.rightmost(), 1e-9, building.name() + " about its middle, across");
        }
    }

    private static long named(final Form building, final String part) {
        return building.vertices().stream().map(Form.Vertex::name).filter(n -> n.endsWith(part)).count();
    }

    @Test
    void theBrutalistFlatsAreALongLowSlabOfFiveStoreysItsStructureOnly() {
        final Form.Bounds box = FLATS.bounds();
        assertEquals(3600, box.width(), 1e-9);
        assertEquals(1200, box.depth(), 1e-9);
        assertEquals(1620 + 350, box.height(), 1e-9, "the roof at 16.2 m, its stair tower over it");
        assertTrue(box.width() > 2.5 * box.depth(), "long and shallow");
        // its grid of columns, at six levels; no windows nor balconies: those are its facades' to say
        assertEquals(6 * 7 * 3, FLATS.vertices().stream().filter(v -> v.name().contains(", x")
                && !v.name().startsWith("roof tower") && !v.name().startsWith("parapet")).count());
        assertEquals(0, named(FLATS, "low left"));
        assertEquals(2, FLATS.pieces(), "its frame, and the stair tower standing on its roof");
    }

    @Test
    void theSkyscraperIsTallSlenderAndSetBackItsSpireOnTop() {
        final Form.Bounds box = TOWER.bounds();
        assertEquals(4800, box.width(), 1e-9, "its podium the widest of it");
        assertEquals(20_000, box.height(), 1e-9, "200 m to the tip of its spire");
        assertTrue(box.height() > 4 * box.width());
        assertEquals(1, TOWER.pieces(), "all one frame");
        // the floors, and the set-back: 32 m square below it, 24 m above
        assertEquals(37, TOWER.vertices().stream().filter(v -> v.name().startsWith("tower, floor ")
                && v.name().endsWith(", x0 z0")).count());
        assertEquals(-1600, TOWER.vertices().get(TOWER.vertex("tower, floor 27, x0 z0")).across(), 1e-9);
        assertEquals(-1200, TOWER.vertices().get(TOWER.vertex("tower, floor 28, x0 z0")).across(), 1e-9);
    }
}
