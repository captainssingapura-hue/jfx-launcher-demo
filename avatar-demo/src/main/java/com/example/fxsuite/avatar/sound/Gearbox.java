package com.example.fxsuite.avatar.sound;

/**
 * A gearbox that follows a vehicle driven by something else, the game's own driving: given how
 * fast it goes, as a share of its top speed, and the throttle, it says which gear it is in and
 * how fast its {@link Engine} turns ({@link Geared}). It changes gear by itself, as an automatic
 * does: up near the red line pulling hard, up early cruising, down as it slows, down a gear or two
 * flooring it; a change cuts the throttle for {@link #shift} seconds (blipping it, going down).
 * Pulling away, the clutch slips; stopped, it idles; going back, it is in its first gear.
 *
 * <p>Its {@code count} gears are spaced evenly in ratio, from a first that runs out of revs at
 * {@code first} of the top speed, up to a top that turns the engine at {@code top} of its red line
 * at the top speed: as cars have them, a little short of the red line flat out in top.
 *
 * @param count how many gears
 * @param first the share of the top speed the first gear takes it to, at the red line
 * @param top   the share of the red line the top gear turns the engine at, at the top speed
 * @param shift how long a change takes, in seconds
 */
public record Gearbox(int count, double first, double top, double shift) {

    /** Left to itself, it changes up this near the red line; down this far up the range. */
    private static final double UP = 0.92;
    private static final double DOWN = 0.2;
    /** Cruising (the throttle under this), up as soon as the next gear keeps it this far up the range. */
    private static final double CRUISE = 0.5;
    private static final double CRUISE_REVS = 0.35;
    /** Flooring it (the throttle past this), down a gear if that one keeps under this share of the red line. */
    private static final double FLOOR = 0.9;
    private static final double KICK = 0.9;
    /** And only when the engine is under this share of the red line: not straight after a change up. */
    private static final double KICKED = 0.65;
    /** Pulling away flat out, the engine revs this far up its range on the slipping clutch. */
    private static final double LAUNCH = 0.3;
    /** The clutch bites once the engine is within this of the wheels; brought down to them this fast. */
    private static final double CATCH = 60;
    private static final double BITE = 6000;
    /** A CVT's lowest ratio turns the engine this many times faster than its highest. */
    private static final double SPREAD = 3;
    /** A blip of the throttle, changing down. */
    private static final double BLIP = 0.6;

    public Gearbox {
        if (count == 0 ? first <= 0 || first > 1 || top <= 0 || top > 1
                : count < 1 || first <= 0 || first >= 1 / top || top <= 0 || top > 1 || shift < 0) {
            throw new IllegalArgumentException("gears from a short first up to a top that reaches the top speed");
        }
    }

    /**
     * A CVT, as a scooter has: no gears, but a ratio that changes as it goes, holding the engine at
     * the revs the throttle asks for, {@code hold} of its red line flat out, as the speed builds,
     * until it has no higher ratio to go to, where it turns the engine at {@code top} of its red
     * line at the top speed. Under half again its idle, its clutch lets go.
     */
    public static Gearbox cvt(final double hold, final double top) {
        return new Gearbox(0, hold, top, 0);
    }

    /** Whether it is a CVT ({@link #cvt}). */
    public boolean continuous() {
        return count == 0;
    }

    /**
     * Which gear it is in, how fast the engine turns, what the clutch is doing, and the throttle
     * the cylinders get for its sound ({@code firing}): none while changing up, a blip going down.
     */
    public record Geared(int gear, double rpm, double shifting, int next, Driving.Clutch clutch, double firing,
                         boolean back) {

        /** The gear as a dashboard shows it: R going back, else the number, the one coming if changing. */
        public String shown() {
            return back ? "R" : gear == 0 ? "D" : Integer.toString(shifting > 0 ? next : gear);
        }
    }

    /** Stopped, in first, idling. */
    public Geared stopped(final Engine engine) {
        return new Geared(count == 0 ? 0 : 1, engine.idle(), 0, count == 0 ? 0 : 1, Driving.Clutch.OUT, 0, false);
    }

    /** The share of the top speed gear {@code gear} (from 1) takes it to at the red line. */
    public double reach(final int gear) {
        if (count == 1) {
            return 1 / top;
        }
        return first * Math.pow(1 / top / first, (gear - 1.0) / (count - 1));
    }

    /** How fast the engine turns in gear {@code gear}, going at {@code share} of the top speed. */
    public double rpm(final Engine engine, final int gear, final double share) {
        return engine.redline() * share / reach(gear);
    }

