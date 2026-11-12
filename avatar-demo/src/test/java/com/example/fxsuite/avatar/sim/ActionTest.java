package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What the player does once comes in with a step, and is done at the start of it, in order. */
class ActionTest {

    private static final double STEP = Clock.STEP;

    private static Avatar standing(final boolean pack) {
        return new Avatar(Body.standing(Vec3.ZERO, 0), pack);
    }

    @Test
    void aWeaponDrawnInAStepIsHeldAfterIt() {
        final Avatar.Stepped after = standing(false).step(Controls.doing(new Action.Draw(Weapon.GATLING)), STEP,
                Obstacles.NONE);
        assertEquals(Weapon.GATLING, after.avatar().holding());
        assertEquals(List.of(new Event.Drew(Weapon.GATLING)), after.events());
    }

    @Test
    void actionsComeBeforeTheMoveSoTheyCountInTheSameStep() {
        // drawn and squeezed in one step: the barrels are already turning at its end
        final Controls drawAndFire = new Controls(0, 0, 0, 0, false, true, 0,
                List.of(new Action.Draw(Weapon.GATLING)));
        assertTrue(standing(false).step(drawAndFire, STEP, Obstacles.NONE).avatar().arms().spin() > 0);
        // and a jump leaves the ground in the step it comes with
        final Avatar.Stepped jumped = standing(false).step(Controls.doing(Action.JUMP), STEP, Obstacles.NONE);
        assertTrue(jumped.avatar().body().height() > 0);
        assertEquals(List.of(new Event.Jumped()), jumped.events());
    }

    @Test
    void theyAreDoneInTheOrderTheyWereDone() {
        // the pack on, then take off: flying
        final Avatar.Stepped packThenFly = standing(false).step(Controls.doing(Action.PACK, Action.FLY), STEP,
                Obstacles.NONE);
        assertTrue(packThenFly.avatar().body().flying());
        assertEquals(List.of(new Event.PackOn(), new Event.TookOff()), packThenFly.events());
        // take off first, without the pack: nothing, then the pack goes on
        final Avatar.Stepped flyThenPack = standing(false).step(Controls.doing(Action.FLY, Action.PACK), STEP,
                Obstacles.NONE);
        assertFalse(flyThenPack.avatar().body().flying());
        assertEquals(List.of(new Event.PackOn()), flyThenPack.events());
    }

    @Test
    void whatCannotBeDoneIsLeftUndone() {
        final Avatar inFlight = standing(true).toggledFlight();
        assertSame(inFlight, inFlight.act(Action.JUMP).avatar(), "no jumping in flight");
        assertTrue(inFlight.act(Action.JUMP).events().isEmpty(), "and nothing happens");
        final Avatar packless = standing(false);
        assertSame(packless, packless.act(Action.FLY).avatar(), "no flight without the pack");
    }

    @Test
    void drawingWhatIsHeldPutsItAway() {
        final Avatar armed = standing(false).drawn(Weapon.RIFLES);
        final Avatar.Stepped after = armed.step(Controls.doing(new Action.Draw(Weapon.RIFLES)), STEP, Obstacles.NONE);
        assertEquals(Weapon.NONE, after.avatar().holding());
        assertEquals(List.of(new Event.Drew(Weapon.NONE)), after.events());
    }

    @Test
    void controlsKeepTheirActionsAndANewSetCanBeAdded() {
        final Controls held = new Controls(1, 0, 2, 0, true, false, 5);
        assertTrue(held.actions().isEmpty());
        final Controls doing = held.with(List.of(Action.JUMP));
        assertEquals(List.of(Action.JUMP), doing.actions());
        assertEquals(held.forward(), doing.forward());
        assertEquals(held.aim(), doing.aim());
    }
}
