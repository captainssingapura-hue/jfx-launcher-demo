package com.example.fxsuite.avatar;

import javafx.geometry.Point3D;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The jet-pack: only wearing it can the avatar fly. */
class AvatarTest {

    private static final double FRAME = 1.0 / 60;

    private static Avatar standing(final boolean pack) {
        return new Avatar(Body.standing(Point3D.ZERO, 0), pack);
    }

    private static Avatar run(Avatar avatar, final Controls controls, final double seconds) {
        for (double t = 0; t < seconds - 1e-9; t += FRAME) {
            avatar = avatar.step(controls, FRAME, Obstacles.NONE);
        }
        return avatar;
    }

    @Test
    void withoutThePackItCannotTakeOff() {
        final Avatar plain = standing(false);
        assertSame(plain, plain.toggledFlight());
        final Avatar later = run(plain.toggledFlight(), new Controls(0, 0, 0, 1, false), 1);
        assertFalse(later.body().flying());
        assertEquals(0, later.body().height(), 1e-9);
    }

    @Test
    void wearingItItTakesOffAndFlies() {
        final Avatar up = run(standing(true).toggledFlight(), new Controls(0, 0, 0, 1, false), 1);
        assertTrue(up.body().flying());
        assertTrue(up.body().height() > 300, "climbing: " + up.body().height());
    }

    @Test
    void puttingItOnAndTakingItOffOnTheGroundChangesNothingElse() {
        final Avatar on = standing(false).toggledPack();
        assertTrue(on.wearingPack());
        assertEquals(standing(false).body(), on.body());
        assertFalse(on.toggledPack().wearingPack());
    }

    @Test
    void takingItOffInFlightFalls() {
        final Avatar flying = run(standing(true).toggledFlight(), new Controls(0, 0, 0, 1, false), 1);
        final Avatar dropped = flying.toggledPack();
        assertFalse(dropped.wearingPack());
        assertFalse(dropped.body().flying());
        final Avatar landed = run(dropped, Controls.NONE, 3);
        assertTrue(landed.body().grounded());
    }

    @Test
    void landingNeedsNoPackButFlyingAgainDoes() {
        // a flight can always be ended, pack or not; starting one needs it
        final Avatar flying = run(standing(true).toggledFlight(), Controls.NONE, 1);
        final Avatar falling = flying.toggledFlight();
        assertFalse(falling.body().flying());
        assertTrue(falling.wearingPack());
    }

    @Test
    void aDroneBrakesBrisklyWhenLetGo() {
        final Avatar cruising = run(standing(true).toggledFlight(), new Controls(1, 0, 0, 0, false), 2);
        final Avatar coasting = run(cruising, Controls.NONE, 1);
        assertTrue(coasting.body().flying());
        assertTrue(coasting.body().speed() < Body.FLY * 0.05, "all but stopped: " + coasting.body().speed());
    }
}
