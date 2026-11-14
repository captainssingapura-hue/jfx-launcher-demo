package com.example.fxsuite.avatar.sound;

/**
 * A {@link Gatling} gun turning and firing: its sound made as it goes, sample by sample, from how
 * fast its barrels turn and whether it is firing, not a recording looped. Set what it is doing
 * ({@link #set}) as often as wanted, and how it is mixed ({@link #style}); {@link #render} the
 * next stretch of sound.
 *
 * <ul>
 *   <li><b>The barrels go round</b> as fast as they are spun, and each time one comes to the top
 *       it is fired, if the gun is firing and up to speed: so the shots come exactly as often as
 *       the gun fires them, and run together into its note. Turning without firing, each bolt
 *       clicks dry over its cam instead.</li>
 *   <li><b>Each shot</b> is a blast of air out of the muzzle, a short push and pull (the
 *       N-shaped wave a muzzle sends out), as long as the round is big; a thump, the air ringing
 *       low after it, lower the bigger the round; the round's crack; and the clack of its bolt.
 *       Each a little unlike the last, as no two rounds are quite alike.</li>
 *   <li><b>The motor</b> whines as it turns, its gears meshing at their note, rising as it spins
 *       the barrels up and falling as they run down; firing, it labours, and sags a little. The
 *       barrels whoosh round through the air. A hydraulic one hisses and moans as well.</li>
 *   <li><b>The empty cases</b> fly out, fall, and ring as they land, as long after as they take to
 *       fall: none heard from high up.</li>
 *   <li><b>The ground</b> sends the blast back, as late as it is far below; and the air round it
 *       rings on, briefly outdoors, long in a film's mix ({@link GunStyle}).</li>
 * </ul>
 *
 * Changes to the spin and the height are smoothed, so the sound never clicks. The same seed gives
 * the same sound. Not thread-safe: one thread drives it.
 */
public final class GatlingVoice {

    private static final int RATE = Clip.RATE;
    /** How long the spin takes to follow what it is set to. */
    private static final double SPIN_SMOOTH = 0.02;
    /** How long the motor takes to feel the load of firing, and to let it go. */
    private static final double LOAD_SMOOTH = 0.06;
    /** How much the motor sags, firing. */
    private static final double SAG = 0.04;
    /** Up to speed enough to fire: the gun fires only at full speed. */
    private static final double UP = 0.97;
    /** Turning fast enough to be heard clicking over. */
    private static final double TURNING = 0.03;
    /** The most shots and clicks ringing out at once. */
    private static final int SHOTS = 96;
    /** The most cases falling at once. */
    private static final int CASES = 256;
    /** How fast sound goes, in metres a second; and how fast things fall, in metres a second a second. */
    private static final double SOUND = 343;
    private static final double GRAVITY = 9.81;
    /** The furthest below the ground can be and still be heard sending the blast back: 1.6 s there and back. */
    private static final double ECHO = 1.6;
    /** Above this, the cases fall too far to be heard landing. */
    private static final double CASES_HEARD = 12;
    /** How far from the ear the muzzle is: what the ground's path back is set against. */
    private static final double NEAR = 1.5;
    /** The whole sound is scaled by this, to sit where the other sounds do. */
    private static final double GAIN = 0.5;
    /** The ringing's combs and its diffusers, in seconds. */
    private static final double[] COMBS = {0.0297, 0.0371, 0.0411, 0.0437};
    private static final double[] DIFFUSERS = {0.0050, 0.0017};

    private Gatling gatling;
    private final Dsp.Noise noise;
    private GunStyle style;

    private final Dsp.Filter crackFilter = new Dsp.Filter(3400, 0.7);
    private final Dsp.Filter clackFilter;
    private final Dsp.Filter air = new Dsp.Filter(420, 1.2);
    private final Dsp.Filter hiss = new Dsp.Filter(5200, 0.8);
    private final Dsp.Filter brush;
    private final Dsp.Filter[] cases;
    private double blastSmooth;

    private double spin;
    private double wantSpin;
    private boolean firing;
    private double load;
    private double height;
    private double wantHeight;
    private double rotor;
    private double shaft;
    private double mesh;
    private double dc;

