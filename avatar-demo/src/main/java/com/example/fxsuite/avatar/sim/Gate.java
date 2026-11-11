package com.example.fxsuite.avatar.sim;

/**
 * A gate's opening, as data: the square hole its four bars stand round, which the bars as
 * blocks know nothing of. Centred over ({@code x}, {@code z}) at {@code height} above the
 * ground, turned by {@code yaw} as its bars are, and {@code span} across inside the bars.
 * Its face looks along {@link Num#heading} of the yaw; its width runs along {@link Num#right}.
 */
public record Gate(double x, double z, double yaw, double height, double span) {

    /**
     * Whether a body {@code tall} high, moving from {@code from} to {@code to}, went through
     * the opening: it crossed the gate's face, either way, with its middle inside the square.
     */
    public boolean passedBy(final Vec3 from, final Vec3 to, final double tall) {
        final Vec3 face = Num.heading(yaw);
        final double before = (from.x() - x) * face.x() + (from.z() - z) * face.z();
        final double after = (to.x() - x) * face.x() + (to.z() - z) * face.z();
        if ((before < 0) == (after < 0) || before == after) {
            return false;
        }
        // where it was as it crossed
        final Vec3 at = from.add(to.subtract(from).multiply(before / (before - after)));
        final Vec3 across = Num.right(yaw);
        final double side = (at.x() - x) * across.x() + (at.z() - z) * across.z();
        final double middle = -at.y() + tall / 2;
        return Math.abs(side) <= span / 2 && Math.abs(middle - height) <= span / 2;
    }
}
