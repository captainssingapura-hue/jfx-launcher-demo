package com.example.fxsuite.avatar.sound;

import com.example.fxsuite.avatar.sim.Action;
import com.example.fxsuite.avatar.sim.Arms;
import com.example.fxsuite.avatar.sim.Avatar;
import com.example.fxsuite.avatar.sim.Block;
import com.example.fxsuite.avatar.sim.Body;
import com.example.fxsuite.avatar.sim.Controls;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Event;
import com.example.fxsuite.avatar.sim.Num;
import com.example.fxsuite.avatar.sim.Obstacles;
import com.example.fxsuite.avatar.sim.Surface;
import com.example.fxsuite.avatar.sim.Vec3;
import com.example.fxsuite.avatar.sim.Weapon;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** From what happened to the avatar to what is heard, and from where. */
class CuesTest {

    private static final Profile PROFILE = Profiles.ROBOT;

    /** An avatar as it stands after a step, and what that step reported. */
    private record Moment(Avatar avatar, List<Event> events) {
    }

    /** An avatar at the origin facing +z, moving at {@code speed} on the ground, and these events. */
    private static Moment with(final double speed, final boolean pack, final Event... events) {
        final Body body = new Body(Vec3.ZERO, 0, 0, speed, 0, 0, 0, false, true, 0, 0);
        return new Moment(new Avatar(body, pack), List.of(events));
    }

    /** A step, as a moment: the avatar after it, and what it reported. */
    private static Moment of(final Avatar.Stepped stepped) {
        return new Moment(stepped.avatar(), stepped.events());
    }

    private static Cues.Heard of(final Moment moment, final Profile profile) {
        return Cues.of(moment.avatar(), moment.events(), profile);
    }

    private static Cues.Heard of(final Moment moment, final Profile profile, final List<Vec3> airships) {
        return Cues.of(moment.avatar(), moment.events(), profile, airships);
    }

    private static List<Sound> played(final Moment moment) {
        return of(moment, PROFILE).plays().stream().map(Cues.Play::sound).toList();
    }

    @Test
    void eachFootIsHeardWhereItFallsAndOnWhat() {
        final List<Cues.Play> plays = of(with(Body.WALK, false,
                new Event.Footfall(true, Surface.GROUND), new Event.Footfall(false, Surface.BLOCK)), PROFILE).plays();
        assertEquals(Sound.STEP_GROUND, plays.get(0).sound());
        assertEquals(Sound.STEP_BLOCK, plays.get(1).sound());
        final Vec3 right = Num.right(0);
        assertTrue(plays.get(0).at().dotProduct(right) < 0, "the left foot, on the left");
        assertTrue(plays.get(1).at().dotProduct(right) > 0, "the right foot, on the right");
        final double walking = plays.get(0).amount();
        final double running = of(with(Body.RUN, false, new Event.Footfall(true, Surface.GROUND)), PROFILE)
                .plays().get(0).amount();
        assertTrue(running > walking, "running treads heavier: " + running + " > " + walking);
        assertEquals(1, running, 1e-9);
    }

    @Test
    void aLandingIsHeardAsHardAsItFellAndOnWhat() {
        final Cues.Play hop = of(with(0, false, new Event.Landed(Body.JUMP, false, Surface.GROUND)), PROFILE)
                .plays().get(0);
        final Cues.Play drop = of(with(0, false, new Event.Landed(4000, false, Surface.BLOCK)), PROFILE)
                .plays().get(0);
        assertEquals(Sound.LANDING, hop.sound());
        assertEquals(Sound.LANDING_BLOCK, drop.sound());
        assertTrue(hop.amount() > 0.2 && hop.amount() < 0.6, "a hop is middling: " + hop.amount());
        assertEquals(1, drop.amount(), 1e-9, "a drop from on high is as hard as it gets");
        assertEquals(Vec3.ZERO, hop.at(), "at the feet");
    }

