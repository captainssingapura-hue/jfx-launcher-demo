package com.example.fxsuite.avatar.sound;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A turbocharger: its boost comes late, and only when the engine works; on boost it whistles,
 * higher the faster it spins; lifting off on boost, the blow-off valve hisses, and changing gear
 * too; and driven, it pulls harder the more boost there is.
 */
class TurboTest {

    private static final int RATE = Clip.RATE;
    private static final Engine FIVE = Engine.I5.with(Turbo.STANDARD);
    private static final Turbo TURBO = Turbo.STANDARD;

    private static double spooled(final double rpm, final double throttle, final double load, final double seconds) {
        double boost = 0;
        for (int i = 0; i < seconds * 120; i++) {
            boost = TURBO.spool(FIVE, boost, rpm, throttle, load, 1.0 / 120);
        }
        return boost;
    }

    private static float[] run(final EngineVoice voice, final double rpm, final double throttle, final double load,
                               final double seconds) {
        voice.set(rpm, throttle, load);
        final float[] out = new float[(int) Math.round(seconds * RATE)];
        voice.render(out, 0, out.length);
        return out;
    }

    /** How strong {@code samples} are at {@code hz} (Goertzel). */
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

    @Test
    void itsBoostComesLateAndOnlyWhenTheEngineWorks() {
        final double high = 0.8 * FIVE.redline();
        assertTrue(spooled(high, 1, 1, 0.2) < 0.4, "late: " + spooled(high, 1, 1, 0.2));
        assertTrue(spooled(high, 1, 1, 3) > 0.85, "but it comes: " + spooled(high, 1, 1, 3));
        assertTrue(spooled(high, 1, 0, 3) < 0.3, "revving free, little: " + spooled(high, 1, 0, 3));
        assertEquals(0, spooled(FIVE.idle(), 1, 1, 3), 1e-9, "none low in the revs");
        assertTrue(spooled(high, 0.5, 1, 3) < spooled(high, 1, 1, 3), "less, the throttle part open");
    }

    @Test
    void onBoostItWhistlesHigherTheFasterItSpins() {
        final double high = 0.8 * FIVE.redline();
        final EngineVoice on = new EngineVoice(FIVE, 1);
        run(on, high, 1, 1, 3);
        final double boost = on.boost();
        final double note = TURBO.whistle() * (0.12 + 0.88 * boost);
        final float[] whistling = run(on, high, 1, 1, 0.5);
        final EngineVoice off = new EngineVoice(FIVE, 1);
        run(off, high, 1, 0, 3);
        final float[] free = run(off, high, 1, 0, 0.5);
        assertTrue(at(whistling, note) > 5 * at(free, note), "whistling at " + note + " Hz on boost " + boost);
    }

    @Test
    void liftingOffOnBoostTheValveBlowsAndOnlyThen() {
        final EngineVoice voice = new EngineVoice(FIVE, 1);
        run(voice, 0.8 * FIVE.redline(), 1, 1, 3);
        assertEquals(0, voice.dumps(), "not on the throttle");
        final double boost = voice.boost();
        run(voice, 0.8 * FIVE.redline(), 0, 0, 0.3);
        assertEquals(1, voice.dumps(), "lifting off on boost");
        assertTrue(voice.boost() < boost / 2, "and the boost let go");
        // off boost, low in the revs: lifting off is just lifting off
        final EngineVoice low = new EngineVoice(FIVE, 1);
        run(low, FIVE.idle() * 1.5, 1, 1, 2);
        run(low, FIVE.idle() * 1.5, 0, 0, 0.5);
        assertEquals(0, low.dumps());
    }

    @Test
    void noTurbochargerNoBoost() {
        for (final Engine engine : Engine.ALL) {
            if (engine.charged()) {
                continue;
            }
            final EngineVoice voice = new EngineVoice(engine, 1);
            run(voice, 0.8 * engine.redline(), 1, 1, 1);
            run(voice, 0.8 * engine.redline(), 0, 0, 0.5);
            assertEquals(0, voice.boost(), engine.name());
            assertEquals(0, voice.dumps(), engine.name());
        }
    }

    @Test
    void drivenItPullsHarderOnBoost() {
        final Vehicle rally = Vehicle.RALLY;
        final double speed = 20;
        final double rpm = speed / rally.wheel() * 60 / (2 * Math.PI) * rally.ratio(2);
        final Driving off = new Driving(speed, 2, rpm, 0, 0, 0, 1, 1, Driving.Clutch.IN, 0);
        final Driving on = new Driving(speed, 2, rpm, 0, 0, 0, 1, 1, Driving.Clutch.IN, 1);
        final double gainedOff = off.step(rally, 1, 0, 0, false, 0.05).speed() - speed;
        final double gainedOn = on.step(rally, 1, 0, 0, false, 0.05).speed() - speed;
        assertTrue(gainedOn > 1.4 * gainedOff, "on boost " + gainedOn + " against " + gainedOff);
    }

    @Test
    void flatOutThroughTheGearsItBlowsOffAtTheChanges() {
        final Vehicle rally = Vehicle.RALLY;
        Driving driving = Driving.stopped(rally);
        final EngineVoice voice = new EngineVoice(rally.engine(), 1);
        final float[] block = new float[RATE / 120];
        double most = 0;
        int changes = 0;
        for (int i = 0; i < 20 * 120; i++) {
            final Driving next = driving.step(rally, 1, 0, 0, true, 1.0 / 120);
            if (next.clutch() == Driving.Clutch.SHIFTING && driving.clutch() != Driving.Clutch.SHIFTING) {
                changes++;
            }
            driving = next;
            most = Math.max(most, driving.boost());
            voice.set(driving.rpm(), driving.firing(), driving.load());
            voice.render(block, 0, block.length);
        }
        assertTrue(most > 0.8, "on boost, driving: " + most);
        assertTrue(changes >= 3, "up through the gears: " + changes);
        assertTrue(voice.dumps() >= changes - 1, "a hiss at the changes: " + voice.dumps() + " of " + changes);
    }
}