    private final int[] age = new int[SHOTS];
    private final int[] life = new int[SHOTS];
    private final double[] loud = new double[SHOTS];
    private final double[] tune = new double[SHOTS];
    private final boolean[] shot = new boolean[SHOTS];
    private int active;

    private final double[] caseDue = new double[CASES];
    private final double[] caseLoud = new double[CASES];
    private int falling;
    private long now;

    private final float[] echo;
    private final int echoMask;
    private int echoAt;
    private double echoSmooth;
    private double echoLate;

    private final float[][] combs = new float[COMBS.length][];
    private final int[] combAt = new int[COMBS.length];
    private final double[] combDamp = new double[COMBS.length];
    private final double[] combBack = new double[COMBS.length];
    private final float[][] diffusers = new float[DIFFUSERS.length][];
    private final int[] diffuserAt = new int[DIFFUSERS.length];

    private long shots;
    private long clicks;
    private long landed;

    public GatlingVoice(final Gatling gatling, final GunStyle style, final long seed) {
        noise = new Dsp.Noise(seed);
        clackFilter = new Dsp.Filter(3600, 3);
        brush = new Dsp.Filter(1000, 6);
        cases = new Dsp.Filter[] {new Dsp.Filter(3900, 28), new Dsp.Filter(5600, 24), new Dsp.Filter(7700, 20)};
        gatling(gatling);
        final int size = Integer.highestOneBit((int) (ECHO * RATE) + 2) << 1;
        echo = new float[size];
        echoMask = size - 1;
        for (int c = 0; c < COMBS.length; c++) {
            combs[c] = new float[(int) Math.round(COMBS[c] * RATE)];
        }
        for (int d = 0; d < DIFFUSERS.length; d++) {
            diffusers[d] = new float[(int) Math.round(DIFFUSERS[d] * RATE)];
        }
        style(style);
    }

    public Gatling gatling() {
        return gatling;
    }

    /**
     * The gun retuned, as it turns: bigger or smaller rounds, faster or slower firing, from the
     * next shot on. What rings now rings on; the barrels turn on from where they are.
     */
    public void gatling(final Gatling retuned) {
        gatling = retuned;
        // a bigger bolt clacks lower; the motor's brushes whine at the gears' note
        clackFilter.tune(3600 * Math.pow(7.62 / retuned.calibre(), 0.25), 3);
        brush.tune(retuned.whine(), 6);
        // bigger cases ring lower
        final double ring = Math.sqrt(7.62 / retuned.calibre());
        cases[0].tune(3900 * ring, 28);
        cases[1].tune(5600 * ring, 24);
        cases[2].tune(7700 * ring, 20);
    }

    public GunStyle style() {
        return style;
    }

    /** Mixed so from here; the shots ringing now go on as they were. */
    public void style(final GunStyle style) {
        this.style = style;
        for (int c = 0; c < COMBS.length; c++) {
            // each comb dies away to a thousandth over the tail
            combBack[c] = Math.pow(10, -3 * COMBS[c] / style.tail());
        }
    }

    /**
     * What it is to do from here: its barrels turning at {@code spin} of full speed, 0 still to 1;
     * firing or not (it fires only up to speed); {@code height} metres over the ground.
     */
    public void set(final double spin, final boolean firing, final double height) {
        wantSpin = Math.clamp(spin, 0, 1);
        this.firing = firing;
        wantHeight = Math.max(0, height);
    }

    /** How fast its barrels turn now, smoothed: 0 to 1 of full speed. */
    public double spin() {
        return spin;
    }

    /** How many rounds it has fired, all told; how many dry clicks; how many cases heard landing. */
    public long shots() {
        return shots;
    }

    public long clicks() {
        return clicks;
    }

    public long landed() {
        return landed;
    }

    /** How long after the blast the ground sends it back, from {@code height} metres up, in seconds. */
    public static double echoDelay(final double height) {
        return (Math.hypot(NEAR, 2 * height) - NEAR) / SOUND;
    }

