package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A hinge driven hydraulically ({@link Actuator}): its pump started, its lock drawn, its valve
 * opened, easing up to its rate, cushioned near its end, stopped; shut, its lock dropped back in.
 */
class ActuatorTest {

    private static final double STEP = 1.0 / 120;
    private static final Actuator SHUT = Actuator.shut(Actuator.Drive.HYDRAULIC, 0, 100, 40);

    /** What happened, each with when, as a door is run for {@code seconds}. */
    private record Run(Actuator actuator, List<Actuator.Event> events, List<Double> times) {
    }

    private static Run run(final Actuator from, final double seconds) {
        Actuator a = from;
        final List<Actuator.Event> events = new ArrayList<>();
        final List<Double> times = new ArrayList<>();
        for (int k = 0; k < Math.round(seconds / STEP); k++) {
            final Actuator.Stepped stepped = a.step(STEP);
            a = stepped.actuator();
            for (final Actuator.Event event : stepped.events()) {
                events.add(event);
                times.add(k * STEP);
            }
        }
        return new Run(a, events, times);
    }

    @Test
    void openingItsPumpStartsItsLockIsDrawnItsValveOpensItIsCushionedAndStops() {
        final Run opened = run(SHUT.asked(true), 6);
        assertEquals(List.of(Actuator.Event.PUMP_START, Actuator.Event.UNLOCK, Actuator.Event.VALVE,
                Actuator.Event.CUSHION, Actuator.Event.STOP, Actuator.Event.PUMP_STOP), opened.events());
        assertEquals(Actuator.Drive.HYDRAULIC.unlock(), opened.times().get(1), 2 * STEP, "its lock drawn first");
        assertEquals(100, opened.actuator().angle(), 1e-9);
        assertEquals(Actuator.Stage.OPEN, opened.actuator().stage());
        // no faster than its rate, its travel and its cushioning taking as long as they should
        final double travel = opened.times().get(4) - opened.times().get(2);
        assertTrue(travel > 100 / 40.0 && travel < 100 / 40.0 + 1.2, "about its travel at its rate: " + travel);
    }

    @Test
    void neverFasterThanItsRateAndSlowerCushionedFasterForAFasterRate() {
        Actuator a = SHUT.asked(true);
        double fastest = 0;
        double cushioned = Double.POSITIVE_INFINITY;
        for (int k = 0; k < 6 / STEP; k++) {
            a = a.step(STEP).actuator();
            fastest = Math.max(fastest, a.speed());
            if (a.cushion()) {
                cushioned = Math.min(cushioned, a.speed() > 0 ? a.speed() : cushioned);
            }
            assertTrue(a.flow() <= 1 + 1e-9);
        }
        assertEquals(40, fastest, 1e-6, "up to its rate, no faster");
        assertTrue(cushioned < 0.5 * 40, "slower, cushioned: " + cushioned);
        final Run fast = run(SHUT.rate(90).asked(true), 6);
        final Run slow = run(SHUT.asked(true), 6);
        assertTrue(fast.times().get(4) < slow.times().get(4), "sooner open, faster");
    }

    @Test
    void closingItsValveOpensItIsCushionedStopsAndItsLockDropsIn() {
        final Run closed = run(run(SHUT.asked(true), 6).actuator().asked(false), 6);
        assertEquals(List.of(Actuator.Event.PUMP_START, Actuator.Event.VALVE, Actuator.Event.CUSHION,
                Actuator.Event.STOP, Actuator.Event.LOCK, Actuator.Event.PUMP_STOP), closed.events());
        assertEquals(0, closed.actuator().angle(), 1e-9);
        assertEquals(Actuator.Stage.SHUT, closed.actuator().stage());
    }

    @Test
    void askedBackPartWayItsValveSwitchesAndItTurnsBack() {
        final Run part = run(SHUT.asked(true), 1.5);
        assertEquals(Actuator.Stage.OPENING, part.actuator().stage());
        final double was = part.actuator().angle();
        assertTrue(was > 10 && was < 90, "part-way: " + was);
        final Run back = run(part.actuator().asked(false), 6);
        assertEquals(Actuator.Event.VALVE, back.events().getFirst(), "its valve switched over");
        assertEquals(Actuator.Stage.SHUT, back.actuator().stage());
        assertTrue(back.events().contains(Actuator.Event.LOCK));
    }

    @Test
    void askedToShutWhileItsLockIsDrawnItsPinDropsStraightBack() {
        final Run drawing = run(SHUT.asked(true), 0.1);
        assertEquals(Actuator.Stage.UNLOCKING, drawing.actuator().stage());
        final Run back = run(drawing.actuator().asked(false), 1);
        assertEquals(Actuator.Event.LOCK, back.events().getFirst());
        assertEquals(Actuator.Stage.SHUT, back.actuator().stage());
        assertEquals(0, back.actuator().angle(), 1e-9, "it never moved");
    }

    @Test
    void putPartWayByHandItIsHeldThereUntilAskedThenGoesWhicheverWay() {
        final Actuator held = SHUT.placed(50);
        assertEquals(Actuator.Stage.HELD, held.stage());
        assertEquals(50, run(held, 2).actuator().angle(), 1e-9, "going nowhere unasked");
        final Run opened = run(held.asked(true), 4);
        assertEquals(List.of(Actuator.Event.PUMP_START, Actuator.Event.VALVE), opened.events().subList(0, 2));
        assertEquals(Actuator.Stage.OPEN, opened.actuator().stage());
        final Run shut = run(held.asked(false), 4);
        assertEquals(Actuator.Stage.SHUT, shut.actuator().stage());
        assertEquals(Actuator.Stage.SHUT, SHUT.placed(-5).stage(), "at its least, shut");
        assertEquals(Actuator.Stage.OPEN, SHUT.placed(100).stage());
    }

    @Test
    void itsPumpSpoolsUpWhileItWorksAndRunsDownWhenItHasDone() {
        final Run working = run(SHUT.asked(true), 1);
        assertEquals(1, working.actuator().spool(), 1e-9, "running");
        assertTrue(working.actuator().busy());
        final Run done = run(SHUT.asked(true), 6);
        assertEquals(0, done.actuator().spool(), 1e-9, "run down");
        assertFalse(done.actuator().busy());
    }
}
