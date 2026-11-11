package com.example.fxsuite.avatar.audio;

import com.example.fxsuite.avatar.sim.Vec3;
import com.example.fxsuite.avatar.sound.Clip;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * A software mixer: the voices playing now, summed into stereo, each placed by {@link Space}
 * and pitched by playing it faster or slower. What OpenAL does for itself, for the outputs
 * that cannot: Java Sound plays what this renders, and the bench renders it to a file.
 *
 * <p>Pure and device-free. Safe to drive from one thread while another renders: every method
 * holds the mixer's lock, and a render is short.
 *
 * <p>A voice whose loudness or place changes ramps to it over one render, rather than jumping,
 * which would click.
 */
public final class Mix {

    private final List<Voice> voices = new ArrayList<>();
    private Vec3 ear = Vec3.ZERO;
    private double yaw;
    private double volume = 1;

    public synchronized void listen(final Vec3 at, final double yaw) {
        this.ear = at;
        this.yaw = yaw;
    }

    public synchronized void volume(final double volume) {
        this.volume = Math.max(0, volume);
    }

    /** Plays a clip once; {@code at} null for at the listener. */
    public synchronized void play(final Clip clip, final double gain, final double pitch, final Vec3 at) {
        final Voice voice = new Voice(clip, false);
        voice.gain = gain;
        voice.pitch = pitch;
        voice.at = at;
        voices.add(voice);
    }

    /** A clip that plays round and round until stopped, starting silent. */
    public synchronized Voice loop(final Clip clip) {
        final Voice voice = new Voice(clip, true);
        voices.add(voice);
        return voice;
    }

    /** How many voices are sounding. */
    public synchronized int playing() {
        return voices.size();
    }

    public synchronized void clear() {
        voices.clear();
    }

    /** The next {@code frames} of sound, into {@code out} as left and right in turn. */
    public synchronized void render(final float[] out, final int frames) {
        Arrays.fill(out, 0, frames * 2, 0);
        final Iterator<Voice> each = voices.iterator();
        while (each.hasNext()) {
            if (!each.next().mixInto(out, frames)) {
                each.remove();
            }
        }
    }

    /** One clip playing. As a loop, the handle to steer it by. */
    public final class Voice implements Speakers.Loop {
        private final Clip clip;
        private final boolean looping;
        private double position;
        private double gain;
        private double pitch = 1;
        private Vec3 at;
        private boolean stopping;
        private boolean started;
        private double left;
        private double right;

        private Voice(final Clip clip, final boolean looping) {
            this.clip = clip;
            this.looping = looping;
        }

        @Override
        public void set(final double gain, final double pitch) {
            synchronized (Mix.this) {
                this.gain = gain;
                this.pitch = pitch;
            }
        }

        @Override
        public void place(final Vec3 at) {
            synchronized (Mix.this) {
                this.at = at;
            }
        }

        /** Fades out over the next render, then is gone. */
        @Override
        public void stop() {
            synchronized (Mix.this) {
                stopping = true;
            }
        }

        /** Adds this voice into {@code out}; false once it has finished. */
        private boolean mixInto(final float[] out, final int frames) {
            double level = stopping ? 0 : Math.max(0, gain) * volume;
            double side = 0;
            if (at != null) {
                level *= Space.fade(at.distance(ear));
                side = Space.side(ear, yaw, at);
            }
            // equal power: as loud overall wherever it sits
            final double angle = (side + 1) * Math.PI / 4;
            final double toLeft = level * Math.cos(angle);
            final double toRight = level * Math.sin(angle);
            if (!started) {
                left = toLeft;
                right = toRight;
                started = true;
            }
            final int length = clip.length();
            if (length == 0) {
                return false;
            }
            final double step = Math.max(0.01, pitch);
            boolean playing = true;
            for (int f = 0; f < frames && playing; f++) {
                final double ramp = (f + 1.0) / frames;
                final double l = left + (toLeft - left) * ramp;
                final double r = right + (toRight - right) * ramp;
                final int i = (int) position;
                final double between = position - i;
                final float here = clip.sample(i);
                final float next = i + 1 < length ? clip.sample(i + 1) : looping ? clip.sample(0) : 0;
                final double s = here + (next - here) * between;
                out[2 * f] += (float) (s * l);
                out[2 * f + 1] += (float) (s * r);
                position += step;
                if (position >= length) {
                    if (looping) {
                        position %= length;
                    } else {
                        playing = false;
                    }
                }
            }
            left = toLeft;
            right = toRight;
            return playing && !stopping;
        }
    }
}
