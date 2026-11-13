package com.example.fxsuite.avatar.sound;

/**
 * What blows an engine's intake full of air, if anything does: a {@link Turbo}, spun by its
 * exhaust, or a {@link Supercharger}, driven off its crank. Either way, how much it is doing at
 * any moment is its boost, 0 to 1 ({@link #spool}); the engine pulls harder the more there is
 * ({@link #pull}); and it is heard beside the engine, each in its own way ({@link #voice}).
 */
public sealed interface Charger permits Turbo, Supercharger {

    /**
     * Its boost {@code seconds} on from {@code boost}, the engine turning at {@code rpm} with the
     * throttle open {@code throttle} and working {@code load}.
     */
    double spool(Engine engine, double boost, double rpm, double throttle, double load, double seconds);

    /** How much more the engine pulls with {@code boost}, against none: 1 off boost. */
    double pull(double boost);

    /** How much it softens the exhaust's ring, 0 not at all to 1 entirely. */
    double muffle();

    /** What it is, as a name is finished with it: "turbo", "supercharged". */
    String kind();

    /** It, heard beside {@code engine}. */
    ChargerVoice voice(Engine engine);
}
