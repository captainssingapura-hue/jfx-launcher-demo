package com.example.fxsuite.avatar.sim;

/**
 * A box whose two sides across it lean: square to its own frame, turned {@code yaw} about ({@code x}, {@code z}) as a
 * town turns a building, from {@code along0} to {@code along1} along; at its bottom, {@code low} up, from
 * {@code westLow} to {@code eastLow} across, at its top, {@code high} up, from {@code westHigh} to {@code eastHigh},
 * evenly between. A storey of a wall of a tower whose faces curve, its floor, a pane of its glass: what is in the way
 * between a body's feet and its head is what of it is between those heights, as it leans there.
 */
public record Lean(double x, double z, double yaw, double along0, double along1, double westLow, double eastLow,
                   double westHigh, double eastHigh, double low, double high) implements Volume {

    public Lean {
        if (!(along0 <= along1 && westLow <= eastLow && westHigh <= eastHigh && low <= high)) {
            throw new IllegalArgumentException("a leaning box from its least to its most each way");
        }
    }

    /** An upright box in its own frame: across from {@code west} to {@code east}, along, up. */
    public static Lean upright(final double west, final double east, final double along0, final double along1,
                               final double low, final double high) {
        return new Lean(0, 0, 0, along0, along1, west, east, west, east, low, high);
    }

    /**
     * The same, laid out about the world's origin, put {@code at}, {@code scale} times as big: turned with it about
     * the origin, moved and raised with it.
     */
    public Lean within(final Place at, final double scale) {
        final Vec3 right = Num.right(at.yaw());
        final Vec3 ahead = Num.heading(at.yaw());
        return new Lean(at.x() + (right.x() * x + ahead.x() * z) * scale, at.z() + (right.z() * x + ahead.z() * z) * scale,
                Num.wrap(yaw + at.yaw()), along0 * scale, along1 * scale, westLow * scale, eastLow * scale,
                westHigh * scale, eastHigh * scale, low * scale + at.height(), high * scale + at.height());
    }

    /** All of it, in its own frame: across, along, up. */
    public Box box() {
        return new Box(Math.min(westLow, westHigh), Math.max(eastLow, eastHigh), along0, along1, low, high);
    }

    private double share(final double up) {
        return high > low ? Num.clamp((up - low) / (high - low), 0, 1) : 0;
    }

    private double west(final double up) {
        return westLow + (westHigh - westLow) * share(up);
    }

    private double east(final double up) {
        return eastLow + (eastHigh - eastLow) * share(up);
    }

    /** What of it is between {@code from} and {@code to} up, seen from above, as a block: as far as it leans there. */
    private Block band(final double from, final double to) {
        final double f = Math.max(from, low);
        final double t = Math.min(to, high);
        final double west = Math.min(west(f), west(t));
        final double east = Math.max(east(f), east(t));
        final Vec3 right = Num.right(yaw);
        final Vec3 ahead = Num.heading(yaw);
        final double a = (west + east) / 2;
        final double l = (along0 + along1) / 2;
        return new Block(x + right.x() * a + ahead.x() * l, z + right.z() * a + ahead.z() * l, (east - west) / 2,
                (along1 - along0) / 2, yaw, f, t);
    }

    @Override
    public Bounds bounds() {
        final Box b = box();
        final Vec3 right = Num.right(yaw);
        final Vec3 ahead = Num.heading(yaw);
        final java.util.List<Vec2> corners = new java.util.ArrayList<>(4);
        for (int k = 0; k < 4; k++) {
            final double a = (k & 1) == 0 ? b.minX() : b.maxX();
            final double l = (k & 2) == 0 ? b.minZ() : b.maxZ();
            corners.add(new Vec2(x + right.x() * a + ahead.x() * l, z + right.z() * a + ahead.z() * l));
        }
        return Bounds.of(corners);
    }

    @Override
    public double low() {
        return low;
    }

    @Override
    public double high() {
        return high;
    }

    @Override
    public boolean covers(final double px, final double pz, final double radius) {
        return band(low, high).covers(px, pz, radius);
    }

    @Override
    public double topAt(final double px, final double pz, final double radius, final double below) {
        return high <= below && band(high, high).covers(px, pz, radius) ? high : Double.NEGATIVE_INFINITY;
    }

    @Override
    public double undersideAt(final double px, final double pz, final double radius, final double above) {
        return low >= above && band(low, low).covers(px, pz, radius) ? low : Double.POSITIVE_INFINITY;
    }

    @Override
    public Vec2 push(final double px, final double pz, final double radius, final double feet, final double head) {
        if (high <= feet || low >= head) {
            return null;
        }
        return band(feet, head).push(px, pz, radius);
    }

    @Override
    public Vec2 push(final double ax, final double az, final double bx, final double bz, final double radius,
                     final double feet, final double head) {
        if (high <= feet || low >= head) {
            return null;
        }
        return band(feet, head).push(ax, az, bx, bz, radius);
    }

    /**
     * Where a ray first enters it: in its own frame, the latest entry through any of its six sides and the earliest
     * exit, each side a plane, the two leaning ones leaning; {@code null} if it misses, or starts inside.
     */
    @Override
    public Hit ray(final Vec3 from, final Vec3 direction, final double reach) {
        final Vec3 right = Num.right(yaw);
        final Vec3 ahead = Num.heading(yaw);
        final double dx = from.x() - x;
        final double dz = from.z() - z;
        // across, along, up
        final double[] at = {dx * right.x() + dz * right.z(), dx * ahead.x() + dz * ahead.z(), -from.y()};
        final double[] go = {direction.x() * right.x() + direction.z() * right.z(),
                direction.x() * ahead.x() + direction.z() * ahead.z(), -direction.y()};
        final double kw = high > low ? (westHigh - westLow) / (high - low) : 0;
        final double ke = high > low ? (eastHigh - eastLow) / (high - low) : 0;
        // each side as n . p <= d, n pointing out
        final double[][] sides = {
                {0, -1, 0, -along0}, {0, 1, 0, along1}, {0, 0, -1, -low}, {0, 0, 1, high},
                {-1, 0, kw, -(westLow - kw * low)}, {1, 0, -ke, eastLow - ke * low}};
        double enter = Double.NEGATIVE_INFINITY;
        double exit = Double.POSITIVE_INFINITY;
        double[] face = null;
        for (final double[] s : sides) {
            final double going = s[0] * go[0] + s[1] * go[1] + s[2] * go[2];
            final double room = s[3] - (s[0] * at[0] + s[1] * at[1] + s[2] * at[2]);
            if (Math.abs(going) < 1e-12) {
                if (room < 0) {
                    return null;
                }
                continue;
            }
            final double t = room / going;
            if (going > 0) {
                exit = Math.min(exit, t);
            } else if (t > enter) {
                enter = t;
                face = s;
            }
        }
        if (face == null || enter > exit || enter < 0 || enter > reach) {
            return null;
        }
        final double length = Math.sqrt(face[0] * face[0] + face[1] * face[1] + face[2] * face[2]);
        final double na = face[0] / length;
        final double nl = face[1] / length;
        final double nu = face[2] / length;
        final Vec3 normal = new Vec3(na * right.x() + nl * ahead.x() + 0.0, 0.0 - nu, na * right.z() + nl * ahead.z() + 0.0);
        return new Hit(from.add(direction.multiply(enter)), normal, Surface.BLOCK, enter);
    }

    @Override
    public boolean overlaps(final Block block) {
        return band(low, high).overlaps(block);
    }
}
