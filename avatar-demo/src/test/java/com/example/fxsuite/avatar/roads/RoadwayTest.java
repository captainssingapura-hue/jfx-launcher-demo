package com.example.fxsuite.avatar.roads;

import com.example.fxsuite.avatar.sim.Vec2;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A road designed on its own against a profile, never the land: its heights within its grade and held over its
 * clearances; what carries it, as told or as chosen; its formation under its surface by its courses; the ground
 * shaped to that formation where the ground carries it, and left as it was where something else does.
 */
class RoadwayTest {

    private static final Section ROAD = Sections.standard().get("two-lane road");
    private static final double LENGTH = 50_000;

    private static List<Vec2> straight() {
        final List<Vec2> line = new ArrayList<>();
        for (double z = 0; z <= LENGTH; z += 1_000) {
            line.add(new Vec2(0, z));
        }
        return line;
    }

    private static final Profile HILL = (x, z, below) -> 1_300 * Math.exp(-Math.pow((z - 25_000) / 9_000, 2)) + 0.01 * x;
    private static final Profile VALLEY = (x, z, below) -> -2_600 * Math.exp(-Math.pow((z - 25_000) / 8_000, 2));
    private static final Profile RIDGE = (x, z, below) -> 3_600 * Math.exp(-Math.pow((z - 25_000) / 11_000, 2));

    private static Roadway on(final Profile profile) {
        return Roadway.design("test", straight(), ROAD, profile, List.of(), List.of());
    }

    @Test
    void itsPointsEvenlyApartFromEndToEnd() {
        final Roadway road = on(Profile.level(0));
        assertEquals(LENGTH, road.length(), 1e-6);
        assertEquals(LENGTH / Roadway.STEP + 1, road.size());
        for (int i = 1; i < road.size(); i++) {
            assertEquals(Roadway.STEP, road.along(i) - road.along(i - 1), 1e-6);
        }
    }

    @Test
    void onLevelGroundItLiesOnItAndNothingCarriesItButTheGround() {
        final Roadway road = on(Profile.level(0));
        for (int i = 0; i < road.size(); i++) {
            assertEquals(0, road.level(i), 1e-6);
        }
        assertEquals(List.of(new Roadway.Stretch(0, road.size() - 1, Carrier.GROUND)), road.stretches());
        assertEquals(List.of(), road.problems());
    }

    @Test
    void neverSteeperThanItsGrade() {
        for (final Profile p : List.of(HILL, VALLEY, RIDGE)) {
            assertTrue(on(p).steepest() <= ROAD.grade() + 1e-9);
        }
    }

    @Test
    void itsFormationUnderItsSurfaceByItsCoursesEverywhere() {
        final Roadway road = on(HILL);
        for (int j = 0; j + 1 < road.size(); j += 17) {
            for (double a = -650; a <= 650; a += 50) {
                assertEquals(ROAD.depth(a), road.top(j, 0.3, a) - road.formation(j, 0.3, a), 1e-9);
            }
        }
    }

    @Test
    void whereTheGroundCarriesItTheGroundUnderItIsItsFormation() {
        final Roadway road = on(HILL);
        for (int i = 3; i + 3 < road.size(); i += 11) {
            for (double a = -680; a <= 680; a += 40) {
                final Vec2 p = road.at(i, a);
                assertEquals(road.level(i) + ROAD.formation(a), road.shape(p.x(), p.z(), HILL.height(p.x(), p.z())), 1e-6,
                        "at " + road.along(i) + " along, " + a + " across");
            }
        }
    }

    @Test
    void itsGroundStripsGradedToTheirSurfaceTheGroundUpToItsPavementsTop() {
        final Roadway road = on(HILL);
        final double paved = ROAD.pavedEdge(1);
        final double edge = ROAD.edge(1);
        for (int i = 5; i + 5 < road.size(); i += 13) {
            final Vec2 in = road.at(i, paved - 1);
            final Vec2 out = road.at(i, paved + 1);
            final Prepared under = road.prepared(in.x(), in.z(), HILL.height(in.x(), in.z()));
            final Prepared verge = road.prepared(out.x(), out.z(), HILL.height(out.x(), out.z()));
            assertEquals(road.level(i) + ROAD.top(paved - 1) - 55, under.height(), 1e-6, "its formation, under its pavement");
            assertEquals(Cover.EARTH, under.cover(), "bare under it, till paved");
            assertEquals(road.level(i) + ROAD.top(paved + 1), verge.height(), 1e-6, "the verge at its surface");
            assertEquals(Cover.GRASS, verge.cover());
            assertTrue(Math.abs(verge.height() - (road.level(i) + ROAD.top(paved))) < 1,
                    "the ground graded up to the pavement's top: no step but the pavement's own side");
            // at the verge's outer edge, on into the side slope without a step
            final Vec2 a = road.at(i, edge - 1);
            final Vec2 b = road.at(i, edge + 1);
            assertEquals(road.shape(a.x(), a.z(), HILL.height(a.x(), a.z())),
                    road.shape(b.x(), b.z(), HILL.height(b.x(), b.z())), 2, "no step at the verge's edge");
        }
        assertTrue(Double.isNaN(road.surface(road.at(50, paved + 50).x(), road.at(50, paved + 50).z())),
                "its pavement's surface not on its verge");
    }

