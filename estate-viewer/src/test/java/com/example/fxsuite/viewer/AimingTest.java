package com.example.fxsuite.viewer;

import com.example.fxsuite.deployment.Version;
import com.example.fxsuite.estate.DemoEstate;
import com.example.fxsuite.estate.Ledger;
import com.example.fxsuite.viewer.EstateLayout.Placed;

import javafx.geometry.Point3D;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Phase 3's maths: what the crosshair is on, where to stand to read it, and walking a graph. */
class AimingTest {

    private static final double FOV = 35;
    private static final Point3D SLATE = new Point3D(100, 30, 50);

    private static void near(Point3D expected, Point3D actual) {
        assertTrue(expected.distance(actual) < 1e-6, "expected " + expected + " but was " + actual);
    }

    private static Aim.Target<String> box(String name, double x, double y, double z) {
        return new Aim.Target<>(name, new Point3D(x, y, z), new Point3D(100, 30, 10));
    }

    @Test
    void theCrosshairIsOnTheNearestSlateItsLineOfSightPassesThrough() {
        List<Aim.Target<String>> slates = List.of(box("far", 0, 0, 900), box("near", 0, 0, 400), box("aside", 600, 0, 200));
        Optional<Aim.Hit<String>> hit = new Aim(5000).nearest(Point3D.ZERO, new Point3D(0, 0, 1), slates);
        assertEquals("near", hit.orElseThrow().thing(), "a slate hides those behind it");
        assertEquals(390, hit.orElseThrow().distance(), 1e-9, "to its face, not its centre");
    }

    @Test
    void missingOrBehindOrOutOfRangeIsNothing() {
        Aim aim = new Aim(5000);
        assertTrue(aim.nearest(Point3D.ZERO, new Point3D(0, 0, 1), List.of(box("above", 0, -200, 400))).isEmpty());
        assertTrue(aim.nearest(Point3D.ZERO, new Point3D(0, 0, 1), List.of(box("behind", 0, 0, -400))).isEmpty());
        assertTrue(new Aim(300).nearest(Point3D.ZERO, new Point3D(0, 0, 1), List.of(box("far", 0, 0, 400))).isEmpty());
    }

    @Test
    void aimingAtAnAngleStillFindsTheEdgeOfASlate() {
        // from the left, looking right and a little ahead, it clips the slate's left end
        Point3D from = new Point3D(-1000, 0, 400);
        Point3D toLeftEnd = new Point3D(-95, 0, 400).subtract(from);
        assertEquals("s", new Aim(5000).nearest(from, toLeftEnd, List.of(box("s", 0, 0, 400))).orElseThrow().thing());
        Point3D pastIt = new Point3D(-95, 0, 420).subtract(from);
        assertTrue(new Aim(5000).nearest(from, pastIt, List.of(box("s", 0, 0, 400))).isEmpty());
    }

    @Test
    void readingASlateMeansStandingInFrontOfTheFaceYouAreOnLookingAtIt() {
        Approach approach = new Approach(FOV, 1060 / 880.0, 0.4);
        double back = approach.distance(306, 90);

        FlyCamera front = approach.standpoint(SLATE, 306, 90, new Point3D(-2000, -800, -1500));
        near(SLATE.subtract(0, 0, back), front.position());
        near(SLATE.subtract(front.position()).normalize(), front.forward());

        FlyCamera behind = approach.standpoint(SLATE, 306, 90, new Point3D(900, 0, 3000));
        near(SLATE.add(0, 0, back), behind.position());
        near(SLATE.subtract(behind.position()).normalize(), behind.forward());
    }

    @Test
    void theSlateTakesUpTheAskedShareOfTheScreen() {
        double aspect = 1060 / 880.0;
        double tanV = Math.tan(Math.toRadians(FOV) / 2);
        Approach approach = new Approach(FOV, aspect, 0.4);
        double back = approach.distance(306, 90);
        assertEquals(0.4, 306 / (2 * back * tanV * aspect), 1e-9, "wide slates go by their width");
        // a tall one goes by its height instead
        double tall = approach.distance(50, 400);
        assertEquals(0.4, 400 / (2 * tall * tanV), 1e-9);
    }

    @Test
    void glidingBetweenStandpointsTurnsTheShortWayRound() {
        FlyCamera from = new FlyCamera(Point3D.ZERO, 170, 20);
        FlyCamera to = new FlyCamera(new Point3D(1000, 0, 0), -170, -20);
        FlyCamera half = from.towards(to, 0.5);
        assertEquals(180, half.yaw(), 1e-9);
        assertEquals(0, half.pitch(), 1e-9);
        near(new Point3D(500, 0, 0), half.position());
        near(to.position(), from.towards(to, 1).position());
    }

    @Test
    void aTourStartsAtTheReleaseThenGoesBackThroughTheLayersAndRoundAgain() {
        EstateLayout layout = new EstateLayout(new Releases(new DemoEstate().units()).all());
        Version<?> ledger = new Ledger.V1_20261101_0();
        Highlighter.Highlight highlight = new Highlighter().of(layout, ledger);
        Tour tour = Tour.of(layout, highlight.members());

        assertEquals(highlight.members().size(), tour.size());
        assertEquals(ledger, tour.stops().get(0));
        int last = Integer.MAX_VALUE;
        for (Version<?> stop : tour.stops()) {
            int depth = layout.of(stop).map(Placed::depth).orElseThrow();
            assertTrue(depth <= last, "never forward again: " + stop);
            last = depth;
        }
        assertEquals(0, last, "ends at the base layer");

        assertEquals(tour.stops().get(1), tour.step(ledger, 1));
        assertEquals(tour.stops().get(tour.size() - 1), tour.step(ledger, -1), "back from the first is the last");
        assertEquals(ledger, tour.step(tour.stops().get(tour.size() - 1), 1), "and on from the last is the first");
        assertEquals(1, tour.number(ledger));
    }
}
