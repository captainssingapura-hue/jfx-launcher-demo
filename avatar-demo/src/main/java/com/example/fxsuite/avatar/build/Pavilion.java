package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Crossing;
import com.example.fxsuite.avatar.sim.Curve;
import com.example.fxsuite.avatar.sim.Extrusion;
import com.example.fxsuite.avatar.sim.Form;
import com.example.fxsuite.avatar.sim.Prism;
import com.example.fxsuite.avatar.sim.Vec2;
import com.example.fxsuite.avatar.sim.Vec3;
import com.example.fxsuite.avatar.sim.Volume;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A pavilion: a building that is open, hollow but for what holds it up. A platform, its steps up at
 * its front; on it a ring of pillars; beams tying their heads; on those a roof. Each of those is a
 * part of its own, in the way as it is shaped, and between them is open space: walked into, stood
 * under, flown through.
 *
 * <p>Made to order ({@link Spec}): its footprint (square, a hexagon, an octagon, round, or a
 * rectangle of so many bays), how big; its pillars (round or square, how thick and how tall); its
 * roof (a pyramid to a point, round over a round one; a hip roof or a gable over a rectangle; or
 * flat), how steep and how far out over its pillars; how high its platform. What its roof is
 * tiled in and its pillars made of are choices, as other buildings' finishes are.
 *
 * <p>In centimetres, in its own frame: x across, y along (its front, with its steps, +y), up.
 */
public final class Pavilion {

    /** Its footprint: a regular polygon of so many sides (round: a circle on so many pillars), or a rectangle. */
    public enum Footprint {
        SQUARE(4), HEXAGON(6), OCTAGON(8), ROUND(8), RECTANGLE(4);

        private final int sides;

        Footprint(final int sides) {
            this.sides = sides;
        }

        public int sides() {
            return sides;
        }
    }

    /** Its pillars' section. */
    public enum Pillar { ROUND, SQUARE }

    /** Its roof: to a point (round over a round one), a hip roof or a gable over a rectangle, or flat. */
    public enum Roof { PYRAMID, HIP, GABLE, FLAT }

    /**
     * What it is to be: called {@code name}; its {@code footprint}, {@code width} across (a polygon's,
     * corner to opposite corner; a circle's, across) and {@code depth} (a rectangle's), of
     * {@code bays} along its front (a rectangle's); its pillars {@code pillar}, {@code thick} and
     * {@code tall}; its {@code roof}, rising {@code pitch} for each across (a pyramid's from its eaves'
     * middle), out {@code overhang} beyond its pillars; its platform {@code platform} high; its roof
     * tiled in {@code tiles}, its pillars of {@code columns}.
     */
    public record Spec(String name, Footprint footprint, double width, double depth, int bays, Pillar pillar,
                       double thick, double tall, Roof roof, double pitch, double overhang, double platform,
                       Material tiles, Material columns, double upturn) {

        /** Its eaves straight. */
        public Spec(final String name, final Footprint footprint, final double width, final double depth, final int bays,
                    final Pillar pillar, final double thick, final double tall, final Roof roof, final double pitch,
                    final double overhang, final double platform, final Material tiles, final Material columns) {
            this(name, footprint, width, depth, bays, pillar, thick, tall, roof, pitch, overhang, platform, tiles, columns, 0);
        }

        public Spec {
            if (upturn < 0 || upturn > 0 && (roof == Roof.GABLE || roof == Roof.FLAT)) {
                throw new IllegalArgumentException(name + ": eaves turned up over a roof to a point, or a hip roof");
            }
            final boolean rectangle = footprint == Footprint.RECTANGLE;
            if (!rectangle && (roof == Roof.HIP || roof == Roof.GABLE)) {
                throw new IllegalArgumentException(name + ": a hip roof or a gable is over a rectangle");
            }
            if (rectangle && roof == Roof.PYRAMID && Math.abs(width - depth) > 1e-6) {
                throw new IllegalArgumentException(name + ": a pyramid over a rectangle is over a square one");
            }
            if (width < 200 || (rectangle && (depth < 200 || bays < 1)) || thick < 10 || tall < 220 || pitch < 0
                    || overhang < 0 || platform < 0) {
                throw new IllegalArgumentException(name + ": a pavilion that cannot stand");
            }
            if (rectangle && bays % 2 == 0) {
                throw new IllegalArgumentException(name + ": an odd number of bays, its middle one its way in");
            }
            if (rectangle && roof == Roof.HIP && depth > width) {
                throw new IllegalArgumentException(name + ": a hip roof's ridge along its longer side");
            }
        }
    }

