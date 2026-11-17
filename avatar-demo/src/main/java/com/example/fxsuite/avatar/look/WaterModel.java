package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.sim.Land;
import com.example.fxsuite.avatar.sim.Pond;
import com.example.fxsuite.avatar.sim.River;
import com.example.fxsuite.avatar.sim.Vec2;
import com.example.fxsuite.avatar.sim.Vec3;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;

import java.util.ArrayList;
import java.util.List;

/**
 * Water lying in the land ({@link Land}): each river's, along its line, its top as high over the ground
 * under its line as it is deep ({@link River#top}), as wide as it is and {@link #TUCKED} more each side,
 * so its edges go in under its banks and its shore is where its top meets them; each pond's, flat, its top
 * its level, over its loop, the land round it higher than it. Lit as the land is, and, from
 * {@link Horizon#HAZE} out, fading into the haze as it does ({@link Horizon#haze}): its base colour lit by
 * the sun, the rest in one picture lit by itself ({@link TerrainModel#picture}).
 */
public final class WaterModel {

    /** How far past where it is as wide as it is a river's water goes, in under its banks. */
    static final double TUCKED = 60;
    /** Water's colour, and its base colour, lit by the sun: the rest of it lighting itself. */
    static final Color WATER = Color.web("#3b6f8c");
    static final Color BASE = Color.web("#2d5568");
    private static final int HAZES = 64;

    private final Group root = new Group();
    private final List<TriangleMesh> meshes = new ArrayList<>();
    private final List<float[]> places = new ArrayList<>();
    private final List<float[]> coords = new ArrayList<>();
    private Vec3 hazedFrom;
    private double hazedReach;

    public WaterModel(final Land land, final List<River> rivers, final List<Pond> ponds, final double[] lit) {
        final PhongMaterial material = new PhongMaterial(BASE);
        material.setSpecularColor(Color.BLACK);
        material.setSelfIlluminationMap(picture(lit));
        for (final River river : rivers) {
            add(river(land, river), material);
        }
        for (final Pond pond : ponds) {
            add(pond(pond), material);
        }
        seenFrom(View.all(Vec3.ZERO));
    }

    public Node node() {
        return root;
    }

    /**
     * Its haze worked out again as {@code view} sees, if the camera has moved far enough since, or how far it sees has
     * changed enough.
     */
    public void seenFrom(final View view) {
        final Vec3 eye = view.eye();
        if (hazedFrom != null && Math.hypot(eye.x() - hazedFrom.x(), eye.z() - hazedFrom.z()) <= TerrainModel.MOVED
                && Math.abs(view.reach() - hazedReach) <= TerrainModel.SHARE * view.reach()) {
            return;
        }
        hazedFrom = eye;
        hazedReach = view.reach();
        for (int m = 0; m < meshes.size(); m++) {
            final float[] points = places.get(m);
            final float[] c = coords.get(m);
            for (int v = 0; v < c.length / 2; v++) {
                final double haze = view.haze(Math.hypot(points[3 * v] - eye.x(), points[3 * v + 2] - eye.z()));
                c[2 * v + 1] = (float) ((haze * (HAZES - 1) + 0.5) / HAZES);
            }
            meshes.get(m).getTexCoords().setAll(c);
        }
    }

    private void add(final float[][] made, final PhongMaterial material) {
        final float[] points = made[0];
        final int n = points.length / 3;
        final float[] normals = new float[3 * n];
        final float[] c = new float[2 * n];
        for (int v = 0; v < n; v++) {
            // up, the world's y down
            normals[3 * v + 1] = -1;
            c[2 * v] = 0.5f;
        }
        final int[] corners = new int[made[1].length];
        for (int k = 0; k < corners.length; k++) {
            corners[k] = (int) made[1][k];
        }
        final int[] faces = new int[3 * corners.length];
        for (int k = 0; k < corners.length; k++) {
            faces[3 * k] = corners[k];
            faces[3 * k + 1] = corners[k];
            faces[3 * k + 2] = corners[k];
        }
        final TriangleMesh mesh = new TriangleMesh(VertexFormat.POINT_NORMAL_TEXCOORD);
        mesh.getPoints().setAll(points);
        mesh.getNormals().setAll(normals);
        mesh.getTexCoords().setAll(c);
        mesh.getFaces().setAll(faces);
        final MeshView view = new MeshView(mesh);
        view.setMaterial(material);
        view.setCullFace(CullFace.NONE);
        root.getChildren().add(view);
        meshes.add(mesh);
        places.add(points);
        coords.add(c);
    }

