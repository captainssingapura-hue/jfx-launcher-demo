package com.example.fxsuite.avatar.sim;

import java.util.ArrayList;
import java.util.List;

/**
 * A platform flown from the console on it, like a magic carpet: piece {@code piece} of the
 * course, its {@code deck} (as big as it was made, {@link Course.Deck}), where it now is, how fast
 * it is going in its own terms ({@code ahead} along its heading, {@code aside} to its right,
 * {@code climb} up), and whether the avatar has its controls. Pure state, stepped with the rest
 * of the world ({@link Play}).
 *
 * <ul>
 *   <li><b>The controls</b> are taken standing on the deck behind the console, within
 *       {@link #REACH} of where whoever flies it stands ({@link Action#TAKE}); and let go the same
 *       way, or by no longer standing there.</li>
 *   <li><b>At the controls</b>, the keys that move the avatar fly the platform instead: W and S
 *       ahead and back, A and D sideways, the mouse turns it, Space climbs and Ctrl descends.
 *       Shift held goes carefully. It eases up to speed and back to a stop, and keeps between
 *       {@link Course#LIFT_LOW} and {@link Course#LIFT_HIGH}, inside the world.</li>
 *   <li><b>It stops short</b> of whatever is in its way: its deck, and the room above it for
 *       whoever rides it ({@link Course.Deck#room}), may not go into anything solid. Where the
 *       whole move would, it tries turning only, then going across only, then climbing only; and
 *       if none will do, it stays where it is. Something that has come into it (an airship going
 *       by) does not hold it: from there, any move goes.</li>
 *   <li><b>Let go</b>, it eases to a stop and hangs where it is.</li>
 * </ul>
 */
public record Lift(int piece, Course.Deck deck, Place place, double ahead, double aside, double climb,
                   boolean held) {

    /** How fast it flies, at full, across the ground: units a second. */
    public static final double SPEED = 700;
    /** How fast it climbs or descends, at full. */
    public static final double CLIMB = 350;
    /** Sideways is slower than ahead, and going carefully slower still, by these. */
    public static final double SIDEWAYS = 0.6;
    public static final double CAREFUL = 0.35;
    /** How briskly it gets up to speed, and stops. */
    public static final double GRIP = 2.5;
    /** How near where the pilot stands the avatar must be to reach the controls. */
    public static final double REACH = 110;

    /** A platform, still, no one at its controls. */
    public Lift(final int piece, final Course.Deck deck, final Place place) {
        this(piece, deck, place, 0, 0, 0, false);
    }

    /** The platforms among {@code damage}'s pieces: each where it was built, still, and no one at its controls. */
    public static List<Lift> of(final Damage damage) {
        final List<Lift> lifts = new ArrayList<>();
        for (final int piece : damage.moving()) {
            final Course.Piece what = damage.piece(piece);
            if (!what.flies()) {
                lifts.add(new Lift(piece, Course.Deck.of(what.shape()), what.place()));
            }
        }
        return lifts;
    }

    /** The same, its controls taken or let go. */
    public Lift held(final boolean taken) {
        return new Lift(piece, deck, place, ahead, aside, climb, taken);
    }

    /** How fast it is going, all told: across the ground and up or down. */
    public double speed() {
        return Math.sqrt(ahead * ahead + aside * aside + climb * climb);
    }

    /** Where whoever flies it stands: on the deck, just behind the console, facing the way it heads. */
    public Vec3 pilot() {
        return new Vec3(place.x(), -place.height(), place.z()).add(Num.heading(place.yaw()).multiply(deck.pilot()));
    }

    /** Whether {@code body}, among {@code world}, stands on this platform's deck within reach of its controls. */
    public boolean reaches(final Body body, final Obstacles world) {
        if (!body.grounded()) {
            return false;
        }
        final Vec3 at = body.position();
        if (world.under(at.x(), at.z(), Body.FOOTING, body.height()) != piece) {
            return false;
        }
        final Vec3 pilot = pilot();
        return Math.hypot(at.x() - pilot.x(), at.z() - pilot.z()) <= REACH;
    }

    /**
     * {@code seconds} on, flown by {@code controls} if held (and only easing to a stop if not),
     * among {@code world}, its own blocks in it or not.
     */
    public Lift step(final Controls controls, final double seconds, final Obstacles world) {
        final double pace = held ? (controls.run() ? 1 : CAREFUL) : 0;
        final double nowAhead = ease(ahead, Num.clamp(controls.forward(), -1, 1) * SPEED * pace, seconds);
        final double nowAside = ease(aside, Num.clamp(controls.sideways(), -1, 1) * SPEED * SIDEWAYS * pace, seconds);
        final double nowClimb = ease(climb, Num.clamp(controls.lift(), -1, 1) * CLIMB * (held ? 1 : 0), seconds);
        final double yaw = held ? Num.wrap(place.yaw() + controls.turn()) : place.yaw();
        // the average speeds over the step, along the way it heads at the end of it
        final double a = (ahead + nowAhead) / 2 * seconds;
        final double b = (aside + nowAside) / 2 * seconds;
        final double up = (climb + nowClimb) / 2 * seconds;
        final Vec3 across = Num.heading(yaw).multiply(a).add(Num.right(yaw).multiply(b));
        final double x = Num.clamp(place.x() + across.x(), -Body.EDGE, Body.EDGE);
        final double z = Num.clamp(place.z() + across.z(), -Body.EDGE, Body.EDGE);
        final double height = Num.clamp(place.height() + up, Course.LIFT_LOW, Course.LIFT_HIGH);
        final double climbing = height == Course.LIFT_LOW || height == Course.LIFT_HIGH ? 0 : nowClimb;
        // the whole move if it is clear; else turning only, going across only, climbing only; else none
        final boolean stuck = !clear(place, world);
        final Place[] tries = {
                new Place(x, z, height, yaw),
                new Place(place.x(), place.z(), place.height(), yaw),
                new Place(x, z, place.height(), place.yaw()),
                new Place(place.x(), place.z(), height, place.yaw()),
        };
        for (int k = 0; k < tries.length; k++) {
            if (stuck || clear(tries[k], world)) {
                final boolean goesAcross = k == 0 || k == 2;
                final boolean goesUp = k == 0 || k == 3;
                return new Lift(piece, deck, tries[k], goesAcross ? nowAhead : 0, goesAcross ? nowAside : 0,
                        goesUp ? climbing : 0, held);
            }
        }
        return new Lift(piece, deck, place, 0, 0, 0, held);
    }

    /** Whether at {@code at} its deck and the room over it are clear of everything solid in {@code world} but itself. */
    public boolean clear(final Place at, final Obstacles world) {
        final Block room = deck.room().placed(at);
        for (int i = 0; i < world.blocks().size(); i++) {
            if (world.pieces().get(i) != piece && room.overlaps(world.blocks().get(i))) {
                return false;
            }
        }
        return true;
    }

    /** Eased toward {@code wanted}; let go, at a crawl, it stops rather than creeping on for ever. */
    private static double ease(final double speed, final double wanted, final double seconds) {
        final double now = Num.ease(speed, wanted, GRIP, seconds);
        return wanted == 0 && Math.abs(now) < 1 ? 0 : now;
    }
}
