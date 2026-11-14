package com.example.fxsuite.avatar.sim;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * What the keys mean to the world, by their names ({@code W}, {@code SPACE}, {@code DIGIT1}...):
 * the same for a player at the keyboard and for a {@link Script}, so the two cannot drift apart.
 * Keys that mean nothing to the world (the camera's, the sound's) are simply not in it.
 *
 * <p>W/S go ahead and back, A/D sideways, Space climbs and Ctrl descends while flying; the
 * avatar goes at full speed unless Shift is held. Pressed, Space jumps, F takes off or lands,
 * J puts the pack on or off, T takes the controls of a platform or lets go of them, and 1, 2 and
 * 3 draw the rifles, the glaive and the Gatling gun.
 */
public final class Keys {

    private Keys() {
    }

    /** What pressing {@code key} does once, if anything. */
    public static Optional<Action> action(final String key) {
        return Optional.ofNullable(switch (key) {
            case "SPACE" -> Action.JUMP;
            case "F" -> Action.FLY;
            case "J" -> Action.PACK;
            case "T" -> Action.TAKE;
            case "G" -> Action.GUN;
            case "DIGIT1" -> new Action.Draw(Weapon.RIFLES);
            case "DIGIT2" -> new Action.Draw(Weapon.GLAIVE);
            case "DIGIT3" -> new Action.Draw(Weapon.GATLING);
            default -> null;
        });
    }

    /**
     * A step's inputs: the keys {@code held}, those {@code pressed} since the last step (in the
     * order they were pressed), the step's {@code turn} in degrees, the trigger, and the aim.
     */
    public static Controls controls(final Set<String> held, final List<String> pressed, final double turn,
                                    final boolean trigger, final double aim) {
        return controls(held, pressed, turn, trigger, aim, false);
    }

    /** As {@link #controls(Set, List, double, boolean, double)}, the other button held ({@code bomb}), or not. */
    public static Controls controls(final Set<String> held, final List<String> pressed, final double turn,
                                    final boolean trigger, final double aim, final boolean bomb) {
        return new Controls(
                axis(held, "W") - axis(held, "S"),
                axis(held, "D") - axis(held, "A"),
                turn,
                axis(held, "SPACE") - axis(held, "CONTROL"),
                // full speed by default, as in most shooters; Shift held to go carefully
                !held.contains("SHIFT"),
                trigger,
                aim,
                pressed.stream().map(Keys::action).flatMap(Optional::stream).toList(),
                bomb);
    }

    private static double axis(final Set<String> held, final String key) {
        return held.contains(key) ? 1 : 0;
    }
}
