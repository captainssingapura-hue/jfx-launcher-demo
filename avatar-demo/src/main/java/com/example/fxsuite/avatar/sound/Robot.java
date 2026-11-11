package com.example.fxsuite.avatar.sound;

import com.example.fxsuite.avatar.sim.Num;
import com.example.fxsuite.avatar.sim.Surface;
import com.example.fxsuite.avatar.sound.Dsp.Filter;
import com.example.fxsuite.avatar.sound.Dsp.Noise;

import static com.example.fxsuite.avatar.sound.Dsp.glide;
import static com.example.fxsuite.avatar.sound.Dsp.hit;
import static com.example.fxsuite.avatar.sound.Dsp.samples;
import static com.example.fxsuite.avatar.sound.Dsp.time;

/**
 * The robot profile, the first: sounds as a small machine of metal and motors might make them,
 * as near to real as noise, filters and tones get. Feet thud and click, the pack clunks and
 * buckles, servos whine, and the jets roar, hiss and whine, rising smoothly with the thrust.
 *
 * <p>Worked out rather than recorded: nothing to license or ship, and each sound can be made
 * harder or softer, or a little different each time, by a number. Pure: the same arguments
 * always give the same samples. {@code variant} seeds the noise and nudges the pitch, so ten
 * footsteps in a row are not ten copies of one.
 */
public final class Robot implements Profile {

    private static final double TAU = 2 * Math.PI;

    Robot() {
    }

    @Override
    public String id() {
        return "robot";
    }

    @Override
    public String title() {
        return "Robot";
    }

    @Override
    public String about() {
        return "Metal and motors, as real as can be made: thuds, clunks, servo whine, roaring jets.";
    }

    @Override
    public Clip render(final Sound sound, final double amount, final int variant) {
        return switch (sound) {
            case STEP_GROUND -> step(Surface.GROUND, amount, variant);
            case STEP_BLOCK -> step(Surface.BLOCK, amount, variant);
            case JUMP -> jump(variant);
            case LANDING -> landing(Surface.GROUND, amount, variant);
            case LANDING_BLOCK -> landing(Surface.BLOCK, amount, variant);
            case BUMP -> bump(amount, variant);
            case PACK_ON -> packOn(variant);
            case PACK_OFF -> packOff(variant);
            case WINGS_OPEN -> wings(true, variant);
            case WINGS_FOLD -> wings(false, variant);
            case IGNITION -> ignition(variant);
            case CUT -> cut(variant);
            case GATE -> gate(amount, variant);
            case JET_RUMBLE -> jetRumble();
            case JET_HISS -> jetHiss();
            case JET_WHINE -> jetWhine();
            case WIND -> wind();
        };
    }

    /**
     * The roar is there whenever the jets fire, and grows and deepens less than the rest; the
     * hiss is faint at a hover and fierce at full boost; the turbines' whine climbs with the
     * thrust. No wind at a walk, a rush at full boost.
     */
    @Override
    public Level level(final Sound loop, final double thrust, final double speed) {
        if (loop == Sound.WIND) {
            final double pace = Profile.pace(speed);
            final double gain = Math.pow(Num.clamp((pace - 0.2) / 0.8, 0, 1), 1.4);
            return gain <= 0 ? Level.OFF : new Level(0.9 * gain, 0.7 + 0.5 * pace);
        }
        if (thrust <= 0) {
            return Level.OFF;
        }
        return switch (loop) {
            case JET_RUMBLE -> new Level(0.35 + 0.65 * thrust, 0.8 + 0.45 * thrust);
            case JET_HISS -> new Level(0.08 + 0.8 * thrust * thrust, 0.9 + 0.3 * thrust);
            case JET_WHINE -> new Level(0.2 + 0.25 * thrust, 0.75 + 0.6 * thrust);
            default -> throw new IllegalArgumentException(loop + " is not a loop");
        };
    }

