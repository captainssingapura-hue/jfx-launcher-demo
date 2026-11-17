package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How the gunship flies ({@link Flight}): held up by its rotors' thrust, tipping to go anywhere,
 * its rotors tilting forward and its wings taking its weight as it speeds up, and its assist
 * bringing it to a hover when let go.
 */
class FlightTest {

    private static final Flight GUNSHIP = Course.GUNSHIP.flight();
    private static final double FLOOR = Course.GUNSHIP.standing();
    private static final double G = Body.GRAVITY;

    /** On its wheels, still, level, its rotors upright. */
    private static final Flight.State PARKED = new Flight.State(FLOOR, 0, 0, 0, 0, 0, 0, 0, 0, true, 0);

    /** W/S, Q/E and Space/Ctrl, as given: -1 to 1. */
    private static Flight.Input keys(final double forward, final double aside, final double lift) {
        return turning(forward, aside, lift, 0);
    }

    /** And A/D: turning, -1 to 1, right when positive. */
    private static Flight.Input turning(final double forward, final double aside, final double lift,
                                        final double turn) {
        return new Flight.Input(forward, aside, lift, turn, 1, true, true);
    }

    private static Flight.State fly(final Flight.State from, final Flight.Input input, final double seconds) {
        Flight.State state = from;
        for (long step = Math.round(seconds / Clock.STEP); step > 0; step--) {
            state = GUNSHIP.step(state, input, Clock.STEP, FLOOR);
        }
        return state;
    }

    /** Lifted off and hovering, 30 m or so up. */
    private static Flight.State hovering() {
        return fly(fly(PARKED, keys(0, 0, 1), 3), keys(0, 0, 0), 5);
    }

    @Test
    void itSitsOnItsWheelsItsRotorsIdlingUntilAskedToClimb() {
        final Flight.State sat = fly(PARKED, keys(0, 0, 0), 2);
        assertEquals(FLOOR, sat.height(), 1e-9);
        assertTrue(sat.grounded());
        assertEquals(0, sat.thrust(), 1e-9);
        // W on its wheels: it is not flying yet, so it goes nowhere
        final Flight.State pushed = fly(PARKED, keys(1, 0, 0), 2);
        assertEquals(0, pushed.ahead(), 1e-9);
        assertEquals(0, pushed.pitch(), 1e-9);
    }

    @Test
    void itLiftsOffAndClimbsAsFastAsWantedAndNoFaster() {
        final Flight.State climbing = fly(PARKED, keys(0, 0, 1), 3);
        assertFalse(climbing.grounded());
        assertTrue(climbing.height() > FLOOR + 1500, "well up: " + climbing.height());
        assertTrue(climbing.climb() <= GUNSHIP.climb() * 1.02, "no faster than wanted: " + climbing.climb());
        assertEquals(0, climbing.pitch(), 1e-9, "straight up, level");
        assertTrue(climbing.thrust() > 0.6 && climbing.thrust() <= 1);
    }

    @Test
    void letGoItHangsWhereItIsOnItsRotors() {
        final Flight.State hover = hovering();
        assertEquals(0, hover.climb(), 5);
        final Flight.State later = fly(hover, keys(0, 0, 0), 2);
        assertEquals(hover.height(), later.height(), 5, "holding its height");
        // its rotors holding its weight: a share of their most as its weight is of their most
        assertEquals(1 / GUNSHIP.power(), later.thrust(), 0.02);
        assertEquals(0, later.nacelle(), 1e-9, "upright, hovering");
    }

    @Test
    void toGoAheadItDipsItsNoseThenTiltsItsRotorsForwardAndItsWingsTakeItsWeight() {
        final Flight.State hover = hovering();
        final Flight.State starting = fly(hover, keys(1, 0, 0), 0.6);
        assertTrue(starting.pitch() < -10, "its nose down: " + starting.pitch());
        assertTrue(starting.ahead() > 0);
        assertEquals(0, starting.nacelle(), 1, "its rotors upright still, going slowly");
        final Flight.State cruising = fly(starting, keys(1, 0, 0), 15);
        assertEquals(GUNSHIP.ahead(), cruising.ahead(), 0.03 * GUNSHIP.ahead(), "at its cruising speed");
        assertEquals(GUNSHIP.forward(), cruising.nacelle(), 1, "its rotors forward");
        assertTrue(Math.abs(cruising.pitch()) < 8, "about level: " + cruising.pitch());
        assertTrue(GUNSHIP.wingLift(cruising.ahead()) >= G, "its wings carrying its weight");
        assertEquals(0, cruising.climb(), 30, "neither climbing nor sinking");
        assertTrue(cruising.height() > hover.height() - 300, "having lost little height getting going");
    }

    @Test
    void letGoAtSpeedItTiltsItsRotorsBackPitchesUpAndComesToAHover() {
        final Flight.State cruising = fly(hovering(), keys(1, 0, 0), 15);
        final Flight.State slowing = fly(cruising, keys(0, 0, 0), 2);
        assertTrue(slowing.pitch() > 5, "its nose up to brake: " + slowing.pitch());
        assertTrue(slowing.nacelle() < cruising.nacelle(), "its rotors tilting back");
        final Flight.State stopped = fly(slowing, keys(0, 0, 0), 10);
        assertEquals(0, stopped.ahead(), 30);
        assertEquals(0, stopped.nacelle(), 1e-9);
        assertTrue(stopped.height() > cruising.height() - 500, "losing little height: "
                + (cruising.height() - stopped.height()));
    }

