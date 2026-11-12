package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The timeline of what happened, read by each reader at its own pace. */
class JournalTest {

    private static final Event JUMPED = new Event.Jumped();
    private static final Event LANDED = new Event.Landed(500, false, Surface.GROUND);

    @Test
    void entriesAreStampedWithTheirStepAndTheirOrderInIt() {
        final Journal journal = new Journal();
        final Journal.Reader reader = journal.reader();
        journal.append(1, List.of(JUMPED, LANDED));
        journal.append(2, List.of(JUMPED));
        final Journal.Read read = reader.read();
        assertEquals(List.of(new Journal.Logged(1, 0, JUMPED), new Journal.Logged(1, 1, LANDED),
                new Journal.Logged(2, 0, JUMPED)), read.entries());
        assertEquals(List.of(JUMPED, LANDED, JUMPED), read.events());
        assertEquals(0, read.missed());
        assertTrue(reader.read().entries().isEmpty(), "nothing new since");
    }

    @Test
    void eachReaderKeepsItsOwnPlace() {
        final Journal journal = new Journal();
        final Journal.Reader eager = journal.reader();
        final Journal.Reader lazy = journal.reader();
        final List<Event> eagerGot = new ArrayList<>();
        for (long step = 1; step <= 30; step++) {
            journal.append(step, List.of(new Event.Footfall(step % 2 == 0, Surface.GROUND)));
            eagerGot.addAll(eager.read().events());
        }
        // the lazy one reads once, at the end, and gets the same, in the same order
        final Journal.Read late = lazy.read();
        assertEquals(eagerGot, late.events());
        assertEquals(0, late.missed());
        // a reader made now sees only what comes next
        final Journal.Reader newcomer = journal.reader();
        journal.append(31, List.of(JUMPED));
        assertEquals(List.of(JUMPED), newcomer.read().events());
        assertEquals(List.of(JUMPED), eager.read().events(), "and reading moved no one else");
    }

    @Test
    void itKeepsAWindowAndAReaderFarBehindIsToldWhatItMissed() {
        final Journal journal = new Journal(10);
        final Journal.Reader behind = journal.reader();
        for (long step = 1; step <= 25; step++) {
            journal.append(step, List.of(JUMPED));
        }
        final Journal.Read read = behind.read();
        assertEquals(10, read.entries().size(), "the last ten steps are kept");
        assertEquals(16, read.entries().getFirst().step());
        assertEquals(15, read.missed(), "and it is told it missed the first fifteen");
        // the oldest still kept, for a reader that wants all there is
        assertEquals(10, journal.readerFromOldest().read().entries().size());
    }

    @Test
    void stepsOnlyGoForwardAndALaterAddToAStepCarriesOnItsOrder() {
        final Journal journal = new Journal();
        final Journal.Reader reader = journal.reader();
        journal.append(5, List.of(JUMPED));
        journal.append(5, List.of(LANDED));
        assertEquals(List.of(new Journal.Logged(5, 0, JUMPED), new Journal.Logged(5, 1, LANDED)),
                reader.read().entries());
        assertThrows(IllegalArgumentException.class, () -> journal.append(4, List.of(JUMPED)));
        journal.append(6, List.of());
        assertEquals(6, journal.latest(), "a step with nothing in it still moves the timeline on");
    }

    @Test
    void theWorldsStepsWriteToItAndTheSoundAndEffectsReadTheSame() {
        // a few seconds at the range, each step written to the journal, read by two readers apart
        final Journal journal = new Journal();
        final Journal.Reader everyStep = journal.reader();
        final Journal.Reader everyTenth = journal.reader();
        Play play = new Play(new Avatar(Body.standing(Vec3.ZERO, Course.RANGE_HEADING), false)
                .drawn(Weapon.RIFLES), Damage.of(new Course()));
        final Controls fire = new Controls(0, 0, 0, 0, false, true, 0);
        final List<Event> direct = new ArrayList<>();
        final List<Event> a = new ArrayList<>();
        final List<Event> b = new ArrayList<>();
        for (long step = 1; step <= 120; step++) {
            final Play.Stepped stepped = play.step(fire, Clock.STEP);
            play = stepped.play();
            direct.addAll(stepped.events());
            journal.append(step, stepped.events());
            a.addAll(everyStep.read().events());
            if (step % 10 == 0) {
                b.addAll(everyTenth.read().events());
            }
        }
        assertTrue(direct.stream().anyMatch(e -> e instanceof Event.Struck));
        assertEquals(direct, a);
        assertEquals(direct, b);
    }
}
