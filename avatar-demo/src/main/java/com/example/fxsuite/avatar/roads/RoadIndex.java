package com.example.fxsuite.avatar.roads;

import com.example.fxsuite.avatar.sim.Vec2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Which roads may change what is at a point: each road filed by the squares of the ground its line comes within its
 * reach of ({@link Roadway#reach}), so what is asked at a point is asked only of the roads near it, however many there
 * are. Filed in the order they are laid, so those near a point come in that order.
 */
final class RoadIndex {

    /** How wide the squares the roads are filed by. */
    static final double CELL = 5_000;
    private static final int[] NONE = {};

    private final Map<Long, List<Integer>> filed = new HashMap<>();
    /** Asked from many threads at once (the land drawn in pieces apart): what is near each square, kept once worked out. */
    private final Map<Long, int[]> cells = new java.util.concurrent.ConcurrentHashMap<>();
    private int count;

    /** Files the {@code k}th road laid, {@code road}: by every square its line comes within its reach of. */
    void add(final int k, final Roadway road) {
        final double reach = road.reach();
        for (int i = 0; i + 1 < road.size(); i++) {
            final Vec2 a = road.point(i);
            final Vec2 b = road.point(i + 1);
            for (long x = cell(Math.min(a.x(), b.x()) - reach); x <= cell(Math.max(a.x(), b.x()) + reach); x++) {
                for (long z = cell(Math.min(a.z(), b.z()) - reach); z <= cell(Math.max(a.z(), b.z()) + reach); z++) {
                    final long key = key(x, z);
                    final List<Integer> here = filed.computeIfAbsent(key, c -> new ArrayList<>());
                    if (here.isEmpty() || here.getLast() != k) {
                        here.add(k);
                        cells.remove(key);
                    }
                }
            }
        }
        count = Math.max(count, k + 1);
    }

    /** How many roads have been filed. */
    int count() {
        return count;
    }

    /** The roads near (x, z), in the order they were laid; none if none is. */
    int[] near(final double x, final double z) {
        final long key = key(cell(x), cell(z));
        final int[] kept = cells.get(key);
        if (kept != null) {
            return kept;
        }
        final List<Integer> here = filed.get(key);
        final int[] near = here == null ? NONE : here.stream().mapToInt(Integer::intValue).toArray();
        cells.put(key, near);
        return near;
    }

    private static long cell(final double v) {
        return (long) Math.floor(v / CELL);
    }

    private static long key(final long x, final long z) {
        return x * 0x1_0000_0000L + (z & 0xffff_ffffL);
    }
}
