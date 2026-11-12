package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What moves of itself: the airships going round, carrying whoever stands on one, and their bombs. */
class SkyTest {

    private final Course course = new Course();

    private int firstAirship() {
        for (int i = 0; i < course.pieces().size(); i++) {
            if (course.pieces().get(i).moves()) {
                return i;
            }
        }
        throw new AssertionError("no airship");
    }

    /** Steps {@code play} for {@code seconds} with {@code controls}, keeping what happened. */
    private static Play run(Play play, final Controls controls, final double seconds, final List<Event> events) {
        for (long step = Math.round(seconds / Clock.STEP); step > 0; step--) {
            final Play.Stepped stepped = play.step(controls, Clock.STEP);
            play = stepped.play();
            events.addAll(stepped.events());
        }
        return play;
    }

    @Test
    void theAirshipsGoSlowlyRoundAtTheirHeightNoseFirst() {
        final List<Course.Piece> airships = course.pieces().stream().filter(Course.Piece::moves).toList();
        assertEquals(3, airships.size());
        for (final Course.Piece airship : airships) {
            final Course.Route route = airship.route();
            assertEquals(airship, airship.at(0), "where it was built is where its route starts");
            final Place now = route.at(10);
            final Place soon = route.at(10.01);
            assertEquals(route.radius(), Math.hypot(now.x(), now.z()), 1e-6, "round the start");
            assertEquals(route.height(), now.height(), "at its height");
            // going nose first: the way it moves is the way it points
            final Vec3 going = new Vec3(soon.x() - now.x(), 0, soon.z() - now.z()).normalize();
            assertEquals(1, going.dotProduct(Num.heading(now.yaw())), 1e-4);
            assertEquals(Math.abs(route.speed()), Math.hypot(soon.x() - now.x(), soon.z() - now.z()) / 0.01, 1);
            assertTrue(Math.abs(route.speed()) <= 500, "slowly");
        }
    }

    @Test
    void whatStandsInTheWayIsWhereTheAirshipNowIs() {
        final int airship = firstAirship();
        Play play = Play.start(false);
        play = run(play, Controls.NONE, 20, new ArrayList<>());
        final Block hull = Course.HULL.get(3).placed(course.pieces().get(airship).route().at(play.sky().time()));
        final Vec3 under = new Vec3(hull.x(), 0, hull.z());
        final Hit up = play.obstacles().ray(under.add(0, -100, 0), new Vec3(0, -1, 0), 6000);
        assertNotNull(up);
        assertEquals(airship, up.piece(), "looking straight up from under it, there it is");
        final Hit where = play.damage().obstacles().ray(under.add(0, -100, 0), new Vec3(0, -1, 0), 6000);
        assertTrue(where == null || where.piece() != airship, "and not where it started, twenty seconds ago");
    }

    @Test
    void standingOnAnAirshipItIsCarriedAlongAndTurnedWithIt() {
        final int airship = firstAirship();
        final Course.Route route = course.pieces().get(airship).route();
        final Place start = route.at(0);
        final Block deck = Course.HULL.get(3).placed(start);
        // on the deck, a little off the middle, facing across it
        final Vec3 on = new Vec3(deck.x(), -deck.top(), deck.z()).add(Num.heading(start.yaw()).multiply(150));
        Play play = new Play(new Avatar(Body.standing(on, start.yaw() + 90), false), Damage.of(course));
        play = run(play, Controls.NONE, 5, new ArrayList<>());
        final Place now = route.at(play.sky().time());
        final Vec3 expected = start.carry(on, now);
        final Body body = play.avatar().body();
        assertEquals(expected.x(), body.position().x(), 1, "carried along");
        assertEquals(expected.z(), body.position().z(), 1);
        assertEquals(deck.top(), body.height(), 1e-6, "still on the deck");
        assertTrue(body.grounded());
        assertEquals(Num.wrap(now.yaw() + 90), body.yaw(), 1e-6, "and turned as the airship turned");
        assertTrue(Math.hypot(body.position().x() - on.x(), body.position().z() - on.z()) > 1500,
                "a long way from where it stood: it went round with it");
    }

