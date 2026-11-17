package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.sim.Vec3;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The land in pieces: all of it shown, once each, whatever is built; small fine pieces round the camera, big
 * rough ones far off, as fineness asks; no more shown for the first time a frame than the budget; what was
 * shown kept till what is to replace it is built; finer again only past a margin, coarser as soon as asked;
 * and the fine pieces behind a long drive let go.
 */
class PiecesTest {

    /** Ten kilometres a side, as the meadow is. */
    private static final double EDGE = 500_000;
    private static final int POINTS = 1_000;

    private static Pieces<Integer> pieces(final Executor builders) {
        return new Pieces<>(-EDGE, -EDGE, EDGE, EDGE, p -> POINTS, Integer::intValue, builders);
    }

    /** {@code frames} frames seen from {@code eye} at rest; the last one's. */
    private static Pieces.Shown<Integer> settle(final Pieces<Integer> pieces, final Vec3 eye, final int frames) {
        Pieces.Shown<Integer> shown = null;
        for (int f = 0; f < frames; f++) {
            shown = pieces.seenFrom(eye, 0);
        }
        return shown;
    }

    /** All of it, once: no gap, no piece over another. */
    private static void coversItAllOnce(final List<Pieces.Piece<Integer>> shown) {
        double area = 0;
        for (final Pieces.Piece<Integer> p : shown) {
            area += (p.x1 - p.x0) * (p.z1 - p.z0);
        }
        assertEquals(4 * EDGE * EDGE, area, 1, "all of it");
        for (int a = 0; a < shown.size(); a++) {
            for (int b = a + 1; b < shown.size(); b++) {
                final Pieces.Piece<Integer> p = shown.get(a);
                final Pieces.Piece<Integer> q = shown.get(b);
                final boolean over = p.x0 < q.x1 && q.x0 < p.x1 && p.z0 < q.z1 && q.z0 < p.z1;
                assertFalse(over, p + " over " + q);
            }
        }
    }

    private static Pieces.Piece<Integer> at(final List<Pieces.Piece<Integer>> shown, final double x, final double z) {
        return shown.stream().filter(p -> p.x0 <= x && x < p.x1 && p.z0 <= z && z < p.z1).findFirst().orElseThrow();
    }

    @Test
    void smallFinePiecesRoundTheCameraBigRoughOnesFarOffAsFinenessAsks() {
        final Pieces<Integer> pieces = pieces(Runnable::run);
        final Vec3 eye = new Vec3(1_000, 0, 1_000);
        final List<Pieces.Piece<Integer>> shown = settle(pieces, eye, 200).shown();
        coversItAllOnce(shown);
        final Pieces.Piece<Integer> under = at(shown, eye.x(), eye.z());
        assertEquals(0, under.level, "the finest under the camera");
        assertEquals(Pieces.SMALLEST, under.x1 - under.x0, 1e-9, "125 m");
        assertEquals(4, at(shown, -480_000, -480_000).level, "rough far off: as rough as 7 km off asks");
        // each as fine as fineness at its nearest point asks: or a level coarser, within its margin; or a level
        // finer, beside a nearer one of the four it was split into with
        for (final Pieces.Piece<Integer> p : shown) {
            final int asked = Fineness.level(Fineness.of(p.near(eye), 0), -1);
            assertTrue(Math.abs(p.level - asked) <= 1, p + ": " + asked);
        }
        assertTrue(shown.size() < 200, "a few hundred meshes at most: " + shown.size());
    }

    @Test
    void allOfItShownWhateverIsBuiltWhatWasShownKeptTillItsReplacementIs() {
        final List<Runnable> waiting = new ArrayList<>();
        final Pieces<Integer> pieces = pieces(waiting::add);
        // the roughest, built
        new ArrayList<>(waiting).forEach(Runnable::run);
        waiting.clear();
        final Vec3 eye = new Vec3(1_000, 0, 1_000);
        List<Pieces.Piece<Integer>> shown = pieces.seenFrom(eye, 0).shown();
        coversItAllOnce(shown);
        assertTrue(shown.stream().allMatch(p -> p.level == Pieces.LEVELS - 1), "nothing finer built yet");
        assertFalse(waiting.isEmpty(), "the finer asked for");
        // built a level at a time: each frame shows what is built, all of it still covered
        for (int round = 0; round < 10; round++) {
            new ArrayList<>(waiting).forEach(Runnable::run);
            waiting.clear();
            shown = pieces.seenFrom(eye, 0).shown();
            coversItAllOnce(shown);
        }
        assertEquals(0, at(shown, eye.x(), eye.z()).level);
    }

