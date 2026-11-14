package com.example.fxsuite.avatar.sound;

import com.example.fxsuite.avatar.sim.Weapon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A Gatling gun made as it turns and fires: a shot at each barrel's pass, as often as the gun
 * fires, the bigger gun lower; dry clicks turning unfired; the cases heard landing from low down
 * only; the ground's echo as late as it is far below; and the two mixes, the film's ringing on.
 */
class GatlingVoiceTest {

    private static final int RATE = Clip.RATE;

    /** {@code seconds} of {@code voice} doing what it was set to. */
    private static float[] run(final GatlingVoice voice, final double seconds) {
        final float[] out = new float[(int) Math.round(seconds * RATE)];
        for (int at = 0; at < out.length; at += 512) {
            voice.render(out, at, Math.min(512, out.length - at));
        }
        return out;
    }

    private static double rms(final float[] s, final int from, final int to) {
        double sum = 0;
        for (int i = from; i < to; i++) {
            sum += s[i] * s[i];
        }
        return Math.sqrt(sum / (to - from));
    }

    /** How much of the sound's power is below 150 Hz: the low end, the thumps and the booms. */
    private static double low(final float[] s) {
        final double a = 1 - Math.exp(-2 * Math.PI * 150.0 / RATE);
        double one = 0;
        double two = 0;
        double under = 0;
        double all = 0;
        for (final float x : s) {
            // through two poles, gently
            one += (x - one) * a;
            two += (one - two) * a;
            under += two * two;
            all += x * x;
        }
        return under / all;
    }

    @Test
    void stillAndNotFiringItIsSilent() {
        for (final Gatling gun : Gatling.ALL) {
            final GatlingVoice voice = new GatlingVoice(gun, GunStyle.CINEMATIC, 1);
            final float[] s = run(voice, 0.5);
            assertEquals(0, rms(s, 0, s.length), 1e-6, gun.name());
            assertEquals(0, voice.shots() + voice.clicks());
        }
    }

    @Test
    void upToSpeedItFiresAShotAtEachBarrelsPassAsOftenAsTheGunDoes() {
        for (final Gatling gun : Gatling.ALL) {
            final GatlingVoice voice = new GatlingVoice(gun, GunStyle.REALISTIC, 1);
            voice.set(1, false, 1);
            run(voice, 0.3);
            final long before = voice.shots();
            voice.set(1, true, 1);
            run(voice, 2);
            assertEquals(2 * gun.perSecond(), voice.shots() - before, 1.5, gun.name());
        }
        assertEquals(40, Gatling.HAND.perSecond(), 1e-9);
        assertEquals(30, Gatling.HEAVY.perSecond(), 1e-9);
        assertEquals(20, Gatling.GIANT.perSecond(), 1e-9);
    }

    @Test
    void notUpToSpeedItDoesNotFireButClicksOverDry() {
        final GatlingVoice voice = new GatlingVoice(Gatling.HEAVY, GunStyle.REALISTIC, 1);
        voice.set(0.5, true, 1);
        final float[] s = run(voice, 1);
        assertEquals(0, voice.shots(), "only at full speed, as the gun fires");
        assertEquals(0.5 * Gatling.HEAVY.perSecond(), voice.clicks(), 2, "a click at each barrel's pass");
        assertTrue(rms(s, RATE / 2, s.length) > 0.005, "the motor and the clicks heard");
    }

    @Test
    void firingItIsLouderThanSpinningAndWithinFullScale() {
        for (final GunStyle style : GunStyle.values()) {
            for (final Gatling gun : Gatling.ALL) {
                final GatlingVoice voice = new GatlingVoice(gun, style, 1);
                voice.set(1, false, 1);
                final float[] spinning = run(voice, 0.5);
                voice.set(1, true, 1);
                final float[] firing = run(voice, 1);
                final String what = style + " " + gun.name();
                assertTrue(rms(firing, 0, firing.length) > 2 * rms(spinning, RATE / 4, spinning.length), what);
                for (final float x : firing) {
                    assertTrue(Float.isFinite(x) && Math.abs(x) <= 1, what);
                }
            }
        }
    }

