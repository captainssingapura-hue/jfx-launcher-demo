package com.example.fxsuite.avatar.build;

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
}
