package com.example.fxsuite.avatar.hear;

import com.example.fxsuite.avatar.audio.Space;
import com.example.fxsuite.avatar.audio.Speakers;
import com.example.fxsuite.avatar.sim.Avatar;
import com.example.fxsuite.avatar.sim.Block;
import com.example.fxsuite.avatar.sim.Body;
import com.example.fxsuite.avatar.sim.Controls;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Event;
import com.example.fxsuite.avatar.sim.Obstacles;
import com.example.fxsuite.avatar.sim.Surface;
import com.example.fxsuite.avatar.sim.Vec3;
import com.example.fxsuite.avatar.sim.Weapon;
import com.example.fxsuite.avatar.sound.Clip;
import com.example.fxsuite.avatar.sound.Cues;
import com.example.fxsuite.avatar.sound.Engine;
import com.example.fxsuite.avatar.sound.GunStyle;
import com.example.fxsuite.avatar.sound.Profiles;
import com.example.fxsuite.avatar.sound.Sound;
import com.example.fxsuite.avatar.view.Chase;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The game's sound, played on speakers that only take notes. */
class HearingTest {

    /** Speakers that write down what they are asked to do. */
    private static final class Notebook implements Speakers {
        final List<Clip> played = new ArrayList<>();
        final List<Vec3> at = new ArrayList<>();
        final List<Double> near = new ArrayList<>();
        final List<Clip> looped = new ArrayList<>();
        /** Each loop's gain, as last set, in the order they were started. */
        final List<double[]> gains = new ArrayList<>();
        int stopped;
        double volume = -1;
        Vec3 ear;
        double facing;

        @Override
        public Kind kind() {
            return Kind.SILENT;
        }

        @Override
        public String device() {
            return "notebook";
        }

        @Override
        public void listen(final Vec3 at, final double yaw) {
            ear = at;
            facing = yaw;
        }

        @Override
        public void volume(final double volume) {
            this.volume = volume;
        }

        @Override
        public void play(final Clip clip, final double gain, final double pitch) {
            play(clip, gain, pitch, null);
        }

        @Override
        public void play(final Clip clip, final double gain, final double pitch, final Vec3 at, final double near) {
            played.add(clip);
            this.at.add(at);
            this.near.add(near);
        }

        @Override
        public Loop loop(final Clip clip) {
            looped.add(clip);
            final double[] gain = {0};
            gains.add(gain);
            return new Loop() {
                @Override
                public void set(final double level, final double pitch) {
                    gain[0] = level;
                }

                @Override
                public void place(final Vec3 at) {
                }

                @Override
                public void stop() {
                    stopped++;
                }
            };
        }

        /** Each stream started: what makes it, and its gain and place as last set, and whether stopped. */
        static final class Streamed {
            Source source;
            double gain;
            Vec3 at;
            boolean stopped;
        }

        final List<Streamed> streams = new ArrayList<>();

        @Override
        public Stream stream(final Source source) {
            final Streamed streamed = new Streamed();
            streamed.source = source;
            streams.add(streamed);
            return new Stream() {
                @Override
                public void set(final double gain) {
                    streamed.gain = gain;
                }

                @Override
                public void place(final Vec3 at) {
                    streamed.at = at;
                }

                @Override
                public void stop() {
                    streamed.stopped = true;
                }
            };
        }

        @Override
        public void close() {
        }
    }

    private final Notebook speakers = new Notebook();
    private final Hearing hearing = new Hearing(speakers, List.of(), Profiles.ROBOT);
    private final Chase chase = Chase.behind(new Vec3(0, -160, 0), 30);

    /** Standing still, wearing the pack. */
    private static final Avatar STANDING = new Avatar(Body.standing(Vec3.ZERO, 0), true);

    @Test
    void theListenerIsAtTheAvatarFacingAsTheCameraDoes() {
        hearing.hear(STANDING, List.of(), chase);
        assertEquals(chase.pivot(), speakers.ear);
        assertEquals(chase.yaw(), speakers.facing);
    }

    @Test
    void stepsInARowAreDifferentTakesMadeOnceAndKept() {
        for (int i = 0; i < 8; i++) {
            hearing.hear(STANDING, List.of(new Event.Footfall(i % 2 == 0, Surface.GROUND)), chase);
        }
        final List<Clip> steps = speakers.played;
        assertEquals(8, steps.size());
        assertNotSame(steps.get(0), steps.get(1), "the next step is another take");
        assertSame(steps.get(0), steps.get(4), "four takes, then round again, made only once");
        assertTrue(steps.get(0).name().startsWith("step-ground"));
    }

