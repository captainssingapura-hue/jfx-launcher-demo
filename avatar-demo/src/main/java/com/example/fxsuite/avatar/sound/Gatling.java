package com.example.fxsuite.avatar.sound;

import com.example.fxsuite.avatar.sim.Gun;
import com.example.fxsuite.avatar.sim.Weapon;

import java.util.List;
import java.util.Locale;

/**
 * A Gatling gun, as it is heard: what {@link GatlingVoice} makes its sound from. Not a recording,
 * but the gun itself, roughly: its barrels going round, each fired as it comes to the top, so many
 * rounds a minute; how big its rounds are, which says how hard, how low and how loud each one's
 * blast is; how long its barrels take to spin up and run down; and the motor that turns them,
 * geared to them, whining as it spins them up.
 *
 * <p>So the shots come exactly as often as the gun fires them, and run together into its note:
 * forty a second for the hand-held gun, a snarl; twenty for the giant one, a rattle you can count;
 * sixty-five for the rapid giant, one tearing note. Its size and its rate are its own, each set
 * apart from the other ({@link #sized}, {@link #firing}): the same gun bigger, or faster.
 *
 * @param key       what it is called for short, as {@link #called} finds it
 * @param name      what it is called
 * @param about     a line on what it sounds like, and why
 * @param rate      how many rounds it fires a minute, at full speed
 * @param spinUp    how long its barrels take to spin up from still, in seconds
 * @param spinDown  how long they take to run down from full speed
 * @param barrels   how many barrels go round, one fired at each pass
 * @param calibre   how big its rounds are, in millimetres: the blast's size, and how loud
 * @param motor     how fast the motor's shaft turns with the barrels at full speed, in turns a second
 * @param teeth     the teeth on the motor's pinion: the gears whine at this many times the shaft's turns
 * @param hydraulic whether a hydraulic motor drives it, hissing as well as whining, as the biggest have
 */
public record Gatling(String key, String name, String about, double rate, double spinUp, double spinDown,
                      int barrels, double calibre, double motor, int teeth, boolean hydraulic) {

    /** The rounds it may be sized to, in millimetres; and the rates it may fire at, a minute. */
    public static final double SMALLEST = 5;
    public static final double BIGGEST = 40;
    public static final double SLOWEST = 300;
    public static final double FASTEST = 6000;

    public Gatling {
        if (barrels < 2 || calibre <= 0 || rate <= 0 || spinUp <= 0 || spinDown <= 0 || motor <= 0 || teeth < 1) {
            throw new IllegalArgumentException(name + ": barrels, rounds, a rate, a motor and gears");
        }
    }

    /** The hand-held one: rifle rounds, six barrels, an electric motor, 2400 a minute. */
    public static final Gatling HAND = of(Gun.GATLING, "HAND", "Hand-held Gatling gun",
            "Rifle rounds, forty a second: a ripping snarl over a thin electric whine.",
            6, 7.62, 110, 13, false);
    /** The heavy one, on a mount: heavy machine-gun rounds, 1800 a minute, a bigger motor. */
    public static final Gatling HEAVY = of(Gun.HEAVY_GATLING, "HEAVY", "Heavy Gatling gun",
            "Heavy rounds, thirty a second: a deep, tearing roar, the motor lower and stronger.",
            6, 12.7, 85, 15, false);
    /** The giant one, under the airship: cannon shells, 1200 a minute, turned by a hydraulic motor. */
    public static final Gatling GIANT = of(Gun.GIANT_GATLING, "GIANT", "Giant Gatling gun",
            "Cannon shells, twenty a second: a pounding you can almost count, each a thump in the chest,"
                    + " the hydraulic drive hissing and moaning.",
            6, 30, 55, 17, true);
    /**
     * A giant that fires very fast, under the airship: seven barrels of cannon shells, 3900 a minute,
     * spun up in half a second by a great hydraulic drive, as the biggest aircraft cannon are.
     */
    public static final Gatling RAPID = of(Gun.RAPID_GATLING, "RAPID", "Rapid giant Gatling gun",
            "Cannon shells, sixty-five a second: too fast to count, the shots fused into one tearing,"
                    + " ripping note, the hydraulic drive screaming under it.",
            7, 30, 95, 19, true);

    public static final List<Gatling> ALL = List.of(HAND, HEAVY, GIANT, RAPID);

    /** The one the game's {@code gun} is: its rate and how fast it spins up and down, with these figures. */
    private static Gatling of(final Gun gun, final String key, final String name, final String about,
                              final int barrels, final double calibre, final double motor, final int teeth,
                              final boolean hydraulic) {
        return new Gatling(key, name, about, 60 / gun.interval(), gun.spinUp(), gun.spinDown(), barrels, calibre,
                motor, teeth, hydraulic);
    }

    /** The one firing {@code weapon}'s rounds; the hand-held one for any other. */
    public static Gatling of(final Weapon weapon) {
        return switch (weapon) {
            case HEAVY_GATLING -> HEAVY;
            case GIANT_GATLING -> GIANT;
            case RAPID_GATLING -> RAPID;
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

    /** The same gun, its rounds {@code calibre} millimetres: bigger, lower and louder, or smaller. */
    public Gatling sized(final double calibre) {
        return new Gatling(key, name, about, rate, spinUp, spinDown, barrels,
                Math.clamp(calibre, SMALLEST, BIGGEST), motor, teeth, hydraulic);
    }

    /**
     * The same gun firing {@code rate} rounds a minute: its barrels turning that much faster or
     * slower, and the motor geared to them with them, so its whine goes up or down too.
     */
    public Gatling firing(final double rate) {
        final double wanted = Math.clamp(rate, SLOWEST, FASTEST);
        return new Gatling(key, name, about, wanted, spinUp, spinDown, barrels, calibre, motor * wanted / this.rate,
                teeth, hydraulic);
    }

    /** How many rounds a second: the note its shots run together into. */
    public double perSecond() {
        return rate / 60;
    }

    /** How many times a second its barrels go round, at full speed. */
    public double turns() {
        return perSecond() / barrels;
    }

    /** The gears' whine at full speed, in Hz: the pinion's teeth meshing. */
    public double whine() {
        return motor * teeth;
    }

    /** How loud each shot is, against a rifle round's: louder the bigger the round. */
    public double loud() {
        return Math.pow(calibre / 7.62, 0.55);
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
