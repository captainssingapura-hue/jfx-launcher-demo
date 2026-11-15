package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Block;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Curve;
import com.example.fxsuite.avatar.sim.Num;
import com.example.fxsuite.avatar.sim.Place;
import com.example.fxsuite.avatar.sim.Prism;
import com.example.fxsuite.avatar.sim.Vec2;
import com.example.fxsuite.avatar.sim.Volume;
import com.example.fxsuite.avatar.sim.Extrusion;
import com.example.fxsuite.avatar.sim.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A wall: a list of units, each from one point to another across the ground, straight or curved
 * ({@link Curve}), all built alike ({@link Style}): a stone plinth, the wall on it, and a tiled
 * coping over it, sloping down each side from a ridge along its middle. In the world each unit
 * stands on its own ({@link Course.Kind#WALL_UNIT}): struck, it is that unit that wears down and
 * breaks, the rest standing.
 *
 * <p>Where units meet, end to end, they are cut to fit, whatever the angle and however many meet
 * there: each unit's end runs from where its left face meets the face of the next unit round to
 * its left, in to the point they meet at (an inner vertex), and out to where its right face meets
 * the face of the next round to its right. So two units in a line run on as one; two at a corner
 * are mitred; three or four at a junction each take a slice of it. A free end is cut square.
 * Gaps (a gateway, a door) are where there is no unit.
 *
 * <p>In centimetres; the units' points across and along the ground (its x and z), up 0.
 */
public final class Wall {

    /**
     * How every unit of a wall is built: {@code height} to the top of the wall, under its coping;
     * {@code thickness} through; a plinth {@code plinth} high standing out {@code projection} each
     * side; a coping out {@code overhang} each side, {@code edge} thick at its edges and {@code rise}
     * over the wall's top at its ridge.
     */
    public record Style(double height, double thickness, double plinth, double projection, double overhang,
                        double rise, double edge) {

        /** A temple's: 3.6 m high, 0.9 m through, on a 0.6 m stone plinth, its coping out 25 cm and up 35 cm. */
        public static final Style TEMPLE = new Style(360, 90, 60, 8, 25, 35, 10);

        public Style {
            if (thickness <= 0 || plinth < 0 || height <= plinth || overhang < 0 || projection < 0 || rise < edge
                    || edge <= 0) {
                throw new IllegalArgumentException("a wall that cannot stand: " + height + " high, " + thickness
                        + " through, its plinth " + plinth + ", its coping " + edge + " to " + rise);
            }
        }

        /** How high it stands, to the ridge of its coping. */
        public double top() {
            return height + rise;
        }
    }

    /** A unit: from one point to another, straight if {@code curve} is null, or curved as it says. */
    public record Unit(Point from, Point to, Curve curve) {

        public Unit {
            if (Math.hypot(to.across() - from.across(), to.along() - from.along()) < 1) {
                throw new IllegalArgumentException("a unit from a point to itself: " + from);
            }
        }

        /** Straight, from one point to the other. */
        public Unit(final Point from, final Point to) {
            this(from, to, null);
        }

        /** The point {@code s} of the way along it, 0 at its start, 1 at its end. */
        public Point at(final double s) {
            if (curve == null) {
                return from.plus(to.plus(from.times(-1)).times(s));
            }
            final Vec3 p = curve.at(vec(from), vec(to), s);
            return new Point(p.x(), p.y(), 0);
        }

        /** How long it runs, along its curve. */
        public double length() {
            if (curve == null) {
                return flat(to.plus(from.times(-1)));
            }
            double length = 0;
            Point last = from;
            for (int k = 1; k <= 64; k++) {
                final Point next = at(k / 64.0);
                length += flat(next.plus(last.times(-1)));
                last = next;
            }
            return length;
        }
    }

    /** How far apart, at most, the points a curved unit is drawn through are. */
    private static final double FINE = 50;
    /** How long, at most, each of the boxes a curved unit is in the way as is. */
    private static final double CHORD = 150;
    /** Beyond this many half-thicknesses out, a corner too sharp to cut is cut square instead. */
    private static final double SPIKE = 4;

    private final String name;
    private final Style style;
    private final List<Unit> units;
    /** Which units' ends meet at each point, by that point (to the centimetre). */
    private final Map<String, List<End>> joints = new HashMap<>();

    /** One end of a unit: which, which end, where, and the way the unit runs from it, across the ground. */
    private record End(int unit, boolean start, Point at, double dx, double dz) {

        double angle() {
            return Math.atan2(dz, dx);
        }
    }

    public Wall(final String name, final Style style, final List<Unit> units) {
        this.name = name;
        this.style = style;
        this.units = List.copyOf(units);
        for (int u = 0; u < this.units.size(); u++) {
            final Unit unit = this.units.get(u);
            for (final boolean start : new boolean[] {true, false}) {
                final Point at = start ? unit.from() : unit.to();
                final Point near = unit.at(start ? 1e-3 : 1 - 1e-3);
                final double dx = near.across() - at.across();
                final double dz = near.along() - at.along();
                final double length = Math.hypot(dx, dz);
                joints.computeIfAbsent(key(at), k -> new ArrayList<>()).add(new End(u, start, at, dx / length, dz / length));
            }
        }
        joints.values().forEach(ends -> ends.sort(Comparator.comparingDouble(End::angle)));
    }

    public String name() {
        return name;
    }

    public Style style() {
        return style;
    }

    public List<Unit> units() {
        return units;
    }

    /** How many units' ends meet where unit {@code u}'s start (or end) is: 1 for a free end. */
    public int meeting(final int u, final boolean start) {
        final Unit unit = units.get(u);
        return joints.get(key(start ? unit.from() : unit.to())).size();
    }

    /** Straight units, from {@code a} to {@code b}, as few as can be none longer than {@code longest}, all as long. */
    public static List<Unit> run(final Point a, final Point b, final double longest) {
        final int count = Math.max(1, (int) Math.ceil(flat(b.plus(a.times(-1))) / longest));
        final List<Unit> run = new ArrayList<>();
        for (int k = 0; k < count; k++) {
            run.add(new Unit(a.plus(b.plus(a.times(-1)).times((double) k / count)),
                    a.plus(b.plus(a.times(-1)).times((double) (k + 1) / count))));
        }
        return run;
    }

    /**
     * Curved units round a circle about {@code centre}, {@code radius} out, from heading {@code from}
     * to heading {@code to} (degrees, 0 along, 90 across), as few as can be none longer than
     * {@code longest}: each a circular arc.
     */
    public static List<Unit> arc(final Point centre, final double radius, final double from, final double to,
                                 final double longest) {
        final double sweep = Math.toRadians(Math.abs(to - from)) * radius;
        final int count = Math.max(1, (int) Math.ceil(sweep / longest));
        final List<Unit> arc = new ArrayList<>();
        for (int k = 0; k < count; k++) {
            final double a = from + (to - from) * k / count;
            final double b = from + (to - from) * (k + 1) / count;
            final Point pa = round(centre, radius, a);
            final Point pb = round(centre, radius, b);
            final Point middle = round(centre, radius, (a + b) / 2);
            final Point chord = pa.plus(pb).times(0.5);
            arc.add(new Unit(pa, pb, Curve.arc(vec(middle.plus(chord.times(-1))))));
        }
        return arc;
    }

    private static Point round(final Point centre, final double radius, final double heading) {
        final double h = Math.toRadians(heading);
        return new Point(centre.across() + radius * Math.sin(h), centre.along() + radius * Math.cos(h), 0);
    }

    /**
     * Unit {@code u}'s footprint at {@code half} each side of its line: at each point along it, its
     * left side, its line, its right side (seen going from its start to its end); at its ends, cut
     * to fit the units it meets there.
     */
    public List<Point[]> footprint(final int u, final double half) {
        final Unit unit = units.get(u);
        final int count = unit.curve() == null ? 1 : Math.max(2, (int) Math.ceil(unit.length() / FINE));
        final List<Point[]> across = new ArrayList<>();
        for (int k = 0; k <= count; k++) {
            final Point at = unit.at((double) k / count);
            if (k == 0) {
                final Point[] sides = sides(end(u, true), half);
                across.add(new Point[] {sides[0], at, sides[1]});
            } else if (k == count) {
                // seen from its end, its left is the unit's right
                final Point[] sides = sides(end(u, false), half);
                across.add(new Point[] {sides[1], at, sides[0]});
            } else {
                final Point before = unit.at((k - 1.0) / count);
                final Point after = unit.at((k + 1.0) / count);
                final double dx = after.across() - before.across();
                final double dz = after.along() - before.along();
                final double length = Math.hypot(dx, dz);
                final Point left = new Point(-dz / length * half, dx / length * half, 0);
                across.add(new Point[] {at.plus(left), at, at.plus(left.times(-1))});
            }
        }
        return across;
    }

    private End end(final int u, final boolean start) {
        final Unit unit = units.get(u);
        return joints.get(key(start ? unit.from() : unit.to())).stream()
                .filter(e -> e.unit() == u && e.start() == start).findFirst().orElseThrow();
    }

    /**
     * Where an end's left and right faces stop, {@code half} out from its line, seen going away from
     * the point it is at: square across, at a free end; where they meet the faces of the units next
     * round each way, where it meets others.
     */
    private Point[] sides(final End end, final double half) {
        final List<End> here = joints.get(key(end.at()));
        final Point left = new Point(-end.dz() * half, end.dx() * half, 0);
        if (here.size() == 1) {
            return new Point[] {end.at().plus(left), end.at().plus(left.times(-1))};
        }
        final int k = here.indexOf(end);
        final End next = here.get((k + 1) % here.size());
        final End before = here.get((k - 1 + here.size()) % here.size());
        // its left face against the next one round's right; its right against the one before's left
        return new Point[] {meet(end, 1, next, -1, half), meet(end, -1, before, 1, half)};
    }

    /** Where {@code a}'s face on its {@code sideA} (1 left, -1 right) meets {@code b}'s on its {@code sideB}. */
    private static Point meet(final End a, final int sideA, final End b, final int sideB, final double half) {
        final double ax = -a.dz() * half * sideA;
        final double az = a.dx() * half * sideA;
        final double bx = -b.dz() * half * sideB;
        final double bz = b.dx() * half * sideB;
        // a's face: at + (ax, az) + t (a.dx, a.dz); b's: at + (bx, bz) + s (b.dx, b.dz)
        final double det = -a.dx() * b.dz() + b.dx() * a.dz();
        if (Math.abs(det) < 1e-9) {
            return a.at().plus(new Point(ax, az, 0));
        }
        final double rx = bx - ax;
        final double rz = bz - az;
        final double t = (-rx * b.dz() + b.dx() * rz) / det;
        if (Math.abs(t) > SPIKE * half) {
            return a.at().plus(new Point(ax, az, 0));
        }
        return a.at().plus(new Point(ax + t * a.dx(), az + t * a.dz(), 0));
    }

    /**
     * Unit {@code u}, dressed in {@code palette}: its plinth (its base), its wall, its coping (its
     * roof), each a solid either side of its line between each two of its points along it.
     */
    public List<Solid> solids(final int u, final Palette palette) {
        final List<Solid> solids = new ArrayList<>();
        final double half = style.thickness() / 2;
        layer(solids, u, "plinth", Role.BASE, palette, half + style.projection(), 0, style.plinth(), style.plinth());
        layer(solids, u, "wall", Role.WALL, palette, half, style.plinth(), style.height(), style.height());
        layer(solids, u, "coping", Role.ROOF, palette, half + style.overhang(), style.height(),
                style.height() + style.edge(), style.height() + style.rise());
        return solids;
    }

    /**
     * A layer of unit {@code u}, {@code half} each side of its line, from {@code low} up to
     * {@code edge} at its sides and {@code ridge} along its line.
     */
    private void layer(final List<Solid> solids, final int u, final String part, final Role role, final Palette palette,
                       final double half, final double low, final double edge, final double ridge) {
        final List<Point[]> across = footprint(u, half);
        final Material material = palette.of(role);
        for (int k = 0; k + 1 < across.size(); k++) {
            final Point[] a = across.get(k);
            final Point[] b = across.get(k + 1);
            // its left half, from its left side in to its line; its right, from its line out to its right side
            solids.add(hexahedron(name + " " + u + ", " + part + " " + k + " left", role, material, a[0], b[0], a[1], b[1],
                    low, edge, ridge));
            solids.add(hexahedron(name + " " + u + ", " + part + " " + k + " right", role, material, a[1], b[1], a[2], b[2],
                    low, ridge, edge));
        }
    }

    /**
     * A solid between two lines across the ground, {@code p0} to {@code p1} and {@code q0} to
     * {@code q1}, from {@code low} up to {@code high0} over the first and {@code high1} over the second.
     */
    private static Solid hexahedron(final String name, final Role role, final Material material, final Point p0,
                                    final Point p1, final Point q0, final Point q1, final double low,
                                    final double high0, final double high1) {
        return new Solid(name, role, material, List.of(
                new Point(p0.across(), p0.along(), low), new Point(p1.across(), p1.along(), low),
                new Point(q0.across(), q0.along(), low), new Point(q1.across(), q1.along(), low),
                new Point(p0.across(), p0.along(), high0), new Point(p1.across(), p1.along(), high0),
                new Point(q0.across(), q0.along(), high1), new Point(q1.across(), q1.along(), high1)));
    }

    /**
     * Unit {@code u} in the way, as boxes: straight, one box along it as thick as its plinth, drawn on
     * to the point it meets others at so its corners close; curved, a box along each short chord of
     * it. As high as its coping's ridge.
     */
    public List<Block> blocks(final int u) {
        final Unit unit = units.get(u);
        final double half = style.thickness() / 2 + style.projection();
        final List<Block> blocks = new ArrayList<>();
        final int count = unit.curve() == null ? 1 : Math.max(2, (int) Math.ceil(unit.length() / CHORD));
        for (int k = 0; k < count; k++) {
            Point a = unit.at((double) k / count);
            Point b = unit.at((k + 1.0) / count);
            final double length = flat(b.plus(a.times(-1)));
            final Point along = b.plus(a.times(-1)).times(1 / length);
            // on past the ends it meets others at, by its half thickness; and a little over each other, curved
            final double startBy = k == 0 ? (meeting(u, true) > 1 ? half : 0) : 5;
            final double endBy = k == count - 1 ? (meeting(u, false) > 1 ? half : 0) : 5;
            a = a.plus(along.times(-startBy));
            b = b.plus(along.times(endBy));
            final Point middle = a.plus(b).times(0.5);
            blocks.add(new Block(middle.across(), middle.along(), half, (length + startBy + endBy) / 2,
                    Math.toDegrees(Math.atan2(along.across(), along.along())), 0, style.top()));
        }
        return blocks;
    }

    /**
     * Unit {@code u} in the way as it is shaped: its plinth and its wall each its footprint stood up
     * (cut to fit where it meets others, as it is drawn); its coping, on a straight unit, its section
     * drawn along it (sloping to its ridge, on past its ends where it meets others, so its corners
     * close); on a curved one, its footprint stood up as high as its ridge.
     */
    public List<Volume> volumes(final int u) {
        final double half = style.thickness() / 2;
        final List<Volume> volumes = new ArrayList<>();
        volumes.add(new Prism(outline(u, half + style.projection()), 0, style.plinth()));
        volumes.add(new Prism(outline(u, half), style.plinth(), style.height()));
        final Unit unit = units.get(u);
        final double cope = half + style.overhang();
        if (unit.curve() == null) {
            final double length = unit.length();
            final double startBy = meeting(u, true) > 1 ? cope : 0;
            final double endBy = meeting(u, false) > 1 ? cope : 0;
            final Point along = unit.to().plus(unit.from().times(-1)).times(1 / length);
            final Point middle = unit.from().plus(unit.to()).times(0.5).plus(along.times((endBy - startBy) / 2));
            final double h = style.height();
            volumes.add(new Extrusion(middle.across(), middle.along(),
                    Math.toDegrees(Math.atan2(along.across(), along.along())), (length + startBy + endBy) / 2,
                    List.of(new Vec2(-cope, h), new Vec2(cope, h), new Vec2(cope, h + style.edge()),
                            new Vec2(0, style.top()), new Vec2(-cope, h + style.edge()))));
        } else {
            volumes.add(new Prism(outline(u, cope), style.height(), style.top()));
        }
        return volumes;
    }

    /**
     * Unit {@code u}'s footprint at {@code half} each side of its line, as an outline: up its left
     * side, round its end (in to the point it meets others at, if it does), back down its right side,
     * round its start.
     */
    private List<Vec2> outline(final int u, final double half) {
        final List<Point[]> across = footprint(u, half);
        final List<Vec2> outline = new ArrayList<>();
        across.forEach(a -> outline.add(new Vec2(a[0].across(), a[0].along())));
        if (meeting(u, false) > 1) {
            final Point end = across.getLast()[1];
            outline.add(new Vec2(end.across(), end.along()));
        }
        across.reversed().forEach(a -> outline.add(new Vec2(a[2].across(), a[2].along())));
        if (meeting(u, true) > 1) {
            final Point start = across.getFirst()[1];
            outline.add(new Vec2(start.across(), start.along()));
        }
        return outline;
    }

    /**
     * Unit {@code u} as a piece in the world, drawn by {@code key}: its box along it from its start to
     * its end, round all of it, as high as its coping's ridge; in the way as it is shaped ({@link #volumes}).
     */
    public Course.Piece piece(final int u, final int key) {
        final Place at = place(u);
        double[] box = {Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY};
        for (final Point[] across : footprint(u, widest())) {
            for (final Point p : new Point[] {across[0], across[2]}) {
                final Point local = local(at, p);
                box = new double[] {Math.min(box[0], local.across()), Math.max(box[1], local.across()),
                        Math.min(box[2], local.along()), Math.max(box[3], local.along())};
            }
        }
        // its box's middle, back in the world, so what is drawn about it is drawn where it is
        final Vec3 right = Num.right(at.yaw());
        final Vec3 ahead = Num.heading(at.yaw());
        final double ma = (box[0] + box[1]) / 2;
        final double ml = (box[2] + box[3]) / 2;
        final Place middle = new Place(at.x() + right.x() * ma + ahead.x() * ml, at.z() + right.z() * ma + ahead.z() * ml,
                0, at.yaw());
        final Block block = new Block(middle.x(), middle.z(), (box[1] - box[0]) / 2, (box[3] - box[2]) / 2, at.yaw(), 0,
                style.top());
        return Course.Piece.shaped(block, Course.Kind.WALL_UNIT, key, List.of(), volumes(u), middle);
    }

    /** Unit {@code u}'s solids, dressed in {@code palette}, about the middle of its piece's box and turned with it. */
    public List<Solid> drawn(final int u, final Palette palette) {
        final Place at = piece(u, 0).place();
        return solids(u, palette).stream().map(s -> new Solid(s.name(), s.role(), s.material(),
                s.corners().stream().map(p -> {
                    final Point local = local(at, p);
                    return new Point(local.across(), local.along(), p.up());
                }).toList())).toList();
    }

    /** Where unit {@code u} stands: at its start, heading to its end. */
    private Place place(final int u) {
        final Unit unit = units.get(u);
        final double yaw = Math.toDegrees(Math.atan2(unit.to().across() - unit.from().across(),
                unit.to().along() - unit.from().along()));
        return new Place(unit.from().across(), unit.from().along(), 0, yaw);
    }

    /** {@code p} across the ground in the frame of {@code at}: across to its right, along ahead of it. */
    private static Point local(final Place at, final Point p) {
        final Vec3 right = Num.right(at.yaw());
        final Vec3 ahead = Num.heading(at.yaw());
        final double dx = p.across() - at.x();
        final double dz = p.along() - at.z();
        return new Point(dx * right.x() + dz * right.z(), dx * ahead.x() + dz * ahead.z(), 0);
    }

    /** How far out each side its widest layer reaches. */
    private double widest() {
        return style.thickness() / 2 + Math.max(style.projection(), style.overhang());
    }

    private static String key(final Point p) {
        return Math.round(p.across()) + "," + Math.round(p.along());
    }

    private static double flat(final Point p) {
        return Math.hypot(p.across(), p.along());
    }

    private static Vec3 vec(final Point p) {
        return new Vec3(p.across(), p.along(), p.up());
    }
}
