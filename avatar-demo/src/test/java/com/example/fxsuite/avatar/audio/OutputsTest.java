package com.example.fxsuite.avatar.audio;

import com.example.fxsuite.avatar.audio.Speakers.Kind;

import org.junit.jupiter.api.Test;

import javax.sound.sampled.LineUnavailableException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Choosing the output: what was asked for if it opens, else the next, down to silence. */
class OutputsTest {

    @Test
    void theOneAskedForComesFirstThenTheRestInOrder() {
        assertEquals(List.of(Kind.OPENAL, Kind.JAVA_SOUND, Kind.SILENT), Outputs.order(Kind.OPENAL));
        assertEquals(List.of(Kind.JAVA_SOUND, Kind.OPENAL, Kind.SILENT), Outputs.order(Kind.JAVA_SOUND));
        assertEquals(Kind.SILENT, Outputs.order(Kind.SILENT).get(0));
    }

    @Test
    void withoutOpenAlOrADeviceItFallsBackAndSaysWhy() {
        final List<Kind> tried = new ArrayList<>();
        final Outputs.Opened opened = Outputs.open(Outputs.order(Kind.OPENAL), kind -> {
            tried.add(kind);
            return switch (kind) {
                // as when the jar was built without LWJGL
                case OPENAL -> throw new NoClassDefFoundError("org/lwjgl/openal/ALC10");
                case JAVA_SOUND -> throw new LineUnavailableException("no line");
                case SILENT -> new SilentSpeakers();
            };
        });
        assertEquals(Kind.SILENT, opened.speakers().kind());
        assertEquals(List.of(Kind.OPENAL, Kind.JAVA_SOUND, Kind.SILENT), tried);
        assertEquals(2, opened.notes().size());
        assertTrue(opened.notes().get(0).startsWith("OpenAL unavailable: not in this build"), opened.notes().get(0));
        assertEquals("Java Sound unavailable: no line", opened.notes().get(1));
    }

    @Test
    void aFailingNativeLibraryIsJustAnotherReasonToFallBack() {
        final Outputs.Opened opened = Outputs.open(Outputs.order(Kind.OPENAL), kind -> {
            if (kind == Kind.OPENAL) {
                throw new UnsatisfiedLinkError("Failed to locate library: OpenAL.dll");
            }
            return new SilentSpeakers();
        });
        assertEquals(Kind.SILENT, opened.speakers().kind());
        assertEquals(List.of("OpenAL unavailable: Failed to locate library: OpenAL.dll"), opened.notes());
    }

    @Test
    void silenceAlwaysOpens() {
        final Outputs.Opened opened = Outputs.open(Kind.SILENT);
        assertEquals(Kind.SILENT, opened.speakers().kind());
        assertTrue(opened.notes().isEmpty());
        opened.speakers().close();
    }

    @Test
    void theSettingNamesTheOutput() {
        final String was = System.getProperty("avatar.audio");
        try {
            System.setProperty("avatar.audio", "Java-Sound");
            assertEquals(Kind.JAVA_SOUND, Kind.fromSettings());
            System.setProperty("avatar.audio", "silent");
            assertEquals(Kind.SILENT, Kind.fromSettings());
            System.clearProperty("avatar.audio");
            assertEquals(Kind.OPENAL, Kind.fromSettings());
        } finally {
            if (was != null) {
                System.setProperty("avatar.audio", was);
            }
        }
    }
}
