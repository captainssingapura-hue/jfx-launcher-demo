package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.DoubleSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The simulation's own clock, and a world that comes out the same at any frame rate. */
class ClockTest {

    @Test
    void sixtyFramesASecondTakeTwoStepsEach() {
        Clock clock = Clock.START;
        for (int i = 0; i < 60; i++) {
            final Clock.Ticked ticked = clock.advance(1.0 / 60);
            assertEquals(2, ticked.steps(), "frame " + i);
            assertEquals(0, ticked.blend(), 1e-6);
            clock = ticked.clock();
        }
    }

    @Test
    void framesShorterThanAStepCarryOverAndBlendOnward() {
        Clock clock = Clock.START;
        int steps = 0;
        double lastBlend = -1;
        for (int i = 0; i < 144; i++) {
            final Clock.Ticked ticked = clock.advance(1.0 / 144);
            assertTrue(ticked.steps() <= 1);
            assertTrue(ticked.blend() >= 0 && ticked.blend() < 1);
            if (ticked.steps() == 0) {
                assertTrue(ticked.blend() > lastBlend, "no step: further on toward the next");
            }
            lastBlend = ticked.blend();
            steps += ticked.steps();
            clock = ticked.clock();
        }
        assertEquals(120, steps, "a second is 120 steps, however it is cut up");
    }

    @Test
    void aVeryLongFrameTakesNoMoreThanItsShareAndLetsTheRestGo() {
        final Clock.Ticked ticked = Clock.START.advance(2.5);
        assertEquals(Clock.MOST, ticked.steps());
        assertEquals(0, ticked.clock().owed(), 1e-12, "nothing left owed: it does not try to catch up");
        assertEquals(0, Clock.START.advance(-1).steps(), "time never runs back");
    }

    /**
     * The same three seconds, played at 60 frames a second, at 144, and at an uneven mix of short
     * and long frames: running at the dummies with the rifles blazing, the outcome is the same to
     * the last bit, where each frame's own time was stepped before.
     */
    @Test
    void theWorldComesOutTheSameAtAnyFrameRate() {
        final Play at60 = play(() -> 1.0 / 60);
        final Play at144 = play(() -> 1.0 / 144);
        final double[] uneven = {0.005, 0.041, 0.012, 0.023, 0.0167, 0.0023};
        final int[] next = {0};
        final Play jittery = play(() -> uneven[next[0]++ % uneven.length]);

        for (final Play other : List.of(at144, jittery)) {
            assertEquals(at60.avatar().body(), other.avatar().body());
            assertEquals(at60.avatar().arms(), other.avatar().arms());
            for (int i = 0; i < at60.damage().size(); i++) {
                assertEquals(at60.damage().left(i), other.damage().left(i), 0, "piece " + i);
            }
        }
        assertTrue(at60.damage().broken(Course.Kind.DUMMY) > 0, "and it did something: a dummy came down");
    }

    /** Three seconds of the range, frames of the lengths {@code frames} gives, stepped by the clock. */
    private static Play play(final DoubleSupplier frames) {
        final Course course = new Course();
        Play play = new Play(new Avatar(Body.standing(Vec3.ZERO, Course.RANGE_HEADING), false)
                .drawn(Weapon.RIFLES), Damage.of(course));
        final Controls charge = new Controls(0.6, 0, 0, 0, false, true, 0);
        Clock clock = Clock.START;
        int steps = 0;
        while (steps < 360) {
            final Clock.Ticked ticked = clock.advance(Math.min(frames.getAsDouble(), (360 - steps) * Clock.STEP));
            clock = ticked.clock();
            for (int i = 0; i < ticked.steps(); i++) {
                play = play.step(charge, Clock.STEP).play();
            }
            steps += ticked.steps();
        }
        assertEquals(360, steps);
        return play;
    }
}
