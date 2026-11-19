package com.example.fxsuite.avatar;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;

import java.nio.file.Path;

/**
 * Whether slow frames are logged, and how slow is slow: one setting for every window that draws (a game's, a bench's),
 * each logging its own slow frames while it is on ({@link SlowFrames}), to the console and to {@link #FILE}. Turned on
 * and off, and its threshold changed, as things run: from a game's View menu, from a bench's bar. On from the start
 * with {@code -Davatar.slow=20} (milliseconds); else off, {@link #DEFAULT} once on. On the screen's thread.
 */
public final class FrameLog {

    /** How slow a frame must be to be told of, unless said otherwise, ms. */
    public static final double DEFAULT = 20;
    /** Where the lines are kept, added to from run to run. */
    public static final Path FILE = Path.of(System.getProperty("java.io.tmpdir"), "avatar-frames.log");

    /** Whether slow frames are logged. */
    public static final BooleanProperty ON = new SimpleBooleanProperty(false);
    /** How long after the one before a frame must come to be slow, ms. */
    public static final DoubleProperty SLOW = new SimpleDoubleProperty(DEFAULT);

    static {
        final String ms = System.getProperty("avatar.slow");
        if (ms != null && !ms.isBlank()) {
            SLOW.set(Double.parseDouble(ms));
            ON.set(true);
        }
    }

    private FrameLog() {
    }
}
