package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The vehicles' own demos, and the airship, closed: boarded at the hatch in its gondola's floor,
 * flown from inside, out of sight, and stepped out of; and broken, throwing out whoever flies it.
 */
class VehicleTest {

    private static final Controls TAKE = Controls.doing(Action.TAKE);
    private static final Controls ENGINE = Controls.doing(Action.FLY);
    private static final Controls UP = new Controls(0, 0, 0, 1, true, false, 0);
    private static final Controls AHEAD = new Controls(1, 0, 0, 0, true, false, 0);
    /** The other button: aboard the airship, whose trigger is its gun's, it lets the bombs go. */
    private static final Controls DROP = new Controls(0, 0, 0, 0, true, false, 0).bombing(true);

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

    /** The airship demo, the avatar standing on the ground under the airship's hatch. */
    private static Play underTheHatch() {
        final Play start = Play.start(Demo.AIRSHIP, false);
        final Vec3 hatch = start.lifts().getFirst().pilot();
        return new Play(new Avatar(Body.standing(new Vec3(hatch.x(), 0, hatch.z()), 0), false), start.damage(),
                start.sky(), start.lifts(), start.people(), start.props());
    }

    @Test
    void eachDemoBuildsAndEachVehicleDemoHasItsVehicle() {
        for (final Demo demo : Demo.values()) {
            final Play play = Play.start(demo, false);
            assertFalse(play.damage().size() == 0, demo.name());
            assertEquals(demo.startYaw(), play.avatar().body().yaw(), 1e-9);
        }
        final List<Lift> platform = Play.start(Demo.PLATFORM, false).lifts();
        assertEquals(List.of(Course.CARPET, Course.MEETING), platform.stream().map(Lift::deck).toList(), "and the stand-up's");
        assertFalse(platform.getFirst().craft().closed(), "a platform is open");
        final List<Lift> airship = Play.start(Demo.AIRSHIP, false).lifts();
        assertEquals(1, airship.size());
        assertTrue(airship.getFirst().craft().closed(), "an airship is closed");
        assertEquals(Course.SHIP, airship.getFirst().craft());
        assertTrue(Play.start(Demo.AIRSHIP, false).airships().isEmpty(), "it is not one that goes round");
    }

    @Test
    void theAvatarWalksUnderTheMooredAirshipAndCanReachItsHatch() {
        final Play play = underTheHatch();
        final Course.Piece ship = play.damage().piece(play.lifts().getFirst().piece());
        assertTrue(ship.block().bottom() > Body.TALL, "the gondola clears the avatar's head");
        assertTrue(play.withinReach().isPresent());
        assertTrue(play.gunWithinReach().isEmpty(), "its gun taken with it, not on its own");
        assertFalse(Play.start(Demo.AIRSHIP, false).withinReach().isPresent(), "not from the start");
    }

    @Test
    void boardedItIsFlownFromInsideAndSteppedOutOf() {
        final List<Event> events = new ArrayList<>();
        Play play = once(underTheHatch(), TAKE, events);
        final Lift ship = play.lifts().getFirst();
        assertEquals(List.of(new Event.TookControls(ship.piece())), events);
        assertTrue(play.aboard().isPresent(), "inside, out of sight");
        assertEquals(ship.at(Course.SHIP.cabin()).y(), play.avatar().body().position().y(), 1e-9);
        // its engine is never cut: the key that would does nothing
        events.clear();
        play = once(play, ENGINE, events);
        assertTrue(events.isEmpty());
        assertTrue(play.lifts().getFirst().engine());
        // flown up and ahead, the avatar inside goes with it
        play = run(play, UP, 3, events);
        play = run(play, AHEAD, 3, events);
        final Lift flown = play.lifts().getFirst();
        assertTrue(flown.place().height() > Course.MOORED + 400, "it climbed");
        assertTrue(Math.hypot(flown.place().x() - ship.place().x(), flown.place().z() - ship.place().z()) > 1000,
                "it went ahead");
        final Vec3 cabin = flown.at(Course.SHIP.cabin());
        assertEquals(0, play.avatar().body().position().distance(cabin), 1e-6, "the avatar inside, carried");
        assertTrue(play.aboard().isPresent(), "still inside: away from the hatch is no letting go");
        // let go, it steps out, drops from the hatch, and lands
        events.clear();
        play = once(play, TAKE, events);
        assertEquals(List.of(new Event.LetGo(ship.piece())), events);
        assertFalse(play.aboard().isPresent());
        assertTrue(play.avatar().body().height() > 300, "out under the hatch, aloft");
        play = run(play, Controls.NONE, 3, events);
        assertTrue(play.avatar().body().grounded(), "down on the ground");
        assertEquals(0, play.avatar().body().height(), 1e-9);
    }

