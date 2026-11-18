package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.sim.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * The land cut into pieces, each as big as its fineness asks ({@link Fineness}): a quadtree, its pieces from
 * {@link #ROOT} a side, the roughest, down to {@link #SMALLEST}, the finest, each level half the size of the
 * one before. A piece splits in four while fineness at its nearest point asks for finer than its level, and
 * four go back into one once it no longer does; finer again only past a margin, so it does not flick back
 * and forth. So round the camera, small fine pieces; further off, bigger ones; far off, a few big rough ones;
 * each about as many points as the next, so showing any one is about as cheap.
 *
 * <p>What a piece is made of ({@code B}) is built apart from the screen ({@code builders}), ahead of being
 * wanted (as if {@link #AHEAD} as near), and until it is, what was shown there stays shown: the piece it would
 * split from, or the pieces it would go back into. No more than {@link #BUDGET} of what has never been shown is
 * shown for the first time in a frame (or one piece, if more): the rest waits a frame or so, showing what it did.
 * The roughest pieces are built to start with, all of them, so there is always something to show.
 *
 * <p>A piece's finer ones are let go once it is shown itself and would not split even as near again: so
 * the land behind a long drive is not kept, fine, for nothing.
 *
 * <p>Nothing here draws: which pieces to show, and what each is made of; drawing them is another's.
 */
final class Pieces<B> {

    /** The finest pieces' size, and how many levels there are: 125 m, 250 m, 500 m, 1 km, 2 km. */
    static final double SMALLEST = 12_500;
    static final int LEVELS = Fineness.LEVELS;
    static final double ROOT = SMALLEST * (1 << (LEVELS - 1));
    /** How much nearer than it is a piece is built for, ahead of needing to be. */
    static final double AHEAD = 0.7;
    /** How many points a frame shows for the first time, at most (or one piece, if more). */
    static final int BUDGET = 12_000;

    /** A piece of the land: its level, 0 the finest; from ({@code x0}, {@code z0}) to ({@code x1}, {@code z1}). */
    static final class Piece<B> {
        final int level;
        final double x0;
        final double z0;
        final double x1;
        final double z1;
        /** Its four finer ones, once wanted; null till then, or once let go. */
        List<Piece<B>> finer;
        FutureTask<B> building;
        volatile B built;
        /** Whether it was shown last frame; whether it has been shown at all, since it was built. */
        boolean shown;
        boolean seen;
        /** Whether, last frame, what was shown here was finer than it (its finer ones, or theirs). */
        boolean below;
        volatile boolean dropped;

        Piece(final int level, final double x0, final double z0, final double x1, final double z1) {
            this.level = level;
            this.x0 = x0;
            this.z0 = z0;
            this.x1 = x1;
            this.z1 = z1;
        }

        /** How far {@code eye} is across the ground from its nearest point. */
        double near(final Vec3 eye) {
            final double dx = Math.max(0, Math.max(x0 - eye.x(), eye.x() - x1));
            final double dz = Math.max(0, Math.max(z0 - eye.z(), eye.z() - z1));
            return Math.hypot(dx, dz);
        }

        /** How far {@code eye} is across the ground from its furthest point. */
        double far(final Vec3 eye) {
            return Math.hypot(Math.max(Math.abs(eye.x() - x0), Math.abs(eye.x() - x1)),
                    Math.max(Math.abs(eye.z() - z0), Math.abs(eye.z() - z1)));
        }

        @Override
        public String toString() {
            return "piece " + level + " at " + Math.round(x0 / 100) + "," + Math.round(z0 / 100);
        }
    }

    /** What a frame shows, and what it let go of (that had been shown: whoever draws it may forget it). */
    record Shown<B>(List<Piece<B>> shown, List<Piece<B>> dropped) {
    }

    private final List<Piece<B>> roots = new ArrayList<>();
    private final Function<Piece<B>, B> build;
    private final ToIntFunction<B> points;
    private final Executor builders;
    private int spent;
    private double height;

    /**
     * The land from ({@code x0}, {@code z0}) to ({@code x1}, {@code z1}) in pieces; each built by {@code build} on
     * {@code builders}, so many points as {@code points} says; the roughest built at once.
     */
    Pieces(final double x0, final double z0, final double x1, final double z1, final Function<Piece<B>, B> build,
           final ToIntFunction<B> points, final Executor builders) {
        this.build = build;
        this.points = points;
        this.builders = builders;
        for (double z = z0; z < z1; z += ROOT) {
            for (double x = x0; x < x1; x += ROOT) {
                roots.add(new Piece<>(LEVELS - 1, x, z, Math.min(x1, x + ROOT), Math.min(z1, z + ROOT)));
            }
        }
        for (final Piece<B> root : roots) {
            ask(root);
        }
    }

    /** The roughest pieces, side by side. */
    List<Piece<B>> roots() {
        return roots;
    }

    /** Whether the roughest pieces are all built: there is something to show everywhere. */
    boolean ready() {
        return roots.stream().allMatch(r -> r.built != null);
    }

    /**
     * The pieces to show seen from {@code eye}, the camera going {@code speed}: each as fine as its fineness asks,
     * once it is built and the budget lets it be shown; and what is let go.
     */
    Shown<B> seenFrom(final Vec3 eye, final double speed) {
        return seenFrom(eye, speed, 0);
    }

    /**
     * As {@link #seenFrom(Vec3, double)}, the camera {@code height} over the ground: how far a piece is, for its
     * fineness, is from the camera to its nearest point, height and all.
     */
    Shown<B> seenFrom(final Vec3 eye, final double speed, final double height) {
        this.height = height;
        spent = 0;
        final List<Piece<B>> shown = new ArrayList<>();
        // the nearest first, so what is near gets its share of the budget before what is far
        for (final Piece<B> root : nearestFirst(roots, eye)) {
            if (!select(root, eye, speed, shown, false)) {
                throw new IllegalStateException("the land's roughest pieces are not built yet");
            }
        }
        for (final Piece<B> root : roots) {
            unshow(root);
        }
        for (final Piece<B> piece : shown) {
            piece.shown = true;
            piece.seen = true;
        }
        final List<Piece<B>> dropped = new ArrayList<>();
        for (final Piece<B> root : roots) {
            mark(root);
            letGo(root, eye, speed, dropped);
        }
        return new Shown<>(shown, dropped);
    }

    /**
     * Piece {@code piece}'s part of what is shown, added to {@code shown}: its finer ones', if it should split,
     * was shown itself or finer last frame (so it gets finer a level a frame), and they can all be, within the
     * budget, or first this frame; else itself, if it is built and the budget lets it; else what was shown there
     * last frame. One of four shown together ({@code grouped}) is not held to the budget by itself: the four are,
     * together. Whether anything could be.
     */
    private boolean select(final Piece<B> piece, final Vec3 eye, final double speed, final List<Piece<B>> shown,
                           final boolean grouped) {
        final double near = piece.near(eye);
        final int was = piece.shown ? piece.level : piece.below ? piece.level - 1 : piece.level + 1;
        final boolean split = piece.level > 0 && (piece.shown || piece.below) && finer(near, speed, was, piece.level);
        final boolean ahead = piece.level > 0 && finer(near * AHEAD, speed, was, piece.level);
        if (split || ahead) {
            for (final Piece<B> each : finer(piece)) {
                ask(each);
            }
        }
        final int mark = shown.size();
        final int spentBefore = spent;
        if (split) {
            boolean all = true;
            for (final Piece<B> each : nearestFirst(finer(piece), eye)) {
                all &= select(each, eye, speed, shown, true);
                if (!all) {
                    break;
                }
            }
            // all four together, or none: within the budget, or if nothing else has been shown new this frame, or if
            // they show nothing new themselves (else what others showed new would send all the land back to its
            // roughest for a frame, and every piece of it sent to be drawn again as it came back)
            if (all && (spentBefore == 0 || spent <= BUDGET || spent == spentBefore)) {
                return true;
            }
            truncate(shown, mark);
            spent = spentBefore;
        }
        final B made = piece.built;
        if (made != null) {
            final int cost = piece.seen ? 0 : points.applyAsInt(made);
            if (cost == 0 || grouped || spent == 0 || spent + cost <= BUDGET) {
                spent += cost;
                shown.add(piece);
                return true;
            }
        } else {
            ask(piece);
        }
        // not to be shown yet: what was shown here last frame, if anything was
        if (asWas(piece, shown)) {
            return true;
        }
        truncate(shown, mark);
        // and the roughest, if built, shown whatever the budget: there is nothing coarser to show instead
        if (made != null && piece.level == LEVELS - 1) {
            spent += points.applyAsInt(made);
            shown.add(piece);
            return true;
        }
        return false;
    }

    /** What was shown last frame over {@code piece}, added to {@code shown}, if all of it was. */
    private boolean asWas(final Piece<B> piece, final List<Piece<B>> shown) {
        if (piece.shown) {
            shown.add(piece);
            return true;
        }
        if (!piece.below || piece.finer == null) {
            return false;
        }
        final int mark = shown.size();
        for (final Piece<B> each : piece.finer) {
            if (!asWas(each, shown)) {
                truncate(shown, mark);
                return false;
            }
        }
        return true;
    }

    /**
     * Whether fineness {@code near} off across the ground, at the camera's height over it, at {@code speed}, asks for
     * finer than {@code level}, having been at {@code was}.
     */
    private boolean finer(final double near, final double speed, final int was, final int level) {
        return Fineness.level(Fineness.of(Math.hypot(near, height), speed), was) < level;
    }

    /** Pieces {@code pieces}, the nearest {@code eye} first. */
    private static <B> List<Piece<B>> nearestFirst(final List<Piece<B>> pieces, final Vec3 eye) {
        final List<Piece<B>> sorted = new ArrayList<>(pieces);
        sorted.sort(java.util.Comparator.comparingDouble(p -> p.near(eye)));
        return sorted;
    }

    /** Piece {@code piece}'s four finer ones, made if they are not yet. */
    private List<Piece<B>> finer(final Piece<B> piece) {
        if (piece.finer == null) {
            // halves of its size as its level has it, not as cut short at the land's edge
            final double half = SMALLEST * (1 << piece.level) / 2;
            final List<Piece<B>> four = new ArrayList<>(4);
            for (int b = 0; b < 2; b++) {
                for (int a = 0; a < 2; a++) {
                    final double x0 = piece.x0 + a * half;
                    final double z0 = piece.z0 + b * half;
                    if (x0 < piece.x1 && z0 < piece.z1) {
                        four.add(new Piece<>(piece.level - 1, x0, z0, Math.min(piece.x1, x0 + half),
                                Math.min(piece.z1, z0 + half)));
                    }
                }
            }
            piece.finer = four;
        }
        return piece.finer;
    }

    /** Piece {@code piece} built, if it is not and is not being: apart from the screen. */
    private void ask(final Piece<B> piece) {
        if (piece.built == null && piece.building == null) {
            final FutureTask<B> task = new FutureTask<>(() -> {
                final B made = build.apply(piece);
                if (!piece.dropped) {
                    piece.built = made;
                }
                return made;
            });
            piece.building = task;
            builders.execute(task);
        }
    }

    private static void unshow(final Piece<?> piece) {
        piece.shown = false;
        if (piece.finer != null) {
            for (final Piece<?> each : piece.finer) {
                unshow(each);
            }
        }
    }

    /** Whether anything finer than {@code piece} is shown, marked on it and on those under it. */
    private static boolean mark(final Piece<?> piece) {
        boolean finer = false;
        if (piece.finer != null) {
            for (final Piece<?> each : piece.finer) {
                finer |= mark(each) || each.shown;
            }
        }
        piece.below = finer;
        return finer;
    }

    /**
     * Piece {@code piece}'s finer ones let go, if it is shown itself and would not split even as near as it is built
     * ahead for; those that had been shown added to {@code dropped}.
     */
    private void letGo(final Piece<B> piece, final Vec3 eye, final double speed, final List<Piece<B>> dropped) {
        if (piece.finer == null) {
            return;
        }
        if (piece.shown && !finer(piece.near(eye) * AHEAD, speed, piece.level, piece.level)) {
            for (final Piece<B> each : piece.finer) {
                drop(each, dropped);
            }
            piece.finer = null;
            return;
        }
        for (final Piece<B> each : piece.finer) {
            letGo(each, eye, speed, dropped);
        }
    }

    private static <B> void drop(final Piece<B> piece, final List<Piece<B>> dropped) {
        piece.dropped = true;
        if (piece.building != null) {
            piece.building.cancel(false);
        }
        if (piece.seen) {
            dropped.add(piece);
        }
        if (piece.finer != null) {
            for (final Piece<B> each : piece.finer) {
                drop(each, dropped);
            }
        }
    }

    private static <T> void truncate(final List<T> list, final int size) {
        while (list.size() > size) {
            list.removeLast();
        }
    }
}
