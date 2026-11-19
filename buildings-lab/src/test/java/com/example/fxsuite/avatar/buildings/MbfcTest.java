package com.example.fxsuite.avatar.buildings;

import com.example.fxsuite.avatar.lifts.HighRiseBank;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tower 1, as its published figures have it: its floors where they are, its 33rd at 149 m; its plant floor where its
 * sheet says; its sixteen lifts in two zones, eight a zone, each zone's floors served by one; its floor, less its core,
 * near what its sheet says it lets.
 */
class MbfcTest {

    private static final OfficeTower T1 = Mbfc.tower1();

    @Test
    void itsFloorsWhereItsFiguresHaveThem() {
        assertEquals(Mbfc.storey(33) + 1, T1.storeys(), "the lobby, floors 3 to 33");
        assertEquals(14_900, T1.levels().get(Mbfc.storey(33)), 1e-6, "its 33rd floor 149 m up");
        assertEquals(OfficeTower.PLANT, T1.uses().get(Mbfc.storey(30)));
        for (final int n : new int[] {3, 17, 18, 29, 31, 32}) {
            assertEquals(OfficeTower.OFFICE, T1.uses().get(Mbfc.storey(n)), "floor " + n);
        }
        assertEquals(0, T1.front(), "its way in on its south side");
    }

    @Test
    void sixteenLiftsInTwoZonesEachFloorServedByOne() {
        assertEquals(16, T1.banks().stream().mapToInt(p -> p.bank().cars()).sum());
        assertEquals(2, T1.banks().stream().map(p -> p.bank().zoneFrom()).distinct().count());
        for (int f = 1; f < T1.storeys(); f++) {
            final int floor = f;
            assertEquals(1, T1.banks().stream().map(Placed::bank)
                    .filter(b -> b.zoneFrom() <= floor && floor <= b.zoneTo()).map(HighRiseBank::zoneFrom).distinct()
                    .count(), "floor " + (f + 2));
        }
        // no bank in another's way, nor in the core's
        for (int i = 0; i < T1.banks().size(); i++) {
            final double[] a = T1.banks().get(i).hole();
            for (int j = i + 1; j < T1.banks().size(); j++) {
                assertTrue(apart(a, T1.banks().get(j).hole()), "banks " + i + " and " + j);
            }
            for (final double[] s : T1.service()) {
                assertTrue(apart(a, s), "bank " + i + " and the core");
            }
        }
    }

    @Test
    void aLowFloorLessItsCoreNearWhatItLets() {
        double core = 0;
        for (final double[] s : T1.service()) {
            core += (s[1] - s[0]) * (s[3] - s[2]) / 10_000;
        }
        final double net = T1.area(Mbfc.storey(10)) - core;
        // its sheet: some 1,950 m² let; its plan, 2,000 m²
        assertTrue(Math.abs(net - 1_975) / 1_975 < 0.1, net + " m²");
    }

    private static final OfficeTower T2 = Mbfc.tower2();

    @Test
    void tower2ItsFloorsAndItsTwentyThreeLiftsInThreeZones() {
        assertEquals(Mbfc.storey(50) + 1, T2.storeys(), "the lobby, floors 3 to 50");
        assertEquals(24_500, T2.roof() + T2.crown(), 100, "245 m to its crown's top");
        assertEquals(OfficeTower.PLANT, T2.uses().get(Mbfc.storey(38)));
        assertEquals(OfficeTower.PARKING, T2.uses().get(Mbfc.storey(6)));
        assertEquals(OfficeTower.OFFICE, T2.uses().get(Mbfc.storey(7)));
        assertEquals(23, T2.banks().stream().mapToInt(p -> p.bank().cars()).sum());
        assertEquals(3, T2.banks().stream().map(p -> p.bank().zoneFrom()).distinct().count());
        for (int n = 7; n <= 50; n++) {
            final int floor = Mbfc.storey(n);
            assertEquals(1, T2.banks().stream().map(Placed::bank)
                    .filter(b -> b.zoneFrom() <= floor && floor <= b.zoneTo()).map(HighRiseBank::zoneFrom).distinct()
                    .count(), "floor " + n);
        }
        for (int i = 0; i < T2.banks().size(); i++) {
            final double[] a = T2.banks().get(i).hole();
            for (int j = i + 1; j < T2.banks().size(); j++) {
                assertTrue(apart(a, T2.banks().get(j).hole()), "banks " + i + " and " + j);
            }
            for (final double[] s : T2.service()) {
                assertTrue(apart(a, s), "bank " + i + " and the core");
            }
        }
    }

    @Test
    void tower2StepsInAsItRisesItsFloorsNearItsPlans() {
        // each a step in from the one under it; each zone's floor, less its core, near what its plan says
        final double low = T2.area(Mbfc.storey(11));
        final double lowerHigh = T2.area(Mbfc.storey(40));
        final double upperHigh = T2.area(Mbfc.storey(46));
        assertTrue(low > lowerHigh && lowerHigh > upperHigh, low + ", " + lowerHigh + ", " + upperHigh);
        assertNear(2_340, low - core(T2, Mbfc.storey(11)), "11th floor");
        assertNear(2_395, T2.area(Mbfc.storey(28)) - core(T2, Mbfc.storey(28)), "28th floor");
        assertNear(2_000, lowerHigh - core(T2, Mbfc.storey(40)), "lower high-rise");
        assertNear(1_800, upperHigh - core(T2, Mbfc.storey(46)), "upper high-rise");
        // every storey's outline within the one under it
        for (int f = 1; f < T2.storeys(); f++) {
            for (final double[] p : T2.outline(f)) {
                assertTrue(inside(T2.outline(f - 1), p[0], p[1]), "storey " + f + " at " + p[0] + ", " + p[1]);
            }
        }
    }

    /** The core's solid parts on storey {@code f}, m². */
    private static double core(final OfficeTower t, final int f) {
        double core = 0;
        for (final double[] s : t.service()) {
            if (s.length < 6 || s[4] <= f && f <= s[5]) {
                core += (s[1] - s[0]) * (s[3] - s[2]) / 10_000;
            }
        }
        return core;
    }

    private static void assertNear(final double plan, final double net, final String what) {
        assertTrue(Math.abs(net - plan) / plan < 0.1, what + ": " + net + " m² against " + plan);
    }

    /** Whether a point is in {@code outline} or on its edge, within a few cm. */
    private static boolean inside(final java.util.List<double[]> outline, final double x, final double z) {
        boolean in = false;
        for (int i = 0, j = outline.size() - 1; i < outline.size(); j = i++) {
            final double[] a = outline.get(i);
            final double[] b = outline.get(j);
            // on the edge
            final double len = Math.hypot(b[0] - a[0], b[1] - a[1]);
            final double cross = ((b[0] - a[0]) * (z - a[1]) - (b[1] - a[1]) * (x - a[0])) / len;
            final double dot = ((x - a[0]) * (b[0] - a[0]) + (z - a[1]) * (b[1] - a[1])) / len;
            if (Math.abs(cross) < 5 && dot >= -5 && dot <= len + 5) {
                return true;
            }
            if ((a[1] > z) != (b[1] > z) && x < (b[0] - a[0]) * (z - a[1]) / (b[1] - a[1]) + a[0]) {
                in = !in;
            }
        }
        return in;
    }

    private static boolean apart(final double[] a, final double[] b) {
        return a[1] <= b[0] + 1 || b[1] <= a[0] + 1 || a[3] <= b[2] + 1 || b[3] <= a[2] + 1;
    }
}
