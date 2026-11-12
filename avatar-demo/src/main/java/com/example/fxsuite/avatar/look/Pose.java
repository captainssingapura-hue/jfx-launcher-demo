package com.example.fxsuite.avatar.look;

/**
 * How the figure holds itself this frame, in degrees and world units. Signs are in the
 * figure's own terms, so {@link BoxRig} turns them into JavaFX rotations in one place:
 *
 * @param lean       the whole body tipped forward (back when negative)
 * @param bank       rolled to the right (right side down), as into a right turn
 * @param lift       the body raised off its feet, for a walking bob or hovering
 * @param leftArm    the upper arm swung forward from the shoulder (back when negative); likewise
 *                   the others
 * @param leftSpread the left arm raised out to its side (across the body when negative);
 *                   likewise the right
 * @param leftElbow  the left forearm bent forward at the elbow, 0 straight; likewise the right
 * @param legSpread  both legs out to the sides, as when side-stepping
 * @param headYaw    the head turned right, relative to the body
 * @param headPitch  the head tipped up
 * @param grip       the right wrist turned, swinging what it holds forward about the hand: at 0 a
 *                   long weapon points on out along the arm, at 180 back past the shoulder
 * @param leftTwist  the left upper arm twisted about its own length, turning which way the elbow
 *                   bends; likewise the right. 0 bends it forward
 */
public record Pose(double lean, double bank, double lift,
                   double leftArm, double rightArm, double leftSpread, double rightSpread,
                   double leftElbow, double rightElbow,
                   double leftLeg, double rightLeg, double legSpread,
                   double headYaw, double headPitch, double grip,
                   double leftTwist, double rightTwist) {

    /** A pose with the upper arms untwisted, the elbows bending forward. */
    public Pose(final double lean, final double bank, final double lift,
                final double leftArm, final double rightArm, final double leftSpread, final double rightSpread,
                final double leftElbow, final double rightElbow,
                final double leftLeg, final double rightLeg, final double legSpread,
                final double headYaw, final double headPitch, final double grip) {
        this(lean, bank, lift, leftArm, rightArm, leftSpread, rightSpread, leftElbow, rightElbow,
                leftLeg, rightLeg, legSpread, headYaw, headPitch, grip, 0, 0);
    }

    /** The same pose, with the arms (and the wrist) held another way, untwisted. */
    Pose withArms(final double left, final double right, final double leftOut, final double rightOut,
                  final double leftBend, final double rightBend, final double wrist) {
        return withArms(left, right, leftOut, rightOut, leftBend, rightBend, wrist, 0, 0);
    }

    /** The same pose, with the arms (and the wrist) held another way, the upper arms twisted. */
    Pose withArms(final double left, final double right, final double leftOut, final double rightOut,
                  final double leftBend, final double rightBend, final double wrist,
                  final double leftTurn, final double rightTurn) {
        return new Pose(lean, bank, lift, left, right, leftOut, rightOut, leftBend, rightBend,
                leftLeg, rightLeg, legSpread, headYaw, headPitch, wrist, leftTurn, rightTurn);
    }
}