    @Test
    void aSideKeySlidesItAsideBankingThatWay() {
        final Flight.State right = fly(hovering(), keys(0, 1, 0), 4);
        assertEquals(GUNSHIP.aside(), right.aside(), 0.1 * GUNSHIP.aside());
        assertTrue(right.roll() > 10, "its right wing down: " + right.roll());
        assertEquals(0, right.ahead(), 5);
        final Flight.State left = fly(hovering(), keys(0, -1, 0), 4);
        assertTrue(left.aside() < -0.9 * GUNSHIP.aside() && left.roll() < -10);
    }

    @Test
    void aOrDTurnsItItsTurningBuildingUpSoATapTurnsItALittle() {
        final Flight.State hover = hovering();
        final Flight.State tapped = fly(fly(hover, turning(0, 0, 0, 1), 0.1), keys(0, 0, 0), 1);
        final double little = Num.wrap(tapped.yaw() - hover.yaw());
        assertTrue(little > 0 && little < 3, "a tap of D, a little right: " + little);
        assertEquals(0, tapped.turning(), 1e-9, "and no more");
        final Flight.State turning = fly(hover, turning(0, 0, 0, 1), 1);
        assertEquals(GUNSHIP.turn(), turning.turning(), 1e-9, "held, as fast as it turns, hovering");
        final Flight.State on = fly(turning, turning(0, 0, 0, 1), 1);
        assertEquals(GUNSHIP.turn(), Num.wrap(on.yaw() - turning.yaw()), 0.5);
        final Flight.State left = fly(hover, turning(0, 0, 0, -1), 1);
        assertEquals(-GUNSHIP.turn(), left.turning(), 1e-9, "A, left");
        assertEquals(0, left.ahead(), 1, "turning on the spot");
        // on its wheels it does not turn
        assertEquals(0, fly(PARKED, turning(0, 0, 0, 1), 1).yaw(), 1e-9);
    }

    @Test
    void goingFastATurnBanksItRoundWithoutSkiddingNoFasterThanItsBankAllows() {
        final Flight.State cruising = fly(hovering(), keys(1, 0, 0), 15);
        final Flight.State round = fly(cruising, turning(1, 0, 0, 1), 4);
        assertTrue(round.roll() > 25, "banked into the turn: " + round.roll());
        assertTrue(Math.abs(round.aside()) < 0.05 * round.ahead(), "going the way it heads: " + round.aside());
        final double banked = Math.toDegrees(G * Math.tan(Math.toRadians(GUNSHIP.bank())) / cruising.ahead());
        assertTrue(Num.wrap(round.yaw() - cruising.yaw()) <= 4 * banked * 1.1, "no faster than its bank allows");
        assertTrue(round.turning() < GUNSHIP.turn(), "slower than hovering: " + round.turning());
    }

    @Test
    void goingFastQOrESlidesItAsideNoMore() {
        final Flight.State cruising = fly(hovering(), keys(1, 0, 0), 15);
        final Flight.State slid = fly(cruising, keys(1, 1, 0), 3);
        assertEquals(0, slid.aside(), 20, "an aeroplane, now: it does not slide");
        assertEquals(0, slid.yaw(), 1e-9);
    }

    @Test
    void itComesDownGentlyOntoItsWheelsAndSitsThere() {
        Flight.State state = hovering();
        double touched = Double.NaN;
        for (int step = 0; step < Math.round(20 / Clock.STEP); step++) {
            final Flight.State next = GUNSHIP.step(state, keys(0, 0, -1), Clock.STEP, FLOOR);
            if (next.grounded() && !state.grounded()) {
                touched = state.climb();
            }
            state = next;
        }
        assertTrue(state.grounded());
        assertEquals(FLOOR, state.height(), 1e-9);
        assertTrue(touched > -2.5 * Flight.SETTLE, "settling, not dropping: " + touched);
    }

    @Test
    void itsEngineCutItHasNoThrustAndDrops() {
        final Flight.State hover = hovering();
        final Flight.State cut = fly(hover, new Flight.Input(0, 0, 0, 0, 1, true, false), 0.5);
        assertEquals(0, cut.thrust(), 1e-9);
        assertTrue(cut.climb() < -G * 0.3, "falling: " + cut.climb());
        final Flight.State down = fly(cut, Flight.Input.none(false), 10);
        assertTrue(down.grounded());
        assertEquals(FLOOR, down.height(), 1e-9);
    }

    @Test
    void itsTopSpeedIsWhereItsThrustMeetsTheDrag() {
        assertTrue(GUNSHIP.top() > GUNSHIP.ahead(), "with something to spare at its cruising speed");
        assertEquals(0, GUNSHIP.nacelleFor(GUNSHIP.converts()), 1e-9);
        assertEquals(GUNSHIP.forward(), GUNSHIP.nacelleFor(GUNSHIP.converted()), 1e-9);
    }
}
