package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A box, and the plain inside: a box cut into a grid of cells, the last each way cut short; every point in it in the
 * one cell whose box holds it, none outside it; the cells a box reaches exactly those whose boxes it meets.
 */
class GridInsideTest {

    /** 10 m across, 22.5 m along, five storeys of 3.6 m: cells of 3 m, 7.5 m, a storey; the last across cut short. */
    private static final GridInside GRID = new GridInside(new Box(-500, 500, 0, 2250, 0, 1800), 300, 750, 360);

    @Test
    void aBoxItsSidesPartOfIt() {
        final Box box = new Box(0, 100, 0, 200, 0, 300);
        assertTrue(box.holds(100, 200, 300));
        assertFalse(box.holds(100.01, 200, 300));
        assertTrue(box.meets(new Box(100, 150, 50, 60, 0, 10)), "touching");
        assertFalse(box.meets(new Box(101, 150, 50, 60, 0, 10)));
        assertFalse(box.meets(new Box(0, 100, 0, 200, 301, 400)), "over it");
        assertEquals(new Box(-10, 110, -10, 210, -5, 305), box.grown(10, 5));
        assertEquals(new Box(70, 130, -20, 40, 50, 230), Box.around(100, 10, 50, 30, 180));
        assertThrows(IllegalArgumentException.class, () -> new Box(1, 0, 0, 1, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new Box(0, 1, 0, 1, 0, Double.NaN));
    }

    @Test
    void cutIntoCellsTheLastEachWayCutShort() {
        assertEquals(4 * 3 * 5, GRID.count());
        assertEquals(new Box(-500, -200, 0, 750, 0, 360), GRID.box(0));
        assertEquals(new Box(400, 500, 1500, 2250, 1440, 1800), GRID.box(GRID.count() - 1), "a metre across");
        assertThrows(IllegalArgumentException.class, () -> GRID.box(GRID.count()));
        // the cells' boxes tile it: their volumes add up to its own
        double volume = 0;
        for (int c = 0; c < GRID.count(); c++) {
            final Box b = GRID.box(c);
            volume += (b.maxX() - b.minX()) * (b.maxZ() - b.minZ()) * (b.high() - b.low());
        }
        assertEquals(1000.0 * 2250 * 1800, volume, 1e-3);
    }

    @Test
    void everyPointInItInTheCellWhoseBoxHoldsItNoneOutside() {
        for (int k = 0; k < 2000; k++) {
            final double a = -600 + 1200 * Num.hash(k, 1);
            final double l = -100 + 2450 * Num.hash(k, 2);
            final double u = -100 + 2000 * Num.hash(k, 3);
            final int cell = GRID.cell(a, l, u);
            if (GRID.bounds().holds(a, l, u)) {
                assertTrue(cell >= 0 && GRID.box(cell).holds(a, l, u), "in its cell: " + a + ", " + l + ", " + u);
            } else {
                assertEquals(-1, cell, "outside");
            }
        }
        // on its far sides, in the last cells
        assertEquals(GRID.count() - 1, GRID.cell(500, 2250, 1800));
    }

    @Test
    void theCellsABoxReachesThoseWhoseBoxesItMeets() {
        for (int k = 0; k < 500; k++) {
            final double a = -700 + 1400 * Num.hash(k, 4);
            final double l = -200 + 2650 * Num.hash(k, 5);
            final double u = -200 + 2200 * Num.hash(k, 6);
            final Box box = new Box(a, a + 900 * Num.hash(k, 7), l, l + 900 * Num.hash(k, 8), u,
                    u + 500 * Num.hash(k, 9));
            final List<Integer> met = new ArrayList<>();
            for (int c = 0; c < GRID.count(); c++) {
                if (GRID.box(c).meets(box)) {
                    met.add(c);
                }
            }
            assertArrayEquals(met.stream().mapToInt(Integer::intValue).toArray(), GRID.cells(box), "" + box);
        }
        assertEquals(0, GRID.cells(new Box(600, 700, 0, 10, 0, 10)).length, "outside it");
    }
}
