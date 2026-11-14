package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Block;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Damage;
import com.example.fxsuite.avatar.sim.Demo;
import com.example.fxsuite.avatar.sim.Play;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The town: every building on a site of its own, one piece each, solid where its parts are, clear of
 * the others and of the start; the boxer motorcycle by the start; nothing else.
 */
class TownTest {

    @Test
    void everyBuildingStandsOnASiteOfItsOwnAndTheBoxerByTheStart() {
        final List<Course.Piece> pieces = Town.pieces();
        assertEquals(Town.SITES.size() + 1, pieces.size(), "nine buildings and the motorcycle, nothing else");
        assertEquals(9, Town.SITES.size());
        assertEquals(Course.Kind.CAR, pieces.getLast().kind(), "the boxer last");
        assertEquals(Course.BOXER.shape(null), pieces.getLast().shape());
        for (int k = 0; k < Town.SITES.size(); k++) {
            final Course.Piece building = pieces.get(k);
            assertEquals(Course.Kind.BUILDING, building.kind());
            assertEquals(k, building.tint(), "its site's number, to draw it by");
            assertFalse(building.solid().isEmpty());
            assertTrue(Double.isFinite(Damage.toughness(building)), "broken as a whole");
            // its solid blocks within its box
            for (final Block part : building.solid()) {
                assertTrue(part.top() <= building.block().top() + 1e-6, Town.SITES.get(k).name());
            }
        }
    }

    @Test
    void noBuildingStandsOnAnotherOrOnTheStart() {
        final List<Course.Piece> pieces = Town.pieces();
        for (int a = 0; a < Town.SITES.size(); a++) {
            for (int b = a + 1; b < Town.SITES.size(); b++) {
                assertFalse(pieces.get(a).block().overlaps(pieces.get(b).block()),
                        Town.SITES.get(a).name() + " and " + Town.SITES.get(b).name());
            }
            for (final Block part : pieces.get(a).solid()) {
                assertFalse(part.covers(0, 0, 100), Town.SITES.get(a).name() + " on the start");
                assertFalse(part.covers(300, 600, 200), Town.SITES.get(a).name() + " on the boxer");
            }
        }
    }

    @Test
    void solidWhereItsPartsAreOpenBetween() {
        // the church: its nave, its tower, its spire each a box of their own, not one box round it all
        final Course.Piece church = Town.piece(0);
        assertTrue(church.solid().size() >= 6, church.solid().size() + " parts");
        assertEquals("nave", Town.part("nave, left wall, slab 3"));
        assertEquals("column", Town.part("column 12"));
        assertEquals("roof", Town.part("roof, front slope, slab 40"));
    }

    @Test
    void theTownDemoIsItsBuildingsAndTheMotorcycle() {
        Course.town(Town::pieces);
        final Play play = Play.start(Demo.TOWN, false);
        assertEquals(Town.SITES.size() + 1, play.damage().size());
        assertTrue(play.lifts().stream().anyMatch(l -> l.craft() == Course.BOXER), "the boxer, to ride");
    }
}
