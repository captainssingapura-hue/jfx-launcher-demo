package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.sim.Channel;
import com.example.fxsuite.avatar.sim.Land;
import com.example.fxsuite.avatar.sim.Vec2;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A piece of the land as triangles: as close to it as asked, everywhere; fine only where it needs to be,
 * by a channel, and coarse over open ground; whole, every side between two triangles shared by both and
 * none left open but round its edges, where its skirt hangs; and wound so its face is up.
 */
class LandSheetTest {

    /** A channel across the middle of a 100 m square, east to west, a little slanted. */
    private static final Land LAND = new Land(null, List.of(
            new Channel(List.of(new Vec2(-10_000, -1_000), new Vec2(20_000, 2_000)), 900, 400, 150)));

    private static LandSheet.Made made(final Land land) {
        return LandSheet.of(land, 0, -5_000, 10_000, 5_000, 3, 50, 4_000, 400, 1_000, 500);
    }

    @Test
    void asCloseToTheLandAsAskedEverywhere() {
        final LandSheet.Made made = made(LAND);
        final float[] p = made.points();
        final int[] t = made.triangles();
        double worst = 0;
        for (int k = 0; k < t.length; k += 3) {
            // its top alone, not its skirt
            if (made.heights()[t[k]] != -p[3 * t[k] + 1] || made.heights()[t[k + 1]] != -p[3 * t[k + 1] + 1]
                    || made.heights()[t[k + 2]] != -p[3 * t[k + 2] + 1]) {
                continue;
            }
            // across each triangle, where the land is and where the triangle is
            for (final double[] w : new double[][] {{1 / 3.0, 1 / 3.0}, {0.5, 0.25}, {0.25, 0.5}, {0.25, 0.25}}) {
                final double u = w[0];
                final double v = w[1];
                final double x = p[3 * t[k]] * (1 - u - v) + p[3 * t[k + 1]] * u + p[3 * t[k + 2]] * v;
                final double z = p[3 * t[k] + 2] * (1 - u - v) + p[3 * t[k + 1] + 2] * u + p[3 * t[k + 2] + 2] * v;
                final double y = -(p[3 * t[k] + 1] * (1 - u - v) + p[3 * t[k + 1] + 1] * u + p[3 * t[k + 2] + 1] * v);
                worst = Math.max(worst, Math.abs(LAND.height(x, z) - y));
            }
        }
        assertTrue(worst < 6, "within twice what it was asked, between where it looked: " + worst);
    }

    @Test
    void fineByTheChannelCoarseOverOpenGround() {
        final LandSheet.Made channelled = made(LAND);
        final LandSheet.Made open = made(Land.LEVEL);
        // open ground: squares no bigger than 40 m, so sixteen of 25 m, two triangles each, and its skirt down
        // each of its four edges, four squares' sides each
        assertEquals(16 * 2 + 4 * 4 * 2, open.triangles().length / 3);
        assertTrue(channelled.triangles().length / 3 > 1_000, "fine by the channel: " + channelled.triangles().length / 3);
        // and fine only near it
        final float[] p = channelled.points();
        int far = 0;
        for (int v = 0; v < channelled.size(); v++) {
            if (Math.abs(p[3 * v + 2]) > 3_000) {
                far++;
            }
        }
        assertTrue(far < channelled.size() / 10, "few points far from it: " + far + " of " + channelled.size());
    }

    @Test
    void wholeItsSidesSharedNoneOpenButRoundItsEdgesItsFaceUp() {
        final LandSheet.Made made = made(LAND);
        final float[] p = made.points();
        final int[] t = made.triangles();
        final Map<Long, Integer> sides = new HashMap<>();
        for (int k = 0; k < t.length; k += 3) {
            for (int e = 0; e < 3; e++) {
                final int a = t[k + e];
                final int b = t[k + (e + 1) % 3];
                // its top's sides: the skirt's own open below
                if (made.heights()[a] == -p[3 * a + 1] && made.heights()[b] == -p[3 * b + 1]) {
                    sides.merge(Math.min(a, b) * 1_000_000L + Math.max(a, b), 1, Integer::sum);
                }
            }
            // its face up, the world's y down: the turn from its first corner to the others is clockwise seen from over it
            final double ax = p[3 * t[k + 1]] - p[3 * t[k]];
            final double az = p[3 * t[k + 1] + 2] - p[3 * t[k] + 2];
            final double bx = p[3 * t[k + 2]] - p[3 * t[k]];
            final double bz = p[3 * t[k + 2] + 2] - p[3 * t[k] + 2];
            final double up = az * bx - ax * bz;
            final boolean skirt = ax == 0 && bx == 0 || az == 0 && bz == 0;
            assertTrue(skirt || up < 0, "face up");
        }
        for (final int count : sides.values()) {
            assertEquals(2, count, "each side of its top shared by two triangles, its skirt's at its edges");
        }
    }
}
