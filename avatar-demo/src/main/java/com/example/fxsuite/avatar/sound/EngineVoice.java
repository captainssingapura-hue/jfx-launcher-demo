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
 *   <li><b>Each pulse goes down its cylinder's runner</b> ({@link Exhaust}), as late as the runner
 *       is long, ringing at the runner's note, into its bank's collector: unlike runners bring evenly
 *       fired pulses to it unevenly, so the firing order is heard. A crossover may mix the banks.</li>
 *   <li><b>Then it rings in its bank's pipe</b>: a few resonances at the {@link Engine#pipes}
 *       notes, which do not move with the revs; the second bank's tuned a little apart, and as
 *       loud as {@link Engine#far} has it. Under them, the pulses themselves, for body.</li>
 *   <li><b>The intake roars</b> with the throttle open, more so the faster it turns.</li>
 *   <li><b>Worked hard</b> (throttle and load), it is driven into a soft clip: louder and harsher.</li>
 *   <li><b>A turbocharger</b>, if it has one ({@link TurboVoice}), softens the pipes' ring, and
 *       whistles and rushes as it spools up; snapping the throttle shut on boost, it hisses. <b>A
 *       supercharger</b> ({@link SuperchargerVoice}) whines with the revs.</li>
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
    /** How much each runner rings at its note, against the pulse coming straight through; and how sharply. */
    private static final double RUNNER = 0.6;
    private static final double RUNNER_RING = 2.5;
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
    /** What blows its intake, heard; null, nothing. */
    private final ChargerVoice charger;
    /** How much of the exhaust's ring gets past a turbine, if one is in the way, above its first note. */
    private final double past;
    /** Each cylinder's runner, in the order they fire: the pulses on their way down it, how late they come
     * out, in samples, and its ring; the place in them being written; and how much the banks are mixed. */
    private final float[][] runner;
    private final double[] lag;
    private final Dsp.Filter[] ringing;
    private final int reach;
    private int written;
    private final double[] excite;
    private final double crossover;

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
    private final int[] pulseSlot = new int[PULSES];
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
        // each cylinder a little unlike the next, the same every time round, wherever it comes in the order
        final Exhaust exhaust = engine.exhaust();
        final int[] order = exhaust.order();
        final Dsp.Noise each = new Dsp.Noise(seed * 31 + 7);
        final double[] byNumber = new double[firing.length];
        for (int c = 0; c < firing.length; c++) {
            byNumber[c] = 1 + 0.12 * each.next();
        }
        character = new double[firing.length];
        lag = new double[firing.length];
        ringing = new Dsp.Filter[firing.length];
        double longest = 0;
        for (int s = 0; s < firing.length; s++) {
            character[s] = byNumber[order[s] - 1];
            lag[s] = exhaust.delay(s) * RATE;
            ringing[s] = new Dsp.Filter(exhaust.note(s), RUNNER_RING);
            longest = Math.max(longest, lag[s]);
        }
        final int size = Integer.highestOneBit((int) Math.ceil(longest) + 2) << 1;
        reach = size - 1;
        runner = new float[firing.length][size];
        excite = new double[firing.length];
        crossover = engine.twoBanks() ? exhaust.crossover() : 0;
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
        charger = engine.charged() ? engine.charger().voice(engine) : null;
        past = engine.charged() ? 1 - engine.charger().muffle() : 1;
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

    /** How much boost its turbocharger or supercharger makes, 0 to 1; none without one. */
    public double boost() {
        return charger == null ? 0 : charger.boost();
    }

    /** How many times its turbocharger's blow-off valve has blown. */
    public long dumps() {
        return charger == null ? 0 : charger.dumps();
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
            // the pulses, each out of its cylinder's port
            for (int s = 0; s < excite.length; s++) {
                excite[s] = 0;
            }
            double crack = 0;
            for (int p = 0; p < active; p++) {
                final double t = age[p] / (double) RATE;
                final double w = width[p] / (double) RATE;
                final double knock = age[p] < width[p] ? Math.sin(Math.PI * age[p] / width[p]) : 0;
                final double puff = noise.next() * Math.exp(-t / (0.7 * w));
                excite[pulseSlot[p]] += strength[p] * (knock + engine.rasp() * 0.5 * puff);
                if (bang[p]) {
                    crack += strength[p] * noise.next() * Math.exp(-t / 0.004);
                }
                age[p]++;
            }
            retire();
            // each down its runner, as late as the runner is long, ringing at its note, into its
            // bank's collector
            into[0] = 0;
            into[1] = 0;
            for (int s = 0; s < excite.length; s++) {
                final float[] line = runner[s];
                line[written] = (float) excite[s];
                final double back = written - lag[s];
                final int whole = (int) Math.floor(back);
                final double part = back - whole;
                final double late = line[whole & reach] * (1 - part) + line[(whole + 1) & reach] * part;
                into[bank[s]] += late + RUNNER * ringing[s].push(late).band / RUNNER_RING;
            }
            written = (written + 1) & reach;
            // and the crossover, if any, mixing the banks' pipes
            if (crossover > 0) {
                final double left = into[0];
                into[0] += crossover * (into[1] - left);
                into[1] += crossover * (left - into[1]);
            }
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
            // a turbocharger whistling, rushing and hissing as it blows off; or a supercharger whining
            if (charger != null) {
                sound += charger.next(rpm, throttle, load, noise);
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
        pulseSlot[active] = cylinder;
        bang[active] = banged;
        active++;
    }

    /** Lets go of the pulses that have died away. */
    private void retire() {
        int kept = 0;
        for (int p = 0; p < active; p++) {
            final boolean alive = age[p] < width[p] * 4 || (bang[p] && age[p] < RATE / 40);
            if (alive) {
                age[kept] = age[p];
                width[kept] = width[p];
                strength[kept] = strength[p];
                pulseSlot[kept] = pulseSlot[p];
                bang[kept] = bang[p];
                kept++;
            }
        }
        active = kept;
    }
}
