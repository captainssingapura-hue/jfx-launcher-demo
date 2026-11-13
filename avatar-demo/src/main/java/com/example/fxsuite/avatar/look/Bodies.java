package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.look.Sheets.Sheet;
import com.example.fxsuite.avatar.look.Sheets.Slot;
import com.example.fxsuite.avatar.sim.Course;

import java.util.ArrayList;
import java.util.List;

/**
 * Vehicles' bodies, as they are seen: the sheets of metal and glass each is made of, on its chassis,
 * in the chassis's frame (across, up from the top of its bed, along from its middle). Rough for
 * now: flat panels, few of them; each body is built from a handful of shapes:
 *
 * <ul>
 *   <li>a <b>profile</b>, the line over the top of the body seen from its side, pushed out across
 *       its width: a strip of sheet between each two points of it (the bonnet, the windscreen, the
 *       roof, ...), each of its own slot;</li>
 *   <li>a <b>side</b>, a flat outline at one side of it (the doors and wings below the windows, say),
 *       which may be any shape, hollows and all;</li>
 *   <li>a <b>panel</b>, a flat sheet with its corners given (a tailgate, a bumper, a lamp).</li>
 * </ul>
 *
 * <p>Only seen: to the world, a vehicle is its chassis, as a whole. What its body changes there is
 * how it takes harm ({@link Course.Bodywork}).
 */
public final class Bodies {

    /** How thick the panels are, the glass, and the bumpers. */
    private static final double SHEET = 4;
    private static final double PANE = 3;
    private static final double BUMPER = 8;

    private Bodies() {
    }

    /** The sheets of {@code body}, by its name; none for none, or for one there is no drawing of. */
    public static List<Sheet> of(final Course.Bodywork body) {
        if (body == null) {
            return List.of();
        }
        return switch (body.name()) {
            case "pickup" -> pickup();
            case "roadster" -> roadster();
            case "truck" -> truck();
            case "van" -> van();
            default -> List.of();
        };
    }

    /**
     * A pickup's: a cab over the seat, its bonnet before it, a windscreen sloping back to a roof,
     * a rear window, the side windows open; behind, an open bed with low sides and a tailgate, round
     * the gun's post. Its sides stand out over the wheels, as wings do.
     */
    private static List<Sheet> pickup() {
        final double w = 132;
        final List<Sheet> sheets = new ArrayList<>();
        // the cab: front, bonnet, scuttle, windscreen, roof, rear window, back
        profile(sheets, w, new double[][] {{260, -28}, {260, 50}, {200, 66}, {188, 70}, {126, 172}, {40, 172},
                {40, 108}, {40, -28}}, Slot.PAINT, Slot.PAINT, Slot.PAINT, Slot.GLASS, Slot.PAINT, Slot.GLASS, Slot.PAINT);
        for (final double side : new double[] {-w, w}) {
            // below the windows, and the pillars either side of them
            side(sheets, side, new double[][] {{40, -28}, {260, -28}, {260, 50}, {200, 66}, {188, 70}, {180, 84},
                    {40, 84}}, Slot.PAINT);
            side(sheets, side, new double[][] {{180, 84}, {170, 84}, {117, 172}, {126, 172}}, Slot.PAINT);
            side(sheets, side, new double[][] {{40, 84}, {54, 84}, {54, 172}, {40, 172}}, Slot.PAINT);
            // the bed's sides
            side(sheets, side, new double[][] {{-262, -28}, {40, -28}, {40, 48}, {-262, 48}}, Slot.PAINT);
        }
        // the tailgate
        panel(sheets, SHEET, Slot.PAINT, new double[] {-w, -28, -262}, new double[] {w, -28, -262},
                new double[] {w, 48, -262}, new double[] {-w, 48, -262});
        ends(sheets, w, 260, -262);
        return sheets;
    }

