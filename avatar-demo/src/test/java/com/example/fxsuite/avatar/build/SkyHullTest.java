package com.example.fxsuite.avatar.build;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The sky hull, built as a ship is: its shape, as wide as its beam through its middle, to a point at its bow, rounded
 * in at its stern, its bottom rising to its ends; its section, flat across its bottom, round its bilge, up its side;
 * its keel, frames, floors, inner bottom, stringers, beams, stanchions and bulkheads, so many of each; its plating
 * closed, strake to strake and station to station; its plating steel, cut away, or glass; made only to a size it can
 * be built at.
 */
class SkyHullTest {

    private static final SkyHull HULL = SkyHull.standard();
    private static final SkyHull.Size SIZE = HULL.size();
    private static final List<Solid> SOLIDS = HULL.solids(SkyHull.palette());

    @Test
    void asWideAsItsBeamThroughItsMiddleToAPointAtItsBowRoundedInAtItsStern() {
        assertEquals(SIZE.beam() / 2, HULL.half(0), 1e-9);
        assertEquals(0, HULL.half(SIZE.length() / 2), 1e-9, "a point at its bow");
        assertEquals(0, HULL.half(-SIZE.length() / 2), 1e-9, "closed at its stern");
        // to its bow, ever faster; to its stern, as a circle rounds in: half its beam 30% into it still 95% of it
        final double bowStarts = SIZE.length() / 2 - SIZE.bow();
        final double sternEnds = -SIZE.length() / 2 + SIZE.stern();
        assertEquals(SIZE.beam() / 2 * (1 - 0.09), HULL.half(bowStarts + 0.3 * SIZE.bow()), 1e-6);
        assertEquals(SIZE.beam() / 2 * Math.sqrt(1 - 0.09), HULL.half(sternEnds - 0.3 * SIZE.stern()), 1e-6);
        // never wider than its beam, narrowing all the way to each end
        for (double y = 0; y < SIZE.length() / 2; y += 100) {
            assertTrue(HULL.half(y + 100) <= HULL.half(y) + 1e-9 && HULL.half(-y - 100) <= HULL.half(-y) + 1e-9);
        }
        // its bottom level under its middle body, rising to its ends
        assertEquals(0, HULL.keel(0));
        assertEquals(SIZE.rise(), HULL.keel(SIZE.length() / 2), 1e-9);
        assertEquals(SIZE.rise(), HULL.keel(-SIZE.length() / 2), 1e-9);
    }

    @Test
    void itsSectionFlatAcrossItsBottomRoundItsBilgeUpItsSide() {
        final double[][] s = HULL.section(0, 0);
        assertEquals(SkyHull.BOTTOM + SkyHull.BILGE + SkyHull.SIDE + 1, s.length);
        assertEquals(0, s[0][0], 1e-9, "from its middle line");
        assertEquals(0, s[0][1], 1e-9, "on its bottom");
        assertEquals(SIZE.beam() / 2 - SIZE.bilge(), s[SkyHull.BOTTOM][0], 1e-9, "its flat bottom to its bilge");
        // round its bilge, every point as far from the bilge's middle as its radius
        for (int k = SkyHull.BOTTOM; k <= SkyHull.BOTTOM + SkyHull.BILGE; k++) {
            assertEquals(SIZE.bilge(), Math.hypot(s[k][0] - (SIZE.beam() / 2 - SIZE.bilge()), s[k][1] - SIZE.bilge()), 1e-9);
        }
        assertEquals(SIZE.beam() / 2, s[s.length - 1][0], 1e-9, "up its side");
        assertEquals(SIZE.depth(), s[s.length - 1][1], 1e-9, "to its deck");
        // in from its skin, inside it all the way round
        final double[][] in = HULL.section(0, 30);
        for (int k = 0; k < s.length; k++) {
            assertTrue(in[k][0] <= s[k][0] + 1e-9 && in[k][1] >= s[k][1] - 1e-9, "in, " + k);
        }
        assertEquals(30, in[0][1], 1e-9);
        assertEquals(SIZE.beam() / 2 - 30, in[s.length - 1][0], 1e-9);
    }

    private static long named(final String pattern) {
        return SOLIDS.stream().filter(s -> s.name().matches(pattern)).count();
    }

