package com.example.fxsuite.avatar.sim;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * What moves of itself: the airships going round on their routes, and the bombs they drop.
 * Pure state, stepped with the rest of the world ({@link Play}).
 *
 * <ul>
 *   <li><b>The airships</b> are where their routes put them at {@code time}, the seconds since
 *       the start ({@link Course.Piece#at}): nothing about them needs keeping but the time.</li>
 *   <li><b>Each drops a bomb</b> every {@link #DROP_EVERY} seconds, each in its turn, from under
 *       its gondola, going as fast as the airship was. Carpet bombing: it drops wherever it is,
 *       whatever is under it.</li>
 *   <li><b>A bomb falls</b> as everything does ({@link Body#GRAVITY}), and goes off at the first
 *       thing in its way, a block or the ground. Every piece that can break within
 *       {@link #BLAST} of it takes a blow ({@link Weapon#BOMB}). It does nothing to the avatar,
 *       which takes no harm yet: it falls through it.</li>
 * </ul>
 *
 * @param time    seconds since the start
 * @param bombs   those falling
 * @param dropped how many have been dropped, each bomb numbered by it
 */
public record Sky(double time, List<Bomb> bombs, long dropped) {

    /** The sky at the start: the airships where their routes begin, and nothing falling. */
    public static final Sky START = new Sky(0, List.of(), 0);
    /** Seconds between one airship's bombs. */
    public static final double DROP_EVERY = 3;
    /** How far a bomb's blast reaches, in units. */
    public static final double BLAST = 350;
    /** How far under the gondola a bomb starts. */
    public static final double BELOW = 40;

    public Sky {
        bombs = List.copyOf(bombs);
    }

    /** A bomb in the air: the {@code number}th dropped, from airship {@code from}, at {@code at} going at {@code velocity}. */
    public record Bomb(long number, Vec3 at, Vec3 velocity, int from) {
    }

    /** The sky after a step, what happened in it, and the blows its bombs dealt, for the world to take. */
    public record Stepped(Sky sky, List<Event> events, List<Blow> blows) {

        public Stepped {
            events = List.copyOf(events);
            blows = List.copyOf(blows);
        }
    }

    /** The same, with one more bomb let go: from vehicle {@code from}, at {@code at}, going at {@code velocity}. */
    public Sky dropped(final Vec3 at, final Vec3 velocity, final int from) {
        final List<Bomb> falling = new ArrayList<>(bombs);
        falling.add(new Bomb(dropped, at, velocity, from));
        return new Sky(time, falling, dropped + 1);
    }

    /**
     * {@code seconds} on, among {@code world} as it now stands, the airships still flying being
     * {@code damage}'s: the bombs fall, those that meet something go off, and each airship whose
     * time has come lets another go.
     */
    public Stepped step(final double seconds, final Damage damage, final Obstacles world) {
        final double to = time + seconds;
        final List<Bomb> falling = new ArrayList<>();
        final List<Event> events = new ArrayList<>();
        final List<Blow> blows = new ArrayList<>();
        for (final Bomb bomb : bombs) {
            final Vec3 velocity = bomb.velocity().add(0, Body.GRAVITY * seconds, 0);
            // the average of the two: exact under constant gravity
            final Vec3 move = bomb.velocity().add(velocity).multiply(seconds / 2);
            final double reach = move.magnitude();
            final Hit hit = reach > 0 ? world.ray(bomb.at(), move.multiply(1 / reach), reach) : null;
            if (hit == null) {
                falling.add(new Bomb(bomb.number(), bomb.at().add(move), velocity, bomb.from()));
            } else {
                events.add(new Event.Exploded(hit.at(), hit.on()));
                blast(hit.at(), damage, world, blows);
            }
        }
        long count = dropped;
        for (final int piece : damage.flying()) {
            // each in its turn, a third of the interval apart, by its number
            final double offset = DROP_EVERY * (piece % 3) / 3;
            if (Math.floor((to + offset) / DROP_EVERY) > Math.floor((time + offset) / DROP_EVERY)) {
                final Course.Route route = damage.piece(piece).route();
                final Place now = route.at(to);
                final Place before = route.at(time);
                final Block gondola = Course.GONDOLA.placed(now);
                final Vec3 at = new Vec3(gondola.x(), -(gondola.bottom() - BELOW), gondola.z());
                final Vec3 going = seconds > 0
                        ? new Vec3(now.x() - before.x(), 0, now.z() - before.z()).multiply(1 / seconds) : Vec3.ZERO;
                falling.add(new Bomb(count++, at, going, piece));
                events.add(new Event.Dropped(at, piece));
            }
        }
        return new Stepped(new Sky(to, falling, count), events, blows);
    }

    /** A blow on each piece that can still break within {@link #BLAST} of {@code at}. */
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
    }
}
