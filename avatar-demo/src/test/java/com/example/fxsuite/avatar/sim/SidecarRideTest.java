package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The boxer with its sidecar, in the world: an outfit of its own, on three wheels, not leaning, got
 * onto from the side away from its sidecar, taking the sidecar's room; ridden too hard toward its
 * sidecar, held against its table, it goes over on its side, throwing its rider; away from it, it
 * holds; over, it lies still until it is righted, beside it, and then it can be ridden again.
 */
class SidecarRideTest {

    private static final Controls TAKE = Controls.doing(Action.TAKE);
    private static final Course.Chassis OUTFIT = Course.BOXER_SIDECAR;
    /** 30 km/h, in cm/s. */
    private static final double THIRTY = 3000 / 3.6;

    private static Play run(Play play, final Controls controls, final double seconds, final List<Event> events) {
        for (long step = Math.round(seconds / Clock.STEP); step > 0; step--) {
            final Play.Stepped stepped = play.step(controls, Clock.STEP);
            play = stepped.play();
            events.addAll(stepped.events());
        }
        return play;
    }

    private static int outfit(final Play play) {
        for (int i = 0; i < play.lifts().size(); i++) {
            if (play.lifts().get(i).craft() == OUTFIT) {
                return i;
            }
        }
        throw new AssertionError("no outfit");
    }

    /** The roads demo, the avatar on the outfit, riding it ahead at {@code speed}. */
    private static Play riding(final double speed, final List<Event> events) {
        final Play start = Play.start(Demo.FLYOVER, false);
        final int k = outfit(start);
        final Vec3 door = start.lifts().get(k).at(OUTFIT.door());
        Play play = new Play(new Avatar(Body.standing(new Vec3(door.x(), 0, door.z()), 0), false), start.damage(),
                start.sky(), start.lifts(), start.people(), start.props(), start.turrets());
        assertTrue(play.withinReach().isPresent(), "got onto from its door");
        play = run(play, TAKE, Clock.STEP, events);
        assertTrue(play.lifts().get(k).held());
        // going at the speed asked, straight ahead
        final Lift was = play.lifts().get(k);
        final List<Lift> lifts = new ArrayList<>(play.lifts());
        lifts.set(k, new Lift(was.piece(), OUTFIT, was.place(), speed, 0, 0, true, true, 0, 0, 0, 0));
        return new Play(play.avatar(), play.damage(), play.sky(), lifts, play.people(), play.props(), play.turrets());
    }

    @Test
    void itIsAnOutfitOfItsOwnThatDoesNotLeanTakingTheSidecarsRoom() {
        assertEquals(OUTFIT, Course.Chassis.of(OUTFIT.shape()), "told from the boxer by its sidecar's parts");
        assertEquals(Course.BOXER, Course.Chassis.of(Course.BOXER.shape()));
        assertFalse(OUTFIT.leans());
        assertTrue(Course.BOXER.leans());
        assertTrue(OUTFIT.door().across() < 0, "got onto from the left, away from the sidecar on its right");
        final double[] reach = OUTFIT.reach();
        assertEquals(Sidecar.BOXER.track() + Sidecar.BOXER.width() / 2, reach[1], 1e-9, "out to its sidecar's wheel");
        assertEquals(-OUTFIT.half(), reach[0], 1e-9);
        final Course.Section room = OUTFIT.room();
        assertTrue(room.across() - room.halfAcross() <= reach[0] + 1e-9 && room.across() + room.halfAcross() >= reach[1] - 1e-9,
                "its room all of it");
        assertTrue(OUTFIT.drive().top() < Course.BOXER.drive().top(), "slower than the boxer alone");
        assertTrue(OUTFIT.drive().mass() > Course.BOXER.drive().mass(), "and heavier");
    }

    @Test
    void riddenStraightItStaysOnItsWheels() {
        final List<Event> events = new ArrayList<>();
        Play play = riding(THIRTY, events);
        final int k = outfit(play);
        play = run(play, new Controls(1, 0, 0, 0, true), 2, events);
        final Lift going = play.lifts().get(k);
        assertFalse(going.over());
        assertEquals(0, going.tipped(), 1e-12);
        assertEquals(0, going.lean(), 1e-12, "it does not lean");
        assertEquals(0, going.heel(), 1e-12);
        assertTrue(going.held());
    }

    @Test
    void steeredHardTowardItsSidecarItGoesOverThrowingItsRider() {
        final List<Event> events = new ArrayList<>();
        Play play = riding(THIRTY, events);
        final int k = outfit(play);
        final int piece = play.lifts().get(k).piece();
        events.clear();
        // D: right, toward the sidecar on its right
        play = run(play, new Controls(0, 1, 0, 0, true), 1, events);
        final Lift over = play.lifts().get(k);
        assertTrue(over.over(), "over on its side");
        assertFalse(over.held(), "its rider thrown off");
        assertTrue(events.contains(new Event.LetGo(piece)));
        assertTrue(events.stream().anyMatch(e -> e instanceof Event.Settled s && s.piece() == piece), "with a thud");
        assertEquals(-Lift.ON_ITS_SIDE, over.heel(), 1e-9, "on its left side, the sidecar up");
        // it lies where it is, still
        final Place lying = over.place();
        play = run(play, Controls.NONE, 1, events);
        assertEquals(lying, play.lifts().get(k).place());
        assertTrue(play.lifts().get(k).over());
    }

    @Test
    void theSameSteerAwayFromItsSidecarItHolds() {
        final List<Event> events = new ArrayList<>();
        Play play = riding(THIRTY, events);
        final int k = outfit(play);
        // A: left, away from the sidecar
        play = run(play, new Controls(0, -1, 0, 0, true), 1, events);
        assertFalse(play.lifts().get(k).over());
        assertTrue(play.lifts().get(k).held());
        assertTrue(play.lifts().get(k).steer() < -10, "steered hard: " + play.lifts().get(k).steer());
    }

