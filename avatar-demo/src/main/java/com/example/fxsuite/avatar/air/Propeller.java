package com.example.fxsuite.avatar.air;

import com.example.fxsuite.avatar.build.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * A propeller, for wherever one turns in the open: on an engine's nose, at an airship's tail, on a
 * drone's arm. Made to any size, every part of it a share of its {@code diameter}, so the same
 * propeller is a model's or a giant's; and drawn by its lines, as everything is ({@link Lines}):
 *
 * <ul>
 *   <li><b>Its blades</b>, each an aerofoil section carried out along its span, widest a little
 *       inside its middle, narrowing to a rounded tip, thinner toward it; and twisted as a real
 *       blade is, for a steady pitch ({@code pitch}, how far it would screw ahead in a turn, a share
 *       of its diameter): steep at its root, flat at its tip.</li>
 *   <li><b>Its spinner</b>, the cone over its hub, round ({@link Lines#around}), its blades' roots
 *       in it.</li>
 * </ul>
 *
 * <p>It turns about +along, pulling that way, {@code rate} turns a second ({@link #build}): the
 * right-hand way about its axis, its blades' leading edges first. Its blades as many as its size
 * calls for ({@link #of}), or as wanted; and a pair on one axis, the one behind turning the other
 * way ({@link #coaxial}).
 *
 * @param diameter how far across it is, tip to tip, centimetres
 * @param blades   how many blades it has
 * @param pitch    how far it would screw ahead in one turn, a share of its diameter
 * @param rate     how fast it turns, turns a second, at full power
 */
public record Propeller(double diameter, int blades, double pitch, double rate) {

    /** How far ahead it would screw in a turn, as most propellers are made, a share of its diameter. */
    public static final double PITCH = 0.9;
    /** How fast its tips go, as it is drawn turning, centimetres a second; and how fast it turns at most, to be seen to. */
    public static final double TIPS = 23_000;
    public static final double MOST_RATE = 12;
    /** Its spinner: how far round, how far ahead of its blades, and how far behind them it reaches, shares of its diameter. */
    static final double HUB = 0.075;
    static final double NOSE = 0.14;
    static final double BACK = 0.06;
    /** Where its blades' roots are, out from its axis, a share of its radius: in its spinner. */
    static final double ROOT = 0.12;

    public Propeller {
        if (!(diameter > 0) || blades < 2 || blades > 8 || !(pitch > 0) || !(rate > 0)) {
            throw new IllegalArgumentException("a propeller " + diameter + " across, " + blades + " blades, pitch " + pitch
                    + ", turning " + rate);
        }
    }

    /**
     * A propeller {@code diameter} across, as one that size is made: more blades the bigger it is
     * (two on a model's, up to five on a giant's), its usual pitch, turning as fast as its tips allow.
     */
    public static Propeller of(final double diameter) {
        final int blades = diameter < 150 ? 2 : diameter < 400 ? 3 : diameter < 800 ? 4 : 5;
        return new Propeller(diameter, blades, PITCH, Math.min(MOST_RATE, TIPS / (Math.PI * diameter)));
    }

    /** The same, with {@code count} blades. */
    public Propeller blades(final int count) {
        return new Propeller(diameter, count, pitch, rate);
    }

    // ---------------------------------------------------------------- a blade

    /** Its blade's lines round a section: from its leading edge over its back (the side it pulls toward) and round under it. */
    private static final List<String> EDGES = List.of("leading edge", "back front", "back middle", "trailing edge",
            "face middle", "face front");
    /** Its blade's section: cambered, round from its leading edge (0) to its trailing edge (1) and back; thickness a share of its own. */
    private static final double[][] SECTION = {{0, 0}, {0.1, 0.5}, {0.38, 0.55}, {1, 0}, {0.38, -0.2}, {0.1, -0.22}};
    /** Where its blade's sections are along its span, shares of its radius; how wide, shares of its diameter (for three blades). */
    private static final double[] SPAN = {ROOT, 0.25, 0.45, 0.65, 0.82, 0.93, 0.98};
    private static final double[] CHORD = {0.065, 0.085, 0.095, 0.09, 0.075, 0.055, 0.038};
    /** Where along its chord it is turned about for its pitch, from its leading edge, a share of its chord. */
    private static final double AXIS = 0.3;

    /** How wide its blades are, for so many: narrower the more there are, for much the same blade area all told. */
    private double width(final int k) {
        return CHORD[k] * diameter * Math.sqrt(3.0 / blades);
    }

    /** How steeply its blade is set, {@code r} out from its axis, to its turning: for a steady pitch, steep near its hub. */
    private double angle(final double r) {
        return Math.atan(pitch * diameter / (2 * Math.PI * r));
    }

    /**
     * Its blade's lines: its span along, from its root to its tip; each section, its chord's
     * {@code x} and thickness {@code y} turned by its angle there, across the way it turns (+) and,
     * up, back against its pull (so that, pitched up a quarter, its span out along +up, it pulls +along).
     */
    public Lines blade() {
        final double radius = diameter / 2;
        final List<Lines.Section> sections = new ArrayList<>();
        for (int k = 0; k < SPAN.length; k++) {
            final double r = SPAN[k] * radius;
            final double chord = width(k);
            final double thick = 0.22 - 0.16 * SPAN[k];
            final double beta = angle(r);
            final List<double[]> points = new ArrayList<>();
            for (final double[] p : SECTION) {
                points.add(set(AXIS - p[0], p[1] * thick, chord, beta));
            }
            sections.add(new Lines.Section(r, points));
        }
        // the tip, rounded off to a point a little beyond the last section, two-fifths back along its chord
        final double beta = angle(radius);
        final double[] tip = set(AXIS - 0.4, 0, width(SPAN.length - 1), beta);
        final List<double[]> point = new ArrayList<>();
        for (int k = 0; k < SECTION.length; k++) {
            point.add(tip.clone());
        }
        sections.add(new Lines.Section(radius, point));
        return new Lines("blade", EDGES, sections, false, 1);
    }

    /** A point of a section {@code ahead} of its pitch axis and {@code out} from its chord line, shares of {@code chord}, set at {@code beta}. */
    private static double[] set(final double ahead, final double out, final double chord, final double beta) {
        final double u = ahead * chord;
        final double v = out * chord;
        final double across = u * Math.cos(beta) - v * Math.sin(beta);
        final double pulled = u * Math.sin(beta) + v * Math.cos(beta);
        return new double[] {across, -pulled};
    }

    // ---------------------------------------------------------------- its spinner

    /** Its spinner's lines round it, from below up its right side to its top. */
    private static final List<String> ROUND = List.of("bottom", "low", "side", "high", "top");

    /** Its spinner's lines: closed behind, round, its widest a little behind its blades, drawn to a point ahead. */
    public Lines spinner() {
        final double h = HUB * diameter;
        final double[][] profile = {{-BACK, 0}, {-0.95 * BACK, 0.8}, {-0.75 * BACK, 1}, {0, 1}, {0.25 * NOSE, 0.93},
                {0.55 * NOSE, 0.68}, {0.82 * NOSE, 0.34}, {NOSE, 0}};
        final List<Lines.Section> sections = new ArrayList<>();
        for (final double[] at : profile) {
            final double r = at[1] * h;
            final List<double[]> points = new ArrayList<>();
            for (int i = 0; i < ROUND.size(); i++) {
                final double a = Math.toRadians(-90 + 180.0 * i / (ROUND.size() - 1));
                points.add(new double[] {r * Math.cos(a), r * Math.sin(a)});
            }
            sections.add(new Lines.Section(at[0] * diameter, points));
        }
        return new Lines("spinner", ROUND, sections, true, 1, 3);
    }

    // ---------------------------------------------------------------- made whole

    /**
     * It, its hub at the origin, turning about +along and pulling that way: its spinner, and its
     * blades round it, evenly, the first straight up.
     */
    public Craft build() {
        final Craft blade = Craft.named("blade").lines(blade(), (band, along) -> Paint.DARK, new Mount(Point.ZERO, 0, 90, 0))
                .build();
        final Craft.Builder turning = Craft.named("propeller")
                .lines(spinner(), (band, along) -> Paint.METAL, Mount.at(Point.ZERO));
        for (int k = 0; k < blades; k++) {
            turning.with(blade, new Mount(Point.ZERO, 0, 0, 360.0 * k / blades));
        }
        return Craft.named("Propeller").spinner(Spinner.of("propeller", Point.ZERO, new Point(0, 1, 0), rate,
                turning.build())).build();
    }

    /**
     * A pair of them on one axis, {@code apart} (a share of its diameter) one behind the other, about
     * the origin: the one behind turning the other way, its blades the other hand, so the two pull
     * the same way and their turning cancels out.
     */
    public Craft coaxial(final double apart) {
        final Craft front = build().placed(Mount.at(0, apart * diameter / 2, 0));
        final Spinner ahead = front.spinners().getFirst();
        final Spinner behind = ahead.placed(Mount.at(0, -apart * diameter, 0)).mirrored();
        return Craft.named("Coaxial propeller").spinner(ahead)
                .spinner(new Spinner("propeller behind", behind.hub(), behind.axis(), behind.rate(), behind.pieces(),
                        behind.skins(), behind.panels(), behind.skeletons())).build();
    }
}
