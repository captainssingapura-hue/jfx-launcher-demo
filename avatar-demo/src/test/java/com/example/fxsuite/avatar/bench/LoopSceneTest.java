package com.example.fxsuite.avatar.bench;

import com.example.fxsuite.avatar.bench.kit.Played;
import com.example.fxsuite.avatar.roads.Carrier;
import com.example.fxsuite.avatar.roads.Roadway;
import com.example.fxsuite.avatar.roads.Roadworks;
import com.example.fxsuite.avatar.roads.Section;
import com.example.fxsuite.avatar.roads.Sections;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Obstacles;
import com.example.fxsuite.avatar.sim.Vec2;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The loop: as long round as set, its bends as round, up on a viaduct all round as high as set; the vehicles parked on
 * its deck, driven round on its pavement.
 */
class LoopSceneTest {

    private static final RoadScenes.Scene LOOP = RoadScenes.all().get("loop");
    private static final Section ROAD = Sections.standard().get(LOOP.section());

    private static Roadworks laid(final double length, final double radius, final double height) {
        return Roadworks.design(LOOP.profile(), LOOP.roads(ROAD, Map.of(RoadScenes.LOOP_LENGTH, length,
                RoadScenes.LOOP_RADIUS, radius, RoadScenes.LOOP_HEIGHT, height)));
    }

    @Test
    void asLongRoundAsSetOnAViaductAllRoundAsHighAsSet() {
        for (final double[] set : new double[][] {{1_000, 80, 6}, {2_000, 150, 12}, {400, 36.5, 3}}) {
            final Roadway road = laid(set[0], set[1], set[2]).roads().getFirst();
            assertTrue(road.loop());
            assertEquals(set[0] * 100, road.length(), 100, "as long round as set");
            assertEquals(List.of(new Roadway.Stretch(0, road.size() - 1, Carrier.STRUCTURE)), road.stretches());
            for (int i = 0; i < road.size(); i++) {
                assertEquals(set[2] * 100, road.level(i), 1, "as high as set");
            }
            // its bends as round as set: the farthest from its middle across, the bends' middles
            double widest = 0;
            for (int i = 0; i < road.size(); i++) {
                widest = Math.max(widest, Math.abs(road.point(i).x()));
            }
            assertEquals(set[1] * 100, widest, 5);
        }
    }

    private static Roadway tilted(final double degrees) {
        return Roadworks.design(LOOP.profile(), LOOP.roads(ROAD, Map.of(RoadScenes.LOOP_LENGTH, 1_000.0,
                RoadScenes.LOOP_RADIUS, 80.0, RoadScenes.LOOP_HEIGHT, 6.0, RoadScenes.LOOP_TILT, degrees))).roads().getFirst();
    }

    /** How much its surface rises across from its left paved edge to its right at its {@code i}th point, rise over run. */
    private static double across(final Roadway road, final int i) {
        final Section s = road.section(i);
        final double w = s.pavedEdge(1);
        return (s.top(w) - s.top(-w)) / (2 * w);
    }

    /** Where along its east straight's middle is, and where its north bend begins and is at its middle, from its start. */
    private static final double HALF_STRAIGHT = (100_000 - 2 * Math.PI * 8_000) / 4;

    @Test
    void tiltedIntoItsBendsAsMuchAsSetTowardsTheInside() {
        final double tilt = Math.tan(Math.toRadians(6));
        final Roadway road = tilted(6);
        // the north bend's middle: a quarter of its half-circle on from where it begins
        final int middle = road.nearestPoint(HALF_STRAIGHT + Math.PI * 8_000 / 2);
        // going round it to the left, its inside is its left: its right side the higher
        assertEquals(tilt, across(road, middle), 1e-9, "the set tilt, all across, rising to the outside");
        final Section s = road.section(middle);
        assertEquals(s.top(-200) - s.top(-400), s.top(400) - s.top(200), 1e-9, "one plane: its crown gone");
        // the surface stood on there: higher on the outside
        final Vec2 in = road.at(middle, -400);
        final Vec2 out = road.at(middle, 400);
        assertTrue(road.surface(out.x(), out.z()) - road.surface(in.x(), in.z()) > 0.95 * 800 * tilt);
    }