    @Test
    void theSameSeedGivesTheSameSound() {
        final GatlingVoice a = new GatlingVoice(Gatling.GIANT, GunStyle.CINEMATIC, 7);
        final GatlingVoice b = new GatlingVoice(Gatling.GIANT, GunStyle.CINEMATIC, 7);
        a.set(1, true, 30);
        b.set(1, true, 30);
        assertArrayEquals(run(a, 0.5), run(b, 0.5));
    }

    @Test
    void theBiggerTheRoundTheLowerItsThumpAndItsSound() {
        assertTrue(Gatling.GIANT.thump() < Gatling.HEAVY.thump());
        assertTrue(Gatling.HEAVY.thump() < Gatling.HAND.thump());
        assertTrue(Gatling.GIANT.blast() > Gatling.HAND.blast(), "a longer push of air");
        final double[] heard = new double[Gatling.ALL.size()];
        for (int g = 0; g < heard.length; g++) {
            final GatlingVoice voice = new GatlingVoice(Gatling.ALL.get(g), GunStyle.CINEMATIC, 1);
            voice.set(1, true, 1);
            heard[g] = low(run(voice, 1));
        }
        assertTrue(heard[2] > heard[1] && heard[1] > heard[0],
                String.format("more of it low, the bigger the gun: %.2f, %.2f, %.2f", heard[0], heard[1], heard[2]));
    }

    @Test
    void theFilmsMixRingsOnLongerAfterItStops() {
        final double[] after = new double[2];
        for (final GunStyle style : GunStyle.values()) {
            final GatlingVoice voice = new GatlingVoice(Gatling.HEAVY, style, 1);
            // high enough that no case is heard landing after it, and the ground's echo long gone
            voice.set(1, true, 20);
            final float[] firing = run(voice, 1);
            voice.set(0, false, 20);
            final float[] tail = run(voice, 1);
            // how loud it still is, half a second on and after, against while it fired
            after[style.ordinal()] = rms(tail, RATE / 2, tail.length) / rms(firing, 0, firing.length);
        }
        assertTrue(after[GunStyle.CINEMATIC.ordinal()] > after[GunStyle.REALISTIC.ordinal()],
                "cinematic " + after[1] + ", realistic " + after[0]);
        assertTrue(GunStyle.CINEMATIC.tail() > GunStyle.REALISTIC.tail());
    }

    @Test
    void theCasesAreHeardLandingFromLowDownOnly() {
        final GatlingVoice low = new GatlingVoice(Gatling.HAND, GunStyle.REALISTIC, 1);
        low.set(1, true, 1);
        run(low, 1);
        low.set(0, false, 1);
        run(low, 1);
        assertEquals(low.shots(), low.landed(), "each case landed, once it had fallen");
        final GatlingVoice high = new GatlingVoice(Gatling.GIANT, GunStyle.REALISTIC, 1);
        high.set(1, true, 100);
        run(high, 2);
        assertTrue(high.shots() > 0);
        assertEquals(0, high.landed(), "too far below to be heard");
    }

    @Test
    void theGroundSendsTheBlastBackAsLateAsItIsFarBelow() {
        assertEquals(0, GatlingVoice.echoDelay(0), 1e-12);
        assertEquals(200.0 / 343, GatlingVoice.echoDelay(100), 0.01, "a hundred metres down and back");
        assertTrue(GatlingVoice.echoDelay(1.2) < 0.01, "at the hip, so close it only colours it");
    }

    @Test
    void eachGunIsTheOneThatFiresItsRounds() {
        assertEquals(Gatling.HAND, Gatling.of(Weapon.GATLING));
        assertEquals(Gatling.HEAVY, Gatling.of(Weapon.HEAVY_GATLING));
        assertEquals(Gatling.GIANT, Gatling.of(Weapon.GIANT_GATLING));
        assertEquals(Gatling.GIANT, Gatling.called("giant", Gatling.HAND));
        assertEquals(GunStyle.REALISTIC, GunStyle.named("real"));
        assertEquals(GunStyle.CINEMATIC, GunStyle.named(null));
    }
}
