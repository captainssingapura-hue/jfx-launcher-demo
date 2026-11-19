package com.example.fxsuite.avatar.lifts;

import com.example.fxsuite.avatar.build.Material;
import com.example.fxsuite.avatar.build.Role;
import com.example.fxsuite.avatar.build.Solid;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Place;

import java.util.ArrayList;
import java.util.List;

/**
 * A paternoster: a chain of open cabins going round a loop and never stopping, up one shaft ({@code upX}) and down
 * the other ({@code downX}), side by side, over the top and under the foot of the loop in a half circle each, the
 * cabins upright all the way. Stepped into and out of as a cabin's floor passes a landing; its front open, toward
 * along.
 *
 * <p>Where each cabin is is a function of time alone: {@code cabins} of them evenly round the loop, at {@code speed}
 * along it; a cabin's floor from {@code bottom} up to {@code top} and down again.
 *
 * @param upX    across, the middle of the shaft its cabins go up
 * @param downX  across, the middle of the shaft its cabins come down
 * @param z      along, the middle of both shafts
 * @param bottom how low a cabin's floor goes before it turns under
 * @param top    how high a cabin's floor goes before it turns over
 * @param cabins how many cabins
 * @param speed  how fast they go, cm/s
 * @param wide   how wide a cabin is, across
 * @param deep   how deep a cabin is, along
 * @param high   how high a cabin is inside
 */
record Paternoster(double upX, double downX, double z, double bottom, double top, int cabins, double speed,
                   double wide, double deep, double high) {

    /** A cabin as they mostly are: so wide across, so deep along, so high inside. Its floor so thick, its walls. */
    static final double WIDE = 180;
    static final double DEEP = 120;
    static final double HIGH = 220;
    static final double FLOOR = 8;
    static final double WALL = 4;
    /** Between its two shafts' cabins, and round them in their shafts, so much room; its shaft's walls so thick. */
    static final double APART = 20;
    static final double ROOM = 5;
    static final double SHAFT_WALL = 10;

    Paternoster {
        if (!(wide > 2 * WALL && deep > WALL && high > 0 && downX - upX >= wide + APART && top > bottom && cabins > 0
                && speed > 0)) {
            throw new IllegalArgumentException("a paternoster's shafts side by side, its loop some height, cabins going");
        }
        // round its loop, its cabins no nearer one another than they are high
        if (2 * (top - bottom) + Math.PI * (downX - upX) < cabins * (high + 2 * FLOOR)) {
            throw new IllegalArgumentException("a paternoster's " + cabins + " cabins too many for its loop");
        }
    }

    /**
     * One about ({@code x}, {@code z}), its cabins so big, its loop from {@code bottom} to {@code top}, as many cabins
     * as go round it {@code spacing} apart, at {@code speed}.
     */
    static Paternoster about(final double x, final double z, final double bottom, final double top,
                             final double spacing, final double speed, final double wide, final double deep,
                             final double high) {
        final double half = (wide + APART) / 2;
        final double loop = 2 * (top - bottom) + 2 * Math.PI * half;
        final int cabins = Math.max(1, (int) Math.floor(loop / Math.max(spacing, high + 2 * FLOOR)));
        return new Paternoster(x - half, x + half, z, bottom, top, cabins, speed, wide, deep, high);
    }

    /** How far apart its shafts' middles are, half: the radius of its turns over the top and under the foot. */
    double radius() {
        return (downX - upX) / 2;
    }

    /** How long its loop is, all round. */
    double loop() {
        return 2 * (top - bottom) + 2 * Math.PI * radius();
    }

    /** Where on its loop a cabin's floor is, {@code s} along it from the foot of its up shaft: across, and up. */
    double[] along(final double s) {
        final double h = top - bottom;
        final double r = radius();
        final double middle = (upX + downX) / 2;
        final double t = ((s % loop()) + loop()) % loop();
        if (t < h) {
            return new double[] {upX, bottom + t};
        }
        if (t < h + Math.PI * r) {
            final double angle = Math.PI - (t - h) / r;
            return new double[] {middle + r * Math.cos(angle), top + r * Math.sin(angle)};
        }
        if (t < 2 * h + Math.PI * r) {
            return new double[] {downX, top - (t - h - Math.PI * r)};
        }
        final double angle = -(t - 2 * h - Math.PI * r) / r;
        return new double[] {middle + r * Math.cos(angle), bottom + r * Math.sin(angle)};
    }

    /** Cabin {@code k}'s way: its foot (its floor's underside) where its floor is at each moment. */
    Course.Route cabin(final int k) {
        final double start = k * loop() / cabins;
        return seconds -> {
            final double[] at = along(start + speed * seconds);
            return new Place(at[0], z, at[1] - FLOOR, 0);
        };
    }

    /** A cabin: its floor, its back, its sides, its roof; open in front. */
    List<Box> cabin() {
        final double a = wide / 2;
        final double l = deep / 2;
        return List.of(
                new Box("cabin, floor", Role.BASE, Material.TIMBER, -a, a, -l, l, 0, FLOOR),
                new Box("cabin, back", Role.WALL, Material.TIMBER, -a, a, -l, -l + WALL, FLOOR, FLOOR + high),
                new Box("cabin, left side", Role.WALL, Material.TIMBER, -a, -a + WALL, -l + WALL, l, FLOOR,
                        FLOOR + high),
                new Box("cabin, right side", Role.WALL, Material.TIMBER, a - WALL, a, -l + WALL, l, FLOOR,
                        FLOOR + high),
                new Box("cabin, roof", Role.ROOF, Material.TIMBER, -a, a, -l, l, FLOOR + high, FLOOR + high + FLOOR));
    }

    /** Its cabins, each a mover drawn by {@code key}. */
    List<Mover> movers(final int key) {
        final List<Mover> movers = new ArrayList<>();
        for (int k = 0; k < cabins; k++) {
            movers.add(new Mover("paternoster cabin " + (k + 1), cabin(), cabin(k), key));
        }
        return movers;
    }

    /** The hole each floor is cut for it: both shafts, a little wider than its cabins (min x, max x, min z, max z). */
    double[] hole() {
        return new double[] {upX - wide / 2 - ROOM, downX + wide / 2 + ROOM, z - deep / 2 - ROOM, z + deep / 2 + ROOM};
    }

    /** How high its shafts go: past its turn over the top, a cabin and some over it. */
    double over() {
        return top + radius() + FLOOR + high + FLOOR + 40;
    }

    /** Its shafts: walls behind and either side from {@code foot} up past its turn, open in front; capped over. */
    List<Solid> shaft(final double foot) {
        final double[] h = hole();
        final double w = SHAFT_WALL;
        final double over = over();
        return List.of(
                new Box("paternoster, back", Role.WALL, Material.CONCRETE, h[0] - w, h[1] + w, h[2] - w, h[2], foot,
                        over).solid(),
                new Box("paternoster, left", Role.WALL, Material.CONCRETE, h[0] - w, h[0], h[2], h[3], foot, over)
                        .solid(),
                new Box("paternoster, right", Role.WALL, Material.CONCRETE, h[1], h[1] + w, h[2], h[3], foot, over)
                        .solid(),
                new Box("paternoster, cap", Role.ROOF, Material.CONCRETE, h[0] - w, h[1] + w, h[2] - w, h[3], over,
                        over + 25).solid());
    }

    /** How long a cabin takes to go round. */
    double round() {
        return loop() / speed;
    }
}
