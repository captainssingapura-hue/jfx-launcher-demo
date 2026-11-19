package com.example.fxsuite.avatar.buildings;

import com.example.fxsuite.avatar.build.Material;
import com.example.fxsuite.avatar.build.Point;
import com.example.fxsuite.avatar.build.Role;
import com.example.fxsuite.avatar.build.Solid;

import java.util.ArrayList;
import java.util.List;

/**
 * Flat things of any outline, as solids: a floor on its outline, cut where shafts go through it; and panels along an
 * outline's edges (glass, a spandrel). An outline is points round it, across (x) and along (z), cm, in its frame; any
 * outline that does not cross itself, its edges straight. A hole is a square cut (least and most across, least and
 * most along).
 *
 * <p>A floor is cut into strips across, at every corner of its outline and every side of its holes: in each strip the
 * outline's edges are straight lines, so each piece of it is a four-sided solid, its sides across square, its sides
 * along as the outline's edges run. Each piece is convex, as a solid must be to be drawn and stood on.
 */
final class Plates {

    private Plates() {
    }

    /**
     * A floor on {@code outline}, from {@code bottom} to {@code top}, cut where {@code holes} go through it, made of
     * {@code material}: its pieces, each a solid.
     */
    static List<Solid> floor(final String name, final List<double[]> outline, final List<double[]> holes,
                             final double bottom, final double top, final Role role, final Material material) {
        final List<Double> xs = new ArrayList<>();
        for (final double[] p : outline) {
            xs.add(p[0]);
        }
        double least = Double.POSITIVE_INFINITY;
        double most = Double.NEGATIVE_INFINITY;
        for (final double[] p : outline) {
            least = Math.min(least, p[0]);
            most = Math.max(most, p[0]);
        }
        for (final double[] h : holes) {
            xs.add(Math.clamp(h[0], least, most));
            xs.add(Math.clamp(h[1], least, most));
        }
        final List<Double> cuts = xs.stream().distinct().sorted().toList();
        final List<Solid> pieces = new ArrayList<>();
        for (int i = 0; i + 1 < cuts.size(); i++) {
            final double a = cuts.get(i);
            final double b = cuts.get(i + 1);
            if (b - a < 0.5) {
                continue;
            }
            final double mid = (a + b) / 2;
            // the outline's edges this strip crosses, in order along: each two an inside between them
            final List<double[]> edges = new ArrayList<>();
            for (int e = 0; e < outline.size(); e++) {
                final double[] p = outline.get(e);
                final double[] q = outline.get((e + 1) % outline.size());
                if (Math.min(p[0], q[0]) <= mid && mid < Math.max(p[0], q[0])) {
                    edges.add(new double[] {p[0], p[1], q[0], q[1]});
                }
            }
            edges.sort((e1, e2) -> Double.compare(along(e1, mid), along(e2, mid)));
            final List<double[]> crossing = holes.stream().filter(h -> h[0] < mid && mid < h[1])
                    .sorted((h1, h2) -> Double.compare(h1[2], h2[2])).toList();
            for (int k = 0; k + 1 < edges.size(); k += 2) {
                final double[] low = edges.get(k);
                final double[] high = edges.get(k + 1);
                // the inside between them, less any hole across it
                double[] from = {along(low, a), along(low, b)};
                for (final double[] h : crossing) {
                    if (h[3] <= along(low, mid) || h[2] >= along(high, mid)) {
                        continue;
                    }
                    final double[] to = {Math.min(h[2], along(high, a)), Math.min(h[2], along(high, b))};
                    if (to[0] - from[0] > 0.5 || to[1] - from[1] > 0.5) {
                        pieces.add(piece(name, a, b, from, to, bottom, top, role, material));
                    }
                    from = new double[] {Math.max(h[3], from[0]), Math.max(h[3], from[1])};
                }
                final double[] to = {along(high, a), along(high, b)};
                if (to[0] - from[0] > 0.5 || to[1] - from[1] > 0.5) {
                    pieces.add(piece(name, a, b, from, to, bottom, top, role, material));
                }
            }
        }
        return pieces;
    }

    /** Where along edge {@code e} (from x, from z, to x, to z) is at {@code x} across. */
    private static double along(final double[] e, final double x) {
        final double t = (x - e[0]) / (e[2] - e[0]);
        return e[1] + t * (e[3] - e[1]);
    }

    /** A piece from {@code a} to {@code b} across; along, from {@code from} to {@code to} at each. */
    private static Solid piece(final String name, final double a, final double b, final double[] from,
                               final double[] to, final double bottom, final double top, final Role role,
                               final Material material) {
        final List<Point> corners = new ArrayList<>(8);
        for (int c = 0; c < 8; c++) {
            final boolean far = (c & 1) != 0;
            corners.add(new Point(far ? b : a, (c & 2) == 0 ? (far ? from[1] : from[0]) : (far ? to[1] : to[0]),
                    (c & 4) == 0 ? bottom : top));
        }
        return new Solid(name, role, material, corners);
    }

    /**
     * A panel along {@code outline}'s edge from point {@code e} to the next, {@code thick} thick inward from it, from
     * {@code bottom} to {@code top}: glass, a spandrel. Its corners given so it is drawn its right way out whichever way
     * the edge runs.
     */
    static Solid edge(final String name, final List<double[]> outline, final int e, final double thick,
                      final double bottom, final double top, final Role role, final Material material) {
        return panel(name, outline.get(e), outline.get((e + 1) % outline.size()), Math.signum(area(outline)), thick,
                bottom, top, role, material);
    }

