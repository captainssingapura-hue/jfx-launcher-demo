package com.example.fxsuite.avatar.audio;

import com.example.fxsuite.avatar.sim.Vec3;
import com.example.fxsuite.avatar.sound.Clip;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** How far sounds carry. */
class SpaceTest {

    @Test
    void aSoundIsFullWithinItsReachAndFadesBeyond() {
        assertEquals(1, Space.fade(200), 1e-12);
        assertEquals(0.1, Space.fade(3000), 1e-12, "a footstep 30 m off: a tenth");
        // a blast that carries 60 m is in full at 30 m, and at half at 120 m
        assertEquals(1, Space.fade(3000, 6000), 1e-12);
        assertEquals(0.5, Space.fade(12_000, 6000), 1e-12);
        assertEquals(Space.fade(5000), Space.fade(5000, Space.NEAR), 1e-12);
    }

    @Test
    void theMixerFadesEachVoiceByItsOwnReach() {
        final Clip tone = new Clip("tone", new float[] {0.5f, 0.5f, 0.5f, 0.5f}, false);
        final Vec3 far = new Vec3(0, 0, 3000);
        assertTrue(level(tone, far, 6000) > 9 * level(tone, far, Space.NEAR), "the far-reaching one, ten times louder");
    }

    private static double level(final Clip clip, final Vec3 at, final double near) {
        final Mix mix = new Mix();
        mix.listen(Vec3.ZERO, 0);
        mix.volume(1);
        mix.play(clip, 1, 1, at, near);
        final float[] out = new float[8];
        mix.render(out, 4);
        return Math.abs(out[2]) + Math.abs(out[3]);
    }
}