    @Test
    void anAirshipNeverComesDownBelowItsMoorings() {
        final List<Event> events = new ArrayList<>();
        Play play = once(underTheHatch(), TAKE, events);
        play = run(play, new Controls(0, 0, 0, -1, true, false, 0), 2, events);
        assertEquals(Course.MOORED, play.lifts().getFirst().place().height(), 1e-9);
    }

    @Test
    void brokenItThrowsOutWhoeverFliesIt() {
        final List<Event> events = new ArrayList<>();
        Play play = once(underTheHatch(), TAKE, events);
        play = run(play, UP, 2, events);
        final int piece = play.lifts().getFirst().piece();
        final List<Blow> blows = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            blows.add(new Blow(Vec3.ZERO, new Vec3(0, 0, -1), Surface.BLOCK, Weapon.GLAIVE, piece));
        }
        final Damage broken = play.damage().took(blows).damage();
        assertTrue(broken.broken(piece));
        play = new Play(play.avatar(), broken, play.sky(), play.lifts(), play.people(), play.props(), play.turrets());
        events.clear();
        play = once(play, Controls.NONE, events);
        assertTrue(events.contains(new Event.LetGo(piece)));
        assertFalse(play.aboard().isPresent());
        assertFalse(play.withinReach().isPresent(), "a wreck is not to be flown");
        assertFalse(play.gunWithinReach().isPresent(), "nor its gun manned");
        play = run(play, Controls.NONE, 3, events);
        assertTrue(play.avatar().body().grounded(), "fallen to the ground");
    }

    @Test
    void aloftTheOtherButtonDropsItsBombsOneARackApartUntilTheBayIsEmpty() {
        final List<Event> events = new ArrayList<>();
        Play play = once(underTheHatch(), TAKE, events);
        assertEquals(Course.SHIP.bombs(), play.lifts().getFirst().bombs(), "a full bay");
        assertTrue(play.lifts().getFirst().moored());
        // moored, the bay is shut
        events.clear();
        play = run(play, DROP, 1, events);
        assertTrue(events.stream().noneMatch(e -> e instanceof Event.Dropped), "nothing dropped at the moorings");
        play = run(play, UP, 3, events);
        assertFalse(play.lifts().getFirst().moored());
        events.clear();
        play = run(play, DROP, 2, events);
        final int piece = play.lifts().getFirst().piece();
        final long dropped = events.stream().filter(e -> e instanceof Event.Dropped d && d.from() == piece).count();
        assertEquals(Math.ceil(2 / Lift.RACK), dropped, 1, "one a rack apart");
        assertEquals(Course.SHIP.bombs() - dropped, play.lifts().getFirst().bombs());
        assertFalse(play.sky().bombs().isEmpty(), "falling");
        final double height = play.lifts().getFirst().place().height();
        assertTrue(play.sky().bombs().stream().allMatch(b -> b.at().y() > -height),
                "from under it");
        // they go off on the ground below
        events.clear();
        play = run(play, Controls.NONE, 3, events);
        assertTrue(events.stream().anyMatch(e -> e instanceof Event.Exploded));
        play = run(play, DROP, 10, events);
        assertEquals(0, play.lifts().getFirst().bombs(), "the bay emptied");
        events.clear();
        play = run(play, DROP, 1, events);
        assertTrue(events.stream().noneMatch(e -> e instanceof Event.Dropped), "and none left to drop");
    }

    @Test
    void mooredAgainItLoadsItsBayBackABombEachReload() {
        final List<Event> events = new ArrayList<>();
        Play play = once(underTheHatch(), TAKE, events);
        play = run(play, UP, 2, events);
        play = run(play, DROP, 2, events);
        final int left = play.lifts().getFirst().bombs();
        assertTrue(left < Course.SHIP.bombs());
        final Controls down = new Controls(0, 0, 0, -1, true, false, 0);
        for (int k = 0; k < 1200 && !play.lifts().getFirst().moored(); k++) {
            play = once(play, down, events);
        }
        assertTrue(play.lifts().getFirst().moored(), "down at its moorings");
        assertTrue(play.lifts().getFirst().bombs() <= left + 1, "none loaded on the way down");
        final int moored = play.lifts().getFirst().bombs();
        play = run(play, Controls.NONE, 2.5, events);
        assertEquals(moored + 2, play.lifts().getFirst().bombs(), 1, "a bomb back each second");
        play = run(play, Controls.NONE, Course.SHIP.bombs() * Lift.RELOAD, events);
        assertEquals(Course.SHIP.bombs(), play.lifts().getFirst().bombs(), "full again, and no more");
    }

    @Test
    void itsBombsGoOffOnTheGroundBelowAndSpareItHighUp() {
        final List<Event> events = new ArrayList<>();
        Play play = once(underTheHatch(), TAKE, events);
        play = run(play, UP, 2, events);
        final Lift ship = play.lifts().getFirst();
        play = run(play, DROP, 1, events);
        play = run(play, Controls.NONE, 3, events);
        assertTrue(events.stream().anyMatch(e -> e instanceof Event.Exploded x && x.on() == Surface.GROUND));
        assertEquals(1.0, play.damage().left(ship.piece()), 1e-9, "high enough, it takes none of its own blast");
    }

    @Test
    void aStickOfBombsOverTheYardBreaksTheCrateUnder() {
        final Play start = Play.start(Demo.AIRSHIP, false);
        final int crate = java.util.stream.IntStream.range(0, start.damage().size())
                .filter(i -> start.damage().piece(i).kind() == Course.Kind.CRATE).findFirst().orElseThrow();
        final Block box = start.damage().piece(crate).block();
        // the airship held still over it, ten metres up, the avatar at its controls inside
        final Lift ship = start.lifts().getFirst();
        final Lift over = new Lift(ship.piece(), Course.SHIP, new Place(box.x(), box.z(), box.top() + 1000, 0), 0, 0,
                0, true, true);
        final Vec3 bay = over.at(Course.SHIP.bay());
        final Lift centred = new Lift(ship.piece(), Course.SHIP,
                new Place(2 * box.x() - bay.x(), 2 * box.z() - bay.z(), box.top() + 1000, 0), 0, 0, 0, true, true);
        Play play = new Play(new Avatar(Body.standing(centred.at(Course.SHIP.cabin()), 0), false), start.damage(),
                start.sky(), List.of(centred), start.people(), start.props(),
                // its gun manned, as it is from inside: the airship is flown by its gunner
                start.turrets().stream().map(t -> t.host() == ship.piece() ? t.carried(centred.place()).held(true) : t)
                        .toList());
        final List<Event> events = new ArrayList<>();
        play = run(play, DROP, 3, events);
        play = run(play, Controls.NONE, 3, events);
        assertTrue(play.damage().broken(crate), "broken");
        assertTrue(events.stream().anyMatch(e -> e instanceof Event.Broke b && b.piece() == crate
                && b.by() == Weapon.BOMB), "by the bombs");
    }

    @Test
    void aPlatformHasNoBombBay() {
        final Lift platform = Play.start(Demo.PLATFORM, false).lifts().getFirst();
        assertEquals(0, platform.bombs());
        assertEquals(platform, platform.loaded(1, true));
    }

    @Test
    void aRecordingKeepsItsDemo() throws IOException {
        final StringWriter out = new StringWriter();
        final Recording.Recorder recorder = new Recording.Recorder(out, false, Demo.AIRSHIP);
        recorder.close();
        final Recording read = Recording.read(new StringReader(out.toString()));
        assertEquals(Demo.AIRSHIP, read.demo());
        assertEquals(Play.start(Demo.AIRSHIP, false).checksum(), read.start().checksum());
    }
}
