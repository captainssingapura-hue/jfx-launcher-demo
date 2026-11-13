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
        assertEquals(1, platform.size());
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
        play = new Play(play.avatar(), broken, play.sky(), play.lifts(), play.people(), play.props());
        events.clear();
        play = once(play, Controls.NONE, events);
        assertTrue(events.contains(new Event.LetGo(piece)));
        assertFalse(play.aboard().isPresent());
        assertFalse(play.withinReach().isPresent(), "a wreck is not to be flown");
        play = run(play, Controls.NONE, 3, events);
        assertTrue(play.avatar().body().grounded(), "fallen to the ground");
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
