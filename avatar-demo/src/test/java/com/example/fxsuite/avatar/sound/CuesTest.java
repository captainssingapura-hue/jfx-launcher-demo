package com.example.fxsuite.avatar.sound;

import com.example.fxsuite.avatar.sim.Arms;
import com.example.fxsuite.avatar.sim.Avatar;
import com.example.fxsuite.avatar.sim.Body;
import com.example.fxsuite.avatar.sim.Controls;
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

    /** An avatar at the origin facing +z, moving at {@code speed} on the ground, with these events. */
    private static Avatar with(final double speed, final boolean pack, final Event... events) {
        final Body body = new Body(Vec3.ZERO, 0, 0, speed, 0, 0, 0, false, true, 0, 0);
        return new Avatar(body, pack, List.of(events));
    }

    private static List<Sound> played(final Avatar avatar) {
        return Cues.of(avatar, PROFILE).plays().stream().map(Cues.Play::sound).toList();
    }

    @Test
    void eachFootIsHeardWhereItFallsAndOnWhat() {
        final List<Cues.Play> plays = Cues.of(with(Body.WALK, false,
                new Event.Footfall(true, Surface.GROUND), new Event.Footfall(false, Surface.BLOCK)), PROFILE).plays();
        assertEquals(Sound.STEP_GROUND, plays.get(0).sound());
        assertEquals(Sound.STEP_BLOCK, plays.get(1).sound());
        final Vec3 right = Num.right(0);
        assertTrue(plays.get(0).at().dotProduct(right) < 0, "the left foot, on the left");
        assertTrue(plays.get(1).at().dotProduct(right) > 0, "the right foot, on the right");
        final double walking = plays.get(0).amount();
        final double running = Cues.of(with(Body.RUN, false, new Event.Footfall(true, Surface.GROUND)), PROFILE)
                .plays().get(0).amount();
        assertTrue(running > walking, "running treads heavier: " + running + " > " + walking);
        assertEquals(1, running, 1e-9);
    }

    @Test
    void aLandingIsHeardAsHardAsItFellAndOnWhat() {
        final Cues.Play hop = Cues.of(with(0, false, new Event.Landed(Body.JUMP, false, Surface.GROUND)), PROFILE)
                .plays().get(0);
        final Cues.Play drop = Cues.of(with(0, false, new Event.Landed(4000, false, Surface.BLOCK)), PROFILE)
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
        final Cues.Play on = Cues.of(with(0, false, new Event.PackOn()), PROFILE).plays().get(0);
        assertTrue(on.at().dotProduct(Num.heading(0)) < 0, "behind");
        assertTrue(-on.at().y() > Body.TALL / 2, "up the back");
        final Cues.Play gate = Cues.of(with(0, true, new Event.PassedGate(Body.BOOST)), PROFILE).plays().get(0);
        assertEquals(Sound.GATE, gate.sound());
        assertNull(gate.at());
        assertEquals(1, gate.amount(), 1e-9);
        final Cues.Play bump = Cues.of(with(0, false, new Event.Bumped(Body.RUN)), PROFILE).plays().get(0);
        assertEquals(Sound.BUMP, bump.sound());
        assertEquals(Body.RUN / Body.BOOST, bump.amount(), 1e-9);
    }

    @Test
    void shotsSoundFromEachMuzzleAndStrokesFromTheBlade() {
        final Body still = Body.standing(Vec3.ZERO, 0);
        final Vec3 far = new Vec3(0, -130, 5000);
        final List<Cues.Play> shots = Cues.of(with(0, false, new Event.Fired(true, Arms.muzzle(still, true), far),
                new Event.Fired(false, Arms.muzzle(still, false), far)), PROFILE).plays();
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
        assertEquals(far, Cues.of(with(0, false, new Event.Struck(far, new Vec3(0, 0, -1), Surface.BLOCK,
                Weapon.RIFLES)), PROFILE).plays().get(0).at(), "heard where it struck");
        assertEquals(Arms.muzzle(still, true), shots.get(0).at(), "and a shot from its muzzle");
        assertEquals(List.of(Sound.GLAIVE_CLEAVE, Sound.GLAIVE_SWEEP),
                played(with(0, false, new Event.Swung(Arms.CLEAVE), new Event.Swung(Arms.SWEEP))));
        assertEquals(List.of(Sound.RIFLES_DRAWN, Sound.GLAIVE_DRAWN),
                played(with(0, false, new Event.Drew(Weapon.RIFLES), new Event.Drew(Weapon.GLAIVE),
                        new Event.Drew(Weapon.NONE))), "putting a weapon away is silent");
    }

    @Test
    void everyLoopIsSteeredEveryFrameTheJetsOnlyWhenFiring() {
        final Cues.Heard standing = Cues.of(with(0, true), PROFILE);
        assertEquals(Sound.loops(), standing.hums().stream().map(Cues.Hum::loop).toList());
        assertTrue(standing.hums().stream().allMatch(h -> h.level().gain() == 0), "quiet on the ground");
        assertTrue(standing.plays().isEmpty());

        Avatar flying = new Avatar(Body.standing(Vec3.ZERO, 0), true).toggledFlight().delivered();
        for (int i = 0; i < 90; i++) {
            flying = flying.step(new Controls(1, 0, 0, 0, true), 1.0 / 60, Obstacles.NONE).delivered();
        }
        final Cues.Heard boosting = Cues.of(flying, PROFILE);
        for (final Cues.Hum hum : boosting.hums()) {
            assertTrue(hum.level().gain() > 0, hum.loop() + " sounds while boosting");
            if (hum.loop() == Sound.WIND) {
                assertNull(hum.at(), "the wind is round the listener");
            } else {
                assertEquals(Cues.pack(flying.body()), hum.at(), hum.loop() + " at the pack");
            }
        }
    }
}
