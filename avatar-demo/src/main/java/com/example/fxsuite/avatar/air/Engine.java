package com.example.fxsuite.avatar.air;

import com.example.fxsuite.avatar.air.Fuselage.Station;
import com.example.fxsuite.avatar.build.Point;

import java.util.List;

/**
 * An engine, made whole in a frame of its own (its middle at the origin, its thrust ahead, +along)
 * and fixed on an aircraft as one ({@link Craft.Builder#with}): its bodies, its parts, and what
 * turns. Four kinds:
 *
 * <ul>
 *   <li><b>A radial piston engine</b>: a short round cowling, its lip banded; through its open front,
 *       dark, the ring of its nine cylinders; exhaust stubs under its flanks; a three-bladed
 *       propeller before it, on a spinner.</li>
 *   <li><b>A turboprop</b>: a long slim nacelle, drawn in to the spinner at its front and to its
 *       exhaust behind; an intake scoop under its chin; an exhaust stub either side; a four-bladed
 *       propeller on a long pointed spinner.</li>
 *   <li><b>A ducted fan</b>: a deep ring, its lip banded; in it a fan of seven blades on a hub drawn
 *       to a cone either end, held by five stators behind it.</li>
 *   <li><b>A coaxial ducted rotor</b>: a deep, faceted duct, its lip banded; two three-bladed rotors
 *       in it turning opposite ways; a hub with a nose cone and a tail cone, held by four stators; three
 *       vanes across its exit, to steer its thrust; a trunnion either side, to tilt it on.</li>
 *   <li><b>A turboshaft</b>: no propeller of its own (it turns a rotor): a housing with its edges
 *       rounded, a dark intake in its face, grilles in its flanks, panel lines round it, and two
 *       exhaust nozzles splayed out of its back.</li>
 * </ul>
 *
 * @param kind     which
 * @param length   how long, nose to tail (a ducted fan, how far across)
 * @param diameter how far across its cowling, nacelle, duct or housing
 * @param blades   how many blades its propeller or fan has (none, a turboshaft's)
 */
public record Engine(Kind kind, double length, double diameter, int blades) {

    /** The kinds of engine. */
    public enum Kind { RADIAL, TURBOPROP, DUCTED_FAN, TURBOSHAFT, COAXIAL_DUCT }

    /** How fast what turns turns, at full power, turns a second. */
    public static final double PROPELLER_RATE = 12;
    public static final double FAN_RATE = 30;

    public Engine {
        if (!(length > 0 && diameter > 0) || blades < 0) {
            throw new IllegalArgumentException(kind + ": " + length + " long, " + diameter + " across, " + blades + " blades");
        }
    }

    /** A radial piston engine {@code length} long, its cowling three quarters as wide. */
    public static Engine radial(final double length) {
        return new Engine(Kind.RADIAL, length, 0.75 * length, 3);
    }

    /** A turboprop {@code length} long, its nacelle a quarter as wide. */
    public static Engine turboprop(final double length) {
        return new Engine(Kind.TURBOPROP, length, 0.24 * length, 4);
    }

    /** A ducted fan {@code across} across its duct, half as deep. */
    public static Engine ductedFan(final double across) {
        return new Engine(Kind.DUCTED_FAN, across, across, 7);
    }

    /** A coaxial ducted rotor {@code across} across its duct, a little over half as deep. */
    public static Engine coaxialDuct(final double across) {
        return new Engine(Kind.COAXIAL_DUCT, across, across, 3);
    }

    /** A turboshaft {@code length} long, its housing a little over a third as wide. */
    public static Engine turboshaft(final double length) {
        return new Engine(Kind.TURBOSHAFT, length, 0.36 * length, 0);
    }

    /** It, whole, in its own frame. */
    public Craft build() {
        final Craft.Builder engine = Craft.named(switch (kind) {
            case RADIAL -> "Radial piston engine";
            case TURBOPROP -> "Turboprop engine";
            case DUCTED_FAN -> "Ducted fan";
            case TURBOSHAFT -> "Turboshaft engine";
            case COAXIAL_DUCT -> "Coaxial ducted rotor";
        });
        switch (kind) {
            case RADIAL -> radial(engine);
            case TURBOPROP -> turboprop(engine);
            case DUCTED_FAN -> ductedFan(engine);
            case TURBOSHAFT -> turboshaft(engine);
            case COAXIAL_DUCT -> coaxialDuct(engine);
        }
        return engine.build();
    }

