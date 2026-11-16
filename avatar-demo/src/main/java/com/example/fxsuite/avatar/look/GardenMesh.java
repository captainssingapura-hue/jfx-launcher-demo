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
     * A lawn's top, {@code y} (the part's own, down), {@code radius} round its middle: but where
     * {@code middle} lies (a box about its own middle, from the lawn's, turned its way), if its middle is
     * in it; and where each of {@code holes} lies, each a disc wholly in it, apart from the others.
     */
    static MeshView lawn(final double radius, final Block middle, final List<Disc> holes, final float y) {
        final List<double[]> rays = new ArrayList<>();
        for (int k = 0; k <= SIDES; k++) {
            final double a = 2 * Math.PI * k / SIDES;
            final double dx = Math.sin(a);
            final double dz = Math.cos(a);
            final double from = middle == null ? 0 : out(middle, dx, dz);
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
