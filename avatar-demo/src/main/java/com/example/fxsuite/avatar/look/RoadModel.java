package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.sim.Road;
import com.example.fxsuite.avatar.sim.RoadMap;
import com.example.fxsuite.avatar.sim.Vec2;
import com.example.fxsuite.avatar.sim.Vec3;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

import java.util.ArrayList;
import java.util.List;

/**
 * The lines painted on the roads ({@link RoadMap.Kind#markings}): along each road marked out in lanes, each of
 * its lines a thin strip, as wide as it is, laid {@link #LIFT} over the road's surface where it is across it
 * (its camber and all), solid or dashed, each dash cut where it starts and ends along the road. None on a
 * bridge, whose deck has its own.
 *
 * <p>In stretches of {@link #STRETCH} of a road's points, each shown only within {@link #SEEN} of the camera:
 * further off, a line a hand wide is lost anyway, and the land drawn under it no longer so close to the road.
 */
public final class RoadModel {

    /** How far over the road's surface its lines lie: enough not to be lost in it, near. */
    static final double LIFT = 5;
    /** How many of a road's points a stretch of its lines is. */
    static final int STRETCH = 50;
    /** How far off a stretch is shown; and how much nearer it must come back to be shown again. */
    static final double SEEN = 35_000;
    static final double BACK = 0.9;
    private static final Color PAINT = Color.web("#e6e4dc");

    /** A stretch of lines: its mesh, the box round it, whether shown. */
    private static final class Stretch {
        final MeshView view;
        final double minX;
        final double minZ;
        final double maxX;
        final double maxZ;
        boolean shown = true;

        Stretch(final MeshView view, final double[] box) {
            this.view = view;
            this.minX = box[0];
            this.minZ = box[1];
            this.maxX = box[2];
            this.maxZ = box[3];
        }

        double away(final Vec3 eye) {
            final double dx = Math.max(0, Math.max(minX - eye.x(), eye.x() - maxX));
            final double dz = Math.max(0, Math.max(minZ - eye.z(), eye.z() - maxZ));
            return Math.hypot(dx, dz);
        }
    }

    private final Group root = new Group();
    private final List<Stretch> stretches = new ArrayList<>();

    public RoadModel(final List<Road> roads) {
        final PhongMaterial paint = new PhongMaterial(PAINT);
        paint.setSpecularColor(Color.BLACK);
        for (final Road road : roads) {
            final int n = road.line().size();
            for (int from = 0; from + 1 < n; from += STRETCH) {
                final Lines lines = lines(road, from, Math.min(n - 1, from + STRETCH));
                if (lines.faces().length == 0) {
                    continue;
                }
                final TriangleMesh mesh = new TriangleMesh();
                mesh.getPoints().setAll(lines.points());
                mesh.getTexCoords().setAll(0, 0);
                mesh.getFaces().setAll(lines.faces());
                final MeshView view = new MeshView(mesh);
                view.setMaterial(paint);
                view.setCullFace(CullFace.NONE);
                root.getChildren().add(view);
                stretches.add(new Stretch(view, lines.box()));
            }
        }
        seenFrom(Vec3.ZERO);
    }

    public Node node() {
        return root;
    }

    /** Each stretch shown, or not, by how far {@code eye} is from it. */
    public void seenFrom(final Vec3 eye) {
        for (final Stretch s : stretches) {
            final boolean now = s.away(eye) < SEEN * (s.shown ? 1 : BACK);
            if (now != s.shown) {
                s.shown = now;
                s.view.setVisible(now);
            }
        }
    }

    /** A stretch of a road's lines: its points (the world's y down), its triangles (point, texture, each), the box round it. */
    record Lines(float[] points, int[] faces, double[] box) {
    }

    /**
     * Road {@code road}'s lines from its point {@code from} to its point {@code to}: for each segment between two
     * points, off a bridge, each of its kind's markings, as much of it as is painted there (all of a solid line;
     * of a dashed one, the dashes falling there, cut where they start and end).
     */
    static Lines lines(final Road road, final int from, final int to) {
        final List<Float> points = new ArrayList<>();
        final List<Integer> faces = new ArrayList<>();
        final double[] box = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
                Double.NEGATIVE_INFINITY};
        final List<Vec2> line = road.line();
        for (int i = from; i < to; i++) {
            if (road.bridged(i) || road.bridged(i + 1)) {
                continue;
            }
            final double s0 = road.alignment().along(i);
            final double s1 = road.alignment().along(i + 1);
            if (s1 <= s0) {
                continue;
            }
            for (final RoadMap.Marking mark : road.kind(i).markings()) {
                if (mark.solid()) {
                    strip(road, line, i, 0, 1, mark, points, faces, box);
                    continue;
                }
                final double period = mark.dash() + mark.gap();
                for (double k = Math.floor(s0 / period) * period; k < s1; k += period) {
                    final double a = Math.max(s0, k);
                    final double b = Math.min(s1, k + mark.dash());
                    if (b > a) {
                        strip(road, line, i, (a - s0) / (s1 - s0), (b - s0) / (s1 - s0), mark, points, faces, box);
                    }
                }
            }
        }
        final float[] p = new float[points.size()];
        for (int k = 0; k < p.length; k++) {
            p[k] = points.get(k);
        }
        return new Lines(p, faces.stream().mapToInt(Integer::intValue).toArray(), box);
    }

    /** Mark {@code mark} between road's points {@code i} and {@code i + 1}, from {@code t0} to {@code t1} of the way: a strip. */
    private static void strip(final Road road, final List<Vec2> line, final int i, final double t0, final double t1,
                              final RoadMap.Marking mark, final List<Float> points, final List<Integer> faces,
                              final double[] box) {
        final int base = points.size() / 3;
        for (final double t : new double[] {t0, t1}) {
            final Vec2 a = line.get(i);
            final Vec2 b = line.get(i + 1);
            final double dx = b.x() - a.x();
            final double dz = b.z() - a.z();
            final double length = Math.hypot(dx, dz);
            // to the left, going along it
            final double lx = -dz / length;
            final double lz = dx / length;
            final double half = (road.wide(i) + (road.wide(i + 1) - road.wide(i)) * t) / 2;
            final double x = a.x() + dx * t;
            final double z = a.z() + dz * t;
            for (final double edge : new double[] {-mark.wide() / 2, mark.wide() / 2}) {
                // so far in from the road's left edge
                final double across = half - mark.from() + edge;
                final double up = road.surface(i, across) + (road.surface(i + 1, across) - road.surface(i, across)) * t;
                final double px = x + lx * across;
                final double pz = z + lz * across;
                points.add((float) px);
                points.add((float) -(up + LIFT));
                points.add((float) pz);
                box[0] = Math.min(box[0], px);
                box[1] = Math.min(box[1], pz);
                box[2] = Math.max(box[2], px);
                box[3] = Math.max(box[3], pz);
            }
        }
        // its two triangles: point and texture, each
        for (final int v : new int[] {base, base + 2, base + 1, base + 1, base + 2, base + 3}) {
            faces.add(v);
            faces.add(0);
        }
    }
}
