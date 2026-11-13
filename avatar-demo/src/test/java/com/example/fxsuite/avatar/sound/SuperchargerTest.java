package com.example.fxsuite.avatar.sound;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A supercharger: its boost at once, some even low down, more up the revs; a whine geared to the
 * revs; no hiss lifting off. And the cars' engines, any of them, turbocharged or supercharged.
 */
class SuperchargerTest {

    private static final int RATE = Clip.RATE;
    private static final Supercharger BLOWER = Supercharger.STANDARD;
    private static final Engine V8 = Engine.V8.with(BLOWER);
    private static final List<Charger> CHARGERS = List.of(Turbo.STANDARD, Supercharger.STANDARD);

    private static double spooled(final Engine engine, final double rpm, final double load, final double seconds) {
        double boost = 0;
        for (int i = 0; i < seconds * 120; i++) {
            boost = engine.charger().spool(engine, boost, rpm, 1, load, 1.0 / 120);
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

    private static double rms(final float[] samples) {
        double sum = 0;
        for (final float s : samples) {
            sum += s * s;
        }
        return Math.sqrt(sum / samples.length);
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

    @Test
    void itsBoostComesAtOnceSomeLowDownMoreUpTheRevs() {
        final double high = 0.8 * V8.redline();
        assertTrue(spooled(V8, high, 1, 0.3) > 0.85, "at once: " + spooled(V8, high, 1, 0.3));
        assertTrue(spooled(V8, V8.idle() * 1.5, 1, 0.3) > 0.4, "some low down");
        assertTrue(spooled(V8, high, 1, 0.3) > spooled(V8, V8.idle() * 1.5, 1, 0.3), "more up the revs");
        assertTrue(spooled(V8, high, 0, 0.3) < 0.5 * spooled(V8, high, 1, 0.3), "less revving free");
        // where a turbocharger, at the same moment, has hardly begun
        final Engine turbo = Engine.V8.with(Turbo.STANDARD);
        assertTrue(spooled(turbo, high, 1, 0.3) < 0.5 * spooled(V8, high, 1, 0.3));
    }

    @Test
    void itWhinesAtANoteGearedToTheRevs() {
        for (final double rpm : new double[] {2000, 4000}) {
            final EngineVoice voice = new EngineVoice(V8, 1);
            run(voice, rpm, 1, 1, 0.5);
            final float[] sound = run(voice, rpm, 1, 1, 0.5);
            final double note = BLOWER.whine() * rpm / 1000;
            final double other = BLOWER.whine() * (rpm == 2000 ? 4000 : 2000) / 1000;
            assertTrue(at(sound, note) > 4 * at(sound, other), "whining at " + note + " Hz at " + rpm + " rpm");
        }
    }

    @Test
    void liftingOffItLetsTheBoostGoWithoutAHiss() {
        final EngineVoice voice = new EngineVoice(V8, 1);
        run(voice, 0.8 * V8.redline(), 1, 1, 1);
        assertTrue(voice.boost() > 0.8);
        run(voice, 0.8 * V8.redline(), 0, 0, 0.3);
        assertTrue(voice.boost() < 0.05, "gone at once: " + voice.boost());
        assertEquals(0, voice.dumps(), "no blow-off valve to hiss");
    }

    @Test
    void pullingAwayLowInTheRevsItPullsHarderThanATurbo() {
        final Vehicle muscle = Vehicle.MUSCLE;
        final Vehicle blown = muscle.with(V8);
        final Vehicle turbo = muscle.with(Engine.V8.with(Turbo.STANDARD));
        assertTrue(distance(blown, 1.5) > distance(turbo, 1.5), "off the line, before the turbo comes on");
    }

    /** How far {@code vehicle} gets in {@code seconds} from a standstill, flat out. */
    private static double distance(final Vehicle vehicle, final double seconds) {
        Driving driving = Driving.stopped(vehicle);
        double gone = 0;
        for (int i = 0; i < seconds * 120; i++) {
            driving = driving.step(vehicle, 1, 0, 0, true, 1.0 / 120);
            gone += driving.speed() / 120;
        }
        return gone;
    }

    @Test
    void anyCarsEngineMayBeChargedEitherWayAndOnlyACars() {
        for (final Engine engine : Engine.ALL) {
            assertEquals(Engine.CARS.contains(engine), engine.car(), engine.name());
        }
        for (final Engine plain : Engine.CARS) {
            for (final Charger charger : CHARGERS) {
                final Engine engine = plain.with(charger);
                assertTrue(engine.charged() && engine.car());
                assertEquals(plain.name() + ", " + charger.kind(), engine.toString());
                // it fires as ever, stays in bounds, is louder worked hard, and is silent stopped
                final double rpm = (engine.idle() + engine.redline()) / 2;
                final EngineVoice voice = new EngineVoice(engine, 2);
                run(voice, rpm, 1, 1, 0.5);
                final long before = voice.firings();
                final float[] hard = run(voice, rpm, 1, 1, 1);
                assertEquals(engine.firingRate(rpm), voice.firings() - before, 1.5, engine.toString());
                for (final float s : hard) {
                    assertTrue(Math.abs(s) <= 1);
                }
                final EngineVoice gentle = new EngineVoice(engine, 2);
                run(gentle, rpm, 0.15, 0, 0.5);
                assertTrue(rms(hard) > 1.3 * rms(run(gentle, rpm, 0.15, 0, 1)), engine + " louder worked hard");
                run(voice, 0, 0, 0, 1.5);
                assertTrue(rms(run(voice, 0, 0, 0, 0.5)) < 1e-3, engine + " silent stopped");
                // and driven, it goes up through every gear, and to 100 sooner than breathing on its own
                final Vehicle car = Vehicle.of(plain).with(engine);
                assertTrue(toHundred(car) < toHundred(Vehicle.of(plain).with(plain)), engine + " quicker");
                Driving driving = Driving.stopped(car);
                int highest = 1;
                for (int i = 0; i < 60 * 120; i++) {
                    driving = driving.step(car, i < 45 * 120 ? 1 : 0.4, 0, 0, true, 1.0 / 120);
                    highest = Math.max(highest, driving.gear());
                    assertTrue(driving.rpm() <= engine.redline() * 1.02);
                }
                assertEquals(car.count(), highest, engine + " up into top");
            }
        }
        assertFalse(Engine.THUMPER.car());
    }

    /** From a standstill to 100 km/h, flat out, the gearbox left to it, in seconds. */
    private static double toHundred(final Vehicle vehicle) {
        Driving driving = Driving.stopped(vehicle);
        double seconds = 0;
        while (driving.kmh() < 100 && seconds < 60) {
            driving = driving.step(vehicle, 1, 0, 0, true, 1.0 / 120);
            seconds += 1.0 / 120;
        }
        return seconds;
    }
}
