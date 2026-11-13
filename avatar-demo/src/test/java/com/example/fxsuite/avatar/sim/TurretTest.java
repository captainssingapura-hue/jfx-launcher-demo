package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A gun on a mount, aimed freely: limits, slew, heat, its rounds passing through what carries it;
 * and the bunker's, manned from its door, out of sight, and let go.
 */
class TurretTest {

    private static final Controls TAKE = Controls.doing(Action.TAKE);
    private static final Controls FIRE = new Controls(0, 0, 0, 0, true, true, 0);

    private static Play run(Play play, final Controls controls, final double seconds, final List<Event> events) {
        for (long step = Math.round(seconds / Clock.STEP); step > 0; step--) {
            final Play.Stepped stepped = play.step(controls, Clock.STEP);
            play = stepped.play();
            events.addAll(stepped.events());
        }
        return play;
    }

    /** A turret on its own, manned, on open ground facing +z. */
    private static Turret manned() {
        return new Turret(-1, Course.BUNKER_GUN, new Place(0, 0, 0, 0)).held(true);
    }

    /** The bunker demo, the avatar standing at the bunker's door. */
    private static Play atTheDoor() {
        final Play start = Play.start(Demo.GUNS, false);
        final Turret gun = start.turrets().getFirst();
        return new Play(new Avatar(Body.standing(gun.at(gun.mount().station()), 0), false), start.damage(),
                start.sky(), start.lifts(), start.people(), start.props(), start.turrets());
    }

    @Test
    void itSwingsNoFasterThanItsSlewAndNoFurtherThanItsLimits() {
        Turret gun = manned();
        // asked round by 90 degrees at once: it swings at its own pace, and stops at its limit
        final Turret.Stepped first = gun.step(new Controls(0, 0, 90, 0, true, false, 0), 0.1, Obstacles.NONE);
        assertEquals(Course.BUNKER_GUN.traverse(), first.turret().wantYaw(), 1e-9, "asked no further than it turns");
        assertEquals(Course.BUNKER_GUN.slew() * 0.1, first.turret().yaw(), 1e-9, "swung no faster than it slews");
        gun = first.turret();
        for (int i = 0; i < 20; i++) {
            gun = gun.step(Controls.NONE.with(List.of()), 0.1, Obstacles.NONE).turret();
        }
        // let go of the mouse, it carries on to where it was asked
        assertEquals(Course.BUNKER_GUN.traverse(), gun.yaw(), 1e-9);
        // and up and down, as far as it goes
        gun = gun.step(new Controls(0, 0, 0, 0, true, false, 80), 2, Obstacles.NONE).turret();
        assertEquals(Course.BUNKER_GUN.elevation(), gun.pitch(), 1e-9);
        gun = gun.step(new Controls(0, 0, 0, 0, true, false, -80), 2, Obstacles.NONE).turret();
        assertEquals(-Course.BUNKER_GUN.depression(), gun.pitch(), 1e-9);
    }

    @Test
    void itFiresWhereItPointsHeavierThanTheHandHeldGun() {
        // a crate off to its right, 30 degrees round: it hits it only once turned to it
        final Vec3 off = Num.heading(30).multiply(2000);
        final Block crate = new Block(off.x(), off.z(), 150, 150, 30, 0, 300);
        final Obstacles world = new Obstacles(List.of(crate));
        Turret gun = manned().step(new Controls(0, 0, 30, 0, true, false, 0), 1, world).turret();
        assertEquals(30, gun.yaw(), 1e-9);
        final List<Blow> blows = new ArrayList<>();
        for (int i = 0; i < 240; i++) {
            final Turret.Stepped fired = gun.step(FIRE, Clock.STEP, world);
            gun = fired.turret();
            blows.addAll(fired.blows());
        }
        assertFalse(blows.isEmpty(), "struck once spun up");
        assertTrue(blows.stream().allMatch(b -> b.by() == Weapon.HEAVY_GATLING));
        assertTrue(Weapon.HEAVY_GATLING.blow() >= 4 * Weapon.GATLING.blow(), "far heavier rounds");
    }

