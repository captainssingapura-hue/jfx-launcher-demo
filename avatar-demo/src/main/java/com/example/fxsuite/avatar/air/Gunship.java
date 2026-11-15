package com.example.fxsuite.avatar.air;

import com.example.fxsuite.avatar.air.Fuselage.Station;
import com.example.fxsuite.avatar.build.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * A gunship, drawn by its lines first ({@link Lines}), dressed on them, then lifted any of several
 * ways. Its fuselage's lines: six longerons (a keel, a chine, its widest line, a shoulder, a canopy's
 * rail, a spine) through thirteen sections from the tip of its tail to the point of its nose: a
 * thin boom raised to its tail; a deep middle, its spine humped over its engine; a cockpit for two,
 * one behind the other, its canopy the bands over its shoulders there, glazed; a chiselled nose. Its
 * stub wings and its V-tail's fins are lines too: an aerofoil's section along their span, drawn to a
 * point at the tip.
 *
 * <p>So that it can be lifted any of several ways, it says where things are fixed to it
 * ({@link Hardpoints}), from its lines: a mast on the hump (a rotor, two), each wing's tip (a duct,
 * a pod), the boom's end (a tail fan), under the nose (a turret), pylons under each wing (weapons).
 * Each kind of lift ({@link Lift}) is fixed to those; the body the same for all.
 */
public final class Gunship {

    private Gunship() {
    }

    /** How it is lifted. */
    public enum Lift {
        /** None: the body alone, its hardpoints marked. */
        NONE,
        /** A coaxial duct at each wing's tip, tilting together ({@code "tilt"}) from hovering, 0°, to flying, 90°. */
        TILT_DUCTS,
        /** A four-bladed main rotor on the mast; a ducted fan at the boom's end, on its right, against its turning. */
        MAIN_ROTOR,
        /** Two three-bladed rotors on the mast, one over the other, turning opposite ways: no tail rotor needed. */
        COAXIAL_ROTORS
    }

    /**
     * Where things are fixed to the body, each a mount square to it, on the right where there is a
     * pair (mirrored for the left).
     *
     * @param mast    the mast's foot, on the hump
     * @param wingTip the middle of the right wing's tip
     * @param tail    the boom's end, on its middle line
     * @param nose    under its nose
     * @param pylons  under the right wing, inboard to outboard
     * @param ground  how high the ground is under its wheels
     */
    public record Hardpoints(Mount mast, Mount wingTip, Mount tail, Mount nose, List<Mount> pylons, double ground) {

        public Hardpoints {
            pylons = List.copyOf(pylons);
        }
    }

    // ---------------------------------------------------------------- its lines

    /** Its fuselage's longerons, keel up round to spine. */
    public static final List<String> LONGERONS = List.of("keel", "chine", "side", "shoulder", "rail", "spine");

    /**
     * Its fuselage's sections: each {t, then across and up where each longeron crosses it}, t the way
     * from the tip of its tail (0) to the point of its nose (1), all shares of its length.
     */
    private static final double[][] SECTIONS = {
            {0, 0, 0.06, 0, 0.06, 0, 0.06, 0, 0.06, 0, 0.06, 0, 0.06},
            {0.02, 0, 0.036, 0.016, 0.04, 0.022, 0.054, 0.018, 0.068, 0.009, 0.075, 0, 0.077},
            {0.3, 0, 0.028, 0.022, 0.032, 0.03, 0.05, 0.023, 0.068, 0.012, 0.077, 0, 0.079},
            {0.4, 0, -0.02, 0.05, -0.012, 0.062, 0.025, 0.05, 0.058, 0.026, 0.074, 0, 0.08},
            // its hips: broad and low, its beltline a crease standing out over its flanks, its cabin drawn in
            {0.48, 0, -0.066, 0.07, -0.06, 0.086, -0.004, 0.066, 0.05, 0.04, 0.086, 0, 0.104},
            {0.56, 0, -0.076, 0.076, -0.07, 0.092, -0.012, 0.07, 0.05, 0.044, 0.094, 0, 0.114},
            {0.64, 0, -0.078, 0.074, -0.072, 0.086, -0.016, 0.066, 0.044, 0.04, 0.09, 0, 0.106},
            // its cockpit, over a flat broad belly, its sides upright
            {0.69, 0, -0.077, 0.072, -0.071, 0.082, -0.018, 0.064, 0.04, 0.044, 0.09, 0, 0.104},
            {0.78, 0, -0.074, 0.07, -0.069, 0.078, -0.022, 0.062, 0.03, 0.04, 0.078, 0, 0.09},
            {0.86, 0, -0.07, 0.066, -0.066, 0.072, -0.026, 0.058, 0.018, 0.034, 0.052, 0, 0.062},
            // a long broad deck ahead of it, to a wide, squared nose
            {0.9, 0, -0.067, 0.063, -0.064, 0.068, -0.03, 0.055, 0.006, 0.03, 0.022, 0, 0.026},
            {0.955, 0, -0.061, 0.056, -0.059, 0.06, -0.036, 0.048, -0.012, 0.026, -0.006, 0, -0.004},
            {0.985, 0, -0.052, 0.04, -0.051, 0.043, -0.038, 0.034, -0.022, 0.018, -0.017, 0, -0.016},
            {1, 0, -0.035, 0, -0.035, 0, -0.035, 0, -0.035, 0, -0.035, 0, -0.035}};

