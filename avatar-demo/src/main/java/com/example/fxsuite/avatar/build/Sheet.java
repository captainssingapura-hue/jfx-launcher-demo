package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * A face of a building of any shape, flat or curved, as triangles: its points, the triangles between
 * them, and at each point the way out of it (its normal). Made two ways:
 *
 * <ul>
 *   <li>{@link #flat}: a flat outline, holes in it (a wall and its windows; a doorway cut into its
 *       foot), cut into triangles by clipping ears, each hole first joined to the outline by a
 *       slit.</li>
 *   <li>{@link #patch}: a curved surface filled in from the curves round its edge (a Coons patch):
 *       four (a roof's slope: its eave, its hips, its ridge) or three (a hip's end, a spire's face, the
 *       apse's half cone), cut into a grid of triangles.</li>
 * </ul>
 *
 * <p>A design dresses it as a slab ({@link #slab}): each triangle a prism, from so far in to so far
 * out along the normals at its corners, so a curved slab has no cracks between its triangles.
 *
 * @param name      which it is
 * @param kind      what sort it is ("roof", "tower walls"): how a set of designs says which it dresses
 * @param points    its points
 * @param triangles its triangles, each three of its points' numbers, anticlockwise seen from outside
 * @param normals   the way out at each of its points, of length one
 */
public record Sheet(String name, String kind, List<Vec3> points, List<int[]> triangles, List<Vec3> normals) {

    public Sheet {
        points = List.copyOf(points);
        triangles = List.copyOf(triangles);
        normals = List.copyOf(normals);
    }

    /** How much of it there is, in square centimetres: its triangles' areas, all told. */
    public double area() {
        double area = 0;
        for (final int[] t : triangles) {
            area += cross(points.get(t[1]).subtract(points.get(t[0])), points.get(t[2]).subtract(points.get(t[0])))
                    .magnitude() / 2;
        }
        return area;
    }

    static Vec3 cross(final Vec3 a, final Vec3 b) {
        return new Vec3(a.y() * b.z() - a.z() * b.y(), a.z() * b.x() - a.x() * b.z(), a.x() * b.y() - a.y() * b.x());
    }

    /**
     * A flat face: its outline {@code outer} and {@code holes} in it, all in one plane, its outside the
     * side {@code out} points to (a direction). Cut into triangles: each hole joined to the outline by a
     * slit from its point furthest one way to the nearest point of the outline it can see, then ears
     * clipped from what is left.
     */
    public static Sheet flat(final String name, final String kind, final List<Vec3> outer, final List<List<Vec3>> holes,
                             final Vec3 out) {
        // its plane: out, and two directions in it
        final Vec3 normal = out.normalize();
        final Vec3 any = Math.abs(normal.z()) < 0.9 ? new Vec3(0, 0, 1) : new Vec3(1, 0, 0);
        final Vec3 u = cross(any, normal).normalize();
        final Vec3 v = cross(normal, u);
        final List<Vec3> points = new ArrayList<>();
        final List<double[]> flat = new ArrayList<>();
        final List<Integer> ring = loop(outer, points, flat, u, v, true);
        // the holes, the furthest along u first, each slit into the ring
        final List<List<Integer>> rings = new ArrayList<>();
        for (final List<Vec3> hole : holes) {
            rings.add(loop(hole, points, flat, u, v, false));
        }
        rings.sort((a, b) -> Double.compare(furthest(b, flat), furthest(a, flat)));
        for (final List<Integer> hole : rings) {
            slit(ring, hole, flat);
        }
        final List<int[]> triangles = clip(ring, flat);
        final List<Vec3> normals = new ArrayList<>();
        points.forEach(p -> normals.add(normal));
        return new Sheet(name, kind, points, triangles, normals);
    }

    /** {@code loop}'s points onto {@code points}, as flat (u, v) too; anticlockwise if {@code outline}, else clockwise. */
    private static List<Integer> loop(final List<Vec3> loop, final List<Vec3> points, final List<double[]> flat,
                                      final Vec3 u, final Vec3 v, final boolean outline) {
        final List<Integer> ring = new ArrayList<>();
        for (final Vec3 p : loop) {
            ring.add(points.size());
            points.add(p);
            flat.add(new double[] {p.dotProduct(u), p.dotProduct(v)});
        }
        if ((area(ring, flat) > 0) != outline) {
            java.util.Collections.reverse(ring);
        }
        return ring;
    }

    private static double area(final List<Integer> ring, final List<double[]> flat) {
        double twice = 0;
        for (int k = 0; k < ring.size(); k++) {
            final double[] a = flat.get(ring.get(k));
            final double[] b = flat.get(ring.get((k + 1) % ring.size()));
            twice += a[0] * b[1] - b[0] * a[1];
        }
        return twice / 2;
    }

    private static double furthest(final List<Integer> ring, final List<double[]> flat) {
        return ring.stream().mapToDouble(i -> flat.get(i)[0]).max().orElse(0);
    }

    /** {@code hole} slit into {@code ring}: from its point furthest along u to the nearest point of the ring it can see, and back. */
    private static void slit(final List<Integer> ring, final List<Integer> hole, final List<double[]> flat) {
        int h = 0;
        for (int k = 1; k < hole.size(); k++) {
            if (flat.get(hole.get(k))[0] > flat.get(hole.get(h))[0]) {
                h = k;
            }
        }
        final double[] from = flat.get(hole.get(h));
        int best = -1;
        double nearest = Double.POSITIVE_INFINITY;
        for (int r = 0; r < ring.size(); r++) {
            final double[] to = flat.get(ring.get(r));
            final double d = Math.hypot(to[0] - from[0], to[1] - from[1]);
            if (d < nearest && sees(from, to, ring, hole, flat)) {
                nearest = d;
                best = r;
            }
        }
        if (best < 0) {
            best = 0;
        }
        // the ring to its point, round the hole from its point and back to it, back to the ring's point, on
        final List<Integer> joined = new ArrayList<>(ring.subList(0, best + 1));
        for (int k = 0; k <= hole.size(); k++) {
            joined.add(hole.get((h + k) % hole.size()));
        }
        joined.add(ring.get(best));
        joined.addAll(ring.subList(best + 1, ring.size()));
        ring.clear();
        ring.addAll(joined);
    }

    /** Whether the line from {@code a} to {@code b} crosses no edge of {@code ring} or {@code hole} (but at its ends). */
    private static boolean sees(final double[] a, final double[] b, final List<Integer> ring, final List<Integer> hole,
                                final List<double[]> flat) {
        for (final List<Integer> loop : List.of(ring, hole)) {
            for (int k = 0; k < loop.size(); k++) {
                final double[] c = flat.get(loop.get(k));
                final double[] d = flat.get(loop.get((k + 1) % loop.size()));
                if (crosses(a, b, c, d)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean crosses(final double[] a, final double[] b, final double[] c, final double[] d) {
        final double d1 = turn(c, d, a);
        final double d2 = turn(c, d, b);
        final double d3 = turn(a, b, c);
        final double d4 = turn(a, b, d);
        return d1 * d2 < -1e-9 && d3 * d4 < -1e-9;
    }

    private static double turn(final double[] a, final double[] b, final double[] c) {
        return (b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0]);
    }

    /** {@code ring} (anticlockwise, maybe slit) cut into triangles by clipping ears. */
    private static List<int[]> clip(final List<Integer> ring, final List<double[]> flat) {
        final List<Integer> left = new ArrayList<>(ring);
        final List<int[]> triangles = new ArrayList<>();
        int guard = 0;
        while (left.size() > 3 && guard++ < ring.size() * ring.size() + 10) {
            boolean cut = false;
            for (int k = 0; k < left.size(); k++) {
                final int a = left.get((k + left.size() - 1) % left.size());
                final int b = left.get(k);
                final int c = left.get((k + 1) % left.size());
                if (turn(flat.get(a), flat.get(b), flat.get(c)) <= 1e-9) {
                    continue;
                }
                boolean empty = true;
                for (final int p : left) {
                    if (p != a && p != b && p != c && !same(flat.get(p), flat.get(a)) && !same(flat.get(p), flat.get(b))
                            && !same(flat.get(p), flat.get(c)) && within(flat.get(p), flat.get(a), flat.get(b), flat.get(c))) {
                        empty = false;
                        break;
                    }
                }
                if (empty) {
                    triangles.add(new int[] {a, b, c});
                    left.remove(k);
                    cut = true;
                    break;
                }
            }
            if (!cut) {
                // nothing more clips cleanly (points in a line): cut off a corner that does not turn back
                for (int k = 0; k < left.size(); k++) {
                    final int a = left.get((k + left.size() - 1) % left.size());
                    final int b = left.get(k);
                    final int c = left.get((k + 1) % left.size());
                    if (turn(flat.get(a), flat.get(b), flat.get(c)) >= -1e-9) {
                        left.remove(k);
                        cut = true;
                        break;
                    }
                }
                if (!cut) {
                    break;
                }
            }
        }
        if (left.size() == 3 && turn(flat.get(left.get(0)), flat.get(left.get(1)), flat.get(left.get(2))) > 1e-9) {
            triangles.add(new int[] {left.get(0), left.get(1), left.get(2)});
        }
        return triangles;
    }

    private static boolean same(final double[] a, final double[] b) {
        return Math.abs(a[0] - b[0]) < 1e-6 && Math.abs(a[1] - b[1]) < 1e-6;
    }

    private static boolean within(final double[] p, final double[] a, final double[] b, final double[] c) {
        return turn(a, b, p) >= -1e-9 && turn(b, c, p) >= -1e-9 && turn(c, a, p) >= -1e-9;
    }

    /**
     * A curved surface filled in from the curves round it (a Coons patch): {@code bottom} and
     * {@code top} running the same way, {@code left} and {@code right} from the bottom's ends to the
     * top's; a top of one point, a three-sided one. Each curve as points along it, as many as it has;
     * resampled, so the patch is {@code across} triangles' pairs one way, {@code up} the other. Its
     * outside the side away from {@code inside}.
     */
    public static Sheet patch(final String name, final String kind, final List<Vec3> bottom, final List<Vec3> right,
                              final List<Vec3> top, final List<Vec3> left, final int across, final int up,
                              final Vec3 inside) {
        final List<Vec3> b = resample(bottom, across);
        final List<Vec3> t = top.size() == 1 ? java.util.Collections.nCopies(across + 1, top.getFirst())
                : resample(top, across);
        final List<Vec3> l = resample(left, up);
        final List<Vec3> r = resample(right, up);
        final Vec3 p00 = b.getFirst();
        final Vec3 p10 = b.getLast();
        final Vec3 p01 = t.getFirst();
        final Vec3 p11 = t.getLast();
        final Vec3[][] grid = new Vec3[across + 1][up + 1];
        for (int i = 0; i <= across; i++) {
            final double s = (double) i / across;
            for (int j = 0; j <= up; j++) {
                final double w = (double) j / up;
                final Vec3 ruled = b.get(i).multiply(1 - w).add(t.get(i).multiply(w))
                        .add(l.get(j).multiply(1 - s)).add(r.get(j).multiply(s));
                final Vec3 corners = p00.multiply((1 - s) * (1 - w)).add(p10.multiply(s * (1 - w)))
                        .add(p01.multiply((1 - s) * w)).add(p11.multiply(s * w));
                grid[i][j] = ruled.subtract(corners);
            }
        }
        final List<Vec3> points = new ArrayList<>();
        final List<Vec3> normals = new ArrayList<>();
        for (int i = 0; i <= across; i++) {
            for (int j = 0; j <= up; j++) {
                points.add(grid[i][j]);
                // the way out here: square to the grid's two ways, away from inside
                final Vec3 di = grid[Math.min(i + 1, across)][j].subtract(grid[Math.max(i - 1, 0)][j]);
                Vec3 dj = grid[i][Math.min(j + 1, up)].subtract(grid[i][Math.max(j - 1, 0)]);
                if (dj.magnitude() < 1e-9) {
                    dj = grid[i][Math.min(j + 1, up)].subtract(grid[i][Math.max(j - 2, 0)]);
                }
                Vec3 n = cross(di, dj);
                if (n.magnitude() < 1e-9) {
                    n = grid[i][j].subtract(inside);
                }
                n = n.normalize();
                normals.add(n.dotProduct(grid[i][j].subtract(inside)) < 0 ? n.multiply(-1) : n);
            }
        }
        final List<int[]> triangles = new ArrayList<>();
        for (int i = 0; i < across; i++) {
            for (int j = 0; j < up; j++) {
                final int a = i * (up + 1) + j;
                final int c = (i + 1) * (up + 1) + j;
                triangles.add(new int[] {a, c, c + 1});
                if (points.get(a + 1).distance(points.get(c + 1)) > 1e-6) {
                    triangles.add(new int[] {a, c + 1, a + 1});
                }
            }
        }
        return new Sheet(name, kind, points, triangles, normals);
    }

    /** {@code curve}'s points resampled to {@code pieces + 1} evenly along it. */
    static List<Vec3> resample(final List<Vec3> curve, final int pieces) {
        final double[] along = new double[curve.size()];
        for (int k = 1; k < curve.size(); k++) {
            along[k] = along[k - 1] + curve.get(k).distance(curve.get(k - 1));
        }
        final double length = along[along.length - 1];
        final List<Vec3> out = new ArrayList<>();
        int k = 1;
        for (int p = 0; p <= pieces; p++) {
            final double want = length * p / pieces;
            while (k < curve.size() - 1 && along[k] < want) {
                k++;
            }
            final double span = along[k] - along[k - 1];
            final double f = span < 1e-12 ? 0 : Math.clamp((want - along[k - 1]) / span, 0, 1);
            out.add(curve.get(k - 1).add(curve.get(k).subtract(curve.get(k - 1)).multiply(f)));
        }
        return out;
    }

    /**
     * A slab of it, from {@code w0} to {@code w1} out along its normals, doing {@code role}: a prism on
     * each of its triangles, each a box two of whose corners are one.
     */
    public List<Solid> slab(final String called, final Palette palette, final Role role, final double w0,
                            final double w1) {
        final List<Solid> slab = new ArrayList<>();
        int n = 0;
        for (final int[] t : triangles) {
            final int[] quad = {t[0], t[1], t[2], t[2]};
            final List<Point> corners = new ArrayList<>(8);
            for (int k = 0; k < 8; k++) {
                final int p = quad[(k & 1) + ((k & 2) == 0 ? 0 : 2)];
                final Vec3 at = points.get(p).add(normals.get(p).multiply((k & 4) == 0 ? w0 : w1));
                corners.add(new Point(at.x(), at.y(), at.z()));
            }
            slab.add(new Solid(name + ", " + called + " " + n++, role, palette.of(role), corners));
        }
        return slab;
    }
}
