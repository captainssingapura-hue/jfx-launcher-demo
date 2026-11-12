package com.example.fxsuite.avatar.sim;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Everything solid, and the three questions {@link Body#step} asks of it each sub-step: what
 * the feet can stand on, what the head bumps into, and how to slide clear of whatever is in
 * the way. The ground itself, at height 0, is always there underneath. Pure maths.
 *
 * <p>And the gates' openings, which are not solid at all, for telling when the avatar flies
 * through one.
 *
 * <p>Each block is a piece of the {@link Course}, by number ({@code pieces}), so a ray can say
 * which one it met, and a broken one can be taken out ({@link #without}).
 *
 * <p>A couple of hundred blocks are checked one by one: cheap at this size. A grid of cells,
 * each listing the blocks over it, would take over if the world grew a hundredfold.
 */
public record Obstacles(List<Block> blocks, List<Gate> gates, List<Integer> pieces) {

    /** Open ground and nothing else. */
    public static final Obstacles NONE = new Obstacles(List.of());

    public Obstacles {
        blocks = List.copyOf(blocks);
        gates = List.copyOf(gates);
        pieces = List.copyOf(pieces);
        if (pieces.size() != blocks.size()) {
            throw new IllegalArgumentException(blocks.size() + " blocks but " + pieces.size() + " piece numbers");
        }
    }

    /** Blocks and gates, the blocks numbered in order from 0. */
    public Obstacles(final List<Block> blocks, final List<Gate> gates) {
        this(blocks, gates, IntStream.range(0, blocks.size()).boxed().toList());
    }

    /** Blocks, and no gates to go through. */
    public Obstacles(final List<Block> blocks) {
        this(blocks, List.of());
    }

    /** The same world with piece {@code piece} gone from it: broken, and no longer in the way. */
    public Obstacles without(final int piece) {
        final int at = pieces.indexOf(piece);
        if (at < 0) {
            return this;
        }
        final List<Block> left = new ArrayList<>(blocks);
        final List<Integer> numbers = new ArrayList<>(pieces);
        left.remove(at);
        numbers.remove(at);
        return new Obstacles(left, gates, numbers);
    }

    /** What there is to stand on: how high, and what it is. */
    public record Footing(double height, Surface surface) {
    }

    /** Where the avatar ends up after sliding, and its velocity with the part into anything removed. */
    public record Contact(double x, double z, double vx, double vz, boolean blocked) {
    }

    /** How far a stroke was moved to slide clear, and its velocity with the part into anything removed. */
    public record Shift(double dx, double dz, double vx, double vz, boolean blocked) {
    }

    /**
     * The highest surface a circle at ({@code x}, {@code z}) can stand on, no higher than
     * {@code within}: the top of a block under it, or the ground.
     */
    public Footing footing(final double x, final double z, final double radius, final double within) {
        double floor = 0;
        for (final Block block : blocks) {
            if (block.top() > floor && block.top() <= within && block.covers(x, z, radius)) {
                floor = block.top();
            }
        }
        return new Footing(floor, floor > 0 ? Surface.BLOCK : Surface.GROUND);
    }

    /**
     * The first thing a ray from {@code from} along {@code direction} (a unit vector) meets
     * within {@code reach}: a block, which piece it is among them, or the ground; {@code null}
     * for open sky.
     */
    public Hit ray(final Vec3 from, final Vec3 direction, final double reach) {
        Hit first = null;
        // the ground, at height 0 (y down), from above
        if (direction.y() > 1e-12 && from.y() < 0) {
            final double t = -from.y() / direction.y();
            if (t <= reach) {
                final Vec3 down = from.add(direction.multiply(t));
                first = new Hit(new Vec3(down.x(), 0, down.z()),
                        new Vec3(0, -1, 0), Surface.GROUND, t);
            }
        }
        for (int i = 0; i < blocks.size(); i++) {
            final Hit hit = blocks.get(i).ray(from, direction, first == null ? reach : first.distance());
            if (hit != null && (first == null || hit.distance() < first.distance())) {
                first = hit.on(pieces.get(i));
            }
        }
        return first;
    }

    /** Just the height of the {@linkplain #footing footing}. */
    public double floor(final double x, final double z, final double radius, final double within) {
        return footing(x, z, radius, within).height();
    }

    /**
     * The lowest underside above a circle at ({@code x}, {@code z}), no lower than
     * {@code from}; infinity if open sky.
     */
    public double ceiling(final double x, final double z, final double radius, final double from) {
        double ceiling = Double.POSITIVE_INFINITY;
        for (final Block block : blocks) {
            if (block.bottom() < ceiling && block.bottom() >= from && block.covers(x, z, radius)) {
                ceiling = block.bottom();
            }
        }
        return ceiling;
    }

    /**
     * Pushes a circle clear of every block reaching into the heights {@code feet} to
     * {@code head}, and takes the part of its velocity heading into each one away, so running
     * into a wall at a slant slides along it and running straight at it stops. Two passes,
     * for a corner between two blocks.
     */
    public Contact slide(double x, double z, double vx, double vz, final double radius,
                         final double feet, final double head) {
        final Shift shift = slide(x, z, x, z, vx, vz, radius, feet, head);
        return new Contact(x + shift.dx(), z + shift.dz(), shift.vx(), shift.vz(), shift.blocked());
    }

    /**
     * As {@link #slide(double, double, double, double, double, double, double)}, for a stroke:
     * a line from (ax, az) to (bx, bz), {@code radius} thick, such as a wing seen from above.
     * Says how far it moved, rather than where it ended up.
     */
    public Shift slide(final double ax, final double az, final double bx, final double bz, double vx, double vz,
                       final double radius, final double feet, final double head) {
        double dx = 0;
        double dz = 0;
        boolean blocked = false;
        for (int pass = 0; pass < 2; pass++) {
            for (final Block block : blocks) {
                if (block.top() <= feet || block.bottom() >= head) {
                    continue;
                }
                final boolean point = ax == bx && az == bz;
                final Vec2 push = point ? block.push(ax + dx, az + dz, radius)
                        : block.push(ax + dx, az + dz, bx + dx, bz + dz, radius);
                if (push == null) {
                    continue;
                }
                blocked = true;
                dx += push.x();
                dz += push.z();
                final double length = push.magnitude();
                final double nx = push.x() / length;
                final double nz = push.z() / length;
                final double into = vx * nx + vz * nz;
                if (into < 0) {
                    vx -= nx * into;
                    vz -= nz * into;
                }
            }
        }
        return new Shift(dx, dz, vx, vz, blocked);
    }
}
