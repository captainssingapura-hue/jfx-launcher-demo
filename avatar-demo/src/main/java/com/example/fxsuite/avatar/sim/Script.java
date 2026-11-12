package com.example.fxsuite.avatar.sim;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * A run without a keyboard, as inputs stamped with the step they come in at: a script and the
 * world it starts from give the same run every time, step for step, however the frames fall.
 *
 * <p>It is written as {@code "F;W:2;W+D+SHIFT:1.5"}: each part holds its keys for its seconds
 * (a part without seconds is a tap, {@value #TAP} s, and {@code :1} on its own just waits).
 * Among a part's keys, {@code FIRE} holds the trigger, {@code @90} turns 90° over the part, as
 * the mouse would, and {@code ^20} tilts the view, and so the aim, up 20° (down, when negative).
 * The first part comes in {@value #LEAD} s after the start. The keys are named as in {@link Keys};
 * those the world takes no notice of (the camera's) are kept for whatever shows it.
 *
 * <p>Steps are counted from the start, the first being step 1. Seconds become steps on the
 * {@link Clock}: a part from {@code t} for {@code hold} seconds covers the steps after
 * {@code t} up to {@code t + hold}, at least one.
 */
public final class Script {

    /** How long after the start the first part comes in, in seconds. */
    public static final double LEAD = 0.5;
    /** How long a tap is held, in seconds. */
    public static final double TAP = 0.15;

    /** What one part does over steps {@code first} to {@code last}: a key held, the trigger, a turn or a tilt. */
    public record Span(Kind kind, String key, long first, long last, double perStep) {

        public enum Kind { KEY, TRIGGER, TURN, TILT }

        boolean covers(final long step) {
            return step >= first && step <= last;
        }

        /** How many of this span's steps come by step {@code step}. */
        long by(final long step) {
            return Math.max(0, Math.min(last, step) - first + 1);
        }
    }

    private final List<Span> spans;
    private final long length;

    private Script(final List<Span> spans, final long length) {
        this.spans = List.copyOf(spans);
        this.length = length;
    }

    /** Reads {@code text}; a blank one does nothing and is over at once. */
    public static Script parse(final String text) {
        final List<Span> spans = new ArrayList<>();
        double t = LEAD;
        boolean any = false;
        for (final String part : text.split(";")) {
            if (part.isBlank()) {
                continue;
            }
            any = true;
            final String[] pieces = part.trim().split(":");
            final double hold = pieces.length > 1 ? Double.parseDouble(pieces[1].trim()) : TAP;
            if (hold < 0) {
                throw new IllegalArgumentException("a part held for less than nothing: " + part);
            }
            final long first = steps(t) + 1;
            final long last = Math.max(first, steps(t + hold));
            final long count = last - first + 1;
            if (!pieces[0].isBlank()) {
                for (final String raw : pieces[0].split("\\+")) {
                    final String name = raw.trim().toUpperCase(Locale.ROOT);
                    if (name.isEmpty()) {
                        throw new IllegalArgumentException("a key without a name: " + part);
                    }
                    if (name.equals("FIRE")) {
                        spans.add(new Span(Span.Kind.TRIGGER, name, first, last, 0));
                    } else if (name.startsWith("@")) {
                        spans.add(new Span(Span.Kind.TURN, name, first, last,
                                Double.parseDouble(name.substring(1)) / count));
                    } else if (name.startsWith("^")) {
                        spans.add(new Span(Span.Kind.TILT, name, first, last,
                                Double.parseDouble(name.substring(1)) / count));
                    } else {
                        spans.add(new Span(Span.Kind.KEY, name, first, last, 0));
                    }
                }
            }
            t += hold;
        }
        return new Script(spans, any ? steps(t) : 0);
    }

    private static long steps(final double seconds) {
        return Math.round(seconds / Clock.STEP);
    }

    /** What each part does, in the order written. */
    public List<Span> spans() {
        return spans;
    }

    /** The last step the script covers; 0 for one that does nothing. */
    public long length() {
        return length;
    }

    /** The keys the script names, for whatever reads them to check they are real. */
    public Set<String> keys() {
        final Set<String> keys = new LinkedHashSet<>();
        spans.stream().filter(s -> s.kind() == Span.Kind.KEY).forEach(s -> keys.add(s.key()));
        return keys;
    }

    /** The keys held at step {@code step}. */
    public Set<String> held(final long step) {
        final Set<String> held = new LinkedHashSet<>();
        spans.stream().filter(s -> s.kind() == Span.Kind.KEY && s.covers(step)).forEach(s -> held.add(s.key()));
        return held;
    }

    /** The keys pressed at step {@code step}, in the order written. */
    public List<String> pressed(final long step) {
        return spans.stream().filter(s -> s.kind() == Span.Kind.KEY && s.first() == step).map(Span::key).toList();
    }

    /** Whether the trigger is held at step {@code step}. */
    public boolean trigger(final long step) {
        return spans.stream().anyMatch(s -> s.kind() == Span.Kind.TRIGGER && s.covers(step));
    }

    /** Degrees turned in step {@code step}. */
    public double turn(final long step) {
        return during(Span.Kind.TURN, step);
    }

    /** Degrees the view tilts up in step {@code step}. */
    public double tilt(final long step) {
        return during(Span.Kind.TILT, step);
    }

    /** How far the view is tilted from rest by the end of step {@code step}, in degrees up: the aim. */
    public double aim(final long step) {
        double aim = 0;
        for (final Span span : spans) {
            if (span.kind() == Span.Kind.TILT) {
                aim += span.perStep() * span.by(step);
            }
        }
        return aim;
    }

    /** Step {@code step}'s inputs to the world. */
    public Controls controls(final long step) {
        return Keys.controls(held(step), pressed(step), turn(step), trigger(step), aim(step));
    }

    private double during(final Span.Kind kind, final long step) {
        double sum = 0;
        for (final Span span : spans) {
            if (span.kind() == kind && span.covers(step)) {
                sum += span.perStep();
            }
        }
        return sum;
    }
}
