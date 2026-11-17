package com.example.fxsuite.avatar.air;

import com.example.fxsuite.avatar.build.Point;

/**
 * Where something is fixed on an aircraft, in its frame (across to the right, along to the nose,
 * up), and how it is turned there: rolled about its own length (positive, its right side down),
 * then pitched (positive, its nose up), then yawed (positive, its nose to the right), then put at
 * {@code at}. A part is made in a frame of its own about its origin, then mounted.
 *
 * @param at    where its origin goes
 * @param yaw   degrees, nose to the right
 * @param pitch degrees, nose up
 * @param roll  degrees, right side down
 */
public record Mount(Point at, double yaw, double pitch, double roll) {

    /** Put at {@code at}, square to the aircraft. */
    public static Mount at(final Point at) {
        return new Mount(at, 0, 0, 0);
    }

    /** Put at ({@code across}, {@code along}, {@code up}), square to the aircraft. */
    public static Mount at(final double across, final double along, final double up) {
        return at(new Point(across, along, up));
    }

    /** The same, turned as well. */
    public Mount turned(final double byYaw, final double byPitch, final double byRoll) {
        return new Mount(at, yaw + byYaw, pitch + byPitch, roll + byRoll);
    }

    /** Where {@code p}, in the mounted thing's own frame, is in the aircraft's. */
    public Point place(final Point p) {
        double x = p.across();
        double y = p.along();
        double z = p.up();
        // rolled: the right side down
        final double r = Math.toRadians(roll);
        final double x1 = x * Math.cos(r) + z * Math.sin(r);
        final double z1 = -x * Math.sin(r) + z * Math.cos(r);
        // pitched: the nose up
        final double q = Math.toRadians(pitch);
        final double y2 = y * Math.cos(q) - z1 * Math.sin(q);
        final double z2 = y * Math.sin(q) + z1 * Math.cos(q);
        // yawed: the nose to the right
        final double w = Math.toRadians(yaw);
        x = x1 * Math.cos(w) + y2 * Math.sin(w);
        y = -x1 * Math.sin(w) + y2 * Math.cos(w);
        return new Point(x + at.across(), y + at.along(), z2 + at.up());
    }

    /** Which way {@code direction}, in the mounted thing's own frame, points in the aircraft's: turned, not moved. */
    public Point turn(final Point direction) {
        final Point placed = place(direction);
        return new Point(placed.across() - at.across(), placed.along() - at.along(), placed.up() - at.up());
    }
}
