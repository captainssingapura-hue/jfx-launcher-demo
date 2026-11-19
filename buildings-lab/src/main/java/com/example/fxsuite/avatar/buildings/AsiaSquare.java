package com.example.fxsuite.avatar.buildings;

import com.example.fxsuite.avatar.lifts.HighRiseBank;
import com.example.fxsuite.avatar.sim.Place;

import java.util.ArrayList;
import java.util.List;

/**
 * The bay's Asia Square, its two towers: each an office tower ({@link OfficeTower}) from the typical floor plans its
 * owner publishes, as the financial centre's and One Raffles Quay's are; their heights and storeys as published. Their
 * plans write only how far their cores stand from their glass, not their lengths: so they are scaled by those, some
 * 7.8 of the plans' pixels a metre, and their figures are as good as that. No sheet lists their floors' uses or their
 * lifts' zones: those are estimates, said so where they are made.
 *
 * <p>Each in its own frame, its plan's top along, cm, about its outline's middle; a storey the model's, the lobby the
 * first (0), double height, the building's floors 1 and 2; floor {@code n} over it the model's {@code n - 2}.
 */
final class AsiaSquare {

    private AsiaSquare() {
    }

    /** The first key the pair is drawn by in the bay, each taking {@link Standing#KEYS} from it in turn. */
    static final int KEYS = 1_000_000;

