package com.example.fxsuite.avatar.air;

import com.example.fxsuite.avatar.air.Fuselage.Station;
import com.example.fxsuite.avatar.build.Point;

import java.util.List;

/**
 * What an armed aircraft carries, each made whole in a frame of its own (its middle at the origin,
 * its business end ahead, +along) and fixed on as one ({@link Craft.Builder#with}): a rocket pod, a
 * rotary cannon, a missile on its rail, a chin turret. Angular, faceted, as a military machine's are.
 */
public final class Weapons {

    private Weapons() {
    }

    /** How fast a rotary cannon's barrels turn, firing, turns a second. */
    public static final double CANNON_RATE = 15;

    /**
     * A rocket pod {@code length} long, a little over a quarter as wide: a faceted tube, a warning
     * band round it, its front a dark face with seven tubes' mouths in it, a lug on its back to hang it by.
     */
    public static Craft rocketPod(final double length) {
        final double h = length / 2;
        final double r = 0.14 * length;
        final Fuselage tube = new Fuselage("rocket pod", Paint.CAR, List.of(
                Station.round(-h, 0),
                Station.round(-h + 0.01 * length, 0.55 * r),
                Station.round(-0.85 * h, 0.9 * r),
                Station.round(-0.7 * h, r),
                Station.round(0.85 * h, r),
                Station.round(h - 0.002 * length, 0.96 * r),
                Station.round(h, 0)), 12).faceted(12);
        final Craft.Builder pod = Craft.named("Rocket pod").body(tube, Mount.at(Point.ZERO));
        pod.parts(Parts.patch("band", Paint.TRIM, tube, 0.45 * h, 0.55 * h, 0, 360, 0.01 * length, 1, 12),
                Mount.at(Point.ZERO));
        // its face, dark, and the tubes' mouths in it: one in its middle, six round it
        pod.parts(Parts.ring("face", Paint.DARK, 0, 0.9 * r, 0.004 * length, 12),
                new Mount(new Point(0, h + 0.002 * length, 0), 0, -90, 0));
        for (int k = -1; k < 6; k++) {
            final double a = Math.toRadians(90 + 60 * k);
            final Point at = k < 0 ? Point.ZERO : new Point(0.56 * r * Math.cos(a), 0, 0.56 * r * Math.sin(a));
            pod.parts(Parts.ring("tube", Paint.METAL, 0.13 * r, 0.05 * r, 0.012 * length, 8),
                    new Mount(at.plus(new Point(0, h + 0.004 * length, 0)), 0, -90, 0));
        }
        pod.part(Piece.box("lug", Paint.METAL, new Point(0, 0, r + 0.02 * length), 0.12 * r, 0.25 * length,
                0.05 * length));
        return pod.build();
    }

    /**
     * A rotary cannon {@code length} long: a faceted housing behind, and ahead of it six barrels in a
     * ring, held by two clamps, turning together ({@link #CANNON_RATE}).
     */
    public static Craft rotaryCannon(final double length) {
        final double rb = 0.045 * length;
        final Fuselage housing = new Fuselage("housing", Paint.METAL, List.of(
                Station.round(-0.45 * length, 0),
                Station.round(-0.44 * length, 0.09 * length),
                Station.round(-0.1 * length, 0.1 * length),
                Station.round(-0.01 * length, 0.08 * length),
                Station.round(0, 0)), 10).faceted(10);
        final Craft.Builder cannon = Craft.named("Rotary cannon").body(housing, Mount.at(Point.ZERO));
        // the barrels, made about their axis (+up) and turned to face ahead
        final List<Piece> barrels = new java.util.ArrayList<>();
        for (int k = 0; k < 6; k++) {
            final double a = 2 * Math.PI * k / 6;
            final double x = rb * Math.cos(a);
            final double y = rb * Math.sin(a);
            barrels.add(Piece.bar("barrel", Paint.DARK, new Point(x, y, -0.05 * length), new Point(x, y, 0.55 * length),
                    0.024 * length, 0.024 * length));
        }
        for (final double at : new double[] {0.25 * length, 0.5 * length}) {
            Parts.ring("clamp", Paint.METAL, rb - 0.016 * length, 0.032 * length, 0.03 * length, 12)
                    .forEach(p -> barrels.add(p.placed(Mount.at(0, 0, at))));
        }
        cannon.spinner("barrels", barrels, List.of(), new Mount(Point.ZERO, 0, -90, 0), CANNON_RATE);
        return cannon.build();
    }

