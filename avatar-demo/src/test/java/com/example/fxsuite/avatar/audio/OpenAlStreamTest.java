package com.example.fxsuite.avatar.audio;

import com.example.fxsuite.avatar.sim.Vec3;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.lwjgl.openal.AL10.AL_NO_ERROR;
import static org.lwjgl.openal.AL10.alGetError;

/**
 * A stream through OpenAL, on a real device, silent: kept fed as the frames go by, as much as it
 * plays, with OpenAL finding nothing wrong. Where OpenAL will not open here, there is nothing to try.
 */
class OpenAlStreamTest {

    @Test
    void aStreamIsKeptFedAsTheFramesGoBy() throws InterruptedException {
        Speakers opened = null;
        try {
            opened = OpenAlSpeakers.open();
        } catch (RuntimeException | LinkageError e) {
            assumeTrue(false, "no OpenAL here: " + e);
        }
        try (Speakers speakers = opened) {
            final int[] asked = {0};
            final Speakers.Stream stream = speakers.stream((into, count) -> {
                Arrays.fill(into, 0, count, 0);
                asked[0] += count;
            });
            stream.set(0);
            final int queued = asked[0];
            assertTrue(queued > 0, "some queued to begin with");
            // three quarters of a second of frames
            final long start = System.nanoTime();
            for (int frame = 0; frame < 45; frame++) {
                speakers.listen(Vec3.ZERO, 0);
                Thread.sleep(16);
            }
            final double seconds = (System.nanoTime() - start) / 1e9;
            final int fed = asked[0] - queued;
            assertTrue(fed > 0.6 * seconds * 44_100, "fed as it played: " + fed + " in " + seconds + " s");
            assertTrue(fed < 1.2 * seconds * 44_100 + queued, "and no faster");
            stream.stop();
            assertEquals(AL_NO_ERROR, alGetError(), "OpenAL found nothing wrong");
        }
    }
}
