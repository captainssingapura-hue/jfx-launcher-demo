package com.example.fxsuite.viewer;

import javafx.geometry.Point3D;

import java.util.Collection;
import java.util.Optional;

/**
 * What a line of sight hits first among boxes that are never rotated, as the slates are.
 * Pure maths: no scene graph, so JavaFX's mouse picking, which only works under the cursor,
 * is not needed for the crosshair in the middle of the screen.
 */
public final class Aim {

    /** A box around {@code thing}: its centre and half its size along each axis. */
    public record Target<T>(T thing, Point3D centre, Point3D half) {
    }

    /** What was hit, and how far along the line. */
    public record Hit<T>(T thing, double distance) {
    }

    private final double range;

    /** @param range how far the line of sight reaches */
    public Aim(final double range) {
        this.range = range;
    }

    /** The nearest box the line from {@code from} in {@code direction} passes through, within range. */
    public <T> Optional<Hit<T>> nearest(final Point3D from, final Point3D direction,
                                        final Collection<Target<T>> targets) {
        final Point3D along = direction.normalize();
        Hit<T> best = null;
        for (final Target<T> target : targets) {
            final double distance = entry(from, along, target);
            if (distance >= 0 && distance <= range && (best == null || distance < best.distance())) {
                best = new Hit<>(target.thing(), distance);
            }
        }
        return Optional.ofNullable(best);
    }

    /**
     * Where the line enters the box (0 if it starts inside), or -1 if it misses: the slab
     * test, narrowing the stretch of the line that is inside the box one axis at a time.
     */
    private double entry(final Point3D from, final Point3D along, final Target<?> target) {
        final double[] origin = {from.getX(), from.getY(), from.getZ()};
        final double[] step = {along.getX(), along.getY(), along.getZ()};
        final double[] low = {target.centre().getX() - target.half().getX(),
                target.centre().getY() - target.half().getY(), target.centre().getZ() - target.half().getZ()};
        final double[] high = {target.centre().getX() + target.half().getX(),
                target.centre().getY() + target.half().getY(), target.centre().getZ() + target.half().getZ()};
        double enter = Double.NEGATIVE_INFINITY;
        double leave = Double.POSITIVE_INFINITY;
        for (int axis = 0; axis < 3; axis++) {
            if (Math.abs(step[axis]) < 1e-12) {
                // parallel to this pair of sides: inside them or never
                if (origin[axis] < low[axis] || origin[axis] > high[axis]) {
                    return -1;
                }
                continue;
            }
            final double a = (low[axis] - origin[axis]) / step[axis];
            final double b = (high[axis] - origin[axis]) / step[axis];
            enter = Math.max(enter, Math.min(a, b));
            leave = Math.min(leave, Math.max(a, b));
        }
        if (enter > leave || leave < 0) {
            return -1;
        }
        return Math.max(0, enter);
    }
}
