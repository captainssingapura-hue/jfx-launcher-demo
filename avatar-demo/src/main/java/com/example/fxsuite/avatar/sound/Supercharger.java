package com.example.fxsuite.avatar.sound;

/**
 * A supercharger, as an {@link Engine} may have: a blower driven by a belt off the crank, its
 * rotors pumping air into the intake. Being geared to the engine, it has no lag: its boost comes
 * the moment the throttle opens, some even low in the revs, more as they rise. Heard, it whines,
 * a note geared to the revs like the blower itself; off the throttle its bypass valve quietly
 * lets the air round, with no hiss. It stands at the intake, so the exhaust rings as ever.
 *
 * @param gain  how much more the engine pulls on full boost than off it: 0.6 more than half again
 * @param whine the note its rotors whine at, for each thousand turns a minute of the engine, in Hz
 * @param loud  how loud the whine is, on full boost
 */
public record Supercharger(double gain, double whine, double loud) implements Charger {

    /** A supercharger, as any of the cars' engines may be given: a Roots blower. */
    public static final Supercharger STANDARD = new Supercharger(0.6, 640, 1);

    /** At the bottom of the revs, it makes this share of what it makes at the top. */
    private static final double LOW = 0.45;
    /** How briskly its boost follows the throttle: all but at once. */
    private static final double FOLLOW = 0.08;

    @Override
    public double spool(final Engine engine, final double boost, final double rpm, final double throttle,
                        final double load, final double seconds) {
        final double revs = Math.clamp(rpm / engine.redline(), 0, 1);
        final double target = Math.clamp(throttle, 0, 1) * (LOW + (1 - LOW) * revs)
                * (0.3 + 0.7 * Math.clamp(load, 0, 1));
        return boost + (target - boost) * (1 - Math.exp(-seconds / FOLLOW));
    }

    @Override
    public double pull(final double boost) {
        return 1 + gain * boost;
    }

    @Override
    public double muffle() {
        return 0;
    }

    @Override
    public String kind() {
        return "supercharged";
    }

    @Override
    public ChargerVoice voice(final Engine engine) {
        return new SuperchargerVoice(engine, this);
    }
}