    @Test
    void crownedOnItsStraightsTheTiltRunInTwoThirdsBeforeEachBendAThirdOnIt() {
        final double tilt = Math.tan(Math.toRadians(6));
        final double crown = 0.025;
        final Roadway road = tilted(6);
        final double wide = ROAD.pavedEdge(1);
        final double run = (crown + tilt) * wide * 200;
        // the far straight's middle: crowned as it always is
        final int straight = road.nearestPoint(2 * HALF_STRAIGHT + Math.PI * 8_000);
        assertEquals(ROAD.top(300), road.section(straight).top(300), 1e-9);
        assertEquals(ROAD.top(-300), road.section(straight).top(-300), 1e-9);
        // going into the north bend: as crowned until two thirds of the run before it, fully tilted a third on it
        final int before = road.nearestPoint(HALF_STRAIGHT - run * 2 / 3 - 300);
        assertEquals(ROAD.top(300), road.section(before).top(300), 1e-9, "not yet running in");
        final int on = road.nearestPoint(HALF_STRAIGHT + run / 3 + 300);
        assertEquals(tilt, across(road, on), 1e-6, "fully tilted");
        final int halfway = road.nearestPoint(HALF_STRAIGHT - run * 2 / 3 + run / 2);
        assertTrue(across(road, halfway) > 0 && across(road, halfway) < tilt, "on its way");
    }

    @Test
    void itsOuterEdgeClimbingNoFasterAgainstItsLineThanOneInTwoHundredAndRoundItsJoinTheSame() {
        final Roadway road = tilted(6);
        final double w = ROAD.pavedEdge(1);
        for (int i = 1; i < road.size(); i++) {
            final double run = road.along(i) - road.along(i - 1);
            for (final double edge : new double[] {-w, w}) {
                final double climb = Math.abs(road.section(i).top(edge) - road.section(i - 1).top(edge)) / run;
                assertTrue(climb <= 1.0 / 200 + 1e-6, "at " + road.along(i) + ": " + climb);
            }
        }
        assertEquals(road.section(0), road.section(road.size() - 1), "as it is either side of its join");
        // untilted, as it was
        final Roadway flat = tilted(0);
        for (int i = 0; i < flat.size(); i += 50) {
            assertEquals(ROAD, flat.section(i));
        }
    }

    @Test
    void itsBendsNoRounderThanACircleAsLongRound() {
        final Roadway road = laid(500, 400, 6).roads().getFirst();
        assertEquals(50_000, road.length(), 100);
        double widest = 0;
        for (int i = 0; i < road.size(); i++) {
            widest = Math.max(widest, Math.abs(road.point(i).x()));
        }
        assertEquals(50_000 / (2 * Math.PI), widest, 50, "a circle");
    }

    @Test
    void theVehiclesParkedOnItsDeckItsPavementStoodOnAllRound() {
        final Roadworks works = laid(1_000, 80, 6);
        final TestSection section = new TestSection(LOOP, ROAD, works);
        final Course course = new Course(new Played(section, Played.Guests.of(TestSection.guests())));
        final Obstacles world = course.obstacles();
        for (final Course.Piece car : course.pieces().stream().filter(p -> p.kind() == Course.Kind.CAR).toList()) {
            assertTrue(car.place().height() > 600, "up on the deck");
        }
        final Roadway road = works.roads().getFirst();
        // past where they are parked, round to its start again
        for (int i = 100; i < road.size(); i += 37) {
            final Vec2 p = road.at(i, -150);
            final double top = road.surface(p.x(), p.z());
            assertEquals(top, world.footing(p.x(), p.z(), 30, top + 50).height(), 5, "on its pavement, " + i);
        }
    }
}
