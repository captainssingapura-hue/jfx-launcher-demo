package com.example.fxsuite.avatar.audio;

import com.example.fxsuite.avatar.sim.Vec3;
import com.example.fxsuite.avatar.sound.Clip;
import com.example.fxsuite.avatar.sound.Wav;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

/**
 * Sound through Java Sound, which comes with Java and needs no native library of ours. It
 * only plays a stream, so the placing and mixing is done here, by {@link Mix}, on a thread
 * of its own that keeps the line fed a few milliseconds ahead.
 */
final class JavaSoundSpeakers implements Speakers {

    /** Frames mixed at a time: about 12 ms. */
    private static final int BLOCK = 512;
    /** The line holds four blocks: about 46 ms from a play to the ear. */
    private static final int AHEAD = 4;

    private final Mix mix = new Mix();
    private final SourceDataLine line;
    private final String device;
    private final Thread feeder;
    private volatile boolean running = true;

    private JavaSoundSpeakers(final SourceDataLine line, final String device) {
        this.line = line;
        this.device = device;
        feeder = new Thread(this::feed, "java-sound-mix");
        feeder.setDaemon(true);
        feeder.start();
    }

    static Speakers open() throws LineUnavailableException {
        final AudioFormat format = new AudioFormat(Clip.RATE, 16, 2, true, false);
        final DataLine.Info wanted = new DataLine.Info(SourceDataLine.class, format);
        for (final Mixer.Info info : AudioSystem.getMixerInfo()) {
            final Mixer mixer = AudioSystem.getMixer(info);
            if (mixer.isLineSupported(wanted)) {
                final SourceDataLine line = (SourceDataLine) mixer.getLine(wanted);
                line.open(format, BLOCK * 4 * AHEAD);
                line.start();
                return new JavaSoundSpeakers(line, info.getName());
            }
        }
        throw new LineUnavailableException("no device can play 16-bit stereo at " + Clip.RATE + " Hz");
    }

    private void feed() {
        final float[] mixed = new float[BLOCK * 2];
        final byte[] bytes = new byte[BLOCK * 4];
        try {
            while (running) {
                mix.render(mixed, BLOCK);
                for (int i = 0; i < mixed.length; i++) {
                    final short s = Wav.toShort(mixed[i]);
                    bytes[2 * i] = (byte) s;
                    bytes[2 * i + 1] = (byte) (s >> 8);
                }
                line.write(bytes, 0, bytes.length);       // waits while the line is full
            }
        } catch (RuntimeException e) {
            // the device went away: from here on, silence
            running = false;
        }
    }

    @Override
    public Kind kind() {
        return Kind.JAVA_SOUND;
    }

    @Override
    public String device() {
        return device;
    }

    @Override
    public void listen(final Vec3 at, final double yaw) {
        mix.listen(at, yaw);
    }

    @Override
    public void volume(final double volume) {
        mix.volume(volume);
    }

    @Override
    public void play(final Clip clip, final double gain, final double pitch) {
        mix.play(clip, gain, pitch, null);
    }

    @Override
    public void play(final Clip clip, final double gain, final double pitch, final Vec3 at, final double near) {
        mix.play(clip, gain, pitch, at, near);
    }

    @Override
    public Loop loop(final Clip clip) {
        return mix.loop(clip);
    }

    @Override
    public Stream stream(final Source source) {
        return mix.stream(source);
    }

    @Override
    public void close() {
        running = false;
        mix.clear();
        try {
            feeder.join(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        line.stop();
        line.flush();
        line.close();
    }
}
