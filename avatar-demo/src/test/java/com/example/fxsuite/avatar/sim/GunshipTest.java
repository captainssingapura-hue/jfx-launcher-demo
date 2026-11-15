package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The gunship in the playground: parked on its wheels, its engine off; boarded from beside its
 * door; started (F), lifted off (Space), flown (the avatar's keys and mouse), brought down onto its
 * wheels (Ctrl) and stepped out of (T).
 */
class GunshipTest {

    private static Play run(Play play, final Controls controls, final double seconds, final List<Event> events) {
        for (long step = Math.round(seconds / Clock.STEP); step > 0; step--) {
            final Play.Stepped stepped = play.step(controls, Clock.STEP);
            play = stepped.play();
            events.addAll(stepped.events());
        }
        return play;
    }

    private static Lift gunship(final Play play) {
        return play.lifts().stream().filter(l -> l.craft() == Course.GUNSHIP).findFirst().orElseThrow();
    }

    /** How high it is over the ground under its wheels. */
    private static double up(final Lift lift) {
        return lift.place().height() - Course.GUNSHIP.standing();
    }

    @Test
    void itIsParkedInThePlaygroundOnItsWheelsItsEngineOff() {
        final Play start = Play.start(false);
        final Lift parked = gunship(start);
        assertEquals(Course.Kind.AIRCRAFT, start.damage().piece(parked.piece()).kind());
        assertEquals(0, up(parked), 1e-9);
        assertFalse(parked.engine());
        assertFalse(parked.held());
        assertTrue(parked.clear(parked.place(), start.obstacles(), List.of()),
                "clear of everything about it");
    }

    @Test
    void boardedFromBesideItsDoorItIsStartedFlownBroughtDownAndSteppedOutOf() {
        final List<Event> events = new ArrayList<>();
        final Play start = Play.start(false);
        final Lift parked = gunship(start);
        final Vec3 door = parked.at(Course.GUNSHIP.door());
        Play play = new Play(new Avatar(Body.standing(door, 0), false), start.damage(), start.sky(), start.lifts(),
                start.people(), start.props(), start.turrets());
        assertTrue(play.withinReach().isPresent(), "within reach of its door");
        play = run(play, Controls.doing(Action.TAKE), Clock.STEP, events);
        assertTrue(gunship(play).held());
        assertTrue(play.hidden(), "inside it, out of sight");

        // started, then lifted off and climbing
        play = run(play, Controls.doing(Action.FLY), Clock.STEP, events);
        assertTrue(gunship(play).engine());
        assertTrue(events.contains(new Event.EngineStarted(parked.piece())));
        play = run(play, new Controls(0, 0, 0, 1, true), 3, events);
        final Lift aloft = gunship(play);
        assertTrue(up(aloft) > 1500, "well up: " + up(aloft));

        // ahead the way it heads, its nose down
        play = run(play, new Controls(1, 0, 0, 0, true), 1, events);
        assertTrue(gunship(play).tilt() < -5, "its nose down: " + gunship(play).tilt());
        play = run(play, new Controls(1, 0, 0, 0, true), 4, events);
        final Lift going = gunship(play);
        final Vec3 heading = Num.heading(aloft.place().yaw());
        final double gone = (going.place().x() - aloft.place().x()) * heading.x()
                + (going.place().z() - aloft.place().z()) * heading.z();
        assertTrue(gone > 3000, "well on its way: " + gone);
        assertTrue(going.steer() > 10, "its rotors tilting forward: " + going.steer());

        // the mouse turns it; A and D slide it aside, not turning it
        play = run(play, new Controls(0, 0, 0, 0, true), 8, events);
        final double yaw = gunship(play).place().yaw();
        play = run(play, new Controls(0, 0, 0.5, 0, true), 1, events);
        assertTrue(Num.wrap(gunship(play).place().yaw() - yaw) > 30, "turned right by the mouse");
        final double turned = gunship(play).place().yaw();
        play = run(play, new Controls(0, 1, 0, 0, true), 2, events);
        assertEquals(turned, gunship(play).place().yaw(), 1e-9, "D does not turn it");
        assertTrue(gunship(play).aside() > 300 && gunship(play).roll() > 5, "it slides and banks right");

        // brought down onto its wheels, and stepped out of, onto the ground beside its door
        play = run(play, new Controls(0, 0, 0, -1, true), 20, events);
        final Lift landed = gunship(play);
        assertEquals(0, up(landed), 1, "on its wheels");
        assertEquals(0, landed.speed(), 1);
        play = run(play, Controls.doing(Action.TAKE), Clock.STEP, events);
        assertFalse(gunship(play).held());
        assertFalse(play.hidden());
        final Vec3 out = gunship(play).at(Course.GUNSHIP.door());
        assertEquals(out.x(), play.avatar().body().position().x(), 1);
        assertEquals(out.z(), play.avatar().body().position().z(), 1);
    }
}
