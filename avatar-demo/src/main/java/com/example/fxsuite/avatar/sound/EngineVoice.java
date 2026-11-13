package com.example.fxsuite.avatar.sound;

/**
 * An {@link Engine} running: its sound made as it goes, sample by sample, from how fast it turns
 * and how hard it is worked, not a recording sped up. Set the revs, the throttle and the load
 * ({@link #set}), as often as wanted; {@link #render} the next stretch of sound.
 *
 * <ul>
 *   <li><b>The crank turns</b> at the revs, and as it comes round to each cylinder's place in the
 *       cycle, that cylinder fires: a pulse of exhaust, a knock and a puff of rough air, a little
 *       unlike the last, louder the more throttle there is. So the firing note is the revs'.</li>
 *   <li><b>Each pulse rings in its bank's pipe</b>: a few resonances at the {@link Engine#pipes}
 *       notes, which do not move with the revs; the second bank's tuned a little apart, and as
 *       loud as {@link Engine#far} has it. Under them, the pulses themselves, for body.</li>
 *   <li><b>The intake roars</b> with the throttle open, more so the faster it turns.</li>
 *   <li><b>Worked hard</b> (throttle and load), it is driven into a soft clip: louder and harsher.</li>
 *   <li><b>A turbocharger</b>, if it has one ({@link TurboVoice}), softens the pipes' ring, and
 *       whistles and rushes as it spools up; snapping the throttle shut on boost, it hisses.</li>
 *   <li><b>Off the throttle</b> at speed, the cylinders fire weakly or not at all, and now and
 *       then one bangs in the pipe: the overrun pops and crackle.</li>
 * </ul>
 *
 * Changes to the revs and throttle are smoothed over a few hundredths of a second, so the
 * sound never clicks. The same seed gives the same sound. Not thread-safe: one thread drives it.
 */
public final class EngineVoice {

    private static final int RATE = Clip.RATE;
    /** How long the revs, the throttle and the load take to follow what they are set to. */
    private static final double REV_SMOOTH = 0.02;
    private static final double THROTTLE_SMOOTH = 0.03;
    /** The most pulses ringing out at once. */
    private static final int PULSES = 48;
    /** How loud each of the pipes' resonances is, the first the loudest. */
    private static final double[] MODES = {1, 0.75, 0.5};
    /** The second bank's pipes are tuned this much apart from the first's. */
    private static final double DETUNE = 1.045;
    /** Off the throttle, past this share of the redline, now and then a bang in the pipe. */
    private static final double OVERRUN = 0.4;
    private static final double POP_CHANCE = 0.06;
    /** The whole sound is scaled by this, to sit where the other sounds do. */
    private static final double GAIN = 0.55;

    private final Engine engine;
    private final double cycle;
    private final double[] firing;
    private final int[] bank;
    private final double[] character;
    private final Dsp.Noise noise;
    private final Dsp.Filter[][] pipes;
    private final Dsp.Filter[] body;
    private final Dsp.Filter intake;
    private final Dsp.Filter crackle;
    private final double ringGain;
    /** Its turbocharger, heard; null, none. */
    private final TurboVoice turbo;
    /** How much of the exhaust's ring gets past the turbine, above its first note. */
    private final double past;

    private double rpm;
    private double throttle;
    private double load;
    private double wantRpm;
    private double wantThrottle;
    private double wantLoad;
    private double crank;
    private double dc;

    private final double[] into = new double[2];
    private final int[] age = new int[PULSES];
    private final int[] width = new int[PULSES];
    private final double[] strength = new double[PULSES];
    private final int[] pulseBank = new int[PULSES];
    private final boolean[] bang = new boolean[PULSES];
    private int active;

    private long firings;
    private long pops;

    public EngineVoice(final Engine engine, final long seed) {
        this.engine = engine;
        cycle = engine.cycle();
        firing = engine.firing();
        bank = engine.bank();
        noise = new Dsp.Noise(seed);
        // each cylinder a little unlike the next, the same every time round
        final Dsp.Noise each = new Dsp.Noise(seed * 31 + 7);
        character = new double[firing.length];
        for (int c = 0; c < firing.length; c++) {
            character[c] = 1 + 0.12 * each.next();
        }
        final double[] notes = engine.pipes();
        pipes = new Dsp.Filter[2][notes.length];
        body = new Dsp.Filter[2];
        for (int b = 0; b < 2; b++) {
            for (int m = 0; m < notes.length; m++) {
                pipes[b][m] = new Dsp.Filter(notes[m] * (b == 0 ? 1 : DETUNE), engine.ring());
            }
            body[b] = new Dsp.Filter(notes[0] * 4, 0.7);
        }
        intake = new Dsp.Filter(engine.intake(), 1.4);
        crackle = new Dsp.Filter(2600, 1.2);
        ringGain = 1 / engine.ring();
        turbo = engine.turbocharged() ? new TurboVoice(engine) : null;
        past = engine.turbocharged() ? 1 - engine.turbo().muffle() : 1;
    }

    public Engine engine() {
        return engine;
    }

    /**
     * What it is to do from here: turn at {@code rpm} (0 stopped), with the throttle open
     * {@code throttle}, 0 to 1, and working {@code load}, 0 (revving free) to 1 (pulling hard).
     */
    public void set(final double rpm, final double throttle, final double load) {
        wantRpm = Math.max(0, rpm);
        wantThrottle = Math.clamp(throttle, 0, 1);
        wantLoad = Math.clamp(load, 0, 1);
    }

