package com.example.fxsuite.avatar.sound;

import java.util.Arrays;

/**
 * An engine's exhaust, cylinder by cylinder: which cylinder fires when (its firing order), and
 * the runner each one's pulse goes down to its bank's collector: how long it is, so how late its
 * pulse gets there and the note it rings at. Runners of unlike lengths bring the pulses to the
 * collector unevenly, though the cylinders fire evenly; so the order the cylinders fire in, and
 * which runner each has, is heard. And between the two banks' pipes, if there are two, a
 * crossover may mix them: an H-pipe a little, an X-pipe half and half, evening out what each
 * bank alone would make lopsided.
 *
 * @param order     the cylinders, by number from 1, in the order they fire
 * @param runners   each cylinder's runner, by its number (the first for cylinder 1), in metres
 * @param crossover how much of each bank's pipe the other gets: 0, true dual pipes, to 0.5, all mixed
 */
public record Exhaust(int[] order, double[] runners, double crossover) {

    /** How fast a pulse goes down a runner, in hot exhaust gas, in metres a second. */
    public static final double SOUND = 500;
    /** The crossovers there are: none, an H-pipe, an X-pipe. */
    public static final double NONE = 0;
    public static final double H_PIPE = 0.2;
    public static final double X_PIPE = 0.5;

    public Exhaust {
        if (order.length == 0 || order.length != runners.length) {
            throw new IllegalArgumentException("a runner for each cylinder");
        }
        final boolean[] seen = new boolean[order.length];
        for (final int cylinder : order) {
            if (cylinder < 1 || cylinder > order.length || seen[cylinder - 1]) {
                throw new IllegalArgumentException("each cylinder fires once: " + Arrays.toString(order));
            }
            seen[cylinder - 1] = true;
        }
        for (final double runner : runners) {
            if (runner <= 0) {
                throw new IllegalArgumentException("a runner has a length");
            }
        }
        if (crossover < 0 || crossover > 0.5) {
            throw new IllegalArgumentException("a crossover mixes the banks at most half and half");
        }
        order = order.clone();
        runners = runners.clone();
    }

    /** {@code cylinders} firing in turn, 1 first, down runners all alike: as plain as an exhaust is. */
    public static Exhaust even(final int cylinders) {
        final int[] order = new int[cylinders];
        final double[] runners = new double[cylinders];
        for (int i = 0; i < cylinders; i++) {
            order[i] = i + 1;
            runners[i] = 0.4;
        }
        return new Exhaust(order, runners, NONE);
    }

    /** The runner of the cylinder that fires {@code slot}th in the cycle, from 0. */
    public double runner(final int slot) {
        return runners[order[slot] - 1];
    }

    /** How late a pulse from the cylinder firing {@code slot}th gets to its collector, in seconds. */
    public double delay(final int slot) {
        return runner(slot) / SOUND;
    }

    /** The note the runner of the cylinder firing {@code slot}th rings at: a quarter wave of it, in Hz. */
    public double note(final int slot) {
        return SOUND / (4 * runner(slot));
    }

    /** The same, its runners all made as long as their average: tuned, equal-length headers. */
    public Exhaust equalLength() {
        final double mean = Arrays.stream(runners).average().orElseThrow();
        final double[] alike = new double[runners.length];
        Arrays.fill(alike, mean);
        return new Exhaust(order, alike, crossover);
    }

    /** Whether its runners are all one length. */
    public boolean equal() {
        return Arrays.stream(runners).allMatch(r -> r == runners[0]);
    }

    /** The same, with crossover {@code mixed} between the banks. */
    public Exhaust crossed(final double mixed) {
        return new Exhaust(order, runners, mixed);
    }

    /** The firing order, as it is written: 1-8-4-3-6-5-7-2. */
    public String written() {
        final StringBuilder out = new StringBuilder();
        for (final int cylinder : order) {
            out.append(out.isEmpty() ? "" : "-").append(cylinder);
        }
        return out.toString();
    }

    @Override
    public int[] order() {
        return order.clone();
    }

    @Override
    public double[] runners() {
        return runners.clone();
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof Exhaust e && Arrays.equals(order, e.order) && Arrays.equals(runners, e.runners)
                && crossover == e.crossover;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(order) * 31 + Arrays.hashCode(runners);
    }
}
