package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The airship to be flown, armed: a giant Gatling gun in a turret under its gondola, manned from
 * inside by whoever flies it, boarded at the hatch as ever. Its keys fly it, A and D turning it; the
 * mouse aims the gun, which holds its aim as the airship turns; the trigger fires the gun, and the
 * other button lets the bombs go.
 */
class AirshipGunTest {

    private static final Controls TAKE = Controls.doing(Action.TAKE);
    private static final Controls UP = new Controls(0, 0, 0, 1, true, false, 0);
    private static final Controls TURN_RIGHT = new Controls(0, 1, 0, 0, true, false, 0);
    private static final Controls FIRE = new Controls(0, 0, 0, 0, true, true, 0);
    private static final Controls BOMB = new Controls(0, 0, 0, 0, true, false, 0).bombing(true);

    private static Play run(Play play, final Controls controls, final double seconds, final List<Event> events) {
        for (long step = Math.round(seconds / Clock.STEP); step > 0; step--) {
            final Play.Stepped stepped = play.step(controls, Clock.STEP);
            play = stepped.play();
            events.addAll(stepped.events());
        }
        return play;
    }

    private static Turret gun(final Play play) {
        return play.turrets().stream().filter(t -> t.mount() == Course.AIRSHIP_GUN).findFirst().orElseThrow();
    }

    private static Lift ship(final Play play) {
        return play.lifts().getFirst();
    }

    /** The airship demo, the avatar on the ground under the airship's hatch. */
    private static Play underTheHatch() {
        final Play start = Play.start(Demo.AIRSHIP, false);
        final Vec3 hatch = start.lifts().getFirst().pilot();
        return new Play(new Avatar(Body.standing(new Vec3(hatch.x(), 0, hatch.z()), 0), false), start.damage(),
                start.sky(), start.lifts(), start.people(), start.props(), start.turrets());
    }

    /** Boarded, and flown up out of its moorings for {@code seconds}. */
    private static Play aloft(final double seconds, final List<Event> events) {
        return run(run(underTheHatch(), TAKE, Clock.STEP, events), UP, seconds, events);
    }

    @Test
    void theAirshipToBeFlownCarriesTheGiantGunUnderItsGondola() {
        final Play start = Play.start(Demo.AIRSHIP, false);
        final Turret gun = gun(start);
        assertEquals(ship(start).piece(), gun.host());
        assertEquals(Gun.RAPID_GATLING, gun.mount().gun());
        assertTrue(gun.mount().closed(), "manned from inside");
        assertTrue(gun.mount().stabilized(), "holding its aim as the airship turns");
        assertTrue(Course.AIRSHIP_GUN.pivot().up() < Course.GONDOLA.up() - Course.GONDOLA.halfUp(),
                "hung under the gondola");
        assertTrue(Course.AIRSHIP_GUN.barrel() > Course.MOUNTED_GUN.barrel(), "bigger than the heavy one");
        assertTrue(Weapon.RAPID_GATLING.blow() > Weapon.HEAVY_GATLING.blow(), "and its rounds heavier");
        assertTrue(Gun.RAPID_GATLING.interval() < Gun.GIANT_GATLING.interval() / 3, "and more than three times as fast as the giant");
        // those going round on their routes carry none
        final Play playground = Play.start(Demo.PLAYGROUND, false);
        assertEquals(1, playground.turrets().stream().filter(t -> t.mount() == Course.AIRSHIP_GUN).count());
    }

    @Test
    void boardedAtTheHatchTheGunAndTheAirshipAreTakenTogether() {
        final List<Event> events = new ArrayList<>();
        Play play = run(underTheHatch(), TAKE, Clock.STEP, events);
        final int piece = ship(play).piece();
        assertEquals(List.of(new Event.TookControls(piece)), events, "once");
        assertTrue(gun(play).held());
        assertTrue(ship(play).held());
        assertTrue(play.hidden(), "inside, out of sight");
        // flown up, the gunner inside goes with it
        play = run(play, UP, 3, events);
        assertTrue(ship(play).place().height() > Course.MOORED + 400, "it climbed");
        assertEquals(0, play.avatar().body().position().distance(gun(play).at(Course.AIRSHIP_GUN.seat())), 1e-6);
        // let go aloft, out through the hatch, and down to the ground
        events.clear();
        play = run(play, TAKE, Clock.STEP, events);
        assertEquals(List.of(new Event.LetGo(piece)), events);
        assertFalse(gun(play).held());
        assertFalse(ship(play).held());
        assertFalse(play.hidden());
        assertTrue(play.avatar().body().height() > 300, "out under the hatch, aloft");
        play = run(play, Controls.NONE, 4, events);
        assertTrue(play.avatar().body().grounded(), "down on the ground");
    }