    @Test
    void theWingsFollowFlightAndTheEnginesStopWhenItEnds() {
        assertEquals(List.of(Sound.IGNITION, Sound.WINGS_OPEN), played(with(0, true, new Event.TookOff())));
        assertEquals(List.of(Sound.CUT, Sound.WINGS_FOLD), played(with(0, true, new Event.FlightCut())));
        assertEquals(List.of(Sound.LANDING, Sound.CUT, Sound.WINGS_FOLD),
                played(with(0, true, new Event.Landed(Body.LIFT, true, Surface.GROUND))));
        assertEquals(List.of(Sound.CUT, Sound.WINGS_FOLD, Sound.PACK_OFF),
                played(with(0, true, new Event.FlightCut(), new Event.PackOff())));
        assertEquals(List.of(Sound.PACK_ON), played(with(0, false, new Event.PackOn())));
    }

    @Test
    void thePackSoundsFromTheBackAndAGateAtTheEar() {
        final Cues.Play on = of(with(0, false, new Event.PackOn()), PROFILE).plays().get(0);
        assertTrue(on.at().dotProduct(Num.heading(0)) < 0, "behind");
        assertTrue(-on.at().y() > Body.TALL / 2, "up the back");
        final Cues.Play gate = of(with(0, true, new Event.PassedGate(Body.BOOST)), PROFILE).plays().get(0);
        assertEquals(Sound.GATE, gate.sound());
        assertNull(gate.at());
        assertEquals(1, gate.amount(), 1e-9);
        final Cues.Play bump = of(with(0, false, new Event.Bumped(Body.RUN)), PROFILE).plays().get(0);
        assertEquals(Sound.BUMP, bump.sound());
        assertEquals(Body.RUN / Body.BOOST, bump.amount(), 1e-9);
    }

    @Test
    void shotsSoundFromEachMuzzleAndStrokesFromTheBlade() {
        final Body still = Body.standing(Vec3.ZERO, 0);
        final Vec3 far = new Vec3(0, -130, 5000);
        final List<Cues.Play> shots = of(with(0, false, new Event.Fired(true, Arms.muzzle(still, true), far, Weapon.RIFLES),
                new Event.Fired(false, Arms.muzzle(still, false), far, Weapon.RIFLES)), PROFILE).plays();
        assertEquals(List.of(Sound.RIFLE_SHOT, Sound.RIFLE_SHOT), shots.stream().map(Cues.Play::sound).toList());
        assertTrue(shots.get(0).at().dotProduct(Num.right(0)) < 0, "the left rifle, on the left");
        assertTrue(shots.get(1).at().dotProduct(Num.right(0)) > 0, "the right, on the right");
        assertTrue(shots.get(0).at().dotProduct(Num.heading(0)) > Body.RADIUS, "out ahead of the body");
        assertEquals(List.of(Sound.ROUND_ON_BLOCK, Sound.ROUND_ON_GROUND, Sound.BLADE_ON_BLOCK, Sound.BLADE_ON_GROUND),
                played(with(0, false,
                        new Event.Struck(far, new Vec3(0, 0, -1), Surface.BLOCK, Weapon.RIFLES),
                        new Event.Struck(far, new Vec3(0, -1, 0), Surface.GROUND, Weapon.RIFLES),
                        new Event.Struck(far, new Vec3(0, 0, -1), Surface.BLOCK, Weapon.GLAIVE),
                        new Event.Struck(far, new Vec3(0, -1, 0), Surface.GROUND, Weapon.GLAIVE))));
        assertEquals(far, of(with(0, false, new Event.Struck(far, new Vec3(0, 0, -1), Surface.BLOCK,
                Weapon.RIFLES)), PROFILE).plays().get(0).at(), "heard where it struck");
        assertEquals(Arms.muzzle(still, true), shots.get(0).at(), "and a shot from its muzzle");
        assertEquals(List.of(Sound.GLAIVE_CLEAVE, Sound.GLAIVE_SWEEP),
                played(with(0, false, new Event.Swung(Arms.CLEAVE), new Event.Swung(Arms.SWEEP))));
        assertEquals(List.of(Sound.RIFLES_DRAWN, Sound.GLAIVE_DRAWN),
                played(with(0, false, new Event.Drew(Weapon.RIFLES), new Event.Drew(Weapon.GLAIVE),
                        new Event.Drew(Weapon.NONE))), "putting a weapon away is silent");
    }

