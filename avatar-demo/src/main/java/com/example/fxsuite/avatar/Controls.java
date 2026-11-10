package com.example.fxsuite.avatar;

/**
 * What the player is asking for this frame. {@code forward} (back when negative),
 * {@code sideways} (right when positive) and {@code lift} (up when positive, used only while
 * flying) run from -1 to 1, as keys give them; {@code run} speeds everything up. {@code turn}
 * is how many degrees to turn by during this frame, right when positive, as the mouse gives it:
 * the keys move the avatar, the mouse turns it.
 */
public record Controls(double forward, double sideways, double turn, double lift, boolean run) {

    public static final Controls NONE = new Controls(0, 0, 0, 0, false);
}
