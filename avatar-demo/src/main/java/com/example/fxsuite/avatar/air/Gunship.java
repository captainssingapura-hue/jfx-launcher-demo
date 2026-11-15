package com.example.fxsuite.avatar.air;

import com.example.fxsuite.avatar.build.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * A gunship, drawn by its lines first ({@link Lines}) and dressed on them; its lift a unit of its
 * own, set into a bay in its back. From the side: a long, slender, level boom to its tail; a deep
 * body under its middle, its belly long and flat, curving up into the boom behind; ahead, its cockpit
 * rising as a block, its roof falling forward to a raked windscreen and a blunt, rounded nose. Between
 * the boom's root and the cockpit's back, over its centre of gravity, the top of it is cut down to a
 * bay ({@link Bay}): upright walls fore and aft, a flat floor.
 *
 * <p>Whatever lifts it is set into that bay, the body the same for all ({@link Lift}): a unit's
 * saddle fills the bay and closes its top line, and its lift is built out from that: a fixed high
 * wing with two turboprops; two big propellers in tilting ducts on short beams; a main rotor on a
 * pylon, and a fan at its tail; or two coaxial rotors on a pylon.
 */
public final class Gunship {

    private Gunship() {
    }

    /** What lifts it, set into its bay. */
    public enum Lift {
        /** Nothing: its bay empty. */
        NONE,
        /** A high wing through its bay, a turboprop under each side of it. */
        FIXED_WING,
        /** Two big propellers, in coaxial ducts on short beams out of its bay, tilting together ({@code "tilt"}) from hovering, 0°, to flying, 90°. */
        TWIN_PROPELLERS,
        /** A four-bladed main rotor on a pylon in its bay; a ducted fan at the boom's end, on its right. */
        MAIN_ROTOR,
        /** Two three-bladed rotors on a pylon in its bay, one over the other, turning opposite ways. */
        COAXIAL_ROTORS
    }

    /**
     * The bay its lift is set into, over its centre of gravity: {@code floor} its floor's middle;
     * {@code length} fore and aft between its walls; {@code width} across its floor; {@code depth} from
     * its floor up to its top line.
     */
    public record Bay(Point floor, double length, double width, double depth) {
    }

    /**
     * Where things are fixed to the body.
     *
     * @param bay    its lift's bay
     * @param tail   the boom's end, on its middle line
     * @param nose   under its nose
     * @param ground how high the ground is under its wheels
     */
    public record Hardpoints(Bay bay, Mount tail, Mount nose, double ground) {
    }

    // ---------------------------------------------------------------- its lines

    /** Its fuselage's longerons, keel up round to spine. */
    public static final List<String> LONGERONS = List.of("keel", "chine", "side", "shoulder", "rail", "spine");

    /** How high its centre of gravity is over its belly, a share of its length: its frame's origin is level with it. */
    private static final double CENTRE = 0.094;

    /** Where its bay's walls are, t from its tail (0) to its nose (1); its floor's height, and its top line's, over its belly. */
    private static final double BAY_BACK = 0.4705;
    private static final double BAY_FRONT = 0.6665;
    /** Where its centre of gravity is along it, t. */
    public static final double CENTRE_AT = 0.583;

    /**
     * Its fuselage's lines plan, from the side as sketched: each {t, then across and up where each
     * longeron crosses its section, up over its belly; and 1 if the section is sharp}, all shares of its
     * length. Its tail's end squared off; a straight, level boom; its belly curving down into a long flat
     * bottom; the bay's walls upright and its floor flat; the cockpit's block, its roof falling to a
     * raked windscreen; a blunt, rounded nose.
     */
    private static final double[][] PLAN = {
            {0, 0, 0.212, 0, 0.212, 0, 0.212, 0, 0.212, 0, 0.212, 0, 0.212, 1},
            {0.012, 0, 0.18, 0.018, 0.185, 0.026, 0.205, 0.022, 0.232, 0.012, 0.24, 0, 0.242, 1},
            {0.3, 0, 0.135, 0.024, 0.14, 0.032, 0.18, 0.026, 0.232, 0.014, 0.242, 0, 0.244},
            {0.38, 0, 0.05, 0.05, 0.06, 0.062, 0.13, 0.05, 0.225, 0.03, 0.242, 0, 0.245},
            {0.44, 0, 0.012, 0.062, 0.02, 0.074, 0.1, 0.064, 0.215, 0.04, 0.242, 0, 0.245},
            // the bay: its back wall's top edge, its foot, its front wall's foot, its top edge
            {0.469, 0, 0.008, 0.064, 0.016, 0.076, 0.095, 0.066, 0.21, 0.042, 0.24, 0, 0.244, 1},
            {0.472, 0, 0.006, 0.065, 0.014, 0.077, 0.092, 0.068, 0.155, 0.04, 0.158, 0, 0.16, 1},
            {0.665, 0, 0.002, 0.066, 0.01, 0.077, 0.092, 0.068, 0.155, 0.04, 0.158, 0, 0.16, 1},
            {0.668, 0, 0.002, 0.066, 0.01, 0.077, 0.095, 0.066, 0.2, 0.046, 0.238, 0, 0.245, 1},
            // the cockpit's block, its roof falling forward
            {0.76, 0, 0.001, 0.064, 0.008, 0.074, 0.09, 0.062, 0.19, 0.044, 0.225, 0, 0.232},
            {0.86, 0, 0, 0.06, 0.006, 0.068, 0.085, 0.056, 0.165, 0.04, 0.19, 0, 0.196},
            // where the roof meets the windscreen
            {0.92, 0, 0, 0.056, 0.006, 0.062, 0.08, 0.05, 0.14, 0.034, 0.158, 0, 0.162, 1},
            // the windscreen raked down to a blunt, rounded nose
            {0.975, 0, 0.006, 0.046, 0.012, 0.05, 0.06, 0.04, 0.085, 0.026, 0.095, 0, 0.098},
            {0.995, 0, 0.014, 0.034, 0.018, 0.037, 0.05, 0.03, 0.07, 0.02, 0.076, 0, 0.078},
            {1, 0, 0.045, 0, 0.045, 0, 0.045, 0, 0.045, 0, 0.045, 0, 0.045}};

