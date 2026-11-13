package com.example.fxsuite.avatar.sound;

/**
 * A {@link Charger}, heard, a sample at a time, beside its engine's {@link EngineVoice}: its boost
 * worked out from the engine's revs, throttle and load as they go.
 */
interface ChargerVoice {

    /** Its next sample, the engine turning at {@code rpm}, the throttle at {@code throttle}, working {@code load}. */
    double next(double rpm, double throttle, double load, Dsp.Noise noise);

    /** How much boost it makes now, 0 to 1. */
    double boost();

    /** How many times it has blown off its boost, a turbo’s valve: none, for a supercharger. */
    long dumps();
}
