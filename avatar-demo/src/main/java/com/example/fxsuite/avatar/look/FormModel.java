package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.sim.Form;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

import java.util.EnumMap;
import java.util.Map;

/**
 * A {@link Form}, drawn as its skeleton: each edge a thin square rod from vertex to vertex, all of
 * them that lean alike in one mesh (upright, level or sloped, each in a colour of its own, so a
 * building's columns, floors and roofs read apart at a glance); and, if asked, a small cube at each
 * vertex. A building of thousands of edges is so drawn as a handful of meshes, not thousands of
 * shapes. In its owner's frame, standing on the ground: across to the right is x, up is -y (y points
 * down), along ahead is z.
 */
public final class FormModel {

    /** How an edge leans: straight up, level, or sloped between. */
    public enum Lean { UPRIGHT, LEVEL, SLOPED }

    /** Within this of straight up, or of level, an edge counts as that: about a twentieth of a degree. */
    private static final double SQUARE = 1e-3;

    private FormModel() {
    }

    /** How {@code edge} of {@code form} leans. */
    public static Lean lean(final Form form, final Form.Edge edge) {
        final Form.Vertex a = form.vertices().get(edge.from());
        final Form.Vertex b = form.vertices().get(edge.to());
        final double rise = Math.abs(b.up() - a.up()) / a.distance(b);
        return rise > 1 - SQUARE ? Lean.UPRIGHT : rise < SQUARE ? Lean.LEVEL : Lean.SLOPED;
    }

    /**
     * Its edges, {@code thickness} across, each in the material for how it leans; and its vertices,
     * if {@code vertices}, as cubes twice as big, in {@code dots}.
     */
    public static Group of(final Form form, final double thickness, final Map<Lean, PhongMaterial> materials,
                           final boolean vertices, final PhongMaterial dots) {
        final Map<Lean, Mesh> meshes = new EnumMap<>(Lean.class);
        for (final Lean lean : Lean.values()) {
            meshes.put(lean, new Mesh());
        }
        for (final Form.Edge edge : form.edges()) {
            meshes.get(lean(form, edge)).rod(point(form.vertices().get(edge.from())),
                    point(form.vertices().get(edge.to())), thickness);
        }
        final Group drawn = new Group();
        for (final Lean lean : Lean.values()) {
            if (!meshes.get(lean).empty()) {
                drawn.getChildren().add(meshes.get(lean).view(materials.get(lean)));
            }
        }
        if (vertices && !form.vertices().isEmpty()) {
            final Mesh cubes = new Mesh();
            for (final Form.Vertex v : form.vertices()) {
                cubes.cube(point(v), thickness * 2.2);
            }
            drawn.getChildren().add(cubes.view(dots));
        }
        return drawn;
    }

    /** Where {@code vertex} is, in the model's terms. */
    public static Point3D point(final Form.Vertex vertex) {
        return new Point3D(vertex.across(), -vertex.up(), vertex.along());
    }

    /** A mesh put together a piece at a time. */
    private static final class Mesh {
        private float[] points = new float[3 * 1024];
        private int[] faces = new int[6 * 1024];
        private int pointCount;
        private int faceCount;

        boolean empty() {
            return faceCount == 0;
        }

        private int point(final Point3D p) {
            if (3 * (pointCount + 1) > points.length) {
                points = java.util.Arrays.copyOf(points, points.length * 2);
            }
            points[3 * pointCount] = (float) p.getX();
            points[3 * pointCount + 1] = (float) p.getY();
            points[3 * pointCount + 2] = (float) p.getZ();
            return pointCount++;
        }

        private void triangle(final int a, final int b, final int c) {
            if (6 * (faceCount + 1) > faces.length) {
                faces = java.util.Arrays.copyOf(faces, faces.length * 2);
            }
            final int at = 6 * faceCount;
            faces[at] = a;
            faces[at + 2] = b;
            faces[at + 4] = c;
            faceCount++;
        }

        private void quad(final int a, final int b, final int c, final int d) {
            triangle(a, b, c);
            triangle(a, c, d);
        }

        /** A square rod {@code width} across from {@code a} to {@code b}. */
        void rod(final Point3D a, final Point3D b, final double width) {
            final Point3D run = b.subtract(a).normalize();
            // two directions square to the run, and to each other
            final Point3D other = Math.abs(run.getY()) < 0.9 ? new Point3D(0, 1, 0) : new Point3D(1, 0, 0);
            final Point3D u = run.crossProduct(other).normalize().multiply(width / 2);
            final Point3D v = run.crossProduct(u).normalize().multiply(width / 2);
            final int[] at = new int[8];
            final Point3D[] corners = {u.add(v), u.subtract(v), u.multiply(-1).subtract(v), v.subtract(u)};
            for (int k = 0; k < 4; k++) {
                at[k] = point(a.add(corners[k]));
                at[k + 4] = point(b.add(corners[k]));
            }
            for (int k = 0; k < 4; k++) {
                final int next = (k + 1) % 4;
                quad(at[k], at[next], at[next + 4], at[k + 4]);
            }
            quad(at[0], at[1], at[2], at[3]);
            quad(at[4], at[7], at[6], at[5]);
        }

        /** A cube {@code size} across about {@code middle}. */
        void cube(final Point3D middle, final double size) {
            final double h = size / 2;
            final int[] at = new int[8];
            for (int k = 0; k < 8; k++) {
                at[k] = point(middle.add((k & 1) == 0 ? -h : h, (k & 2) == 0 ? -h : h, (k & 4) == 0 ? -h : h));
            }
            quad(at[0], at[1], at[3], at[2]);
            quad(at[4], at[6], at[7], at[5]);
            quad(at[0], at[4], at[5], at[1]);
            quad(at[2], at[3], at[7], at[6]);
            quad(at[0], at[2], at[6], at[4]);
            quad(at[1], at[5], at[7], at[3]);
        }

        MeshView view(final PhongMaterial material) {
            final TriangleMesh mesh = new TriangleMesh();
            mesh.getPoints().setAll(points, 0, 3 * pointCount);
            mesh.getTexCoords().setAll(0, 0);
            mesh.getFaces().setAll(faces, 0, 6 * faceCount);
            final MeshView view = new MeshView(mesh);
            view.setMaterial(material);
            view.setCullFace(CullFace.NONE);
            return view;
        }
    }
}
