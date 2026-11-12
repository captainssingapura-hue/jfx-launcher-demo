package com.example.fxsuite.avatar.sound;

import com.example.fxsuite.avatar.sim.Num;
import com.example.fxsuite.avatar.sound.Dsp.Noise;

import static com.example.fxsuite.avatar.sound.Dsp.samples;
import static com.example.fxsuite.avatar.sound.Dsp.time;

/**
 * The arcade profile: sounds as an old games console's sound chip would make them. There are
 * only square waves of a few widths, a coarse triangle, and noise. Pitch and loudness change
 * in steps, sixty times a second, the way the chip's clock moved them. Loudness has just
 * sixteen levels. A jump is a blip sweeping up, a landing a burst of noise, and putting the
 * pack on plays a little rising arpeggio. The jets buzz, and climb in semitone steps as the
 * thrust rises.
 *
 * <p>The same promises as every profile: pure, within full scale, and click-free at the ends
 * except where a step is the point. A {@code variant} detunes a take slightly and reseeds its noise.
 */
public final class Arcade implements Profile {

    /** The chip's clock: one tick a frame, at sixty frames a second. */
    private static final double FRAME = 1.0 / 60;

    Arcade() {
    }

    @Override
    public String id() {
        return "arcade";
    }

    @Override
    public String title() {
        return "Arcade";
    }

    @Override
    public String about() {
        return "An old console's sound chip: square-wave blips, noise bursts, arpeggios, stepping pitch.";
    }

    @Override
    public Clip render(final Sound sound, final double amount, final int variant) {
        return switch (sound) {
            case STEP_GROUND -> step(false, amount, variant);
            case STEP_BLOCK -> step(true, amount, variant);
            case JUMP -> jump(variant);
            case LANDING -> landing(amount, variant);
            case LANDING_BLOCK -> landingOnBlock(amount, variant);
            case BUMP -> bump(amount, variant);
            case PACK_ON -> arpeggio("pack-on", new double[] {523.25, 659.25, 783.99, 1046.5}, variant);
            case PACK_OFF -> arpeggio("pack-off", new double[] {1046.5, 783.99, 659.25, 523.25}, variant);
            case WINGS_OPEN -> wings(true, variant);
            case WINGS_FOLD -> wings(false, variant);
            case IGNITION -> ignition(variant);
            case CUT -> cut(variant);
            case GATE -> gate(amount, variant);
            case RIFLE_SHOT -> rifleShot(variant);
            case RIFLES_DRAWN -> riflesDrawn(variant);
            case GLAIVE_CLEAVE -> glaiveStroke(true, variant);
            case GLAIVE_SWEEP -> glaiveStroke(false, variant);
            case GLAIVE_DRAWN -> arpeggio("glaive-drawn", new double[] {659.25, 987.77, 1318.51, 1975.53}, variant);
            case ROUND_ON_BLOCK -> impact("round-on-block", 16_000, 2, 1760, 1, 0.3, variant);
            case ROUND_ON_GROUND -> impact("round-on-ground", 3500, 3, 0, 0, 0.3, variant);
            case BLADE_ON_BLOCK -> clink(variant);
            case BLADE_ON_GROUND -> impact("blade-on-ground", 2000, 6, 110, 5, 0.45, variant);
            case GATLING_DRAWN -> arpeggio("gatling-drawn", new double[] {110, 146.83, 220, 220}, variant);
            case GATLING_SPIN -> gatlingSpin();
            case GATLING_FIRE -> gatlingFire();
            case JET_RUMBLE -> noiseLoop("jet-rumble", 2400, 9300, 0.45, false);
            case JET_HISS -> noiseLoop("jet-hiss", 14_000, 9400, 0.25, false);
            case JET_WHINE -> jetWhine();
            case WIND -> noiseLoop("wind", 7000, 9500, 0.3, true);
        };
    }

