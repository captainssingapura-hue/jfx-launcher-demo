package com.example.fxsuite.avatar.buildings;

import com.example.fxsuite.avatar.lifts.HighRiseBank;
import com.example.fxsuite.avatar.sim.Place;

import java.util.ArrayList;
import java.util.List;

/**
 * The bay's other pair of office towers, beside the financial centre's three: the North Tower and the South Tower, each
 * an office tower ({@link OfficeTower}) described from the figures their owner publishes, as the financial centre's are
 * ({@link Mbfc}): the lengths written on their typical floor plans, the floors and lifts their sheets list, nothing
 * traced. Their heights and storeys the map's.
 *
 * <p>Each in its own frame, its plan's north along, cm, about its outline's middle; a storey the model's, the lobby
 * the first (0), double height, the building's floors 1 and 2; floor {@code n} over it the model's {@code n - 2}.
 */
final class Orq {

    private Orq() {
    }

    /** The first key the pair is drawn by in the bay, each taking {@link Standing#KEYS} from it in turn. */
    static final int KEYS = 990_000;

    /**
     * The pair where they stand in the bay ({@link com.example.fxsuite.avatar.build.MarinaBay}'s frame, cm): each where
     * the map has its middle, turned as its sides run there, its feet on its land. The North Tower's plan's north to its
     * north-west face, where the map draws the jogs its plan has at that face's corners; the South Tower's long sides
     * along the longer pair of the map's, its outline drawn small there.
     */
    static List<Standing> standing() {
        final double[][] where = {{-85_040, -49_850, -48.3}, {-87_950, -57_550, -74.3}};
        final List<OfficeTower> towers = List.of(north(), south());
        final com.example.fxsuite.avatar.sim.Land land = com.example.fxsuite.avatar.build.MarinaBay.land();
        final List<Standing> standing = new ArrayList<>();
        for (int k = 0; k < towers.size(); k++) {
            final double[] w = where[k];
            standing.add(new Standing(towers.get(k), new Place(w[0], w[1], land.height(w[0], w[1]), w[2]),
                    KEYS + k * Standing.KEYS));
        }
        return standing;
    }

    /**
     * The North Tower: 50 floors, 245 m to its crown's top; its offices on floors 8 to 25, 27 to 38 and 39 to 50, floor
     * 26 plant, floors 3 to 7 parking; 4.5 m floor to floor over a lobby 9.5 m high (an estimate: its sheet gives its
     * ceilings, 2.75 m clear, not its storeys). 55.02 by 38.27 m, its plan says, its two north corners drawn in on the
     * slant for the top 4 m of its depth, a jog at the foot of each.
     *
     * <p>Its core 40 m long and 13.3 m deep, 12.5 m from its glass either side, as its plans write it: west to east, the
     * low zone's lift lobby (floors 8 to 25, four cars either side), its fire lifts, a stair and risers; the high zone's
     * (39 to 50, two cars one side, four the other); the mid zone's (27 to 38, four one side, two the other, a stair
     * under them); toilets in a zone's lobby where it does not stop, and where the low zone's east bank was over it.
     * Twenty lifts in three zones. Its way in on its south side.
     */
    static OfficeTower north() {
        final double a = 2751;
        final double l = 1913.5;
        final double in = 2636;
        final double jog = 1517;
        final List<double[]> outline = List.of(
                new double[] {-a, -l}, new double[] {a, -l}, new double[] {a, jog}, new double[] {in, jog},
                new double[] {a, l}, new double[] {-a, l}, new double[] {-in, jog}, new double[] {-a, jog});
        final int top = 50;
        final List<Double> levels = levels(top, 450);
        final List<String> uses = new ArrayList<>(List.of(OfficeTower.LOBBY));
        for (int n = 3; n <= top; n++) {
            uses.add(n <= 7 ? OfficeTower.PARKING : n == 26 ? OfficeTower.PLANT : OfficeTower.OFFICE);
        }
        final int lowFrom = Mbfc.storey(8);
        final int lowTo = Mbfc.storey(25);
        final int midFrom = Mbfc.storey(27);
        final int midTo = Mbfc.storey(38);
        final int highFrom = Mbfc.storey(39);
        final int highTo = Mbfc.storey(top);
        // the banks of two cars stand in the north half of the core, a stair or a service under them
        final List<Placed> banks = List.of(
                new Placed(bank(levels, lowFrom, lowTo, 4, 400), -1940, 0, 90),
                new Placed(bank(levels, lowFrom, lowTo, 4, 400), -1360, 0, -90),
                new Placed(bank(levels, highFrom, highTo, 2, 700), -40, 300, 90),
                new Placed(bank(levels, highFrom, highTo, 4, 700), 520, 0, -90),
                new Placed(bank(levels, midFrom, midTo, 4, 550), 940, 0, 90),
                new Placed(bank(levels, midFrom, midTo, 2, 550), 1530, 300, -90));
        final List<double[]> service = List.of(
                // the low zone's west bank's back
                new double[] {-2190, -2070, -666, 666, 0, lowTo},
                // its fire lifts, a stair, its risers; under the high zone's west bank, a service
                new double[] {-1230, -170, -666, 666},
                new double[] {-170, 90, -666, 0},
                // the core's wall between the high zone's east bank and the mid zone's west
                new double[] {650, 810, -666, 666},
                // a stair under the mid zone's east bank; the core's east end
                new double[] {1400, 1660, -666, 0},
                new double[] {1660, 1820, -666, 666},
                // toilets in the high and the mid zones' lobbies on the floors under where they stop
                new double[] {90, 390, -666, 666, 1, highFrom - 1},
                new double[] {1070, 1400, -666, 666, 1, midFrom - 1},
                // toilets where the low zone's east bank was, over it
                new double[] {-1490, -1150, -666, 666, lowTo + 1, top});
        return new OfficeTower("ORQ North Tower", outline, levels, uses, service, banks, 24_500 - levels.getLast(), 0);
    }

