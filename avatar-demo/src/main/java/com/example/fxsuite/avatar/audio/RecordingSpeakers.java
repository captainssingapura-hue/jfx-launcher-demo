package com.example.fxsuite.avatar.audio;

import com.example.fxsuite.avatar.sim.Vec3;
import com.example.fxsuite.avatar.sound.Clip;
import com.example.fxsuite.avatar.sound.Wav;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.LongSupplier;

/**
 * Records what would have played, to a stereo WAV file, rather than playing it: for hearing an
 * unattended run afterwards, or checking one without ears. The mixing is the Java Sound
 * output's ({@link Mix}), so the file is what the speakers would have given.
 *
 * <p>It keeps time by the clock, as a sound card does: whenever it is called, it first mixes on
 * up to now, so each sound starts in the file when it was played. The game calls it every
 * frame, so the file runs as long as the game did. No thread of its own. The file is written as
 * it goes, and its sizes are filled in on {@link #close}; until then it is not a valid WAV.
 */
final class RecordingSpeakers implements Speakers {

    /** Frames mixed at a time, at most: catching up after a stall goes in steps of this. */
    private static final int BLOCK = 4096;

    private final Mix mix = new Mix();
    private final Path file;
    private final RandomAccessFile out;
    private final LongSupplier clock;
    private final long start;
    private final float[] mixed = new float[BLOCK * 2];
    private final byte[] bytes = new byte[BLOCK * 4];
    private long frames;
    private boolean closed;

    RecordingSpeakers(final Path file, final LongSupplier nanos) throws IOException {
        final Path folder = file.toAbsolutePath().getParent();
        if (folder != null) {
            Files.createDirectories(folder);
        }
        this.file = file;
        this.out = new RandomAccessFile(file.toFile(), "rw");
        out.setLength(0);
        out.write(Wav.header(0, 2));          // sizes filled in at the end
        this.clock = nanos;
        this.start = nanos.getAsLong();
    }

    static Speakers open(final Path file) throws IOException {
        return new RecordingSpeakers(file, System::nanoTime);
    }

    /** How long the recording runs so far, in seconds. */
    double seconds() {
        return (double) frames / Clip.RATE;
    }

    /** Mixes on to now, into the file. */
    private void catchUp() {
        if (closed) {
            return;
        }
        final long due = Math.round((clock.getAsLong() - start) / 1e9 * Clip.RATE);
        try {
            while (frames < due) {
                final int n = (int) Math.min(BLOCK, due - frames);
                mix.render(mixed, n);
                for (int i = 0; i < 2 * n; i++) {
                    final short s = Wav.toShort(mixed[i]);
                    bytes[2 * i] = (byte) s;
                    bytes[2 * i + 1] = (byte) (s >> 8);
                }
                out.write(bytes, 0, 4 * n);
                frames += n;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Kind kind() {
        return Kind.RECORD;
    }

    @Override
    public String device() {
        return file.toAbsolutePath().toString();
    }

    @Override
    public void listen(final Vec3 at, final double yaw) {
        catchUp();
        mix.listen(at, yaw);
    }

    @Override
    public void volume(final double volume) {
        catchUp();
        mix.volume(volume);
    }

    @Override
    public void play(final Clip clip, final double gain, final double pitch) {
        catchUp();
        mix.play(clip, gain, pitch, null);
    }

    @Override
    public void play(final Clip clip, final double gain, final double pitch, final Vec3 at) {
        catchUp();
        mix.play(clip, gain, pitch, at);
    }

    @Override
    public Loop loop(final Clip clip) {
        catchUp();
        final Mix.Voice voice = mix.loop(clip);
        // a loop's changes count from when they are made, too
        return new Loop() {
            @Override
            public void set(final double gain, final double pitch) {
                catchUp();
                voice.set(gain, pitch);
            }

            @Override
            public void place(final Vec3 at) {
                catchUp();
                voice.place(at);
            }

            @Override
            public void stop() {
                catchUp();
                voice.stop();
            }
        };
    }

    /** Mixes on to now, fills in the file's sizes, and closes it. */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        catchUp();
        closed = true;
        try (out) {
            out.seek(0);
            out.write(Wav.header((int) Math.min(Integer.MAX_VALUE - 36, frames * 4), 2));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        System.out.printf("recorded %.1f s of sound to %s%n", seconds(), device());
    }
}
