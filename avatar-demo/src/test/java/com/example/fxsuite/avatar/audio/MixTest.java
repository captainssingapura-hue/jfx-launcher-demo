package com.example.fxsuite.avatar.audio;

import com.example.fxsuite.avatar.sim.Vec3;
import com.example.fxsuite.avatar.sound.Clip;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The software mixer, as Java Sound plays it: placing, fading, pitching, looping, stopping. */
class MixTest {

    private static final int BLOCK = 512;

    /** A steady clip, every sample 1: what comes out is the gain applied. */
    private static Clip steady(final int length, final boolean loops) {
        final float[] samples = new float[length];
        Arrays.fill(samples, 1);
        return new Clip("steady", samples, loops);
    }

    private static float[] render(final Mix mix) {
        final float[] out = new float[BLOCK * 2];
        mix.render(out, BLOCK);
        return out;
    }

    @Test
    void atTheEarItIsInBothEarsAlike() {
        final Mix mix = new Mix();
        mix.play(steady(10_000, false), 1, 1, null);
        final float[] out = render(mix);
        assertEquals(out[0], out[1], 1e-6);
        assertEquals(Math.sqrt(0.5), out[0], 1e-6, "equal power, centred");
    }

    @Test
    void aSoundOnTheRightIsHeardOnTheRight() {
        final Mix mix = new Mix();
        mix.play(steady(10_000, false), 1, 1, new Vec3(200, 0, 0));
        final float[] out = render(mix);
        assertEquals(0, out[0], 1e-6, "nothing left");
        assertEquals(1, out[1], 1e-6, "all right");
        // turn to face +x, and the same sound is straight ahead
        final Mix facing = new Mix();
        facing.listen(Vec3.ZERO, 90);
        facing.play(steady(10_000, false), 1, 1, new Vec3(200, 0, 0));
        final float[] ahead = render(facing);
        assertEquals(ahead[0], ahead[1], 1e-6);
    }

    @Test
    void fartherIsQuieterAsSpaceSays() {
        final Mix mix = new Mix();
        mix.play(steady(10_000, false), 1, 1, new Vec3(0, 0, 4 * Space.NEAR));
        final float[] out = render(mix);
        assertEquals(Space.fade(4 * Space.NEAR) * Math.sqrt(0.5), out[0], 1e-6);
        assertEquals(0.25, Space.fade(4 * Space.NEAR), 1e-9);
        assertEquals(1, Space.fade(0), 1e-9);
        assertEquals(Space.fade(Space.FAR), Space.fade(10 * Space.FAR), 1e-12, "no quieter past FAR");
    }

    @Test
    void aOneShotEndsAndIsGoneAndPitchSetsItsPace() {
        final Mix normal = new Mix();
        normal.play(steady(BLOCK * 4, false), 1, 1, null);
        final Mix doubled = new Mix();
        doubled.play(steady(BLOCK * 4, false), 1, 2, null);
        int blocks = 0;
        while (normal.playing() > 0) {
            render(normal);
            blocks++;
        }
        int quick = 0;
        while (doubled.playing() > 0) {
            render(doubled);
            quick++;
        }
        assertEquals(4, blocks);
        assertEquals(2, quick, "an octave up, twice as quick");
    }

    @Test
    void aLoopGoesOnRampsToNewLevelsAndFadesWhenStopped() {
        final Mix mix = new Mix();
        final Mix.Voice loop = mix.loop(steady(300, true));
        for (int i = 0; i < 10; i++) {
            render(mix);
        }
        assertEquals(1, mix.playing(), "still going after many times round");
        assertEquals(0, render(mix)[0], 1e-9, "starts silent");
        loop.set(1, 1);
        final float[] up = render(mix);
        assertTrue(up[0] < 0.01, "ramps up, no jump: " + up[0]);
        assertEquals(Math.sqrt(0.5), up[2 * BLOCK - 2], 1e-6, "there by the end of the block");
        loop.stop();
        final float[] down = render(mix);
        assertTrue(down[0] > 0.7 && down[2 * BLOCK - 2] < 1e-6, "fades out over one block");
        assertEquals(0, mix.playing());
    }

    @Test
    void volumeScalesEverything() {
        final Mix mix = new Mix();
        mix.volume(0);
        mix.play(steady(10_000, false), 1, 1, null);
        final float[] out = render(mix);
        for (final float s : out) {
            assertEquals(0, s, 1e-9);
        }
    }

    @Test
    void sideKnowsLeftFromRightAndCountsHeight() {
        // a foot 10 to the right but 160 below the ear is nearly in the middle, as OpenAL hears it
        assertEquals(10 / Math.hypot(10, 160), Space.side(Vec3.ZERO, 0, new Vec3(10, 160, 0)), 1e-9);
        assertEquals(1, Space.side(Vec3.ZERO, 0, new Vec3(100, 0, 0)), 1e-9);
        assertEquals(-1, Space.side(Vec3.ZERO, 0, new Vec3(-100, 0, 0)), 1e-9);
        assertEquals(0, Space.side(Vec3.ZERO, 0, new Vec3(0, 0, 100)), 1e-9, "ahead");
        assertEquals(0, Space.side(Vec3.ZERO, 0, new Vec3(0, -300, 0)), 1e-9, "overhead");
        assertEquals(1, Space.side(Vec3.ZERO, 90, new Vec3(0, 0, -100)), 1e-9, "facing +x, -z is on the right");
    }

    @Test
    void aStreamIsAskedForEachRenderPlacedAsAVoiceIsAndGoneOnceStopped() {
        final Mix mix = new Mix();
        final int[] asked = {0};
        // a source as steady as the clip: every sample 1, and how much it was asked for counted
        final Speakers.Stream stream = mix.stream((into, count) -> {
            Arrays.fill(into, 0, count, 1);
            asked[0] += count;
        });
        stream.set(1);
        render(mix);
        final float[] out = render(mix);
        assertEquals(2 * BLOCK, asked[0], "as much as each render needs, no more");
        assertEquals(Math.sqrt(0.5), out[0], 1e-6, "at the ear, centred, as a voice is");
        assertEquals(out[0], out[1], 1e-6);
        stream.place(new Vec3(400, 0, 0));
        render(mix);
        final float[] right = render(mix);
        assertTrue(right[1] > right[0], "on the right, heard on the right");
        stream.stop();
        render(mix);
        assertEquals(0, mix.playing(), "stopped, gone after fading out");
    }
}