    @Test
    void noMoreShownForTheFirstTimeAFrameThanTheBudget() {
        final Pieces<Integer> pieces = pieces(Runnable::run);
        final Vec3 eye = new Vec3(1_000, 0, 1_000);
        pieces.seenFrom(eye, 0);
        // the roughest shown; then each frame no more than the budget's worth of new ones
        for (int f = 0; f < 30; f++) {
            final Pieces.Shown<Integer> shown = pieces.seenFrom(eye, 0);
            final long fresh = shown.shown().stream().filter(p -> p.level < Pieces.LEVELS - 1).count();
            assertTrue(fresh * POINTS <= Pieces.BUDGET * (f + 1) + POINTS, "frame " + f + ": " + fresh);
            coversItAllOnce(shown.shown());
        }
    }

    @Test
    void finerAgainOnlyPastAMarginCoarserAsSoonAsAsked() {
        // a piece 250 m a side from 250 m to 500 m east: its near edge 274 m off, where fineness is 0.8,
        // past the finest's edge (0.75) but not by its margin (0.85)
        final double x0 = 25_000;
        final Vec3 there = new Vec3(x0 - 27_400, 0, 12_500);
        assertEquals(0.8, Fineness.of(27_400, 0), 0.005);
        // coming from far off: it is not split
        final Pieces<Integer> fromFar = pieces(Runnable::run);
        settle(fromFar, new Vec3(-300_000, 0, 12_500), 100);
        final Pieces.Piece<Integer> far = at(settle(fromFar, there, 100).shown(), x0 + 100, 12_500);
        assertEquals(1, far.level, "not finer till past the margin");
        // coming from inside it: it stays split
        final Pieces<Integer> fromNear = pieces(Runnable::run);
        settle(fromNear, new Vec3(x0 + 12_500, 0, 12_500), 100);
        final Pieces.Piece<Integer> near = at(settle(fromNear, there, 100).shown(), x0 + 100, 12_500);
        assertEquals(0, near.level, "not coarser till fineness is under the edge");
    }

    @Test
    void fromHighUpNothingUnderItFine() {
        final Pieces<Integer> pieces = pieces(Runnable::run);
        final Vec3 eye = new Vec3(1_000, -300_000, 1_000);
        Pieces.Shown<Integer> shown = null;
        for (int f = 0; f < 200; f++) {
            shown = pieces.seenFrom(eye, 0, 300_000);
        }
        coversItAllOnce(shown.shown());
        assertTrue(shown.shown().stream().allMatch(p -> p.level >= 3), "3 km up, nothing under it finer than 1 km pieces");
    }

    @Test
    void theFinePiecesBehindALongDriveLetGo() {
        final Pieces<Integer> pieces = pieces(Runnable::run);
        final Vec3 start = new Vec3(1_000, 0, 1_000);
        final List<Pieces.Piece<Integer>> before = settle(pieces, start, 200).shown();
        final Pieces.Piece<Integer> fine = at(before, start.x(), start.z());
        // driven 4 km off: what was fine at the start let go, and the tree no bigger than it was
        final int was = count(pieces);
        boolean droppedIt = false;
        for (int f = 0; f < 400; f++) {
            final Vec3 eye = new Vec3(1_000 - f * 1_000, 0, 1_000);
            droppedIt |= pieces.seenFrom(eye, 0).dropped().contains(fine);
        }
        assertTrue(droppedIt, "the fine piece where it started let go");
        assertTrue(count(pieces) <= was + 50, "no more kept than is wanted: " + count(pieces) + " from " + was);
        assertFalse(at(settle(pieces, new Vec3(-399_000, 0, 1_000), 50).shown(), start.x(), start.z()).level < 3,
                "the start rough again, 4 km off");
    }

    private static int count(final Pieces<Integer> pieces) {
        int n = 0;
        final List<Pieces.Piece<Integer>> todo = new ArrayList<>(pieces.roots());
        while (!todo.isEmpty()) {
            final Pieces.Piece<Integer> p = todo.removeLast();
            n++;
            if (p.finer != null) {
                todo.addAll(p.finer);
            }
        }
        return n;
    }
}