    /** Where its cockpit's glazing runs, t; its engine's cowling; its intakes; the band round its boom. */
    private static final double[] CANOPY = {0.685, 0.885};
    private static final double[] COWLING = {0.47, 0.665};
    private static final double[] INTAKE = {0.585, 0.64};
    private static final double[] BAND = {0.29, 0.315};

    /** Its fuselage's lines, {@code length} long, as drawn. */
    public static Lines fuselage(final double length) {
        return fuselage(length, BodyStyle.PLAIN);
    }

    /** Its fuselage's lines, {@code length} long, styled as {@code style} says. */
    public static Lines fuselage(final double length, final BodyStyle style) {
        return style.lines("fuselage", LONGERONS, SECTIONS, length, 2);
    }

    /**
     * How its fuselage's bands are painted, {@code length} long: glazed over its cockpit, its cowling,
     * its intakes dark, a band round its boom, its belly darker.
     */
    private static Lines.Dress dress(final double length) {
        return (band, along) -> {
            final double t = along / length + 0.5;
            final boolean upper = band.equals("shoulder to rail") || band.equals("rail to spine");
            if (upper && t > CANOPY[0] && t < CANOPY[1]) {
                return Paint.GLASS;
            }
            if (band.equals("shoulder to rail") && t > INTAKE[0] && t < INTAKE[1]) {
                return Paint.DARK;
            }
            if (upper && t > COWLING[0] && t < COWLING[1]) {
                return Paint.CAR;
            }
            if (t > BAND[0] && t < BAND[1]) {
                return Paint.TRIM;
            }
            return band.equals("keel to chine") ? Paint.CAR : Paint.SKIN;
        };
    }

    /** An aerofoil's section, round from its leading edge over its top and back under it: {share of the chord back, share of its depth up}. */
    private static final double[][] AEROFOIL = {{0, 0}, {0.1, 0.45}, {0.38, 0.5}, {1, 0}, {0.38, -0.22}, {0.1, -0.2}};
    private static final List<String> WING = List.of("leading edge", "upper front", "upper middle", "trailing edge",
            "lower middle", "lower front");

    /**
     * A wing's lines, made as {@link Parts#wing} makes one: its root's leading edge at the origin, its
     * span out along +across, its chord back from its leading edge, swept {@code sweep} and raised
     * {@code dihedral} degrees, {@code thickness} its depth a share of its chord; its tip drawn to a point.
     */
    public static Craft wing(final String name, final Paint paint, final double span, final double root,
                             final double tip, final double sweep, final double dihedral, final double thickness) {
        // in the lines' frame: along its span, its chord back across (+), its depth up
        final List<Lines.Section> sections = new ArrayList<>();
        for (final double share : new double[] {0, 0.5, 1}) {
            final double s = span * share;
            final double chord = root + (tip - root) * share;
            final double back = s * Math.tan(Math.toRadians(sweep));
            final double up = s * Math.tan(Math.toRadians(dihedral));
            final List<double[]> points = new ArrayList<>();
            for (final double[] p : AEROFOIL) {
                points.add(new double[] {back + p[0] * chord, up + p[1] * thickness * chord});
            }
            sections.add(new Lines.Section(s, points));
        }
        // the tip, rounded off to a point a little beyond it
        final double back = span * Math.tan(Math.toRadians(sweep)) + 0.4 * tip;
        final double up = span * Math.tan(Math.toRadians(dihedral));
        final List<double[]> point = new ArrayList<>();
        for (int k = 0; k < AEROFOIL.length; k++) {
            point.add(new double[] {back, up});
        }
        sections.add(new Lines.Section(span + 0.06 * tip, point));
        final Lines lines = new Lines(name, WING, sections, false, 1);
        // its span along +across, its chord back (-along): yawed a quarter to the right
        return Craft.named(name).lines(lines, (band, along) -> paint, new Mount(Point.ZERO, 90, 0, 0)).build();
    }

