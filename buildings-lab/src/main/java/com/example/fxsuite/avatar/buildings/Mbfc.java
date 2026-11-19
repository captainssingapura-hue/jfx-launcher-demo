package com.example.fxsuite.avatar.buildings;

import com.example.fxsuite.avatar.lifts.HighRiseBank;

import java.util.ArrayList;
import java.util.List;

/**
 * The bay's three office towers on their podium, each an office tower ({@link OfficeTower}) described from the figures
 * its owner publishes: the lengths written on its typical floor plans, the floors its sheet lists, the lifts it counts.
 * Figures only: nothing of the plans is traced or kept, and the tower is our own built to them.
 *
 * <p>Each in its own frame, plan north along: across (x) east, along (z) north, cm, about the middle of its outline.
 * Where it stands in the bay and which way it is turned is the bay's to say.
 *
 * <p>A storey here is one of the model's: the lobby the first (0), double height, the building's own floors 1 and 2;
 * each over it its own, the building's floor {@code n} the model's {@code n - 2}.
 */
final class Mbfc {

    private Mbfc() {
    }

    /** The building's floor numbered {@code n} (3 and up) as the model's storey. */
    static int storey(final int n) {
        return n - 2;
    }

    /**
     * Tower 1: 33 floors, its offices on floors 3 to 29 and 31 to 32, floor 30 plant, floor 33 its bar; 4.65 m floor to
     * floor over a lobby 9.5 m high, so floor 33 is 149 m up, as is known of it. About 51 m square, notched at its
     * north-west corner, its south-east set in and cut on the slant. Its core: a fixed part with its stairs, service
     * lift and toilets, and two lift lobbies side by side, each with four cars either side of it; the high zone's to
     * the west (floors 18 to 33), the low zone's to the east (3 to 17), its east bank standing apart from the rest of
     * the core; a passage across the core south of them; its way in on its south side.
     */
    static OfficeTower tower1() {
        final List<double[]> outline = List.of(
                new double[] {-2550, -2550}, new double[] {1820, -2550}, new double[] {1820, -1740},
                new double[] {2550, -1420}, new double[] {2550, 2550}, new double[] {-1740, 2550},
                new double[] {-1740, 2100}, new double[] {-2550, 2100});
        final double lobby = 950;
        final double storey = 465;
        final int top = 33;
        final List<Double> levels = new ArrayList<>(List.of(0.0));
        final List<String> uses = new ArrayList<>(List.of(OfficeTower.LOBBY));
        for (int n = 3; n <= top; n++) {
            levels.add(lobby + (storey(n) - 1) * storey);
            uses.add(n == 30 ? OfficeTower.PLANT : n == top ? "bar" : OfficeTower.OFFICE);
        }
        levels.add(lobby + storey(top) * storey);
        // its two zones, eight cars each, four either side of its lift lobby; the high zone the faster
        final int lowTo = storey(17);
        final int highTo = storey(top);
        // the banks' middles across, each facing its lobby; all of them along the core's north part
        final double along = 370;
        final List<Placed> banks = List.of(
                new Placed(bank(levels, lowTo + 1, highTo, 4, 600), -620, along, 90),
                new Placed(bank(levels, lowTo + 1, highTo, 4, 600), -4, along, -90),
                new Placed(bank(levels, 1, lowTo, 4, 400), 380, along, 90),
                new Placed(bank(levels, 1, lowTo, 4, 400), 950, along, -90));
        final List<double[]> service = List.of(
                // the core's south part: its stairs, its service lift, its toilets, its risers
                new double[] {-1180, 510, -1390, -430},
                // the low zone's east bank's block, its stair under it
                new double[] {820, 1190, -1390, -430},
                // toilets west of the high zone's lifts; the core's walls between and beside its banks
                new double[] {-1180, -750, -260, 1000},
                new double[] {126, 250, -260, 1000},
                new double[] {1080, 1190, -260, 1000});
        return new OfficeTower("MBFC Tower 1", outline, levels, uses, service, banks, 800, 0);
    }

