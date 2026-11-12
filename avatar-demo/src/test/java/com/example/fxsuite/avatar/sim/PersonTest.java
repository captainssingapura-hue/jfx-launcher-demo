package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The people: riding the barge, pushed about, off its edge, and knocked down. */
class PersonTest {

    private static Play run(Play play, final Controls controls, final double seconds, final List<Event> events) {
        for (long step = Math.round(seconds / Clock.STEP); step > 0; step--) {
            final Play.Stepped stepped = play.step(controls, Clock.STEP);
            play = stepped.play();
            events.addAll(stepped.events());
        }
        return play;
    }

    private static Person customer(final Play play) {
        return play.people().stream().filter(p -> p.role() == Person.Role.CUSTOMER).findFirst().orElseThrow();
    }

    /**
     * The start, with the avatar standing {@code back} to the customer's left on the barge's deck,
     * facing them: walking on pushes them across the deck toward its right-hand edge.
     */
    private static Play behindCustomer(final double back, final Avatar avatar) {
        final Play start = Play.start(false);
        final Lift barge = start.lifts().get(1);
        final Vec3 at = customer(start).body().position()
                .add(Num.right(barge.place().yaw()).multiply(-back));
        final Body body = Body.standing(at, Num.wrap(barge.place().yaw() + 90));
        return new Play(new Avatar(body, avatar.wearingPack(), avatar.sweep(), avatar.arms()), start.damage(),
                start.sky(), start.lifts(), start.people());
    }

    @Test
    void theCastStandsOnTheBargeTheStallholderAndTheRiderSitting() {
        final Play start = Play.start(false);
        assertEquals(3, start.people().size());
        final double deck = start.lifts().get(1).place().height();
        for (final Person person : start.people()) {
            assertEquals(deck, person.body().height(), 1e-9, person.role() + " on the deck");
            assertEquals(Person.TOUGHNESS, person.health());
            assertFalse(person.down());
        }
        assertEquals(List.of(true, false, true), start.people().stream().map(Person::sitting).toList());
        // and they stay put, standing still, as the world goes on
        final Play later = run(start, Controls.NONE, 2, new ArrayList<>());
        for (int k = 0; k < 3; k++) {
            assertEquals(start.people().get(k).body().position(), later.people().get(k).body().position());
        }
    }

    @Test
    void theyRideTheBargeWhereverItIsFlown() {
        final Play start = Play.start(false);
        final Lift barge = start.lifts().get(1);
        Play play = new Play(new Avatar(Body.standing(barge.pilot(), barge.place().yaw()), false), start.damage(),
                start.sky(), start.lifts(), start.people());
        final List<Event> events = new ArrayList<>();
        play = run(play, Controls.doing(Action.TAKE), Clock.STEP, events);
        play = run(play, new Controls(0, 0, 0.3, 1, true, false, 0), 2, events);
        final Place now = play.lifts().get(1).place();
        assertTrue(now.height() > barge.place().height() + 300);
        for (int k = 0; k < 3; k++) {
            final Vec3 expected = barge.place().carry(start.people().get(k).body().position(), now);
            final Vec3 is = play.people().get(k).body().position();
            assertEquals(expected.x(), is.x(), 1e-6, "person " + k + " carried");
            assertEquals(expected.z(), is.z(), 1e-6);
            assertEquals(expected.y(), is.y(), 1e-6);
        }
    }

    @Test
    void walkingIntoSomeoneShovesThem() {
        Play play = behindCustomer(120, Play.start(false).avatar());
        final int who = customer(play).id();
        final Vec3 was = customer(play).body().position();
        final List<Event> events = new ArrayList<>();
        play = run(play, new Controls(1, 0, 0, 0, false, false, 0), 0.8, events);
        final Vec3 is = customer(play).body().position();
        final Vec3 walked = Num.heading(play.avatar().body().yaw());
        assertTrue(is.subtract(was).dotProduct(walked) > 60, "pushed along the way the avatar walked: "
                + is.subtract(was).dotProduct(walked));
        assertTrue(events.stream().anyMatch(e -> e instanceof Event.Shoved s && s.person() == who));
        final double apart = Math.hypot(is.x() - play.avatar().body().position().x(),
                is.z() - play.avatar().body().position().z());
        assertTrue(apart >= 2 * Body.RADIUS - 1e-6, "and never inside the avatar");
    }

    @Test
    void shovedOverTheEdgeTheyFallOff() {
        // the customer stands some 4.5 m from the barge's right-hand edge; run them over it
        Play play = behindCustomer(150, Play.start(false).avatar());
        final List<Event> events = new ArrayList<>();
        play = run(play, new Controls(1, 0, 0, 0, true, false, 0), 1.6, events);
        play = run(play, Controls.NONE, 1.5, events);
        final Person fell = customer(play);
        assertEquals(0, fell.body().height(), 1e-9, "down on the ground, off the barge");
        assertTrue(fell.body().grounded());
        assertFalse(fell.down(), "fallen off, but not knocked down");
    }

