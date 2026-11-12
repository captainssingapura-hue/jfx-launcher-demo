package com.example.fxsuite.avatar.sim;

/**
 * An intent: a round, or the blade, reaching something within a step. At {@code at}, on a surface
 * facing {@code normal}, the ground or a block, which is piece {@code piece} of the
 * {@link Course} (-1 for the ground), by {@code by}, as hard as that weapon hits
 * ({@link Weapon#blow}).
 *
 * <p>Inside the step only: the weapons make blows, and the world resolves them ({@link Damage}),
 * in order. What came of each is what goes out, as {@link Event.Struck} and {@link Event.Broke}.
 * A blow itself is never an output, and never recorded: it follows from the state and the inputs.
 */
public record Blow(Vec3 at, Vec3 normal, Surface on, Weapon by, int piece) {

    /** Reaching no piece in particular: the ground. */
    public Blow(final Vec3 at, final Vec3 normal, final Surface on, final Weapon by) {
        this(at, normal, on, by, -1);
    }
}
