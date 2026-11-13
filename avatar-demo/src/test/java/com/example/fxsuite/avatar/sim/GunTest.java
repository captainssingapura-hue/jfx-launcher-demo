package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The firing common to every gun, whatever holds it: driven here by bores given outright, as a
 * mount would give them, with no body at all.
 */
class GunTest {

    /** Level along +z from 1.5 m up, its right being +x. */
    private static final Gun.Bore AHEAD = new Gun.Bore(new Vec3(0, -150, 0), new Vec3(0, 0, 1), new Vec3(1, 0, 0));
    private static final Gun.Muzzles STILL = left -> AHEAD;

    private static long fired(final Gun.Fired fired) {
        return fired.events().stream().filter(e -> e instanceof Event.Fired).count();
    }

    @Test
    void aPairFiresInTurnLeftThenRightAtItsRate() {
        final Gun.Fired one = Gun.RIFLES.fire(Gun.State.READY, true, 1, STILL, Obstacles.NONE);
        // a round at once, then one each interval: both rifles together, twice a rifle's rate
        assertEquals(1 / Gun.RIFLES.interval(), fired(one), 1);
        final List<Boolean> sides = one.events().stream().map(e -> ((Event.Fired) e).left()).toList();
        for (int i = 0; i < sides.size(); i++) {
            assertEquals(i % 2 == 0, sides.get(i), "left, right, left");
        }
    }

    @Test
    void aGunThatSpinsFiresOnlyOnceUpToSpeed() {
        Gun.State state = Gun.State.READY;
        long rounds = 0;
        for (double t = 0; t < Gun.GATLING.spinUp() - Clock.STEP; t += Clock.STEP) {
            final Gun.Fired step = Gun.GATLING.fire(state, true, Clock.STEP, STILL, Obstacles.NONE);
            state = step.state();
            rounds += fired(step);
        }
        assertEquals(0, rounds, "still spinning up");
        assertTrue(state.spin() < 1);
        final Gun.Fired second = Gun.GATLING.fire(state, true, 1, STILL, Obstacles.NONE);
        assertEquals(1, second.state().spin(), 1e-12);
        assertEquals(1 / Gun.GATLING.interval(), fired(second), 2, "then at its rate");
        // let go, it stops at once and spins down
        final Gun.Fired off = Gun.GATLING.fire(second.state(), false, Gun.GATLING.spinDown() / 2, STILL,
                Obstacles.NONE);
        assertEquals(0, fired(off));
        assertEquals(0.5, off.state().spin(), 1e-9);
    }

    @Test
    void aSingleGunThatDoesNotSpinFiresFromItsOneMuzzle() {
        final Gun single = new Gun(Weapon.RIFLES, 0.25, 0, 0, 0, false, 10_000);
        final Gun.Fired fired = single.fire(Gun.State.READY, true, 1, STILL, Obstacles.NONE);
        assertEquals(5, fired(fired), "at once, then each quarter second");
        assertTrue(fired.events().stream().noneMatch(e -> ((Event.Fired) e).left()), "never the left");
    }

    @Test
    void itsRoundsFlyAlongTheBoreTheyAreGivenToWhatTheyStrike() {
        // a crate 10 m off to the right, and the bore turned to it: wherever the gun is pointed
        final Block crate = new Block(1000, 0, 100, 100, 0, 0, 300);
        final Obstacles world = new Obstacles(List.of(crate));
        final Gun.Bore right = new Gun.Bore(new Vec3(0, -150, 0), new Vec3(1, 0, 0), new Vec3(0, 0, -1));
        final Gun.Fired fired = Gun.GATLING.fire(new Gun.State(0, true, 1e9, 1e9, 0, 1), true, 0.2, left -> right,
                world);
        assertFalse(fired.blows().isEmpty());
        for (final Blow blow : fired.blows()) {
            assertEquals(900, blow.at().x(), 1e-6, "on its near face");
            assertEquals(Weapon.GATLING, blow.by());
        }
        // the same, pointed straight ahead instead: past it, into the open
        assertTrue(Gun.GATLING.fire(new Gun.State(0, true, 1e9, 1e9, 0, 1), true, 0.2, STILL, world).blows()
                .isEmpty());
    }

    @Test
    void offTrueByNoMoreThanItsSpreadAndTheSameEveryTime() {
        final Gun.Fired once = Gun.RIFLES.fire(Gun.State.READY, true, 2, STILL, Obstacles.NONE);
        final Gun.Fired again = Gun.RIFLES.fire(Gun.State.READY, true, 2, STILL, Obstacles.NONE);
        assertEquals(once.events(), again.events(), "a run repeats exactly");
        final double most = Math.tan(Math.toRadians(Gun.RIFLES.spread())) * Math.sqrt(2) + 1e-9;
        for (final Event e : once.events()) {
            final Event.Fired f = (Event.Fired) e;
            final Vec3 way = f.to().subtract(f.from()).normalize();
            assertTrue(Math.hypot(way.x(), way.y()) / way.z() <= most, "within the spread");
        }
    }
}
