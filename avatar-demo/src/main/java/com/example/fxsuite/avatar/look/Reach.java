package com.example.fxsuite.avatar.look;

/**
 * Where an arm reaches, and how to reach somewhere: forward and inverse kinematics for the
 * box rig's two-part arms, in the torso's own frame (x to the right, y down, z ahead, from
 * between the hips), so a lean or a bank of the whole body changes nothing here. Pure maths.
 *
 * <p>The joints turn as {@link BoxRig} turns them: an arm hangs from its shoulder, swings
 * forward about x, spreads out to its side about z, and twists about its own length (y),
 * which turns the way its elbow bends; the forearm bends forward about x at the elbow,
 * {@link #UPPER} down the arm; the hand is {@link Arsenal#HAND} down the forearm. Angles come
 * as {swing, spread, elbow, twist}.
 */
final class Reach {

    /** The shoulders, out from the middle and up from the hips; the upper arm's length. */
    static final double SHOULDER_OUT = 41;
    static final double SHOULDER_UP = 144;
    static final double UPPER = 30;
    /** How far the elbow leans out to its side, against hanging straight down: a relaxed arm's. */
    private static final double ELBOW_OUT = 0.4;

    private Reach() {
    }

    /**
     * A point held in an arm's forearm frame, in the torso's frame: for the left arm or the
     * right, swung forward {@code swing}, spread out to its side {@code spread} (across the body
     * when negative), bent {@code elbow} at the elbow and twisted {@code twist} about its
     * length, all in degrees as a {@link Pose} has them.
     */
    static double[] point(final boolean left, final double swing, final double spread, final double elbow,
                          final double twist, final double[] inForearm) {
        // out to the side is a positive turn about z for the left arm, a negative one for the right
        final double signed = left ? spread : -spread;
        final double[] inArm = rotX(inForearm, elbow);
        inArm[1] += UPPER;
        final double[] inTorso = rotX(rotZ(rotY(inArm, twist), signed), swing);
        inTorso[0] += left ? -SHOULDER_OUT : SHOULDER_OUT;
        inTorso[1] -= SHOULDER_UP;
        return inTorso;
    }

    /** As {@link #point(boolean, double, double, double, double, double[])}, untwisted. */
    static double[] point(final boolean left, final double swing, final double spread, final double elbow,
                          final double[] inForearm) {
        return point(left, swing, spread, elbow, 0, inForearm);
    }

    /** Where an arm's hand is, in the torso's frame. */
    static double[] hand(final boolean left, final double swing, final double spread, final double elbow,
                         final double twist) {
        return point(left, swing, spread, elbow, twist, new double[] {0, Arsenal.HAND, 0});
    }

    /** Where an untwisted arm's hand is, in the torso's frame. */
    static double[] hand(final boolean left, final double swing, final double spread, final double elbow) {
        return hand(left, swing, spread, elbow, 0);
    }

    /**
     * The swing, spread, elbow and twist, in degrees, that put an arm's hand on {@code target}
     * (in the torso's frame); out of reach, as near as it can get, the arm pointing straight at
     * it. Worked out exactly, as for any two-part limb:
     * <ol>
     *   <li>the elbow's bend follows from how far the target is, by the law of cosines;</li>
     *   <li>that leaves the elbow somewhere on a circle round the line from shoulder to target,
     *       and it goes where a relaxed elbow would: as low as it can, a little out to its side;</li>
     *   <li>the shoulder's swing and spread point the upper arm at the elbow, and the twist turns
     *       the elbow's bend toward the target.</li>
     * </ol>
     * One answer for each target, and nearby targets give nearby answers: the arm moves smoothly
     * as what it holds does, and never turns about.
     */
    static double[] solve(final boolean left, final double[] target) {
        final double lower = Arsenal.HAND;
        final double[] shoulder = {left ? -SHOULDER_OUT : SHOULDER_OUT, -SHOULDER_UP, 0};
        final double[] toTarget = minus(target, shoulder);
        final double reach = Math.min(UPPER + lower - 1e-6,
                Math.max(Math.abs(UPPER - lower) + 1e-6, length(toTarget)));
        final double[] along = scale(toTarget, 1 / Math.max(1e-9, length(toTarget)));

        // where along the line the elbow's circle is, and how wide it is
        final double x = (UPPER * UPPER - lower * lower + reach * reach) / (2 * reach);
        final double radius = Math.sqrt(Math.max(0, UPPER * UPPER - x * x));
        // down (+y), and a little out to its side, flattened onto the circle's plane
        final double[] hint = {left ? -ELBOW_OUT : ELBOW_OUT, 1, 0};
        double[] across = minus(hint, scale(along, dot(hint, along)));
        if (length(across) < 1e-6) {
            across = minus(new double[] {0, 0, 1}, scale(along, along[2]));
        }
        across = scale(across, 1 / length(across));
        final double[] elbowAt = add(scale(along, x), scale(across, radius));
        final double[] handAt = scale(along, reach);

        // the upper arm along the elbow: (-sin spread, cos spread cos swing, cos spread sin swing)
        final double[] upper = scale(elbowAt, 1 / UPPER);
        final double signed = -Math.toDegrees(Math.asin(Math.max(-1, Math.min(1, upper[0]))));
        final double swing = Math.toDegrees(Math.atan2(upper[2], upper[1]));
        // the forearm, seen from the upper arm: (sin elbow sin twist, cos elbow, sin elbow cos twist)
        final double[] forearm = rotZ(rotX(scale(minus(handAt, elbowAt), 1 / lower), -swing), -signed);
        final double elbow = Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, forearm[1]))));
        final double twist = Math.toDegrees(Math.atan2(forearm[0], forearm[2]));
        return new double[] {swing, left ? signed : -signed, elbow, twist};
    }

    /** Turned about x as JavaFX turns it: +y toward +z for a positive angle. */
    static double[] rotX(final double[] v, final double degrees) {
        final double c = Math.cos(Math.toRadians(degrees));
        final double s = Math.sin(Math.toRadians(degrees));
        return new double[] {v[0], v[1] * c - v[2] * s, v[1] * s + v[2] * c};
    }

    /** Turned about y as JavaFX turns it: +z toward +x for a positive angle. */
    static double[] rotY(final double[] v, final double degrees) {
        final double c = Math.cos(Math.toRadians(degrees));
        final double s = Math.sin(Math.toRadians(degrees));
        return new double[] {v[0] * c + v[2] * s, v[1], -v[0] * s + v[2] * c};
    }

    /** Turned about z as JavaFX turns it: +x toward +y for a positive angle. */
    static double[] rotZ(final double[] v, final double degrees) {
        final double c = Math.cos(Math.toRadians(degrees));
        final double s = Math.sin(Math.toRadians(degrees));
        return new double[] {v[0] * c - v[1] * s, v[0] * s + v[1] * c, v[2]};
    }

    private static double[] add(final double[] a, final double[] b) {
        return new double[] {a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    private static double[] minus(final double[] a, final double[] b) {
        return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    private static double[] scale(final double[] a, final double by) {
        return new double[] {a[0] * by, a[1] * by, a[2] * by};
    }

    private static double dot(final double[] a, final double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static double length(final double[] a) {
        return Math.sqrt(dot(a, a));
    }
}