    /** How high a step is (each under a body's step), how far out it goes; how wide the steps are; the platform's margin round its pillars. */
    private static final double RISE = 16;
    private static final double GOING = 35;
    private static final double STEPS_WIDE = 160;
    private static final double MARGIN = 60;
    /** Its beams: how deep, how thick; its eaves: how far over its beams; its roof's tiles how thick; a flat roof's slab. */
    private static final double BEAM = 30;
    private static final double BEAM_THICK = 20;
    private static final double EAVES = 20;
    private static final double TILES = 15;
    private static final double SLAB = 30;
    /** How many points round a round pillar, and a round roof's eaves. */
    private static final int ROUND_PILLAR = 12;
    private static final int ROUND_ROOF = 24;

    private final Spec spec;

    private Pavilion(final Spec spec) {
        this.spec = spec;
    }

    /** The pavilion made to {@code spec}. */
    public static Pavilion of(final Spec spec) {
        return new Pavilion(spec);
    }

    public Spec spec() {
        return spec;
    }

    private boolean rectangle() {
        return spec.footprint() == Footprint.RECTANGLE;
    }

    /** Its pillars' middles, round it anticlockwise from its front right. */
    public List<Vec2> pillars() {
        final List<Vec2> at = new ArrayList<>();
        if (rectangle()) {
            final double w = spec.width() / 2;
            final double d = spec.depth() / 2;
            for (int k = 0; k <= spec.bays(); k++) {
                at.add(new Vec2(w - spec.width() * k / spec.bays(), d));
            }
            for (int k = 0; k <= spec.bays(); k++) {
                at.add(new Vec2(-w + spec.width() * k / spec.bays(), -d));
            }
            return at;
        }
        return polygon(spec.width() / 2, spec.footprint().sides());
    }

    /** A regular polygon of {@code sides}, its corners {@code radius} out, one side square to its front. */
    private static List<Vec2> polygon(final double radius, final int sides) {
        final List<Vec2> at = new ArrayList<>();
        for (int k = 0; k < sides; k++) {
            final double heading = Math.toRadians(180.0 / sides + 360.0 * k / sides);
            at.add(new Vec2(radius * Math.sin(heading), radius * Math.cos(heading)));
        }
        return at.reversed();
    }

    /** Its outline as far out as {@code beyond} past its pillars' line: its platform's, its eaves'. */
    private List<Vec2> outline(final double beyond) {
        if (rectangle()) {
            final double w = spec.width() / 2 + beyond;
            final double d = spec.depth() / 2 + beyond;
            return List.of(new Vec2(-w, -d), new Vec2(w, -d), new Vec2(w, d), new Vec2(-w, d));
        }
        final int sides = spec.footprint().sides();
        if (spec.footprint() == Footprint.ROUND) {
            return polygon(spec.width() / 2 + beyond, ROUND_ROOF);
        }
        // each side out by beyond: its corners out by beyond over the cosine of half the angle between them
        return polygon(spec.width() / 2 + beyond / Math.cos(Math.PI / sides), sides);
    }

    /** Its platform's top; its pillars' heads; its eaves; its roof's highest. */
    public double floor() {
        return spec.platform();
    }

    public double heads() {
        return spec.platform() + spec.tall();
    }

    public double eaves() {
        return heads() + EAVES;
    }

    /** How far in from its eaves its roof's point or ridge is: half its eaves' narrowest. */
    private double inward() {
        if (rectangle()) {
            return spec.depth() / 2 + spec.overhang();
        }
        final double apothem = spec.width() / 2 * Math.cos(Math.PI / spec.footprint().sides());
        return (spec.footprint() == Footprint.ROUND ? spec.width() / 2 : apothem) + spec.overhang();
    }

