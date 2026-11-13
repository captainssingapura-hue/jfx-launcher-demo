package com.example.fxsuite.avatar.sound;

/**
 * An engine revving free, out of gear: the throttle pulls the revs up as briskly as the engine
 * goes ({@link Engine#climb}, easing off as it nears the top), off the throttle they fall back
 * ({@link Engine#fall}) to the idle, which it holds. At the red line the limiter cuts the fuel
 * for a moment, so held flat out it bounces off it, as engines do.
 *
 * @param rpm    how fast it turns
 * @param cut    how much longer the limiter has the fuel cut, in seconds
 * @param firing the throttle the cylinders actually get: none while the limiter cuts
 */
public record Revs(double rpm, double cut, double firing) {

    /** How long the limiter cuts the fuel each time it is hit. */
    public static final double CUT = 0.07;

    /** At its idle, the throttle shut. */
    public static Revs idling(final Engine engine) {
        return new Revs(engine.idle(), 0, 0);
    }

    /** {@code seconds} on, with the throttle open {@code throttle}, 0 to 1. */
    public Revs step(final Engine engine, final double throttle, final double seconds) {
        double cutting = Math.max(0, cut - seconds);
        final double open = cutting > 0 ? 0 : Math.clamp(throttle, 0, 1);
        // up with the throttle, harder the more of it, easing off near the top; down without it
        final double room = Math.clamp((engine.redline() * 1.05 - rpm) / engine.redline(), 0, 1);
        final double rise = open * engine.climb() * (0.35 + 0.65 * Math.sqrt(room));
        final double drop = (1 - open) * engine.fall() * (0.3 + 0.7 * rpm / engine.redline());
        double next = rpm + (rise - drop) * seconds;
        // the idle holds it up: coming down, it settles there; started below it, it is brought up to it
        if (next < engine.idle()) {
            next = rpm < engine.idle() ? rpm + (engine.idle() - rpm) * Math.min(1, 8 * seconds) : engine.idle();
        }
        // and the limiter holds it down
        if (next >= engine.redline()) {
            next = engine.redline();
            cutting = CUT;
        }
        return new Revs(next, cutting, cutting > 0 ? 0 : open);
    }
}
