package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A platform's engine: cut, it comes down and rests on what is under it, and docked on what moves, goes with it. */
class EngineTest {

    private static final Controls TAKE = Controls.doing(Action.TAKE);
    private static final Controls ENGINE = Controls.doing(Action.FLY);
    private static final Controls UP = new Controls(0, 0, 0, 1, true, false, 0);
    private static final Controls AHEAD = new Controls(1, 0, 0, 0, true, false, 0);

    private final Course course = new Course();

    private static Play run(Play play, final Controls controls, final double seconds, final List<Event> events) {
        for (long step = Math.round(seconds / Clock.STEP); step > 0; step--) {
            final Play.Stepped stepped = play.step(controls, Clock.STEP);
            play = stepped.play();
            events.addAll(stepped.events());
        }
        return play;
    }

    private static Play once(final Play play, final Controls controls, final List<Event> events) {
        final Play.Stepped stepped = play.step(controls, Clock.STEP);
        events.addAll(stepped.events());
        return stepped.play();
    }

    /** The start, the small platform put as {@code lift}, with the avatar at its controls, taken. */
    private static Play at(final Lift lift, final List<Event> events) {
        final Play start = Play.start(false);
        final List<Lift> lifts = new ArrayList<>(start.lifts());
        lifts.set(0, lift);
        final Play play = new Play(new Avatar(Body.standing(lift.pilot(), lift.place().yaw()), false), start.damage(),
                start.sky(), lifts);
        return once(play, TAKE, events);
    }

    private static Lift carpet(final Play play) {
        return play.lifts().getFirst();
    }

    @Test
    void cutItDropsToTheGroundWithThePilotAndWillNotFlyUntilStartedAgain() {
        final List<Event> events = new ArrayList<>();
        Play play = at(Play.start(false).lifts().getFirst(), events);
        play = run(play, UP, 2, events);
        assertTrue(carpet(play).place().height() > 400);
        events.clear();
        play = once(play, ENGINE, events);
        assertEquals(List.of(new Event.EngineCut(carpet(play).piece())), events);
        assertFalse(carpet(play).engine());
        play = run(play, Controls.NONE, 3, events);
        assertEquals(Lift.THICK, carpet(play).place().height(), 1e-9, "down on the ground, its underside on it");
        assertTrue(carpet(play).resting(play.under(carpet(play))));
        assertEquals(Lift.THICK, play.avatar().body().height(), 1e-9, "the pilot down with it");
        assertTrue(events.stream().anyMatch(e -> e instanceof Event.Settled s && s.speed() > 500), "with a bump");
        assertTrue(play.atControls().isPresent(), "still at the controls");
        final Place resting = carpet(play).place();
        play = run(play, AHEAD, 1, events);
        assertEquals(resting, carpet(play).place(), "its engine cut, it does not fly");
        events.clear();
        play = once(play, ENGINE, events);
        assertEquals(List.of(new Event.EngineStarted(carpet(play).piece())), events);
        play = run(play, UP, 1.5, events);
        assertTrue(carpet(play).place().height() > Course.LIFT_LOW + 200, "started, it lifts off again");
    }

    @Test
    void itSettlesOnTopOfWhatIsUnderIt() {
        final Block pillar = course.pieces().stream().filter(p -> p.kind() == Course.Kind.PILLAR
                && p.block().top() < 2000 && p.block().halfWidth() > 100).findFirst().orElseThrow().block();
        final Lift over = new Lift(Play.start(false).lifts().getFirst().piece(), Course.CARPET,
                new Place(pillar.x(), pillar.z(), pillar.top() + 300, 0), 0, 0, 0, false, false);
        final List<Event> events = new ArrayList<>();
        final Play play = run(at(over, events), Controls.NONE, 2, events);
        assertEquals(pillar.top() + Lift.THICK, carpet(play).place().height(), 1e-9, "on the pillar's top");
        assertEquals(carpet(play).place().height(), play.avatar().body().height(), 1e-9);
    }

    @Test
    void itWillNotComeDownOnTheAvatarStandingUnderIt() {
        final Play start = Play.start(false);
        final Lift above = new Lift(start.lifts().getFirst().piece(), Course.CARPET, new Place(0, 0, 600, 0), 0, 0, 0,
                false, false);
        final List<Lift> lifts = new ArrayList<>(start.lifts());
        lifts.set(0, above);
        final Play play = run(new Play(start.avatar(), start.damage(), start.sky(), lifts), Controls.NONE, 2,
                new ArrayList<>());
        assertEquals(Body.TALL + Lift.THICK, carpet(play).place().height(), 1e-9, "on the avatar's head, not through it");
    }