    /** How far out its propeller or fan reaches from its axis; its body's, if it has none. */
    public double reach() {
        return switch (kind) {
            case RADIAL -> 1.15 * diameter;
            case TURBOPROP -> 1.35 * diameter;
            case DUCTED_FAN, TURBOSHAFT, COAXIAL_DUCT -> diameter / 2;
        };
    }

    private void radial(final Craft.Builder engine) {
        final double h = length / 2;
        final double r = diameter / 2;
        final Fuselage cowling = new Fuselage("cowling", Paint.CAR, List.of(
                Station.round(-h, 0),
                Station.round(-h + 0.02 * length, 0.28 * r),
                Station.round(-0.55 * h, 0.62 * r),
                Station.round(-0.1 * h, 0.9 * r),
                Station.round(0.35 * h, r),
                Station.round(0.8 * h, r),
                Station.round(h - 0.02 * length, 0.96 * r),
                Station.round(h - 0.001 * length, 0.86 * r),
                Station.round(h, 0)), 28);
        engine.body(cowling, Mount.at(Point.ZERO));
        engine.parts(Parts.patch("lip", Paint.TRIM, cowling, 0.62 * h, 0.78 * h, 0, 360, 0.006 * length, 1, 28),
                Mount.at(Point.ZERO));
        // its open front, dark, a hair before the cowling; the engine in it
        engine.parts(Parts.ring("face", Paint.DARK, 0, 0.85 * r, 0.004 * length, 28),
                new Mount(new Point(0, h + 0.002 * length, 0), 0, -90, 0));
        for (int k = 0; k < 9; k++) {
            // from the top round, so either side is the same
            final double a = Math.toRadians(90 + 40 * k);
            final Point out = new Point(Math.cos(a), 0, Math.sin(a));
            engine.part(Piece.bar("cylinder", Paint.METAL, out.times(0.3 * r).plus(new Point(0, h + 0.012 * length, 0)),
                    out.times(0.8 * r).plus(new Point(0, h + 0.012 * length, 0)), 0.17 * r, 0.14 * r));
        }
        // exhaust stubs under its flanks, raked back
        for (final double around : new double[] {200, 340}) {
            final Point on = cowling.surface(-0.25 * h, around);
            final double a = Math.toRadians(around);
            final Point out = new Point(Math.cos(a), 0, Math.sin(a));
            engine.part(Piece.bar("exhaust", Paint.DARK, on.plus(out.times(-0.05 * r)),
                    on.plus(out.times(0.16 * r)).plus(new Point(0, -0.14 * length, 0)), 0.12 * r, 0.12 * r));
        }
        propeller(engine, h + 0.05 * length, 1.15 * diameter, 0.24 * diameter, 30, 0.42 * diameter, 0.4 * diameter);
    }

    private void turboprop(final Craft.Builder engine) {
        final double h = length / 2;
        final double r = diameter / 2;
        final Fuselage nacelle = new Fuselage("nacelle", Paint.CAR, List.of(
                Station.round(-h, 0),
                Station.round(-h + 0.01 * length, 0.3 * r),
                Station.round(-0.75 * h, 0.58 * r),
                Station.round(-0.3 * h, 0.95 * r),
                Station.round(0.2 * h, r),
                Station.round(0.6 * h, 0.93 * r),
                Station.round(0.9 * h, 0.66 * r),
                Station.round(h - 0.001 * length, 0.5 * r),
                Station.round(h, 0)), 24);
        engine.body(nacelle, Mount.at(Point.ZERO));
        for (final double at : new double[] {-0.35 * h, 0.3 * h}) {
            engine.parts(Parts.patch("panel line", Paint.FRAME, nacelle, at, at + 0.008 * length, 0, 360, 0.002 * length,
                    1, 24), Mount.at(Point.ZERO));
        }
        // the intake scoop under its chin, dark in its mouth
        final double scoop = 0.3 * length;
        final BoxBody intake = new BoxBody("intake", Paint.CAR, scoop, 0.5 * diameter, 0.24 * diameter,
                0.05 * diameter, 0, 0.5);
        final Point under = new Point(0, 0.5 * h, -0.86 * r);
        engine.body(intake, Mount.at(under));
        engine.part(Piece.box("intake mouth", Paint.DARK, under.plus(new Point(0, scoop / 2 + 0.002 * length, 0)),
                0.38 * diameter, 0.004 * length, 0.15 * diameter));
        // an exhaust stub either side, raked back and out
        engine.pair(Piece.bar("exhaust", Paint.DARK, new Point(0.8 * r, -0.05 * h, -0.15 * r),
                new Point(1.22 * r, -0.32 * h, -0.08 * r), 0.2 * r, 0.2 * r));
        propeller(engine, h + 0.04 * diameter, 1.35 * diameter, 0.2 * diameter, 32, 0.6 * diameter, 0.52 * diameter);
    }

