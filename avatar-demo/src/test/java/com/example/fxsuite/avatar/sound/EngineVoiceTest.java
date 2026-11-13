package com.example.fxsuite.avatar.sound;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Engines made from their revs: each fires as often as it turns, its firing note following the
 * revs; a cross-plane V8's banks fire unevenly, and it is heard; off the throttle at speed it pops;
 * worked hard it is louder; and it is the same every time, never past full scale, and silent stopped.
 */
class EngineVoiceTest {

    private static final int RATE = Clip.RATE;

    private static float[] run(final EngineVoice voice, final double rpm, final double throttle, final double load,
                               final double seconds) {
        voice.set(rpm, throttle, load);
        final float[] out = new float[(int) Math.round(seconds * RATE)];
        voice.render(out, 0, out.length);
        return out;
    }

    /** At {@code rpm}, settled there first. */
    private static float[] settled(final Engine engine, final double rpm, final double throttle, final double load,
                                   final double seconds) {
        final EngineVoice voice = new EngineVoice(engine, 1);
        run(voice, rpm, throttle, load, 0.5);
        return run(voice, rpm, throttle, load, seconds);
    }

    private static double rms(final float[] samples) {
        double sum = 0;
        for (final float s : samples) {
            sum += s * s;
        }
        return Math.sqrt(sum / samples.length);
    }

    /** How strong {@code samples} are at {@code hz} (Goertzel), on the whole of them. */
    private static double at(final float[] samples, final double hz) {
        final double w = 2 * Math.PI * hz / RATE;
        final double c = 2 * Math.cos(w);
        double s1 = 0;
        double s2 = 0;
        for (final float x : samples) {
            final double s0 = x + c * s1 - s2;
            s2 = s1;
            s1 = s0;
        }
        return Math.sqrt(s1 * s1 + s2 * s2 - c * s1 * s2) / samples.length;
    }

    @Test
    void eachFiresAsOftenAsItTurns() {
        for (final Engine engine : Engine.ALL) {
            for (final double rpm : new double[] {engine.idle(), 3000, engine.redline()}) {
                final EngineVoice voice = new EngineVoice(engine, 3);
                run(voice, rpm, 1, 0.5, 0.5);
                final long before = voice.firings();
                run(voice, rpm, 1, 0.5, 2);
                assertEquals(2 * engine.firingRate(rpm), voice.firings() - before, 1.5, engine + " at " + rpm);
            }
        }
    }

    @Test
    void theFiringNoteFollowsTheRevs() {
        // an inline four, every pulse alike through one pipe: strong at its firing note, wherever that is
        for (final double rpm : new double[] {1500, 3000, 6000}) {
            final float[] sound = settled(Engine.I4, rpm, 0.8, 0.3, 1);
            final double note = Engine.I4.firingRate(rpm);
            assertTrue(at(sound, note) > 4 * at(sound, note * 0.75), "at " + rpm + " rpm");
        }
    }

    @Test
    void aCrossPlaneV8LopesWhereAFlatPlaneOneDoesNot() {
        // the cross-plane's banks fire unevenly, over the whole cycle: sound at odd eighths of its firing note
        final double rpm = 1500;
        final double note = Engine.V8.firingRate(rpm);
        final float[] cross = settled(Engine.V8, rpm, 0.6, 0.3, 2);
        final float[] flat = settled(Engine.FLAT_V8, rpm, 0.6, 0.3, 2);
        final double lope = (at(cross, note * 3 / 8) + at(cross, note * 5 / 8)) / at(cross, note);
        final double even = (at(flat, note * 3 / 8) + at(flat, note * 5 / 8)) / at(flat, note);
        assertTrue(lope > 3 * even, "the burble: " + lope + " against " + even);
    }