    /**
     * A footfall: a heel and then a toe. On the ground, a soft crunch over a dull thump; on a
     * block, a hard click and a short wooden knock. {@code weight}, 0 to 1, from a stroll to a
     * sprint.
     */
    public static Clip step(final Surface surface, final double weight, final int variant) {
        final Noise noise = new Noise(variant * 31L + surface.ordinal());
        final double tune = 1 + 0.08 * noise.next();
        final double toe = 0.03 + 0.012 * noise.next();
        final double[] y = new double[samples(0.2)];
        if (surface == Surface.GROUND) {
            final Filter crunch = new Filter(950 * tune, 0.9);
            final Filter dirt = new Filter(240 * tune, 0.7);
            double phase = 0;
            for (int i = 0; i < y.length; i++) {
                final double t = time(i);
                final double strike = hit(t, 0.002, 0.028) + 0.55 * hit(t - toe, 0.002, 0.022);
                final double w = noise.next();
                crunch.push(w);
                dirt.push(w);
                phase += TAU * 80 * tune / Dsp.RATE;
                y[i] = (crunch.band + 1.6 * dirt.low) * strike + 0.5 * Math.sin(phase) * hit(t, 0.003, 0.03);
            }
        } else {
            final Filter click = new Filter(2700 * tune, 1.1);
            final Filter knock = new Filter(280 * tune, 9);
            for (int i = 0; i < y.length; i++) {
                final double t = time(i);
                final double strike = hit(t, 0.0005, 0.004) + 0.6 * hit(t - toe, 0.0005, 0.003);
                final double w = noise.next() * strike;
                click.push(w);
                knock.push(w);
                y[i] = 1.4 * click.band + 2.5 * knock.band;
            }
        }
        return new Clip(name("step-" + surface.name().toLowerCase(), weight, variant),
                Dsp.finish(y, 0.22 + 0.3 * weight), false);
    }

    /** Pushing off: a scuff of the feet and the whoosh of the body going up. */
    public static Clip jump(final int variant) {
        final Noise noise = new Noise(1000L + variant);
        final double[] y = new double[samples(0.42)];
        final Filter whoosh = new Filter(400, 1.3);
        final Filter scuff = new Filter(1600, 0.8);
        for (int i = 0; i < y.length; i++) {
            final double t = time(i);
            final double w = noise.next();
            whoosh.tune(400 * Math.pow(4, Math.min(1, t / 0.3)), 1.3).push(w);
            scuff.push(w);
            y[i] = whoosh.band * hit(t, 0.07, 0.1) + 0.6 * scuff.band * hit(t, 0.001, 0.02);
        }
        return new Clip(name("jump", 1, variant), Dsp.finish(y, 0.32), false);
    }

    /**
     * Coming down: a thump that drops in pitch, with on the ground a puff of dust and a scuff,
     * and on a block a hard crack and the block's own short, hollow knock. {@code impact}, 0
     * to 1, from a hop to a fall from on high: harder is louder, deeper and longer.
     */
    public static Clip landing(final Surface surface, final double impact, final int variant) {
        final Noise noise = new Noise(2000L + variant + (surface == Surface.BLOCK ? 500 : 0));
        final double tune = 1 + 0.06 * noise.next();
        final double[] y = new double[samples(0.25 + 0.3 * impact)];
        final Filter dust = new Filter(300 + 1000 * impact, 0.7);
        final Filter scuff = new Filter(1700, 0.9);
        final Filter crack = new Filter(2600 * tune, 1.2);
        final Filter knock = new Filter((210 - 50 * impact) * tune, 7);
        double phase = 0;
        for (int i = 0; i < y.length; i++) {
            final double t = time(i);
            final double w = noise.next();
            phase += TAU * (45 + 85 * Math.exp(-t / 0.05)) * tune / Dsp.RATE;
            final double thump = Math.sin(phase) * hit(t, 0.002, 0.06 + 0.12 * impact);
            if (surface == Surface.GROUND) {
                dust.push(w);
                scuff.push(w);
                y[i] = thump + 1.4 * dust.low * hit(t, 0.002, 0.04 + 0.06 * impact)
                        + 0.5 * impact * scuff.band * hit(t, 0.001, 0.015);
            } else {
                final double strike = w * hit(t, 0.0005, 0.005);
                crack.push(strike);
                knock.push(strike);
                y[i] = 0.7 * thump + (0.6 + 0.6 * impact) * crack.band + 3 * knock.band;
            }
        }
        final String what = surface == Surface.GROUND ? "landing" : "landing-block";
        return new Clip(name(what, impact, variant), Dsp.finish(y, 0.3 + 0.6 * impact), false);
    }

