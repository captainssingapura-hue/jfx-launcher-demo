package com.example.fxsuite.avatar.sound;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * An engine, as it is heard: what {@link EngineVoice} makes its sound from, at any revs. Not a
 * recording, but the engine itself, roughly: how many cylinders, when each fires in its cycle
 * (two turns of the crank for a four-stroke, one for a two-stroke), and into which bank's
 * exhaust; how fast it idles and how far it revs; how briskly it revs up and how it falls back;
 * and its exhaust, as the few notes its pipes ring at, which stay where they are however fast it
 * turns.
 *
 * <p>So the firing note rises with the revs, as a real engine's does, while the pipes' ring
 * stays put: it sounds like the same engine working harder, not a tape played faster.
 *
 * @param key      what it is called for short, as {@link #called} finds it
 * @param name     what it is called
 * @param about    a line on what it sounds like, and why
 * @param strokes  4, firing each cylinder every other turn; or 2, every turn
 * @param firing   where in the cycle ({@link #cycle}: 0 to 720 degrees of crank, or 360 for a
 *                 two-stroke) each cylinder fires, in order
 * @param bank     the bank each of those cylinders exhausts into: 0 or 1
 * @param far      how loud the second bank is, against the first: under 1 if its pipe is further
 *                 from the ear, or its pulses meet less squarely
 * @param idle     its idle, in turns a minute
 * @param redline  where the limiter cuts in
 * @param climb    how fast it revs up, free, at full throttle: turns a minute, each second
 * @param fall     how fast it falls back, off the throttle
 * @param pipes    the notes its exhaust rings at, in Hz: the pipe's first few modes
 * @param ring     how long they ring: the resonance's Q, 2 dull to 10 bell-like
 * @param pulse    how long each cylinder's pulse of exhaust is, in milliseconds, at most
 * @param rasp     how much rough air comes with each pulse, against its knock
 * @param intake   the note the intake roars about, in Hz, at full throttle
 * @param charger  what blows its intake full of air ({@link Turbo}, {@link Supercharger}); null, nothing
 * @param exhaust  its firing order, and each cylinder's runner down to its bank's pipe
 */
public record Engine(String key, String name, String about, int strokes, double[] firing, int[] bank, double far,
                     double idle, double redline, double climb, double fall, double[] pipes, double ring, double pulse,
                     double rasp, double intake, Charger charger, Exhaust exhaust) {

    public Engine {
        if (strokes != 2 && strokes != 4) {
            throw new IllegalArgumentException(name + ": two strokes or four");
        }
        if (firing.length == 0 || firing.length != bank.length) {
            throw new IllegalArgumentException(name + ": a bank for each cylinder");
        }
        for (int i = 0; i < firing.length; i++) {
            if (firing[i] < 0 || firing[i] >= 180.0 * strokes || (i > 0 && firing[i] <= firing[i - 1])) {
                throw new IllegalArgumentException(name + ": firing angles rise through the cycle");
            }
            if (bank[i] != 0 && bank[i] != 1) {
                throw new IllegalArgumentException(name + ": two banks at most");
            }
        }
        if (idle <= 0 || redline <= idle) {
            throw new IllegalArgumentException(name + ": revs up from its idle");
        }
        if (exhaust == null || exhaust.order().length != firing.length) {
            throw new IllegalArgumentException(name + ": a runner for each cylinder");
        }
        firing = firing.clone();
        bank = bank.clone();
        pipes = pipes.clone();
    }

    /** One breathing on its own, nothing blowing its intake. */
    public Engine(final String key, final String name, final String about, final int strokes, final double[] firing,
                  final int[] bank, final double far, final double idle, final double redline, final double climb,
                  final double fall, final double[] pipes, final double ring, final double pulse, final double rasp,
                  final double intake) {
        this(key, name, about, strokes, firing, bank, far, idle, redline, climb, fall, pipes, ring, pulse, rasp, intake,
                null, Exhaust.even(firing.length));
    }

    // ---- cars

    /** An inline four: evenly, every half turn, through one pipe. Busy and buzzy, and it loves to rev. */
    public static final Engine I4 = new Engine("I4", "Inline four",
            "Fires every half turn through one pipe: a busy, buzzy note that climbs eagerly.", 4,
            new double[] {0, 180, 360, 540}, new int[] {0, 0, 0, 0}, 1,
            850, 7000, 9000, 5200, new double[] {190, 520, 1350}, 3.5, 3.2, 1.6, 520)
            .piped(new Exhaust(new int[] {1, 3, 4, 2}, new double[] {0.46, 0.32, 0.32, 0.46}, Exhaust.NONE));

    /**
     * A 60-degree V6: evenly, every third of a turn, its two banks taking turns. Smooth and even,
     * with a rasp to it, higher than a V8's.
     */
    public static final Engine V6 = new Engine("V6", "V6",
            "Fires every third of a turn, the banks taking turns: smooth and even, with a rasp.", 4,
            new double[] {0, 120, 240, 360, 480, 600}, new int[] {0, 1, 0, 1, 0, 1}, 0.85,
            800, 6500, 7800, 4800, new double[] {150, 410, 1100}, 3.5, 3.6, 1.4, 430)
            .piped(new Exhaust(new int[] {1, 2, 3, 4, 5, 6},
                    new double[] {0.36, 0.36, 0.42, 0.42, 0.50, 0.50}, Exhaust.NONE));

    /**
     * A cross-plane V8, as American ones are: every quarter turn, but its banks not taking
     * turns. The firing order 1-8-4-3-6-5-7-2 goes left, right, right, left, right, left, left,
     * right; so each bank's pulses come unevenly, and with the nearer bank louder, the beat
     * lopes: the burble. Deep, in wide pipes.
     */
    public static final Engine V8 = new Engine("V8", "V8, cross-plane",
            "Fires every quarter turn, but each bank unevenly (L R R L R L L R): the lopsided burble.", 4,
            new double[] {0, 90, 180, 270, 360, 450, 540, 630}, new int[] {0, 1, 1, 0, 1, 0, 0, 1}, 0.45,
            700, 6000, 6400, 4200, new double[] {95, 260, 720}, 3, 4.2, 1.1, 330)
            .piped(new Exhaust(new int[] {1, 8, 4, 3, 6, 5, 7, 2},
                    new double[] {0.30, 0.30, 0.38, 0.38, 0.46, 0.46, 0.54, 0.54}, Exhaust.NONE));

    /**
     * A flat-plane V8, as racing ones are: every quarter turn, the banks taking turns, so each
     * bank fires evenly, as a four does: no burble, a hard scream, and quick to rev.
     */
    public static final Engine FLAT_V8 = new Engine("FLAT_V8", "V8, flat-plane",
            "Every quarter turn, the banks taking turns: each bank an even four, so it screams.", 4,
            new double[] {0, 90, 180, 270, 360, 450, 540, 630}, new int[] {0, 1, 0, 1, 0, 1, 0, 1}, 0.9,
            1000, 8500, 12000, 6500, new double[] {140, 380, 1000}, 4, 3.0, 1.5, 560)
            .piped(new Exhaust(new int[] {1, 5, 3, 7, 4, 8, 2, 6},
                    new double[] {0.42, 0.44, 0.43, 0.45, 0.42, 0.44, 0.43, 0.45}, Exhaust.NONE));

    /**
     * An inline five, as the rally cars of the eighties had, turbocharged: one cylinder every 144
     * degrees, an odd number of them through one pipe, so its note warbles, between a four's and
     * a six's.
     */
    public static final Engine I5 = new Engine("I5", "Inline five",
            "Every 144° through one pipe: an odd number of cylinders, and the five's warble.", 4,
            new double[] {0, 144, 288, 432, 576}, new int[] {0, 0, 0, 0, 0}, 1,
            850, 7000, 8000, 5000, new double[] {125, 340, 950}, 3.5, 3.4, 1.5, 480)
            .piped(new Exhaust(new int[] {1, 2, 4, 5, 3},
                    new double[] {0.58, 0.42, 0.30, 0.40, 0.55}, Exhaust.NONE));

    // ---- scooters and motorcycles

    /**
     * A little two-stroke scooter, 50 cc: one cylinder firing every turn, not every other, into
     * an expansion chamber that rings high. Light, so it revs in a flash: the high, raspy buzz.
     */
    public static final Engine SCOOTER_2T = new Engine("SCOOTER_2T", "Scooter, 50 cc two-stroke",
            "One cylinder firing every turn into an expansion chamber: the high, raspy buzz.", 2,
            new double[] {0}, new int[] {0}, 1,
            1800, 9500, 15000, 9500, new double[] {310, 880, 2400}, 4.5, 1.8, 2.6, 900)
            .piped(new Exhaust(new int[] {1}, new double[] {0.35}, Exhaust.NONE));

    /**
     * A four-stroke scooter, 125 cc: one small cylinder, every other turn, in a short pipe. A
     * busy little buzz, a pop at each firing at idle.
     */
    public static final Engine SCOOTER = new Engine("SCOOTER", "Scooter, 125 cc single",
            "One small cylinder, every other turn, in a short pipe: a busy little buzz.", 4,
            new double[] {0}, new int[] {0}, 1,
            1700, 9500, 11500, 7500, new double[] {240, 620, 1600}, 3, 2.5, 1.8, 700)
            .piped(new Exhaust(new int[] {1}, new double[] {0.30}, Exhaust.NONE));

    /**
     * A big single, a thumper, as dual-sport bikes have: one big cylinder, every other turn, so
     * slowly at idle each thump is heard on its own, in a deep pipe.
     */
    public static final Engine THUMPER = new Engine("THUMPER", "Big single (thumper)",
            "One big cylinder every other turn: at idle, each thump heard on its own.", 4,
            new double[] {0}, new int[] {0}, 1,
            1100, 7000, 7500, 5000, new double[] {110, 300, 820}, 3, 5, 1.2, 380)
            .piped(new Exhaust(new int[] {1}, new double[] {0.55}, Exhaust.NONE));

    /**
     * A 45-degree V-twin, as cruisers have: both on one crank pin, so they fire 315 degrees
     * apart, then 405: the uneven beat of a cruiser at idle. Low-revving, deep, in staggered pipes.
     */
    public static final Engine V_TWIN = new Engine("V_TWIN", "V-twin 45° (cruiser)",
            "Two cylinders on one pin, firing 315° then 405° apart: the uneven cruiser beat.", 4,
            new double[] {0, 315}, new int[] {0, 1}, 0.85,
            900, 5500, 5200, 3800, new double[] {85, 240, 650}, 3, 5.5, 1.0, 300)
            .piped(new Exhaust(new int[] {1, 2}, new double[] {0.62, 0.45}, Exhaust.NONE));

    /**
     * A 90-degree V-twin, an L-twin, as sports bikes have: 270 degrees apart, then 450. Uneven
     * still, but harder, in short pipes, and it revs high.
     */
    public static final Engine L_TWIN = new Engine("L_TWIN", "L-twin 90° (sport)",
            "Two cylinders at 90°, firing 270° then 450° apart: a hard, uneven beat that revs high.", 4,
            new double[] {0, 270}, new int[] {0, 1}, 0.9,
            1200, 10500, 11000, 6500, new double[] {130, 360, 950}, 3.5, 4, 1.4, 480)
            .piped(new Exhaust(new int[] {1, 2}, new double[] {0.52, 0.42}, Exhaust.NONE));

    /**
     * A parallel twin with its crank pins together, as classic British ones are: both pistons
     * rising together, firing a turn apart, evenly. A steady beat, like a single's at twice the rate.
     */
    public static final Engine PARALLEL_TWIN = new Engine("PARALLEL_TWIN", "Parallel twin 360° (classic)",
            "Two cylinders firing a turn apart, evenly: a steady beat, a single's at twice the rate.", 4,
            new double[] {0, 360}, new int[] {0, 1}, 0.95,
            1000, 7500, 8500, 5200, new double[] {120, 330, 900}, 3.5, 4.5, 1.3, 420)
            .piped(new Exhaust(new int[] {1, 2}, new double[] {0.50, 0.50}, Exhaust.NONE));

    /** The cars' engines: any of them may be turbocharged or supercharged ({@link #with}). */
    public static final List<Engine> CARS = List.of(I4, I5, V6, V8, FLAT_V8);

    /** All of them, as the bench offers them: the cars', then the scooters' and the motorcycles'. */
    public static final List<Engine> ALL = List.of(I4, I5, V6, V8, FLAT_V8,
            SCOOTER_2T, SCOOTER, THUMPER, V_TWIN, L_TWIN, PARALLEL_TWIN);

    private static final Map<String, Engine> KEYS = new LinkedHashMap<>();

    static {
        for (final Engine engine : ALL) {
            KEYS.put(engine.key, engine);
        }
    }

    /** The engine called {@code key} for short ({@code V8}, {@code THUMPER}, ...), any case; or {@code otherwise}. */
    public static Engine called(final String key, final Engine otherwise) {
        return key == null ? otherwise : KEYS.getOrDefault(key.toUpperCase(Locale.ROOT), otherwise);
    }

    /** Whether anything blows its intake full of air. */
    public boolean charged() {
        return charger != null;
    }

    /** Whether that is a turbocharger. */
    public boolean turbocharged() {
        return charger instanceof Turbo;
    }

    /** Whether it is a car's, which may be charged. */
    public boolean car() {
        return CARS.stream().anyMatch(e -> e.key.equals(key));
    }

    /** The same engine, with {@code fitted} blowing its intake; null, nothing. */
    public Engine with(final Charger fitted) {
        return new Engine(key, name, about, strokes, firing, bank, far, idle, redline, climb, fall, pipes, ring, pulse,
                rasp, intake, fitted, exhaust);
    }

    /** The same engine, with {@code piping} for its exhaust. */
    public Engine piped(final Exhaust piping) {
        return new Engine(key, name, about, strokes, firing, bank, far, idle, redline, climb, fall, pipes, ring, pulse,
                rasp, intake, charger, piping);
    }

    /** Whether it has two banks, so two pipes for a crossover to mix. */
    public boolean twoBanks() {
        return Arrays.stream(bank).anyMatch(b -> b == 1);
    }

    public int cylinders() {
        return firing.length;
    }

    /** Its cycle, in degrees of crank: two turns for a four-stroke, one for a two-stroke. */
    public double cycle() {
        return 180.0 * strokes;
    }

    /** How many times a second it fires, at {@code rpm}: each cylinder once a cycle. */
    public double firingRate(final double rpm) {
        return rpm / 60 * 360 / cycle() * cylinders();
    }

    @Override
    public double[] firing() {
        return firing.clone();
    }

    @Override
    public int[] bank() {
        return bank.clone();
    }

    @Override
    public double[] pipes() {
        return pipes.clone();
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof Engine e && key.equals(e.key) && name.equals(e.name) && strokes == e.strokes
                && Arrays.equals(firing, e.firing) && Arrays.equals(bank, e.bank) && Arrays.equals(pipes, e.pipes)
                && far == e.far && idle == e.idle && redline == e.redline && climb == e.climb && fall == e.fall
                && ring == e.ring && pulse == e.pulse && rasp == e.rasp && intake == e.intake
                && java.util.Objects.equals(charger, e.charger) && exhaust.equals(e.exhaust);
    }

    @Override
    public int hashCode() {
        return key.hashCode() * 31 + Arrays.hashCode(firing);
    }

    @Override
    public String toString() {
        return charger == null ? name : name + ", " + charger.kind();
    }
}
