package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Box;
import com.example.fxsuite.avatar.sim.Form;
import com.example.fxsuite.avatar.sim.Num;

import java.util.ArrayList;
import java.util.List;

/**
 * The park on the sky hull's deck ({@link SkyHull}): laid out on its deck as it is straight, and bent with it. In its
 * middle, a football pitch, a real one's proportions scaled to as wide as the deck allows, its lines and its goals; to
 * its stern, a pool along its port side, the inside of the curve, a deck to lie by it beside, with palms; to its bow, a
 * basketball court, its lines and its hoops; the rest garden, lawn and trees, a path down its middle, hedges along
 * its edges, benches; and a glass rail round all of it, at the deck's edge, out to the tip of its bow.
 *
 * <p>In the hull's own frame, as it is laid out straight (across, along, up; its middle at across 0, along 0; up from
 * the lowest of its bottom), on its deck.
 */
public final class SkyPark {

    /** What it is called, on its hull. */
    public static final String NAME = "sky park";

    /** A football pitch, as the game lays one out: 105 m by 68; and what is marked on it, in its own measures. */
    static final double PITCH_LONG = 10_500;
    static final double PITCH_WIDE = 6_800;
    /** How far clear of the deck's edge the pitch keeps each side, and past its lines it is grass. */
    static final double CLEAR = 200;
    static final double RUN_OFF = 150;
    /** A basketball court, as the game lays one out: 28 m by 15. */
    static final double COURT_LONG = 2_800;
    static final double COURT_WIDE = 1_500;
    /** How wide its painted lines are, how high off what they are painted on. */
    static final double LINE = 10;
    /** How high the glass rail round it is, and how thick. */
    static final double RAIL = 110;
    static final double RAILING = 5;
    /** How deep the pool is, its water how high in it, its walls how thick. */
    static final double POOL_DEEP = 130;
    static final double WATER = 120;
    static final double POOL_WALL = 30;
    /** How far under its rim the pool's water lies; how thick its floor is. */
    static final double SURFACE = 15;
    static final double FLOOR = 10;
    /** Its steps up out of it: each no higher than this, so deep, so wide. */
    static final double STEP_UP = 19;
    static final double TREAD = 35;
    static final double STEPS_WIDE = 200;
    /** A path between what is laid out, how wide. */
    static final double PATH = 300;

    private final SkyHull hull;
    private final double deck;

    public SkyPark(final SkyHull hull) {
        this.hull = hull;
        this.deck = hull.size().depth() + SkyHull.DECK;
    }

    /** How high its deck's top is: what the park is laid on. */
    public double deck() {
        return deck;
    }

    // --- where each thing is: rectangles on the deck, across and along -----------------------------------------

    /** How much smaller than a real pitch the pitch is: as wide as the deck allows, clear of its edges. */
    double scale() {
        return (hull.size().beam() - 2 * CLEAR - 2 * RUN_OFF) / PITCH_WIDE;
    }

    /** The pitch, its lines: in the middle of the deck. */
    public Box pitch() {
        final double w = PITCH_WIDE * scale() / 2;
        final double l = PITCH_LONG * scale() / 2;
        return new Box(-w, w, -l, l, deck, deck);
    }

    /** The pitch's grass: its lines and as far again as its run-off. */
    Box grass() {
        return pitch().grown(RUN_OFF, 0);
    }

    /**
     * The pool: to the stern of the pitch, along its port side; from station to station of the hull, so its deck is
     * opened for it plate by whole plate; sunk in it, its rim the deck, its floor as deep as the pool.
     */
    public Box pool() {
        final double north = hull.snapped(grass().minZ() - 2 * PATH);
        final double half = hull.size().beam() / 2;
        return new Box(-half + CLEAR, half * 0.3, hull.snapped(north - 9_500), north, deck - POOL_DEEP, deck);
    }

