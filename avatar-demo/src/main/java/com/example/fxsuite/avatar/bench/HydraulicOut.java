package com.example.fxsuite.avatar.bench;

import com.example.fxsuite.avatar.sim.Actuator;
import com.example.fxsuite.avatar.sound.Clip;
import com.example.fxsuite.avatar.sound.HydraulicVoice;
import com.example.fxsuite.avatar.sound.Wav;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * The aircraft bench's hinges driven, and heard: each control's {@link Actuator}, stepped on a thread
 * of its own a block of sound at a time, and its {@link HydraulicVoice} made from it, fed to a Java
 * Sound line a few milliseconds ahead of the ear, as the gun bench's gun is. So what moves and what
 * is heard keep exactly in time; the bench reads where each hinge is ({@link #angle}) to draw it.
 * Asked from the bench's thread whenever it likes; with no device to play on, or asked to be quiet,
 * it goes on in time all the same, so what is drawn still moves.
 */
final class HydraulicOut implements AutoCloseable {

    /** Samples made at a time: about 12 ms. */
    private static final int BLOCK = 512;
    /** The line holds this many blocks: about 46 ms from a change to the ear. */
    private static final int AHEAD = 4;

    private final ConcurrentLinkedQueue<Runnable> asked = new ConcurrentLinkedQueue<>();
    private final Map<String, Actuator> drives = new LinkedHashMap<>();
    private final Map<String, HydraulicVoice> voices = new LinkedHashMap<>();
    private final Map<String, Double> angles = new ConcurrentHashMap<>();
    private final Map<String, Actuator.Stage> stages = new ConcurrentHashMap<>();
    private volatile double volume = 0.8;
    private volatile boolean running = true;
    private final SourceDataLine line;
    private final Thread thread;

    HydraulicOut(final boolean quiet) {
        SourceDataLine opened = null;
        if (!quiet) {
            try {
                final AudioFormat format = new AudioFormat(Clip.RATE, 16, 1, true, false);
                opened = AudioSystem.getSourceDataLine(format);
                opened.open(format, BLOCK * 2 * AHEAD);
                opened.start();
            } catch (LineUnavailableException | IllegalArgumentException e) {
                opened = null;
            }
        }
        line = opened;
        thread = new Thread(this::run, "hinge-bench-sound");
        thread.setDaemon(true);
        thread.start();
    }

    /** The hinges driven: each control, from {@code least} to {@code most}, standing at {@code at}, at {@code rate}; any others let go. */
    void hinges(final Map<String, double[]> controls, final Map<String, Double> at, final double rate) {
        final Map<String, Actuator> made = new LinkedHashMap<>();
        controls.forEach((control, range) -> {
            final double angle = Math.clamp(at.getOrDefault(control, range[0]), range[0], range[1]);
            made.put(control, Actuator.shut(Actuator.Drive.HYDRAULIC, range[0], range[1], rate).placed(angle));
            angles.put(control, angle);
            stages.put(control, made.get(control).stage());
        });
        asked.add(() -> {
            drives.clear();
            drives.putAll(made);
            voices.keySet().retainAll(made.keySet());
            made.keySet().forEach(control -> voices.computeIfAbsent(control, c -> new HydraulicVoice(c.hashCode())));
        });
        angles.keySet().retainAll(controls.keySet());
    }

    /** {@code control}'s hinge asked to open, or shut. */
    void ask(final String control, final boolean open) {
        asked.add(() -> drives.computeIfPresent(control, (c, drive) -> drive.asked(open)));
    }

    /** {@code control}'s hinge put at {@code angle} by hand, still there. */
    void place(final String control, final double angle) {
        angles.put(control, angle);
        asked.add(() -> drives.computeIfPresent(control, (c, drive) -> drive.placed(angle)));
    }

    /** Every hinge driven at {@code rate} degrees a second at most. */
    void rate(final double rate) {
        asked.add(() -> drives.replaceAll((c, drive) -> drive.rate(rate)));
    }

    void volume(final double volume) {
        this.volume = volume;
    }

    /** Where {@code control}'s hinge is, degrees: as last heard; or {@code otherwise}, for none. */
    double angle(final String control, final double otherwise) {
        return angles.getOrDefault(control, otherwise);
    }

    /** What {@code control}'s hinge is doing; shut, for none. */
    Actuator.Stage stage(final String control) {
        return stages.getOrDefault(control, Actuator.Stage.SHUT);
    }

    private void run() {
        final float[] block = new float[BLOCK];
        final float[] mixed = new float[BLOCK];
        final byte[] bytes = new byte[BLOCK * 2];
        final double seconds = (double) BLOCK / Clip.RATE;
        final long blockNanos = Math.round(1e9 * BLOCK / Clip.RATE);
        long due = System.nanoTime();
        try {
            while (running) {
                for (Runnable ask = asked.poll(); ask != null; ask = asked.poll()) {
                    ask.run();
                }
                java.util.Arrays.fill(mixed, 0);
                for (final Map.Entry<String, Actuator> drive : drives.entrySet()) {
                    final Actuator.Stepped stepped = drive.getValue().step(seconds);
                    final Actuator now = stepped.actuator();
                    drive.setValue(now);
                    angles.put(drive.getKey(), now.angle());
                    stages.put(drive.getKey(), now.stage());
                    final HydraulicVoice voice = voices.get(drive.getKey());
                    stepped.events().forEach(voice::hit);
                    voice.set(now.spool(), now.flow(), now.cushion());
                    voice.render(block, 0, BLOCK);
                    for (int i = 0; i < BLOCK; i++) {
                        mixed[i] += block[i];
                    }
                }
                if (line != null) {
                    final double gain = volume;
                    for (int i = 0; i < BLOCK; i++) {
                        final short s = Wav.toShort((float) Math.tanh(mixed[i] * gain));
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