    private void ductedFan(final Craft.Builder engine) {
        final double outer = diameter / 2;
        final double inner = 0.86 * outer;
        final double depth = 0.5 * diameter;
        // the duct: an aerofoil in section swept round, its lip rounded at the front, thickest a third
        // back, thin at its trailing edge; out over its outside, back in through its inside
        final double h = depth / 2;
        final double thick = outer - inner;
        engine.skin(Skin.revolved("duct", Paint.CAR, new double[][] {
                {h, inner + 0.45 * thick}, {0.94 * h, outer - 0.12 * thick}, {0.7 * h, outer - 0.01 * thick},
                {0.2 * h, outer}, {-0.4 * h, outer - 0.15 * thick}, {-0.85 * h, outer - 0.45 * thick},
                {-h, inner + 0.3 * thick}, {-0.85 * h, inner + 0.08 * thick}, {-0.3 * h, inner},
                {0.5 * h, inner}, {0.9 * h, inner + 0.12 * thick}}, 40), Mount.at(Point.ZERO));
        // its lip, banded: a skin a hair over the duct's, round its leading edge
        final double over = 0.012 * outer;
        engine.skin(Skin.revolved("lip", Paint.TRIM, new double[][] {
                {0.62 * h, outer - 0.03 * thick + over}, {0.94 * h, outer - 0.12 * thick + over},
                {h + over, inner + 0.45 * thick}, {0.9 * h, inner + 0.12 * thick - over}, {0.62 * h, inner - over},
                {0.62 * h, inner + 0.5 * thick}}, 40), Mount.at(Point.ZERO));
        // the hub: a cone ahead of the fan, flush with the duct's front; a longer one behind it, to its tail
        final double fan = 0.05 * depth;
        engine.body(Fuselage.spinner("hub", Paint.METAL, 0.2 * depth, 0.36 * outer), Mount.at(0, fan + 0.1 * depth, 0));
        engine.body(Fuselage.spinner("tail cone", Paint.METAL, 0.6 * depth, 0.36 * outer),
                new Mount(new Point(0, fan - 0.3 * depth, 0), 180, 0, 0));
        // five stators behind the fan, from the hub out to the duct
        for (int k = 0; k < 5; k++) {
            final double a = Math.toRadians(90 + 72 * k);
            final Point out = new Point(Math.cos(a), 0, Math.sin(a));
            final Point at = new Point(0, -0.22 * depth, 0);
            engine.part(Parts.strut("stator", Paint.METAL, at.plus(out.times(0.15 * outer)), at.plus(out.times(inner + 0.02 * outer)),
                    0.22 * depth));
        }
        engine.spinner("fan", Parts.propeller("fan", Paint.DARK, Paint.METAL, blades, 0.97 * inner, 0.42 * outer, 38),
                List.of(), new Mount(new Point(0, fan, 0), 0, -90, 0), FAN_RATE);
    }

