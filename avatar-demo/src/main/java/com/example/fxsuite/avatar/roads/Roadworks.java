package com.example.fxsuite.avatar.roads;

import com.example.fxsuite.avatar.sim.Land;
import com.example.fxsuite.avatar.sim.Vec2;

import java.util.ArrayList;
import java.util.List;

/**
 * Several roads laid on the same ground, in order, each designed against the ground as those before it left it
 * ({@link Roadway}). Their prepared grounds together, by two rules:
 *
 * <ul>
 *   <li><b>no intrusion</b>: no road's prepared ground changes ground an earlier road occupies, across its strips
 *       where the ground carries it ({@link Roadway#occupies}); there, the earlier stands;</li>
 *   <li><b>meeting slopes</b>: elsewhere each road's slopes are worked out against the ground as those before it
 *       left it, so where two meet the lower is taken in a cutting and the higher on a bank.</li>
 * </ul>
 *
 * <p>Where a later road's slope would have changed an earlier's ground at its edge, and was kept from it, the ground
 * steps there: too steep for a slope, it needs a retaining wall ({@link Wall}), reported where and how high.
 */
public final class Roadworks {

    /** A road to lay: its name, its line, its kind across, what carries it where that is given, what it must clear. */
    public record Plan(String name, List<Vec2> line, Section section, List<Roadway.Given> given,
                       List<Roadway.Clearance> clearances) {
    }

    /**
     * Where the ground between two roads steps too much for a slope: along {@code road}'s edge on its {@code side},
     * from {@code from} to {@code to} along it, where {@code against}'s slope was kept off it; {@code highest} the
     * most it steps, how high a wall it needs.
     */
    public record Wall(int road, int side, int against, double from, double to, double highest) {
    }

    /** The least step in the ground worth a wall. */
    static final double STEP = 30;

    private final Profile raw;
    private final List<Roadway> roads;
    private final List<Wall> walls;
    private final List<String> problems;

    private Roadworks(final Profile raw, final List<Roadway> roads) {
        this.raw = raw;
        this.roads = List.copyOf(roads);
        this.walls = walls();
        final List<String> found = new ArrayList<>();
        for (final Roadway r : roads) {
            found.addAll(r.problems());
        }
        for (final Wall w : walls) {
            found.add(String.format(java.util.Locale.ROOT,
                    "\"%s\": a wall up to %.1f m high wanted on its %s, %.0f–%.0f m along, against \"%s\"",
                    roads.get(w.road()).name(), w.highest() / 100, w.side() < 0 ? "left" : "right", w.from() / 100,
                    w.to() / 100, roads.get(w.against()).name()));
        }
        this.problems = List.copyOf(found);
    }

    /**
     * {@code plans} laid on {@code raw} in order, each designed against the ground as those before it left it where
     * they occupy it, and as it was elsewhere: it follows an earlier road it meets, never the slopes and ditches an
     * earlier road would have dug where it is itself to be.
     */
    public static Roadworks design(final Profile raw, final List<Plan> plans) {
        final List<Roadway> laid = new ArrayList<>();
        for (final Plan p : plans) {
            final Roadworks before = new Roadworks(raw, laid, false);
            final Profile profile = (x, z, below) -> before.occupied(x, z)
                    ? before.ground().at(x, z).height() : raw.height(x, z, below);
            laid.add(Roadway.design(p.name(), p.line(), p.section(), profile, p.given(), p.clearances()));
        }
        return new Roadworks(raw, laid);
    }

    /** Whether any of its roads occupies (x, z). */
    private boolean occupied(final double x, final double z) {
        return occupiedBefore(roads.size(), x, z);
    }

    /** As far as the roads laid so far: no walls looked for. */
    private Roadworks(final Profile raw, final List<Roadway> roads, final boolean looked) {
        this.raw = raw;
        this.roads = List.copyOf(roads);
        this.walls = List.of();
        this.problems = List.of();
    }

