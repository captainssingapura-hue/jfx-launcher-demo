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
    PLATFORM("Flying platform", "A platform to fly like a magic carpet, among a few pillars and gates; and a stand-up on another.", 0),
    /** An airship, closed: the avatar goes aboard at the gondola, and is seen no more until it steps out. */
    AIRSHIP("Airship", "An airship moored by the start, to board, fly, and bomb a yard of crates with.", 0),
    /** Ramps: a flyover across the way ahead, to run up, over and under; and a ramp to leap off. */
    FLYOVER("Roads", "A flyover to run or drive up, across and under, a ramp to leap off, and a car and a truck chassis to drive.", 0),
    /** Heavy Gatling guns on mounts: in a bunker, and on a gunship platform; and crates and dummies to fire at. */
    GUNS("Guns", "A bunker and a gunship, each with a heavy Gatling gun, to turn on crates and dummies.", 0),
    /** A race track: a NASCAR-style oval, banked in its turns, and every land vehicle on pit road to take round it. */
    TRACK("Track", "A NASCAR-style oval, a mile round, banked 24° in its turns; the cars, the truck and the bikes on"
            + " pit road, to take round it flat out.", 105),
    /** The buildings, put up along streets; the boxer motorcycle to ride between them; the melon stall and the stand-up; the gunship. */
    TOWN("Town", "The buildings: the brutalist apartments, the towers, the church and the temple hall, along streets;"
            + " the boxer motorcycle to ride between them; before them, the melon stall and the stand-up, each on"
            + " its platform; and the gunship, to fly over it all.", 0),
    /** A temple's courtyard: its great hall at the back, a smaller hall each side, three monks at their stand-up. */
    TEMPLE("Temple", "A temple's courtyard: its great hall at the back, a smaller hall facing in from each side, and"
            + " three monks at their stand-up in the middle. (Its gate is to come.)", 0),
    /** Pyramids, each in the way as it is shaped: a great smooth one, a low one to walk up, a stepped one with a stairway. */
    PYRAMID("Pyramids", "A great smooth pyramid, too steep to climb; a low one, turned, to walk up to its point; and a"
            + " stepped one with a stairway up to the shrine on its top. Each is in the way as it is shaped.", 0),
    /** Pavilions round a lawn, one of each kind: open but for their pillars and roofs, to walk into and stand under. */
    PAVILION("Pavilions", "Pavilions round a lawn, one of each kind: square, hexagonal, octagonal and round ones to a point,"
            + " a long gallery, a gable, a modern one with a flat roof. Open but for their pillars and roofs: walk in.", 0);

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
