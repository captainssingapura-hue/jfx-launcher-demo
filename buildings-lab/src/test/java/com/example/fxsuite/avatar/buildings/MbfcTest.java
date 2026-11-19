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

    private static boolean apart(final double[] a, final double[] b) {
        return a[1] <= b[0] + 1 || b[1] <= a[0] + 1 || a[3] <= b[2] + 1 || b[3] <= a[2] + 1;
    }
}
