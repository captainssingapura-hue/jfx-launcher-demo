package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Blow;
import com.example.fxsuite.avatar.sim.Body;
import com.example.fxsuite.avatar.sim.Clock;
import com.example.fxsuite.avatar.sim.Controls;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Damage;
import com.example.fxsuite.avatar.sim.Demo;
import com.example.fxsuite.avatar.sim.Event;
import com.example.fxsuite.avatar.sim.Place;
import com.example.fxsuite.avatar.sim.Play;
import com.example.fxsuite.avatar.sim.Recording;
import com.example.fxsuite.avatar.sim.Siege;
import com.example.fxsuite.avatar.sim.Surface;
import com.example.fxsuite.avatar.sim.Vec3;
import com.example.fxsuite.avatar.sim.Weapon;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The temple under siege, in the town: its airships waiting until they come, coming one after another
 * from the edge of the world into a free slot, high over everything; circling over the temple's hall,
 * bombing it until it falls; and what comes of that. Shot down, a slot is free again for the next.
 */
class SiegeTest {

    /** A quick siege: four at a time, one every 3 s, fast, bombing four times a second, the temple a tenth as tough. */
    private static final Siege QUICK = new Siege(4, 3, 0, 30, 1, 0.25, 0, 0.1, 1, Siege.Fallen.LOST, 10, 42);

    @BeforeAll
    static void town() {
        Course.town(Town::pieces, Town::things, Town::people);
    }

    /** {@code play}, {@code seconds} on, unattended; each step's events into {@code events}. */
    private static Play run(Play play, final double seconds, final List<Event> events) {
        for (int k = 0; k < Math.round(seconds / Clock.STEP); k++) {
            final Play.Stepped stepped = play.step(Controls.NONE, Clock.STEP);
            events.addAll(stepped.events());
            play = stepped.play();
        }
        return play;
    }

    @Test
    void itsSettingsAreCheckedAndWrittenAndReadBack() {
        assertEquals(QUICK, Siege.read(QUICK.written()));
        assertEquals(Siege.STANDARD, Siege.read(Siege.STANDARD.written()));
        assertThrows(IllegalArgumentException.class, () -> new Siege(0, 20, 0, 8, 2, 1, 0.25, 1, 1, Siege.Fallen.LOST,
                60, 1), "no airships");
        assertThrows(IllegalArgumentException.class, () -> new Siege(Siege.MOST_SHIPS + 1, 20, 0, 8, 2, 1, 0.25, 1, 1,
                Siege.Fallen.LOST, 60, 1), "too many");
        assertThrows(IllegalArgumentException.class, () -> new Siege(6, 20, 0, 8, 2, 1, 1.5, 1, 1, Siege.Fallen.LOST, 60,
                1), "a chance over one");
    }

    @Test
    void itsAirshipsWaitThenComeFromTheEdgeHighOverEverythingIntoFreeSlots() {
        final Play start = Play.start(Demo.SIEGE, false, QUICK);
        final Siege.State siege = start.sky().siege();
        assertEquals(QUICK.ships(), siege.raiders().size());
        for (final Siege.Raider raider : siege.raiders()) {
            assertTrue(start.damage().broken(raider.piece()), "waiting, out of the world");
            assertEquals(Course.Kind.AIRSHIP, start.damage().piece(raider.piece()).kind());
        }
        assertEquals(Course.MAIN_HALL, start.damage().piece(siege.hall()).tint(), "the temple's main hall");
        assertEquals(Damage.BUILDING * QUICK.templeToughness(), start.damage().health(siege.hall()), 1e-6);
        // none before the first is due; then one at a time, as many as there are slots
        final List<Event> events = new ArrayList<>();
        Play play = run(start, Siege.FIRST - 0.5, events);
        assertEquals(0, play.sky().siege().flying());
        play = run(play, 1, events);
        assertEquals(1, play.sky().siege().flying());
        play = run(play, 3 * QUICK.every(), events);
        assertEquals(QUICK.ships(), play.sky().siege().flying(), "every slot taken");
        final List<Event.Arrived> arrived = events.stream().filter(Event.Arrived.class::isInstance)
                .map(Event.Arrived.class::cast).toList();
        assertEquals(QUICK.ships(), arrived.size());
        for (final Event.Arrived came : arrived) {
            assertEquals(Siege.ENTRY, Math.max(Math.abs(came.at().x()), Math.abs(came.at().z())), 1, "on the edge");
            assertTrue(-came.at().y() > siege.tallest() + Siege.CLEAR, "over the tallest building");
            assertFalse(play.damage().broken(came.piece()), "in the world");
            assertEquals(Damage.AIRSHIP * QUICK.shipToughness(), play.damage().health(came.piece()), 1e-6, "whole");
        }
        // each from somewhere of its own
        assertEquals(arrived.size(), arrived.stream().map(a -> Math.round(a.at().x() / 100) + "," + Math.round(a.at().z() / 100))
                .distinct().count());
    }