    @Test
    void itsSideSlopesAtItsKindsRatiosOutToTheGroundAndNoFurther() {
        final Roadway road = on(HILL);
        final Earthworks e = ROAD.earthworks();
        final double edge = ROAD.edge(1);
        boolean cut = false;
        boolean banked = false;
        for (int i = 10; i + 10 < road.size(); i += 5) {
            final double edgeTop = road.level(i) + ROAD.top(edge);
            for (double d = 10; d < 3_000; d += 70) {
                final Vec2 p = road.at(i, edge + d);
                final double was = HILL.height(p.x(), p.z());
                final double now = road.shape(p.x(), p.z(), was);
                if (was > edgeTop && d > e.ditchWide()) {
                    // in a cutting, past the ditch: up at the cut slope, until the ground
                    assertEquals(Math.min(was, edgeTop + (d - e.ditchWide()) / e.cut()), now, 1e-6);
                    cut |= now < was;
                } else if (was < edgeTop) {
                    assertEquals(Math.max(was, edgeTop - d / e.fill()), now, 1e-6);
                    banked |= now > was;
                }
            }
        }
        assertTrue(cut && banked, "the hill both cut and banked");
        // far off, untouched
        final Vec2 far = road.at(road.size() / 2, road.reach() + 500);
        assertEquals(HILL.height(far.x(), far.z()), road.shaped().height(far.x(), far.z()), 1e-9);
    }

    @Test
    void aValleyTooDeepToBankAcrossIsCarriedOverOnAStructureChosen() {
        final Roadway road = on(VALLEY);
        final Roadway.Stretch middle = road.stretches().stream().filter(s -> s.carrier() == Carrier.STRUCTURE)
                .findFirst().orElseThrow();
        assertTrue(road.along(middle.from()) < 25_000 && road.along(middle.to()) > 25_000, "over the valley's floor");
        assertTrue(road.highestBank() <= ROAD.earthworks().bank() + 1, "no bank higher than it takes");
        // the valley's floor untouched under it
        final int i = road.nearestPoint(25_000);
        final Vec2 p = road.at(i, 100);
        assertEquals(VALLEY.height(p.x(), p.z()), road.shape(p.x(), p.z(), VALLEY.height(p.x(), p.z())), 1e-9);
        assertFalse(road.structures().getFirst().piers().isEmpty(), "piers down to it");
        assertFalse(road.grounded(i), "no ground on it for its verges");
    }

    @Test
    void aRidgeTooHighToCutThroughIsCarriedThroughInATunnelChosen() {
        final Roadway road = on(RIDGE);
        assertTrue(road.stretches().stream().anyMatch(s -> s.carrier() == Carrier.TUNNEL));
        assertTrue(road.deepestCutting() <= ROAD.earthworks().cutting() + 1, "no cutting deeper than it takes");
        final int i = road.nearestPoint(25_000);
        assertEquals(Carrier.TUNNEL, road.carrier(i));
        final Vec2 p = road.point(i);
        assertEquals(RIDGE.height(p.x(), p.z()), road.shape(p.x(), p.z(), RIDGE.height(p.x(), p.z())), 1e-9,
                "the ridge left as it is over it");
    }

    @Test
    void aBridgeGivenIsHeldOverItsClearance() {
        final Profile river = (x, z, below) -> Math.abs(z - 25_000) < 1_500 ? -500 : 0;
        final Roadway road = Roadway.design("river", straight(), ROAD, river,
                List.of(new Roadway.Given(23_000, 27_000, Carrier.STRUCTURE)),
                List.of(new Roadway.Clearance(23_500, 26_500, 450)));
        for (int i = 0; i < road.size(); i++) {
            if (road.along(i) >= 23_500 && road.along(i) <= 26_500) {
                assertTrue(road.level(i) >= 450 - 1, "held over the water: " + road.level(i));
            }
            if (road.along(i) > 23_000 && road.along(i) < 27_000) {
                assertEquals(Carrier.STRUCTURE, road.carrier(i));
            }
        }
        assertTrue(road.steepest() <= ROAD.grade() + 1e-9);
    }