    /** Where its hull's deck is left open for what is sunk in it ({@link SkyHull#opened}): the pool, down to under its floor. */
    public List<Box> holes() {
        final Box p = pool();
        return List.of(new Box(p.minX(), p.maxX(), p.minZ(), p.maxZ(), p.low() - FLOOR, p.high()));
    }

    /**
     * The pool's water: a little under its rim, its edges in under its walls. Not one of its solids: where the park
     * lies, it is still water, as a pond is, drawn as the rivers' and the ponds' is, never in the way.
     */
    public Box water() {
        final Box p = pool();
        final double t = POOL_WALL / 2;
        final double surface = deck - SURFACE;
        return new Box(p.minX() + t, p.maxX() - t, p.minZ() + t, p.maxZ() - t, surface, surface);
    }

    /** The pool's water as a solid, a thin sheet: where there is no still water to draw it as (a bench). */
    public Solid waterSolid() {
        final Box w = water();
        return slab("pool water", Role.GLAZING, Material.WATER, w.minX(), w.maxX(), w.minZ(), w.maxZ(), w.low() - 1,
                w.high());
    }

    /** The deck to lie by the pool: beside it, to starboard. */
    Box lounge() {
        final Box pool = pool();
        return new Box(pool.maxX() + PATH / 2, hull.size().beam() / 2 - CLEAR, pool.minZ(), pool.maxZ(), deck, deck);
    }

    /** The melon stall's spot: how far out across it its middle is past the court's side and its path, and how big. */
    static final double STALL_OUT = 230;
    static final double STALL_FRONT = 130;
    static final double STALL_BACK = 110;
    static final double STALL_HALF = 190;

    /**
     * Where the melon stall stands ({@code DeckScene#stall}): on the lawn to starboard of the court, halfway along it;
     * its table along the hull, its front, where its customer stands, toward the court, its stallholder on its far side.
     * Its middle at the box's middle across and along; turned so its front faces the court ({@link #STALL_YAW}).
     */
    public Box stall() {
        final Box court = court();
        final double x = court.maxX() + RUN_OFF + PATH + STALL_OUT;
        final double y = (court.minZ() + court.maxZ()) / 2;
        return new Box(x - STALL_FRONT, x + STALL_BACK, y - STALL_HALF, y + STALL_HALF, deck + 3, deck + 3);
    }

    /** Which way the stall's frame heads, in the hull's frame laid out straight: out to starboard, its front to the court. */
    public static final double STALL_YAW = 90;

    /** The basketball court, its lines: to the bow of the pitch, along the middle of the deck. */
    public Box court() {
        final double south = grass().maxZ() + 2 * PATH + RUN_OFF;
        return new Box(-COURT_WIDE / 2, COURT_WIDE / 2, south, south + COURT_LONG, deck, deck);
    }

    /** What is laid out, each as far as it reaches, and a path's width round it: where no tree, no hedge, no bench is. */
    private List<Box> laid() {
        return List.of(grass().grown(PATH, 0), pool().grown(PATH, 0), lounge().grown(PATH / 2, 0),
                court().grown(RUN_OFF + PATH, 0), stall().grown(PATH / 2, 0));
    }

    /** Whether ({@code across}, {@code along}) is on the deck, {@code clear} in from its edge. */
    boolean onDeck(final double across, final double along, final double clear) {
        return Math.abs(along) < hull.size().length() / 2 && Math.abs(across) < hull.half(along) - clear;
    }

    /** Whether ({@code across}, {@code along}) is garden: on the deck, clear of its edge, and of all that is laid out. */
    boolean garden(final double across, final double along, final double clear) {
        if (!onDeck(across, along, clear)) {
            return false;
        }
        for (final Box b : laid()) {
            if (b.grown(clear, 0).holds(across, along, b.low())) {
                return false;
            }
        }
        return true;
    }

    // --- all of it ------------------------------------------------------------------------------------------------

