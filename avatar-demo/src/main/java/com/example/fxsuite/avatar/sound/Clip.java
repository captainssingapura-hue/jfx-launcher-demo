package com.example.fxsuite.avatar.sound;

/**
 * A sound, ready to play: mono samples from -1 to 1 at {@link #RATE} a second. Mono, so an
 * output can place it anywhere round the listener. A clip that {@linkplain #loops loops} is
 * made to run on from its last sample into its first without a click.
 *
 * <p>The samples are not copied in or out, for speed; nothing changes them once made.
 */
public final class Clip {

    public static final int RATE = 44_100;

    private final String name;
    final float[] samples;
    private final boolean loops;

    public Clip(final String name, final float[] samples, final boolean loops) {
        this.name = name;
        this.samples = samples;
        this.loops = loops;
    }

    public String name() {
        return name;
    }

    public boolean loops() {
        return loops;
    }

    public int length() {
        return samples.length;
    }

    public float sample(final int index) {
        return samples[index];
    }

    public double seconds() {
        return (double) samples.length / RATE;
    }

    /** The loudest sample, 0 to 1. */
    public double peak() {
        double peak = 0;
        for (final float s : samples) {
            peak = Math.max(peak, Math.abs(s));
        }
        return peak;
    }

    /** The average loudness, as a root mean square, 0 to 1. */
    public double rms() {
        double sum = 0;
        for (final float s : samples) {
            sum += s * s;
        }
        return samples.length == 0 ? 0 : Math.sqrt(sum / samples.length);
    }

    /** As 16-bit samples, the form a sound card takes. */
    public short[] pcm16() {
        final short[] out = new short[samples.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = Wav.toShort(samples[i]);
        }
        return out;
    }

    @Override
    public String toString() {
        return name;
    }
}
