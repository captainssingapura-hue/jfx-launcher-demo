package com.example.fxsuite.avatar.sound;

import com.example.fxsuite.avatar.sim.Actuator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How a hydraulic drive sounds ({@link HydraulicVoice}): silent at rest; its pump and its fluid
 * heard as they run; each thing that happens heard; within full scale; the same for the same.
 */
class HydraulicVoiceTest {

    private static final int BLOCK = 512;

    /** A door opened and shut, its voice made as it goes. */
    private static float[] cycle(final long seed) {
        final float[] out = new float[9 * Clip.RATE];
        final HydraulicVoice voice = new HydraulicVoice(seed);
        Actuator a = Actuator.shut(Actuator.Drive.HYDRAULIC, 0, 100, 40).asked(true);
        for (int at = 0; at + BLOCK <= out.length; at += BLOCK) {
            if (at == 5 * Clip.RATE / BLOCK * BLOCK) {
                a = a.asked(false);
            }
            final Actuator.Stepped stepped = a.step((double) BLOCK / Clip.RATE);
            a = stepped.actuator();
            stepped.events().forEach(voice::hit);
            voice.set(a.spool(), a.flow(), a.cushion());
            voice.render(out, at, BLOCK);
        }
        return out;
    }

    private static double rms(final float[] samples, final int from, final int to) {
        double sum = 0;
        for (int i = from; i < to; i++) {
            sum += samples[i] * samples[i];
        }
        return Math.sqrt(sum / (to - from));
    }

    @Test
    void silentAtRest() {
        final HydraulicVoice voice = new HydraulicVoice(1);
        final float[] out = new float[Clip.RATE];
        voice.render(out, 0, out.length);
        assertEquals(0, rms(out, 0, out.length), 1e-12);
        assertTrue(!voice.sounding());
    }

    @Test
    void aWholeCycleIsWithinFullScaleAndTheSameForTheSame() {
        final float[] once = cycle(3);
        for (final float s : once) {
            assertTrue(Math.abs(s) < 1, "within full scale");
        }
        assertArrayEquals(once, cycle(3), "the same for the same");
        assertTrue(rms(once, 0, once.length) > 0.01, "heard");
    }

    @Test
    void eachThingThatHappensIsHeard() {
        for (final Actuator.Event event : Actuator.Event.values()) {
            final HydraulicVoice voice = new HydraulicVoice(5);
            voice.hit(event);
            final float[] out = new float[Clip.RATE / 10];
            voice.render(out, 0, out.length);
            assertTrue(rms(out, 0, out.length) > 0.005, event + " heard");
        }
    }

    @Test
    void itsFluidFlowingIsLouderThanItsPumpIdlingAndItsPumpLouderThanNothing() {
        final float[] idle = new float[Clip.RATE / 2];
        final float[] flowing = new float[Clip.RATE / 2];
        final HydraulicVoice a = new HydraulicVoice(9);
        a.set(1, 0, false);
        a.render(idle, 0, idle.length);
        final HydraulicVoice b = new HydraulicVoice(9);
        b.set(1, 1, false);
        b.render(flowing, 0, flowing.length);
        final int settled = Clip.RATE / 5;
        assertTrue(rms(idle, settled, idle.length) > 0.01, "its pump heard");
        assertTrue(rms(flowing, settled, flowing.length) > 1.2 * rms(idle, settled, idle.length), "louder flowing");
    }
}
