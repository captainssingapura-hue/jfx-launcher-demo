package com.example.fxsuite.avatar.buildings;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * An outline drawn in all round: each edge moved in square to itself, whichever way the outline goes round, at a
 * corner turning out (a square's) and one turning in (a notch's), and along an edge running straight on.
 */
class PlatesTest {

    @Test
    void aSquareDrawnInAllRoundEitherWayRound() {
        final List<double[]> round = List.of(new double[] {0, 0}, new double[] {10, 0}, new double[] {10, 10},
                new double[] {0, 10});
        same(List.of(new double[] {1, 1}, new double[] {9, 1}, new double[] {9, 9}, new double[] {1, 9}),
                Plates.inset(round, 1));
        same(List.of(new double[] {1, 9}, new double[] {9, 9}, new double[] {9, 1}, new double[] {1, 1}),
                Plates.inset(round.reversed(), 1));
    }

    @Test
    void aNotchedCornerAndAnEdgeRunningStraightOn() {
        // a 10 square, its north-east corner notched 4 in; a point midway along its south edge
        final List<double[]> notched = List.of(new double[] {0, 0}, new double[] {5, 0}, new double[] {10, 0},
                new double[] {10, 6}, new double[] {6, 6}, new double[] {6, 10}, new double[] {0, 10});
        same(List.of(new double[] {1, 1}, new double[] {5, 1}, new double[] {9, 1}, new double[] {9, 5},
                new double[] {5, 5}, new double[] {5, 9}, new double[] {1, 9}), Plates.inset(notched, 1));
        assertEquals(2 * (100 - 16), Plates.area(notched), 1e-9);
    }

    private static void same(final List<double[]> expected, final List<double[]> actual) {
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            assertArrayEquals(expected.get(i), actual.get(i), 1e-9, "corner " + i);
        }
    }
}
