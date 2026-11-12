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
 *   <li><b>How tough:</b> a dummy takes {@link #DUMMY} and a drone {@link #DRONE}. A crate's
 *       toughness goes by its size, from {@link #CRATE} for the smallest up: a small crate falls to
 *       a short burst, the biggest takes a couple of seconds of the Gatling gun.</li>
 *   <li><b>A blow</b> takes away what its weapon does ({@link Weapon#blow}): a round, or a stroke
 *       of the glaive. The one that takes the last of it breaks the piece.</li>
 *   <li><b>Broken</b> is for good, until the demo starts again: the piece is gone from the world
 *       ({@link #obstacles}), so rounds fly on past where it stood, and whatever stood on it
 *       falls.</li>
 * </ul>
 *
 * <p>Each break is reported ({@link Event.Broke}), for the graphics to throw the pieces about and
 * the sound to be heard.
 */
public final class Damage {

    /** How tough a dummy is, a drone, and the smallest crate. */
    public static final double DUMMY = 300;
    public static final double DRONE = 120;
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
            case CRATE -> Math.round(CRATE + 4 * block.halfWidth() * block.halfDepth()
                    * (block.top() - block.bottom()) / CRATE_VOLUME);
            case PILLAR, GATE -> Double.POSITIVE_INFINITY;
        };
    }

    /** The new damage, and what broke. */
    public record Taken(Damage damage, List<Event> events) {
        public Taken {
            events = List.copyOf(events);
        }
    }

    /**
     * The blows among {@code events} ({@link Event.Struck}) taken, in order: each one on a piece
     * that can break wears it down, and the one that takes the last of it breaks it. Blows on the
     * ground, on what cannot break, or on what is already broken change nothing.
     */
    public Taken took(final List<Event> events) {
        double[] left = null;
        Obstacles standing = obstacles;
        final List<Event> broke = new ArrayList<>();
        for (final Event event : events) {
            if (!(event instanceof Event.Struck struck) || !harms(struck)) {
                continue;
            }
            final int piece = struck.piece();
            final double[] now = left == null ? health : left;
            if (now[piece] <= 0) {
                continue;  // broken earlier this frame
            }
            if (left == null) {
                left = health.clone();
            }
            left[piece] = Math.max(0, left[piece] - struck.by().blow());
            if (left[piece] <= 0) {
                final Course.Piece what = pieces.get(piece);
                standing = standing.without(piece);
                broke.add(new Event.Broke(piece, what.kind(), what.block(), struck.at(), struck.by()));
            }
        }
        return left == null ? new Taken(this, List.of())
                : new Taken(new Damage(pieces, toughness, left, standing), broke);
    }

    private boolean harms(final Event.Struck struck) {
        final int piece = struck.piece();
        return piece >= 0 && piece < health.length && pieces.get(piece).kind().breakable()
                && struck.by().blow() > 0;
    }

    /** What is still standing, to move among and shoot at: the course, less what is broken. */
    public Obstacles obstacles() {
        return obstacles;
    }

    /** How much piece {@code piece} has left of what it can take: 1 unharmed, 0 broken; always 1 for what cannot break. */
    public double left(final int piece) {
        return Double.isInfinite(toughness[piece]) ? 1 : health[piece] / toughness[piece];
    }

    /** What piece {@code piece} has left to take, in the weapons' own units; infinite for what cannot break. */
    public double health(final int piece) {
        return health[piece];
    }

    public boolean broken(final int piece) {
        return health[piece] <= 0;
    }

    /** How many pieces there are, broken or not. */
    public int size() {
        return pieces.size();
    }

    /** How many pieces of {@code kind} there were, and how many of them are broken. */
    public int count(final Course.Kind kind) {
        return (int) pieces.stream().filter(p -> p.kind() == kind).count();
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
