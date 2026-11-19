package com.example.fxsuite.avatar.lifts;

import com.example.fxsuite.avatar.build.Material;
import com.example.fxsuite.avatar.build.Role;
import com.example.fxsuite.avatar.build.Solid;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * What the lifts' buildings are made of: floors cut where a lift goes through them, a rail along a floor's open edge,
 * columns under the floors. Each square to the frame, across (x) and along (z), up from the ground.
 */
final class Frame {

    /** A floor so thick; a rail so high and so thick; a column so wide. */
    static final double SLAB = 25;
    static final double RAIL = 100;
    static final double RAIL_THICK = 5;
    static final double COLUMN = 40;

    private Frame() {
    }

    /**
     * A floor from {@code x0} to {@code x1} across, {@code z0} to {@code z1} along, its top at {@code top}, cut where
     * {@code holes} (each min x, max x, min z, max z) go through it: in boxes, each square, round the holes.
     */
    static List<Solid> floor(final String name, final double x0, final double x1, final double z0, final double z1,
                             final double top, final List<double[]> holes) {
        final List<Double> xs = new ArrayList<>(List.of(x0, x1));
        for (final double[] h : holes) {
            xs.add(Math.clamp(h[0], x0, x1));
            xs.add(Math.clamp(h[1], x0, x1));
        }
        final List<Double> across = xs.stream().distinct().sorted().toList();
        final List<Solid> boxes = new ArrayList<>();
        for (int i = 0; i + 1 < across.size(); i++) {
            final double a = across.get(i);
            final double b = across.get(i + 1);
            final double mid = (a + b) / 2;
            // along this strip, what the holes crossing it leave
            final List<double[]> cuts = holes.stream().filter(h -> h[0] < mid && mid < h[1])
                    .sorted(Comparator.comparingDouble(h -> h[2])).toList();
            double from = z0;
            for (final double[] h : cuts) {
                if (h[2] > from) {
                    boxes.add(slab(name, a, b, from, Math.min(h[2], z1), top));
                }
                from = Math.max(from, h[3]);
            }
            if (z1 > from) {
                boxes.add(slab(name, a, b, from, z1, top));
            }
        }
        return boxes;
    }

    private static Solid slab(final String name, final double a0, final double a1, final double l0, final double l1,
                              final double top) {
        return new Box(name, Role.BASE, Material.CONCRETE, a0, a1, l0, l1, top - SLAB, top).solid();
    }

    /** A rail along {@code x0} to {@code x1} across, {@code z0} to {@code z1} along, on a floor whose top is {@code top}. */
    static Solid rail(final String name, final double x0, final double x1, final double z0, final double z1,
                      final double top) {
        return new Box(name, Role.GLAZING, Material.GLASS, x0, x1, z0, z1, top, top + RAIL).solid();
    }

    /** A column at ({@code x}, {@code z}), from the ground up to under a floor whose top is {@code top}. */
    static Solid column(final double x, final double z, final double top) {
        final double c = COLUMN / 2;
        return new Box("column", Role.STRUCTURE, Material.CONCRETE, x - c, x + c, z - c, z + c, 0, top - SLAB).solid();
    }

    /**
     * Floors over the ground at each of {@code tops} (the ground's own left out), from {@code x0} to {@code x1} across
     * and {@code z0} to {@code z1} along, cut for {@code holes}; railed along their front ({@code z1}) and sides; a
     * column at each front corner up to the highest. Behind ({@code z0}), whatever they back onto.
     */
    static List<Solid> landings(final List<Double> tops, final double x0, final double x1, final double z0,
                                final double z1, final List<double[]> holes) {
        final List<Solid> solids = new ArrayList<>();
        final double t = RAIL_THICK;
        double highest = 0;
        for (int f = 0; f < tops.size(); f++) {
            final double top = tops.get(f);
            if (top <= 0) {
                continue;
            }
            highest = Math.max(highest, top);
            solids.addAll(floor("floor " + f, x0, x1, z0, z1, top, holes));
            solids.add(rail("rail, front, floor " + f, x0, x1, z1 - t, z1, top));
            solids.add(rail("rail, left, floor " + f, x0, x0 + t, z0, z1 - t, top));
            solids.add(rail("rail, right, floor " + f, x1 - t, x1, z0, z1 - t, top));
        }
        if (highest > 0) {
            final double c = COLUMN / 2;
            solids.add(column(x0 + c, z1 - c, highest));
            solids.add(column(x1 - c, z1 - c, highest));
        }
        return solids;
    }
}
