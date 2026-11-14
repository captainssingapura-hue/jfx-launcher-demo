package com.example.fxsuite.avatar.sound;

import java.util.Locale;

/**
 * How a {@link GatlingVoice} is mixed: the same gun, the same shots at the same moments, heard
 * two ways.
 *
 * <ul>
 *   <li><b>Realistic:</b> as a microphone a few metres off outdoors would have it. Each shot is
 *       mostly its crack, sharp and dry; the thump is there, but no more than it is; the motor's
 *       whine and the barrels' clatter are plain under it; the empty cases ring as they fall; the
 *       ground sends the blast back, and the air carries it off quickly.</li>
 *   <li><b>Cinematic:</b> as a film would mix it. Each shot thumps low, with a boom an octave
 *       under it, driven hard into a microphone's overload so the roar is loud all through; the
 *       crack is softer; the motor whines up louder; and it rolls away in a long tail, as off
 *       hills.</li>
 * </ul>
 *
 * @param title  what it is called
 * @param about  a line on how it sounds
 * @param crack  how loud each shot's crack is
 * @param push   how loud each shot's blast, the push of air out of the muzzle
 * @param thump  how loud each shot's thump, its body
 * @param boom   how loud a boom an octave under the thump; 0 for none
 * @param clack  how loud the bolts, cams and feed, firing or turning dry
 * @param whine  how loud the motor and its gears
 * @param brass  how loud the empty cases, falling
 * @param drive  how hard the shots are driven into overload: 1 barely, 3 hard
 * @param ground how loud the blast sent back off the ground
 * @param tail   how long it rings on, in seconds, to a thousandth as loud
 * @param wet    how loud that ringing is, against the shots themselves
 * @param level  how loud it all is, so that both sit about as loud as each other
 */
public enum GunStyle {

    REALISTIC("Realistic", "As a microphone outdoors would hear it: a sharp, dry crack, the mechanism plain under it.",
            1.0, 0.8, 0.55, 0, 0.45, 0.55, 1, 1.15, 0.55, 0.45, 0.10, 1.7),
    CINEMATIC("Cinematic", "As a film would mix it: a thump and a boom in each shot, driven hard, rolling away.",
            0.55, 1.0, 1.15, 0.9, 0.3, 0.8, 0.35, 2.8, 0.4, 1.9, 0.30, 1.0);

    private final String title;
    private final String about;
    private final double crack;
    private final double push;
    private final double thump;
    private final double boom;
    private final double clack;
    private final double whine;
    private final double brass;
    private final double drive;
    private final double ground;
    private final double tail;
    private final double wet;
    private final double level;

    GunStyle(final String title, final String about, final double crack, final double push, final double thump,
             final double boom, final double clack, final double whine, final double brass, final double drive,
             final double ground, final double tail, final double wet, final double level) {
        this.title = title;
        this.about = about;
        this.crack = crack;
        this.push = push;
        this.thump = thump;
        this.boom = boom;
        this.clack = clack;
        this.whine = whine;
        this.brass = brass;
        this.drive = drive;
        this.ground = ground;
        this.tail = tail;
        this.wet = wet;
        this.level = level;
    }

    public String title() {
        return title;
    }

    public String about() {
        return about;
    }

    double crack() {
        return crack;
    }

    double push() {
        return push;
    }

    double thump() {
        return thump;
    }

    double boom() {
        return boom;
    }

    double clack() {
        return clack;
    }

    double whine() {
        return whine;
    }

    double brass() {
        return brass;
    }

    double drive() {
        return drive;
    }

    double ground() {
        return ground;
    }

    public double tail() {
        return tail;
    }

    double wet() {
        return wet;
    }

    double level() {
        return level;
    }

    /** The other one. */
    public GunStyle next() {
        return values()[(ordinal() + 1) % values().length];
    }

    /** From {@code -Davatar.guns=realistic|cinematic}; cinematic if unset. */
    public static GunStyle fromSettings() {
        return named(System.getProperty("avatar.guns"));
    }

    /** By its name or the start of it, ignoring case; cinematic if none. */
    public static GunStyle named(final String wanted) {
        final String w = wanted == null ? "" : wanted.strip().toLowerCase(Locale.ROOT);
        return !w.isEmpty() && "realistic".startsWith(w) ? REALISTIC : CINEMATIC;
    }

    @Override
    public String toString() {
        return title;
    }
}