    /**
     * Stepped, as the chip did: loudness in sixteen levels, and pitch in whole semitones, so the
     * jets climb a scale as the thrust rises rather than gliding.
     */
    @Override
    public Level level(final Sound loop, final Drive drive) {
        final double thrust = drive.thrust();
        final double speed = drive.speed();
        if (loop == Sound.GATLING_SPIN) {
            return drive.spin() <= 0 ? Level.OFF : new Level(volume(0.2 + 0.4 * drive.spin()), semitones(0.5 + drive.spin()));
        }
        if (loop == Sound.GATLING_FIRE) {
            return drive.firing() ? new Level(volume(0.8), 1) : Level.OFF;
        }
        if (loop == Sound.WIND) {
            final double pace = Profile.pace(speed);
            final double gain = volume(0.8 * Math.pow(Num.clamp((pace - 0.2) / 0.8, 0, 1), 1.2));
            return gain <= 0 ? Level.OFF : new Level(gain, semitones(0.8 + 0.4 * pace));
        }
        if (thrust <= 0) {
            return Level.OFF;
        }
        return switch (loop) {
            case JET_RUMBLE -> new Level(volume(0.4 + 0.6 * thrust), semitones(0.8 + 0.6 * thrust));
            case JET_HISS -> new Level(volume(0.1 + 0.7 * thrust * thrust), 1);
            case JET_WHINE -> new Level(volume(0.3), semitones(1 + thrust));
            default -> throw new IllegalArgumentException(loop + " is not a loop");
        };
    }

    // ---- the one-shots

    /** A footfall: a short burst of noise, low on the ground; on a block, higher, with a tick. */
    private static Clip step(final boolean block, final double weight, final int variant) {
        final Noise seed = new Noise(variant * 31L + (block ? 1 : 0));
        final Chip noise = new Chip(seed);
        final double tune = tune(variant);
        final double[] y = new double[samples(0.09)];
        for (int i = 0; i < y.length; i++) {
            final double t = time(i);
            double s = noise.next((block ? 15_000 : 5000) * tune) * fall(t, block ? 3 : 4);
            if (block && t < FRAME) {
                s += 0.6 * square(1320 * tune * t, 0.5);
            }
            y[i] = s * onset(t);
        }
        return clip(block ? "step-block" : "step-ground", weight, variant, y, 0.2 + 0.25 * weight);
    }

    /** A blip sweeping up two octaves, a frame at a time. */
    private static Clip jump(final int variant) {
        final double tune = tune(variant);
        final double[] y = new double[samples(0.22)];
        double phase = 0;
        for (int i = 0; i < y.length; i++) {
            final double t = time(i);
            phase += 280 * tune * Math.pow(2, 2 * Math.min(1, tick(t) / 0.16)) / Dsp.RATE;
            y[i] = square(phase, 0.25) * volume(1 - 0.6 * tick(t) / 0.22) * onset(t);
        }
        return clip("jump", 1, variant, y, 0.3);
    }

    /** A burst of noise over a falling thud; harder lands lower, longer and louder. */
    private static Clip landing(final double impact, final int variant) {
        final Chip noise = new Chip(new Noise(2000L + variant));
        final double tune = tune(variant);
        final double length = 0.12 + 0.25 * impact;
        final double[] y = new double[samples(length)];
        double phase = 0;
        for (int i = 0; i < y.length; i++) {
            final double t = time(i);
            final double rate = (3500 - 2000 * impact) * tune * (1 - 0.5 * tick(t) / length);
            phase += (45 + 105 * Math.max(0, 1 - tick(t) / 0.1)) / Dsp.RATE;
            final double left = volume(1 - tick(t) / length);
            y[i] = (0.7 * noise.next(rate) + 0.8 * triangle(phase)) * left * onset(t);
        }
        return clip("landing", impact, variant, y, 0.3 + 0.55 * impact);
    }

