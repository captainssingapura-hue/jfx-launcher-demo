package com.example.fxsuite.avatar.sim;

import java.util.List;

/**
 * The player's inputs for one step. {@code forward} (back when negative), {@code sideways}
 * (right when positive) and {@code lift} (up when positive, used only while flying) run from -1
 * to 1, as keys give them; {@code run} speeds everything up. {@code turn} is how many degrees to
 * turn by during this step, right when positive, as the mouse gives it: the keys move the
 * avatar, the mouse turns it. {@code trigger} is held to fire, or to swing. {@code aim} is how far
 * up (down when negative) to shoot, in degrees, 0 level: the weapons always shoot straight ahead
 * of the body, tilted only by this. {@code bomb} is the other button, held: aboard an airship
 * with a gun, the trigger is the gun's, and this lets its bombs go.
 *
 * <p>{@code actions} are what the player did once, rather than holds (a jump, a take-off, the
 * pack, a weapon drawn), applied at the start of this step, in order, before it moves. So
 * nothing reaches the world between steps: everything the player does comes in with one.
 */
public record Controls(double forward, double sideways, double turn, double lift, boolean run, boolean trigger,
                       double aim, List<Action> actions, boolean bomb) {

    public static final Controls NONE = new Controls(0, 0, 0, 0, false, false, 0);

    public Controls {
        actions = List.copyOf(actions);
    }

    /** With the other button left alone. */
    public Controls(final double forward, final double sideways, final double turn, final double lift,
                    final boolean run, final boolean trigger, final double aim, final List<Action> actions) {
        this(forward, sideways, turn, lift, run, trigger, aim, actions, false);
    }

    /** The same, the other button held, or not. */
    public Controls bombing(final boolean held) {
        return new Controls(forward, sideways, turn, lift, run, trigger, aim, actions, held);
    }

    /** Held keys, the turn and the aim, with nothing done once. */
    public Controls(final double forward, final double sideways, final double turn, final double lift,
                    final boolean run, final boolean trigger, final double aim) {
        this(forward, sideways, turn, lift, run, trigger, aim, List.of());
    }

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

    /** Nothing held, just {@code actions} done. */
    public static Controls doing(final Action... actions) {
        return new Controls(0, 0, 0, 0, false, false, 0, List.of(actions));
    }

    /** The same held keys, turn and aim, with {@code actions} done as well. */
    public Controls with(final List<Action> done) {
        return new Controls(forward, sideways, turn, lift, run, trigger, aim, done);
    }
}
