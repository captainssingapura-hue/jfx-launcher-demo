package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The gunship: a platform with a heavy Gatling gun on its console, flown by whoever mans the gun,
 * the keys flying it, A and D turning it, and the mouse aiming the gun, which holds its aim on the
 * world as the gunship turns under it.
 */
class GunshipTest {

    private static final Controls TAKE = Controls.doing(Action.TAKE);
    private static final Controls UP = new Controls(0, 0, 0, 1, true, false, 0);
    private static final Controls AHEAD = new Controls(1, 0, 0, 0, true, false, 0);
    private static final Controls TURN_RIGHT = new Controls(0, 1, 0, 0, true, false, 0);

    private static Play run(Play play, final Controls controls, final double seconds, final List<Event> events) {
        for (long step = Math.round(seconds / Clock.STEP); step > 0; step--) {
            final Play.Stepped stepped = play.step(controls, Clock.STEP);
            play = stepped.play();
            events.addAll(stepped.events());
        }
        return play;
    }

    private static Turret gun(final Play play) {
        return play.turrets().stream().filter(t -> t.mount() == Course.GUNSHIP_GUN).findFirst().orElseThrow();
    }

    private static Lift ship(final Play play) {
        final int host = gun(play).host();
        return play.lifts().stream().filter(l -> l.piece() == host).findFirst().orElseThrow();
    }

    /** The guns demo, the avatar on the gunship's deck behind its gun, and manning it. */
    private static Play manned(final List<Event> events) {
        final Play start = Play.start(Demo.GUNS, false);
        final Turret gun = gun(start);
        final Play aboard = new Play(new Avatar(Body.standing(gun.at(gun.mount().station()), 0), false),
                start.damage(), start.sky(), start.lifts(), start.people(), start.props(), start.turrets());
        assertTrue(aboard.gunWithinReach().isPresent());
        assertTrue(aboard.withinReach().isEmpty(), "its console is its gun's: not taken on its own");
        return run(aboard, TAKE, Clock.STEP, events);
    }

    @Test
    void theGunsDemoHasAGunshipWithItsGun() {
        final Play start = Play.start(Demo.GUNS, false);
        assertEquals(2, start.turrets().size(), "the bunker's, and the gunship's");
        assertEquals(Course.GUNSHIP, ship(start).craft());
    }

    @Test
    void mannedItsGunFliesIt() {
        final List<Event> events = new ArrayList<>();
        Play play = manned(events);
        assertTrue(gun(play).held());
        assertTrue(ship(play).held(), "and its controls with it");
        assertEquals(List.of(new Event.TookControls(gun(play).host())), events, "once");
        assertFalse(play.hidden(), "the gunner in plain sight");
        final Place was = ship(play).place();
        play = run(play, UP, 2, events);
        play = run(play, AHEAD, 2, events);
        final Place now = ship(play).place();
        assertTrue(now.height() > was.height() + 300, "it climbed");
        assertTrue(now.z() > was.z() + 500, "and went ahead");
        // the gunner went with it, still behind the gun, facing the way it points
        final Vec3 station = gun(play).at(gun(play).mount().station());
        assertEquals(0, play.avatar().body().position().distance(station), 1, "at the gun");
        assertEquals(gun(play).heading(), play.avatar().body().yaw(), 1e-9);
    }

    @Test
    void ADAndDTurnItTheMouseAimsTheGunWhichHoldsItsAim() {
        final List<Event> events = new ArrayList<>();
        Play play = manned(events);
        play = run(play, UP, 1, events);
        // the mouse swings the gun round, not the gunship
        final double yaw = ship(play).place().yaw();
        play = run(play, new Controls(0, 0, 0.5, 0, true, false, 0), 1, events);
        assertEquals(yaw, ship(play).place().yaw(), 1e-9, "the gunship as it was");
        assertEquals(60, gun(play).yaw(), 1, "the gun swung round");
        // D turns the gunship; the gun holds its aim on the world, turning the other way on it
        final double aim = gun(play).heading();
        play = run(play, TURN_RIGHT, 1, events);
        assertEquals(Play.GUNSHIP_TURN, Num.wrap(ship(play).place().yaw() - yaw), 1, "turned at its rate");
        assertEquals(aim, gun(play).heading(), 1e-6, "the gun still pointing where it was");
    }

    @Test
    void itsRoundsGoThroughTheGunshipAndDownAtTheGround() {
        final List<Event> events = new ArrayList<>();
        Play play = manned(events);
        play = run(play, UP, 3, events);
        events.clear();
        // aimed down as far as it goes, and fired
        play = run(play, new Controls(0, 0, 0, 0, true, true, -90), 3, events);
        assertEquals(-Course.GUNSHIP_GUN.depression(), gun(play).pitch(), 1e-9);
        final List<Event.Struck> struck = events.stream().filter(e -> e instanceof Event.Struck)
                .map(e -> (Event.Struck) e).toList();
        assertTrue(events.stream().anyMatch(e -> e instanceof Event.Fired f && f.by() == Weapon.HEAVY_GATLING));
        final int host = gun(play).host();
        assertTrue(struck.stream().noneMatch(s -> s.piece() == host), "never its own deck");
        assertEquals(1, play.damage().left(host), 1e-12);
    }

    @Test
    void letGoItHangsWhereItIsWithTheGunnerAboard() {
        final List<Event> events = new ArrayList<>();
        Play play = manned(events);
        play = run(play, UP, 2, events);
        events.clear();
        play = run(play, TAKE, Clock.STEP, events);
        assertFalse(gun(play).held());
        assertFalse(ship(play).held());
        assertEquals(List.of(new Event.LetGo(gun(play).host())), events);
        // it eases to a stop, as a platform let go does, and hangs there
        play = run(play, Controls.NONE, 3, events);
        final double height = ship(play).place().height();
        play = run(play, Controls.NONE, 2, events);
        assertEquals(height, ship(play).place().height(), 1e-6, "hanging where it stopped");
        assertTrue(play.avatar().body().grounded(), "the gunner on its deck");
        assertEquals(height, play.avatar().body().height(), 1);
    }
}
