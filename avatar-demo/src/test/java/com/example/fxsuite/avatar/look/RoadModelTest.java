package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.sim.Land;
import com.example.fxsuite.avatar.sim.Road;
import com.example.fxsuite.avatar.sim.RoadMap;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The lines painted on a road: each where its kind's markings put it across the road, as wide as it is, lying
 * just over the road's surface as it falls to its edges; the solid ones the road's length, the dashed ones cut
 * into dashes so long, so far apart; none on a bridge.
 */
class RoadModelTest {

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
    void eachLineWhereItsKindPutsItAsWideAsItIsJustOverTheSurface() {
        final RoadModel.Lines lines = RoadModel.lines(ROAD, 0, 30);
        final float[] p = lines.points();
        // going east, its left edge is north (z up): 5.8 m out; its lines 1 m, 4.65 m and 8.3 m in from it
        final TreeSet<Long> edges = new TreeSet<>();
        for (int v = 0; v < p.length / 3; v++) {
            edges.add(Math.round((double) p[3 * v + 2]));
            final double across = p[3 * v + 2];
            final double surface = -0.02 * Math.min(Math.abs(across), 580);
            assertEquals(surface + RoadModel.LIFT, -p[3 * v + 1], 1e-3, "lifted just over the surface, camber and all");
        }
        assertEquals(new TreeSet<>(java.util.List.of(470L, 490L, 108L, 123L, -260L, -240L)), edges,
                "an edge line 20 cm wide at 4.8 m, a lane line 15 cm wide at 1.15 m, an edge line at -2.5 m");
    }

    @Test
    void solidLinesAlongItDashedOnesCutIntoDashesNoneOnABridge() {
        final RoadModel.Lines lines = RoadModel.lines(ROAD, 0, ROAD.line().size() - 1);
        final float[] p = lines.points();
        double dashed = 0;
        double solid = 0;
        for (int q = 0; q < p.length / 3; q += 4) {
            final double z = p[3 * q + 2];
            final double length = Math.abs(p[3 * (q + 2)] - p[3 * q]);
            if (Math.abs(z - 115) < 10) {
                dashed += length;
            } else {
                solid += length;
            }
        }
        // 170 m off the bridge: each edge line all of it; the lane line a quarter of it, 3 m in every 12 m
        assertEquals(2 * 17_000, solid, 2 * 2 * 200 + 1, "but for a point's length either end of the bridge");
        assertEquals(17_000 / 4.0, dashed, 400);
        for (int q = 0; q < p.length / 3; q += 4) {
            final double x = (p[3 * q] + p[3 * (q + 2)]) / 2;
            assertTrue(x < 12_000 || x > 15_000, "none on the bridge: " + x);
        }
    }
}
