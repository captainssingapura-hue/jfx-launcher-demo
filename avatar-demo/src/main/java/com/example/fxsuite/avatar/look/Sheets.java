package com.example.fxsuite.avatar.look;

import javafx.scene.Group;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Thin sheets of metal (or glass), drawn: a renderer of its own, for shapes that are not boxes, such
 * as a vehicle's body panels. Only seen: nothing in the world meets them.
 *
 * <p>A {@link Sheet} is a flat panel: its outline, a polygon of corners in some frame (across, up,
 * along: as a node's x, -y and z), a thickness, and a {@link Slot} saying what it is made of and so
 * how it is coloured. Its outline may be any simple polygon, hollows and all (a car's side, seen
 * from the side), and is cut into triangles by clipping off its ears. Each sheet is drawn as a slab
 * as thick as it is: its face either side, lit from that side, and a rim round its edge; flat, so
 * that each panel reads as one. The sheets of a slot are drawn as one mesh.
 *
 * <p>Rough to begin with: few, flat, big panels. Finer shapes are more, smaller sheets, from the
 * same renderer.
 */
public final class Sheets {

    /** What a sheet is made of, and so how it is coloured: painted metal, dark trim, glass, and lamps, white at the front and red at the back. */
    public enum Slot { PAINT, TRIM, GLASS, LAMP, TAIL }

    /**
     * A flat panel: {@code corners} in order round its outline, each (across, up, along); as thick
     * as {@code thickness}, half of it either side of the outline's plane.
     */
    public record Sheet(List<double[]> corners, double thickness, Slot slot) {

        public Sheet {
            corners = List.copyOf(corners);
            if (corners.size() < 3) {
                throw new IllegalArgumentException("a sheet needs three corners at least");
            }
        }
    }

    private Sheets() {
    }

    /**
     * {@code sheets} as meshes, one for each slot, coloured by {@code materials}; in a frame whose
     * middle is {@code middle} up from where the sheets' heights are reckoned from.
     */
    public static Group draw(final List<Sheet> sheets, final Map<Slot, PhongMaterial> materials, final double middle) {
        final Map<Slot, TriangleMesh> meshes = new EnumMap<>(Slot.class);
        for (final Sheet sheet : sheets) {
            add(meshes.computeIfAbsent(sheet.slot(), s -> blank()), sheet, middle);
        }
        final Group group = new Group();
        for (final Map.Entry<Slot, TriangleMesh> mesh : meshes.entrySet()) {
            final MeshView view = new MeshView(mesh.getValue());
            view.setMaterial(materials.get(mesh.getKey()));
            view.setCullFace(CullFace.NONE);
            group.getChildren().add(view);
        }
        return group;
    }

    private static TriangleMesh blank() {
        final TriangleMesh mesh = new TriangleMesh();
        mesh.getTexCoords().addAll(0, 0);
        return mesh;
    }

    /** One sheet into {@code mesh}: its two faces and its rim, each lit from outside it. */
    private static void add(final TriangleMesh mesh, final Sheet sheet, final double middle) {
        final int n = sheet.corners().size();
        // each corner as a point of the node's frame: x across, y down, z along
        final double[][] p = new double[n][];
        for (int i = 0; i < n; i++) {
            final double[] c = sheet.corners().get(i);
            p[i] = new double[] {c[0], middle - c[1], c[2]};
        }
        final double[] normal = newell(p);
        final double half = sheet.thickness() / 2;
        final double[][] front = new double[n][];
        final double[][] back = new double[n][];
        for (int i = 0; i < n; i++) {
            front[i] = plus(p[i], normal, half);
            back[i] = plus(p[i], normal, -half);
        }
        final double[] centre = centroid(p);
        for (final int[] t : ears(p, normal)) {
            triangle(mesh, front[t[0]], front[t[1]], front[t[2]], normal);
            triangle(mesh, back[t[0]], back[t[1]], back[t[2]], scale(normal, -1));
        }
        // the rim: each edge, facing out from the middle of the sheet
        for (int i = 0; i < n; i++) {
            final int j = (i + 1) % n;
            final double[] mid = {(p[i][0] + p[j][0]) / 2, (p[i][1] + p[j][1]) / 2, (p[i][2] + p[j][2]) / 2};
            final double[] out = minus(mid, centre);
            triangle(mesh, front[i], front[j], back[j], out);
            triangle(mesh, front[i], back[j], back[i], out);
        }
    }

    /** One triangle, wound so that it faces {@code facing}: so that it is lit from that side. */
    private static void triangle(final TriangleMesh mesh, final double[] a, final double[] b, final double[] c,
                                 final double[] facing) {
        // JavaFX takes a face to look the way (b - a) x (c - a) points
        final double[] cross = cross(minus(b, a), minus(c, a));
        final boolean flip = dot(cross, facing) < 0;
        final int at = mesh.getPoints().size() / 3;
        for (final double[] v : flip ? new double[][] {a, c, b} : new double[][] {a, b, c}) {
            mesh.getPoints().addAll((float) v[0], (float) v[1], (float) v[2]);
        }
        mesh.getFaces().addAll(at, 0, at + 1, 0, at + 2, 0);
        // no smoothing: each face as flat as the panel it belongs to
        mesh.getFaceSmoothingGroups().addAll(0);
    }

    /**
     * The polygon {@code p} (in the plane square to {@code normal}) cut into triangles by clipping
     * off its ears, one by one: a corner whose triangle with its neighbours turns the polygon's way
     * and holds no other corner. Indices into {@code p}.
     */
    static List<int[]> ears(final double[][] p, final double[] normal) {
        // seen square on: drop the axis the plane faces most along
        final int drop = Math.abs(normal[0]) > Math.abs(normal[1])
                ? (Math.abs(normal[0]) > Math.abs(normal[2]) ? 0 : 2)
                : (Math.abs(normal[1]) > Math.abs(normal[2]) ? 1 : 2);
        final int u = drop == 0 ? 1 : 0;
        final int v = drop == 2 ? 1 : 2;
        final List<Integer> left = new ArrayList<>();
        for (int i = 0; i < p.length; i++) {
            left.add(i);
        }
        // which way round the outline goes, seen so
        double area = 0;
        for (int i = 0; i < p.length; i++) {
            final double[] a = p[i];
            final double[] b = p[(i + 1) % p.length];
            area += a[u] * b[v] - b[u] * a[v];
        }
        final double turn = Math.signum(area);
        final List<int[]> triangles = new ArrayList<>();
        int guard = 0;
        while (left.size() > 3 && guard++ < 10_000) {
            boolean clipped = false;
            for (int k = 0; k < left.size(); k++) {
                final int i0 = left.get((k + left.size() - 1) % left.size());
                final int i1 = left.get(k);
                final int i2 = left.get((k + 1) % left.size());
                final double[] a = p[i0];
                final double[] b = p[i1];
                final double[] c = p[i2];
                final double corner = (b[u] - a[u]) * (c[v] - a[v]) - (b[v] - a[v]) * (c[u] - a[u]);
                if (corner * turn <= 1e-9) {
                    continue;  // a hollow, not an ear
                }
                boolean empty = true;
                for (final int other : left) {
                    if (other != i0 && other != i1 && other != i2 && inside(p[other], a, b, c, u, v, turn)) {
                        empty = false;
                        break;
                    }
                }
                if (empty) {
                    triangles.add(new int[] {i0, i1, i2});
                    left.remove(k);
                    clipped = true;
                    break;
                }
            }
            if (!clipped) {
                break;  // no ear: a crossed outline; what is left is fanned out as it is
            }
        }
        for (int k = 1; k + 1 < left.size(); k++) {
            triangles.add(new int[] {left.get(0), left.get(k), left.get(k + 1)});
        }
        return triangles;
    }

    private static boolean inside(final double[] q, final double[] a, final double[] b, final double[] c,
                                  final int u, final int v, final double turn) {
        return side(a, b, q, u, v) * turn >= 0 && side(b, c, q, u, v) * turn >= 0 && side(c, a, q, u, v) * turn >= 0;
    }

    private static double side(final double[] a, final double[] b, final double[] q, final int u, final int v) {
        return (b[u] - a[u]) * (q[v] - a[v]) - (b[v] - a[v]) * (q[u] - a[u]);
    }

    /** The way a polygon faces, by Newell's method: sound even for one not quite flat. A unit vector. */
    private static double[] newell(final double[][] p) {
        final double[] n = new double[3];
        for (int i = 0; i < p.length; i++) {
            final double[] a = p[i];
            final double[] b = p[(i + 1) % p.length];
            n[0] += (a[1] - b[1]) * (a[2] + b[2]);
            n[1] += (a[2] - b[2]) * (a[0] + b[0]);
            n[2] += (a[0] - b[0]) * (a[1] + b[1]);
        }
        final double length = Math.sqrt(dot(n, n));
        return length < 1e-12 ? new double[] {0, -1, 0} : scale(n, 1 / length);
    }

    private static double[] centroid(final double[][] p) {
        final double[] c = new double[3];
        for (final double[] q : p) {
            c[0] += q[0] / p.length;
            c[1] += q[1] / p.length;
            c[2] += q[2] / p.length;
        }
        return c;
    }

    private static double[] plus(final double[] a, final double[] d, final double by) {
        return new double[] {a[0] + d[0] * by, a[1] + d[1] * by, a[2] + d[2] * by};
    }

    private static double[] minus(final double[] a, final double[] b) {
        return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    private static double[] scale(final double[] a, final double by) {
        return new double[] {a[0] * by, a[1] * by, a[2] * by};
    }

    private static double[] cross(final double[] a, final double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static double dot(final double[] a, final double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }
}
