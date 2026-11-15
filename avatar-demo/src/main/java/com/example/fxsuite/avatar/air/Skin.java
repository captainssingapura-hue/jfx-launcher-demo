package com.example.fxsuite.avatar.air;

import com.example.fxsuite.avatar.build.Point;

import java.util.List;

/**
 * A body's skin: rings of points round it, one after another along it, each as many points round
 * and each closed round; drawn smooth, ring to ring. Its ends are closed by rings drawn to a point
 * (an apex: a nose, a tail, the middle of a flat face), so it is closed all over.
 *
 * @param name  what it is
 * @param paint what it is finished in
 * @param rings its rings, end to end
 */
public record Skin(String name, Paint paint, List<List<Point>> rings) {

    public Skin {
        rings = rings.stream().map(List::copyOf).toList();
        if (rings.size() < 2) {
            throw new IllegalArgumentException(name + ": a skin of " + rings.size() + " rings");
        }
        final int round = rings.getFirst().size();
        if (round < 3 || rings.stream().anyMatch(r -> r.size() != round)) {
            throw new IllegalArgumentException(name + ": rings of different sizes, or too few points round");
        }
    }

    /**
     * A skin swept round the along axis: {@code profile}, each point {along, out from the axis}, a
     * closed loop (its first point again at its end), each point swept into a ring of {@code sides}
     * points round. A duct, a ring with a section of any shape.
     */
    public static Skin revolved(final String name, final Paint paint, final double[][] profile, final int sides) {
        final List<List<Point>> rings = new java.util.ArrayList<>();
        for (int k = 0; k <= profile.length; k++) {
            final double[] p = profile[k % profile.length];
            final List<Point> ring = new java.util.ArrayList<>();
            for (int j = 0; j < sides; j++) {
                final double a = 2 * Math.PI * j / sides;
                ring.add(new Point(p[1] * Math.cos(a), p[0], p[1] * Math.sin(a)));
            }
            rings.add(ring);
        }
        return new Skin(name, paint, rings);
    }

    /** The same skin, mounted ({@link Mount}). */
    public Skin placed(final Mount mount) {
        return new Skin(name, paint, rings.stream().map(r -> r.stream().map(mount::place).toList()).toList());
    }

    /** The same skin on the other side: across turned to the left. */
    public Skin mirrored() {
        return new Skin(name, paint, rings.stream().map(r -> r.stream().map(Skin::mirror).toList()).toList());
    }

    static Point mirror(final Point p) {
        return new Point(-p.across(), p.along(), p.up());
    }

    /** All its points. */
    public List<Point> points() {
        return rings.stream().flatMap(List::stream).toList();
    }

    /** Whether a ring is all one point: an apex. */
    public static boolean apex(final List<Point> ring) {
        final Point first = ring.getFirst();
        return ring.stream().allMatch(p -> p.plus(first.times(-1)).length() < 1e-6);
    }
}
