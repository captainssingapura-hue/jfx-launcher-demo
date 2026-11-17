package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A contour loop: what it goes round, how far a point is from it, whether it crosses, its farthest point in. */
class ContourTest {

    /** A loop round ({@code x}, {@code z}), {@code r} out, of {@code n} points, the first straight across. */
    static List<Vec2> circle(final double x, final double z, final double r, final int n) {
        final List<Vec2> loop = new ArrayList<>();
        for (int k = 0; k < n; k++) {
            final double a = 2 * Math.PI * k / n;
            loop.add(new Vec2(x + r * Math.cos(a), z + r * Math.sin(a)));
        }
        return loop;
    }

    static List<Vec2> square(final double x, final double z, final double half) {
        return List.of(new Vec2(x - half, z - half), new Vec2(x + half, z - half), new Vec2(x + half, z + half),
                new Vec2(x - half, z + half));
    }

    @Test
    void whatItGoesRoundAndHowFarAPointIsFromIt() {
        final Contour square = new Contour(500, square(0, 0, 100));
        assertEquals(40_000, square.area(), 1e-9);
        assertTrue(square.inside(0, 0));
        assertFalse(square.inside(150, 0));
        assertEquals(30, square.distance(70, 0), 1e-9, "from inside");
        assertEquals(50, square.distance(150, 0), 1e-9, "from outside");
        assertFalse(square.flat());
        assertThrows(IllegalArgumentException.class,
                () -> new Contour(0, List.of(new Vec2(0, 0), new Vec2(1, 0), new Vec2(2, 0))), "round nothing");
    }

    @Test
    void whetherItCrossesItselfOrAnother() {
        final Contour square = new Contour(0, square(0, 0, 100));
        assertFalse(square.crossesItself());
        final Contour bowTie = new Contour(0, List.of(new Vec2(0, 0), new Vec2(100, 100), new Vec2(100, 0), new Vec2(0, 150)));
        assertTrue(bowTie.crossesItself());
        assertTrue(square.crosses(new Contour(0, square(150, 0, 100))));
        assertFalse(square.crosses(new Contour(0, square(0, 0, 50))), "one inside the other");
        assertFalse(square.crosses(new Contour(0, square(500, 0, 100))), "apart");
    }

    @Test
    void itsFarthestPointInIsItsMiddleForARound() {
        final Vec2 pole = new Contour(0, circle(300, -200, 1_000, 48)).pole();
        assertEquals(300, pole.x(), 5);
        assertEquals(-200, pole.z(), 5);
        // an L, its arms 200 wide: not where its points average out, but in its corner, as far from its
        // outer sides as from its inner corner
        final Contour ell = new Contour(0, List.of(new Vec2(0, 0), new Vec2(1_000, 0), new Vec2(1_000, 200),
                new Vec2(200, 200), new Vec2(200, 1_000), new Vec2(0, 1_000)));
        final Vec2 in = ell.pole();
        assertTrue(ell.inside(in.x(), in.z()));
        assertEquals(200 * Math.sqrt(2) / (1 + Math.sqrt(2)), ell.distance(in.x(), in.z()), 1);
    }
}
