package com.example.fxsuite.avatar.sound;

/**
 * A turbocharger, as an {@link Engine} may have: a turbine in its exhaust spinning a compressor
 * that blows the intake full of air. How far it is spooled up, its boost, 0 to 1, is the one
 * thing to know of it at any moment ({@link #spool}): it builds, late, with the revs (all of it
 * a little past where it starts, {@link #from}), the throttle and the load, and falls away off
 * the throttle. Heard, it whistles, higher the faster it spins; it softens the exhaust, as the
 * turbine stands in the way; and lifting off on boost, the blow-off valve lets it go with a hiss.
 * Driven, it pulls harder the more boost there is.
 *
 * @param lag     how long it takes to spool up, in seconds, near enough
 * @param from    where in the revs it starts to make boost: a share of the red line
 * @param whistle the note it whistles at, spinning flat out, in Hz
 * @param dump    how loud the blow-off valve's hiss is
 * @param muffle  how much it softens the exhaust's ring, 0 not at all to 1 entirely
 * @param gain    how much more it pulls on full boost than off it: 0.8 nearly twice
 */
public record Turbo(double lag, double from, double whistle, double dump, double muffle, double gain) implements Charger {

    /** A turbocharger, as any of the cars' engines may be given. */
    public static final Turbo STANDARD = new Turbo(0.9, 0.22, 5200, 1, 0.35, 0.8);

    /** From where it starts to make boost, it makes it all this share of the red line further up. */
    private static final double BAND = 0.35;
    /** Falling away, it takes this share of its lag. */
    private static final double FALL = 0.5;

    /**
     * Its boost {@code seconds} on from {@code boost}, the engine turning at {@code rpm} with the
     * throttle open {@code throttle} and working {@code load}: revving free, it makes little.
     */
    @Override
    public double spool(final Engine engine, final double boost, final double rpm, final double throttle,
                        final double load, final double seconds) {
        final double revs = Math.clamp((rpm / engine.redline() - from) / BAND, 0, 1);
        final double target = Math.clamp(throttle, 0, 1) * revs * (0.25 + 0.75 * Math.clamp(load, 0, 1));
        final double time = target > boost ? lag : lag * FALL;
        return boost + (target - boost) * (1 - Math.exp(-seconds / time));
    }

    @Override
    public double pull(final double boost) {
        return 1 + gain * boost;
    }

    @Override
    public String kind() {
        return "turbo";
    }

    @Override
    public ChargerVoice voice(final Engine engine) {
        return new TurboVoice(engine, this);
    }
}
