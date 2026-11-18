package com.example.fxsuite.avatar.roads;

import com.example.fxsuite.avatar.sim.Vec2;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Several roads on the same ground, laid in order: a divided road as two carriageways, two roads, each its line at its
 * inner edge either side of the median; neither's prepared ground on the other's; the median between them one strip
 * of grass; and where one is built up beside the other too close for a slope, a wall wanted.
 */
class RoadworksTest {

    private static final Section CARRIAGEWAY = Sections.standard().get("carriageway");
    private static final double LENGTH = 50_000;
    private static final Profile GENTLE = (x, z, below) -> 200 * Math.sin(z / 8_000) + 0.01 * x;

    private static List<Vec2> straight() {
        final List<Vec2> line = new ArrayList<>();
        for (double z = 0; z <= LENGTH; z += 1_000) {
            line.add(new Vec2(0, z));
        }
        return line;
    }

    @Test
    void aDividedRoadIsTwoCarriagewaysEachItsLineAtItsInnerEdge() {
        final List<Roadworks.Plan> pair = Roadworks.carriageways("dual", straight(), CARRIAGEWAY, List.of(), List.of());
        assertEquals(2, pair.size());
        final double half = CARRIAGEWAY.edge(1);
        // keeping left: the one going north on the west of the centre, the other coming back on the east
        assertEquals(-half, pair.get(0).line().getFirst().x(), 1e-9);
        assertEquals(0, pair.get(0).line().getFirst().z(), 1e-9);
        assertEquals(half, pair.get(1).line().getFirst().x(), 1e-9);
        assertEquals(LENGTH, pair.get(1).line().getFirst().z(), 1e-9, "the other way back");
        final Roadworks works = Roadworks.design(GENTLE, pair);
        // each paved on its own side, away from the centre
        final Roadway one = works.roads().get(0);
        final Roadway other = works.roads().get(1);
        assertFalse(Double.isNaN(one.surface(-half - 500, 20_000)));
        assertFalse(Double.isNaN(other.surface(half + 500, 20_000)));
        assertTrue(Double.isNaN(one.surface(half + 500, 20_000)));
    }

    @Test
    void neitherCarriagewaysGroundOnTheOthersAndTheMedianOneStripOfGrass() {
        final Roadworks works = Roadworks.design(GENTLE,
                Roadworks.carriageways("dual", straight(), CARRIAGEWAY, List.of(), List.of()));
        final Ground ground = works.ground();
        for (final Roadway road : works.roads()) {
            for (int i = 5; i + 5 < road.size(); i += 20) {
                for (double a = -1_100; a <= -50; a += 150) {
                    final Vec2 p = road.at(i, a);
                    assertEquals(road.formation(p.x(), p.z()), ground.at(p.x(), p.z()).height(), 1e-6,
                            "its formation under its pavement, untouched by the other");
                }
            }
        }
        for (double z = 5_000; z < 45_000; z += 2_500) {
            for (double x = -140; x <= 140; x += 35) {
                assertEquals(Cover.GRASS, ground.at(x, z).cover(), "the median grass at " + x + ", " + z);
            }
        }
        assertEquals(List.of(), works.wanted(), "side by side at one level: no wall");
    }

    @Test
    void oneBuiltUpBesideTheOtherTooCloseForASlopeWantsAWall() {
        final List<Roadworks.Plan> pair = Roadworks.carriageways("dual", straight(), CARRIAGEWAY, List.of(), List.of());
        final Roadworks.Plan lifted = new Roadworks.Plan(pair.get(1).name(), pair.get(1).line(), CARRIAGEWAY, List.of(),
                List.of(new Roadway.Clearance(18_000, 32_000, 500)));
        final Roadworks works = Roadworks.design(Profile.level(0), List.of(pair.get(0), lifted));
        assertFalse(works.wanted().isEmpty(), "a wall wanted");
        final Roadworks.Wall wall = works.wanted().getFirst();
        assertEquals(0, wall.road(), "along the first's edge");
        assertEquals(1, wall.against());
        assertEquals(1, wall.side(), "on its median side");
        assertTrue(wall.highest() > 300 && wall.highest() < 600, "about as high as one is over the other: " + wall.highest());
        assertTrue(wall.from() < 25_000 && wall.to() > 25_000);
        assertTrue(works.problems().stream().anyMatch(p -> p.contains("wall")));
    }

