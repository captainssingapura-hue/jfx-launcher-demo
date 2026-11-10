package com.example.fxsuite.avatar;

/**
 * The procedural animation: a {@link Pose} worked out from how the {@link Body} is moving,
 * with no keyframes or recorded clips. Walking swings the limbs on a sine of the walk cycle,
 * opposite arm to leg, wider the faster it goes; flying tips the body forward with speed,
 * keeps the arms in along it and banks into turns; standing still breathes. Pure maths.
 */
public final class Gait {

    /** The head turns no further than this either way, and tips no further than ±{@code HEAD_TIP}. */
    public static final double HEAD_TURN = 70;
    public static final double HEAD_TIP = 35;

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
        final double headYaw = Body.clamp(Body.wrap(lookYaw - body.yaw()), -HEAD_TURN, HEAD_TURN);
        final double headPitch = Body.clamp(lookUp * 0.6 + moving.lean() * 0.7, -HEAD_TIP, HEAD_TIP);
        return new Pose(moving.lean(), moving.bank(), moving.lift(), moving.leftArm(), moving.rightArm(),
                moving.armSpread(), moving.leftLeg(), moving.rightLeg(), headYaw, headPitch, moving.thrust());
    }

    private static Pose onFoot(final Body body) {
        final double t = body.time();
        final double pace = Body.clamp(Math.abs(body.speed()) / Body.RUN, 0, 1);
        // fade the swing in from a standstill, so stopping mid-stride settles rather than snaps
        final double amplitude = (18 + 37 * pace) * Body.clamp(pace / 0.1, 0, 1);
        final double swing = Math.sin(body.phase()) * amplitude;
        final double cos = Math.cos(body.phase());
        final double breath = Math.sin(t * 1.8);
        return new Pose(
                body.speed() >= 0 ? 2 + 10 * pace : -3,
                Body.clamp(body.turnRate() / Body.TURN, -1, 1) * 6 * pace,
                pace * 5 * cos * cos + breath * 0.8,
                -swing * 0.8, swing * 0.8, 3 + breath,
                swing, -swing,
                0, 0, 0);
    }

    private static Pose airborne(final Body body) {
        // a jump or a fall: arms up and out, one knee up
        final double rising = Body.clamp(body.climb() / Body.JUMP, -1, 1);
        return new Pose(4, 0, 0, 30 + 15 * rising, 30 + 15 * rising, 28, 32, -14, 0, 0, 0);
    }

    private static Pose flying(final Body body) {
        final double t = body.time();
        final double pace = Body.clamp(body.speed() / Body.BOOST, -0.3, 1);
        final double cruise = Body.clamp(Math.abs(body.speed()) / Body.FLY, 0, 1);
        final double up = Body.clamp(body.climb() / Body.LIFT, -1, 1);
        final double sway = Math.sin(t * 1.7) * 4;
        return new Pose(
                8 + 62 * pace - 8 * up,
                Body.clamp(body.turnRate() / Body.TURN, -1, 1) * (12 + 28 * cruise),
                Math.sin(t * 2.4) * 6 * (1 - cruise),
                -8 - 10 * pace + sway * 0.5, -8 - 10 * pace - sway * 0.5, 6 + 12 * (1 - cruise),
                -6 - 10 * pace + sway, -6 - 10 * pace - sway,
                0, 0,
                Body.clamp(0.35 + 0.65 * Math.max(pace, up), 0.2, 1));
    }
}