    public double ridge() {
        return switch (spec.roof()) {
            case FLAT -> eaves() + SLAB;
            default -> eaves() + spec.pitch() * inward();
        };
    }

    /** A hip roof's ridge, half its length: as much shorter than its eaves as its hips run in at 45°. */
    private double ridgeHalf() {
        return (spec.width() - spec.depth()) / 2;
    }

    // ---------------------------------------------------------------- its skeleton

    /** Its skeleton: its pillars up from its platform, the beams round their heads, its eaves, its roof to its point or ridge. */
    public Form skeleton() {
        final Form.Builder form = new Form.Builder(spec.name());
        final List<Vec2> pillars = pillars();
        final int[] feet = new int[pillars.size()];
        final int[] heads = new int[pillars.size()];
        for (int k = 0; k < pillars.size(); k++) {
            final Vec2 p = pillars.get(k);
            feet[k] = form.vertex("pillar " + k + ", foot", p.x(), p.z(), floor());
            heads[k] = form.vertex("pillar " + k + ", head", p.x(), p.z(), heads());
            form.edge(feet[k], heads[k]);
        }
        form.loop(heads);
        final List<Vec2> eaves = outline(spec.overhang());
        final List<Vec3> corners = corners();
        final int[] eave = new int[eaves.size()];
        for (int k = 0; k < eaves.size(); k++) {
            eave[k] = form.vertex("eave " + k, corners.get(k).x(), corners.get(k).y(), corners.get(k).z());
        }
        // its eaves, each a bow between its corners, turned up and out at them
        for (int k = 0; k < eave.length; k++) {
            final Vec3 a = corners.get(k);
            final Vec3 b = corners.get((k + 1) % corners.size());
            form.edge(eave[k], eave[(k + 1) % eave.length], Curve.bow(bow(a, b), SWEEP, a, b, 0, 1));
        }
        switch (spec.roof()) {
            case PYRAMID -> {
                final int point = form.vertex("point", 0, 0, ridge());
                for (final int e : eave) {
                    form.edge(e, point, hipBow());
                }
            }
            case HIP -> {
                final int west = form.vertex("ridge west", -ridgeHalf(), 0, ridge());
                final int east = form.vertex("ridge east", ridgeHalf(), 0, ridge());
                form.edge(west, east);
                for (int k = 0; k < eave.length; k++) {
                    form.edge(eave[k], eaves.get(k).x() < 0 ? west : east, hipBow());
                }
            }
            case GABLE -> {
                final double w = spec.width() / 2 + spec.overhang();
                final int west = form.vertex("ridge west", -w, 0, ridge());
                final int east = form.vertex("ridge east", w, 0, ridge());
                form.edge(west, east);
                for (int k = 0; k < eave.length; k++) {
                    form.edge(eave[k], eaves.get(k).x() < 0 ? west : east);
                }
            }
            case FLAT -> {
            }
        }
        // each pillar's head tied to the eaves: its bracket
        for (int k = 0; k < heads.length && k < eave.length && eaves.size() == pillars.size(); k++) {
            form.edge(heads[k], eave[k]);
        }
        return form.build();
    }

    // ---------------------------------------------------------------- dressed

    /** Its palette: red lacquered pillars, painted beams, its tiles, a stone platform. */
    public Palette palette() {
        return new Palette(spec.name(), Map.of(Role.STRUCTURE, spec.columns(), Role.FRAME, Material.PAINTED,
                Role.ROOF, spec.tiles(), Role.BASE, Material.STONE, Role.WALL, Material.TIMBER, Role.DRESSING,
                Material.PAINTED, Role.SPIRE, spec.tiles(), Role.GLAZING, Material.GLASS), 30, 30, 0);
    }

    /** The roofs' tiles to choose among, and the pillars' materials. */
    private static final List<Material> TILES_TO_CHOOSE = List.of(Material.GLAZED, Material.GREEN_GLAZED,
            Material.GREY_TILE, Material.TIMBER, Material.COPPER, Material.CONCRETE);
    private static final List<Material> PILLARS_TO_CHOOSE = List.of(Material.LACQUER, Material.TIMBER,
            Material.STONE, Material.CONCRETE, Material.METAL);

