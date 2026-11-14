package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.build.Grid.Quarter;

/**
 * Common floor plans for a tower, each composed of rectangles of grid ({@link Plan}), its middle at
 * 0, 0: a square, a rectangle; a rectangle with its corners rounded; a circle, an oval as round or
 * as square as asked; an L; and the two put together: a rectangle with one end round, a rectangle
 * with a round bow out of its front. Each as big as asked, its bays as near as asked, and one more
 * thing as each has it ({@link Shape#extra}).
 */
public final class Plans {

    private Plans() {
    }

    /** A plan's shape, and what its one more thing is, and how far it goes. */
    public enum Shape {
        SQUARE("Square", null, 0, 0, 0),
        RECTANGLE("Rectangle", null, 0, 0, 0),
        ROUNDED("Rounded rectangle", "Corner radius (m)", 0, 1500, 600),
        CIRCLE("Circle", null, 0, 0, 0),
        OVAL("Oval", "Squareness", 1, 8, 2),
        L_SHAPE("L shape", "Arm width (m)", 900, 3600, 1800),
        ROUND_END("Rectangle, one end round", null, 0, 0, 0),
        BOW("Rectangle with a bow front", "Bow radius (m)", 450, 2400, 1200);

        private final String title;
        private final String extra;
        private final double least;
        private final double most;
        private final double usual;

        Shape(final String title, final String extra, final double least, final double most, final double usual) {
            this.title = title;
            this.extra = extra;
            this.least = least;
            this.most = most;
            this.usual = usual;
        }

        public String title() {
            return title;
        }

        /** What its one more thing is called; null, if it has none. */
        public String extra() {
            return extra;
        }

        public double least() {
            return least;
        }

        public double most() {
            return most;
        }

        public double usual() {
            return usual;
        }

        /** Whether it is as deep as it is wide, whatever it is asked. */
        public boolean even() {
            return this == SQUARE || this == CIRCLE;
        }
    }

    /**
     * How big: {@code width} across, {@code depth} along, bays as near {@code bay} as divide them
     * evenly, and its one more thing ({@code extra}), in centimetres but for an oval's squareness.
     */
    public record Size(double width, double depth, double bay, double extra) {
    }

    /** Which of {@code lines}, but its first and last, is nearest {@code at}. */
    private static int nearest(final double[] lines, final double at) {
        int best = 1;
        for (int k = 1; k < lines.length - 1; k++) {
            if (Math.abs(lines[k] - at) < Math.abs(lines[best] - at)) {
                best = k;
            }
        }
        return Math.min(best, lines.length - 1);
    }

    /** A plan of {@code shape}, as big as {@code size} says. */
    public static Plan of(final Shape shape, final Size size) {
        final double w = size.width();
        final double d = shape.even() ? size.width() : size.depth();
        final double bay = size.bay();
        final double e = size.extra();
        final String name = shape.title().toLowerCase(java.util.Locale.ROOT);
        final Grid whole = Grid.of(name, -w / 2, -d / 2, w, d, bay);
        return switch (shape) {
            case SQUARE, RECTANGLE -> Plan.of(name, whole);
            case ROUNDED -> Plan.of(name, whole.shaped(Corner.round(Math.min(e, Math.min(w, d) / 2))));
            case CIRCLE -> Plan.of(name, whole.shaped(Corner.round(w / 2)));
            case OVAL -> Plan.of(name, whole.shaped(Corner.oval(w / 2, d / 2, e)));
            case L_SHAPE -> {
                // one grid, its north-east corner cut out along its own lines: its arms as near as asked
                final double[] xs = whole.xs();
                final double[] ys = whole.ys();
                final int i = nearest(xs, -w / 2 + e);
                final int j = nearest(ys, -d / 2 + e);
                final Grid corner = Grid.lines("cut", java.util.Arrays.copyOfRange(xs, i, xs.length),
                        java.util.Arrays.copyOfRange(ys, j, ys.length), java.util.Map.of());
                yield Plan.of(name, whole).cut(corner);
            }
            case ROUND_END -> Plan.of(name, whole.shaped(Quarter.SOUTH_EAST, Corner.round(Math.min(d / 2, w)))
                    .shaped(Quarter.NORTH_EAST, Corner.round(Math.min(d / 2, w))));
            case BOW -> {
                final double r = Math.min(e, Math.min(w / 2, d / 2));
                // a grid out of the front's middle, as deep as half its width, its outer corners rounded to
                // quarter circles: a half circle; its south side on the front, its lines moved onto the front's
                final Grid bow = Grid.of("bow", -r, d / 2, 2 * r, r, bay)
                        .shaped(Quarter.NORTH_WEST, Corner.round(r)).shaped(Quarter.NORTH_EAST, Corner.round(r));
                yield Plan.of(name, whole, bow);
            }
        };
    }
}
