package com.example.fxsuite.avatar.roads;

import com.example.fxsuite.avatar.sim.CellMap;
import com.example.fxsuite.avatar.sim.Vec2;

import java.util.Arrays;

/**
 * Which roads may change what is at a point: each road filed by the squares of the ground its line comes within its
 * reach of ({@link Roadway#reach}), so what is asked at a point is asked only of the roads near it, however many there
 * are. Filed in the order they are laid, so those near a point come in that order.
 *
 * <p>Filed as they are laid, one thread laying them; asked from many at once (the land drawn in pieces apart) once
 * they are all laid.
 */
final class RoadIndex {

    /** How wide the squares the roads are filed by. */
    static final double CELL = 5_000;
    private static final int[] NONE = {};

    private final CellMap<int[]> filed = new CellMap<>();
    private int count;

    /** Files the {@code k}th road laid, {@code road}: by every square its line comes within its reach of. */
    void add(final int k, final Roadway road) {
        final double reach = road.reach();
        for (int i = 0; i + 1 < road.size(); i++) {
            final Vec2 a = road.point(i);
            final Vec2 b = road.point(i + 1);
            for (long x = cell(Math.min(a.x(), b.x()) - reach); x <= cell(Math.max(a.x(), b.x()) + reach); x++) {
                for (long z = cell(Math.min(a.z(), b.z()) - reach); z <= cell(Math.max(a.z(), b.z()) + reach); z++) {
                    final long key = CellMap.key(x, z);
                    final int[] here = filed.get(key);
                    if (here == null) {
                        filed.put(key, new int[] {k});
                    } else if (here[here.length - 1] != k) {
                        final int[] more = Arrays.copyOf(here, here.length + 1);
                        more[here.length] = k;
                        filed.put(key, more);
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

    /** The roads near (x, z), in the order they were laid; none if none is. Not to be changed. */
    int[] near(final double x, final double z) {
        final int[] here = filed.get(CellMap.key(cell(x), cell(z)));
        return here == null ? NONE : here;
    }

    private static long cell(final double v) {
        return (long) Math.floor(v / CELL);
    }
}
