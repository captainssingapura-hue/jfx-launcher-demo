package com.example.fxsuite.avatar.sound;

/**
 * A {@link Turbo}, heard, a sample at a time, beside its engine's {@link EngineVoice}: its boost
 * spooled from the engine's revs, throttle and load as they go. It whistles, a thin high note
 * rising as it spins up; the air rushes through it; and when the throttle snaps shut on boost
 * (lifting off, or changing gear), the blow-off valve lets the boost go with a hiss, and it
 * spins down.
 */
final class TurboVoice implements ChargerVoice {

    private static final int RATE = Clip.RATE;
    /** The throttle shut past this, on at least this much boost, the valve blows. */
    private static final double SHUT = 0.25;
    private static final double BLOWS = 0.3;
    /** Blown, this much of the boost is left. */
    private static final double LEFT = 0.35;
    /** How long the hiss takes to die away, in seconds. */
    private static final double HISS = 0.28;
    /** How loud the whistle and the rush of air are, at full boost. */
    private static final double WHISTLE = 0.05;
    private static final double RUSH = 0.12;

    private final Turbo turbo;
    private final Engine engine;
    private final Dsp.Filter hiss = new Dsp.Filter(2300, 0.9);
    private final Dsp.Filter rush = new Dsp.Filter(900, 1.0);
    private final double hissFade = Math.exp(-1 / (HISS * RATE));

    private double boost;
    private double phase;
    private double venting;
    private double was;
    private long dumps;

    TurboVoice(final Engine engine, final Turbo turbo) {
        this.engine = engine;
        this.turbo = turbo;
    }

    @Override
    public double boost() {
        return boost;
    }

    @Override
    public long dumps() {
        return dumps;
    }

    @Override
    public double next(final double rpm, final double throttle, final double load, final Dsp.Noise noise) {
        boost = turbo.spool(engine, boost, rpm, throttle, load, 1.0 / RATE);
        // the throttle snapped shut on boost: the valve lets it go
        if (throttle < SHUT && was >= SHUT && boost > BLOWS) {
            venting = boost;
            boost *= LEFT;
            dumps++;
        }
        was = throttle;
        // the whistle, higher the faster it spins
        phase += turbo.whistle() * (0.12 + 0.88 * boost) / RATE;
        phase -= Math.floor(phase);
        final double whistle = WHISTLE * Math.pow(boost, 1.5)
                * (Math.sin(2 * Math.PI * phase) + 0.3 * Math.sin(4 * Math.PI * phase));
        final double air = RUSH * boost * rush.push(noise.next()).band;
        double blown = 0;
        if (venting > 1e-4) {
            blown = turbo.dump() * 0.5 * venting * hiss.push(noise.next()).band;
            venting *= hissFade;
        }
        return whistle + air + blown;
    }
}
