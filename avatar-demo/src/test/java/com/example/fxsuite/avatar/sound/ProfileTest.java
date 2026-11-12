package com.example.fxsuite.avatar.sound;

import com.example.fxsuite.avatar.sim.Body;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What every profile promises, held to each of them: every sound made, audible, within full
 * scale, click-free at its ends, repeatable, harder for more, and the loops driven sensibly.
 * A new profile is tested by adding it to {@link Profiles}.
 */
class ProfileTest {

    static List<Profile> profiles() {
        return Profiles.all();
    }

    @ParameterizedTest
    @MethodSource("profiles")
    void everySoundIsAudibleAndWithinFullScale(final Profile profile) {
        for (final Sound sound : Sound.values()) {
            for (final double amount : new double[] {0, 0.5, 1}) {
                final Clip clip = profile.render(sound, amount, 3);
                assertTrue(clip.length() > 0, profile.id() + " " + sound + " has samples");
                assertTrue(clip.peak() > 0.05 && clip.peak() <= 1, profile.id() + " " + sound + " peak " + clip.peak());
                for (int i = 0; i < clip.length(); i++) {
                    assertFalse(Float.isNaN(clip.sample(i)), profile.id() + " " + sound + " sample " + i);
                }
                assertEquals(sound.loop(), clip.loops(), profile.id() + " " + sound + " loops as catalogued");
            }
        }
    }

    @ParameterizedTest
    @MethodSource("profiles")
    void oneShotsStartAndEndInSilence(final Profile profile) {
        for (final Sound sound : Sound.oneShots()) {
            final Clip clip = profile.render(sound, 1, 0);
            assertTrue(Math.abs(clip.sample(0)) < 0.02, profile.id() + " " + sound + " starts at " + clip.sample(0));
            assertEquals(0, clip.sample(clip.length() - 1), 1e-6, profile.id() + " " + sound + " fades out to nothing");
        }
    }

    @ParameterizedTest
    @MethodSource("profiles")
    void loopsRunOnIntoTheirStartWithoutAJump(final Profile profile) {
        for (final Sound sound : Sound.loops()) {
            final Clip clip = profile.render(sound, 1, 0);
            double biggest = 0;
            for (int i = 1; i < clip.length(); i++) {
                biggest = Math.max(biggest, Math.abs(clip.sample(i) - clip.sample(i - 1)));
            }
            final double wrap = Math.abs(clip.sample(0) - clip.sample(clip.length() - 1));
            assertTrue(wrap <= biggest * 1.01 + 1e-6,
                    profile.id() + " " + sound + ": the seam " + wrap + " is no bigger than any step " + biggest);
        }
    }

    @ParameterizedTest
    @MethodSource("profiles")
    void moreIsLouderAndHarderLandingsLonger(final Profile profile) {
        for (final Sound sound : Sound.oneShots()) {
            if (sound.amount() != null) {
                assertTrue(profile.render(sound, 1, 0).peak() > profile.render(sound, 0.1, 0).peak(),
                        profile.id() + " " + sound);
            }
        }
        assertTrue(profile.render(Sound.LANDING, 1, 0).seconds() > profile.render(Sound.LANDING, 0.2, 0).seconds());
    }

    @ParameterizedTest
    @MethodSource("profiles")
    void theSameTakeIsTheSameAndAnotherDiffers(final Profile profile) {
        for (final Sound sound : Sound.oneShots()) {
            final Clip one = profile.render(sound, 0.5, 4);
            final Clip again = profile.render(sound, 0.5, 4);
            final Clip other = profile.render(sound, 0.5, 5);
            assertArrayEquals(one.samples, again.samples, profile.id() + " " + sound + " is repeatable");
            assertFalse(Arrays.equals(one.samples, other.samples), profile.id() + " " + sound + " takes differ");
        }
    }

    @ParameterizedTest
    @MethodSource("profiles")
    void aBlockSoundsHarderAndBrighterThanTheGround(final Profile profile) {
        final Clip ground = profile.render(Sound.STEP_GROUND, 0.5, 0);
        final Clip block = profile.render(Sound.STEP_BLOCK, 0.5, 0);
        assertTrue(brightness(block) > brightness(ground),
                profile.id() + ": block " + brightness(block) + ", ground " + brightness(ground));
        final Clip landGround = profile.render(Sound.LANDING, 0.5, 0);
        final Clip landBlock = profile.render(Sound.LANDING_BLOCK, 0.5, 0);
        assertTrue(brightness(landBlock) > brightness(landGround),
                profile.id() + " landing: block " + brightness(landBlock) + ", ground " + brightness(landGround));
    }

