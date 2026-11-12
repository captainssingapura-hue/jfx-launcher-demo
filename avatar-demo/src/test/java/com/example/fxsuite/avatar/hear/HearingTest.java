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
import com.example.fxsuite.avatar.sound.Profiles;
import com.example.fxsuite.avatar.sound.Sound;
import com.example.fxsuite.avatar.view.Chase;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

        @Override
        public void close() {
        }
    }

    private final Notebook speakers = new Notebook();
    private final Hearing hearing = new Hearing(speakers, List.of(), Profiles.ROBOT);
    private final Chase chase = Chase.behind(new Vec3(0, -160, 0), 30);

    private static Avatar stepping(final Event... events) {
        return new Avatar(Body.standing(Vec3.ZERO, 0), true, List.of(events));
    }

    @Test
    void theListenerIsAtTheAvatarFacingAsTheCameraDoes() {
        hearing.hear(stepping(), chase);
        assertEquals(chase.pivot(), speakers.ear);
        assertEquals(chase.yaw(), speakers.facing);
    }

    @Test
    void stepsInARowAreDifferentTakesMadeOnceAndKept() {
        for (int i = 0; i < 8; i++) {
            hearing.hear(stepping(new Event.Footfall(i % 2 == 0, Surface.GROUND)), chase);
        }
        final List<Clip> steps = speakers.played;
        assertEquals(8, steps.size());
        assertNotSame(steps.get(0), steps.get(1), "the next step is another take");
        assertSame(steps.get(0), steps.get(4), "four takes, then round again, made only once");
        assertTrue(steps.get(0).name().startsWith("step-ground"));
    }

    @Test
    void theLoopsStartOnceAndStartAgainInAnotherProfile() {
        hearing.hear(stepping(), chase);
        hearing.hear(stepping(), chase);
        final int loops = Sound.loops().size();
        assertEquals(loops, speakers.looped.size(), "one each, kept running");
        hearing.nextProfile();
        assertEquals(Profiles.ARCADE, hearing.profile());
        assertEquals(loops, speakers.stopped);
        hearing.hear(stepping(), chase);
        assertEquals(2 * loops, speakers.looped.size(), "started again, in arcade sounds");
        hearing.hear(stepping(new Event.PackOn()), chase);
        assertTrue(speakers.played.get(0).name().startsWith("pack-on"));
        hearing.nextProfile();
        assertEquals(Profiles.ROBOT, hearing.profile(), "and round to the first");
    }

    @Test
    void aBlastCarriesFarAndPushesTheLoopsDownForAMoment() {
        // flying, so the jets' loops are going
        Avatar flying = new Avatar(Body.standing(Vec3.ZERO, 0), true).toggledFlight().delivered();
        flying = flying.step(new Controls(0, 0, 0, 0, false), 0.5, Obstacles.NONE).delivered();
        hearing.hear(flying, chase);
        final int rumble = Sound.loops().indexOf(Sound.JET_RUMBLE);
        final double steady = speakers.gains.get(rumble)[0];
        assertTrue(steady > 0);

        final Block drone = new Block(0, 800, 65, 65, 0, 280, 320);
        hearing.hear(flying.reporting(List.of(new Event.Broke(1, Course.Kind.DRONE, drone, new Vec3(0, -300, 790),
                Weapon.GATLING))), chase);
        assertTrue(speakers.played.getLast().name().startsWith("break-drone"));
        assertEquals(Sound.BREAK_DRONE.reach(), speakers.near.getLast(), "heard at full out to 15 m");
        assertTrue(Sound.BREAK_DRONE.reach() > 800, "so in full from 8 m off");
        assertEquals(steady * (1 - Sound.BREAK_DRONE.duck()), speakers.gains.get(rumble)[0], 1e-9,
                "the jets pushed down under it");

        // and back up over the next second or two
        for (int i = 0; i < 4; i++) {
            flying = flying.step(new Controls(0, 0, 0, 0, false), 0.5, Obstacles.NONE).delivered();
            hearing.hear(flying, chase);
        }
        assertTrue(hearing.ducked() < 0.02, "back up: " + hearing.ducked());
    }

    @Test
    void aFootstepFadesAsItAlwaysHas() {
        hearing.hear(stepping(new Event.Footfall(true, Surface.GROUND)), chase);
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
}
