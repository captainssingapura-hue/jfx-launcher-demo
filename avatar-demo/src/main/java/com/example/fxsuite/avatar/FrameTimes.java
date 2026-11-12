package com.example.fxsuite.avatar;

/**
 * How the frames are going, summed up once a second for the HUD: how many there were, the
 * average and the worst gap between them, and how long the demo's own work took in each (the
 * simulation, the posing, the effects, the sound), apart from JavaFX's drawing. The worst gap
 * is what is felt as a stutter, which an average hides.
 *
 * <p>Pure: it is told each frame's times in nanoseconds, and keeps nothing else.
 */
final class FrameTimes {

    /** How often a summary is made, in nanoseconds: once a second. */
    static final long WINDOW = 1_000_000_000L;

    /**
     * A second's frames: how many a second, the gap between them (average and worst), and the
     * demo's own work in each (average and worst), in milliseconds.
     */
    record Summary(double fps, double frameMs, double worstFrameMs, double updateMs, double worstUpdateMs) {

        /** As the HUD shows it. */
        String text() {
            return String.format("%.0f fps · frame %.1f ms, worst %.1f · update %.1f ms, worst %.1f",
                    fps, frameMs, worstFrameMs, updateMs, worstUpdateMs);
        }

        /** Whether it is short of a smooth 60: under 55 a second, or a frame took twice as long as it should. */
        boolean struggling() {
            return fps < 55 || worstFrameMs > 2 * 1000.0 / 60;
        }
    }

    private long windowStart = -1;
    private long last = -1;
    private int frames;
    private long gaps;
    private long worstGap;
    private long work;
    private long worstWork;

    /**
     * One frame, which began at {@code now} and whose own work took {@code update}, both in
     * nanoseconds.
     *
     * @return a summary of the second just ended, if this frame ended it; otherwise null
     */
    Summary frame(final long now, final long update) {
        if (last < 0) {
            last = now;
            windowStart = now;
            return null;
        }
        final long gap = now - last;
        last = now;
        frames++;
        gaps += gap;
        worstGap = Math.max(worstGap, gap);
        work += update;
        worstWork = Math.max(worstWork, update);
        if (now - windowStart < WINDOW) {
            return null;
        }
        final Summary summary = new Summary(frames * 1e9 / (now - windowStart), gaps / 1e6 / frames,
                worstGap / 1e6, work / 1e6 / frames, worstWork / 1e6);
        windowStart = now;
        frames = 0;
        gaps = 0;
        worstGap = 0;
        work = 0;
        worstWork = 0;
        return summary;
    }
}