    /**
     * Onto a block: a hollow "tock", a square blip falling a fifth, over a short crack of
     * high noise and the same triangle thud as on the ground, but cut short.
     */
    private static Clip landingOnBlock(final double impact, final int variant) {
        final Chip noise = new Chip(new Noise(2500L + variant));
        final double tune = tune(variant);
        final double length = 0.1 + 0.15 * impact;
        final double[] y = new double[samples(length)];
        double tock = 0;
        double thud = 0;
        for (int i = 0; i < y.length; i++) {
            final double t = time(i);
            tock += (t < 2 * FRAME ? 660 : 440) * (1 - 0.3 * impact) * tune / Dsp.RATE;
            thud += (45 + 105 * Math.max(0, 1 - tick(t) / 0.06)) / Dsp.RATE;
            final double crack = noise.next(13_000) * fall(t, 2);
            y[i] = (0.7 * square(tock, 0.5) * fall(t, 5) + 0.5 * crack + 0.6 * triangle(thud) * fall(t, 4))
                    * onset(t);
        }
        return clip("landing-block", impact, variant, y, 0.3 + 0.55 * impact);
    }

    /**
     * Through a gate: three quick notes climbing (G, D, G an octave up), the last held a moment,
     * over a whoosh of noise as loud as the gate was taken fast.
     */
    private static Clip gate(final double speed, final int variant) {
        final Chip noise = new Chip(new Noise(10_000L + variant));
        final double tune = tune(variant);
        final double[] notes = {783.99, 1174.66, 1567.98};
        final double note = 2 * FRAME;
        final double[] y = new double[samples(2 * note + 0.3)];
        double phase = 0;
        for (int i = 0; i < y.length; i++) {
            final double t = time(i);
            final int which = Math.min(notes.length - 1, (int) (t / note));
            final double since = t - which * note;
            phase += notes[which] * tune / Dsp.RATE;
            final double level = which < notes.length - 1 ? 1 : volume(1 - since / 0.3);
            final double whoosh = speed * noise.next(9000 - 6000 * Math.min(1, tick(t) / 0.2)) * fall(t, 12);
            y[i] = (square(phase, 0.25) * level + 0.5 * whoosh) * onset(t);
        }
        return clip("gate", speed, variant, y, 0.35 + 0.3 * speed);
    }

    /** A low square blip dropping an octave, with a crack of noise as hard as the bump. */
    private static Clip bump(final double impact, final int variant) {
        final Chip noise = new Chip(new Noise(3000L + variant));
        final double tune = tune(variant);
        final double[] y = new double[samples(0.12)];
        double phase = 0;
        for (int i = 0; i < y.length; i++) {
            final double t = time(i);
            phase += 150 * tune * (1 - 0.5 * Math.min(1, tick(t) / 0.1)) / Dsp.RATE;
            final double crack = t < 2 * FRAME ? impact * noise.next(2500) : 0;
            y[i] = (square(phase, 0.5) * fall(t, 7) + 0.6 * crack) * onset(t);
        }
        return clip("bump", impact, variant, y, 0.25 + 0.5 * impact);
    }

    /** Four notes, three frames each, the last left to ring out: up to put on, down to take off. */
    private static Clip arpeggio(final String name, final double[] notes, final int variant) {
        final double tune = tune(variant);
        final double note = 3 * FRAME;
        final double[] y = new double[samples(note * (notes.length - 1) + 0.12)];
        double phase = 0;
        for (int i = 0; i < y.length; i++) {
            final double t = time(i);
            final int which = Math.min(notes.length - 1, (int) (t / note));
            final double since = t - which * note;
            final boolean last = which == notes.length - 1;
            phase += notes[which] * tune / Dsp.RATE;
            y[i] = square(phase, 0.125) * volume(1 - since / (last ? 0.12 : 2 * note)) * onset(t);
        }
        return clip(name, 1, variant, y, 0.35);
    }

