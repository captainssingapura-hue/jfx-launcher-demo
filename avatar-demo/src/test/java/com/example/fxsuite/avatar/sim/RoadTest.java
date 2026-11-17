package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A road laid on the land: its heights an even sweep over lumpy ground, within its grade, through its pins,
 * its ends on the ground; level across but for its camber; its side slopes as wide as its cutting is deep or
 * its bank high; its surface thinning into the grass; carried over a bridge, the land under it left as it
 * was; and a road forking off another, meeting it as high as it is.
 */
class RoadTest {

    /** Ground rising east, 1 in 20, lumped: up and down 1 m every 40 m. */
    private record Lumpy() implements Land.Layer {

        @Override
        public double shape(final double x, final double z, final double under) {
            return under + x / 20 + 100 * Math.sin(x / 2_000 * Math.PI);
        }

        @Override
        public boolean touches(final double minX, final double minZ, final double maxX, final double maxZ) {
            return true;
        }

        @Override
        public double deepest() {
            return Double.POSITIVE_INFINITY;
        }

        @Override
        public double tallest() {
            return Double.POSITIVE_INFINITY;
        }
    }

    private static final Land GROUND = new Land(null, List.of(new Lumpy()));

    private static final String KINDS = """
            kind lane width=4m surface=gravel grade=8% curve=1/15m slope=1:2 camber=0% transition=12m vertical=40m
            kind road width=7m surface=asphalt grade=8% curve=1/50m slope=1:2 camber=2% transition=30m vertical=60m
            """;

    private static RoadMap map(final String roads) {
        return RoadMap.parse(KINDS + roads);
    }

    @Test
    void anEvenSweepOverLumpyGroundItsEndsOnIt() {
        final RoadMap lane = map("road \"lane\" kind=lane\n  at 0,0\n  to 200,0\n");
        final Road road = Road.of(lane, lane.ways().getFirst(), GROUND, Map.of(), 100);
        final Land laid = GROUND.with(road);
        // level across
        for (double x = 1_000; x < 19_000; x += 700) {
            assertEquals(laid.height(x, 0), laid.height(x, 190), 1e-9, "level across at " + x);
        }
        // the lumps smoothed out of it: within its grade, and its heights far nearer a straight sweep than the ground's
        assertTrue(road.steepest() <= 0.08 + 1e-9, "within its grade: " + road.steepest());
        double lumps = 0;
        for (int i = 1; i + 1 < road.line().size(); i++) {
            lumps = Math.max(lumps, Math.abs(road.level(i - 1) - 2 * road.level(i) + road.level(i + 1)));
        }
        assertTrue(lumps < 2, "an even sweep, its grade changing by little from metre to metre: " + lumps);
        assertEquals(GROUND.height(0, 0), road.level(0), 1e-9, "from the ground it comes from");
        assertEquals(GROUND.height(20_000, 0), road.level(road.line().size() - 1), 1e-9, "to the ground it goes to");
        assertEquals(List.of(), road.problems());
    }

    @Test
    void itsCamberItsSideSlopesAndItsSurface() {
        final RoadMap map = map("road \"road\" kind=road\n  at 0,0\n  to 200,0\n");
        final Road road = Road.of(map, map.ways().getFirst(), GROUND, Map.of(), 100);
        final Land laid = GROUND.with(road);
        final double x = 10_500;
        // its crown, falling 2% to its edges
        assertEquals(laid.height(x, 0) - 0.02 * 350, laid.height(x, 350), 1e-6);
        // its side slope as wide as the bank is high, twice over, then the ground as it was
        final double edge = laid.height(x, 350);
        final double rise = GROUND.height(x, 0) - edge;
        final double wide = Math.max(Road.SLOPE, Math.abs(rise) * 2);
        assertEquals(GROUND.height(x, 350 + wide + 1), laid.height(x, 350 + wide + 1), 1e-9, "past its side slope");
        assertEquals(edge + rise / 2, laid.height(x, 350 + wide / 2), 1, "half way down it");
        // asphalt, thinning into the grass at its edges, none far off
        assertEquals(1, laid.cover(Land.Cover.ASPHALT, x, 0), 1e-9);
        assertEquals(0.5, laid.cover(Land.Cover.ASPHALT, x, 350), 1e-9, "half way, at its edge");
        assertEquals(0, laid.cover(Land.Cover.ASPHALT, x, 350 + Road.EDGE), 1e-9);
        assertEquals(0, laid.cover(Land.Cover.GRAVEL, x, 0), 1e-9, "not gravel");
        assertFalse(road.touches(0, 5_000, 1_000, 6_000));
    }

    @Test
    void carriedOverABridgeThroughItsPins() {
        // a dip 4 m deep across its middle, spanned
        final Land dip = new Land(null, List.of(new Land.Layer() {
            @Override
            public double shape(final double x, final double z, final double under) {
                return under - (Math.abs(x - 10_000) < 1_000 ? 400 : 0);
            }

            @Override
            public boolean touches(final double minX, final double minZ, final double maxX, final double maxZ) {
                return true;
            }

            @Override
            public double deepest() {
                return 400;
            }
        }));
        final RoadMap map = map("road \"road\" kind=road\n  at 0,0\n  to 85,0\n  to 115,0 bridge height=1m\n"
                + "  to 200,0 ground\n");
        final RoadMap.Way way = RoadMap.parse(map.text()).ways().getFirst();
        final Road road = Road.of(map, map.ways().getFirst(), dip, Map.of(1, 100.0), 100);
        assertTrue(way.segments().get(1).bridge());
        final Land laid = dip.with(road);
        assertEquals(dip.height(10_000, 0), laid.height(10_000, 0), 1e-9, "the dip under the bridge as it was");
        assertEquals(0, laid.cover(Land.Cover.ASPHALT, 10_000, 0), 1e-9, "no road laid on it");
        assertEquals(100, road.level(10_000, 0), 1e-6, "carried across as high as its pins");
        assertTrue(road.steepest() <= 0.08 + 1e-9);
    }

    @Test
    void aRoadForkingOffAnotherMeetsItAsHighAsItIs() {
        final RoadMap map = map("""
                node fork at=100,0
                road "trunk" kind=road
                  at 0,0
                  to fork
                  to 200,0
                road "branch" kind=lane
                  from fork
                  to 150,80 curve=+1/100m
                """);
        assertEquals(List.of(), map.problems());
        final List<Road> roads = map.laid(GROUND, 100);
        assertEquals(roads.get(0).level(10_000, 0), roads.get(1).level(0), 1e-9);
    }
}
