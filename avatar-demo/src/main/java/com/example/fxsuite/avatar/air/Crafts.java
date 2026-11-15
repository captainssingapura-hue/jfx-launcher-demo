package com.example.fxsuite.avatar.air;

import com.example.fxsuite.avatar.build.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * Aircraft made of the two kinds of body ({@link Fuselage}, {@link BoxBody}) and the parts they
 * carry ({@link Parts}), each to one size: its length (or a drone's span). An airship is both: an
 * envelope, and a gondola hung under it.
 */
public final class Crafts {

    private Crafts() {
    }

    /** The kinds made, each with its usual size and how far it may be made smaller or bigger, in metres. */
    public enum Kind {
        AIRSHIP("Airship", 24, 12, 60, Category.AIRCRAFT),
        BOMBER("Heavy bomber airship", 70, 30, 150, Category.AIRCRAFT),
        QUADCOPTER("Quadcopter drone", 1.2, 0.4, 6, Category.AIRCRAFT),
        HELICOPTER("Helicopter", 12, 6, 24, Category.AIRCRAFT),
        TANDEM("Tandem-rotor helicopter", 15, 8, 30, Category.AIRCRAFT),
        LIGHT_PLANE("Light aeroplane", 8, 5, 20, Category.AIRCRAFT),
        /** Engines on their own ({@link Engine}): a radial and a turboprop by their length, a duct by its width. */
        RADIAL_ENGINE("Radial piston engine", 1.6, 0.4, 6, Category.ENGINE),
        TURBOPROP("Turboprop engine", 4, 1, 10, Category.ENGINE),
        DUCTED_FAN("Ducted fan", 1.8, 0.3, 8, Category.ENGINE),
        TURBOSHAFT("Turboshaft engine", 2.4, 0.6, 6, Category.ENGINE),
        COAXIAL_DUCT("Coaxial ducted rotor", 3, 1, 8, Category.ENGINE),
        /** Weapons on their own ({@link Weapons}), by their length; a turret by its ball. */
        ROCKET_POD("Rocket pod", 1.8, 0.6, 4, Category.WEAPON),
        ROTARY_CANNON("Rotary cannon", 1.6, 0.5, 4, Category.WEAPON),
        MISSILE("Missile on its rail", 1.8, 0.6, 5, Category.WEAPON),
        CHIN_TURRET("Chin turret", 1, 0.4, 3, Category.WEAPON),
        /** Bodies on their own, angular, by their length. */
        FACETED_FUSELAGE("Faceted fuselage", 12, 4, 30, Category.BODY),
        CHAMFERED_BOX("Chamfered box body", 4, 1, 20, Category.BODY),
        CANOPY("Framed canopy", 3, 1, 8, Category.BODY);

        private final String title;
        private final double usual;
        private final double least;
        private final double most;
        private final Category category;

        Kind(final String title, final double usual, final double least, final double most, final Category category) {
            this.title = title;
            this.usual = usual;
            this.least = least;
            this.most = most;
            this.category = category;
        }

        /** What it is: a whole aircraft, or a part of one on its own. */
        public Category category() {
            return category;
        }

        public String title() {
            return title;
        }

        /** Its usual size, in metres; and the least and most. */
        public double usual() {
            return usual;
        }

        public double least() {
            return least;
        }

        public double most() {
            return most;
        }

        /** Whether it is an engine on its own. */
        public boolean engine() {
            return category == Category.ENGINE;
        }

        /** It, {@code metres} long (a drone, a ducted fan, across). */
        public Craft build(final double metres) {
            if (metres < least || metres > most) {
                throw new IllegalArgumentException(title + " " + metres + " m: from " + least + " to " + most);
            }
            final double size = metres * 100;
            return switch (this) {
                case AIRSHIP -> airship(size);
                case BOMBER -> bomber(size);
                case QUADCOPTER -> quadcopter(size);
                case HELICOPTER -> helicopter(size);
                case TANDEM -> tandem(size);
                case LIGHT_PLANE -> lightPlane(size);
                case RADIAL_ENGINE -> Engine.radial(size).build();
                case TURBOPROP -> Engine.turboprop(size).build();
                case DUCTED_FAN -> Engine.ductedFan(size).build();
                case TURBOSHAFT -> Engine.turboshaft(size).build();
                case COAXIAL_DUCT -> Engine.coaxialDuct(size).build();
                case ROCKET_POD -> Weapons.rocketPod(size);
                case ROTARY_CANNON -> Weapons.rotaryCannon(size);
                case MISSILE -> Weapons.missile(size);
                case CHIN_TURRET -> Weapons.chinTurret(size);
                case FACETED_FUSELAGE -> facetedFuselage(size);
                case CHAMFERED_BOX -> chamferedBox(size);
                case CANOPY -> canopy(size);
            };
        }
    }

    /** What a kind is: a whole aircraft, or one of its parts on its own. */
    public enum Category { AIRCRAFT, ENGINE, WEAPON, BODY }

    // ---------------------------------------------------------------- bodies on their own

    /** An aeroplane's fuselage {@code length} long, faceted: eight flat sides round, angular. */
    public static Craft facetedFuselage(final double length) {
        return Craft.named("Faceted fuselage")
                .body(Fuselage.aircraft("fuselage", Paint.SKIN, length, 0.15 * length).faceted(8), Mount.at(Point.ZERO))
                .build();
    }

    /** A box body {@code length} long, its edges and corners cut flat, drawn in toward its nose. */
    public static Craft chamferedBox(final double length) {
        return Craft.named("Chamfered box body").body(new BoxBody("box", Paint.SKIN, length, 0.4 * length, 0.3 * length,
                0.06 * length, 0.3, 0.1).withChamfers(), Mount.at(Point.ZERO)).build();
    }

    /**
     * A canopy {@code length} long, framed: faceted glass, a bow across it at three places and a spine
     * down its top; in a block up to its sill, as it would sit in a body.
     */
    public static Craft canopy(final double length) {
        final Fuselage glass = Fuselage.canopy("canopy", length, 0.42 * length, 0.3 * length, 10);
        final Craft.Builder canopy = Craft.named("Framed canopy").body(glass, Mount.at(Point.ZERO));
        for (final double at : new double[] {-0.15 * length, 0.12 * length, 0.3 * length}) {
            canopy.parts(Parts.patch("bow", Paint.FRAME, glass, at, at + 0.02 * length, 0, 180, 0.006 * length, 1, 8),
                    Mount.at(Point.ZERO));
        }
        canopy.parts(Parts.patch("spine", Paint.FRAME, glass, -0.45 * length, 0.45 * length, 86, 94, 0.006 * length, 10,
                1), Mount.at(Point.ZERO));
        // the body it sits in, up to its sill: hiding its lower half, as a body does
        canopy.part(Piece.box("sill", Paint.CAR, new Point(0, 0, -0.15 * length), 0.46 * length, 1.04 * length,
                0.3 * length));
        return canopy.build();
    }

    // ---------------------------------------------------------------- airships

    /**
     * An airship {@code length} long: a silver envelope ringed by its frames, a cross of fins at its
     * tail; a gondola under it on struts, windows down its sides and round its nose; and an engine
     * ducted fan either side on an outrigger.
     */
    public static Craft airship(final double length) {
        final double d = length / 4.6;
        final Fuselage envelope = Fuselage.envelope("envelope", Paint.SKIN, length, d);
        final Craft.Builder craft = Craft.named("Airship").body(envelope, Mount.at(Point.ZERO));
        frames(craft, envelope, length, 4);
        fins(craft, envelope, length, 0.11 * length, 0.17 * length, 0.08 * length);
        // the gondola, hung under its front half
        final double gl = 0.28 * length;
        final double gw = 0.26 * d;
        final double gh = 0.24 * d;
        final BoxBody gondola = new BoxBody("gondola", Paint.CAR, gl, gw, gh, Math.min(gw, gh) * 0.22, 0.3, 0.4);
        final double gAlong = 0.1 * length;
        final double gUp = -(d / 2 + gh / 2 + 0.06 * d);
        final Mount gMount = Mount.at(0, gAlong, gUp);
        craft.body(gondola, gMount);
        windows(craft, gondola, gMount, 7, 0.5);
        hangers(craft, envelope, gondola, gMount, gl, 0.025 * d);
        // a ducted fan either side, on an outrigger from the gondola's after end
        final Engine fan = Engine.ductedFan(0.075 * length);
        final double out = gw / 2 + 0.06 * length + fan.diameter() / 2;
        final Point fanAt = new Point(out, gAlong - 0.3 * gl, gUp + 0.1 * gh);
        craft.withPair(fan.build(), Mount.at(fanAt));
        craft.pair(Parts.strut("outrigger", Paint.METAL, gondola.surface(-0.3 * gl, 0).plus(gMount.at()),
                fanAt.plus(new Point(-0.43 * fan.diameter(), 0, 0)), 0.035 * length));
        return craft.build();
    }

    /**
     * A heavy bomber airship, {@code length} long: a dark, fatter envelope, red bands round its nose;
     * a long armoured gondola, its bridge glazed round its nose and a bomb bay in its belly; six
     * engines on pylons down its lower flanks; a gun blister on its back and another under its tail.
     */
    public static Craft bomber(final double length) {
        final double d = length / 4;
        final Fuselage envelope = Fuselage.envelope("envelope", Paint.SKIN, length, d);
        final Craft.Builder craft = Craft.named("Heavy bomber airship").body(envelope, Mount.at(Point.ZERO));
        frames(craft, envelope, length, 6);
        // red bands round its nose, its marks
        for (final double at : new double[] {0.3 * length, 0.33 * length}) {
            craft.parts(Parts.patch("band", Paint.TRIM, envelope, at, at + 0.012 * length, 0, 360, 0.002 * d, 1, 28),
                    Mount.at(Point.ZERO));
        }
        fins(craft, envelope, length, 0.1 * length, 0.15 * length, 0.07 * length);
        // the gondola: long, low, armoured
        final double gl = 0.42 * length;
        final double gw = 0.24 * d;
        final double gh = 0.17 * d;
        final BoxBody gondola = new BoxBody("gondola", Paint.CAR, gl, gw, gh, Math.min(gw, gh) * 0.2, 0.35, 0.3);
        final double gAlong = 0.08 * length;
        final double gUp = -(d / 2 + gh / 2 - 0.02 * d);
        final Mount gMount = Mount.at(0, gAlong, gUp);
        craft.body(gondola, gMount);
        windows(craft, gondola, gMount, 5, 0.32);
        // its armour: plates down its sides; its bomb bay's doors in its belly
        craft.parts(Parts.patch("armour", Paint.FRAME, gondola, -0.3 * gl, 0.1 * gl, -25, 25, 0.004 * d, 6, 2), gMount);
        craft.parts(Parts.patch("armour", Paint.FRAME, gondola, -0.3 * gl, 0.1 * gl, 155, 205, 0.004 * d, 6, 2), gMount);
        craft.parts(Parts.patch("bomb bay", Paint.DARK, gondola, -0.32 * gl, 0.05 * gl, 250, 290, 0.002 * d, 4, 2),
                gMount);
        hangers(craft, envelope, gondola, gMount, gl, 0.02 * d);
        // six radial engines: three either side, on pylons from the envelope's lower flanks
        final Engine radial = Engine.radial(0.05 * length);
        for (int k = 0; k < 3; k++) {
            final Point flank = envelope.surface((0.22 - 0.2 * k) * length, 330);
            final Point engineAt = flank.plus(new Point(0.18 * d, 0, -0.12 * d));
            craft.withPair(radial.build(), Mount.at(engineAt));
            craft.pair(Parts.strut("pylon", Paint.METAL, engineAt.plus(new Point(-0.3 * radial.diameter(), 0,
                    0.2 * radial.diameter())), flank, 0.4 * radial.length()));
        }
        // a gun blister on its back, and one under its tail
        final Fuselage blister = Fuselage.spinner("blister", Paint.CAR, 0.08 * d, 0.1 * d);
        craft.body(blister, new Mount(envelope.surface(0.15 * length, 90).plus(new Point(0, 0, -0.01 * d)), 0, 90, 0));
        craft.body(blister, new Mount(envelope.surface(-0.25 * length, 270).plus(new Point(0, 0, 0.01 * d)), 0, -90, 0));
        return craft.build();
    }

    /** Frame bands round an envelope, {@code count} of them evenly down its length, standing a little proud of it. */
    private static void frames(final Craft.Builder craft, final Fuselage envelope, final double length, final int count) {
        final double d = envelope.section(0).halfWidth() * 2;
        for (int k = 0; k < count; k++) {
            final double at = -0.36 * length + 0.62 * length * k / (count - 1);
            craft.parts(Parts.patch("frame", Paint.FRAME, envelope, at - 0.004 * length, at + 0.004 * length, 0, 360,
                    0.006 * d, 1, 28), Mount.at(Point.ZERO));
        }
        // and a keel down its belly, from its nose to its tail
        craft.parts(Parts.patch("keel", Paint.FRAME, envelope, -0.42 * length, 0.44 * length, 265, 275, 0.008 * d, 14, 1),
                Mount.at(Point.ZERO));
    }

    /** A cross of fins at an envelope's tail: up, down, and either side; their roots sunk into it. */
    private static void fins(final Craft.Builder craft, final Fuselage envelope, final double length, final double span,
                             final double root, final double tip) {
        final double lead = -0.3 * length;
        // sunk into the envelope as far as it has narrowed by the root's after end
        final double sunk = envelope.section(lead - root).halfWidth() * 0.92;
        final List<Piece> fin = Parts.wing("fin", Paint.WING, span + envelope.section(lead).halfWidth() - sunk, root,
                tip, 38, 0, 0.1);
        craft.pair(fin, Mount.at(sunk, lead, 0));
        craft.parts(fin, new Mount(new Point(0, lead, sunk), 0, 0, -90));
        craft.parts(fin, new Mount(new Point(0, lead, -sunk), 0, 0, 90));
    }

    /** Windows down a gondola's sides and round its nose. */
    private static void windows(final Craft.Builder craft, final BoxBody gondola, final Mount mount, final int count,
                                final double share) {
        final double gl = gondola.length();
        final double proud = gondola.height() * 0.02;
        for (int k = 0; k < count; k++) {
            final double at = gl * (0.3 - share * k / count);
            craft.parts(Parts.patch("window", Paint.GLASS, gondola, at - gl * 0.025, at + gl * 0.025, -12, 18, proud, 1, 2),
                    mount);
            craft.parts(Parts.patch("window", Paint.GLASS, gondola, at - gl * 0.025, at + gl * 0.025, 162, 192, proud, 1, 2),
                    mount);
        }
        // the bridge: glazed round the nose
        craft.parts(Parts.patch("bridge", Paint.GLASS, gondola, gl * 0.38, gl * 0.47, -20, 200, proud, 2, 10), mount);
    }

    /** Struts from a gondola's top up to the envelope: two pairs, splayed. */
    private static void hangers(final Craft.Builder craft, final Fuselage envelope, final BoxBody gondola,
                                final Mount mount, final double gl, final double thick) {
        for (final double share : new double[] {-0.3, 0.25}) {
            final Point foot = gondola.surface(share * gl, 60).plus(mount.at());
            final Point head = envelope.surface(share * gl + mount.at().along(), 300);
            craft.pair(Parts.strut("hanger", Paint.METAL, foot, head.plus(new Point(0, 0, thick)), 3 * thick));
        }
    }

    // ---------------------------------------------------------------- rotorcraft and the rest

    /**
     * A quadcopter {@code span} across its rotors: a box body, four arms out to its corners, a motor
     * on the end of each turning a two-bladed rotor; two legs and skids under it; a camera under its nose.
     */
    public static Craft quadcopter(final double span) {
        final double s = span;
        final BoxBody body = new BoxBody("body", Paint.SKIN, 0.36 * s, 0.24 * s, 0.1 * s, 0.03 * s, 0.15, 0.1);
        final Craft.Builder craft = Craft.named("Quadcopter drone").body(body, Mount.at(Point.ZERO));
        // a stripe down its back, its lights
        craft.parts(Parts.patch("stripe", Paint.TRIM, body, -0.12 * s, 0.12 * s, 80, 100, 0.002 * s, 3, 1),
                Mount.at(Point.ZERO));
        final double out = 0.3 * s;
        for (final double along : new double[] {out, -out}) {
            final Point motor = new Point(out, along, 0.01 * s);
            craft.pair(Piece.bar("arm", Paint.CAR, new Point(0.06 * s, along * 0.3, 0), motor, 0.035 * s, 0.025 * s));
            craft.bodies(Fuselage.nacelle("motor", Paint.METAL, 0.07 * s, 0.05 * s), new Mount(motor, 0, 90, 0));
            craft.spinners("rotor", Parts.rotor("rotor", Paint.DARK, Paint.METAL, 2, 0.2 * s, 0.035 * s, 12, 0),
                    List.of(), Mount.at(motor.plus(new Point(0, 0, 0.04 * s))), along > 0 ? 40 : -40);
        }
        // legs and skids
        for (final double along : new double[] {0.1 * s, -0.1 * s}) {
            craft.pair(Piece.bar("leg", Paint.METAL, new Point(0.07 * s, along, -0.04 * s),
                    new Point(0.12 * s, along, -0.16 * s), 0.012 * s, 0.012 * s));
        }
        craft.pair(Piece.bar("skid", Paint.METAL, new Point(0.12 * s, -0.16 * s, -0.16 * s),
                new Point(0.12 * s, 0.16 * s, -0.16 * s), 0.014 * s, 0.014 * s));
        // a camera, a ball under its nose
        craft.body(Fuselage.spinner("camera", Paint.DARK, 0.06 * s, 0.07 * s), new Mount(new Point(0, 0.14 * s, -0.06 * s),
                0, -90, 0));
        return craft.build();
    }

    /**
     * A helicopter {@code length} long: a pod and boom, glazed round its nose; its engine housed on
     * its back behind the mast of a four-bladed main rotor; a fin, a tailplane and a tail rotor at
     * the boom's end; on skids.
     */
    public static Craft helicopter(final double length) {
        final double w = 0.2 * length;
        final double h = 0.21 * length;
        final Fuselage pod = Fuselage.pod("pod", Paint.SKIN, length, w, h);
        final Craft.Builder craft = Craft.named("Helicopter").body(pod, Mount.at(Point.ZERO));
        // the canopy, and the cabin's side windows
        craft.parts(Parts.patch("canopy", Paint.GLASS, pod, 0.3 * length, 0.47 * length, 15, 165, 0.004 * length, 4, 8),
                Mount.at(Point.ZERO));
        craft.parts(Parts.patch("window", Paint.GLASS, pod, 0.14 * length, 0.26 * length, -5, 35, 0.004 * length, 2, 2),
                Mount.at(Point.ZERO));
        craft.parts(Parts.patch("window", Paint.GLASS, pod, 0.14 * length, 0.26 * length, 145, 185, 0.004 * length, 2, 2),
                Mount.at(Point.ZERO));
        craft.parts(Parts.patch("stripe", Paint.TRIM, pod, -0.05 * length, 0.3 * length, 340, 352, 0.003 * length, 6, 1),
                Mount.at(Point.ZERO));
        craft.parts(Parts.patch("stripe", Paint.TRIM, pod, -0.05 * length, 0.3 * length, 188, 200, 0.003 * length, 6, 1),
                Mount.at(Point.ZERO));
        // its turboshaft on its back, and the rotor's mast up through it, the hub clear over it
        final Point top = pod.surface(0.12 * length, 90);
        final Engine shaft = Engine.turboshaft(0.3 * length);
        craft.with(shaft.build(), Mount.at(top.plus(new Point(0, -0.06 * length, 0.4 * shaft.diameter()))));
        final Point hub = top.plus(new Point(0, 0.02 * length, 0.9 * shaft.diameter() + 0.02 * length));
        craft.part(Piece.bar("mast", Paint.METAL, top, hub, 0.05 * w, 0.05 * w));
        craft.spinner("main rotor", Parts.rotor("main rotor", Paint.DARK, Paint.METAL, 4, 0.46 * length, 0.03 * length,
                10, 1.5), List.of(), Mount.at(hub), 5);
        // at the boom's end: a fin, a tailplane, a tail rotor on its right
        final double tail = -0.44 * length;
        final Point boom = pod.surface(tail, 90);
        final double boomR = pod.section(tail).halfWidth();
        craft.parts(Parts.wing("fin", Paint.WING, 0.14 * length, 0.1 * length, 0.06 * length, 35, 0, 0.12),
                new Mount(boom.plus(new Point(0, 0.03 * length, -0.3 * boomR)), 0, 0, -90));
        craft.pair(Parts.wing("tailplane", Paint.WING, 0.08 * length, 0.06 * length, 0.04 * length, 10, 0, 0.12),
                Mount.at(new Point(boomR * 0.7, tail + 0.1 * length, pod.section(tail + 0.1 * length).up())));
        craft.spinner("tail rotor", Parts.rotor("tail rotor", Paint.DARK, Paint.METAL, 2, 0.09 * length, 0.012 * length,
                10, 0), List.of(), new Mount(boom.plus(new Point(boomR * 1.6, 0.0, 0.08 * length)), 0, 0, 90), 25);
        // skids: two tubes, each on two struts from its belly
        for (final double along : new double[] {0.22 * length, 0.02 * length}) {
            final Point belly = pod.surface(along, 300);
            craft.pair(Piece.bar("strut", Paint.METAL, belly, new Point(0.45 * w, along, -h / 2 - 0.06 * length),
                    0.01 * length, 0.01 * length));
        }
        craft.pair(Piece.bar("skid", Paint.METAL, new Point(0.45 * w, -0.04 * length, -h / 2 - 0.06 * length),
                new Point(0.45 * w, 0.3 * length, -h / 2 - 0.05 * length), 0.012 * length, 0.012 * length));
        return craft.build();
    }

    /**
     * A tandem-rotor helicopter {@code length} long: a box body, square-sided, its nose drawn in and
     * glazed; a short pylon at its nose and a tall one at its tail, each with a three-bladed rotor;
     * two engines on the rear pylon's flanks; on four wheels.
     */
    public static Craft tandem(final double length) {
        final double w = 0.2 * length;
        final double h = 0.22 * length;
        final BoxBody body = new BoxBody("body", Paint.SKIN, length, w, h, 0.14 * w, 0.22, 0.08);
        final Craft.Builder craft = Craft.named("Tandem-rotor helicopter").body(body, Mount.at(Point.ZERO));
        craft.parts(Parts.patch("cockpit", Paint.GLASS, body, 0.4 * length, 0.49 * length, 20, 160, 0.003 * length, 2, 8),
                Mount.at(Point.ZERO));
        for (int k = 0; k < 6; k++) {
            final double at = 0.25 * length - 0.1 * length * k;
            craft.pair(Parts.patch("window", Paint.GLASS, body, at - 0.02 * length, at + 0.02 * length, 5, 25,
                    0.003 * length, 1, 1), Mount.at(Point.ZERO));
        }
        craft.parts(Parts.patch("stripe", Paint.TRIM, body, -0.45 * length, 0.4 * length, -2, 2, 0.003 * length, 8, 1),
                Mount.at(Point.ZERO));
        craft.parts(Parts.patch("stripe", Paint.TRIM, body, -0.45 * length, 0.4 * length, 178, 182, 0.003 * length, 8, 1),
                Mount.at(Point.ZERO));
        // the pylons, and their rotors
        final BoxBody front = new BoxBody("front pylon", Paint.CAR, 0.16 * length, 0.5 * w, 0.18 * h, 0.06 * w, 0.3, 0.1);
        final Point frontAt = new Point(0, 0.34 * length, h / 2);
        craft.body(front, Mount.at(frontAt));
        final BoxBody rear = new BoxBody("rear pylon", Paint.CAR, 0.24 * length, 0.55 * w, 0.55 * h, 0.06 * w, 0.1, 0.25);
        final Point rearAt = new Point(0, -0.38 * length, h / 2 + 0.2 * h);
        craft.body(rear, Mount.at(rearAt));
        craft.spinner("front rotor", Parts.rotor("front rotor", Paint.DARK, Paint.METAL, 3, 0.4 * length,
                0.035 * length, 10, 1.5), List.of(), Mount.at(frontAt.plus(new Point(0, 0, 0.14 * h))), 4);
        craft.spinner("rear rotor", Parts.rotor("rear rotor", Paint.DARK, Paint.METAL, 3, 0.4 * length,
                0.035 * length, 10, 1.5), List.of(), Mount.at(rearAt.plus(new Point(0, 0, 0.32 * h))), -4);
        final Engine shaft = Engine.turboshaft(0.2 * length);
        craft.withPair(shaft.build(), Mount.at(new Point(0.275 * w + shaft.diameter() / 2, -0.36 * length, h / 2 + 0.12 * h)));
        // wheels
        for (final double along : new double[] {0.3 * length, -0.2 * length}) {
            craft.pair(Parts.wheel("wheel", Paint.DARK, Paint.METAL, 0.045 * length, 0.03 * length),
                    Mount.at(new Point(0.45 * w, along, -h / 2 - 0.02 * length)));
        }
        return craft.build();
    }

    /**
     * A light aeroplane {@code length} long: a fuselage, a low wing, a tailplane and a fin; a
     * two-bladed propeller and spinner at its nose; a canopy over the wing; on three wheels.
     */
    public static Craft lightPlane(final double length) {
        final double d = 0.15 * length;
        final Fuselage body = Fuselage.aircraft("fuselage", Paint.SKIN, length, d);
        final Craft.Builder craft = Craft.named("Light aeroplane").body(body, Mount.at(Point.ZERO));
        craft.parts(Parts.patch("canopy", Paint.GLASS, body, 0.02 * length, 0.2 * length, 35, 145, 0.006 * length, 4, 6),
                Mount.at(Point.ZERO));
        craft.pair(Parts.patch("stripe", Paint.TRIM, body, -0.4 * length, 0.4 * length, -4, 4, 0.002 * length, 10, 1),
                Mount.at(Point.ZERO));
        // the wing, low, from the fuselage's sides
        final List<Piece> wing = Parts.wing("wing", Paint.WING, 0.58 * length, 0.2 * length, 0.12 * length, 3, 6, 0.14);
        craft.pair(wing, Mount.at(new Point(d * 0.3, 0.14 * length, -0.3 * d)));
        // the tail
        final double tail = -0.36 * length;
        final Fuselage.Station at = body.section(tail);
        craft.pair(Parts.wing("tailplane", Paint.WING, 0.17 * length, 0.11 * length, 0.07 * length, 8, 0, 0.1),
                Mount.at(new Point(at.halfWidth() * 0.6, tail, at.up())));
        craft.parts(Parts.wing("fin", Paint.WING, 0.16 * length, 0.13 * length, 0.07 * length, 30, 0, 0.1),
                new Mount(new Point(0, tail + 0.02 * length, at.up() + at.halfHeight() * 0.6), 0, 0, -90));
        // the propeller and its spinner at the nose
        final double nose = length / 2;
        craft.body(Fuselage.spinner("spinner", Paint.TRIM, 0.07 * length, 0.08 * length),
                Mount.at(new Point(0, nose + 0.02 * length, 0)));
        craft.spinner("propeller", Parts.rotor("propeller", Paint.DARK, Paint.METAL, 2, 0.12 * length, 0.016 * length,
                26, 0), List.of(), new Mount(new Point(0, nose - 0.005 * length, 0), 0, -90, 0), 30);
        // its wheels: two under the wing, one under the nose
        final double ground = -d / 2 - 0.1 * length;
        craft.pair(Piece.bar("leg", Paint.METAL, new Point(0.1 * length, 0.08 * length, -0.35 * d),
                new Point(0.16 * length, 0.08 * length, ground), 0.01 * length, 0.01 * length));
        craft.pair(Parts.wheel("wheel", Paint.DARK, Paint.METAL, 0.04 * length, 0.025 * length),
                Mount.at(new Point(0.16 * length, 0.08 * length, ground)));
        craft.part(Piece.bar("leg", Paint.METAL, new Point(0, 0.38 * length, -0.3 * d), new Point(0, 0.4 * length, ground),
                0.01 * length, 0.01 * length));
        craft.parts(Parts.wheel("wheel", Paint.DARK, Paint.METAL, 0.032 * length, 0.02 * length),
                Mount.at(new Point(0, 0.4 * length, ground)));
        return craft.build();
    }

    /** All of them, each its usual size. */
    public static List<Craft> all() {
        final List<Craft> all = new ArrayList<>();
        for (final Kind kind : Kind.values()) {
            all.add(kind.build(kind.usual()));
        }
        return all;
    }
}