    /**
     * The servos as a stepping tone, an octave up (opening) or down (folding) in eight steps,
     * its width flicking each frame for a buzz, and a tick of noise as the wings lock.
     */
    private static Clip wings(final boolean opening, final int variant) {
        final Chip noise = new Chip(new Noise(6000L + variant));
        final double tune = tune(variant);
        final double run = 0.6;
        final double[] y = new double[samples(run + 2 * FRAME)];
        double phase = 0;
        for (int i = 0; i < y.length; i++) {
            final double t = time(i);
            final int step = Math.min(7, (int) (t / (run / 8)));
            final double pitch = opening ? 330 * Math.pow(2, step / 8.0) : 660 * Math.pow(2, -step / 8.0);
            phase += pitch * tune / Dsp.RATE;
            final double duty = ((int) (t / FRAME)) % 2 == 0 ? 0.25 : 0.5;
            y[i] = (t < run ? 0.7 * square(phase, duty) : 0.8 * noise.next(12_000) * fall(t - run, 2)) * onset(t);
        }
        return clip(opening ? "wings-open" : "wings-fold", 1, variant, y, 0.25);
    }

    /** A rising trill as the igniters fire, and the noise of the jets swelling up under it. */
    private static Clip ignition(final int variant) {
        final Chip noise = new Chip(new Noise(7000L + variant));
        final double tune = tune(variant);
        final double[] y = new double[samples(0.7)];
        double phase = 0;
        for (int i = 0; i < y.length; i++) {
            final double t = time(i);
            final double rate = (1500 + 7500 * Math.min(1, tick(t) / 0.5)) * tune;
            final double swell = volume(Math.min(1, tick(t) / 0.2)) * (t > 0.5 ? 1 - (t - 0.5) / 0.2 : 1);
            final boolean high = ((int) (t / (2 * FRAME))) % 2 == 1;
            phase += (high ? 660 : 440) * tune / Dsp.RATE;
            final double trill = t < 0.24 ? 0.6 * square(phase, 0.25) : 0;
            y[i] = (noise.next(rate) * swell + trill) * onset(t);
        }
        return clip("ignition", 1, variant, y, 0.65);
    }

    /** The engines dying: a tone stepping down three octaves, over a puff of noise. */
    private static Clip cut(final int variant) {
        final Chip noise = new Chip(new Noise(8000L + variant));
        final double tune = tune(variant);
        final double[] y = new double[samples(0.9)];
        double phase = 0;
        for (int i = 0; i < y.length; i++) {
            final double t = time(i);
            phase += 880 * tune * Math.pow(2, -3 * Math.min(1, tick(t) / 0.8)) / Dsp.RATE;
            y[i] = (square(phase, 0.5) * volume(1 - tick(t) / 0.9) + 0.6 * noise.next(3000) * fall(t, 6)) * onset(t);
        }
        return clip("cut", 1, variant, y, 0.55);
    }

    // ---- the weapons

    /** A round: a burst of noise falling fast in pitch, over a low square thump. */
    private static Clip rifleShot(final int variant) {
        final Chip noise = new Chip(new Noise(11_000L + variant));
        final double tune = tune(variant);
        final double[] y = new double[samples(0.13)];
        double phase = 0;
        for (int i = 0; i < y.length; i++) {
            final double t = time(i);
            final double rate = 12_000 * tune * Math.pow(0.25, Math.min(1, tick(t) / (5 * FRAME)));
            phase += 140 * tune * (1 - 0.5 * Math.min(1, tick(t) / (4 * FRAME))) / Dsp.RATE;
            y[i] = (noise.next(rate) * fall(t, 7) + 0.6 * square(phase, 0.5) * fall(t, 4)) * onset(t);
        }
        return clip("rifle-shot", 1, variant, y, 0.6);
    }

    /** Drawing the rifles: two short clicks each, a high one and a lower one, one rifle then the other. */
    private static Clip riflesDrawn(final int variant) {
        final double tune = tune(variant);
        final double[] y = new double[samples(0.4)];
        final double[] at = {0, 0.07, 0.2, 0.27};
        final double[] pitch = {1320, 880, 1320, 880};
        final double[] phases = new double[at.length];
        for (int i = 0; i < y.length; i++) {
            final double t = time(i);
            double s = 0;
            for (int k = 0; k < at.length; k++) {
                if (t >= at[k] && t < at[k] + 2 * FRAME) {
                    phases[k] += pitch[k] * tune / Dsp.RATE;
                    s += square(phases[k], 0.25) * fall(t - at[k], 2);
                }
            }
            y[i] = s * onset(t);
        }
        return clip("rifles-drawn", 1, variant, y, 0.35);
    }