    /**
     * A roadster's: open and low, a long bonnet before the seat, under the gun's barrels; a little
     * windscreen before the driver alone, to leave the gun clear; and a deck behind the seat.
     */
    private static List<Sheet> roadster() {
        final double w = 120;
        final List<Sheet> sheets = new ArrayList<>();
        // the front, the bonnet and the scuttle, up to the open cockpit
        profile(sheets, w, new double[][] {{202, -26}, {202, 28}, {110, 40}, {98, 44}}, Slot.PAINT, Slot.PAINT,
                Slot.PAINT);
        // the deck behind the seat, and the back
        profile(sheets, w, new double[][] {{2, 56}, {-120, 58}, {-198, 50}, {-202, -26}}, Slot.PAINT, Slot.PAINT,
                Slot.PAINT);
        for (final double side : new double[] {-w, w}) {
            side(sheets, side, new double[][] {{-202, -26}, {202, -26}, {202, 28}, {110, 40}, {98, 44}, {70, 56},
                    {-120, 58}, {-198, 50}}, Slot.PAINT);
        }
        // the driver's windscreen, on the left, in its frame
        panel(sheets, PANE, Slot.GLASS, new double[] {-100, 44, 98}, new double[] {-6, 44, 98},
                new double[] {-6, 92, 84}, new double[] {-100, 92, 84});
        panel(sheets, SHEET, Slot.TRIM, new double[] {-104, 42, 99}, new double[] {-2, 42, 99},
                new double[] {-2, 46, 99}, new double[] {-104, 46, 99});
        ends(sheets, w, 202, -202);
        return sheets;
    }

    /**
     * A van's: tall and boxy on the car's chassis. A short low nose, under the barrels of the gun
     * fixed beside the seat; a big windscreen sloping back to a high roof, glazed before the driver
     * and open on the right, a port for the gun to fire through; the driver's door window open, the
     * sides glazed behind it between their pillars; and at the back a hatch, glazed above.
     */
    private static List<Sheet> van() {
        final double w = 118;
        final List<Sheet> sheets = new ArrayList<>();
        // the front, the low bonnet; the roof; the hatch, glazed above and painted below
        profile(sheets, w, new double[][] {{202, -26}, {202, 40}, {150, 46}}, Slot.PAINT, Slot.PAINT);
        profile(sheets, w, new double[][] {{96, 186}, {-186, 188}, {-202, 108}, {-202, -26}}, Slot.PAINT,
                Slot.GLASS, Slot.PAINT);
        // the windscreen: glass before the driver, open on the right for the gun, a pillar between
        panel(sheets, PANE, Slot.GLASS, new double[] {-w, 46, 150}, new double[] {-4, 46, 150},
                new double[] {-4, 186, 96}, new double[] {-w, 186, 96});
        panel(sheets, SHEET, Slot.PAINT, new double[] {-4, 46, 150}, new double[] {6, 46, 150},
                new double[] {6, 186, 96}, new double[] {-4, 186, 96});
        // and over the port, the windscreen's frame along the roof
        panel(sheets, SHEET, Slot.PAINT, new double[] {6, 170, 102}, new double[] {w, 170, 102},
                new double[] {w, 186, 96}, new double[] {6, 186, 96});
        for (final double side : new double[] {-w, w}) {
            // below the windows, all along
            side(sheets, side, new double[][] {{-202, -26}, {202, -26}, {202, 40}, {150, 46}, {131, 96},
                    {-202, 96}}, Slot.PAINT);
            // the pillars: by the windscreen, behind the driver's door, and at the back
            side(sheets, side, new double[][] {{131, 96}, {119, 96}, {84, 186}, {96, 186}}, Slot.PAINT);
            side(sheets, side, new double[][] {{56, 96}, {40, 96}, {40, 186}, {56, 186}}, Slot.PAINT);
            side(sheets, side, new double[][] {{-180, 96}, {-202, 96}, {-202, 108}, {-186, 188}, {-180, 188}},
                    Slot.PAINT);
            // the glass behind the driver's door, and the rail over it
            side(sheets, side, new double[][] {{40, 96}, {-180, 96}, {-180, 172}, {40, 172}}, Slot.GLASS);
            side(sheets, side, new double[][] {{96, 172}, {-180, 172}, {-180, 188}, {96, 188}}, Slot.PAINT);
        }
        ends(sheets, w, 202, -202);
        return sheets;
    }

