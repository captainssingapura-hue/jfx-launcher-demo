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
 * <p>Wearing the pack, it has wings ({@link Wings}), whose sweep it carries: easing, each step,
 * toward what the flight calls for. They collide as the body does.
 *
 * <p>It holds a weapon, or nothing ({@link Arms}): drawn by {@link #drawn}, fired or swung
 * while the trigger is held.
 *
 * <p>What the player does once (a jump, a take-off, the pack, a weapon drawn) comes in with a
 * step, as {@link Action}s among its {@link Controls}, and is done at the start of it: so the
 * avatar changes only at a step, never between two.
 *
 * <p>Events gather from each {@linkplain #step step}, until {@link #delivered} clears them: once
 * a frame, after the outputs have had them.
 */
public record Avatar(Body body, boolean wearingPack, double sweep, Arms arms, List<Event> events) {

    public Avatar {
        events = List.copyOf(events);
    }

    /** With the wings already swept as this state calls for. */
    public Avatar(final Body body, final boolean wearingPack, final List<Event> events) {
        this(body, wearingPack, Wings.sweepFor(wearingPack, body), Arms.EMPTY, events);
    }

    public Avatar(final Body body, final boolean wearingPack) {
        this(body, wearingPack, List.of());
    }

    /** The wings, if the pack is on; null if not. */
    public Wings wings() {
        return wearingPack ? new Wings(sweep) : null;
    }

    /**
     * One step: first what the player did once ({@link Controls#actions}), in order, then the
     * move, gathering what happened on the way: the body's events, and any gate gone through.
     * The wings swing toward the sweep the flight calls for first, and the body moves with them
     * as they now are.
     */
    public Avatar step(final Controls controls, final double seconds, final Obstacles world) {
        Avatar acted = this;
        for (final Action action : controls.actions()) {
            acted = acted.act(action);
        }
        return acted.moved(controls, seconds, world);
    }

    /** One thing done once: the same as the method it names, so whatever cannot be done is left undone. */
    public Avatar act(final Action action) {
        return switch (action) {
            case Action.Jump _ -> jumped();
            case Action.Fly _ -> toggledFlight();
            case Action.Pack _ -> toggledPack();
            case Action.Draw draw -> drawn(draw.weapon());
        };
    }

    private Avatar moved(final Controls controls, final double seconds, final Obstacles world) {
        final double swung = Num.ease(sweep, Wings.sweepFor(wearingPack, body), Wings.SWING, seconds);
        final Body.Moved moved = body.move(controls, seconds, world, wearingPack ? new Wings(swung) : null);
        final Body after = moved.body();
        final Arms.Stepped armed = arms.step(controls.trigger(), seconds, body, world, controls.aim());
        final List<Event> happened = new ArrayList<>(moved.events());
        happened.addAll(armed.events());
        for (final Gate gate : world.gates()) {
            if (gate.passedBy(body.position(), after.position(), Body.TALL)) {
                happened.add(new Event.PassedGate(
                        Math.sqrt(after.speed() * after.speed() + after.drift() * after.drift()
                                + after.climb() * after.climb())));
            }
        }
        return new Avatar(after, wearingPack, swung, armed.arms(), with(happened));
    }

    public Avatar jumped() {
        final Body jumped = body.jumped();
        return jumped == body ? this : new Avatar(jumped, wearingPack, sweep, arms, with(List.of(new Event.Jumped())));
    }

    /** Takes off, if wearing the pack, or stops flying. */
    public Avatar toggledFlight() {
        if (body.flying()) {
            return new Avatar(body.toggledFlight(), wearingPack, sweep, arms, with(List.of(new Event.FlightCut())));
        }
        return wearingPack ? new Avatar(body.toggledFlight(), true, sweep, arms, with(List.of(new Event.TookOff()))) : this;
    }

    /** Puts the pack on, or takes it off; off in flight, it falls. */
    public Avatar toggledPack() {
        if (!wearingPack) {
            return new Avatar(body, true, Wings.STOWED, arms, with(List.of(new Event.PackOn())));
        }
        final List<Event> happened = body.flying()
                ? List.of(new Event.FlightCut(), new Event.PackOff())
                : List.of(new Event.PackOff());
        return new Avatar(body.flying() ? body.toggledFlight() : body, false, Wings.STOWED, arms, with(happened));
    }

    /**
     * Draws a weapon into the hands, or, if it is already there, puts it away: the keys for the
     * weapons toggle them. What was held before is put away first.
     */
    public Avatar drawn(final Weapon weapon) {
        final Weapon now = arms.held() == weapon ? Weapon.NONE : weapon;
        return new Avatar(body, wearingPack, sweep, Arms.holding(now), with(List.of(new Event.Drew(now))));
    }

    /** What is in its hands. */
    public Weapon holding() {
        return arms.held();
    }

    /** How hard the jet-pack is firing, 0 to 1. */
    public double thrust() {
        return wearingPack ? body.thrust() : 0;
    }

    /**
     * This avatar as it is {@code share} of the way on from {@code before}: its body and wings
     * blended between them, for drawing between two steps of the {@link Clock}. All else (what it
     * holds, what has happened) is as it is now.
     */
    public Avatar between(final Avatar before, final double share) {
        if (share >= 1 || before == this) {
            return this;
        }
        final double s = Num.clamp(share, 0, 1);
        return new Avatar(body.between(before.body, s), wearingPack, before.sweep + (sweep - before.sweep) * s,
                arms, events);
    }

    /**
     * The same avatar, with more to report: what its blows did to the world ({@link Damage}),
     * heard and seen with the rest of this frame's events.
     */
    public Avatar reporting(final List<Event> more) {
        return more.isEmpty() ? this : new Avatar(body, wearingPack, sweep, arms, with(more));
    }

    /** The same avatar, its events handed on: what happens next starts a fresh list. */
    public Avatar delivered() {
        return events.isEmpty() ? this : new Avatar(body, wearingPack, sweep, arms, List.of());
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