    /**
     * The glaive's swing: chip noise sweeping up and back down as the blade comes through, a
     * frame at a time. The cleave sweeps lower and longer than the sweep.
     */
    private static Clip glaiveStroke(final boolean cleave, final int variant) {
        final Chip noise = new Chip(new Noise((cleave ? 13_000L : 13_500L) + variant));
        final double tune = tune(variant);
        final double length = cleave ? 0.3 : 0.24;
        final double base = (cleave ? 1800 : 2800) * tune;
        final double[] y = new double[samples(length + 2 * FRAME)];
        for (int i = 0; i < y.length; i++) {
            final double t = time(i);
            final double through = Math.sin(Math.PI * Math.min(1, tick(t) / length));
            y[i] = noise.next(base * (1 + 3 * through)) * volume(through) * onset(t);
        }
        return clip(cleave ? "glaive-cleave" : "glaive-sweep", 1, variant, y, cleave ? 0.45 : 0.38);
    }

    // ---- what the weapons strike

    /**
     * A strike: a burst of noise at {@code rate}, falling silent over {@code frames}, over a
     * square blip at {@code pitch} for {@code blips} frames (none if 0). High and short for a
     * round on a block; low for the ground; long and deep for the blade's chop.
     */
    private static Clip impact(final String name, final double rate, final int frames, final double pitch,
                               final int blips, final double peak, final int variant) {
        final Chip noise = new Chip(new Noise(15_000L + name.hashCode() + variant));
        final double tune = tune(variant);
        final double[] y = new double[samples((Math.max(frames, blips) + 1) * FRAME)];
        double phase = 0;
        for (int i = 0; i < y.length; i++) {
            final double t = time(i);
            phase += pitch * tune * (1 - 0.4 * Math.min(1, tick(t) / (blips * FRAME + 1e-9))) / Dsp.RATE;
            final double blip = pitch > 0 ? 0.7 * square(phase, 0.5) * fall(t, blips) : 0;
            y[i] = (noise.next(rate * tune) * fall(t, frames) + blip) * onset(t);
        }
        return clip(name, 1, variant, y, peak);
    }

    /** The blade striking a block: two high notes at once, a narrow pulse, dying over a few frames. */
    private static Clip clink(final int variant) {
        final double tune = tune(variant);
        final double[] y = new double[samples(8 * FRAME)];
        for (int i = 0; i < y.length; i++) {
            final double t = time(i);
            y[i] = (square(1318.51 * tune * t, 0.125) + square(1975.53 * tune * t, 0.125)) * fall(t, 7) * onset(t);
        }
        return clip("blade-on-block", 1, variant, y, 0.4);
    }

    // ---- the loops

    /** The noise channel at one rate, as a loop; the wind with its gusts. */
    private static Clip noiseLoop(final String name, final double rate, final long seed, final double peak,
                                  final boolean gusts) {
        final double seconds = gusts ? 4 : 2;
        final int length = samples(seconds);
        final Chip noise = new Chip(new Noise(seed));
        final double[] raw = new double[length + samples(0.25)];
        for (int i = 0; i < raw.length; i++) {
            raw[i] = noise.next(rate);
        }
        final double[] y = Dsp.seamless(raw, length);
        if (gusts) {
            for (int i = 0; i < length; i++) {
                final double t = time(i);
                y[i] *= 0.7 + 0.2 * Math.sin(2 * Math.PI * 0.25 * t) + 0.1 * Math.sin(2 * Math.PI * 0.75 * t + 1);
            }
        }
        return new Clip(name, Dsp.normalized(y, peak), true);
    }