    /** All of it, in the hull's frame laid out straight, on its deck. */
    public List<Solid> solids() {
        final List<Solid> solids = new ArrayList<>();
        pitch(solids);
        pool(solids);
        court(solids);
        garden(solids);
        rail(solids);
        return solids;
    }

    /** The pitch: its grass; its lines, as a real one's, scaled; its goals. */
    private void pitch(final List<Solid> solids) {
        final Box g = grass();
        solids.add(slab("pitch grass", Role.STRUCTURE, Material.GRASS, g.minX(), g.maxX(), g.minZ(), g.maxZ(), deck,
                deck + 3));
        final double s = scale();
        final Box p = pitch();
        final double up = deck + 3;
        final double w = p.maxX();
        final double l = p.maxZ();
        rectangle(solids, "pitch line", -w, w, -l, l, up);
        line(solids, "pitch halfway line", -w, 0, w, 0, up);
        circle(solids, "pitch centre circle", 0, 0, 915 * s, 0, 360, up);
        solids.add(slab("pitch centre spot", Role.FRAME, Material.LIMESTONE, -LINE, LINE, -LINE, LINE, up, up + 1));
        for (final int end : new int[] {-1, 1}) {
            final double goal = end * l;
            final String at = end < 0 ? "south" : "north";
            // its penalty area and its goal area, as wide and as deep as a real one's, scaled
            box(solids, "pitch " + at + " penalty area", 4032 * s / 2, goal, goal - end * 1650 * s, up);
            box(solids, "pitch " + at + " goal area", 1832 * s / 2, goal, goal - end * 550 * s, up);
            final double spot = goal - end * 1100 * s;
            solids.add(slab("pitch " + at + " penalty spot", Role.FRAME, Material.LIMESTONE, -LINE, LINE, spot - LINE,
                    spot + LINE, up, up + 1));
            // its arc, outside the penalty area
            final double r = 915 * s;
            final double reach = Math.toDegrees(Math.acos((1650 * s - 1100 * s) / r));
            circle(solids, "pitch " + at + " penalty arc", 0, spot, r, end < 0 ? 90 - reach : 270 - reach,
                    end < 0 ? 90 + reach : 270 + reach, up);
            // its corners' arcs
            for (final int side : new int[] {-1, 1}) {
                final double from = Math.toDegrees(Math.atan2(-end, -side));
                circle(solids, "pitch " + at + " corner " + side, side * w, goal, 100 * s, from - 45, from + 45, up);
            }
            goal(solids, "pitch " + at + " goal", goal, end, up);
        }
    }

    /** A goal on goal line {@code along}, its net's frame toward {@code end}: posts, a crossbar, and its frame back. */
    private void goal(final List<Solid> solids, final String name, final double along, final int end, final double up) {
        final double half = 150;
        final double high = 200;
        final double back = along + end * 120;
        final double post = 10;
        for (final int side : new int[] {-1, 1}) {
            solids.add(member(name + ", post " + side, side * half, along, up, side * half, along, up + high, post));
            solids.add(member(name + ", stay " + side, side * half, along, up + high, side * half, back, up, post / 2));
            solids.add(member(name + ", foot " + side, side * half, along, up + post / 2, side * half, back, up + post / 2,
                    post / 2));
        }
        solids.add(member(name + ", crossbar", -half, along, up + high, half, along, up + high, post));
        solids.add(member(name + ", back", -half, back, up + post / 2, half, back, up + post / 2, post / 2));
    }

