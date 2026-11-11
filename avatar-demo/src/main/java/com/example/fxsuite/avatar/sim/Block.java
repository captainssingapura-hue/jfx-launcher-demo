package com.example.fxsuite.avatar.sim;


/**
 * A solid box in the world: a footprint centred on ({@code x}, {@code z}), half its width and
 * depth across, turned {@code yaw} degrees about the vertical as {@code Rotate(yaw, Y_AXIS)}
 * turns a node, and spanning heights {@code bottom} to {@code top} (up, so {@code -y} in
 * JavaFX terms). Pure maths.
 *
 * <p>The avatar meets it as an upright cylinder: {@link #push} says how far a circle of the
 * avatar's radius must move across the ground to stop overlapping the footprint. The test runs
 * in the block's own frame, where the footprint is an ordinary rectangle, and the answer is
 * turned back into the world's.
 */
public record Block(double x, double z, double halfWidth, double halfDepth, double yaw,
                    double bottom, double top) {

    /** A block standing on the ground. */
    public static Block standing(final double x, final double z, final double width, final double depth,
                                 final double height, final double yaw) {
        return new Block(x, z, width / 2, depth / 2, yaw, 0, height);
    }

    /**
     * How far, across the ground, a circle at ({@code px}, {@code pz}) must move to clear the
     * footprint; {@code null} if it already does. The push points straight out of the nearest
     * face, or corner, so pushing along it slides the circle round the block.
     */
    public Vec2 push(final double px, final double pz, final double radius) {
        final double c = Math.cos(Math.toRadians(yaw));
        final double s = Math.sin(Math.toRadians(yaw));
        final double dx = px - x;
        final double dz = pz - z;
        // into the block's frame: the inverse of Rotate(yaw, Y)
        final double lx = dx * c - dz * s;
        final double lz = dx * s + dz * c;
        final double ex = lx - Num.clamp(lx, -halfWidth, halfWidth);
        final double ez = lz - Num.clamp(lz, -halfDepth, halfDepth);
        final double gap = Math.hypot(ex, ez);
        final double plx;
        final double plz;
        if (gap > 0) {
            if (gap >= radius) {
                return null;
            }
            plx = ex / gap * (radius - gap);
            plz = ez / gap * (radius - gap);
        } else {
            // the centre is inside: out through whichever side is nearest
            final double outX = halfWidth - Math.abs(lx) + radius;
            final double outZ = halfDepth - Math.abs(lz) + radius;
            if (outX < outZ) {
                plx = (lx >= 0 ? 1 : -1) * outX;
                plz = 0;
            } else {
                plx = 0;
                plz = (lz >= 0 ? 1 : -1) * outZ;
            }
        }
        // back into the world's frame
        return new Vec2(plx * c + plz * s, -plx * s + plz * c);
    }

    /** Whether a circle at ({@code px}, {@code pz}) overlaps the footprint. */
    public boolean covers(final double px, final double pz, final double radius) {
        return push(px, pz, radius) != null;
    }
}
