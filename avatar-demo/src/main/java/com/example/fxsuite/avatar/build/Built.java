package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Box;
import com.example.fxsuite.avatar.sim.Inside;

import java.util.ArrayList;
import java.util.List;

/**
 * What of a building is built in each cell of its inside: each of its solids filed, once, in every cell its box
 * reaches; a wall between two rooms in both, a floor in the room over it and the one under. Worked out from its
 * solids, never moving: what a body in a room tests itself against is its room's and its neighbours', not the whole
 * building's.
 */
public final class Built {

    private final List<Solid> solids;
    private final int[][] byCell;

    public Built(final Inside inside, final List<Solid> solids) {
        this.solids = List.copyOf(solids);
        final List<List<Integer>> lists = new ArrayList<>(inside.count());
        for (int c = 0; c < inside.count(); c++) {
            lists.add(new ArrayList<>());
        }
        for (int s = 0; s < this.solids.size(); s++) {
            for (final int c : inside.cells(box(this.solids.get(s)))) {
                lists.get(c).add(s);
            }
        }
        byCell = new int[inside.count()][];
        for (int c = 0; c < byCell.length; c++) {
            byCell[c] = lists.get(c).stream().mapToInt(Integer::intValue).toArray();
        }
    }

    /** All a solid takes up, in the building's frame. */
    public static Box box(final Solid solid) {
        double minA = Double.POSITIVE_INFINITY;
        double maxA = Double.NEGATIVE_INFINITY;
        double minL = Double.POSITIVE_INFINITY;
        double maxL = Double.NEGATIVE_INFINITY;
        double low = Double.POSITIVE_INFINITY;
        double high = Double.NEGATIVE_INFINITY;
        for (final Point p : solid.corners()) {
            minA = Math.min(minA, p.across());
            maxA = Math.max(maxA, p.across());
            minL = Math.min(minL, p.along());
            maxL = Math.max(maxL, p.along());
            low = Math.min(low, p.up());
            high = Math.max(high, p.up());
        }
        return new Box(minA, maxA, minL, maxL, low, high);
    }

    /** What is built in a cell: its solids, in the building's order. */
    public List<Solid> in(final int cell) {
        final List<Solid> in = new ArrayList<>(byCell[cell].length);
        for (final int s : byCell[cell]) {
            in.add(solids.get(s));
        }
        return in;
    }

    /** What is built in the cells a box reaches: each solid once, in the building's order. */
    public List<Solid> near(final Inside inside, final Box box) {
        final java.util.BitSet found = new java.util.BitSet(solids.size());
        for (final int c : inside.cells(box)) {
            for (final int s : byCell[c]) {
                found.set(s);
            }
        }
        final List<Solid> near = new ArrayList<>(found.cardinality());
        for (int s = found.nextSetBit(0); s >= 0; s = found.nextSetBit(s + 1)) {
            near.add(solids.get(s));
        }
        return near;
    }
}
