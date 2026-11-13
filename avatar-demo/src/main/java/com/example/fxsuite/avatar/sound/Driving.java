package com.example.fxsuite.avatar.sound;

/**
 * A {@link Vehicle} being driven, as far as its engine's sound needs: how fast it goes, in which
 * gear, how fast the engine turns, and how hard it works. A step at a time, from the throttle,
 * the brake and the gear lever.
 *
 * <ul>
 *   <li><b>In gear</b>, the clutch in, the engine turns with the wheels: its revs are the road
 *       speed through the gear. It pulls as hard as its torque at those revs and the throttle say;
 *       off the throttle it holds the vehicle back, and pops.</li>
 *   <li><b>Pulling away</b>, the wheels too slow for the engine to idle in gear, the clutch slips:
 *       the engine revs up to its launch and pulls through the slipping clutch until the wheels
 *       catch up. Stopped, the throttle off, the clutch is out and it idles.</li>
 *   <li><b>Changing gear</b> takes the vehicle's {@link Vehicle#shift}: the clutch out, the
 *       throttle off (blipped, going down), the engine falling free; then the new gear takes it.
 *       Up at the top of the revs, as the driver would, or down as it slows, if the gearbox is
 *       left to it; or at the driver's word.</li>
 *   <li><b>A CVT</b>, a scooter's, has no gears: it holds the engine at the revs the throttle
 *       asks for ({@link Vehicle#cvt} flat out) while the speed builds, until it runs out of
 *       ratio and the revs rise with the speed. Below its revs, its clutch lets go.</li>
 *   <li>A charged engine pulls harder the more boost it makes: a turbocharger's late, after a
 *       lag; a supercharger's at once.</li>
 *   <li>At the red line the limiter cuts the fuel, in gear as out of it.</li>
 * </ul>
 *
 * @param speed    how fast it goes, in metres a second
 * @param gear     which gear: 1 the first; 0 for a CVT
 * @param rpm      how fast the engine turns
 * @param shifting how much longer the change of gear under way takes, in seconds; 0 none
 * @param next     the gear being changed into
 * @param cut      how much longer the limiter cuts the fuel
 * @param firing   the throttle the cylinders get, for its sound: none while changing or cut
 * @param load     how hard the engine works, for its sound: 0 free, 1 pulling flat out
 * @param clutch   what the clutch is doing
 * @param boost    how much boost its charger makes, 0 to 1; none without one ({@link Charger#spool})
 */
