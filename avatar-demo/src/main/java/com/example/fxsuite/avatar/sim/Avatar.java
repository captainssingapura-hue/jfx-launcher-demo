package com.example.fxsuite.avatar.sim;

import java.util.ArrayList;
import java.util.List;

/**
 * The avatar as a whole: how it moves ({@link Body}), what it is wearing, and what has happened
 * to it since the outputs last looked ({@link Event}s). Pure: the one truth the graphics and
 * the sound both read, and neither writes.
 *
 * <p>For now it wears just the jet-pack, and the one rule that brings: only with the pack on can
 * it fly.
 * <ul>
 *   <li>Without the pack, taking off does nothing.</li>
 *   <li>Taking the pack off in flight stops the flight, and it falls.</li>
 *   <li>Putting it on, or taking it off on the ground or mid-jump, changes nothing else.</li>
 * </ul>
 *
 * <p>Events gather from the actions (a jump, a take-off, the pack) and from each
 * {@linkplain #step step}, until {@link #delivered} clears them: once a frame, after the outputs
 * have had them.
 */
public record Avatar(Body body, boolean wearingPack, List<Event> events) {

    public Avatar {
        events = List.copyOf(events);
    }

    public Avatar(final Body body, final boolean wearingPack) {
        this(body, wearingPack, List.of());
    }

    /** Moves on, gathering what happened on the way: the body's events, and any gate gone through. */
    public Avatar step(final Controls controls, final double seconds, final Obstacles world) {
        final Body.Moved moved = body.move(controls, seconds, world);
        final Body after = moved.body();
        final List<Event> happened = new ArrayList<>(moved.events());
        for (final Gate gate : world.gates()) {
            if (gate.passedBy(body.position(), after.position(), Body.TALL)) {
                happened.add(new Event.PassedGate(
                        Math.sqrt(after.speed() * after.speed() + after.drift() * after.drift()
                                + after.climb() * after.climb())));
            }
        }
        return new Avatar(after, wearingPack, with(happened));
    }

    public Avatar jumped() {
        final Body jumped = body.jumped();
        return jumped == body ? this : new Avatar(jumped, wearingPack, with(List.of(new Event.Jumped())));
    }

    /** Takes off, if wearing the pack, or stops flying. */
    public Avatar toggledFlight() {
        if (body.flying()) {
            return new Avatar(body.toggledFlight(), wearingPack, with(List.of(new Event.FlightCut())));
        }
        return wearingPack ? new Avatar(body.toggledFlight(), true, with(List.of(new Event.TookOff()))) : this;
    }

    /** Puts the pack on, or takes it off; off in flight, it falls. */
    public Avatar toggledPack() {
        if (!wearingPack) {
            return new Avatar(body, true, with(List.of(new Event.PackOn())));
        }
        final List<Event> happened = body.flying()
                ? List.of(new Event.FlightCut(), new Event.PackOff())
                : List.of(new Event.PackOff());
        return new Avatar(body.flying() ? body.toggledFlight() : body, false, with(happened));
    }

    /** How hard the jet-pack is firing, 0 to 1. */
    public double thrust() {
        return wearingPack ? body.thrust() : 0;
    }

    /** The same avatar, its events handed on: what happens next starts a fresh list. */
    public Avatar delivered() {
        return events.isEmpty() ? this : new Avatar(body, wearingPack, List.of());
    }

    private List<Event> with(final List<Event> more) {
        if (more.isEmpty()) {
            return events;
        }
        final List<Event> all = new ArrayList<>(events);
        all.addAll(more);
        return all;
    }
}