    /** How fast it is turning now, smoothed, in turns a minute. */
    public double rpm() {
        return rpm;
    }

    /** How many times a cylinder has fired, all told; and how many bangs in the pipe. */
    public long firings() {
        return firings;
    }

    public long pops() {
        return pops;
    }

    /** How much boost its turbocharger makes, 0 to 1; none without one. */
    public double boost() {
        return turbo == null ? 0 : turbo.boost();
    }

    /** How many times its blow-off valve has blown. */
    public long dumps() {
        return turbo == null ? 0 : turbo.dumps();
    }

    /** The next {@code count} samples of its sound, into {@code out} from {@code from}. */
    public void render(final float[] out, final int from, final int count) {
        final double revFollow = 1 - Math.exp(-1 / (REV_SMOOTH * RATE));
        final double throttleFollow = 1 - Math.exp(-1 / (THROTTLE_SMOOTH * RATE));
        final double dcFollow = 1 - Math.exp(-2 * Math.PI * 25.0 / RATE);
        for (int i = 0; i < count; i++) {
            rpm += (wantRpm - rpm) * revFollow;
            throttle += (wantThrottle - throttle) * throttleFollow;
            load += (wantLoad - load) * throttleFollow;
            turn();
            // the pulses ringing out, into each bank's pipe
            into[0] = 0;
            into[1] = 0;
            double crack = 0;
            for (int p = 0; p < active; p++) {
                final double t = age[p] / (double) RATE;
                final double w = width[p] / (double) RATE;
                final double knock = age[p] < width[p] ? Math.sin(Math.PI * age[p] / width[p]) : 0;
                final double puff = noise.next() * Math.exp(-t / (0.7 * w));
                into[pulseBank[p]] += strength[p] * (knock + engine.rasp() * 0.5 * puff);
                if (bang[p]) {
                    crack += strength[p] * noise.next() * Math.exp(-t / 0.004);
                }
                age[p]++;
            }
            retire();
            double sound = 0;
            for (int b = 0; b < 2; b++) {
                double rung = 0;
                for (int m = 0; m < pipes[b].length; m++) {
                    final double mode = MODES[Math.min(m, MODES.length - 1)] * (m == 0 ? 1 : past);
                    rung += mode * pipes[b][m].push(into[b]).band * ringGain;
                }
                final double under = body[b].push(into[b]).low;
                sound += (b == 0 ? 1 : engine.far()) * (1.6 * rung + 0.8 * under);
            }
            // the intake, roaring with the throttle open, the more the faster it turns
            final double share = Math.clamp((rpm - engine.idle()) / (engine.redline() - engine.idle()), 0, 1);
            sound += 0.35 * throttle * (0.25 + 0.75 * share) * intake.push(noise.next()).band / 1.4;
            sound += 0.8 * crackle.push(crack).band;
            // worked hard, driven into a soft clip: louder, and harsher with it
            final double drive = 1 + 3 * throttle * load;
            sound = Math.tanh(drive * sound) / Math.sqrt(drive);
            // the turbocharger, whistling and rushing over it all, and hissing as it blows off
            if (turbo != null) {
                sound += turbo.next(rpm, throttle, load, noise);
            }
            // no creeping off centre
            dc += (sound - dc) * dcFollow;
            out[from + i] = (float) Math.clamp(GAIN * (sound - dc), -1, 1);
        }
    }

    /** The crank a sample on: each cylinder whose place in the cycle it passes fires. */
    private void turn() {
        if (rpm < 1) {
            return;
        }
        final double was = crank;
        crank += 6 * rpm / RATE;
        final boolean round = crank >= cycle;
        if (round) {
            crank -= cycle;
        }
        for (int c = 0; c < firing.length; c++) {
            final double at = firing[c];
            if (round ? (at > was || at <= crank) : (at > was && at <= crank)) {
                fire(c);
            }
        }
    }

    private void fire(final int cylinder) {
        firings++;
        if (active == PULSES) {
            return;
        }
        // as loud as the throttle is open; off it, weak, and some not at all
        double loud = character[cylinder] * (0.35 + 0.65 * throttle) * (1 + 0.08 * noise.next());
        final double share = rpm / engine.redline();
        boolean banged = false;
        if (throttle < 0.08 && share > OVERRUN) {
            final double roll = (noise.next() + 1) / 2;
            if (roll < POP_CHANCE) {
                loud = 1.8 + 0.6 * roll / POP_CHANCE;
                banged = true;
                pops++;
            } else if (roll < 0.35) {
                loud *= 0.25;
            }
        }
        // its pulse no longer than it has before the next one comes
        final double gap = 1 / engine.firingRate(rpm);
        final double w = Math.min(engine.pulse() / 1000, 0.55 * gap);
        age[active] = 0;
        width[active] = Math.max(8, (int) Math.round(w * RATE));
        strength[active] = loud;
        pulseBank[active] = bank[cylinder];
        bang[active] = banged;
        active++;
    }

    /** Lets go of the pulses that have died away. */
    private void retire() {
        int kept = 0;
        for (int p = 0; p < active; p++) {
            final boolean ringing = age[p] < width[p] * 4 || (bang[p] && age[p] < RATE / 40);
            if (ringing) {
                age[kept] = age[p];
                width[kept] = width[p];
                strength[kept] = strength[p];
                pulseBank[kept] = pulseBank[p];
                bang[kept] = bang[p];
                kept++;
            }
        }
        active = kept;
    }
}
