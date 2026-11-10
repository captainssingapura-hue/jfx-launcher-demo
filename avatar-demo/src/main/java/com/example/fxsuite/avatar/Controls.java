package com.example.fxsuite.avatar;

/**
 * What the player is asking for this frame, each from -1 to 1: {@code forward} (back when
 * negative), {@code turn} (right when positive) and {@code lift} (up when positive, used only
 * while flying); {@code run} speeds everything up.
 */
public record Controls(double forward, double turn, double lift, boolean run) {

    public static final Controls NONE = new Controls(0, 0, 0, false);
}