    // ---------------------------------------------------------------- its hardpoints

    private static final double WING_ALONG = 0.05;
    private static final double WING_SPAN = 0.2;
    private static final double WING_ROOT = 0.13;
    private static final double WING_TIP = 0.09;
    private static final double WING_SWEEP = 6;
    private static final double WING_ANHEDRAL = -5;

    /** Where the stub wing's root's leading edge is: on its side, a little up, sunk into it. */
    private static Point wingRoot(final Lines body, final double l) {
        final Point side = body.on("side", WING_ALONG * l);
        return new Point(side.across() - 0.012 * l, WING_ALONG * l + WING_ROOT * l / 2, side.up() + 0.018 * l);
    }

    /** The middle of the stub wing's chord {@code share} of the way out from its root. */
    private static Point onWing(final Point root, final double l, final double share) {
        final double out = WING_SPAN * l * share;
        final double chord = WING_ROOT * l + (WING_TIP - WING_ROOT) * l * share;
        return new Point(root.across() + out, root.along() - out * Math.tan(Math.toRadians(WING_SWEEP)) - chord / 2,
                root.up() + out * Math.tan(Math.toRadians(WING_ANHEDRAL)));
    }

    /** Where things are fixed to a body {@code length} long, as drawn. */
    public static Hardpoints hardpoints(final double length) {
        return hardpoints(length, BodyStyle.PLAIN);
    }

    /** Where things are fixed to a body {@code length} long, styled. */
    public static Hardpoints hardpoints(final double length, final BodyStyle style) {
        final double l = length;
        final Lines body = fuselage(l, style);
        final Point root = wingRoot(body, l);
        final List<Mount> pylons = new ArrayList<>();
        for (final double share : new double[] {0.35, 0.72}) {
            pylons.add(Mount.at(onWing(root, l, share).plus(new Point(0, 0, -0.035 * l))));
        }
        final double tailAt = -0.45 * l;
        final double tailUp = (body.on("keel", tailAt).up() + body.on("spine", tailAt).up()) / 2;
        return new Hardpoints(Mount.at(body.on("spine", WING_ALONG * l)), Mount.at(onWing(root, l, 1)),
                Mount.at(0, tailAt, tailUp), Mount.at(body.on("keel", 0.38 * l).plus(new Point(0, 0, -0.004 * l))),
                pylons, body.on("keel", WING_ALONG * l).up() - 0.035 * l);
    }

    // ---------------------------------------------------------------- the body, dressed on its lines

    /** Its fuselage alone, {@code length} long: its lines, and its skin dressed on them; nothing fixed to it. */
    public static Craft fuselageAlone(final double length, final BodyStyle style) {
        return Craft.named("Gunship fuselage").lines(fuselage(length, style), dress(length), Mount.at(Point.ZERO)).build();
    }

    /** The body, {@code length} long: all of it but its lift; its hardpoints marked, if {@code marked}. */
    public static Craft body(final double length, final boolean marked) {
        return body(length, marked, BodyStyle.PLAIN);
    }