    @Test
    void theSameSeedTheSameSiegeAnotherAnother() {
        final List<Event> events = new ArrayList<>();
        final long once = run(Play.start(Demo.SIEGE, false, QUICK), 20, events).checksum();
        final long again = run(Play.start(Demo.SIEGE, false, QUICK), 20, events).checksum();
        final long other = run(Play.start(Demo.SIEGE, false, QUICK.seeded(43)), 20, events).checksum();
        assertEquals(once, again);
        assertNotEquals(once, other);
    }

    @Test
    void undefendedTheyCircleOverTheHallAndBombItUntilItFallsAndTheSiegeIsLost() {
        final List<Event> events = new ArrayList<>();
        Play play = Play.start(Demo.SIEGE, false, QUICK);
        final int hall = play.sky().siege().hall();
        final Course.Piece target = play.damage().piece(hall);
        double fell = Double.NaN;
        for (int k = 0; k < 240 && Double.isNaN(fell); k++) {
            play = run(play, 1, events);
            if (play.sky().siege().lost()) {
                fell = play.sky().time();
            }
        }
        assertFalse(Double.isNaN(fell), "the hall fell");
        assertTrue(play.damage().broken(hall));
        assertEquals(1, events.stream().filter(Event.Besieged.class::isInstance).count(), "lost once");
        // its bombs fell on the hall, from over it
        assertTrue(events.stream().anyMatch(e -> e instanceof Event.Struck s && s.piece() == hall && s.by() == Weapon.BOMB));
        // those come so far, circling over it; the rest still on their way
        final Play then = play;
        assertTrue(play.sky().siege().raiders().stream().map(r -> then.damage().piece(r.piece()).at(then.sky().time()).place())
                .anyMatch(at -> Math.hypot(at.x() - target.block().x(), at.z() - target.block().z()) <= Siege.ORBIT_MOST + 1),
                "circling over the hall");
        // and lost: none come, none bomb
        final long before = play.sky().siege().arrivals();
        final List<Event> after = new ArrayList<>();
        play = run(play, 10, after);
        assertEquals(before, play.sky().siege().arrivals());
        assertTrue(after.stream().noneMatch(Event.Dropped.class::isInstance), "no more bombs");
    }

    @Test
    void shotDownItsSlotIsFreeForTheNextOnceItsWreckHasFallen() {
        final List<Event> events = new ArrayList<>();
        Play play = run(Play.start(Demo.SIEGE, false, QUICK), Siege.FIRST + 4 * QUICK.every(), events);
        final Siege.Raider first = play.sky().siege().raiders().getFirst();
        assertTrue(first.flying());
        // shot down
        final List<Blow> blows = new ArrayList<>();
        for (int k = 0; k < 1000 && !play.damage().broken(first.piece()); k++) {
            blows.add(new Blow(Vec3.ZERO, new Vec3(0, 1, 0), Surface.BLOCK, Weapon.GATLING, first.piece()));
        }
        final Damage hit = play.damage().took(blows, play.sky().time()).damage();
        assertTrue(hit.broken(first.piece()));
        play = new Play(play.avatar(), hit, play.sky(), play.lifts(), play.people(), play.props(), play.turrets());
        play = run(play, 1, events);
        assertEquals(1, play.sky().siege().downed());
        assertFalse(play.sky().siege().raiders().getFirst().flying());
        // the next, into it, once its wreck has fallen: whole again
        play = run(play, Siege.SETTLE + QUICK.every(), events);
        final Siege.Raider again = play.sky().siege().raiders().getFirst();
        assertTrue(again.flying(), "the next come into its slot");
        assertTrue(again.arrival() >= QUICK.ships(), "another arrival");
        assertEquals(1, play.damage().left(again.piece()), 1e-9, "whole");
    }

