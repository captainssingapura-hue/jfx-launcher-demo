package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Form;
import com.example.fxsuite.avatar.sim.Num;
import com.example.fxsuite.avatar.sim.Prism;
import com.example.fxsuite.avatar.sim.Vec2;
import com.example.fxsuite.avatar.sim.Volume;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * An earth house, as homesteaders on open plains built them: round, its walls of earth, thick, a
 * little battered and a little uneven, plastered smooth; a low cone of a roof on them, turf with grass
 * growing on it, its eave drooping, the ends of the poles that carry it showing under it; a doorway in
 * its front, a plank door standing open inward; a small window to one side, shuttered; a mud chimney
 * at its back and a stovepipe through its roof. A whole house, inside and out: its walls as thick
 * inside as out, a floor of beaten earth, the poles under its roof overhead; walked into through its
 * door, stood up in under its roof.
 *
 * <p>Made to a size ({@link Size}): how wide, how high its walls and how thick, how high its roof
 * rises and how far its eave reaches, its door, its window, its chimney. What its walls are of (mud
 * plaster, red earth, pale clay, sod, rammed earth, fieldstone), its roof (turf, thatch, reed), its
 * window (shuttered or glazed) and its woodwork are choices, each its own colour.
 *
 * <p>In centimetres, in its own frame: x across, y along (its front, its door, +y), up.
 */
public final class EarthHouse {

    /** What it is called. */
    public static final String NAME = "earth house";

    /**
     * How big it is built: {@code width} across outside, its walls {@code walls} high outside (at the foot of
     * its eave) and {@code thick}; its roof rising {@code rise} over its eave, which reaches {@code eaves}
     * past its walls; its door {@code door} wide and {@code doorHigh} high; its window {@code window} wide
     * (none, 0); and a chimney, or not.
     */
    public record Size(double width, double walls, double thick, double rise, double eaves, double door,
                       double doorHigh, double window, boolean chimney) {

        /** As homesteaders built them, a little taller, to walk in upright: 5.6 m across, its walls 2.45 m high and half a metre thick. */
        public static final Size STANDARD = new Size(560, 245, 50, 170, 35, 95, 205, 70, true);

        public Size {
            check(width >= 300 && width <= 1400, "from 3 m to 14 m across");
            check(walls >= 180 && walls <= 450, "its walls from 1.8 m to 4.5 m high");
            check(thick >= 25 && thick <= 100 && thick <= width / 5, "its walls from 25 cm to 1 m thick, no more than a fifth as thick as it is wide");
            check(rise >= 40 && rise <= 600, "its roof rising from 40 cm to 6 m");
            check(eaves >= 0 && eaves <= 120, "its eave reaching up to 1.2 m past its walls");
            check(door >= 60 && door <= 180 && door <= width * 0.35, "its door from 60 cm to 1.8 m wide, no more than a third as wide as it");
            check(doorHigh >= 140 && doorHigh <= walls - LINTEL, "its door from 1.4 m high to a lintel's depth under its eave");
            check(window == 0 || window >= 30 && window <= 160 && window <= width * 0.3, "its window none, or from 30 cm to 1.6 m wide");
            check(window == 0 || SILL + WINDOW_LEAST <= walls - LINTEL, "room for its window under its eave");
        }

        private static void check(final boolean ok, final String what) {
            if (!ok) {
                throw new IllegalArgumentException("an earth house " + what);
            }
        }
    }

    /** How many ways round it is made: its walls' and its roof's pieces. */
    static final int SIDES = 40;
    /** How many rings its roof is made in, from its eave to its top. */
    private static final int RINGS = 8;
    /** How thick its roof's covering is; how much its walls lean in, outside, of their thickness; how uneven, outside. */
    static final double ROOF = 30;
    private static final double BATTER = 0.15;
    private static final double UNEVEN = 2;
    /** How deep the earth over its door and window must be, at least; its window's sill so high, and its window at least so tall. */
    static final double LINTEL = 15;
    static final double SILL = 90;
    private static final double WINDOW_LEAST = 30;
    private static final double WINDOW_TALL = 70;
    /** How thick its floor of earth. */
    static final double FLOOR = 4;
    /** Where round it its window is, and its chimney: headings from its front, to its right. */
    static final double WINDOW_AT = 62;
    static final double CHIMNEY_AT = 150;
    /** Its chimney: half as wide each way; how far it stands over its roof. */
    private static final double CHIMNEY = 32;
    private static final double CHIMNEY_OVER = 80;
    /** The poles under its roof: how many, how thick, how far their ends show past its eave. */
    private static final int RAFTERS = 16;
    private static final double RAFTER = 9;
    private static final double RAFTER_OUT = 18;
    /** Its door: how far it stands open, inward; how thick its leaf. */
    static final double DOOR_OPEN = 110;
    private static final double LEAF = 5;
    /** Its woodwork's posts, how thick. */
    private static final double POST = 10;
    /** On a roof of turf, tufts of grass: so many to a square metre. */
    private static final double TUFTS = 1.4;

