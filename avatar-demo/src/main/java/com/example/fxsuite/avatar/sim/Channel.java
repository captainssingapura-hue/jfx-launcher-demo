package com.example.fxsuite.avatar.sim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A river's channel cut into the land ({@link Land.Layer}): along its middle {@code line}, a bed
 * {@code bed} wide, level, {@code depth} below the ground beside it; each side, a bank rising from the
 * bed to the ground over {@code bank}, easing out of the one and into the other, so there is no edge
 * at its foot or its top to trip on. Worked out exactly from the line wherever it is asked.
 *
 * <p>The line's segments are kept by where they are on a grid of cells ({@link #CELL}), each cell
 * listing those that come within the channel's reach of it: so a point far from it is told so at once,
 * and one near it looks at a few segments.
 */
public final class Channel implements Land.Layer {

    /** How wide the cells the line's segments are kept in. */
    static final double CELL = 2_000;
    /** How far down from the top of its banks the grass thins out to bare earth. */
    static final double FRINGE = 150;

    private final List<Vec2> line;
    private final double bed;
    private final double bank;
    private final double depth;
    private final Map<Long, int[]> cells = new HashMap<>();

    public Channel(final List<Vec2> line, final double bed, final double bank, final double depth) {
        this.line = List.copyOf(line);
        if (line.size() < 2 || !(bed >= 0) || !(bank > 0) || !(depth > 0)) {
            throw new IllegalArgumentException("a channel runs somewhere, its banks some way across, some depth");
        }
        this.bed = bed;
        this.bank = bank;
        this.depth = depth;
        final Map<Long, List<Integer>> kept = new HashMap<>();
        final double reach = reach();
        for (int s = 0; s + 1 < line.size(); s++) {
            final Vec2 a = line.get(s);
            final Vec2 b = line.get(s + 1);
            for (long i = cell(Math.min(a.x(), b.x()) - reach); i <= cell(Math.max(a.x(), b.x()) + reach); i++) {
                for (long j = cell(Math.min(a.z(), b.z()) - reach); j <= cell(Math.max(a.z(), b.z()) + reach); j++) {
                    kept.computeIfAbsent(key(i, j), k -> new ArrayList<>()).add(s);
                }
            }
        }
        kept.forEach((k, segments) -> cells.put(k, segments.stream().mapToInt(Integer::intValue).toArray()));
    }

    /** How far from its middle line it reaches: half its bed, and a bank. */
    public double reach() {
        return bed / 2 + bank;
    }

    public List<Vec2> line() {
        return line;
    }

    public double depth() {
        return depth;
    }

    @Override
    public double shape(final double x, final double z, final double under) {
        return under - cut(distance(x, z));
    }

    /** How far below the ground beside it the channel is, {@code away} from its middle line. */
    double cut(final double away) {
        if (away >= reach()) {
            return 0;
        }
        if (away <= bed / 2) {
            return depth;
        }
        final double t = (away - bed / 2) / bank;
        return depth * (1 - t * t * (3 - 2 * t));
    }

    /** How far ({@code x}, {@code z}) is from its middle line; infinity if beyond its reach. */
    public double distance(final double x, final double z) {
        final int[] near = cells.get(key(cell(x), cell(z)));
        if (near == null) {
            return Double.POSITIVE_INFINITY;
        }
        double nearest = Double.POSITIVE_INFINITY;
        for (final int s : near) {
            nearest = Math.min(nearest, toSegment(x, z, line.get(s), line.get(s + 1)));
        }
        return nearest;
    }

    @Override
    public boolean touches(final double minX, final double minZ, final double maxX, final double maxZ) {
        final double reach = reach();
        for (long i = cell(minX); i <= cell(maxX); i++) {
            for (long j = cell(minZ); j <= cell(maxZ); j++) {
                final int[] near = cells.get(key(i, j));
                if (near == null) {
                    continue;
                }
                for (final int s : near) {
                    if (segmentNearBox(line.get(s), line.get(s + 1), minX - reach, minZ - reach, maxX + reach, maxZ + reach)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public double deepest() {
        return depth;
    }

    /**
     * Its bed and its banks, bare earth: no grass grows where the water runs, or comes up to; the grass
     * thinning out over the top {@link #FRINGE} of its banks.
     */
    @Override
    public double bare(final double x, final double z) {
        final double t = (reach() - distance(x, z)) / FRINGE;
        return t <= 0 ? 0 : t >= 1 ? 1 : t * t * (3 - 2 * t);
    }

    private static long cell(final double v) {
        return (long) Math.floor(v / CELL);
    }

    private static long key(final long i, final long j) {
        return i * 0x1_0000_0000L + (j & 0xffff_ffffL);
    }

    private static double toSegment(final double x, final double z, final Vec2 a, final Vec2 b) {
        final double dx = b.x() - a.x();
        final double dz = b.z() - a.z();
        final double length = dx * dx + dz * dz;
        final double t = length == 0 ? 0 : Num.clamp(((x - a.x()) * dx + (z - a.z()) * dz) / length, 0, 1);
        return Math.hypot(x - a.x() - t * dx, z - a.z() - t * dz);
    }

    /** Whether segment a–b comes into the box: either end in it, or it crosses one of the box's sides. */
    private static boolean segmentNearBox(final Vec2 a, final Vec2 b, final double minX, final double minZ,
                                          final double maxX, final double maxZ) {
        if (inBox(a, minX, minZ, maxX, maxZ) || inBox(b, minX, minZ, maxX, maxZ)) {
            return true;
        }
        final Vec2[] corners = {new Vec2(minX, minZ), new Vec2(maxX, minZ), new Vec2(maxX, maxZ), new Vec2(minX, maxZ)};
        for (int k = 0; k < 4; k++) {
            if (Contour.cross(a, b, corners[k], corners[(k + 1) % 4])) {
                return true;
            }
        }
        return false;
    }

    private static boolean inBox(final Vec2 p, final double minX, final double minZ, final double maxX, final double maxZ) {
        return p.x() >= minX && p.x() <= maxX && p.z() >= minZ && p.z() <= maxZ;
    }
}