    /**
     * It as a building to dress ({@link Building}): its skeleton; its roof's faces; its platform and
     * steps, its pillars, its beams, the finial on its point, as members. Its roof's tiles a choice,
     * and its pillars' material, its own first.
     */
    public Building building() {
        final Map<String, Design> faces = Map.of(
                "roof", Design.slab(Role.ROOF, 0, TILES),
                "gable", Design.slab(Role.WALL, -4, 4));
        final Map<String, List<Building.Option>> choices = new LinkedHashMap<>();
        choices.put("Roof", first(spec.tiles(), TILES_TO_CHOOSE).stream()
                .map(m -> Building.Option.choosing(capitalised(m.title()), faces, Map.of(Role.ROOF, m, Role.SPIRE, m)))
                .toList());
        choices.put("Pillars", first(spec.columns(), PILLARS_TO_CHOOSE).stream()
                .map(m -> Building.Option.choosing(capitalised(m.title()), Map.of(), Map.of(Role.STRUCTURE, m)))
                .toList());
        final Building.Structure base = (skeleton, palette) -> base(palette);
        final Building.Structure columns = (skeleton, palette) -> columns(palette);
        final Building.Structure beams = (skeleton, palette) -> beams(palette);
        final Building.Structure flat = (skeleton, palette) -> spec.roof() == Roof.FLAT ? flatRoof(palette) : List.of();
        return new Building(spec.name(), skeleton(), List.of(), List.of(), sheets(), List.of(base, columns, beams, flat),
                choices, List.of(palette()));
    }

    private static List<Material> first(final Material own, final List<Material> all) {
        final List<Material> ordered = new ArrayList<>(List.of(own));
        all.stream().filter(m -> m != own).forEach(ordered::add);
        return ordered;
    }

