package com.example.fxsuite.avatar.buildings;

import com.example.fxsuite.avatar.lifts.HighRiseBank;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * One Raffles Quay's two towers, as their published figures have them: their floors where their sheets put their
 * offices, their heights the map's; their lifts as many and in as many zones as their sheets say, each office floor
 * served by one, each bank clear of the others and of the core where both are; their floors, less their cores, near
 * what their sheets say they let.
 */
class OrqTest {

    private static final OfficeTower NORTH = Orq.north();
    private static final OfficeTower SOUTH = Orq.south();

    @Test
    void theNorthTowersFloorsAndItsTwentyLiftsInThreeZones() {
        assertEquals(Mbfc.storey(50) + 1, NORTH.storeys(), "the lobby, floors 3 to 50");
        assertEquals(24_500, NORTH.roof() + NORTH.crown(), 1, "245 m, as the map has it");
        assertEquals(OfficeTower.PLANT, NORTH.uses().get(Mbfc.storey(26)));
        assertEquals(OfficeTower.PARKING, NORTH.uses().get(Mbfc.storey(7)));
        lifts(NORTH, 20, 3, 8, 50);
    }

    @Test
    void theSouthTowersFloorsAndItsSixteenLiftsInTwoZones() {
        assertEquals(Mbfc.storey(29) + 1, SOUTH.storeys(), "the lobby, floors 3 to 29");
        assertEquals(13_990, SOUTH.roof() + SOUTH.crown(), 1, "139.9 m, as the map has it");
        assertEquals(OfficeTower.PARKING, SOUTH.uses().get(Mbfc.storey(11)));
        assertEquals(OfficeTower.OFFICE, SOUTH.uses().get(Mbfc.storey(12)));
        lifts(SOUTH, 16, 2, 12, 29);
    }

    @Test
    void theirFloorsLessTheirCoresNearWhatTheirSheetsSayTheyLet() {
        // the North Tower's sheet: some 1,700 m² let a floor; the South Tower's, some 2,900 m²
        near(1_700, NORTH, Mbfc.storey(15), "the North Tower's low zone");
        near(1_700, NORTH, Mbfc.storey(30), "its mid zone");
        near(1_700, NORTH, Mbfc.storey(45), "its high zone");
        near(2_900, SOUTH, Mbfc.storey(15), "the South Tower's low zone");
        near(2_900, SOUTH, Mbfc.storey(25), "its high zone");
    }

    /** {@code tower}'s lifts: so many, in so many zones; each office floor from {@code from} to {@code to} served by one. */
    private static void lifts(final OfficeTower tower, final int cars, final int zones, final int from, final int to) {
        assertEquals(cars, tower.banks().stream().mapToInt(p -> p.bank().cars()).sum(), tower.name());
        assertEquals(zones, tower.banks().stream().map(p -> p.bank().zoneFrom()).distinct().count(), tower.name());
        for (int n = from; n <= to; n++) {
            if (!OfficeTower.OFFICE.equals(tower.uses().get(Mbfc.storey(n)))) {
                continue;
            }
            final int floor = Mbfc.storey(n);
            assertEquals(1, tower.banks().stream().map(Placed::bank)
                    .filter(b -> b.zoneFrom() <= floor && floor <= b.zoneTo()).map(HighRiseBank::zoneFrom).distinct()
                    .count(), tower.name() + ", floor " + n);
        }
        // on each storey, each bank still going up through it clear of the others and of the core there
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

    private static void near(final double sheet, final OfficeTower tower, final int f, final String what) {
        double core = 0;
        for (final double[] s : tower.service()) {
            if (s.length < 6 || s[4] <= f && f <= s[5]) {
                core += (s[1] - s[0]) * (s[3] - s[2]) / 10_000;
            }
        }
        final double net = tower.area(f) - core;
        assertTrue(Math.abs(net - sheet) / sheet < 0.1, what + ": " + net + " m² against " + sheet);
    }

    private static boolean apart(final double[] a, final double[] b) {
        return a[1] <= b[0] + 1 || b[1] <= a[0] + 1 || a[3] <= b[2] + 1 || b[3] <= a[2] + 1;
    }
}
