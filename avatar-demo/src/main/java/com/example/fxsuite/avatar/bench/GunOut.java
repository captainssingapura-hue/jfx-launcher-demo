package com.example.fxsuite.avatar.bench;

import com.example.fxsuite.avatar.sound.Clip;
import com.example.fxsuite.avatar.sound.Gatling;
import com.example.fxsuite.avatar.sound.GatlingVoice;
import com.example.fxsuite.avatar.sound.GunStyle;
import com.example.fxsuite.avatar.sound.Wav;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 * The gun bench's sound: a {@link GatlingVoice} made on a thread of its own, a block at a time,
 * and fed to a Java Sound line a few milliseconds ahead of the ear, as the engine bench's is. The
 * bench says what the gun is to do, from its own thread, whenever it likes; the latest stretch of
 * sound is kept for it to draw. With no device to play on, or asked to be quiet, it goes on making
 * the sound in time all the same, so what is drawn still moves.
 */
final class GunOut implements AutoCloseable {

    /** Samples made at a time: about 12 ms. */
    private static final int BLOCK = 512;
    /** The line holds this many blocks: about 46 ms from a change to the ear. */
    private static final int AHEAD = 4;
    /** How much of the latest sound is kept to be drawn: about three quarters of a second. */
    private static final int KEPT = 1 << 15;

    private volatile Gatling gatling;
    private volatile GunStyle style;
    private volatile double spin;
    private volatile boolean firing;
    private volatile double height;
    private volatile double volume = 0.8;
    private volatile long shots;
    private volatile double heardSpin;
    private volatile boolean running = true;

    private final SourceDataLine line;
    private final String device;
    private final Thread thread;
    private final float[] kept = new float[KEPT];
    private long made;

    GunOut(final Gatling gatling, final GunStyle style, final boolean quiet) {
        this.gatling = gatling;
        this.style = style;
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
        thread = new Thread(this::run, "gun-bench-sound");
        thread.setDaemon(true);
        thread.start();
    }

    /** What it plays on. */
    String device() {
        return device;
    }

    /** Which gun; another starts afresh. */
    void gatling(final Gatling gatling) {
        this.gatling = gatling;
    }

    /** How it is mixed, from here. */
    void style(final GunStyle style) {
        this.style = style;
    }

    /** What the gun is to do: as {@link GatlingVoice#set}. */
    void set(final double spin, final boolean firing, final double height) {
        this.spin = spin;
        this.firing = firing;
        this.height = height;
    }

    void volume(final double volume) {
        this.volume = volume;
    }

    /** How many rounds the gun playing has fired; how fast its barrels turn, as heard. */
    long shots() {
        return shots;
    }

    double spin() {
        return heardSpin;
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
        Gatling playing = gatling;
        GatlingVoice voice = new GatlingVoice(playing, style, 1);
        final long blockNanos = Math.round(1e9 * BLOCK / Clip.RATE);
        long due = System.nanoTime();
        try {
            while (running) {
                if (gatling != playing) {
                    playing = gatling;
                    voice = new GatlingVoice(playing, style, 1);
                }
                if (style != voice.style()) {
                    voice.style(style);
                }
                voice.set(spin, firing, height);
                voice.render(block, 0, BLOCK);
                shots = voice.shots();
                heardSpin = voice.spin();
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
