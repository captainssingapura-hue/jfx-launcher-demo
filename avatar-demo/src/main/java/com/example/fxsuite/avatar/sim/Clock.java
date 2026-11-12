package com.example.fxsuite.avatar.sim;

/**
 * The simulation's own clock: it moves in steps of exactly {@link #STEP}, however long the
 * frames that drive it are. Each frame's time is added to what is owed, and as many whole steps
 * are taken as fit; what is left over waits for the next frame. So the world comes out the same
 * at 30 frames a second, at 60, at 144, or at an uneven mix of them: the same steps, in the same
 * size, with the same inputs.
 *
 * <p>What is left over is also how far the next step has got: {@link Ticked#blend}, for drawing
 * the world between the last two steps rather than jerking from one to the next.
 *
 * <p>A frame that runs very long (the window dragged, the machine busy) owes no more than
 * {@link #MOST} steps: the rest of that time is let go, and the world runs slow for a moment,
 * rather than trying to catch up and falling further behind each frame.
 *
 * @param owed seconds of time not yet stepped, less than a step
 */
public record Clock(double owed) {

    /** One step: 1/120 of a second, twice for each frame at 60. */
    public static final double STEP = 1.0 / 120;
    /** The most steps one frame may take: a tenth of a second's worth. */
    public static final int MOST = 12;

    public static final Clock START = new Clock(0);

    /**
     * What a frame does: the clock after it, how many steps to take, and how far on the step
     * after those is, 0 to 1, for blending.
     */
    public record Ticked(Clock clock, int steps, double blend) {
    }

    /** After a frame of {@code seconds}. */
    public Ticked advance(final double seconds) {
        double total = owed + Math.max(0, seconds);
        int steps = (int) Math.floor(total / STEP + 1e-9);
        if (steps > MOST) {
            steps = MOST;
            total = MOST * STEP;
        }
        final double left = Math.max(0, total - steps * STEP);
        return new Ticked(new Clock(left), steps, Math.min(1, left / STEP));
    }
}