    /**
     * The pool, sunk in the deck, its rim flush with it, to be stepped or jumped into from anywhere round it: its
     * floor, its walls, its water a sheet a little under the rim, its edges in under the walls, so no face of it lies on
     * another's; its west wall just over the water, an edge it seems to flow over; steps up out of it in its north-east
     * corner, each no higher than a step.
     */
    private void pool(final List<Solid> solids) {
        final Box p = pool();
        final double low = p.low();
        solids.add(slab("pool floor", Role.STRUCTURE, Material.POOL_TILE, p.minX(), p.maxX(), p.minZ(), p.maxZ(),
                low - FLOOR, low));
        final double t = POOL_WALL;
        final double surface = deck - SURFACE;
        solids.add(slab("pool west wall", Role.STRUCTURE, Material.POOL_TILE, p.minX(), p.minX() + t, p.minZ(),
                p.maxZ(), low, surface + 1));
        solids.add(slab("pool east wall", Role.STRUCTURE, Material.POOL_TILE, p.maxX() - t, p.maxX(), p.minZ(),
                p.maxZ(), low, deck));
        solids.add(slab("pool south wall", Role.STRUCTURE, Material.POOL_TILE, p.minX() + t, p.maxX() - t, p.minZ(),
                p.minZ() + t, low, deck));
        solids.add(slab("pool north wall", Role.STRUCTURE, Material.POOL_TILE, p.minX() + t, p.maxX() - t,
                p.maxZ() - t, p.maxZ(), low, deck));
        // its water not among its solids: it lies in it as still water does, drawn as the rivers' and the ponds' is ({@link #water})
        final int steps = (int) Math.ceil(POOL_DEEP / STEP_UP);
        for (int k = 1; k <= steps; k++) {
            solids.add(slab("pool step " + k, Role.STRUCTURE, Material.POOL_TILE, p.maxX() - t - STEPS_WIDE,
                    p.maxX() - t, p.maxZ() - t - (steps + 1 - k) * TREAD, p.maxZ() - t, low,
                    low + POOL_DEEP * k / steps));
        }
        // the deck to lie by it, its loungers in two rows, palms along its far side
        final Box d = lounge();
        solids.add(slab("lounge deck", Role.STRUCTURE, Material.TIMBER, d.minX(), d.maxX(), d.minZ(), d.maxZ(), deck,
                deck + 5));
        final double top = deck + 5;
        int n = 0;
        for (double y = d.minZ() + 200; y + 70 < d.maxZ(); y += 300) {
            for (final double x : new double[] {d.minX() + 150, d.minX() + 450}) {
                solids.add(slab("lounger " + n, Role.FRAME, Material.PAINTED, x - 35, x + 35, y, y + 190, top,
                        top + 35));
                solids.add(slab("lounger " + n++ + ", back", Role.FRAME, Material.PAINTED, x - 35, x + 35, y + 140,
                        y + 190, top + 35, top + 80));
            }
        }
        int k = 0;
        for (double y = d.minZ() + 400; y < d.maxZ(); y += 1_200) {
            tree(solids, "palm " + k, d.maxX() - 150, y, top, 550 + 100 * Num.hash(k++, 0x9A1), 280, 80);
        }
    }

    /** The basketball court: its surface, its lines, a real one's; a hoop at each end. */
    private void court(final List<Solid> solids) {
        final Box c = court();
        final Box surface = c.grown(RUN_OFF + 50, 0);
        solids.add(slab("court", Role.STRUCTURE, Material.TILE, surface.minX(), surface.maxX(), surface.minZ(),
                surface.maxZ(), deck, deck + 3));
        final double up = deck + 3;
        final double w = c.maxX();
        final double mid = (c.minZ() + c.maxZ()) / 2;
        rectangle(solids, "court line", -w, w, c.minZ(), c.maxZ(), up);
        line(solids, "court centre line", -w, mid, w, mid, up);
        circle(solids, "court centre circle", 0, mid, 180, 0, 360, up);
        for (final int end : new int[] {-1, 1}) {
            final double base = end < 0 ? c.minZ() : c.maxZ();
            final String at = end < 0 ? "south" : "north";
            final double in = -end;
            // its key, its free-throw circle
            box(solids, "court " + at + " key", 245, base, base + in * 580, up);
            circle(solids, "court " + at + " free throw circle", 0, base + in * 580, 180, 0, 360, up);
            // its three-point line: straight from its baseline 90 cm in from each side, then round its basket
            final double basket = base + in * 157.5;
            final double straight = Math.sqrt(675 * 675 - 660 * 660);
            for (final int side : new int[] {-1, 1}) {
                line(solids, "court " + at + " three " + side, side * 660, base, side * 660, basket + in * straight, up);
            }
            final double reach = Math.toDegrees(Math.asin(660 / 675.0));
            final double facing = end < 0 ? 90 : 270;
            circle(solids, "court " + at + " three-point arc", 0, basket, 675, facing - reach, facing + reach, up);
            hoop(solids, "court " + at + " hoop", base, in, up);
        }
    }

