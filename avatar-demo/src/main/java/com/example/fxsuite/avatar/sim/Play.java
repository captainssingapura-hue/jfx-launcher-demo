package com.example.fxsuite.avatar.sim;

import java.util.List;

/**
 * Everything that moves: the avatar, and what its weapons have done to the world, stepped
 * together. Each step moves the avatar among what still stands, then lets that step's blows
 * wear the world down; what they break is reported with the step's other events. Pure.
 */
public record Play(Avatar avatar, Damage damage) {

    /** One step of {@code seconds}, with {@code controls}. */
    public Play step(final Controls controls, final double seconds) {
        final int before = avatar.events().size();
        final Avatar moved = avatar.step(controls, seconds, damage.obstacles());
        // only this step's blows: the ones before it have been taken already
        final List<Event> now = moved.events().subList(before, moved.events().size());
        final Damage.Taken taken = damage.took(now);
        return new Play(moved.reporting(taken.events()), taken.damage());
    }

    /** The same world, the avatar changed between steps: by a key pressed, say. */
    public Play with(final Avatar changed) {
        return new Play(changed, damage);
    }
}
