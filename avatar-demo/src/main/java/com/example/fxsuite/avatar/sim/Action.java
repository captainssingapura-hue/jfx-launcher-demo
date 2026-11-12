package com.example.fxsuite.avatar.sim;

/**
 * Something the player does once, rather than holds: an input applied at the start of the step it
 * comes with ({@link Controls#actions}), in the order it was done. Whatever the action cannot do
 * (taking off without the pack, jumping in mid-air) it leaves as it was, as the key would.
 */
public sealed interface Action {

    /** Jump, if on its feet. */
    record Jump() implements Action {
    }

    /** Take off, wearing the pack; or stop flying, and fall. */
    record Fly() implements Action {
    }

    /** Put the pack on, or take it off; off in flight, it falls. */
    record Pack() implements Action {
    }

    /** Draw {@code weapon}, putting away whatever is held; or, if it is already held, put it away. */
    record Draw(Weapon weapon) implements Action {
    }

    Action JUMP = new Jump();
    Action FLY = new Fly();
    Action PACK = new Pack();
}
