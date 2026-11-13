package com.example.fxsuite.avatar.sim;

/**
 * Which demo to play: the whole playground, or one built round a single vehicle, simple and open,
 * so the vehicle is all there is to look at ({@link Course#Course(Demo)}). Each says where the
 * avatar starts, and which way it faces.
 *
 * @param title       what it is called, to choose it by
 * @param description what there is in it, in a line
 * @param startYaw    which way the avatar faces at the start
 */
public enum Demo {

    /** Everything at once: the course, the range, the airships going round, the platforms and the stall. */
    PLAYGROUND("Playground", "Everything at once: the course, the targets, the airships, the stall, the flyovers, the vehicles,"
            + " the bunker, the gunship, and an airship to fly.",
            Course.START_YAW),
    /** A flying platform, open: the avatar rides it, on its deck at the console. */
    PLATFORM("Flying platform", "A platform to fly like a magic carpet, among a few pillars and gates.", 0),
    /** An airship, closed: the avatar goes aboard at the gondola, and is seen no more until it steps out. */
    AIRSHIP("Airship", "An airship moored by the start, to board, fly, and bomb a yard of crates with.", 0),
    /** Ramps: a flyover across the way ahead, to run up, over and under; and a ramp to leap off. */
    FLYOVER("Roads", "A flyover to run or drive up, across and under, a ramp to leap off, and a car and a truck chassis to drive.", 0),
    /** Heavy Gatling guns on mounts: in a bunker, and on a gunship platform; and crates and dummies to fire at. */
    GUNS("Guns", "A bunker and a gunship, each with a heavy Gatling gun, to turn on crates and dummies.", 0),
    /** A race track: a NASCAR-style oval, banked in its turns, and every land vehicle on pit road to take round it. */
    TRACK("Track", "A NASCAR-style oval, a mile round, banked 24° in its turns; the cars, the truck and the bikes on"
            + " pit road, to take round it flat out.", 105);

    private final String title;
    private final String description;
    private final double startYaw;

    Demo(final String title, final String description, final double startYaw) {
        this.title = title;
        this.description = description;
        this.startYaw = startYaw;
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
}
