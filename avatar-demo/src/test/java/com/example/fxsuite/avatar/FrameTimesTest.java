package com.example.fxsuite.avatar;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Frame times, summed up a second at a time. */
class FrameTimesTest {

    private static final long MS = 1_000_000;

    /** The gap between frames at {@code fps}, rounded up, so that many of them fill a second. */
    private static long gap(final int fps) {
        return (FrameTimes.WINDOW + fps - 1) / fps;
    }

    @Test
    void aSmoothSecondAtSixty() {
        final FrameTimes times = new FrameTimes();
        FrameTimes.Summary summary = null;
        long now = 0;
        for (int i = 0; i <= 60 && summary == null; i++) {
            summary = times.frame(now, 2 * MS);
            now += gap(60);
        }
        assertNotNull(summary, "summed up once a second has gone");
        assertEquals(60, summary.fps(), 0.5);
        assertEquals(1000.0 / 60, summary.frameMs(), 0.01);
        assertEquals(2, summary.updateMs(), 1e-9);
        assertFalse(summary.struggling());
        assertEquals("60 fps · frame 16.7 ms, worst 16.7 · update 2.0 ms, worst 2.0", summary.text());
    }

    @Test
    void oneLongFrameShowsAsTheWorstAndCountsAsStruggling() {
        final FrameTimes times = new FrameTimes();
        FrameTimes.Summary summary = null;
        long now = 0;
        for (int i = 0; summary == null; i++) {
            // one frame in the middle takes 80 ms, its update most of that
            final boolean slow = i == 30;
            now += slow ? 80 * MS : gap(60);
            summary = times.frame(now, slow ? 70 * MS : MS);
        }
        assertEquals(80, summary.worstFrameMs(), 1e-9);
        assertEquals(70, summary.worstUpdateMs(), 1e-9);
        assertTrue(summary.fps() < 60);
        assertTrue(summary.struggling(), "a stutter, though the average is near 60");
    }

    @Test
    void eachSecondStartsAfresh() {
        final FrameTimes times = new FrameTimes();
        long now = 0;
        int summaries = 0;
        FrameTimes.Summary last = null;
        for (int i = 0; i < 30 * 3 + 1; i++) {
            final FrameTimes.Summary s = times.frame(now, MS);
            if (s != null) {
                summaries++;
                last = s;
            }
            now += gap(30);
        }
        assertEquals(3, summaries);
        assertEquals(30, last.fps(), 0.5);
        assertTrue(last.struggling(), "30 a second is short of smooth");
        assertNull(new FrameTimes().frame(0, MS), "the first frame has nothing to measure from");
    }
}