    /** The same, styled. */
    public static Craft body(final double length, final boolean marked, final BodyStyle style) {
        final double l = length;
        final Lines lines = fuselage(l, style);
        final Hardpoints at = hardpoints(l, style);
        final Craft.Builder gunship = Craft.named("Gunship body").lines(lines, dress(l), Mount.at(Point.ZERO));
        // the canopy's frames: a bow over it at each of its sections
        for (final double t : new double[] {0.68, 0.77, 0.85, 0.89}) {
            final double along = (t - 0.5) * l;
            final Point shoulder = lines.on("shoulder", along);
            final Point rail = lines.on("rail", along);
            final Point spine = lines.on("spine", along);
            gunship.pair(Piece.bar("bow", Paint.FRAME, shoulder, rail, 0.006 * l, 0.004 * l));
            gunship.pair(Piece.bar("bow", Paint.FRAME, rail, spine, 0.006 * l, 0.004 * l));
        }
        // its exhausts, out of the back of the hump, splayed
        gunship.withPair(Craft.named("exhaust").body(Fuselage.nacelle("exhaust", Paint.METAL, 0.06 * l, 0.026 * l)
                .faceted(8), Mount.at(Point.ZERO)).build(), new Mount(lines.on("rail", -0.035 * l).plus(new Point(-0.006 * l,
                0, -0.004 * l)), 160, 12, 0));
        // the stub wings, from its shoulders, a pod at each tip
        final Point root = wingRoot(lines, l);
        gunship.withPair(wing("stub wing", Paint.WING, WING_SPAN * l, WING_ROOT * l, WING_TIP * l, WING_SWEEP,
                WING_ANHEDRAL, 0.14), Mount.at(root));
        gunship.bodies(new Fuselage("tip pod", Paint.CAR, List.of(Station.round(-0.06 * l, 0),
                Station.round(-0.055 * l, 0.018 * l), Station.round(0.04 * l, 0.022 * l), Station.round(0.065 * l, 0)), 8)
                .faceted(8), at.wingTip());
        for (final Mount pylon : at.pylons()) {
            gunship.pair(Piece.box("pylon", Paint.METAL, pylon.at().plus(new Point(0, 0, 0.022 * l)), 0.012 * l, 0.07 * l,
                    0.03 * l));
        }
        // the V-tail on the boom's end, and a strake under it
        final double finAt = -0.38 * l;
        final Point top = lines.on("rail", finAt);
        gunship.withPair(wing("fin", Paint.WING, 0.12 * l, 0.1 * l, 0.045 * l, 38, 0, 0.1),
                new Mount(top.plus(new Point(-0.004 * l, 0, -0.004 * l)), 0, 0, -50));
        gunship.with(wing("strake", Paint.WING, 0.05 * l, 0.07 * l, 0.03 * l, 30, 0, 0.1),
                new Mount(lines.on("keel", finAt).plus(new Point(0, 0, 0.004 * l)), 0, 0, 90));
        // wheels: two under its middle, one under the strake
        final double ground = at.ground();
        final double mainAt = (WING_ALONG - 0.03) * l;
        final Point foot = new Point(0.075 * l, mainAt, ground + 0.025 * l);
        gunship.pair(Piece.bar("leg", Paint.METAL, lines.on("chine", mainAt).plus(new Point(-0.004 * l, 0, 0)), foot,
                0.012 * l, 0.012 * l));
        gunship.pair(Parts.wheel("wheel", Paint.DARK, Paint.METAL, 0.025 * l, 0.018 * l), Mount.at(foot));
        final Point tailFoot = new Point(0, finAt - 0.02 * l, ground + 0.016 * l);
        gunship.part(Piece.bar("tail leg", Paint.METAL, lines.on("keel", finAt - 0.02 * l).plus(new Point(0, 0, -0.05 * l)),
                tailFoot, 0.01 * l, 0.01 * l));
        gunship.parts(Parts.wheel("tail wheel", Paint.DARK, Paint.METAL, 0.016 * l, 0.012 * l), Mount.at(tailFoot));
        if (marked) {
            // its hardpoints, each a small bright cube
            final double c = 0.018 * l;
            for (final Mount m : List.of(at.mast(), at.tail(), at.nose())) {
                gunship.part(Piece.box("hardpoint", Paint.TRIM, m.at(), c, c, c));
            }
            gunship.pair(Piece.box("hardpoint", Paint.TRIM, at.wingTip().at().plus(new Point(0.03 * l, 0, 0)), c, c, c));
            for (final Mount m : at.pylons()) {
                gunship.pair(Piece.box("hardpoint", Paint.TRIM, m.at(), c, c, c));
            }
        }
        return gunship.build();
    }

