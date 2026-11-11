package com.example.fxsuite.avatar;

import javafx.geometry.Point2D;

import java.util.List;

/**
 * Everything solid, and the three questions {@link Body#step} asks of it each sub-step: what
 * the feet can stand on, what the head bumps into, and how to slide clear of whatever is in
 * the way. The ground itself, at height 0, is always there underneath. Pure maths.
 *
 * <p>A couple of hundred blocks are checked one by one: cheap at this size. A grid of cells,
 * each listing the blocks over it, would take over if the world grew a hundredfold.
 */
public record Obstacles(List<Block> blocks) {

    /** Open ground and nothing else. */
    public static final Obstacles NONE = new Obstacles(List.of());

    public Obstacles {
        blocks = List.copyOf(blocks);
    }

    /** Where the avatar ends up after sliding, and its velocity with the part into anything removed. */
    public record Contact(double x, double z, double vx, double vz, boolean blocked) {
    }

    /**
     * The highest surface a circle at ({@code x}, {@code z}) can stand on, no higher than
     * {@code within}: the top of a block under it, or the ground.
     */
    public double floor(final double x, final double z, final double radius, final double within) {
        double floor = 0;
        for (final Block block : blocks) {
            if (block.top() > floor && block.top() <= within && block.covers(x, z, radius)) {
                floor = block.top();
            }
        }
        return floor;
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
        boolean blocked = false;
        for (int pass = 0; pass < 2; pass++) {
            for (final Block block : blocks) {
                if (block.top() <= feet || block.bottom() >= head) {
                    continue;
                }
                final Point2D push = block.push(x, z, radius);
                if (push == null) {
                    continue;
                }
                blocked = true;
                x += push.getX();
                z += push.getY();
                final double length = push.magnitude();
                final double nx = push.getX() / length;
                final double nz = push.getY() / length;
                final double into = vx * nx + vz * nz;
                if (into < 0) {
                    vx -= nx * into;
                    vz -= nz * into;
                }
            }
        }
        return new Contact(x, z, vx, vz, blocked);
    }
}