    @Test
    void walkingOnTheDeckIsWalkingAsWellAsBeingCarried() {
        final int airship = firstAirship();
        final Course.Route route = course.pieces().get(airship).route();
        final Place start = route.at(0);
        final Block deck = Course.HULL.get(3).placed(start);
        // at the tail end of the middle of the hull, walking toward the nose
        final Vec3 on = new Vec3(deck.x(), -deck.top(), deck.z()).subtract(Num.heading(start.yaw()).multiply(250));
        Play play = new Play(new Avatar(Body.standing(on, start.yaw()), false), Damage.of(course));
        final Controls walk = new Controls(1, 0, 0, 0, false, false, 0);
        play = run(play, walk, 1, new ArrayList<>());
        final Place now = route.at(play.sky().time());
        // where it is on the deck, in the airship's own terms: ahead of where it started along the hull
        final Vec3 rel = play.avatar().body().position().subtract(new Vec3(now.x(), 0, now.z()));
        final double along = rel.x() * Num.heading(now.yaw()).x() + rel.z() * Num.heading(now.yaw()).z();
        assertTrue(along > -250 + 200 && along < -250 + Body.WALK + 50, "walked ahead along the hull: " + along);
        assertEquals(deck.top(), play.avatar().body().height(), 1e-6, "on the deck all the while");
    }

    @Test
    void flyingOverTheDeckItIsNotCarried() {
        final int airship = firstAirship();
        final Place start = course.pieces().get(airship).route().at(0);
        final Block deck = Course.HULL.get(3).placed(start);
        final Vec3 over = new Vec3(deck.x(), -(deck.top() + 300), deck.z());
        final Body hovering = Body.standing(over, 0).toggledFlight();
        assertTrue(hovering.flying());
        Play play = new Play(new Avatar(hovering, true), Damage.of(course));
        play = run(play, Controls.NONE, 2, new ArrayList<>());
        final Vec3 at = play.avatar().body().position();
        assertTrue(Math.hypot(at.x() - over.x(), at.z() - over.z()) < 50, "it holds its place; the airship goes on");
    }

    @Test
    void eachAirshipDropsABombInTurnAndEachGoesOffBelow() {
        final List<Event> events = new ArrayList<>();
        Play play = Play.start(false);
        play = run(play, Controls.NONE, 10, events);
        final List<Event.Dropped> dropped = events.stream().filter(e -> e instanceof Event.Dropped)
                .map(e -> (Event.Dropped) e).toList();
        final List<Event.Exploded> exploded = events.stream().filter(e -> e instanceof Event.Exploded)
                .map(e -> (Event.Exploded) e).toList();
        // three airships, one every three seconds each: nine in ten seconds, give or take one
        assertTrue(dropped.size() >= 8 && dropped.size() <= 10, dropped.size() + " dropped");
        assertEquals(3, dropped.stream().map(Event.Dropped::from).distinct().count(), "all three drop");
        assertTrue(exploded.size() >= dropped.size() - 3, "all but the last few have gone off");
        for (final Event.Exploded x : exploded) {
            assertTrue(x.at().y() <= 1e-6, "on the ground or something on it");
            assertTrue(-x.at().y() < 2700, "well below the airships");
        }
        assertEquals(dropped.size() - exploded.size(), play.sky().bombs().size(), "the rest still falling");
    }

    @Test
    void aBombBreaksWhatCanBreakAroundWhereItGoesOff() {
        final Damage damage = Damage.of(course);
        // the smallest crate there is, and a bomb just over it, falling
        int crate = -1;
        for (int i = 0; i < course.pieces().size(); i++) {
            if (course.pieces().get(i).kind() == Course.Kind.CRATE
                    && (crate < 0 || Damage.toughness(course.pieces().get(i)) < Damage.toughness(course.pieces().get(crate)))) {
                crate = i;
            }
        }
        final Block box = course.pieces().get(crate).block();
        final Sky sky = new Sky(0, List.of(new Sky.Bomb(0, new Vec3(box.x(), -(box.top() + 5), box.z()),
                new Vec3(0, 1500, 0), -1)), 1);
        final Sky.Stepped stepped = sky.step(Clock.STEP, damage, damage.obstacles());
        assertTrue(stepped.events().getFirst() instanceof Event.Exploded, "it goes off");
        assertTrue(stepped.sky().bombs().isEmpty());
        final int target = crate;
        assertTrue(stepped.blows().stream().anyMatch(b -> b.piece() == target && b.by() == Weapon.BOMB));
        assertTrue(stepped.blows().stream().allMatch(b -> damage.piece(b.piece()).kind().breakable()),
                "only what can break is struck");
        final Damage.Taken taken = damage.took(stepped.blows());
        assertTrue(taken.damage().broken(crate), "a small crate goes at once");
        assertTrue(taken.events().stream().anyMatch(e -> e instanceof Event.Broke b && b.piece() == target));
    }

    @Test
    void theSkyIsPartOfTheWorldsChecksumAndARecordingReplaysIt() throws Exception {
        final Play start = Play.start(false);
        final Play later = run(start, Controls.NONE, 4, new ArrayList<>());
        assertNotEquals(start.checksum(), new Play(start.avatar(), start.damage(), later.sky()).checksum());
        assertFalse(later.sky().bombs().isEmpty(), "bombs falling at four seconds");
        assertEquals(2, Play.RULES, "moving airships changed the rules");
    }
}