    @Test
    void theLoopsStartOnceAndStartAgainInAnotherProfile() {
        hearing.hear(STANDING, List.of(), chase);
        hearing.hear(STANDING, List.of(), chase);
        final int loops = Sound.loops().size();
        assertEquals(loops, speakers.looped.size(), "one each, kept running");
        hearing.nextProfile();
        assertEquals(Profiles.ARCADE, hearing.profile());
        assertEquals(loops, speakers.stopped);
        hearing.hear(STANDING, List.of(), chase);
        assertEquals(2 * loops, speakers.looped.size(), "started again, in arcade sounds");
        hearing.hear(STANDING, List.of(new Event.PackOn()), chase);
        assertTrue(speakers.played.get(0).name().startsWith("pack-on"));
        hearing.nextProfile();
        assertEquals(Profiles.ROBOT, hearing.profile(), "and round to the first");
    }

    @Test
    void aBlastCarriesFarAndPushesTheLoopsDownForAMoment() {
        // flying, so the jets' loops are going
        Avatar flying = new Avatar(Body.standing(Vec3.ZERO, 0), true).toggledFlight();
        flying = flying.step(new Controls(0, 0, 0, 0, false), 0.5, Obstacles.NONE).avatar();
        hearing.hear(flying, List.of(), chase);
        final int rumble = Sound.loops().indexOf(Sound.JET_RUMBLE);
        final double steady = speakers.gains.get(rumble)[0];
        assertTrue(steady > 0);

        final Block drone = new Block(0, 800, 65, 65, 0, 280, 320);
        hearing.hear(flying, List.of(new Event.Broke(1, Course.Kind.DRONE, drone, new Vec3(0, -300, 790),
                Weapon.GATLING)), chase);
        assertTrue(speakers.played.getLast().name().startsWith("break-drone"));
        assertEquals(Sound.BREAK_DRONE.reach(), speakers.near.getLast(), "heard at full out to 15 m");
        assertTrue(Sound.BREAK_DRONE.reach() > 800, "so in full from 8 m off");
        assertEquals(steady * (1 - Sound.BREAK_DRONE.duck()), speakers.gains.get(rumble)[0], 1e-9,
                "the jets pushed down under it");

        // and back up over the next second or two
        for (int i = 0; i < 4; i++) {
            flying = flying.step(new Controls(0, 0, 0, 0, false), 0.5, Obstacles.NONE).avatar();
            hearing.hear(flying, List.of(), chase);
        }
        assertTrue(hearing.ducked() < 0.02, "back up: " + hearing.ducked());
    }

    @Test
    void aFootstepFadesAsItAlwaysHas() {
        hearing.hear(STANDING, List.of(new Event.Footfall(true, Surface.GROUND)), chase);
        assertEquals(Space.NEAR, speakers.near.getLast());
        assertEquals(0, hearing.ducked(), "and pushes nothing down");
    }

    @Test
    void muteSilencesAndRestores() {
        final double normal = speakers.volume;
        assertTrue(normal > 0);
        hearing.toggleMute();
        assertEquals(0, speakers.volume);
        assertTrue(hearing.muted());
        hearing.toggleMute();
        assertEquals(normal, speakers.volume);
    }

    private static final Vec3 BONNET = new Vec3(0, -20, 200);

    private void driving(final Cues.Motor motor) {
        hearing.hear(STANDING, List.of(), chase, List.of(), null, motor);
    }

    /** How loud the loops made from clips named {@code start...} are now, all told. */
    private double looping(final String start) {
        double gain = 0;
        for (int i = 0; i < speakers.looped.size(); i++) {
            if (speakers.looped.get(i).name().startsWith(start)) {
                gain += speakers.gains.get(i)[0];
            }
        }
        return gain;
    }

    @Test
    void aDrivenEngineIsMadeAsItRunsFromUnderItsBonnet() {
        driving(new Cues.Motor(Engine.V8, 3000, 1, 0.5, BONNET));
        driving(new Cues.Motor(Engine.V8, 3200, 1, 0.5, BONNET));
        assertEquals(1, speakers.streams.size(), "one engine, one stream, kept");
        final Notebook.Streamed stream = speakers.streams.getFirst();
        assertFalse(stream.stopped);
        assertEquals(BONNET, stream.at);
        assertTrue(stream.gain > 0.5);
        final float[] made = new float[Clip.RATE / 4];
        stream.source.fill(made, made.length);
        double sum = 0;
        for (final float s : made) {
            sum += s * s;
        }
        assertTrue(Math.sqrt(sum / made.length) > 0.02, "and it is heard");
        assertEquals(0, looping("engine"), "not the engine loops as well");
    }

