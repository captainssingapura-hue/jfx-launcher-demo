package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The mounted gun: a heavy Gatling gun on a pedestal, a seat behind it, on top of each pillar.
 * Manned from behind it, its gunner sits, and the seat turns with the gun as it aims. Aimed by the
 * keys alone: A and D run an electric motor that turns it all round; W and S raise and lower it,
 * 30° down to 75° up, its hydraulics easing it smoothly to where it is asked to point; Space or
 * a click fires it.
 */
class MountedGunTest {

    private static final Controls TAKE = Controls.doing(Action.TAKE);

    private static Play run(Play play, final Controls controls, final double seconds, final List<Event> events) {
        for (long step = Math.round(seconds / Clock.STEP); step > 0; step--) {
            final Play.Stepped stepped = play.step(controls, Clock.STEP);
            play = stepped.play();
            events.addAll(stepped.events());
        }
        return play;
    }

    private static Turret gun(final Play play) {
        return play.turrets().stream().filter(t -> t.mount() == Course.MOUNTED_GUN).findFirst().orElseThrow();
    }

    /** The guns' demo, the avatar on top of the first pillar, behind its gun. */
    private static Play onThePillar() {
        final Play start = Play.start(Demo.GUNS, false);
        final Turret gun = gun(start);
        return new Play(new Avatar(Body.standing(gun.at(gun.mount().station()), gun.base().yaw()), false),
                start.damage(), start.sky(), start.lifts(), start.people(), start.props(), start.turrets());
    }

    private static Play manned(final List<Event> events) {
        final Play at = onThePillar();
        assertTrue(at.gunWithinReach().isPresent(), "within reach of its seat");
        return run(at, TAKE, Clock.STEP, events);
    }

    @Test
    void eachPillarHasAGunOnItsTopFacingOut() {
        final Play start = Play.start(Demo.GUNS, false);
        int pillars = 0;
        for (int piece = 0; piece < start.damage().size(); piece++) {
            final Course.Piece what = start.damage().piece(piece);
            if (what.kind() != Course.Kind.PILLAR) {
                continue;
            }
            pillars++;
            final int host = piece;
            final Turret gun = start.turrets().stream().filter(t -> t.host() == host).findFirst().orElseThrow();
            assertEquals(Course.MOUNTED_GUN, gun.mount());
            final Block block = what.block();
            assertEquals(block.top(), gun.base().height(), 1e-9, "on its top");
            assertEquals(block.x(), gun.base().x(), 1e-9);
            // facing out: further from the middle of the world ahead of it than behind
            final Vec3 ahead = gun.at(new Course.Section(0, 100, 0, 0, 0, 0));
            assertTrue(Math.hypot(ahead.x(), ahead.z()) > Math.hypot(block.x(), block.z()));
        }
        assertTrue(pillars > 10, "pillars about: " + pillars);
        assertEquals(Gun.HEAVY_GATLING, Course.MOUNTED_GUN.gun());
        assertEquals(180, Course.MOUNTED_GUN.traverse(), "all round, without stops");
        assertTrue(Course.MOUNTED_GUN.keyed(), "aimed by the keys");
    }

    private static final Controls RIGHT = new Controls(0, 1, 0, 0, true, false, 0);
    private static final Controls LEFT = new Controls(0, -1, 0, 0, true, false, 0);
    private static final Controls UP = new Controls(1, 0, 0, 0, true, false, 0);
    private static final Controls DOWN = new Controls(-1, 0, 0, 0, true, false, 0);

    @Test
    void mannedItsGunnerSitsBehindItAndTurnsWithIt() {
        final List<Event> events = new ArrayList<>();
        Play play = manned(events);
        final Turret gun = gun(play);
        assertTrue(gun.held());
        assertEquals(List.of(new Event.TookControls(gun.host())), events);
        assertFalse(play.hidden(), "in plain sight");
        assertEquals(0, play.avatar().body().position().distance(gun.seated()), 1e-6, "at its seat");
        // D turns it round, the seat goes round with it, and the gunner with the seat
        play = run(play, RIGHT, 1, events);
        final Turret turned = gun(play);
        assertTrue(turned.yaw() > 30, "turned: " + turned.yaw());
        final Vec3 seat = turned.seated();
        assertEquals(0, play.avatar().body().position().distance(seat), 1e-6, "still at its seat, wherever it is now");
        assertEquals(turned.heading(), play.avatar().body().yaw(), 1e-9, "facing the way it points");
        assertTrue(seat.distance(gun.seated()) > 30, "the seat moved round with it");
        assertEquals(gun.seated().distance(gun.pivot()), seat.distance(turned.pivot()), 1e-6,
                "round where the gun turns");
    }

