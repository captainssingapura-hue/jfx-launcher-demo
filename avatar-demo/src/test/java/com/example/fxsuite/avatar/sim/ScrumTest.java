package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The stand-up on the meeting platform: a scrum master and five developers stood in a ring, each
 * facing its middle, the scrum master before the team's board at the back; room between them all,
 * and for whoever flies it at the console. Flown, it takes them and the board along.
 */
class ScrumTest {

    private static Lift meeting(final Play play) {
        return play.lifts().stream().filter(l -> Course.MEETING.equals(l.deck())).findFirst().orElseThrow();
    }

    private static List<Person> standUp(final Play play) {
        return play.people().stream()
                .filter(p -> p.role() == Person.Role.SCRUM_MASTER || p.role() == Person.Role.DEVELOPER).toList();
    }

    @Test
    void aScrumMasterAndFiveDevelopersStandInARingOnTheDeck() {
        final Play start = Play.start(false);
        final Lift deck = meeting(start);
        final List<Person> team = standUp(start);
        assertEquals(1, team.stream().filter(p -> p.role() == Person.Role.SCRUM_MASTER).count());
        assertEquals(5, team.stream().filter(p -> p.role() == Person.Role.DEVELOPER).count());
        // the ring's middle, in the world
        double x = 0;
        double z = 0;
        for (final Person person : team) {
            assertFalse(person.sitting(), "a stand-up");
            assertEquals(deck.place().height(), person.body().height(), 1e-9, "on the deck");
            x += person.body().position().x() / team.size();
            z += person.body().position().z() / team.size();
        }
        for (final Person person : team) {
            if (person.role() == Person.Role.DEVELOPER) {
                final Vec3 at = person.body().position();
                final double toward = Math.toDegrees(Math.atan2(x - at.x(), z - at.z()));
                assertTrue(Math.abs(Num.wrap(toward - person.body().yaw())) < 30, "facing in: " + person);
            }
        }
        // room between them, and for the pilot
        for (int a = 0; a < team.size(); a++) {
            for (int b = a + 1; b < team.size(); b++) {
                assertTrue(team.get(a).body().position().distance(team.get(b).body().position())
                        > 2 * Body.RADIUS + 20, "apart");
            }
            assertTrue(team.get(a).body().position().distance(deck.pilot()) > 2 * Body.RADIUS,
                    "clear of the console");
        }
        // and the board behind the scrum master, on the deck
        final Prop board = start.props().stream().filter(p -> p.thing() == Furniture.BOARD).findFirst().orElseThrow();
        assertEquals(deck.place().height(), board.place().height(), 1e-9);
        final Person master = team.stream().filter(p -> p.role() == Person.Role.SCRUM_MASTER).findFirst().orElseThrow();
        final Block standing = master.box();
        assertFalse(standing.overlaps(board.box()), "the scrum master clear of the board");
    }

    @Test
    void theyStayPutAndTheMeetingPlatformIsInThePlatformDemoToo() {
        Play play = Play.start(false);
        final List<Person> team = standUp(play);
        for (int step = 0; step < 240; step++) {
            play = play.step(Controls.NONE, Clock.STEP).play();
        }
        assertEquals(team.stream().map(p -> p.body().position()).toList(),
                standUp(play).stream().map(p -> p.body().position()).toList());
        final Play platform = Play.start(Demos.PLATFORM, false);
        assertEquals(6, standUp(platform).size());
        assertEquals(1, platform.props().stream().filter(p -> p.thing() == Furniture.BOARD).count());
    }

    @Test
    void flownItTakesAVehicleParkedOnItsDeckAlong() {
        // the carpet, no one at it, parked on the meeting platform's deck behind its console, its engine left running
        // as its driver left it: no one drives it, so it stands, and is carried as anything standing there is
        final Play start = Play.start(Demos.PLATFORM, false);
        final Lift deck = meeting(start);
        final int k = start.lifts().indexOf(deck);
        final int c = start.lifts().indexOf(start.lifts().stream().filter(l -> Course.CARPET.equals(l.deck()))
                .findFirst().orElseThrow());
        final Vec3 back = Num.heading(deck.place().yaw()).multiply(-300);
        final Lift parked = new Lift(start.lifts().get(c).piece(), start.lifts().get(c).craft(),
                new Place(deck.place().x() + back.x(), deck.place().z() + back.z(), deck.place().height() + Lift.THICK,
                        deck.place().yaw()), 0, 0, 0, false, true);
        final List<Lift> lifts = new java.util.ArrayList<>(start.lifts());
        lifts.set(c, parked);
        Play play = new Play(new Avatar(Body.standing(deck.pilot(), deck.place().yaw()), false), start.damage(),
                start.sky(), lifts, start.people(), start.props(), start.turrets());
        play = play.step(Controls.doing(Action.TAKE), Clock.STEP).play();
        assertTrue(play.lifts().get(k).held(), "the meeting platform's controls taken");
        assertFalse(play.lifts().get(c).held(), "no one at the carpet");
        for (int step = 0; step < 240; step++) {
            play = play.step(new Controls(0.5, 0, 0.3, 1, true, false, 0), Clock.STEP).play();
        }
        final Place now = play.lifts().get(k).place();
        assertTrue(now.height() > deck.place().height() + 300, "it climbed");
        final Vec3 expected = deck.place().carry(new Vec3(parked.place().x(), -parked.place().height(),
                parked.place().z()), now);
        final Place carpet = play.lifts().get(c).place();
        assertEquals(expected.x(), carpet.x(), 1e-6, "the carpet carried with it");
        assertEquals(expected.z(), carpet.z(), 1e-6);
        assertEquals(-expected.y(), carpet.height(), 1e-6, "still on its deck");
    }

    @Test
    void flownItTakesTheStandUpAndItsBoardAlong() {
        final Play start = Play.start(Demos.PLATFORM, false);
        final Lift deck = meeting(start);
        final int k = start.lifts().indexOf(deck);
        Play play = new Play(new Avatar(Body.standing(deck.pilot(), deck.place().yaw()), false), start.damage(),
                start.sky(), start.lifts(), start.people(), start.props(), start.turrets());
        play = play.step(Controls.doing(Action.TAKE), Clock.STEP).play();
        assertTrue(play.lifts().get(k).held(), "the controls taken");
        for (int step = 0; step < 240; step++) {
            play = play.step(new Controls(0, 0, 0.3, 1, true, false, 0), Clock.STEP).play();
        }
        final Place now = play.lifts().get(k).place();
        assertTrue(now.height() > deck.place().height() + 300, "it climbed");
        for (final Person person : standUp(play)) {
            assertEquals(now.height(), person.body().height(), 1e-6, "still on the deck: " + person.role());
        }
        final Prop board = play.props().stream().filter(p -> p.thing() == Furniture.BOARD).findFirst().orElseThrow();
        assertEquals(now.height(), board.place().height(), 1e-6, "and the board");
    }
}
