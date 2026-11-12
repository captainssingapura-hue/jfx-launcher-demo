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
     * The swing, spread, elbow and twist, in degrees, that bring an arm's hand as near to {@code target}
     * (in the torso's frame) as it can reach. Starts from {@code from} and closes in, a joint at
     * a time, in ever finer steps: a few hundred cheap sums a frame. Out of reach, the hand
     * stops as near as it can get.
     */
    static double[] solve(final boolean left, final double[] target, final double[] from) {
        // the elbow bend the distance alone calls for, by the law of cosines: a close target needs
        // the arm folded, a far one straight; then a few ways of pointing the shoulder, the best kept
        final double[] shoulder = {left ? -SHOULDER_OUT : SHOULDER_OUT, -SHOULDER_UP, 0};
        final double d = Math.min(UPPER + Arsenal.HAND, Math.sqrt(
                Math.pow(target[0] - shoulder[0], 2) + Math.pow(target[1] - shoulder[1], 2)
                        + Math.pow(target[2] - shoulder[2], 2)));
        final double inner = Math.toDegrees(Math.acos(Math.max(-1, Math.min(1,
                (UPPER * UPPER + Arsenal.HAND * Arsenal.HAND - d * d) / (2 * UPPER * Arsenal.HAND)))));
        final double bend = limit(2, 180 - inner);
        double[] best = null;
        double nearest = Double.POSITIVE_INFINITY;
        for (final double[] seed : new double[][] {
                from, {from[0], from[1], bend, 0}, {from[0] + 30, from[1] - 20, bend, -40},
                {from[0] - 30, from[1] + 20, bend, 40}, {from[0], from[1], bend, -80}}) {
            final double[] solved = descend(left, target, seed);
            final double gap = gap(left, solved, target);
            if (gap < nearest) {
                nearest = gap;
                best = solved;
            }
        }
        return best;
    }

    /** Closes in on the target from {@code from}, a joint at a time, in ever finer steps. */
    private static double[] descend(final boolean left, final double[] target, final double[] from) {
        final double[] angles = from.clone();
        double best = gap(left, angles, target);
        for (double step = 16; step > 0.05; step /= 2) {
            boolean better = true;
            while (better) {
                better = false;
                for (int joint = 0; joint < 4; joint++) {
                    for (final double way : new double[] {step, -step}) {
                        final double[] tried = angles.clone();
                        tried[joint] = limit(joint, tried[joint] + way);
                        final double gap = gap(left, tried, target);
                        if (gap < best - 1e-9) {
                            best = gap;
                            System.arraycopy(tried, 0, angles, 0, 4);
                            better = true;
                        }
                    }
                }
            }
        }
        return angles;
    }

    /** How far, squared, the hand is from {@code target} with these angles. */
    private static double gap(final boolean left, final double[] angles, final double[] target) {
        final double[] at = hand(left, angles[0], angles[1], angles[2], angles[3]);
        final double dx = at[0] - target[0];
        final double dy = at[1] - target[1];
        final double dz = at[2] - target[2];
        return dx * dx + dy * dy + dz * dz;
    }

    /** Within what a shoulder and an elbow can do. */
    private static double limit(final int joint, final double angle) {
        return switch (joint) {
            case 0 -> Math.max(-90, Math.min(180, angle));       // swing: back a little, up over the head
            case 1 -> Math.max(-120, Math.min(90, angle));       // spread: up across the chest, out to the side
            case 2 -> Math.max(0, Math.min(150, angle));         // elbow: straight to well bent, never backward
            default -> Math.max(-100, Math.min(100, angle));     // twist: the elbow turned in or out
        };
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
}
