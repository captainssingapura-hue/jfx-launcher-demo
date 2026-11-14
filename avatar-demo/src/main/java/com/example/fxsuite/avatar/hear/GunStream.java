package com.example.fxsuite.avatar.hear;

import com.example.fxsuite.avatar.audio.Speakers;
import com.example.fxsuite.avatar.sound.Gatling;
import com.example.fxsuite.avatar.sound.GatlingVoice;
import com.example.fxsuite.avatar.sound.GunStyle;

/**
 * The Gatling gun being fired, made as it turns and fires ({@link GatlingVoice}), for the speakers
 * to play as a stream. The frames say what it is doing ({@link #set}) and how it is mixed
 * ({@link #style}); the output asks it for its sound on whatever thread it mixes on, and only
 * there is the voice touched: what the frames say reaches it through fields safe to share.
 */
final class GunStream implements Speakers.Source {

    private final Gatling gatling;
    private final GatlingVoice voice;
    private volatile double spin;
    private volatile boolean firing;
    private volatile double height;
    private volatile GunStyle style;

    GunStream(final Gatling gatling, final GunStyle style) {
        this.gatling = gatling;
        this.style = style;
        voice = new GatlingVoice(gatling, style, 1);
    }

    Gatling gatling() {
        return gatling;
    }

    /** What it is to do from here, as {@link GatlingVoice#set}. */
    void set(final double spin, final boolean firing, final double height) {
        this.spin = spin;
        this.firing = firing;
        this.height = height;
    }

    /** Let go: its barrels running down, firing no more, where it was. */
    void letGo() {
        spin = 0;
        firing = false;
    }

    /** Mixed so from here. */
    void style(final GunStyle style) {
        this.style = style;
    }

    @Override
    public void fill(final float[] into, final int count) {
        final GunStyle now = style;
        if (now != voice.style()) {
            voice.style(now);
        }
        voice.set(spin, firing, height);
        voice.render(into, 0, count);
    }
}