    @Test
    void aGunThatOverheatsHeldDownTooLongGetsTooHotAndMustCool() {
        final Course.Mount b = Course.BUNKER_GUN;
        final Course.Mount hot = new Course.Mount(b.gun(), b.pivot(), b.barrel(), b.traverse(), b.depression(),
                b.elevation(), b.slew(), b.stabilized(), b.closed(), b.station(), b.seat(), true);
        Turret gun = new Turret(-1, hot, new Place(0, 0, 0, 0)).held(true);
        long rounds = 0;
        for (int i = 0; i < 120 * 8 && !gun.cooling(); i++) {
            final Turret.Stepped fired = gun.step(FIRE, Clock.STEP, Obstacles.NONE);
            gun = fired.turret();
            rounds += fired.events().size();
        }
        assertTrue(gun.cooling(), "too hot");
        assertEquals(1, gun.heat(), 1e-9);
        assertTrue(rounds >= 1 / Turret.HEAT, "after a hundred rounds or so: " + rounds);
        // the trigger still held, it fires no more until it has cooled
        final Turret.Stepped held = gun.step(FIRE, 1, Obstacles.NONE);
        assertTrue(held.events().isEmpty());
        gun = held.turret();
        for (int i = 0; i < 120 * 6 && gun.cooling(); i++) {
            gun = gun.step(FIRE, Clock.STEP, Obstacles.NONE).turret();
        }
        assertFalse(gun.cooling(), "cooled enough to fire again");
        assertTrue(gun.heat() <= Turret.RECOVERED + 1e-9);
    }

    @Test
    void theBunkersGunDoesNotOverheatForNow() {
        Turret gun = manned();
        long rounds = 0;
        for (int i = 0; i < 120 * 12; i++) {
            final Turret.Stepped fired = gun.step(FIRE, Clock.STEP, Obstacles.NONE);
            gun = fired.turret();
            rounds += fired.events().size();
        }
        assertFalse(gun.cooling());
        assertEquals(0, gun.heat(), 1e-12);
        assertEquals((12 - Gun.HEAVY_GATLING.spinUp()) / Gun.HEAVY_GATLING.interval(), rounds, 2, "firing all the while");
    }

    @Test
    void theBunkersGunIsMannedFromItsDoorAndFiresOutThroughItsWalls() {
        final List<Event> events = new ArrayList<>();
        Play play = atTheDoor();
        assertTrue(play.gunWithinReach().isPresent());
        assertFalse(Play.start(Demo.GUNS, false).gunWithinReach().isPresent(), "not from the start");
        play = run(play, TAKE, Clock.STEP, events);
        final Turret gun = play.turrets().getFirst();
        assertTrue(gun.held());
        assertTrue(events.contains(new Event.TookControls(gun.host())));
        assertTrue(play.hidden(), "inside, out of sight");
        // fired straight ahead: its rounds leave the bunker and hit nothing of it
        events.clear();
        play = run(play, FIRE, 2.5, events);
        assertTrue(events.stream().anyMatch(e -> e instanceof Event.Fired f && f.by() == Weapon.HEAVY_GATLING));
        assertEquals(1, play.damage().left(gun.host()), 1e-12, "its own bunker unharmed");
        // the dummies straight ahead are struck, and fall to it
        assertTrue(events.stream().anyMatch(e -> e instanceof Event.Struck s && s.by() == Weapon.HEAVY_GATLING));
        assertTrue(events.stream().anyMatch(e -> e instanceof Event.Broke b && b.kind() == Course.Kind.DUMMY));
        // and let go, the avatar is back at the door, on its feet
        events.clear();
        play = run(play, TAKE, Clock.STEP, events);
        assertTrue(events.contains(new Event.LetGo(gun.host())));
        assertFalse(play.hidden());
        play = run(play, Controls.NONE, 0.5, events);
        assertTrue(play.avatar().body().grounded());
        assertTrue(play.turrets().getFirst().reaches(play.avatar().body()), "at the door again");
    }

    @Test
    void theBunkerBrokenItsGunnerIsThrownOut() {
        final List<Event> events = new ArrayList<>();
        Play play = run(atTheDoor(), TAKE, Clock.STEP, events);
        final int bunker = play.turrets().getFirst().host();
        final List<Blow> blows = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            blows.add(new Blow(Vec3.ZERO, new Vec3(0, 0, -1), Surface.BLOCK, Weapon.GLAIVE, bunker));
        }
        final Damage broken = play.damage().took(blows).damage();
        assertTrue(broken.broken(bunker));
        play = new Play(play.avatar(), broken, play.sky(), play.lifts(), play.people(), play.props(), play.turrets());
        events.clear();
        play = run(play, Controls.NONE, Clock.STEP, events);
        assertTrue(events.contains(new Event.LetGo(bunker)));
        assertFalse(play.hidden());
        assertFalse(play.gunWithinReach().isPresent(), "a wreck is not to be manned");
    }

    @Test
    void aWorldWithoutGunsSumsAsItAlwaysHas() {
        final Play play = Play.start(Demo.PLATFORM, false);
        assertTrue(play.turrets().isEmpty());
        assertEquals(play.checksum(), new Play(play.avatar(), play.damage(), play.sky(), play.lifts(), play.people(),
                play.props(), List.of()).checksum());
    }
}
