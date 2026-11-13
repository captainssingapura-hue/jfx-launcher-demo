package com.example.fxsuite.avatar.sound;

/**
 * A {@link Supercharger}, heard, a sample at a time, beside its engine's {@link EngineVoice}. It
 * whines, its rotors geared to the crank, so the note goes up and down with the revs, never late:
 * a little even idling, louder on boost. Under it, the howl of air through the rotors, and the
 * intake breathing harder the more boost there is.
 */
final class SuperchargerVoice implements ChargerVoice {

    private static final int RATE = Clip.RATE;
    /** How loud the whine is, idling and on full boost; and the howl of air, and the breathing. */
    private static final double IDLING = 0.008;
    private static final double WHINE = 0.045;
    private static final double HOWL = 0.03;
    private static final double BREATH = 0.08;

    private final Supercharger blower;
    private final Engine engine;
    private final Dsp.Filter howl;
    private final Dsp.Filter breath = new Dsp.Filter(700, 1.0);

    private double boost;
    private double phase;

    SuperchargerVoice(final Engine engine, final Supercharger blower) {
        this.engine = engine;
        this.blower = blower;
        howl = new Dsp.Filter(blower.whine() * engine.idle() / 1000, 8);
    }

    @Override
    public double boost() {
        return boost;
    }

    @Override
    public long dumps() {
        return 0;
    }

    @Override
    public double next(final double rpm, final double throttle, final double load, final Dsp.Noise noise) {
        boost = blower.spool(engine, boost, rpm, throttle, load, 1.0 / RATE);
        final double note = blower.whine() * rpm / 1000;
        phase += note / RATE;
        phase -= Math.floor(phase);
        // the rotors' whine, a little square from their lobes
        final double loud = blower.loud() * (IDLING + WHINE * boost);
        final double whine = loud * (Math.sin(2 * Math.PI * phase) + 0.35 * Math.sin(6 * Math.PI * phase)
                + 0.2 * Math.sin(4 * Math.PI * phase));
        // the air through them, howling about the same note
        howl.tune(Math.max(40, note), 8);
        final double howling = HOWL * (0.3 + boost) * howl.push(noise.next()).band;
        final double breathing = BREATH * boost * breath.push(noise.next()).band;
        return whine + howling + breathing;
    }
}
