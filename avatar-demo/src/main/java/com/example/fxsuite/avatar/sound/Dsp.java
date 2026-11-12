package com.example.fxsuite.avatar.sound;

/**
 * The few building blocks every sound is made of: noise, a filter, envelopes, and finishing a
 * clip off so it neither clicks nor clips.
 */
final class Dsp {

    static final int RATE = Clip.RATE;

    private Dsp() {
    }

    static int samples(final double seconds) {
        return (int) Math.round(seconds * RATE);
    }

    static double time(final int sample) {
        return (double) sample / RATE;
    }

    /** White noise, -1 to 1, the same every time for the same seed (xorshift64*). */
    static final class Noise {
        private long state;

        Noise(final long seed) {
            state = (seed + 1) * 0x9E3779B97F4A7C15L | 1;
        }

        double next() {
            state ^= state >>> 12;
            state ^= state << 25;
            state ^= state >>> 27;
            final long bits = state * 0x2545F4914F6CDD1DL;
            return (bits >>> 11) * 0x1.0p-52 - 1;
        }
    }

    /**
     * A state-variable filter, the "topology-preserving" kind: stable however it is swept, and
     * giving low-, band- and high-passed copies of its input at once.
     */
    static final class Filter {
        private double ic1;
        private double ic2;
        private double k;
        private double a1;
        private double a2;
        private double a3;
        double low;
        double band;
        double high;

        Filter(final double cutoff, final double q) {
            tune(cutoff, q);
        }

        /** The corner (or centre) frequency, in Hz, and how sharp: 0.7 gentle, 10 ringing. */
        Filter tune(final double cutoff, final double q) {
            final double g = Math.tan(Math.PI * Math.min(cutoff, 0.45 * RATE) / RATE);
            k = 1 / q;
            a1 = 1 / (1 + g * (g + k));
            a2 = g * a1;
            a3 = g * a2;
            return this;
        }

        Filter push(final double input) {
            final double v3 = input - ic2;
            final double v1 = a1 * ic1 + a2 * v3;
            final double v2 = ic2 + a2 * ic1 + a3 * v3;
            ic1 = 2 * v1 - ic1;
            ic2 = 2 * v2 - ic2;
            low = v2;
            band = v1;
            high = input - k * v1 - v2;
            return this;
        }
    }

    /** A sharp rise over {@code attack} seconds, then an exponential fall, a time constant {@code tau}. */
    static double hit(final double t, final double attack, final double tau) {
        if (t < 0) {
            return 0;
        }
        return t < attack ? t / attack : Math.exp(-(t - attack) / tau);
    }

    /** Eases from {@code from} to {@code to} as {@code t} goes 0 to 1, smoothly at both ends. */
    static double glide(final double from, final double to, final double t) {
        final double u = Math.max(0, Math.min(1, t));
        return from + (to - from) * u * u * (3 - 2 * u);
    }

    /** A one-shot, done: faded to silence over its last few milliseconds and brought to {@code peak}. */
    static float[] finish(final double[] signal, final double peak) {
        final int fade = Math.min(signal.length, samples(0.006));
        for (int i = 0; i < fade; i++) {
            signal[signal.length - 1 - i] *= (double) i / fade;
        }
        return normalized(signal, peak);
    }

    /**
     * A loop, done: the first {@code length} samples of {@code signal}, which has a little more,
     * with that extra cross-faded into the start, so the end runs on into the beginning.
     */
    static double[] seamless(final double[] signal, final int length) {
        final int fade = signal.length - length;
        final double[] out = new double[length];
        System.arraycopy(signal, 0, out, 0, length);
        for (int i = 0; i < fade; i++) {
            // equal power, for noise: the two halves are unrelated
            final double in = Math.sqrt((double) i / fade);
            final double over = Math.sqrt(1 - (double) i / fade);
            out[i] = signal[i] * in + signal[length + i] * over;
        }
        return out;
    }

    /**
     * Saturated, as a blast overloads a microphone: brought to full scale, then rounded off by
     * {@code tanh}, {@code drive} times over. The loudest instants are held back and all the rest
     * brought up, so a boom is loud all through, not just for its first crack. 1 is gentle, 4 hard.
     */
    static double[] punch(final double[] signal, final double drive) {
        double most = 0;
        for (final double s : signal) {
            most = Math.max(most, Math.abs(s));
        }
        if (most == 0) {
            return signal;
        }
        final double top = Math.tanh(drive);
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.tanh(drive * signal[i] / most) / top;
        }
        return signal;
    }

    static float[] normalized(final double[] signal, final double peak) {
        double most = 0;
        for (final double s : signal) {
            most = Math.max(most, Math.abs(s));
        }
        final double scale = most == 0 ? 0 : peak / most;
        final float[] out = new float[signal.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = (float) (signal[i] * scale);
        }
        return out;
    }
}
