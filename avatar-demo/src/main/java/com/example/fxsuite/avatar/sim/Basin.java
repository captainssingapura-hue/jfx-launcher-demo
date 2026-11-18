package com.example.fxsuite.avatar.sim;

import java.util.ArrayList;
import java.util.List;

/**
 * Where the ground goes down under still water ({@link Land.Layer}): within its shores, as deep as {@code depth} under
 * the ground round it, going down over a bank {@code bank} wide from the shore. Its shores are lines, open or closed,
 * and which side of them is water is told by how many of them lie between a point and far off (an odd number in, an
 * even out), so they need not be joined into rings: a river's shore and a bay's that meet at its mouth, the stretch
 * they share left out of both, are one water. Its {@code bounds} are lines that close its shores into rings without
 * being shore (where land is cut off at the world's edge): counted, but no bank along them. Turned {@code outside},
 * the water is where an even number lie between: the sea round the land, and the bay within it, as one.
 *
 * <p>Worked out once on a grid {@link #CELL} apart over its shores, as a survey is: in or out by its rows, how far from
 * the nearest shore by each shore's own reach; read between the grid's points smoothly.
 */
public final class Basin implements Land.Layer {

    /** How far apart its grid's points are. */
    public static final double CELL = 250;

    private final double depth;
    private final double bank;
    private final double x0;
    private final double z0;
    private final int across;
    private final int along;
    /** How far under the ground round it the bed is at each point of its grid: 0 on land. */
    private final float[] down;

    /**
     * @param shores  lines along which water meets land, in the world's x and z
     * @param bounds  lines that close the shores into rings, without being shore
     * @param outside whether the water is where an even number of lines lie between a point and far off, not an odd
     * @param x0      its grid's west edge; and its east, south and north
     */
    public Basin(final List<List<Vec2>> shores, final List<List<Vec2>> bounds, final boolean outside, final double depth,
                 final double bank, final double x0, final double x1, final double z0, final double z1) {
        if (!(depth > 0 && bank > 0 && x1 > x0 && z1 > z0)) {
            throw new IllegalArgumentException("a basin some depth, its bank some width, over some ground");
        }
        this.depth = depth;
        this.bank = bank;
        this.x0 = x0;
        this.z0 = z0;
        across = (int) Math.ceil((x1 - x0) / CELL) + 1;
        along = (int) Math.ceil((z1 - z0) / CELL) + 1;
        final List<double[]> lines = new ArrayList<>();
        final List<double[]> shore = new ArrayList<>();
        for (final List<Vec2> line : shores) {
            segments(line, lines);
            segments(line, shore);
        }
        for (final List<Vec2> line : bounds) {
            segments(line, lines);
        }
        // how far from the nearest shore, near one; far, as far as a bank
        final float[] far = new float[across * along];
        java.util.Arrays.fill(far, (float) bank);
        for (final double[] s : shore) {
            final int i0 = Math.max(0, (int) Math.floor((Math.min(s[0], s[2]) - bank - x0) / CELL));
            final int i1 = Math.min(across - 1, (int) Math.ceil((Math.max(s[0], s[2]) + bank - x0) / CELL));
            final int j0 = Math.max(0, (int) Math.floor((Math.min(s[1], s[3]) - bank - z0) / CELL));
            final int j1 = Math.min(along - 1, (int) Math.ceil((Math.max(s[1], s[3]) + bank - z0) / CELL));
            for (int j = j0; j <= j1; j++) {
                for (int i = i0; i <= i1; i++) {
                    final double d = distance(x0 + i * CELL, z0 + j * CELL, s);
                    if (d < far[j * across + i]) {
                        far[j * across + i] = (float) d;
                    }
                }
            }
        }
        // in or out, row by row: where every line crosses the row, in order; water between each two, or outside them
        down = new float[across * along];
        final double[] crossings = new double[lines.size()];
        for (int j = 0; j < along; j++) {
            final double z = z0 + j * CELL;
            int n = 0;
            for (final double[] s : lines) {
                // half-open, so a line's end on the row is counted once
                if ((s[1] <= z) != (s[3] <= z)) {
                    crossings[n++] = s[0] + (z - s[1]) / (s[3] - s[1]) * (s[2] - s[0]);
                }
            }
            java.util.Arrays.sort(crossings, 0, n);
            int k = 0;
            for (int i = 0; i < across; i++) {
                final double x = x0 + i * CELL;
                while (k < n && crossings[k] <= x) {
                    k++;
                }
                final boolean wet = (k % 2 == 1) != outside;
                if (wet) {
                    final double t = far[j * across + i] / bank;
                    // down over its bank, eased in at its top and its foot
                    down[j * across + i] = (float) (depth * t * t * (3 - 2 * t));
                }
            }
        }
    }

    private static void segments(final List<Vec2> line, final List<double[]> into) {
        for (int k = 0; k + 1 < line.size(); k++) {
            final Vec2 a = line.get(k);
            final Vec2 b = line.get(k + 1);
            into.add(new double[] {a.x(), a.z(), b.x(), b.z()});
        }
    }

    private static double distance(final double x, final double z, final double[] s) {
        final double dx = s[2] - s[0];
        final double dz = s[3] - s[1];
        final double length = dx * dx + dz * dz;
        final double t = length == 0 ? 0 : Math.clamp(((x - s[0]) * dx + (z - s[1]) * dz) / length, 0, 1);
        return Math.hypot(x - (s[0] + t * dx), z - (s[1] + t * dz));
    }

    /** How far under the ground round it its bed is at ({@code x}, {@code z}): 0 on land, up to its depth. */
    public double down(final double x, final double z) {
        final double fi = (x - x0) / CELL;
        final double fj = (z - z0) / CELL;
        if (fi < 0 || fj < 0 || fi > across - 1 || fj > along - 1) {
            return 0;
        }
        final int i = Math.min(across - 2, (int) fi);
        final int j = Math.min(along - 2, (int) fj);
        final double u = fi - i;
        final double v = fj - j;
        final double a = down[j * across + i] * (1 - u) + down[j * across + i + 1] * u;
        final double b = down[(j + 1) * across + i] * (1 - u) + down[(j + 1) * across + i + 1] * u;
        return a * (1 - v) + b * v;
    }

    @Override
    public double shape(final double x, final double z, final double under) {
        return under - down(x, z);
    }

    @Override
    public boolean touches(final double minX, final double minZ, final double maxX, final double maxZ) {
        return maxX >= x0 && minX <= x0 + (across - 1) * CELL && maxZ >= z0 && minZ <= z0 + (along - 1) * CELL;
    }

    @Override
    public double deepest() {
        return depth;
    }
}
