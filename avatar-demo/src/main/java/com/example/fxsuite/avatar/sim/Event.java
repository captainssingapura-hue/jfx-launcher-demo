package com.example.fxsuite.avatar.sim;

/**
 * Something that happened to the avatar, as the simulation reports it: the moments that cannot
 * be read back from its state afterwards, because a landing has already zeroed the fall that
 * made it, or a bump the speed it took away. Each output (the graphics, the sound) listens for
 * the ones it has a use for. Speeds are in world units per second.
 */
public sealed interface Event {

    /** A foot came down, walking or running: the left one, or the right, on the ground or a block. */
    record Footfall(boolean left, Surface on) implements Event {
    }

    record Jumped() implements Event {
    }

    /**
     * It came down, falling at {@code speed}, onto the ground or a block (a crate, a pillar's
     * top); out of a flight, or a jump.
     */
    record Landed(double speed, boolean fromFlight, Surface on) implements Event {
    }

    /** It ran, or rose, into something, losing {@code speed} against it. */
    record Bumped(double speed) implements Event {
    }

    record TookOff() implements Event {
    }

    /** A flight ended in mid-air, and it falls. */
    record FlightCut() implements Event {
    }

    record PackOn() implements Event {
    }

    record PackOff() implements Event {
    }

    /** It went through a gate's opening, at {@code speed}, either way. */
    record PassedGate(double speed) implements Event {
    }

    /** A weapon was drawn into the hands, or (for {@link Weapon#NONE}) the hands emptied. */
    record Drew(Weapon weapon) implements Event {
    }

    /** A rifle fired a round: the one in the left hand, or the right. */
    record Fired(boolean left) implements Event {
    }

    /** The glaive's blade came through: {@link Arms#CLEAVE} or {@link Arms#SWEEP}. */
    record Swung(int stroke) implements Event {
    }
}