    /** The next {@code count} samples of its sound, into {@code out} from {@code from}. */
    public void render(final float[] out, final int from, final int count) {
        final double spinFollow = 1 - Math.exp(-1 / (SPIN_SMOOTH * RATE));
        final double loadFollow = 1 - Math.exp(-1 / (LOAD_SMOOTH * RATE));
        final double heightFollow = 1 - Math.exp(-1 / (0.25 * RATE));
        final double dcFollow = 1 - Math.exp(-2 * Math.PI * 20.0 / RATE);
        final double blastFollow = 1 - Math.exp(-2 * Math.PI * 7000.0 / RATE);
        final double groundFollow = 1 - Math.exp(-2 * Math.PI * 1800.0 / RATE);
        final double size = 0.55 * Math.sqrt(gatling.loud());
        final double drive = style.drive();
        final double driven = Math.tanh(drive);
        for (int i = 0; i < count; i++) {
            spin += (wantSpin - spin) * spinFollow;
            load += ((firing && spin > UP ? 1 : 0) - load) * loadFollow;
            height += (wantHeight - height) * heightFollow;
            final double turning = spin * (1 - SAG * load);
            // the barrels go round as fast as the gun spins them, firing in step with it; only the
            // motor, labouring, is heard to sag
            turn(spin);
            // the shots and clicks ringing out
            double push = 0;
            double thump = 0;
            double boom = 0;
            double crack = 0;
            double clack = 0;
            for (int p = 0; p < active; p++) {
                final double t = age[p] / (double) RATE;
                final double hit = noise.next();
                clack += loud[p] * hit * Math.exp(-t / 0.0025) * (shot[p] ? 1 : 0.35);
                if (shot[p]) {
                    final double span = gatling.blast() * tune[p];
                    final double note = gatling.thump() / tune[p];
                    final double ring = 2.2 / note;
                    // the N-shaped wave: a push, falling through to a pull, and back
                    push += loud[p] * (t < span ? 1 - 2 * t / span
                            : t < 1.2 * span ? -1 + (t - span) / (0.2 * span) : 0);
                    final double onset = 1 - Math.exp(-t / 0.0006);
                    thump += loud[p] * onset * Math.sin(2 * Math.PI * note * t) * Math.exp(-t / ring);
                    boom += loud[p] * onset * Math.sin(Math.PI * note * t) * Math.exp(-t / (2.5 * ring));
                    crack += loud[p] * hit * Math.exp(-t / 0.0012);
                }
                age[p]++;
            }
            retire();
            blastSmooth += (style.push() * push - blastSmooth) * blastFollow;
            final double blast = blastSmooth + style.thump() * thump + style.boom() * boom
                    + style.crack() * crackFilter.push(crack).high;
            // driven into the microphone's overload: loud all through
            double sound = size * Math.tanh(drive * blast) / driven;
            sound += style.clack() * 0.6 * clackFilter.push(clack).band;
            sound += mechanism(turning);
            sound += style.brass() * landing();
            // the ground sends it back, softened, as late as it is far below
            sound += ground(sound, groundFollow);
            // and the air round it rings on
            sound += style.wet() * ringing(sound);
            dc += (sound - dc) * dcFollow;
            out[from + i] = (float) Math.clamp(GAIN * style.level() * (sound - dc), -1, 1);
            now++;
        }
    }

    /** The barrels a sample on, turning at {@code turning} of full speed: each that comes to the top fires, or clicks. */
    private void turn(final double turning) {
        rotor += turning * gatling.perSecond() / RATE;
        if (rotor < 1) {
            return;
        }
        rotor -= 1;
        if (firing && spin > UP) {
            fire();
        } else if (spin > TURNING) {
            click();
        }
    }

    private void fire() {
        shots++;
        // a case flies out, to land as long after as it takes to fall
        if (height < CASES_HEARD && falling < CASES) {
            final double fall = Math.sqrt(2 * (height + 1) / GRAVITY) * (1 + 0.15 * noise.next());
            caseDue[falling] = now + fall * RATE;
            caseLoud[falling] = (0.6 + 0.3 * noise.next()) / (1 + height);
            falling++;
        }
        start(true, 1 + 0.08 * noise.next(), 1 + 0.025 * noise.next(),
                (int) (RATE * Math.max(0.03, 7 * 2.2 / gatling.thump())));
    }

    private void click() {
        clicks++;
        start(false, spin * (1 + 0.1 * noise.next()), 1, (int) (RATE * 0.015));
    }