    /**
     * Through a gate: the rush of the air past its bars, rising and falling as they go by, and
     * a bright bell-like chime to say well done. Faster ({@code speed} 0 to 1, of full boost)
     * makes the rush louder and quicker.
     */
    public static Clip gate(final double speed, final int variant) {
        final Noise noise = new Noise(10_000L + variant);
        final double tune = 1 + 0.02 * noise.next();
        final double pass = 0.2 - 0.1 * speed;
        final double[] y = new double[samples(1.0)];
        final Filter rush = new Filter(1000, 1.5);
        for (int i = 0; i < y.length; i++) {
            final double t = time(i);
            // the rush sweeps down as the bars go by, like a car passing
            rush.tune(2200 * Math.pow(0.3, Math.min(1, t / (2 * pass))), 1.5).push(noise.next());
            final double swell = Math.exp(-Math.pow((t - pass) / (0.6 * pass), 2));
            // a bell: a tone and its bell-like, not-quite-harmonic partials, each dying away
            final double bell = hit(t - 0.03, 0.002, 0.35) * (Math.sin(TAU * 1568 * tune * (t - 0.03))
                    + 0.5 * Math.sin(TAU * 1568 * 2.76 * tune * (t - 0.03)) * hit(t - 0.03, 0.002, 0.12)
                    + 0.3 * Math.sin(TAU * 2349 * tune * (t - 0.03)));
            y[i] = (0.6 + 0.8 * speed) * rush.band * swell * Math.min(1, t / 0.01) + 0.35 * bell;
        }
        return new Clip(name("gate", speed, variant), Dsp.finish(y, 0.4 + 0.3 * speed), false);
    }

    /** Running into something: a dull knock, harder with {@code impact}, 0 to 1. */
    public static Clip bump(final double impact, final int variant) {
        final Noise noise = new Noise(3000L + variant);
        final double[] y = new double[samples(0.26)];
        final Filter thud = new Filter(650, 0.8);
        final Filter rattle = new Filter(2300, 1.5);
        double phase = 0;
        for (int i = 0; i < y.length; i++) {
            final double t = time(i);
            final double w = noise.next();
            thud.push(w);
            rattle.push(w);
            phase += TAU * (70 + 80 * Math.exp(-t / 0.03)) / Dsp.RATE;
            y[i] = Math.sin(phase) * hit(t, 0.001, 0.05)
                    + 1.2 * thud.low * hit(t, 0.001, 0.03)
                    + 0.4 * impact * rattle.band * hit(t - 0.012, 0.001, 0.025);
        }
        return new Clip(name("bump", impact, variant), Dsp.finish(y, 0.25 + 0.5 * impact), false);
    }

    /** Putting the pack on: two buckles click shut, then it settles on the back with a clunk. */
    public static Clip packOn(final int variant) {
        final Noise noise = new Noise(4000L + variant);
        final double[] y = new double[samples(0.45)];
        final Filter weight = new Filter(500, 0.7);
        for (int i = 0; i < y.length; i++) {
            final double t = time(i);
            final double w = noise.next();
            weight.push(w);
            y[i] = click(t, 3100, w) + click(t - 0.075, 3500, w)
                    + clunk(t - 0.15, 165) + 0.8 * weight.low * hit(t - 0.15, 0.002, 0.03);
        }
        return new Clip(name("pack-on", 1, variant), Dsp.finish(y, 0.45), false);
    }