    @ParameterizedTest
    @MethodSource("profiles")
    void theJetsAreSilentUnlessFiringAndRiseWithThrust(final Profile profile) {
        final List<Sound> jets = List.of(Sound.JET_RUMBLE, Sound.JET_HISS, Sound.JET_WHINE);
        for (final Sound jet : jets) {
            assertEquals(Level.OFF, profile.level(jet, 0, Body.FLY), profile.id() + " " + jet + " off");
            Level was = profile.level(jet, 0.2, 0);
            for (double t = 0.3; t <= 1.0001; t += 0.1) {
                final Level now = profile.level(jet, t, 0);
                assertTrue(now.gain() >= was.gain() && now.pitch() >= was.pitch() && now.gain() <= 1,
                        profile.id() + " " + jet + " at " + t);
                was = now;
            }
        }
        assertTrue(profile.level(Sound.JET_RUMBLE, 1, 0).gain() > profile.level(Sound.JET_RUMBLE, 0.2, 0).gain());
        assertTrue(profile.level(Sound.JET_HISS, 1, 0).gain() > profile.level(Sound.JET_HISS, 0.2, 0).gain());
        assertTrue(profile.level(Sound.JET_WHINE, 1, 0).pitch() > profile.level(Sound.JET_WHINE, 0.2, 0).pitch());
    }

    @ParameterizedTest
    @MethodSource("profiles")
    void theGatlingGunsMotorFollowsItsSpinAndItsRoarItsFiring(final Profile profile) {
        final Profile.Drive still = new Profile.Drive(0, 0, 0, false);
        assertEquals(Level.OFF, profile.level(Sound.GATLING_SPIN, still), "still, silent");
        assertEquals(Level.OFF, profile.level(Sound.GATLING_FIRE, still));
        final Level slow = profile.level(Sound.GATLING_SPIN, new Profile.Drive(0, 0, 0.3, false));
        final Level fast = profile.level(Sound.GATLING_SPIN, new Profile.Drive(0, 0, 1, false));
        assertTrue(fast.pitch() > slow.pitch() && fast.gain() >= slow.gain(), "the motor whines up as it spins up");
        assertEquals(Level.OFF, profile.level(Sound.GATLING_FIRE, new Profile.Drive(0, 0, 1, false)),
                "spinning but not firing: no roar");
        assertTrue(profile.level(Sound.GATLING_FIRE, new Profile.Drive(0, 0, 1, true)).gain() > 0, "firing: roaring");
        assertEquals(Level.OFF, profile.level(Sound.JET_RUMBLE, new Profile.Drive(0, 0, 1, true)),
                "and the jets are not the gun");
    }

    @ParameterizedTest
    @MethodSource("profiles")
    void theWindRisesWithSpeedFromNothingAtAWalk(final Profile profile) {
        assertEquals(Level.OFF, profile.level(Sound.WIND, 1, 0));
        assertEquals(0, profile.level(Sound.WIND, 0, Body.WALK).gain(), "no wind walking");
        assertTrue(profile.level(Sound.WIND, 0, Body.FLY).gain() > 0);
        assertTrue(profile.level(Sound.WIND, 0, Body.BOOST).gain() > profile.level(Sound.WIND, 0, Body.FLY).gain());
        assertTrue(profile.level(Sound.WIND, 0, 5 * Body.BOOST).gain() <= 1);
    }

    @Test
    void theProfilesSoundDifferentAndAreFoundByName() {
        for (final Sound sound : Sound.values()) {
            assertFalse(Arrays.equals(Profiles.ROBOT.render(sound, 0.5, 0).samples,
                    Profiles.ARCADE.render(sound, 0.5, 0).samples), sound + " differs");
        }
        assertEquals(Profiles.all().size(), Profiles.all().stream().map(Profile::id).distinct().count(), "unique ids");
        assertEquals(Profiles.ARCADE, Profiles.named(" Arcade "));
        assertEquals(Profiles.ROBOT, Profiles.named("no such"));
        assertEquals(Profiles.ROBOT, Profiles.named(null));
    }

    /** How much of a clip changes fast: the RMS of its differences over its own RMS. */
    private static double brightness(final Clip clip) {
        double change = 0;
        for (int i = 1; i < clip.length(); i++) {
            final double d = clip.sample(i) - clip.sample(i - 1);
            change += d * d;
        }
        return Math.sqrt(change / clip.length()) / clip.rms();
    }
}
