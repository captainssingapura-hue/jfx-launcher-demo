package com.example.fxsuite.avatar.lifts;

import com.example.fxsuite.avatar.build.Material;
import com.example.fxsuite.avatar.build.Point;
import com.example.fxsuite.avatar.build.Role;
import com.example.fxsuite.avatar.build.Solid;
import com.example.fxsuite.avatar.sim.Course;

import java.util.ArrayList;
import java.util.List;

/**
 * A box of a lift's part, square to its frame: across ({@code a0} to {@code a1}), along ({@code l0} to {@code l1}) and up
 * ({@code u0} to {@code u1}), cm; what it does and what it is made of. The same box is what is in the way of whoever
 * moves about it ({@link #section}) and what is drawn ({@link #solid}), so the two never differ.
 */
record Box(String name, Role role, Material material, double a0, double a1, double l0, double l1, double u0,
           double u1) {

    Box {
        if (!(a1 > a0 && l1 > l0 && u1 > u0)) {
            throw new IllegalArgumentException(name + ": a box from its least to its most each way");
        }
    }

    /** What is in the way: a box in its part's frame. */
    Course.Section section() {
        return new Course.Section((a0 + a1) / 2, (l0 + l1) / 2, (u0 + u1) / 2, (a1 - a0) / 2, (l1 - l0) / 2,
                (u1 - u0) / 2);
    }

    /**
     * What is drawn: its eight corners, corner {@code c} at the least or the most across as bit 0 of {@code c} says,
     * along as bit 1, up as bit 2, as a solid's corners are drawn.
     */
    Solid solid() {
        final List<Point> corners = new ArrayList<>(8);
        for (int c = 0; c < 8; c++) {
            corners.add(new Point((c & 1) == 0 ? a0 : a1, (c & 2) == 0 ? l0 : l1, (c & 4) == 0 ? u0 : u1));
        }
        return new Solid(name, role, material, corners);
    }

    /** The same box moved so far across, along and up. */
    Box moved(final double across, final double along, final double up) {
        return new Box(name, role, material, a0 + across, a1 + across, l0 + along, l1 + along, u0 + up, u1 + up);
    }
}