public record Driving(double speed, int gear, double rpm, double shifting, int next, double cut, double firing,
                      double load, Clutch clutch, double boost) {

    /** What the clutch is doing. */
    public enum Clutch {
        /** Out: the engine on its own, the vehicle rolling or stopped. */
        OUT,
        /** Slipping: pulling away, the engine faster than the wheels in gear. */
        SLIPPING,
        /** In: the engine turning with the wheels. */
        IN,
        /** Out for a change of gear. */
        SHIFTING
    }

    private static final double G = 9.81;
    private static final double AIR = 1.2;
    /** How hard the tyres roll, against the vehicle's weight. */
    private static final double ROLLING = 0.015;
    /** How much of the engine's pull reaches the road. */
    private static final double EFFICIENCY = 0.9;
    /** The spinning parts make it heavier to speed up by this much. */
    private static final double SPINNING = 1.08;
    /** Braking flat out, it slows this hard, in metres a second each second. */
    private static final double BRAKING = 8;
    /** Off the throttle, the engine holds back with this much of its torque, more at higher revs. */
    private static final double ENGINE_BRAKE = 0.12;
    /** Pulling away flat out, the engine revs this far up its range on the slipping clutch. */
    private static final double LAUNCH = 0.3;
    /** The clutch bites, its slipping done, once the engine is within this of the wheels in gear. */
    private static final double CATCH = 60;
    /** And it is brought down to them this fast, in turns a minute each second. */
    private static final double BITE = 6000;
    /** Left to itself, it changes up this near the red line, and down this far up the range. */
    private static final double UP = 0.94;
    private static final double DOWN = 0.2;
    /**
     * Cruising (the throttle under this), it changes up as soon as the next gear would keep the
     * engine this far up its range: well clear of {@link #DOWN}, so it does not change straight back.
     */
    private static final double CRUISE = 0.5;
    private static final double CRUISE_REVS = 0.35;
    /** A blip of the throttle, changing down. */
    private static final double BLIP = 0.6;

    /** Stopped, in first (or a CVT's drive), idling. */
    public static Driving stopped(final Vehicle vehicle) {
        return new Driving(0, vehicle.cvt() > 0 ? 0 : 1, vehicle.engine().idle(), 0, 0, 0, 0, 0, Clutch.OUT, 0);
    }

    /** The speed, in kilometres an hour. */
    public double kmh() {
        return speed * 3.6;
    }

    /**
     * {@code seconds} on: the throttle open {@code throttle} and the brake on {@code brake}, each
     * 0 to 1; {@code lever} 1 to change up, -1 down, 0 neither; and the gearbox changing gear by
     * itself or not ({@code auto}).
     */
    public Driving step(final Vehicle vehicle, final double throttle, final double brake, final int lever,
                        final boolean auto, final double seconds) {
        final Engine engine = vehicle.engine();
        double cutting = Math.max(0, cut - seconds);
        final double open = cutting > 0 ? 0 : Math.clamp(throttle, 0, 1);
        final double spin = speed / vehicle.wheel() * 60 / (2 * Math.PI);    // the wheels, turns a minute
        int now = gear;
        int into = next;
        double changing = shifting;
        // a change of gear under way, finishing; or one asked for, by the driver or the gearbox
        if (vehicle.cvt() <= 0) {
            if (changing > 0) {
                changing -= seconds;
                if (changing <= 0) {
                    changing = 0;
                    now = into;
                }
            } else {
                int wanted = lever;
                if (auto && wanted == 0 && clutch == Clutch.IN) {
                    final double range = engine.redline() - engine.idle();
                    final boolean top = now < vehicle.count() && rpm >= UP * engine.redline() && open > 0.3;
                    // cruising, it goes up as soon as the next gear keeps the engine well off its idle
                    final boolean cruising = now < vehicle.count() && open < CRUISE
                            && spin * vehicle.ratio(now + 1) > engine.idle() + CRUISE_REVS * range;
                    if (top || cruising) {
                        wanted = 1;
                    } else if (now > 1 && rpm < engine.idle() + DOWN * (engine.redline() - engine.idle())
                            && spin * vehicle.ratio(now - 1) < 0.85 * engine.redline()) {
                        wanted = -1;
                    }
                }
                final int target = now + Integer.signum(wanted);
                if (wanted != 0 && target >= 1 && target <= vehicle.count()) {
                    changing = vehicle.shift();
                    into = target;
                }
            }
        }
        double turning;
        double ratio = 0;
        double pull = 0;
        double fired = open;
        double working = 0;
        Clutch state;
        if (changing > 0) {
            // the clutch out and the throttle off, or blipped going down: the engine on its own
            final double blip = into < now ? BLIP : 0;
            turning = new Revs(rpm, 0, 0).step(engine, blip, seconds).rpm();
            fired = blip;
            state = Clutch.SHIFTING;
        } else {
            final double engage = vehicle.cvt() > 0 ? 1.5 * engine.idle() : engine.idle();
            final double coupled;
            if (vehicle.cvt() > 0) {
                // the ratio that holds the engine where the throttle asks, as far as the ratios go
                final double want = engine.idle() + (vehicle.cvt() - engine.idle()) * open;
                coupled = Math.clamp(want, spin * vehicle.highest(), Math.max(spin * vehicle.lowest(), spin * vehicle.highest()));
            } else {
                coupled = spin * vehicle.ratio(now);
            }
            if (coupled >= engage && (clutch == Clutch.IN || coupled >= rpm - CATCH)) {
                state = Clutch.IN;
                turning = coupled;
                ratio = spin > 0 ? coupled / spin : 0;
            } else if (open > 0.02) {
                // pulling away, the engine up to its launch, pulling through the slipping clutch (a CVT's
                // held there); or, a new gear taken while moving, the engine brought to the wheels as it bites
                state = Clutch.SLIPPING;
                final double launch = vehicle.cvt() > 0
                        ? engine.idle() + (vehicle.cvt() - engine.idle()) * open
                        : engine.idle() + LAUNCH * open * (engine.redline() - engine.idle());
                if (vehicle.cvt() <= 0 && coupled >= engage) {
                    // brought down to the wheels, steadily, so it catches them as they gather speed
                    turning = Math.max(coupled, rpm - BITE * seconds);
                } else {
                    turning = rpm + (launch - rpm) * Math.min(1, 6 * seconds);
                }
                ratio = vehicle.cvt() > 0 ? vehicle.lowest() : vehicle.ratio(now);
            } else {
                state = Clutch.OUT;
                turning = new Revs(rpm, 0, 0).step(engine, 0, seconds).rpm();
            }
            if (state != Clutch.OUT) {
                if (turning >= engine.redline()) {
                    turning = Math.min(turning, engine.redline() * 1.02);
                    cutting = Revs.CUT;
                }
                final double fuel = cutting > 0 ? 0 : open;
                fired = fuel;
                final double boosting = engine.charged() ? engine.charger().pull(boost) : 1;
                final double torque = fuel > 0 ? vehicle.torqueAt(turning) * fuel * boosting
                        : -ENGINE_BRAKE * vehicle.torque() * (0.3 + 0.7 * turning / engine.redline());
                pull = (state == Clutch.SLIPPING ? Math.max(0, torque) : torque) * ratio * EFFICIENCY / vehicle.wheel();
                // no harder than the tyres grip, or a bike keeps its front wheel down
                pull = Math.min(pull, vehicle.grip() * vehicle.mass() * G);
                working = fuel;
            }
        }
        // the road: what pulls it, what holds it back
        final double resist = speed > 0 ? 0.5 * AIR * vehicle.drag() * speed * speed + ROLLING * vehicle.mass() * G : 0;
        final double braking = speed > 0 ? Math.clamp(brake, 0, 1) * BRAKING * vehicle.mass() : 0;
        final double faster = (pull - resist - braking) / (vehicle.mass() * SPINNING);
        final double moving = Math.max(0, speed + faster * seconds);
        if (state == Clutch.IN && vehicle.cvt() <= 0) {
            turning = moving / vehicle.wheel() * 60 / (2 * Math.PI) * vehicle.ratio(now);
        }
        // a turbocharger spooling up, or down, with what the engine does; a supercharger with it at once
        final double boosted = engine.charged()
                ? engine.charger().spool(engine, boost, turning, fired, working, seconds) : 0;
        return new Driving(moving, now, turning, changing, into, cutting, fired, working, state, boosted);
    }

    /** The gear, as a dashboard shows it: the number, or D for a CVT. */
    public String shown() {
        return gear == 0 ? "D" : Integer.toString(shifting > 0 ? next : gear);
    }
}
