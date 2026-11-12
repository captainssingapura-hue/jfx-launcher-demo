package com.example.fxsuite.avatar.sim;

import java.util.List;

/**
 * The jet-pack's swing wings, as the simulation knows them: where they are, so they stop
 * against what they meet. The graphics draw them from the same numbers, so what is seen is
 * what collides. Pure.
 *
 * <p>Each wing lies in the plane of the back, behind the body, pivoting at the side of the
 * pack. {@code sweep} is how far back from straight out to the side it points, in degrees. Seen
 * from above, then, a wing is a straight stroke from its pivot out to its tip. That is how it
 * collides: a line {@link #REACH} thick each side, at the heights the wing covers. A flat
 * leading edge that meets a post square on is stopped; a tip caught on a corner glances off.
 * As the wings sweep back they reach less far out to the sides and lower down.
 *
 * <p>The sweep is scheduled by speed, as a variable-sweep fighter's is (see {@link #sweepFor}).
 */
public record Wings(double sweep) {

    /** The pivots: out from the middle of the back, behind it, and up from the feet. */
    public static final double PIVOT_OUT = 32;
    public static final double PIVOT_BEHIND = 36;
    public static final double PIVOT_HEIGHT = 132;
    /** From the pivot out to the tip, and the chord where it is widest, at the root. */
    public static final double SPAN = 62;
    public static final double ROOT_CHORD = 28;
    /** Degrees back from straight out to the side: stowed, spread, and fully swept. */
    public static final double STOWED = 75;
    public static final double SPREAD = 20;
    public static final double SWEPT = 68;
    /** Below this forward speed the wings stay spread; by full boost they are fully swept. */
    public static final double SLOW = 0.3 * Body.FLY;
    /** How quickly the wings swing: most of the way in about a sixth of a second. */
    public static final double SWING = 6;
    /** How thick a wing is taken to be, each side of its line, with a little to spare. */
    public static final double REACH = 8;
    /**
     * The furthest the body moves in one collision step with its wings out: less than a wing's
     * thickness, so a wing never gets into a bar before it is found to touch it.
     */
    public static final double STEP = 7;

    /**
     * One wing, from above, in the body's own frame: from {@code from} out to its right to
     * {@code to} (both negative for the left wing), {@code behind} it, covering the heights
     * {@code bottom} to {@code top} above the feet.
     */
    public record Span(double from, double to, double behind, double bottom, double top) {
    }

    /**
     * The sweep the flight calls for:
     * <ul>
     *   <li>stowed, tucked down along the engines, when not flying;</li>
     *   <li>spread wide when hovering upright on the jets or flying slowly;</li>
     *   <li>swept back more and more as it speeds up, fully at full boost.</li>
     * </ul>
     */
    public static double sweepFor(final boolean worn, final Body body) {
        if (!worn || !body.flying()) {
            return STOWED;
        }
        final double fast = Num.clamp((body.speed() - SLOW) / (Body.BOOST - SLOW), 0, 1);
        return SPREAD + (SWEPT - SPREAD) * fast * fast * (3 - 2 * fast);
    }

    /** How far out to the side a tip reaches, from the middle of the body, thickness and all. */
    public double reach() {
        return PIVOT_OUT + SPAN * Math.cos(Math.toRadians(sweep)) + REACH;
    }

    /** Both wings, the left then the right. */
    public List<Span> spans() {
        final double c = Math.cos(Math.toRadians(sweep));
        final double s = Math.sin(Math.toRadians(sweep));
        final double tip = PIVOT_OUT + SPAN * c;
        // from the root's leading edge, the chord turned with the wing, down to the tip
        final double top = PIVOT_HEIGHT + ROOT_CHORD / 2 * c + REACH;
        final double bottom = PIVOT_HEIGHT - SPAN * s - ROOT_CHORD / 2 * c - REACH;
        return List.of(new Span(-PIVOT_OUT, -tip, PIVOT_BEHIND, bottom, top),
                new Span(PIVOT_OUT, tip, PIVOT_BEHIND, bottom, top));
    }
}