    /** A gunship {@code length} long, lifted as {@code lift} says. */
    public static Craft of(final double length, final Lift lift) {
        return of(length, lift, BodyStyle.PLAIN);
    }

    /** The same, its body styled. */
    public static Craft of(final double length, final Lift lift, final BodyStyle style) {
        final double l = length;
        final Hardpoints at = hardpoints(l, style);
        final Craft.Builder gunship = Craft.named(switch (lift) {
            case NONE -> "Gunship body";
            case TILT_DUCTS -> "Gunship, tilting ducts";
            case MAIN_ROTOR -> "Gunship, main rotor";
            case COAXIAL_ROTORS -> "Gunship, coaxial rotors";
        }).with(body(l, lift == Lift.NONE, style), Mount.at(Point.ZERO));
        switch (lift) {
            case NONE -> { }
            case TILT_DUCTS -> tiltDucts(gunship, at, l);
            case MAIN_ROTOR -> mainRotor(gunship, at, l);
            case COAXIAL_ROTORS -> coaxialRotors(gunship, at, l);
        }
        return gunship.build();
    }

    /**
     * A coaxial duct beyond each wing's tip, on its trunnion, its axis up to hover; tilting together on
     * the trunnions' line ({@code "tilt"}), forward to fly, 0° to 90°.
     */
    private static void tiltDucts(final Craft.Builder gunship, final Hardpoints at, final double l) {
        final Engine duct = Engine.coaxialDuct(0.24 * l);
        final double d = duct.diameter();
        // its inner trunnion's end against the tip pod: the trunnion is 0.05 out from its duct, 0.07 each way long
        final Point centre = at.wingTip().at().plus(new Point(0.02 * l + d / 2 + 0.12 * d, 0, 0));
        final Craft tilting = duct.build().placed(new Mount(centre, 0, 90, 0))
                .hinged("tilt", centre, new Point(-1, 0, 0), 0, 90);
        gunship.withPair(tilting, Mount.at(Point.ZERO));
    }

    /** A four-bladed main rotor on the mast over a hub; a ducted fan at the boom's end, on its right. */
    private static void mainRotor(final Craft.Builder gunship, final Hardpoints at, final double l) {
        final Point foot = at.mast().at();
        final Point hub = foot.plus(new Point(0, 0, 0.05 * l));
        gunship.part(Piece.bar("mast", Paint.METAL, foot, hub, 0.014 * l, 0.014 * l));
        gunship.body(Fuselage.spinner("hub", Paint.METAL, 0.03 * l, 0.04 * l).faceted(8),
                new Mount(hub.plus(new Point(0, 0, 0.012 * l)), 0, 90, 0));
        gunship.spinner("main rotor", Parts.rotor("main rotor", Paint.DARK, Paint.METAL, 4, 0.42 * l, 0.03 * l, 10, 1.5),
                List.of(), Mount.at(hub), 5);
        final Engine fan = Engine.ductedFan(0.085 * l);
        gunship.with(fan.build(), new Mount(at.tail().at().plus(new Point(0.025 * l + fan.diameter() * 0.25, 0, 0)), 90, 0,
                0));
    }

    /** Two three-bladed rotors on a longer mast, one over the other, turning opposite ways. */
    private static void coaxialRotors(final Craft.Builder gunship, final Hardpoints at, final double l) {
        final Point foot = at.mast().at();
        final Point lower = foot.plus(new Point(0, 0, 0.045 * l));
        final Point upper = foot.plus(new Point(0, 0, 0.085 * l));
        gunship.part(Piece.bar("mast", Paint.METAL, foot, upper.plus(new Point(0, 0, 0.01 * l)), 0.016 * l, 0.016 * l));
        gunship.body(Fuselage.spinner("hub", Paint.METAL, 0.03 * l, 0.04 * l).faceted(8),
                new Mount(upper.plus(new Point(0, 0, 0.012 * l)), 0, 90, 0));
        gunship.spinner("lower rotor", Parts.rotor("lower rotor", Paint.DARK, Paint.METAL, 3, 0.38 * l, 0.032 * l, 10,
                1), List.of(), Mount.at(lower), 5);
        gunship.spinner("upper rotor", Parts.rotor("upper rotor", Paint.DARK, Paint.METAL, 3, 0.38 * l, 0.032 * l, -10,
                1), List.of(), Mount.at(upper), -5);
    }
}
