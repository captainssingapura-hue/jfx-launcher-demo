package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.sim.Arms;
import com.example.fxsuite.avatar.sim.Avatar;
import com.example.fxsuite.avatar.sim.Body;
import com.example.fxsuite.avatar.sim.Controls;
import com.example.fxsuite.avatar.sim.Obstacles;
import com.example.fxsuite.avatar.sim.Vec3;
import com.example.fxsuite.avatar.sim.Weapon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** How the weapons are held: the rifles from the hip, and the glaive's strokes. */
class HoldTest {

    private static Avatar holding(final Body body, final Weapon weapon) {
        return new Avatar(body, true).drawn(weapon);
    }

    /**
     * Which way a forearm points, in degrees forward from hanging straight down: the body leans
     * forward by a negative turn, the upper arm and the elbow forward by positive ones.
     */
    private static double forearm(final Pose pose, final double upperArm, final double elbow) {
        return upperArm + elbow - pose.lean();
    }

    @Test
    void theRiflesAreHeldFromTheHipLevelWhateverTheLeanOrTheLook() {
        final Body standing = Body.standing(Vec3.ZERO, 0);
        final Body boosting = new Body(new Vec3(0, -500, 0), 0, 0, Body.BOOST, 0, 0, 0, true, false, 0, 0);
        for (final Body body : new Body[] {standing, boosting}) {
            final Pose pose = Gait.of(holding(body, Weapon.RIFLES), 0, 0);
            assertTrue(pose.leftArm() < 15 && pose.rightArm() < 15, "the upper arms down along the body");
            assertTrue(pose.leftSpread() < 0 && pose.rightSpread() < 0, "pressed in to clamp the stocks");
            assertEquals(90, forearm(pose, pose.leftArm(), pose.leftElbow()), 1e-9, "the forearms level, leaning "
                    + pose.lean());
            assertEquals(90, forearm(pose, pose.rightArm(), pose.rightElbow()), 1e-9);
        }
        // and still level when the player only looks round: they shoot where the body faces
        final Pose looking = Gait.of(holding(standing, Weapon.RIFLES), 40, 25);
        assertEquals(90, forearm(looking, looking.leftArm(), looking.leftElbow()), 1e-9, "not aimed by the look");
        // aimed up, the forearms tilt up as far as the rounds do
        final Avatar aimed = holding(standing, Weapon.RIFLES)
                .step(new Controls(0, 0, 0, 0, false, false, 25), 1.0 / 60, Obstacles.NONE).avatar();
        final Pose up = Gait.of(aimed, 0, 0);
        assertEquals(90 + 25, forearm(up, up.leftArm(), up.leftElbow()), 1e-9, "tilted with the aim");
    }

    @Test
    void eachRoundKicksItsOwnForearmAndItSettles() {
        Avatar avatar = holding(Body.standing(Vec3.ZERO, 0), Weapon.RIFLES);
        avatar = avatar.step(new Controls(0, 0, 0, 0, false, true), 1.0 / 60, Obstacles.NONE).avatar();
        final Pose fired = Gait.of(avatar, 0, 0);
        assertTrue(fired.leftElbow() > fired.rightElbow() + 5, "the left fired first, and kicked up");
        for (int i = 0; i < 30; i++) {
            avatar = avatar.step(Controls.NONE, 1.0 / 60, Obstacles.NONE).avatar();
        }
        final Pose settled = Gait.of(avatar, 0, 0);
        assertEquals(settled.leftElbow(), settled.rightElbow(), 0.01, "and settles back");
    }

    @Test
    void theCleaveGoesOverTheTopToComeDownInFront() {
        // the upper arm only ever turns one way through the cleave: back, up, over, down in front
        double was = Gait.keyed(Gait.CLEAVE, 0)[1];
        for (double p = 0.01; p <= 1.0001; p += 0.01) {
            final double now = Gait.keyed(Gait.CLEAVE, p)[1];
            assertTrue(now <= was + 1e-9, "turning on round at " + p);
            was = now;
        }
        final double strike = Gait.keyed(Gait.CLEAVE, Arms.STRIKE)[1];
        assertTrue(strike < -180 && strike > -270, "at the strike, up in front: " + strike);
        assertEquals(Gait.keyed(Gait.CLEAVE, 0)[1] - 360, Gait.keyed(Gait.CLEAVE, 1)[1], 1e-9,
                "and once round, back at rest");
    }

    @Test
    void theGlaiveRestsUprightAndTheWristTurnsItForAStroke() {
        final Pose rest = Gait.of(holding(Body.standing(Vec3.ZERO, 0), Weapon.GLAIVE), 0, 0);
        assertEquals(180, rest.grip(), 1e-9, "upright, blade high");
        assertEquals(0, Gait.keyed(Gait.SWEEP, Arms.STRIKE)[3], 1e-9, "turned out along the arm to strike");
        assertEquals(0, Gait.keyed(Gait.CLEAVE, Arms.STRIKE)[3], 1e-9);
    }
}
