package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The three typical buildings, as skeletons: each sound (nothing wrong with it), each standing on
 * the ground about its middle, each as big as it should be; the frame of each one piece, but for
 * the outlines of its openings, standing apart until faces hold them.
 */
class BuildingsTest {

    private static final Form FLATS = Buildings.apartments();
    private static final Form TOWER = Buildings.skyscraper();
    private static final Form CHURCH = Buildings.church();

    @Test
    void eachIsSoundAndStandsOnTheGroundAboutItsMiddle() {
        for (final Form building : Buildings.all()) {
            assertEquals(List.of(), building.problems(), building.name());
            final Form.Bounds box = building.bounds();
            assertEquals(0, box.low(), 1e-9, building.name() + " on the ground");
            assertEquals(0, box.leftmost() + box.rightmost(), 1e-9, building.name() + " about its middle, across");
        }
    }

    /** How many pieces it is in, less the outlines standing apart: those named as openings, each one piece. */
    private static int framePieces(final Form building, final int outlines) {
        return building.pieces() - outlines;
    }

    private static long named(final Form building, final String part) {
        return building.vertices().stream().map(Form.Vertex::name).filter(n -> n.endsWith(part)).count();
    }

    @Test
    void theBrutalistFlatsAreALongLowSlabOfFiveStoreys() {
        final Form.Bounds box = FLATS.bounds();
        assertEquals(3600, box.width(), 1e-9);
        assertEquals(1620 + 350, box.height(), 1e-9, "the roof at 16.2 m, its stair tower over it");
        assertTrue(box.width() > 2.5 * 1200, "long and shallow");
        // a balcony on every other bay of the four upper floors; a window or a door in every bay
        assertEquals(12, named(FLATS, "balcony front left"));
        assertEquals(24, named(FLATS, "back window, low left"));
        assertEquals(12, named(FLATS, "front window, low left"));
        assertEquals(12, named(FLATS, "balcony door, low left"));
        // its frame, its balconies (each apart from it, standing on its floor's edge), the roof tower,
        // and 61 windows' and doors' outlines
        assertEquals(1 + 12 + 1, framePieces(FLATS, 24 + 12 + 12 + 1));
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

    @Test
    void theChurchIsANaveAnApseAndATowerItsCrossTheHighestPoint() {
        final Form.Bounds box = CHURCH.bounds();
        assertEquals(1000, box.width(), 1e-9, "the nave's width");
        assertEquals(3900, box.height(), 1e-9);
        final Form.Vertex cross = CHURCH.vertices().get(CHURCH.vertex("cross, head"));
        assertEquals(box.high(), cross.up(), 1e-9, "the cross on top of all");
        assertTrue(box.depth() > 2.5 * box.width(), "long: nave, apse and tower");
        // the apse beyond the nave's back, the tower beyond its front
        assertEquals(-1700, box.back(), 1e-9);
        assertEquals(1700, box.front(), 1e-9);
        // a pointed window in each of six bays each side, and an opening in each face of the belfry
        assertEquals(12, named(CHURCH, "point") - 1 - 4, "all the points, less the door's and the belfry's");
        assertEquals(1 + 12 + 4, CHURCH.pieces(), "the church, its twelve windows and four belfry openings");
        // the pointed arches are equilateral: as high over their springing as 0.866 of their width
        final Form.Vertex point = CHURCH.vertices().get(CHURCH.vertex("window, left, bay 0, point"));
        final Form.Vertex spring = CHURCH.vertices().get(CHURCH.vertex("window, left, bay 0, spring left"));
        assertEquals(120 * Math.sqrt(3) / 2, point.up() - spring.up(), 1e-9);
    }
}
