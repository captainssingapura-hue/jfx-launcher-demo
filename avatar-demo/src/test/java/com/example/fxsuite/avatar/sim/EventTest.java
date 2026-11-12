package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the simulation reports, for the outputs to act on: the moments a state alone would lose,
 * and how hard the jet is firing.
 */
class EventTest {

    private static final double FRAME = 1.0 / 60;
    private static final Controls AHEAD = new Controls(1, 0, 0, 0, false);

    /** Runs the avatar, a step at a time, and gathers what each step reports. */
    private static List<Event> heard(Avatar avatar, final Controls controls, final double seconds,
                                     final Obstacles world, final List<Avatar> last) {
        final List<Event> all = new ArrayList<>();
        for (double t = 0; t < seconds - 1e-9; t += FRAME) {
            final Avatar.Stepped stepped = avatar.step(controls, FRAME, world);
            all.addAll(stepped.events());
            avatar = stepped.avatar();
        }
        last.add(avatar);
        return all;
    }

    private static Avatar standing(final boolean pack) {
        return new Avatar(Body.standing(Vec3.ZERO, 0), pack);
    }

    @Test
    void walkingPutsTheFeetDownInTurnOnceAStride() {
        final List<Avatar> end = new ArrayList<>();
        final List<Event> heard = heard(standing(false), AHEAD, 3, Obstacles.NONE, end);
        final List<Boolean> feet = heard.stream()
                .filter(e -> e instanceof Event.Footfall).map(e -> ((Event.Footfall) e).left()).toList();
        // about 3 s at walking pace, a stride's worth of ground per two footfalls
        final double strides = end.get(0).body().position().z() / Body.stride(Body.WALK);
        assertEquals(2 * strides, feet.size(), 2, "footfalls: " + feet);
        for (int i = 1; i < feet.size(); i++) {
            assertTrue(feet.get(i) != feet.get(i - 1), "left and right in turn: " + feet);
        }
        assertTrue(heard.stream().allMatch(e -> e instanceof Event.Footfall), "nothing else: " + heard);
    }

    @Test
    void standingStillMakesNoFootfalls() {
        assertTrue(heard(standing(false), Controls.NONE, 2, Obstacles.NONE, new ArrayList<>()).isEmpty());
    }

    @Test
    void aJumpIsHeardLeavingAndLandingAndHowHard() {
        final Avatar.Stepped jumped = standing(false).act(Action.JUMP);
        assertEquals(List.of(new Event.Jumped()), jumped.events());
        final List<Event> heard = heard(jumped.avatar(), Controls.NONE, 1.5, Obstacles.NONE, new ArrayList<>());
        assertTrue(heard.stream().noneMatch(e -> e instanceof Event.Jumped), "the jump was reported once, as it was done");
        final Event.Landed landed = (Event.Landed) heard.stream()
                .filter(e -> e instanceof Event.Landed).findFirst().orElseThrow();
        assertEquals(Body.JUMP, landed.speed(), Body.JUMP * 0.05, "lands as fast as it left");
        assertFalse(landed.fromFlight());
        assertEquals(Surface.GROUND, landed.on());
    }

    /** A wide, low block: a platform 100 high, the avatar standing in the middle of it. */
    private static final Obstacles PLATFORM = new Obstacles(List.of(Block.standing(0, 0, 3000, 3000, 100, 0)));

    private static Avatar onThePlatform() {
        return new Avatar(Body.standing(new Vec3(0, -100, 0), 0), false);
    }

    @Test
    void walkingSaysWhatTheFeetAreOn() {
        final List<Event> onGround = heard(standing(false), AHEAD, 2, Obstacles.NONE, new ArrayList<>());
        final List<Event> onBlock = heard(onThePlatform(), AHEAD, 2, PLATFORM, new ArrayList<>());
        assertFalse(onGround.isEmpty());
        assertFalse(onBlock.isEmpty());
        assertTrue(onGround.stream().allMatch(e -> ((Event.Footfall) e).on() == Surface.GROUND), "" + onGround);
        assertTrue(onBlock.stream().allMatch(e -> ((Event.Footfall) e).on() == Surface.BLOCK), "" + onBlock);
    }

    @Test
    void landingOnAPlatformSaysSo() {
        final List<Event> heard = heard(onThePlatform().jumped(), Controls.NONE, 1.5, PLATFORM, new ArrayList<>());
        final Event.Landed landed = (Event.Landed) heard.stream()
                .filter(e -> e instanceof Event.Landed).findFirst().orElseThrow();
        assertEquals(Surface.BLOCK, landed.on());
        // and stepping off the edge of a high crate, onto the ground below
        final Obstacles crate = new Obstacles(List.of(Block.standing(0, 0, 200, 200, 150, 0)));
        final List<Event> off = heard(new Avatar(Body.standing(new Vec3(0, -150, 0), 0), false), AHEAD, 2, crate,
                new ArrayList<>());
        final Event.Landed down = (Event.Landed) off.stream()
                .filter(e -> e instanceof Event.Landed).findFirst().orElseThrow();
        assertEquals(Surface.GROUND, down.on());
    }