    /**
     * Panels along {@code outline}'s edge from point {@code e} to the next, as {@link #edge} makes one, from {@code s0}
     * to {@code s1} of the way along it, cut into as few of a width as are each no wider than {@code most}: panes of
     * glass, each to be broken on its own. Named {@code name}, each with its number after.
     */
    static List<Solid> panes(final String name, final List<double[]> outline, final int e, final double s0,
                             final double s1, final double most, final double thick, final double bottom,
                             final double top, final Role role, final Material material) {
        final double[] p = outline.get(e);
        final double[] q = outline.get((e + 1) % outline.size());
        final double inward = Math.signum(area(outline));
        final double length = Math.hypot(q[0] - p[0], q[1] - p[1]) * (s1 - s0);
        final int n = Math.max(1, (int) Math.ceil(length / most - 1e-9));
        final List<Solid> panes = new ArrayList<>(n);
        for (int k = 0; k < n; k++) {
            final double a = s0 + (s1 - s0) * k / n;
            final double b = s0 + (s1 - s0) * (k + 1) / n;
            panes.add(panel(name + " " + (k + 1), new double[] {p[0] + (q[0] - p[0]) * a, p[1] + (q[1] - p[1]) * a},
                    new double[] {p[0] + (q[0] - p[0]) * b, p[1] + (q[1] - p[1]) * b}, inward, thick, bottom, top,
                    role, material));
        }
        return panes;
    }

    /** A panel from {@code p} to {@code q}, {@code thick} thick to their left ({@code inward} 1) or right (-1). */
    private static Solid panel(final String name, final double[] p, final double[] q, final double inward,
                               final double thick, final double bottom, final double top, final Role role,
                               final Material material) {
        final double dx = q[0] - p[0];
        final double dz = q[1] - p[1];
        final double length = Math.hypot(dx, dz);
        // inward: to the left of the edge, going round the way the outline does (anticlockwise, x east and z north)
        final double nx = -dz / length * thick * inward;
        final double nz = dx / length * thick * inward;
        double[] a = p;
        double[] b = q;
        // corner 0 to 1 along the edge, 0 to 2 inward: turning as x does to z, or swapped so it does
        if (dx * nz - dz * nx < 0) {
            a = q;
            b = p;
        }
        final double[][] quad = {a, b, {a[0] + nx, a[1] + nz}, {b[0] + nx, b[1] + nz}};
        final List<Point> corners = new ArrayList<>(8);
        for (int c = 0; c < 8; c++) {
            final double[] at = quad[c & 3];
            corners.add(new Point(at[0], at[1], (c & 4) == 0 ? bottom : top));
        }
        return new Solid(name, role, material, corners);
    }

    /**
     * {@code outline} drawn in by {@code by} all round: each edge moved in square to itself, each corner where the
     * edges either side of it, so moved, meet. For a floor stopped at the inside of what stands round its edge (a
     * spandrel over it), not reaching its face: two faces in one plane flicker through each other, seen from afar.
     */
    static List<double[]> inset(final List<double[]> outline, final double by) {
        final int n = outline.size();
        final double inward = Math.signum(area(outline));
        final List<double[]> in = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            final double[] o = outline.get((i + n - 1) % n);
            final double[] p = outline.get(i);
            final double[] q = outline.get((i + 1) % n);
            // the edges either side, each as a direction and its inward square, of a length
            final double ax = p[0] - o[0];
            final double az = p[1] - o[1];
            final double al = Math.hypot(ax, az);
            final double bx = q[0] - p[0];
            final double bz = q[1] - p[1];
            final double bl = Math.hypot(bx, bz);
            final double[] na = {-az / al * inward, ax / al * inward};
            final double[] nb = {-bz / bl * inward, bx / bl * inward};
            final double cross = ax / al * (bz / bl) - az / al * (bx / bl);
            if (Math.abs(cross) < 1e-9) {
                // running straight on: in square to both
                in.add(new double[] {p[0] + na[0] * by, p[1] + na[1] * by});
                continue;
            }
            // where p + na·by + s·a meets p + nb·by + t·b: s from the two lines' cross
            final double dx = (nb[0] - na[0]) * by;
            final double dz = (nb[1] - na[1]) * by;
            final double s = (dx * (bz / bl) - dz * (bx / bl)) / cross;
            in.add(new double[] {p[0] + na[0] * by + s * ax / al, p[1] + na[1] * by + s * az / al});
        }
        return in;
    }

    /** Twice the area {@code outline} goes round: more than nothing anticlockwise (x east, z north). */
    static double area(final List<double[]> outline) {
        double sum = 0;
        for (int i = 0; i < outline.size(); i++) {
            final double[] a = outline.get(i);
            final double[] b = outline.get((i + 1) % outline.size());
            sum += a[0] * b[1] - b[0] * a[1];
        }
        return sum;
    }

    /** How big a floor's pieces are, all told, seen from above: m². */
    static double area(final List<Solid> pieces, final boolean metres) {
        double sum = 0;
        for (final Solid s : pieces) {
            final List<Point> c = s.corners();
            // its four corners round, from above
            final double[][] quad = {{c.get(0).across(), c.get(0).along()}, {c.get(1).across(), c.get(1).along()},
                {c.get(3).across(), c.get(3).along()}, {c.get(2).across(), c.get(2).along()}};
            double twice = 0;
            for (int i = 0; i < 4; i++) {
                twice += quad[i][0] * quad[(i + 1) % 4][1] - quad[(i + 1) % 4][0] * quad[i][1];
            }
            sum += Math.abs(twice) / 2;
        }
        return metres ? sum / 10_000 : sum;
    }
}
