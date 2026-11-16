package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Body;
import com.example.fxsuite.avatar.sim.Survey;
import com.example.fxsuite.avatar.sim.Terrain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The terrains drawn so far: read without a problem, and the land they say. */
class TerrainsTest {

    @Test
    void theValleyIsAValley() {
        final Terrain valley = Terrains.valley();
        assertEquals(List.of(), valley.problems());
        final Survey land = valley.survey();
        assertEquals(0, land.height(-19_000, 11_000), 1e-3, "its village's pad, level");
        assertTrue(land.height(19_000, -11_800) > 1_700, "its knoll's top: " + land.height(19_000, -11_800));
        assertTrue(land.height(0, 60_000) > 2_000, "the hills round it");
        assertEquals(4_000, land.height(100_000, 100_000), 100, "its edge");
    }

    @Test
    void theMeadowIsTheRiversValleyTheVillageOnItsFloor() {
        final Terrain meadow = Terrains.meadow();
        assertEquals(List.of(), meadow.problems());
        assertEquals(meadow, Terrain.parse(meadow.text()), "drawn to the metre, written and read back the same");
        final Survey land = meadow.survey();
        assertEquals(0, land.height(Village.WELL.x(), Village.WELL.z()), 1e-3);
        for (final Village.House house : Village.VILLAGE) {
            assertEquals(0, land.height(house.place().x(), house.place().z()), 1e-3, "each house on the level floor");
        }
        for (double x = -Body.EDGE; x <= Body.EDGE; x += 50_000) {
            assertEquals(0, land.height(x, Village.riverAt(x)), 1e-3, "the river on the floor, out of the world at both ends");
        }
        // each spot height held at the height nearest it, read between that and those round it
        assertEquals(3_400, land.height(Terrains.KNOLL.x(), Terrains.KNOLL.z()), 50, "its knoll");
        assertEquals(6_600, land.height(Terrains.BEACON.x(), Terrains.BEACON.z()), 50, "its beacon hill");
        assertTrue(land.height(Terrains.HOLLOW.x(), Terrains.HOLLOW.z()) < 300, "its hollow");
        assertTrue(land.height(0, 100_000) > 1_000 && land.height(0, -100_000) > 3_000, "up gently north, steeply south");
        assertEquals(10_000, land.height(0, Body.EDGE), 100, "the moors at its edge");
    }
}