    private void start(final boolean round, final double strength, final double tuned, final int lasts) {
        if (active == SHOTS) {
            return;
        }
        age[active] = 0;
        life[active] = lasts;
        loud[active] = strength;
        tune[active] = tuned;
        shot[active] = round;
        active++;
    }

    /** Lets go of the shots and clicks that have died away. */
    private void retire() {
        int kept = 0;
        for (int p = 0; p < active; p++) {
            if (age[p] < life[p]) {
                age[kept] = age[p];
                life[kept] = life[p];
                loud[kept] = loud[p];
                tune[kept] = tune[p];
                shot[kept] = shot[p];
                kept++;
            }
        }
        active = kept;
    }

    /** The motor and its gears, the barrels through the air, and a hydraulic drive's hiss and moan. */
    private double mechanism(final double turning) {
        if (turning <= 0.001) {
            return 0;
        }
        shaft += gatling.motor() * turning / RATE;
        mesh += gatling.whine() * turning / RATE;
        shaft -= Math.floor(shaft);
        mesh -= Math.floor(mesh);
        final double gears = 0.5 * Math.sin(2 * Math.PI * mesh) + 0.2 * Math.sin(4 * Math.PI * mesh)
                + 0.12 * Math.sin(2 * Math.PI * shaft) + 0.25 * brush.push(noise.next()).band / 6
                // labouring under the load of firing, rougher
                + 0.15 * load * Math.sin(6 * Math.PI * mesh);
        double sound = style.whine() * 0.28 * Math.pow(turning, 1.3) * gears;
        // the barrels whooshing round, a pulse as each passes
        sound += 0.22 * turning * turning * (0.6 + 0.4 * Math.sin(2 * Math.PI * rotor)) * air.push(noise.next()).band;
        if (gatling.hydraulic()) {
            sound += turning * (0.07 * hiss.push(noise.next()).band + 0.12 * Math.sin(Math.PI * shaft));
        }
        return sound;
    }

    /** The cases that land now, each ringing the case's notes. */
    private double landing() {
        double strike = 0;
        int kept = 0;
        for (int c = 0; c < falling; c++) {
            if (caseDue[c] <= now) {
                strike += caseLoud[c];
                landed++;
            } else {
                caseDue[kept] = caseDue[c];
                caseLoud[kept] = caseLoud[c];
                kept++;
            }
        }
        falling = kept;
        double ring = 0;
        for (int k = 0; k < cases.length; k++) {
            ring += cases[k].push(strike * (k == 0 ? 1 : 0.7)).band;
        }
        return 0.9 * ring;
    }

    /** What the ground sends back of {@code sound}: as late as it is far below, softer the further, duller. */
    private double ground(final double sound, final double follow) {
        echo[echoAt] = (float) sound;
        final double wanted = echoDelay(height) * RATE;
        echoLate += (wanted - echoLate) * 0.0005;
        final double back = echoAt - Math.max(1, echoLate);
        final int whole = (int) Math.floor(back);
        final double part = back - whole;
        final double late = echo[whole & echoMask] * (1 - part) + echo[(whole + 1) & echoMask] * part;
        echoAt = (echoAt + 1) & echoMask;
        echoSmooth += (late - echoSmooth) * follow;
        return style.ground() * Math.pow(NEAR / (NEAR + 2 * height), 0.6) * echoSmooth;
    }

    /** The air round it ringing on: a few combs, each dying away over the tail, then diffused. */
    private double ringing(final double sound) {
        double wet = 0;
        for (int c = 0; c < COMBS.length; c++) {
            final float[] line = combs[c];
            final double out = line[combAt[c]];
            // duller as it rings on
            combDamp[c] += (out - combDamp[c]) * 0.35;
            line[combAt[c]] = (float) (sound + combBack[c] * combDamp[c]);
            combAt[c] = (combAt[c] + 1) % line.length;
            wet += out;
        }
        wet /= COMBS.length;
        for (int d = 0; d < DIFFUSERS.length; d++) {
            final float[] line = diffusers[d];
            final double held = line[diffuserAt[d]];
            final double in = wet + 0.7 * held;
            line[diffuserAt[d]] = (float) in;
            diffuserAt[d] = (diffuserAt[d] + 1) % line.length;
            wet = held - 0.7 * in;
        }
        return wet;
    }
}