    /** 60 km/h, in cm/s. */
    private static final double SIXTY = 6000 / 3.6;

    @Test
    void itsBarsTurnTheGentlierTheFasterItGoesAndNoFurtherThanItCanTake() {
        final Outfit outfit = Outfit.of(OUTFIT);
        final List<Outfit.Row> table = Outfit.table(OUTFIT);
        final double fastest = OUTFIT.drive().steerRate();
        // toward the sidecar (right): slower, and less far, the faster it goes
        double lastRate = Double.POSITIVE_INFINITY;
        double lastLock = Double.POSITIVE_INFINITY;
        for (final double kmh : new double[] {20, 30, 40, 60, 90}) {
            final double v = kmh * 100 / 3.6;
            final double rate = outfit.rate(table, v, 1, fastest);
            final double lock = outfit.lock(table, v, 1, OUTFIT.drive().steer());
            assertTrue(rate <= lastRate && lock < lastLock, kmh + " km/h: " + rate + " deg/s, " + lock + " deg");
            // round to the sharpest it may be steered in half a second (or as slow as the bars go)
            final double sharpest = Outfit.lookUp(table, v, true, OUTFIT.drive().steer());
            assertEquals(Math.max(Outfit.SLOWEST, sharpest / Outfit.ROLL_IN), rate, 1e-9);
            assertEquals(Outfit.LOCK * sharpest, lock, 1e-9, "held over, still past its limit");
            lastRate = rate;
            lastLock = lock;
        }
        // away from it, never so far as its limit that way
        final double away = outfit.lock(table, 40 * 100 / 3.6, -1, OUTFIT.drive().steer());
        assertTrue(away < Outfit.lookUp(table, 40 * 100 / 3.6, false, OUTFIT.drive().steer()));
    }

    @Test
    void aTouchTowardTheSidecarAtSpeedDoesNotTipItAndTheBarsComeBack() {
        final List<Event> events = new ArrayList<>();
        Play play = riding(SIXTY, events);
        final int k = outfit(play);
        // a tenth of a second of D, as a tap is
        play = run(play, new Controls(1, 1, 0, 0, true), 0.1, events);
        final double tapped = play.lifts().get(k).steer();
        assertTrue(tapped > 0 && tapped < Outfit.lookUp(Outfit.table(OUTFIT), SIXTY, true, 20) * Outfit.LIFTS,
                "a little, short of lifting the chair: " + tapped);
        play = run(play, new Controls(1, 0, 0, 0, true), 1, events);
        assertFalse(play.lifts().get(k).over());
        assertTrue(play.lifts().get(k).held());
        assertEquals(0, play.lifts().get(k).steer(), 1e-9, "the bars back straight");
    }

    @Test
    void heldTowardTheSidecarTheChairLiftsThenItGoesOver() {
        final List<Event> events = new ArrayList<>();
        Play play = riding(SIXTY, events);
        final int k = outfit(play);
        boolean lifted = false;
        for (int step = 0; step < 240 && !play.lifts().get(k).over(); step++) {
            play = run(play, new Controls(1, 1, 0, 0, true), Clock.STEP, events);
            lifted |= play.lifts().get(k).heel() != 0 && !play.lifts().get(k).over();
        }
        assertTrue(lifted, "the chair lifting first");
        assertTrue(play.lifts().get(k).over(), "held, over within two seconds");
    }

    @Test
    void heldAwayFromTheSidecarItNeverTips() {
        final List<Event> events = new ArrayList<>();
        Play play = riding(40 * 100 / 3.6, events);
        final int k = outfit(play);
        play = run(play, new Controls(1, -1, 0, 0, true), 3, events);
        assertFalse(play.lifts().get(k).over());
        assertTrue(play.lifts().get(k).held());
    }

    @Test
    void letGoTheBarsComeBackFasterThanTheyTurnedIn() {
        final List<Event> events = new ArrayList<>();
        Play play = riding(THIRTY, events);
        final int k = outfit(play);
        play = run(play, new Controls(1, 1, 0, 0, true), 0.3, events);
        final double turned = play.lifts().get(k).steer();
        int back = 0;
        while (play.lifts().get(k).steer() > 0 && back < 240) {
            play = run(play, new Controls(1, 0, 0, 0, true), Clock.STEP, events);
            back++;
        }
        assertTrue(back * Clock.STEP < 0.3 / 2, "back in " + back * Clock.STEP + " s from " + turned + " degrees");
        assertFalse(play.lifts().get(k).over());
    }

    @Test
    void overItIsRightedBesideItAndThenRiddenAgain() {
        final List<Event> events = new ArrayList<>();
        Play play = riding(THIRTY, events);
        final int k = outfit(play);
        final int piece = play.lifts().get(k).piece();
        play = run(play, new Controls(0, 1, 0, 0, true), 1, events);
        assertTrue(play.lifts().get(k).over());
        play = run(play, Controls.NONE, 1, events);
        // thrown off beside it: T there rights it, rather than getting on
        events.clear();
        play = run(play, TAKE, Clock.STEP, events);
        final Lift righted = play.lifts().get(k);
        assertFalse(righted.over(), "back on its wheels");
        assertFalse(righted.held(), "not got onto yet");
        assertEquals(0, righted.heel(), 1e-12);
        assertTrue(events.stream().noneMatch(e -> e instanceof Event.TookControls));
        // and T again gets on
        play = run(play, TAKE, Clock.STEP, events);
        assertTrue(play.lifts().get(k).held());
        assertTrue(events.contains(new Event.TookControls(piece)));
    }
}
