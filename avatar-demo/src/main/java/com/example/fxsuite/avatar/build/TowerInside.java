package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Box;
import com.example.fxsuite.avatar.sim.Inside;

/**
 * The wall tower's inside ({@link WallTower#inside}): its rooms, worked out by arithmetic from its size, nothing
 * stored to find them. A room is a storey of a leg, between the middles of two of its walls (the walls shared by the
 * rooms each side), from its one face to its other, balconies and all: the storey is the height over a storey's,
 * rounded down; the bay, which two walls it falls between; the leg, west or east, or the shaft above where they meet,
 * which of its legs' faces it falls between at that height. The atrium between the legs, and all outside its faces,
 * is in no room. Its roof is its top storey's.
 *
 * <p>Rooms are numbered storey by storey from the ground; in a storey, its west leg's (or the shaft's) bays first,
 * then its east leg's.
 */
public final class TowerInside implements Inside {

    private final int storeys;
    private final int bays;
    private final double storey;
    /** Along it, where each bay begins and ends: bay {@code j} from {@code marks[j]} to {@code marks[j + 1]}. */
    private final double[] marks;
    /** Each storey's legs, each its west face and east face at the storey's floor and its top. */
    private final double[][][][] legs;
    /** The first room of each storey; and, at the end, how many there are. */
    private final int[] first;
    private final int[] storeyOf;
    private final Box bounds;

    TowerInside(final WallTower tower) {
        final WallTower.Size size = tower.size();
        storeys = size.storeys();
        bays = size.walls() - 1;
        storey = size.storey();
        marks = new double[size.walls()];
        for (int j = 0; j < size.walls(); j++) {
            marks[j] = tower.wallAt(j);
        }
        marks[0] = -size.length() / 2;
        marks[bays] = size.length() / 2;
        legs = new double[storeys][][][];
        first = new int[storeys + 1];
        for (int k = 0; k < storeys; k++) {
            legs[k] = tower.legs(k * storey, (k + 1) * storey).toArray(new double[0][][]);
            first[k + 1] = first[k] + legs[k].length * bays;
        }
        storeyOf = new int[first[storeys]];
        for (int k = 0; k < storeys; k++) {
            for (int c = first[k]; c < first[k + 1]; c++) {
                storeyOf[c] = k;
            }
        }
        double west = Double.POSITIVE_INFINITY;
        double east = Double.NEGATIVE_INFINITY;
        for (final double[][][] storeyLegs : legs) {
            for (final double[][] leg : storeyLegs) {
                west = Math.min(west, Math.min(leg[0][0], leg[0][1]));
                east = Math.max(east, Math.max(leg[1][0], leg[1][1]));
            }
        }
        bounds = new Box(west, east, marks[0], marks[bays], 0, size.high() + WallTower.FLOOR);
    }

    // --- by storey, leg and bay --------------------------------------------------------------------------------

    /** The room of storey {@code k}, its leg {@code leg} (0 the west, or the shaft; 1 the east), bay {@code j}. */
    public int cell(final int k, final int leg, final int j) {
        if (k < 0 || k >= storeys || leg < 0 || leg >= legs[k].length || j < 0 || j >= bays) {
            throw new IllegalArgumentException("no room at storey " + k + ", leg " + leg + ", bay " + j);
        }
        return first[k] + leg * bays + j;
    }

    public int storey(final int cell) {
        return storeyOf[check(cell)];
    }

    public int leg(final int cell) {
        return (check(cell) - first[storeyOf[cell]]) / bays;
    }

    public int bay(final int cell) {
        return (check(cell) - first[storeyOf[cell]]) % bays;
    }

    /** How many legs storey {@code k} has: two, with the atrium between, or one, the shaft. */
    public int legs(final int k) {
        return legs[k].length;
    }

    private int check(final int cell) {
        if (cell < 0 || cell >= count()) {
            throw new IllegalArgumentException("no room " + cell + " of " + count());
        }
        return cell;
    }

    // --- as an inside ------------------------------------------------------------------------------------------

    @Override
    public Box bounds() {
        return bounds;
    }

    @Override
    public int count() {
        return first[storeys];
    }

    @Override
    public int cell(final double across, final double along, final double up) {
        if (!bounds.holds(across, along, up)) {
            return -1;
        }
        final int k = Math.min(storeys - 1, (int) Math.floor(up / storey));
        int j = 0;
        while (j < bays - 1 && along > marks[j + 1]) {
            j++;
        }
        // its faces straight up each storey, as it is built
        final double s = Math.clamp((up - k * storey) / storey, 0, 1);
        for (int leg = 0; leg < legs[k].length; leg++) {
            final double[][] faces = legs[k][leg];
            final double west = faces[0][0] + (faces[0][1] - faces[0][0]) * s;
            final double east = faces[1][0] + (faces[1][1] - faces[1][0]) * s;
            if (across >= west && across <= east) {
                return first[k] + leg * bays + j;
            }
        }
        return -1;
    }

    @Override
    public int[] cells(final Box box) {
        if (!bounds.meets(box)) {
            return new int[0];
        }
        final int k0 = Math.clamp((long) Math.ceil(box.low() / storey) - 1, 0, storeys - 1);
        final int k1 = Math.clamp((long) Math.floor(box.high() / storey), 0, storeys - 1);
        int j0 = 0;
        while (marks[j0 + 1] < box.minZ()) {
            j0++;
        }
        int j1 = bays - 1;
        while (marks[j1] > box.maxZ()) {
            j1--;
        }
        int n = 0;
        int[] found = new int[(k1 - k0 + 1) * 2 * (j1 - j0 + 1)];
        for (int k = k0; k <= k1; k++) {
            for (int leg = 0; leg < legs[k].length; leg++) {
                if (west(k, leg) <= box.maxX() && east(k, leg) >= box.minX()) {
                    for (int j = j0; j <= j1; j++) {
                        found[n++] = first[k] + leg * bays + j;
                    }
                }
            }
        }
        return java.util.Arrays.copyOf(found, n);
    }

    @Override
    public Box box(final int cell) {
        final int k = storey(cell);
        final int leg = leg(cell);
        final int j = bay(cell);
        final double top = k == storeys - 1 ? bounds.high() : (k + 1) * storey;
        return new Box(west(k, leg), east(k, leg), marks[j], marks[j + 1], k * storey, top);
    }

    /** How far west a leg of a storey reaches, at its floor or its top. */
    private double west(final int k, final int leg) {
        return Math.min(legs[k][leg][0][0], legs[k][leg][0][1]);
    }

    private double east(final int k, final int leg) {
        return Math.max(legs[k][leg][1][0], legs[k][leg][1][1]);
    }
}
