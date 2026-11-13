package com.example.fxsuite.avatar.bench;

import com.example.fxsuite.avatar.sound.Clip;
import com.example.fxsuite.avatar.sound.Engine;
import com.example.fxsuite.avatar.sound.EngineVoice;
import com.example.fxsuite.avatar.sound.Wav;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 * The engine bench's sound: an {@link EngineVoice} made on a thread of its own, a block at a
 * time, and fed to a Java Sound line a few milliseconds ahead of the ear. The bench says what
 * the engine is to do, from its own thread, whenever it likes; the latest stretch of sound is
 * kept for it to draw. With no device to play on, or asked to be quiet, it goes on making the
 * sound in time all the same, so what is drawn still moves.
 */
final class EngineOut implements AutoCloseable {

    /** Samples made at a time: about 12 ms. */
    private static final int BLOCK = 512;
    /** The line holds this many blocks: about 46 ms from a change to the ear. */
    private static final int AHEAD = 4;
    /** How much of the latest sound is kept to be drawn: about three quarters of a second. */
    private static final int KEPT = 1 << 15;

    private volatile Engine engine;
    private volatile double rpm;
    private volatile double throttle;
    private volatile double load;
    private volatile double volume = 0.8;
    private volatile double boost;
    private volatile boolean running = true;

    private final SourceDataLine line;
    private final String device;
    private final Thread thread;
    private final float[] kept = new float[KEPT];
    private long made;

    EngineOut(final Engine engine, final boolean quiet) {
        this.engine = engine;
        SourceDataLine opened = null;
        String name = "none: drawing only";
        if (!quiet) {
            try {
                final AudioFormat format = new AudioFormat(Clip.RATE, 16, 1, true, false);
                opened = AudioSystem.getSourceDataLine(format);
                opened.open(format, BLOCK * 2 * AHEAD);
                opened.start();
                name = "Java Sound";
            } catch (LineUnavailableException | IllegalArgumentException e) {
                opened = null;
                name = "none (" + e.getMessage() + "): drawing only";
            }
        }
        line = opened;
        device = name;
        thread = new Thread(this::run, "engine-bench-sound");
        thread.setDaemon(true);
        thread.start();
    }

    /** What it plays on. */
    String device() {
        return device;
    }

    /** Which engine; a new one starts afresh. */
    void engine(final Engine engine) {
        this.engine = engine;
    }

    /** What the engine is to do: as {@link EngineVoice#set}. */
    void set(final double rpm, final double throttle, final double load) {
        this.rpm = rpm;
        this.throttle = throttle;
        this.load = load;
    }

    /** How much boost the engine playing makes, its turbocharger spooled up as it plays. */
    double boost() {
        return boost;
    }

    void volume(final double volume) {
        this.volume = volume;
    }

    /** The latest {@code into.length} samples made, oldest first. */
    void latest(final float[] into) {
        synchronized (kept) {
            final int n = Math.min(into.length, KEPT);
            for (int i = 0; i < n; i++) {
                into[i] = kept[(int) ((made - n + i) & (KEPT - 1))];
            }
        }
    }

    private void run() {
        final float[] block = new float[BLOCK];
        final byte[] bytes = new byte[BLOCK * 2];
        Engine playing = engine;
        EngineVoice voice = new EngineVoice(playing, 1);
        final long blockNanos = Math.round(1e9 * BLOCK / Clip.RATE);
        long due = System.nanoTime();
        try {
            while (running) {
                if (engine != playing) {
                    playing = engine;
                    voice = new EngineVoice(playing, 1);
                }
                voice.set(rpm, throttle, load);
                voice.render(block, 0, BLOCK);
                boost = voice.boost();
                synchronized (kept) {
                    for (final float s : block) {
                        kept[(int) (made & (KEPT - 1))] = s;
                        made++;
                    }
                }
                if (line != null) {
                    final double gain = volume;
                    for (int i = 0; i < BLOCK; i++) {
                        final short s = Wav.toShort((float) (block[i] * gain));
                        bytes[2 * i] = (byte) s;
                        bytes[2 * i + 1] = (byte) (s >> 8);
                    }
                    line.write(bytes, 0, bytes.length);     // waits while the line is full
                } else {
                    due += blockNanos;
                    final long wait = due - System.nanoTime();
                    if (wait > 0) {
                        Thread.sleep(wait / 1_000_000, (int) (wait % 1_000_000));
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException e) {
            // the device went away: from here on, silence
            running = false;
        }
    }

    @Override
    public void close() {
        running = false;
        try {
            thread.join(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (line != null) {
            line.stop();
            line.flush();
            line.close();
        }
    }
}
