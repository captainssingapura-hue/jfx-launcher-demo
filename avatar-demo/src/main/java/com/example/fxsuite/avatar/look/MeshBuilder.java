package com.example.fxsuite.avatar.look;

import javafx.geometry.Point3D;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

/** A mesh put together a piece at a time. */
final class MeshBuilder {
    private float[] points = new float[3 * 1024];
    private int[] faces = new int[6 * 1024];
    private int pointCount;
    private int faceCount;
    /**
     * Each triangle's smoothing group: a quad's two triangles one, each quad of a box another, so a
     * box's faces are each shaded flat, not smoothed round its edges (which streaks long thin ones).
     */
    private int[] groups = new int[1024];
    private int quads;

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
        if (faceCount + 1 > groups.length) {
            groups = java.util.Arrays.copyOf(groups, groups.length * 2);
        }
        final int at = 6 * faceCount;
        faces[at] = a;
        faces[at + 2] = b;
        faces[at + 4] = c;
        groups[faceCount] = 1 << (quads % 6);
        faceCount++;
    }

    private void quad(final int a, final int b, final int c, final int d) {
        triangle(a, b, c);
        triangle(a, c, d);
        quads++;
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
        final Point3D[] corners = new Point3D[8];
        for (int k = 0; k < 8; k++) {
            corners[k] = middle.add((k & 1) == 0 ? -h : h, (k & 2) == 0 ? -h : h, (k & 4) == 0 ? -h : h);
        }
        box(corners);
    }

    /**
     * A box by its eight corners: corner k at the low or high end of its first edge as bit 0 of k
     * says, of its second as bit 1, of its third as bit 2.
     */
    void box(final Point3D[] corners) {
        final int[] at = new int[8];
        for (int k = 0; k < 8; k++) {
            at[k] = point(corners[k]);
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
        mesh.getFaceSmoothingGroups().setAll(groups, 0, faceCount);
        final MeshView view = new MeshView(mesh);
        view.setMaterial(material);
        view.setCullFace(CullFace.NONE);
        return view;
    }
}
