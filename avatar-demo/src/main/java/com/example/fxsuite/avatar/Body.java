package com.example.fxsuite.avatar;

import javafx.geometry.Point3D;

/**
 * Where the avatar is and how it is moving. Pure maths: no scene graph.
 *
 * <p>JavaFX's y axis points down, so the height above the ground is {@code -y}. The avatar
 * faces along {@code (sin yaw, 0, cos yaw)}, so a positive turn goes right, matching
 * {@code Rotate(yaw, Y_AXIS)} on its node; its right is {@code (cos yaw, 0, -sin yaw)}.
 *
 * <p>It moves ahead and sideways (strafing) without turning; turning is separate, by however
 * much the controls say, and {@code turnRate} only records how fast that has been, for
 * banking into turns. Speeds ease toward what the controls ask for, exponentially, so the
 * result barely depends on how the time is split into frames. On the ground the feet grip and
 * speed follows quickly; in the air momentum carries.
 *
 * @param speed    world units per second along the heading (negative backs up)
 * @param drift    world units per second to the right (negative to the left)
 * @param turnRate degrees per second, right when positive, smoothed
 * @param climb    world units per second upward
 * @param phase    the walk cycle, in radians: advances with distance covered on the ground
 * @param time     seconds since the start, for idle and hover motion
 */
public record Body(Point3D position, double yaw, double speed, double drift, double turnRate, double climb,
                   boolean flying, double phase, double time) {

    /** World units per second. 100 units is a metre. */
    public static final double WALK = 320;
    public static final double RUN = 760;
    public static final double FLY = 1100;
    public static final double BOOST = 2000;
    /** Sideways is slower than ahead, by this much. */
    public static final double SIDESTEP = 0.75;
    /** A brisk turn, degrees per second: the most the body banks or leans into. */
    public static final double TURN = 140;
    /** Climb or descent while flying, units per second. */
    public static final double LIFT = 800;
    public static final double GRAVITY = 2600;
    public static final double JUMP = 950;
    /** The world is a square this far from the centre each way, and this tall. */
    public static final double EDGE = 12000;
    public static final double CEILING = 5000;

    public static Body standing(final Point3D at, final double yaw) {
        return new Body(at, yaw, 0, 0, 0, 0, false, 0, 0);
    }

    public double height() {
        return -position.getY();
    }

    /** On its feet: not flying, and touching the ground. */
    public boolean grounded() {
        return !flying && height() <= 0;
    }

    /** How fast it is going across the ground, ahead and sideways together. */
    public double groundSpeed() {
        return Math.hypot(speed, drift);
    }

    public Point3D heading() {
        return heading(yaw);
    }

    /** After {@code seconds} of the given controls. */
    public Body step(final Controls controls, final double seconds) {
        final double top = flying ? (controls.run() ? BOOST : FLY) : (controls.run() ? RUN : WALK);
        // backing up is slower than going ahead
        final double wanted = controls.forward() * top * (controls.forward() < 0 ? 0.5 : 1);
        final double grip = flying ? 2.5 : grounded() ? 10 : 0.8;
        final double newSpeed = ease(speed, wanted, grip, seconds);
        final double newDrift = ease(drift, controls.sideways() * top * SIDESTEP, grip, seconds);
        final double newYaw = wrap(yaw + controls.turn());
        final double newTurn = seconds > 0 ? ease(turnRate, controls.turn() / seconds, 8, seconds) : turnRate;

        double newClimb = flying
                ? ease(climb, controls.lift() * LIFT * (controls.run() ? 1.6 : 1), 4, seconds)
                : climb - GRAVITY * seconds;
        // the average of the climb over the frame: exact under constant gravity, so a jump
        // reaches the same height whatever the frame rate
        double height = height() + (climb + newClimb) / 2 * seconds;
        boolean stillFlying = flying;
        if (height <= 0) {
            // touching down ends a flight
            height = 0;
            newClimb = 0;
            stillFlying = false;
        }
        if (height >= CEILING) {
            height = CEILING;
            newClimb = Math.min(0, newClimb);
        }

        final Point3D moved = heading(newYaw).multiply(newSpeed * seconds)
                .add(right(newYaw).multiply(newDrift * seconds));
        final double x = clamp(position.getX() + moved.getX(), -EDGE, EDGE);
        final double z = clamp(position.getZ() + moved.getZ(), -EDGE, EDGE);

        final boolean onFeet = !stillFlying && height == 0;
        final double ground = Math.hypot(newSpeed, newDrift);
        final double newPhase = onFeet
                ? (phase + ground * seconds / stride(ground) * 2 * Math.PI) % (2 * Math.PI)
                : phase;
        return new Body(new Point3D(x, -height, z), newYaw, newSpeed, newDrift, newTurn, newClimb, stillFlying,
                newPhase, time + seconds);
    }

    /** Up off the ground, if standing on it; otherwise unchanged. */
    public Body jumped() {
        return grounded() ? new Body(position, yaw, speed, drift, turnRate, JUMP, false, phase, time) : this;
    }

    /** Takes off with a little lift, or stops flying and falls, keeping its momentum. */
    public Body toggledFlight() {
        return flying
                ? new Body(position, yaw, speed, drift, turnRate, climb, false, phase, time)
                : new Body(position, yaw, speed, drift, turnRate, Math.max(climb, LIFT * 0.7), true, phase, time);
    }

    /** One full walk cycle, two steps, in world units: longer strides when running. */
    static double stride(final double groundSpeed) {
        return 150 + 110 * Math.min(1, Math.abs(groundSpeed) / RUN);
    }

    static Point3D heading(final double yaw) {
        final double y = Math.toRadians(yaw);
        return new Point3D(Math.sin(y), 0, Math.cos(y));
    }

    static Point3D right(final double yaw) {
        final double y = Math.toRadians(yaw);
        return new Point3D(Math.cos(y), 0, -Math.sin(y));
    }

    /** {@code value} eased toward {@code target} for {@code seconds} at the given rate. */
    static double ease(final double value, final double target, final double rate, final double seconds) {
        return target + (value - target) * Math.exp(-rate * seconds);
    }

    /** An angle in degrees, brought into (-180, 180]. */
    static double wrap(final double degrees) {
        final double d = ((degrees % 360) + 360) % 360;
        return d > 180 ? d - 360 : d;
    }

    static double clamp(final double value, final double low, final double high) {
        return Math.max(low, Math.min(high, value));
    }
}