    @Test
    void ADTurnTheAirshipTheMouseAimsTheGunWhichHoldsItsAim() {
        final List<Event> events = new ArrayList<>();
        Play play = aloft(2, events);
        // the mouse swings the gun round, not the airship
        final double yaw = ship(play).place().yaw();
        play = run(play, new Controls(0, 0, 0.5, 0, true, false, 0), 1, events);
        assertEquals(yaw, ship(play).place().yaw(), 1e-9, "the airship as it was");
        assertEquals(60, gun(play).yaw(), 1, "the gun swung round");
        // D turns the airship; the gun holds its aim on the world, turning the other way in its turret
        final double aim = gun(play).heading();
        play = run(play, TURN_RIGHT, 2, events);
        assertTrue(Num.wrap(ship(play).place().yaw() - yaw) > 5, "turned right");
        assertEquals(aim, gun(play).heading(), 1e-6, "the gun still pointing where it was");
    }

    @Test
    void theTriggerFiresTheGunAndTheOtherButtonLetsTheBombsGo() {
        final List<Event> events = new ArrayList<>();
        Play play = aloft(3, events);
        events.clear();
        play = run(play, FIRE, 3, events);
        final long rounds = events.stream().filter(e -> e instanceof Event.Fired f && f.by() == Weapon.RAPID_GATLING)
                .count();
        // spun up first, then a round each interval
        assertEquals((3 - Gun.RAPID_GATLING.spinUp()) / Gun.RAPID_GATLING.interval(), rounds, 2);
        assertTrue(events.stream().noneMatch(e -> e instanceof Event.Dropped), "the trigger is the gun's");
        // the gun spun down, the other button drops a stick of bombs, and fires nothing
        play = run(play, Controls.NONE, Gun.RAPID_GATLING.spinDown() + 0.5, events);
        events.clear();
        final int bombs = ship(play).bombs();
        play = run(play, BOMB, 1, events);
        assertTrue(events.stream().anyMatch(e -> e instanceof Event.Dropped), "bombs away");
        assertTrue(ship(play).bombs() < bombs);
        assertTrue(events.stream().noneMatch(e -> e instanceof Event.Fired), "and the gun quiet");
    }

    @Test
    void theGunGivenUpItIsStillSteeredAsACarAndTakenBackItIsTheGunsAgain() {
        final List<Event> events = new ArrayList<>();
        Play play = aloft(3, events);
        play = run(play, Controls.doing(Action.GUN), Clock.STEP, events);
        assertFalse(gun(play).held(), "the gun given up");
        assertTrue(ship(play).held(), "the airship still flown");
        assertTrue(play.hidden(), "from inside");
        // the mouse neither turns the airship nor aims the gun
        final double still = ship(play).place().yaw();
        play = run(play, new Controls(0, 0, 0.5, 0, true, false, 0), 1, events);
        assertEquals(still, ship(play).place().yaw(), 1e-9, "the mouse does not steer it");
        // A and D still turn it, as a car's wheel would; the gun, let be, holds its aim on the world
        final double yaw = ship(play).place().yaw();
        final double gunYaw = gun(play).yaw();
        play = run(play, TURN_RIGHT, 1, events);
        assertTrue(Num.wrap(ship(play).place().yaw() - yaw) > 1, "the airship turned");
        assertEquals(gunYaw, Num.wrap(gun(play).yaw() + ship(play).place().yaw() - yaw), 1e-6,
                "the gun held where it was pointing");
        // and a click lets the bombs go, the gun silent
        events.clear();
        play = run(play, FIRE, 1, events);
        assertTrue(events.stream().anyMatch(e -> e instanceof Event.Dropped));
        assertTrue(events.stream().noneMatch(e -> e instanceof Event.Fired));
        // taken back, the mouse is the gun's
        play = run(play, Controls.doing(Action.GUN), Clock.STEP, events);
        assertTrue(gun(play).held());
        final double turned = ship(play).place().yaw();
        play = run(play, new Controls(0, 0, 0.5, 0, true, false, 0), 1, events);
        assertEquals(turned, ship(play).place().yaw(), 1e-9, "the airship as it was");
    }

    @Test
    void aimedDownItsRoundsStrikeTheGroundFarBelowAndNeverTheAirship() {
        final List<Event> events = new ArrayList<>();
        Play play = aloft(3, events);
        events.clear();
        play = run(play, new Controls(0, 0, 0, 0, true, true, -90), 3, events);
        assertEquals(-Course.AIRSHIP_GUN.depression(), gun(play).pitch(), 1e-9, "as far down as it goes");
        final int host = gun(play).host();
        final List<Event.Struck> struck = events.stream().filter(e -> e instanceof Event.Struck)
                .map(e -> (Event.Struck) e).toList();
        assertFalse(struck.isEmpty(), "they strike something");
        assertTrue(struck.stream().noneMatch(s -> s.piece() == host), "never its own gondola");
        assertTrue(struck.stream().anyMatch(s -> s.on() == Surface.GROUND), "the ground below");
        assertEquals(1, play.damage().left(host), 1e-12);
    }
}