    /** A hoop: its pole behind the baseline, its arm, its backboard, its ring. */
    private void hoop(final List<Solid> solids, final String name, final double base, final double in, final double up) {
        final double pole = base - in * 100;
        final double board = base + in * 120;
        solids.add(member(name + ", pole", 0, pole, up, 0, pole, up + 350, 15));
        solids.add(member(name + ", arm", 0, pole, up + 340, 0, board, up + 340, 10));
        solids.add(slab(name + ", board", Role.GLAZING, Material.GLASS, -90, 90, Math.min(board, board + in * 3),
                Math.max(board, board + in * 3), up + 290, up + 395));
        final double ring = base + in * 157.5;
        final double[][] points = new double[13][];
        for (int k = 0; k <= 12; k++) {
            final double a = 2 * Math.PI * k / 12;
            points[k] = new double[] {22.5 * Math.cos(a), ring + 22.5 * Math.sin(a)};
        }
        for (int k = 0; k < 12; k++) {
            solids.add(member(name + ", ring " + k, points[k][0], points[k][1], up + 305, points[k + 1][0],
                    points[k + 1][1], up + 305, 2));
        }
    }

    /**
     * The garden: lawn wherever nothing else is laid, a path down the deck's middle, trees where its seed puts them,
     * hedges along its edges, a bench every so often by its path.
     */
    private void garden(final List<Solid> solids) {
        final double length = hull.size().length();
        final double step = 300;
        int n = 0;
        // lawn and path, a strip at a time, from its stern to its bow, where there is garden across all of the strip
        for (double y = -length / 2; y < length / 2; y += step) {
            final double y1 = Math.min(length / 2, y + step);
            final double h0 = hull.half(y) - CLEAR;
            final double h1 = hull.half(y1) - CLEAR;
            if (h0 <= PATH || h1 <= PATH) {
                continue;  // so near its bow's tip, its deck is left bare, to look out from
            }
            // beside the court, lawn each side of it, as far as its edge
            final Box court = court().grown(RUN_OFF + PATH, 0);
            if (court.minZ() <= y && y1 <= court.maxZ()) {
                for (final int side : new int[] {-1, 1}) {
                    final double in = court.maxX();
                    solids.add(slab("lawn " + n + ", " + (side > 0 ? "starboard" : "port"), Role.STRUCTURE,
                            Material.GRASS, side > 0 ? in : -Math.min(h0, h1), side > 0 ? Math.min(h0, h1) : -in, y,
                            y1, deck, deck + 3));
                }
                n++;
                continue;
            }
            final double mid = (y + y1) / 2;
            if (!garden(0, mid, 0) || !garden(0, y + 1, 0) || !garden(0, y1 - 1, 0)) {
                continue;
            }
            solids.add(quad("lawn " + n, Role.STRUCTURE, Material.GRASS, h0, h1, y, y1, deck, deck + 3));
            solids.add(slab("path " + n, Role.STRUCTURE, Material.STONE, -PATH / 2, PATH / 2, y, y1, deck, deck + 6));
            n++;
        }
        // trees, as their seed puts them, on the lawn, clear of the path and the edge
        int k = 0;
        for (int t = 0; t < 400 && k < 60; t++) {
            final double y = (Num.hash(t, 0x7A1) - 0.5) * length;
            final double side = Num.hash(t, 0x7A2) < 0.5 ? -1 : 1;
            final double x = side * (PATH + 200 + Num.hash(t, 0x7A3) * (hull.half(y) - CLEAR - PATH - 500));
            final double wide = 250 + 200 * Num.hash(t, 0x7A5);
            // its crown all over the deck, where it narrows to its ends too
            final double reach = Math.abs(x) + wide / 2;
            if (!garden(x, y, 300) || Math.abs(x) < PATH || !onDeck(reach, y - wide / 2, 50) || !onDeck(reach, y + wide / 2, 50)) {
                continue;
            }
            tree(solids, "tree " + k++, x, y, deck + 3, 400 + 250 * Num.hash(t, 0x7A4), wide, 40);
        }
        // hedges along both edges, and benches by the path, where there is garden
        int h = 0;
        int b = 0;
        for (double y = -length / 2 + 200; y < length / 2 - 400; y += 600) {
            for (final int side : new int[] {-1, 1}) {
                final double x = side * (hull.half(y + 200) - CLEAR - 60);
                if (garden(x, y, 0) && garden(x, y + 400, 0) && Math.abs(x) > PATH) {
                    solids.add(slab("hedge " + h++, Role.DRESSING, Material.LEAVES, x - 50, x + 50, y, y + 400,
                            deck + 3, deck + 90));
                }
            }
            if ((int) Math.round(y / 600) % 4 == 0 && garden(PATH, y, 0) && garden(PATH, y + 180, 0)) {
                final double x = PATH / 2 + 60;
                solids.add(slab("bench " + b++, Role.FRAME, Material.TIMBER, x, x + 50, y, y + 180, deck + 40,
                        deck + 46));
            }
        }
    }

