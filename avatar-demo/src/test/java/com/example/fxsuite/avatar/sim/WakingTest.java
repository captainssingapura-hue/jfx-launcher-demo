package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntPredicate;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cells awake round the avatar: its own, those beside it, the storeys over and under it, the rest asleep; one at the
 * edge woken coming near and left asleep only a margin further off; none asleep with something moving in it; those
 * what happens reaches woken; and the same steps, the same cells awake.
 */
class WakingTest {

    /** A corridor of ten bays of 7.5 m, a room's depth across, five storeys of 3.6 m. */
    private static final GridInside FLOOR = new GridInside(new Box(-400, 400, 0, 7_500, 0, 1_800), 800, 750, 360);
    private static final IntPredicate STILL = c -> false;

    /** Cell by bay and storey. */
    private static int cell(final int bay, final int storey) {
        return storey * 10 + bay;
    }

    /** The avatar stood in a bay's middle, on a storey's floor. */
    private static Box avatar(final double along, final int storey) {
        return Box.around(0, along, storey * 360 + 30, 32, 180);
    }

    @Test
    void itsOwnThoseBesideItTheStoreysOverAndUnderItTheRestAsleep() {
        final Waking waking = new Waking(FLOOR, 750, 360, 200);
        final Waking.Change change = waking.step(avatar(4 * 750 + 375, 2), STILL);
        final List<Integer> expected = new ArrayList<>();
        for (int storey = 1; storey <= 3; storey++) {
            for (int bay = 3; bay <= 5; bay++) {
                expected.add(cell(bay, storey));
            }
        }
        assertArrayEquals(expected.stream().mapToInt(Integer::intValue).toArray(), waking.awake());
        assertArrayEquals(waking.awake(), change.woke());
        assertEquals(0, change.slept().length);
        assertFalse(waking.awake(cell(0, 2)));
        assertFalse(waking.awake(cell(4, 4)));
        // gone out of it: all asleep but what moves
        final Waking.Change gone = waking.step(null, STILL);
        assertArrayEquals(expected.stream().mapToInt(Integer::intValue).toArray(), gone.slept());
        assertEquals(0, waking.awake().length);
    }

    @Test
    void oneAtTheEdgeWokenComingNearLeftAsleepOnlyAMarginFurtherOff() {
        final Waking waking = new Waking(FLOOR, 300, 0, 200);
        // walking along the corridor, then back, 10 cm a step
        final List<Integer> wokeAt = new ArrayList<>();
        final List<Integer> sleptAt = new ArrayList<>();
        final int bay6 = cell(6, 0);
        for (int step = 0; step <= 400; step++) {
            final double along = step <= 200 ? 3_000 + 10 * step : 7_000 - 10 * step;
            final Waking.Change change = waking.step(avatar(along, 0), STILL);
            if (Arrays.stream(change.woke()).anyMatch(c -> c == bay6)) {
                wokeAt.add(step);
            }
            if (Arrays.stream(change.slept()).anyMatch(c -> c == bay6)) {
                sleptAt.add(step);
            }
        }
        // bay 6 starts at 4,500: woken once the avatar's box comes within 3 m of it; asleep again only once it is
        // more than 5 m off, going back
        assertEquals(List.of(1_168 / 10 + 1), wokeAt, "woken once, coming near");
        assertEquals(1, sleptAt.size(), "asleep once, going back");
        final double sleptAlong = 7_000 - 10 * sleptAt.get(0);
        assertTrue(sleptAlong + 32 + 300 + 200 < 4_500 && sleptAlong + 32 + 300 + 200 >= 4_490, "at " + sleptAlong);
    }

    @Test
    void noneAsleepWithSomethingMovingInItThoseWhatHappensReachesWoken() {
        final Waking waking = new Waking(FLOOR, 0, 0, 0);
        waking.step(avatar(375, 0), STILL);
        assertArrayEquals(new int[] {cell(0, 0)}, waking.awake());
        // a shot down the corridor wakes the bays its path enters
        final int[] path = FLOOR.cells(new Box(0, 0, 375, 7_000, 150, 150));
        final int[] woke = waking.wake(path);
        assertEquals(path.length - 1, woke.length, "its own awake already");
        // the next step, those with nothing moving fall asleep; a vase still falling in bay 7 keeps it awake
        final int[] moving = {cell(7, 0)};
        final Waking.Change change = waking.step(avatar(375, 0), c -> c == moving[0]);
        assertArrayEquals(new int[] {cell(0, 0), cell(7, 0)}, waking.awake());
        assertEquals(path.length - 2, change.slept().length);
        // once it lies still, it sleeps
        waking.step(avatar(375, 0), STILL);
        assertArrayEquals(new int[] {cell(0, 0)}, waking.awake());
        assertThrows(IllegalArgumentException.class, () -> waking.wake(FLOOR.count()));
    }

    @Test
    void theSameStepsTheSameCellsAwake() {
        final List<String> first = run();
        assertEquals(first, run());
        assertTrue(first.size() > 10);
    }

    private static List<String> run() {
        final Waking waking = new Waking(FLOOR, 750, 360, 150);
        final List<String> changes = new ArrayList<>();
        for (int step = 0; step < 300; step++) {
            final double along = 3_750 + 3_500 * Math.sin(step * 0.05);
            final int storey = (int) (5 * Num.hash(step / 40));
            final int s = step;
            final Waking.Change change = waking.step(avatar(along, storey), c -> Num.hash(s, c) < 0.1);
            if (!change.none()) {
                changes.add(step + ": " + Arrays.toString(change.woke()) + " " + Arrays.toString(change.slept()));
            }
        }
        return changes;
    }
}
