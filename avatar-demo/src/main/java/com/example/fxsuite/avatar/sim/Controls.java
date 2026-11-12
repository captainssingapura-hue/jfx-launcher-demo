package com.example.fxsuite.avatar.sim;

/**
 * What the player is asking for this frame. {@code forward} (back when negative),
 * {@code sideways} (right when positive) and {@code lift} (up when positive, used only while
 * flying) run from -1 to 1, as keys give them; {@code run} speeds everything up. {@code turn}
 * is how many degrees to turn by during this frame, right when positive, as the mouse gives it:
 * the keys move the avatar, the mouse turns it. {@code trigger} is held to fire, or to swing.
 * {@code aim} is how far up (down when negative) to shoot, in degrees, 0 level: the weapons
 * always shoot straight ahead of the body, tilted only by this.
 */
public record Controls(double forward, double sideways, double turn, double lift, boolean run, boolean trigger,
                       double aim) {

    public static final Controls NONE = new Controls(0, 0, 0, 0, false, false, 0);

    /** Moving, with the trigger left alone. */
    public Controls(final double forward, final double sideways, final double turn, final double lift,
                    final boolean run) {
        this(forward, sideways, turn, lift, run, false, 0);
    }

    /** Moving, and firing or not, level. */
    public Controls(final double forward, final double sideways, final double turn, final double lift,
                    final boolean run, final boolean trigger) {
        this(forward, sideways, turn, lift, run, trigger, 0);
    }
}
