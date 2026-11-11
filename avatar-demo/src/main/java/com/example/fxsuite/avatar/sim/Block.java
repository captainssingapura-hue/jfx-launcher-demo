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

    /**
     * How far a stroke must move to clear the footprint: a straight line from (ax, az) to (bx, bz),
     * {@code radius} thick each side, rounded at its ends; {@code null} if it already does. The
     * push is out of the face or corner nearest the line. So a line meeting a face square on is
     * pushed straight back, and only one caught on a corner slides round it.
     */
    public Vec2 push(final double ax, final double az, final double bx, final double bz, final double radius) {
        // most blocks are nowhere near: the line's nearest approach to the block's middle says so
        final double ex = bx - ax;
        final double ez = bz - az;
        final double along = Num.clamp(((x - ax) * ex + (z - az) * ez) / (ex * ex + ez * ez), 0, 1);
        if (Math.hypot(ax + ex * along - x, az + ez * along - z) > Math.hypot(halfWidth, halfDepth) + radius) {
            return null;
        }
        // the gap from the footprint is convex along the line: home in on its smallest
        double lo = 0;
        double hi = 1;
        for (int i = 0; i < 40; i++) {
            final double m1 = lo + (hi - lo) / 3;
            final double m2 = hi - (hi - lo) / 3;
            if (gap(ax + (bx - ax) * m1, az + (bz - az) * m1) <= gap(ax + (bx - ax) * m2, az + (bz - az) * m2)) {
                hi = m2;
            } else {
                lo = m1;
            }
        }
        final double t = (lo + hi) / 2;
        return push(ax + (bx - ax) * t, az + (bz - az) * t, radius);
    }

    /** How far, across the ground, a point is from the footprint; 0 inside. */
    private double gap(final double px, final double pz) {
        final double c = Math.cos(Math.toRadians(yaw));
        final double s = Math.sin(Math.toRadians(yaw));
        final double dx = px - x;
        final double dz = pz - z;
        final double lx = dx * c - dz * s;
        final double lz = dx * s + dz * c;
        return Math.hypot(lx - Num.clamp(lx, -halfWidth, halfWidth), lz - Num.clamp(lz, -halfDepth, halfDepth));
    }

    /** Whether a circle at ({@code px}, {@code pz}) overlaps the footprint. */
    public boolean covers(final double px, final double pz, final double radius) {
        return push(px, pz, radius) != null;
    }
}
