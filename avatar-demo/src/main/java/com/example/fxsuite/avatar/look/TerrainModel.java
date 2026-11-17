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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * The land drawn, as surveyed ({@link Survey}): its heights as a sheet of triangles, lit as the sun falls
 * on its slopes, tinted by how high it is (the meadow's grass low down, drier grass up the slopes,
 * moor on the heights), a little mottled; and, from {@link Horizon#HAZE} out, fading into the haze as the
 * far ground round the camera does ({@link Horizon#haze}), so the hills at the world's edge go into the sky.
 *
 * <p>Its base colour is lit by the sun and the sky; its tint, a little mottled, and the haze, are in one
 * picture lit by itself ({@link #picture}).
 *
 * <p>Drawn in tiles, {@link #TILE} of the survey's heights a side, each as fine as it is near and the camera
 * slow ({@link Fineness}), by its nearest point and the camera's speed smoothed ({@link Fineness.Pace}): a
 * tile changing only once it is well past where it should, so it does not flick back and forth. As fine as the land needs and no finer ({@link LandSheet}): near, within
 * {@link #TOLERANCE} 3 cm of it, an open meadow in squares tens of metres across and a river's bank in
 * squares of half a metre; further off, less closely, and no finer for what covers the ground. A skirt
 * hangs from each tile's edges, so where a fine tile meets a coarse one no gap shows between them.
 *
 * <p>A tile is built apart from the screen ({@link #BUILDERS}), ahead of being wanted ({@link #AHEAD}); until
 * it is, it is shown as it was. To start with, every tile is built at its coarsest, side by side.
 *
 * <p>Each tile's haze is worked out again, height by height, as the camera moves: once it has moved
 * {@link #MOVED}, or {@link #SHARE} of how far off the tile is, if more, since far off the haze changes
 * little with a few metres more or less; those near enough to have none are left alone.
 */
public final class TerrainModel {

    /** Heights along a tile's side, less one: 500 m, at 10 m apart. */
    static final int TILE = 50;
    /**
     * How close to the land each level of a tile keeps, nearest first; its smallest squares, its biggest, and
     * its biggest where a feature of the land lies; and how far what covers the ground may be from what its
     * squares' corners make of it (only near does that ask for finer squares).
     */
    static final double[] TOLERANCE = {3, 10, 40, 150, 400};
    static final double[] FINEST = {50, 100, 400, 1_000, 2_500};
    static final double[] LARGEST = {4_000, 8_000, 16_000, 31_250, 50_000};
    static final double[] FEATURED = {400, 1_600, 6_400, 25_000, 50_000};
    static final double[] COVERS = {0.08, 0.25, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY};
    /** How many levels a tile has: as many as fineness rounds to. */
    static final int LEVELS = Fineness.LEVELS;
    /** How much nearer than it is a tile is built for, ahead of needing to be: so it is ready when it comes. */
    static final double AHEAD = 0.7;
    /** How far a tile's skirt hangs. */
    static final double SKIRT = 500;
    /**
     * How far the camera moves before a tile's haze is worked out again: at least so far, and so much of how far
     * off the tile is, since far off the haze changes so little with a few metres more or less.
     */
    static final double MOVED = 2_500;
    static final double SHARE = 0.05;
    /** How many heights' worth of tiles a frame sends again to be drawn, at most (or one tile, if more). */
    static final int BUDGET = 3_000;
    /** How many tints, low to high, and how many shades of the haze, none to all, the land's picture has. */
    private static final int TINTS = 256;
    private static final int HAZES = 64;
    /** How much each height's tint is moved up or down, at random: the grass mottled. */
    private static final double MOTTLE = 0.025;
    /** How far apart the mottle's spots are. */
    private static final double MOTTLED = 100;

    /** Where tiles are built, apart from the screen: as many at once as there are processors but one. */
    private static final ExecutorService BUILDERS = Executors.newFixedThreadPool(
            Math.max(1, Runtime.getRuntime().availableProcessors() - 1), r -> {
                final Thread thread = new Thread(r, "land builder");
                thread.setDaemon(true);
                return thread;
            });

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
    private static final Color[] COVERED_BY = {EARTH, GRAVEL, ASPHALT};
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
    /** The tile whose haze is looked at next. */
    private int next;
    /** How fast the camera moves, smoothed. */
    private final Fineness.Pace pace = new Fineness.Pace();

    /** A level of a tile, built: its points, the way its ground faces at each, its tint, its triangles. */
    private record Built(float[] points, float[] normals, float[] coords, int[] faces) {
    }

    /** A tile: from height {@code i0} to {@code i1} across, {@code j0} to {@code j1} along, each way inclusive. */
    private final class Tile {
        final int i0;
        final int i1;
        final int j0;
        final int j1;
        final MeshView view = new MeshView();
        final TriangleMesh[] meshes = new TriangleMesh[LEVELS];
        /** Each level as built, once it is; and being built, until it is. */
        final Built[] built = new Built[LEVELS];
        final Future<?>[] building = new Future<?>[LEVELS];
        /** Which way each height's ground faces, as shown, turned up by the haze laid over it. */
        final float[][] shown = new float[LEVELS][];
        int level = -1;
        /** Whether it has any haze on it; where the camera was when it was last worked out. */
        boolean hazed;
        Vec3 hazedFrom;

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

        /** Its level {@code level} built, if it is not and is not being: apart from the screen. */
        void ask(final int level) {
            if (built[level] == null && building[level] == null) {
                building[level] = BUILDERS.submit(() -> {
                    built[level] = build(this, level);
                });
            }
        }

        /** Whether its level {@code level} is built, and ready to be shown. */
        boolean ready(final int level) {
            if (built[level] != null) {
                return true;
            }
            final Future<?> being = building[level];
            if (being != null && being.isDone()) {
                try {
                    being.get();
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (final ExecutionException e) {
                    throw new IllegalStateException("a tile of the land could not be built", e.getCause());
                }
                building[level] = null;
            }
            return built[level] != null;
        }
    }

    /**
     * The land, its lie surveyed, lit as {@code lit} lights level ground, each channel ({@link Horizon#lighting}):
     * its base colour shaded by the sun as its slopes face it, all else in one picture lit by itself (see
     * {@link #picture}). Every tile at its coarsest at once, built side by side; each finer as it is wanted,
     * built apart from the screen.
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
        // every tile at its coarsest, to start with, and shown
        final int coarsest = LEVELS - 1;
        for (final Tile tile : tiles) {
            tile.ask(coarsest);
        }
        for (final Tile tile : tiles) {
            try {
                tile.building[coarsest].get();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (final ExecutionException e) {
                throw new IllegalStateException("a tile of the land could not be built", e.getCause());
            }
            tile.ready(coarsest);
            show(tile, coarsest, Vec3.ZERO);
        }
        seenFrom(Vec3.ZERO, 0);
    }

    public Node node() {
        return root;
    }

    /**
     * Each tile as fine as it should be, seen from {@code eye}, where the camera now is, {@code seconds} since it
     * was last (how fast it moves counting, smoothed: {@link Fineness.Pace}), as soon as it is built
     * (it is built apart from the screen, ahead of being wanted; until it is, the tile is shown as it was); and
     * its haze worked out again where the camera has moved far enough for it to show. A frame does no more than
     * {@link #BUDGET} heights' worth of either: what a tile changes, the whole of it is sent again to be drawn,
     * and all of them at once would hold the frame up. So the tiles that should be finer or coarser go first,
     * then the haze, round the tiles in turn, a few each frame: so far off, it hardly moves while it waits.
     */
    public void seenFrom(final Vec3 eye, final double seconds) {
        final double speed = pace.at(eye, seconds);
        work = new Work();
        int left = BUDGET;
        int done = 0;
        for (final Tile tile : tiles) {
            final double near = tile.away(eye)[0];
            final int wanted = level(near, speed, tile.level);
            tile.ask(wanted);
            // and, ahead of coming nearer, finer
            tile.ask(level(near * AHEAD, speed, tile.level));
            if (wanted != tile.level && tile.ready(wanted) && (left > 0 || done == 0)) {
                show(tile, wanted, eye);
                left -= tile.built[wanted].coords().length / 2;
                done++;
            }
        }
        for (int k = 0; k < tiles.size() && (left > 0 || done == 0); k++) {
            final Tile tile = tiles.get(next);
            next = (next + 1) % tiles.size();
            if (stale(tile, eye)) {
                final int heights = tile.built[tile.level].coords().length / 2;
                if (heights > left && done > 0) {
                    break;
                }
                haze(tile, eye);
                left -= heights;
                done++;
                work.hazes++;
                work.hazed += heights;
            }
        }
    }

    /**
     * What the last frame did to the land, to be measured: how many tiles it swapped to another level, how many
     * of those were sent to be drawn for the first time, and how many heights those were; and how many tiles'
     * haze it worked out again, and how many heights; how fast it reckoned the camera went.
     */
    public static final class Work {
        public int swaps;
        public int fresh;
        public int swapped;
        public int hazes;
        public int hazed;
        public double speed;
        public final StringBuilder levels = new StringBuilder();
    }

    private Work work = new Work();

    /** What the last frame did to the land ({@link Work}). */
    public Work work() {
        work.speed = pace.speed();
        return work;
    }

    /** Tile {@code tile} shown at level {@code level}, its haze as seen from {@code eye}. */
    private void show(final Tile tile, final int level, final Vec3 eye) {
        work.swaps++;
        work.swapped += tile.built[level].coords().length / 2;
        work.levels.append(work.levels.isEmpty() ? "" : ",").append(tile.level).append('>').append(level);
        if (tile.meshes[level] == null) {
            work.fresh++;
            final Built built = tile.built[level];
            final TriangleMesh mesh = new TriangleMesh(VertexFormat.POINT_NORMAL_TEXCOORD);
            mesh.getPoints().setAll(built.points());
            mesh.getNormals().setAll(built.normals());
            mesh.getTexCoords().setAll(built.coords());
            mesh.getFaces().setAll(built.faces());
            tile.meshes[level] = mesh;
            tile.shown[level] = built.normals().clone();
        }
        tile.level = level;
        haze(tile, eye);
        tile.view.setMesh(tile.meshes[level]);
    }

    /**
     * Whether tile {@code tile}'s haze is to be worked out again from {@code eye}: if it has any, or had, and the
     * camera has moved since by {@link #MOVED}, or by {@link #SHARE} of how far off the tile is, if more.
     */
    private boolean stale(final Tile tile, final Vec3 eye) {
        if (tile.level < 0) {
            return false;
        }
        final double[] away = tile.away(eye);
        final boolean far = away[1] > Horizon.HAZE;
        if (!far && !tile.hazed) {
            return false;
        }
        if (!far || tile.hazedFrom == null) {
            return true;
        }
        final double moved = Math.hypot(eye.x() - tile.hazedFrom.x(), eye.z() - tile.hazedFrom.z());
        return moved > Math.max(MOVED, SHARE * away[0]);
    }

    /**
     * Which level a tile {@code away} from the camera is drawn at, the camera moving at {@code speed}, having been
     * at {@code was} (-1 if not yet): its fineness, rounded ({@link Fineness}).
     */
    static int level(final double away, final double speed, final int was) {
        return Fineness.level(Fineness.of(away, speed), was);
    }

    /**
     * Tile {@code tile} at level {@code level}: the land over it as fine as that level needs ({@link LandSheet}),
     * and its skirt hanging from its edges; each point tinted by how high it is, a little mottled, no haze yet.
     * Worked out apart from the screen: what it reads of the land is read only.
     */
    private Built build(final Tile tile, final int level) {
        final LandSheet.Made made = LandSheet.of(land, lie.x(tile.i0), lie.z(tile.j0), lie.x(tile.i1), lie.z(tile.j1),
                TOLERANCE[level], FINEST[level], LARGEST[level], FEATURED[level], lie.every(), COVERS[level], SKIRT);
        final float[] coords = new float[2 * made.size()];
        final float[] points = made.points();
        final double[] covered = new double[Land.Cover.values().length];
        for (int v = 0; v < made.size(); v++) {
            final double px = points[3 * v];
            final double pz = points[3 * v + 2];
            final double tint = (made.heights()[v] - low) / span
                    + MOTTLE * (2 * Num.hash(Math.round(px / MOTTLED), Math.round(pz / MOTTLED)) - 1);
            for (final Land.Cover c : Land.Cover.values()) {
                covered[c.ordinal()] = land.cover(c, px, pz);
            }
            coords[2 * v] = (float) ((column(covered, tint) + 0.5) / TINTS);
            coords[2 * v + 1] = (float) (0.5 / HAZES);
        }
        // each corner's point, the way it faces and its tint, all by the same number
        final int[] corners = made.triangles();
        final int[] faces = new int[3 * corners.length];
        for (int k = 0; k < corners.length; k++) {
            faces[3 * k] = corners[k];
            faces[3 * k + 1] = corners[k];
            faces[3 * k + 2] = corners[k];
        }
        return new Built(points, made.normals(), coords, faces);
    }

    /**
     * Tile {@code tile}'s haze, height by height, seen from {@code eye}: and its ground turned up as the haze
     * thickens, so a far slope is lit as the far ground is, level, and fades into the haze as that does. Lit
     * full on by the sun far off, it would show brighter than the haze it is going into.
     */
    private void haze(final Tile tile, final Vec3 eye) {
        final int level = tile.level;
        final float[] coords = tile.built[level].coords();
        final float[] points = tile.built[level].points();
        final float[] facing = tile.built[level].normals();
        final float[] shown = tile.shown[level];
        final boolean far = tile.away(eye)[1] > Horizon.HAZE;
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
        tile.hazed = far;
        tile.hazedFrom = eye;
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
        final int laid = COVERED_BY.length * COVERED;
        double column = laid + Math.clamp(t, 0, 1) * (TINTS - 1 - laid);
        for (int c = 0; c < COVERED_BY.length; c++) {
            final double middle = (COVERED_BY.length - 1 - c) * COVERED + COVERED / 2.0;
            column += (middle - column) * Math.clamp(covers[c], 0, 1);
        }
        return column;
    }

    /** The colour of the picture's column {@code x}: a cover, or a tint of the grass. */
    static Color colour(final int x) {
        final int laid = COVERED_BY.length * COVERED;
        return x < laid ? COVERED_BY[COVERED_BY.length - 1 - x / COVERED] : tint((x - laid) / (double) (TINTS - 1 - laid));
    }

    /** The land's tint {@code t} of the way from its lowest to its highest. */
    static Color tint(final double t) {
        final double at = Math.clamp(t, 0, 1) * (RAMP.length - 1);
        final int k = Math.min(RAMP.length - 2, (int) at);
        return RAMP[k].interpolate(RAMP[k + 1], at - k);
    }
}