    /**
     * Tower 2: 50 floors, its offices on floors 7 to 37 and 39 to 50, floor 38 plant, floors 3 to 6 parking in the
     * podium; 4.65 m floor to floor over a lobby 9.5 m high, as Tower 1's, its roof some 233 m up and its crown taking
     * it to the 245 m known of it. Some 63 by 48 m low down, notched at its north-west and south-east corners and cut
     * on the slant at its north-east; stepped in on its west and south over floor 38, its south-west cut on the slant,
     * and on its west and north again over floor 44, each step a terrace. Its plans give no lengths: they are scaled by
     * its columns, 9 m apart as the others', and set over each other by its core's fixed part, the same in each.
     *
     * <p>Its core along the middle of its plan, west to east: the low zone's lift lobby (floors 7 to 21, four cars
     * either side, its west bank standing apart), the high zone's (37 to 50, three cars one side, four the other), the
     * mid zone's (22 to 36, four either side), its stairs, service lifts and toilets to the east; toilets under each
     * zone's lobby where that zone does not stop. Twenty-three lifts in three zones. Its way in on its south side.
     */
    static OfficeTower tower2() {
        final List<double[]> low = List.of(
                new double[] {-3160, -2414}, new double[] {2199, -2414}, new double[] {2199, -2119},
                new double[] {3160, -2119}, new double[] {3160, 1960}, new double[] {2353, 2138},
                new double[] {2353, 2414}, new double[] {-2527, 2414}, new double[] {-2527, 1655},
                new double[] {-3160, 1655});
        final List<double[]> lowerHigh = List.of(
                new double[] {-680, -2119}, new double[] {3160, -2119}, new double[] {3160, 1750},
                new double[] {1630, 2140}, new double[] {1630, 2414}, new double[] {-2490, 2414},
                new double[] {-2490, -982}, new double[] {-2228, -982}, new double[] {-2228, -1798});
        final List<double[]> upperHigh = List.of(
                new double[] {-680, -2119}, new double[] {3160, -2119}, new double[] {3160, 1700},
                new double[] {1582, 2036}, new double[] {-2189, 2036}, new double[] {-2189, -1800});
        final double lobby = 950;
        final double storey = 465;
        final int top = 50;
        final List<Double> levels = new ArrayList<>(List.of(0.0));
        final List<String> uses = new ArrayList<>(List.of(OfficeTower.LOBBY));
        for (int n = 3; n <= top; n++) {
            levels.add(lobby + (storey(n) - 1) * storey);
            uses.add(n <= 6 ? OfficeTower.PARKING : n == 38 ? OfficeTower.PLANT : OfficeTower.OFFICE);
        }
        levels.add(lobby + storey(top) * storey);
        final int lowFrom = storey(7);
        final int lowTo = storey(21);
        final int midFrom = storey(22);
        final int midTo = storey(36);
        final int highFrom = storey(37);
        final int highTo = storey(top);
        final double along = 130;
        final List<Placed> banks = List.of(
                new Placed(bank(levels, lowFrom, lowTo, 4, 400), -1893, along, 90),
                new Placed(bank(levels, lowFrom, lowTo, 4, 400), -1305, along, -90),
                new Placed(bank(levels, highFrom, highTo, 3, 800), -880, along, 90),
                new Placed(bank(levels, highFrom, highTo, 4, 800), -310, along, -90),
                new Placed(bank(levels, midFrom, midTo, 4, 600), 58, along, 90),
                new Placed(bank(levels, midFrom, midTo, 4, 600), 655, along, -90));
        final List<double[]> service = List.of(
                // its stairs, service lifts, toilets and risers, east of its lifts
                new double[] {870, 2082, -797, 800},
                // toilets along the core's south side; under the low zone's banks only as high as they go
                new double[] {-1010, 870, -797, -490},
                new double[] {-2140, -1175, -797, -490, 0, lowTo},
                // the core's walls between its banks
                new double[] {-1175, -1010, -490, 800},
                new double[] {-180, -72, -490, 800},
                new double[] {785, 870, -490, 800},
                // toilets in the high and the mid zones' lobbies on the floors under where they stop
                new double[] {-750, -440, -490, 800, 1, highFrom - 1},
                new double[] {188, 525, -490, 800, 1, midFrom - 1});
        return new OfficeTower("MBFC Tower 2", low, levels, uses, service, banks, 1_230, 0,
                List.of(new OfficeTower.Setback(storey(39), lowerHigh), new OfficeTower.Setback(storey(45), upperHigh)));
    }

