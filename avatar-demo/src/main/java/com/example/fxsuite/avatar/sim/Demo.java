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
    PLAYGROUND("Playground", "The course, the targets, the airships and their bombs, the platforms and the fruit stall.",
            Course.START_YAW),
    /** A flying platform, open: the avatar rides it, on its deck at the console. */
    PLATFORM("Flying platform", "A platform to fly like a magic carpet, among a few pillars and gates.", 0),
    /** An airship, closed: the avatar goes aboard at the gondola, and is seen no more until it steps out. */
    AIRSHIP("Airship", "An airship moored by the start, to board at its gondola and fly.", 0);

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
