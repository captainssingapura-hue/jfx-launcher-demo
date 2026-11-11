package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.sim.Arms;
import com.example.fxsuite.avatar.sim.Avatar;
import com.example.fxsuite.avatar.sim.Body;
import com.example.fxsuite.avatar.sim.Num;

/**
 * The procedural animation: a {@link Pose} worked out from how the {@link Body} is moving,
 * with no keyframes or recorded clips. Walking swings the limbs on a sine of the walk cycle,
 * opposite arm to leg, wider the faster it goes; side-stepping opens and closes the legs
 * instead; flying keeps it upright as a drone, tipped gently toward where it goes and rolled
 * into turns and sideways drift; standing still breathes. Pure maths.
 */
public final class Gait {

    /** The head turns no further than this either way, and tips no further than ±{@code HEAD_TIP}. */
    public static final double HEAD_TURN = 70;
    public static final double HEAD_TIP = 35;
    /** Flying flat out, it tips forward this far; drifting flat out sideways, it rolls this far. */
    public static final double DRONE_LEAN = 28;
    public static final double DRONE_BANK = 18;

    private Gait() {
    }

    /**
     * @param lookYaw the heading the player is looking along (the camera's), in degrees
     * @param lookUp  how far above the camera's usual tilt the player is looking, in degrees
     */
    public static Pose of(final Body body, final double lookYaw, final double lookUp) {
        final Pose moving = body.flying() ? flying(body)
                : body.grounded() ? onFoot(body)
                : airborne(body);
        // the head looks where the player looks, and holds level against the body's lean
        final double headYaw = Num.clamp(Num.wrap(lookYaw - body.yaw()), -HEAD_TURN, HEAD_TURN);
        final double headPitch = Num.clamp(lookUp * 0.6 + moving.lean() * 0.7, -HEAD_TIP, HEAD_TIP);
        return new Pose(moving.lean(), moving.bank(), moving.lift(), moving.leftArm(), moving.rightArm(),
                moving.leftSpread(), moving.rightSpread(), moving.leftElbow(), moving.rightElbow(),
                moving.leftLeg(), moving.rightLeg(), moving.legSpread(), headYaw, headPitch, 0);
    }

    /**
     * The pose with what the hands hold: as {@link #of(Body, double, double)}, then the arms
     * taken over by the weapon.
     * <ul>
     *   <li><b>Rifles:</b> held from the hip, as a soldier fires from it: the upper arms down along
     *       the body, clamping each rifle's stock against its side, the forearms folded forward
     *       along the rifles to the pistol grips. Aimed level whatever the body's lean, and up or
     *       down as the player looks. Each round kicks its forearm up, and it settles back in a
     *       few hundredths of a second.</li>
     *   <li><b>Glaive:</b> at rest it stands upright in the right hand, blade high above the head.
     *       A stroke winds up, comes through, and recovers, by the keyframes below.</li>
     * </ul>
     */
    public static Pose of(final Avatar avatar, final double lookYaw, final double lookUp) {
        final Pose pose = of(avatar.body(), lookYaw, lookUp);
        final Arms arms = avatar.arms();
        return switch (arms.held()) {
            case NONE -> pose;
            case RIFLES -> {
                // the body leans forward by a negative turn, the arms by positive ones: so to
                // hold a forearm level, the upper arm and elbow together turn as far as the lean
                final double forearm = 90 + pose.lean() - TUCK + Num.clamp(lookUp * 0.8, -AIM, AIM);
                yield pose.withArms(TUCK, TUCK, -PRESS, -PRESS,
                        forearm + kick(arms.sinceLeft()), forearm + kick(arms.sinceRight()), 0);
            }
            case GLAIVE -> {
                final double p = arms.progress();
                if (p < 0) {
                    // at rest: upright in the hand, swinging only a little as it walks
                    yield pose.withArms(pose.leftArm(), GLAIVE_REST[0] + 0.25 * pose.rightArm(), pose.leftSpread(),
                            GLAIVE_REST[1], pose.leftElbow(), GLAIVE_BEND, GLAIVE_REST[2]);
                }
                final double[] held = keyed(arms.strokeKind() == Arms.CLEAVE ? CLEAVE : SWEEP, p);
                yield pose.withArms(pose.leftArm(), held[1], pose.leftSpread(), held[2], pose.leftElbow(),
                        GLAIVE_BEND, held[3]);
            }
        };
    }

    /**
     * Rifles: the upper arms a little forward of hanging straight, and pressed in to the body by
     * this much, to clamp the stocks; how far up or down the look aims them; a round's kick, and
     * how fast it settles.
     */
    static final double TUCK = 6;
    static final double PRESS = 4;
    static final double AIM = 35;
    static final double RECOIL = 9;
    static final double RECOIL_SETTLE = 0.045;
    /** The glaive's arm is all but straight, so the long weapon swings from the shoulder. */
    static final double GLAIVE_BEND = 4;

    /** The glaive at rest: the arm (swing, spread) and the wrist. */
    static final double[] GLAIVE_REST = {12, 10, 180};