    private void coaxialDuct(final Craft.Builder engine) {
        final double outer = diameter / 2;
        final double inner = 0.88 * outer;
        final double depth = 0.56 * diameter;
        final double h = depth / 2;
        final double thick = outer - inner;
        // the duct: deep, blunt at its lip, faceted
        engine.skin(Skin.revolved("duct", Paint.CAR, new double[][] {
                {h, inner + 0.5 * thick}, {0.9 * h, outer - 0.05 * thick}, {0.5 * h, outer}, {-0.6 * h, outer},
                {-h, outer - 0.3 * thick}, {-h, inner + 0.2 * thick}, {-0.6 * h, inner}, {0.6 * h, inner},
                {0.9 * h, inner + 0.08 * thick}}, 16, true), Mount.at(Point.ZERO));
        final double over = 0.012 * outer;
        engine.skin(Skin.revolved("lip", Paint.TRIM, new double[][] {
                {0.68 * h, outer + over}, {0.9 * h, outer - 0.05 * thick + over}, {h + over, inner + 0.5 * thick},
                {0.9 * h, inner + 0.08 * thick - over}, {0.68 * h, inner - over}, {0.68 * h, inner + 0.5 * thick}}, 16, true),
                Mount.at(Point.ZERO));
        // two rotors, one over the other, turning opposite ways
        final double upper = 0.3 * h;
        final double lower = -0.15 * h;
        engine.spinner("upper rotor", Parts.rotor("upper rotor", Paint.DARK, Paint.METAL, blades, 0.96 * inner,
                0.17 * outer, 14, 0), List.of(), new Mount(new Point(0, upper, 0), 0, -90, 0), PROPELLER_RATE);
        engine.spinner("lower rotor", Parts.rotor("lower rotor", Paint.DARK, Paint.METAL, blades, 0.96 * inner,
                0.17 * outer, -14, 0), List.of(), new Mount(new Point(0, lower, 0), 0, -90, 0), -PROPELLER_RATE);
        // the hub: a cone ahead of the rotors, a longer one behind, held by four stators
        engine.body(Fuselage.spinner("hub", Paint.METAL, 0.3 * depth, 0.3 * outer), Mount.at(0, upper + 0.15 * depth, 0));
        engine.body(Fuselage.spinner("tail cone", Paint.METAL, 0.65 * depth, 0.3 * outer),
                new Mount(new Point(0, lower - 0.3 * depth, 0), 180, 0, 0));
        for (int k = 0; k < 4; k++) {
            final double a = Math.toRadians(45 + 90 * k);
            final Point out = new Point(Math.cos(a), 0, Math.sin(a));
            final Point at = new Point(0, -0.5 * h, 0);
            engine.part(Parts.strut("stator", Paint.METAL, at.plus(out.times(0.12 * outer)),
                    at.plus(out.times(inner + 0.02 * outer)), 0.2 * depth));
        }
        // three vanes across its exit, level, to steer its thrust
        for (final double z : new double[] {-0.5 * inner, 0, 0.5 * inner}) {
            final double half = Math.sqrt(inner * inner - z * z) * 0.98;
            engine.part(Piece.box("vane", Paint.METAL, new Point(0, -0.85 * h, z), 2 * half, 0.14 * depth, 0.025 * depth));
        }
        // a trunnion either side, to tilt it on
        engine.bodies(new Fuselage("trunnion", Paint.METAL, List.of(Station.round(-0.07 * diameter, 0),
                Station.round(-0.069 * diameter, 0.07 * diameter), Station.round(0.069 * diameter, 0.07 * diameter),
                Station.round(0.07 * diameter, 0)), 10).faceted(10), new Mount(new Point(outer + 0.05 * diameter, 0, 0), 90, 0,
                0));
    }

    private void turboshaft(final Craft.Builder engine) {
        final double w = diameter;
        final double ht = 0.84 * diameter;
        final BoxBody housing = new BoxBody("housing", Paint.CAR, length, w, ht, 0.16 * Math.min(w, ht), 0.12, 0.3);
        engine.body(housing, Mount.at(Point.ZERO));
        // its intake, dark in its face; grilles in its flanks; panel lines round it
        engine.part(Piece.box("intake", Paint.DARK, new Point(0, length / 2 + 0.002 * length, 0), 0.5 * w,
                0.004 * length, 0.4 * ht));
        engine.pair(Parts.patch("grille", Paint.DARK, housing, -0.05 * length, 0.2 * length, -14, 14, 0.004 * length, 3, 1),
                Mount.at(Point.ZERO));
        for (final double at : new double[] {-0.28 * length, 0.3 * length}) {
            engine.parts(Parts.patch("panel line", Paint.FRAME, housing, at, at + 0.012 * length, 0, 360, 0.003 * length,
                    1, 24), Mount.at(Point.ZERO));
        }
        // two exhaust nozzles splayed out of its back, a little up
        engine.withPair(Craft.named("exhaust").body(Fuselage.nacelle("exhaust", Paint.METAL, 0.24 * length, 0.3 * ht),
                Mount.at(Point.ZERO)).build(), new Mount(new Point(0.3 * w, -0.4 * length, 0.08 * ht), 145, 18, 0));
    }

    /**
     * A propeller {@code radius} out turning about the axis {@code at} ahead of the middle, its blades
     * {@code chord} at their broadest pitched {@code pitch} at the root; and a spinner over its hub,
     * {@code spinnerLength} long and {@code spinnerDiameter} across.
     */
    private void propeller(final Craft.Builder engine, final double at, final double radius, final double chord,
                           final double pitch, final double spinnerLength, final double spinnerDiameter) {
        engine.spinner("propeller", Parts.propeller("propeller", Paint.DARK, Paint.METAL, blades, radius, chord, pitch),
                List.of(), new Mount(new Point(0, at, 0), 0, -90, 0), PROPELLER_RATE);
        engine.body(Fuselage.spinner("spinner", Paint.TRIM, spinnerLength, spinnerDiameter),
                Mount.at(0, at - 0.05 * spinnerLength + spinnerLength / 2, 0));
    }
}