    /** Taking it off: the buckles snap open, it drops off the back, the straps slide away. */
    public static Clip packOff(final int variant) {
        final Noise noise = new Noise(5000L + variant);
        final double[] y = new double[samples(0.5)];
        final Filter straps = new Filter(1200, 1.2);
        for (int i = 0; i < y.length; i++) {
            final double t = time(i);
            final double w = noise.next();
            straps.tune(1400 - 700 * Math.min(1, Math.max(0, t - 0.12) / 0.3), 1.2).push(w);
            y[i] = click(t, 2800, w) + click(t - 0.04, 2600, w)
                    + 0.8 * clunk(t - 0.09, 130)
                    + 0.35 * straps.band * Math.sin(Math.PI * Math.min(1, Math.max(0, t - 0.12) / 0.32));
        }
        return new Clip(name("pack-off", 1, variant), Dsp.finish(y, 0.42), false);
    }

    /**
     * The wings' servo motors, opening (rising in pitch) or folding (falling), with the buzz of
     * their gears, and a click as they lock. About as long as the wings take to move.
     */
    public static Clip wings(final boolean opening, final int variant) {
        final Noise noise = new Noise(6000L + variant);
        final double run = 0.62;
        final double[] y = new double[samples(0.72)];
        final Filter thunk = new Filter(700, 0.7);
        double phase = 0;
        for (int i = 0; i < y.length; i++) {
            final double t = time(i);
            final double u = t / run;
            final double pitch = opening ? glide(260, 470, u) : glide(470, 260, u);
            phase += TAU * pitch / Dsp.RATE;
            final double motor = Math.sin(phase) + 0.5 * Math.sin(2 * phase) + 0.33 * Math.sin(3 * phase);
            final double gears = 0.75 + 0.25 * Math.sin(TAU * 55 * t);
            final double on = Math.min(1, t / 0.03) * Math.max(0, Math.min(1, (run - t) / 0.05));
            final double w = noise.next();
            thunk.push(w);
            y[i] = 0.3 * motor * gears * on + click(t - run, opening ? 2300 : 1900, w)
                    + 0.6 * thunk.low * hit(t - run, 0.001, 0.02);
        }
        return new Clip(name(opening ? "wings-open" : "wings-fold", 1, variant), Dsp.finish(y, 0.26), false);
    }

    /** Taking off: the igniters tick, then the engines light with a whump. */
    public static Clip ignition(final int variant) {
        final Noise noise = new Noise(7000L + variant);
        final double[] y = new double[samples(1.0)];
        final Filter ticks = new Filter(3200, 1);
        final Filter whump = new Filter(150, 0.8);
        double phase = 0;
        for (int i = 0; i < y.length; i++) {
            final double t = time(i);
            final double w = noise.next();
            ticks.push(w);
            whump.tune(150 * Math.pow(16, Math.min(1, Math.max(0, t - 0.1) / 0.25)), 0.8).push(w);
            phase += TAU * 55 / Dsp.RATE;
            final double tick = hit(t, 0.0003, 0.003) + hit(t - 0.04, 0.0003, 0.003) + hit(t - 0.08, 0.0003, 0.003);
            final double swell = hit(t - 0.1, 0.06, 0.25);
            y[i] = 0.7 * ticks.band * tick + (1.5 * whump.low + 0.5 * Math.sin(phase)) * swell;
        }
        return new Clip(name("ignition", 1, variant), Dsp.finish(y, 0.7), false);
    }

    /** Stopping flight: the engines cut with a pop, and spool down. */
    public static Clip cut(final int variant) {
        final Noise noise = new Noise(8000L + variant);
        final double[] y = new double[samples(1.4)];
        final Filter pop = new Filter(1000, 0.7);
        final Filter rumble = new Filter(400, 0.7);
        double phase = 0;
        for (int i = 0; i < y.length; i++) {
            final double t = time(i);
            final double w = noise.next();
            pop.push(w);
            rumble.tune(80 + 320 * Math.exp(-t / 0.4), 0.7).push(w);
            phase += TAU * (150 + 1100 * Math.exp(-t / 0.45)) / Dsp.RATE;
            y[i] = 1.2 * pop.low * hit(t, 0.001, 0.02)
                    + 0.25 * Math.sin(phase) * hit(t, 0.005, 0.5)
                    + 1.5 * rumble.low * hit(t, 0.005, 0.35);
        }
        return new Clip(name("cut", 1, variant), Dsp.finish(y, 0.6), false);
    }

