package com.example.fxsuite.avatar.sim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A vehicle flown from on board: piece {@code piece} of the course, what kind of craft it is
 * ({@link Course.Craft}: a platform's deck, open; an airship, closed), where it now is, how fast it
 * is going in its own terms ({@code ahead} along its heading, {@code aside} to its right,
 * {@code climb} up), whether the avatar has its controls, and whether its {@code engine} is
 * running. Pure state, stepped with the rest of the world ({@link Play}).
 *
 * <ul>
 *   <li><b>The controls</b> are taken at its station ({@link Course.Craft#station}), within
 *       {@link #REACH} of it ({@link Action#TAKE}): on a platform, standing on the deck behind the
 *       console; at an airship, under the hatch in the gondola's floor, after which the avatar is inside and out of
 *       sight. They are let go the same way, or, on a platform, by no longer standing there. At a
 *       platform's controls the key that takes off or lands ({@link Action#FLY}) starts the engine
 *       or cuts it; an airship's is never cut.</li>
 *   <li><b>Its engine running</b>, the keys that move the avatar fly it instead: W and S ahead and
 *       back, A and D sideways (if it can go sideways), the mouse turns it (as fast as it will
 *       turn), Space climbs and Ctrl descends. Shift held goes carefully. It eases up to speed and
 *       back to a stop, and keeps between how low and how high it goes, inside the world; as it
 *       handles ({@link Course.Handling}). Let go, it eases to a stop and hangs where it is.</li>
 *   <li><b>It stops short</b> of whatever is in its way: the room it needs ({@link Course.Craft#room})
 *       may not go into anything solid. Where the whole move would, it tries turning only, then
 *       going across only, then climbing only; and if none will do, it stays where it is.
 *       Something that has come into it (an airship going by) does not hold it: from there, any
 *       move goes.</li>
 *   <li><b>Its engine cut</b>, it cannot be flown: it drops as everything does
 *       ({@link Body#GRAVITY}) until it settles on whatever is under it and rests there. Resting on
 *       something that moves, it is docked: it goes where that goes ({@link #carried}). Started
 *       again, it lifts off from there.</li>
 * </ul>
 */
public record Lift(int piece, Course.Craft craft, Place place, double ahead, double aside, double climb,
                   boolean held, boolean engine, int bombs, double ready, double steer, double tilt) {

    /** How fast a platform flies, at full, across the ground; and climbs. */
    public static final double SPEED = Course.PLATFORM_HANDLING.speed();
    public static final double CLIMB = Course.PLATFORM_HANDLING.climb();
    /** Going carefully is slower, by this. */
    public static final double CAREFUL = 0.35;
    /** How briskly it gets up to speed, and stops. */
    public static final double GRIP = 2.5;
    /** How near its station the avatar must be to take the controls. */
    public static final double REACH = 110;
    /** How thick a platform's deck is: how far below where it is its underside is. */
    public static final double THICK = 40;

    /** Seconds between one bomb and the next, the trigger held. */
    public static final double RACK = 0.4;
    /** Seconds to load each bomb back into the bay, moored. */
    public static final double RELOAD = 1;

    /** Going as given, its bomb bay (if it has one) full. */
    public Lift(final int piece, final Course.Craft craft, final Place place, final double ahead, final double aside,
                final double climb, final boolean held, final boolean engine) {
        this(piece, craft, place, ahead, aside, climb, held, engine, craft.bombs(), 0, 0, 0);
    }

    /** A craft, still, its engine running, no one at its controls. */
    public Lift(final int piece, final Course.Craft craft, final Place place) {
        this(piece, craft, place, 0, 0, 0, false, true);
    }

    /** Going as given, its engine running. */
    public Lift(final int piece, final Course.Craft craft, final Place place, final double ahead, final double aside,
                final double climb, final boolean held) {
        this(piece, craft, place, ahead, aside, climb, held, true);
    }

    /** The crafts among {@code damage}'s pieces (the platforms, an airship to be flown), each where it was built, still. */
    public static List<Lift> of(final Damage damage) {
        final List<Lift> lifts = new ArrayList<>();
        for (int piece = 0; piece < damage.size(); piece++) {
            final Course.Piece what = damage.piece(piece);
            if (what.kind() == Course.Kind.PLATFORM) {
                lifts.add(new Lift(piece, Course.Deck.of(what.shape()), what.place()));
            } else if (what.kind() == Course.Kind.AIRSHIP && what.moves() && !what.flies()) {
                lifts.add(new Lift(piece, Course.SHIP, what.place()));
            } else if (what.kind() == Course.Kind.CAR) {
                lifts.add(new Lift(piece, Course.Chassis.of(what.shape()), what.place()));
            }
        }
        return lifts;
    }

    /** What it rests on, or would: piece {@code piece} (-1 for the ground or the avatar), whose top is {@code top} high. */
    public record Support(int piece, double top) {
    }

    /** Its deck, if it is a platform; null if not. */
    public Course.Deck deck() {
        return craft instanceof Course.Deck d ? d : null;
    }

    /** The same, its controls taken or let go. */
    public Lift held(final boolean taken) {
        return new Lift(piece, craft, place, ahead, aside, climb, taken, engine, bombs, ready, steer, tilt);
    }

    /** The same, its engine started or cut. */
    public Lift engine(final boolean running) {
        return new Lift(piece, craft, place, ahead, aside, climb, held, running, bombs, ready, steer, tilt);
    }

    /** The same, carried by what it rests on as that moved from {@code from} to {@code to}: docked. */
    public Lift carried(final Place from, final Place to) {
        final Vec3 at = from.carry(new Vec3(place.x(), -place.height(), place.z()), to);
        return new Lift(piece, craft, new Place(at.x(), at.z(), -at.y(), Num.wrap(place.yaw() + to.yaw() - from.yaw())),
                ahead, aside, climb, held, engine, bombs, ready, steer, tilt);
    }

    /** How fast it is going, all told: across the ground and up or down. */
    public double speed() {
        return Math.sqrt(ahead * ahead + aside * aside + climb * climb);
    }

    /** How fast it is going, and which way, in the world's terms. */
    public Vec3 velocity() {
        return Num.heading(place.yaw()).multiply(ahead).add(Num.right(place.yaw()).multiply(aside)).add(0, -climb, 0);
    }

    /** Down at its moorings: its engine running, as low as it goes, and still. */
    public boolean moored() {
        return engine && place.height() <= craft.handling().low() + 0.5 && speed() < 1;
    }

    /**
     * Its bomb bay, {@code seconds} on: one bomb gone if {@code drop} asks for one and there is one
     * ready ({@link #RACK} after the last), aloft; or, moored, one more loaded back in, a bomb each
     * {@link #RELOAD}, until it is full. Unchanged if it has no bay.
     */
    public Lift loaded(final double seconds, final boolean drop) {
        if (craft.bombs() == 0) {
            return this;
        }
        final double wait = Math.max(0, ready - seconds);
        final int now;
        final double next;
        if (moored()) {
            final boolean loads = bombs < craft.bombs() && wait == 0;
            now = loads ? bombs + 1 : bombs;
            next = loads ? RELOAD : wait;
        } else {
            final boolean drops = drop && bombs > 0 && wait == 0;
            now = drops ? bombs - 1 : bombs;
            next = drops ? RACK : wait;
        }
        return new Lift(piece, craft, place, ahead, aside, climb, held, engine, now, next, steer, tilt);
    }

    /** Where {@code spot} of its frame (across, along, up from where it is) is, in the world. */
    public Vec3 at(final Course.Section spot) {
        return new Vec3(place.x(), -(place.height() + spot.up()), place.z())
                .add(Num.right(place.yaw()).multiply(spot.across()))
                .add(Num.heading(place.yaw()).multiply(spot.along()));
    }

    /** Where whoever takes it stands: on a platform, behind its console; at an airship, at the hatch in its floor. */
    public Vec3 pilot() {
        return at(craft.station());
    }

    /**
     * Whether {@code body}, among {@code world}, can reach its controls: standing on its deck
     * within reach of the console, for a platform; standing under its hatch, for a closed craft; and
     * for a land vehicle, either at its seat or on the ground beside the driver's door.
     */
    public boolean reaches(final Body body, final Obstacles world) {
        if (!body.grounded()) {
            return false;
        }
        final Vec3 at = body.position();
        final Vec3 station = pilot();
        if (craft.closed()) {
            // under its hatch, the hatch within reach overhead
            final double overhead = at.y() - station.y();
            return Math.hypot(at.x() - station.x(), at.z() - station.z()) <= REACH * 1.5
                    && overhead >= Body.TALL && overhead <= Body.TALL + 80;
        }
        if (craft instanceof Course.Chassis) {
            // from the ground beside the driver's door
            final Vec3 door = at(craft.door());
            if (Math.hypot(at.x() - door.x(), at.z() - door.z()) <= REACH * 1.2 && Math.abs(at.y() - door.y()) <= KERB) {
                return true;
            }
        }
        if (world.under(at.x(), at.z(), Body.FOOTING, body.height()) != piece) {
            return false;
        }
        return Math.hypot(at.x() - station.x(), at.z() - station.z()) <= REACH;
    }

    /** How far below where it is its underside is: what it rests on is that far down. */
    double below() {
        final Course.Section footing = craft.footing();
        return footing.halfUp() - footing.up();
    }

    /**
     * The highest thing under it where it is, among {@code world} and {@code also} (the avatar,
     * say), but for itself and what rides on it ({@code aboard}): what it rests on, or would
     * settle on, falling. The ground if nothing else.
     */
    public Support support(final Obstacles world, final Collection<Integer> aboard, final List<Block> also) {
        final double underside = place.height() - below();
        final Course.Section footing = craft.footing();
        final Block below = new Course.Section(footing.across(), footing.along(),
                (underside + 0.5 - 1) / 2 - place.height(), footing.halfAcross(), footing.halfAlong(),
                (underside + 0.5 + 1) / 2).placed(place);
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
        return !engine && Math.abs(place.height() - below() - under.top()) < 0.5;
    }

    /**
     * {@code seconds} on, among {@code world}, its own blocks in it or not, and those of what rides
     * on it, {@code aboard}; with {@code also} (the avatar, if not aboard) solid too. Its engine
     * running, flown by {@code controls} if held, and only easing to a stop if not; cut, falling
     * onto what is under it, or resting there.
     */
    public Lift step(final Controls controls, final double seconds, final Obstacles world,
                     final Collection<Integer> aboard, final List<Block> also) {
        if (craft instanceof Course.Chassis chassis) {
            return drive(chassis, controls, seconds, world, aboard, also);
        }
        return engine ? fly(controls, seconds, world, aboard) : fall(seconds, world, aboard, also);
    }

    /** As {@link #step(Controls, double, Obstacles, Collection, List)}, with nothing else solid. */
    public Lift step(final Controls controls, final double seconds, final Obstacles world,
                     final Collection<Integer> aboard) {
        return step(controls, seconds, world, aboard, List.of());
    }

    /** How much higher than its wheels stand a land vehicle drives up onto: a kerb, a street, a ramp's foot. */
    public static final double KERB = 60;
    /** How far below its wheels the ground may fall away and it still keeps to it, rather than leaving it. */
    public static final double SNAP = 25;
    /** At this speed, its wheels turn half as far as at a crawl. */
    public static final double STEER_FADE = 1800;
    /** The handbrake, how much harder than the brakes it stops it. */
    public static final double HANDBRAKE = 1.5;
    /** Going up faster than this, units a second, a land vehicle is launched off the top of a slope. */
    public static final double LAUNCH = 300;

    /**
     * A land vehicle, {@code seconds} on: steered, sped up, braked or rolling, turned about its back
     * wheels as its front wheels are turned, as racing games have it; then set on the ground under its
     * wheels there, tipped up or down as that slopes, or falling if it has driven off an edge. It
     * stops dead against anything higher than a kerb, or against {@code also}.
     */
    private Lift drive(final Course.Chassis chassis, final Controls controls, final double seconds,
                       final Obstacles world, final Collection<Integer> aboard, final List<Block> also) {
        final Course.Drive drive = chassis.drive();
        // the front wheels: turned as asked, the less the faster it goes, no faster than they turn
        final double most = drive.steer() / (1 + Math.abs(ahead) / STEER_FADE);
        final double wanted = held ? Num.clamp(controls.sideways(), -1, 1) * most : 0;
        final double turning = drive.steerRate() * seconds;
        final double nowSteer = steer + Num.clamp(wanted - steer, -turning, turning);
        // the pedals: ahead speeds it up (braking first, going back), back brakes then reverses
        final double pedal = held ? Num.clamp(controls.forward(), -1, 1) : 0;
        final double top = held && !controls.run() ? drive.top() * CAREFUL : drive.top();
        final double v;
        if (held && controls.lift() > 0) {
            v = toward(ahead, 0, drive.brake() * HANDBRAKE * seconds);
        } else if (pedal > 0) {
            v = ahead < 0 ? toward(ahead, 0, drive.brake() * seconds)
                    : ahead > top ? toward(ahead, top, drive.drag() * seconds)
                    : Math.min(top, ahead + drive.accel() * pedal * seconds);
        } else if (pedal < 0) {
            v = ahead > 0 ? toward(ahead, 0, drive.brake() * seconds)
                    : Math.max(-drive.reverse(), ahead + drive.accel() * pedal * seconds);
        } else {
            v = toward(ahead, 0, drive.drag() * seconds);
        }
        // turned about its back wheels, and moved along the way it heads half way through the turn
        final double turn = Math.toDegrees(v * seconds / chassis.wheelbase() * Math.tan(Math.toRadians(nowSteer)));
        final double yaw = Num.wrap(place.yaw() + turn);
        final Vec3 rear = Num.heading(place.yaw()).multiply(chassis.rear());
        final Vec3 newRear = Num.heading(yaw).multiply(chassis.rear());
        final Vec3 going = Num.heading(Num.wrap(place.yaw() + turn / 2)).multiply(v * seconds);
        final double x = Num.clamp(place.x() + rear.x() + going.x() - newRear.x(), -Body.EDGE, Body.EDGE);
        final double z = Num.clamp(place.z() + rear.z() + going.z() - newRear.z(), -Body.EDGE, Body.EDGE);
        // what else there is: not itself, nor what rides on it
        Obstacles others = world.without(piece);
        for (final int riding : aboard) {
            others = others.without(riding);
        }
        final Settled moved = settle(chassis, new Place(x, z, place.height(), yaw), v, nowSteer, seconds, others);
        if (!moved.lift().blocked(chassis, others, also, moved.aloft())) {
            return moved.lift();
        }
        // in the way: it stops dead where it was, its wheels as they were turned
        return settle(chassis, place, 0, nowSteer, seconds, others).lift();
    }

    /** {@code value} brought toward {@code target} by no more than {@code by}. */
    private static double toward(final double value, final double target, final double by) {
        return value + Num.clamp(target - value, -by, by);
    }

    /** A land vehicle set where it is to be, and whether it is off the ground there ({@code aloft}). */
    private record Settled(Lift lift, boolean aloft) {
    }

    /**
     * This vehicle at {@code at}, going {@code v} with its wheels turned {@code nowSteer}: set on the
     * ground under its wheels among {@code others}, as high as the ground under its middle and tipped
     * as it slopes from front to back, if it is on it or comes down onto it; falling if the ground
     * has fallen away under it.
     */
    private Settled settle(final Course.Chassis chassis, final Place at, final double v, final double nowSteer,
                           final double seconds, final Obstacles others) {
        // the ground under the wheels of each axle, no higher than a kerb above where they are: each
        // where it is along it, as it is tipped, so that up a steep slope its front wheels find it too
        final double underside = place.height() - chassis.ride();
        final double lean = Math.tan(Math.toRadians(tilt));
        final double reach = underside + KERB;
        final List<Double> axles = chassis.axles();
        final double[] ground = new double[axles.size()];
        for (int k = 0; k < axles.size(); k++) {
            for (final double side : new double[] {-1, 1}) {
                final Vec3 wheel = new Vec3(at.x(), 0, at.z())
                        .add(Num.right(at.yaw()).multiply(side * chassis.wheelOut()))
                        .add(Num.heading(at.yaw()).multiply(axles.get(k)));
                ground[k] = Math.max(ground[k], others.floor(wheel.x(), wheel.z(), chassis.wheelWidth() / 2,
                        reach + lean * axles.get(k)));
            }
        }
        double back = 0;
        for (int k = 1; k < ground.length; k++) {
            back += ground[k];
        }
        back /= ground.length - 1;
        // going up a slope fast, past its top the nose keeps the line it was climbing along until
        // the back wheels leave it too: it is launched off a ramp's head, rather than tipping over it
        final double line = back + Math.tan(Math.toRadians(tilt)) * chassis.wheelbase();
        final double front = climb > LAUNCH && ground[0] < line ? line : ground[0];
        // under the middle: between the front wheels and the back
        double middle = back + (front - back) * (0 - chassis.rear()) / chassis.wheelbase();
        final double slope = Math.toDegrees(Math.atan2(front - back, chassis.wheelbase()));
        // and never lower than lets its underside clear the ground under all of it: over a ramp's crest
        // between its wheels, it rests on its belly, high-centred, rather than sinking into it
        final double rise = Math.tan(Math.toRadians(slope));
        final double clearance = chassis.ride() - Course.Chassis.BED;
        final double half = chassis.wheelOut() + chassis.wheelWidth() / 2;
        final int rows = (int) Math.ceil(chassis.length() / 100) + 1;
        for (int r = 0; r < rows; r++) {
            final double along = -chassis.length() / 2 + chassis.length() * r / (rows - 1);
            for (final double across : new double[] {-half, 0, half}) {
                final Vec3 under = new Vec3(at.x(), 0, at.z()).add(Num.right(at.yaw()).multiply(across))
                        .add(Num.heading(at.yaw()).multiply(along));
                final double floor = others.floor(under.x(), under.z(), 1, reach + lean * along);
                middle = Math.max(middle, floor - rise * along - clearance);
            }
        }
        final double target = middle + chassis.ride();
        // on its wheels, it keeps to the ground going down too, unless it is going up off it
        final boolean grounded = climb <= 0 && place.height() - target <= SNAP;
        if (target >= place.height() || grounded) {
            // on its wheels, going up or down as the ground does; how fast it goes up is what carries
            // it on up, off a ramp's head
            final double rising = seconds > 0 ? Math.max(0, (target - place.height()) / seconds) : 0;
            return new Settled(new Lift(piece, craft, new Place(at.x(), at.z(), target, at.yaw()), v, 0, rising, held,
                    true, bombs, ready, nowSteer, slope), false);
        }
        // off an edge, or a ramp's head: falling, as everything does, until it comes down onto the ground
        final double falling = climb - Body.GRAVITY * seconds;
        final double height = place.height() + (climb + falling) / 2 * seconds;
        if (height <= target) {
            return new Settled(new Lift(piece, craft, new Place(at.x(), at.z(), target, at.yaw()), v, 0, 0, held, true,
                    bombs, ready, nowSteer, slope), false);
        }
        return new Settled(new Lift(piece, craft, new Place(at.x(), at.z(), height, at.yaw()), v, 0, falling, held,
                true, bombs, ready, nowSteer, tilt), true);
    }

    /**
     * Whether a land vehicle here is in the way of something among {@code others}, or of
     * {@code also}: anything standing more than a kerb higher than the ground it stands on, at any
     * point under it, and lower than the top of whoever rides it. Looked for at points a metre or
     * less apart all over it, the ground under each going up or down as it is tipped: a ramp it is
     * on is never in its way; a ramp's side, standing high beside it, is.
     */
    private boolean blocked(final Course.Chassis chassis, final Obstacles others, final List<Block> also,
                            final boolean aloft) {
        final double middle = place.height() - chassis.ride();
        // on its wheels, the ground under it slopes as it is tipped; in the air, it is level under it
        final double rise = aloft ? 0 : Math.tan(Math.toRadians(tilt));
        final double over = place.height() + Body.TALL + 30;
        final double half = chassis.wheelOut() + chassis.wheelWidth() / 2;
        final int rows = (int) Math.ceil(chassis.length() / 100) + 1;
        final Vec3 ahead = Num.heading(place.yaw());
        final Vec3 right = Num.right(place.yaw());
        for (int r = 0; r < rows; r++) {
            final double along = -chassis.length() / 2 + chassis.length() * r / (rows - 1);
            final double ground = middle + rise * along;
            for (final double across : new double[] {-half, 0, half}) {
                final double x = place.x() + ahead.x() * along + right.x() * across;
                final double z = place.z() + ahead.z() * along + right.z() * across;
                if (standsIn(others.blocks(), x, z, ground, over) || standsIn(also, x, z, ground, over)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Whether any of {@code blocks} stands at ({@code x}, {@code z}) more than a kerb over {@code ground}, and lower than {@code over}. */
    private static boolean standsIn(final List<Block> blocks, final double x, final double z, final double ground,
                                    final double over) {
        for (final Block block : blocks) {
            if (block.bottom() < over && block.covers(x, z, 0) && block.topAt(x, z, 0) > ground + KERB) {
                return true;
            }
        }
        return false;
    }

    private Lift fall(final double seconds, final Obstacles world, final Collection<Integer> aboard,
                      final List<Block> also) {
        final Support under = support(world, aboard, also);
        final double dropping = climb - Body.GRAVITY * seconds;
        final double height = place.height() + (climb + dropping) / 2 * seconds;
        if (height - below() <= under.top()) {
            // settled: on it, and still
            return new Lift(piece, craft, new Place(place.x(), place.z(), under.top() + below(), place.yaw()), 0, 0,
                    0, held, false, bombs, ready, steer, tilt);
        }
        return new Lift(piece, craft, new Place(place.x(), place.z(), height, place.yaw()), 0, 0, dropping, held, false, bombs, ready, steer, tilt);
    }

    private Lift fly(final Controls controls, final double seconds, final Obstacles world,
                     final Collection<Integer> aboard) {
        final Course.Handling handles = craft.handling();
        final double pace = held ? (controls.run() ? 1 : CAREFUL) : 0;
        final double nowAhead = ease(ahead, Num.clamp(controls.forward(), -1, 1) * handles.speed() * pace, seconds);
        final double nowAside = ease(aside,
                Num.clamp(controls.sideways(), -1, 1) * handles.speed() * handles.sideways() * pace, seconds);
        final double nowClimb = ease(climb, Num.clamp(controls.lift(), -1, 1) * handles.climb() * (held ? 1 : 0),
                seconds);
        // turned as asked, as fast as it will turn
        final double turn = handles.turn() > 0
                ? Num.clamp(controls.turn(), -handles.turn() * seconds, handles.turn() * seconds) : controls.turn();
        final double yaw = held ? Num.wrap(place.yaw() + turn) : place.yaw();
        // the average speeds over the step, along the way it heads at the end of it
        final double a = (ahead + nowAhead) / 2 * seconds;
        final double b = (aside + nowAside) / 2 * seconds;
        final double up = (climb + nowClimb) / 2 * seconds;
        final Vec3 across = Num.heading(yaw).multiply(a).add(Num.right(yaw).multiply(b));
        final double x = Num.clamp(place.x() + across.x(), -Body.EDGE, Body.EDGE);
        final double z = Num.clamp(place.z() + across.z(), -Body.EDGE, Body.EDGE);
        final double height = Num.clamp(place.height() + up, handles.low(), handles.high());
        final double climbing = height == handles.low() || height == handles.high() ? 0 : nowClimb;
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
                return new Lift(piece, craft, tries[k], goesAcross ? nowAhead : 0, goesAcross ? nowAside : 0,
                        goesUp ? climbing : 0, held, true, bombs, ready, steer, tilt);
            }
        }
        return new Lift(piece, craft, place, 0, 0, 0, held, true, bombs, ready, steer, tilt);
    }

    /**
     * Whether at {@code at} the room it needs is clear of everything solid in {@code world} but
     * itself and what rides on it, {@code aboard}: that goes where it goes.
     */
    public boolean clear(final Place at, final Obstacles world, final Collection<Integer> aboard) {
        final Block room = craft.room().placed(at);
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
