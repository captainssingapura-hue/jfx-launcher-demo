package com.example.fxsuite.avatar.sim;

import java.util.BitSet;
import java.util.function.IntPredicate;

/**
 * Which cells of an inside are awake: those the avatar is within reach of, so far round it and so far over and under
 * it; the rest asleep, costing nothing. A cell wakes when the avatar comes within reach of it and falls asleep only
 * once it has gone a margin further, so cells at the edge do not flick awake and asleep. A cell with something moving
 * in it does not fall asleep; what happens wakes the cells it reaches. All of it follows from where the avatar is,
 * step by step, never from the clock: the same steps, the same cells awake.
 */
public final class Waking {

    /** What changed in a step: the cells that woke, and those that fell asleep, each in order. */
    public record Change(int[] woke, int[] slept) {

        public boolean none() {
            return woke.length == 0 && slept.length == 0;
        }
    }

    private final Inside inside;
    private final double reach;
    private final double rise;
    private final double margin;
    private BitSet awake = new BitSet();

    /**
     * @param reach  how far round the avatar, over the ground, cells wake
     * @param rise   how far over and under it they wake: a storey, for the storeys over and under its own
     * @param margin how much further it goes before they fall asleep again
     */
    public Waking(final Inside inside, final double reach, final double rise, final double margin) {
        if (!(reach >= 0 && rise >= 0 && margin >= 0)) {
            throw new IllegalArgumentException("reach, rise and margin not less than none: " + reach + ", " + rise
                    + ", " + margin);
        }
        this.inside = inside;
        this.reach = reach;
        this.rise = rise;
        this.margin = margin;
    }

    /**
     * A step: the avatar where it now is (its box in the inside's frame, or null where it is nowhere near), and which
     * cells have something moving in them. Cells within reach wake; those it has gone a margin past fall asleep,
     * unless something in them moves.
     */
    public Change step(final Box avatar, final IntPredicate restless) {
        final BitSet next = new BitSet();
        final BitSet keep = new BitSet();
        if (avatar != null) {
            for (final int c : inside.cells(avatar.grown(reach, rise))) {
                next.set(c);
            }
            for (final int c : inside.cells(avatar.grown(reach + margin, rise + margin))) {
                keep.set(c);
            }
        }
        for (int c = awake.nextSetBit(0); c >= 0; c = awake.nextSetBit(c + 1)) {
            if (keep.get(c) || restless.test(c)) {
                next.set(c);
            }
        }
        final BitSet woke = (BitSet) next.clone();
        woke.andNot(awake);
        final BitSet slept = (BitSet) awake.clone();
        slept.andNot(next);
        awake = next;
        return new Change(woke.stream().toArray(), slept.stream().toArray());
    }

    /** What happens wakes the cells it reaches (a shot's path, a thing thrown): those not awake already, now. */
    public int[] wake(final int... cells) {
        final BitSet woke = new BitSet();
        for (final int c : cells) {
            if (c < 0 || c >= inside.count()) {
                throw new IllegalArgumentException("no cell " + c + " of " + inside.count());
            }
            if (!awake.get(c)) {
                woke.set(c);
            }
        }
        awake.or(woke);
        return woke.stream().toArray();
    }

    public boolean awake(final int cell) {
        return awake.get(cell);
    }

    /** The cells awake, in order. */
    public int[] awake() {
        return awake.stream().toArray();
    }
}
