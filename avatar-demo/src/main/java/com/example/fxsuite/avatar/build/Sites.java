package com.example.fxsuite.avatar.build;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

/**
 * What each building in the world is drawn as, by its key ({@code Course.Piece.tint}): the town's
 * buildings from 0, the temple's halls from {@link TempleYard#KEYS}, its wall's units from
 * {@link TempleYard#WALL_KEYS}, the pyramids from {@link Pyramids#KEYS}, the pavilions from
 * {@link Pavilions#KEYS}, the village's from {@link Village#KEYS}, the wall tower from {@link TowerMap#KEYS};
 * and whatever else registers a range of its own ({@link #register}): what is built on roads (its bridges), from
 * outside the buildings, without their knowing of it.
 */
public final class Sites {

    private Sites() {
    }

    /** A range of keys registered, and what each is drawn as, by its place in the range. */
    private record Range(int from, int to, IntFunction<List<Solid>> drawn) {
    }

    private static final List<Range> RANGES = new ArrayList<>();

    /**
     * Keys {@code from} to {@code to}, not including it, drawn as {@code drawn} says by each one's place among them
     * (0 for {@code from}): asked before the buildings' own. A range already registered there is replaced.
     */
    public static synchronized void register(final int from, final int to, final IntFunction<List<Solid>> drawn) {
        if (to <= from) {
            throw new IllegalArgumentException("a range of keys from " + from + " to past it, not " + to);
        }
        RANGES.removeIf(r -> r.from() < to && from < r.to());
        RANGES.add(new Range(from, to, drawn));
    }

    private static synchronized Range registered(final int key) {
        for (final Range r : RANGES) {
            if (key >= r.from() && key < r.to()) {
                return r;
            }
        }
        return null;
    }

    /**
     * Building {@code key}'s door {@code door}, all of it, {@code open} as far open, about the middle of its box as its
     * solids are: drawn apart from them, as the door is opened and shut, and broken. None for a building without doors.
     */
    public static List<Solid> door(final int key, final int door, final double open) {
        return registered(key) == null && key >= TowerMap.KEYS ? TowerMap.door(door, open) : List.of();
    }

    /** Building {@code key}'s solids, about the middle of its box. */
    public static List<Solid> drawn(final int key) {
        final Range range = registered(key);
        if (range != null) {
            return range.drawn().apply(key - range.from());
        }
        if (key >= TowerMap.KEYS) {
            return TowerMap.solids(key - TowerMap.KEYS);
        }
        if (key >= Village.KEYS) {
            return Village.solids(key - Village.KEYS);
        }
        if (key >= Pavilions.KEYS) {
            return Pavilions.solids(key - Pavilions.KEYS);
        }
        return key >= Pyramids.KEYS ? Pyramids.solids(key - Pyramids.KEYS) : TempleYard.drawn(key);
    }
}