    @Test
    void onAStructureAlreadyThereTheGroundIsNotTouched() {
        final Profile deck = Profile.level(1_000);
        final Roadway road = Roadway.design("deck", straight(), ROAD, deck,
                List.of(new Roadway.Given(0, LENGTH, Carrier.ON)), List.of());
        final Vec2 p = road.at(50, 200);
        assertEquals(1_000, road.shape(p.x(), p.z(), 1_000), 1e-9);
        assertEquals(1_000 + ROAD.top(200), road.surface(p.x(), p.z()), 1e-6, "its surface on it");
    }

    @Test
    void theEarthItMovesCutFromAHillFilledIntoItsSides() {
        final double[] flat = on(Profile.level(0)).earthMoved();
        final double[] hill = on(HILL).earthMoved();
        // on level ground, its formation dug out under it, its ditches either side; nothing banked
        assertTrue(flat[0] > 0 && flat[1] < 1, "flat: " + flat[0] + " cut, " + flat[1] + " filled");
        assertTrue(hill[0] > flat[0] + 2_000, "cut from the hill: " + hill[0] + " against " + flat[0]);
        assertTrue(hill[1] > 100, "filled where it is banked: " + hill[1]);
    }

    /** A circle {@code radius} round, a point every metre, its last its first. */
    private static List<Vec2> circle(final double radius) {
        final List<Vec2> line = new ArrayList<>();
        final int n = (int) Math.round(2 * Math.PI * radius / 100);
        for (int k = 0; k < n; k++) {
            final double t = 2 * Math.PI * k / n;
            line.add(new Vec2(radius * Math.cos(t), radius * Math.sin(t)));
        }
        line.add(line.getFirst());
        return line;
    }

    @Test
    void aLoopGoesOnRoundItsHeightsWithNoEndsWithinItsGradeAcrossItsJoin() {
        final Profile rolling = (x, z, below) -> 600 * Math.sin(x / 7_000) + 400 * Math.cos(z / 5_000);
        final Roadway road = Roadway.design("ring", circle(15_000), ROAD, rolling, List.of(), List.of());
        assertTrue(road.loop());
        assertEquals(2 * Math.PI * 15_000, road.length(), 200, "round it");
        final int n = road.size();
        assertEquals(road.point(0), road.point(n - 1), "its last point its first");
        assertEquals(road.level(0), road.level(n - 1), 1e-6, "as high there both ways");
        assertTrue(road.steepest() <= ROAD.grade() + 1e-9);
        // its join no step: from its last segment into its first as from any into the next
        final double before = road.level(n - 1) - road.level(n - 2);
        final double after = road.level(1) - road.level(0);
        assertEquals(before, after, 30, "no kink at its join");
        // square to it at its join, as either side of it
        final Vec2 r0 = road.right(0);
        final Vec2 rn = road.right(n - 1);
        assertEquals(r0.x(), rn.x(), 1e-9);
        assertEquals(r0.z(), rn.z(), 1e-9);
        // a point by its join is on it, either side
        final Vec2 p = road.at(0, 100);
        assertTrue(road.nearest(p.x(), p.z()) != null, "nothing beyond an end: it has none");
    }

    @Test
    void aLoopOnAStructureAllRoundHeldLevelAsHighAsItsClearanceAsks() {
        final Roadway road = Roadway.design("viaduct ring", circle(12_000), ROAD, Profile.level(0),
                List.of(new Roadway.Given(0, Double.MAX_VALUE, Carrier.STRUCTURE)),
                List.of(new Roadway.Clearance(0, Double.MAX_VALUE, 600)));
        for (int i = 0; i < road.size(); i++) {
            assertEquals(600, road.level(i), 1, "level all round");
        }
        assertEquals(List.of(new Roadway.Stretch(0, road.size() - 1, Carrier.STRUCTURE)), road.stretches());
        assertTrue(road.structures().getFirst().piers().size() > 15, "piers all round");
        assertEquals(List.of(), road.problems());
    }

    @Test
    void pinnedAsHighAsItIsWhereItIsWithinItsGradeBetween() {
        final Roadway road = Roadway.design("pinned", straight(), ROAD, Profile.level(0), List.of(), List.of(), 0,
                List.of(new Roadway.Pin(25_000, 300)));
        assertEquals(300, road.level(road.nearestPoint(25_000)), 1e-9);
        assertTrue(road.steepest() <= ROAD.grade() + 1e-9);
        assertEquals(0, road.level(0), 1e-9, "its ends on the ground still");
    }

