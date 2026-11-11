package com.example.fxsuite.avatar.audio;

import com.example.fxsuite.avatar.sim.Vec3;
import com.example.fxsuite.avatar.sound.Clip;

/** No sound at all: for unattended runs, or a machine with nothing to play on. Never fails. */
final class SilentSpeakers implements Speakers {

    private static final Loop QUIET = new Loop() {
        @Override
        public void set(final double gain, final double pitch) {
        }

        @Override
        public void place(final Vec3 at) {
        }

        @Override
        public void stop() {
        }
    };

    @Override
    public Kind kind() {
        return Kind.SILENT;
    }

    @Override
    public String device() {
        return "none";
    }

    @Override
    public void listen(final Vec3 at, final double yaw) {
    }

    @Override
    public void volume(final double volume) {
    }

    @Override
    public void play(final Clip clip, final double gain, final double pitch) {
    }

    @Override
    public void play(final Clip clip, final double gain, final double pitch, final Vec3 at) {
    }

    @Override
    public Loop loop(final Clip clip) {
        return QUIET;
    }

    @Override
    public void close() {
    }
}
