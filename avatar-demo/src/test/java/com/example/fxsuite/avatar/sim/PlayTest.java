package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A step in its phases: the avatar acts and deals blows; the world takes them; what came of it goes out. */
class PlayTest {

    private final Course course = new Course();

    private int middleDummy() {
        final List<Course.Piece> pieces = course.pieces();
        for (int i = 0; i < pieces.size(); i++) {
            if (pieces.get(i).kind() == Course.Kind.DUMMY) {
                return i + 2;
            }
        }
        throw new AssertionError("no dummy");
    }

    @Test
    void theAvatarsOwnEventsComeFirstThenWhatCameOfItsBlows() {
        Play play = new Play(new Avatar(Body.standing(Vec3.ZERO, Course.RANGE_HEADING), false), Damage.of(course));
        // drawn and fired in the same step, at the range: a round goes out, and strikes
        final Controls drawAndFire = new Controls(0, 0, 0, 0, false, true, 0,
                List.of(new Action.Draw(Weapon.RIFLES)));
        final Play.Stepped first = play.step(drawAndFire, Clock.STEP);
        final List<Event> events = first.events();
        assertEquals(new Event.Drew(Weapon.RIFLES), events.getFirst(), "the action, first of all");
        final int fired = indexOf(events, Event.Fired.class);
        final int struck = indexOf(events, Event.Struck.class);
        assertTrue(fired >= 0 && struck > fired, "fired, then struck: " + events);
    }

    @Test
    void eachRoundOnADummyComesOutWithWhatTheDummyHasLeft() {
        final int dummy = middleDummy();
        Play play = new Play(new Avatar(Body.standing(Vec3.ZERO, Course.RANGE_HEADING), false)
                .drawn(Weapon.RIFLES), Damage.of(course));
        final Controls fire = new Controls(0, 0, 0, 0, false, true, 0);
        final List<Event.Struck> onIt = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            final Play.Stepped stepped = play.step(fire, Clock.STEP);
            play = stepped.play();
            stepped.events().stream().filter(e -> e instanceof Event.Struck s && s.piece() == dummy)
                    .map(e -> (Event.Struck) e).forEach(onIt::add);
        }
        assertTrue(onIt.size() > 3);
        for (int i = 0; i < onIt.size(); i++) {
            assertEquals(1 - (i + 1) * Weapon.RIFLES.blow() / Damage.DUMMY, onIt.get(i).left(), 1e-9,
                    "what it has left after round " + (i + 1));
        }
        assertEquals(onIt.getLast().left(), play.damage().left(dummy), 1e-12, "and so the world stands");
    }

    private static int indexOf(final List<Event> events, final Class<? extends Event> kind) {
        for (int i = 0; i < events.size(); i++) {
            if (kind.isInstance(events.get(i))) {
                return i;
            }
        }
        return -1;
    }
}
