package com.example.fxsuite.avatar.sim;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Everything that moves: the avatar, what its weapons have done to the world, and what moves
 * of itself (the airships and their bombs, {@link Sky}), stepped together. Pure state; what
 * happened comes out of each step beside it.
 *
 * <p>A step goes in five phases:
 * <ol>
 *   <li><b>The world moves on:</b> the airships go on along their routes, and whoever stands
 *       on one is carried with it, turned as it turns.</li>
 *   <li><b>Inputs:</b> what the player did once ({@link Controls#actions}) is done, in order;
 *       the held keys, the turn and the aim are set.</li>
 *   <li><b>Act:</b> the avatar moves on among what still stands, where it now is, and its
 *       weapon deals its {@link Blow}s; the bombs fall, and those that meet something go off,
 *       dealing theirs. Intents, not yet anything that happened.</li>
 *   <li><b>Resolve:</b> the world takes the blows, in the order they were dealt ({@link Damage}).</li>
 *   <li><b>Report:</b> what happened goes out: the avatar's own events, the bombs', then what
 *       came of each blow (struck, with what the piece has left; what broke).</li>
 * </ol>
 * The blows never leave the step: they follow from the state and the inputs, and only what came
 * of them is an output.
 */
public record Play(Avatar avatar, Damage damage, Sky sky) {

    /**
     * Which rules the world steps by. Raise it whenever a change would step the same inputs to a
     * different world, so a recording made under the old rules says so rather than going astray.
     */
    public static final int RULES = 2;

    /** The avatar and the world, with the sky as it starts. */
    public Play(final Avatar avatar, final Damage damage) {
        this(avatar, damage, Sky.START);
    }

    /** The world at the start: the avatar standing at the start, with the pack on or not, and the course unharmed. */
    public static Play start(final boolean pack) {
        return new Play(new Avatar(Body.standing(Vec3.ZERO, Course.START_YAW), pack), Damage.of(new Course()));
    }

    /**
     * The whole world stirred into one number, to the bit: two worlds with the same checksum are,
     * as near as matters, the same. The avatar and the sky are all records, so their text is
     * exact and complete.
     */
    public long checksum() {
        long sum = damage.checksum();
        final String text = avatar.toString() + sky.toString();
        for (int i = 0; i < text.length(); i++) {
            sum = Num.mix(sum, text.charAt(i));
        }
        return sum;
    }

    /** The world after a step, and what happened in it. */
    public record Stepped(Play play, List<Event> events) {

        public Stepped {
            events = List.copyOf(events);
        }
    }

    /** What still stands, where it now is: to move among, to shoot at, to fall onto. */
    public Obstacles obstacles() {
        return obstacles(sky.time());
    }

    /** The airships still flying, each where it now is. */
    public List<Course.Piece> airships() {
        return damage.moving().stream().map(i -> damage.piece(i).at(sky.time())).toList();
    }

    /** One step of {@code seconds}, with {@code controls}. */
    public Stepped step(final Controls controls, final double seconds) {
        // the world moves on, carrying whoever stands on what moves
        final double to = sky.time() + seconds;
        final Avatar carried = carried(avatar, sky.time(), to);
        final Obstacles now = obstacles(to);
        // inputs, and act: the avatar does what it was asked, moves, and deals its blows; the bombs fall
        final Avatar.Stepped acted = carried.step(controls, seconds, now);
        final Sky.Stepped fell = sky.step(seconds, damage, now);
        // resolve: the world takes the blows, the avatar's and then the bombs'
        final List<Blow> blows = new ArrayList<>(acted.blows());
        blows.addAll(fell.blows());
        final Damage.Taken taken = damage.took(blows, to);
        // report: the avatar's own events, the bombs', then what came of the blows
        final List<Event> happened = new ArrayList<>(acted.events());
        happened.addAll(fell.events());
        happened.addAll(taken.events());
        return new Stepped(new Play(acted.avatar(), taken.damage(), fell.sky()), happened);
    }

    /** {@code avatar}, carried by what it stands on as that moves on from {@code from} to {@code to}, if it moves. */
    private Avatar carried(final Avatar avatar, final double from, final double to) {
        final Body body = avatar.body();
        if (!body.grounded()) {
            return avatar;
        }
        final int piece = obstacles(from).under(body.position().x(), body.position().z(), Body.FOOTING,
                body.height());
        if (piece < 0 || !damage.piece(piece).moves()) {
            return avatar;
        }
        final Course.Route route = damage.piece(piece).route();
        return avatar.carried(route.at(from), route.at(to));
    }

    /** What still stands, {@code seconds} after the start: the course less what is broken, the airships where they then are. */
    private Obstacles obstacles(final double seconds) {
        final Obstacles built = damage.obstacles();
        final List<Integer> moving = damage.moving();
        if (moving.isEmpty()) {
            return built;
        }
        // each airship's blocks, in the order it was built with, put where it now is
        final Map<Integer, List<Block>> placed = new TreeMap<>();
        for (final int piece : moving) {
            placed.put(piece, damage.piece(piece).at(seconds).solid());
        }
        final Map<Integer, Integer> next = new TreeMap<>();
        final List<Block> blocks = new ArrayList<>(built.blocks().size());
        for (int i = 0; i < built.blocks().size(); i++) {
            final int piece = built.pieces().get(i);
            final List<Block> now = placed.get(piece);
            if (now == null) {
                blocks.add(built.blocks().get(i));
            } else {
                final int k = next.merge(piece, 1, Integer::sum) - 1;
                blocks.add(now.get(k));
            }
        }
        return new Obstacles(blocks, built.gates(), built.pieces());
    }
}
