package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.sim.Block;

import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A garden's round pieces drawn as their tops alone, none over another: a lawn, with holes where the
 * paving at its middle and the ponds in it lie, so that nothing lies a few centimetres over it and
 * flickers through it far off; a pond's rim a ring round its water, its water a ring round its island.
 * Each swept round its middle, a ray at a time: where along each ray it is, and where not.
 */
final class GardenMesh {

    /** How many rays round: a lawn a hundred metres across, round to a few centimetres at its edge. */
    static final int SIDES = 240;

    private GardenMesh() {
    }

    /** A hole in a lawn, round: its middle (from the lawn's) and how far round. */
    record Disc(double x, double z, double radius) {
    }

    /** A ring's top, {@code y} (the part's own, down), from {@code inner} round its middle out to {@code outer}. */
    static MeshView ring(final double inner, final double outer, final float y) {
        final List<double[]> rays = new ArrayList<>();
        for (int k = 0; k <= SIDES; k++) {
            rays.add(new double[] {inner, outer});
        }
        return swept(rays, y);
    }

    /**
     * A ring with walls, standing: its top {@code y} (the part's own, down) from {@code inner} round its
     * middle out to {@code outer}, walled down to {@code bottom} round its outside and, if it is a ring
     * and not a disc, round its inside too: a kerb round a pond, an island.
     */
    static MeshView walled(final double inner, final double outer, final float y, final float bottom) {
        final TriangleMesh mesh = new TriangleMesh();
        mesh.getTexCoords().addAll(0, 0);
        // its points: round its inside and outside, at its top and at its foot, ray by ray
        for (int k = 0; k <= SIDES; k++) {
            final double a = 2 * Math.PI * k / SIDES;
            final float sx = (float) Math.sin(a);
            final float cz = (float) Math.cos(a);
            mesh.getPoints().addAll((float) inner * sx, y, (float) inner * cz, (float) outer * sx, y, (float) outer * cz,
                    (float) inner * sx, bottom, (float) inner * cz, (float) outer * sx, bottom, (float) outer * cz);
        }
        for (int k = 0; k < SIDES; k++) {
            final int p = 4 * k;
            final int q = 4 * (k + 1);
            // its top, facing up
            up(mesh, p, p + 1, q + 1);
            up(mesh, p, q + 1, q);
            // its outside, and its inside, each a band from its top down
            mesh.getFaces().addAll(p + 1, 0, q + 1, 0, q + 3, 0, p + 1, 0, q + 3, 0, p + 3, 0);
            mesh.getFaceSmoothingGroups().addAll(2, 2);
            if (inner > 0) {
                mesh.getFaces().addAll(p, 0, p + 2, 0, q + 2, 0, p, 0, q + 2, 0, q, 0);
                mesh.getFaceSmoothingGroups().addAll(4, 4);
            }
        }
        final MeshView view = new MeshView(mesh);
        view.setCullFace(CullFace.NONE);
        return view;
    }

