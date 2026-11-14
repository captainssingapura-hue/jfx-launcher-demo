package com.example.fxsuite.avatar.sim;

import java.util.ArrayList;
import java.util.List;

/**
 * The avatar as a whole: how it moves ({@link Body}), what it is wearing, and what it holds.
 * Pure state: the one truth the graphics and the sound both read, and neither writes.
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
 * <p>It holds a weapon, or nothing ({@link Arms}): drawn by an {@link Action}, fired or swung
 * while the trigger is held.
 *
 * <p>What the player does once (a jump, a take-off, the pack, a weapon drawn) comes in with a
 * step, as {@link Action}s among its {@link Controls}, and is done at the start of it: so the
 * avatar changes only at a step, never between two.
 *
 * <p>What happened ({@link Event}s) is not part of it. A step, or an action, gives the new state
 * and what happened as two things ({@link Stepped}): the state for the next step, the events for
 * whatever is outside to react to.
 */
public record Avatar(Body body, boolean wearingPack, double sweep, Arms arms) {

    /** With the wings already swept as this state calls for, and empty hands. */
    public Avatar(final Body body, final boolean wearingPack) {
        this(body, wearingPack, Wings.sweepFor(wearingPack, body), Arms.EMPTY);
    }

    /**
     * The avatar after a step or an action, what happened on the way, and the {@link Blow}s its
     * weapon dealt: intents, for the world to take within the same step ({@link Play}), never
     * outputs themselves.
     */
    public record Stepped(Avatar avatar, List<Event> events, List<Blow> blows) {

        public Stepped {
            events = List.copyOf(events);
            blows = List.copyOf(blows);
        }

        /** What came of it, and no blows dealt. */
        public Stepped(final Avatar avatar, final List<Event> events) {
            this(avatar, events, List.of());
        }

        /** Nothing happened, and nothing changed. */
        static Stepped still(final Avatar avatar) {
            return new Stepped(avatar, List.of());
        }

        /** {@code then}, after this: its avatar, and both lots of events and blows, these first. */
        Stepped then(final Stepped then) {
            if (events.isEmpty() && blows.isEmpty()) {
                return then;
            }
            final List<Event> happened = new ArrayList<>(events);
            happened.addAll(then.events);
            final List<Blow> dealt = new ArrayList<>(blows);
            dealt.addAll(then.blows);
            return new Stepped(then.avatar, happened, dealt);
        }
    }

    /** The wings, if the pack is on; null if not. */
    public Wings wings() {
        return wearingPack ? new Wings(sweep) : null;
    }

    /**
     * One step: first what the player did once ({@link Controls#actions}), in order, then the
     * move, gathering what happened on the way: the actions' events, the body's, the weapon's,
     * and any gate gone through. The wings swing toward the sweep the flight calls for first, and
     * the body moves with them as they now are.
     */
    public Stepped step(final Controls controls, final double seconds, final Obstacles world) {
        return step(controls, seconds, world, world);
    }

    /** As {@link #step(Controls, double, Obstacles)}, its weapons striking at {@code targets}: the world, and whoever is in it. */
    public Stepped step(final Controls controls, final double seconds, final Obstacles world, final Obstacles targets) {
        Stepped done = Stepped.still(this);
        for (final Action action : controls.actions()) {
            done = done.then(done.avatar().act(action));
        }
        return done.then(done.avatar().moved(controls, seconds, world, targets));
    }

