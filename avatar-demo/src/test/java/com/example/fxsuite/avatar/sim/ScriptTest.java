package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A script as inputs stamped with their steps: the same script from the same world, the same run. */
class ScriptTest {

    @Test
    void secondsBecomeStepsOnTheClock() {
        // in at half a second, held for two: steps 61 to 300
        final Script script = Script.parse("W:2");
        assertEquals(300, script.length());
        assertEquals(Set.of(), script.held(60));
        assertEquals(Set.of("W"), script.held(61));
        assertEquals(Set.of("W"), script.held(300));
        assertEquals(Set.of(), script.held(301));
        assertEquals(List.of("W"), script.pressed(61), "pressed in the step it comes in at");
        assertEquals(List.of(), script.pressed(62), "and only then");
    }

    @Test
    void aPartWithoutSecondsIsATapAndOneWithoutKeysJustWaits() {
        final Script script = Script.parse("f; :1 ;j");
        assertEquals(List.of("F"), script.pressed(61));
        assertTrue(script.held(78).contains("F"), "a tap is held for 0.15 s, 18 steps");
        assertFalse(script.held(79).contains("F"));
        assertEquals(List.of("J"), script.pressed(199), "after the second's wait");
        assertEquals(216, script.length());
        assertEquals(0, Script.parse(" ").length(), "a blank script is over at once");
    }

    @Test
    void aTurnAndATiltComeOutExactlyAsWrittenSpreadOverTheirSteps() {
        final Script script = Script.parse("@90:1;^20:0.5");
        double turned = 0;
        for (long step = 1; step <= script.length(); step++) {
            turned += script.turn(step);
        }
        assertEquals(90, turned, 1e-9);
        assertEquals(90.0 / 120, script.turn(100), 1e-12, "the same share each step");
        assertEquals(0, script.aim(180), 1e-12, "level until the tilt comes in");
        assertEquals(10, script.aim(210), 1e-9, "half way up half way through");
        assertEquals(20, script.aim(240), 1e-9);
        assertEquals(20, script.aim(1000), 1e-9, "and the aim stays where it was left");
    }

    @Test
    void theScriptsKeysMeanWhatTheKeyboardsDo() {
        final Script script = Script.parse("SPACE+W+SHIFT+FIRE:0.5");
        final Controls first = script.controls(61);
        assertEquals(Keys.controls(Set.of("SPACE", "W", "SHIFT"), List.of("SPACE", "W", "SHIFT"), 0, true, 0), first);
        assertEquals(1, first.forward());
        assertEquals(1, first.lift(), "space held climbs");
        assertFalse(first.run(), "shift held goes carefully");
        assertTrue(first.trigger());
        assertEquals(List.of(Action.JUMP), first.actions(), "and pressed, it jumps, once");
        assertEquals(List.of(), script.controls(62).actions());
        assertEquals(List.of(new Action.Draw(Weapon.GATLING)), Script.parse("DIGIT3").controls(61).actions());
        assertEquals(Controls.NONE.forward(), Script.parse("Q:1").controls(70).forward(),
                "the camera's keys mean nothing to the world");
    }

    @Test
    void theSameScriptFromTheSameWorldIsTheSameRunEveryTime() {
        final Script script = Script.parse("DIGIT1;FIRE:2;@30+W:1;FIRE+^8:1.5");
        final Run once = run(script);
        final Run again = run(script);
        assertTrue(once.events.stream().anyMatch(e -> e instanceof Event.Struck), "it hits something");
        assertEquals(once.events, again.events);
        assertEquals(once.play.avatar(), again.play.avatar());
        assertEquals(once.left, again.left);
    }

    @Test
    void howeverTheFramesFallTheRunIsTheSame() {
        // as the app plays it: frames of uneven lengths, as many steps as each holds, each step
        // taking its own inputs from the script
        final Script script = Script.parse("DIGIT1;FIRE+@-20:2;W+SHIFT:1");
        final Run straight = run(script);
        final double[] frames = {1 / 60.0, 1 / 144.0, 0.031, 1 / 60.0, 0.0042, 0.05};
        Clock clock = Clock.START;
        Play play = start();
        final List<Event> events = new ArrayList<>();
        long step = 0;
        for (int frame = 0; step < script.length(); frame++) {
            final Clock.Ticked ticked = clock.advance(frames[frame % frames.length]);
            clock = ticked.clock();
            for (int i = 0; i < ticked.steps() && step < script.length(); i++) {
                final Play.Stepped stepped = play.step(script.controls(++step), Clock.STEP);
                play = stepped.play();
                events.addAll(stepped.events());
            }
        }
        assertEquals(straight.events, events);
        assertEquals(straight.play.avatar(), play.avatar());
        assertEquals(straight.left, left(play));
    }

    private record Run(Play play, List<Event> events, List<Double> left) {
    }

    private static Play start() {
        return new Play(new Avatar(Body.standing(Vec3.ZERO, Course.RANGE_HEADING), false), Damage.of(new Course()));
    }

    private static Run run(final Script script) {
        Play play = start();
        final List<Event> events = new ArrayList<>();
        for (long step = 1; step <= script.length(); step++) {
            final Play.Stepped stepped = play.step(script.controls(step), Clock.STEP);
            play = stepped.play();
            events.addAll(stepped.events());
        }
        return new Run(play, events, left(play));
    }

    /** What each piece of the world has left. */
    private static List<Double> left(final Play play) {
        final List<Double> left = new ArrayList<>();
        for (int piece = 0; piece < new Course().pieces().size(); piece++) {
            left.add(play.damage().left(piece));
        }
        return left;
    }
}