    private static String capitalised(final String text) {
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    /** Its roof's faces: to its point, its hip roof's, its gable's slopes and their ends. A flat roof is a slab, not faces. */
    public List<Sheet> sheets() {
        final List<Sheet> sheets = new ArrayList<>();
        final List<Vec2> eaves = outline(spec.overhang());
        final double e = eaves();
        final List<Vec3> corner = corners();
        final Vec3 inside = new Vec3(0, 0, e - 100);
        switch (spec.roof()) {
            case PYRAMID -> {
                // each face filled in between its eave, swept up at its corners, and its two hips to the point
                final Vec3 point = new Vec3(0, 0, ridge());
                for (int k = 0; k < corner.size(); k++) {
                    final Vec3 a = corner.get(k);
                    final Vec3 b = corner.get((k + 1) % corner.size());
                    sheets.add(Sheet.patch("roof, face " + k, "roof", eave(a, b), hip(b, point), List.of(point),
                            hip(a, point), 12, 8, inside));
                }
            }
            case HIP -> {
                // as the temple hall's: its long slopes between their eaves, hips and ridge; its ends up to the ridge's
                final Vec3 west = new Vec3(-ridgeHalf(), 0, ridge());
                final Vec3 east = new Vec3(ridgeHalf(), 0, ridge());
                sheets.add(Sheet.patch("roof, front", "roof", eave(corner.get(3), corner.get(2)), hip(corner.get(2), east),
                        List.of(west, east), hip(corner.get(3), west), 24, 8, inside));
                sheets.add(Sheet.patch("roof, back", "roof", eave(corner.get(0), corner.get(1)), hip(corner.get(1), east),
                        List.of(west, east), hip(corner.get(0), west), 24, 8, inside));
                sheets.add(Sheet.patch("roof, east end", "roof", eave(corner.get(1), corner.get(2)), hip(corner.get(2), east),
                        List.of(east), hip(corner.get(1), east), 12, 8, inside));
                sheets.add(Sheet.patch("roof, west end", "roof", eave(corner.get(0), corner.get(3)), hip(corner.get(3), west),
                        List.of(west), hip(corner.get(0), west), 12, 8, inside));
            }
            case GABLE -> {
                final double w = spec.width() / 2 + spec.overhang();
                final double d = spec.depth() / 2 + spec.overhang();
                final Vec3 west = new Vec3(-w, 0, ridge());
                final Vec3 east = new Vec3(w, 0, ridge());
                sheets.add(face("roof, front", "roof", List.of(new Vec3(w, d, e), new Vec3(-w, d, e), west, east)));
                sheets.add(face("roof, back", "roof", List.of(new Vec3(-w, -d, e), new Vec3(w, -d, e), east, west)));
                // its gables, boarded, over its end beams
                final double g = spec.width() / 2;
                final double gd = spec.depth() / 2;
                final double low = heads();
                final double high = ridge() - spec.pitch() * spec.overhang();
                sheets.add(Sheet.flat("gable, east", "gable", List.of(new Vec3(g, -gd, low), new Vec3(g, gd, low),
                        new Vec3(g, 0, high)), List.of(), new Vec3(1, 0, 0)));
                sheets.add(Sheet.flat("gable, west", "gable", List.of(new Vec3(-g, gd, low), new Vec3(-g, -gd, low),
                        new Vec3(-g, 0, high)), List.of(), new Vec3(-1, 0, 0)));
            }
            case FLAT -> {
            }
        }
        return sheets;
    }

    /** How sharply its eaves sweep up at their corners: as the temple hall's. */
    private static final double SWEEP = 3;

    /** How far out its eaves' corners turn, as well as up: five sevenths as far. */
    private double flare() {
        return spec.upturn() * 5 / 7;
    }

    /**
     * Its eaves' corners: round its eaves' outline, each turned up by its upturn and out by its flare;
     * round a round roof, every point of its rim so, its rafters sagging, a flare all round.
     */
    public List<Vec3> corners() {
        final List<Vec3> corners = new ArrayList<>();
        for (final Vec2 p : outline(spec.overhang())) {
            final double r = Math.hypot(p.x(), p.z());
            final double out = (r + flare()) / r;
            corners.add(new Vec3(p.x() * out, p.z() * out, eaves() + spec.upturn()));
        }
        return corners;
    }

    /**
     * The bow of its eave from corner {@code a} to corner {@code b}: down by its upturn and in by its
     * flare at its middle, so the middle is where a straight eave is and its ends turn up and out. A
     * round roof's rim does not bow between its points: it is turned up all round.
     */
    private Vec3 bow(final Vec3 a, final Vec3 b) {
        if (spec.footprint() == Footprint.ROUND) {
            return Vec3.ZERO;
        }
        final double mx = (a.x() + b.x()) / 2;
        final double my = (a.y() + b.y()) / 2;
        final double length = Math.hypot(mx, my);
        return new Vec3(-mx / length * flare(), -my / length * flare(), -spec.upturn());
    }

    /** A hip, from an eave's corner up to the point or a ridge's end, sagging as far as its eaves turn up. */
    private Curve hipBow() {
        return Curve.bow(new Vec3(0, 0, -spec.upturn()), 2);
    }

    /** Its eave from corner {@code a} to corner {@code b}, as points along its bow. */
    private List<Vec3> eave(final Vec3 a, final Vec3 b) {
        final Vec3 bow = bow(a, b);
        final List<Vec3> points = new ArrayList<>();
        for (int k = 0; k <= 16; k++) {
            points.add(Curve.bowAt(bow, SWEEP, a, b, k / 16.0));
        }
        return points;
    }

    /** A hip from {@code corner} up to {@code end}, as points along its sag. */
    private List<Vec3> hip(final Vec3 corner, final Vec3 end) {
        final Curve sag = hipBow();
        final List<Vec3> points = new ArrayList<>();
        for (int k = 0; k <= 12; k++) {
            points.add(sag.at(corner, end, k / 12.0));
        }
        return points;
    }

    /** A flat face through {@code points}, its outside up and out. */
    private static Sheet face(final String name, final String kind, final List<Vec3> points) {
        final Vec3 a = points.get(1).subtract(points.get(0));
        final Vec3 b = points.get(2).subtract(points.get(0));
        Vec3 out = new Vec3(a.y() * b.z() - a.z() * b.y(), a.z() * b.x() - a.x() * b.z(), a.x() * b.y() - a.y() * b.x());
        if (out.z() < 0) {
            out = out.multiply(-1);
        }
        return Sheet.flat(name, kind, points, List.of(), out);
    }

    /** Its platform, and its steps down at its front. */
    private List<Solid> base(final Palette palette) {
        final List<Solid> solids = new ArrayList<>(stood("platform", Role.BASE, palette.of(Role.BASE), outline(MARGIN), 0,
                floor()));
        for (final Prism step : steps()) {
            solids.addAll(stood("steps", Role.BASE, palette.of(Role.BASE), step.outline(), step.bottom(), step.top()));
        }
        return solids;
    }

    /** Its steps, down from its platform's front edge, each a rise under a body's step. */
    private List<Prism> steps() {
        final List<Prism> steps = new ArrayList<>();
        final int count = (int) Math.ceil(floor() / RISE);
        if (count < 2) {
            return steps;
        }
        final double front = edge();
        final double half = STEPS_WIDE / 2;
        for (int k = 1; k < count; k++) {
            final double top = floor() * (count - k) / count;
            final double near = front + GOING * (k - 1);
            steps.add(new Prism(List.of(new Vec2(-half, near - 1), new Vec2(half, near - 1), new Vec2(half, near + GOING),
                    new Vec2(-half, near + GOING)), 0, top));
        }
        return steps;
    }

    /** How far forward its platform's front edge is. */
    private double edge() {
        return outline(MARGIN).stream().mapToDouble(Vec2::z).max().orElseThrow()
                - (spec.footprint() == Footprint.ROUND ? 10 : 0);
    }

    /** Its pillars, each its section stood up from its platform to its head. */
    private List<Solid> columns(final Palette palette) {
        final List<Solid> solids = new ArrayList<>();
        int k = 0;
        for (final Vec2 p : pillars()) {
            solids.addAll(stood("pillar " + k++, Role.STRUCTURE, palette.of(Role.STRUCTURE), section(p), floor(), heads()));
        }
        return solids;
    }

    /** A pillar's section about {@code p}: round, or square. */
    private List<Vec2> section(final Vec2 p) {
        final double r = spec.thick() / 2;
        if (spec.pillar() == Pillar.SQUARE) {
            return List.of(new Vec2(p.x() - r, p.z() - r), new Vec2(p.x() + r, p.z() - r), new Vec2(p.x() + r, p.z() + r),
                    new Vec2(p.x() - r, p.z() + r));
        }
        final List<Vec2> round = new ArrayList<>();
        for (int k = 0; k < ROUND_PILLAR; k++) {
            final double a = 2 * Math.PI * k / ROUND_PILLAR;
            round.add(new Vec2(p.x() + r * Math.cos(a), p.z() + r * Math.sin(a)));
        }
        return round;
    }

    /** Its beams, from each pillar's head to the next's, just under it. */
    private List<Solid> beams(final Palette palette) {
        final List<Solid> solids = new ArrayList<>();
        final List<Vec2> pillars = pillars();
        for (int k = 0; k < pillars.size(); k++) {
            solids.addAll(stood("beam " + k, Role.FRAME, palette.of(Role.FRAME),
                    beam(pillars.get(k), pillars.get((k + 1) % pillars.size())), heads() - BEAM, heads()));
        }
        return solids;
    }

    /** A beam's footprint, from {@code a} to {@code b}. */
    private static List<Vec2> beam(final Vec2 a, final Vec2 b) {
        final double dx = b.x() - a.x();
        final double dz = b.z() - a.z();
        final double length = Math.hypot(dx, dz);
        final double nx = -dz / length * BEAM_THICK / 2;
        final double nz = dx / length * BEAM_THICK / 2;
        return List.of(new Vec2(a.x() - nx, a.z() - nz), new Vec2(b.x() - nx, b.z() - nz), new Vec2(b.x() + nx, b.z() + nz),
                new Vec2(a.x() + nx, a.z() + nz));
    }

    /** A flat roof: a slab over its eaves' outline. */
    private List<Solid> flatRoof(final Palette palette) {
        return stood("roof, slab", Role.ROOF, palette.of(Role.ROOF), outline(spec.overhang()), eaves(), ridge());
    }

    /**
     * A convex outline stood up from {@code low} to {@code high}, as solids: a wedge from its middle
     * out to each of its sides, each a box two of whose corners are one.
     */
    private static List<Solid> stood(final String name, final Role role, final Material material, final List<Vec2> outline,
                                     final double low, final double high) {
        final double mx = outline.stream().mapToDouble(Vec2::x).average().orElseThrow();
        final double mz = outline.stream().mapToDouble(Vec2::z).average().orElseThrow();
        final List<Solid> solids = new ArrayList<>();
        for (int k = 0; k < outline.size(); k++) {
            final Vec2 a = outline.get(k);
            final Vec2 b = outline.get((k + 1) % outline.size());
            final List<Point> corners = new ArrayList<>();
            for (int c = 0; c < 8; c++) {
                final double up = (c & 4) == 0 ? low : high;
                final Vec2 at = (c & 2) == 0 ? new Vec2(mx, mz) : (c & 1) == 0 ? a : b;
                corners.add(new Point(at.x(), at.z(), up));
            }
            solids.add(new Solid(name + ", " + k, role, material, corners));
        }
        return solids;
    }

    // ---------------------------------------------------------------- in the way

    /**
     * It in the way as it is shaped, in its own frame (x across, z along): its platform and steps;
     * each pillar its section stood up; each beam; its roof, from its eaves up (a pyramid the part so
     * many triangles drawn across it share; a hip roof two gables crossed; a gable one drawn along
     * it; a flat roof its slab). Between them, open: walked into, stood under, its roof's underside
     * overhead.
     */
    public List<Volume> shape() {
        final List<Volume> shape = new ArrayList<>();
        if (floor() > 0) {
            shape.add(new Prism(outline(MARGIN), 0, floor()));
        }
        shape.addAll(steps());
        for (final Vec2 p : pillars()) {
            shape.add(new Prism(section(p), floor(), heads()));
        }
        final List<Vec2> pillars = pillars();
        for (int k = 0; k < pillars.size(); k++) {
            shape.add(new Prism(beam(pillars.get(k), pillars.get((k + 1) % pillars.size())), heads() - BEAM, heads()));
        }
        final double e = eaves();
        final double in = inward();
        switch (spec.roof()) {
            case PYRAMID -> {
                // a triangle across each pair of its opposite sides, drawn along them: all of them share a pyramid
                final int sides = spec.footprint() == Footprint.ROUND ? 8 : spec.footprint().sides();
                final List<Vec2> triangle = List.of(new Vec2(-in, e), new Vec2(in, e), new Vec2(0, ridge()));
                final List<Extrusion> across = new ArrayList<>();
                for (int k = 0; k < sides / 2; k++) {
                    // drawn along its sides: square to the way the pair of them faces
                    across.add(new Extrusion(0, 0, 90 + 360.0 * k / sides, in * 2, triangle));
                }
                shape.add(new Crossing(across));
            }
            case HIP -> {
                final double w = spec.width() / 2 + spec.overhang();
                shape.add(new Crossing(
                        new Extrusion(0, 0, 90, w, List.of(new Vec2(-in, e), new Vec2(in, e), new Vec2(0, ridge()))),
                        new Extrusion(0, 0, 0, in, List.of(new Vec2(-w, e), new Vec2(w, e), new Vec2(ridgeHalf(), ridge()),
                                new Vec2(-ridgeHalf(), ridge())))));
            }
            case GABLE -> shape.add(new Extrusion(0, 0, 90, spec.width() / 2 + spec.overhang(),
                    List.of(new Vec2(-in, e), new Vec2(in, e), new Vec2(0, ridge()))));
            case FLAT -> shape.add(new Prism(outline(spec.overhang()), e, ridge()));
        }
        return shape;
    }
}
