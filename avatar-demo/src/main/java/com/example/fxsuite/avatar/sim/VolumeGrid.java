package com.example.fxsuite.avatar.sim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Where a world's volumes are, seen from above: the ground cut into square cells, each listing the
 * volumes whose boxes ({@link Volume#bounds}) reach into it; one reaching over very many cells (a
 * race track's) listed apart, as near everywhere. Asked what is near a circle, it says which
 * volumes may be, in their order in the world, without going through all of them: what a town of
 * shaped buildings, things set out on their floors, needs.
 */
final class VolumeGrid {

    /** How big a cell is, each way: about a bay or two of a building. */
    static final double CELL = 2000;
    /** A volume over more cells than this is listed as everywhere. */
    private static final int WIDE = 256;

    private final Map<Long, int[]> cells = new HashMap<>();
    private final int[] everywhere;

    VolumeGrid(final List<Volume> volumes) {
        final Map<Long, List<Integer>> lists = new HashMap<>();
        final List<Integer> wide = new ArrayList<>();
        for (int i = 0; i < volumes.size(); i++) {
            final Volume.Bounds b = volumes.get(i).bounds();
            final long x0 = cell(b.minX());
            final long x1 = cell(b.maxX());
            final long z0 = cell(b.minZ());
            final long z1 = cell(b.maxZ());
            if ((x1 - x0 + 1) * (z1 - z0 + 1) > WIDE) {
                wide.add(i);
                continue;
            }
            for (long cx = x0; cx <= x1; cx++) {
                for (long cz = z0; cz <= z1; cz++) {
                    lists.computeIfAbsent(key(cx, cz), k -> new ArrayList<>()).add(i);
                }
            }
        }
        lists.forEach((k, list) -> cells.put(k, list.stream().mapToInt(Integer::intValue).toArray()));
        everywhere = wide.stream().mapToInt(Integer::intValue).toArray();
    }

    private static long cell(final double at) {
        return (long) Math.floor(at / CELL);
    }

    private static long key(final long cx, final long cz) {
        return (cx << 32) ^ (cz & 0xffffffffL);
    }

    /** The volumes that may be under or over a circle at ({@code x}, {@code z}), {@code radius} round: each once, in order. */
    int[] near(final double x, final double z, final double radius) {
        int[] found = Arrays.copyOf(everywhere, Math.max(16, everywhere.length * 2));
        int n = everywhere.length;
        for (long cx = cell(x - radius); cx <= cell(x + radius); cx++) {
            for (long cz = cell(z - radius); cz <= cell(z + radius); cz++) {
                final int[] in = cells.get(key(cx, cz));
                if (in == null) {
                    continue;
                }
                if (n + in.length > found.length) {
                    found = Arrays.copyOf(found, Math.max(found.length * 2, n + in.length));
                }
                System.arraycopy(in, 0, found, n, in.length);
                n += in.length;
            }
        }
        // in order, each once (one in several cells is listed in each)
        Arrays.sort(found, 0, n);
        int kept = 0;
        for (int k = 0; k < n; k++) {
            if (kept == 0 || found[k] != found[kept - 1]) {
                found[kept++] = found[k];
            }
        }
        return Arrays.copyOf(found, kept);
    }
}