    /**
     * A lawn's top, {@code y} (the part's own, down), {@code radius} round its middle: but where
     * {@code middle} lies (a box about its own middle, from the lawn's, turned its way), if its middle is
     * in it, and where each of {@code spokes} runs out from that (a path to it); and where each of
     * {@code holes} lies, each a disc wholly in it, apart from the others.
     */
    static MeshView lawn(final double radius, final Block middle, final List<Block> spokes, final List<Disc> holes,
                         final float y) {
        final List<double[]> rays = new ArrayList<>();
        for (int k = 0; k <= SIDES; k++) {
            final double a = 2 * Math.PI * k / SIDES;
            final double dx = Math.sin(a);
            final double dz = Math.cos(a);
            double from = middle == null ? 0 : out(middle, dx, dz);
            // a spoke the ray is in where it leaves the middle: on to where it leaves that too
            for (final Block spoke : spokes) {
                final double[] through = through(spoke, dx, dz);
                if (through != null && through[0] <= from + 1 && through[1] > from) {
                    from = Math.min(radius, through[1]);
                }
            }
            // each hole along the ray: where it goes in and out; past one, where it comes nearest, no wider
            final List<double[]> spans = new ArrayList<>();
            for (final Disc hole : holes) {
                final double nearest = hole.x() * dx + hole.z() * dz;
                final double aside = hole.x() * hole.x() + hole.z() * hole.z() - nearest * nearest;
                final double half = aside < hole.radius() * hole.radius() ? Math.sqrt(hole.radius() * hole.radius() - aside) : 0;
                spans.add(new double[] {nearest, nearest - half, nearest + half});
            }
            // in order along it (two change places only where both are no wider than a point, at its middle)
            spans.sort(Comparator.comparingDouble(s -> s[0]));
            final double[] ray = new double[2 + 2 * spans.size()];
            ray[0] = from;
            for (int s = 0; s < spans.size(); s++) {
                ray[1 + 2 * s] = spans.get(s)[1];
                ray[2 + 2 * s] = spans.get(s)[2];
            }
            ray[ray.length - 1] = radius;
            // each where it is, between where the lawn starts and its edge, none before the one before it
            for (int i = 0; i < ray.length; i++) {
                ray[i] = Math.clamp(ray[i], from, radius);
                if (i > 0) {
                    ray[i] = Math.max(ray[i], ray[i - 1]);
                }
            }
            rays.add(ray);
        }
        return swept(rays, y);
    }

    /**
     * The ground's top, a square {@code half} each way about the world's middle, at the ground (y 0),
     * but where a lawn {@code radius} round ({@code x}, {@code z}) lies: open there, its edge meeting the
     * lawn's, ray for ray ({@link #SIDES}), so neither lies over the other nor leaves a gap.
     */
    static MeshView ground(final double half, final double x, final double z, final double radius) {
        final TriangleMesh mesh = new TriangleMesh();
        mesh.getTexCoords().addAll(0, 0);
        for (int k = 0; k <= SIDES; k++) {
            final double a = 2 * Math.PI * k / SIDES;
            final double dx = Math.sin(a);
            final double dz = Math.cos(a);
            final double reach = edge(half, x, z, dx, dz);
            mesh.getPoints().addAll((float) (x + radius * dx), 0, (float) (z + radius * dz),
                    (float) (x + reach * dx), 0, (float) (z + reach * dz));
        }
        for (int k = 0; k < SIDES; k++) {
            final int p = 2 * k;
            final int q = 2 * (k + 1);
            up(mesh, p, p + 1, q + 1);
            up(mesh, p, q + 1, q);
        }
        final MeshView view = new MeshView(mesh);
        view.setCullFace(CullFace.NONE);
        return view;
    }

    /**
     * Where a ray from the lawn's middle along ({@code dx}, {@code dz}) goes into {@code box} (its middle
     * given from the lawn's) and where it comes out, distances along it; null if it misses it.
     */
    static double[] through(final Block box, final double dx, final double dz) {
        final double c = Math.cos(Math.toRadians(box.yaw()));
        final double s = Math.sin(Math.toRadians(box.yaw()));
        final double oa = -box.x() * c + box.z() * s;
        final double ol = -box.x() * s - box.z() * c;
        final double da = dx * c - dz * s;
        final double dl = dx * s + dz * c;
        double in = 0;
        double out = Double.POSITIVE_INFINITY;
        for (final double[] slab : new double[][] {{oa, da, box.halfWidth()}, {ol, dl, box.halfDepth()}}) {
            if (Math.abs(slab[1]) < 1e-12) {
                if (Math.abs(slab[0]) > slab[2]) {
                    return null;
                }
                continue;
            }
            final double t0 = (-slab[2] - slab[0]) / slab[1];
            final double t1 = (slab[2] - slab[0]) / slab[1];
            in = Math.max(in, Math.min(t0, t1));
            out = Math.min(out, Math.max(t0, t1));
        }
        return in < out ? new double[] {in, out} : null;
    }