    /**
     * Two carriageways of a divided road, of one-way kind {@code section}, along {@code centre}: each its line at its
     * inner edge, its half of the median between it and the centre; the first going along {@code centre}, the second
     * back along it, each with its paved strips away from the centre. Named {@code name} and the way each goes.
     */
    public static List<Plan> carriageways(final String name, final List<Vec2> centre, final Section section,
                                          final List<Roadway.Given> given, final List<Roadway.Clearance> clearances) {
        final int side = section.pavedSide();
        if (side == 0) {
            throw new IllegalArgumentException("\"" + section.name() + "\" is not one way: paved on both sides, or neither");
        }
        // its line as far from the centre as its half of the median is wide, on the side its pavement is
        final double half = section.edge(-side);
        final List<Vec2> one = offset(centre, side * half);
        final List<Vec2> other = offset(centre, -side * half).reversed();
        final double length = length(centre);
        final List<Roadway.Given> back = given.stream()
                .map(g -> new Roadway.Given(length - g.to(), length - g.from(), g.carrier())).toList();
        final List<Roadway.Clearance> clearBack = clearances.stream()
                .map(c -> new Roadway.Clearance(length - c.to(), length - c.from(), c.over())).toList();
        return List.of(new Plan(name + " one way", one, section, given, clearances),
                new Plan(name + " the other way", other, section, back, clearBack));
    }

    /** {@code line} moved {@code by} to its right (to its left, less than nothing), square to it at each point. */
    static List<Vec2> offset(final List<Vec2> line, final double by) {
        final List<Vec2> moved = new ArrayList<>();
        for (int i = 0; i < line.size(); i++) {
            final Vec2 a = line.get(Math.max(i - 1, 0));
            final Vec2 b = line.get(Math.min(i + 1, line.size() - 1));
            final double dx = b.x() - a.x();
            final double dz = b.z() - a.z();
            final double len = Math.hypot(dx, dz);
            moved.add(new Vec2(line.get(i).x() + dz / len * by, line.get(i).z() - dx / len * by));
        }
        return moved;
    }

    private static double length(final List<Vec2> line) {
        double length = 0;
        for (int i = 1; i < line.size(); i++) {
            length += Math.hypot(line.get(i).x() - line.get(i - 1).x(), line.get(i).z() - line.get(i - 1).z());
        }
        return length;
    }

    public List<Roadway> roads() {
        return roads;
    }

    /** What was there before them all. */
    public Profile raw() {
        return raw;
    }

    /** Where walls are wanted. */
    public List<Wall> wanted() {
        return walls;
    }

    /** What is wrong with them: each road's own, and the walls wanted. */
    public List<String> problems() {
        return problems;
    }

    /** The ground as they all leave it. */
    public Ground ground() {
        return (x, z) -> at(x, z, roads.size());
    }

    /** The box round its roads and as far as they reach, and some: min x, max x, min z, max z. */
    public double[] bounds(final double margin) {
        double x0 = Double.POSITIVE_INFINITY;
        double x1 = Double.NEGATIVE_INFINITY;
        double z0 = Double.POSITIVE_INFINITY;
        double z1 = Double.NEGATIVE_INFINITY;
        for (final Roadway road : roads) {
            for (int i = 0; i < road.size(); i++) {
                final Vec2 p = road.point(i);
                x0 = Math.min(x0, p.x() - road.reach() - margin);
                x1 = Math.max(x1, p.x() + road.reach() + margin);
                z0 = Math.min(z0, p.z() - road.reach() - margin);
                z1 = Math.max(z1, p.z() + road.reach() + margin);
            }
        }
        return new double[] {x0, x1, z0, z1};
    }

    /** The pavement's top at (x, z) where a road's pavement lies on the ground (or on a structure already there); else not a number. */
    public double pavement(final double x, final double z) {
        for (final Roadway road : roads) {
            if (road.occupies(x, z)) {
                return road.surface(x, z);
            }
        }
        return Double.NaN;
    }