    @Test
    void rebuiltTheHallStandsAgainAndTheyBombItAgain() {
        final Siege rebuilt = new Siege(4, 3, 0, 30, 1, 0.25, 0, 0.1, 1, Siege.Fallen.REBUILT, 5, 42);
        final List<Event> events = new ArrayList<>();
        Play play = Play.start(Demo.SIEGE, false, rebuilt);
        final int hall = play.sky().siege().hall();
        for (int k = 0; k < 240 && !play.damage().broken(hall); k++) {
            play = run(play, 1, events);
        }
        assertTrue(play.damage().broken(hall), "fallen");
        play = run(play, rebuilt.rebuildAfter() + 1, events);
        assertFalse(play.sky().siege().lost());
        assertTrue(play.damage().left(hall) > 0.5, "rebuilt");
    }

    @Test
    void nextTheyGoForTheBuildingNearestTheHall() {
        final Siege next = new Siege(4, 3, 0, 30, 1, 0.25, 0, 0.1, 1, Siege.Fallen.NEXT, 5, 42);
        final List<Event> events = new ArrayList<>();
        Play play = Play.start(Demo.SIEGE, false, next);
        final int hall = play.sky().siege().hall();
        for (int k = 0; k < 240 && !play.damage().broken(hall); k++) {
            play = run(play, 1, events);
        }
        play = run(play, 1, events);
        final int target = play.sky().siege().target();
        assertNotEquals(hall, target);
        assertFalse(Course.temple(play.damage().piece(target)), "not the temple's");
        assertEquals(Course.Kind.BUILDING, play.damage().piece(target).kind());
        // each in the air sent there
        play = run(play, 60, events);
        final Course.Piece aim = play.damage().piece(target);
        for (final Siege.Raider raider : play.sky().siege().raiders()) {
            if (raider.flying()) {
                final Place at = play.damage().piece(raider.piece()).at(play.sky().time()).place();
                assertTrue(Math.hypot(at.x() - aim.block().x(), at.z() - aim.block().z()) <= Siege.ORBIT_MOST + 1,
                        "circling over its next target");
            }
        }
    }

    @Test
    void aBombHarmsABuildingShapedMoreCloselyThanABox() {
        // the hall is in the way as it is shaped: a bomb going off on its roof harms it
        Play play = Play.start(Demo.SIEGE, false, QUICK);
        final int hall = play.sky().siege().hall();
        final Course.Piece target = play.damage().piece(hall);
        play = new Play(play.avatar(), play.damage(), play.sky().dropped(new Vec3(target.block().x(),
                -(target.block().top() + 500), target.block().z()), Vec3.ZERO, -1), play.lifts(), play.people(),
                play.props(), play.turrets());
        play = run(play, 2, new ArrayList<>());
        assertTrue(play.damage().left(hall) < 1, "struck");
    }

    @Test
    void aRecordingKeepsHowItsSiegeWasSet() throws IOException {
        final StringWriter out = new StringWriter();
        try (Recording.Recorder recorder = new Recording.Recorder(out, false, Demo.SIEGE, QUICK)) {
            recorder.step(1, Controls.NONE, Play.start(Demo.SIEGE, false, QUICK));
        }
        final Recording read = Recording.read(new StringReader(out.toString()));
        assertEquals(Demo.SIEGE, read.demo());
        assertEquals(QUICK, read.siege());
        assertEquals(Play.start(Demo.SIEGE, false, QUICK).checksum(), read.start().checksum());
        assertTrue(Body.EDGE > Siege.ENTRY, "they come from within the world");
    }
}
