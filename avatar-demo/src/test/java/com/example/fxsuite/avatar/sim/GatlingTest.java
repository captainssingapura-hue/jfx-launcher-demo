package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Gatling gun: spinning up, firing at its rate, spinning down. */
class GatlingTest {

    private static final double FRAME = 1.0 / 60;

    /** Holds (or not) the trigger for {@code seconds}, gathering the rounds fired; the arms as left, last. */
    private static List<Event.Fired> hold(Arms arms, final boolean trigger, final double seconds, final double frame,
                                          final List<Arms> end) {
        final List<Event.Fired> fired = new ArrayList<>();
        for (double t = 0; t < seconds - 1e-9; t += frame) {
            final Arms.Stepped stepped = arms.step(trigger, frame);
            stepped.events().stream().filter(e -> e instanceof Event.Fired).map(e -> (Event.Fired) e)
                    .forEach(fired::add);
            arms = stepped.arms();
        }
        end.add(arms);
        return fired;
    }

    @Test
    void itSpinsUpBeforeItFires() {
        final List<Arms> end = new ArrayList<>();
        assertTrue(hold(Arms.holding(Weapon.GATLING), true, Arms.SPIN_UP - 0.05, FRAME, end).isEmpty(),
                "nothing while it spins up");
        assertTrue(end.get(0).spin() > 0.85 && end.get(0).spin() < 1, "nearly up to speed: " + end.get(0).spin());
        final List<Event.Fired> fired = hold(end.get(0), true, 1, FRAME, new ArrayList<>());
        // from full speed, the rest of that second at its rate
        assertEquals((1 - 0.05) * Arms.GATLING_RATE / 60, fired.size(), 2);
    }

    @Test
    void letGoItStopsFiringAtOnceAndSpinsDown() {
        final List<Arms> firing = new ArrayList<>();
        hold(Arms.holding(Weapon.GATLING), true, 1.5, FRAME, firing);
        assertTrue(firing.get(0).firing());
        final List<Arms> slowing = new ArrayList<>();
        assertTrue(hold(firing.get(0), false, Arms.SPIN_DOWN / 2, FRAME, slowing).isEmpty(), "not a round more");
        assertFalse(slowing.get(0).firing());
        assertEquals(0.5, slowing.get(0).spin(), 0.03, "half way down");
        // squeezed again while still turning, it is back up to speed the sooner
        final List<Arms> again = new ArrayList<>();
        final List<Event.Fired> sooner = hold(slowing.get(0), true, Arms.SPIN_UP * 0.6, FRAME, again);
        assertFalse(sooner.isEmpty(), "firing again before a full spin-up would have");
        final List<Arms> stopped = new ArrayList<>();
        hold(slowing.get(0), false, Arms.SPIN_DOWN, FRAME, stopped);
        assertEquals(0, stopped.get(0).spin(), 1e-9, "and still, in the end");
    }

    @Test
    void theRateHoldsWhateverTheFrameRate() {
        final int smooth = hold(Arms.holding(Weapon.GATLING), true, 3, FRAME, new ArrayList<>()).size();
        final int choppy = hold(Arms.holding(Weapon.GATLING), true, 3, 0.1, new ArrayList<>()).size();
        assertEquals(smooth, choppy, 2, "ten frames a second fire as many rounds as sixty");
        assertEquals((3 - Arms.SPIN_UP) * Arms.GATLING_RATE / 60, smooth, 2);
    }

    @Test
    void roundsFlyFromItsMuzzleStraightAheadAndStrikeAsTheGatlingGun() {
        final Body body = Body.standing(Vec3.ZERO, 40);
        // a long wall square across the way ahead, 15 m off
        final Obstacles wall = new Obstacles(List.of(Block.standing(Num.heading(40).x() * 1500,
                Num.heading(40).z() * 1500, 3000, 100, 400, 40)));
        Arms arms = Arms.holding(Weapon.GATLING);
        final List<Event> events = new ArrayList<>();
        for (int i = 0; i < 90; i++) {
            final Arms.Stepped stepped = arms.step(true, FRAME, body, wall, 0);
            events.addAll(stepped.events());
            // each blow dealt, as what the world makes of it where nothing breaks: struck
            stepped.blows().forEach(blow -> events.add(Event.Struck.of(blow, 1)));
            arms = stepped.arms();
        }
        final List<Event.Fired> fired = events.stream().filter(e -> e instanceof Event.Fired)
                .map(e -> (Event.Fired) e).toList();
        assertFalse(fired.isEmpty());
        final double cone = Math.cos(Math.toRadians(Arms.GATLING_SPREAD * 1.5)) - 1e-9;
        for (final Event.Fired f : fired) {
            assertEquals(Arms.gatlingMuzzle(body), f.from());
            assertTrue(f.to().subtract(f.from()).normalize().dotProduct(Num.heading(40)) > cone, "along the facing");
        }
        assertTrue(events.stream().anyMatch(e -> e instanceof Event.Struck s && s.by() == Weapon.GATLING
                && s.on() == Surface.BLOCK), "striking the wall ahead, by the Gatling gun");
    }

    @Test
    void itIsHeldDeadSteadyEveryRoundAlongOneLine() {
        final Body body = Body.standing(Vec3.ZERO, 25);
        Arms arms = Arms.holding(Weapon.GATLING);
        final List<Vec3> ways = new ArrayList<>();
        for (int i = 0; i < 120; i++) {
            final Arms.Stepped stepped = arms.step(true, FRAME, body, Obstacles.NONE, 10);
            arms = stepped.arms();
            for (final Event e : stepped.events()) {
                if (e instanceof Event.Fired f) {
                    ways.add(f.to().subtract(f.from()).normalize());
                }
            }
        }
        assertTrue(ways.size() > 40);
        for (final Vec3 way : ways) {
            assertEquals(1, way.dotProduct(Arms.line(body, 10)), 1e-12, "along the line, not off it");
        }
    }

    @Test
    void itsSightIsOnItsOwnLine() {
        final Body body = Body.standing(Vec3.ZERO, 0);
        final Vec3 sight = Arms.sight(body, Obstacles.NONE, 0, Weapon.GATLING);
        assertEquals(Arms.GATLING_OUT, sight.x(), 1e-6, "out at the right hip, where the gun is");
        assertEquals(-Arms.GATLING_HEIGHT, sight.y(), 1e-6);
    }
}
