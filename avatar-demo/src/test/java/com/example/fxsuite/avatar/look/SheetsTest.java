package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.sim.Course;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The sheet renderer's cutting of outlines into triangles, and the bodies drawn with it. */
class SheetsTest {

    /** Twice the area of triangle (a, b, c) in the plane x = 0 (a side sheet), by its y and z. */
    private static double area(final double[] a, final double[] b, final double[] c) {
        return Math.abs((b[1] - a[1]) * (c[2] - a[2]) - (b[2] - a[2]) * (c[1] - a[1])) / 2;
    }

    @Test
    void anOutlineWithAHollowIsCutIntoTrianglesCoveringJustIt() {
        // an L, seen from the side: six corners, a hollow at one of them
        final double[][] l = {{0, 0, 0}, {0, 0, 100}, {0, 40, 100}, {0, 40, 40}, {0, 100, 40}, {0, 100, 0}};
        final List<int[]> triangles = Sheets.ears(l, new double[] {1, 0, 0});
        assertEquals(l.length - 2, triangles.size());
        double total = 0;
        for (final int[] t : triangles) {
            total += area(l[t[0]], l[t[1]], l[t[2]]);
        }
        assertEquals(100 * 40 + 40 * 60, total, 1e-9, "just the L, nothing of its hollow");
    }

    @Test
    void eachBodyIsDrawnAndAPlainChassisHasNone() {
        for (final Course.Bodywork body : List.of(Course.PICKUP_BODY, Course.ROADSTER, Course.TRUCK_CAB, Course.VAN)) {
            final List<Sheets.Sheet> sheets = Bodies.of(body);
            assertFalse(sheets.isEmpty(), body.name());
            assertTrue(sheets.stream().anyMatch(s -> s.slot() == Sheets.Slot.GLASS), body.name() + " has glass");
        }
        assertTrue(Bodies.of(null).isEmpty());
    }
}
