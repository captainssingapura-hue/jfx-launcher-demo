package com.example.fxsuite.avatar.sound;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

/** Sound as a WAV file (16-bit PCM), to listen to in any player or check by eye. */
public final class Wav {

    private Wav() {
    }

    /** One clip, mono. */
    public static byte[] of(final Clip clip) {
        return of(clip.samples, 1);
    }

    /** Samples for {@code channels} channels, interleaved (left, right, left, ...). */
    public static byte[] of(final float[] samples, final int channels) {
        final int data = samples.length * 2;
        final ByteBuffer out = ByteBuffer.allocate(44 + data).order(ByteOrder.LITTLE_ENDIAN);
        out.put(ascii("RIFF")).putInt(36 + data).put(ascii("WAVE"));
        out.put(ascii("fmt ")).putInt(16)
                .putShort((short) 1)                              // PCM
                .putShort((short) channels)
                .putInt(Clip.RATE)
                .putInt(Clip.RATE * channels * 2)                 // bytes a second
                .putShort((short) (channels * 2))                 // bytes a frame
                .putShort((short) 16);
        out.put(ascii("data")).putInt(data);
        for (final float s : samples) {
            out.putShort(toShort(s));
        }
        return out.array();
    }

    public static void write(final Path file, final Clip clip) throws IOException {
        Files.write(file, of(clip));
    }

    public static void write(final Path file, final float[] samples, final int channels) throws IOException {
        Files.write(file, of(samples, channels));
    }

    /** A sample, -1 to 1, as 16 bits; beyond full scale it is clipped. */
    public static short toShort(final double sample) {
        return (short) Math.round(Math.max(-1, Math.min(1, sample)) * Short.MAX_VALUE);
    }

    private static byte[] ascii(final String text) {
        return text.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    }
}
