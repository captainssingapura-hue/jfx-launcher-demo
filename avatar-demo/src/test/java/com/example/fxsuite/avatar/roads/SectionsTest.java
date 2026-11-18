package com.example.fxsuite.avatar.roads;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        assertEquals(List.of("two-lane road", "town street", "carriageway", "country lane", "footpath"),
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
        assertEquals(0, road.depth(600), 1e-9, "a verge: ground, no courses");
        assertEquals(road.top(600), road.formation(600), 1e-9, "graded to its surface");
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
        // a carriageway, one way: no centre line; an edge line by the median, a lane line, an edge line by the shoulder
        final List<Section.Marking> one = STANDARD.get("carriageway").markings();
        assertEquals(3, one.size());
        assertTrue(one.stream().allMatch(m -> m.across() < 0), "all on its paved side");
        assertEquals(1, one.stream().filter(m -> !m.solid()).count());
        // a footpath: none
        assertEquals(List.of(), STANDARD.get("footpath").markings());
    }

    @Test
    void itsStripsPavedOrGroundAndItsPavementBetweenItsPavedEdges() {
        final Section road = STANDARD.get("two-lane road");
        assertTrue(road.left().get(1).paved());
        assertFalse(road.left().get(2).paved(), "a verge is ground");
        assertEquals(Cover.GRASS, road.cover(600));
        assertNull(road.cover(100), "a lane has none: it is paved");
        assertEquals(450, road.pavedEdge(-1), 1e-9);
        assertEquals(450, road.pavedEdge(1), 1e-9);
        assertEquals(4, road.paved().size(), "lanes and shoulders");
        assertEquals(0, road.pavedSide(), "paved both sides");
        final Section one = STANDARD.get("carriageway");
        assertEquals(-1, one.pavedSide(), "paved on its left: keeping left");
        assertEquals(0, one.pavedEdge(1), 1e-9, "its median half ground");
        assertEquals(150, one.edge(1), 1e-9);
    }

    @Test
    void whatIsWrongWithItsTextSaysWhichLine() {
        final IllegalArgumentException unknown = assertThrows(IllegalArgumentException.class, () -> Sections.parse("""
                courses verge: turf 50mm
                section "x"
                  lane 3m tarmac
                  |
                """));
        assertTrue(unknown.getMessage().startsWith("line 3:"), unknown.getMessage());
        final IllegalArgumentException unlined = assertThrows(IllegalArgumentException.class, () -> Sections.parse("""
                courses verge: turf 50mm
                section "x"
                  verge 3m grass
                """));
        assertTrue(unlined.getMessage().contains("no line"), unlined.getMessage());
    }

    @Test
    void itsLanesEachWayMoreOrFewerTheOutermostRepeatedOrTakenOff() {
        final Section one = STANDARD.get("carriageway");
        assertEquals(2, one.lanes());
        final Section three = one.withLanes(3);
        assertEquals(3, three.lanes());
        assertEquals(one.width() + 365, three.width(), 1e-9, "a lane more, as wide as the others");
        assertEquals(Strip.Use.SHOULDER, three.left().get(4).use(), "the outer shoulder still outermost but the verge");
        assertEquals(2, three.markings().stream().filter(m -> !m.solid()).count(), "a lane line between each two");
        assertEquals(one, three.withLanes(2), "and back");
        final Section road = STANDARD.get("two-lane road").withLanes(2);
        assertEquals(2, road.left().stream().filter(s -> s.use() == Strip.Use.LANE).count(), "each way");
        assertEquals(2, road.right().stream().filter(s -> s.use() == Strip.Use.LANE).count());
        assertThrows(IllegalArgumentException.class, () -> one.withLanes(0));
    }

    @Test
    void itsStripsWiderOrFallingOtherwiseItsLimitsAndItsGroundChanged() {
        final Section road = STANDARD.get("two-lane road");
        final Section wide = road.withWidth(Strip.Use.LANE, 375);
        assertEquals(1_450, wide.width(), 1e-9);
        assertEquals(375, wide.width(Strip.Use.LANE), 1e-9);
        assertEquals(0.03, road.withFall(Strip.Use.LANE, 0.03).fall(Strip.Use.LANE), 1e-12);
        assertEquals(0.08, road.withLimits(0.08, 4_000).grade(), 1e-12);
        assertEquals(List.of(Strip.Use.VERGE, Strip.Use.SHOULDER, Strip.Use.LANE), road.uses());
        assertTrue(Double.isNaN(road.width(Strip.Use.KERB)));
    }

    @Test
    void writtenAsTextAndReadBackTheSame() {
        final Sections.Catalogue read = Sections.read(Sections.STANDARD);
        final Sections.Catalogue again = Sections.read(Sections.write(read));
        assertEquals(read.sections(), again.sections());
        assertEquals(read.courses(), again.courses());
        // changed, then written: read back as changed
        final Map<String, Section> changed = new java.util.LinkedHashMap<>(read.sections());
        changed.put("carriageway", changed.get("carriageway").withLanes(4).withWidth(Strip.Use.MEDIAN, 425));
        final Sections.Catalogue written = Sections.read(Sections.write(new Sections.Catalogue(read.courses(), changed)));
        assertEquals(changed, written.sections());
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