    private final Size size;

    private EarthHouse(final Size size) {
        this.size = size;
    }

    /** The house built to {@code size}. */
    public static EarthHouse of(final Size size) {
        return new EarthHouse(size);
    }

    /** The house as homesteaders built them ({@link Size#STANDARD}). */
    public static EarthHouse standard() {
        return of(Size.STANDARD);
    }

    public Size size() {
        return size;
    }

    // ---------------------------------------------------------------- its lines

    /** Its walls' outside at their foot, their inside, and their outside at their top (leaning in); its eave. */
    double outside() {
        return size.width() / 2;
    }

    double inside() {
        return outside() - size.thick();
    }

    private double outsideTop() {
        return outside() - size.thick() * BATTER;
    }

    double eave() {
        return outsideTop() + size.eaves();
    }

    private double middle() {
        return (inside() + outside()) / 2;
    }

    /** How far up its roof's cone is, 1 at its point and 0 at its eave, at {@code r} out: evenly, a straight slope. */
    private double cone(final double r) {
        return 1 - Math.min(1, r / eave());
    }

    /**
     * Its roof's covering at its eave: as low as lets its underside meet its walls' tops outside at their
     * height, its eave drooping; but no lower than its walls' height, so its eave is walked under.
     */
    private double base() {
        return Math.max(size.walls(), size.walls() + ROOF - size.rise() * cone(outsideTop()));
    }

    /** How high its roof's covering is, at {@code r} out; and its underside. */
    double over(final double r) {
        return base() + size.rise() * cone(r);
    }

    double under(final double r) {
        return over(r) - ROOF;
    }

    /** How high it stands: its roof's top. */
    public double top() {
        return over(0);
    }

    /** Half the angle, degrees, an opening {@code wide} takes at its walls' middle. */
    private double half(final double wide) {
        return Math.toDegrees(Math.asin(wide / 2 / middle()));
    }

    /** Its window's top. */
    double windowTop() {
        return SILL + Math.min(WINDOW_TALL, size.walls() - LINTEL - SILL);
    }

    /** What is at heading {@code a} round its walls: its doorway, its window, or wall. */
    private enum Bay { DOOR, WINDOW, WALL }

    private Bay bay(final double a) {
        if (Math.abs(Num.wrap(a)) < half(size.door())) {
            return Bay.DOOR;
        }
        if (size.window() > 0 && Math.abs(Num.wrap(a - WINDOW_AT)) < half(size.window())) {
            return Bay.WINDOW;
        }
        return Bay.WALL;
    }

    /** Where its walls are cut round it: evenly, and at each side of its door and window, each pair a stretch of wall or an opening. */
    private List<Double> cuts() {
        final List<Double> edges = new ArrayList<>(List.of(-half(size.door()), half(size.door())));
        if (size.window() > 0) {
            edges.add(WINDOW_AT - half(size.window()));
            edges.add(WINDOW_AT + half(size.window()));
        }
        final TreeSet<Double> cuts = new TreeSet<>(edges.stream().map(EarthHouse::round).toList());
        for (int k = 0; k < SIDES; k++) {
            final double a = round(-180 + 360.0 * k / SIDES);
            // none so near an opening's side as to leave a sliver
            if (edges.stream().noneMatch(e -> Math.abs(Num.wrap(e - a)) < 360.0 / SIDES / 3)) {
                cuts.add(a);
            }
        }
        return new ArrayList<>(cuts);
    }

    private static double round(final double degrees) {
        return Math.round(Num.wrap(degrees) * 1e6) / 1e6;
    }

