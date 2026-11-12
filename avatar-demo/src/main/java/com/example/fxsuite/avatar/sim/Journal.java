package com.example.fxsuite.avatar.sim;

import java.util.ArrayList;
import java.util.List;

/**
 * What happened, as a timeline: each step's events, stamped with the step and their order in it,
 * for whatever is outside the simulation to read at its own pace. The simulation never reads it.
 *
 * <ul>
 *   <li><b>One writer</b> adds to it ({@link #append}), a step at a time, in order; nothing in it
 *       is ever changed, only added.</li>
 *   <li><b>Each reader keeps its own place</b> ({@link Reader}): it asks for what is new since it
 *       last looked, which moves only its own cursor. Nothing is cleared on anyone's behalf, so the
 *       effects, the sound, a recorder or a minimap each read as often as suits them.</li>
 *   <li><b>It keeps a window</b>, the last {@link #WINDOW} steps (two seconds), not everything. A
 *       reader that falls further behind than that is told how many entries it missed. That is
 *       fine for what reads it: nothing in the world depends on its outputs.</li>
 * </ul>
 *
 * <p>Safe to read from another thread than the one writing: each call holds the journal's lock
 * for as long as it takes to add or to copy out, and the entries themselves never change.
 */
public final class Journal {

    /** How much is kept: two seconds of steps. */
    public static final long WINDOW = 240;

    /** One event, as it happened: in step {@code step}, the {@code sequence}th of that step. */
    public record Logged(long step, int sequence, Event event) {
    }

    /**
     * What a reader got: the entries new since it last looked, in order, and how many it missed by
     * falling further behind than the window.
     */
    public record Read(List<Logged> entries, long missed) {

        public Read {
            entries = List.copyOf(entries);
        }

        /** Just the events, in order. */
        public List<Event> events() {
            return entries.stream().map(Logged::event).toList();
        }
    }

    private final long window;
    private final List<Logged> kept = new ArrayList<>();
    /** The position of the oldest entry kept, and of the next to be added: every entry ever added has its own. */
    private long first;
    private long end;
    private long latest = -1;
    private int sequence;

    public Journal() {
        this(WINDOW);
    }

    /** Keeping the last {@code window} steps. */
    public Journal(final long window) {
        if (window < 1) {
            throw new IllegalArgumentException("a window of at least one step");
        }
        this.window = window;
    }

    /**
     * Adds step {@code step}'s events, in order. Steps only go forward: a step before the latest
     * is refused. Adding to the latest step again carries on its order.
     */
    public synchronized void append(final long step, final List<Event> events) {
        if (step < latest) {
            throw new IllegalArgumentException("step " + step + " comes before step " + latest);
        }
        if (step > latest) {
            latest = step;
            sequence = 0;
        }
        for (final Event event : events) {
            kept.add(new Logged(step, sequence++, event));
            end++;
        }
        // let go of what has fallen out of the window
        int gone = 0;
        while (gone < kept.size() && kept.get(gone).step() <= latest - window) {
            gone++;
        }
        if (gone > 0) {
            kept.subList(0, gone).clear();
            first += gone;
        }
    }

    /** The latest step anything was added for; -1 before anything was. */
    public synchronized long latest() {
        return latest;
    }

    /** A new reader, which will read what is added from now on. */
    public synchronized Reader reader() {
        return new Reader(end);
    }

    /** A new reader, which will read everything still kept, then what is added. */
    public synchronized Reader readerFromOldest() {
        return new Reader(first);
    }

    /** One reader's place in the journal. Each keeps its own; reading moves no one else's. */
    public final class Reader {
        private long cursor;

        private Reader(final long cursor) {
            this.cursor = cursor;
        }

        /** Everything added since this reader last looked, and how many it missed. */
        public Read read() {
            synchronized (Journal.this) {
                final long missed = Math.max(0, first - cursor);
                final long from = Math.max(cursor, first);
                final List<Logged> entries = new ArrayList<>(kept.subList((int) (from - first), (int) (end - first)));
                cursor = end;
                return new Read(entries, missed);
            }
        }
    }
}
