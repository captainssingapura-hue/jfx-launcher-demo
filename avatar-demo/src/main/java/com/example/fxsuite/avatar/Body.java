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
 * result barely depends on how the time is split into frames. On its feet it grips and speed
 * follows quickly; in the air momentum carries.
 *
 * <p>Solid {@link Obstacles} stop it: to the avatar they are an upright cylinder,
 * {@link #RADIUS} round and {@link #TALL} high. It stands on whatever is under its feet, the
 * ground or the top of a block; steps up onto a ledge its feet come within {@link #STEP} of,
 * so a jump onto a crate lands on it; falls when it walks off an edge; stops climbing when its
 * head meets an underside; and slides along whatever it runs into, losing the speed that went
 * into it. Each step moves in sub-steps of at most {@link #SUBSTEP}, so not even a boost at a
 * slow frame rate carries it through a thin bar.
 *
 * @param speed    world units per second along the heading (negative backs up)
 * @param drift    world units per second to the right (negative to the left)
 * @param turnRate degrees per second, right when positive, smoothed
 * @param climb    world units per second upward
 * @param onGround standing on something: the ground or a block
 * @param phase    the walk cycle, in radians: advances with distance covered on its feet
 * @param time     seconds since the start, for idle and hover motion
 */
public record Body(Point3D position, double yaw, double speed, double drift, double turnRate, double climb,
                   boolean flying, boolean onGround, double phase, double time) {

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

    /** The avatar's size against obstacles: an upright cylinder. */
    public static final double RADIUS = 32;
    public static final double TALL = 200;
    /** A ledge no higher than this above the feet is stepped up onto. */
    public static final double STEP = 20;
    /** How far the feet must reach onto a top to stand on it: less than the full radius, so it slips off edges. */
    public static final double FOOTING = 16;
    /** The most it moves in one sub-step, in any direction. */
    public static final double SUBSTEP = 20;

    public static Body standing(final Point3D at, final double yaw) {
        return new Body(at, yaw, 0, 0, 0, 0, false, true, 0, 0);
    }

    public double height() {
        return -position.getY();
    }

    /** On its feet: not flying, and standing on something. */
    public boolean grounded() {
        return !flying && onGround;
    }

    /** How fast it is going across the ground, ahead and sideways together. */
    public double groundSpeed() {
        return Math.hypot(speed, drift);
    }

    public Point3D heading() {
        return heading(yaw);
    }

    /** After {@code seconds} of the given controls, on open ground. */
    public Body step(final Controls controls, final double seconds) {
        return step(controls, seconds, Obstacles.NONE);
    }

    /** After {@code seconds} of the given controls, among the given obstacles. */
    public Body step(final Controls controls, final double seconds, final Obstacles world) {
        final double top = flying ? (controls.run() ? BOOST : FLY) : (controls.run() ? RUN : WALK);
        // backing up is slower than going ahead
        final double wanted = controls.forward() * top * (controls.forward() < 0 ? 0.5 : 1);
        // in the air it still steers, but more slowly than on its feet
        final double grip = flying ? 2.5 : grounded() ? 10 : 3;
        final double newSpeed = ease(speed, wanted, grip, seconds);
        final double newDrift = ease(drift, controls.sideways() * top * SIDESTEP, grip, seconds);
        final double newYaw = wrap(yaw + controls.turn());
        final double newTurn = seconds > 0 ? ease(turnRate, controls.turn() / seconds, 8, seconds) : turnRate;
        double newClimb = flying
                ? ease(climb, controls.lift() * LIFT * (controls.run() ? 1.6 : 1), 4, seconds)
                : climb - GRAVITY * seconds;

        final Point3D ahead = heading(newYaw);
        final Point3D right = right(newYaw);
        double vx = ahead.getX() * newSpeed + right.getX() * newDrift;
        double vz = ahead.getZ() * newSpeed + right.getZ() * newDrift;
        // the average climb over the frame: exact under constant gravity, so a jump reaches the
        // same height whatever the frame rate
        double rise = (climb + newClimb) / 2;

        final double reach = Math.max(Math.hypot(vx, vz), Math.abs(rise)) * seconds;
        final int substeps = Math.max(1, (int) Math.ceil(reach / SUBSTEP));
        final double dt = seconds / substeps;
        double x = position.getX();
        double z = position.getZ();
        double height = height();
        boolean stillFlying = flying;
        boolean footing = false;
        for (int i = 0; i < substeps; i++) {
            final double before = height;
            x = clamp(x + vx * dt, -EDGE, EDGE);
            z = clamp(z + vz * dt, -EDGE, EDGE);
            height += rise * dt;

            // the head meets an underside it was below
            final double ceiling = Math.min(CEILING + TALL,
                    world.ceiling(x, z, RADIUS, before + TALL - 1e-6));
            if (height + TALL > ceiling) {
                height = ceiling - TALL;
                rise = Math.min(0, rise);
                newClimb = Math.min(0, newClimb);
            }
            // the feet meet a top they were above, or within a step of
            final double floor = world.floor(x, z, FOOTING, before + STEP);
            if (height <= floor) {
                height = floor;
                // a flight ends on touching down; flying level over a low ledge just lifts over it
                if (!stillFlying || rise < 0) {
                    stillFlying = false;
                    footing = true;
                    rise = 0;
                    newClimb = 0;
                }
            }
            // and everything else in the way is slid along; a ledge within a step is left alone,
            // to be stepped up onto once the feet are over it
            final Obstacles.Contact contact = world.slide(x, z, vx, vz, RADIUS, height + STEP, height + TALL);
            x = contact.x();
            z = contact.z();
            vx = contact.vx();
            vz = contact.vz();
        }

        // what is left of the velocity, back in the avatar's own terms
        final double keptSpeed = vx * ahead.getX() + vz * ahead.getZ();
        final double keptDrift = vx * right.getX() + vz * right.getZ();
        final double ground = Math.hypot(keptSpeed, keptDrift);
        final double newPhase = footing
                ? (phase + ground * seconds / stride(ground) * 2 * Math.PI) % (2 * Math.PI)
                : phase;
        return new Body(new Point3D(x, -height, z), newYaw, keptSpeed, keptDrift, newTurn, newClimb, stillFlying,
                footing, newPhase, time + seconds);
    }

    /** Up off whatever it stands on, if standing; otherwise unchanged. */
    public Body jumped() {
        return grounded() ? new Body(position, yaw, speed, drift, turnRate, JUMP, false, false, phase, time) : this;
    }

    /** Takes off with a little lift, or stops flying and falls, keeping its momentum. */
    public Body toggledFlight() {
        return flying
                ? new Body(position, yaw, speed, drift, turnRate, climb, false, false, phase, time)
                : new Body(position, yaw, speed, drift, turnRate, Math.max(climb, LIFT * 0.7), true, false,
                        phase, time);
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
