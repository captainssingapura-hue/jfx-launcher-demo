package com.example.fxsuite.avatar.audio;

import com.example.fxsuite.avatar.sim.Vec3;
import com.example.fxsuite.avatar.sound.Clip;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The recording output, on a clock the test moves by hand. */
class RecordingSpeakersTest {

    private static final long SECOND = 1_000_000_000L;

    private final AtomicLong now = new AtomicLong(5 * SECOND);

    private static Clip steady(final double seconds, final boolean loops) {
        final float[] samples = new float[(int) Math.round(seconds * Clip.RATE)];
        Arrays.fill(samples, 0.5f);
        return new Clip("steady", samples, loops);
    }

    /** The recording's frames, as left and right in turn, from its file. */
    private static short[] frames(final Path file) throws IOException {
        final ByteBuffer wav = ByteBuffer.wrap(Files.readAllBytes(file)).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(2, wav.getShort(22), "stereo");
        assertEquals(wav.capacity() - 44, wav.getInt(40), "the data's size filled in");
        assertEquals(wav.capacity() - 8, wav.getInt(4), "the file's size filled in");
        final short[] out = new short[(wav.capacity() - 44) / 2];
        wav.position(44).asShortBuffer().get(out);
        return out;
    }

    @Test
    void soundsLandInTheFileWhenTheyWerePlayed(@TempDir final Path folder) throws IOException {
        final Path file = folder.resolve("run.wav");
        final RecordingSpeakers recording = new RecordingSpeakers(file, now::get);
        now.addAndGet(SECOND / 2);
        recording.play(steady(0.25, false), 1, 1);
        now.addAndGet(SECOND / 2);
        recording.close();

        final short[] frames = frames(file);
        assertEquals(Clip.RATE * 2, frames.length, "a second, as long as the clock ran");
        final int half = Clip.RATE / 2;
        for (int f = 0; f < half; f++) {
            assertEquals(0, frames[2 * f], "silent before the sound, at " + f);
        }
        assertTrue(frames[2 * half] > 0 && frames[2 * half] == frames[2 * half + 1], "then in both ears");
        assertEquals(0, frames[2 * (half + Clip.RATE / 4 + 10)], "and silent once it is over");
    }

    @Test
    void placesAndLoopsAreMixedAsJavaSoundWould(@TempDir final Path folder) throws IOException {
        final Path file = folder.resolve("loop.wav");
        final RecordingSpeakers recording = new RecordingSpeakers(file, now::get);
        recording.listen(Vec3.ZERO, 0);
        final Speakers.Loop hum = recording.loop(steady(0.1, true));
        hum.set(1, 1);
        hum.place(new Vec3(200, 0, 0));
        now.addAndGet(SECOND);
        recording.close();
        final short[] frames = frames(file);
        final int late = Clip.RATE / 2;
        assertEquals(0, frames[2 * late], 1, "on the right: nothing on the left");
        assertTrue(frames[2 * late + 1] > 10_000, "and loud on the right, still looping a second on");
    }

    @Test
    void closingTwiceIsHarmlessAndTheKindIsNeverAFallBack(@TempDir final Path folder) throws IOException {
        final RecordingSpeakers recording = new RecordingSpeakers(folder.resolve("x.wav"), now::get);
        recording.close();
        recording.close();
        assertEquals(Speakers.Kind.RECORD, recording.kind());
        assertFalse(Outputs.order(Speakers.Kind.OPENAL).contains(Speakers.Kind.RECORD));
        assertEquals(Speakers.Kind.RECORD, Outputs.order(Speakers.Kind.RECORD).get(0), "only when asked for");
        final String was = System.getProperty("avatar.audio");
        try {
            System.setProperty("avatar.audio", "record");
            assertEquals(Speakers.Kind.RECORD, Speakers.Kind.fromSettings());
        } finally {
            if (was == null) {
                System.clearProperty("avatar.audio");
            } else {
                System.setProperty("avatar.audio", was);
            }
        }
    }
}
