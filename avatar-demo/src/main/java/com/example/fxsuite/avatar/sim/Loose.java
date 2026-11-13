package com.example.fxsuite.avatar.sim;

/**
 * What a loose thing is ({@link Prop}): something that stands on whatever is under it, can be
 * pushed about, falls, and can be broken. Fruit, and furniture: each kind says how big it is, how
 * much it takes, how readily it is pushed, and how hard a landing it comes through.
 */
public sealed interface Loose permits Fruit, Furniture {

    /** What it is called. */
    String title();

    /** How long it is, across its own frame. */
    double length();

    /** How wide, along its own frame. */
    double width();

    /** How tall. */
    double height();

    /** How much it takes before it breaks ({@link Weapon#blow}). */
    double toughness();

    /** How readily it is pushed, 0 to 1: as fast as whoever pushes it, for 1; a heavy table half that. */
    double give();

    /** How fast it can come down and land whole, units a second: any faster, and it breaks. */
    double landing();

    /** Its solid box, its middle {@code across} and {@code along} in the frame it is put in, sitting on what is {@code on} high. */
    default Course.Section at(final double across, final double along, final double on) {
        return new Course.Section(across, along, on + height() / 2, length() / 2, width() / 2, height() / 2);
    }
}