    /** How far from ({@code x}, {@code z}) along ({@code dx}, {@code dz}) the edge of a square {@code half} each way about the world's middle is. */
    static double edge(final double half, final double x, final double z, final double dx, final double dz) {
        double reach = Double.POSITIVE_INFINITY;
        if (Math.abs(dx) > 1e-12) {
            reach = Math.min(reach, ((dx > 0 ? half : -half) - x) / dx);
        }
        if (Math.abs(dz) > 1e-12) {
            reach = Math.min(reach, ((dz > 0 ? half : -half) - z) / dz);
        }
        return reach;
    }

    /**
     * How far a ray from the lawn's middle along ({@code dx}, {@code dz}) goes before it leaves
     * {@code box} (its middle given from the lawn's); 0 if it starts outside it.
     */
    static double out(final Block box, final double dx, final double dz) {
        final double c = Math.cos(Math.toRadians(box.yaw()));
        final double s = Math.sin(Math.toRadians(box.yaw()));
        // the lawn's middle, and the way along the ray, in the box's own frame (its across and along)
        final double ox = -box.x();
        final double oz = -box.z();
        final double oa = ox * c - oz * s;
        final double ol = ox * s + oz * c;
        if (Math.abs(oa) > box.halfWidth() || Math.abs(ol) > box.halfDepth()) {
            return 0;
        }
        final double da = dx * c - dz * s;
        final double dl = dx * s + dz * c;
        double t = Double.POSITIVE_INFINITY;
        if (Math.abs(da) > 1e-12) {
            t = Math.min(t, ((da > 0 ? box.halfWidth() : -box.halfWidth()) - oa) / da);
        }
        if (Math.abs(dl) > 1e-12) {
            t = Math.min(t, ((dl > 0 ? box.halfDepth() : -box.halfDepth()) - ol) / dl);
        }
        return t;
    }

    /**
     * The top swept round: each ray in turn, its pairs of distances along it (from, to; from, to; ...),
     * each pair a stretch of it; each stretch joined to the same one on the next ray.
     */
    private static MeshView swept(final List<double[]> rays, final float y) {
        final TriangleMesh mesh = new TriangleMesh();
        mesh.getTexCoords().addAll(0, 0);
        final int each = rays.getFirst().length;
        for (int k = 0; k < rays.size(); k++) {
            final double a = 2 * Math.PI * k / SIDES;
            for (final double r : rays.get(k)) {
                mesh.getPoints().addAll((float) (r * Math.sin(a)), y, (float) (r * Math.cos(a)));
            }
        }
        for (int k = 0; k + 1 < rays.size(); k++) {
            for (int s = 0; s + 1 < each; s += 2) {
                final int p = k * each + s;
                final int q = (k + 1) * each + s;
                // its quad, two triangles, wound to face up: lit from above
                up(mesh, p, p + 1, q + 1);
                up(mesh, p, q + 1, q);
            }
        }
        final MeshView view = new MeshView(mesh);
        view.setCullFace(CullFace.NONE);
        return view;
    }

    /** Triangle {@code a}, {@code b}, {@code c} added, wound so that it faces up (the way the world's y does not). */
    private static void up(final TriangleMesh mesh, final int a, final int b, final int c) {
        final float ax = mesh.getPoints().get(3 * a);
        final float az = mesh.getPoints().get(3 * a + 2);
        final double ux = mesh.getPoints().get(3 * b) - ax;
        final double uz = mesh.getPoints().get(3 * b + 2) - az;
        final double vx = mesh.getPoints().get(3 * c) - ax;
        final double vz = mesh.getPoints().get(3 * c + 2) - az;
        if (ux * vz - uz * vx >= 0) {
            mesh.getFaces().addAll(a, 0, b, 0, c, 0);
        } else {
            mesh.getFaces().addAll(a, 0, c, 0, b, 0);
        }
        mesh.getFaceSmoothingGroups().addAll(1);
    }
}