    // ---- the loops, two to four seconds each; how loud and how high they play is set live

    /** The jets' deep roar: brown noise, low-passed, with a slight flutter. */
    public static Clip jetRumble() {
        final int length = samples(2);
        final Noise noise = new Noise(9000);
        final Filter low = new Filter(170, 0.7);
        final double[] raw = new double[length + samples(0.25)];
        double brown = 0;
        for (int i = 0; i < raw.length; i++) {
            brown = 0.995 * brown + 0.1 * noise.next();
            raw[i] = low.push(brown).low;
        }
        final double[] y = Dsp.seamless(raw, length);
        for (int i = 0; i < length; i++) {
            y[i] *= 1 + 0.12 * Math.sin(TAU * 25 * time(i));      // 50 whole flutters in 2 s
        }
        return new Clip("jet-rumble", Dsp.normalized(y, 0.5), true);
    }

    /** The rush of the exhaust: hissing noise, band-passed high. */
    public static Clip jetHiss() {
        final int length = samples(2);
        final Noise noise = new Noise(9100);
        final Filter high = new Filter(1800, 0.7);
        final Filter top = new Filter(7000, 0.7);
        final double[] raw = new double[length + samples(0.25)];
        for (int i = 0; i < raw.length; i++) {
            raw[i] = top.push(high.push(noise.next()).high).low;
        }
        return new Clip("jet-hiss", Dsp.normalized(Dsp.seamless(raw, length), 0.35), true);
    }

    /** The turbines' whine: a pure tone and its octave, gently beating. */
    public static Clip jetWhine() {
        final double[] y = new double[samples(2)];
        for (int i = 0; i < y.length; i++) {
            final double t = time(i);
            // whole numbers of cycles in 2 s, so the loop meets itself exactly
            y[i] = Math.sin(TAU * 1180 * t) + 0.35 * Math.sin(TAU * 2360 * t) + 0.2 * Math.sin(TAU * 1187 * t);
        }
        return new Clip("jet-whine", Dsp.normalized(y, 0.15), true);
    }

    /** Air rushing past: broad noise with a faint whistle, rising and falling in gusts. */
    public static Clip wind() {
        final int length = samples(4);
        final Noise noise = new Noise(9200);
        final Filter body = new Filter(450, 0.6);
        final Filter whistle = new Filter(1300, 2.5);
        final double[] raw = new double[length + samples(0.4)];
        for (int i = 0; i < raw.length; i++) {
            final double w = noise.next();
            raw[i] = body.push(w).band + 0.3 * whistle.push(w).band;
        }
        final double[] y = Dsp.seamless(raw, length);
        for (int i = 0; i < length; i++) {
            final double t = time(i);
            // one and three whole gusts in 4 s
            y[i] *= 0.75 + 0.15 * Math.sin(TAU * 0.25 * t) + 0.1 * Math.sin(TAU * 0.75 * t + 1);
        }
        return new Clip("wind", Dsp.normalized(y, 0.4), true);
    }

    /** A buckle or a latch: a tiny burst of noise ringing at {@code pitch}. */
    private static double click(final double t, final double pitch, final double noise) {
        if (t < 0) {
            return 0;
        }
        return 0.6 * Math.sin(TAU * pitch * t) * hit(t, 0.0003, 0.012) + 0.5 * noise * hit(t, 0.0001, 0.0015);
    }

    /** Something heavy settling: a short low tone with its fifth. */
    private static double clunk(final double t, final double pitch) {
        if (t < 0) {
            return 0;
        }
        return (Math.sin(TAU * pitch * t) + 0.5 * Math.sin(TAU * pitch * 1.5 * t)) * hit(t, 0.002, 0.05);
    }

    private static String name(final String what, final double amount, final int variant) {
        return String.format("%s-%02d-v%d", what, Math.round(amount * 100), variant);
    }
}
