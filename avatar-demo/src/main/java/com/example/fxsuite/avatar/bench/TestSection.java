package com.example.fxsuite.avatar.bench;

import com.example.fxsuite.avatar.bench.kit.Guest;
import com.example.fxsuite.avatar.bench.kit.PlayWorld;
import com.example.fxsuite.avatar.bench.kit.Spot;
import com.example.fxsuite.avatar.look.RoadScenery;
import com.example.fxsuite.avatar.look.Scenery;
import com.example.fxsuite.avatar.roads.Decks;
import com.example.fxsuite.avatar.roads.Roadway;
import com.example.fxsuite.avatar.roads.Roadworks;
import com.example.fxsuite.avatar.roads.Section;
import com.example.fxsuite.avatar.roads.Strip;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Land;
import com.example.fxsuite.avatar.sim.Place;
import com.example.fxsuite.avatar.sim.Pond;
import com.example.fxsuite.avatar.sim.Terrain;
import com.example.fxsuite.avatar.sim.Vec2;

import java.util.ArrayList;
import java.util.List;

/**
 * A scene of the roads bench as a world to play ({@link PlayWorld}): its roads as laid ({@link Roadworks}), of the kind
 * across the bench has now, to drive in, with whoever the bench brings.
 *
 * <ul>
 *   <li><b>its land</b>: the ground as the roads leave it, and under each pavement where the ground carries it, the
 *       pavement's top a little lower than drawn ({@link #SINK}): what wheels and feet stand on;</li>
 *   <li><b>its structures</b> in the way ({@link Decks}): decks to drive over, parapets, piers;</li>
 *   <li><b>its water</b>, as the scene has it;</li>
 *   <li><b>its spots</b>: vehicles parked one after another along the first road's leftmost lane from 20 m on, nose to
 *       tail with room between, facing along it; the avatar starting just behind them; people standing along the
 *       road's far edge from beside the avatar, facing across at the vehicles;</li>
 *   <li><b>drawn</b>: the roads' pavements, markings and walls wanted, by the roads' own drawing
 *       ({@link RoadScenery}); their structures as buildings are, by their keys; the land and the water as the game
 *       draws any. A tunnel's lining not yet.</li>
 * </ul>
 */
final class TestSection implements PlayWorld {

    /** How far under the pavement as drawn its top is stood on: out of sight under it. */
    static final double SINK = 3;
    /** The keys its structures are drawn by, as buildings are. */
    static final int KEYS = 900_000;
    /** The land chassis it brings unless asked otherwise, in the order parked. */
    static final List<Course.Chassis> VEHICLES = List.of(Course.CAR_CHASSIS, Course.PICKUP_CHASSIS, Course.TRUCK_CHASSIS,
            Course.SCOOTER, Course.SPORT_BIKE, Course.CRUISER, Course.BOXER, Course.BOXER_SIDECAR);
    /** Where along the first road the first is parked, how far apart their middles are at least, and the room between. */
    static final double FIRST = 2_000;
    static final double APART = 900;
    static final double ROOM = 200;
    /** How far behind the first vehicle's spot the avatar starts; how far apart people stand, and in from the far edge. */
    static final double BEHIND = 600;
    static final double PEOPLE_APART = 150;
    static final double EDGE_IN = 100;

    private final RoadScenes.Scene scene;
    private final Section section;
    private final Roadworks works;
    /** What is stood on: the land's one feature. */
    private final Land.Layer footing;
    /** Whether its pavements are things of their own, laid over the land and the decks, or in the land. */
    private final boolean ownPavement;

    TestSection(final RoadScenes.Scene scene, final Section section, final Roadworks works) {
        this(scene, section, works, true);
    }

    /** The same; {@code ownPavement}: its pavements things of their own ({@link Roadway#pavement}), or in the land. */
    TestSection(final RoadScenes.Scene scene, final Section section, final Roadworks works, final boolean ownPavement) {
        this.scene = scene;
        this.section = section;
        this.works = works;
        this.ownPavement = ownPavement;
        this.footing = works.land(ownPavement ? Double.NaN : SINK);
    }

    /** The vehicles it brings unless asked otherwise: one of each land chassis. */
    static List<Guest> guests() {
        return VEHICLES.stream().map(c -> (Guest) new Guest.Vehicle(c)).toList();
    }

    @Override
    public String title() {
        return "Test section · " + scene.name() + " · " + section.name() + (ownPavement ? "" : " · pavement in the land");
    }