    /** Its lines plan with its heights over its centre of gravity, not its belly. */
    private static double[][] plan() {
        final double[][] plan = new double[PLAN.length][];
        for (int k = 0; k < PLAN.length; k++) {
            plan[k] = PLAN[k].clone();
            for (int i = 0; i < LONGERONS.size(); i++) {
                plan[k][2 + 2 * i] -= CENTRE;
            }
        }
        return plan;
    }

    /** Its fuselage's lines, {@code length} long, as drawn. */
    public static Lines fuselage(final double length) {
        return fuselage(length, BodyStyle.PLAIN);
    }

    /** Its fuselage's lines, {@code length} long, styled as {@code style} says. */
    public static Lines fuselage(final double length, final BodyStyle style) {
        return style.lines("fuselage", LONGERONS, plan(), length, 2);
    }

    /**
     * How its fuselage's bands are painted, {@code length} long: its bay's floor and walls a deck to
     * fix its lift to; its cockpit's side windows and its windscreen glazed, its roof solid; a band
     * round its boom; its belly darker.
     */
    private static Lines.Dress dress(final double length) {
        return (band, along) -> {
            final double t = along / length + 0.5;
            final boolean sides = band.equals("shoulder to rail");
            final boolean top = band.equals("rail to spine");
            if ((sides || top) && t > BAY_BACK - 0.002 && t < BAY_FRONT + 0.002) {
                return Paint.FRAME;
            }
            if (sides && t > 0.68 && t < 0.99 || top && t > 0.915 && t < 0.995) {
                return Paint.GLASS;
            }
            if (top && t > BAY_FRONT) {
                return Paint.CAR;
            }
            if (t > 0.29 && t < 0.31) {
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

    /** Where things are fixed to a body {@code length} long, as drawn. */
    public static Hardpoints hardpoints(final double length) {
        return hardpoints(length, BodyStyle.PLAIN);
    }

    /** Where things are fixed to a body {@code length} long, styled. */
    public static Hardpoints hardpoints(final double length, final BodyStyle style) {
        final double l = length;
        final Lines body = fuselage(l, style);
        final double bayAt = ((BAY_BACK + BAY_FRONT) / 2 - 0.5) * l;
        final Point floor = new Point(0, bayAt, body.on("spine", bayAt).up());
        final double top = body.on("spine", (BAY_FRONT - 0.5) * l + 0.004 * l).up();
        final Bay bay = new Bay(floor, (BAY_FRONT - BAY_BACK) * l, 2 * body.on("shoulder", bayAt).across(),
                top - floor.up());
        final double tailAt = -0.48 * l;
        final double tailUp = (body.on("keel", tailAt).up() + body.on("spine", tailAt).up()) / 2;
        return new Hardpoints(bay, Mount.at(0, tailAt, tailUp), Mount.at(body.on("keel", 0.42 * l)),
                body.on("keel", (CENTRE_AT - 0.5) * l).up() - 0.04 * l);
    }

    // ---------------------------------------------------------------- the body, dressed on its lines

    /** Its fuselage alone, {@code length} long: its lines, and its skin dressed on them; nothing fixed to it. */
    public static Craft fuselageAlone(final double length, final BodyStyle style) {
        return Craft.named("Gunship fuselage").lines(fuselage(length, style), dress(length), Mount.at(Point.ZERO)).build();
    }

    /** The body, {@code length} long, as drawn: all of it but its lift, its bay empty. */
    public static Craft body(final double length) {
        return body(length, BodyStyle.PLAIN);
    }

    /** The body, {@code length} long, styled. */
    public static Craft body(final double length, final BodyStyle style) {
        final double l = length;
        final Lines lines = fuselage(l, style);
        final Hardpoints at = hardpoints(l, style);
        final Craft.Builder gunship = Craft.named("Gunship body").lines(lines, dress(l), Mount.at(Point.ZERO));
        // its cockpit's frames: a post between each two side windows, its windscreen's frame
        for (final double t : new double[] {0.69, 0.79, 0.86, 0.92, 0.99}) {
            final double along = (t - 0.5) * l;
            gunship.pair(Piece.bar("post", Paint.FRAME, lines.on("shoulder", along), lines.on("rail", along), 0.006 * l,
                    0.004 * l));
        }
        final double screen = (0.92 - 0.5) * l;
        gunship.pair(Piece.bar("post", Paint.FRAME, lines.on("rail", screen), lines.on("spine", screen), 0.006 * l,
                0.004 * l));
        // a V-tail at the boom's end, a strake under it
        final double finAt = -0.43 * l;
        gunship.withPair(wing("fin", Paint.WING, 0.1 * l, 0.08 * l, 0.04 * l, 40, 0, 0.1),
                new Mount(lines.on("rail", finAt).plus(new Point(-0.004 * l, 0, -0.004 * l)), 0, 0, -50));
        gunship.with(wing("strake", Paint.WING, 0.045 * l, 0.06 * l, 0.03 * l, 30, 0, 0.1),
                new Mount(lines.on("keel", finAt).plus(new Point(0, 0, 0.004 * l)), 0, 0, 90));
        // wheels: two under its middle, one under its nose
        final double ground = at.ground();
        final double mainAt = (CENTRE_AT - 0.5) * l - 0.02 * l;
        final Point foot = new Point(0.085 * l, mainAt, ground + 0.025 * l);
        gunship.pair(Piece.bar("leg", Paint.METAL, lines.on("chine", mainAt).plus(new Point(-0.006 * l, 0, 0)), foot,
                0.012 * l, 0.012 * l));
        gunship.pair(Parts.wheel("wheel", Paint.DARK, Paint.METAL, 0.025 * l, 0.018 * l), Mount.at(foot));
        final double noseAt = 0.36 * l;
        final Point noseFoot = new Point(0, noseAt, ground + 0.018 * l);
        gunship.part(Piece.bar("nose leg", Paint.METAL, lines.on("keel", noseAt), noseFoot, 0.01 * l, 0.01 * l));
        gunship.parts(Parts.wheel("nose wheel", Paint.DARK, Paint.METAL, 0.018 * l, 0.014 * l), Mount.at(noseFoot));
        return gunship.build();
    }

    /** A gunship {@code length} long, lifted as {@code lift} says. */
    public static Craft of(final double length, final Lift lift) {
        return of(length, lift, BodyStyle.PLAIN);
    }

    /** The same, its body styled: its lift set into its bay, wherever the style puts it. */
    public static Craft of(final double length, final Lift lift, final BodyStyle style) {
        final double l = length;
        final Bay bay = hardpoints(l, style).bay();
        final Craft.Builder gunship = Craft.named(switch (lift) {
            case NONE -> "Gunship body";
            case FIXED_WING -> "Gunship, fixed wing";
            case TWIN_PROPELLERS -> "Gunship, two big propellers";
            case MAIN_ROTOR -> "Gunship, main rotor";
            case COAXIAL_ROTORS -> "Gunship, coaxial rotors";
        }).with(body(l, style), Mount.at(Point.ZERO));
        switch (lift) {
            case NONE -> { }
            case FIXED_WING -> fixedWing(gunship, bay, l);
            case TWIN_PROPELLERS -> twinPropellers(gunship, bay, l);
            case MAIN_ROTOR -> {
                final Point hub = pylonAndMast(gunship, bay, l, 0.06 * l);
                gunship.spinner("main rotor", Parts.rotor("main rotor", Paint.DARK, Paint.METAL, 4, 0.42 * l, 0.03 * l,
                        10, 1.5), List.of(), Mount.at(hub), 5);
                final Hardpoints at = hardpoints(l, style);
                final Engine fan = Engine.ductedFan(0.085 * l);
                gunship.with(fan.build(), new Mount(at.tail().at().plus(new Point(0.025 * l + fan.diameter() * 0.25, 0, 0)),
                        90, 0, 0));
            }
            case COAXIAL_ROTORS -> {
                final Point upper = pylonAndMast(gunship, bay, l, 0.1 * l);
                final Point lower = upper.plus(new Point(0, 0, -0.04 * l));
                gunship.spinner("lower rotor", Parts.rotor("lower rotor", Paint.DARK, Paint.METAL, 3, 0.38 * l,
                        0.032 * l, 10, 1), List.of(), Mount.at(lower), 5);
                gunship.spinner("upper rotor", Parts.rotor("upper rotor", Paint.DARK, Paint.METAL, 3, 0.38 * l,
                        0.032 * l, -10, 1), List.of(), Mount.at(upper), -5);
            }
        }
        return gunship.build();
    }

    // ---------------------------------------------------------------- its lift units, set into its bay

    /**
     * A lift unit's saddle: a block filling the bay, its top {@code over} above the body's top line
     * (0, flush with it), its lower part sunk below the bay's floor; chamfered.
     */
    private static void saddle(final Craft.Builder gunship, final Bay bay, final double l, final double over) {
        final double high = 2 * bay.depth() + over;
        final BoxBody block = new BoxBody("saddle", Paint.CAR, bay.length() * 0.985, bay.width() * 0.98, high,
                Math.min(0.012 * l, high / 4), 0, 0).withChamfers();
        gunship.body(block, Mount.at(bay.floor().plus(new Point(0, 0, over / 2))));
    }

    /** A high wing through the bay, flush with the top line; a turboprop slung under each side. */
    private static void fixedWing(final Craft.Builder gunship, final Bay bay, final double l) {
        saddle(gunship, bay, l, 0);
        final double root = bay.length() * 0.9;
        final double span = 0.42 * l;
        final double top = bay.floor().up() + bay.depth();
        final Point rootAt = new Point(bay.width() * 0.45, bay.floor().along() + root / 2, top - 0.012 * l);
        gunship.withPair(wing("wing", Paint.WING, span, root, 0.45 * root, 6, 3, 0.12), Mount.at(rootAt));
        final Engine engine = Engine.turboprop(0.15 * l);
        final double out = rootAt.across() + 0.32 * span;
        final Point engineAt = new Point(out, bay.floor().along() + 0.01 * l, top - 0.05 * l);
        gunship.withPair(engine.build(), Mount.at(engineAt));
        gunship.pair(Parts.strut("pylon", Paint.METAL, engineAt.plus(new Point(0, -0.02 * l, engine.diameter() * 0.35)),
                new Point(out, engineAt.along() - 0.02 * l, top - 0.008 * l), 0.07 * l));
    }

    /**
     * Two big propellers: a short beam out of each side of the saddle, a coaxial duct beyond each, on
     * its trunnion, its axis up to hover; tilting together ({@code "tilt"}), forward to fly, 0° to 90°.
     */
    private static void twinPropellers(final Craft.Builder gunship, final Bay bay, final double l) {
        saddle(gunship, bay, l, 0);
        final double root = bay.length() * 0.6;
        final double span = 0.13 * l;
        final double up = bay.floor().up() + bay.depth() * 0.55;
        final Point rootAt = new Point(bay.width() * 0.45, bay.floor().along() + root / 2, up);
        gunship.withPair(wing("beam", Paint.CAR, span, root, root * 0.85, 0, 0, 0.22), Mount.at(rootAt));
        final Engine duct = Engine.coaxialDuct(0.26 * l);
        final double d = duct.diameter();
        // its inner trunnion's end against the beam's tip: the trunnion is 0.05 out from its duct, 0.07 each way long
        final Point centre = new Point(rootAt.across() + span + d / 2 + 0.12 * d, bay.floor().along(), up);
        final Craft tilting = duct.build().placed(new Mount(centre, 0, 90, 0))
                .hinged("tilt", centre, new Point(-1, 0, 0), 0, 90);
        gunship.withPair(tilting, Mount.at(Point.ZERO));
    }

    /** A pylon standing {@code over} above the top line out of the bay, and a mast on it: its hub. */
    private static Point pylonAndMast(final Craft.Builder gunship, final Bay bay, final double l, final double over) {
        saddle(gunship, bay, l, over);
        final Point foot = bay.floor().plus(new Point(0, 0, bay.depth() + over));
        final Point hub = foot.plus(new Point(0, 0, 0.04 * l));
        gunship.part(Piece.bar("mast", Paint.METAL, foot, hub.plus(new Point(0, 0, 0.01 * l)), 0.016 * l, 0.016 * l));
        gunship.body(Fuselage.spinner("hub", Paint.METAL, 0.03 * l, 0.04 * l).faceted(8),
                new Mount(hub.plus(new Point(0, 0, 0.012 * l)), 0, 90, 0));
        return hub;
    }
}