    @Test
    void builtAsAShipIsSoManyOfEach() {
        final int n = SIZE.spaces();
        assertEquals(n, named("keel \\d+"), "its keel, frame space by frame space");
        final int segments = SkyHull.BOTTOM + SkyHull.BILGE + SkyHull.SIDE;
        assertEquals(2L * segments * (n - 1), named("frame .*"), "a frame at every station between its ends");
        assertEquals(n - 1, named("floor \\d+"));
        assertEquals(n, named("inner bottom \\d+"));
        assertEquals(n - 1, named("deck beam \\d+"));
        assertEquals(2L * 5 * n, named(".* stringer .*"), "five stringers each side");
        assertEquals(2L * ((n - 1) / SkyHull.STANCHIONS), named("stanchion .*"));
        final int bulkheads = (n - 1) / SIZE.bulkhead();
        assertEquals(2L * (SkyHull.BOTTOM + SkyHull.BILGE) * bulkheads, named("bulkhead .*"),
                "across its bottom and its bilge, each side, at each bulkhead");
        assertEquals(2L * segments * n, named(".* strake .*"), "its skin, strake by strake");
        assertEquals(n, named("deck \\d+"));
        // the strakes as a ship's are called, from its keel out
        assertEquals(2L * n, named("starboard garboard strake .*") + named("port garboard strake .*"));
        assertEquals(2L * n, named(".* sheer strake .*"));
        assertTrue(SOLIDS.size() > 5_000 && SOLIDS.size() < 12_000, "a third of a tower or so: " + SOLIDS.size());
    }

    private static Solid solid(final String name) {
        return SOLIDS.stream().filter(s -> s.name().equals(name)).findFirst().orElseThrow();
    }

    @Test
    void itsPlatingClosedStrakeToStrakeStationToStation() {
        for (int i = 0; i + 1 < SIZE.spaces(); i += 7) {
            for (int j = 0; j + 1 < SkyHull.BOTTOM + SkyHull.BILGE + SkyHull.SIDE; j++) {
                final Solid here = solid("starboard " + SkyHull.strake(j) + " " + j + ", " + i);
                final Solid on = solid("starboard " + SkyHull.strake(j) + " " + j + ", " + (i + 1));
                final Solid up = solid("starboard " + SkyHull.strake(j + 1) + " " + (j + 1) + ", " + i);
                // its next station's edge the next plate's first; its upper edge the next strake's lower
                assertEquals(here.corners().get(2), on.corners().get(0));
                assertEquals(here.corners().get(3), on.corners().get(1));
                assertEquals(here.corners().get(1), up.corners().get(0));
                assertEquals(here.corners().get(3), up.corners().get(2));
            }
        }
        // and port and starboard meet at its keel
        final Solid port = solid("port garboard strake 0, 50");
        final Solid starboard = solid("starboard garboard strake 0, 50");
        assertEquals(port.corners().get(0), starboard.corners().get(0));
        // its deck from its edge to its edge, on its sheer strakes
        final Solid deck = solid("deck 50");
        final Solid sheer = solid("starboard sheer strake 9, 50");
        assertEquals(deck.corners().get(1), sheer.corners().get(1));
    }

    @Test
    void itsPlatingSteelCutAwayOrGlass() {
        final Building hull = HULL.building();
        final List<Building.Option> plating = hull.choices().get("Plating");
        assertEquals(List.of("Steel", "Cutaway", "Glass"), plating.stream().map(Building.Option::name).toList());
        final Map<String, Material> steel = materials(hull, plating.get(0));
        final Map<String, Material> cut = materials(hull, plating.get(1));
        final Map<String, Material> glass = materials(hull, plating.get(2));
        assertEquals(Material.PRECAST, steel.get("starboard side strake 8, 50"));
        assertEquals(Material.GLASS, cut.get("starboard side strake 8, 50"), "cut away: its starboard side");
        assertEquals(Material.GLASS, cut.get("deck 50"), "and its deck");
        assertEquals(Material.PRECAST, cut.get("port side strake 8, 50"), "its port side kept");
        assertEquals(Material.PRECAST, cut.get("starboard garboard strake 0, 50"), "and its bottom");
        assertEquals(Material.GLASS, glass.get("port side strake 8, 50"), "glass: all its skin");
        assertEquals(Material.GLASS, glass.get("starboard garboard strake 0, 50"));
        assertEquals(Material.METAL, glass.get("frame 50, port 3"), "its frames, steel, to see");
        assertTrue(hull.skeleton().problems().isEmpty(), "its skeleton: " + hull.skeleton().problems());
    }

    private static Map<String, Material> materials(final Building hull, final Building.Option option) {
        final Map<String, Material> by = new java.util.HashMap<>();
        for (final Solid s : hull.dressed(option, SkyHull.palette())) {
            by.put(s.name(), s.material());
        }
        return by;
    }

    @Test
    void madeOnlyToASizeItCanBeBuiltAt() {
        assertThrows(IllegalArgumentException.class, () -> new SkyHull.Size(34_000, 3_800, 700, 800, 9_000, 2_500,
                400, 300, 8, 120), "its bilge inside its depth");
        assertThrows(IllegalArgumentException.class, () -> new SkyHull.Size(34_000, 3_800, 700, 300, 20_000, 15_000,
                400, 300, 8, 120), "its ends within its length");
        assertThrows(IllegalArgumentException.class, () -> new SkyHull.Size(34_000, 3_800, 700, 300, 9_000, 2_500,
                600, 300, 8, 120), "deeper than its double bottom at its ends");
    }
}