    /** A CVT's step: the engine held at the revs the throttle asks for, as far as its ratios go. */
    private Geared continuous(final Engine engine, final Geared was, final double going, final double open,
                              final boolean back, final double seconds) {
        final double held = engine.idle() + (first * engine.redline() - engine.idle()) * open;
        final double highest = top * engine.redline() * going;
        final double lowest = highest * SPREAD;
        final double coupled = Math.clamp(held, highest, Math.max(highest, lowest));
        final double engage = 1.5 * engine.idle();
        final double rpm;
        final Driving.Clutch clutch;
        if (coupled >= engage && !(open > 0.02 && lowest < held)) {
            clutch = Driving.Clutch.IN;
            rpm = was.rpm() + (coupled - was.rpm()) * Math.min(1, 8 * seconds);
        } else if (open > 0.02) {
            // pulling away, too slow yet for even its lowest ratio to hold the revs asked for: its
            // clutch slipping, the engine up at them
            clutch = Driving.Clutch.SLIPPING;
            rpm = was.rpm() + (held - was.rpm()) * Math.min(1, 6 * seconds);
        } else {
            clutch = Driving.Clutch.OUT;
            rpm = new Revs(was.rpm(), 0, 0).step(engine, 0, seconds).rpm();
        }
        return new Geared(0, Math.min(rpm, engine.redline() * 1.02), 0, 0, clutch, open, back);
    }

    /**
     * {@code seconds} on from {@code was}: going at {@code share} of the top speed (backwards, if
     * {@code back}), the throttle open {@code throttle}.
     */
    public Geared step(final Engine engine, final Geared was, final double share, final double throttle,
                       final boolean back, final double seconds) {
        final double open = Math.clamp(throttle, 0, 1);
        final double going = Math.abs(share);
        if (count == 0) {
            return continuous(engine, was, going, open, back, seconds);
        }
        final double range = engine.redline() - engine.idle();
        int gear = back ? 1 : was.gear();
        int next = was.next();
        double shifting = back ? 0 : was.shifting();
        if (shifting > 0) {
            shifting -= seconds;
            if (shifting <= 0) {
                shifting = 0;
                gear = next;
            }
        } else if (!back && was.clutch() == Driving.Clutch.IN) {
            final double rpm = was.rpm();
            int wanted = 0;
            if (gear < count && (rpm >= UP * engine.redline() && open > 0.3
                    || open < CRUISE && rpm(engine, gear + 1, going) > engine.idle() + CRUISE_REVS * range)) {
                wanted = 1;
            } else if (gear > 1 && (rpm < engine.idle() + DOWN * range
                    && rpm(engine, gear - 1, going) < 0.85 * engine.redline()
                    || open > FLOOR && rpm(engine, gear - 1, going) < KICK * engine.redline()
                    && rpm < KICKED * engine.redline())) {
                wanted = -1;
            }
            if (wanted != 0) {
                next = gear + wanted;
                shifting = shift;
            }
        }
        if (shifting > 0) {
            // the clutch out, the throttle off, or blipped going down: the engine on its own
            final double blip = next < gear ? BLIP : 0;
            final double rpm = new Revs(was.rpm(), 0, 0).step(engine, blip, seconds).rpm();
            return new Geared(gear, rpm, shifting, next, Driving.Clutch.SHIFTING, blip, back);
        }
        final double coupled = rpm(engine, gear, going);
        final double rpm;
        final Driving.Clutch clutch;
        if (coupled >= engine.idle()
                && (was.clutch() == Driving.Clutch.IN || coupled >= was.rpm() - CATCH)) {
            clutch = Driving.Clutch.IN;
            rpm = Math.min(coupled, engine.redline() * 1.02);
        } else if (coupled >= engine.idle()) {
            // a new gear taken, moving: the engine brought down to the wheels as the clutch bites
            clutch = Driving.Clutch.SLIPPING;
            rpm = Math.max(coupled, was.rpm() - BITE * seconds);
        } else if (open > 0.02) {
            // pulling away: up to its launch, on the slipping clutch
            clutch = Driving.Clutch.SLIPPING;
            final double launch = engine.idle() + LAUNCH * open * range;
            rpm = was.rpm() + (launch - was.rpm()) * Math.min(1, 6 * seconds);
        } else {
            clutch = Driving.Clutch.OUT;
            rpm = new Revs(was.rpm(), 0, 0).step(engine, 0, seconds).rpm();
        }
        return new Geared(gear, rpm, 0, gear, clutch, open, back);
    }
}