    /** How far its walls' outside is from its middle at heading {@code a}, at their foot: a little uneven, as earth is. */
    private double outsideAt(final double a, final boolean top) {
        final double uneven = UNEVEN * (2 * Num.hash(Math.round(Num.wrap(a) * 1000), 17) - 1);
        return (top ? outsideTop() : outside()) + uneven;
    }

    /** A point {@code r} out at heading {@code a}, {@code up} high. */
    private static Point at(final double r, final double a, final double up) {
        final double t = Math.toRadians(a);
        return new Point(r * Math.sin(t), r * Math.cos(t), up);
    }

    // ---------------------------------------------------------------- its skeleton

    /** Its lines: its walls' middle round its foot and its top, an upright every so often; its eave, the ribs of its cone to its point. */
    public Form skeleton() {
        final Form.Builder form = new Form.Builder(NAME);
        final int round = SIDES / 2;
        final int[] feet = new int[round];
        final int[] heads = new int[round];
        for (int k = 0; k < round; k++) {
            final double a = 360.0 * k / round;
            final Point foot = at(middle(), a, 0);
            final Point head = at(middle(), a, size.walls());
            feet[k] = form.vertex("wall " + k + ", foot", foot.across(), foot.along(), 0);
            heads[k] = form.vertex("wall " + k + ", head", head.across(), head.along(), size.walls());
            if (k % 2 == 0) {
                form.edge(feet[k], heads[k]);
            }
        }
        form.loop(feet);
        form.loop(heads);
        final int top = form.vertex("roof's top", 0, 0, top());
        final int ribs = 8;
        final int[] eaves = new int[ribs];
        for (int k = 0; k < ribs; k++) {
            final double a = 360.0 * k / ribs;
            int last = -1;
            for (int i = 0; i < RINGS; i++) {
                final double r = ring(i);
                final Point p = at(r, a, over(r));
                final int v = form.vertex("rib " + k + ", " + i, p.across(), p.along(), p.up());
                if (i == 0) {
                    eaves[k] = v;
                } else {
                    form.edge(last, v);
                }
                last = v;
            }
            form.edge(last, top);
        }
        form.loop(eaves);
        return form.build();
    }

    /** Its roof's {@code i}th ring, from its eave (0) in to its point, evenly. */
    private double ring(final int i) {
        return eave() * (1 - (double) i / RINGS);
    }

    // ---------------------------------------------------------------- dressed

    /** Its palette: mud walls, a turf roof, a floor of earth, its woodwork weathered timber, its window shuttered. */
    public static Palette palette() {
        return new Palette("earth and turf", Map.of(Role.WALL, Material.MUD, Role.SPIRE, Material.MUD, Role.ROOF, Material.TURF,
                Role.BASE, Material.EARTH, Role.FRAME, Material.TIMBER, Role.STRUCTURE, Material.TIMBER,
                Role.GLAZING, Material.TIMBER, Role.DRESSING, Material.GRASS), 50, 50, 0);
    }

    /** What its walls can be of; its roof. */
    public static final List<Material> WALLS = List.of(Material.MUD, Material.RED_EARTH, Material.PALE_CLAY, Material.SOD,
            Material.RAMMED_EARTH, Material.STONE);
    public static final List<Material> ROOFS = List.of(Material.TURF, Material.THATCH, Material.REED);

    /**
     * It as a building to dress ({@link Building}): its skeleton, and all of it made as its palette says
     * (one structure). Its choices: what its walls are of, and its chimney with them; its roof; its window,
     * shuttered or glazed; its woodwork, weathered or painted.
     */
    public Building building() {
        final Map<String, List<Building.Option>> choices = new LinkedHashMap<>();
        choices.put("Walls", WALLS.stream().map(m -> Building.Option.choosing(capitalised(m.title()), Map.of(),
                Map.of(Role.WALL, m, Role.SPIRE, m))).toList());
        choices.put("Roof", ROOFS.stream().map(m -> Building.Option.choosing(capitalised(m.title()), Map.of(),
                Map.of(Role.ROOF, m))).toList());
        choices.put("Window", List.of(Building.Option.choosing("Shuttered", Map.of(), Map.of(Role.GLAZING, Material.TIMBER)),
                Building.Option.choosing("Glazed", Map.of(), Map.of(Role.GLAZING, Material.GLASS))));
        choices.put("Woodwork", List.of(
                Building.Option.choosing("Weathered timber", Map.of(), Map.of(Role.FRAME, Material.TIMBER)),
                Building.Option.choosing("Painted", Map.of(), Map.of(Role.FRAME, Material.PAINTED))));
        final Building.Structure all = (skeleton, palette) -> solids(palette);
        return new Building(NAME, skeleton(), List.of(), List.of(all), choices, List.of(palette()));
    }