    @Override
    public String description() {
        return "The roads bench's " + scene.name() + " scene, its roads of kind \"" + section.name()
                + "\", to drive on.";
    }

    @Override
    public void lay(final Course.Layout world) {
        // its survey level ground in whole steps, out to the roads' reach and some
        final double every = 1_000;
        final double[] box = works.bounds(5_000);
        for (int k = 0; k < 4; k++) {
            box[k] = (k % 2 == 0 ? Math.floor(box[k] / every) : Math.ceil(box[k] / every)) * every;
        }
        final Terrain flat = new Terrain("test section", box[0], box[2], box[1], box[3], every, 0, 0, Terrain.Noise.NONE,
                List.of(), List.of());
        final Land land = new Land(flat.survey(), List.of(footing));
        world.land(land).pieces(Decks.pieces(works, KEYS, ownPavement)).ruled(false);
        if (ownPavement) {
            world.ways(works.roads().stream().flatMap(r -> r.pavements().stream())
                    .map(p -> (com.example.fxsuite.avatar.sim.Volume) p).toList());
        }
        if (scene.watered()) {
            world.ponds(List.of(new Pond(List.of(new Vec2(box[0], box[2]), new Vec2(box[1], box[2]),
                    new Vec2(box[1], box[3]), new Vec2(box[0], box[3])), scene.water())));
        }
    }

    @Override
    public Scenery scenery() {
        return RoadScenery.of(works);
    }

    /** Just behind the first vehicle's spot, in its lane, on the road, facing along it. */
    @Override
    public Spot start() {
        return lane("start", FIRST - BEHIND);
    }

    /** Along the first road's leftmost lane from {@link #FIRST}, each {@link #APART} after the last or more, if long. */
    @Override
    public List<Spot> parked(final List<Double> lengths) {
        final List<Spot> spots = new ArrayList<>();
        double along = FIRST;
        for (int k = 0; k < lengths.size(); k++) {
            if (k > 0) {
                along += Math.max(APART, (lengths.get(k - 1) + lengths.get(k)) / 2 + ROOM);
            }
            spots.add(lane("parked " + (k + 1), along));
        }
        return spots;
    }

    /**
     * Along the first road's far edge, a metre in from it, one after another from beside where the avatar starts,
     * facing across at the vehicles: out of the way of the avatar, and of the camera behind it.
     */
    @Override
    public List<Spot> standing(final int count) {
        final Roadway road = first();
        final double across = road.section().pavedEdge(1) - EDGE_IN;
        final List<Spot> spots = new ArrayList<>();
        for (int k = 0; k < count; k++) {
            final int i = road.nearestPoint(FIRST - BEHIND + k * PEOPLE_APART);
            final Vec2 at = road.at(i, across);
            spots.add(new Spot("standing " + (k + 1), new Place(at.x(), at.z(), stood(at), yaw(road, i) - 90)));
        }
        return spots;
    }

    /** The road the vehicles are parked on, and the avatar starts by. */
    private Roadway first() {
        return works.roads().getFirst();
    }

    /** {@code along} the first road, in the middle of its leftmost lane, on what is stood on there, facing along it. */
    private Spot lane(final String name, final double along) {
        final Roadway road = first();
        final int i = road.nearestPoint(along);
        final Vec2 at = road.at(i, lane(road.section()));
        return new Spot(name, new Place(at.x(), at.z(), stood(at), yaw(road, i)));
    }

    /** How high what is stood on at {@code at} is: the land, or a pavement of its own over it. */
    private double stood(final Vec2 at) {
        double top = footing.shape(at.x(), at.z(), 0);
        if (ownPavement) {
            for (final Roadway road : works.roads()) {
                final double paved = road.surface(at.x(), at.z());
                if (!Double.isNaN(paved)) {
                    top = Math.max(top, paved);
                }
            }
        }
        return top;
    }

    /** The middle of the leftmost lane across {@code section}; its line, if it has none. */
    private static double lane(final Section section) {
        for (final Section.Laid l : section.laid()) {
            if (l.strip().use() == Strip.Use.LANE) {
                return (l.innerAcross() + l.outerAcross()) / 2;
            }
        }
        return 0;
    }

    /** Which way {@code road} goes at its {@code i}th point, degrees. */
    private static double yaw(final Roadway road, final int i) {
        final Vec2 r = road.right(i);
        return Math.toDegrees(Math.atan2(-r.z(), r.x()));
    }
}
