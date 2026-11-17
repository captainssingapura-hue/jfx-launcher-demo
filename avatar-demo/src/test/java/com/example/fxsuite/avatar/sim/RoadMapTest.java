package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Roads as described: vertices and homogeneous segments, written and read as text; their lines worked out,
 * straights and arcs, eased through the transitions at the vertices; splitting a segment changing nothing;
 * and what is wrong with them reported.
 */
class RoadMapTest {

    private static final String KINDS = """
            kind lane width=4m surface=gravel grade=12% curve=1/15m slope=1:2 camber=0% transition=12m vertical=30m
            kind race width=12m surface=asphalt grade=5% curve=1/40m slope=1:2 camber=2% transition=20m vertical=60m
            kind path width=1.5m surface=earth grade=20% curve=1/2m slope=1:1 camber=0% transition=0m vertical=5m corners
            """;

    /** The race track: two straights and two half circles, a closed loop of four segments. */
    private static final String TRACK = KINDS + """
            road "track" kind=race loop
              at -80,-60
              to 80,-60
              to 80,60 curve=+1/60m
              to -80,60 curve=0
              to -80,-60 curve=+1/60m
            """;

    @Test
    void readAndWrittenAsText() {
        final RoadMap track = RoadMap.parse(TRACK);
        assertEquals(3, track.kinds().size());
        assertEquals(new RoadMap.Kind("race", 1_200, Land.Cover.ASPHALT, 0.05, 1.0 / 4_000, false, 2, 0.02, 2_000, 6_000),
                track.kind("race"));
        final RoadMap.Way way = track.ways().getFirst();
        assertTrue(way.loop());
        assertEquals(4, way.vertices().size(), "its last back at its first, not kept twice");
        assertEquals(4, way.segments().size());
        assertEquals(1.0 / 6_000, way.segments().get(1).curvature(), 1e-15);
        assertEquals(0, way.segments().get(2).curvature(), "said otherwise");
        assertEquals(1.0 / 6_000, way.segments().get(3).curvature(), 1e-15);
        assertEquals(track, RoadMap.parse(track.text()), "written and read back the same");
        assertEquals(List.of(), track.problems());
        assertTrue(assertThrows(IllegalArgumentException.class, () -> RoadMap.parse(KINDS + "road \"x\" kind=lane\n  to 1,1\n"))
                .getMessage().startsWith("line 5"));
    }

    @Test
    void aKindsMarkingsFollowFromItsLanes() {
        final RoadMap map = RoadMap.parse(KINDS + """
                kind motorway width=11.6m surface=asphalt grade=5% curve=1/200m slope=1:2 camber=2% transition=60m vertical=100m lanes=2x3.65m inner=1m
                """);
        final RoadMap.Kind motorway = map.kind("motorway");
        assertEquals(2, motorway.lanes());
        assertEquals(365, motorway.lane());
        assertEquals(100, motorway.inner());
        assertEquals(map, RoadMap.parse(map.text()), "its lanes written and read back");
        // a solid line at each edge of its lanes, a dashed one between them
        assertEquals(List.of(new RoadMap.Marking(100, RoadMap.EDGE_LINE, 0, 0),
                new RoadMap.Marking(465, RoadMap.LANE_LINE, RoadMap.DASH, RoadMap.GAP),
                new RoadMap.Marking(830, RoadMap.EDGE_LINE, 0, 0)), motorway.markings());
        assertEquals(List.of(), map.kind("lane").markings(), "no lanes, no lines");
        assertThrows(IllegalArgumentException.class, () -> new RoadMap.Kind("narrow", 500, Land.Cover.ASPHALT, 0.05,
                0.01, false, 2, 0, 0, 0, 2, 365, 0), "its lanes fit across it");
    }

