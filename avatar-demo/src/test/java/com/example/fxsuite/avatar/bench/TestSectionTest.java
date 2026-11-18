package com.example.fxsuite.avatar.bench;

import com.example.fxsuite.avatar.roads.Carrier;
import com.example.fxsuite.avatar.roads.Roadway;
import com.example.fxsuite.avatar.roads.Roadworks;
import com.example.fxsuite.avatar.roads.Section;
import com.example.fxsuite.avatar.roads.Sections;
import com.example.fxsuite.avatar.sim.Body;
import com.example.fxsuite.avatar.sim.Clock;
import com.example.fxsuite.avatar.sim.Controls;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Obstacles;
import com.example.fxsuite.avatar.sim.Vec2;
import com.example.fxsuite.avatar.sim.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A scene of the roads bench as a world to drive in: a demo made on the fly, never recorded; its vehicles parked on the
 * first road, standing on its top; the road stood on where the ground carries it; a viaduct's deck stood on over the
 * valley, the valley's floor far below.
 */
class TestSectionTest {

    private static TestSection section(final String name) {
        return section(name, true);
    }

    private static TestSection section(final String name, final boolean ownPavement) {
        final RoadScenes.Scene scene = RoadScenes.all().get(name);
        final Section kind = Sections.standard().get(scene.section());
        return new TestSection(scene, kind, Roadworks.design(scene.profile(), scene.roads(kind, java.util.Map.of())), ownPavement);
    }

    private static Roadworks works(final String name) {
        final RoadScenes.Scene scene = RoadScenes.all().get(name);
        return Roadworks.design(scene.profile(), scene.roads(Sections.standard().get(scene.section()), java.util.Map.of()));
    }

    @Test
    void madeOnTheFlyNeverOfferedNorRecorded() {
        final TestSection demo = section("hill");
        assertFalse(demo.offered());
        assertFalse(demo.recordable());
        assertTrue(demo.title().contains("hill"));
    }

    @Test
    void oneOfEachVehicleParkedOnTheFirstRoadStandingOnItsTop() {
        final Roadworks works = works("hill");
        final Course course = new Course(section("hill"));
        final List<Course.Piece> cars = course.pieces().stream().filter(p -> p.kind() == Course.Kind.CAR).toList();
        assertEquals(TestSection.VEHICLES.size(), cars.size());
        for (int k = 0; k < cars.size(); k++) {
            final Course.Piece car = cars.get(k);
            final double top = works.pavement(car.place().x(), car.place().z());
            assertFalse(Double.isNaN(top), "on the road's pavement");
            assertEquals(top + TestSection.VEHICLES.get(k).ride(), car.place().height(), 1e-6, "on its own pavement's top");
        }
    }

    @Test
    void theRoadStoodOnItsOwnPavementOverTheLandAtItsFormation() {
        final Roadworks works = works("hill");
        final Obstacles world = new Course(section("hill")).obstacles();
        final Roadway road = works.roads().getFirst();
        for (final int i : new int[] {30, 120, 200}) {
            final Vec2 p = road.at(i, -200);
            final double top = works.pavement(p.x(), p.z());
            // the highest of it under a circle 30 cm round: a little over the middle's, on a grade and a crossfall
            assertEquals(top, world.footing(p.x(), p.z(), 30, top + 50).height(), 5, "on the pavement");
            assertEquals(road.formation(p.x(), p.z()), world.land().height(p.x(), p.z()), 1e-6, "the land at its formation");
            // a body stood there a second: on it still
            Body body = Body.standing(new Vec3(p.x(), -top, p.z()), 0);
            for (int n = 0; n < 120; n++) {
                body = body.step(Controls.NONE, Clock.STEP, world);
            }
            assertEquals(top, body.height(), 5, "standing on it still");
        }
    }

    @Test
    void orThePavementInTheLandALittleUnderWhereItIsDrawn() {
        final Roadworks works = works("hill");
        final Obstacles world = new Course(section("hill", false)).obstacles();
        final Roadway road = works.roads().getFirst();
        final Vec2 p = road.at(120, -200);
        final double top = works.pavement(p.x(), p.z());
        assertEquals(top - TestSection.SINK, world.footing(p.x(), p.z(), 30, top + 50).height(), 1e-6);
    }

    @Test
    void aViaductsDeckStoodOnOverTheValley() {
        final Roadworks works = works("valley");
        final Roadway road = works.roads().getFirst();
        final int i = road.nearestPoint(25_000);
        assertEquals(Carrier.STRUCTURE, road.carrier(i));
        final Vec2 p = road.point(i);
        final Obstacles world = new Course(section("valley")).obstacles();
        final double deck = world.footing(p.x(), p.z(), 30, road.level(i) + 50).height();
        assertEquals(road.level(i), deck, 30, "on its deck, at its road's level");
        assertTrue(road.level(i) - works.ground().at(p.x(), p.z()).height() > 1_000, "the valley's floor far below");
        // stood on it a second: not fallen through
        Body body = Body.standing(new Vec3(p.x(), -deck, p.z()), 0);
        for (int n = 0; n < 120; n++) {
            body = body.step(Controls.NONE, Clock.STEP, world);
        }
        assertEquals(deck, body.height(), 3, "standing on it still");
    }
}
