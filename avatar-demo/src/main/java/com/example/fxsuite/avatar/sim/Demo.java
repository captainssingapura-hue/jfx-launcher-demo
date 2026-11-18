package com.example.fxsuite.avatar.sim;

/**
 * A demo: anything that can lay a world into a course ({@link Course#Course(Demo)}), and say what it is. The built-in
 * ones ({@link Demos}); one defined in the package that makes its world, registered by name ({@link Demos#register});
 * or one made on the fly, from whatever asks for it, and played at once.
 */
public interface Demo {

    /** What it is known by: in a recording, and asked for to play ({@code -Davatar.demo}). */
    String name();

    /** What it is called, to choose it by. */
    String title();

    /** What there is in it, in a line. */
    String description();

    /** Which way the avatar faces at the start, degrees. */
    double startYaw();

    /** Whether it is offered to play in the menu. */
    default boolean offered() {
        return true;
    }

    /** Whether a run of it can be kept and replayed: found again by its {@link #name}. Not, unless it says so. */
    default boolean recordable() {
        return false;
    }

    /** Its world laid into a course: where the avatar starts, what stands there, its water, who is about, its land. */
    void lay(Course.Layout world);
}