    /**
     * A missile {@code length} long, under a short rail: a slim body, its nose drawn to a point and
     * banded, four fins at its tail and four smaller ones (canards) toward its nose.
     */
    public static Craft missile(final double length) {
        final double h = length / 2;
        final double r = 0.045 * length;
        final Fuselage body = new Fuselage("missile", Paint.WING, List.of(
                Station.round(-h, 0),
                Station.round(-h + 0.002 * length, 0.85 * r),
                Station.round(-0.45 * h, r),
                Station.round(0.55 * h, r),
                Station.round(0.75 * h, 0.85 * r),
                Station.round(0.9 * h, 0.5 * r),
                Station.round(h, 0)), 12).faceted(12);
        final Craft.Builder missile = Craft.named("Missile").body(body, Mount.at(Point.ZERO));
        missile.parts(Parts.patch("band", Paint.TRIM, body, 0.5 * h, 0.58 * h, 0, 360, 0.002 * length, 1, 12),
                Mount.at(Point.ZERO));
        final List<Piece> fin = Parts.wing("fin", Paint.WING, 0.08 * length, 0.14 * length, 0.05 * length, 45, 0, 0.06);
        final List<Piece> canard = Parts.wing("canard", Paint.WING, 0.04 * length, 0.07 * length, 0.03 * length, 40, 0,
                0.06);
        for (final double[] at : new double[][] {{-0.72 * h, 0}, {0.42 * h, 1}}) {
            final List<Piece> pieces = at[1] == 0 ? fin : canard;
            missile.pair(pieces, Mount.at(0.9 * r, at[0], 0));
            missile.parts(pieces, new Mount(new Point(0, at[0], -0.9 * r), 0, 0, 90));
        }
        // the rail over it, and the shoe it hangs from
        missile.part(Piece.box("rail", Paint.METAL, new Point(0, 0, r + 0.012 * length), 0.035 * length, 0.7 * length,
                0.02 * length));
        missile.part(Piece.box("shoe", Paint.METAL, new Point(0, 0.05 * length, r + 0.035 * length), 0.06 * length,
                0.3 * length, 0.03 * length));
        return missile.build();
    }

    /**
     * A chin turret, its ball {@code size} across: a plate to fix it under a nose by; under it a faceted
     * ball that traverses ({@code "turret traverse"}, 120° either way), and in the ball a rotary cannon
     * that is raised or lowered ({@code "turret elevation"}, 60° down to 15° up). Hinges on hinges: the
     * gun goes round with the ball.
     */
    public static Craft chinTurret(final double size) {
        final double r = size / 2;
        final Fuselage ball = new Fuselage("ball", Paint.CAR, Fuselage.profile(size, r, 10,
                t -> Math.sqrt(Math.max(0, 1 - (2 * t - 1) * (2 * t - 1))), t -> 0, t -> 1, 2), 12).faceted(12);
        final Craft gun = Craft.named("gun").with(rotaryCannon(1.4 * size), Mount.at(0, 0.62 * size, 0)).build()
                .hinged("turret elevation", Point.ZERO, new Point(1, 0, 0), -60, 15);
        final Craft turret = Craft.named("turret").body(ball, Mount.at(Point.ZERO)).with(gun, Mount.at(Point.ZERO)).build()
                .hinged("turret traverse", Point.ZERO, new Point(0, 0, -1), -120, 120);
        return Craft.named("Chin turret")
                .part(Piece.box("plate", Paint.METAL, new Point(0, 0, 0.56 * size), 0.95 * size, 0.95 * size, 0.08 * size))
                .part(Piece.box("collar", Paint.METAL, new Point(0, 0, 0.47 * size), 0.6 * size, 0.6 * size, 0.12 * size))
                .with(turret, Mount.at(Point.ZERO)).build();
    }
}
