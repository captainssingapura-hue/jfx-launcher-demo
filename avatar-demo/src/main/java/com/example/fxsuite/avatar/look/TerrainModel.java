package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.sim.Land;
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
 * <p>Drawn in tiles, {@link #TILE} of the survey's heights a side, each as fine as it is near
 * ({@link #OUT}), a tile changing only once it is well past where it should ({@link #BACK}), so it does
 * not flick back and forth. As fine as the land needs and no finer ({@link LandSheet}): near, within
 * {@link #TOLERANCE} 3 cm of it, an open meadow in squares tens of metres across and a river's bank in
 * squares of half a metre; further off, less closely. A skirt hangs from each tile's edges, so where a
 * fine tile meets a coarse one no gap shows between them. Each tile's haze is worked out again, height by height, as the camera moves; those near
 * enough to have none are left alone.
 */
public final class TerrainModel {

    /** Heights along a tile's side, less one: 500 m, at 10 m apart. */
    static final int TILE = 50;
    /**
     * How close to the land each level of a tile keeps, nearest first; its smallest squares, its biggest, and
     * its biggest where a feature of the land lies.
     */
    static final double[] TOLERANCE = {3, 10, 30, 100, 300};
    static final double[] FINEST = {50, 100, 400, 1_000, 2_500};
    static final double[] LARGEST = {4_000, 8_000, 16_000, 31_250, 50_000};
    static final double[] FEATURED = {400, 800, 1_600, 3_200, 6_400};
    /** How many levels a tile has. */
    static final int LEVELS = TOLERANCE.length;
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
    /** How far apart the mottle's spots are. */
    private static final double MOTTLED = 100;

    /**
     * The land's base colour, shaded by the sun as its slopes face it: no brighter than any of its tints,
     * so its picture is what the tints have more.
     */
    static final Color BASE = Color.web("#55644d");

    /**
     * What covers the ground where a feature lays something on it ({@link Land#cover}), each its colour: bare
     * earth (a channel's bed and banks), gravel (a country road's top), asphalt (a road's); in the order of
     * {@link Land.Cover}. Each no darker than the base colour in any channel, so its picture is what it has more.
     */
    static final Color EARTH = Color.web("#7d6a4e");
    static final Color GRAVEL = Color.web("#8a8578");
    static final Color ASPHALT = Color.web("#5f6566");
    private static final Color[] COVERS = {EARTH, GRAVEL, ASPHALT};
    /** How many of the picture's columns each cover is: the last cover first, then the one before, the grass after them all. */
    static final int COVERED = 16;

    /** The land's tints, low to high. */
    private static final Color[] RAMP = {Color.web("#66775c"), Color.web("#6c7e55"), Color.web("#7b8857"),
            Color.web("#857f58"), Color.web("#8a7d66")};

    private final Land land;
    /** The land's lie, as surveyed: its tiles are so many of its heights a side. */
    private final Survey lie;
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
        final TriangleMesh[] meshes = new TriangleMesh[LEVELS];
        final float[][] coords = new float[LEVELS][];
        final float[][] places = new float[LEVELS][];
        /** Which way each height's ground faces, as the land has it; and as shown, turned up by the haze laid over it. */
        final float[][] facing = new float[LEVELS][];
        final float[][] shown = new float[LEVELS][];
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
            final double minX = lie.x(i0);
            final double maxX = lie.x(i1);
            final double minZ = lie.z(j0);
            final double maxZ = lie.z(j1);
            final double dx = Math.max(0, Math.max(minX - eye.x(), eye.x() - maxX));
            final double dz = Math.max(0, Math.max(minZ - eye.z(), eye.z() - maxZ));
            final double fx = Math.max(Math.abs(eye.x() - minX), Math.abs(eye.x() - maxX));
            final double fz = Math.max(Math.abs(eye.z() - minZ), Math.abs(eye.z() - maxZ));
            return new double[] {Math.hypot(dx, dz), Math.hypot(fx, fz)};
        }
    }

    /**
     * The land, its lie surveyed, lit as {@code lit} lights level ground, each channel ({@link Horizon#lighting}):
     * its base colour shaded by the sun as its slopes face it, all else in one picture lit by itself (see
     * {@link #picture}).
     */
    public TerrainModel(final Land land, final double[] lit) {
        if (land.base() == null) {
            throw new IllegalArgumentException("the land drawn is surveyed");
        }
        this.land = land;
        this.lie = land.base();
        this.low = land.lowest();
        this.span = Math.max(1, land.highest() - low);
        material = new PhongMaterial(BASE);
        material.setSpecularColor(Color.BLACK);
        material.setSelfIlluminationMap(picture(lit));
        final int last = lie.size() - 1;
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

    /**
     * Tile {@code tile} at level {@code level}: the land over it as fine as that level needs ({@link LandSheet}),
     * and its skirt hanging from its edges; each point tinted by how high it is, a little mottled, no haze yet.
     */
    private void build(final Tile tile, final int level) {
        final LandSheet.Made made = LandSheet.of(land, lie.x(tile.i0), lie.z(tile.j0), lie.x(tile.i1), lie.z(tile.j1),
                TOLERANCE[level], FINEST[level], LARGEST[level], FEATURED[level], lie.every(), SKIRT);
        final float[] coords = new float[2 * made.size()];
        final float[] points = made.points();
        for (int v = 0; v < made.size(); v++) {
            final long x = Math.round(points[3 * v] / MOTTLED);
            final long z = Math.round(points[3 * v + 2] / MOTTLED);
            final double tint = (made.heights()[v] - low) / span + MOTTLE * (2 * Num.hash(x, z) - 1);
            final double px = points[3 * v];
            final double pz = points[3 * v + 2];
            final double[] covered = new double[Land.Cover.values().length];
            for (final Land.Cover c : Land.Cover.values()) {
                covered[c.ordinal()] = land.cover(c, px, pz);
            }
            coords[2 * v] = (float) ((column(covered, tint) + 0.5) / TINTS);
            coords[2 * v + 1] = (float) (0.5 / HAZES);
        }
        final TriangleMesh mesh = new TriangleMesh(VertexFormat.POINT_NORMAL_TEXCOORD);
        mesh.getPoints().setAll(points);
        mesh.getNormals().setAll(made.normals());
        mesh.getTexCoords().setAll(coords);
        // each corner's point, the way it faces and its tint, all by the same number
        final int[] corners = made.triangles();
        final int[] faces = new int[3 * corners.length];
        for (int k = 0; k < corners.length; k++) {
            faces[3 * k] = corners[k];
            faces[3 * k + 1] = corners[k];
            faces[3 * k + 2] = corners[k];
        }
        mesh.getFaces().setAll(faces);
        tile.meshes[level] = mesh;
        tile.coords[level] = coords;
        tile.places[level] = points;
        tile.facing[level] = made.normals();
        tile.shown[level] = made.normals().clone();
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
                final Color tint = colour(x);
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

    /**
     * The picture's column for ground covered so much by each cover ({@code covers}, in the order of
     * {@link Land.Cover}), {@code t} of the way from the land's lowest to its highest: asphalt in the first few,
     * then gravel, then bare earth, then the grass's tints, low to high. Each cover laid over the grass in
     * turn, so between bare ground and the grass beside it the one goes into the other; and from a road into
     * the grass beside it, through a thin edge of what lies between them, as a road's edges are worn.
     */
    static double column(final double[] covers, final double t) {
        final int laid = COVERS.length * COVERED;
        double column = laid + Math.clamp(t, 0, 1) * (TINTS - 1 - laid);
        for (int c = 0; c < COVERS.length; c++) {
            final double middle = (COVERS.length - 1 - c) * COVERED + COVERED / 2.0;
            column += (middle - column) * Math.clamp(covers[c], 0, 1);
        }
        return column;
    }

    /** The colour of the picture's column {@code x}: a cover, or a tint of the grass. */
    static Color colour(final int x) {
        final int laid = COVERS.length * COVERED;
        return x < laid ? COVERS[COVERS.length - 1 - x / COVERED] : tint((x - laid) / (double) (TINTS - 1 - laid));
    }

    /** The land's tint {@code t} of the way from its lowest to its highest. */
    static Color tint(final double t) {
        final double at = Math.clamp(t, 0, 1) * (RAMP.length - 1);
        final int k = Math.min(RAMP.length - 2, (int) at);
        return RAMP[k].interpolate(RAMP[k + 1], at - k);
    }
}
