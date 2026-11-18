package com.example.fxsuite.avatar.roads;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kinds of road across, read from their text: their strips either side of the line, how high their surface is
 * across them, their formation under it by their courses, the lines painted on them, and as they are on a structure.
 */
class SectionsTest {

    private static final Map<String, Section> STANDARD = Sections.standard();

    @Test
    void theStandardKindsAllRead() {
        assertEquals(List.of("two-lane road", "town street", "dual carriageway", "country lane", "footpath"),
                List.copyOf(STANDARD.keySet()));
        final Section road = STANDARD.get("two-lane road");
        assertEquals(3, road.left().size());
        assertEquals(Strip.Use.LANE, road.left().getFirst().use(), "from the line outward");
        assertEquals(Strip.Use.VERGE, road.left().getLast().use());
        assertEquals(1_400, road.width(), 1e-9);
        assertEquals(0.06, road.grade(), 1e-12);
        assertEquals(2, road.earthworks().fill(), 1e-12);
        assertEquals(800, road.earthworks().cutting(), 1e-9);
    }

    @Test
    void itsSurfaceFallsFromItsCrownByEachStripsFall() {
        final Section road = STANDARD.get("two-lane road");
        assertEquals(0, road.top(0), 1e-9);
        assertEquals(-350 * 0.025, road.top(350), 1e-9);
        assertEquals(-350 * 0.025, road.top(-350), 1e-9, "the same either side");
        assertEquals(-450 * 0.025, road.top(450), 1e-9, "the shoulder on, falling as the lane does");
        assertEquals(-450 * 0.025 - 250 * 0.05, road.top(700), 1e-9, "the verge, steeper");
        assertEquals(road.top(700), road.top(2_000), 1e-9, "beyond its edge, as at it");
    }

    @Test
    void itsFormationUnderItsSurfaceByItsCourses() {
        final Section road = STANDARD.get("two-lane road");
        assertEquals(55, road.depth(100), 1e-9, "asphalt, binder, base, subbase");
        assertEquals(-55, road.formation(0), 1e-9);
        assertEquals(20, road.depth(600), 1e-9, "turf and topsoil");
        for (double a = -700; a <= 700; a += 37) {
            assertEquals(road.top(a) - road.depth(a), road.formation(a), 1e-9);
        }
    }

    @Test
    void aKerbStepsUpAndTheFootwayRisesFromIt() {
        final Section street = STANDARD.get("town street");
        final double gutter = street.top(325);
        assertEquals(-325 * 0.025, gutter, 1e-9);
        assertEquals(gutter + 12, street.top(330), 1e-9, "the kerb's upstand");
        assertTrue(street.top(540) > street.top(345), "the footway rising away from it");
    }

    @Test
    void itsLinesPaintedWhereLanesMeetAndAlongTheirEdges() {
        final List<Section.Marking> road = STANDARD.get("two-lane road").markings();
        assertEquals(3, road.size());
        assertEquals(0, road.get(1).across(), 1e-9);
        assertTrue(!road.get(1).solid(), "the centre line dashed");
        assertTrue(road.get(0).solid() && road.get(2).solid(), "the edge lines solid");
        assertEquals(350 - Section.EDGE_LINE / 2, road.get(2).across(), 1e-9, "just inside the lane");
        // a dual carriageway: no centre line, an edge line by the median, a lane line, an edge line by the shoulder
        final List<Section.Marking> dual = STANDARD.get("dual carriageway").markings();
        assertEquals(6, dual.size());
        assertTrue(dual.stream().noneMatch(m -> Math.abs(m.across()) < 1));
        assertEquals(2, dual.stream().filter(m -> !m.solid()).count());
        // a footpath: none
        assertEquals(List.of(), STANDARD.get("footpath").markings());
    }

    @Test
    void onAStructureItsOuterVergesGo() {
        final Section road = STANDARD.get("two-lane road");
        final Section bare = road.bare();
        assertEquals(900, bare.width(), 1e-9);
        assertEquals(road.top(300), bare.top(300), 1e-9);
        final Section street = STANDARD.get("town street");
        assertSame(street, street.bare(), "nothing to take off");
    }

    @Test
    void whatIsWrongWithItsTextSaysWhichLine() {
        final IllegalArgumentException unknown = assertThrows(IllegalArgumentException.class, () -> Sections.parse("""
                courses verge: turf 50mm
                section "x"
                  lane 3m carriageway
                  |
                """));
        assertTrue(unknown.getMessage().startsWith("line 3:"), unknown.getMessage());
        final IllegalArgumentException unlined = assertThrows(IllegalArgumentException.class, () -> Sections.parse("""
                courses verge: turf 50mm
                section "x"
                  verge 3m verge
                """));
        assertTrue(unlined.getMessage().contains("no line"), unlined.getMessage());
    }

    @Test
    void itsMeasuresReadInTheirUnits() {
        assertEquals(250, Sections.length("2.5m"), 1e-9);
        assertEquals(15, Sections.length("15cm"), 1e-9);
        assertEquals(4, Sections.length("40mm"), 1e-9);
        assertEquals(0.025, Sections.fraction("2.5%"), 1e-12);
        assertEquals(-0.02, Sections.fraction("-2%"), 1e-12);
        assertEquals(2, Sections.ratio("1:2"), 1e-12);
        assertEquals(1.5, Sections.ratio("1.5"), 1e-12);
    }
}
