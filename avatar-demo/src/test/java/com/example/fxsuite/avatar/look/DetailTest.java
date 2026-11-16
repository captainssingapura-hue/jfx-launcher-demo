package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.build.Material;
import com.example.fxsuite.avatar.build.Point;
import com.example.fxsuite.avatar.build.Role;
import com.example.fxsuite.avatar.build.Solid;
import com.example.fxsuite.avatar.build.Town;
import com.example.fxsuite.avatar.sim.Block;
import com.example.fxsuite.avatar.sim.Vec3;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How much of a building is drawn by how far off it is seen: its body always; what dresses it within
 * so far; the finest of it nearer still; each coming back a little nearer than it went.
 */
class DetailTest {

    /** A box {@code across} by {@code along} by {@code up}, doing {@code role}. */
    private static Solid box(final Role role, final double across, final double along, final double up) {
        final List<Point> corners = new ArrayList<>();
        for (int k = 0; k < 8; k++) {
            corners.add(new Point((k & 1) == 0 ? 0 : across, (k & 2) == 0 ? 0 : along, (k & 4) == 0 ? 0 : up));
        }
        return new Solid("a box", role, Material.CONCRETE, corners);
    }

    @Test
    void itsShapeIsItsBodyWhatDressesItDetailTheThinnestAndSmallestFine() {
        assertEquals(Detail.Level.BODY, Detail.of(box(Role.WALL, 600, 20, 300)), "a wall, thin as it is");
        assertEquals(Detail.Level.BODY, Detail.of(box(Role.STRUCTURE, 4000, 3000, 30)), "a floor slab");
        assertEquals(Detail.Level.BODY, Detail.of(box(Role.GLAZING, 150, 2, 280)), "glass: a tower's face");
        assertEquals(Detail.Level.DETAIL, Detail.of(box(Role.DRESSING, 600, 25, 15)), "a sill");
        assertEquals(Detail.Level.DETAIL, Detail.of(box(Role.FRAME, 100, 12, 220)), "a door's frame");
        assertEquals(Detail.Level.FINE, Detail.of(box(Role.FRAME, 5, 8, 280)), "a mullion");
        assertEquals(Detail.Level.FINE, Detail.of(box(Role.DRESSING, 900, 4, 4)), "a trim");
        assertEquals(Detail.Level.FINE, Detail.of(box(Role.WALL, 40, 40, 40)), "anything that small");
    }

    @Test
    void nothingIsLostSplittingABuildingAndATowersFramesAreNotItsBody() {
        final List<Solid> tower = Town.solids(Town.FIRST_TOWER + 1);
        final Map<Detail.Level, List<Solid>> split = Detail.split(tower);
        assertEquals(tower.size(), split.values().stream().mapToInt(List::size).sum(), "every solid in a level");
        assertFalse(split.get(Detail.Level.BODY).isEmpty());
        assertTrue(split.get(Detail.Level.BODY).stream().noneMatch(s -> s.role() == Role.FRAME), "no frame drawn far off");
        assertTrue(split.get(Detail.Level.BODY).size() < tower.size() * 0.6,
                split.get(Detail.Level.BODY).size() + " of " + tower.size() + " drawn far off");
    }

    @Test
    void eachIsShownWithinSoFarAndComesBackALittleNearerThanItWent() {
        assertTrue(Detail.shown(Detail.Level.BODY, 1e9, true), "its body, however far");
        assertTrue(Detail.shown(Detail.Level.FINE, Detail.FINE_FAR - 1, true));
        assertFalse(Detail.shown(Detail.Level.FINE, Detail.FINE_FAR + 1, true), "gone, past so far");
        assertFalse(Detail.shown(Detail.Level.FINE, Detail.FINE_FAR - 1, false), "not back just inside it");
        assertTrue(Detail.shown(Detail.Level.FINE, Detail.FINE_FAR * Detail.BACK - 1, false), "back, a little nearer");
        assertTrue(Detail.shown(Detail.Level.DETAIL, Detail.FINE_FAR * 2, true), "what dresses it, further than the finest");
        assertFalse(Detail.shown(Detail.Level.DETAIL, Detail.DETAIL_FAR + 1, true));
    }

    @Test
    void howFarIsFromItsNearestPoint() {
        // a box 20 by 10 m about (100 m, 0), 30 m tall, turned square to the way
        final Block box = new Block(10_000, 0, 1_000, 500, 90, 0, 3_000);
        assertEquals(0, World.away(box, new Vec3(10_000, -1_000, 0)), 1e-9, "in it");
        assertEquals(1_000, World.away(box, new Vec3(10_000, -4_000, 0)), 1e-6, "over it");
        // turned 90°, its width runs along the world's z, its depth along its x
        assertEquals(500, World.away(box, new Vec3(11_000, -1_000, 0)), 1e-6, "off its side");
        assertEquals(1_000, World.away(box, new Vec3(10_000, -1_000, 2_000)), 1e-6, "off its end");
    }
}
