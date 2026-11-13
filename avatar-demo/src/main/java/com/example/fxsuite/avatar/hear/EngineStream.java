package com.example.fxsuite.avatar.hear;

import com.example.fxsuite.avatar.audio.Speakers;
import com.example.fxsuite.avatar.sound.Engine;
import com.example.fxsuite.avatar.sound.EngineVoice;

/**
 * The engine of the vehicle being driven, made as it runs ({@link EngineVoice}), for the speakers
 * to play as a stream. The frames say what it is doing ({@link #set}); the output asks it for its
 * sound on whatever thread it mixes on, and only there is the voice touched: what the frames say
 * reaches it through fields safe to share.
 */
final class EngineStream implements Speakers.Source {

    private final Engine engine;
    private final EngineVoice voice;
    private volatile double rpm;
    private volatile double throttle;
    private volatile double load;

    EngineStream(final Engine engine) {
        this.engine = engine;
        voice = new EngineVoice(engine, 1);
        rpm = engine.idle();
    }

    Engine engine() {
        return engine;
    }

    /** What it is to do from here, as {@link EngineVoice#set}. */
    void set(final double rpm, final double throttle, final double load) {
        this.rpm = rpm;
        this.throttle = throttle;
        this.load = load;
    }

    @Override
    public void fill(final float[] into, final int count) {
        voice.set(rpm, throttle, load);
        voice.render(into, 0, count);
    }
}