    @Test
    void ADRunItsMotorAllRoundAtASteadyPaceAndTheMouseLeavesItBe() {
        final List<Event> events = new ArrayList<>();
        Play play = manned(events);
        play = run(play, RIGHT, 0.5, events);
        assertEquals(Course.MOUNTED_GUN.slew() * 0.5, gun(play).yaw(), 1, "at its motor's steady pace");
        // let go, it stops where it is
        final double stopped = gun(play).yaw();
        play = run(play, Controls.NONE, 1, events);
        assertEquals(stopped, gun(play).yaw(), 1e-9);
        // held on, round past where a stop would be, and all the way round
        play = run(play, RIGHT, 6, events);
        assertEquals(Num.wrap(stopped + Course.MOUNTED_GUN.slew() * 6), gun(play).yaw(), 1, "a full circle and more");
        final double now = gun(play).yaw();
        play = run(play, LEFT, 1, events);
        assertEquals(Num.wrap(now - Course.MOUNTED_GUN.slew()), gun(play).yaw(), 1, "and back the other way");
        // the mouse neither turns it nor raises it
        final Turret before = gun(play);
        play = run(play, new Controls(0, 0, 3, 0, true, false, 40), 1, events);
        assertEquals(before.yaw(), gun(play).yaw(), 1e-9);
        assertEquals(before.pitch(), gun(play).pitch(), 1e-9);
    }

    @Test
    void WSRaiseAndLowerItItsHydraulicsEasingItWithinItsLimits() {
        final List<Event> events = new ArrayList<>();
        Play play = manned(events);
        // raised a while, then let go: it eases on to where it was asked, quick at first, slower as it comes
        play = run(play, UP, 0.5, events);
        final double asked = Turret.RAISE * 0.5;
        assertTrue(gun(play).pitch() < asked, "behind the keys, eased: " + gun(play).pitch());
        final List<Double> pitches = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            play = run(play, Controls.NONE, Clock.STEP, events);
            pitches.add(gun(play).pitch());
        }
        assertTrue(pitches.get(1) - pitches.get(0) > pitches.get(23) - pitches.get(22), pitches.toString());
        play = run(play, Controls.NONE, Course.MOUNTED_GUN.ease() * 6, events);
        assertEquals(asked, gun(play).pitch(), 0.5, "there in the end, and held");
        // no higher than 75°, no lower than 30° down
        play = run(play, UP, 4, events);
        play = run(play, Controls.NONE, 2, events);
        assertEquals(75, gun(play).pitch(), 0.5);
        play = run(play, DOWN, 6, events);
        play = run(play, Controls.NONE, 2, events);
        assertEquals(-30, gun(play).pitch(), 0.5);
    }

    @Test
    void SpaceOrAClickFiresHeavyRoundsAndLetGoItsGunnerGetsUpOnThePillar() {
        final List<Event> events = new ArrayList<>();
        Play play = manned(events);
        events.clear();
        play = run(play, new Controls(0, 0, 0, 1, true, false, 0), 2, events);
        assertTrue(events.stream().anyMatch(e -> e instanceof Event.Fired f && f.by() == Weapon.HEAVY_GATLING),
                "Space fires it");
        assertTrue(gun(play).held(), "and leaves the gunner sitting");
        play = run(play, Controls.NONE, Gun.HEAVY_GATLING.spinDown() + 0.5, events);
        events.clear();
        play = run(play, new Controls(0, 0, 0, 0, true, true, 0), 2, events);
        assertTrue(events.stream().anyMatch(e -> e instanceof Event.Fired), "and so does a click");
        play = run(play, RIGHT, 1, events);
        final Turret gun = gun(play);
        events.clear();
        play = run(play, TAKE, Clock.STEP, events);
        assertEquals(List.of(new Event.LetGo(gun.host())), events);
        assertFalse(gun(play).held());
        // up from the seat where it had turned to, and standing on the pillar's top
        play = run(play, Controls.NONE, 1, events);
        assertTrue(play.avatar().body().grounded());
        assertEquals(gun.base().height(), play.avatar().body().height(), 1, "on the pillar, not fallen off it");
    }
}
