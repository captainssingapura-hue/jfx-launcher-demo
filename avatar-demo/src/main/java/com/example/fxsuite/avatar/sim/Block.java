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
 *
 * <p>Its top may slope: a ramp. Then {@code toe} is how high the top is at the back of the
 * footprint (its own {@code -z}, against its heading), and it rises (or falls) evenly to
 * {@code top} at the front; a flat block has its toe as high as its top. What stands on it
 * stands on the slope where it is ({@link #topAt}), and what is shot at it meets the slope.
 */
public record Block(double x, double z, double halfWidth, double halfDepth, double yaw,
                    double bottom, double top, double toe) {

    /** A box, its top flat. */
    public Block(final double x, final double z, final double halfWidth, final double halfDepth, final double yaw,
                 final double bottom, final double top) {
        this(x, z, halfWidth, halfDepth, yaw, bottom, top, top);
    }

    /**
     * A ramp standing on the ground, {@code width} across, from its foot at ({@code fromX},
     * {@code fromZ}), {@code low} high, up to its head at ({@code toX}, {@code toZ}), {@code high}
     * high: solid down to the ground under all of it.
     */
    public static Block ramp(final double fromX, final double fromZ, final double low, final double toX,
                             final double toZ, final double high, final double width) {
        final double length = Math.hypot(toX - fromX, toZ - fromZ);
        final double yaw = Math.toDegrees(Math.atan2(toX - fromX, toZ - fromZ));
        return new Block((fromX + toX) / 2, (fromZ + toZ) / 2, width / 2, length / 2, yaw, 0, high, low);
    }

    /** Whether its top slopes. */
    public boolean sloped() {
        return toe != top;
    }

    /** The highest its top reaches: at the front, or at the back if it slopes down. */
    public double highest() {
        return Math.max(top, toe);
    }

    /**
     * How high its top is under a circle at ({@code px}, {@code pz}), {@code radius} across: where
     * the circle reaches highest on it. Its top, if it is flat.
     */
    public double topAt(final double px, final double pz, final double radius) {
        if (!sloped()) {
            return top;
        }
        final double c = Math.cos(Math.toRadians(yaw));
        final double s = Math.sin(Math.toRadians(yaw));
        final double along = (px - x) * s + (pz - z) * c;
        final double reach = top > toe ? along + radius : along - radius;
        final double share = (Num.clamp(reach, -halfDepth, halfDepth) + halfDepth) / (2 * halfDepth);
        return toe + (top - toe) * share;
    }

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
        // far off, whichever way it is turned: no nearer than this can it reach, so it does not
        final double reach = halfWidth + halfDepth + radius;
        if (Math.abs(px - x) > reach || Math.abs(pz - z) > reach) {
            return null;
        }
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

    /**
     * Where a ray from {@code from} along {@code direction} (a unit vector) first enters the
     * block, within {@code reach}; {@code null} if it misses, or starts inside. Worked out in the
     * block's own frame, where it is a plain box: slab by slab, the latest entry and the earliest
     * exit.
     */
    public Hit ray(final Vec3 from, final Vec3 direction, final double reach) {
        final double c = Math.cos(Math.toRadians(yaw));
        final double s = Math.sin(Math.toRadians(yaw));
        final double dx = from.x() - x;
        final double dz = from.z() - z;
        // across, up and along, in the block's frame (up is -y)
        final double[] at = {dx * c - dz * s, -from.y(), dx * s + dz * c};
        final double[] go = {direction.x() * c - direction.z() * s, -direction.y(),
                direction.x() * s + direction.z() * c};
        final double[] low = {-halfWidth, bottom, -halfDepth};
        final double[] high = {halfWidth, highest(), halfDepth};
        double enter = Double.NEGATIVE_INFINITY;
        double exit = Double.POSITIVE_INFINITY;
        int face = -1;
        for (int axis = 0; axis < 3; axis++) {
            if (Math.abs(go[axis]) < 1e-12) {
                if (at[axis] < low[axis] || at[axis] > high[axis]) {
                    return null;
                }
                continue;
            }
            final double t1 = (low[axis] - at[axis]) / go[axis];
            final double t2 = (high[axis] - at[axis]) / go[axis];
            final double near = Math.min(t1, t2);
            if (near > enter) {
                enter = near;
                face = axis;
            }
            exit = Math.min(exit, Math.max(t1, t2));
        }
        // a sloped top: under the plane up - rise * along <= level, as well as in the box
        final double rise = sloped() ? (top - toe) / (2 * halfDepth) : 0;
        if (sloped()) {
            final double level = toe + rise * halfDepth;
            final double outside = at[1] - rise * at[2] - level;
            final double going = go[1] - rise * go[2];
            if (Math.abs(going) < 1e-12) {
                if (outside > 0) {
                    return null;
                }
            } else {
                final double t = -outside / going;
                if (going > 0) {
                    exit = Math.min(exit, t);
                } else if (t > enter) {
                    enter = t;
                    face = 3;
                }
            }
        }
        if (enter > exit || enter < 0 || enter > reach) {
            return null;
        }
        // out of the face it came through, against the way it was going: the slope's, square to it
        final double[] out = new double[3];
        if (face == 3) {
            final double length = Math.sqrt(1 + rise * rise);
            out[1] = 1 / length;
            out[2] = -rise / length;
        } else {
            out[face] = go[face] > 0 ? -1 : 1;
        }
        // (0.0 - and + 0.0 keep a zero from coming out as -0.0, which is not equal to 0.0)
        final Vec3 normal = new Vec3(out[0] * c + out[2] * s + 0.0, 0.0 - out[1], -out[0] * s + out[2] * c + 0.0);
        return new Hit(from.add(direction.multiply(enter)), normal, Surface.BLOCK, enter);
    }

    /**
     * Part way, {@code share} from 0 to 1, from {@code before} to this: where something that moves
     * is drawn between two steps. Its middle, its heights and its yaw, the short way round.
     */
    public Block between(final Block before, final double share) {
        return new Block(before.x + (x - before.x) * share, before.z + (z - before.z) * share, halfWidth, halfDepth,
                Num.wrap(before.yaw + Num.wrap(yaw - before.yaw) * share), before.bottom + (bottom - before.bottom) * share,
                before.top + (top - before.top) * share, before.toe + (toe - before.toe) * share);
    }

    /**
     * A point in the world, in the block's own frame: from its middle, and turned back by its yaw.
     * The frame a node is in when it is placed at the middle and turned with {@code Rotate(yaw, Y)},
     * so something put there at this point is at {@code world} now, and goes with the block when
     * it moves.
     */
    public Vec3 toLocal(final Vec3 world) {
        final Vec3 turned = turnToLocal(new Vec3(world.x() - x, 0, world.z() - z));
        return new Vec3(turned.x(), world.y() + (bottom + top) / 2, turned.z());
    }

    /** A point in the block's own frame, in the world: the inverse of {@link #toLocal}. */
    public Vec3 toWorld(final Vec3 local) {
        final double c = Math.cos(Math.toRadians(yaw));
        final double s = Math.sin(Math.toRadians(yaw));
        // Rotate(yaw, Y)
        return new Vec3(x + local.x() * c + local.z() * s, local.y() - (bottom + top) / 2,
                z - local.x() * s + local.z() * c);
    }

    /** A direction in the world, in the block's own frame: turned back by its yaw. */
    public Vec3 turnToLocal(final Vec3 direction) {
        final double c = Math.cos(Math.toRadians(yaw));
        final double s = Math.sin(Math.toRadians(yaw));
        // the inverse of Rotate(yaw, Y)
        return new Vec3(direction.x() * c - direction.z() * s, direction.y(), direction.x() * s + direction.z() * c);
    }

    /**
     * Whether this and {@code other} take up any of the same space: their heights cross, and their
     * footprints, turned as they are, cross too (no line across the ground parts them).
     */
    public boolean overlaps(final Block other) {
        if (top <= other.bottom || other.top <= bottom) {
            return false;
        }
        for (final Block side : new Block[] {this, other}) {
            for (final Vec3 axis : new Vec3[] {Num.right(side.yaw), Num.heading(side.yaw)}) {
                final double apart = Math.abs((other.x - x) * axis.x() + (other.z - z) * axis.z());
                if (apart >= reach(axis) + other.reach(axis)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** How far the footprint reaches from its middle along {@code axis}, a unit vector across the ground. */
    private double reach(final Vec3 axis) {
        final Vec3 across = Num.right(yaw);
        final Vec3 along = Num.heading(yaw);
        return halfWidth * Math.abs(across.x() * axis.x() + across.z() * axis.z())
                + halfDepth * Math.abs(along.x() * axis.x() + along.z() * axis.z());
    }

    /** Whether a circle at ({@code px}, {@code pz}) overlaps the footprint. */
    public boolean covers(final double px, final double pz, final double radius) {
        return push(px, pz, radius) != null;
    }
}
