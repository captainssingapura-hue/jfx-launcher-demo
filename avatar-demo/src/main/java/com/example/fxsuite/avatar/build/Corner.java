package com.example.fxsuite.avatar.build;

/**
 * How an outside corner of a floor plan is shaped: as it is, square; or cut back {@code across}
 * along the edge that runs across and {@code along} along the one that runs along, the two joined
 * by a quarter of a superellipse of exponent {@code squareness}: 1 a straight cut (a chamfer), 2 a
 * quarter of an ellipse (of a circle, if the two are equal), more than 2 fuller, toward the square
 * corner it was. A rectangle all four of whose corners are so cut back by half its width and depth
 * is an ellipse (a circle, if it is square); at 1, a diamond; at 4 or so, a rounded square.
 *
 * <p>The structure's grid stays square under it: only the plan's outline, its slab's edge and its
 * facade, follows the curve, in straight pieces no wider than a facade's panel ({@link #pieces}).
 *
 * @param across     how far back along its edge that runs across, in centimetres (0: square)
 * @param along      how far back along its edge that runs along
 * @param squareness the superellipse's exponent: 1 straight, 2 round, more fuller
 */
public record Corner(double across, double along, double squareness) {

    /** As it is: square. */
    public static final Corner SQUARE = new Corner(0, 0, 2);
    /** A facade's panels on a curve are this wide at most: the curve is drawn in straight pieces so long. */
    public static final double PANEL = 150;

    public Corner {
        if (across < 0 || along < 0 || squareness <= 0) {
            throw new IllegalArgumentException("a corner cut back by " + across + " and " + along + ", " + squareness);
        }
    }

    /** Cut straight back by {@code back} each way. */
    public static Corner chamfer(final double back) {
        return new Corner(back, back, 1);
    }

    /** Rounded, a quarter circle of {@code radius}. */
    public static Corner round(final double radius) {
        return new Corner(radius, radius, 2);
    }

    /** Rounded, a quarter ellipse {@code across} by {@code along}, as full as {@code squareness}. */
    public static Corner oval(final double across, final double along, final double squareness) {
        return new Corner(across, along, squareness);
    }

    public boolean square() {
        return across == 0 || along == 0;
    }

    /**
     * Its curve, from where it leaves the edge coming in ({@code t} 0) to where it meets the edge
     * going out ({@code t} 1), as fractions of the way back along each: at {@code t}, how far back
     * from the corner along the edge coming in, and how far along the edge going out, each as a
     * fraction of its cut (0 at the corner, 1 at the cut's end).
     */
    public double[] at(final double t) {
        final double angle = t * Math.PI / 2;
        // a quarter superellipse round the point a cut back along both edges, from the edge coming in to the one going out
        final double c = Math.pow(Math.cos(angle), 2 / squareness);
        final double s = Math.pow(Math.sin(angle), 2 / squareness);
        return new double[] {1 - s, 1 - c};
    }

    /** How many straight pieces its curve is drawn in, each no wider than a panel: one, cut straight. */
    public int pieces(final double in, final double out) {
        if (square()) {
            return 0;
        }
        if (squareness == 1) {
            return 1;
        }
        double length = 0;
        double[] last = at(0);
        for (int k = 1; k <= 64; k++) {
            final double[] p = at(k / 64.0);
            length += Math.hypot((p[0] - last[0]) * in, (p[1] - last[1]) * out);
            last = p;
        }
        return Math.max(2, (int) Math.ceil(length / PANEL));
    }
}
