package com.example.fxsuite.viewer;

import javafx.geometry.Point3D;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlyCameraTest {

    private static void near(Point3D expected, Point3D actual) {
        assertTrue(expected.distance(actual) < 1e-6, "expected " + expected + " but was " + actual);
    }

    @Test
    void switchingBetweenOrbitAndFlyKeepsTheViewExactly() {
        Orbit orbit = new Orbit(new Point3D(100, -50, 300), 1500, 28, -22);
        FlyCamera fly = FlyCamera.from(orbit);
        near(orbit.position(), fly.position());
        near(orbit.forward(), fly.forward());

        Orbit back = fly.toOrbit(1500);
        near(orbit.pivot(), back.pivot());
        assertEquals(orbit.yaw(), back.yaw());
        assertEquals(orbit.pitch(), back.pitch());
        // any distance gives the same view: only the point turned around differs
        near(fly.position(), fly.toOrbit(400).position());
    }

    @Test
    void movingAheadFollowsTheViewUpOrDownIncluded() {
        FlyCamera lookingDown = new FlyCamera(Point3D.ZERO, 0, -45);
        FlyCamera moved = lookingDown.moved(100, 0, 0);
        near(lookingDown.forward().multiply(100), moved.position());
        assertTrue(moved.position().getY() > 0, "looking down, ahead goes down (y is down)");
    }

    @Test
    void risingIsStraightUpWhateverTheTilt() {
        FlyCamera tilted = new FlyCamera(Point3D.ZERO, 37, -60);
        near(new Point3D(0, -50, 0), tilted.moved(0, 0, 50).position());
    }

    @Test
    void sidewaysIsLevel() {
        FlyCamera tilted = new FlyCamera(Point3D.ZERO, 37, -60);
        assertEquals(0, tilted.moved(0, 80, 0).position().getY(), 1e-9);
        assertEquals(80, tilted.moved(0, 80, 0).position().magnitude(), 1e-9);
    }

    @Test
    void aDroneKeepsItsHeightWhateverItLooksAt() {
        for (double pitch : new double[] {-80, -30, 0, 45}) {
            FlyCamera drone = new FlyCamera(new Point3D(10, -400, 20), 37, pitch);
            FlyCamera moved = drone.cruised(250, -60, 0);
            assertEquals(-400, moved.position().getY(), 1e-9, "level at pitch " + pitch);
            // ahead is where it faces, along the ground, and it does not turn
            Point3D level = new Point3D(drone.forward().getX(), 0, drone.forward().getZ()).normalize();
            near(drone.position().add(level.multiply(250)).add(drone.right().multiply(-60)), moved.position());
            assertEquals(drone.yaw(), moved.yaw());
            assertEquals(drone.pitch(), moved.pitch());
        }
    }

    @Test
    void aDroneChangesHeightOnlyWhenToldAndThenStraightUp() {
        FlyCamera drone = new FlyCamera(new Point3D(10, -400, 20), 37, -60);
        FlyCamera climbed = drone.cruised(0, 0, 120);
        near(drone.position().add(0, -120, 0), climbed.position());
        near(drone.forward(), climbed.forward());
    }

    @Test
    void lookingTurnsInPlaceAndStopsShortOfStraightUpOrDown() {
        FlyCamera fly = new FlyCamera(new Point3D(5, 6, 7), 10, 0);
        FlyCamera turned = fly.looked(30, 200);
        near(fly.position(), turned.position());
        assertEquals(40, turned.yaw());
        assertEquals(Orbit.MAX_PITCH, turned.pitch());
    }

    @Test
    void flyingStaysInsideItsBox() {
        FlyCamera far = new FlyCamera(new Point3D(9000, -9000, 50), 0, 0);
        near(new Point3D(1000, -1000, 50),
                far.within(new Point3D(-1000, -1000, -1000), new Point3D(1000, 1000, 1000)).position());
    }

    @Test
    void speedEasesTowardTheKeysTheSameWayAtAnyFrameRate() {
        Velocity target = new Velocity(400, -200, 100);
        Velocity once = Velocity.STILL.toward(target, 0.1);
        Velocity inSteps = Velocity.STILL;
        for (int i = 0; i < 10; i++) {
            inSteps = inSteps.toward(target, 0.01);
        }
        assertEquals(once.ahead(), inSteps.ahead(), 1e-9);
        assertEquals(once.rightward(), inSteps.rightward(), 1e-9);
        // partway after a tenth of a second, nearly there after half a second
        assertTrue(once.ahead() > 200 && once.ahead() < 400, "" + once.ahead());
        assertTrue(Velocity.STILL.toward(target, 0.5).ahead() > 396);
        // and it settles back to still when the keys are let go
        assertTrue(target.toward(Velocity.STILL, 1).isStill());
    }
}
