package com.example.fxsuite.avatar.roads;

/**
 * Ground as roads prepare it: at a point, how high it is and what covers it ({@link Prepared}). One road's
 * ({@link Roadway#ground}), or several roads' together, each laid on the ground as those before it left it.
 */
@FunctionalInterface
public interface Ground {

    /** The ground at (x, z). */
    Prepared at(double x, double z);

    /** The ground as {@code profile} has it, untouched. */
    static Ground untouched(final Profile profile) {
        return (x, z) -> new Prepared(profile.height(x, z), null);
    }

    /** Just how high it is: a profile, for whatever is designed against it next. */
    default Profile profile() {
        return (x, z, below) -> at(x, z).height();
    }
}