    /**
     * They as a feature of the land ({@link Land.Layer}), laid over level ground at 0: the ground as they leave it; and,
     * under a road's pavement where the ground carries it, the pavement's top, {@code sink} lower: what is stood and
     * driven on, until the pavement is a surface of its own. The ground's covers as theirs: bare earth, gravel; the
     * pavement as asphalt, or gravel where its top is. Under a structure, the ground: the structure stands on it.
     * {@code sink} not a number: the pavement not in the land, a thing of its own ({@link Roadway#pavement}), the land
     * under it its formation.
     */
    public Land.Layer land(final double sink) {
        final Ground ground = ground();
        final double[] box = bounds(0);
        double lowest = 0;
        double highest = 0;
        for (int i = 0; i <= 40; i++) {
            for (int j = 0; j <= 40; j++) {
                final double h = raw.height(box[0] + (box[1] - box[0]) * i / 40, box[2] + (box[3] - box[2]) * j / 40);
                lowest = Math.min(lowest, h);
                highest = Math.max(highest, h);
            }
        }
        for (final Roadway road : roads) {
            for (int i = 0; i < road.size(); i++) {
                lowest = Math.min(lowest, road.level(i) - 2_000);
                highest = Math.max(highest, road.level(i) + 500);
            }
        }
        final double deepest = -lowest;
        final double tallest = highest;
        return new Land.Layer() {
            @Override
            public double shape(final double x, final double z, final double under) {
                final double top = Double.isNaN(sink) ? Double.NaN : pavement(x, z);
                return Double.isNaN(top) ? ground.at(x, z).height() : top - sink;
            }

            @Override
            public boolean touches(final double minX, final double minZ, final double maxX, final double maxZ) {
                return true;
            }

            @Override
            public double deepest() {
                return deepest;
            }

            @Override
            public double tallest() {
                return tallest;
            }

            @Override
            public double cover(final Land.Cover cover, final double x, final double z) {
                for (final Roadway road : roads) {
                    if (road.occupies(x, z) && !Double.isNaN(road.surface(x, z))) {
                        final Roadway.Near near = road.nearest(x, z);
                        final Course.Stuff top =
                                road.section().at(near.across()).strip().courses().getFirst().stuff();
                        final Land.Cover is = top == Course.Stuff.GRAVEL ? Land.Cover.GRAVEL : Land.Cover.ASPHALT;
                        return is == cover ? 1 : 0;
                    }
                }
                final Cover prepared = ground.at(x, z).cover();
                if (prepared == Cover.EARTH) {
                    return cover == Land.Cover.EARTH ? 1 : 0;
                }
                if (prepared == Cover.GRAVEL) {
                    return cover == Land.Cover.GRAVEL ? 1 : 0;
                }
                return 0;
            }
        };
    }

    /** The ground at (x, z) as the first {@code count} roads leave it. */
    private Prepared at(final double x, final double z, final int count) {
        Prepared p = new Prepared(raw.height(x, z), null);
        for (int k = 0; k < count; k++) {
            if (occupiedBefore(k, x, z)) {
                continue;
            }
            final Prepared q = roads.get(k).prepared(x, z, p.height());
            if (q.touched()) {
                p = q;
            }
        }
        return p;
    }

    private boolean occupiedBefore(final int k, final double x, final double z) {
        for (int m = 0; m < k; m++) {
            if (roads.get(m).occupies(x, z)) {
                return true;
            }
        }
        return false;
    }

    /**
     * How low and how high wall {@code w} goes at its road's {@code i}th point: from the lower of the ground as kept
     * at the road's edge and as the later road would have had it there, to the higher.
     */
    public double[] span(final Wall w, final int i) {
        final Roadway road = roads.get(w.road());
        final Vec2 p = road.at(i, w.side() * (road.section().edge(w.side()) - 1));
        final double kept = ground().at(p.x(), p.z()).height();
        final double wanted = roads.get(w.against()).prepared(p.x(), p.z(), kept).height();
        return new double[] {Math.min(kept, wanted), Math.max(kept, wanted)};
    }

    /**
     * Along each road's edges where the ground carries it, just inside them: how far each later road's prepared
     * ground would have had it, against how it is. Where that differs by more than {@link #STEP}, a wall.
     */
    private List<Wall> walls() {
        final List<Wall> found = new ArrayList<>();
        final Ground all = ground();
        for (int m = 0; m < roads.size(); m++) {
            final Roadway road = roads.get(m);
            for (int k = m + 1; k < roads.size(); k++) {
                final Roadway later = roads.get(k);
                for (final int side : new int[] {-1, 1}) {
                    final double inside = side * (road.section().edge(side) - 1);
                    int from = -1;
                    double highest = 0;
                    for (int i = 0; i <= road.size(); i++) {
                        double step = 0;
                        if (i < road.size() && road.grounded(Math.min(i, road.size() - 2))) {
                            final Vec2 p = road.at(i, inside);
                            final double kept = all.at(p.x(), p.z()).height();
                            final Prepared wanted = later.prepared(p.x(), p.z(), kept);
                            step = wanted.touched() ? Math.abs(wanted.height() - kept) : 0;
                        }
                        if (step > STEP) {
                            if (from < 0) {
                                from = i;
                                highest = 0;
                            }
                            highest = Math.max(highest, step);
                        } else if (from >= 0) {
                            found.add(new Wall(m, side, k, road.along(from), road.along(i - 1), highest));
                            from = -1;
                        }
                    }
                }
            }
        }
        return List.copyOf(found);
    }
}