    @Test
    void flyingThroughAGateIsHeardOnceAtItsSpeed() {
        // a gate 1500 ahead, its middle 800 up; flown at with the body's middle at that height
        final Gate gate = new Gate(0, 1500, 0, 800, 560);
        final Obstacles world = new Obstacles(List.of(), List.of(gate));
        final Body flying = new Body(new Vec3(0, -(800 - Body.TALL / 2), 0), 0, 0, Body.FLY, 0, 0, 0, true, false, 0, 0);
        final List<Event> through = heard(new Avatar(flying, true), AHEAD, 2.5, world, new ArrayList<>());
        final List<Event> passed = through.stream().filter(e -> e instanceof Event.PassedGate).toList();
        assertEquals(1, passed.size(), "" + through);
        assertEquals(Body.FLY, ((Event.PassedGate) passed.get(0)).speed(), Body.FLY * 0.05);

        // beside it, or over it, nothing
        for (final Vec3 elsewhere : List.of(new Vec3(400, flying.position().y(), 0), new Vec3(0, -1200, 0))) {
            final Body missing = new Body(elsewhere, 0, 0, Body.FLY, 0, 0, 0, true, false, 0, 0);
            assertTrue(heard(new Avatar(missing, true), AHEAD, 2.5, world, new ArrayList<>()).stream()
                    .noneMatch(e -> e instanceof Event.PassedGate), "missed from " + elsewhere);
        }
        // and back the other way, it counts again
        assertTrue(gate.passedBy(new Vec3(0, -700, 1600), new Vec3(0, -700, 1400), Body.TALL));
    }

    @Test
    void aJumpThatCannotHappenIsNotHeard() {
        final Avatar inAir = standing(false).jumped().step(Controls.NONE, 0.1, Obstacles.NONE).avatar();
        assertTrue(inAir.act(Action.JUMP).events().isEmpty());
    }

    @Test
    void runningIntoAWallBumpsOnceThenOnlyLeans() {
        final Obstacles wall = new Obstacles(List.of(Block.standing(0, 300, 4000, 100, 1000, 0)));
        final List<Event> heard = heard(standing(false), new Controls(1, 0, 0, 0, true), 3, wall, new ArrayList<>());
        final List<Event> bumps = heard.stream().filter(e -> e instanceof Event.Bumped).toList();
        assertEquals(1, bumps.size(), "one bump, not one a frame: " + bumps);
        assertTrue(((Event.Bumped) bumps.get(0)).speed() > Body.WALK, "at running speed: " + bumps);
    }

    @Test
    void flyingIsHeardTakingOffAndTouchingDown() {
        final Avatar.Stepped off = standing(true).act(Action.FLY);
        assertEquals(List.of(new Event.TookOff()), off.events());
        final List<Avatar> end = new ArrayList<>();
        heard(off.avatar(), new Controls(0, 0, 0, 1, false), 1, Obstacles.NONE, end);
        final List<Event> down = heard(end.get(0), new Controls(0, 0, 0, -1, false), 5, Obstacles.NONE,
                new ArrayList<>());
        final Event.Landed landed = (Event.Landed) down.stream()
                .filter(e -> e instanceof Event.Landed).findFirst().orElseThrow();
        assertTrue(landed.fromFlight());
        assertEquals(Body.LIFT, landed.speed(), Body.LIFT * 0.05, "at full descent");
    }

    @Test
    void thePackIsHeardGoingOnAndOffAndCuttingAFlight() {
        assertEquals(List.of(new Event.PackOn()), standing(false).act(Action.PACK).events());
        final Avatar flying = standing(true).toggledFlight();
        assertEquals(List.of(new Event.FlightCut(), new Event.PackOff()), flying.act(Action.PACK).events());
        assertTrue(standing(false).act(Action.FLY).events().isEmpty(), "no pack, no take-off, nothing heard");
    }

    @Test
    void eachStepReportsOnlyWhatHappenedInIt() {
        final Avatar jumped = standing(false).act(Action.JUMP).avatar();
        final Avatar.Stepped next = jumped.step(Controls.NONE, FRAME, Obstacles.NONE);
        assertTrue(next.events().stream().noneMatch(e -> e instanceof Event.Jumped), "the jump was its own");
    }

    @Test
    void theJetFiresOnlyFlyingAndWithThePack() {
        assertEquals(0, standing(true).thrust(), 1e-9, "on the ground");
        final Avatar hovering = standing(true).toggledFlight().step(Controls.NONE, 1, Obstacles.NONE).avatar();
        assertTrue(hovering.thrust() > 0);
        Avatar boosting = hovering;
        for (int i = 0; i < 120; i++) {
            boosting = boosting.step(new Controls(1, 0, 0, 0, true), FRAME, Obstacles.NONE).avatar();
        }
        assertTrue(boosting.thrust() > hovering.thrust(), "harder to go fast");
        assertEquals(0, new Avatar(boosting.body(), false).thrust(), 1e-9, "no pack, no jet");
    }
}
