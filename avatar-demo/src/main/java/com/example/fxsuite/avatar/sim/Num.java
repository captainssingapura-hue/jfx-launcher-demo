package com.example.fxsuite.avatar.sim;

/** The few bits of arithmetic every layer leans on: clamping, easing, angles and headings. */
public final class Num {

    private Num() {
    }

    public static double clamp(final double value, final double low, final double high) {
        return Math.max(low, Math.min(high, value));
    }

    /**
     * {@code value} eased toward {@code target} for {@code seconds} at the given rate:
     * exponential, so the result is the same however the time is split into frames.
     */
    public static double ease(final double value, final double target, final double rate, final double seconds) {
        return target + (value - target) * Math.exp(-rate * seconds);
    }

    /** An angle in degrees, brought into (-180, 180]. */
    public static double wrap(final double degrees) {
        final double d = ((degrees % 360) + 360) % 360;
        return d > 180 ? d - 360 : d;
    }

    /** Ahead, along a heading: {@code (sin yaw, 0, cos yaw)}, as {@code Rotate(yaw, Y_AXIS)} turns +z. */
    public static Vec3 heading(final double yaw) {
        final double y = Math.toRadians(yaw);
        return new Vec3(Math.sin(y), 0, Math.cos(y));
    }

    /** To the right of a heading: {@code (cos yaw, 0, -sin yaw)}. */
    public static Vec3 right(final double yaw) {
        final double y = Math.toRadians(yaw);
        return new Vec3(Math.cos(y), 0, -Math.sin(y));
    }
}