    @Test
    void aLowStructuresDeckNoDeeperThanTheRoomUnderItDownToASlab() {
        // a short bridge, its ends pinned 80 cm over level ground
        final Roadway low = Roadway.design("low", straight(), ROAD, Profile.level(0),
                List.of(new Roadway.Given(24_000, 26_000, Carrier.STRUCTURE)), List.of(), 0,
                List.of(new Roadway.Pin(24_000, 80), new Roadway.Pin(26_000, 80)));
        final int middle = low.nearestPoint(25_000);
        assertEquals(Carrier.STRUCTURE, low.carrier(middle));
        assertEquals(80 - Roadway.ROOM, low.deck(middle), 1, "as deep as leaves room under it");
        assertEquals(Roadway.DECK, low.deck(0), 1e-9, "elsewhere as deep as a deck is");
        // a viaduct high over its valley: as deep as a deck is
        final Roadway high = on(VALLEY);
        assertEquals(Roadway.DECK, high.deck(high.nearestPoint(25_000)), 1e-9);
        // lower still: a slab
        final Roadway lower = Roadway.design("lower", straight(), ROAD, Profile.level(0),
                List.of(new Roadway.Given(24_000, 26_000, Carrier.STRUCTURE)), List.of(), 0,
                List.of(new Roadway.Pin(24_000, 30), new Roadway.Pin(26_000, 30)));
        assertEquals(Roadway.SHALLOWEST, lower.deck(lower.nearestPoint(25_000)), 1e-9);
    }

    @Test
    void theSameRoadEveryTime() {
        final Roadway a = on(VALLEY);
        final Roadway b = on(VALLEY);
        for (int i = 0; i < a.size(); i++) {
            assertEquals(a.level(i), b.level(i), 0);
        }
        assertEquals(a.stretches(), b.stretches());
    }
    @Test
    void aKindBlendedIsEachEndAtItsEndsALaneGrowingAsAShoulderBetween() {
        final Section wider = ROAD.withLanes(2);
        assertEquals(ROAD, Section.blend(ROAD, wider, 0));
        assertEquals(wider, Section.blend(ROAD, wider, 1));
        final Section half = Section.blend(ROAD, wider, 0.5);
        // as many strips as the wider, the new lane half as wide, not yet a lane
        assertEquals(wider.laid().size(), half.laid().size());
        assertEquals(1, half.lanes(), "the lane coming is a shoulder till it is one");
        final double grown = (wider.edge(1) - ROAD.edge(1)) / 2;
        assertEquals(ROAD.edge(1) + grown, half.edge(1), 1);
        assertEquals(ROAD.edge(-1) + grown, half.edge(-1), 1);
        // narrowing back: as wide as on the way out
        assertEquals(half.edge(1), Section.blend(wider, ROAD, 0.5).edge(1), 1);
    }

    @Test
    void itsKindChangingAlongItEasedOverItsTransitionCentredWhereItIsSaid() {
        final Section wider = ROAD.withLanes(2);
        final List<Roadway.Part> parts = List.of(new Roadway.Part(15_000, wider, 6_000),
                new Roadway.Part(35_000, ROAD, 6_000));
        final Roadway road = Roadway.design("widening", straight(), ROAD, Profile.level(0), List.of(), List.of(), 0,
                List.of(), parts);
        double was = 0;
        for (int i = 0; i < road.size(); i++) {
            final double s = road.along(i);
            final double edge = road.section(i).edge(1);
            if (s <= 12_000 || s >= 38_000) {
                assertEquals(ROAD.edge(1), edge, 1e-6, "its own kind at " + s);
            } else if (s >= 18_000 && s <= 32_000) {
                assertEquals(wider.edge(1), edge, 1e-6, "the wider at " + s);
            }
            if (s <= 25_000) {
                assertTrue(edge >= was - 1e-6, "widening steadily at " + s);
            }
            was = edge;
        }
        // half way through the first change, half as much wider
        final int mid = (int) Math.round(15_000 / (LENGTH / (road.size() - 1)));
        assertEquals((ROAD.edge(1) + wider.edge(1)) / 2, road.section(mid).edge(1), 30);
        // paved where it is wider, not where it is not
        final double out = (ROAD.pavedEdge(1) + wider.pavedEdge(1)) / 2;
        assertFalse(Double.isNaN(road.surface(out, 25_000)), "the new lane paved");
        assertTrue(Double.isNaN(road.surface(out, 5_000)), "nothing there before it widens");
    }
}
