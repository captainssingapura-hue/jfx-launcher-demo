package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.sim.Num;
import com.example.fxsuite.avatar.sim.Vec3;

/**
 * What the camera sees, across the ground: from {@code eye}, facing {@code yaw}, {@code half} degrees either
 * side, and {@link #MARGIN} more, so what is just out of view as it turns is drawn already; and everything
 * within {@link #ROUND} of it, whichever way it looks, for what it looks down on. How far up or down it looks
 * is left out: what it sees is never less than this says. And how far it sees ({@code reach}, {@link Sight}), how high
 * it is over the ground ({@code height}).
 */
public record View(Vec3 eye, double yaw, double half, double reach, double height) {

    /** How much wider either side than the camera's view, degrees. */
    static final double MARGIN = 15;
    /** How near the camera anything is seen, whichever way it looks. */
    static final double ROUND = 15_000;

    /** On the ground, seeing as far as from there. */
    public View(final Vec3 eye, final double yaw, final double half) {
        this(eye, yaw, half, Sight.reach(0), 0);
    }

    /** Seen from everywhere, on the ground: when nothing is known of which way the camera looks. */
    public static View all(final Vec3 eye) {
        return new View(eye, 0, 180);
    }

    /** How much haze there is over ground {@code away} from the camera, across the ground ({@link Sight#haze}). */
    public double haze(final double away) {
        return Sight.haze(away, reach);
    }

    /** How far out the haze begins. */
    public double clear() {
        return Sight.CLEAR * reach;
    }

    /** Whether what is {@code near} off across the ground at its nearest is near enough to be drawn at all. */
    public boolean draws(final double near) {
        return near <= Sight.BUILT * reach;
    }

    /** How far what is {@code near} off across the ground is from the camera, its height over the ground and all. */
    public double away(final double near) {
        return Math.hypot(near, height);
    }

    /** Whether any of the ground from ({@code x0}, {@code z0}) to ({@code x1}, {@code z1}) is in view. */
    public boolean sees(final double x0, final double z0, final double x1, final double z1) {
        if (half + MARGIN >= 180) {
            return true;
        }
        final double dx = Math.max(0, Math.max(x0 - eye.x(), eye.x() - x1));
        final double dz = Math.max(0, Math.max(z0 - eye.z(), eye.z() - z1));
        if (Math.hypot(dx, dz) <= ROUND) {
            return true;
        }
        // the box's corners as seen from the eye, either side of the way to its middle: less than half round,
        // since the eye is outside it
        final double middle = bearing((x0 + x1) / 2, (z0 + z1) / 2);
        double least = 0;
        double most = 0;
        for (final double x : new double[] {x0, x1}) {
            for (final double z : new double[] {z0, z1}) {
                final double off = Num.wrap(bearing(x, z) - middle);
                least = Math.min(least, off);
                most = Math.max(most, off);
            }
        }
        final double toMiddle = Num.wrap(middle - yaw);
        final double reach = half + MARGIN;
        // and the same once round either way, for a box reaching round behind
        for (final double round : new double[] {0, -360, 360}) {
            if (toMiddle + most + round >= -reach && toMiddle + least + round <= reach) {
                return true;
            }
        }
        return false;
    }

    /** The heading from the eye to ({@code x}, {@code z}), as {@link Num#heading} has it: 0 along +z, 90 along +x. */
    private double bearing(final double x, final double z) {
        return Math.toDegrees(Math.atan2(x - eye.x(), z - eye.z()));
    }
}