    /**
     * A truck's: a square cab over its front wheels, its windscreen upright in front, the side
     * windows open; behind it, a flatbed with low sides and a tailgate.
     */
    private static List<Sheet> truck() {
        final double w = 156;
        final List<Sheet> sheets = new ArrayList<>();
        profile(sheets, w, new double[][] {{352, -30}, {352, 95}, {340, 205}, {95, 205}, {95, -30}}, Slot.PAINT,
                Slot.GLASS, Slot.PAINT, Slot.PAINT);
        for (final double side : new double[] {-w, w}) {
            side(sheets, side, new double[][] {{95, -30}, {352, -30}, {352, 95}, {350, 110}, {95, 110}}, Slot.PAINT);
            side(sheets, side, new double[][] {{350, 110}, {336, 110}, {326, 205}, {340, 205}}, Slot.PAINT);
            side(sheets, side, new double[][] {{95, 110}, {112, 110}, {112, 205}, {95, 205}}, Slot.PAINT);
            side(sheets, side, new double[][] {{-354, -30}, {95, -30}, {95, 40}, {-354, 40}}, Slot.PAINT);
        }
        panel(sheets, SHEET, Slot.PAINT, new double[] {-w, -30, -354}, new double[] {w, -30, -354},
                new double[] {w, 40, -354}, new double[] {-w, 40, -354});
        ends(sheets, w, 352, -354);
        return sheets;
    }

    /** Bumpers across the front at {@code front} and the back at {@code back}; headlamps on the front, tail lamps on the back. */
    private static void ends(final List<Sheet> sheets, final double w, final double front, final double back) {
        for (final double along : new double[] {front + 6, back - 6}) {
            panel(sheets, BUMPER, Slot.TRIM, new double[] {-w, -32, along}, new double[] {w, -32, along},
                    new double[] {w, 2, along}, new double[] {-w, 2, along});
        }
        for (final double side : new double[] {-1, 1}) {
            final double across = side * (w - 30);
            panel(sheets, PANE, Slot.LAMP, new double[] {across - 16, 18, front + 2},
                    new double[] {across + 16, 18, front + 2}, new double[] {across + 16, 34, front + 2},
                    new double[] {across - 16, 34, front + 2});
            panel(sheets, PANE, Slot.TAIL, new double[] {across - 12, 14, back - 2},
                    new double[] {across + 12, 14, back - 2}, new double[] {across + 12, 32, back - 2},
                    new double[] {across - 12, 32, back - 2});
        }
    }

    /**
     * The line {@code points} (each along, up) over the top of a body, pushed out across it from
     * {@code -w} to {@code w}: a strip of sheet between each two of its points, each of its own slot.
     */
    private static void profile(final List<Sheet> sheets, final double w, final double[][] points,
                                final Slot... slots) {
        for (int i = 0; i + 1 < points.length; i++) {
            final double[] a = points[i];
            final double[] b = points[i + 1];
            panel(sheets, slots[i] == Slot.GLASS ? PANE : SHEET, slots[i], new double[] {-w, a[1], a[0]},
                    new double[] {w, a[1], a[0]}, new double[] {w, b[1], b[0]}, new double[] {-w, b[1], b[0]});
        }
    }

    /** A flat outline {@code points} (each along, up) at {@code across}, at one side of a body. */
    private static void side(final List<Sheet> sheets, final double across, final double[][] points, final Slot slot) {
        final List<double[]> corners = new ArrayList<>();
        for (final double[] p : points) {
            corners.add(new double[] {across, p[1], p[0]});
        }
        sheets.add(new Sheet(corners, SHEET, slot));
    }

    /** A flat panel, its {@code corners} (each across, up, along) in order round it. */
    private static void panel(final List<Sheet> sheets, final double thickness, final Slot slot,
                              final double[]... corners) {
        sheets.add(new Sheet(List.of(corners), thickness, slot));
    }
}