    /**
     * The South Tower: 29 floors, 139.9 m to its top; its offices on floors 12 to 29, floors 3 to 11 parking (its five
     * car park lifts); 4.6 m floor to floor over a lobby 9.5 m high, its crown taking it to the map's (an estimate, as
     * the North Tower's). 64.39 by 52.44 m, its plans say, square at its corners.
     *
     * <p>Its core some 43 m long and 13 m deep as its plans write it: west to east, the low zone's lift lobby (floors 12
     * to 20, four cars either side, its west bank standing out of the core); a stair, the fire lifts and a shaft; the
     * high zone's (21 to 29, four either side); toilets, and a stair standing out at its east end. Toilets in the high
     * zone's lobby under where it stops, and where the low zone's east bank was over it. Sixteen lifts in two zones. Its
     * way in on its south side.
     */
    static OfficeTower south() {
        final double a = 3219.5;
        final double l = 2622;
        final List<double[]> outline = List.of(new double[] {-a, -l}, new double[] {a, -l}, new double[] {a, l},
                new double[] {-a, l});
        final int top = 29;
        final List<Double> levels = levels(top, 460);
        final List<String> uses = new ArrayList<>(List.of(OfficeTower.LOBBY));
        for (int n = 3; n <= top; n++) {
            uses.add(n <= 11 ? OfficeTower.PARKING : OfficeTower.OFFICE);
        }
        final int lowFrom = Mbfc.storey(12);
        final int lowTo = Mbfc.storey(20);
        final int highFrom = Mbfc.storey(21);
        final int highTo = Mbfc.storey(top);
        final List<Placed> banks = List.of(
                new Placed(bank(levels, lowFrom, lowTo, 4, 400), -1890, 0, 90),
                new Placed(bank(levels, lowFrom, lowTo, 4, 400), -1280, 0, -90),
                new Placed(bank(levels, highFrom, highTo, 4, 500), 490, 0, 90),
                new Placed(bank(levels, highFrom, highTo, 4, 500), 1080, 0, -90));
        final List<double[]> service = List.of(
                // the low zone's west bank's back, standing out of the core
                new double[] {-2090, -2020, -640, 640, 0, lowTo},
                // a stair, the fire lifts, a shaft
                new double[] {-1150, 360, -640, 640},
                // toilets, and a stair standing out at its east end
                new double[] {1210, 2200, -640, 640},
                // toilets in the high zone's lobby under where it stops; where the low zone's east bank was, over it
                new double[] {620, 950, -640, 640, 1, highFrom - 1},
                new double[] {-1410, -1150, -640, 640, lowTo + 1, top});
        return new OfficeTower("ORQ South Tower", outline, levels, uses, service, banks, 13_990 - levels.getLast(), 0);
    }

    /** Floors 3 to {@code top} over a lobby 9.5 m high standing for floors 1 and 2, each {@code storey} high; the roof. */
    private static List<Double> levels(final int top, final double storey) {
        final double lobby = 950;
        final List<Double> levels = new ArrayList<>(List.of(0.0));
        for (int n = 3; n <= top; n++) {
            levels.add(lobby + (Mbfc.storey(n) - 1) * storey);
        }
        levels.add(lobby + Mbfc.storey(top) * storey);
        return levels;
    }

    /** A bank of {@code cars} of the towers' cars, as the financial centre's are, serving the lobby and a zone. */
    private static HighRiseBank bank(final List<Double> levels, final int from, final int to, final int cars,
                                     final double speed) {
        return OfficeTower.bank(levels.subList(0, to + 1), from, to, cars, speed, 260, 210, 270, 110);
    }
}
