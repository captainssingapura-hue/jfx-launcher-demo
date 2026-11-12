package com.example.fxsuite.avatar.sim;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything that moves: the avatar, and what its weapons have done to the world, stepped
 * together. Each step moves the avatar among what still stands, then lets that step's blows
 * wear the world down. Pure state; what happened comes out of each step beside it.
 */
public record Play(Avatar avatar, Damage damage) {

    /** The world after a step, and what happened in it: the avatar's events, then what broke. */
    public record Stepped(Play play, List<Event> events) {

        public Stepped {
            events = List.copyOf(events);
        }
    }

    /** One step of {@code seconds}, with {@code controls}. */
    public Stepped step(final Controls controls, final double seconds) {
        final Avatar.Stepped moved = avatar.step(controls, seconds, damage.obstacles());
        final Damage.Taken taken = damage.took(moved.events());
        if (taken.events().isEmpty()) {
            return new Stepped(new Play(moved.avatar(), taken.damage()), moved.events());
        }
        final List<Event> all = new ArrayList<>(moved.events());
        all.addAll(taken.events());
        return new Stepped(new Play(moved.avatar(), taken.damage()), all);
    }
}