    /** The Gatling gun's motor: a narrow pulse buzzing at 330 Hz, 660 whole cycles in 2 s. */
    private static Clip gatlingSpin() {
        final double[] y = new double[samples(2)];
        for (int i = 0; i < y.length; i++) {
            y[i] = square(330 * time(i), 0.125);
        }
        return new Clip("gatling-spin", Dsp.normalized(y, 0.18), true);
    }

    /**
     * The Gatling gun firing: the noise channel chopped on and off forty times a second, a round
     * in each, eighty in the 2 s loop, with the chip's low thump under it.
     */
    private static Clip gatlingFire() {
        final Chip noise = new Chip(new Noise(9600));
        final double[] y = new double[samples(2)];
        final double period = 60.0 / 2400;
        for (int i = 0; i < y.length; i++) {
            final double t = time(i);
            final double since = t % period;
            final double on = since < period * 0.55 ? 1 - since / (period * 0.55) : 0;
            y[i] = (noise.next(7000) + 0.6 * square(80 * t, 0.5)) * on;
        }
        return new Clip("gatling-fire", Dsp.normalized(y, 0.45), true);
    }

    /** A narrow square buzzing at 220 Hz: 440 whole cycles in the 2 s loop, so it meets itself. */
    private static Clip jetWhine() {
        final double[] y = new double[samples(2)];
        for (int i = 0; i < y.length; i++) {
            y[i] = square(220 * time(i), 0.25);
        }
        return new Clip("jet-whine", Dsp.normalized(y, 0.15), true);
    }

    // ---- the chip's parts

    /** The noise channel: a random high or low, held for each period of its rate. */
    private static final class Chip {
        private final Noise noise;
        private double phase = 1;
        private double held;

        Chip(final Noise noise) {
            this.noise = noise;
        }

        double next(final double rate) {
            phase += rate / Dsp.RATE;
            while (phase >= 1) {
                phase -= 1;
                held = noise.next() >= 0 ? 1 : -1;
            }
            return held;
        }
    }

    /**
     * A pulse wave, {@code duty} of each cycle high; {@code cycles} counts the cycles so far.
     * Centred on zero, as the capacitor on a chip's output left it: a narrow pulse otherwise
     * sits mostly below the line, and thumps as it starts and stops.
     */
    private static double square(final double cycles, final double duty) {
        return (cycles - Math.floor(cycles) < duty ? 1 : -1) - (2 * duty - 1);
    }

    /** A triangle in sixteen steps, as the chip drew it. */
    private static double triangle(final double cycles) {
        final double p = cycles - Math.floor(cycles);
        final double v = p < 0.5 ? 4 * p - 1 : 3 - 4 * p;
        return Math.round(v * 7.5) / 7.5;
    }

    /** One of sixteen loudness levels. */
    private static double volume(final double level) {
        return Math.round(Num.clamp(level, 0, 1) * 15) / 15.0;
    }

    /** The nearest whole semitone to a pitch. */
    private static double semitones(final double pitch) {
        return Math.pow(2, Math.round(12 * Math.log(pitch) / Math.log(2)) / 12.0);
    }

    /** The time as the chip saw it: the start of the frame it falls in. */
    private static double tick(final double t) {
        return Math.floor(t / FRAME) * FRAME;
    }

    /** Full, then down a step each frame, silent after {@code frames}. */
    private static double fall(final double t, final int frames) {
        return t < 0 ? 0 : volume(1 - tick(t) / (frames * FRAME));
    }

    /** A millisecond and a half to come in, so even a square wave starts without a crack. */
    private static double onset(final double t) {
        return Math.min(1, t / 0.0015);
    }

    /** A take's slight detuning, within a few percent. */
    private static double tune(final int variant) {
        return 1 + 0.025 * new Noise(variant * 7919L + 17).next();
    }

    private static Clip clip(final String what, final double amount, final int variant, final double[] y,
                             final double peak) {
        return new Clip(String.format("%s-%02d-v%d", what, Math.round(amount * 100), variant), Dsp.finish(y, peak), false);
    }
}
