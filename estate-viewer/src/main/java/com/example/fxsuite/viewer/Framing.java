package com.example.fxsuite.viewer;

import javafx.geometry.Point3D;

import java.util.Collection;

/**
 * How far back to stand to see a set of points from a given angle: looking at the centre of
 * their bounding box, at the smallest distance where every point, grown by a margin for the
 * size of what sits there, falls inside both fields of view. Pure maths.
 */
public final class Framing {

    /**
     * @param points      what must be in view
     * @param margin      added around each point, for the size of what sits there
     * @param fieldOfView the camera's vertical field of view, in degrees
     * @param aspect      width over height of the view
     */
    public Orbit frame(final Collection<Point3D> points, final double margin, final double fieldOfView,
                       final double aspect, final double yaw, final double pitch) {
        if (points.isEmpty()) {
            return new Orbit(Point3D.ZERO, 1000, yaw, pitch);
        }
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (final Point3D p : points) {
            minX = Math.min(minX, p.getX());
            minY = Math.min(minY, p.getY());
            minZ = Math.min(minZ, p.getZ());
            maxX = Math.max(maxX, p.getX());
            maxY = Math.max(maxY, p.getY());
            maxZ = Math.max(maxZ, p.getZ());
        }
        final Point3D centre = new Point3D((minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2);
        final Orbit aim = new Orbit(centre, Orbit.MIN_DISTANCE, yaw, pitch);
        final double tanVertical = Math.tan(Math.toRadians(fieldOfView) / 2);
        final double tanHorizontal = tanVertical * aspect;

        // A point at (across, up, along) from the centre, in the camera's axes, is in view at
        // distance d when |across| <= (d + along) * tanHorizontal, and likewise vertically.
        double distance = Orbit.MIN_DISTANCE;
        for (final Point3D p : points) {
            final Point3D from = p.subtract(centre);
            final double across = Math.abs(from.dotProduct(aim.right())) + margin;
            final double up = Math.abs(from.dotProduct(aim.down())) + margin;
            final double along = from.dotProduct(aim.forward());
            distance = Math.max(distance, Math.max(across / tanHorizontal, up / tanVertical) - along + margin);
        }
        return new Orbit(centre, distance, yaw, pitch);
    }
}