    @Test
    void aCruisersVTwinLopesWhereAParallelTwinBeatsEvenly() {
        // 315 degrees then 405 between its firings: sound at half its firing note, once a cycle
        final double rpm = 1200;
        final float[] vee = settled(Engine.V_TWIN, rpm, 0.5, 0.3, 2);
        final float[] even = settled(Engine.PARALLEL_TWIN, rpm, 0.5, 0.3, 2);
        final double veeNote = Engine.V_TWIN.firingRate(rpm);
        final double evenNote = Engine.PARALLEL_TWIN.firingRate(rpm);
        final double lope = at(vee, veeNote / 2) / at(vee, veeNote);
        final double steady = at(even, evenNote / 2) / at(even, evenNote);
        assertTrue(lope > 3 * steady, "the lope: " + lope + " against " + steady);
    }

    @Test
    void offTheThrottleAtSpeedItPopsAndOnlyThen() {
        for (final Engine engine : Engine.ALL) {
            final EngineVoice voice = new EngineVoice(engine, 5);
            run(voice, engine.redline() * 0.75, 1, 0.5, 2);
            assertEquals(0, voice.pops(), engine + ": not on the throttle");
            run(voice, engine.idle(), 0, 0, 2);
            assertEquals(0, voice.pops(), engine + ": nor at idle");
            run(voice, engine.redline() * 0.75, 0, 0, 3);
            assertTrue(voice.pops() > 0, engine + ": but off it at speed");
        }
    }

    @Test
    void workedHardItIsLouder() {
        for (final Engine engine : Engine.ALL) {
            final double rpm = (engine.idle() + engine.redline()) / 2;
            assertTrue(rms(settled(engine, rpm, 1, 1, 1)) > 1.5 * rms(settled(engine, rpm, 0.15, 0, 1)), engine.name());
        }
    }

    @Test
    void theSameEveryTimeNeverPastFullScaleAndSilentStopped() {
        for (final Engine engine : Engine.ALL) {
            final EngineVoice voice = new EngineVoice(engine, 9);
            final float[] flat = run(voice, engine.redline(), 1, 1, 2);
            for (final float s : flat) {
                assertTrue(Math.abs(s) <= 1);
            }
            final EngineVoice again = new EngineVoice(engine, 9);
            assertArrayEquals(flat, run(again, engine.redline(), 1, 1, 2));
            run(voice, 0, 0, 0, 1);
            final long fired = voice.firings();
            final float[] stopped = run(voice, 0, 0, 0, 0.5);
            assertEquals(fired, voice.firings(), "no more firing");
            assertTrue(rms(stopped) < 1e-3, engine + " silent: " + rms(stopped));
        }
    }

    @Test
    void anEngineFiresInOrderThroughTheCycleFromBankToBank() {
        assertThrows(IllegalArgumentException.class, () -> new Engine("x", "x", "", 4, new double[] {0, 360, 180},
                new int[] {0, 0, 0}, 1, 800, 6000, 1, 1, new double[] {100}, 4, 3, 1, 400));
        assertThrows(IllegalArgumentException.class, () -> new Engine("x", "x", "", 4, new double[] {0, 360},
                new int[] {0, 2}, 1, 800, 6000, 1, 1, new double[] {100}, 4, 3, 1, 400));
        assertEquals(8, Engine.V8.cylinders());
        assertEquals(400, Engine.V8.firingRate(6000), 1e-9, "eight cylinders, each every other turn");
        assertThrows(IllegalArgumentException.class, () -> new Engine("x", "x", "", 2, new double[] {0, 400},
                new int[] {0, 0}, 1, 800, 6000, 1, 1, new double[] {100}, 4, 3, 1, 400), "a two-stroke's cycle is one turn");
        assertThrows(IllegalArgumentException.class, () -> new Engine("x", "x", "", 3, new double[] {0},
                new int[] {0}, 1, 800, 6000, 1, 1, new double[] {100}, 4, 3, 1, 400));
        assertEquals(100, Engine.SCOOTER_2T.firingRate(6000), 1e-9, "a two-stroke single, every turn");
        assertEquals(50, Engine.THUMPER.firingRate(6000), 1e-9, "a four-stroke single, every other turn");
        assertEquals(Engine.THUMPER, Engine.called("thumper", null));
        assertEquals(Engine.V8, Engine.called("no such", Engine.V8));
    }
}