    /** A tree: its trunk {@code high}, a crown {@code wide} on it, a smaller one over that. */
    private static void tree(final List<Solid> solids, final String name, final double x, final double y,
                             final double foot, final double high, final double wide, final double trunk) {
        solids.add(slab(name + ", trunk", Role.FRAME, Material.BARK, x - trunk / 2, x + trunk / 2, y - trunk / 2,
                y + trunk / 2, foot, foot + high));
        solids.add(slab(name + ", crown", Role.DRESSING, Material.LEAVES, x - wide / 2, x + wide / 2, y - wide / 2,
                y + wide / 2, foot + high - wide * 0.4, foot + high + wide * 0.2));
        solids.add(slab(name + ", top", Role.DRESSING, Material.LEAVES, x - wide / 3, x + wide / 3, y - wide / 3,
                y + wide / 3, foot + high + wide * 0.2, foot + high + wide * 0.45));
    }

    /** The rail: glass at the deck's edge, each side, from its stern to the tip of its bow. */
    private void rail(final List<Solid> solids) {
        final int n = hull.size().spaces();
        for (int i = 0; i < n; i++) {
            final double y0 = hull.station(i);
            final double y1 = hull.station(i + 1);
            final double h0 = hull.half(y0);
            final double h1 = hull.half(y1);
            for (final int side : new int[] {-1, 1}) {
                final double[][] c = new double[8][];
                for (int k = 0; k < 8; k++) {
                    final double y = (k & 2) == 0 ? y0 : y1;
                    final double h = (k & 2) == 0 ? h0 : h1;
                    final double in = (k & 1) == 0 ? 0 : Math.min(RAILING, h);
                    c[k] = new double[] {side * (h - in) + 0.0, y, (k & 4) == 0 ? deck : deck + RAIL};
                }
                solids.add(hexa("rail " + (side > 0 ? "starboard " : "port ") + i, Role.GLAZING, Material.GLASS, c));
            }
        }
    }

    // --- marking out: lines, boxes, circles, slabs ----------------------------------------------------------------

