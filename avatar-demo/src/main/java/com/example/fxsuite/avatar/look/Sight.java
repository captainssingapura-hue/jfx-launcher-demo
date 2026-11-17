package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.sim.Num;

/**
 * How far the camera sees, by how high it is over the ground under it ({@link #reach}): a few kilometres on the
 * ground, tens of kilometres from thousands of metres up. Everything that ends the view follows it: the haze, none
 * within {@link #CLEAR} of it and all of it short of its end ({@link #haze}), the sky round it, how far the camera
 * draws; and the land is drawn no further than {@link #BUILT} of it.
 *
 * <p>How far is smoothed as the height is ({@link #at}): taken in quickly climbing, let go slowly coming down, so
 * the land does not come and go with every bump.
 */
public final class Sight {

    /** How far is seen from on the ground; how much higher sees that much further again, as its square root; the most. */
    static final double GROUND = 400_000;
    static final double HIGHER = 5_000;
    static final double MOST = 4_000_000;
    /** How much of how far is clear of haze; how much is hazed, from there, till all of it is. */
    static final double CLEAR = 0.3;
    static final double HAZED = 0.59;
    /** How much further than is seen the land is drawn: so what comes into view as the camera climbs is there. */
    static final double BUILT = 1.15;
    /** How fast how far is taken in, and let go, a second. */
    static final double RISE = 2;
    static final double FALL = 0.3;

    private double reach = GROUND;

    /** How far is seen from {@code height} over the ground. */
    public static double reach(final double height) {
        return Math.min(MOST, GROUND * Math.sqrt(1 + Math.max(0, height) / HIGHER));
    }

    /**
     * How much of the haze there is over ground {@code away} from the camera, seeing {@code reach}: none within
     * {@link #CLEAR} of it; then gently at first, thickening, all of it at {@link #CLEAR} and {@link #HAZED} of it.
     */
    public static double haze(final double away, final double reach) {
        final double t = Math.clamp((away - CLEAR * reach) / (HAZED * reach), 0, 1);
        return t * t * (3 - 2 * t);
    }

    /** How far is seen, {@code height} over the ground, {@code seconds} since last: smoothed. */
    public double at(final double height, final double seconds) {
        final double wanted = reach(height);
        reach = seconds <= 0 ? wanted : Num.ease(reach, wanted, wanted > reach ? RISE : FALL, seconds);
        return reach;
    }

    public double reach() {
        return reach;
    }
}
