package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.sim.Num;
import com.example.fxsuite.avatar.sim.Survey;
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
 * The land drawn, as surveyed ({@link Survey}): its heights as a sheet of triangles, lit as the sun falls
 * on its slopes, tinted by how high it is (the meadow's grass low down, drier grass up the slopes,
 * moor on the heights), a little mottled; and, from {@link Horizon#HAZE} out, fading into the haze as the
 * far ground round the camera does ({@link Horizon#haze}), so the hills at the world's edge go into the sky.
 *
 * <p>Its base colour is lit by the sun and the sky; its tint, a little mottled, and the haze, are in one
 * picture lit by itself ({@link #picture}).
 *
 * <p>Drawn in tiles, {@link #TILE} heights a side, each as fine as it is near: every height near the
 * camera, every second, fifth, tenth, then twenty-fifth further off ({@link #STEPS}, {@link #OUT}), a
 * tile changing only once it is well past where it should ({@link #BACK}), so it does not flick back and
 * forth. A skirt hangs from each tile's edges, so where a fine tile meets a coarse one no gap shows
 * between them. Each tile's haze is worked out again, height by height, as the camera moves; those near
 * enough to have none are left alone.
 */
public final class TerrainModel {

    /** Heights along a tile's side, less one: 500 m, at 10 m apart. */
    static final int TILE = 50;
    /** Each height, or every so many, as a tile is further off. */
    static final int[] STEPS = {1, 2, 5, 10, 25};
    /** Beyond each of these, across the ground from the camera to a tile's nearest point, the next coarser step. */
    static final double[] OUT = {100_000, 200_000, 350_000, 600_000};
    /** How much nearer than where it went coarser a tile must come to go finer again. */
    static final double BACK = 0.9;
    /** How far a tile's skirt hangs. */
    static final double SKIRT = 500;
    /** How far the camera moves before the haze is worked out again. */
    static final double MOVED = 2_500;
    /** How many heights' worth of tiles a frame sends again to be drawn, at most (or one tile, if more). */
    static final int BUDGET = 3_000;
    /** How many tints, low to high, and how many shades of the haze, none to all, the land's picture has. */
    private static final int TINTS = 256;
    private static final int HAZES = 64;
    /** How much each height's tint is moved up or down, at random: the grass mottled. */
    private static final double MOTTLE = 0.025;

    /**
     * The land's base colour, shaded by the sun as its slopes face it: no brighter than any of its tints,
     * so its picture is what the tints have more.
     */
    static final Color BASE = Color.web("#55644d");

    /** The land's tints, low to high. */
    private static final Color[] RAMP = {Color.web("#66775c"), Color.web("#6c7e55"), Color.web("#7b8857"),
            Color.web("#857f58"), Color.web("#8a7d66")};

    private final Survey land;
    private final Group root = new Group();
    private final List<Tile> tiles = new ArrayList<>();
    private final double low;
    private final double span;
    private final PhongMaterial material;
    private Vec3 hazedFrom;
    /** The tile whose haze is looked at next. */
    private int next;

    /** A tile: from height {@code i0} to {@code i1} across, {@code j0} to {@code j1} along, each way inclusive. */
    private final class Tile {
        final int i0;
        final int i1;
        final int j0;
        final int j1;
        final MeshView view = new MeshView();
        final TriangleMesh[] meshes = new TriangleMesh[STEPS.length];
        final float[][] coords = new float[STEPS.length][];
        final float[][] places = new float[STEPS.length][];
        /** Which way each height's ground faces, as the land has it; and as shown, turned up by the haze laid over it. */
        final float[][] facing = new float[STEPS.length][];
        final float[][] shown = new float[STEPS.length][];
        int level = -1;
        /** Whether it has any haze on it; whether that is to be worked out again, the camera having moved. */
        boolean hazed;
        boolean stale;

        Tile(final int i0, final int i1, final int j0, final int j1) {
            this.i0 = i0;
            this.i1 = i1;
            this.j0 = j0;
            this.j1 = j1;
            view.setMaterial(material);
            view.setCullFace(CullFace.NONE);
        }

        /** How far {@code eye} is across the ground from its nearest point, and from its furthest. */
        double[] away(final Vec3 eye) {
            final double minX = land.x(i0);
            final double maxX = land.x(i1);
            final double minZ = land.z(j0);
            final double maxZ = land.z(j1);
            final double dx = Math.max(0, Math.max(minX - eye.x(), eye.x() - maxX));
            final double dz = Math.max(0, Math.max(minZ - eye.z(), eye.z() - maxZ));
            final double fx = Math.max(Math.abs(eye.x() - minX), Math.abs(eye.x() - maxX));
            final double fz = Math.max(Math.abs(eye.z() - minZ), Math.abs(eye.z() - maxZ));
            return new double[] {Math.hypot(dx, dz), Math.hypot(fx, fz)};
        }
    }

    /**
     * The land as surveyed, lit as {@code lit} lights level ground, each channel ({@link Horizon#lighting}): its
     * base colour shaded by the sun as its slopes face it, all else in one picture lit by itself (see {@link #picture}).
     */
    public TerrainModel(final Survey land, final double[] lit) {
        this.land = land;
        this.low = land.lowest();
        this.span = Math.max(1, land.highest() - low);
        material = new PhongMaterial(BASE);
        material.setSpecularColor(Color.BLACK);
        material.setSelfIlluminationMap(picture(lit));
        final int last = land.size() - 1;
        for (int j = 0; j < last; j += TILE) {
            for (int i = 0; i < last; i += TILE) {
                final Tile tile = new Tile(i, Math.min(last, i + TILE), j, Math.min(last, j + TILE));
                tiles.add(tile);
                root.getChildren().add(tile.view);
            }
        }
        // all of it at once, to start with
        seenFrom(Vec3.ZERO, Integer.MAX_VALUE);
    }

    public Node node() {
        return root;
    }

    /**
     * Each tile as fine as it should be, seen from {@code eye}, where the camera now is; its haze, if the
     * camera has moved. A frame does no more than {@link #BUDGET} heights' worth: what a tile changes, the
     * whole of it is sent again to be drawn, and all of them at once would hold the frame up. So the tiles
     * that should be finer or coarser go first, then the haze, round the tiles in turn, a few each frame:
     * so far off, it hardly moves while it waits.
     */
    public void seenFrom(final Vec3 eye) {
        seenFrom(eye, BUDGET);
    }

    private void seenFrom(final Vec3 eye, final int budget) {
        if (hazedFrom == null || Math.hypot(eye.x() - hazedFrom.x(), eye.z() - hazedFrom.z()) > MOVED) {
            hazedFrom = eye;
            for (final Tile tile : tiles) {
                tile.stale = true;
            }
        }
        int left = budget;
        for (final Tile tile : tiles) {
            if (left <= 0) {
                break;
            }
            final int level = level(tile.away(eye)[0], tile.level);
            if (level != tile.level) {
                if (tile.meshes[level] == null) {
                    build(tile, level);
                }
                tile.level = level;
                tile.view.setMesh(tile.meshes[level]);
                left -= hazed(tile, eye, true);
            }
        }
        for (int k = 0; k < tiles.size() && left > 0; k++) {
            final Tile tile = tiles.get(next);
            next = (next + 1) % tiles.size();
            left -= hazed(tile, eye, false);
        }
    }

    /**
     * Tile {@code tile}'s haze worked out again from {@code eye}, if it is out of date ({@code anyway}, if it has
     * just changed how fine it is), and it has any or had: how many heights that took.
     */
    private int hazed(final Tile tile, final Vec3 eye, final boolean anyway) {
        if (!tile.stale && !anyway) {
            return 0;
        }
        tile.stale = false;
        final boolean far = tile.away(eye)[1] > Horizon.HAZE;
        if (!far && !tile.hazed && !anyway) {
            return 0;
        }
        haze(tile, eye);
        tile.hazed = far;
        return tile.coords[tile.level].length / 2;
    }

    /**
     * Which step a tile {@code away} from the camera is drawn at, having been at {@code was} (-1 if not yet):
     * past each of {@link #OUT}, coarser; back again only once {@link #BACK} as far.
     */
    static int level(final double away, final int was) {
        int level = 0;
        while (level < OUT.length && away > OUT[level] * (level < was ? BACK : 1)) {
            level++;
        }
        return level;
    }

    /** From {@code from} to {@code to}, every {@code step}: and {@code to}, whether it falls on a step or not. */
    static int[] along(final int from, final int to, final int step) {
        final List<Integer> at = new ArrayList<>();
        for (int k = from; k < to; k += step) {
            at.add(k);
        }
        at.add(to);
        return at.stream().mapToInt(Integer::intValue).toArray();
    }

    /** Tile {@code tile} at step {@code level}: its sheet, and its skirt hanging from its four edges. */
    private void build(final Tile tile, final int level) {
        final int[] is = along(tile.i0, tile.i1, STEPS[level]);
        final int[] js = along(tile.j0, tile.j1, STEPS[level]);
        final List<int[]> top = new ArrayList<>();
        for (final int j : js) {
            for (final int i : is) {
                top.add(new int[] {i, j});
            }
        }
        // the skirt: each edge's heights again, hanging below them
        final List<int[]> edges = new ArrayList<>();
        edges.add(edge(is, js, 0, true));
        edges.add(edge(is, js, js.length - 1, true));
        edges.add(edge(is, js, 0, false));
        edges.add(edge(is, js, is.length - 1, false));
        int count = top.size();
        for (final int[] e : edges) {
            count += e.length;
        }
        final float[] points = new float[3 * count];
        final float[] normals = new float[3 * count];
        final float[] coords = new float[2 * count];
        int v = 0;
        for (final int[] at : top) {
            put(points, normals, coords, v++, at[0], at[1], 0);
        }
        final int[] hung = new int[edges.size()];
        for (int e = 0; e < edges.size(); e++) {
            hung[e] = v;
            for (final int index : edges.get(e)) {
                put(points, normals, coords, v++, top.get(index)[0], top.get(index)[1], SKIRT);
            }
        }
        final TriangleMesh mesh = new TriangleMesh(VertexFormat.POINT_NORMAL_TEXCOORD);
        mesh.getPoints().setAll(points);
        mesh.getNormals().setAll(normals);
        mesh.getTexCoords().setAll(coords);
        final int nx = is.length;
        final List<Integer> faces = new ArrayList<>();
        for (int b = 0; b + 1 < js.length; b++) {
            for (int a = 0; a + 1 < nx; a++) {
                final int p = b * nx + a;
                // wound so its face is up, the world's y down
                triangle(faces, p, p + 1, p + nx);
                triangle(faces, p + 1, p + nx + 1, p + nx);
            }
        }
        for (int e = 0; e < edges.size(); e++) {
            final int[] edge = edges.get(e);
            for (int k = 0; k + 1 < edge.length; k++) {
                triangle(faces, edge[k], edge[k + 1], hung[e] + k);
                triangle(faces, edge[k + 1], hung[e] + k + 1, hung[e] + k);
            }
        }
        mesh.getFaces().setAll(faces.stream().mapToInt(Integer::intValue).toArray());
        tile.meshes[level] = mesh;
        tile.coords[level] = coords;
        tile.places[level] = points;
        tile.facing[level] = normals;
        tile.shown[level] = normals.clone();
    }

    /** The numbers, among a tile's heights, of those along one of its edges: a row ({@code across}) or a column. */
    private static int[] edge(final int[] is, final int[] js, final int at, final boolean across) {
        final int[] edge = new int[across ? is.length : js.length];
        for (int k = 0; k < edge.length; k++) {
            edge[k] = across ? at * is.length + k : k * is.length + at;
        }
        return edge;
    }

    private static void triangle(final List<Integer> faces, final int a, final int b, final int c) {
        for (final int p : new int[] {a, b, c}) {
            faces.add(p);
            faces.add(p);
            faces.add(p);
        }
    }

    /** Height ({@code i}, {@code j}) as point {@code v}, {@code down} below it: where, which way its ground faces, its tint. */
    private void put(final float[] points, final float[] normals, final float[] coords, final int v, final int i, final int j,
                     final double down) {
        final double h = land.at(i, j);
        points[3 * v] = (float) land.x(i);
        points[3 * v + 1] = (float) -(h - down);
        points[3 * v + 2] = (float) land.z(j);
        final int n = land.size();
        final double dx = (land.at(Math.min(n - 1, i + 1), j) - land.at(Math.max(0, i - 1), j))
                / (land.x(Math.min(n - 1, i + 1)) - land.x(Math.max(0, i - 1)));
        final double dz = (land.at(i, Math.min(n - 1, j + 1)) - land.at(i, Math.max(0, j - 1)))
                / (land.z(Math.min(n - 1, j + 1)) - land.z(Math.max(0, j - 1)));
        final double length = Math.sqrt(dx * dx + 1 + dz * dz);
        // up the world's y is down
        normals[3 * v] = (float) (-dx / length);
        normals[3 * v + 1] = (float) (-1 / length);
        normals[3 * v + 2] = (float) (-dz / length);
        final double tint = (h - low) / span + MOTTLE * (2 * Num.hash(i, j) - 1);
        coords[2 * v] = (float) ((Math.clamp(tint, 0, 1) * (TINTS - 1) + 0.5) / TINTS);
        coords[2 * v + 1] = (float) (0.5 / HAZES);
    }

    /**
     * Tile {@code tile}'s haze, height by height, seen from {@code eye}: and its ground turned up as the haze
     * thickens, so a far slope is lit as the far ground is, level, and fades into the haze as that does. Lit
     * full on by the sun far off, it would show brighter than the haze it is going into.
     */
    private void haze(final Tile tile, final Vec3 eye) {
        final int level = tile.level;
        final float[] coords = tile.coords[level];
        final float[] points = tile.places[level];
        final float[] facing = tile.facing[level];
        final float[] shown = tile.shown[level];
        for (int v = 0; v < coords.length / 2; v++) {
            final double haze = Horizon.haze(Math.hypot(points[3 * v] - eye.x(), points[3 * v + 2] - eye.z()));
            coords[2 * v + 1] = (float) ((haze * (HAZES - 1) + 0.5) / HAZES);
            // up, the world's y down
            final double nx = facing[3 * v] * (1 - haze);
            final double ny = facing[3 * v + 1] * (1 - haze) - haze;
            final double nz = facing[3 * v + 2] * (1 - haze);
            final double length = Math.sqrt(nx * nx + ny * ny + nz * nz);
            shown[3 * v] = (float) (nx / length);
            shown[3 * v + 1] = (float) (ny / length);
            shown[3 * v + 2] = (float) (nz / length);
        }
        tile.meshes[level].getTexCoords().setAll(coords);
        tile.meshes[level].getNormals().setAll(shown);
    }

    /**
     * The land's picture, lit by itself: its tints across, the haze down. The land is to show its tint as
     * level ground is lit ({@code lit}), less as there is more haze, and the haze as much as there is of it;
     * the sun and the sky light its base colour ({@link #BASE}) as its slopes face them, so the picture holds
     * the rest. One picture, not a picture lit and one lighting itself: far off, where each is drawn from
     * coarser copies of itself, the two would not go together, and the land would show dark streaks across
     * it and a white edge against the sky.
     */
    static WritableImage picture(final double[] lit) {
        final WritableImage image = new WritableImage(TINTS, HAZES);
        final PixelWriter writer = image.getPixelWriter();
        for (int y = 0; y < HAZES; y++) {
            final double haze = y / (HAZES - 1.0);
            for (int x = 0; x < TINTS; x++) {
                final Color tint = tint(x / (TINTS - 1.0));
                writer.setColor(x, y, Color.color(
                        glow(tint.getRed(), BASE.getRed(), Horizon.HAZY.getRed(), lit[0], haze),
                        glow(tint.getGreen(), BASE.getGreen(), Horizon.HAZY.getGreen(), lit[1], haze),
                        glow(tint.getBlue(), BASE.getBlue(), Horizon.HAZY.getBlue(), lit[2], haze)));
            }
        }
        return image;
    }

    /** One channel of the picture: the tint as lit, less as hazed, the haze, less what the base colour lit gives already. */
    static double glow(final double tint, final double base, final double hazy, final double lit, final double haze) {
        return Math.clamp(tint * lit * (1 - haze) + hazy * haze - base * lit, 0, 1);
    }

    /** The land's tint {@code t} of the way from its lowest to its highest. */
    static Color tint(final double t) {
        final double at = Math.clamp(t, 0, 1) * (RAMP.length - 1);
        final int k = Math.min(RAMP.length - 2, (int) at);
        return RAMP[k].interpolate(RAMP[k + 1], at - k);
    }
}
