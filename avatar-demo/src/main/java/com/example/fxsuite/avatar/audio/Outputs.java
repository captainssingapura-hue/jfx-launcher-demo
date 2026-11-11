package com.example.fxsuite.avatar.audio;

import java.util.ArrayList;
import java.util.List;

/**
 * Opens the sound output: the kind asked for if it works, or else the next that does, down
 * to silence, which always does. Each that failed leaves a note why, for the user to see.
 *
 * <p>OpenAL is reached only through {@link OpenAlSpeakers}, and only when tried, so a build or
 * a classpath without LWJGL still runs: trying it then just fails, like a missing device.
 */
public final class Outputs {

    private Outputs() {
    }

    /** What opened, and why anything tried before it did not. */
    public record Opened(Speakers speakers, List<String> notes) {
        public Opened {
            notes = List.copyOf(notes);
        }
    }

    /** Opens one kind, or fails. */
    @FunctionalInterface
    interface Opener {
        Speakers open(Speakers.Kind kind) throws Exception;
    }

    public static Opened open(final Speakers.Kind preferred) {
        return open(order(preferred), Outputs::attempt);
    }

    /** Whether this one kind opens here, and on what: opens it, says, and closes it again. */
    public static String probe(final Speakers.Kind kind) {
        final Opened opened = open(List.of(kind), Outputs::attempt);
        if (!opened.notes().isEmpty()) {
            return opened.notes().get(0);
        }
        try (Speakers speakers = opened.speakers()) {
            return kind.title() + ": " + speakers.device();
        }
    }

    /** The kind asked for, then the others in their usual order. */
    static List<Speakers.Kind> order(final Speakers.Kind preferred) {
        final List<Speakers.Kind> order = new ArrayList<>(List.of(preferred));
        for (final Speakers.Kind kind : Speakers.Kind.values()) {
            if (kind != preferred) {
                order.add(kind);
            }
        }
        return order;
    }

    static Opened open(final List<Speakers.Kind> order, final Opener opener) {
        final List<String> notes = new ArrayList<>();
        for (final Speakers.Kind kind : order) {
            try {
                return new Opened(opener.open(kind), notes);
            } catch (Exception | LinkageError e) {
                notes.add(kind.title() + " unavailable: " + reason(e));
            }
        }
        return new Opened(new SilentSpeakers(), notes);
    }

    private static Speakers attempt(final Speakers.Kind kind) throws Exception {
        return switch (kind) {
            case OPENAL -> OpenAlSpeakers.open();
            case JAVA_SOUND -> JavaSoundSpeakers.open();
            case SILENT -> new SilentSpeakers();
        };
    }

    private static String reason(final Throwable e) {
        if (e instanceof NoClassDefFoundError) {
            return "not in this build (" + e.getMessage() + ")";
        }
        final String message = e.getMessage();
        final String said = message == null || message.isBlank() ? e.getClass().getSimpleName() : message;
        final String line = said.lines().findFirst().orElse(said).strip();
        return line.length() > 120 ? line.substring(0, 117) + "..." : line;
    }
}
