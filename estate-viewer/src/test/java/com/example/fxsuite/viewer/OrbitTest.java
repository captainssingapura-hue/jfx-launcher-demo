package com.example.fxsuite.viewer;

import javafx.geometry.Point3D;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrbitTest {

    private static final double FOV = 35;

    private static void near(Point3D expected, Point3D actual) {
        assertTrue(expected.distance(actual) < 1e-6, "expected " + expected + " but was " + actual);
    }

    /** What the camera rig does: Rotate(yaw, Y) then Rotate(pitch, X). */
    private static Transform rig(Orbit orbit) {
        return new Rotate(orbit.yaw(), Rotate.Y_AXIS).createConcatenation(new Rotate(orbit.pitch(), Rotate.X_AXIS));
    }

    @Test
    void itsDirectionsAreTheOnesTheCameraRigProduces() {
        for (double yaw : new double[] {0, 28, -90, 208}) {
            for (double pitch : new double[] {0, -22, 45, -89}) {
                Orbit orbit = new Orbit(Point3D.ZERO, 1000, yaw, pitch);
                Transform rig = rig(orbit);
                near(rig.transform(new Point3D(0, 0, 1)), orbit.forward());
                near(rig.transform(new Point3D(1, 0, 0)), orbit.right());
                near(rig.transform(new Point3D(0, 1, 0)), orbit.down());
            }
        }
    }

    @Test
    void theCameraStandsBackFromThePivotAlongItsLine() {
        Orbit orbit = new Orbit(new Point3D(10, 20, 30), 500, 28, -22);
        near(orbit.pivot(), orbit.position().add(orbit.forward().multiply(500)));
        // looking down: the camera is above the pivot (y is down)
        assertTrue(orbit.position().getY() < orbit.pivot().getY());
    }

    @Test
    void zoomingTowardAPointKeepsItUnderTheSameSpotOnTheScreen() {
        Orbit orbit = new Orbit(new Point3D(0, 0, 0), 2000, 28, -22);
        Point3D under = orbit.onPivotPlane(900, 200, 1060, 880, FOV);
        Point3D before = under.subtract(orbit.position()).normalize();

        Orbit closer = orbit.zoomedToward(under, 0.5);
        near(before, under.subtract(closer.position()).normalize());
        assertEquals(1000, closer.distance(), 1e-9);
        // and the screen point still finds it
        near(under, closer.onPivotPlane(900, 200, 1060, 880, FOV));
    }

    @Test
    void theRayThroughTheMiddleOfTheScreenIsTheViewDirection() {
        Orbit orbit = new Orbit(Point3D.ZERO, 1000, 40, -30);
        near(orbit.forward(), orbit.ray(530, 440, 1060, 880, FOV));
        near(orbit.pivot(), orbit.onPivotPlane(530, 440, 1060, 880, FOV));
    }

    @Test
    void panningMovesThePivotAcrossTheView() {
        Orbit orbit = new Orbit(Point3D.ZERO, 1000, 28, -22);
        Orbit moved = orbit.panned(100, 50);
        near(orbit.right().multiply(100).add(orbit.down().multiply(50)), moved.pivot());
        assertEquals(orbit.distance(), moved.distance());
    }

    @Test
    void glidingTurnsTheShortWayRoundAndArrives() {
        Orbit from = new Orbit(Point3D.ZERO, 1000, 170, 0);
        Orbit to = new Orbit(new Point3D(100, 0, 0), 4000, -170, -40);
        assertEquals(180, from.towards(to, 0.5).yaw(), 1e-9, "through 180, not back through 0");
        Orbit end = from.towards(to, 1);
        near(to.pivot(), end.pivot());
        assertEquals(4000, end.distance(), 1e-6);
        assertEquals(190, end.yaw(), 1e-9);
        assertEquals(-40, end.pitch(), 1e-9);
        // distance changes evenly in ratio, so the glide does not lurch at either end
        assertEquals(2000, from.towards(to, 0.5).distance(), 1e-6);
    }

    @Test
    void pitchAndDistanceStayInRange() {
        Orbit orbit = new Orbit(Point3D.ZERO, 5, 0, -120);
        assertEquals(-Orbit.MAX_PITCH, orbit.pitch());
        assertEquals(Orbit.MIN_DISTANCE, orbit.distance());
    }

    @Test
    void framingCentresOnTheBoxAndStandsBackJustFarEnoughForAllOfIt() {
        List<Point3D> points = List.of(new Point3D(-500, 0, 0), new Point3D(500, 100, 800),
                new Point3D(-300, -200, 400), new Point3D(200, 300, 100));
        double aspect = 1060 / 880.0;
        Orbit framed = new Framing().frame(points, 0, FOV, aspect, 28, -22);
        near(new Point3D(0, 50, 400), framed.pivot());

        // every point is on screen, and at least one touches an edge
        double tanV = Math.tan(Math.toRadians(FOV) / 2);
        double tanH = tanV * aspect;
        double tightest = 0;
        for (Point3D p : points) {
            Point3D from = p.subtract(framed.position());
            double depth = from.dotProduct(framed.forward());
            double x = Math.abs(from.dotProduct(framed.right())) / (depth * tanH);
            double y = Math.abs(from.dotProduct(framed.down())) / (depth * tanV);
            assertTrue(x <= 1 + 1e-9 && y <= 1 + 1e-9, p + " is off screen");
            tightest = Math.max(tightest, Math.max(x, y));
        }
        assertEquals(1, tightest, 1e-9);

        Orbit roomier = new Framing().frame(points, 200, FOV, aspect, 28, -22);
        assertTrue(roomier.distance() > framed.distance());
    }
}
