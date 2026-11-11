package com.example.fxsuite.avatar.sound;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WavTest {

    @Test
    void aWavFileSaysWhatItHolds() {
        final Clip clip = new Clip("test", new float[] {0, 0.5f, -1, 2}, false);
        final ByteBuffer wav = ByteBuffer.wrap(Wav.of(clip)).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(44 + 8, wav.capacity());
        assertEquals("RIFF", text(wav, 0));
        assertEquals(36 + 8, wav.getInt(4));
        assertEquals("WAVE", text(wav, 8));
        assertEquals(1, wav.getShort(20), "PCM");
        assertEquals(1, wav.getShort(22), "mono");
        assertEquals(Clip.RATE, wav.getInt(24));
        assertEquals(16, wav.getShort(34));
        assertEquals("data", text(wav, 36));
        assertEquals(8, wav.getInt(40));
        assertEquals(0, wav.getShort(44));
        assertEquals(16384, wav.getShort(46), 1);
        assertEquals(-Short.MAX_VALUE, wav.getShort(48));
        assertEquals(Short.MAX_VALUE, wav.getShort(50), "over full scale is clipped");
    }

    private static String text(final ByteBuffer buffer, final int at) {
        final byte[] four = new byte[4];
        buffer.get(at, four);
        return new String(four, StandardCharsets.US_ASCII);
    }
}
