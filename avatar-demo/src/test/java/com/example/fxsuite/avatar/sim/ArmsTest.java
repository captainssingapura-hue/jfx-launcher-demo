package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The weapons' timing: rounds in turn at the rifles' rate, and the glaive's strokes. */
class ArmsTest {

    private static final double FRAME = 1.0 / 60;

    /**
     * Holds (or not) the trigger for {@code seconds}, a frame at a time, gathering the rounds and
     * strokes: their timing, which is what these tests are about. What they strike is AimTest's.
     */
    private static List<Event> hold(Arms arms, final boolean trigger, final double seconds, final List<Arms> end) {
        final List<Event> all = new ArrayList<>();
        for (double t = 0; t < seconds - 1e-9; t += FRAME) {
            final Arms.Stepped stepped = arms.step(trigger, FRAME);
            stepped.events().stream().filter(e -> !(e instanceof Event.Struck)).forEach(all::add);
            arms = stepped.arms();
        }
        end.add(arms);
        return all;
    }

    @Test
    void theRiflesFireInTurnAtTheirRate() {
        final List<Event> fired = hold(Arms.holding(Weapon.RIFLES), true, 2, new ArrayList<>());
        // two rifles at 600 rounds a minute each, for two seconds: about 40 rounds
        assertEquals(2 * 2 * Arms.RIFLE_RATE / 60, fired.size(), 1);
        for (int i = 0; i < fired.size(); i++) {
            assertEquals(i % 2 == 0, ((Event.Fired) fired.get(i)).left(), "left, right, left...");
        }
    }

    @Test
    void theFirstRoundGoesAtOnceAndLettingGoStopsIt() {
        final Arms.Stepped first = Arms.holding(Weapon.RIFLES).step(true, FRAME);
        final List<Event> rounds = first.events().stream().filter(e -> e instanceof Event.Fired).toList();
        assertEquals(1, rounds.size(), "no wait for the first round");
        assertTrue(((Event.Fired) rounds.get(0)).left(), "from the left rifle");
        assertEquals(0, first.arms().sinceLeft(), FRAME, "and the left rifle has just fired");
        final List<Arms> end = new ArrayList<>();
        assertTrue(hold(first.arms(), false, 1, end).isEmpty(), "let go, and nothing fires");
        assertEquals(1, end.get(0).sinceLeft(), 0.05, "the left rifle last fired a second ago");
    }

    @Test
    void theRateHoldsWhateverTheFrameRate() {
        final List<Event> fast = hold(Arms.holding(Weapon.RIFLES), true, 3, new ArrayList<>());
        Arms slow = Arms.holding(Weapon.RIFLES);
        int rounds = 0;
        for (int i = 0; i < 30; i++) {
            final Arms.Stepped stepped = slow.step(true, 0.1);
            rounds += (int) stepped.events().stream().filter(e -> e instanceof Event.Fired).count();
            slow = stepped.arms();
        }
        assertEquals(fast.size(), rounds, 1, "ten frames a second fire as many rounds as sixty");
    }

    @Test
    void theGlaiveCleavesThenSweepsInTurnAndIsHeardAsItComesThrough() {
        final List<Arms> end = new ArrayList<>();
        final List<Event> swung = hold(Arms.holding(Weapon.GLAIVE), true, 4 * Arms.STROKE - 0.01, end);
        assertEquals(List.of(new Event.Swung(Arms.CLEAVE), new Event.Swung(Arms.SWEEP),
                new Event.Swung(Arms.CLEAVE), new Event.Swung(Arms.SWEEP)), swung);
        // the first comes through STRIKE of the way into its stroke, not as it starts
        final List<Event> early = hold(Arms.holding(Weapon.GLAIVE), true, Arms.STRIKE * Arms.STROKE - 0.03,
                new ArrayList<>());
        assertTrue(early.isEmpty(), "still winding up");
    }

    @Test
    void aStrokeLetGoMidwayStillFinishes() {
        final List<Arms> started = new ArrayList<>();
        hold(Arms.holding(Weapon.GLAIVE), true, 0.1, started);
        final List<Arms> end = new ArrayList<>();
        final List<Event> rest = hold(started.get(0), false, 2, end);
        assertEquals(List.of(new Event.Swung(Arms.CLEAVE)), rest, "comes through once, and no more");
        assertTrue(end.get(0).progress() < 0, "and is at rest");
    }

    @Test
    void bareHandsDoNothing() {
        assertTrue(hold(Arms.EMPTY, true, 1, new ArrayList<>()).isEmpty());
    }

    @Test
    void drawingAWeaponIsHeardAndDrawingItAgainPutsItAway() {
        final Avatar avatar = new Avatar(Body.standing(Vec3.ZERO, 0), false);
        final Avatar.Stepped drawn = avatar.act(new Action.Draw(Weapon.RIFLES));
        final Avatar armed = drawn.avatar();
        assertEquals(Weapon.RIFLES, armed.holding());
        assertEquals(List.of(new Event.Drew(Weapon.RIFLES)), drawn.events());
        final Avatar swapped = armed.drawn(Weapon.GLAIVE);
        assertEquals(Weapon.GLAIVE, swapped.holding(), "another weapon replaces it");
        assertEquals(Weapon.NONE, swapped.drawn(Weapon.GLAIVE).holding(), "the same again puts it away");

        Avatar firing = armed;
        int rounds = 0;
        for (int i = 0; i < 30; i++) {
            final Avatar.Stepped stepped = firing.step(new Controls(0, 0, 0, 0, false, true), FRAME, Obstacles.NONE);
            rounds += (int) stepped.events().stream().filter(e -> e instanceof Event.Fired).count();
            firing = stepped.avatar();
        }
        assertTrue(rounds >= 9, "the trigger fires the avatar's rifles: " + rounds);
    }
}
