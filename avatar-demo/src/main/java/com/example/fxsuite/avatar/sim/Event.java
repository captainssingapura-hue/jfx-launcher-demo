package com.example.fxsuite.avatar.sim;

/**
 * Something that happened, as the simulation reports it: to the avatar, or to the world at its
 * hands. The moments that cannot be read back from its state afterwards, because a landing has
 * already zeroed the fall that made it, or a bump the speed it took away. Each output (the
 * graphics, the sound) listens for the ones it has a use for. Speeds are in world units per
 * second.
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

    /**
     * A rifle fired a round: the one in the left hand, or the right. It flew from the muzzle
     * {@code from} to {@code to}, where it struck something, or ran out of range.
     */
    record Fired(boolean left, Vec3 from, Vec3 to) implements Event {
    }

    /**
     * A weapon struck something: at {@code at}, on a surface facing {@code normal}, the ground or a
     * block, which is piece {@code piece} of the {@link Course} (-1 for the ground). A round
     * fired, or the glaive's blade coming through. What came of a {@link Blow}, once the world had
     * taken it.
     *
     * <p>It carries its result: {@code left}, how much the piece has left of what it can take
     * after this blow, 1 unharmed to 0 broken, and always 1 for what cannot break and for the
     * ground. A reader that missed the blows before it still knows where the piece stands.
     */
    record Struck(Vec3 at, Vec3 normal, Surface on, Weapon by, int piece, double left) implements Event {

        /** Struck, on no piece in particular, and none the worse. */
        public Struck(final Vec3 at, final Vec3 normal, final Surface on, final Weapon by) {
            this(at, normal, on, by, -1, 1);
        }

        /** What came of {@code blow}: the piece it reached has {@code left} after it. */
        public static Struck of(final Blow blow, final double left) {
            return new Struck(blow.at(), blow.normal(), blow.on(), blow.by(), blow.piece(), left);
        }
    }

    /**
     * Something was broken: piece {@code piece} of the {@link Course}, a {@code kind} that was
     * the box {@code block}, its last blow struck at {@code struck} by {@code by}. It is gone from
     * the world for good ({@link Damage}).
     */
    record Broke(int piece, Course.Kind kind, Block block, Vec3 struck, Weapon by) implements Event {

        /** The middle of what broke. */
        public Vec3 middle() {
            return new Vec3(block.x(), -(block.bottom() + block.top()) / 2, block.z());
        }
    }

    /** The glaive's blade came through: {@link Arms#CLEAVE} or {@link Arms#SWEEP}. */
    record Swung(int stroke) implements Event {
    }

    /** The avatar took the controls of platform {@code piece}, standing at its console. */
    record TookControls(int piece) implements Event {
    }

    /** The avatar let go of the controls of platform {@code piece}: it let go, or was no longer at them. */
    record LetGo(int piece) implements Event {
    }

    /** An airship let go of a bomb, at {@code at}: airship {@code from}, by its piece number. */
    record Dropped(Vec3 at, int from) implements Event {
    }

    /**
     * A bomb went off at {@code at}, on the ground or a block. What it harmed comes after, each
     * piece struck ({@link Struck}, by {@link Weapon#BOMB}) and what broke.
     */
    record Exploded(Vec3 at, Surface on) implements Event {
    }
}