    /** One thing done once, and what came of it. Whatever cannot be done is left undone, and nothing happens. */
    public Stepped act(final Action action) {
        return switch (action) {
            case Action.Jump _ -> {
                final Body jumped = body.jumped();
                yield jumped == body ? Stepped.still(this)
                        : new Stepped(new Avatar(jumped, wearingPack, sweep, arms), List.of(new Event.Jumped()));
            }
            case Action.Fly _ -> {
                if (body.flying()) {
                    yield new Stepped(new Avatar(body.toggledFlight(), wearingPack, sweep, arms),
                            List.of(new Event.FlightCut()));
                }
                yield wearingPack ? new Stepped(new Avatar(body.toggledFlight(), true, sweep, arms),
                        List.of(new Event.TookOff())) : Stepped.still(this);
            }
            case Action.Pack _ -> {
                if (!wearingPack) {
                    yield new Stepped(new Avatar(body, true, Wings.STOWED, arms), List.of(new Event.PackOn()));
                }
                // off in flight, it falls
                yield new Stepped(new Avatar(body.flying() ? body.toggledFlight() : body, false, Wings.STOWED, arms),
                        body.flying() ? List.of(new Event.FlightCut(), new Event.PackOff())
                                : List.of(new Event.PackOff()));
            }
            case Action.Draw draw -> {
                // the keys for the weapons toggle them; what was held before is put away first
                final Weapon now = arms.held() == draw.weapon() ? Weapon.NONE : draw.weapon();
                yield new Stepped(new Avatar(body, wearingPack, sweep, Arms.holding(now)), List.of(new Event.Drew(now)));
            }
            // the controls are the world's to hand over ({@link Play}), not the avatar's own
            case Action.Take _, Action.Gun _ -> Stepped.still(this);
        };
    }

    private Stepped moved(final Controls controls, final double seconds, final Obstacles world,
                          final Obstacles targets) {
        final double swung = Num.ease(sweep, Wings.sweepFor(wearingPack, body), Wings.SWING, seconds);
        final Body.Moved moved = body.move(controls, seconds, world, wearingPack ? new Wings(swung) : null);
        final Body after = moved.body();
        final Arms.Stepped armed = arms.step(controls.trigger(), seconds, body, targets, controls.aim());
        final List<Event> happened = new ArrayList<>(moved.events());
        happened.addAll(armed.events());
        for (final Gate gate : world.gates()) {
            if (gate.passedBy(body.position(), after.position(), Body.TALL)) {
                happened.add(new Event.PassedGate(
                        Math.sqrt(after.speed() * after.speed() + after.drift() * after.drift()
                                + after.climb() * after.climb())));
            }
        }
        return new Stepped(new Avatar(after, wearingPack, swung, armed.arms()), happened, armed.blows());
    }

    /** The same, {@code seconds} later: still, but its clock running on, for what keeps time by it. */
    public Avatar aged(final double seconds) {
        return new Avatar(body.aged(seconds), wearingPack, sweep, arms);
    }

    /** The same, turned to face {@code heading}, standing still: at a gun, facing the way it points. */
    public Avatar facing(final double heading) {
        return new Avatar(body.facing(heading), wearingPack, sweep, arms);
    }

    /** Carried by what it stands on, moved from {@code from} to {@code to}: see {@link Body#carried}. */
    public Avatar carried(final Place from, final Place to) {
        return new Avatar(body.carried(from, to), wearingPack, sweep, arms);
    }

    // The same actions as plain changes of state, for setting one up (in a test, say): what
    // happened on the way is let go. A running game does them as Actions, in a step.

    /** Jumps, if on its feet. */
    public Avatar jumped() {
        return act(Action.JUMP).avatar();
    }

    /** Takes off, if wearing the pack, or stops flying. */
    public Avatar toggledFlight() {
        return act(Action.FLY).avatar();
    }

    /** Puts the pack on, or takes it off; off in flight, it falls. */
    public Avatar toggledPack() {
        return act(Action.PACK).avatar();
    }

    /** Draws a weapon into the hands, or, if it is already there, puts it away. */
    public Avatar drawn(final Weapon weapon) {
        return act(new Action.Draw(weapon)).avatar();
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
     * blended between them, for drawing between two steps of the {@link Clock}. What it holds is
     * as it is now.
     */
    public Avatar between(final Avatar before, final double share) {
        if (share >= 1 || before == this) {
            return this;
        }
        final double s = Num.clamp(share, 0, 1);
        return new Avatar(body.between(before.body, s), wearingPack, before.sweep + (sweep - before.sweep) * s,
                arms);
    }
}
