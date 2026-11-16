package com.example.fxsuite.avatar.sim;

import java.util.Arrays;
import java.util.List;

/**
 * Which demo to play ({@link Course#Course(Demo)}): the town, everything in it; the playground; the
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
public enum Demo {

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

    Demo(final String title, final String description, final double startYaw, final boolean offered) {
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
    public boolean offered() {
        return offered;
    }

    /** The demos offered to play, in order: the town first. */
    public static List<Demo> playable() {
        return Arrays.stream(values()).filter(Demo::offered).toList();
    }
}
