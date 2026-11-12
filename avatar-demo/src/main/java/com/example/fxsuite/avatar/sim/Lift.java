package com.example.fxsuite.avatar.sim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A platform flown from the console on it, like a magic carpet: piece {@code piece} of the
 * course, its {@code deck} (as big as it was made, {@link Course.Deck}), where it now is, how fast
 * it is going in its own terms ({@code ahead} along its heading, {@code aside} to its right,
 * {@code climb} up), whether the avatar has its controls, and whether its {@code engine} is
 * running. Pure state, stepped with the rest of the world ({@link Play}).
 *
 * <ul>
 *   <li><b>The controls</b> are taken standing on the deck behind the console, within
 *       {@link #REACH} of where whoever flies it stands ({@link Action#TAKE}); and let go the same
 *       way, or by no longer standing there. At them, the key that takes off or lands
 *       ({@link Action#FLY}) starts the engine or cuts it.</li>
 *   <li><b>Its engine running</b>, the keys that move the avatar fly the platform instead: W and
 *       S ahead and back, A and D sideways, the mouse turns it, Space climbs and Ctrl descends.
 *       Shift held goes carefully. It eases up to speed and back to a stop, and keeps between
 *       {@link Course#LIFT_LOW} and {@link Course#LIFT_HIGH}, inside the world. Let go, it eases to
 *       a stop and hangs where it is.</li>
 *   <li><b>It stops short</b> of whatever is in its way: its deck, and the room above it for
 *       whoever rides it ({@link Course.Deck#room}), may not go into anything solid. Where the
 *       whole move would, it tries turning only, then going across only, then climbing only; and
 *       if none will do, it stays where it is. Something that has come into it (an airship going
 *       by) does not hold it: from there, any move goes.</li>
 *   <li><b>Its engine cut</b>, it cannot be flown: it drops as everything does
 *       ({@link Body#GRAVITY}) until it settles on whatever is under it (the ground, a crate, a
 *       pillar, an airship, another platform, or the avatar's head) and rests there. Resting on
 *       something that moves, it is docked: it goes where that goes ({@link #carried}). Started
 *       again, it lifts off from there.</li>
 * </ul>
 */
public record Lift(int piece, Course.Deck deck, Place place, double ahead, double aside, double climb,
                   boolean held, boolean engine) {

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
    /** How thick its deck is: how far below where it is its underside is. */
    public static final double THICK = 40;

    /** A platform, still, its engine running, no one at its controls. */
    public Lift(final int piece, final Course.Deck deck, final Place place) {
        this(piece, deck, place, 0, 0, 0, false, true);
    }

    /** Going as given, its engine running. */
    public Lift(final int piece, final Course.Deck deck, final Place place, final double ahead, final double aside,
                final double climb, final boolean held) {
        this(piece, deck, place, ahead, aside, climb, held, true);
    }

    /** The platforms among {@code damage}'s pieces: each where it was built, still, and no one at its controls. */
    public static List<Lift> of(final Damage damage) {
        final List<Lift> lifts = new ArrayList<>();
        for (final int piece : damage.moving()) {
            final Course.Piece what = damage.piece(piece);
            if (what.kind() == Course.Kind.PLATFORM) {
                lifts.add(new Lift(piece, Course.Deck.of(what.shape()), what.place()));
            }
        }
        return lifts;
    }

    /** What it rests on, or would: piece {@code piece} (-1 for the ground or the avatar), whose top is {@code top} high. */
    public record Support(int piece, double top) {
    }

    /** The same, its controls taken or let go. */
    public Lift held(final boolean taken) {
        return new Lift(piece, deck, place, ahead, aside, climb, taken, engine);
    }

    /** The same, its engine started or cut. */
    public Lift engine(final boolean running) {
        return new Lift(piece, deck, place, ahead, aside, climb, held, running);
    }

    /** The same, carried by what it rests on as that moved from {@code from} to {@code to}: docked. */
    public Lift carried(final Place from, final Place to) {
        final Vec3 at = from.carry(new Vec3(place.x(), -place.height(), place.z()), to);
        return new Lift(piece, deck, new Place(at.x(), at.z(), -at.y(), Num.wrap(place.yaw() + to.yaw() - from.yaw())),
                ahead, aside, climb, held, engine);
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
     * The highest thing under its deck where it is, among {@code world} and {@code also} (the
     * avatar, say), but for itself and what rides on it ({@code aboard}): what it rests on, or
     * would settle on, falling. The ground if nothing else.
     */
    public Support support(final Obstacles world, final Collection<Integer> aboard, final List<Block> also) {
        final double underside = place.height() - THICK;
        final Course.Section slab = deck.slab();
        final Block below = new Course.Section(slab.across(), slab.along(), (underside + 0.5 - 1) / 2 - place.height(),
                slab.halfAcross(), slab.halfAlong(), (underside + 0.5 + 1) / 2).placed(place);
        Support best = new Support(-1, 0);
        for (int i = 0; i < world.blocks().size(); i++) {
            final int other = world.pieces().get(i);
            final Block block = world.blocks().get(i);
            if (other != piece && !aboard.contains(other) && block.top() > best.top()
                    && block.top() <= underside + 0.5 && below.overlaps(block)) {
                best = new Support(other, block.top());
            }
        }
        for (final Block block : also) {
            if (block.top() > best.top() && block.top() <= underside + 0.5 && below.overlaps(block)) {
                best = new Support(-1, block.top());
            }
        }
        return best;
    }

    /** Whether it rests on {@code under}, its engine cut and its underside on it. */
    public boolean resting(final Support under) {
        return !engine && Math.abs(place.height() - THICK - under.top()) < 0.5;
    }

    /**
     * {@code seconds} on, among {@code world}, its own blocks in it or not, and those of what rides
     * on it, {@code aboard}; with {@code also} (the avatar, if not aboard) solid too. Its engine
     * running, flown by {@code controls} if held, and only easing to a stop if not; cut, falling
     * onto what is under it, or resting there.
     */
    public Lift step(final Controls controls, final double seconds, final Obstacles world,
                     final Collection<Integer> aboard, final List<Block> also) {
        return engine ? fly(controls, seconds, world, aboard) : fall(seconds, world, aboard, also);
    }

    /** As {@link #step(Controls, double, Obstacles, Collection, List)}, with nothing else solid. */
    public Lift step(final Controls controls, final double seconds, final Obstacles world,
                     final Collection<Integer> aboard) {
        return step(controls, seconds, world, aboard, List.of());
    }

    private Lift fall(final double seconds, final Obstacles world, final Collection<Integer> aboard,
                      final List<Block> also) {
        final Support under = support(world, aboard, also);
        final double dropping = climb - Body.GRAVITY * seconds;
        final double height = place.height() + (climb + dropping) / 2 * seconds;
        if (height - THICK <= under.top()) {
            // settled: on it, and still
            return new Lift(piece, deck, new Place(place.x(), place.z(), under.top() + THICK, place.yaw()), 0, 0, 0,
                    held, false);
        }
        return new Lift(piece, deck, new Place(place.x(), place.z(), height, place.yaw()), 0, 0, dropping, held, false);
    }

    private Lift fly(final Controls controls, final double seconds, final Obstacles world,
                     final Collection<Integer> aboard) {
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
        final boolean stuck = !clear(place, world, aboard);
        final Place[] tries = {
                new Place(x, z, height, yaw),
                new Place(place.x(), place.z(), place.height(), yaw),
                new Place(x, z, place.height(), place.yaw()),
                new Place(place.x(), place.z(), height, place.yaw()),
        };
        for (int k = 0; k < tries.length; k++) {
            if (stuck || clear(tries[k], world, aboard)) {
                final boolean goesAcross = k == 0 || k == 2;
                final boolean goesUp = k == 0 || k == 3;
                return new Lift(piece, deck, tries[k], goesAcross ? nowAhead : 0, goesAcross ? nowAside : 0,
                        goesUp ? climbing : 0, held, true);
            }
        }
        return new Lift(piece, deck, place, 0, 0, 0, held, true);
    }

    /**
     * Whether at {@code at} its deck and the room over it are clear of everything solid in
     * {@code world} but itself and what rides on it, {@code aboard}: that goes where it goes.
     */
    public boolean clear(final Place at, final Obstacles world, final Collection<Integer> aboard) {
        final Block room = deck.room().placed(at);
        for (int i = 0; i < world.blocks().size(); i++) {
            final int other = world.pieces().get(i);
            if (other != piece && !aboard.contains(other) && room.overlaps(world.blocks().get(i))) {
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
