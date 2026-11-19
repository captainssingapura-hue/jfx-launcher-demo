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
                new Placed(bank(levels, lowTo + 1, highTo, 600), -620, along, 90),
                new Placed(bank(levels, lowTo + 1, highTo, 600), -4, along, -90),
                new Placed(bank(levels, 1, lowTo, 400), 380, along, 90),
                new Placed(bank(levels, 1, lowTo, 400), 950, along, -90));
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
     * A bank of four of Tower 1's cars serving the lobby and floors {@code from} to {@code to} of {@code levels}, at
     * {@code speed}: each car 2.6 m wide and 2.1 m deep outside, 2.7 m high inside, its doorway 1.1 m.
     */
    private static HighRiseBank bank(final List<Double> levels, final int from, final int to, final double speed) {
        return OfficeTower.bank(levels.subList(0, to + 1), from, to, 4, speed, 260, 210, 270, 110);
    }
}