    /**
     * Tower 3: 46 floors, its offices on floors 5 to 6, 8 to 38 and 40 to 46, floors 7 and 39 plant, floors 3 and 4
     * parking in the podium; a lobby 9.5 m high and 4.9 m floor to floor over it (its offices' ceilings higher than
     * the others'), its roof some 225 m up and its crown taking it to the 245 m known of it. 60.25 m square, as its
     * plans write it, a shallow step of 1.8 m round its north-east corner and its south-west, 14.2 m each way.
     *
     * <p>Its core 32 m across and 29.3 m along, as its plans draw it: stairs and service lifts to the west; two pairs of
     * lift banks, each pair either side of a lift lobby, a passage across the core between their north halves and
     * their south; toilets and a stair to the east. Its plans show two of its five zones: the upper mid zone (floors 23
     * to 30), four cars either side of the east pair's lobby in the south half; the lower high zone (31 to 38), four
     * either side of the west pair's in the south half; the west pair's north half running past both, to the zone over
     * them (40 to 46), nine cars; the east pair's north half gone over floor 22, so the two lowest zones (5 to 13, 14 to
     * 22) are there, five cars each, either side of one lobby. Thirty-five lifts in five zones. Its way in on its south.
     */
    static OfficeTower tower3() {
        final double h = 3012.5;
        final double in = 2832.5;
        final double step = 1592.5;
        final List<double[]> outline = List.of(
                new double[] {-step, -h}, new double[] {h, -h}, new double[] {h, step}, new double[] {in, step},
                new double[] {in, in}, new double[] {step, in}, new double[] {step, h}, new double[] {-h, h},
                new double[] {-h, -step}, new double[] {-in, -step}, new double[] {-in, -in},
                new double[] {-step, -in});
        final double lobby = 950;
        final double storey = 490;
        final int top = 46;
        final List<Double> levels = new ArrayList<>(List.of(0.0));
        final List<String> uses = new ArrayList<>(List.of(OfficeTower.LOBBY));
        for (int n = 3; n <= top; n++) {
            levels.add(lobby + (storey(n) - 1) * storey);
            uses.add(n <= 4 ? OfficeTower.PARKING : n == 7 || n == 39 ? OfficeTower.PLANT : OfficeTower.OFFICE);
        }
        levels.add(lobby + storey(top) * storey);
        final int[][] zones = {{5, 13}, {14, 22}, {23, 30}, {31, 38}, {40, top}};
        final int[] from = new int[5];
        final int[] to = new int[5];
        for (int k = 0; k < 5; k++) {
            from[k] = storey(zones[k][0]);
            to[k] = storey(zones[k][1]);
        }
        // its cars a little narrower than the others', so five fit along half its core; the north halves' banks from
        // the passage north, the south halves' from it south
        final double north5 = 50 + bankLength(5) / 2;
        final double north4 = 50 + bankLength(4) / 2;
        final double south4 = -50 - bankLength(4) / 2;
        final List<Placed> banks = List.of(
                new Placed(bank3(levels, from[0], to[0], 5, 400), 407, north5, 90),
                new Placed(bank3(levels, from[1], to[1], 5, 500), 995, north5, -90),
                new Placed(bank3(levels, from[2], to[2], 4, 600), 407, south4, 90),
                new Placed(bank3(levels, from[2], to[2], 4, 600), 995, south4, -90),
                new Placed(bank3(levels, from[3], to[3], 4, 700), -544, south4, 90),
                new Placed(bank3(levels, from[3], to[3], 4, 700), 36, south4, -90),
                new Placed(bank3(levels, from[4], to[4], 5, 800), -544, north5, 90),
                new Placed(bank3(levels, from[4], to[4], 4, 800), 36, north4, -90));
        final List<double[]> service = List.of(
                // its stairs and service lifts, west of its lifts
                new double[] {-1623, -674, -1465, 1465},
                // the core's wall between its two pairs of banks
                new double[] {166, 277, -1465, 1465},
                // a stair, toilets and risers, east of them
                new double[] {1125, 1580, -910, 820},
                // toilets in a zone's lobby on the floors under where it stops
                new double[] {-414, -94, -1440, -50, 1, from[3] - 1},
                new double[] {-414, -94, 50, 1440, 1, from[4] - 1},
                new double[] {537, 865, -1440, -50, 1, from[2] - 1});
        return new OfficeTower("MBFC Tower 3", outline, levels, uses, service, banks, 24_500 - levels.getLast(), 0);
    }

    /** How long a bank of {@code cars} of Tower 3's cars is, side to side. */
    private static double bankLength(final int cars) {
        return cars * (240 + 2 * HighRiseBank.GAP + HighRiseBank.SHAFT_WALL) + HighRiseBank.SHAFT_WALL;
    }

    /** A bank of Tower 3's cars: 2.4 m wide, else as the others'. */
    private static HighRiseBank bank3(final List<Double> levels, final int from, final int to, final int cars,
                                      final double speed) {
        return OfficeTower.bank(levels.subList(0, to + 1), from, to, cars, speed, 240, 210, 270, 110);
    }

    /**
     * A bank of {@code cars} of the towers' cars serving the lobby and floors {@code from} to {@code to} of
     * {@code levels}, at {@code speed}: each car 2.6 m wide and 2.1 m deep outside, 2.7 m high inside, its doorway
     * 1.1 m.
     */
    private static HighRiseBank bank(final List<Double> levels, final int from, final int to, final int cars,
                                     final double speed) {
        return OfficeTower.bank(levels.subList(0, to + 1), from, to, cars, speed, 260, 210, 270, 110);
    }
}