    @Test
    void anotherEngineStartsAnotherStreamAndGettingOutStopsIt() {
        driving(new Cues.Motor(Engine.V8, 3000, 1, 0.5, BONNET));
        driving(new Cues.Motor(Engine.V6, 3000, 1, 0.5, BONNET));
        assertEquals(2, speakers.streams.size());
        assertTrue(speakers.streams.get(0).stopped, "the V8 stopped");
        assertFalse(speakers.streams.get(1).stopped, "the V6 running");
        driving(null);
        assertTrue(speakers.streams.get(1).stopped, "out of the vehicle, silent");
        driving(null);
        assertEquals(2, speakers.streams.size(), "and nothing started");
    }

    @Test
    void theArcadeLoopsItsEnginesAsEverAndChangingProfileStopsTheStream() {
        driving(new Cues.Motor(Engine.V8, 3000, 1, 0.5, BONNET));
        hearing.nextProfile();
        assertTrue(speakers.streams.getFirst().stopped, "the engine made as it runs, stopped");
        driving(new Cues.Motor(Engine.V8, 3000, 1, 0.5, BONNET));
        assertEquals(1, speakers.streams.size(), "the Arcade makes no engine so");
        assertTrue(looping("engine-v8") > 0, "it loops its chip's V8");
        assertEquals(0, looping("engine-v6"));
    }

    private static final Vec3 MUZZLE = new Vec3(0, -120, 80);

    /** Standing still, {@code time} seconds in: how long has passed is what lets a gun ring out. */
    private static Avatar at(final double time) {
        return new Avatar(new Body(Vec3.ZERO, 0, 0, 0, 0, 0, 0, false, true, 0, time), false);
    }

    private void gunning(final double time, final Cues.Gunner gunner) {
        hearing.hear(at(time), List.of(), chase, List.of(), gunner);
    }

    @Test
    void aGunMannedIsMadeAsItFiresFromItsMuzzleAndRingsOutOnceLetGo() {
        final Cues.Gunner firing = new Cues.Gunner(1, true, MUZZLE, 1, Weapon.HEAVY_GATLING);
        gunning(0, firing);
        gunning(0.1, firing);
        assertEquals(1, speakers.streams.size(), "one gun, one stream, kept");
        final Notebook.Streamed stream = speakers.streams.getFirst();
        assertEquals(MUZZLE, stream.at);
        assertTrue(stream.gain > 0.5);
        final float[] made = new float[Clip.RATE / 2];
        stream.source.fill(made, made.length);
        double sum = 0;
        for (final float s : made) {
            sum += s * s;
        }
        assertTrue(Math.sqrt(sum / made.length) > 0.02, "firing, heard");
        assertEquals(0, looping("gatling"), "not its loops as well");
        // let go, it runs down and rings out before it stops
        gunning(1, null);
        assertFalse(stream.stopped, "still ringing out");
        gunning(6, null);
        assertTrue(stream.stopped, "rung out, stopped");
        // another gun, another stream
        gunning(6.1, new Cues.Gunner(1, true, MUZZLE, 0.55, Weapon.GIANT_GATLING));
        assertEquals(2, speakers.streams.size());
    }

    @Test
    void theGunsMixIsChosenAndTheGunFiringTakesItAtOnce() {
        hearing.gunStyle(GunStyle.REALISTIC);
        assertEquals(GunStyle.REALISTIC, hearing.gunStyle());
        gunning(0, new Cues.Gunner(1, true, MUZZLE, 1, Weapon.HEAVY_GATLING));
        hearing.gunStyle(GunStyle.CINEMATIC);
        assertEquals(GunStyle.CINEMATIC, hearing.gunStyle());
        assertTrue(hearing.gunHeard());
        // and the Arcade loops its gun, as ever
        hearing.nextProfile();
        assertTrue(speakers.streams.getFirst().stopped);
        gunning(0.1, new Cues.Gunner(1, true, MUZZLE, 1, Weapon.HEAVY_GATLING));
        assertFalse(hearing.gunHeard());
        assertTrue(looping("gatling") > 0, "its chip's gun");
    }
}
