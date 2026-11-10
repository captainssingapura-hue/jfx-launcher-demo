package com.example.fxsuite.avatar;

import javafx.geometry.Point3D;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GaitTest {

    private static Body walking(final double speed, final double phase) {
        return onFoot(speed, 0, phase);
    }

    private static Body onFoot(final double speed, final double drift, final double phase) {
        return new Body(Point3D.ZERO, 0, speed, drift, 0, 0, false, true, phase, 0);
    }

    private static Body flying(final double speed, final double drift, final double turnRate) {
        return new Body(new Point3D(0, -800, 0), 0, speed, drift, turnRate, 0, true, false, 0, 0);
    }

    private static double widestLegSwing(final double speed) {
        double widest = 0;
        for (int i = 0; i < 64; i++) {
            widest = Math.max(widest, Math.abs(Gait.of(walking(speed, i * Math.PI / 32), 0, 0).leftLeg()));
        }
        return widest;
    }

    @Test
    void standingStillHoldsTheLimbsStill() {
        final Pose pose = Gait.of(walking(0, 1.2), 0, 0);
        assertEquals(0, pose.leftLeg(), 1e-9);
        assertEquals(0, pose.leftArm(), 1e-9);
        assertEquals(0, pose.legSpread(), 1e-9);
        assertEquals(0, pose.thrust(), 1e-9);
    }

    @Test
    void theBodyNeverSinksBelowStandingHeight() {
        // a negative lift would push the feet into the ground, where they flicker against it
        for (int i = 0; i < 400; i++) {
            final double t = i * 0.05;
            final Body standing = new Body(Point3D.ZERO, 0, 0, 0, 0, 0, false, true, 0, t);
            final Body walking = new Body(Point3D.ZERO, 0, Body.WALK, 0, 0, 0, false, true, i * 0.3, t);
            assertTrue(Gait.of(standing, 0, 0).lift() >= 0, "standing at " + t);
            assertTrue(Gait.of(walking, 0, 0).lift() >= 0, "walking at " + t);
        }
    }

    @Test
    void legsSwingOppositeEachOtherAndArmsOppositeTheirLeg() {
        final Pose pose = Gait.of(walking(Body.WALK, Math.PI / 2), 0, 0);
        assertTrue(pose.leftLeg() > 10, "left leg forward: " + pose.leftLeg());
        assertEquals(-pose.leftLeg(), pose.rightLeg(), 1e-9);
        assertTrue(pose.leftArm() < 0, "left arm back");
        assertTrue(pose.rightArm() > 0, "right arm forward");
        assertEquals(0, pose.legSpread(), 1e-9, "walking ahead, the legs stay under the hips");
    }

    @Test
    void runningSwingsWiderThanWalking() {
        assertTrue(widestLegSwing(Body.RUN) > widestLegSwing(Body.WALK) + 10);
    }

    @Test
    void sideSteppingOpensTheLegsInsteadOfSwingingThem() {
        final Pose pose = Gait.of(onFoot(0, Body.WALK, Math.PI / 2), 0, 0);
        assertEquals(0, pose.leftLeg(), 1e-9);
        assertTrue(pose.legSpread() > 5, "feet apart: " + pose.legSpread());
        assertTrue(pose.bank() > 0, "leans into a step to the right");
        assertTrue(Gait.of(onFoot(0, -Body.WALK, Math.PI / 2), 0, 0).bank() < 0);
    }

    @Test
    void flyingFasterLeansFurther() {
        assertTrue(Gait.of(flying(Body.BOOST, 0, 0), 0, 0).lean()
                > Gait.of(flying(Body.FLY, 0, 0), 0, 0).lean() + 20);
    }

    @Test
    void banksIntoTheTurnAndIntoSidewaysDrift() {
        assertTrue(Gait.of(flying(Body.FLY, 0, Body.TURN), 0, 0).bank() > 20, "right turn, right side down");
        assertTrue(Gait.of(flying(Body.FLY, 0, -Body.TURN), 0, 0).bank() < -20);
        assertTrue(Gait.of(flying(0, Body.FLY, 0), 0, 0).bank() > 15, "drifting right, right side down");
    }

    @Test
    void theJetpackFiresOnlyWhileFlying() {
        assertTrue(Gait.of(flying(0, 0, 0), 0, 0).thrust() > 0);
        assertTrue(Gait.of(flying(Body.BOOST, 0, 0), 0, 0).thrust() > Gait.of(flying(0, 0, 0), 0, 0).thrust());
    }

    @Test
    void theHeadLooksWhereThePlayerLooksWithinItsReach() {
        assertEquals(30, Gait.of(walking(0, 0), 30, 0).headYaw(), 1e-9);
        assertEquals(Gait.HEAD_TURN, Gait.of(walking(0, 0), 150, 0).headYaw(), 1e-9);
        // across ±180: the body faces 170, the player looks along -170, 20° to the right
        final Body facing = new Body(Point3D.ZERO, 170, 0, 0, 0, 0, false, true, 0, 0);
        assertEquals(20, Gait.of(facing, -170, 0).headYaw(), 1e-9);
    }

    @Test
    void theHeadHoldsUpAgainstAFlyingLean() {
        final Pose boosting = Gait.of(flying(Body.BOOST, 0, 0), 0, 0);
        assertTrue(boosting.headPitch() > 20, "looks ahead, not at the ground: " + boosting.headPitch());
    }
}