    @Test
    void aRoadJoiningAnotherJoinsItAtItsSurfaceNotItsFormation() {
        final Section road = Sections.standard().get("two-lane road");
        final Roadworks.Plan main = new Roadworks.Plan("main", straight(), road, List.of(), List.of());
        // a road off it westward, from in its left lane, a metre from its crown
        final List<Vec2> off = List.of(new Vec2(-100, 25_000), new Vec2(-10_000, 25_000), new Vec2(-30_000, 25_000));
        final Roadworks works = Roadworks.design(Profile.level(0),
                List.of(main, new Roadworks.Plan("off", off, road, List.of(), List.of())));
        final Roadway joining = works.roads().get(1);
        final double surface = works.roads().getFirst().surface(-100, 25_000);
        assertEquals(surface, joining.level(0), 1, "at its surface, as its end is pinned to what is under it");
        assertTrue(surface - joining.level(0) < 50, "not half a metre under it, at its formation");
        // drawn once where they lie over each other: the first's
        assertTrue(works.pavedBefore(1, -100, 25_000));
        assertFalse(works.pavedBefore(0, -100, 25_000));
        assertFalse(works.pavedBefore(1, -20_000, 25_000));
    }

    @Test
    void aRoadLeavingAnotherPavedUpToItsNoseThenTheGrassNoWallWanted() {
        final Section road = Sections.standard().get("two-lane road");
        final Section slip = Sections.standard().get("carriageway");
        final Roadworks.Plan main = new Roadworks.Plan("main", straight(), road, List.of(), List.of());
        // leaving it from its right lane, slowly, to its right: a metre out every twenty along
        final List<Vec2> off = new ArrayList<>();
        for (double s = 0; s <= 30_000; s += 500) {
            off.add(new Vec2(200 + s / 20 + 1_100, 10_000 + s));
        }
        final Roadworks works = Roadworks.design(GENTLE, List.of(main, new Roadworks.Plan("off", off, slip, List.of(),
                List.of())));
        final Roadway leaving = works.roads().get(1);
        final Roadway first = works.roads().getFirst();
        // between them, up to its nose, paved: one or the other's pavement all across the gap
        int gored = 0;
        for (int i = 0; i < leaving.size(); i++) {
            final Section s = leaving.section(i);
            if (s.pavedEdge(1) > slip.pavedEdge(1) + 1) {
                fail("a gore on its far side, away from the road it leaves, at " + i);
            }
            if (s.pavedEdge(-1) > slip.pavedEdge(-1) + 1) {
                gored++;
                // from its pavement's left edge across to the first's: paved all the way
                for (double d = 0; d < s.pavedEdge(-1) - slip.pavedEdge(-1); d += 20) {
                    final Vec2 p = leaving.at(i, -slip.pavedEdge(-1) - d);
                    assertTrue(!Double.isNaN(leaving.surface(p.x(), p.z())) || !Double.isNaN(first.surface(p.x(), p.z())),
                            "paved across the gore at " + i);
                }
                assertTrue(s.pavedEdge(-1) - slip.pavedEdge(-1) <= Roadworks.NOSE + Roadworks.OVERLAP + 1e-6, "to its nose");
            }
        }
        assertTrue(gored > 10, "a gore: " + gored);
        assertEquals(List.of(), works.wanted(), "a junction, not a slope that cannot fit: no wall");
    }

    @Test
    void itsLandSaysWhereItsPavementsRunSoTheLandIsDrawnOutOfTheirWay() {
        final Roadworks works = Roadworks.design(GENTLE,
                Roadworks.carriageways("dual", straight(), CARRIAGEWAY, List.of(), List.of()));
        final com.example.fxsuite.avatar.sim.Land.Layer land = works.land(Double.NaN);
        assertTrue(land instanceof com.example.fxsuite.avatar.sim.Land.Paved);
        final List<com.example.fxsuite.avatar.sim.Land.Band> bands = ((com.example.fxsuite.avatar.sim.Land.Paved) land).bands();
        assertFalse(bands.isEmpty());
        // each pavement's middle, as wide as its pavement either side
        final Roadway one = works.roads().getFirst();
        final com.example.fxsuite.avatar.sim.Land.Band b = bands.getFirst();
        final Vec2 middle = one.at(0, -CARRIAGEWAY.pavedEdge(-1) / 2);
        assertEquals(middle.x(), b.ax(), 1e-6);
        assertEquals(middle.z(), b.az(), 1e-6);
        assertEquals(CARRIAGEWAY.pavedEdge(-1) / 2, b.ha(), 1e-6);
    }

