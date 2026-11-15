package com.example.fxsuite.avatar.air;

import com.example.fxsuite.avatar.build.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * The heavy bomber airship, drawn by its lines first ({@link Lines}), as a rigid airship is built:
 * its hull a frame of girders, ring frames round it and longitudinal girders along it, the skin
 * stretched over them. So its skeleton is its structure, and its skin flat panels between its
 * girders: a many-sided hull, not a smooth one.
 *
 * <p>Its hull, sixteen-sided, its belly flatter than its back, to carry a long gondola and its bomb
 * bay under it; a blunt nose, a long parallel middle, and a long tail tapering, as a torpedo's does,
 * to a slim hub. Round the hub, held off it by four vanes, a big shallow shroud: a ring, its section
 * an aerofoil, for the giant coaxial propeller that will drive it to turn in. Its shroud and its vanes
 * are drawn by their lines too, so all of it is its skeleton first.
 *
 * <p>In its own frame: across (right), along (nose ahead), up, centimetres; the middle of its length
 * at the origin, on its axis.
 */
public final class Bomber {

    private Bomber() {
    }

    /** Its longitudinal girders, from its keel up round its right side to its spine, 22.5° apart. */
    public static final List<String> LONGERONS = List.of("keel", "bilge", "lower flank", "lower beam", "beam",
            "upper beam", "upper flank", "shoulder", "spine");

    /** How big round it is, a share of its length. */
    public static final double DIAMETER = 0.25;
    /** How much flatter its belly is than its back: its lower girders this much nearer its axis, up and down. */
    public static final double BELLY = 0.85;

    /** Where its main frames are, from its tail (0) to its nose (1): closer where it curves most. */
    static final double[] FRAMES = {0, 0.015, 0.05, 0.08, 0.11, 0.15, 0.21, 0.28, 0.35, 0.42, 0.50, 0.58, 0.66, 0.74,
            0.81, 0.87, 0.92, 0.96, 0.985, 1};

    /** Where its nose begins rounding, and its tail tapering, from its tail. */
    static final double NOSE = 0.70;
    static final double TAIL = 0.40;
    /** Where its taper ends on its hub; and where its hub, running on back, begins to close round. */
    static final double HUB_ENDS = 0.11;
    static final double HUB_CLOSES = 0.05;
    /** How big round its hub is, a share of its widest. */
    static final double HUB = 0.18;

    /**
     * Its shroud: where its leading and trailing edges are, from its tail; how big round the middle of
     * its section is, a share of the hull's widest; how thick it is, a share of its chord.
     */
    static final double SHROUD_LEADS = 0.12;
    static final double SHROUD_TRAILS = 0.05;
    static final double SHROUD = 0.8;
    static final double SHROUD_THICK = 0.12;
    /** The vanes holding it: where their leading edges are, from its tail; their chord; how thick, a share of it. */
    static final double VANES_LEAD = 0.118;
    static final double VANE_CHORD = 0.035;
    static final double VANE_THICK = 0.16;

    /** How far round it is at {@code t} from its tail (0) to its nose (1), a share of its widest. */
    static double round(final double t) {
        if (t >= NOSE) {
            // a blunt nose: rounder than a circle's quarter, to its point
            final double u = (t - NOSE) / (1 - NOSE);
            return Math.pow(Math.max(0, 1 - Math.pow(u, 2.2)), 1 / 2.2);
        }
        if (t >= TAIL) {
            return 1;
        }
        if (t >= HUB_ENDS) {
            // a long tail, full where its middle ends, tapering to its hub
            final double u = (t - HUB_ENDS) / (TAIL - HUB_ENDS);
            return HUB + (1 - HUB) * Math.pow(Math.sin(Math.PI / 2 * u), 0.8);
        }
        // its hub, slim, running on back and closing round to a point
        if (t >= HUB_CLOSES) {
            return HUB;
        }
        final double u = 1 - t / HUB_CLOSES;
        return HUB * Math.sqrt(Math.max(0, 1 - u * u));
    }

    /** Its hull's lines, {@code length} long: its girders through its main frames. */
    public static Lines hull(final double length) {
        final double half = DIAMETER * length / 2;
        final List<Lines.Section> sections = new ArrayList<>();
        for (final double t : FRAMES) {
            final double r = round(t) * half;
            final List<double[]> points = new ArrayList<>();
            for (int i = 0; i < LONGERONS.size(); i++) {
                final double angle = angle(i);
                final double up = r < 1e-6 ? 0 : r * Math.sin(angle) * (angle < 0 ? BELLY : 1);
                points.add(new double[] {r < 1e-6 ? 0 : r * Math.cos(angle), up});
            }
            // where its taper meets its hub, a shoulder: its girders turn there, not curving through
            sections.add(new Lines.Section((t - 0.5) * length, points, t == HUB_ENDS));
        }
        return new Lines("hull", LONGERONS, sections, true, 2);
    }

    /** The {@code i}th girder's angle round from across, up positive: the keel's -90°, the spine's 90°. */
    private static double angle(final int i) {
        return Math.toRadians(-90 + 180.0 * i / (LONGERONS.size() - 1));
    }

