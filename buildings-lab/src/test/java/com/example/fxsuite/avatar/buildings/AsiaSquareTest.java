package com.example.fxsuite.avatar.buildings;

import com.example.fxsuite.avatar.lifts.HighRiseBank;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Asia Square's two towers, as their published plans and heights have them: as many storeys and as tall as published;
 * as many lifts in as many zones as their plans draw, each office floor served by one, each bank clear of the others
 * and of the core where both are; their floors, less their cores, near the floor areas their plans write.
 */
class AsiaSquareTest {

    private static final OfficeTower ONE = AsiaSquare.tower1();
    private static final OfficeTower TWO = AsiaSquare.tower2();

    /** A square foot, in m². */
    private static final double FOOT = 0.0929;

    @Test
    void tower1ItsStoreysAndItsThirtyTwoLiftsInFourZones() {
        assertEquals(Mbfc.storey(43) + 1, ONE.storeys(), "the lobby, floors 3 to 43");
        assertEquals(22_900, ONE.roof() + ONE.crown(), 1, "229 m, as published");
        lifts(ONE, 32, 4);
    }

    @Test
    void tower2ItsStoreysItsHotelAndItsTwentyFourLiftsInThreeZones() {
        assertEquals(Mbfc.storey(46) + 1, TWO.storeys(), "the lobby, floors 3 to 46");
        assertEquals(22_150, TWO.roof() + TWO.crown(), 1, "221.5 m, as published");
        assertEquals("hotel", TWO.uses().get(Mbfc.storey(32)), "its hotel from floor 32, as published");
        assertEquals(OfficeTower.OFFICE, TWO.uses().get(Mbfc.storey(31)));
        lifts(TWO, 24, 3);
        // its hotel's floors served by none of its office lifts
        final int hotel = Mbfc.storey(40);
        assertTrue(TWO.banks().stream().noneMatch(b -> b.bank().zoneFrom() <= hotel && hotel <= b.bank().zoneTo()));
    }

    @Test
    void theirFloorsLessTheirCoresNearWhatTheirPlansWrite() {
        // Tower 1's plans: some 32,350 sq ft low, 33,300 mid, 34,100 high, 35,000 ultra high; Tower 2's: 29,700 low,
        // 30,500 mid, 31,300 high; their plans scaled, so within a tenth
        near(32_350, ONE, Mbfc.storey(8), "Tower 1, low");
        near(33_300, ONE, Mbfc.storey(18), "Tower 1, mid");
        near(34_100, ONE, Mbfc.storey(28), "Tower 1, high");
        near(35_000, ONE, Mbfc.storey(38), "Tower 1, ultra high");
        near(29_700, TWO, Mbfc.storey(8), "Tower 2, low");
        near(30_500, TWO, Mbfc.storey(18), "Tower 2, mid");
        near(31_300, TWO, Mbfc.storey(28), "Tower 2, high");
    }

    private static void lifts(final OfficeTower tower, final int cars, final int zones) {
        assertEquals(cars, tower.banks().stream().mapToInt(p -> p.bank().cars()).sum(), tower.name());
        assertEquals(zones, tower.banks().stream().map(p -> p.bank().zoneFrom()).distinct().count(), tower.name());
        for (int f = 1; f < tower.storeys(); f++) {
            if (!OfficeTower.OFFICE.equals(tower.uses().get(f))) {
                continue;
            }
            final int floor = f;
            assertEquals(1, tower.banks().stream().map(Placed::bank)
                    .filter(b -> b.zoneFrom() <= floor && floor <= b.zoneTo()).map(HighRiseBank::zoneFrom).distinct()
                    .count(), tower.name() + ", storey " + f);
        }
        for (int f = 0; f < tower.storeys(); f++) {
            final int storey = f;
            final List<Placed> through = tower.banks().stream().filter(b -> storey <= b.bank().zoneTo()).toList();
            for (int i = 0; i < through.size(); i++) {
                final double[] a = through.get(i).hole();
                for (int j = i + 1; j < through.size(); j++) {
                    assertTrue(apart(a, through.get(j).hole()), tower.name() + ", storey " + f + ": two banks");
                }
                for (final double[] s : tower.service()) {
                    if (s.length < 6 || s[4] <= f && f <= s[5]) {
                        assertTrue(apart(a, s), tower.name() + ", storey " + f + ": a bank and the core");
                    }
                }
            }
        }
    }

    private static void near(final double feet, final OfficeTower tower, final int f, final String what) {
        double core = 0;
        for (final double[] s : tower.service()) {
            if (s.length < 6 || s[4] <= f && f <= s[5]) {
                core += (s[1] - s[0]) * (s[3] - s[2]) / 10_000;
            }
        }
        final double net = tower.area(f) - core;
        final double plan = feet * FOOT;
        assertTrue(Math.abs(net - plan) / plan < 0.1, what + ": " + net + " m² against " + plan);
    }

    private static boolean apart(final double[] a, final double[] b) {
        return a[1] <= b[0] + 1 || b[1] <= a[0] + 1 || a[3] <= b[2] + 1 || b[3] <= a[2] + 1;
    }
}
