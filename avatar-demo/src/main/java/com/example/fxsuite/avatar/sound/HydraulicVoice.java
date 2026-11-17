package com.example.fxsuite.avatar.sound;

import com.example.fxsuite.avatar.sim.Actuator;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/**
 * How a hydraulic drive sounds ({@link Actuator}), made as it goes, a block at a time, as the
 * Gatling gun's voice is: what runs, and what happens.
 *
 * <ul>
 *   <li><b>Its pump</b>: an electric motor's hum and its harmonics, spooling up and down with it,
 *       sagging a little under the load of its fluid flowing; and its pistons' ripple, a whine above
 *       it, harder as it works.</li>
 *   <li><b>Its fluid</b>: a hiss through its lines, as loud and as bright as it flows; tightening,
 *       higher and thinner, as it is throttled to cushion the end of its stroke.</li>
 *   <li><b>What happens</b> ({@link Actuator.Event}), each a sound of its own: its pump's relay
 *       clicking in and out; its lock pin drawn, a metallic click and a slide; its valve opening,
 *       a thump and a burst of hiss; cushioned, a soft swell of hiss; stopped, a heavy thud against its
 *       end and a ring; locked, a clunk and a click.</li>
 * </ul>
 *
 * <p>Silent when its pump is still and nothing happens. Pure, for its seed: the same settings and
 * the same events at the same moments, the same samples.
 */
public final class HydraulicVoice {

    /** How quickly it follows what it is set to: about this many seconds, so a block's change makes no click. */
    private static final double FOLLOW = 0.03;

    private final SplittableRandom noise;
    private double spool;
    private double flow;
    private double cushion;
    private double wantSpool;
    private double wantFlow;
    private double wantCushion;
    private double phase;
    private double low;
    private double band;
    private final List<Hit> hits = new ArrayList<>();

    /** Something that happened, sounding: which, and how long ago it began, in samples. */
    private static final class Hit {
        private final Actuator.Event event;
        private int age;

        Hit(final Actuator.Event event) {
            this.event = event;
        }
    }

    public HydraulicVoice(final long seed) {
        noise = new SplittableRandom(seed);
    }

    /** How fast its pump turns ({@code spool}, 0 to 1), how hard its fluid flows ({@code flow}, 0 to 1), and whether it is throttled to cushion. */
    public void set(final double spool, final double flow, final boolean cushioned) {
        wantSpool = Math.clamp(spool, 0, 1);
        wantFlow = Math.clamp(flow, 0, 1);
        wantCushion = cushioned ? 1 : 0;
    }

    /** {@code event}, heard from now. */
    public void hit(final Actuator.Event event) {
        hits.add(new Hit(event));
    }

    /** Whether it is making any sound at all. */
    public boolean sounding() {
        return !hits.isEmpty() || spool > 1e-4 || wantSpool > 0;
    }

    /** {@code count} samples into {@code out} from {@code from}. */
    public void render(final float[] out, final int from, final int count) {
        final double follow = 1 - Math.exp(-1.0 / (FOLLOW * Clip.RATE));
        for (int s = 0; s < count; s++) {
            spool += (wantSpool - spool) * follow;
            flow += (wantFlow - flow) * follow;
            cushion += (wantCushion - cushion) * follow * 0.4;
            final double n = noise.nextDouble() * 2 - 1;
            // its pump: a motor's hum, sagging under load, and its pistons' ripple
            final double hz = 60 + 160 * spool * (1 - 0.07 * flow);
            phase += 2 * Math.PI * hz / Clip.RATE;
            if (phase > 2 * Math.PI * 9) {
                phase -= 2 * Math.PI * 9;
            }
            double sample = 0.07 * spool * (Math.sin(phase) + 0.45 * Math.sin(2 * phase) + 0.2 * Math.sin(3 * phase))
                    + 0.03 * spool * (0.4 + 0.6 * flow) * Math.sin(9 * phase);
            // its fluid: a band of hiss, brighter as it flows, higher and thinner throttled
            final double centre = 1500 + 2000 * flow + 2500 * cushion;
            final double f = 2 * Math.sin(Math.PI * Math.min(centre, Clip.RATE / 6.0) / Clip.RATE);
            low += f * band;
            final double high = n - low - 0.5 * band;
            band += f * high;
            sample += (0.1 * flow + 0.025 * spool) * (1 - 0.4 * cushion) * band;
            // and what happens
            for (int h = hits.size() - 1; h >= 0; h--) {
                final Hit hit = hits.get(h);
                final double t = (double) hit.age++ / Clip.RATE;
                if (t > length(hit.event)) {
                    hits.remove(h);
                    continue;
                }
                sample += sound(hit.event, t, n);
            }
            out[from + s] = (float) Math.tanh(sample);
        }
    }