    /** Where its shroud's sections are, from its trailing edge (0) to its leading edge (1). */
    private static final double[] CHORD = {0, 0.1, 0.3, 0.5, 0.7, 0.85, 0.95, 1};

    /**
     * Its shroud's lines, {@code length} long: {@code outside} its outer face, or its inner, from its
     * trailing edge to its leading edge, the two meeting at each; its section an aerofoil, symmetric
     * about its middle ring, thickest a third of the way back from its leading edge. Round, as
     * many-sided as its hull, its corners in line with its girders.
     */
    public static Lines shroud(final double length, final boolean outside) {
        final double middle = SHROUD * DIAMETER * length / 2;
        final double chord = (SHROUD_LEADS - SHROUD_TRAILS) * length;
        final List<Lines.Section> sections = new ArrayList<>();
        for (final double s : CHORD) {
            final double half = thickness(1 - s) * chord / 2;
            final double r = middle + (outside ? half : -half);
            final List<double[]> points = new ArrayList<>();
            for (int i = 0; i < LONGERONS.size(); i++) {
                points.add(new double[] {r * Math.cos(angle(i)), r * Math.sin(angle(i))});
            }
            sections.add(new Lines.Section((SHROUD_TRAILS + s * (SHROUD_LEADS - SHROUD_TRAILS) - 0.5) * length,
                    points));
        }
        return new Lines(outside ? "shroud outside" : "shroud inside", LONGERONS, sections, true, 2);
    }

    /**
     * How thick its shroud's section is, {@code x} back from its leading edge (0) to its trailing edge
     * (1): both faces, a share of its chord; a symmetric aerofoil's, its trailing edge closed.
     */
    static double thickness(final double x) {
        final double y = 5 * SHROUD_THICK * (0.2969 * Math.sqrt(x) - 0.126 * x - 0.3516 * x * x + 0.2843 * x * x * x
                - 0.1036 * x * x * x * x);
        return Math.max(0, 2 * y);
    }

    /** A vane's section, round from its leading edge over one face to its trailing edge and back: symmetric. */
    private static final double[][] VANE = {{0, 0}, {0.1, 0.42}, {0.35, 0.5}, {1, 0}, {0.35, -0.5}, {0.1, -0.42}};
    private static final List<String> VANE_LINES = List.of("leading edge", "front", "middle", "trailing edge",
            "other middle", "other front");

    /**
     * A vane {@code span} long and {@code chord} deep, {@code thick} a share of it, by its lines: its
     * root at the origin, its span out along +across, its leading edge there, its chord back (-along).
     * Square at both ends: its root in the hull, its tip in the shroud.
     */
    static Craft vane(final double span, final double chord, final double thick) {
        final List<Lines.Section> sections = new ArrayList<>();
        for (final double s : new double[] {0, span}) {
            final List<double[]> points = new ArrayList<>();
            for (final double[] p : VANE) {
                points.add(new double[] {p[0] * chord, p[1] * thick * chord});
            }
            sections.add(new Lines.Section(s, points));
        }
        // in its lines' frame its span along, its chord across: yawed a quarter, its span out to the right
        return Craft.named("vane").lines(new Lines("vane", VANE_LINES, sections, false, 0),
                (band, along) -> Paint.CAR, new Mount(Point.ZERO, 90, 0, 0)).build();
    }

    /** How its hull is dressed: its skin, and a darker band down its keel, where its gondola and bay hang. */
    static Lines.Dress dress() {
        return (band, along) -> band.equals("keel to bilge") ? Paint.CAR : Paint.SKIN;
    }

    /**
     * Its body, {@code length} long, by its lines and dressed on them: its hull, and at its tail its
     * shroud, held round its hub by four vanes, at its keel, its spine and either side. Nothing turning
     * in it yet.
     */
    public static Craft body(final double length) {
        final Lines hull = hull(length);
        final Craft.Builder body = Craft.named("Heavy bomber airship body").lines(hull, dress(), Mount.at(Point.ZERO));
        body.lines(shroud(length, true), (band, along) -> Paint.CAR, Mount.at(Point.ZERO));
        body.lines(shroud(length, false), (band, along) -> Paint.DARK, Mount.at(Point.ZERO));
        // the vanes: their roots a little into the hull where the middle of their chord is, their tips
        // to the middle of the shroud's section
        final double lead = (VANES_LEAD - 0.5) * length;
        final double chord = VANE_CHORD * length;
        final double mid = lead - chord / 2;
        final double tip = SHROUD * DIAMETER * length / 2;
        final double side = hull.on("beam", mid).across() * 0.9;
        body.withPair(vane(tip - side, chord, VANE_THICK), Mount.at(new Point(side, lead, 0)));
        final double top = hull.on("spine", mid).up() * 0.9;
        body.with(vane(tip - top, chord, VANE_THICK), new Mount(new Point(0, lead, top), 0, 0, -90));
        final double keel = -hull.on("keel", mid).up() * 0.9;
        body.with(vane(tip - keel, chord, VANE_THICK), new Mount(new Point(0, lead, -keel), 0, 0, 90));
        return body.build();
    }
}
