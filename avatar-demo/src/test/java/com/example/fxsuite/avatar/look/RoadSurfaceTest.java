package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.sim.Land;
import com.example.fxsuite.avatar.sim.Road;
import com.example.fxsuite.avatar.sim.RoadMap;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A road's surface as a strip of its own: its edges and its crown where the road has them, just over its
 * surface, camber and all; facing up, tipped as its camber tips it; none over a bridge.
 */
class RoadSurfaceTest {

    private static final RoadMap MAP = RoadMap.parse("""
            kind motorway width=11.6m surface=asphalt grade=5% curve=1/200m slope=1:2 camber=2% transition=60m vertical=100m lanes=2x3.65m inner=1m
            road "east" kind=motorway
              at 0,0
              to 120,0
              to 150,0 bridge
              to 200,0 ground
            """);

    private static final Road ROAD = Road.of(MAP, MAP.ways().getFirst(), Land.LEVEL, Map.of(), 200);

    @Test
    void itsEdgesAndCrownWhereTheRoadHasThemJustOverItsSurface() {
        final RoadSurface.Strip strip = RoadSurface.strip(ROAD, 0, 10);
        final float[] p = strip.points();
        assertEquals(33, strip.size(), "three across, at each of its points");
        for (int k = 0; k <= 10; k++) {
            // going east, its left is north (+z): its left edge, its crown, its right edge
            assertEquals(580, p[9 * k + 2], 1e-3);
            assertEquals(0, p[9 * k + 5], 1e-3);
            assertEquals(-580, p[9 * k + 8], 1e-3);
            // the crown 2% over the edges, 5.8 m out: 11.6 cm; all of it lifted over the surface
            assertEquals(-(0 + RoadSurface.LIFT), p[9 * k + 4], 1e-3);
            assertEquals(-(-0.02 * 580 + RoadSurface.LIFT), p[9 * k + 1], 1e-3);
            assertEquals(-(-0.02 * 580 + RoadSurface.LIFT), p[9 * k + 7], 1e-3);
        }
        assertEquals(4 * 10, strip.faces().length / 9, "four triangles between each two points");
    }

    @Test
    void facingUpTippedAsItsCamberTipsIt() {
        final float[] n = RoadSurface.strip(ROAD, 0, 10).normals();
        for (int v = 0; v < n.length / 3; v++) {
            assertTrue(n[3 * v + 1] < -0.99, "up, the world's y down");
        }
        // its left edge tipped to the left, its right edge to the right
        assertTrue(n[2] > 0.01, "left: " + n[2]);
        assertTrue(n[8] < -0.01, "right: " + n[8]);
    }

    @Test
    void noneOverABridge() {
        final RoadSurface.Strip strip = RoadSurface.strip(ROAD, 0, ROAD.line().size() - 1);
        final float[] p = strip.points();
        final int[] f = strip.faces();
        for (int t = 0; t < f.length / 9; t++) {
            double x = 0;
            for (int c = 0; c < 3; c++) {
                x += p[3 * f[9 * t + 3 * c]] / 3;
            }
            assertTrue(x < 12_000 || x > 15_000, "none on the bridge: " + x);
        }
    }
}
