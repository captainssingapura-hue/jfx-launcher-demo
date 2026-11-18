package com.example.fxsuite.avatar.roads;

import com.example.fxsuite.avatar.sim.Vec2;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
