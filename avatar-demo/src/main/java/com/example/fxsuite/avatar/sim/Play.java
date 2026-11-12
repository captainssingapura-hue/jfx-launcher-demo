package com.example.fxsuite.avatar.sim;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything that moves: the avatar, and what its weapons have done to the world, stepped
 * together. Pure state; what happened comes out of each step beside it.
 *
 * <p>A step goes in four phases:
 * <ol>
 *   <li><b>Inputs:</b> what the player did once ({@link Controls#actions}) is done, in order;
 *       the held keys, the turn and the aim are set.</li>
 *   <li><b>Act:</b> the avatar moves on among what still stands, and its weapon deals its
 *       {@link Blow}s: intents, not yet anything that happened.</li>
 *   <li><b>Resolve:</b> the world takes the blows, in the order they were dealt ({@link Damage}).</li>
 *   <li><b>Report:</b> what happened goes out: the avatar's own events, then what came of each
 *       blow (struck, with what the piece has left; what broke).</li>
 * </ol>
 * The blows never leave the step: they follow from the state and the inputs, and only what came
 * of them is an output.
 */
public record Play(Avatar avatar, Damage damage) {

    /** The world after a step, and what happened in it. */
    public record Stepped(Play play, List<Event> events) {

        public Stepped {
            events = List.copyOf(events);
        }
    }

    /** One step of {@code seconds}, with {@code controls}. */
    public Stepped step(final Controls controls, final double seconds) {
        // inputs, and act: the avatar does what it was asked, moves, and deals its blows
        final Avatar.Stepped acted = avatar.step(controls, seconds, damage.obstacles());
        // resolve: the world takes the blows
        final Damage.Taken taken = damage.took(acted.blows());
        // report: the avatar's own events, then what came of the blows
        final List<Event> happened = new ArrayList<>(acted.events());
        happened.addAll(taken.events());
        return new Stepped(new Play(acted.avatar(), taken.damage()), happened);
    }
}
