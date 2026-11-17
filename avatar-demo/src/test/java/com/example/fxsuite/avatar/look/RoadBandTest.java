package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.sim.Land;
import com.example.fxsuite.avatar.sim.Road;
import com.example.fxsuite.avatar.sim.RoadMap;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The land drawn lowered out of a road's way, an artificial offset: how far outside a road's edge a point is, on it
 * less than nothing; none of the road's line over a bridge; lowered by all its level may be out and more, out to
 * as far as a triangle crossing the road may reach, easing back to the land over as far again; and a coarse
 * triangle spanning the road, its corners off it, under the road where it crosses it.
 */
class RoadBandTest {

    private static final RoadMap MAP = RoadMap.parse("""
            kind motorway width=11.6m surface=asphalt grade=5% curve=1/200m slope=1:2 camber=2% transition=60m vertical=100m lanes=2x3.65m inner=1m
            road "east" kind=motorway
              at 0,0
              to 120,0
              to 150,0 bridge
              to 300,0 ground
            """);

    private static final RoadBand BAND = new RoadBand(List.of(Road.of(MAP, MAP.ways().getFirst(), Land.LEVEL, Map.of(), 200)));

    @Test
    void howFarOutsideItsEdgeLessThanNothingOnItNoneOverTheBridge() {
        final List<RoadBand.Segment> near = BAND.near(-100_000, -100_000, 400_000, 100_000, 0);
        assertEquals(-580, RoadBand.edge(near, 5_000, 0), 1, "its middle: its whole half width in");
        assertEquals(0, RoadBand.edge(near, 5_000, 580), 1, "its edge");
        assertEquals(1_000, RoadBand.edge(near, 5_000, -1_580), 1, "10 m out");
        assertTrue(RoadBand.edge(near, 13_500, 0) > 900, "over the bridge, none of it: its ends 15 m off: " + RoadBand.edge(near, 13_500, 0));
        assertEquals(Double.POSITIVE_INFINITY, RoadBand.edge(List.of(), 0, 0), "no road near");
        assertTrue(BAND.near(-100_000, 50_000, 0, 60_000, 1_000).isEmpty(), "none near ground 500 m off it");
    }

    @Test
    void loweredByAllItsLevelMayBeOutOutAsFarAsATriangleCrossingItMayReachEasingBack() {
        final double out = 150;
        final double featured = 25_000;
        assertEquals(out + TerrainModel.SUNK, RoadBand.lowered(-500, out, featured), 1e-9, "on it");
        assertEquals(out + TerrainModel.SUNK, RoadBand.lowered(RoadBand.BAND * featured, out, featured), 1e-9,
                "as far out as a square's diagonal");
        assertEquals(0, RoadBand.lowered(2 * RoadBand.BAND * featured, out, featured), 1e-9, "back to the land");
        final double half = RoadBand.lowered(1.5 * RoadBand.BAND * featured, out, featured);
        assertEquals((out + TerrainModel.SUNK) / 2, half, 1e-9, "half way back, half as low");
    }

    @Test
    void aCoarseTriangleAcrossTheRoadItsCornersOffItUnderTheRoad() {
        // a level 3 square, 250 m, its corners 100 m either side of the road, on level land: the road at 0, the land
        // drawn as high as its level may be out, 1.5 m, over it
        final int level = 3;
        final List<RoadBand.Segment> near = BAND.near(-20_000, -20_000, 20_000, 20_000, 2 * RoadBand.BAND * TerrainModel.FEATURED[level]);
        double highest = Double.NEGATIVE_INFINITY;
        for (final double z : new double[] {-10_000, 10_000}) {
            final double drawn = TerrainModel.TOLERANCE[level] - RoadBand.lowered(RoadBand.edge(near, 5_000, z),
                    TerrainModel.TOLERANCE[level], TerrainModel.FEATURED[level]);
            highest = Math.max(highest, drawn);
        }
        assertTrue(highest < -RoadSurface.LIFT, "its corners, and so all of it between, under the road: " + highest);
    }
}
