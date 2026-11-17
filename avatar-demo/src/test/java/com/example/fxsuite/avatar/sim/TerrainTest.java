package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.example.fxsuite.avatar.sim.ContourTest.circle;
import static com.example.fxsuite.avatar.sim.ContourTest.square;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A terrain as a map: written and read as text, its loops nested, and what is wrong with one reported. */
class TerrainTest {

    private static final String MEADOW = """
            # a hill on a meadow, a pad for a village beside it
            terrain "meadow" reach=500m every=10m base=0m seed=7
            noise height=1.5m apart=60m octaves=3
            contour 0m -300,-300 300,-300 300,300 -300,300
            contour 10m 50,50 150,50 150,150 50,150   # the hill
            contour 0.5m flat -150,-150 -50,-150 -50,-50 -150,-50
            spot 22.25m at=100,100
            """;

    private static Terrain terrain(final List<Contour> contours, final List<Terrain.Spot> spots) {
        return new Terrain("test", 50_000, 1_000, 0, 1, Terrain.Noise.NONE, contours, spots);
    }

    @Test
    void itIsReadFromTextInMetres() {
        final Terrain meadow = Terrain.parse(MEADOW);
        assertEquals("meadow", meadow.name());
        assertEquals(-50_000, meadow.x0());
        assertEquals(50_000, meadow.z1());
        assertEquals(1_000, meadow.every());
        assertEquals(101, meadow.across());
        assertEquals(101, meadow.along());
        assertEquals(7, meadow.seed());
        assertEquals(new Terrain.Noise(150, 6_000, 3), meadow.noise());
        assertEquals(3, meadow.contours().size());
        assertEquals(new Contour(1_000, square(10_000, 10_000, 5_000)), meadow.contours().get(1));
        assertTrue(meadow.contours().get(2).flat());
        assertEquals(50, meadow.contours().get(2).height());
        assertEquals(List.of(new Terrain.Spot(new Vec2(10_000, 10_000), 2_225)), meadow.spots());
        assertEquals(List.of(), meadow.problems());
    }

    @Test
    void whatItIsWrittenAsReadsBackTheSame() {
        final Terrain meadow = Terrain.parse(MEADOW);
        assertEquals(meadow, Terrain.parse(meadow.text()));
        final Terrain odd = terrain(List.of(new Contour(-123.45, circle(1_234.5, -6.7, 9_999, 7), false)),
                List.of(new Terrain.Spot(new Vec2(0.01, -0.5), 12)));
        assertEquals(odd.text(), Terrain.parse(odd.text()).text(), "to a tenth of a millimetre");
        assertEquals("terrain \"flat\" reach=100m every=5m base=0m seed=0\n", Terrain.flat("flat", 10_000, 500).text());
    }

    @Test
    void badTextIsToldByItsLine() {
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Terrain.parse("contour 5m 0,0 1,0 1,1"))
                .getMessage().startsWith("line 1"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Terrain.parse(MEADOW + "hill 5m\n"))
                .getMessage().startsWith("line 8"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Terrain.parse(MEADOW + "spot 5m at=1,x\n"))
                .getMessage().startsWith("line 8"));
        assertThrows(IllegalArgumentException.class, () -> Terrain.parse("terrain \"x\" reach=100m every=30m"),
                "its heights go evenly across it");
        assertThrows(IllegalArgumentException.class, () -> Terrain.parse(""));
        assertThrows(IllegalArgumentException.class, () -> Terrain.parse("terrain \"x\" x=-100..100m every=10m"),
                "x and z both");
    }

    @Test
    void itsLoopsNest() {
        final Terrain hills = terrain(List.of(
                new Contour(1_000, circle(-20_000, 0, 5_000, 24)),
                new Contour(0, circle(0, 0, 40_000, 48)),
                new Contour(2_000, circle(-20_000, 0, 2_000, 24)),
                new Contour(1_000, circle(20_000, 0, 5_000, 24))), List.of());
        assertArrayEquals(new int[] {1, -1, 0, 1}, hills.parents());
        assertArrayEquals(new int[] {1, 0, 2, 1}, hills.depths());
        assertEquals(2, hills.within(-20_000, 0));
        assertEquals(1, hills.within(0, 0));
        assertEquals(-1, hills.within(45_000, 45_000));
        assertEquals(List.of(), hills.problems());
    }

    @Test
    void whatIsWrongWithOneIsReported() {
        final Contour ground = new Contour(0, square(0, 0, 30_000));
        assertProblem("cross", terrain(List.of(ground, new Contour(500, square(30_000, 0, 5_000))), List.of()));
        assertProblem("crosses itself", terrain(List.of(new Contour(0, List.of(new Vec2(0, 0), new Vec2(100, 100),
                new Vec2(100, 0), new Vec2(0, 150)))), List.of()));
        assertProblem("neither up nor down", terrain(List.of(ground, new Contour(0, square(0, 0, 5_000))), List.of()));
        assertProblem("is flat, yet", terrain(List.of(new Contour(0, square(0, 0, 30_000), true),
                new Contour(500, square(0, 0, 5_000))), List.of()));
        assertProblem("off the map", terrain(List.of(new Contour(0, square(90_000, 0, 5_000))), List.of()));
        assertProblem("cliff", terrain(List.of(ground, new Contour(2_000, square(0, 0, 29_000))), List.of()));
        assertProblem("which is flat", terrain(List.of(new Contour(0, square(0, 0, 5_000), true)),
                List.of(new Terrain.Spot(new Vec2(0, 0), 300))));
        assertProblem("off the map", terrain(List.of(), List.of(new Terrain.Spot(new Vec2(0, 60_000), 300))));
        // a flat pad at the height of the ground round it is no problem
        assertEquals(List.of(), terrain(List.of(ground, new Contour(0, square(0, 0, 5_000), true)), List.of()).problems());
    }

    private static void assertProblem(final String part, final Terrain terrain) {
        final List<String> problems = terrain.problems();
        assertTrue(problems.stream().anyMatch(p -> p.contains(part)), part + " in " + problems);
    }
}