    /**
     * The glaive's strokes, as keyframes of (share of the stroke, arm swing, arm spread, wrist).
     * The blade comes through at {@link Arms#STRIKE}, between the wind-up and the follow-through.
     * A cleave goes up behind and over the head, and comes down in front. A sweep goes out to the
     * right, at shoulder height, and across to the left.
     */
    static final double[][] CLEAVE = {
            {0, 12, 10, 180},
            {0.30, -165, 18, 0},
            // on round over the top, so the angles go on past -180: -235 is 125 (up in front),
            // -305 is 55 (down in front), -348 is 12 (at rest again)
            {Arms.STRIKE, -235, 12, 0},
            {0.55, -305, 6, 0},
            {0.75, -320, 8, 70},
            {1, -348, 10, 180},
    };
    static final double[][] SWEEP = {
            {0, 12, 10, 180},
            {0.30, 82, 75, 0},
            {Arms.STRIKE, 85, 5, 0},
            {0.55, 85, -65, 0},
            {0.75, 50, -25, 90},
            {1, 12, 10, 180},
    };

    private static double kick(final double since) {
        return RECOIL * Math.exp(-since / RECOIL_SETTLE);
    }

    /** The keyframed values at share {@code p}, eased smoothly from one keyframe to the next. */
    static double[] keyed(final double[][] frames, final double p) {
        for (int i = 1; i < frames.length; i++) {
            if (p <= frames[i][0]) {
                final double[] a = frames[i - 1];
                final double[] b = frames[i];
                final double u = (p - a[0]) / (b[0] - a[0]);
                final double s = u * u * (3 - 2 * u);
                final double[] out = new double[a.length];
                for (int k = 0; k < a.length; k++) {
                    out[k] = a[k] + (b[k] - a[k]) * s;
                }
                return out;
            }
        }
        return frames[frames.length - 1].clone();
    }

    private static Pose onFoot(final Body body) {
        final double t = body.time();
        final double ground = body.groundSpeed();
        final double pace = Num.clamp(ground / Body.RUN, 0, 1);
        // how much of the movement is ahead (back when negative) and how much to the right
        final double ahead = ground > 1e-6 ? body.speed() / ground : 1;
        final double across = ground > 1e-6 ? body.drift() / ground : 0;
        // fade the swing in from a standstill, so stopping mid-stride settles rather than snaps
        final double amplitude = (18 + 37 * pace) * Num.clamp(pace / 0.1, 0, 1);
        final double swing = Math.sin(body.phase()) * amplitude * ahead;
        // side-stepping: the feet part and close twice a cycle
        final double spread = Math.abs(Math.sin(body.phase())) * amplitude * 0.45 * Math.abs(across);
        final double cos = Math.cos(body.phase());
        final double breath = Math.sin(t * 1.8);
        final double arms = 3 + breath + 10 * Math.abs(across) * pace;
        // the lift is never below standing height, so the feet never dip into the ground
        return new Pose(
                ahead >= 0 ? 2 + 10 * pace * ahead : 2 + 5 * pace * ahead,
                Num.clamp(body.turnRate() / Body.TURN, -1, 1) * 6 * pace + across * 7 * pace,
                pace * 5 * cos * cos + (breath + 1) * 0.4,
                -swing * 0.8, swing * 0.8, arms, arms,
                // the elbows bend more the harder it runs, and most on the arm swung forward
                10 + 25 * pace + Math.max(0, -swing) * 0.4, 10 + 25 * pace + Math.max(0, swing) * 0.4,
                swing, -swing, spread,
                0, 0, 0);
    }

    private static Pose airborne(final Body body) {
        // a jump or a fall: arms up and out, one knee up
        final double rising = Num.clamp(body.climb() / Body.JUMP, -1, 1);
        return new Pose(4, 0, 0, 30 + 15 * rising, 30 + 15 * rising, 28, 28, 25, 25, 32, -14, 0, 0, 0, 0);
    }

    private static Pose flying(final Body body) {
        final double t = body.time();
        final double pace = Num.clamp(body.speed() / Body.BOOST, -0.3, 1);
        final double cruise = Num.clamp(body.groundSpeed() / Body.FLY, 0, 1);
        final double up = Num.clamp(body.climb() / Body.LIFT, -1, 1);
        final double sway = Math.sin(t * 1.7) * 4;
        // as a drone flies: upright, tipped only gently toward where it is going and away from
        // a climb, rolled into sideways drift and turns, the arms a little out for balance
        return new Pose(
                DRONE_LEAN * pace - 4 * up,
                Num.clamp(body.turnRate() / Body.TURN, -1, 1) * (6 + 10 * cruise)
                        + Num.clamp(body.drift() / Body.FLY, -1, 1) * DRONE_BANK,
                Math.sin(t * 2.4) * 4 * (1 - cruise),
                8 - 14 * pace + sway * 0.5, 8 - 14 * pace - sway * 0.5, 14, 14, 15, 15,
                -4 - 8 * pace + sway, -4 - 8 * pace - sway, 4,
                0, 0, 0);
    }
}
