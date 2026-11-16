package com.example.fxsuite.avatar.sim;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What moves of itself: the airships going round on their routes, and the bombs they drop.
 * Pure state, stepped with the rest of the world ({@link Play}).
 *
 * <ul>
 *   <li><b>The airships</b> are where their routes put them at {@code time}, the seconds since
 *       the start ({@link Course.Piece#at}): nothing about them needs keeping but the time.</li>
 *   <li><b>Each makes bombing runs</b>, each in its turn: every {@link #RUN} seconds its bomb bay's
 *       doors are opened, hydraulically ({@link Actuator}), and shut again {@link #OPEN_FOR} after.
 *       While they are open, its bombs just drop, one every {@link #STICK}, from under its gondola,
 *       going as fast as the airship was; shut, none. Carpet bombing: it drops wherever it is, whatever
 *       is under it.</li>
 *   <li><b>Under siege</b> ({@code siege}, null for none: {@link Siege}), the siege's airships instead
 *       come and go as it says, and open their bays and drop their bombs when it says: over their
 *       target, or a building they chance to bomb. Their bombs fall past each other.</li>
 *   <li><b>A bomb falls</b> as everything does ({@link Body#GRAVITY}), and goes off at the first
 *       thing in its way, a block or the ground. Every piece that can break within
 *       {@link #BLAST} of it takes a blow ({@link Weapon#BOMB}), whether a box or shaped more closely.
 *       It does nothing to the avatar, which takes no harm yet: it falls through it.</li>
 * </ul>
 *
 * @param time    seconds since the start
 * @param bombs   those falling
 * @param dropped how many have been dropped, each bomb numbered by it
 * @param bays    each airship's bomb bay's doors, by its piece, as they are driven
 * @param siege   the siege, as it stands; null, none
 */
public record Sky(double time, List<Bomb> bombs, long dropped, Map<Integer, Actuator> bays, Siege.State siege) {

    /** The sky at the start: the airships where their routes begin, and nothing falling. */
    public static final Sky START = new Sky(0, List.of(), 0);
    /** Seconds from one airship's bombing run to its next; its bay asked open so long of each. */
    public static final double RUN = 12;
    public static final double OPEN_FOR = 6;
    /** Seconds between its bombs, its bay open: a stick of them. */
    public static final double STICK = 1;
    /** How far its bay's doors open, degrees, and how fast, degrees a second. */
    public static final double BAY_OPENS = 90;
    public static final double BAY_RATE = 60;
    /** How far a bomb's blast reaches, in units. */
    public static final double BLAST = 350;
    /** How far under the gondola a bomb starts. */
    public static final double BELOW = 40;

    public Sky {
        bombs = List.copyOf(bombs);
        bays = Map.copyOf(bays);
    }

    /** No siege. */
    public Sky(final double time, final List<Bomb> bombs, final long dropped, final Map<Integer, Actuator> bays) {
        this(time, bombs, dropped, bays, null);
    }

    /** No bomb bay driven yet: each as it starts, shut. */
    public Sky(final double time, final List<Bomb> bombs, final long dropped) {
        this(time, bombs, dropped, Map.of());
    }

    /** How far airship {@code piece}'s bomb bay's doors are open, degrees: shut, 0, if it has none driven. */
    public double bay(final int piece) {
        final Actuator bay = bays.get(piece);
        return bay == null ? 0 : bay.angle();
    }

    /** A bomb in the air: the {@code number}th dropped, from airship {@code from}, at {@code at} going at {@code velocity}. */
    public record Bomb(long number, Vec3 at, Vec3 velocity, int from) {
    }

    /**
     * The sky after a step, what happened in it, and the blows its bombs dealt, for the world to take;
     * and, under siege, the pieces it made whole again as they now are ({@code revived}: an airship come,
     * the hall rebuilt), and the airships it sent another way ({@code rerouted}).
     */
    public record Stepped(Sky sky, List<Event> events, List<Blow> blows, Map<Integer, Course.Piece> revived,
                          Map<Integer, Course.Piece> rerouted) {

        public Stepped {
            events = List.copyOf(events);
            blows = List.copyOf(blows);
            revived = Map.copyOf(revived);
            rerouted = Map.copyOf(rerouted);
        }

        /** Nothing made whole, none sent another way. */
        public Stepped(final Sky sky, final List<Event> events, final List<Blow> blows) {
            this(sky, events, blows, Map.of(), Map.of());
        }
    }

    /** The same, with one more bomb let go: from vehicle {@code from}, at {@code at}, going at {@code velocity}. */
    public Sky dropped(final Vec3 at, final Vec3 velocity, final int from) {
        final List<Bomb> falling = new ArrayList<>(bombs);
        falling.add(new Bomb(dropped, at, velocity, from));
        return new Sky(time, falling, dropped + 1, bays, siege);
    }

    /**
     * {@code seconds} on, among {@code world} as it now stands, the airships still flying being
     * {@code damage}'s: the bombs fall, those that meet something go off, and each airship whose
     * time has come lets another go.
     */
    public Stepped step(final double seconds, final Damage damage, final Obstacles world) {
        final double to = time + seconds;
        final List<Event> events = new ArrayList<>();
        final List<Blow> blows = new ArrayList<>();
        // the siege's own: they fall past each other
        Obstacles past = world;
        if (siege != null) {
            for (final Siege.Raider raider : siege.raiders()) {
                past = past.without(raider.piece());
            }
        }
        final List<Bomb> falling = new ArrayList<>();
        for (final Bomb bomb : bombs) {
            final Vec3 velocity = bomb.velocity().add(0, Body.GRAVITY * seconds, 0);
            // the average of the two: exact under constant gravity
            final Vec3 move = bomb.velocity().add(velocity).multiply(seconds / 2);
            final double reach = move.magnitude();
            final Obstacles through = siege != null && siege.raider(bomb.from()) ? past : world;
            final Hit hit = reach > 0 ? through.ray(bomb.at(), move.multiply(1 / reach), reach) : null;
            if (hit == null) {
                falling.add(new Bomb(bomb.number(), bomb.at().add(move), velocity, bomb.from()));
            } else {
                events.add(new Event.Exploded(hit.at(), hit.on()));
                blast(hit.at(), damage, world, blows);
            }
        }
        long count = dropped;
        final Map<Integer, Actuator> driven = new java.util.HashMap<>();
        // under siege: its airships come, and bomb as it says
        Siege.State besieged = siege;
        Map<Integer, Course.Piece> revived = Map.of();
        Map<Integer, Course.Piece> rerouted = Map.of();
        if (siege != null) {
            final Siege.State.Stepped orders = siege.step(time, seconds, damage, piece -> {
                final Actuator bay = bays.get(piece);
                return bay != null && bay.stage() == Actuator.Stage.OPEN;
            });
            besieged = orders.state();
            revived = orders.revived();
            rerouted = orders.rerouted();
            events.addAll(orders.events());
            for (final Map.Entry<Integer, Boolean> asked : orders.bays().entrySet()) {
                final int piece = asked.getKey();
                final Course.Piece ship = rerouted.getOrDefault(piece, damage.piece(piece));
                final Actuator was = bays.get(piece);
                final Actuator bay = (was != null ? was : Actuator.shut(Actuator.Drive.HYDRAULIC, 0, BAY_OPENS, BAY_RATE))
                        .asked(asked.getValue()).step(seconds).actuator();
                driven.put(piece, bay);
                if (was == null ? asked.getValue() : was.open() != asked.getValue()) {
                    events.add(new Event.Bay(piece, asked.getValue(), under(ship, to)));
                }
            }
            for (final int piece : orders.drops()) {
                final Course.Piece ship = rerouted.getOrDefault(piece, damage.piece(piece));
                final Vec3 at = under(ship, to).add(0, BELOW, 0);
                falling.add(new Bomb(count++, at, going(ship, seconds), piece));
                events.add(new Event.Dropped(at, piece));
            }
        }
        for (final int piece : damage.flying()) {
            if (siege != null && siege.raider(piece)) {
                continue;
            }
            // each in its turn, a third of a run apart, by its number: its bay asked open for the first
            // part of each run, then shut
            final double offset = RUN * (piece % 3) / 3;
            final boolean open = (to + offset) % RUN < OPEN_FOR;
            final Actuator was = bays.get(piece);
            // (from the start, as its run has it already, silently: it was not heard beginning)
            final Actuator bay = (was != null ? was
                    : Actuator.shut(Actuator.Drive.HYDRAULIC, 0, BAY_OPENS, BAY_RATE).placed(open ? BAY_OPENS : 0))
                    .asked(open).step(seconds).actuator();
            driven.put(piece, bay);
            // heard: its bay asked to open, or to shut, its doors under its gondola
            if (was != null && was.open() != open) {
                events.add(new Event.Bay(piece, open, under(damage.piece(piece), to)));
            }
            // open, its bombs just drop
            if (bay.stage() == Actuator.Stage.OPEN
                    && Math.floor((to + offset) / STICK) > Math.floor((time + offset) / STICK)) {
                final Course.Piece ship = damage.piece(piece);
                final Vec3 at = under(ship, to).add(0, BELOW, 0);
                falling.add(new Bomb(count++, at, going(ship, seconds), piece));
                events.add(new Event.Dropped(at, piece));
            }
        }
        return new Stepped(new Sky(to, falling, count, driven, besieged), events, blows, revived, rerouted);
    }

    /** Where under airship {@code ship}'s gondola its bay is, {@code seconds} after the start: the world's y down. */
    private static Vec3 under(final Course.Piece ship, final double seconds) {
        final Block gondola = Course.part(ship, Course.GONDOLA).placed(ship.route().at(seconds));
        return new Vec3(gondola.x(), -gondola.bottom(), gondola.z());
    }

    /** How fast airship {@code ship} goes, across the ground, over the last {@code seconds} before {@link #time}'s next. */
    private Vec3 going(final Course.Piece ship, final double seconds) {
        if (seconds <= 0) {
            return Vec3.ZERO;
        }
        final Place now = ship.route().at(time + seconds);
        final Place before = ship.route().at(time);
        return new Vec3(now.x() - before.x(), 0, now.z() - before.z()).multiply(1 / seconds);
    }

    /** A blow on each piece that can still break within {@link #BLAST} of {@code at}: a box, or shaped more closely. */
    private static void blast(final Vec3 at, final Damage damage, final Obstacles world, final List<Blow> blows) {
        final double height = -at.y();
        final Set<Integer> reached = new HashSet<>();
        for (int i = 0; i < world.blocks().size(); i++) {
            final int piece = world.pieces().get(i);
            if (reached.contains(piece) || !damage.piece(piece).kind().breakable() || damage.broken(piece)) {
                continue;
            }
            final Block block = world.blocks().get(i);
            if (height >= block.bottom() - BLAST && height <= block.top() + BLAST
                    && block.covers(at.x(), at.z(), BLAST)) {
                reached.add(piece);
                blows.add(new Blow(at, new Vec3(0, -1, 0), Surface.BLOCK, Weapon.BOMB, piece));
            }
        }
        // what is shaped more closely: a hall, a tower, a pyramid, a wall
        final Block round = new Block(at.x(), at.z(), BLAST, BLAST, 0, height - BLAST, height + BLAST);
        for (final int i : world.volumesNear(at.x(), at.z(), BLAST)) {
            final int piece = world.volumePieces().get(i);
            if (reached.contains(piece) || !damage.piece(piece).kind().breakable() || damage.broken(piece)) {
                continue;
            }
            if (world.volumes().get(i).overlaps(round)) {
                reached.add(piece);
                blows.add(new Blow(at, new Vec3(0, -1, 0), Surface.BLOCK, Weapon.BOMB, piece));
            }
        }
    }
}
