package com.example.fxsuite.avatar.sim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * What the weapons have done to the world: how worn down each piece of the {@link Course} is,
 * and which are broken. Pure, and never changed in place: each blow gives a new one.
 *
 * <ul>
 *   <li><b>What can break:</b> crates and the targets, the dummies and the drones
 *       ({@link Course.Kind#breakable}). Pillars and gates are what the world is built on, and
 *       stand whatever is thrown at them.</li>
 *   <li><b>How tough:</b> a dummy takes {@link #DUMMY}, a drone {@link #DRONE}, and an airship,
 *       armoured as it is, {@link #AIRSHIP}: most of ten seconds of the Gatling gun. A crate's
 *       toughness goes by its size, from {@link #CRATE} for the smallest up: a small crate falls to
 *       a short burst, the biggest takes a couple of seconds of the Gatling gun.</li>
 *   <li><b>A blow</b> takes away what its weapon does ({@link Weapon#blow}): a round, or a stroke
 *       of the glaive. The one that takes the last of it breaks the piece.</li>
 *   <li><b>Broken</b> is for good, until the demo starts again: the piece is gone from the world
 *       ({@link #obstacles}), so rounds fly on past where it stood, and whatever stood on it
 *       falls.</li>
 * </ul>
 *
 * <p>The weapons deal {@link Blow}s, and the world takes them here, within the step; what came
 * of each goes out: struck, with what the piece has left ({@link Event.Struck}), and what broke
 * ({@link Event.Broke}), for the graphics to throw the pieces about and the sound to be heard.
 */
public final class Damage {

    /** How tough a dummy is, a drone, an airship, and the smallest crate. */
    public static final double DUMMY = 300;
    public static final double DRONE = 120;
    /** A watermelon: three rifle rounds, four of the Gatling gun, one stroke of the glaive. */
    public static final double MELON = 25;
    public static final double AIRSHIP = 3000;
    public static final double CRATE = 80;
    /** A crate's toughness grows by one for each this much of its volume, in cubic units. */
    public static final double CRATE_VOLUME = 30_000;

    private final List<Course.Piece> pieces;
    private final double[] toughness;
    private final double[] health;
    private final Obstacles obstacles;

    private Damage(final List<Course.Piece> pieces, final double[] toughness, final double[] health,
                   final Obstacles obstacles) {
        this.pieces = pieces;
        this.toughness = toughness;
        this.health = health;
        this.obstacles = obstacles;
    }

    /** The course unharmed. */
    public static Damage of(final Course course) {
        final List<Course.Piece> pieces = course.pieces();
        final double[] toughness = new double[pieces.size()];
        for (int i = 0; i < toughness.length; i++) {
            toughness[i] = toughness(pieces.get(i));
        }
        return new Damage(pieces, toughness, toughness.clone(), course.obstacles());
    }

    /** How much a piece can take before it breaks; infinite for what cannot break. */
    public static double toughness(final Course.Piece piece) {
        final Block block = piece.block();
        return switch (piece.kind()) {
            case DUMMY -> DUMMY;
            case DRONE -> DRONE;
            case AIRSHIP -> AIRSHIP;
            case CRATE -> Math.round(CRATE + 4 * block.halfWidth() * block.halfDepth()
                    * (block.top() - block.bottom()) / CRATE_VOLUME);
            case MELON -> MELON;
            case PILLAR, GATE, PLATFORM, STALL -> Double.POSITIVE_INFINITY;
        };
    }

    /** The new damage, and what came of the blows: each one struck, and what broke. */
    public record Taken(Damage damage, List<Event> events) {
        public Taken {
            events = List.copyOf(events);
        }
    }

    /**
     * {@code blows} taken, in order: each one on a piece that can break wears it down, and the one
     * that takes the last of it breaks it. Blows on the ground, on what cannot break, or on what is
     * already broken change nothing.
     *
     * <p>What came of each blow goes out: a {@link Event.Struck} for every one, carrying what the
     * piece has left after it, and a {@link Event.Broke} after the one that broke it.
     */
    public Taken took(final List<Blow> blows) {
        return took(blows, 0);
    }

    /** As {@link #took(List)}, {@code seconds} after the start: what moves of itself breaks where it then is. */
    public Taken took(final List<Blow> blows, final double seconds) {
        return took(blows, piece -> pieces.get(piece).at(seconds));
    }

    /**
     * As {@link #took(List)}, with {@code where} telling where each piece now is: what moves breaks
     * where it then is, and that is where its pieces fly from.
     */
    public Taken took(final List<Blow> blows, final java.util.function.IntFunction<Course.Piece> where) {
        double[] left = null;
        Obstacles standing = obstacles;
        final List<Event> happened = new ArrayList<>();
        for (final Blow blow : blows) {
            final int piece = blow.piece();
            if (!harms(blow)) {
                happened.add(Event.Struck.of(blow, piece >= 0 && piece < health.length ? share(piece, health) : 1));
                continue;
            }
            final double[] now = left == null ? health : left;
            if (now[piece] <= 0) {
                // broken earlier this step: struck where it stood, and nothing more to break
                happened.add(Event.Struck.of(blow, 0));
                continue;
            }
            if (left == null) {
                left = health.clone();
            }
            left[piece] = Math.max(0, left[piece] - blow.by().blow());
            happened.add(Event.Struck.of(blow, share(piece, left)));
            if (left[piece] <= 0) {
                final Course.Piece what = where.apply(piece);
                standing = standing.without(piece);
                happened.add(new Event.Broke(piece, what.kind(), what.block(), blow.at(), blow.by()));
            }
        }
        return left == null ? new Taken(this, happened)
                : new Taken(new Damage(pieces, toughness, left, standing), happened);
    }

    private boolean harms(final Blow blow) {
        final int piece = blow.piece();
        return piece >= 0 && piece < health.length && pieces.get(piece).kind().breakable() && blow.by().blow() > 0;
    }

    /** How much of what it can take piece {@code piece} has in {@code health}: 1 for what cannot break. */
    private double share(final int piece, final double[] health) {
        return Double.isInfinite(toughness[piece]) ? 1 : health[piece] / toughness[piece];
    }

    /** What is still standing, to move among and shoot at: the course, less what is broken. */
    public Obstacles obstacles() {
        return obstacles;
    }

    /** How much piece {@code piece} has left of what it can take: 1 unharmed, 0 broken; always 1 for what cannot break. */
    public double left(final int piece) {
        return share(piece, health);
    }

    /** What piece {@code piece} has left to take, in the weapons' own units; infinite for what cannot break. */
    public double health(final int piece) {
        return health[piece];
    }

    /** Every piece's health, stirred together, to the bit: for checking two worlds are the same. */
    public long checksum() {
        long sum = Num.SEED;
        for (final double h : health) {
            sum = Num.mix(sum, Double.doubleToLongBits(h));
        }
        return sum;
    }

    public boolean broken(final int piece) {
        return health[piece] <= 0;
    }

    /** Piece {@code piece} of the course, as it was built: where it starts, if it moves ({@link Course.Piece#at}). */
    public Course.Piece piece(final int piece) {
        return pieces.get(piece);
    }

    /** The pieces that can move and are still standing, by number: the airships left, and the platforms. */
    public List<Integer> moving() {
        final List<Integer> moving = new ArrayList<>();
        for (int i = 0; i < health.length; i++) {
            if (pieces.get(i).moves() && health[i] > 0) {
                moving.add(i);
            }
        }
        return moving;
    }

    /** The pieces that fly of themselves and are still in the sky, by number: the airships left. */
    public List<Integer> flying() {
        return moving().stream().filter(i -> pieces.get(i).flies()).toList();
    }

    /** The pieces riding on piece {@code carrier} and still standing, by number: the stall on the barge. */
    public List<Integer> riding(final int carrier) {
        return moving().stream().filter(i -> pieces.get(i).carrier() == carrier).toList();
    }

    /** How many pieces there are, broken or not. */
    public int size() {
        return pieces.size();
    }

    /** How many pieces of {@code kind} there were, and how many of them are broken. */
    public int count(final Course.Kind kind) {
        return (int) pieces.stream().filter(p -> p.kind() == kind).count();
    }

    /** The pieces of {@code kind} still standing. */
    public List<Course.Piece> standing(final Course.Kind kind) {
        final List<Course.Piece> standing = new ArrayList<>();
        for (int i = 0; i < health.length; i++) {
            if (pieces.get(i).kind() == kind && health[i] > 0) {
                standing.add(pieces.get(i));
            }
        }
        return standing;
    }

    public int broken(final Course.Kind kind) {
        int count = 0;
        for (int i = 0; i < health.length; i++) {
            if (pieces.get(i).kind() == kind && health[i] <= 0) {
                count++;
            }
        }
        return count;
    }

    @Override
    public String toString() {
        return "Damage" + Arrays.toString(health);
    }
}
