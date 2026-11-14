package com.example.fxsuite.avatar.sound;

import com.example.fxsuite.avatar.sim.Gun;
import com.example.fxsuite.avatar.sim.Weapon;

import java.util.List;
import java.util.Locale;

/**
 * A Gatling gun, as it is heard: what {@link GatlingVoice} makes its sound from. Not a recording,
 * but the gun itself, roughly: its barrels going round, each fired as it comes to the top, as
 * often as the {@link Gun} it is fires; how big its rounds are, which says how hard and how low
 * each one's blast is; and the motor that turns it, geared down to the barrels, whining as it
 * spins them up.
 *
 * <p>So the shots come exactly as often as the gun fires them, and run together into its note:
 * forty a second for the hand-held gun, a snarl; twenty for the giant one, a rattle you can count.
 * The bigger the rounds, the lower and longer each blast.
 *
 * @param key      what it is called for short, as {@link #called} finds it
 * @param name     what it is called
 * @param about    a line on what it sounds like, and why
 * @param gun      the gun in the world it is: how often it fires, how fast it spins up and down
 * @param barrels  how many barrels go round, one fired at each pass
 * @param calibre  how big its rounds are, in millimetres: the blast's size
 * @param motor    how fast the motor's shaft turns with the barrels at full speed, in turns a second
 * @param teeth    the teeth on the motor's pinion: the gears whine at this many times the shaft's turns
 * @param hydraulic whether a hydraulic motor drives it, hissing as well as whining, as the biggest have
 * @param loud     how loud it is, against the others: 1 for the hand-held one
 */
public record Gatling(String key, String name, String about, Gun gun, int barrels, double calibre, double motor,
                      int teeth, boolean hydraulic, double loud) {

    public Gatling {
        if (barrels < 2 || calibre <= 0 || motor <= 0 || teeth < 1 || loud <= 0) {
            throw new IllegalArgumentException(name + ": barrels, rounds, a motor and gears");
        }
    }

    /** The hand-held one: rifle rounds, six barrels, an electric motor, 2400 a minute. */
    public static final Gatling HAND = new Gatling("HAND", "Hand-held Gatling gun",
            "Rifle rounds, forty a second: a ripping snarl over a thin electric whine.",
            Gun.GATLING, 6, 7.62, 110, 13, false, 1);
    /** The heavy one, on a mount: heavy machine-gun rounds, 1800 a minute, a bigger motor. */
    public static final Gatling HEAVY = new Gatling("HEAVY", "Heavy Gatling gun",
            "Heavy rounds, thirty a second: a deep, tearing roar, the motor lower and stronger.",
            Gun.HEAVY_GATLING, 6, 12.7, 85, 15, false, 1.5);
    /** The giant one, under the airship: cannon shells, 1200 a minute, turned by a hydraulic motor. */
    public static final Gatling GIANT = new Gatling("GIANT", "Giant Gatling gun",
            "Cannon shells, twenty a second: a pounding you can almost count, each a thump in the chest,"
                    + " the hydraulic drive hissing and moaning.",
            Gun.GIANT_GATLING, 6, 30, 55, 17, true, 2.2);

    public static final List<Gatling> ALL = List.of(HAND, HEAVY, GIANT);

    /** The one firing {@code weapon}'s rounds; the hand-held one for any other. */
    public static Gatling of(final Weapon weapon) {
        return switch (weapon) {
            case HEAVY_GATLING -> HEAVY;
            case GIANT_GATLING -> GIANT;
            default -> HAND;
        };
    }

    /** By its {@link #key} or the start of its name, ignoring case; {@code otherwise} if none. */
    public static Gatling called(final String wanted, final Gatling otherwise) {
        if (wanted == null || wanted.isBlank()) {
            return otherwise;
        }
        final String w = wanted.strip().toLowerCase(Locale.ROOT);
        return ALL.stream().filter(g -> g.key().toLowerCase(Locale.ROOT).equals(w)
                || g.name().toLowerCase(Locale.ROOT).startsWith(w)).findFirst().orElse(otherwise);
    }

    /** How many rounds it fires a minute, at full speed. */
    public double rate() {
        return 60 / gun.interval();
    }

    /** How many rounds a second: the note its shots run together into. */
    public double perSecond() {
        return 1 / gun.interval();
    }

    /** How many times a second its barrels go round, at full speed. */
    public double turns() {
        return perSecond() / barrels;
    }

    /** The gears' whine at full speed, in Hz: the pinion's teeth meshing. */
    public double whine() {
        return motor * teeth;
    }

    /**
     * How long the first, pushing part of each blast lasts, in seconds: about a millisecond for a
     * rifle round, longer the bigger the round, as a bigger charge pushes a bigger mass of air.
     */
    public double blast() {
        return 0.00036 * Math.sqrt(calibre);
    }

    /** The note each blast thumps at, in Hz: lower the bigger the round. */
    public double thump() {
        return 1100 / Math.pow(calibre, 0.9);
    }

    @Override
    public String toString() {
        return name;
    }
}
