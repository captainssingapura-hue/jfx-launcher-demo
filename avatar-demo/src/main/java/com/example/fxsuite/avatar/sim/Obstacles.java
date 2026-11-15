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
public record Obstacles(List<Block> blocks, List<Gate> gates, List<Integer> pieces, List<Volume> volumes,
                        List<Integer> volumePieces) {

    /** Open ground and nothing else. */
    public static final Obstacles NONE = new Obstacles(List.of());

    public Obstacles {
        blocks = List.copyOf(blocks);
        gates = List.copyOf(gates);
        pieces = List.copyOf(pieces);
        volumes = List.copyOf(volumes);
        volumePieces = List.copyOf(volumePieces);
        if (pieces.size() != blocks.size()) {
            throw new IllegalArgumentException(blocks.size() + " blocks but " + pieces.size() + " piece numbers");
        }
        if (volumePieces.size() != volumes.size()) {
            throw new IllegalArgumentException(volumes.size() + " volumes but " + volumePieces.size() + " piece numbers");
        }
    }

    /** Blocks, each its piece's number, and gates: nothing shaped more closely ({@link Volume}). */
    public Obstacles(final List<Block> blocks, final List<Gate> gates, final List<Integer> pieces) {
        this(blocks, gates, pieces, List.of(), List.of());
    }

    /** The same, and {@code more} shaped more closely, each its piece's number in {@code numbers}. */
    public Obstacles with(final List<Volume> more, final List<Integer> numbers) {
        final List<Volume> all = new ArrayList<>(volumes);
        all.addAll(more);
        final List<Integer> each = new ArrayList<>(volumePieces);
        each.addAll(numbers);
        return new Obstacles(blocks, gates, pieces, all, each);
    }

    /** Blocks and gates, the blocks numbered in order from 0. */
    public Obstacles(final List<Block> blocks, final List<Gate> gates) {
        this(blocks, gates, IntStream.range(0, blocks.size()).boxed().toList());
    }

    /** Blocks, and no gates to go through. */
    public Obstacles(final List<Block> blocks) {
        this(blocks, List.of());
    }

    /**
     * The same world with piece {@code piece} gone from it, every block of it: broken, and no
     * longer in the way.
     */
    public Obstacles without(final int piece) {
        if (!pieces.contains(piece) && !volumePieces.contains(piece)) {
            return this;
        }
        final List<Block> left = new ArrayList<>();
        final List<Integer> numbers = new ArrayList<>();
        for (int i = 0; i < blocks.size(); i++) {
            if (pieces.get(i) != piece) {
                left.add(blocks.get(i));
                numbers.add(pieces.get(i));
            }
        }
        final List<Volume> volumesLeft = new ArrayList<>();
        final List<Integer> volumeNumbers = new ArrayList<>();
        for (int i = 0; i < volumes.size(); i++) {
            if (volumePieces.get(i) != piece) {
                volumesLeft.add(volumes.get(i));
                volumeNumbers.add(volumePieces.get(i));
            }
        }
        return new Obstacles(left, gates, numbers, volumesLeft, volumeNumbers);
    }

    /**
     * The same world with piece {@code piece}'s blocks, in order, {@code now} instead: it has moved.
     * As many blocks as it had.
     */
    public Obstacles replaced(final int piece, final List<Block> now) {
        final List<Block> moved = new ArrayList<>(blocks);
        int k = 0;
        for (int i = 0; i < blocks.size() && k < now.size(); i++) {
            if (pieces.get(i) == piece) {
                moved.set(i, now.get(k++));
            }
        }
        return new Obstacles(moved, gates, pieces, volumes, volumePieces);
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
            if (block.bottom() < within && block.covers(x, z, radius)) {
                // a ramp's top where the circle stands on it
                final double top = block.topAt(x, z, radius);
                if (top > floor && top <= within) {
                    floor = top;
                }
            }
        }
        for (final Volume volume : volumes) {
            if (volume.low() < within && volume.covers(x, z, radius)) {
                final double top = volume.topAt(x, z, radius);
                // too steep to stand on (rising more than a step from under the circle to as far again
                // out): not footing, as a ramp that steep is not, so nothing creeps up it a step at a time
                if (volume.topAt(x, z, 2 * radius) - top > Body.STEP) {
                    continue;
                }
                if (top > floor && top <= within) {
                    floor = top;
                }
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
        for (int i = 0; i < volumes.size(); i++) {
            final Hit hit = volumes.get(i).ray(from, direction, first == null ? reach : first.distance());
            if (hit != null && (first == null || hit.distance() < first.distance())) {
                first = hit.on(volumePieces.get(i));
            }
        }
        return first;
    }

    /**
     * Which piece a body standing at ({@code x}, {@code z}), {@code height} up, stands on: the
     * piece whose top it is on; -1 on the ground or in the air.
     */
    public int under(final double x, final double z, final double radius, final double height) {
        for (int i = 0; i < blocks.size(); i++) {
            final Block block = blocks.get(i);
            if (block.covers(x, z, radius) && Math.abs(block.topAt(x, z, radius) - height) < 0.5) {
                return pieces.get(i);
            }
        }
        for (int i = 0; i < volumes.size(); i++) {
            final Volume volume = volumes.get(i);
            if (volume.covers(x, z, radius) && Math.abs(volume.topAt(x, z, radius) - height) < 0.5) {
                return volumePieces.get(i);
            }
        }
        return -1;
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
        for (final Volume volume : volumes) {
            final double underside = volume.undersideAt(x, z, radius);
            if (underside < ceiling && underside >= from) {
                ceiling = underside;
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
                final boolean point = ax == bx && az == bz;
                // a ramp is in the way only where its slope is higher than a step: up it, it is walked
                final double top = !block.sloped() ? block.top()
                        : Math.max(block.topAt(ax + dx, az + dz, radius), block.topAt(bx + dx, bz + dz, radius));
                if (top <= feet || block.bottom() >= head) {
                    continue;
                }
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
            for (final Volume volume : volumes) {
                final boolean point = ax == bx && az == bz;
                final Vec2 push = point ? volume.push(ax + dx, az + dz, radius, feet, head)
                        : volume.push(ax + dx, az + dz, bx + dx, bz + dz, radius, feet, head);
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