    private static String capitalised(final String text) {
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    /** All of it, made as {@code palette} says: its walls, its floor, its roof and the poles under it, its chimney, its door, its window. */
    public List<Solid> solids(final Palette palette) {
        final List<Solid> solids = new ArrayList<>();
        solids.addAll(walls(palette));
        solids.addAll(floor(palette));
        solids.addAll(roof(palette));
        solids.addAll(rafters(palette));
        if (size.chimney()) {
            solids.addAll(chimney(palette));
        }
        solids.addAll(door(palette));
        if (size.window() > 0) {
            solids.addAll(window(palette));
        }
        return solids;
    }

    /**
     * Its walls, round it, a piece between each two cuts: whole where it is wall; over its doorway, from its
     * door's head up; at its window, under its sill and over its head. Each up to where its roof's underside
     * meets it, its top sloping up from outside in.
     */
    private List<Solid> walls(final Palette palette) {
        final Material earth = palette.of(Role.WALL);
        final List<Solid> walls = new ArrayList<>();
        final List<Double> cuts = cuts();
        for (int k = 0; k < cuts.size(); k++) {
            final double a0 = cuts.get(k);
            final double a1 = k + 1 < cuts.size() ? cuts.get(k + 1) : cuts.getFirst() + 360;
            switch (bay((a0 + a1) / 2)) {
                case DOOR -> walls.add(wall("over the door " + k, earth, a0, a1, size.doorHigh()));
                case WINDOW -> {
                    walls.add(wall("under the window " + k, earth, a0, a1, 0, SILL));
                    walls.add(wall("over the window " + k, earth, a0, a1, windowTop()));
                }
                case WALL -> walls.add(wall("wall " + k, earth, a0, a1, 0));
            }
        }
        return walls;
    }

    /** A piece of wall from {@code a0} round to {@code a1}, from {@code from} up to its roof's underside. */
    private Solid wall(final String name, final Material earth, final double a0, final double a1, final double from) {
        return wall(name, earth, a0, a1, from, Double.NaN);
    }

    /** As {@link #wall(String, Material, double, double, double)}, up to {@code to} instead, if it is a number. */
    private Solid wall(final String name, final Material earth, final double a0, final double a1, final double from,
                       final double to) {
        final List<Point> corners = new ArrayList<>();
        for (int c = 0; c < 8; c++) {
            final double a = (c & 1) == 0 ? a0 : a1;
            final boolean out = (c & 2) != 0;
            final boolean high = (c & 4) != 0;
            final double r = out ? outsideAt(a, high) : inside();
            final double up = !high ? from : Double.isNaN(to) ? under(out ? outsideTop() : inside()) : to;
            corners.add(at(r, a, up));
        }
        return new Solid(name, Role.WALL, earth, corners);
    }

    /** Its floor of beaten earth, from its middle out to its walls, and through its doorway. */
    private List<Solid> floor(final Palette palette) {
        final List<Solid> floor = new ArrayList<>();
        final List<Double> cuts = cuts();
        for (int k = 0; k < cuts.size(); k++) {
            final double a0 = cuts.get(k);
            final double a1 = k + 1 < cuts.size() ? cuts.get(k + 1) : cuts.getFirst() + 360;
            final double reach = bay((a0 + a1) / 2) == Bay.DOOR ? outside() : inside();
            final List<Point> corners = new ArrayList<>();
            for (int c = 0; c < 8; c++) {
                final double a = (c & 1) == 0 ? a0 : a1;
                corners.add(at((c & 2) == 0 ? 0 : reach, a, (c & 4) == 0 ? 0 : FLOOR));
            }
            floor.add(new Solid("floor " + k, Role.BASE, palette.of(Role.BASE), corners));
        }
        return floor;
    }

    /**
     * Its roof: a cone of its covering, ring by ring from its eave to its point, as thick throughout, its
     * underside overhead inside; and on a roof of turf, tufts of grass growing out of it.
     */
    private List<Solid> roof(final Palette palette) {
        final Material covering = palette.of(Role.ROOF);
        final List<Solid> roof = new ArrayList<>();
        for (int i = 0; i < RINGS; i++) {
            final double r0 = ring(i);
            final double r1 = i + 1 < RINGS ? ring(i + 1) : 0;
            for (int k = 0; k < SIDES; k++) {
                final double a0 = 360.0 * k / SIDES;
                final double a1 = 360.0 * (k + 1) / SIDES;
                final List<Point> corners = new ArrayList<>();
                for (int c = 0; c < 8; c++) {
                    final double a = (c & 1) == 0 ? a0 : a1;
                    final double r = (c & 2) == 0 ? r0 : r1;
                    corners.add(at(r, a, (c & 4) == 0 ? under(r) : over(r)));
                }
                roof.add(new Solid("roof " + i + ", " + k, Role.ROOF, covering, corners));
            }
        }
        if (covering == Material.TURF) {
            roof.addAll(tufts());
        }
        return roof;
    }

    /** Tufts of grass on a roof of turf: scattered over it, as many as its size calls for, none on its chimney. */
    private List<Solid> tufts() {
        final List<Solid> tufts = new ArrayList<>();
        final int count = (int) Math.round(TUFTS * Math.PI * eave() * eave() / 10_000);
        for (int k = 0; k < count; k++) {
            final double a = 360 * Num.hash(k, 1);
            final double r = eave() * 0.92 * Math.sqrt(Num.hash(k, 2));
            if (size.chimney() && Math.abs(Num.wrap(a - CHIMNEY_AT)) < 20 && Math.abs(r - middle()) < 120) {
                continue;
            }
            final double wide = 6 + 7 * Num.hash(k, 3);
            final double tall = 14 + 26 * Num.hash(k, 4);
            final Point foot = at(r, a, over(r) - 4);
            final List<Point> corners = new ArrayList<>();
            for (int c = 0; c < 8; c++) {
                corners.add((c & 4) == 0
                        ? new Point(foot.across() + ((c & 1) == 0 ? -wide : wide), foot.along() + ((c & 2) == 0 ? -wide : wide),
                        foot.up())
                        : new Point(foot.across(), foot.along(), foot.up() + tall));
            }
            tufts.add(new Solid("roof, tuft " + k, Role.DRESSING, Material.GRASS, corners));
        }
        return tufts;
    }

    /**
     * The poles that carry its roof, from its middle out over its walls, their ends showing past its eave:
     * under its roof, none over its door or its window.
     */
    private List<Solid> rafters(final Palette palette) {
        final List<Solid> rafters = new ArrayList<>();
        final double from = eave() * 0.12;
        final double to = eave() + RAFTER_OUT;
        for (int k = 0; k < RAFTERS; k++) {
            final double a = 360.0 * (k + 0.5) / RAFTERS;
            final boolean clear = Math.abs(Num.wrap(a)) > half(size.door()) + 4
                    && (size.window() == 0 || Math.abs(Num.wrap(a - WINDOW_AT)) > half(size.window()) + 4);
            if (!clear) {
                continue;
            }
            final Point inner = at(from, a, under(from) - RAFTER / 2);
            final Point outer = at(to, a, under(eave()) - RAFTER / 2);
            rafters.add(beam("rafter " + k, Role.STRUCTURE, palette.of(Role.STRUCTURE), inner, outer, a, RAFTER, RAFTER));
        }
        return rafters;
    }

    /** A beam from {@code from} to {@code to}, heading {@code a} (its sides square to it), so wide and so deep. */
    private static Solid beam(final String name, final Role role, final Material material, final Point from, final Point to,
                              final double a, final double wide, final double deep) {
        final double t = Math.toRadians(a);
        final double sx = Math.cos(t) * wide / 2;
        final double sy = -Math.sin(t) * wide / 2;
        final List<Point> corners = new ArrayList<>();
        for (int c = 0; c < 8; c++) {
            final Point end = (c & 1) == 0 ? from : to;
            final double side = (c & 2) == 0 ? -1 : 1;
            final double up = (c & 4) == 0 ? -deep / 2 : deep / 2;
            corners.add(new Point(end.across() + side * sx, end.along() + side * sy, end.up() + up));
        }
        return new Solid(name, role, material, corners);
    }

    /**
     * A box in its frame turned to heading {@code a}: from {@code x0} to {@code x1} across it (to its right,
     * facing out that way), {@code y0} to {@code y1} out along it, {@code z0} to {@code z1} up.
     */
    private static Solid box(final String name, final Role role, final Material material, final double a, final double x0,
                             final double x1, final double y0, final double y1, final double z0, final double z1) {
        final double t = Math.toRadians(a);
        final List<Point> corners = new ArrayList<>();
        for (int c = 0; c < 8; c++) {
            final double x = (c & 1) == 0 ? x0 : x1;
            final double y = (c & 2) == 0 ? y0 : y1;
            corners.add(new Point(x * Math.cos(t) + y * Math.sin(t), -x * Math.sin(t) + y * Math.cos(t),
                    (c & 4) == 0 ? z0 : z1));
        }
        return new Solid(name, role, material, corners);
    }

    /** Its chimney: built up out of its back wall, its walls' earth, through its roof and over it; and a stovepipe through its roof. */
    private List<Solid> chimney(final Palette palette) {
        final double r = middle();
        final List<Solid> chimney = new ArrayList<>(List.of(box("chimney", Role.SPIRE, palette.of(Role.SPIRE), CHIMNEY_AT,
                -CHIMNEY, CHIMNEY, r - CHIMNEY, r + CHIMNEY, size.walls() - 40, over(r - CHIMNEY) + CHIMNEY_OVER)));
        final double pipe = eave() * 0.3;
        chimney.add(box("stovepipe", Role.DRESSING, Material.METAL, -40, -7, 7, pipe - 7, pipe + 7, under(pipe),
                over(pipe) + 45));
        return chimney;
    }

    /**
     * Its door: posts either side of its doorway through its walls, a lintel over it; and its leaf, of
     * planks on a frame ({@link Door}), hung at its right post and standing open inward.
     */
    private List<Solid> door(final Palette palette) {
        final Material wood = palette.of(Role.FRAME);
        final double w = size.door() / 2;
        final List<Solid> door = new ArrayList<>();
        for (final double side : new double[] {-1, 1}) {
            door.add(box("door, post " + (side < 0 ? "left" : "right"), Role.FRAME, wood, 0, side < 0 ? -w : w - POST,
                    side < 0 ? -w + POST : w, inside() - 2, outside() + 2, 0, size.doorHigh()));
        }
        door.add(box("door, lintel", Role.FRAME, wood, 0, -w - POST, w + POST, inside() - 2, outside() + 2,
                size.doorHigh(), size.doorHigh() + POST));
        // its leaf: hung at its right post, inside; turned about so it opens inward, away from the doorway
        final Door leaf = Door.single("house door", size.door() - 2 * POST - 4, size.doorHigh() - 4, LEAF, 120);
        final Point hinge = new Point(w - POST - 2, inside() - LEAF / 2 - 1, FLOOR);
        for (final Door.Member member : leaf.members(0)) {
            door.add(new Solid("door, leaf, " + member.name(), Role.FRAME, wood,
                    turnedIn(leaf.corners(0, member, LEAF, DOOR_OPEN), hinge)));
        }
        int panel = 0;
        for (final List<Point> infill : leaf.infills(0)) {
            door.add(new Solid("door, leaf, planks " + panel++, Role.FRAME, wood,
                    turnedIn(leaf.corners(0, infill, Door.INFILL * LEAF, DOOR_OPEN), hinge)));
        }
        return door;
    }

    /** A door's corners, in its own frame (its hinge at the origin, opening out, +along), turned about to open in, and hung at {@code hinge}. */
    private static List<Point> turnedIn(final List<Point> corners, final Point hinge) {
        return corners.stream().map(p -> new Point(hinge.across() - p.across(), hinge.along() - p.along(), hinge.up() + p.up()))
                .toList();
    }

    /**
     * Its window: a frame through its walls, its sill, its head and its posts; and filled as its palette
     * says: boards, a shutter, at its outside; or glass, at its middle.
     */
    private List<Solid> window(final Palette palette) {
        final Material wood = palette.of(Role.FRAME);
        final double w = size.window() / 2;
        final double top = windowTop();
        final List<Solid> window = new ArrayList<>();
        window.add(box("window, sill", Role.FRAME, wood, WINDOW_AT, -w - POST, w + POST, inside() - 4, outside() + 6, SILL - 5,
                SILL));
        window.add(box("window, head", Role.FRAME, wood, WINDOW_AT, -w - POST, w + POST, inside() - 2, outside() + 2, top,
                top + POST));
        for (final double side : new double[] {-1, 1}) {
            window.add(box("window, post " + (side < 0 ? "left" : "right"), Role.FRAME, wood, WINDOW_AT,
                    side < 0 ? -w : w - POST / 2, side < 0 ? -w + POST / 2 : w, inside() - 2, outside() + 2, SILL, top));
        }
        final Material fill = palette.of(Role.GLAZING);
        if (fill == Material.GLASS) {
            window.add(box("window, glass", Role.GLAZING, fill, WINDOW_AT, -w + POST / 2, w - POST / 2, middle() - 1, middle() + 1,
                    SILL, top));
        } else {
            final int boards = Math.max(2, (int) Math.round(size.window() / 18));
            final double each = (2 * w - POST) / boards;
            for (int b = 0; b < boards; b++) {
                final double x0 = -w + POST / 2 + b * each;
                window.add(box("window, shutter " + b, Role.GLAZING, fill, WINDOW_AT, x0 + 0.5, x0 + each - 0.5, outside(),
                        outside() + 3, SILL, top));
            }
        }
        return window;
    }

    // ---------------------------------------------------------------- in the way

    /**
     * It in the way as it is shaped, in its own frame (x across, z along): each piece of its walls stood
     * up (over its doorway, from its door's head; at its window, under its sill and over its head); its
     * floor; its roof, a ring for each of its rings, from its underside at its outer edge to its covering at
     * its inner; its chimney. Inside, open: walked into through its doorway, stood up in under its roof.
     */
    public List<Volume> shape() {
        final List<Volume> shape = new ArrayList<>();
        final List<Double> cuts = cuts();
        for (int k = 0; k < cuts.size(); k++) {
            final double a0 = cuts.get(k);
            final double a1 = k + 1 < cuts.size() ? cuts.get(k + 1) : cuts.getFirst() + 360;
            final List<Vec2> piece = List.of(flat(outside(), a0), flat(outside(), a1), flat(inside(), a1), flat(inside(), a0));
            final double top = under(inside());
            switch (bay((a0 + a1) / 2)) {
                case DOOR -> shape.add(new Prism(piece, size.doorHigh(), top));
                case WINDOW -> {
                    shape.add(new Prism(piece, 0, SILL));
                    shape.add(new Prism(piece, windowTop(), top));
                }
                case WALL -> shape.add(new Prism(piece, 0, top));
            }
        }
        shape.add(new Prism(circle(inside()), 0, FLOOR));
        for (int i = 0; i < RINGS; i++) {
            final double r0 = ring(i);
            final double r1 = i + 1 < RINGS ? ring(i + 1) : 0;
            shape.add(r1 > 0 ? new Prism(circle(r0), List.of(circle(r1)), under(r0), over(r1))
                    : new Prism(circle(r0), under(r0), over(0)));
        }
        if (size.chimney()) {
            final double r = middle();
            final List<Vec2> square = new ArrayList<>();
            for (final double[] c : new double[][] {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}}) {
                final double t = Math.toRadians(CHIMNEY_AT);
                final double x = c[0] * CHIMNEY;
                final double y = r + c[1] * CHIMNEY;
                square.add(new Vec2(x * Math.cos(t) + y * Math.sin(t), -x * Math.sin(t) + y * Math.cos(t)));
            }
            shape.add(new Prism(square, size.walls() - 40, over(r - CHIMNEY) + CHIMNEY_OVER));
        }
        return shape;
    }

    /** A point {@code r} out at heading {@code a}, on the ground (x across, z along). */
    private static Vec2 flat(final double r, final double a) {
        final Point p = at(r, a, 0);
        return new Vec2(p.across(), p.along());
    }

    /** A circle {@code r} round its middle, as an outline. */
    private static List<Vec2> circle(final double r) {
        final List<Vec2> circle = new ArrayList<>();
        for (int k = 0; k < SIDES; k++) {
            circle.add(flat(r, 360.0 * k / SIDES));
        }
        return circle;
    }
}
