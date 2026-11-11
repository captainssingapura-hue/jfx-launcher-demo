package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.sim.Avatar;
import com.example.fxsuite.avatar.sim.Body;
import com.example.fxsuite.avatar.sim.Vec3;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The swing wings' sweep, scheduled by speed as a variable-sweep fighter's is. */
class JetPackTest {

    /** Flying forward at {@code speed}, wearing the pack. */
    private static Avatar flying(final double speed) {
        return new Avatar(new Body(new Vec3(0, -500, 0), 0, 0, speed, 0, 0, 0, true, false, 0, 0), true);
    }

    @Test
    void stowedUnlessFlying() {
        assertEquals(JetPack.STOWED, JetPack.sweepFor(new Avatar(Body.standing(Vec3.ZERO, 0), true)));
        assertEquals(JetPack.STOWED, JetPack.sweepFor(new Avatar(flying(0).body(), false)), "no pack, no flight");
    }

    @Test
    void spreadToHoverOnTheJetsAndSweptBackAsItSpeedsUp() {
        assertEquals(JetPack.SPREAD, JetPack.sweepFor(flying(0)), "hovering upright, held up by thrust");
        assertEquals(JetPack.SPREAD, JetPack.sweepFor(flying(JetPack.SLOW)), "still spread, flying slowly");
        assertEquals(JetPack.SPREAD, JetPack.sweepFor(flying(-Body.FLY)), "and backing off");
        double was = JetPack.SPREAD;
        for (double speed = JetPack.SLOW + 100; speed <= Body.BOOST; speed += 100) {
            final double now = JetPack.sweepFor(flying(speed));
            assertTrue(now > was, "sweeps further back at " + speed);
            was = now;
        }
        assertEquals(JetPack.SWEPT, JetPack.sweepFor(flying(Body.BOOST)), 1e-9, "fully swept at full boost");
        assertTrue(JetPack.SWEPT < JetPack.STOWED, "never as far back in flight as stowed");
    }
}
