package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A jump keeps to its own path: turning in mid-air turns the body, not where it goes. */
class JumpTest {

    private static final double FRAME = 1.0 / 60;

    /** Running flat out along +z (a heading of 0), then jumping. */
    private static Body runningJump() {
        Body body = Body.standing(Vec3.ZERO, 0);
        for (int i = 0; i < 60; i++) {
            body = body.step(new Controls(1, 0, 0, 0, true), FRAME);
        }
        return body.jumped();
    }

    /** Steps with the given controls until it lands. */
    private static Body untilLanded(Body body, final Controls controls) {
        for (int i = 0; i < 600 && !body.grounded(); i++) {
            body = body.step(controls, FRAME);
        }
        assertTrue(body.grounded(), "landed");
        return body;
    }

    @Test
    void turningInMidJumpLeavesThePathAlone() {
        final Body straight = untilLanded(runningJump(), new Controls(1, 0, 0, 0, true));
        final Body turning = untilLanded(runningJump(), new Controls(1, 0, 2, 0, true));
        assertTrue(turning.yaw() > 60, "it did turn to face elsewhere: " + turning.yaw());
        assertEquals(straight.position().x(), turning.position().x(), 1e-9);
        assertEquals(straight.position().z(), turning.position().z(), 1e-9);
    }

    @Test
    void lettingGoInMidJumpKeepsTheMomentum() {
        final Body jump = runningJump();
        final double before = jump.speed();
        Body body = jump;
        for (int i = 0; i < 20; i++) {
            body = body.step(Controls.NONE, FRAME);
        }
        assertTrue(body.inJump());
        assertEquals(before, body.speed(), 1e-9, "no braking in the air");
    }

    @Test
    void theKeysNudgeAlongTheTakeOffHeadingNotTheFacing() {
        // a standing jump, facing +z, then turned to face +x at once and pushed ahead
        Body body = Body.standing(Vec3.ZERO, 0).jumped().step(new Controls(0, 0, 90, 0, false), FRAME);
        assertEquals(90, body.yaw(), 1e-9);
        body = untilLanded(body, new Controls(1, 0, 0, 0, false));
        assertTrue(body.position().z() > 20, "nudged along +z, where it took off facing: " + body.position());
        assertEquals(0, body.position().x(), 1e-9);
    }

    @Test
    void onLandingWalkingFollowsTheFacingAgain() {
        final Body landed = untilLanded(runningJump(), new Controls(1, 0, 2, 0, true));
        assertEquals(landed.yaw(), landed.travel(), 1e-9);
        final Vec3 at = landed.position();
        Body body = landed;
        for (int i = 0; i < 60; i++) {
            body = body.step(new Controls(1, 0, 0, 0, false), FRAME);
        }
        final Vec3 went = body.position().subtract(at);
        assertTrue(went.dotProduct(landed.heading()) > 250, "walks the way it now faces: " + went);
    }

    @Test
    void walkingOffAnEdgeIsAJumpToo() {
        final Obstacles crate = new Obstacles(List.of(Block.standing(0, 0, 200, 200, 100, 90)));
        Body body = Body.standing(new Vec3(0, -100, 0), 90);
        while (!body.inJump()) {
            body = body.step(new Controls(1, 0, 0, 0, false), FRAME, crate);
        }
        final double z = body.position().z();
        body = body.step(new Controls(0, 0, 90, 0, false), FRAME, crate);
        for (int i = 0; i < 600 && !body.grounded(); i++) {
            body = body.step(Controls.NONE, FRAME, crate);
        }
        assertEquals(0, body.height(), 1e-9);
        assertEquals(z, body.position().z(), 1e-9, "fell along +x, as it walked off, though turned to +z");
    }

    @Test
    void flyingStillSteersWithTheFacing() {
        Body body = Body.standing(Vec3.ZERO, 0).toggledFlight();
        for (int i = 0; i < 60; i++) {
            body = body.step(new Controls(1, 0, 0, 0, false), FRAME);
        }
        for (int i = 0; i < 60; i++) {
            body = body.step(new Controls(1, 0, 1.5, 0, false), FRAME);
        }
        assertFalse(body.inJump());
        assertEquals(body.yaw(), body.travel(), 1e-9);
        assertTrue(body.speed() > Body.FLY * 0.8, "still flying ahead, the way it now faces: " + body.speed());
    }
}