    @Test
    void whatBreaksIsHeardWhereItStoodBiggerCratesDeeper() {
        final Block dummy = Block.standing(0, 1000, Course.DUMMY_WIDTH, Course.DUMMY_DEPTH, Course.DUMMY_HEIGHT, 0);
        final Block crate = Block.standing(0, 1000, 260, 260, 240, 0);
        final Block drone = new Block(0, 1000, 65, 65, 0, 400, 440);
        final Vec3 hit = new Vec3(0, -100, 990);
        final List<Cues.Play> plays = of(with(0, false,
                new Event.Broke(1, Course.Kind.DUMMY, dummy, hit, Weapon.RIFLES),
                new Event.Broke(2, Course.Kind.CRATE, crate, hit, Weapon.GATLING),
                new Event.Broke(3, Course.Kind.DRONE, drone, hit, Weapon.RIFLES)), PROFILE).plays();
        assertEquals(List.of(Sound.BREAK_WOOD, Sound.BREAK_WOOD, Sound.BREAK_DRONE),
                plays.stream().map(Cues.Play::sound).toList());
        assertEquals(0, plays.get(0).amount(), 1e-9, "a dummy is the smallest");
        assertEquals(1, plays.get(1).amount(), 1e-9, "the biggest crate the biggest");
        assertEquals(new Vec3(0, -420, 1000), plays.get(2).at(), "from the middle of where it was");
    }

    @Test
    void theNearestAirshipDronesWhereItHangsAndOneComingDownIsHeard() {
        final Vec3 near = new Vec3(0, -3000, 5000);
        final Vec3 far = new Vec3(9000, -3000, 0);
        final Cues.Hum engines = of(with(0, false), PROFILE, List.of(far, near)).hums().stream()
                .filter(h -> h.loop() == Sound.AIRSHIP_ENGINES).findFirst().orElseThrow();
        assertEquals(near, engines.at());
        assertTrue(engines.level().gain() > 0);
        assertEquals(0, of(with(0, false), PROFILE, List.of()).hums().stream()
                .filter(h -> h.loop() == Sound.AIRSHIP_ENGINES).findFirst().orElseThrow().level().gain(), 1e-9,
                "all brought down: silent");

        final Block airship = Course.AIRSHIP_BOX.placed(0, 3000, 5000, 0);
        final Cues.Play down = of(with(0, false, new Event.Broke(4, Course.Kind.AIRSHIP, airship,
                near, Weapon.GATLING)), PROFILE).plays().getFirst();
        assertEquals(Sound.BREAK_AIRSHIP, down.sound());
        assertEquals(new Vec3(0, -3025, 5000), down.at(), "from the middle of where it hung");
    }