    @Test
    void shotTheyFlinchAndAtTheLastAreDestroyed() {
        final Avatar armed = Play.start(false).avatar().drawn(Weapon.RIFLES);
        Play play = behindCustomer(300, armed);
        final int piece = customer(play).piece();
        final Vec3 stood = customer(play).body().position();
        final List<Event> events = new ArrayList<>();
        play = run(play, new Controls(0, 0, 0, 0, false, true, 0), 1, events);
        final Person shot = customer(play);
        assertTrue(shot.down(), "destroyed");
        assertEquals(0, shot.health());
        assertTrue(events.stream().filter(e -> e instanceof Event.Struck s && s.piece() == piece).count() >= 6,
                "struck six rounds or more");
        assertEquals(1, events.stream().filter(e -> e instanceof Event.Destroyed).count(), "and destroyed once");
        final Vec3 away = shot.body().position().subtract(stood);
        assertTrue(away.dotProduct(Num.heading(play.avatar().body().yaw())) > 1, "knocked back, away from the shots");
        assertTrue(Person.is(piece) && Person.which(piece) == shot.id());
    }

    @Test
    void aBombNearbyDestroysThem() {
        final Play start = Play.start(false);
        final Person customer = customer(start);
        final Vec3 above = customer.body().position().add(60, -260, 0);
        final Sky sky = new Sky(0, List.of(new Sky.Bomb(0, above, new Vec3(0, 1500, 0), -1)), 1);
        Play play = new Play(start.avatar(), start.damage(), sky, start.lifts(), start.people());
        final List<Event> events = new ArrayList<>();
        play = run(play, Controls.NONE, 0.5, events);
        assertTrue(events.stream().anyMatch(e -> e instanceof Event.Exploded), "it went off on the deck");
        assertTrue(customer(play).down(), "and the customer is down");
        assertTrue(events.stream().anyMatch(e -> e instanceof Event.Destroyed d && d.person() == customer.id()));
    }

    @Test
    void destroyedTheyAreGoneNotToBePushedNorStruck() {
        final Play start = Play.start(false);
        final List<Person> people = new ArrayList<>(start.people());
        final Person customer = customer(start);
        people.set(customer.id(), customer.struck(Person.TOUGHNESS));
        Play play = behindCustomer(120, start.avatar().drawn(Weapon.RIFLES));
        play = new Play(play.avatar(), play.damage(), play.sky(), play.lifts(), people);
        assertTrue(customer(play).down());
        final Vec3 was = customer(play).body().position();
        final List<Event> events = new ArrayList<>();
        play = run(play, new Controls(0, 0, 0, 0, false, true, 0), 0.5, events);
        assertTrue(events.stream().noneMatch(e -> e instanceof Event.Struck s && s.piece() == customer.piece()),
                "the rounds go through where they were");
        play = run(play, new Controls(1, 0, 0, 0, false, false, 0), 0.8, events);
        assertEquals(was, customer(play).body().position(), "and walking there moves nothing");
        assertTrue(events.stream().noneMatch(e -> e instanceof Event.Shoved));
    }

    /** Someone standing in the open, {@code high} up, over nothing, near the start. */
    private static Play dropping(final double high) {
        final Play start = Play.start(false);
        final List<Person> people = new ArrayList<>(start.people());
        final Person customer = customer(start);
        people.set(customer.id(), new Person(customer.id(), customer.role(),
                Body.standing(new Vec3(300, -high, 300), 0), Person.TOUGHNESS, false));
        return new Play(start.avatar(), start.damage(), start.sky(), start.lifts(), people);
    }

    @Test
    void aFallOfAMetreOrSoDoesNoHarm() {
        final List<Event> events = new ArrayList<>();
        final Play play = run(dropping(150), Controls.NONE, 1.5, events);
        assertEquals(0, customer(play).body().height(), 1e-9, "down on the ground");
        assertEquals(Person.TOUGHNESS, customer(play).health(), "unhurt");
        assertTrue(events.stream().noneMatch(e -> e instanceof Event.Fell));
    }

    @Test
    void aHarderFallHurtsTheHarderItIs() {
        final List<Event> events = new ArrayList<>();
        final double fourMetres = customer(run(dropping(400), Controls.NONE, 2, events)).health();
        final double sixMetres = customer(run(dropping(600), Controls.NONE, 2, events)).health();
        assertTrue(fourMetres < Person.TOUGHNESS && fourMetres > 0, "hurt by four metres: " + fourMetres);
        assertTrue(sixMetres < fourMetres && sixMetres > 0, "and more by six: " + sixMetres);
        assertTrue(events.stream().anyMatch(e -> e instanceof Event.Fell f && f.speed() > Person.SAFE_LANDING));
    }

    @Test
    void aLongFallIsTheEndOfThem() {
        final List<Event> events = new ArrayList<>();
        final Play play = run(dropping(1000), Controls.NONE, 2, events);
        assertTrue(customer(play).down(), "destroyed by ten metres");
        assertEquals(1, events.stream().filter(e -> e instanceof Event.Destroyed).count());
    }
}
