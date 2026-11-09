package com.example.fxsuite.viewer;

/**
 * How big to draw something of a given size: the logarithm of its bytes, mapped linearly
 * from {@link #SMALLEST} at 4 KB or less to {@link #LARGEST} at 32 MB or more. Fixed, not
 * relative to what is on screen, so a release is the same size in every view.
 */
public final class LogScale {

    public static final double SMALLEST = 0.7;
    public static final double LARGEST = 1.5;

    private static final double LOW = Math.log(4 * 1024);
    private static final double HIGH = Math.log(32L * 1024 * 1024);

    public double factor(final long bytes) {
        final double at = (Math.log(Math.max(1, bytes)) - LOW) / (HIGH - LOW);
        return SMALLEST + (LARGEST - SMALLEST) * Math.max(0, Math.min(1, at));
    }
}
