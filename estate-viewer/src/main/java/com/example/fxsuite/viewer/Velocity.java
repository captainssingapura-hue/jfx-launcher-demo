package com.example.fxsuite.viewer;

/**
 * How fast a flying camera moves, in the camera's own terms: ahead, rightward and upward,
 * in world units per second. It eases toward what the keys ask for, so movement glides in
 * and out instead of starting and stopping dead. Pure maths.
 */
public record Velocity(double ahead, double rightward, double upward) {

    public static final Velocity STILL = new Velocity(0, 0, 0);

    /** How quickly speed follows the keys: about 95% of the way in 0.3 seconds. */
    public static final double RESPONSE = 10;

    /**
     * After {@code seconds}, eased toward {@code target}. Exponential, so the result is the
     * same however the time is split into frames.
     */
    public Velocity toward(final Velocity target, final double seconds) {
        final double keep = Math.exp(-RESPONSE * seconds);
        return new Velocity(
                target.ahead + (ahead - target.ahead) * keep,
                target.rightward + (rightward - target.rightward) * keep,
                target.upward + (upward - target.upward) * keep);
    }

    public boolean isStill() {
        return Math.abs(ahead) + Math.abs(rightward) + Math.abs(upward) < 0.5;
    }
}
