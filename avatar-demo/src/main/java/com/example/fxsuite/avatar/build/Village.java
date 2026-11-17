package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Alignment;
import com.example.fxsuite.avatar.sim.Block;
import com.example.fxsuite.avatar.sim.Body;
import com.example.fxsuite.avatar.sim.Channel;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Land;
import com.example.fxsuite.avatar.sim.Num;
import com.example.fxsuite.avatar.sim.Place;
import com.example.fxsuite.avatar.sim.Polyline;
import com.example.fxsuite.avatar.sim.Pond;
import com.example.fxsuite.avatar.sim.Prism;
import com.example.fxsuite.avatar.sim.River;
import com.example.fxsuite.avatar.sim.RoadMap;
import com.example.fxsuite.avatar.sim.Road;
import com.example.fxsuite.avatar.sim.Survey;
import com.example.fxsuite.avatar.sim.Vec2;
import com.example.fxsuite.avatar.sim.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A meadow, a river winding across it, and a small village by its bank: the open country.
 *
 * <ul>
 *   <li><b>The river</b> ({@link River}): across the whole world, east to west, its middle two long
 *       gentle waves laid over each other; its channel cut into the land ({@link Channel}), its water a metre deep in it.</li>
 *   <li><b>A footbridge</b> over it where the village is, square across it: a timber deck on the level,
 *       ramped up to from each bank, railed along both sides.</li>
 *   <li><b>The village</b>, north of the river: earth houses ({@link EarthHouse}) round a stone well,
 *       each facing it, each its own size and finish, as its seed has them: its walls, its roof, its
 *       window, its woodwork.</li>
 *   <li><b>Trees</b> along both banks, and here and there across the meadow.</li>
 *   <li><b>A church</b> ({@link Church}) on a small hill north-west of the village, and <b>a country road</b>
 *       ({@link Road}) winding up to it from the village's edge.</li>
 *   <li><b>A pond</b> ({@link Pond}) in the hollow up the slope east of the village.</li>
 *   <li><b>A car and a motorcycle</b>, parked just ahead of where the avatar starts, facing the river.</li>
 * </ul>
 *
 * <p>The avatar starts south of the river, before the bridge, the village beyond it. Its houses are
 * drawn by their keys from {@link #KEYS}, the church's after them ({@link #CHURCH}).
 */
public final class Village {

    private Village() {
    }

    /** Its houses' keys, to draw them by, start here: past the pavilions'. */
    public static final int KEYS = 500;
    /** What is of timber standing about (a railing, a post) is drawn as a wall of this colour: timber. */
    private static final int TIMBER = 2;

    /** The river: so wide, its banks so wide each side; on level ground, its banks so high over it. */
    public static final double RIVER = 1_200;
    public static final double BANK = 250;
    public static final double BANK_TOP = 3;
    /** Its line, a point so far apart, from one edge of the world to the other. */
    private static final double STEP = 500;
    private static final double REACH = com.example.fxsuite.avatar.sim.Body.EDGE;

    /** Where the river's middle is, north or south, at {@code x} east: two gentle waves laid over each other, south of the village. */
    public static double riverAt(final double x) {
        return 4_000 * Math.sin(x / 18_000 + 0.3) + 1_500 * Math.sin(x / 7_000 + 1.3) - 6_600;
    }

    /** Which way the river runs at {@code x}: a heading, as {@link Num#heading} has them. */
    static double riverHeading(final double x) {
        final double slope = 4_000 / 18_000.0 * Math.cos(x / 18_000 + 0.3) + 1_500 / 7_000.0 * Math.cos(x / 7_000 + 1.3);
        return Math.toDegrees(Math.atan2(1, slope));
    }

    /** The river's middle line, across the world. */
    static List<Vec2> line() {
        final List<Vec2> line = new ArrayList<>();
        for (double x = -REACH; x <= REACH; x += STEP) {
            line.add(new Vec2(x, riverAt(x)));
        }
        return line;
    }

    /**
     * The river across the world: its water {@link #WATER} deep over its channel's bed, as wide as it is
     * where its top meets the banks.
     */
    public static List<River> rivers() {
        return List.of(new River(line(), 2 * channel().across(DEPTH - WATER), WATER));
    }

    /** The well, the village's middle: so far north of the world's middle; its stone ring so far round, so high; its water within. */
    public static final Vec3 WELL = new Vec3(0, 0, 4_000);
    private static final double WELL_RING = 90;
    private static final double WELL_INSIDE = 65;
    private static final double WELL_HIGH = 75;

    /** How many houses; how near and how far from the well; how far apart, at least, beyond their walls; how far clear of the river. */
    public static final int HOUSES = 12;
    private static final double NEAREST = 1_800;
    private static final double FURTHEST = 6_000;
    private static final double APART = 700;
    private static final double DRY = RIVER / 2 + BANK + 1_500;
    /** The way down to the bridge, kept clear: so wide each side of its line. */
    private static final double WAY = 600;
    /** The seed the village is drawn from: the same seed, the same village. */
    private static final long SEED = 11;

    /** A house in the village: as big as it is built, of what, standing where, facing the well. */
    public record House(EarthHouse.Size size, Material walls, Material roof, Material window, Material woodwork,
                        Place place) {
    }

    /** Its houses, drawn from its seed, each where it fits. */
    public static final List<House> VILLAGE = houses();

    private static List<House> houses() {
        final SplittableRandom random = new SplittableRandom(SEED);
        final List<House> houses = new ArrayList<>();
        int tries = 0;
        while (houses.size() < HOUSES && tries++ < 10_000) {
            final double width = 450 + 300 * random.nextDouble();
            final double doorHigh = 205 + 15 * random.nextDouble();
            final double walls = Math.max(doorHigh + 20, 235 + 50 * random.nextDouble());
            final EarthHouse.Size size = new EarthHouse.Size(width, walls, 40 + 25 * random.nextDouble(),
                    120 + 110 * random.nextDouble(), 25 + 25 * random.nextDouble(), 90 + 20 * random.nextDouble(), doorHigh,
                    random.nextDouble() < 0.6 ? 50 + 40 * random.nextDouble() : 0, random.nextDouble() < 0.7);
            final double heading = 360 * random.nextDouble();
            final double out = NEAREST + (FURTHEST - NEAREST) * Math.sqrt(random.nextDouble());
            final Vec3 way = Num.heading(heading);
            final double x = WELL.x() + way.x() * out;
            final double z = WELL.z() + way.z() * out;
            final double reach = EarthHouse.of(size).eave();
            final boolean dry = Math.abs(z - riverAt(x)) > DRY + reach;
            final boolean offTheWay = Math.abs(x - WELL.x()) > WAY + reach || z > WELL.z();
            final boolean apart = houses.stream().allMatch(h -> Math.hypot(h.place().x() - x, h.place().z() - z)
                    > reach + EarthHouse.of(h.size()).eave() + APART);
            final Material walled = EarthHouse.WALLS.get(random.nextInt(EarthHouse.WALLS.size()));
            final Material roof = EarthHouse.ROOFS.get(random.nextInt(EarthHouse.ROOFS.size()));
            final Material window = random.nextDouble() < 0.3 ? Material.GLASS : Material.TIMBER;
            final Material wood = random.nextDouble() < 0.2 ? Material.PAINTED : Material.TIMBER;
            if (!dry || !offTheWay || !apart) {
                continue;
            }
            // its door facing the well
            final double facing = Math.toDegrees(Math.atan2(WELL.x() - x, WELL.z() - z));
            houses.add(new House(size, walled, roof, window, wood, new Place(x, z, 0, facing)));
        }
        return List.copyOf(houses);
    }

    private static final Map<Integer, List<Solid>> DRESSED = new ConcurrentHashMap<>();

    /** The church's key among the village's: after its houses'. */
    public static final int CHURCH = HOUSES;

    /** House {@code k}, or the church ({@link #CHURCH}), dressed: of what it is built. Worked out once, when first wanted. */
    public static List<Solid> solids(final int k) {
        return DRESSED.computeIfAbsent(k, i -> {
            if (i == CHURCH) {
                final Building church = Church.standard().building();
                return church.dressed(church.own(), Palette.STONE);
            }
            final House house = VILLAGE.get(i);
            return EarthHouse.of(house.size()).solids(EarthHouse.palette().with(Map.of(Role.WALL, house.walls(),
                    Role.SPIRE, house.walls(), Role.ROOF, house.roof(), Role.GLAZING, house.window(),
                    Role.FRAME, house.woodwork())));
        });
    }

    /**
     * The river's channel, cut into the meadow: its bed so wide, level, and so deep; its banks rising from
     * it to the meadow as far out as the river and its banks go. Its water so deep over its bed.
     */
    public static final double BED = 900;
    public static final double DEPTH = 150;
    public static final double WATER = 100;

    /** The river's channel, along its line. */
    static Channel channel() {
        return new Channel(line(), BED, RIVER / 2 + BANK - BED / 2, DEPTH);
    }

    // --- its roads ---------------------------------------------------------------------------------------------

    /**
     * The kinds of road in the meadow ({@link RoadMap}): each of the highway's carriageways, a motorway's, a hard
     * strip by the median, two lanes of 3.65 m, a hard shoulder of 3.3 m on its outside; its slip roads, a strip,
     * one lane, a shoulder; the road into the village, a lane each way; the lane to the church, unmarked gravel.
     */
    static final RoadMap.Kind CARRIAGEWAY = new RoadMap.Kind("carriageway", 1_160, Land.Cover.ASPHALT, 0.05,
            1.0 / 20_000, false, 2, 0.02, 6_000, 10_000, 2, 365, 100);
    static final RoadMap.Kind SLIP = new RoadMap.Kind("slip", 565, Land.Cover.ASPHALT, 0.07, 1.0 / 4_000, false,
            2, 0.02, 3_000, 4_000, 1, 365, 100);
    static final RoadMap.Kind VILLAGE_ROAD = new RoadMap.Kind("village", 600, Land.Cover.ASPHALT, 0.08, 1.0 / 3_000, false,
            2, 0.02, 3_000, 5_000, 2, 300, 0);
    static final RoadMap.Kind LANE = new RoadMap.Kind("lane", 400, Land.Cover.GRAVEL, 0.14, 1.0 / 1_500, false,
            2, 0, 1_200, 3_000);

    /** The highway's vertices, so far apart along it; how far it runs past the world's ends; how far south of the river's mean line it runs. */
    private static final double HIGHWAY_EVERY = 10_000;
    private static final double HIGHWAY_PAST = 20_000;
    private static final double HIGHWAY_SOUTH = 10_000;
    /** How wide its median is, between its carriageways: so far apart their middles are. */
    public static final double MEDIAN = 1_000;
    public static final double CARRIAGEWAYS_APART = MEDIAN + 1_160;
    /**
     * Where the exit to the village leaves the westbound carriageway, and the entry back onto it joins it: so far
     * east, each a vertex of it; and where the slip roads meet the road into the village, the junction: so far
     * east, so far north of the westbound carriageway.
     */
    private static final double EXIT = 26_000;
    private static final double ENTRY = -24_000;
    private static final double JUNCTION = 1_000;
    private static final double JUNCTION_OUT = 4_500;
    /** The exit slip road's way out from the carriageway: so far along, so far out, each in turn. */
    private static final double[][] EXITING = {{-4_000, 400}, {-9_000, 1_000}, {-14_000, 2_000}, {-19_000, 3_300}};

    /**
     * Where the highway's middle is, north or south, at {@code x} east: along the river, a hundred metres south of
     * it, its bends, long and short, two and a half times less wide; so about two and a half times less winding.
     */
    public static double highwayAt(final double x) {
        return 1_600 * Math.sin(x / 18_000 + 0.3) + 600 * Math.sin(x / 7_000 + 1.3) - 6_600 - HIGHWAY_SOUTH;
    }

    /** Which way is north of the highway at {@code x}, square to it: to the left going east. */
    private static Vec2 northOf(final double x) {
        final double slope = 1_600 / 18_000.0 * Math.cos(x / 18_000 + 0.3) + 300 / 3_500.0 * Math.cos(x / 7_000 + 1.3);
        return unit(-slope, 1);
    }

    /** The point {@code out} north of the highway's middle at {@code x}, square to it. */
    private static Vec2 offHighway(final double x, final double out) {
        final Vec2 n = northOf(x);
        return new Vec2(x + n.x() * out, highwayAt(x) + n.z() * out);
    }

    /** The point {@code out} north of the westbound carriageway's middle at {@code x}. */
    private static Vec2 offWestbound(final double x, final double out) {
        return offHighway(x, CARRIAGEWAYS_APART / 2 + out);
    }

    /** How far from the river's middle the bridge's ends are; how high its deck is over the meadow; its parapets. */
    private static final double SPAN = 1_300;
    public static final double DECK = 80;
    private static final double DECK_DEEP = 60;
    private static final double PARAPET = 90;
    private static final double PARAPET_THICK = 30;
    /** How far short of the well the road into the village ends: the village's square. */
    private static final double SQUARE = 1_200;

    /** Which way across the river the bridge goes: square across it, northward. */
    private static Vec2 across() {
        final Vec3 a = Num.heading(riverHeading(WELL.x()) - 90);
        return new Vec2(a.x(), a.z());
    }

    /**
     * The meadow's roads, as described:
     *
     * <ul>
     *   <li>the highway along the river, a dual carriageway: its eastbound carriageway south of its median, its
     *       westbound north of it, each two lanes, a vertex every {@link #HIGHWAY_EVERY}, laid along a smooth curve
     *       (driving on the right, each runs the way its traffic does);</li>
     *   <li>the exit to the village: a slip road leaving the westbound carriageway at node {@code exit}, easing out
     *       from it, then curving round to the junction ({@code junction}) north of it; and the entry, a slip road
     *       from the junction curving round to ease back onto the carriageway at node {@code entry};</li>
     *   <li>the road into the village, from the junction round to cross the river square on a bridge, its ends
     *       pinned over the meadow, and on up to the village's square;</li>
     *   <li>the lane from the village's edge winding up to the church.</li>
     * </ul>
     */
    public static RoadMap roads() {
        final RoadMap.Node exit = new RoadMap.Node("exit", offWestbound(EXIT, 0));
        final RoadMap.Node entry = new RoadMap.Node("entry", offWestbound(ENTRY, 0));
        final RoadMap.Node junction = new RoadMap.Node("junction", offWestbound(JUNCTION, JUNCTION_OUT));
        final List<RoadMap.Way> ways = new ArrayList<>();
        // the carriageways, either side of the highway's middle
        final List<Vec2> east = new ArrayList<>();
        final List<Vec2> west = new ArrayList<>();
        final double reach = Body.EDGE + HIGHWAY_PAST;
        for (double x = EXIT - Math.floor((EXIT + reach) / HIGHWAY_EVERY) * HIGHWAY_EVERY; x <= reach; x += HIGHWAY_EVERY) {
            east.add(offHighway(x, -CARRIAGEWAYS_APART / 2));
            west.addFirst(offWestbound(x, 0));
        }
        ways.add(smooth("eastbound", CARRIAGEWAY.name(), east, List.of()));
        ways.add(smooth("westbound", CARRIAGEWAY.name(), west, List.of(exit, entry)));
        // the exit: easing out from the westbound carriageway, then round to the junction
        final List<Vec2> out = new ArrayList<>();
        out.add(exit.at());
        for (final double[] step : EXITING) {
            out.add(offWestbound(EXIT + step[0], step[1]));
        }
        out.add(junction.at());
        ways.add(smooth("exit", SLIP.name(), out, List.of(exit, junction)));
        // the road into the village: round from the junction to the bridge's south end, across, round up to the square
        final Vec2 across = across();
        final Vec2 middle = new Vec2(WELL.x(), riverAt(WELL.x()));
        final Vec2 south = new Vec2(middle.x() - across.x() * SPAN, middle.z() - across.z() * SPAN);
        final Vec2 north = new Vec2(middle.x() + across.x() * SPAN, middle.z() + across.z() * SPAN);
        final Vec2 square = new Vec2(WELL.x(), WELL.z() - SQUARE);
        final double toSouth = -Alignment.bending(south, new Vec2(-across.x(), -across.z()), junction.at());
        final double toSquare = Alignment.bending(north, across, square);
        ways.add(new RoadMap.Way("village road", List.of(
                new RoadMap.Vertex(junction.at(), Double.NaN, junction.name()),
                new RoadMap.Vertex(south, DECK, null),
                new RoadMap.Vertex(north, DECK, null),
                new RoadMap.Vertex(square)), List.of(
                new RoadMap.Segment(VILLAGE_ROAD.name(), toSouth, false),
                new RoadMap.Segment(VILLAGE_ROAD.name(), 0, true),
                new RoadMap.Segment(VILLAGE_ROAD.name(), toSquare, false)), false));
        // the entry: from the junction round, then easing back onto the westbound carriageway
        final List<Vec2> in = new ArrayList<>();
        in.add(junction.at());
        for (int k = EXITING.length - 1; k >= 0; k--) {
            in.add(offWestbound(ENTRY - EXITING[k][0], EXITING[k][1]));
        }
        in.add(entry.at());
        ways.add(smooth("entry", SLIP.name(), in, List.of(junction, entry)));
        ways.add(smooth("church lane", LANE.name(), churchLane(), List.of()));
        return new RoadMap(List.of(CARRIAGEWAY, SLIP, VILLAGE_ROAD, LANE), List.of(exit, entry, junction), ways);
    }

    /**
     * A road of kind {@code kind} through {@code points}, laid along a smooth curve through them, with no kink: at
     * each point heading the way the curve goes there (from the point before it to the one after), and between
     * each two, two arcs (a biarc), each a segment, meeting at a vertex between them where they head the same way.
     * A point at one of {@code nodes} is that node.
     */
    private static RoadMap.Way smooth(final String name, final String kind, final List<Vec2> points,
                                      final List<RoadMap.Node> nodes) {
        final List<Vec2> headings = new ArrayList<>();
        for (int k = 0; k < points.size(); k++) {
            final Vec2 before = points.get(Math.max(0, k - 1));
            final Vec2 after = points.get(Math.min(points.size() - 1, k + 1));
            headings.add(unit(after.x() - before.x(), after.z() - before.z()));
        }
        return smooth(name, kind, new Through(points, headings), nodes);
    }

    /** As {@link #smooth(String, String, List, List)}, heading at each point as {@code through} has it. */
    private static RoadMap.Way smooth(final String name, final String kind, final Through through,
                                      final List<RoadMap.Node> nodes) {
        final List<Vec2> points = through.points();
        final List<Vec2> headings = through.headings();
        final List<RoadMap.Vertex> vertices = new ArrayList<>();
        final List<RoadMap.Segment> segments = new ArrayList<>();
        for (int k = 0; k < points.size(); k++) {
            final Vec2 p = points.get(k);
            final RoadMap.Node node = nodes.stream().filter(n -> n.at().equals(p)).findFirst().orElse(null);
            vertices.add(new RoadMap.Vertex(p, Double.NaN, node == null ? null : node.name()));
            if (k + 1 == points.size()) {
                break;
            }
            final Vec2 q = points.get(k + 1);
            final Vec2 joint = biarc(p, headings.get(k), q, headings.get(k + 1));
            if (joint == null) {
                segments.add(new RoadMap.Segment(kind, Alignment.bending(p, headings.get(k), q), false));
                continue;
            }
            final double first = Alignment.bending(p, headings.get(k), joint);
            vertices.add(new RoadMap.Vertex(joint));
            segments.add(new RoadMap.Segment(kind, first, false));
            segments.add(new RoadMap.Segment(kind, Alignment.bending(joint, Alignment.arriving(p, joint, first), q), false));
        }
        return new RoadMap.Way(name, vertices, segments, false);
    }

    /**
     * Where two arcs from {@code p} heading {@code tp} to {@code q} heading {@code tq} meet, heading the same way
     * (the joint of a biarc, its two tangents as long as each other); null if one arc does (or a straight).
     */
    private static Vec2 biarc(final Vec2 p, final Vec2 tp, final Vec2 q, final Vec2 tq) {
        final double vx = q.x() - p.x();
        final double vz = q.z() - p.z();
        final double vv = vx * vx + vz * vz;
        final double vt = vx * (tp.x() + tq.x()) + vz * (tp.z() + tq.z());
        final double cos = tp.x() * tq.x() + tp.z() * tq.z();
        final double d;
        if (1 - cos < 1e-9) {
            final double along = vx * tq.x() + vz * tq.z();
            if (along <= 0 || Math.abs(vx * tp.z() - vz * tp.x()) < 1e-6 * Math.sqrt(vv)) {
                return null;
            }
            d = vv / (4 * along);
        } else {
            d = (-vt + Math.sqrt(vt * vt + 2 * (1 - cos) * vv)) / (2 * (1 - cos));
        }
        return new Vec2((p.x() + d * tp.x() + q.x() - d * tq.x()) / 2, (p.z() + d * tp.z() + q.z() - d * tq.z()) / 2);
    }

    private static Vec2 unit(final double x, final double z) {
        final double d = Math.hypot(x, z);
        return new Vec2(x / d, z / d);
    }

    /** How far out from the well the church lane starts, at the least: past the houses. */
    private static final double LANE_FROM = 7_000;
    /** How far it winds out either side of the way straight up to the church, at each of its bends. */
    private static final double[] WINDING = {0, 2_000, -2_400, 2_200, -1_600, 800, 0};

    /**
     * The church lane's vertices: from the village's edge, out past its houses toward the church's hill, winding
     * one way and the other up to the churchyard's edge. It starts where none of the houses is near it.
     */
    private static Through churchLane() {
        final Vec2 hill = Terrains.CHURCH_HILL;
        final double dx = hill.x() - WELL.x();
        final double dz = hill.z() - WELL.z();
        final double away = Math.hypot(dx, dz);
        final double ux = dx / away;
        final double uz = dz / away;
        double from = LANE_FROM;
        while (from < away / 2 && clearOfHouses(WELL.x() + ux * from, WELL.z() + uz * from) < LANE.width() / 2 + 600) {
            from += 500;
        }
        final Vec2 start = new Vec2(WELL.x() + ux * from, WELL.z() + uz * from);
        final Vec2 end = new Vec2(hill.x() - ux * (Terrains.CHURCHYARD - 300), hill.z() - uz * (Terrains.CHURCHYARD - 300));
        final List<Vec2> bends = new ArrayList<>();
        for (int k = 0; k < WINDING.length; k++) {
            final double t = (double) k / (WINDING.length - 1);
            bends.add(new Vec2(start.x() + (end.x() - start.x()) * t - uz * WINDING[k],
                    start.z() + (end.z() - start.z()) * t + ux * WINDING[k]));
        }
        return through(bends);
    }

    /** Points along a smooth curve, and which way it heads at each. */
    private record Through(List<Vec2> points, List<Vec2> headings) {
    }

    /**
     * A point every so far along a smooth curve through {@code bends} (a Catmull-Rom curve), and which way the
     * curve heads at each.
     */
    private static Through through(final List<Vec2> bends) {
        final List<Vec2> points = new ArrayList<>();
        final List<Vec2> headings = new ArrayList<>();
        for (int k = 0; k + 1 < bends.size(); k++) {
            final Vec2 p0 = bends.get(Math.max(0, k - 1));
            final Vec2 p1 = bends.get(k);
            final Vec2 p2 = bends.get(k + 1);
            final Vec2 p3 = bends.get(Math.min(bends.size() - 1, k + 2));
            final int steps = Math.max(1, (int) Math.ceil(Math.hypot(p2.x() - p1.x(), p2.z() - p1.z()) / LANE_EVERY));
            for (int s = 0; s < steps; s++) {
                final double t = (double) s / steps;
                points.add(catmullRom(p0, p1, p2, p3, t));
                headings.add(catmullRomHeading(p0, p1, p2, p3, t));
            }
            if (k + 2 == bends.size()) {
                points.add(p2);
                headings.add(catmullRomHeading(p0, p1, p2, p3, 1));
            }
        }
        return new Through(points, headings);
    }

    /** Which way a Catmull-Rom curve heads {@code t} of the way from {@code p1} to {@code p2}. */
    private static Vec2 catmullRomHeading(final Vec2 p0, final Vec2 p1, final Vec2 p2, final Vec2 p3, final double t) {
        final double x = 0.5 * ((-p0.x() + p2.x()) + 2 * (2 * p0.x() - 5 * p1.x() + 4 * p2.x() - p3.x()) * t
                + 3 * (-p0.x() + 3 * p1.x() - 3 * p2.x() + p3.x()) * t * t);
        final double z = 0.5 * ((-p0.z() + p2.z()) + 2 * (2 * p0.z() - 5 * p1.z() + 4 * p2.z() - p3.z()) * t
                + 3 * (-p0.z() + 3 * p1.z() - 3 * p2.z() + p3.z()) * t * t);
        return unit(x, z);
    }

    /** How far apart the church lane's vertices are, about. */
    private static final double LANE_EVERY = 3_500;

    private static Vec2 catmullRom(final Vec2 p0, final Vec2 p1, final Vec2 p2, final Vec2 p3, final double t) {
        final double t2 = t * t;
        final double t3 = t2 * t;
        return new Vec2(0.5 * (2 * p1.x() + (-p0.x() + p2.x()) * t + (2 * p0.x() - 5 * p1.x() + 4 * p2.x() - p3.x()) * t2
                + (-p0.x() + 3 * p1.x() - 3 * p2.x() + p3.x()) * t3),
                0.5 * (2 * p1.z() + (-p0.z() + p2.z()) * t + (2 * p0.z() - 5 * p1.z() + 4 * p2.z() - p3.z()) * t2
                        + (-p0.z() + 3 * p1.z() - 3 * p2.z() + p3.z()) * t3));
    }

    /** How far ({@code x}, {@code z}) is from the nearest house's eave. */
    private static double clearOfHouses(final double x, final double z) {
        double clear = Double.POSITIVE_INFINITY;
        for (final House h : VILLAGE) {
            clear = Math.min(clear, Math.hypot(h.place().x() - x, h.place().z() - z) - EarthHouse.of(h.size()).eave());
        }
        return clear;
    }

    /**
     * The meadow's land: its lie ({@link Terrains#meadow}), surveyed; the river's channel cut into it
     * ({@link Channel}); its roads laid over that, in turn ({@link #roads}). Worked out once, when first wanted.
     */
    public static Land land() {
        return Meadow.LAND;
    }

    /** The meadow's lie alone, surveyed, under its features. */
    public static Survey lie() {
        return Meadow.LIE;
    }

    /** Its roads, laid: the highway, the road into the village, the church lane. */
    public static List<Road> laid() {
        return Meadow.ROADS;
    }

    /** Road {@code name}, laid. */
    public static Road laid(final String name) {
        return Meadow.ROADS.stream().filter(r -> r.name().equals(name)).findFirst().orElseThrow();
    }

    private static final class Meadow {
        static final Survey LIE = Terrains.meadow().survey();
        static final Land CHANNELLED = new Land(LIE, List.of(channel()));
        static final List<Road> ROADS = roads().laid(CHANNELLED, Alignment.STEP);
        static final Land LAND = CHANNELLED.with(ROADS);
        /** Beside the roads, where no tree grows: each one's top, its side slopes, and so much more. */
        static final List<Polyline> ROADSIDES = ROADS.stream()
                .map(r -> new Polyline(r.line(), r.wide(0) / 2 + ROADSIDE)).toList();
    }

    /** How far clear of a road's edges, and of the churchyard, trees keep. */
    private static final double ROADSIDE = 800;
    private static final double CLEAR = 300;

    /** The pond in the hollow up the slope east of the village: filled to a metre under the hollow's rim. */
    public static List<Pond> ponds() {
        return List.of(new Pond(Terrains.hollow().loop(), Terrains.hollow().height() - POND_DOWN));
    }

    private static final double POND_DOWN = 100;

    /**
     * Where the avatar starts: beside the road into the village, on its west, so far south of the river, facing
     * north, the bridge ahead.
     */
    public static Vec3 start() {
        final Road road = laid("village road");
        final List<Vec2> line = road.line();
        final double z = riverAt(WELL.x()) - STARTS_SOUTH;
        int best = 0;
        for (int i = 0; i < line.size(); i++) {
            if (Math.abs(line.get(i).z() - z) < Math.abs(line.get(best).z() - z)) {
                best = i;
            }
        }
        final Vec2 p = line.get(best);
        final Vec2 q = line.get(Math.min(line.size() - 1, best + 1));
        final double d = Math.hypot(q.x() - p.x(), q.z() - p.z());
        // to its left, going along it
        final double lx = -(q.z() - p.z()) / d;
        final double lz = (q.x() - p.x()) / d;
        final double out = road.wide(best) / 2 + STARTS_ASIDE;
        return new Vec3(p.x() + lx * out, 0, p.z() + lz * out);
    }

    private static final double STARTS_SOUTH = 3_500;
    private static final double STARTS_ASIDE = 500;

    /** Tells the simulation all of the meadow village: what stands in it, its river and pond, where the avatar starts, its land. */
    public static void tell() {
        Course.village(Village::pieces, Village::rivers, Village::ponds, Village::start, Village::land);
    }

    /** What stands in the meadow: its houses, each one piece; the well; the footbridge; the trees. */
    public static List<Course.Piece> pieces() {
        final List<Course.Piece> pieces = new ArrayList<>();
        for (int k = 0; k < VILLAGE.size(); k++) {
            final House house = VILLAGE.get(k);
            pieces.add(Town.building(solids(k), house.place(), KEYS + k, EarthHouse.of(house.size()).shape(), 1));
        }
        pieces.add(church());
        pieces.addAll(well());
        pieces.addAll(bridge());
        pieces.addAll(trees());
        pieces.addAll(avenue());
        pieces.addAll(parked());
        return pieces;
    }

    /** Where the church stands: the middle of its hill's level top, as high as that, its front to where the road comes up. */
    public static Place churchPlace() {
        final Vec2 hill = Terrains.CHURCH_HILL;
        final List<Vec2> lane = laid("church lane").line();
        final Vec2 end = lane.getLast();
        return new Place(hill.x(), hill.z(), Terrains.CHURCHYARD_LEVEL,
                Math.toDegrees(Math.atan2(end.x() - hill.x(), end.z() - hill.z())));
    }

    /** The church on its hill: the town's, as first drawn, of stone. */
    private static Course.Piece church() {
        return Town.building(solids(CHURCH), churchPlace(), KEYS + CHURCH, Church.standard().shape(), 1);
    }

    /** Where the car and the motorcycle are parked: so far west of the start, away from the road, so far ahead of it. */
    private static final double CAR_ASIDE = -400;
    private static final double BIKE_ASIDE = -800;
    private static final double PARKED_AHEAD = 400;

    /**
     * A car (the roadster) and a motorcycle (the sport bike), parked side by side just ahead of where the
     * avatar starts, facing the river: on the land, as high as it is there.
     */
    public static List<Course.Piece> parked() {
        final Vec3 start = start();
        final List<Course.Piece> parked = new ArrayList<>();
        final Object[][] vehicles = {{Course.CAR_CHASSIS, Course.ROADSTER, CAR_ASIDE}, {Course.SPORT_BIKE, null, BIKE_ASIDE}};
        for (final Object[] v : vehicles) {
            final Course.Chassis chassis = (Course.Chassis) v[0];
            final double x = start.x() + (double) v[2];
            final double z = start.z() + PARKED_AHEAD;
            parked.add(Course.Piece.vehicle(chassis, (Course.Bodywork) v[1],
                    new Place(x, z, land().height(x, z) + chassis.ride(), 0), 0));
        }
        return parked;
    }

    /** The well: a ring of stone in the way, its water within; two posts and a beam over it. */
    private static List<Course.Piece> well() {
        final List<Course.Piece> well = new ArrayList<>();
        well.add(Course.Piece.shaped(new Block(WELL.x(), WELL.z(), WELL_RING, WELL_RING, 0, 0, WELL_HIGH), Course.Kind.GARDEN,
                Course.GARDEN_STONE, List.of(), List.of(new Prism(circle(WELL_RING), List.of(circle(WELL_INSIDE)), 0, WELL_HIGH)),
                new Place(WELL.x(), WELL.z(), 0, 0)));
        well.add(new Course.Piece(new Block(WELL.x(), WELL.z(), WELL_INSIDE, WELL_INSIDE, 0, 0, WELL_HIGH - 30),
                Course.Kind.GARDEN, Course.GARDEN_WATER, List.of()));
        for (final double side : new double[] {-1, 1}) {
            final Block post = Block.standing(WELL.x() + side * (WELL_RING - 6), WELL.z(), 10, 10, WELL_HIGH + 110, 0);
            well.add(new Course.Piece(post, Course.Kind.WALL, TIMBER, List.of(post)));
        }
        final Block beam = new Block(WELL.x(), WELL.z(), WELL_RING, 6, 0, WELL_HIGH + 100, WELL_HIGH + 112);
        well.add(new Course.Piece(beam, Course.Kind.WALL, TIMBER, List.of(beam)));
        return well;
    }

    private static List<Vec2> circle(final double r) {
        final List<Vec2> circle = new ArrayList<>();
        for (int k = 0; k < 32; k++) {
            final double a = 2 * Math.PI * k / 32;
            circle.add(new Vec2(WELL.x() + r * Math.sin(a), WELL.z() + r * Math.cos(a)));
        }
        return circle;
    }

    /** How far past each end of the bridge's segment its deck goes, onto the road either side. */
    private static final double DECK_PAST = 100;

    /**
     * The bridge carrying the road into the village across the river: built on its bridge segment, as the road
     * worked out has it there. Its deck as wide as the road and its parapets, as high as the road over it,
     * so deep, and a little past each end onto the road; a stone parapet along each side.
     */
    private static List<Course.Piece> bridge() {
        final Road road = laid("village road");
        final List<Vec2> line = road.line();
        int first = -1;
        int last = -1;
        for (int i = 0; i < line.size(); i++) {
            if (road.bridged(i)) {
                first = first < 0 ? i : first;
                last = i;
            }
        }
        // its segment ends at the vertex after its last point on it
        last = Math.min(line.size() - 1, last + 1);
        final Vec2 a = line.get(first);
        final Vec2 b = line.get(last);
        final double long_ = Math.hypot(b.x() - a.x(), b.z() - a.z());
        final double yaw = Math.toDegrees(Math.atan2(b.x() - a.x(), b.z() - a.z()));
        final double level = (road.level(first) + road.level(last)) / 2;
        final double half = road.wide(first) / 2;
        final double mx = (a.x() + b.x()) / 2;
        final double mz = (a.z() + b.z()) / 2;
        final List<Course.Piece> bridge = new ArrayList<>();
        final Block deck = new Block(mx, mz, half + PARAPET_THICK, long_ / 2 + DECK_PAST, yaw, level - DECK_DEEP, level);
        bridge.add(new Course.Piece(deck, Course.Kind.ROAD, 0, List.of(deck)));
        // to its side, going along it
        final double sx = (b.z() - a.z()) / long_;
        final double sz = -(b.x() - a.x()) / long_;
        for (final double side : new double[] {-1, 1}) {
            final double out = side * (half + PARAPET_THICK / 2);
            final Block parapet = new Block(mx + sx * out, mz + sz * out, PARAPET_THICK / 2, long_ / 2 + DECK_PAST, yaw,
                    level, level + PARAPET);
            bridge.add(new Course.Piece(parapet, Course.Kind.WALL, 0, List.of(parapet)));
        }
        return bridge;
    }

    /**
     * The highway's median planted: two lines of trees down it, so far either side of its middle, a tree every so
     * far, so far east and west of the village.
     */
    public static final double AVENUE_ROW = 250;
    private static final double AVENUE_EVERY = 2_000;
    private static final double AVENUE_REACH = 250_000;

    /** The two lines of trees down the highway's median, between its carriageways, each standing on the land. */
    private static List<Course.Piece> avenue() {
        final List<Course.Piece> trees = new ArrayList<>();
        for (double x = -AVENUE_REACH; x <= AVENUE_REACH; x += AVENUE_EVERY) {
            for (final double row : new double[] {-AVENUE_ROW, AVENUE_ROW}) {
                trees.add(tree(offHighway(x, row), 1));
            }
        }
        return trees;
    }

    /** A tree at {@code at}, of kind {@code kind}, its foot as low as the ground under it goes, so it is never clear of it. */
    private static Course.Piece tree(final Vec2 at, final int kind) {
        double foot = Double.POSITIVE_INFINITY;
        for (final double dx : new double[] {-TRUNK / 2, TRUNK / 2}) {
            for (final double dz : new double[] {-TRUNK / 2, TRUNK / 2}) {
                foot = Math.min(foot, land().height(at.x() + dx, at.z() + dz));
            }
        }
        final Block trunk = new Block(at.x(), at.z(), TRUNK / 2, TRUNK / 2, 0, foot, foot + 320);
        return new Course.Piece(trunk, Course.Kind.TREE, kind, List.of(trunk));
    }

    /** How near the world's middle trees grow; how far apart along the banks; how many across the meadow. */
    private static final double WOODED = 80_000;
    private static final double ALONG = 2_500;
    private static final int MEADOW_TREES = 30;
    /** How wide a tree's trunk is. */
    private static final double TRUNK = 36;

    /**
     * Its trees: along both banks, every so far, a little in and out; and across the meadow here and there;
     * none in the water, by the bridge, or by a house.
     */
    private static List<Course.Piece> trees() {
        final SplittableRandom random = new SplittableRandom(SEED + 1);
        final List<Vec2> at = new ArrayList<>();
        for (double x = -WOODED; x <= WOODED; x += ALONG) {
            for (final double side : new double[] {-1, 1}) {
                final double tx = x + (random.nextDouble() - 0.5) * ALONG * 0.6;
                final double out = RIVER / 2 + BANK + 300 + 600 * random.nextDouble();
                at.add(new Vec2(tx, riverAt(tx) + side * out));
            }
        }
        for (int k = 0; k < MEADOW_TREES; k++) {
            final Vec3 way = Num.heading(360 * random.nextDouble());
            final double out = 15_000 + 45_000 * random.nextDouble();
            at.add(new Vec2(WELL.x() + way.x() * out, WELL.z() + way.z() * out));
        }
        final List<Course.Piece> trees = new ArrayList<>();
        int k = 0;
        for (final Vec2 t : at) {
            final boolean wet = Math.abs(t.z() - riverAt(t.x())) < RIVER / 2 + BANK + 150;
            final boolean byTheBridge = Math.abs(t.x() - WELL.x()) < 1_500 && Math.abs(t.z() - riverAt(t.x())) < 3_000;
            final boolean byAHouse = VILLAGE.stream().anyMatch(h -> Math.hypot(h.place().x() - t.x(), h.place().z() - t.z())
                    < EarthHouse.of(h.size()).eave() + 500);
            final boolean byTheWell = Math.hypot(t.x() - WELL.x(), t.z() - WELL.z()) < 1_000;
            final boolean byTheRoad = Meadow.ROADSIDES.stream().anyMatch(r -> r.distance(t.x(), t.z()) < Double.POSITIVE_INFINITY);
            final boolean inTheChurchyard = Math.hypot(t.x() - Terrains.CHURCH_HILL.x(), t.z() - Terrains.CHURCH_HILL.z())
                    < Terrains.CHURCHYARD + CLEAR;
            if (wet || byTheBridge || byAHouse || byTheWell || byTheRoad || inTheChurchyard) {
                continue;
            }
            // standing on the ground there: its foot as low as the ground under it goes, so it is never clear of it
            double foot = Double.POSITIVE_INFINITY;
            for (final double dx : new double[] {-TRUNK / 2, TRUNK / 2}) {
                for (final double dz : new double[] {-TRUNK / 2, TRUNK / 2}) {
                    foot = Math.min(foot, land().height(t.x() + dx, t.z() + dz));
                }
            }
            final Block trunk = new Block(t.x(), t.z(), TRUNK / 2, TRUNK / 2, 0, foot, foot + 320);
            trees.add(new Course.Piece(trunk, Course.Kind.TREE, k++ % 2, List.of(trunk)));
        }
        return trees;
    }
}