    @Test
    void dockedOnAnAirshipItGoesRoundWithItAndThePilotToo() {
        final int ship = Play.start(false).airships().isEmpty() ? -1 : Play.start(false).damage().flying().getFirst();
        final Course.Route route = course.pieces().get(ship).route();
        final Block hull = Course.HULL.get(3).placed(route.at(0));
        // a little over the middle of its hull, the engine already cut
        final Lift over = new Lift(Play.start(false).lifts().getFirst().piece(), Course.CARPET,
                new Place(hull.x(), hull.z(), hull.top() + Lift.THICK + 60, route.at(0).yaw()), 0, 0, 0, false, false);
        final List<Event> events = new ArrayList<>();
        Play play = run(at(over, events), Controls.NONE, 1, events);
        final Lift landed = carpet(play);
        assertTrue(landed.resting(play.under(landed)), "down on it");
        assertEquals(ship, play.under(landed).piece(), "on the airship");
        final double then = play.sky().time();
        final Vec3 stood = play.avatar().body().position();
        play = run(play, Controls.NONE, 6, events);
        final Place a = route.at(then);
        final Place b = route.at(play.sky().time());
        final Vec3 expected = a.carry(new Vec3(landed.place().x(), -landed.place().height(), landed.place().z()), b);
        final Place now = carpet(play).place();
        assertEquals(expected.x(), now.x(), 1e-6, "carried round with it");
        assertEquals(expected.z(), now.z(), 1e-6);
        assertEquals(-expected.y(), now.height(), 1e-6);
        assertEquals(Num.wrap(landed.place().yaw() + b.yaw() - a.yaw()), now.yaw(), 1e-6, "and turned as it turned");
        assertTrue(Math.hypot(now.x() - landed.place().x(), now.z() - landed.place().z()) > 1500, "a long way");
        final Vec3 pilot = a.carry(stood, b);
        assertEquals(pilot.x(), play.avatar().body().position().x(), 1e-6, "the pilot with it");
        assertEquals(pilot.z(), play.avatar().body().position().z(), 1e-6);
        assertTrue(play.atControls().isPresent());
        // started again, it lifts off and is its own again
        play = once(play, ENGINE, events);
        play = run(play, UP, 1, events);
        assertFalse(carpet(play).resting(play.under(carpet(play))));
        assertTrue(carpet(play).place().height() > now.height() + 100);
    }

    @Test
    void dockedOnTheBargeItGoesWhereTheBargeIsFlown() {
        final Play start = Play.start(false);
        final Lift barge = start.lifts().get(1);
        // the small one set down on the barge's deck, clear of the stall and the console
        final Vec3 spot = new Vec3(barge.place().x(), 0, barge.place().z())
                .add(Num.right(barge.place().yaw()).multiply(-300)).add(Num.heading(barge.place().yaw()).multiply(100));
        final Lift onIt = new Lift(start.lifts().getFirst().piece(), Course.CARPET,
                new Place(spot.x(), spot.z(), barge.place().height() + Lift.THICK, barge.place().yaw()), 0, 0, 0, false,
                false);
        final List<Lift> lifts = new ArrayList<>(start.lifts());
        lifts.set(0, onIt);
        final List<Event> events = new ArrayList<>();
        Play play = new Play(new Avatar(Body.standing(barge.pilot(), barge.place().yaw()), false), start.damage(),
                start.sky(), lifts);
        play = once(play, TAKE, events);
        assertTrue(play.atControls().map(l -> l.piece() == barge.piece()).orElse(false), "flying the barge");
        play = run(play, UP, 2, events);
        play = run(play, new Controls(1, 0, 0.4, 0, true, false, 0), 2, events);
        final Lift flown = play.lifts().get(1);
        assertTrue(flown.place().height() > barge.place().height() + 300, "the barge climbed");
        assertTrue(Math.hypot(flown.place().x() - barge.place().x(), flown.place().z() - barge.place().z()) > 500,
                "and flew, turning, the carpet on it not in its way");
        final Vec3 expected = barge.place().carry(new Vec3(spot.x(), -onIt.place().height(), spot.z()), flown.place());
        assertEquals(expected.x(), carpet(play).place().x(), 1e-6, "and the carpet went with it");
        assertEquals(expected.z(), carpet(play).place().z(), 1e-6);
        assertEquals(flown.place().height() + Lift.THICK, carpet(play).place().height(), 1e-6);
    }
}
