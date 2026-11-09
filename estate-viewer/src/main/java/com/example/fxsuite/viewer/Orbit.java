package com.example.fxsuite.viewer;

import javafx.geometry.Point3D;

/**
 * A camera that looks at a pivot point from a distance, at a heading ({@code yaw}, degrees
 * around the vertical axis) and a tilt ({@code pitch}, degrees). Pure maths: no scene graph.
 *
 * <p>The camera's orientation is {@code Rotate(yaw, Y)} then {@code Rotate(pitch, X)}, the
 * order its transforms are applied in. JavaFX's y axis points down, so a positive pitch
 * tilts the view up and a negative one looks down on the estate.
 */
public record Orbit(Point3D pivot, double distance, double yaw, double pitch) {

    public static final double MAX_PITCH = 89;
    public static final double MIN_DISTANCE = 120;
    public static final double MAX_DISTANCE = 40_000;

    public Orbit {
        pitch = Math.max(-MAX_PITCH, Math.min(MAX_PITCH, pitch));
        distance = Math.max(MIN_DISTANCE, Math.min(MAX_DISTANCE, distance));
    }

    /** The direction the camera looks in. */
    public Point3D forward() {
        final double y = Math.toRadians(yaw);
        final double p = Math.toRadians(pitch);
        return new Point3D(Math.cos(p) * Math.sin(y), -Math.sin(p), Math.cos(p) * Math.cos(y));
    }

    /** The camera's right. */
    public Point3D right() {
        final double y = Math.toRadians(yaw);
        return new Point3D(Math.cos(y), 0, -Math.sin(y));
    }

    /** The camera's down: the bottom of the screen. */
    public Point3D down() {
        final double y = Math.toRadians(yaw);
        final double p = Math.toRadians(pitch);
        return new Point3D(Math.sin(p) * Math.sin(y), Math.cos(p), Math.sin(p) * Math.cos(y));
    }

    public Point3D position() {
        return pivot.subtract(forward().multiply(distance));
    }

    /** Turned around the pivot. */
    public Orbit turned(final double dYaw, final double dPitch) {
        return new Orbit(pivot, distance, yaw + dYaw, pitch + dPitch);
    }

    /** Moved across the screen by the given world distances, keeping the view's direction. */
    public Orbit panned(final double rightward, final double downward) {
        return new Orbit(pivot.add(right().multiply(rightward)).add(down().multiply(downward)), distance, yaw, pitch);
    }

    /**
     * Closer by {@code factor} (below 1 zooms in) toward a point on the pivot's plane, so
     * that point stays under the same spot on the screen.
     */
    public Orbit zoomedToward(final Point3D point, final double factor) {
        final Point3D newPivot = point.add(pivot.subtract(point).multiply(factor));
        return new Orbit(newPivot, distance * factor, yaw, pitch);
    }

    /** The view a fraction {@code t} of the way to {@code target}, turning the short way round. */
    public Orbit towards(final Orbit target, final double t) {
        final double turn = ((target.yaw - yaw) % 360 + 540) % 360 - 180;
        return new Orbit(
                pivot.add(target.pivot.subtract(pivot).multiply(t)),
                distance * Math.pow(target.distance / distance, t),
                yaw + turn * t,
                pitch + (target.pitch - pitch) * t);
    }

    public Orbit withAngles(final double newYaw, final double newPitch) {
        return new Orbit(pivot, distance, newYaw, newPitch);
    }

    /**
     * The direction through a point on the screen.
     *
     * @param fieldOfView the camera's vertical field of view, in degrees
     */
    public Point3D ray(final double x, final double y, final double width, final double height,
                       final double fieldOfView) {
        final double scale = Math.tan(Math.toRadians(fieldOfView) / 2) / (height / 2);
        final double across = (x - width / 2) * scale;
        final double up = (y - height / 2) * scale;
        return forward().add(right().multiply(across)).add(down().multiply(up)).normalize();
    }

    /** Where the line through a point on the screen meets the plane through the pivot facing the camera. */
    public Point3D onPivotPlane(final double x, final double y, final double width, final double height,
                                final double fieldOfView) {
        final Point3D ray = ray(x, y, width, height, fieldOfView);
        return position().add(ray.multiply(distance / ray.dotProduct(forward())));
    }
}
