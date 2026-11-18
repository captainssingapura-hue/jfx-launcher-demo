package com.example.fxsuite.avatar.sim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The built-in demos ({@link Demo}), each laid as the course has always laid it: the town, everything in it; the playground; the
 * race track. Each says where the avatar starts, and which way it faces.
 *
 * <p>And a few simple scenes, each round a vehicle or a gun, open and plain: not offered to play
 * ({@link #offered}), as all that is in them is in the town or the playground, but kept for the
 * tests, which drive, fly and fire in them where nothing else is about.
 *
 * @param title       what it is called, to choose it by
 * @param description what there is in it, in a line
 * @param startYaw    which way the avatar faces at the start
 * @param offered     whether it is offered to play
 */
public enum Demos implements Demo {

    /**
     * The town: its buildings round the temple in its middle, a gun on top of each tower; the temple's
     * park round it, its ponds and pavilions; the pyramids at its edge; the vehicles parked in the
     * temple's courtyard, the monks at their stand-up; the stall and the meeting before it.
     */
    TOWN("Town", "The buildings round the temple and its park, a Gatling gun on top of each tower; ponds and"
            + " pavilions; the pyramids at the edge of town; every vehicle and the gunship parked in the temple's"
            + " courtyard, where the monks hold their stand-up; the melon stall and the meeting before it.",
            0, true),
    /**
     * The temple under siege ({@link Siege}): the town, and airships coming in without end from the edge
     * of the world to bomb its temple's main hall until it falls; set before it starts.
     */
    SIEGE("Temple siege", "The town under siege: airships come in from every side without end to bomb the temple's"
            + " hall until it falls. Shoot them down from the tower guns, the vehicles' guns or your own. Set how"
            + " many, how fast, how tough before it starts.", 0, true),
    /** A meadow, a river winding across it, and a small village of earth houses by its bank. */
    VILLAGE("Meadow village", "A meadow with a river winding across it, a footbridge over it, and a small village of"
            + " earth houses round a well by its bank, each its own size and finish: walk in, wade across, look round.",
            0, true),
    /**
     * Three towers borne by walls, end to end along a slight curve, trees scattered round them for kilometres: every
     * room of each enterable, by the jet-pack, a balcony at a time.
     */
    WALL_TOWER("Wall towers", "Three towers borne by walls, 55 storeys each, end to end along a slight curve: fly up to"
            + " any balcony, open its door with T or shoot it out, walk the corridors end to end.",
            90, true),
    /** Everything else at once: the course, the range, the airships going round, the platforms and the stall. */
    PLAYGROUND("Playground", "The course, the targets, the bombers, the stall, the flyovers, the vehicles,"
            + " the bunker, the gunship, and an airship to fly.",
            Course.START_YAW, true),
    /** A race track: a NASCAR-style oval, banked in its turns, and every land vehicle on pit road to take round it. */
    TRACK("Track", "A NASCAR-style oval, a mile round, banked 24° in its turns; the cars, the truck and the bikes on"
            + " pit road, to take round it flat out.", 105, true),
    /** A flying platform, open: the avatar rides it, on its deck at the console. Kept for the tests. */
    PLATFORM("Flying platform", "A platform to fly like a magic carpet, among a few pillars and gates; and a stand-up on another.",
            0, false),
    /** An airship, closed: the avatar goes aboard at the gondola, and is seen no more until it steps out. Kept for the tests. */
    AIRSHIP("Airship", "An airship moored by the start, to board, fly, and bomb a yard of crates with.", 0, false),
    /** Ramps: a flyover across the way ahead, to run up, over and under; and a ramp to leap off. Kept for the tests. */
    FLYOVER("Roads", "A flyover to run or drive up, across and under, a ramp to leap off, and a car and a truck chassis to drive.",
            0, false),
    /** Heavy Gatling guns on mounts: in a bunker, and on a gunship platform; and crates and dummies to fire at. Kept for the tests. */
    GUNS("Guns", "A bunker and a gunship, each with a heavy Gatling gun, to turn on crates and dummies.", 0, false);

    private final String title;
    private final String description;
    private final double startYaw;
    private final boolean offered;

    Demos(final String title, final String description, final double startYaw, final boolean offered) {
        this.title = title;
        this.description = description;
        this.startYaw = startYaw;
        this.offered = offered;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public double startYaw() {
        return startYaw;
    }

    /** Whether it is offered to play: the town, the playground and the track are; the scenes kept for the tests are not. */
    @Override
    public boolean offered() {
        return offered;
    }

    /** A built-in demo's runs can be kept and replayed: it is found again by its name. */
    @Override
    public boolean recordable() {
        return true;
    }

    /** Its world, as the course has always laid it. */
    @Override
    public void lay(final Course.Layout world) {
        world.builtIn(this);
    }

    /** Demos defined elsewhere, by name, in the order told: Marina Bay. */
    private static final Map<String, Demo> REGISTERED = new LinkedHashMap<>();

    /** Tells the demos of one more, defined elsewhere, known by its name: offered to play if it says so, and found by it. */
    public static synchronized void register(final Demo demo) {
        REGISTERED.put(demo.name(), demo);
    }

    /** The demo known by {@code name}: a built-in, else one told of; null if there is none. */
    public static synchronized Demo named(final String name) {
        for (final Demos d : values()) {
            if (d.name().equals(name)) {
                return d;
            }
        }
        return REGISTERED.get(name);
    }

    /** The demos offered to play, in order: the built-ins, the town first; then those told of. */
    public static synchronized List<Demo> playable() {
        final List<Demo> offered = new ArrayList<>(Arrays.stream(values()).filter(Demo::offered).toList());
        REGISTERED.values().stream().filter(Demo::offered).forEach(offered::add);
        return List.copyOf(offered);
    }
}
