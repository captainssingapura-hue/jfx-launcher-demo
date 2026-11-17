package com.example.fxsuite.avatar.sim;

/**
 * An object set at a place in the world, with its inside: points and boxes moved from the world into its own frame
 * and back. Its frame is the one a town places a building's parts in: across to the right of its heading, along
 * ahead, up from its place's height.
 */
public final class Placed {

    private final Place at;
    private final Inside inside;
    private final double rightX;
    private final double rightZ;
    private final double aheadX;
    private final double aheadZ;
    private final Box world;

    public Placed(final Place at, final Inside inside) {
        this.at = at;
        this.inside = inside;
        final Vec3 right = Num.right(at.yaw());
        final Vec3 ahead = Num.heading(at.yaw());
        rightX = right.x();
        rightZ = right.z();
        aheadX = ahead.x();
        aheadZ = ahead.z();
        world = world(inside.bounds());
    }

    public Place at() {
        return at;
    }

    public Inside inside() {
        return inside;
    }

    /** All it may take up in the world: the box round its inside's bounds, turned as it is. */
    public Box world() {
        return world;
    }

    public double across(final double x, final double z) {
        return (x - at.x()) * rightX + (z - at.z()) * rightZ;
    }

    public double along(final double x, final double z) {
        return (x - at.x()) * aheadX + (z - at.z()) * aheadZ;
    }

    public double up(final double height) {
        return height - at.height();
    }

    public double x(final double across, final double along) {
        return at.x() + rightX * across + aheadX * along;
    }

    public double z(final double across, final double along) {
        return at.z() + rightZ * across + aheadZ * along;
    }

    public double height(final double up) {
        return at.height() + up;
    }

    /** The cell of its inside a point in the world falls in, or -1: not in it. */
    public int cell(final double x, final double z, final double height) {
        return inside.cell(across(x, z), along(x, z), up(height));
    }

    /** The cells of its inside a box in the world reaches. */
    public int[] cells(final Box world) {
        return inside.cells(local(world));
    }

    /** The box, in its own frame, round a box in the world: as big, or bigger where it is turned. */
    public Box local(final Box world) {
        double minA = Double.POSITIVE_INFINITY;
        double maxA = Double.NEGATIVE_INFINITY;
        double minL = Double.POSITIVE_INFINITY;
        double maxL = Double.NEGATIVE_INFINITY;
        for (int k = 0; k < 4; k++) {
            final double x = (k & 1) == 0 ? world.minX() : world.maxX();
            final double z = (k & 2) == 0 ? world.minZ() : world.maxZ();
            final double a = across(x, z);
            final double l = along(x, z);
            minA = Math.min(minA, a);
            maxA = Math.max(maxA, a);
            minL = Math.min(minL, l);
            maxL = Math.max(maxL, l);
        }
        return new Box(minA, maxA, minL, maxL, up(world.low()), up(world.high()));
    }

    /** The box, in the world, round a box in its own frame. */
    public Box world(final Box local) {
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (int k = 0; k < 4; k++) {
            final double a = (k & 1) == 0 ? local.minX() : local.maxX();
            final double l = (k & 2) == 0 ? local.minZ() : local.maxZ();
            final double x = x(a, l);
            final double z = z(a, l);
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minZ = Math.min(minZ, z);
            maxZ = Math.max(maxZ, z);
        }
        return new Box(minX, maxX, minZ, maxZ, height(local.low()), height(local.high()));
    }
}
