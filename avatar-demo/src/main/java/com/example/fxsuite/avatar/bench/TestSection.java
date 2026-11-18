package com.example.fxsuite.avatar.bench;

import com.example.fxsuite.avatar.DrawnDemo;
import com.example.fxsuite.avatar.look.RoadworkModel;
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
import com.example.fxsuite.avatar.sim.Vec3;

import javafx.scene.Group;
import javafx.scene.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * A scene of the roads bench played: its roads as laid ({@link Roadworks}), of the kind across the bench has now, as a
 * world to drive in. A demo made on the fly ({@link DrawnDemo}): never offered, never looked up by name, its runs not
 * recorded.
 *
 * <ul>
 *   <li><b>its land</b>: the ground as the roads leave it, and under each pavement where the ground carries it, the
 *       pavement's top a little lower than drawn ({@link #SINK}): what wheels and feet stand on;</li>
 *   <li><b>its structures</b> in the way ({@link Decks}): decks to drive over, parapets, piers;</li>
 *   <li><b>its water</b>, as the scene has it;</li>
 *   <li><b>its vehicles</b>: one of each land chassis, parked one after another along the first road's leftmost
 *       lane from 20 m on, facing along it; the avatar starting just behind them;</li>
 *   <li><b>drawn</b>: the roads' pavements, markings and walls wanted, by the roads' own drawing
 *       ({@link RoadworkModel}); their structures as buildings are, by their keys; the land and the water as the game
 *       draws any. A tunnel's lining not yet.</li>
 * </ul>
 */
final class TestSection implements DrawnDemo {

    /** How far under the pavement as drawn its top is stood on: out of sight under it. */
    static final double SINK = 3;
    /** The keys its structures are drawn by, as buildings are. */
    static final int KEYS = 900_000;
    /** The land chassis parked at the start, in the order parked. */
    static final List<Course.Chassis> VEHICLES = List.of(Course.CAR_CHASSIS, Course.PICKUP_CHASSIS, Course.TRUCK_CHASSIS,
            Course.SCOOTER, Course.SPORT_BIKE, Course.CRUISER, Course.BOXER, Course.BOXER_SIDECAR);
    /** Where along the first road the first is parked, and how far apart they are, nose to nose. */
    static final double FIRST = 2_000;
    static final double APART = 900;

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

    @Override
    public String name() {
        return "TEST_SECTION";
    }

    @Override
    public String title() {
        return "Test section · " + scene.name() + " · " + section.name() + (ownPavement ? "" : " · pavement in the land");
    }

    @Override
    public String description() {
        return "The roads bench's " + scene.name() + " scene, its roads of kind \"" + section.name()
                + "\", to drive on: one of each vehicle parked at the start.";
    }

    @Override
    public double startYaw() {
        return yaw(first(), 0);
    }

    @Override
    public boolean offered() {
        return false;
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
        final List<Course.Piece> pieces = new ArrayList<>(parked());
        pieces.addAll(Decks.pieces(works, KEYS, ownPavement));
        world.land(land).pieces(pieces).ruled(false).start(start());
        if (ownPavement) {
            world.ways(works.roads().stream().map(r -> (com.example.fxsuite.avatar.sim.Volume) r.pavement()).toList());
        }
        if (scene.watered()) {
            world.ponds(List.of(new Pond(List.of(new Vec2(box[0], box[2]), new Vec2(box[1], box[2]),
                    new Vec2(box[1], box[3]), new Vec2(box[0], box[3])), scene.water())));
        }
    }

    @Override
    public Node scenery() {
        final Group drawn = new Group();
        for (final Roadway road : works.roads()) {
            drawn.getChildren().addAll(RoadworkModel.pavement(road, Double.POSITIVE_INFINITY),
                    RoadworkModel.markings(road, Double.POSITIVE_INFINITY));
        }
        drawn.getChildren().add(RoadworkModel.walls(works));
        return drawn;
    }

    /** The road the vehicles are parked on, and the avatar starts by. */
    private Roadway first() {
        return works.roads().getFirst();
    }

    /** One of each land chassis, along the first road's leftmost lane, nose to nose, facing along it, on its top. */
    private List<Course.Piece> parked() {
        final Roadway road = first();
        final double across = lane(road.section());
        final List<Course.Piece> parked = new ArrayList<>();
        for (int k = 0; k < VEHICLES.size(); k++) {
            final Course.Chassis chassis = VEHICLES.get(k);
            final int i = road.nearestPoint(FIRST + k * APART);
            final Vec2 at = road.at(i, across);
            parked.add(Course.Piece.vehicle(chassis, null, new Place(at.x(), at.z(), stood(at) + chassis.ride(),
                    yaw(road, i)), k % 4));
        }
        return parked;
    }

    /** Where the avatar starts: just behind the first vehicle, in its lane, standing on the road. */
    private Vec3 start() {
        final Roadway road = first();
        final Vec2 at = road.at(road.nearestPoint(FIRST - 600), lane(road.section()));
        return new Vec3(at.x(), -stood(at), at.z());
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