    @Test
    void theGatlingGunIsHeardAsItsRoarNotRoundByRound() {
        Moment gun = of(new Avatar(Body.standing(Vec3.ZERO, 0), false).act(new Action.Draw(Weapon.GATLING)));
        assertEquals(List.of(Sound.GATLING_DRAWN), played(gun));
        for (int i = 0; i < 60; i++) {
            gun = of(gun.avatar().step(new Controls(0, 0, 0, 0, false, true), 1.0 / 60, Obstacles.NONE));
            assertTrue(played(gun).stream().noneMatch(s -> s == Sound.RIFLE_SHOT), "no shot a round");
        }
        gun = of(gun.avatar().step(new Controls(0, 0, 0, 0, false, true), 1.0 / 60, Obstacles.NONE));
        // a profile that loops it: its spin and its roar
        final Cues.Heard looped = of(gun, Profiles.ARCADE);
        assertEquals(null, looped.gun());
        for (final Cues.Hum hum : looped.hums()) {
            if (hum.loop() == Sound.GATLING_SPIN || hum.loop() == Sound.GATLING_FIRE) {
                assertTrue(hum.level().gain() > 0, hum.loop() + " sounding, a second in");
                assertEquals(Arms.gatlingMuzzle(gun.avatar().body()), hum.at(), "from the gun");
            }
        }
        // one that makes it as it fires: the gun itself, firing, from its muzzle, in place of its loops
        final Cues.Heard made = of(gun, PROFILE);
        assertEquals(new Cues.Gunfire(Gatling.HAND, 1, true, Arms.gatlingMuzzle(gun.avatar().body())), made.gun());
        assertTrue(made.hums().stream().filter(h -> h.loop() == Sound.GATLING_SPIN || h.loop() == Sound.GATLING_FIRE)
                .allMatch(h -> h.level().gain() == 0), "its loops quiet");
    }

    @Test
    void aGunOnAMountIsMadeAsItFiresTheGunItIs() {
        final Vec3 muzzle = new Vec3(0, -4000, 300);
        final Cues.Heard heard = Cues.of(new Avatar(Body.standing(Vec3.ZERO, 0), false), List.of(), PROFILE, List.of(),
                new Cues.Gunner(0.5, false, muzzle, 0.55, Weapon.GIANT_GATLING));
        assertEquals(new Cues.Gunfire(Gatling.GIANT, 0.5, false, muzzle), heard.gun());
        assertEquals(40, heard.gun().height(), 1e-9, "forty metres over the ground");
        assertEquals(null, Cues.of(new Avatar(Body.standing(Vec3.ZERO, 0), false), List.of(), PROFILE).gun(),
                "no gun in hand or manned, none heard");
    }

    @Test
    void everyLoopIsSteeredEveryFrameTheJetsOnlyWhenFiring() {
        final Cues.Heard standing = of(with(0, true), PROFILE);
        assertEquals(Sound.loops(), standing.hums().stream().map(Cues.Hum::loop).toList());
        assertTrue(standing.hums().stream().allMatch(h -> h.level().gain() == 0), "quiet on the ground");
        assertTrue(standing.plays().isEmpty());

        Avatar flying = new Avatar(Body.standing(Vec3.ZERO, 0), true).toggledFlight();
        for (int i = 0; i < 90; i++) {
            flying = flying.step(new Controls(1, 0, 0, 0, true), 1.0 / 60, Obstacles.NONE).avatar();
        }
        final Cues.Heard boosting = Cues.of(flying, List.of(), PROFILE);
        for (final Cues.Hum hum : boosting.hums()) {
            if (hum.loop() == Sound.GATLING_SPIN || hum.loop() == Sound.GATLING_FIRE) {
                assertEquals(0, hum.level().gain(), 1e-9, "no gun drawn, no gun heard");
                continue;
            }
            if (hum.loop() == Sound.AIRSHIP_ENGINES) {
                assertEquals(0, hum.level().gain(), 1e-9, "no airship about, none heard");
                continue;
            }
            if (hum.loop() == Sound.ENGINE_V6 || hum.loop() == Sound.ENGINE_V8) {
                assertEquals(0, hum.level().gain(), 1e-9, "no vehicle driven, no engine heard");
                continue;
            }
            assertTrue(hum.level().gain() > 0, hum.loop() + " sounds while boosting");
            if (hum.loop() == Sound.WIND) {
                assertNull(hum.at(), "the wind is round the listener");
            } else {
                assertEquals(Cues.pack(flying.body()), hum.at(), hum.loop() + " at the pack");
            }
        }
    }
}
