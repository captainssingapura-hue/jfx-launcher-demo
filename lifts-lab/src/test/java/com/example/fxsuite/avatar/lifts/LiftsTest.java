package com.example.fxsuite.avatar.lifts;

import com.example.fxsuite.avatar.sim.Body;
import com.example.fxsuite.avatar.sim.Place;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Each lift's motion, a function of time alone: a paternoster's cabins evenly round its loop, upright, going smoothly,
 * each floor passing every landing; a shuttle lift level with each floor while it stands there, its doors shut while
 * it goes, a landing's doors open only while it stands there; an escalator's steps a stair up its incline, each rising
 * less than a body steps up of itself, flat at either end and dipping under the comb plates.
 */
class LiftsTest {

    private final YardScene yard = new YardScene();

    @Test
    void aPaternostersCabinsGoEvenlyRoundItsLoopUprightAndSmoothly() {
        final Paternoster p = yard.paternoster;
        // round its loop, it comes back where it was
        for (int k = 0; k < p.cabins(); k++) {
            final Place start = p.cabin(k).at(0);
            final Place round = p.cabin(k).at(p.round());
            assertEquals(start.x(), round.x(), 1e-6);
            assertEquals(start.height(), round.height(), 1e-6);
            assertEquals(0, start.yaw(), 1e-9, "upright, its front along");
        }
        // smoothly: never further in a step than its speed takes it
        final double step = 1.0 / 120;
        for (double t = 0; t < p.round(); t += step) {
            final Place a = p.cabin(0).at(t);
            final Place b = p.cabin(0).at(t + step);
            assertTrue(Math.hypot(b.x() - a.x(), b.height() - a.height()) <= p.speed() * step + 1e-6, "at " + t);
        }
        // its floor passes every landing, going up on one side and down on the other
        for (final double floor : YardScene.FLOORS) {
            boolean up = false;
            boolean down = false;
            for (double t = 0; t < p.round(); t += 0.05) {
                final Place a = p.cabin(0).at(t);
                final Place b = p.cabin(0).at(t + 0.05);
                final double was = a.height() + Paternoster.FLOOR;
                final double now = b.height() + Paternoster.FLOOR;
                if (was < floor && now >= floor) {
                    up = true;
                    assertEquals(p.upX(), a.x(), 1e-6, "going up its up shaft");
                }
                if (was > floor && now <= floor) {
                    down = true;
                    assertEquals(p.downX(), a.x(), 1e-6, "coming down its down shaft");
                }
            }
            assertTrue(up && down, "past the landing at " + floor + " both ways");
        }
    }

    @Test
    void aShuttleLiftIsLevelWithEachFloorAsItStandsAndItsDoorsOpenOnlyThen() {
        final ShuttleLift s = yard.shuttle;
        boolean opened = false;
        for (double t = 0; t < s.cycle(); t += 0.02) {
            final ShuttleLift.State state = s.at(t);
            if (state.at() >= 0) {
                assertEquals(s.floors().get(state.at()), state.level(), 1e-9, "level with its floor, at " + t);
                opened |= state.open() > 0.99;
            } else {
                assertEquals(0, state.open(), 1e-9, "its doors shut while it goes, at " + t);
            }
            for (int f = 0; f < s.floors().size(); f++) {
                final double landing = s.landingOpen(f, t);
                assertTrue(landing == 0 || state.at() == f, "landing " + f + " open only with the car there, at " + t);
            }
            // smoothly: never faster than its top speed
            assertTrue(Math.abs(s.at(t + 0.02).level() - state.level()) <= s.speed() * 0.02 + 1e-6, "at " + t);
        }
        assertTrue(opened, "its doors open wide at its stops");
        assertEquals(6 * (s.dwell() + s.travel(400)), s.cycle(), 1e-9, "six stops round, a floor apart");
    }

    @Test
    void anEscalatorsStepsAreAStairUpItsInclineEachRisingLessThanAStep() {
        final Escalator e = yard.escalator;
        assertTrue(e.riser() <= Body.STEP, "each step rises less than a body steps up of itself: " + e.riser());
        // on its incline, two steps one after the other: one a riser over the other, a tread along
        final double spacing = 2 * e.length() / e.steps();
        final double mid = e.flat() + e.run() / 2;
        final Place a = e.step(0).at(mid / e.speed());
        final Place b = e.step(1).at(mid / e.speed());
        assertEquals(spacing, b.z() - a.z(), 1e-6);
        assertEquals(spacing / e.run() * e.rise(), b.height() - a.height(), 1e-6);
        // flat at its head, at its landing's level; dipped under the comb plate at its very end
        assertEquals(e.rise(), e.top(e.length() - Escalator.DIPPING - 1), 1e-6);
        assertEquals(e.rise() - Escalator.DIP, e.top(e.length()), 1e-6);
        assertEquals(-Escalator.DIP, e.top(0), 1e-6);
        assertEquals(0, e.top(Escalator.DIPPING + 1), 1e-6);
    }
}