    @Test
    void theTracksLineStraightsAndHalfCirclesEasedFromOneToTheNext() {
        final Alignment line = Alignment.of(RoadMap.parse(TRACK), RoadMap.parse(TRACK).ways().getFirst(), 100);
        // two straights of 160 m and two half circles 60 m round: 697 m, less the corners the transitions cut
        assertEquals(2 * 16_000 + 2 * Math.PI * 6_000, line.length(), 1e-6, "as long as its segments, along them");
        // round its far turn, as far from its middle as its radius
        final Vec2 far = line.points().get(line.size() / 4 + 30);
        assertEquals(6_000, Math.hypot(far.x() - 8_000, far.z()), 50);
        // where its line is eased, it heads on smoothly: no turn of more than a few degrees between points 1 m apart
        for (int i = 1; i + 1 < line.size(); i++) {
            final Vec2 a = line.points().get(i - 1);
            final Vec2 b = line.points().get(i);
            final Vec2 c = line.points().get(i + 1);
            final double turn = Math.abs(Math.IEEEremainder(Math.atan2(c.z() - b.z(), c.x() - b.x())
                    - Math.atan2(b.z() - a.z(), b.x() - a.x()), 2 * Math.PI));
            assertTrue(turn < Math.toRadians(3), "smooth at " + i + ": " + Math.toDegrees(turn));
        }
        assertTrue(line.before(1) > 0 && line.after(1) > 0, "eased where a straight meets a curve");
    }

    @Test
    void splittingASegmentChangesNothing() {
        final RoadMap whole = RoadMap.parse(KINDS + """
                road "lane" kind=lane
                  at 0,0
                  to 100,0
                  to 150,50 curve=+1/50m
                """);
        final RoadMap split = RoadMap.parse(KINDS + """
                road "lane" kind=lane
                  at 0,0
                  to 40,0
                  to 100,0
                  to 150,50 curve=+1/50m
                """);
        final Alignment a = Alignment.of(whole, whole.ways().getFirst(), 100);
        final Alignment b = Alignment.of(split, split.ways().getFirst(), 100);
        assertEquals(a.length(), b.length(), 1e-6);
        assertEquals(0, b.before(1) + b.after(1), "no transition where nothing changes");
        for (final Vec2 p : b.points()) {
            final Polyline aLine = new Polyline(a.points(), 100);
            assertTrue(aLine.distance(p.x(), p.z()) < 1, "on the same line");
        }
        assertEquals(List.of(), split.problems());
    }

    @Test
    void whatIsWrongWithThemReported() {
        assertProblem("tighter than a lane", KINDS + "road \"r\" kind=lane\n  at 0,0\n  to 10,0 curve=+1/6m\n");
        assertProblem("cannot reach", KINDS + "road \"r\" kind=lane\n  at 0,0\n  to 100,0 curve=+1/20m\n");
        assertProblem("a corner of 90°", KINDS + "road \"r\" kind=lane\n  at 0,0\n  to 50,0\n  to 50,50\n");
        // a path may turn a corner
        assertEquals(List.of(), RoadMap.parse(KINDS + "road \"r\" kind=path\n  at 0,0\n  to 50,0\n  to 50,50\n").problems());
        assertProblem("too short", KINDS + "road \"r\" kind=race\n  at 0,0\n  to 5,0\n  to 100,30 curve=+1/200m\n");
        assertProblem("no kind of road", "road \"r\" kind=street\n  at 0,0\n  to 50,0\n");
        // two roads crossing, but not where they share a node; and where they do, no problem
        assertProblem("cross", KINDS + "road \"a\" kind=lane\n  at -50,0\n  to 50,0\nroad \"b\" kind=lane\n  at 0,-50\n  to 0,50\n");
        assertEquals(List.of(), RoadMap.parse(KINDS + """
                node middle at=0,0
                road "a" kind=lane
                  at -50,0
                  to middle
                  to 50,0
                road "b" kind=lane
                  from middle
                  to 0,50
                """).problems());
    }

    private static void assertProblem(final String part, final String text) {
        final List<String> problems = RoadMap.parse(text).problems();
        assertTrue(problems.stream().anyMatch(p -> p.contains(part)), part + " in " + problems);
    }
}