    /**
     * A bomb bay's doors, as heard from under it: {@code opening} from shut, or shutting from open, as far
     * and as fast as a bomber's are driven ({@link com.example.fxsuite.avatar.sim.Sky}); from a moment's
     * silence, through its pump, its lock and its valve, its doors moving and stopping, to its pump run
     * down, and silence again. Pure: the same for the same {@code variant}, another take for another.
     */
    public static Clip bay(final boolean opening, final int variant) {
        final int block = 256;
        final int lead = Clip.RATE / 50;
        final float[] out = new float[6 * Clip.RATE];
        final HydraulicVoice voice = new HydraulicVoice((opening ? 21 : 22) + 1000L * variant);
        Actuator a = Actuator.shut(Actuator.Drive.HYDRAULIC, 0, com.example.fxsuite.avatar.sim.Sky.BAY_OPENS,
                com.example.fxsuite.avatar.sim.Sky.BAY_RATE);
        a = opening ? a : a.placed(com.example.fxsuite.avatar.sim.Sky.BAY_OPENS);
        a = a.asked(opening);
        int at = lead;
        while (at + block <= out.length) {
            final Actuator.Stepped stepped = a.step((double) block / Clip.RATE);
            a = stepped.actuator();
            stepped.events().forEach(voice::hit);
            voice.set(a.spool(), a.flow(), a.cushion());
            voice.render(out, at, block);
            at += block;
            if (!a.busy() && !voice.sounding()) {
                break;
            }
        }
        // and a little silence after
        return new Clip(opening ? "bay-open" : "bay-shut", java.util.Arrays.copyOf(out, Math.min(out.length, at + lead)),
                false);
    }

    /** How long {@code event} sounds, seconds. */
    private static double length(final Actuator.Event event) {
        return switch (event) {
            case PUMP_START, PUMP_STOP -> 0.04;
            case UNLOCK -> 0.12;
            case VALVE -> 0.6;
            case CUSHION -> 1.2;
            case STOP -> 0.7;
            case LOCK -> 0.45;
        };
    }

    /** {@code event}'s sound, {@code t} seconds in; {@code n} this sample's noise. */
    private static double sound(final Actuator.Event event, final double t, final double n) {
        return switch (event) {
            // its relay: a small dry click, in higher than out
            case PUMP_START -> 0.25 * Math.sin(2 * Math.PI * 1400 * t) * Math.exp(-t / 0.004)
                    + 0.12 * n * Math.exp(-t / 0.002);
            case PUMP_STOP -> 0.2 * Math.sin(2 * Math.PI * 900 * t) * Math.exp(-t / 0.005)
                    + 0.1 * n * Math.exp(-t / 0.002);
            // its lock pin drawn: a metallic click, and the pin sliding out
            case UNLOCK -> 0.3 * (Math.sin(2 * Math.PI * 2600 * t) + 0.6 * Math.sin(2 * Math.PI * 3900 * t))
                    * Math.exp(-t / 0.012) + 0.08 * n * Math.exp(-t / 0.03);
            // its valve: a thump, and a burst of hiss as the pressure comes on
            case VALVE -> 0.32 * Math.sin(2 * Math.PI * 70 * t) * Math.exp(-t / 0.06)
                    + 0.18 * n * Math.exp(-t / 0.12) * (1 - Math.exp(-t / 0.004));
            // cushioned: a soft swell of hiss, rising and dying away
            case CUSHION -> 0.08 * n * (t < 0.08 ? t / 0.08 : Math.exp(-(t - 0.08) / 0.25));
            // stopped against its end: a heavy thud, a ring, a knock
            case STOP -> 0.45 * (Math.sin(2 * Math.PI * 52 * t) + 0.5 * Math.sin(2 * Math.PI * 127 * t))
                    * Math.exp(-t / 0.09) + 0.12 * Math.sin(2 * Math.PI * 880 * t) * Math.exp(-t / 0.03)
                    + 0.1 * n * Math.exp(-t / 0.01);
            // locked: a clunk as the pin drops in, then its click
            case LOCK -> {
                final double u = t - 0.05;
                yield u < 0 ? 0 : 0.3 * Math.sin(2 * Math.PI * 95 * u) * Math.exp(-u / 0.05)
                        + 0.25 * Math.sin(2 * Math.PI * 3200 * u) * Math.exp(-u / 0.008);
            }
        };
    }
}