    /**
     * A river's water: along its line, two points at each, either side of it, as far out as it is wide and
     * tucked under its banks; its top as high over the ground under its line as it is deep there.
     */
    static float[][] river(final Land land, final River river) {
        final List<Vec2> line = river.line();
        final int n = line.size();
        final double half = river.wide() / 2 + TUCKED;
        final float[] points = new float[6 * n];
        for (int k = 0; k < n; k++) {
            final Vec2 before = line.get(Math.max(0, k - 1));
            final Vec2 after = line.get(Math.min(n - 1, k + 1));
            final double dx = after.x() - before.x();
            final double dz = after.z() - before.z();
            final double length = Math.hypot(dx, dz);
            // to its right, going along it
            final double rx = dz / length;
            final double rz = -dx / length;
            final Vec2 p = line.get(k);
            final double top = land.height(p.x(), p.z()) + river.top();
            for (int side = 0; side < 2; side++) {
                final double s = side == 0 ? -half : half;
                points[6 * k + 3 * side] = (float) (p.x() + rx * s);
                points[6 * k + 3 * side + 1] = (float) -top;
                points[6 * k + 3 * side + 2] = (float) (p.z() + rz * s);
            }
        }
        final float[] corners = new float[6 * (n - 1)];
        for (int k = 0; k + 1 < n; k++) {
            final int a = 2 * k;
            corners[6 * k] = a;
            corners[6 * k + 1] = a + 1;
            corners[6 * k + 2] = a + 2;
            corners[6 * k + 3] = a + 1;
            corners[6 * k + 4] = a + 3;
            corners[6 * k + 5] = a + 2;
        }
        return new float[][] {points, corners};
    }

    /** A pond's water: its loop at its level, a fan of triangles round its middle. */
    static float[][] pond(final Pond pond) {
        final List<Vec2> loop = pond.outline();
        final int n = loop.size();
        double mx = 0;
        double mz = 0;
        for (final Vec2 p : loop) {
            mx += p.x() / n;
            mz += p.z() / n;
        }
        final float[] points = new float[3 * (n + 1)];
        points[0] = (float) mx;
        points[1] = (float) -pond.level();
        points[2] = (float) mz;
        for (int k = 0; k < n; k++) {
            points[3 * (k + 1)] = (float) loop.get(k).x();
            points[3 * (k + 1) + 1] = (float) -pond.level();
            points[3 * (k + 1) + 2] = (float) loop.get(k).z();
        }
        final float[] corners = new float[3 * n];
        for (int k = 0; k < n; k++) {
            corners[3 * k] = 0;
            corners[3 * k + 1] = k + 1;
            corners[3 * k + 2] = (k + 1) % n + 1;
        }
        return new float[][] {points, corners};
    }

    /** Its picture, lit by itself: one colour, the haze down it ({@link TerrainModel#glow}). */
    private static WritableImage picture(final double[] lit) {
        final WritableImage image = new WritableImage(1, HAZES);
        final PixelWriter writer = image.getPixelWriter();
        for (int y = 0; y < HAZES; y++) {
            final double haze = y / (HAZES - 1.0);
            writer.setColor(0, y, Color.color(
                    TerrainModel.glow(WATER.getRed(), BASE.getRed(), Horizon.HAZY.getRed(), lit[0], haze),
                    TerrainModel.glow(WATER.getGreen(), BASE.getGreen(), Horizon.HAZY.getGreen(), lit[1], haze),
                    TerrainModel.glow(WATER.getBlue(), BASE.getBlue(), Horizon.HAZY.getBlue(), lit[2], haze)));
        }
        return image;
    }
}