    /**
     * The pair where they stand in the bay ({@link com.example.fxsuite.avatar.build.MarinaBay}'s frame, cm): each where
     * the map has its middle, turned as its sides run there. The map draws both the same, 64.6 by 56.5 m, its long sides
     * at 106°; Tower 1's plan's long sides along them, Tower 2's, nearly square, its longer sides along them. Which end
     * of each is its plan's top, the plans do not say: a guess.
     */
    static List<Standing> standing() {
        final double[][] where = {{-85_590, -78_710, -74.2}, {-88_680, -89_810, 15.9}};
        final List<OfficeTower> towers = List.of(tower1(), tower2());
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
     * Tower 1: 43 floors, 229 m to its top, as published; 4.8 m floor to floor over a lobby 9.5 m high (its ceilings
     * up to 3.05 m, its raised floors high, as its owner says), its crown the rest. About 56.4 by 66.3 m, as its plans
     * scale, two shallow notches in each face.
     *
     * <p>Its core two columns side by side, a passage between them, each four rows of lifts deep: in the west column
     * the ultra high zone's lobby over the low zone's, in the east the high zone's over the mid zone's, four cars either
     * side of each lobby; a fire lift lobby and stair, a tenant's room and plant at its north end, plant and a stair at
     * its south. Thirty-two lifts in four zones, as its plans draw them. Its offices taken as floors 3 to 43, in zones
     * of some ten floors: an estimate, no sheet saying. Toilets in a zone's lobby where it does not stop, and where a
     * zone's banks were over it, as its plans have them. Its way in on its south side, between its notches.
     */
    static OfficeTower tower1() {
        final List<double[]> outline = notched(2820, 3315, new double[] {-1180, -730, 940, 1380},
                new double[] {-995, -765, 1545, 1775}, 190);
        final int top = 43;
        final List<Double> levels = levels(top, 31, 480, 480);
        final List<String> uses = new ArrayList<>(List.of(OfficeTower.LOBBY));
        for (int n = 3; n <= top; n++) {
            uses.add(OfficeTower.OFFICE);
        }
        final int lowTo = Mbfc.storey(12);
        final int midTo = Mbfc.storey(22);
        final int highTo = Mbfc.storey(32);
        final int ultraTo = Mbfc.storey(top);
        final int lowFrom = Mbfc.storey(3);
        final int midFrom = lowTo + 1;
        final int highFrom = midTo + 1;
        final int ultraFrom = highTo + 1;
        // each lobby's banks either side of it, along the core; a bank's yaw 180 faces south, 0 north
        final List<Placed> banks = List.of(
                new Placed(bank(levels, ultraFrom, ultraTo, 4, 800), -785, 940, 180),
                new Placed(bank(levels, ultraFrom, ultraTo, 4, 800), -785, 300, 0),
                new Placed(bank(levels, highFrom, highTo, 4, 650), 785, 940, 180),
                new Placed(bank(levels, highFrom, highTo, 4, 650), 785, 300, 0),
                new Placed(bank(levels, lowFrom, lowTo, 4, 400), -785, -35, 180),
                new Placed(bank(levels, lowFrom, lowTo, 4, 400), -785, -695, 0),
                new Placed(bank(levels, midFrom, midTo, 4, 500), 785, -35, 180),
                new Placed(bank(levels, midFrom, midTo, 4, 500), 785, -695, 0));
        final List<double[]> service = List.of(
                // the fire lift lobby and a stair; a tenant's room and plant: at its north end
                new double[] {-1400, -188, 1080, 1830},
                new double[] {188, 1420, 1080, 1830},
                // plant; a stair: at its south end
                new double[] {-1400, -188, -1600, -850},
                new double[] {188, 1420, -1620, -850},
                // toilets in the ultra high and the high zones' lobbies under where they stop
                new double[] {-1382, -188, 430, 810, 1, ultraFrom - 1},
                new double[] {188, 1382, 430, 810, 1, highFrom - 1},
                // and where the low and the mid zones' banks and lobbies were, over them
                new double[] {-1382, -188, -840, -150, lowTo + 1, top},
                new double[] {188, 1382, -840, -150, midTo + 1, top});
        return new OfficeTower("Asia Square Tower 1", outline, levels, uses, service, banks,
                22_900 - levels.getLast(), 4);
    }

    /**
     * Tower 2: 46 floors, 221.5 m to its top, as published; its offices under its hotel, which is on floors 32 to 46,
     * its shops on floors 1 and 2, as published; 4.6 m floor to floor for its offices, 3.8 m for its hotel, over a lobby
     * 9.5 m high, its crown the rest (estimates). About 60.0 by 58.5 m, as its plans scale, two shallow notches in each
     * face.
     *
     * <p>Its core a block of stairs, fire lifts, plant and the hotel's lift shafts to the west; to the east, its office
     * lifts, three lobbies one over another along it, four cars either side of each: the mid zone's at its north, the
     * high zone's in its middle, the low zone's at its south. Twenty-four lifts in three zones, as its plans draw them.
     * Its offices taken as floors 3 to 31, in zones of some ten floors: an estimate. Its hotel's lifts not built; over
     * its offices, the east block's lobbies and shafts its core. Its way in on its south side, between its notches.
     */
    static OfficeTower tower2() {
        final List<double[]> outline = notched(3000, 2925, new double[] {-1190, -870, 840, 1190},
                new double[] {-1140, -690, 790, 1180}, 190);
        final int top = 46;
        final List<Double> levels = levels(top, 31, 460, 380);
        final List<String> uses = new ArrayList<>(List.of(OfficeTower.LOBBY));
        for (int n = 3; n <= top; n++) {
            uses.add(n <= 31 ? OfficeTower.OFFICE : "hotel");
        }
        final int lowFrom = Mbfc.storey(3);
        final int lowTo = Mbfc.storey(12);
        final int midFrom = lowTo + 1;
        final int midTo = Mbfc.storey(22);
        final int highFrom = midTo + 1;
        final int highTo = Mbfc.storey(31);
        final List<Placed> banks = List.of(
                new Placed(bank(levels, midFrom, midTo, 4, 500), 730, 1560, 180),
                new Placed(bank(levels, midFrom, midTo, 4, 500), 730, 920, 0),
                new Placed(bank(levels, highFrom, highTo, 4, 600), 730, 600, 180),
                new Placed(bank(levels, highFrom, highTo, 4, 600), 730, -40, 0),
                new Placed(bank(levels, lowFrom, lowTo, 4, 400), 730, -330, 180),
                new Placed(bank(levels, lowFrom, lowTo, 4, 400), 730, -970, 0));
        final List<double[]> service = List.of(
                // the west block: stairs, fire lifts, plant; the hotel's lift shafts, a stair, plant
                new double[] {-1550, -230, 170, 1760},
                new double[] {-1550, -230, -1210, 20},
                // toilets in the mid and the high zones' lobbies under where they stop
                new double[] {133, 1327, 1050, 1430, 1, midFrom - 1},
                new double[] {133, 1327, 90, 470, 1, highFrom - 1},
                // and where each zone's banks and lobby were, over it
                new double[] {133, 1327, 805, 1690, midTo + 1, top},
                new double[] {133, 1327, -170, 730, highTo + 1, top},
                new double[] {133, 1327, -1100, -215, lowTo + 1, top});
        return new OfficeTower("Asia Square Tower 2", outline, levels, uses, service, banks,
                22_150 - levels.getLast(), 4);
    }

    /**
     * A rectangle {@code a} across and {@code l} along either way of its middle, each face notched {@code deep} in at
     * two places: its north and south faces between the pairs of {@code across}, its east and west between the pairs
     * of {@code along}. Anticlockwise from its south-west corner.
     */
    private static List<double[]> notched(final double a, final double l, final double[] across, final double[] along,
                                          final double deep) {
        final List<double[]> outline = new ArrayList<>();
        outline.add(new double[] {-a, -l});
        for (int k = 0; k < across.length; k += 2) {
            outline.add(new double[] {across[k], -l});
            outline.add(new double[] {across[k], -l + deep});
            outline.add(new double[] {across[k + 1], -l + deep});
            outline.add(new double[] {across[k + 1], -l});
        }
        outline.add(new double[] {a, -l});
        for (int k = 0; k < along.length; k += 2) {
            outline.add(new double[] {a, along[k]});
            outline.add(new double[] {a - deep, along[k]});
            outline.add(new double[] {a - deep, along[k + 1]});
            outline.add(new double[] {a, along[k + 1]});
        }
        outline.add(new double[] {a, l});
        for (int k = across.length - 1; k > 0; k -= 2) {
            outline.add(new double[] {across[k], l});
            outline.add(new double[] {across[k], l - deep});
            outline.add(new double[] {across[k - 1], l - deep});
            outline.add(new double[] {across[k - 1], l});
        }
        outline.add(new double[] {-a, l});
        for (int k = along.length - 1; k > 0; k -= 2) {
            outline.add(new double[] {-a, along[k]});
            outline.add(new double[] {-a + deep, along[k]});
            outline.add(new double[] {-a + deep, along[k - 1]});
            outline.add(new double[] {-a, along[k - 1]});
        }
        return outline;
    }

    /**
     * Floors 3 to {@code top} over a lobby 9.5 m high standing for floors 1 and 2: those up to {@code upTo}
     * {@code storey} high, those over it {@code over} high; and the roof.
     */
    private static List<Double> levels(final int top, final int upTo, final double storey, final double over) {
        final List<Double> levels = new ArrayList<>(List.of(0.0, 950.0));
        for (int n = 4; n <= top + 1; n++) {
            levels.add(levels.getLast() + (n - 1 <= upTo ? storey : over));
        }
        return levels;
    }

    /** A bank of {@code cars} of the towers' cars, as the others' are, serving the lobby and a zone. */
    private static HighRiseBank bank(final List<Double> levels, final int from, final int to, final int cars,
                                     final double speed) {
        return OfficeTower.bank(levels.subList(0, to + 1), from, to, cars, speed, 260, 210, 270, 110);
    }
}