    /** A painted line from one point to another, on what is {@code up} high. */
    private static void line(final List<Solid> solids, final String name, final double ax, final double ay,
                             final double bx, final double by, final double up) {
        final double dx = bx - ax;
        final double dy = by - ay;
        final double length = Math.hypot(dx, dy);
        final double nx = -dy / length * LINE / 2;
        final double ny = dx / length * LINE / 2;
        solids.add(hexa(name, Role.FRAME, Material.LIMESTONE, new double[][] {
                {ax - nx, ay - ny, up}, {ax + nx, ay + ny, up}, {bx - nx, by - ny, up}, {bx + nx, by + ny, up},
                {ax - nx, ay - ny, up + 1}, {ax + nx, ay + ny, up + 1}, {bx - nx, by - ny, up + 1},
                {bx + nx, by + ny, up + 1}}));
    }

    /** A painted rectangle, its four sides. */
    private static void rectangle(final List<Solid> solids, final String name, final double x0, final double x1,
                                  final double y0, final double y1, final double up) {
        line(solids, name + " south", x0, y0, x1, y0, up);
        line(solids, name + " north", x0, y1, x1, y1, up);
        line(solids, name + " west", x0, y0, x0, y1, up);
        line(solids, name + " east", x1, y0, x1, y1, up);
    }

    /** A painted box off a line across at {@code from}, {@code half} each side of the middle, out to {@code to}. */
    private static void box(final List<Solid> solids, final String name, final double half, final double from,
                            final double to, final double up) {
        line(solids, name + " west", -half, from, -half, to, up);
        line(solids, name + " east", half, from, half, to, up);
        line(solids, name + " across", -half, to, half, to, up);
    }

    /** A painted arc round ({@code x}, {@code y}), {@code r} out, from {@code from} to {@code to} degrees. */
    private static void circle(final List<Solid> solids, final String name, final double x, final double y,
                               final double r, final double from, final double to, final double up) {
        final int segments = Math.max(4, (int) Math.ceil((to - from) / 360 * 48));
        for (int k = 0; k < segments; k++) {
            final double a = Math.toRadians(from + (to - from) * k / segments);
            final double b = Math.toRadians(from + (to - from) * (k + 1) / segments);
            line(solids, name + " " + k, x + r * Math.cos(a), y + r * Math.sin(a), x + r * Math.cos(b),
                    y + r * Math.sin(b), up);
        }
    }

    /** A square member {@code size} across, white, from one point to another. */
    private static Solid member(final String name, final double x0, final double y0, final double z0, final double x1,
                                final double y1, final double z1, final double size) {
        return Building.member(name, new Form.Vertex("a", x0, y0, z0), new Form.Vertex("b", x1, y1, z1), size,
                Role.FRAME, Material.LIMESTONE);
    }

    /** A slab square to the deck. */
    static Solid slab(final String name, final Role role, final Material material, final double x0, final double x1,
                      final double y0, final double y1, final double z0, final double z1) {
        final double[][] c = new double[8][];
        for (int k = 0; k < 8; k++) {
            c[k] = new double[] {(k & 1) == 0 ? x0 : x1, (k & 2) == 0 ? y0 : y1, (k & 4) == 0 ? z0 : z1};
        }
        return hexa(name, role, material, c);
    }

    /** A slab across the deck, {@code h0} each side of its middle at {@code y0}, {@code h1} at {@code y1}. */
    private static Solid quad(final String name, final Role role, final Material material, final double h0,
                              final double h1, final double y0, final double y1, final double z0, final double z1) {
        return hexa(name, role, material, new double[][] {{-h0, y0, z0}, {h0, y0, z0}, {-h1, y1, z0}, {h1, y1, z0},
                {-h0, y0, z1}, {h0, y0, z1}, {-h1, y1, z1}, {h1, y1, z1}});
    }

    private static Solid hexa(final String name, final Role role, final Material material, final double[][] corners) {
        final List<Point> points = new ArrayList<>(8);
        for (final double[] c : corners) {
            points.add(new Point(c[0], c[1], c[2]));
        }
        return new Solid(name, role, material, points);
    }
}
