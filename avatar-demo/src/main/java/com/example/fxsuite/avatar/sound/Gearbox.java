package com.example.fxsuite.avatar.sound;

/**
 * The gearbox of a {@link Vehicle} driven by something else, the game's own driving: given how
 * fast it goes and the throttle, it says which gear it is in and how fast the vehicle's engine
 * turns ({@link Geared}). Its revs are worked out as the engine bench's are, from the same gears,
 * final drive and wheels ({@link Vehicle#rpm}): at any speed, in any gear, the same.
 *
 * <p>It changes gear by itself, as an automatic does: up near the red line pulling hard, up early
 * cruising, down as it slows, down a gear or two flooring it; a change cuts the throttle for the
 * vehicle's {@link Vehicle#shift} (blipping it, going down). Pulling away, the clutch slips;
 * stopped, it idles; going back, it is in its first gear. A scooter's CVT holds the engine at the
 * revs the throttle asks for as the speed builds, as on the bench ({@link Driving}).
 *
 * @param vehicle what it is the gearbox of
 */
public record Gearbox(Vehicle vehicle) {

    /** Left to itself, it changes up this near the red line; down this far up the range. */
    private static final double UP = 0.92;
    private static final double DOWN = 0.1;
    /** Cruising (the throttle under this), up as soon as the next gear keeps it this far up the range. */
    private static final double CRUISE = 0.5;
    private static final double CRUISE_REVS = 0.15;
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
    /** A blip of the throttle, changing down. */
    private static final double BLIP = 0.6;

    /**
     * Which gear it is in (0 for a CVT), how fast the engine turns, what the clutch is doing, and
     * the throttle the cylinders get for its sound ({@code firing}): none while changing up, a
     * blip going down.
     */
    public record Geared(int gear, double rpm, double shifting, int next, Driving.Clutch clutch, double firing,
                         boolean back) {

        /** The gear as a dashboard shows it: R going back, D a CVT, else the number, the one coming if changing. */
        public String shown() {
            return back ? "R" : gear == 0 ? "D" : Integer.toString(shifting > 0 ? next : gear);
        }
    }

    /** Whether it is a CVT, a scooter's. */
    public boolean continuous() {
        return vehicle.cvt() > 0;
    }

    /** How many gears it has; none, a CVT. */
    public int count() {
        return vehicle.count();
    }

    /** Stopped, in first (or a CVT's drive), idling. */
    public Geared stopped() {
        final int first = continuous() ? 0 : 1;
        return new Geared(first, vehicle.engine().idle(), 0, first, Driving.Clutch.OUT, 0, false);
    }

    /**
     * {@code seconds} on from {@code was}: going {@code speed} metres a second (backwards, if
     * {@code back}), the throttle open {@code throttle}.
     */
    public Geared step(final Geared was, final double speed, final double throttle, final boolean back,
                       final double seconds) {
        final Engine engine = vehicle.engine();
        final double open = Math.clamp(throttle, 0, 1);
        final double going = Math.abs(speed);
        if (continuous()) {
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
            if (gear < count() && (rpm >= UP * engine.redline() && open > 0.3
                    || open < CRUISE && vehicle.rpm(gear + 1, going) > engine.idle() + CRUISE_REVS * range)) {
                wanted = 1;
            } else if (gear > 1 && (rpm < engine.idle() + DOWN * range
                    && vehicle.rpm(gear - 1, going) < 0.85 * engine.redline()
                    || open > FLOOR && vehicle.rpm(gear - 1, going) < KICK * engine.redline()
                    && rpm < KICKED * engine.redline())) {
                wanted = -1;
            }
            if (wanted != 0) {
                next = gear + wanted;
                shifting = vehicle.shift();
            }
        }
        if (shifting > 0) {
            // the clutch out, the throttle off, or blipped going down: the engine on its own
            final double blip = next < gear ? BLIP : 0;
            final double rpm = new Revs(was.rpm(), 0, 0).step(engine, blip, seconds).rpm();
            return new Geared(gear, rpm, shifting, next, Driving.Clutch.SHIFTING, blip, back);
        }
        final double coupled = vehicle.rpm(gear, going);
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

    /**
     * A CVT's step, as on the bench: the engine held at the revs the throttle asks for, as far as
     * its ratios go; too slow yet for even its lowest to hold them, its clutch slips and holds them.
     */
    private Geared continuous(final Engine engine, final Geared was, final double going, final double open,
                              final boolean back, final double seconds) {
        final double spin = going / vehicle.wheel() * 60 / (2 * Math.PI);
        final double held = engine.idle() + (vehicle.cvt() - engine.idle()) * open;
        final double highest = spin * vehicle.highest();
        final double lowest = spin * vehicle.lowest();
        final double coupled = Math.clamp(held, highest, Math.max(highest, lowest));
        final double engage = 1.5 * engine.idle();
        final double rpm;
        final Driving.Clutch clutch;
        if (coupled >= engage && !(open > 0.02 && lowest < held)) {
            clutch = Driving.Clutch.IN;
            rpm = was.rpm() + (coupled - was.rpm()) * Math.min(1, 8 * seconds);
        } else if (open > 0.02) {
            clutch = Driving.Clutch.SLIPPING;
            rpm = was.rpm() + (held - was.rpm()) * Math.min(1, 6 * seconds);
        } else {
            clutch = Driving.Clutch.OUT;
            rpm = new Revs(was.rpm(), 0, 0).step(engine, 0, seconds).rpm();
        }
        return new Geared(0, Math.min(rpm, engine.redline() * 1.02), 0, 0, clutch, open, back);
    }
}
