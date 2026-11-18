package com.example.fxsuite.avatar.roads;

import com.example.fxsuite.avatar.sim.Num;
import com.example.fxsuite.avatar.sim.Pavement;
import com.example.fxsuite.avatar.sim.Polyline;
import com.example.fxsuite.avatar.sim.Vec2;
import com.example.fxsuite.avatar.sim.Volume;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A road designed on its own: along a line, of a kind across ({@link Section}), against what is there before it
 * ({@link Profile}), never the land itself. Worked out, in order:
 *
 * <ol>
 *   <li><b>its heights</b>: as near what is there as they can be while changing evenly over its kind's vertical
 *       length, its ends on what is there, held at least as high as its clearances ask, kept within its steepest
 *       grade. Over a structure or a tunnel what is there is not asked: only its limits hold it;</li>
 *   <li><b>what carries it</b>, stretch by stretch ({@link Carrier}): as it is told where it is told; elsewhere a
 *       structure where a bank would be higher than its kind takes, a tunnel where a cutting would be deeper, the
 *       ground otherwise; and its heights worked out again with that, until it holds;</li>
 *   <li><b>its formation</b> everywhere across it, its surface over that by its courses; and, where the ground
 *       carries it, the ground shaped to its formation and out to the ground as it was ({@link #shape}).</li>
 * </ol>
 *
 * <p>Distances in cm, heights up. Across is to the right going along it.
 */
public final class Roadway {

    /** How far apart its line's points are. */
    public static final double STEP = 200;
    /** How deep a structure's deck is under the road's line. */
    public static final double DECK = 150;
    /** How high a structure's parapets stand over the road's surface at its edge, and how thick. */
    public static final double PARAPET = 110;
    public static final double PARAPET_THICK = 30;
    /** How far apart a structure's piers are, about. */
    public static final double SPAN = 3_000;
    /** How much room there must be under a deck for a pier there. */
    public static final double CLEAR = 200;
    /** How much headroom a tunnel leaves over the road's line, and how thick its lining. */
    public static final double HEADROOM = 600;
    public static final double LINING = 50;
    /** The shortest structure or tunnel worth choosing. */
    static final double SHORTEST = 2_000;
    /** The longest stretch of ground left between two structures, or two tunnels, before they are made one. */
    static final double GAP = 3_000;

    /** Told what carries it from {@code from} to {@code to} along it. */
    public record Given(double from, double to, Carrier carrier) {
    }

    /** Its surface held at least {@code over} high from {@code from} to {@code to} along it: over water, over a road. */
    public record Clearance(double from, double to, double over) {
    }

    /** A stretch of it from its {@code from}th point to its {@code to}th, carried by {@code carrier}. */
    public record Stretch(int from, int to, Carrier carrier) {
    }

    /** A point near it: nearest its {@code segment}th segment, {@code t} of the way along it; so far along it; so far across. */
    public record Near(int segment, double t, double along, double across) {
    }

    /** A structure or a tunnel on one of its stretches, and where along it its piers stand: at which of its points. */
    public record Structure(Stretch stretch, List<Integer> piers) {
    }

    private final String name;
    private final Section section;
    private final Profile profile;
    private final List<Vec2> points;
    private final double[] along;
    private final double[] levels;
    private final double[] ground;
    private final Carrier[] carriers;
    private final List<Stretch> stretches;
    private final List<String> problems;
    private final Polyline line;

    private Roadway(final String name, final Section section, final Profile profile, final List<Vec2> points,
                    final double[] along, final double[] levels, final double[] ground, final Carrier[] carriers,
                    final List<String> problems) {
        this.name = name;
        this.section = section;
        this.profile = profile;
        this.points = List.copyOf(points);
        this.along = along;
        this.levels = levels;
        this.ground = ground;
        this.carriers = carriers;
        this.problems = List.copyOf(problems);
        this.stretches = stretches(carriers);
        // as far as its side slopes may reach: its widest, and its highest bank or deepest cutting, and some, sloped
        double off = 0;
        for (int i = 0; i < levels.length; i++) {
            off = Math.max(off, Math.abs(levels[i] - ground[i]));
        }
        final Earthworks e = section.earthworks();
        final double reach = Math.max(section.edge(-1), section.edge(1)) + e.ditchWide()
                + (off + 1_500) * Math.max(e.fill(), e.cut()) + 500;
        this.line = new Polyline(points, reach);
    }

    /**
     * {@code name}, along {@code line}, of kind {@code section}, designed against {@code profile}: carried as
     * {@code given} says where it says; held over its {@code clearances}.
     */
    public static Roadway design(final String name, final List<Vec2> line, final Section section, final Profile profile,
                                 final List<Given> given, final List<Clearance> clearances) {
        final List<Vec2> points = resampled(line, STEP);
        final int n = points.size();
        final double[] along = new double[n];
        for (int i = 1; i < n; i++) {
            along[i] = along[i - 1] + distance(points.get(i - 1), points.get(i));
        }
        final double[] ground = new double[n];
        final Carrier[] told = new Carrier[n];
        for (int i = 0; i < n; i++) {
            ground[i] = profile.height(points.get(i).x(), points.get(i).z());
            for (final Given g : given) {
                if (along[i] >= g.from() && along[i] <= g.to()) {
                    told[i] = g.carrier();
                }
            }
        }
        final List<String> problems = new ArrayList<>();
        Carrier[] at = new Carrier[n];
        for (int i = 0; i < n; i++) {
            at[i] = told[i] != null ? told[i] : Carrier.GROUND;
        }
        double[] levels = null;
        List<String> found = List.of();
        for (int round = 0; round < 4; round++) {
            final List<String> these = new ArrayList<>();
            levels = levels(name, section, along, ground, at, clearances, these);
            final Carrier[] next = chosen(section.earthworks(), along, levels, ground, told, at);
            found = these;
            if (Arrays.equals(next, at)) {
                break;
            }
            at = next;
        }
        problems.addAll(found);
        // a bank higher, or a cutting deeper, than the kind takes, left so
        final Earthworks e = section.earthworks();
        final Carrier[] carriers = segments(at);
        boolean banked = false;
        boolean cut = false;
        for (int j = 0; j < carriers.length; j++) {
            if (carriers[j] == Carrier.GROUND) {
                if (!banked && levels[j] - ground[j] > e.bank() + 1) {
                    problems.add("\"" + name + "\": a bank " + metres(levels[j] - ground[j]) + " m high "
                            + metres(along[j]) + " m along, higher than it takes, too short to carry otherwise");
                    banked = true;
                }
                if (!cut && ground[j] - levels[j] > e.cutting() + 1) {
                    problems.add("\"" + name + "\": a cutting " + metres(ground[j] - levels[j]) + " m deep "
                            + metres(along[j]) + " m along, deeper than it takes, too short to carry otherwise");
                    cut = true;
                }
            }
        }
        return new Roadway(name, section, profile, points, along, levels, ground, carriers, problems);
    }

    /** Its heights, as near the ground as they may be where it counts, held over its clearances, within its grade. */
    private static double[] levels(final String name, final Section section, final double[] along, final double[] ground,
                                   final Carrier[] at, final List<Clearance> clearances, final List<String> problems) {
        final int n = along.length;
        final double[] weight = new double[n];
        final double[] pinned = new double[n];
        Arrays.fill(pinned, Double.NaN);
        for (int i = 0; i < n; i++) {
            weight[i] = at[i] == Carrier.STRUCTURE || at[i] == Carrier.TUNNEL ? 0 : 1;
        }
        pinned[0] = ground[0];
        pinned[n - 1] = ground[n - 1];
        double[] levels = null;
        for (int round = 0; round < 12; round++) {
            levels = fitted(along, ground, weight, pinned, section.vertical());
            within(along, levels, pinned, section.grade());
            boolean more = false;
            for (final Clearance c : clearances) {
                for (int i = 0; i < n; i++) {
                    if (along[i] >= c.from() && along[i] <= c.to() && levels[i] < c.over() - 0.5 && Double.isNaN(pinned[i])) {
                        pinned[i] = c.over();
                        more = true;
                    }
                }
            }
            if (!more) {
                break;
            }
        }
        for (final Clearance c : clearances) {
            for (int i = 0; i < n; i++) {
                if (along[i] >= c.from() && along[i] <= c.to() && levels[i] < c.over() - 1) {
                    problems.add("\"" + name + "\": " + metres(c.over() - levels[i]) + " m short of its clearance "
                            + metres(along[i]) + " m along");
                    break;
                }
            }
        }
        for (int i = 1; i < n; i++) {
            final double run = along[i] - along[i - 1];
            if (run > 0 && Math.abs(levels[i] - levels[i - 1]) / run > section.grade() + 1e-6) {
                problems.add("\"" + name + "\": steeper than it may be " + metres(along[i]) + " m along");
                break;
            }
        }
        return levels;
    }

    /**
     * What carries each point next: as told; once chosen other than the ground, still so; else a structure where
     * its bank would be higher than its kind takes, a tunnel where its cutting deeper. Then the ground between two
     * of the same close together made that too, and what is chosen too short to be worth it left on the ground.
     */
    private static Carrier[] chosen(final Earthworks e, final double[] along, final double[] levels, final double[] ground,
                                    final Carrier[] told, final Carrier[] was) {
        final int n = along.length;
        final Carrier[] next = new Carrier[n];
        for (int i = 0; i < n; i++) {
            if (told[i] != null) {
                next[i] = told[i];
            } else if (was[i] != Carrier.GROUND) {
                next[i] = was[i];
            } else if (levels[i] - ground[i] > e.bank()) {
                next[i] = Carrier.STRUCTURE;
            } else if (ground[i] - levels[i] > e.cutting()) {
                next[i] = Carrier.TUNNEL;
            } else {
                next[i] = Carrier.GROUND;
            }
        }
        // the ground between two the same, close together, not told
        final List<int[]> runs = runs(next);
        for (int r = 1; r + 1 < runs.size(); r++) {
            final int[] run = runs.get(r);
            final Carrier before = next[runs.get(r - 1)[0]];
            final Carrier after = next[runs.get(r + 1)[0]];
            if (next[run[0]] == Carrier.GROUND && before == after && before != Carrier.GROUND
                    && along[run[1]] - along[run[0]] < GAP && untold(told, run)) {
                Arrays.fill(next, run[0], run[1] + 1, before);
            }
        }
        // too short, not told
        for (final int[] run : runs(next)) {
            final Carrier c = next[run[0]];
            if ((c == Carrier.STRUCTURE || c == Carrier.TUNNEL) && untold(told, run)
                    && along[run[1]] - along[run[0]] < SHORTEST) {
                Arrays.fill(next, run[0], run[1] + 1, Carrier.GROUND);
            }
        }
        return next;
    }

    private static boolean untold(final Carrier[] told, final int[] run) {
        for (int i = run[0]; i <= run[1]; i++) {
            if (told[i] != null) {
                return false;
            }
        }
        return true;
    }

    /** Runs of the same, first to last, inclusive. */
    private static List<int[]> runs(final Carrier[] at) {
        final List<int[]> runs = new ArrayList<>();
        int start = 0;
        for (int i = 1; i <= at.length; i++) {
            if (i == at.length || at[i] != at[start]) {
                runs.add(new int[] {start, i - 1});
                start = i;
            }
        }
        return runs;
    }

    /** What carries each segment: what carries either end of it, if not the ground. */
    private static Carrier[] segments(final Carrier[] at) {
        final Carrier[] carriers = new Carrier[at.length - 1];
        for (int j = 0; j < carriers.length; j++) {
            carriers[j] = at[j] != Carrier.GROUND ? at[j] : at[j + 1];
        }
        return carriers;
    }

    /** Its segments' carriers as stretches, point to point. */
    private static List<Stretch> stretches(final Carrier[] carriers) {
        final List<Stretch> stretches = new ArrayList<>();
        int start = 0;
        for (int j = 1; j <= carriers.length; j++) {
            if (j == carriers.length || carriers[j] != carriers[start]) {
                stretches.add(new Stretch(start, j, carriers[start]));
                start = j;
            }
        }
        return List.copyOf(stretches);
    }

    public String name() {
        return name;
    }

    /** Its kind across, as it is on the ground. */
    public Section section() {
        return section;
    }

    /** Its kind across on its {@code j}th segment. Its ground strips there only where the ground carries it ({@link #grounded}). */
    public Section section(final int j) {
        return section;
    }

    /** Whether the ground carries its {@code j}th segment, and so its ground strips are there: graded, covered. */
    public boolean grounded(final int j) {
        return carriers[j] == Carrier.GROUND;
    }

    /** What it was designed against. */
    public Profile profile() {
        return profile;
    }

    /** How many points its line has. */
    public int size() {
        return points.size();
    }

    public Vec2 point(final int i) {
        return points.get(i);
    }

    /** How far along it its {@code i}th point is. */
    public double along(final int i) {
        return along[i];
    }

    /** How long it is. */
    public double length() {
        return along[along.length - 1];
    }

    /** How high its line is at its {@code i}th point: its surface there, at its crown. */
    public double level(final int i) {
        return levels[i];
    }

    /** How high what is there is under its {@code i}th point, before it. */
    public double ground(final int i) {
        return ground[i];
    }

    /** What carries its {@code j}th segment, from point {@code j} to the next. */
    public Carrier carrier(final int j) {
        return carriers[j];
    }

    /** Its stretches, each carried by one thing, end to end. */
    public List<Stretch> stretches() {
        return stretches;
    }

    /** What is wrong with it. */
    public List<String> problems() {
        return problems;
    }

    /** How far its side slopes may reach from its line. */
    public double reach() {
        return line.reach();
    }

    /** The way to its right at its {@code i}th point, square to the way it goes there (the mitre at a bend). */
    public Vec2 right(final int i) {
        final Vec2 a = points.get(Math.max(i - 1, 0));
        final Vec2 b = points.get(Math.min(i + 1, points.size() - 1));
        final double dx = b.x() - a.x();
        final double dz = b.z() - a.z();
        final double len = Math.hypot(dx, dz);
        return new Vec2(dz / len, -dx / len);
    }

    /** Where its {@code i}th point's cross-section is {@code across} from its line. */
    public Vec2 at(final int i, final double across) {
        final Vec2 p = points.get(i);
        final Vec2 r = right(i);
        return new Vec2(p.x() + r.x() * across, p.z() + r.z() * across);
    }

    /** How high its line is {@code t} of the way along its {@code j}th segment. */
    public double level(final int j, final double t) {
        return levels[j] + (levels[j + 1] - levels[j]) * t;
    }

    /** How high its surface is {@code t} of the way along its {@code j}th segment, {@code across}. */
    public double top(final int j, final double t, final double across) {
        return level(j, t) + section(j).top(across);
    }

    /** How high its formation is {@code t} of the way along its {@code j}th segment, {@code across}. */
    public double formation(final int j, final double t, final double across) {
        return level(j, t) + section(j).formation(across);
    }

    /** How steep it is at most, rise over run. */
    public double steepest() {
        double steepest = 0;
        for (int i = 1; i < levels.length; i++) {
            steepest = Math.max(steepest, Math.abs(levels[i] - levels[i - 1]) / (along[i] - along[i - 1]));
        }
        return steepest;
    }

    /** Its highest bank where the ground carries it: how far its line is over the ground under it. */
    public double highestBank() {
        double most = 0;
        for (int j = 0; j < carriers.length; j++) {
            if (carriers[j] == Carrier.GROUND) {
                most = Math.max(most, Math.max(levels[j] - ground[j], levels[j + 1] - ground[j + 1]));
            }
        }
        return most;
    }

    /** Its deepest cutting where the ground carries it: how far its line is under the ground over it. */
    public double deepestCutting() {
        double most = 0;
        for (int j = 0; j < carriers.length; j++) {
            if (carriers[j] == Carrier.GROUND) {
                most = Math.max(most, Math.max(ground[j] - levels[j], ground[j + 1] - levels[j + 1]));
            }
        }
        return most;
    }

    /** Where on it (x, z) is nearest; null if beyond its reach, or beyond either end. */
    public Near nearest(final double x, final double z) {
        final Polyline.Near near = line.nearest(x, z);
        if (near == null) {
            return null;
        }
        final int j = near.segment();
        final Vec2 a = points.get(j);
        final Vec2 b = points.get(j + 1);
        final double dx = b.x() - a.x();
        final double dz = b.z() - a.z();
        final double len = Math.hypot(dx, dz);
        final double raw = ((x - a.x()) * dx + (z - a.z()) * dz) / (len * len);
        if (j == 0 && raw < -1e-9 || j == points.size() - 2 && raw > 1 + 1e-9) {
            return null;
        }
        final double side = (x - a.x()) * dz - (z - a.z()) * dx;
        return new Near(j, near.t(), along[j] + near.t() * len, side < 0 ? -near.distance() : near.distance());
    }

    /** How high its pavement's surface is at (x, z); not a number off it, on its ground strips or beyond its edges. */
    public double surface(final double x, final double z) {
        final Near near = nearest(x, z);
        if (near == null) {
            return Double.NaN;
        }
        final Section s = section(near.segment());
        if (near.across() < -s.edge(-1) || near.across() > s.edge(1) || !s.at(near.across()).strip().paved()) {
            return Double.NaN;
        }
        return top(near.segment(), near.t(), near.across());
    }

    /**
     * Its pavement as a thing of its own ({@link Pavement}), over every carrier alike, the ground, a deck, a tunnel's
     * floor: solid from its formation up to its surface across its paved strips.
     */
    public Pavement pavement() {
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        double low = Double.POSITIVE_INFINITY;
        double high = Double.NEGATIVE_INFINITY;
        final double wide = Math.max(section.pavedEdge(-1), section.pavedEdge(1));
        final double deepest = section.paved().stream().mapToDouble(l -> l.strip().depth()).max().orElse(0);
        for (int i = 0; i < points.size(); i++) {
            final Vec2 p = points.get(i);
            minX = Math.min(minX, p.x() - wide);
            maxX = Math.max(maxX, p.x() + wide);
            minZ = Math.min(minZ, p.z() - wide);
            maxZ = Math.max(maxZ, p.z() + wide);
            low = Math.min(low, levels[i] - wide - deepest);
            high = Math.max(high, levels[i] + wide);
        }
        final Volume.Bounds bounds = new Volume.Bounds(minX, maxX, minZ, maxZ);
        final double lowest = low;
        final double highest = high;
        return new Pavement(new Pavement.Laid() {
            @Override
            public double top(final double x, final double z) {
                return surface(x, z);
            }

            @Override
            public double underside(final double x, final double z) {
                return formation(x, z);
            }

            @Override
            public Volume.Bounds bounds() {
                return bounds;
            }

            @Override
            public double low() {
                return lowest;
            }

            @Override
            public double high() {
                return highest;
            }
        });
    }

    /** How high its formation is at (x, z), under its pavement; not a number off it. */
    public double formation(final double x, final double z) {
        final Near near = nearest(x, z);
        if (near == null || Double.isNaN(surface(x, z))) {
            return Double.NaN;
        }
        return formation(near.segment(), near.t(), near.across());
    }

    /**
     * Whether (x, z) is its own: across its strips, paved and ground, where the ground or a structure already there
     * carries it. No other road's prepared ground may change it.
     */
    public boolean occupies(final double x, final double z) {
        final Near near = nearest(x, z);
        if (near == null) {
            return false;
        }
        final Carrier c = carriers[near.segment()];
        return (c == Carrier.GROUND || c == Carrier.ON) && near.across() >= -section.edge(-1)
                && near.across() <= section.edge(1);
    }

    /**
     * The ground at (x, z), {@code under} high as it was, as this road leaves it: its prepared ground. Where the
     * ground carries it, one profile across: its formation under its paved strips, its ground strips at their
     * surface, and from its edges its side slopes down or up to the ground as it was, a ditch at the foot of a
     * cutting. Where a structure carries it, as it was, but for the banks it comes off: each ending under the
     * structure in a slope as steep as its side slopes. In a tunnel, or on a structure already there, as it was.
     */
    public double shape(final double x, final double z, final double under) {
        return prepared(x, z, under).height();
    }

    /** The ground at (x, z), {@code under} high as it was, as this road prepares it, and what covers it. */
    public Prepared prepared(final double x, final double z, final double under) {
        final Near near = nearest(x, z);
        if (near == null) {
            return new Prepared(under, null);
        }
        final int j = near.segment();
        return switch (carriers[j]) {
            case GROUND -> earthwork(level(j, near.t()), section, near.across(), under);
            case STRUCTURE -> endSlopes(j, near, under);
            case TUNNEL, ON -> new Prepared(under, null);
        };
    }

    /** What is there, as this road leaves it. */
    public Profile shaped() {
        return (x, z, below) -> shape(x, z, profile.height(x, z, below));
    }

    /** The ground as this road prepares it, on what was there before it: how high, and what covers it. */
    public Ground ground() {
        return (x, z) -> prepared(x, z, profile.height(x, z));
    }

    /** The banks a structure comes off, each running on under it, its end sloping down as its sides do. */
    private Prepared endSlopes(final int j, final Near near, final double under) {
        double h = under;
        for (final Stretch s : stretches) {
            if (j < s.from() || j >= s.to()) {
                continue;
            }
            for (final int end : new int[] {s.from(), s.to()}) {
                final boolean grounded = end == s.from() ? end > 0 && carriers[end - 1] == Carrier.GROUND
                        : end < carriers.length && carriers[end] == Carrier.GROUND;
                if (grounded) {
                    final double bank = earthwork(levels[end], section, near.across(), under).height();
                    if (bank > under) {
                        h = Math.max(h, bank - Math.abs(near.along() - along[end]) / section.earthworks().fill());
                    }
                }
            }
        }
        return new Prepared(h, h > under ? Cover.GRASS : null);
    }

    /** The ground {@code across} from a line {@code level} high, {@code under} high as it was, prepared for {@code s}. */
    private static Prepared earthwork(final double level, final Section s, final double across, final double under) {
        final int side = across < 0 ? -1 : 1;
        final double edge = s.edge(side);
        if (Math.abs(across) <= edge) {
            // under a paved strip its formation, bare until paved; a ground strip at its surface, covered
            final Cover cover = s.cover(across);
            return new Prepared(level + s.formation(across), cover == null ? Cover.EARTH : cover);
        }
        final double edgeTop = level + s.top(side * edge);
        final double d = Math.abs(across) - edge;
        final Earthworks e = s.earthworks();
        if (under > edgeTop) {
            final double w = e.ditched() ? e.ditchWide() : 0;
            if (d < w) {
                return new Prepared(Math.min(under, edgeTop - e.ditchDeep() * (1 - Math.abs(2 * d / w - 1))), Cover.EARTH);
            }
            final double h = edgeTop + (d - w) / e.cut();
            return h < under ? new Prepared(h, Cover.GRASS) : new Prepared(under, null);
        }
        final double h = edgeTop - d / e.fill();
        return h > under ? new Prepared(h, Cover.GRASS) : new Prepared(under, null);
    }

    /**
     * How much earth it moves, in cubic metres: {cut, fill}. Summed over its cross-sections every point along it,
     * each sampled every half metre across as far as it reaches.
     */
    public double[] earthMoved() {
        final Profile shaped = shaped();
        double cut = 0;
        double fill = 0;
        final double across = 50;
        for (int i = 0; i < points.size(); i++) {
            final double length = (along[Math.min(i + 1, along.length - 1)] - along[Math.max(i - 1, 0)]) / 2;
            for (double a = -line.reach(); a <= line.reach(); a += across) {
                final Vec2 p = at(i, a);
                final double was = profile.height(p.x(), p.z());
                final double now = shaped.height(p.x(), p.z());
                if (now > was) {
                    fill += (now - was) * across * length;
                } else {
                    cut += (was - now) * across * length;
                }
            }
        }
        return new double[] {cut / 1e6, fill / 1e6};
    }

    /**
     * Its structures and its tunnels, each on a stretch: a structure's piers about every {@link #SPAN}, evenly,
     * where there is {@link #CLEAR} room under its deck; a tunnel's none.
     */
    public List<Structure> structures() {
        final List<Structure> built = new ArrayList<>();
        for (final Stretch s : stretches) {
            if (s.carrier() == Carrier.STRUCTURE) {
                final double from = along[s.from()];
                final double to = along[s.to()];
                final int spans = Math.max(1, (int) Math.round((to - from) / SPAN));
                final List<Integer> piers = new ArrayList<>();
                for (int m = 1; m < spans; m++) {
                    final int i = nearestPoint(from + (to - from) * m / spans);
                    if (levels[i] - DECK - ground[i] > CLEAR) {
                        piers.add(i);
                    }
                }
                built.add(new Structure(s, List.copyOf(piers)));
            } else if (s.carrier() == Carrier.TUNNEL) {
                built.add(new Structure(s, List.of()));
            }
        }
        return built;
    }

    /** Which of its points is nearest {@code s} along it. */
    public int nearestPoint(final double s) {
        final int found = Arrays.binarySearch(along, s);
        if (found >= 0) {
            return found;
        }
        final int after = Math.min(-found - 1, along.length - 1);
        final int before = Math.max(after - 1, 0);
        return Math.abs(along[before] - s) <= Math.abs(along[after] - s) ? before : after;
    }

    /** {@code line} with a point every {@code step} along it, and its last. */
    static List<Vec2> resampled(final List<Vec2> line, final double step) {
        if (line.size() < 2) {
            throw new IllegalArgumentException("a road goes somewhere");
        }
        final List<Vec2> out = new ArrayList<>();
        out.add(line.getFirst());
        double carried = 0;
        for (int k = 0; k + 1 < line.size(); k++) {
            final Vec2 a = line.get(k);
            final Vec2 b = line.get(k + 1);
            final double len = distance(a, b);
            double s = step - carried;
            while (s < len - 1e-6) {
                out.add(new Vec2(a.x() + (b.x() - a.x()) * s / len, a.z() + (b.z() - a.z()) * s / len));
                s += step;
            }
            carried = len - (s - step);
        }
        final Vec2 last = line.getLast();
        if (distance(out.getLast(), last) > step / 4) {
            out.add(last);
        } else {
            out.set(out.size() - 1, last);
        }
        return out;
    }

    private static double distance(final Vec2 a, final Vec2 b) {
        return Math.hypot(b.x() - a.x(), b.z() - a.z());
    }

    /**
     * Heights along it, as near the ground as they can be while changing evenly: the least of how far each is from
     * the ground (as much as it is {@code weight}ed) and how much each differs from the next (the more, the longer
     * the {@code vertical} length), the pinned ones held. Solved at once, along it.
     */
    private static double[] fitted(final double[] along, final double[] ground, final double[] weight,
                                   final double[] pinned, final double vertical) {
        final int n = ground.length;
        final double[] a = new double[n];
        final double[] b = new double[n];
        final double[] c = new double[n];
        final double[] d = new double[n];
        for (int i = 0; i < n; i++) {
            if (!Double.isNaN(pinned[i])) {
                b[i] = 1;
                d[i] = pinned[i];
                continue;
            }
            final double left = i > 0 ? stiff(vertical, along[i] - along[i - 1]) : 0;
            final double right = i < n - 1 ? stiff(vertical, along[i + 1] - along[i]) : 0;
            a[i] = -left;
            c[i] = -right;
            b[i] = weight[i] + left + right;
            d[i] = weight[i] * ground[i];
            if (b[i] == 0) {
                b[i] = 1;
            }
        }
        for (int i = 1; i < n; i++) {
            final double m = a[i] / b[i - 1];
            b[i] -= m * c[i - 1];
            d[i] -= m * d[i - 1];
        }
        final double[] h = new double[n];
        h[n - 1] = d[n - 1] / b[n - 1];
        for (int i = n - 2; i >= 0; i--) {
            h[i] = (d[i] - c[i] * h[i + 1]) / b[i];
        }
        return h;
    }

    /** How strongly two heights {@code apart} are held to each other, for a vertical length {@code vertical}. */
    private static double stiff(final double vertical, final double apart) {
        return apart <= 0 ? 0 : (vertical / apart) * (vertical / apart);
    }

    /** {@code levels} kept within the steepest grade, up the line and down it, the pinned ones held. */
    private static void within(final double[] along, final double[] levels, final double[] pinned, final double grade) {
        final int n = levels.length;
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 1; i < n; i++) {
                if (Double.isNaN(pinned[i])) {
                    final double most = grade * (along[i] - along[i - 1]);
                    levels[i] = Num.clamp(levels[i], levels[i - 1] - most, levels[i - 1] + most);
                }
            }
            for (int i = n - 2; i >= 0; i--) {
                if (Double.isNaN(pinned[i])) {
                    final double most = grade * (along[i + 1] - along[i]);
                    levels[i] = Num.clamp(levels[i], levels[i + 1] - most, levels[i + 1] + most);
                }
            }
        }
    }

    private static String metres(final double cm) {
        return String.format(java.util.Locale.ROOT, "%.1f", cm / 100);
    }
}
