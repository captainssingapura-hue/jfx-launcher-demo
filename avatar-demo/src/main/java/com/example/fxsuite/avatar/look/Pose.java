package com.example.fxsuite.avatar.look;

/**
 * How the figure holds itself this frame, in degrees and world units. Signs are in the
 * figure's own terms, so {@link BoxRig} turns them into JavaFX rotations in one place:
 *
 * @param lean      the whole body tipped forward (back when negative)
 * @param bank      rolled to the right (right side down), as into a right turn
 * @param lift      the body raised off its feet, for a walking bob or hovering
 * @param leftArm   swung forward from the shoulder (back when negative); likewise the others
 * @param armSpread both arms raised out to the sides
 * @param legSpread both legs out to the sides, as when side-stepping
 * @param headYaw   the head turned right, relative to the body
 * @param headPitch the head tipped up
 */
public record Pose(double lean, double bank, double lift,
                   double leftArm, double rightArm, double armSpread,
                   double leftLeg, double rightLeg, double legSpread,
                   double headYaw, double headPitch) {
}
