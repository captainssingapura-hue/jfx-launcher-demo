package com.example.fxsuite.avatar.view;

import com.example.fxsuite.avatar.sim.Num;
import com.example.fxsuite.avatar.sim.Vec3;


/**
 * A chase camera: it looks at a pivot near the avatar's head from a distance, at a heading
 * ({@code yaw}) and a tilt ({@code pitch}, up when positive), all in degrees. Pure maths.
 *
 * <p>The camera's orientation is {@code Rotate(yaw, Y)} then {@code Rotate(pitch, X)}, the
 * order its transforms are applied in, the same convention as the estate viewer's orbit.
 *
 * <p>Each frame it {@linkplain #follow follows}: the pivot eases toward the avatar quickly,
 * and the heading swings round behind the avatar as it moves or turns, unless the player is
 * holding the view to look round.
 */
public record Chase(Vec3 pivot, double yaw, double pitch, double distance) {

    /** Where the camera looks: about the avatar's head, this far above its feet. */
    public static final double EYE_HEIGHT = 160;
    public static final double DEFAULT_PITCH = -16;
    public static final double DEFAULT_DISTANCE = 640;
    public static final double MIN_DISTANCE = 220;
    public static final double MAX_DISTANCE = 3000;
    public static final double MAX_PITCH = 80;
    /** The camera stays at least this far above the ground. */
    public static final double CLEARANCE = 25;
    /** How quickly the pivot catches up with the avatar, and the heading swings round. */
    public static final double CATCH_UP = 14;
    public static final double SWING = 6;

    public Chase {
        pitch = Num.clamp(pitch, -MAX_PITCH, MAX_PITCH);
        distance = Num.clamp(distance, MIN_DISTANCE, MAX_DISTANCE);
    }

    /** Behind the avatar, looking along its heading. */
    public static Chase behind(final Vec3 pivot, final double heading) {
        return new Chase(pivot, heading, DEFAULT_PITCH, DEFAULT_DISTANCE).aboveGround();
    }

    /** The direction the camera looks in. */
    public Vec3 forward() {
        final double y = Math.toRadians(yaw);
        final double p = Math.toRadians(pitch);
        return new Vec3(Math.cos(p) * Math.sin(y), -Math.sin(p), Math.cos(p) * Math.cos(y));
    }

    public Vec3 position() {
        return pivot.subtract(forward().multiply(distance));
    }

    /** Turned around the pivot: right and up for positive amounts. */
    public Chase turned(final double right, final double up) {
        return new Chase(pivot, Num.wrap(yaw + right), pitch + up, distance).aboveGround();
    }

    /** Closer by {@code factor} (below 1 zooms in). */
    public Chase zoomed(final double factor) {
        return new Chase(pivot, yaw, pitch, distance * factor).aboveGround();
    }

    /**
     * One frame of following.
     *
     * @param target  where the pivot should be: the avatar's head
     * @param heading the avatar's yaw
     * @param moving  how much the avatar is moving, 0 to 1: how strongly the view swings behind it
     * @param held    the player is holding the view, so it does not swing
     */
    public Chase follow(final Vec3 target, final double heading, final double moving,
                        final double seconds, final boolean held) {
        final double keep = Math.exp(-CATCH_UP * seconds);
        final Vec3 newPivot = target.add(pivot.subtract(target).multiply(keep));
        double newYaw = yaw;
        if (!held && moving > 0) {
            final double turn = Num.wrap(heading - yaw);
            newYaw = Num.wrap(yaw + turn * (1 - Math.exp(-SWING * moving * seconds)));
        }
        return new Chase(newPivot, newYaw, pitch, distance).aboveGround();
    }

    /** Tipped down as far as needed to keep the camera {@link #CLEARANCE} above the ground. */
    public Chase aboveGround() {
        // position.y = pivot.y + sin(pitch) * distance, and must stay at or above -CLEARANCE
        final double most = (-CLEARANCE - pivot.y()) / distance;
        if (most >= 1 || Math.sin(Math.toRadians(pitch)) <= most) {
            return this;
        }
        return new Chase(pivot, yaw, Math.toDegrees(Math.asin(Math.max(-1, most))), distance);
    }
}
