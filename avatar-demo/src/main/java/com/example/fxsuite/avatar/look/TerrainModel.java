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
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The land drawn, as surveyed ({@link Survey}): its heights as a sheet of triangles, lit as the sun falls
 * on its slopes, tinted by how high it is (the meadow's grass low down, drier grass up the slopes,
 * moor on the heights), a little mottled; and, far off, fading into the haze as far as the camera sees ({@link Sight}),
 * so the hills at the world's edge go into the sky.
 *
 * <p>Its base colour is lit by the sun and the sky; its tint, a little mottled, and the haze, are in one
 * picture lit by itself ({@link #picture}).
 *
 * <p>Drawn in pieces ({@link Pieces}), each as big as its fineness asks ({@link Fineness}), by its nearest point
 * and the camera's speed smoothed ({@link Fineness.Pace}): 125 m round the camera, 2 km far off; each a mesh of its
 * own. As fine as the land needs and no finer ({@link LandSheet}): near, within {@link #TOLERANCE} 3 cm of it, an
 * open meadow in squares tens of metres across and a river's bank in squares of half a metre; further off, less
 * closely, and no finer for what covers the ground. A skirt hangs from each piece's edges, so where a fine piece
 * meets a coarse one no gap shows between them. A piece is built apart from the screen ({@link #BUILDERS}), ahead
 * of being wanted, and shown once it is; what is out of view is not drawn ({@link View}).
 *
 * <p>Each piece's haze is worked out again, height by height, as the camera moves: once it has moved
 * {@link #MOVED}, or {@link #SHARE} of how far off the piece is, if more, since far off the haze changes
 * little with a few metres more or less; those near enough to have none are left alone, and those out of view
 * wait till they are in it.
 */
public final class TerrainModel {

    /**
     * How close to the land each level keeps, finest first; its smallest squares, its biggest, and its biggest
     * where a feature of the land lies; and how far what covers the ground may be from what its squares' corners
     * make of it (only near does that ask for finer squares).
     */
    static final double[] TOLERANCE = {3, 10, 40, 150, 400, 1_000, 2_500};
    static final double[] FINEST = {50, 100, 400, 1_000, 2_500, 5_000, 10_000};
    static final double[] LARGEST = {4_000, 8_000, 16_000, 31_250, 50_000, 100_000, 200_000};
    static final double[] FEATURED = {400, 1_600, 6_400, 25_000, 50_000, 100_000, 200_000};
    static final double[] COVERS = {0.08, 0.25, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
    /**
     * How far past the surveyed land, each way, it is drawn: a plain, its edge's heights carried on out, flat, so
     * from high up the land runs on past where it was laid out.
     */
    static final double PLAIN = 4_800_000;
    /** How many levels there are: as many as fineness rounds to. */
    static final int LEVELS = Fineness.LEVELS;
    /** How much nearer than it is a piece is built for, ahead of needing to be: so it is ready when it comes. */
    static final double AHEAD = Pieces.AHEAD;
    /** How far a piece's skirt hangs. */
    static final double SKIRT = 500;
    /** How much further than its level may be out the land under a road is drawn sunk, below the road's own strip. */
    static final double SUNK = 10;
    /**
     * How far the camera moves before a piece's haze is worked out again: at least so far, and so much of how far
     * off the piece is, since far off the haze changes so little with a few metres more or less.
     */
    static final double MOVED = 2_500;
    static final double SHARE = 0.05;
    /** How many heights' haze a frame works out again and sends to be drawn, at most (or one piece's, if more). */
    static final int BUDGET = 3_000;
    /** How many tints, low to high, and how many shades of the haze, none to all, the land's picture has. */
    private static final int TINTS = 256;
    private static final int HAZES = 64;
    /** How much each height's tint is moved up or down, at random: the grass mottled. */
    private static final double MOTTLE = 0.025;
    /** How far apart the mottle's spots are. */
    private static final double MOTTLED = 100;

    /** Where pieces are built, apart from the screen: as many at once as there are processors but one. */
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
    /** The land's lie, as surveyed. */
    private final Survey lie;
    private final Group root = new Group();
    private final double low;
    private final double span;
    private final PhongMaterial material;
    /** How fast the camera moves, smoothed. */
    private final Fineness.Pace pace = new Fineness.Pace();
    /** The land in pieces; those drawn, each its mesh; those shown last frame; the one whose haze is looked at next. */
    private final Pieces<Built> pieces;
    private final Map<Pieces.Piece<Built>, Drawn> drawn = new IdentityHashMap<>();
    private List<Pieces.Piece<Built>> shown = List.of();
    private int next;

    /** A piece, built: its points, the way its ground faces at each, its tint, its triangles. */
    record Built(float[] points, float[] normals, float[] coords, int[] faces) {

        int size() {
            return coords.length / 2;
        }
    }

    /** A piece as drawn: its mesh; which way each height's ground faces, as shown; where its haze was worked out from. */
    private static final class Drawn {
        final MeshView view = new MeshView();
        TriangleMesh mesh;
        float[] facing;
        boolean hazed;
        Vec3 hazedFrom;
        double hazedReach;
    }

    /**
     * The land, its lie surveyed, lit as {@code lit} lights level ground, each channel ({@link Horizon#lighting}):
     * its base colour shaded by the sun as its slopes face it, all else in one picture lit by itself (see
     * {@link #picture}). Its roughest pieces built at once, side by side; each finer as it is wanted, built apart
     * from the screen.
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
        pieces = new Pieces<>(lie.x(0) - PLAIN, lie.z(0) - PLAIN, lie.x(lie.across() - 1) + PLAIN, lie.z(lie.along() - 1) + PLAIN, this::build, Built::size, BUILDERS);
        for (final Pieces.Piece<Built> each : pieces.roots()) {
            try {
                each.building.get();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (final ExecutionException e) {
                throw new IllegalStateException("a piece of the land could not be built", e.getCause());
            }
        }
        seenFrom(View.all(Vec3.ZERO), 0);
    }

    public Node node() {
        return root;
    }

    /**
     * Each piece as fine as it should be, seen as {@code view} sees, {@code seconds} since it was last (how fast
     * the camera moves counting, smoothed: {@link Fineness.Pace}), as soon as it is built (until it is, what was
     * shown there is); those out of view not drawn. And the haze of those in view worked out again where the
     * camera has moved far enough for it to show, round them in turn, no more than {@link #BUDGET} heights' worth a
     * frame: all of it sent again to be drawn, and all of them at once would hold the frame up.
     */
    public void seenFrom(final View view, final double seconds) {
        final Vec3 eye = view.eye();
        final double speed = pace.at(eye, seconds);
        work = new Work();
        final Pieces.Shown<Built> now = pieces.seenFrom(eye, speed, view.height());
        for (final Pieces.Piece<Built> gone : now.dropped()) {
            final Drawn was = drawn.remove(gone);
            if (was != null) {
                root.getChildren().remove(was.view);
            }
        }
        for (final Pieces.Piece<Built> piece : shown) {
            if (!piece.shown) {
                final Drawn was = drawn.get(piece);
                if (was != null) {
                    was.view.setVisible(false);
                }
            }
        }
        for (final Pieces.Piece<Built> piece : now.shown()) {
            Drawn d = drawn.get(piece);
            if (d == null) {
                d = draw(piece, view);
            } else if (!shown.contains(piece)) {
                work.swaps++;
                work.levels.append(work.levels.isEmpty() ? "" : ",").append(piece.level);
            }
            d.view.setVisible(view.draws(piece.near(eye)) && view.sees(piece.x0, piece.z0, piece.x1, piece.z1));
        }
        shown = now.shown();
        int left = BUDGET;
        int done = 0;
        for (int k = 0; k < shown.size() && (left > 0 || done == 0); k++) {
            next = (next + 1) % shown.size();
            final Pieces.Piece<Built> piece = shown.get(next);
            final Drawn d = drawn.get(piece);
            if (d.view.isVisible() && stale(piece, d, view)) {
                final int heights = piece.built.size();
                if (heights > left && done > 0) {
                    break;
                }
                haze(piece, d, view);
                left -= heights;
                done++;
                work.hazes++;
                work.hazed += heights;
            }
        }
    }

    /**
     * What the last frame did to the land, to be measured: how many pieces it showed that it had not the frame
     * before, of what levels, how many of those were drawn for the first time and how many heights those were; and
     * how many pieces' haze it worked out again, and how many heights; how fast it reckoned the camera went; and how
     * many pieces are shown, and of those, how many in view.
     */
    public static final class Work {
        public int swaps;
        public int fresh;
        public int swapped;
        public int hazes;
        public int hazed;
        public double speed;
        public int pieces;
        public int inView;
        public final StringBuilder levels = new StringBuilder();
    }

    private Work work = new Work();

    /** What the last frame did to the land ({@link Work}). */
    public Work work() {
        work.speed = pace.speed();
        work.pieces = shown.size();
        work.inView = (int) shown.stream().filter(p -> drawn.get(p).view.isVisible()).count();
        return work;
    }

    /** Piece {@code piece} drawn for the first time: its mesh made, its haze as seen from {@code eye}. */
    private Drawn draw(final Pieces.Piece<Built> piece, final View view) {
        final Built built = piece.built;
        final Drawn d = new Drawn();
        d.mesh = new TriangleMesh(VertexFormat.POINT_NORMAL_TEXCOORD);
        d.mesh.getPoints().setAll(built.points());
        d.mesh.getNormals().setAll(built.normals());
        d.mesh.getTexCoords().setAll(built.coords());
        d.mesh.getFaces().setAll(built.faces());
        d.facing = built.normals().clone();
        d.view.setMaterial(material);
        d.view.setCullFace(CullFace.NONE);
        haze(piece, d, view);
        d.view.setMesh(d.mesh);
        root.getChildren().add(d.view);
        drawn.put(piece, d);
        work.swaps++;
        work.fresh++;
        work.swapped += built.size();
        work.levels.append(work.levels.isEmpty() ? "" : ",").append(piece.level);
        return d;
    }

    /**
     * Whether piece {@code piece}'s haze is to be worked out again as {@code view} sees: if it has any, or had, and the
     * camera has moved since by {@link #MOVED}, or by {@link #SHARE} of how far off the piece is, if more; or how far
     * it sees has changed since by more than {@link #SHARE} of it.
     */
    private static boolean stale(final Pieces.Piece<Built> piece, final Drawn d, final View view) {
        final Vec3 eye = view.eye();
        final boolean far = piece.far(eye) > view.clear();
        if (!far && !d.hazed) {
            return false;
        }
        if (!far || d.hazedFrom == null) {
            return true;
        }
        final double moved = Math.hypot(eye.x() - d.hazedFrom.x(), eye.z() - d.hazedFrom.z());
        return moved > Math.max(MOVED, SHARE * piece.near(eye)) || Math.abs(view.reach() - d.hazedReach) > SHARE * view.reach();
    }

    /**
     * Which level land {@code away} from the camera is drawn at, the camera moving at {@code speed}, having been
     * at {@code was} (-1 if not yet): its fineness, rounded ({@link Fineness}).
     */
    static int level(final double away, final double speed, final int was) {
        return Fineness.level(Fineness.of(away, speed), was);
    }

    /**
     * Piece {@code piece}, at its level: the land over it as fine as that level needs ({@link LandSheet}), and its
     * skirt hanging from its edges; each point tinted by how high it is, a little mottled, no haze yet. Worked out
     * apart from the screen: what it reads of the land is read only.
     */
    private Built build(final Pieces.Piece<Built> piece) {
        final int level = piece.level;
        final LandSheet.Made made = LandSheet.of(land, piece.x0, piece.z0, piece.x1, piece.z1,
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
            // under a road, sunk out of sight below its strip ({@link RoadSurface}): further than this level
            // may be out, so even coarse it never shows through; less toward its edges, none past them
            final double road = Math.max(covered[Land.Cover.ASPHALT.ordinal()], covered[Land.Cover.GRAVEL.ordinal()]);
            points[3 * v + 1] += (float) ((TOLERANCE[level] + SUNK) * Math.clamp(2 * road - 1, 0, 1));
            // and grass, as far as its colour goes: the road's own strip is the road, and its edge would come out, rough
            // and far off, a saw's teeth of what lies between it and the grass
            covered[Land.Cover.ASPHALT.ordinal()] = 0;
            covered[Land.Cover.GRAVEL.ordinal()] = 0;
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
     * Piece {@code piece}'s haze, height by height, seen from {@code eye}: and its ground turned up as the haze
     * thickens, so a far slope is lit as the far ground is, level, and fades into the haze as that does. Lit
     * full on by the sun far off, it would show brighter than the haze it is going into.
     */
    private static void haze(final Pieces.Piece<Built> piece, final Drawn d, final View view) {
        final Vec3 eye = view.eye();
        final float[] coords = piece.built.coords();
        final float[] points = piece.built.points();
        final float[] facing = piece.built.normals();
        final float[] shown = d.facing;
        final boolean far = piece.far(eye) > view.clear();
        for (int v = 0; v < coords.length / 2; v++) {
            final double haze = view.haze(Math.hypot(points[3 * v] - eye.x(), points[3 * v + 2] - eye.z()));
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
        d.mesh.getTexCoords().setAll(coords);
        d.mesh.getNormals().setAll(shown);
        d.hazed = far;
        d.hazedFrom = eye;
        d.hazedReach = view.reach();
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
