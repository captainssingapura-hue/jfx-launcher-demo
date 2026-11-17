package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.sim.Land;
import com.example.fxsuite.avatar.sim.Road;
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
 * The roads' surfaces, each a strip of its own from the road ({@link Road}), not the land's: at each of its
 * line's points, three across it, its edges and its crown, as high as its surface is there, its camber and all,
 * and {@link #LIFT} more; so its edges are where they are and its surface as it is, however finely the land
 * under it is drawn, and it never changes as that does. The land under it is drawn sunk out of sight
 * ({@link TerrainModel}). None on a bridge, whose deck has its own. Coloured as what it is made of is, as the
 * land's covers are, and lit and hazed as the land is.
 *
 * <p>In stretches of {@link #STRETCH} of a road's points, each a mesh, drawn when in view ({@link View}); the
 * haze of those in view worked out again as the land's is, a few a frame.
 */
public final class RoadSurface {

    /** How far over the road's surface its strip lies. */
    static final double LIFT = 2;
    /** How many of a road's points a stretch is: a kilometre, at 2 m apart. */
    static final int STRETCH = 500;
    private static final int HAZES = 64;
    /** What it may be made of, each a column of its picture, in the order of {@link Land.Cover}. */
    private static final Color[] COLOURS = {TerrainModel.EARTH, TerrainModel.GRAVEL, TerrainModel.ASPHALT};

    /** A stretch, as made: its points (the world's y down), the way up at each, its colour, its triangles; the box round it. */
    record Strip(float[] points, float[] normals, float[] coords, int[] faces, double[] box) {

        int size() {
            return points.length / 3;
        }
    }

    /** A stretch as drawn: its mesh, and where its haze was worked out from. */
    private static final class Drawn {
        final Strip strip;
        final TriangleMesh mesh;
        final MeshView view;
        boolean hazed;
        Vec3 hazedFrom;

        Drawn(final Strip strip, final TriangleMesh mesh, final MeshView view) {
            this.strip = strip;
            this.mesh = mesh;
            this.view = view;
        }
    }

    private final Group root = new Group();
    private final List<Drawn> stretches = new ArrayList<>();
    private int next;

    /** The surfaces of {@code roads}, lit as {@code lit} lights level ground ({@link Horizon#lighting}). */
    public RoadSurface(final List<Road> roads, final double[] lit) {
        final PhongMaterial material = new PhongMaterial(TerrainModel.BASE);
        material.setSpecularColor(Color.BLACK);
        material.setSelfIlluminationMap(picture(lit));
        for (final Road road : roads) {
            final int n = road.line().size();
            for (int from = 0; from + 1 < n; from += STRETCH) {
                final Strip strip = strip(road, from, Math.min(n - 1, from + STRETCH));
                if (strip.faces().length == 0) {
                    continue;
                }
                final TriangleMesh mesh = new TriangleMesh(VertexFormat.POINT_NORMAL_TEXCOORD);
                mesh.getPoints().setAll(strip.points());
                mesh.getNormals().setAll(strip.normals());
                mesh.getTexCoords().setAll(strip.coords());
                mesh.getFaces().setAll(strip.faces());
                final MeshView view = new MeshView(mesh);
                view.setMaterial(material);
                view.setCullFace(CullFace.NONE);
                root.getChildren().add(view);
                stretches.add(new Drawn(strip, mesh, view));
            }
        }
        seenFrom(View.all(Vec3.ZERO));
    }

    public Node node() {
        return root;
    }

    /**
     * Each stretch drawn if {@code view} sees it; and the haze of those in view worked out again where the
     * camera has moved far enough for it to show, round them in turn, no more than {@link TerrainModel#BUDGET}
     * points' worth a frame.
     */
    public void seenFrom(final View view) {
        final Vec3 eye = view.eye();
        for (final Drawn d : stretches) {
            final double[] box = d.strip.box();
            d.view.setVisible(view.sees(box[0], box[1], box[2], box[3]));
        }
        int left = TerrainModel.BUDGET;
        int done = 0;
        for (int k = 0; k < stretches.size() && (left > 0 || done == 0); k++) {
            next = (next + 1) % stretches.size();
            final Drawn d = stretches.get(next);
            if (d.view.isVisible() && stale(d, eye)) {
                if (d.strip.size() > left && done > 0) {
                    break;
                }
                haze(d, eye);
                left -= d.strip.size();
                done++;
            }
        }
    }

    /** Whether stretch {@code d}'s haze is to be worked out again from {@code eye}, as the land's would be. */
    private static boolean stale(final Drawn d, final Vec3 eye) {
        final double[] box = d.strip.box();
        final double far = Math.hypot(Math.max(Math.abs(eye.x() - box[0]), Math.abs(eye.x() - box[2])),
                Math.max(Math.abs(eye.z() - box[1]), Math.abs(eye.z() - box[3])));
        if (far <= Horizon.HAZE && !d.hazed) {
            return false;
        }
        if (far <= Horizon.HAZE || d.hazedFrom == null) {
            return true;
        }
        final double dx = Math.max(0, Math.max(box[0] - eye.x(), eye.x() - box[2]));
        final double dz = Math.max(0, Math.max(box[1] - eye.z(), eye.z() - box[3]));
        final double moved = Math.hypot(eye.x() - d.hazedFrom.x(), eye.z() - d.hazedFrom.z());
        return moved > Math.max(TerrainModel.MOVED, TerrainModel.SHARE * Math.hypot(dx, dz));
    }

    private static void haze(final Drawn d, final Vec3 eye) {
        final float[] points = d.strip.points();
        final float[] coords = d.strip.coords();
        boolean any = false;
        for (int v = 0; v < coords.length / 2; v++) {
            final double haze = Horizon.haze(Math.hypot(points[3 * v] - eye.x(), points[3 * v + 2] - eye.z()));
            coords[2 * v + 1] = (float) ((haze * (HAZES - 1) + 0.5) / HAZES);
            any |= haze > 0;
        }
        d.mesh.getTexCoords().setAll(coords);
        d.hazed = any;
        d.hazedFrom = eye;
    }

    /**
     * Road {@code road}'s surface from its point {@code from} to its point {@code to}: at each point off a bridge,
     * its left edge, its crown and its right edge, {@link #LIFT} over its surface; between each two points both off a
     * bridge, four triangles, wound so their faces are up.
     */
    static Strip strip(final Road road, final int from, final int to) {
        final List<Vec2> line = road.line();
        final int n = to - from + 1;
        final float[] points = new float[9 * n];
        final float[] coords = new float[6 * n];
        final double[] box = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
                Double.NEGATIVE_INFINITY};
        for (int k = 0; k < n; k++) {
            final int i = from + k;
            final Vec2 before = line.get(Math.max(0, i - 1));
            final Vec2 after = line.get(Math.min(line.size() - 1, i + 1));
            final double dx = after.x() - before.x();
            final double dz = after.z() - before.z();
            final double length = Math.max(1e-9, Math.hypot(dx, dz));
            // to the left, going along it
            final double lx = -dz / length;
            final double lz = dx / length;
            final double half = road.wide(i) / 2;
            final Vec2 p = line.get(i);
            final float colour = (float) ((road.kind(i).surface().ordinal() + 0.5) / COLOURS.length);
            final double[] across = {half, 0, -half};
            for (int c = 0; c < 3; c++) {
                final double x = p.x() + lx * across[c];
                final double z = p.z() + lz * across[c];
                points[9 * k + 3 * c] = (float) x;
                points[9 * k + 3 * c + 1] = (float) -(road.surface(i, across[c]) + LIFT);
                points[9 * k + 3 * c + 2] = (float) z;
                coords[6 * k + 2 * c] = colour;
                coords[6 * k + 2 * c + 1] = (float) (0.5 / HAZES);
                box[0] = Math.min(box[0], x);
                box[1] = Math.min(box[1], z);
                box[2] = Math.max(box[2], x);
                box[3] = Math.max(box[3], z);
            }
        }
        final List<Integer> corners = new ArrayList<>();
        for (int k = 0; k + 1 < n; k++) {
            if (road.bridged(from + k) || road.bridged(from + k + 1)) {
                continue;
            }
            final int a = 3 * k;
            final int b = 3 * (k + 1);
            for (int c = 0; c < 2; c++) {
                // left to right across, going along: up, the world's y down
                corners.add(a + c);
                corners.add(b + c);
                corners.add(a + c + 1);
                corners.add(a + c + 1);
                corners.add(b + c);
                corners.add(b + c + 1);
            }
        }
        final int[] faces = new int[3 * corners.size()];
        for (int k = 0; k < corners.size(); k++) {
            faces[3 * k] = corners.get(k);
            faces[3 * k + 1] = corners.get(k);
            faces[3 * k + 2] = corners.get(k);
        }
        return new Strip(points, normals(points, n), coords, faces, box);
    }

    /** The way up at each of a strip's points, {@code n} of them along it, three across: square to it along and across. */
    private static float[] normals(final float[] points, final int n) {
        final float[] normals = new float[points.length];
        for (int k = 0; k < n; k++) {
            final int a = Math.max(0, k - 1);
            final int b = Math.min(n - 1, k + 1);
            for (int c = 0; c < 3; c++) {
                final int l = Math.max(0, c - 1);
                final int r = Math.min(2, c + 1);
                // along, and across from left to right
                final double ax = points[9 * b + 3 * c] - points[9 * a + 3 * c];
                final double ay = points[9 * b + 3 * c + 1] - points[9 * a + 3 * c + 1];
                final double az = points[9 * b + 3 * c + 2] - points[9 * a + 3 * c + 2];
                final double cx = points[9 * k + 3 * r] - points[9 * k + 3 * l];
                final double cy = points[9 * k + 3 * r + 1] - points[9 * k + 3 * l + 1];
                final double cz = points[9 * k + 3 * r + 2] - points[9 * k + 3 * l + 2];
                double nx = ay * cz - az * cy;
                double ny = az * cx - ax * cz;
                double nz = ax * cy - ay * cx;
                // up is -y
                if (ny > 0) {
                    nx = -nx;
                    ny = -ny;
                    nz = -nz;
                }
                final double length = Math.sqrt(nx * nx + ny * ny + nz * nz);
                normals[9 * k + 3 * c] = (float) (length > 0 ? nx / length : 0);
                normals[9 * k + 3 * c + 1] = (float) (length > 0 ? ny / length : -1);
                normals[9 * k + 3 * c + 2] = (float) (length > 0 ? nz / length : 0);
            }
        }
        return normals;
    }

    /** Its picture, lit by itself: what a road may be made of across, the haze down, as the land's covers are. */
    private static WritableImage picture(final double[] lit) {
        final WritableImage image = new WritableImage(COLOURS.length, HAZES);
        final PixelWriter writer = image.getPixelWriter();
        final Color base = TerrainModel.BASE;
        for (int y = 0; y < HAZES; y++) {
            final double haze = y / (HAZES - 1.0);
            for (int x = 0; x < COLOURS.length; x++) {
                final Color c = COLOURS[x];
                writer.setColor(x, y, Color.color(
                        TerrainModel.glow(c.getRed(), base.getRed(), Horizon.HAZY.getRed(), lit[0], haze),
                        TerrainModel.glow(c.getGreen(), base.getGreen(), Horizon.HAZY.getGreen(), lit[1], haze),
                        TerrainModel.glow(c.getBlue(), base.getBlue(), Horizon.HAZY.getBlue(), lit[2], haze)));
            }
        }
        return image;
    }
}
