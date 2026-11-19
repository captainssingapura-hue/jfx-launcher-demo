package com.example.fxsuite.avatar.sim;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Everything solid, and the three questions {@link Body#step} asks of it each sub-step: what
 * the feet can stand on, what the head bumps into, and how to slide clear of whatever is in
 * the way. The ground itself is always there underneath: level at 0, or the {@link Land}, rising and
 * falling, laid out as it is. Pure maths.
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
                        List<Integer> volumePieces, Land land) {

    /** Open ground and nothing else. */
    public static final Obstacles NONE = new Obstacles(List.of());

    public Obstacles {
        blocks = List.copyOf(blocks);
        gates = List.copyOf(gates);
        pieces = List.copyOf(pieces);
        volumes = volumes instanceof Hollowed ? volumes : List.copyOf(volumes);
        volumePieces = List.copyOf(volumePieces);
        if (pieces.size() != blocks.size()) {
            throw new IllegalArgumentException(blocks.size() + " blocks but " + pieces.size() + " piece numbers");
        }
        if (volumePieces.size() != volumes.size()) {
            throw new IllegalArgumentException(volumes.size() + " volumes but " + volumePieces.size() + " piece numbers");
        }
        if (land == null) {
            throw new IllegalArgumentException("the ground is there, if level");
        }
    }

    /** As much, on level ground. */
    public Obstacles(final List<Block> blocks, final List<Gate> gates, final List<Integer> pieces, final List<Volume> volumes,
                     final List<Integer> volumePieces) {
        this(blocks, gates, pieces, volumes, volumePieces, Land.LEVEL);
    }

    /** The same, on {@code ground}. */
    public Obstacles on(final Land ground) {
        return new Obstacles(blocks, gates, pieces, volumes, volumePieces, ground);
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
        return new Obstacles(blocks, gates, pieces, all, each, land);
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
        // a piece shaped by no volume (a vehicle, leaving itself out of its own way) leaves the volumes as
        // they are, the very same list: so where they are is not worked out again ({@link #volumesNear})
        if (!volumePieces.contains(piece)) {
            return new Obstacles(left, gates, numbers, volumes, volumePieces, land);
        }
        // its volumes left where they were, each in no one's way: the rest where they were among them, so where they
        // are is known already, from the volumes as they were first indexed ({@link #volumesNear})
        final Volume[] hollowed = volumes.toArray(new Volume[0]);
        for (int i = 0; i < hollowed.length; i++) {
            if (volumePieces.get(i) == piece) {
                hollowed[i] = new Gone(hollowed[i].bounds(), hollowed[i].low(), hollowed[i].high());
            }
        }
        return new Obstacles(left, gates, numbers,
                new Hollowed(volumes instanceof Hollowed h ? h.indexed : volumes, hollowed), volumePieces, land);
    }

    /**
     * Volumes as {@code indexed} has them, some of them gone ({@link Gone}): each where it was among them, so the
     * index of where they are ({@link VolumeGrid}) is {@code indexed}'s. Not to be changed.
     */
    private static final class Hollowed extends java.util.AbstractList<Volume> implements java.util.RandomAccess {

        private final List<Volume> indexed;
        private final Volume[] volumes;

        Hollowed(final List<Volume> indexed, final Volume[] volumes) {
            this.indexed = indexed;
            this.volumes = volumes;
        }

        @Override
        public Volume get(final int index) {
            return volumes[index];
        }

        @Override
        public int size() {
            return volumes.length;
        }
    }

    /** A volume that was, left out: where it was, as far up as it went, in no one's way, under or over nothing. */
    record Gone(Volume.Bounds bounds, double low, double high) implements Volume {

        @Override
        public boolean covers(final double x, final double z, final double radius) {
            return false;
        }

        @Override
        public boolean steep(final double x, final double z, final double radius, final double top) {
            return false;
        }

        @Override
        public double topAt(final double x, final double z, final double radius, final double below) {
            return Double.NEGATIVE_INFINITY;
        }

        @Override
        public double undersideAt(final double x, final double z, final double radius, final double above) {
            return Double.POSITIVE_INFINITY;
        }

        @Override
        public Vec2 push(final double x, final double z, final double radius, final double feet, final double head) {
            return null;
        }

        @Override
        public Vec2 push(final double ax, final double az, final double bx, final double bz, final double radius,
                         final double feet, final double head) {
            return null;
        }

        @Override
        public Hit ray(final Vec3 from, final Vec3 direction, final double reach) {
            return null;
        }

        @Override
        public boolean overlaps(final Block block) {
            return false;
        }
    }

    /** The same world with volume {@code was} of piece {@code piece} {@code now} instead: a building's door opened. */
    public Obstacles reshaped(final int piece, final Volume was, final Volume now) {
        final List<Volume> changed = new ArrayList<>(volumes);
        for (int i = 0; i < changed.size(); i++) {
            if (volumePieces.get(i) == piece && changed.get(i) == was) {
                changed.set(i, now);
            }
        }
        return new Obstacles(blocks, gates, pieces, changed, volumePieces, land);
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
        return new Obstacles(moved, gates, pieces, volumes, volumePieces, land);
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
    /**
     * The last list of volumes asked about, and where they are ({@link VolumeGrid}): kept, as the
     * same list goes from step to step, the world remade round it, until something in it is broken.
     */
    private static volatile Object[] grid;

    /**
     * Which of its volumes may be under or over a circle at ({@code x}, {@code z}), {@code radius}
     * round, by their numbers among them, in order: the others are nowhere near.
     */
    public int[] volumesNear(final double x, final double z, final double radius) {
        // some of them left out, each where it was: where they are is where the volumes they were taken from are
        final List<Volume> indexed = volumes instanceof Hollowed h ? h.indexed : volumes;
        final Object[] kept = grid;
        if (kept != null && kept[0] == indexed) {
            return ((VolumeGrid) kept[1]).near(x, z, radius);
        }
        final VolumeGrid made = new VolumeGrid(indexed);
        grid = new Object[] {indexed, made};
        return made.near(x, z, radius);
    }

    public Footing footing(final double x, final double z, final double radius, final double within) {
        // the land, however high: what is steeper than a step is kept out of by whatever moves ({@link Body})
        final double ground = land.height(x, z);
        double floor = ground;
        for (final Block block : blocks) {
            if (block.bottom() < within && block.covers(x, z, radius)) {
                // a ramp's top where the circle stands on it
                final double top = block.topAt(x, z, radius);
                if (top > floor && top <= within) {
                    floor = top;
                }
            }
        }
        for (final int near : volumesNear(x, z, radius)) {
            final Volume volume = volumes.get(near);
            if (volume.low() < within && volume.covers(x, z, radius)) {
                final double top = volume.topAt(x, z, radius, within);
                // too steep to stand on (rising more than a step from under the circle to as far again
                // out): not footing, as a ramp that steep is not, so nothing creeps up it a step at a time
                if (top == Double.NEGATIVE_INFINITY || volume.steep(x, z, radius, top)) {
                    continue;
                }
                if (top > floor && top <= within) {
                    floor = top;
                }
            }
        }
        return new Footing(floor, floor > ground ? Surface.BLOCK : Surface.GROUND);
    }

    /**
     * The first thing a ray from {@code from} along {@code direction} (a unit vector) meets
     * within {@code reach}: a block, which piece it is among them, or the ground; {@code null}
     * for open sky.
     */
    public Hit ray(final Vec3 from, final Vec3 direction, final double reach) {
        Hit first = land.level() ? null : landed(from, direction, reach);
        // the ground, at height 0 (y down), from above
        if (land.level() && direction.y() > 1e-12 && from.y() < 0) {
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
        // a short ray (a falling bomb's step, say): only the volumes near it; a long one, all of them
        final boolean near = reach <= SHORT_RAY;
        final int[] which = near ? volumesNear(from.x() + direction.x() * reach / 2, from.z() + direction.z() * reach / 2,
                reach / 2 + 1) : null;
        final int count = near ? which.length : volumes.size();
        for (int k = 0; k < count; k++) {
            final int i = near ? which[k] : k;
            final Hit hit = volumes.get(i).ray(from, direction, first == null ? reach : first.distance());
            if (hit != null && (first == null || hit.distance() < first.distance())) {
                first = hit.on(volumePieces.get(i));
            }
        }
        return first;
    }

    /**
     * The first wall of a building ({@link Interior}) a ray from {@code from} along {@code direction} meets within
     * {@code reach}, and nothing else: not the ground, not a vehicle; {@code null} if none. What a camera is kept
     * inside a room by.
     */
    public Hit walls(final Vec3 from, final Vec3 direction, final double reach) {
        Hit first = null;
        for (final int i : volumesNear(from.x() + direction.x() * reach / 2, from.z() + direction.z() * reach / 2,
                reach / 2 + 1)) {
            if (volumes.get(i) instanceof Interior building) {
                final Hit hit = building.ray(from, direction, first == null ? reach : first.distance());
                if (hit != null && (first == null || hit.distance() < first.distance())) {
                    first = hit.on(volumePieces.get(i));
                }
            }
        }
        return first;
    }

    /** The shortest and the longest stride a ray takes over the land, looking for where it meets it. */
    private static final double STRIDE = 50;
    private static final double LONGEST_STRIDE = 2_000;

    /**
     * Where a ray from {@code from} along {@code direction} first meets the land, within {@code reach}; null if
     * it does not, or starts under it. It strides along, each stride as long as it is high over the land
     * (the land rising no steeper than its run, it cannot be stepped over), and, once under, closes in on
     * where it went in by halves.
     */
    private Hit landed(final Vec3 from, final Vec3 direction, final double reach) {
        if (-from.y() < land.height(from.x(), from.z())) {
            return null;
        }
        final double top = land.highest();
        double t = 0;
        double stride = 0;
        while (t <= reach) {
            final Vec3 at = from.add(direction.multiply(t));
            final double over = -at.y() - land.height(at.x(), at.z());
            if (over < 0) {
                double low = Math.max(0, t - stride);
                double high = t;
                for (int k = 0; k < 20; k++) {
                    final double mid = (low + high) / 2;
                    final Vec3 m = from.add(direction.multiply(mid));
                    if (-m.y() - land.height(m.x(), m.z()) < 0) {
                        high = mid;
                    } else {
                        low = mid;
                    }
                }
                final Vec3 hit = from.add(direction.multiply(high));
                final Vec2 rise = land.rise(hit.x(), hit.z(), STRIDE);
                return new Hit(new Vec3(hit.x(), -land.height(hit.x(), hit.z()), hit.z()),
                        new Vec3(-rise.x(), -1, -rise.z()).normalize(), Surface.GROUND, high);
            }
            // going up, and over the highest of the land: it never comes down to it
            if (direction.y() <= 0 && -at.y() > top) {
                return null;
            }
            if (t == reach) {
                break;
            }
            stride = Math.min(Num.clamp(over, STRIDE, LONGEST_STRIDE), reach - t);
            t += stride;
        }
        return null;
    }

    /** A ray no longer than this looks only among the volumes near it ({@link #volumesNear}): a few grid cells' worth. */
    private static final double SHORT_RAY = 4 * VolumeGrid.CELL;

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
        for (final int i : volumesNear(x, z, radius)) {
            final Volume volume = volumes.get(i);
            if (volume.covers(x, z, radius) && Math.abs(volume.topAt(x, z, radius, height + 0.5) - height) < 0.5) {
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
        for (final int near : volumesNear(x, z, radius)) {
            final double underside = volumes.get(near).undersideAt(x, z, radius, from);
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
    /** How much further than its own size about a stroke a volume may be and yet push it, once pushed by others. */
    private static final double SLACK = 200;

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
            // the volumes near the stroke where it now is, and as far again as it may yet be pushed
            final double reach = Math.hypot(bx - ax, bz - az) / 2 + 4 * radius + SLACK;
            for (final int near : volumesNear((ax + bx) / 2 + dx, (az + bz) / 2 + dz, reach)) {
                final Volume volume = volumes.get(near);
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
