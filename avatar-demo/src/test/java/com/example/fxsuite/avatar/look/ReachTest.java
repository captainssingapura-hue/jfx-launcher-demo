package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.sim.Arms;
import com.example.fxsuite.avatar.sim.Avatar;
import com.example.fxsuite.avatar.sim.Body;
import com.example.fxsuite.avatar.sim.Controls;
import com.example.fxsuite.avatar.sim.Obstacles;
import com.example.fxsuite.avatar.sim.Vec3;
import com.example.fxsuite.avatar.sim.Weapon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Where the arms reach, and the Gatling gun held in both hands. */
class ReachTest {

    private static double distance(final double[] a, final double[] b) {
        return Math.sqrt(Math.pow(a[0] - b[0], 2) + Math.pow(a[1] - b[1], 2) + Math.pow(a[2] - b[2], 2));
    }

    @Test
    void anArmReachesAsTheRigTurnsItsJoints() {
        // hanging, forward, out to its side, and bent forward at the elbow
        assertArrayEquals(new double[] {-41, -144 + 62, 0}, Reach.hand(true, 0, 0, 0), 1e-9);
        assertArrayEquals(new double[] {-41, -144, 62}, Reach.hand(true, 90, 0, 0), 1e-9);
        assertArrayEquals(new double[] {-41 - 62, -144, 0}, Reach.hand(true, 0, 90, 0), 1e-9, "the left, out to the left");
        assertArrayEquals(new double[] {41 + 62, -144, 0}, Reach.hand(false, 0, 90, 0), 1e-9, "the right, to the right");
        assertArrayEquals(new double[] {41, -144 + 30, 32}, Reach.hand(false, 0, 0, 90), 1e-9, "forearm folded forward");
        // twisted a quarter turn about the upper arm, the folded forearm points across instead
        assertArrayEquals(new double[] {-41 + 32, -144 + 30, 0}, Reach.hand(true, 0, 0, 90, 90), 1e-9,
                "the left forearm, twisted, across the body");
    }

    @Test
    void theSolverBringsTheHandWhereverItCanReachAndAsNearAsItCanElsewhere() {
        final double[] target = Reach.hand(true, 30, -20, 70);
        final double[] solved = Reach.solve(true, target, new double[] {0, 0, 0, 0});
        assertTrue(distance(Reach.hand(true, solved[0], solved[1], solved[2], solved[3]), target) < 0.5);
        // far out of reach: the arm points straight at it, as far as it goes
        final double[] far = {-41, -144, 500};
        final double[] stretched = Reach.solve(true, far, new double[] {0, 0, 0, 0});
        assertEquals(500 - 62, distance(Reach.hand(true, stretched[0], stretched[1], stretched[2], stretched[3]), far), 0.5);
    }

    /** The pose of an avatar holding the Gatling gun. */
    private static Pose holding(final Body body, final double aim) {
        final Avatar avatar = new Avatar(body, true).drawn(Weapon.GATLING).delivered()
                .step(new Controls(0, 0, 0, 0, false, false, aim), 1.0 / 60, Obstacles.NONE);
        return Gait.of(avatar, 0, 0);
    }

    @Test
    void theLeftHandStaysOnTheLeverWhereverTheGunPoints() {
        final Body standing = Body.standing(Vec3.ZERO, 0);
        final Body boosting = new Body(new Vec3(0, -500, 0), 0, 0, Body.BOOST, 0, 0, 0, true, false, 0, 0);
        for (final Body body : new Body[] {standing, boosting}) {
            // to the limit of its aim either way, and past it, where the aim stops
            for (final double aim : new double[] {0, 30, -30, -45}) {
                final Pose pose = holding(body, aim);
                final double[] lever = Reach.point(false, pose.rightArm(), pose.rightSpread(), pose.rightElbow(),
                        Arsenal.LEVER);
                final double[] hand = Reach.hand(true, pose.leftArm(), pose.leftSpread(), pose.leftElbow(),
                        pose.leftTwist());
                assertTrue(distance(hand, lever) < 2, "on the lever, aimed " + aim + ": " + distance(hand, lever));
            }
        }
    }

    @Test
    void theSimulationsMuzzleIsWhereTheModelsIs() {
        final Pose pose = holding(Body.standing(Vec3.ZERO, 0), 0);
        final double[] inTorso = Reach.point(false, pose.rightArm(), pose.rightSpread(), pose.rightElbow(),
                new double[] {-Arsenal.GUN_IN, Arsenal.HAND + 86, 4});
        // into the world, as the rig sets the body: leaned (a negative turn about x) and banked about
        // its middle, then raised; for a body at the origin facing +z the axes are the world's
        final double[] fromMiddle = {inTorso[0], inTorso[1] + BoxRig.MIDDLE, inTorso[2]};
        final double[] turned = Reach.rotZ(Reach.rotX(fromMiddle, -pose.lean()), pose.bank());
        final double[] world = {turned[0], turned[1] - BoxRig.MIDDLE - pose.lift() - BoxRig.SOLE, turned[2]};
        final Vec3 sim = Arms.gatlingMuzzle(Body.standing(Vec3.ZERO, 0));
        assertEquals(sim.x(), world[0], 2);
        assertEquals(sim.y(), world[1], 2);
        assertEquals(sim.z(), world[2], 2);
    }
}