    @Test
    void eachDesignedAgainstWhatThoseBeforeItOccupyAndTheGroundAsItWasElsewhere() {
        final Roadworks works = Roadworks.design(GENTLE,
                Roadworks.carriageways("dual", straight(), CARRIAGEWAY, List.of(), List.of()));
        final Roadway first = works.roads().get(0);
        final Roadway other = works.roads().get(1);
        // on the first's median half: as the first prepared it
        final Vec2 on = first.at(first.size() / 2, 100);
        assertEquals(first.ground().at(on.x(), on.z()).height(), other.profile().height(on.x(), on.z()), 1e-9);
        // on its own line, past the first's edge, where the first's ditch would have been: the ground as it was
        final Vec2 p = other.point(other.size() / 2);
        assertEquals(GENTLE.height(p.x(), p.z()), other.profile().height(p.x(), p.z()), 1e-9);
    }
    @Test
    void aRoadLaidAfterAFlyoverPassesUnderItOnTheGroundEachDrawnWhereItIs() {
        final Section road = Sections.standard().get("two-lane road");
        // a flyover north, held 7 m up over the middle; a road east under it, laid after it
        final Roadworks.Plan over = new Roadworks.Plan("over", straight(), road,
                List.of(new Roadway.Given(15_000, 35_000, Carrier.STRUCTURE)),
                List.of(new Roadway.Clearance(23_000, 27_000, 700)));
        final List<Vec2> east = new ArrayList<>();
        for (double x = -20_000; x <= 20_000; x += 1_000) {
            east.add(new Vec2(x, 25_000));
        }
        final Roadworks works = Roadworks.design(Profile.level(0),
                List.of(over, new Roadworks.Plan("under", east, road, List.of(), List.of())));
        final Roadway flyover = works.roads().get(0);
        final Roadway under = works.roads().get(1);
        assertTrue(flyover.surface(0, 25_000) >= 700 - 1, "the flyover up over it: " + flyover.surface(0, 25_000));
        assertEquals(0, under.surface(0, 25_000), 30, "under it, on the ground, not up on its deck");
        // its pavement there drawn, under the flyover's: at another level, not one lying over it
        assertTrue(works.pavedBefore(1, 0, 25_000), "a pavement over it");
        assertFalse(works.pavedBefore(1, 0, 25_000, under.surface(0, 25_000)), "but not at its level");
    }

    @Test
    void itsPavementInPiecesEachOnlyAsLargeAsItIsTogetherAllOfIt() {
        final Roadway road = Roadworks.design(GENTLE, List.of(new Roadworks.Plan("one", straight(),
                Sections.standard().get("two-lane road"), List.of(), List.of()))).roads().getFirst();
        final List<com.example.fxsuite.avatar.sim.Pavement> pieces = road.pavements();
        assertTrue(pieces.size() > 1);
        final com.example.fxsuite.avatar.sim.Volume.Bounds whole = road.pavement().bounds();
        for (final com.example.fxsuite.avatar.sim.Pavement piece : pieces) {
            assertTrue(piece.bounds().maxZ() - piece.bounds().minZ() < (whole.maxZ() - whole.minZ()) / 2, "smaller");
        }
        for (double z = 500; z < LENGTH; z += 1_700) {
            final double zz = z;
            final double top = road.surface(0, z);
            assertTrue(pieces.stream().anyMatch(p -> p.bounds().minZ() <= zz && p.bounds().maxZ() >= zz
                    && Math.abs(p.topAt(0, zz, 0, Double.POSITIVE_INFINITY) - top) < 1e-6), "a piece at " + z);
        }
    }

    @Test
    void onAOneWayRoadsLineTheStripAtItsLine() {
        final Section oneWay = Sections.parse("""
                courses c: asphalt 40mm, base 200mm
                section "one way" grade=6% vertical=60m fill=2 cut=2 ditch=0,0 bank=6m cutting=6m
                  shoulder 1m c fall=2%
                  lane 3.5m c fall=2%
                  shoulder 0.5m c fall=2%
                  |
                """).get("one way");
        assertEquals(Strip.Use.SHOULDER, oneWay.at(0).strip().use(), "the strip by its line, though none is on its right");
        assertEquals(0.5 * 100, oneWay.at(0).outer() - oneWay.at(0).inner(), 1e-6);
    }
}
