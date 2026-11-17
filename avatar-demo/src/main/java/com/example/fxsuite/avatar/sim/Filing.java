package com.example.fxsuite.avatar.sim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * What is in an inside's cells ({@link Inside}): furniture, people, dropped things, filed each by its middle, in the
 * cell that holds it. Moved, a thing is filed again only when it crosses into another cell; moved out of every cell
 * (fallen from a balcony, flown out of a door) it is taken out, for the world's level ({@link Places}) to take. Asked
 * what is near, it looks as far round as the largest thing filed may reach: what it gives may be near, and is then
 * tested for what it is.
 */
public final class Filing<T> {

    private final Inside inside;
    private final Map<Integer, List<T>> cells = new HashMap<>();
    private final Map<T, Integer> where = new IdentityHashMap<>();
    private double reach;

    public Filing(final Inside inside) {
        this.inside = inside;
    }

    public Inside inside() {
        return inside;
    }

    /**
     * Files a thing by its middle, {@code radius} round: the cell it is in now, or -1 where it is in none and is not
     * filed. A thing already filed is an error: it is moved, not filed again.
     */
    public int file(final T thing, final double across, final double along, final double up, final double radius) {
        if (where.containsKey(thing)) {
            throw new IllegalArgumentException("already filed: " + thing);
        }
        final int cell = inside.cell(across, along, up);
        if (cell >= 0) {
            put(thing, cell);
            reach = Math.max(reach, radius);
        }
        return cell;
    }

    /** Its middle now: the cell it is in, or -1 where it has left them all and is taken out. */
    public int move(final T thing, final double across, final double along, final double up) {
        final int was = cell(thing);
        if (was < 0) {
            throw new IllegalArgumentException("not filed: " + thing);
        }
        final int cell = inside.cell(across, along, up);
        if (cell != was) {
            take(thing, was);
            if (cell >= 0) {
                put(thing, cell);
            }
        }
        return cell;
    }

    /** Takes a thing out; one not filed is an error. */
    public void remove(final T thing) {
        final int was = cell(thing);
        if (was < 0) {
            throw new IllegalArgumentException("not filed: " + thing);
        }
        take(thing, was);
    }

    /** The cell a thing is filed in, or -1: not filed. */
    public int cell(final T thing) {
        return where.getOrDefault(thing, -1);
    }

    /** What is filed in a cell, in the order it came in. */
    public List<T> in(final int cell) {
        final List<T> in = cells.get(cell);
        return in == null ? List.of() : Collections.unmodifiableList(in);
    }

    /** What may be near a box in the inside's frame: cell by cell, each in the order it came in. */
    public List<T> near(final Box box) {
        final List<T> found = new ArrayList<>();
        for (final int c : inside.cells(box.grown(reach, reach))) {
            final List<T> in = cells.get(c);
            if (in != null) {
                found.addAll(in);
            }
        }
        return found;
    }

    /** How far round the largest thing filed reaches. */
    public double reach() {
        return reach;
    }

    public int size() {
        return where.size();
    }

    private void put(final T thing, final int cell) {
        cells.computeIfAbsent(cell, k -> new ArrayList<>()).add(thing);
        where.put(thing, cell);
    }

    private void take(final T thing, final int cell) {
        final List<T> in = cells.get(cell);
        for (int k = 0; k < in.size(); k++) {
            if (in.get(k) == thing) {
                in.remove(k);
                break;
            }
        }
        if (in.isEmpty()) {
            cells.remove(cell);
        }
        where.remove(thing);
    }
}
