package com.example.fxsuite.avatar.sound;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The exhaust, cylinder by cylinder: each engine's real firing order, its cylinders on the banks
 * that order puts them on; runners of unlike lengths making evenly fired pulses arrive unevenly,
 * so a five warbles and the order is heard; and a crossover evening out a cross-plane V8's banks.
 */
class ExhaustTest {

    private static final int RATE = Clip.RATE;

    private static float[] settled(final Engine engine, final double rpm, final double seconds) {
        final EngineVoice voice = new EngineVoice(engine, 1);
        voice.set(rpm, 0.6, 0.3);
        final float[] warm = new float[RATE / 2];
        voice.render(warm, 0, warm.length);
        final float[] out = new float[(int) (seconds * RATE)];
        voice.render(out, 0, out.length);
        return out;
    }

    private static double at(final float[] samples, final double hz) {
        final double c = 2 * Math.cos(2 * Math.PI * hz / RATE);
        double s1 = 0;
        double s2 = 0;
        for (final float x : samples) {
            final double s0 = x + c * s1 - s2;
            s2 = s1;
            s1 = s0;
        }
        return Math.sqrt(s1 * s1 + s2 * s2 - c * s1 * s2) / samples.length;
    }

    /** How strong {@code sound} is at the whole cycle's multiples between its firing notes, against its firing note. */
    private static double[] between(final Engine engine, final float[] sound, final double rpm) {
        final int n = engine.cylinders();
        final double note = engine.firingRate(rpm);
        final double[] out = new double[n - 1];
        for (int k = 1; k < n; k++) {
            out[k - 1] = at(sound, note * k / n) / at(sound, note);
        }
        return out;
    }

    private static double sum(final double[] values) {
        double total = 0;
        for (final double v : values) {
            total += v;
        }
        return total;
    }

    @Test
    void eachEnginesFiringOrderPutsItsCylindersOnTheirBanks() {
        assertEquals("1-8-4-3-6-5-7-2", Engine.V8.exhaust().written());
        assertEquals("1-5-3-7-4-8-2-6", Engine.FLAT_V8.exhaust().written());
        assertEquals("1-2-4-5-3", Engine.I5.exhaust().written());
        assertEquals("1-3-4-2", Engine.I4.exhaust().written());
        for (int s = 0; s < 8; s++) {
            // the cross-plane's odd cylinders on the left bank; the flat-plane's first four on one
            assertEquals(Engine.V8.exhaust().order()[s] % 2 == 1 ? 0 : 1, Engine.V8.bank()[s]);
            assertEquals(Engine.FLAT_V8.exhaust().order()[s] <= 4 ? 0 : 1, Engine.FLAT_V8.bank()[s]);
        }
        for (int s = 0; s < 6; s++) {
            assertEquals(Engine.V6.exhaust().order()[s] % 2 == 1 ? 0 : 1, Engine.V6.bank()[s]);
        }
        for (final Engine engine : Engine.ALL) {
            assertEquals(engine.cylinders(), engine.exhaust().order().length, engine.name());
        }
    }

    @Test
    void aFivesManifoldMakesItWarbleWhereEqualHeadersDoNot() {
        final double rpm = 4000;
        final Engine stock = Engine.I5;
        final Engine tuned = Engine.I5.piped(Engine.I5.exhaust().equalLength());
        final double warble = sum(between(stock, settled(stock, rpm, 2), rpm));
        final double even = sum(between(tuned, settled(tuned, rpm, 2), rpm));
        assertTrue(warble > 1.5 * even, "the warble: " + warble + " against " + even);
    }

    @Test
    void theOrderTheCylindersFireInIsHeard() {
        final double rpm = 4000;
        final Engine audi = Engine.I5;
        final Engine reversed = audi.piped(new Exhaust(new int[] {1, 3, 5, 4, 2}, audi.exhaust().runners(), 0));
        final double[] one = between(audi, settled(audi, rpm, 2), rpm);
        final double[] other = between(reversed, settled(reversed, rpm, 2), rpm);
        double most = 0;
        for (int k = 0; k < one.length; k++) {
            most = Math.max(most, Math.abs(one[k] - other[k]) / Math.max(one[k], other[k]));
        }
        assertTrue(most > 0.2, "another order, another sound: " + most);
    }

    @Test
    void anXPipeEvensOutACrossPlaneV8sBurbleAndAnHPipeLess() {
        final double rpm = 1500;
        final double note = Engine.V8.firingRate(rpm);
        final double[] lope = new double[3];
        final double[] crossings = {Exhaust.NONE, Exhaust.H_PIPE, Exhaust.X_PIPE};
        for (int i = 0; i < 3; i++) {
            final Engine engine = Engine.V8.piped(Engine.V8.exhaust().crossed(crossings[i]));
            final float[] sound = settled(engine, rpm, 2);
            lope[i] = (at(sound, note * 3 / 8) + at(sound, note * 5 / 8)) / at(sound, note);
        }
        assertTrue(lope[1] < lope[0] && lope[2] < 0.7 * lope[0], "evened out: " + lope[0] + ", " + lope[1] + ", " + lope[2]);
    }

    @Test
    void aCrossoverIsNothingToAnEngineWithOnePipe() {
        final Engine plain = Engine.I4;
        final Engine crossed = plain.piped(plain.exhaust().crossed(Exhaust.X_PIPE));
        assertFalse(plain.twoBanks());
        assertArrayEquals(settled(plain, 3000, 0.5), settled(crossed, 3000, 0.5));
    }

    @Test
    void anExhaustHasARunnerForEachCylinderFiringOnceEach() {
        assertThrows(IllegalArgumentException.class, () -> new Exhaust(new int[] {1, 1}, new double[] {0.4, 0.4}, 0));
        assertThrows(IllegalArgumentException.class, () -> new Exhaust(new int[] {1, 3}, new double[] {0.4, 0.4}, 0));
        assertThrows(IllegalArgumentException.class, () -> new Exhaust(new int[] {1, 2}, new double[] {0.4}, 0));
        assertThrows(IllegalArgumentException.class, () -> new Exhaust(new int[] {1}, new double[] {0}, 0));
        assertThrows(IllegalArgumentException.class, () -> new Exhaust(new int[] {1}, new double[] {0.4}, 0.6));
        assertThrows(IllegalArgumentException.class, () -> Engine.V8.piped(Exhaust.even(6)));
        assertTrue(Engine.I5.exhaust().equalLength().equal());
        assertFalse(Engine.I5.exhaust().equal());
        assertEquals(Exhaust.SOUND / 4 / 0.58, Engine.I5.exhaust().note(0), 1e-9, "cylinder 1's runner, a quarter wave");
    }
}
