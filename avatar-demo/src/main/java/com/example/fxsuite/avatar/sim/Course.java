package com.example.fxsuite.avatar.sim;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The layout of the world, as data: what is where, and how big. {@link World} draws it and
 * {@link Obstacles} makes it solid, so what you see is exactly what blocks you. The same every
 * run. Pure: no scene graph.
 *
 * <ul>
 *   <li>a staircase of crates straight ahead of the start, each step a jump up from the last;</li>
 *   <li>crates scattered about, from knee height to just out of a jump's reach;</li>
 *   <li>tall pillars, to walk between and fly over and land on;</li>
 *   <li>gates up in the air, four bars round an opening to fly through;</li>
 *   <li>targets to shoot at: wooden dummies standing in rows, each facing the start, drones
 *       hovering round it, and, high over everything else and far out, three great armoured
 *       airships going slowly round on their {@link Route}s ({@link Piece#at});</li>
 *   <li>and platforms, flown about from the console on each ({@link Lift}): a small one to
 *       ride like a magic carpet, and a broad barge.</li>
 * </ul>
 *
 * <p>Crates and targets can be broken ({@link Damage}); pillars and gates are what the world is
 * built on, and stand whatever is thrown at them.
 */
public final class Course {

    public enum Kind {
        PILLAR(false), CRATE(true), GATE(false), DUMMY(true), DRONE(true), AIRSHIP(true), PLATFORM(true),
        FURNITURE(true), FRUIT(true), ROAD(false), BUNKER(true), CAR(true);

        private final boolean breakable;

        Kind(final boolean breakable) {
            this.breakable = breakable;
        }

        /** Whether it can be worn down and broken. */
        public boolean breakable() {
            return breakable;
        }

        /** Whether it is something put there to be shot at. */
        public boolean target() {
            return this == DUMMY || this == DRONE || this == AIRSHIP || this == FRUIT;
        }

        /** Whether it is loose ({@link Prop}): it stands on what is under it, and can be pushed about and fall. */
        public boolean loose() {
            return this == FURNITURE || this == FRUIT;
        }
    }

    /**
     * One thing in the world, with what it is and which of its kind's colours to draw it in:
     * {@code block}, the box it fills, and {@code solid}, the blocks that are actually in the way
     * (the same box, but for an airship, whose hull, fins, gondola and engines are each one).
     * Broken, all of it goes at once.
     *
     * <p>One that can move ({@link #moves}) is made of its {@code shape}, at {@code place}: an
     * airship, which flies of itself along its {@code route}, or a platform, moved from its
     * console. Null, all three, for what stays where it is. One carried on another
     * ({@link #rides}, piece {@code carrier}; -1 for none) has its shape in that one's frame, and
     * is where that one is. A loose one ({@link Kind#loose}: fruit, furniture) says what it is
     * ({@code loose}; null for anything else), and is where it was put at the start: how it moves
     * from there is its {@link Prop}'s.
     */
    public record Piece(Block block, Kind kind, int tint, List<Block> solid, Shape shape, Place place,
                        Route route, int carrier, Loose loose, Bodywork body) {

        public Piece {
            solid = List.copyOf(solid);
        }

        /** As given, with no body on it. */
        public Piece(final Block block, final Kind kind, final int tint, final List<Block> solid, final Shape shape,
                     final Place place, final Route route, final int carrier, final Loose loose) {
            this(block, kind, tint, solid, shape, place, route, carrier, loose, null);
        }

        /**
         * A land vehicle: {@code chassis}, wearing {@code body} (null for none, bare), standing on its
         * wheels at {@code place}.
         */
        public static Piece vehicle(final Chassis chassis, final Bodywork body, final Place place, final int tint) {
            final Shape shape = chassis.shape(body);
            return new Piece(shape.box(place), Kind.CAR, tint, shape.solid(place), shape, place, null, -1, null, body);
        }

        /** A piece that stays where it is, its solid blocks given. */
        public Piece(final Block block, final Kind kind, final int tint, final List<Block> solid) {
            this(block, kind, tint, solid, null, null, null, -1, null);
        }

        /** Carried on piece {@code carrier}, made of {@code shape} in its frame, that one being at {@code place}. */
        public static Piece riding(final int carrier, final Shape shape, final Place place, final Kind kind,
                                   final int tint) {
            return new Piece(shape.box(place), kind, tint, shape.solid(place), shape, place, null, carrier, null);
        }

        /** A loose {@code thing}, of {@code kind}, standing at {@code place}: on what is under it, heading its way. */
        public static Piece loose(final Loose thing, final Kind kind, final Place place, final int tint) {
            final Section at = thing.at(0, 0, 0);
            final Shape shape = new Shape(at, List.of(at));
            return new Piece(shape.box(place), kind, tint, shape.solid(place), shape, place, null, -1, thing);
        }

        /** Which fruit it is, if it is fruit; null if not. */
        public Fruit fruit() {
            return loose instanceof Fruit f ? f : null;
        }

        /** Which furniture it is, if it is furniture; null if not. */
        public Furniture furniture() {
            return loose instanceof Furniture f ? f : null;
        }

        /** Whether it is carried on another piece ({@link #carrier}), going where that one goes. */
        public boolean rides() {
            return carrier >= 0;
        }

        /** A piece that is just its box. */
        public Piece(final Block block, final Kind kind, final int tint) {
            this(block, kind, tint, List.of(block));
        }

        /** A piece that can move, made of {@code shape}, at {@code place}; along {@code route} if it flies of itself, or null. */
        public static Piece moving(final Shape shape, final Place place, final Kind kind, final int tint,
                                   final Route route) {
            return new Piece(shape.box(place), kind, tint, shape.solid(place), shape, place, route, -1, null);
        }

        /** Whether it can move: an airship, a platform, what rides on one, or a loose thing. */
        public boolean moves() {
            return shape != null;
        }

        /** Whether it moves of itself, along its {@link #route}: an airship. */
        public boolean flies() {
            return route != null;
        }

        /** The same piece, moved to {@code to} (for one that rides, its carrier moved there). */
        public Piece placed(final Place to) {
            return new Piece(shape.box(to), kind, tint, shape.solid(to), shape, to, route, carrier, loose, body);
        }

        /** Where it is {@code seconds} after the start: along its route, if it has one, or where it is. */
        public Piece at(final double seconds) {
            return route == null ? this : placed(route.at(seconds));
        }
    }

    /**
     * What something that moves is made of, in its own frame ({@link Section}): {@code box}, all
     * of it, and {@code parts}, each solid.
     */
    public record Shape(Section box, List<Section> parts) {

        public Shape {
            parts = List.copyOf(parts);
        }

        /** All of it, at {@code place}. */
        public Block box(final Place place) {
            return box.placed(place);
        }

        /** Its solid parts, at {@code place}, in order. */
        public List<Block> solid(final Place place) {
            return parts.stream().map(part -> part.placed(place)).toList();
        }
    }

    /**
     * The way an airship goes: round the start, {@code radius} out, at {@code height}, from
     * {@code heading} at the start, at {@code speed} (units a second; round the other way when
     * negative), nose first along its way.
     */
    public record Route(double heading, double radius, double height, double speed) {

        /** Where along it, {@code seconds} after the start. */
        public Place at(final double seconds) {
            final double angle = heading + Math.toDegrees(speed * seconds / radius);
            final Vec3 at = Num.heading(angle).multiply(radius);
            // going round, its way is square to the line out from the start
            return new Place(at.x(), at.z(), height, Num.wrap(angle + (speed >= 0 ? 90 : -90)));
        }
    }

    /**
     * A part of an airship, a box in the airship's own frame: its middle {@code across} to the
     * right of the hull's line, {@code along} it toward the nose and {@code up} from it, and half
     * as wide, long and tall as these.
     */
    public record Section(double across, double along, double up, double halfAcross, double halfAlong,
                          double halfUp) {

        /** As a block, for an airship at ({@code x}, {@code height}, {@code z}) heading {@code yaw}. */
        public Block placed(final double x, final double height, final double z, final double yaw) {
            final Vec3 at = new Vec3(x, 0, z).add(Num.right(yaw).multiply(across))
                    .add(Num.heading(yaw).multiply(along));
            return new Block(at.x(), at.z(), halfAcross, halfAlong, yaw, height + up - halfUp, height + up + halfUp);
        }

        /** As a block, for an airship at {@code place}. */
        public Block placed(final Place place) {
            return placed(place.x(), place.height(), place.z(), place.yaw());
        }
    }

    /**
     * An airship's hull: stepping out from a blunt nose to its full girth and back in to the
     * tail, 18 m long and 4.4 m across. Then a cross of fins at the tail, a gondola under the
     * middle, and an engine either side on a pylon. Each is solid.
     */
    public static final List<Section> HULL = List.of(
            new Section(0, 840, 0, 100, 60, 100),
            new Section(0, 700, 0, 160, 80, 160),
            new Section(0, 520, 0, 200, 100, 200),
            new Section(0, 40, 0, 220, 380, 220),
            new Section(0, -460, 0, 200, 120, 200),
            new Section(0, -680, 0, 160, 100, 160),
            new Section(0, -840, 0, 100, 60, 100));
    public static final List<Section> FINS = List.of(
            new Section(0, -760, 250, 8, 140, 130),
            new Section(0, -760, -250, 8, 140, 130),
            new Section(-250, -760, 0, 130, 140, 8),
            new Section(250, -760, 0, 130, 140, 8));
    public static final Section GONDOLA = new Section(0, 80, -275, 75, 190, 55);
    public static final List<Section> PYLONS = List.of(
            new Section(-255, -60, -40, 40, 30, 7),
            new Section(255, -60, -40, 40, 30, 7));
    public static final List<Section> ENGINES = List.of(
            new Section(-330, -60, -40, 40, 80, 40),
            new Section(330, -60, -40, 40, 80, 40));
    /** All of an airship's box: out to the fins' tips, nose to tail, gondola to the top fin. */
    public static final Section AIRSHIP_BOX = new Section(0, 0, 25, 380, 900, 355);
    /** An airship: its box, and each part solid: the hull, the fins, the gondola, the pylons and the engines. */
    public static final Shape AIRSHIP = new Shape(AIRSHIP_BOX, java.util.stream.Stream.of(HULL, FINS, List.of(GONDOLA),
            PYLONS, ENGINES).flatMap(List::stream).toList());

    /**
     * A platform's deck, as big as it is wanted for what it is for: {@code width} across and
     * {@code depth} from back to front, 40 cm thick, its top where the platform is. At the front,
     * in the middle, the console its controls are on, waist high; whoever flies it stands behind
     * the console, facing the way it heads.
     */
    public record Deck(double width, double depth) implements Craft {

        /** The smallest a deck can be: room for the console, and whoever stands at it. */
        public static final double NARROWEST = 150;
        public static final double SHALLOWEST = 220;

        public Deck {
            if (width < NARROWEST || depth < SHALLOWEST) {
                throw new IllegalArgumentException("a deck " + width + " by " + depth + " has no room for its console");
            }
        }

        /** The deck itself, a slab. */
        public Section slab() {
            return new Section(0, 0, -20, width / 2, depth / 2, 20);
        }

        /** The console, at the front, its back toward whoever flies it. */
        public Section console() {
            return new Section(0, depth / 2 - 45, 55, 35, 20, 55);
        }

        /** How far ahead of the middle whoever flies it stands: behind the console, a step from it. */
        public double pilot() {
            return depth / 2 - 105;
        }

        /**
         * The room it needs to fly: the deck, and above it as high as whoever stands on it, so
         * nothing it flies under or past goes through a rider.
         */
        public Section room() {
            return new Section(0, 0, (-40 + Body.TALL + 30) / 2.0, width / 2, depth / 2, (40 + Body.TALL + 30) / 2.0);
        }

        /** All of it, and its parts: the slab first, then the console. */
        public Shape shape() {
            return new Shape(new Section(0, 0, 35, width / 2, depth / 2, 75), List.of(slab(), console()));
        }

        /** The deck a shape was made from ({@link #shape}). */
        public static Deck of(final Shape shape) {
            final Section slab = shape.parts().getFirst();
            return new Deck(slab.halfAcross() * 2, slab.halfAlong() * 2);
        }

        /** It rests on the underside of its slab. */
        @Override
        public Section footing() {
            return slab();
        }

        /** Flown like a magic carpet: quick, sideways too, and its engine can be cut. */
        @Override
        public Handling handling() {
            return PLATFORM_HANDLING;
        }

        /** Open: whoever flies it is seen, standing at the console. */
        @Override
        public boolean closed() {
            return false;
        }

        /** Where whoever flies it stands: on the deck, just behind the console. */
        @Override
        public Section station() {
            return new Section(0, pilot(), 0, 0, 0, 0);
        }
    }

    /**
     * What can be flown from on board ({@link Lift}): its shape, the room it needs to move
     * ({@link #room}), the part it rests on when it is not flying ({@link #footing}), how it handles,
     * whether it is closed (whoever flies it is out of sight, inside), and where whoever takes it
     * stands ({@link #station}, its middle in the craft's frame).
     */
    public sealed interface Craft permits Deck, Ship, Chassis {

        Shape shape();

        Section room();

        Section footing();

        Handling handling();

        boolean closed();

        Section station();

        /** Where whoever flies it is while at its controls: on an open craft, at its station; in a closed one, inside. */
        default Section cabin() {
            return station();
        }

        /** Where whoever takes it from outside it stands, and is let out at: its station, unless it says otherwise. */
        default Section door() {
            return station();
        }

        /** How many bombs its bay holds: none, unless it has one. */
        default int bombs() {
            return 0;
        }

        /** Where its bombs drop from. */
        default Section bay() {
            return station();
        }
    }

    /**
     * How a craft handles: how fast it goes ahead, how much of that sideways (0 for none), how fast
     * it climbs, how fast at most it turns ({@code turn}, degrees a second; 0 for as fast as the
     * mouse), how low and how high it goes ({@code low}, {@code high}: where it is, its own
     * height), and whether its engine can be cut ({@code cuts}).
     */
    public record Handling(double speed, double sideways, double climb, double turn, double low, double high,
                           boolean cuts) {
    }

    /** A platform: quick, sideways too, turning as fast as the mouse, and its engine can be cut. */
    public static final Handling PLATFORM_HANDLING = new Handling(700, 0.6, 350, 0, 60, 4500, true);
    /** An airship: stately, no sideways, turning slowly; it never comes down below its moorings. */
    public static final Handling SHIP_HANDLING = new Handling(900, 0, 250, 22, Course.MOORED, 4500, false);

    /**
     * An airship to be flown ({@link #AIRSHIP}): closed, boarded by the hatch in its gondola's floor,
     * from the ground under it when it is moored.
     */
    public record Ship() implements Craft {

        @Override
        public Shape shape() {
            return AIRSHIP;
        }

        /** All of it: out to its fins' tips. */
        @Override
        public Section room() {
            return AIRSHIP_BOX;
        }

        @Override
        public Section footing() {
            return AIRSHIP_BOX;
        }

        @Override
        public Handling handling() {
            return SHIP_HANDLING;
        }

        @Override
        public boolean closed() {
            return true;
        }

        /** At the hatch in the middle of the gondola's floor, to be reached from under it. */
        @Override
        public Section station() {
            return new Section(0, GONDOLA.along(), GONDOLA.up() - GONDOLA.halfUp(), 0, 0, 0);
        }

        /** Where whoever flies it is, out of sight: inside the gondola, on its floor. */
        @Override
        public Section cabin() {
            return new Section(0, GONDOLA.along(), GONDOLA.up() - GONDOLA.halfUp() + 1, 0, 0, 0);
        }

        /** A bay of a dozen bombs, under the gondola. */
        @Override
        public int bombs() {
            return 12;
        }

        /** Just under the gondola, where the airships going round drop theirs from too. */
        @Override
        public Section bay() {
            return new Section(0, GONDOLA.along(), GONDOLA.up() - GONDOLA.halfUp() - Sky.BELOW, 0, 0, 0);
        }
    }

    /** The airship to be flown, there being only one kind. */
    public static final Ship SHIP = new Ship();

    /**
     * How a land vehicle drives: how fast at most ahead ({@code top}) and in reverse, how briskly it
     * gets up to speed ({@code accel}), stops under the brakes ({@code brake}) and rolls to a stop
     * left to itself ({@code drag}), all in units a second, a second; how far its front wheels turn
     * at most ({@code steer}, degrees, at a crawl: less the faster it goes), and how fast they turn
     * there ({@code steerRate}, degrees a second).
     */
    public record Drive(double top, double reverse, double accel, double brake, double drag, double steer,
                        double steerRate) {
    }

    /**
     * A chassis, bare: what a car or a truck is built on. A flat bed, {@code width} across and
     * {@code length} from back to front, {@code ride} up off the ground, its top where the vehicle
     * is; a wheel either side of the bed on each axle, {@code wheelRadius} in radius and
     * {@code wheelWidth} wide, the axles {@code axles} along it from its middle, the first the one
     * that steers; and a steering wheel on a column, before whoever drives it, standing on the bed
     * at ({@code driverAcross}, {@code driverAlong}). Open: the driver is seen. It drives as
     * {@code drive} says ({@link Lift}).
     */
    public record Chassis(String name, double width, double length, double ride, double wheelRadius,
                          double wheelWidth, List<Double> axles, double driverAcross, double driverAlong, Drive drive)
            implements Craft {

        /** How thick the bed is, and how high the steering wheel stands over it: within reach of a seated driver. */
        public static final double BED = 30;
        public static final double WHEEL_HEIGHT = 80;

        public Chassis {
            axles = List.copyOf(axles);
        }

        /** The bed, a slab, its top where the vehicle is. */
        public Section bed() {
            return new Section(0, 0, -BED / 2, width / 2, length / 2, BED / 2);
        }

        /** How far out from the middle each wheel is: just clear of the bed's side. */
        public double wheelOut() {
            return width / 2 + 2 + wheelWidth / 2;
        }

        /** Its wheels, left then right on each axle, front to back: each touching the ground when it rests. */
        public List<Section> wheels() {
            final List<Section> wheels = new ArrayList<>();
            for (final double along : axles) {
                for (final double side : new double[] {-1, 1}) {
                    wheels.add(new Section(side * wheelOut(), along, wheelRadius - ride, wheelWidth / 2, wheelRadius,
                            wheelRadius));
                }
            }
            return wheels;
        }

        /** The steering column, from the bed up to the steering wheel, in front of the driver. */
        public Section column() {
            return new Section(driverAcross, driverAlong + 50, WHEEL_HEIGHT / 2, 4, 4, WHEEL_HEIGHT / 2);
        }

        /** From its front axle back to the middle of those behind: what it turns about. */
        public double wheelbase() {
            return axles.getFirst() - rear();
        }

        /** Where its back wheels are, between them, along it: the point it turns about. */
        public double rear() {
            return axles.stream().skip(1).mapToDouble(Double::doubleValue).average().orElseThrow();
        }

        /** All of it, and its parts: the bed first, then the steering column, then the wheels. */
        public Shape shape() {
            final List<Section> parts = new ArrayList<>(List.of(bed(), column()));
            parts.addAll(wheels());
            final double out = wheelOut() + wheelWidth / 2;
            return new Shape(new Section(0, 0, (WHEEL_HEIGHT - ride) / 2, out, length / 2, (WHEEL_HEIGHT + ride) / 2),
                    parts);
        }

        /**
         * It and the body it wears ({@code body}; null for none), as one: its own parts first, as
         * {@link #shape} has them, then the body's boxes; its box round all of them.
         */
        public Shape shape(final Bodywork body) {
            if (body == null) {
                return shape();
            }
            final List<Section> parts = new ArrayList<>(shape().parts());
            parts.addAll(body.solid());
            double out = 0;
            double top = WHEEL_HEIGHT;
            double front = length / 2;
            for (final Section s : parts) {
                out = Math.max(out, Math.abs(s.across()) + s.halfAcross());
                top = Math.max(top, s.up() + s.halfUp());
                front = Math.max(front, Math.abs(s.along()) + s.halfAlong());
            }
            return new Shape(new Section(0, 0, (top - ride) / 2, out, front, (top + ride) / 2), parts);
        }

        /** The chassis a shape was made from ({@link #shape}), with whatever body: known by its bed. */
        public static Chassis of(final Shape shape) {
            final Section bed = shape.parts().getFirst();
            return CHASSES.stream().filter(c -> c.bed().equals(bed)).findFirst().orElseThrow();
        }

        /**
         * Where whoever drives it gets in from, and is let out at: on the ground, beside the
         * driver's seat, clear of any body it wears.
         */
        @Override
        public Section door() {
            final double side = driverAcross < 0 ? -1 : 1;
            return new Section(side * (wheelOut() + wheelWidth / 2 + 60), driverAlong, -ride, 0, 0, 0);
        }

        /** From the ground it stands on up as high as whoever rides it: nothing it drives under or past goes through a rider. */
        @Override
        public Section room() {
            final double top = Body.TALL + 30;
            return new Section(0, 0, (top - ride) / 2, wheelOut() + wheelWidth / 2, length / 2, (top + ride) / 2);
        }

        /** It stands on its wheels: the bottom of them. */
        @Override
        public Section footing() {
            return new Section(0, 0, 1 - ride, wheelOut() + wheelWidth / 2, length / 2, 1);
        }

        /** As fast as it drives; not flown, and its engine is not cut. */
        @Override
        public Handling handling() {
            return new Handling(drive.top(), 0, 0, 0, 0, 0, false);
        }

        @Override
        public boolean closed() {
            return false;
        }

        /** Where the driver stands: on the bed, behind the steering wheel. */
        @Override
        public Section station() {
            return new Section(driverAcross, driverAlong, 0, 0, 0, 0);
        }
    }

    /** A car's chassis: 4 m long, 1.8 m wide, four wheels; quick, and nimble. */
    public static final Chassis CAR_CHASSIS = new Chassis("car", 180, 400, 75, 35, 24, List.of(130.0, -130.0), -40,
            40, new Drive(2200, 700, 900, 2600, 350, 34, 140));
    /** A truck's: 7 m long, 2.4 m wide, six wheels, the back four on two axles together; slower, and wider in its turns. */
    public static final Chassis TRUCK_CHASSIS = new Chassis("truck", 240, 700, 105, 50, 30,
            List.of(240.0, -150.0, -280.0), -60, 170, new Drive(1600, 450, 550, 1900, 300, 30, 100));
    /**
     * A pickup's: 5.2 m long, 2 m wide, four wheels, its seat well forward to leave the back of it
     * free, for a gun on a post ({@link #REAR_GUN}); what a pickup with a gun, or an armoured
     * vehicle, is built on.
     */
    public static final Chassis PICKUP_CHASSIS = new Chassis("pickup", 200, 520, 85, 40, 26, List.of(170.0, -170.0),
            -45, 110, new Drive(1900, 600, 750, 2300, 330, 32, 120));
    /**
     * A vehicle's body: the panels on its chassis, which are only seen (the look layer draws them
     * from its {@code name}); to the world, the vehicle is still its chassis, as a whole. What the
     * body changes is how the vehicle takes harm: {@code armour} times as much of it before it
     * breaks, and {@code plating} taken off every blow, so the lightest rounds hardly mark it; and
     * how much room it takes, its {@code solid} boxes (in its chassis's frame) a rough match for its
     * panels: what rounds strike, and what bumps into it.
     */
    public record Bodywork(String name, double armour, double plating, List<Section> solid) {

        public Bodywork {
            solid = List.copyOf(solid);
        }
    }

    /** A pickup's body: a cab with a windscreen over the seat, and an open bed behind with low sides. */
    public static final Bodywork PICKUP_BODY = new Bodywork("pickup", 1.6, 3, List.of(
            // the cab, bonnet and all; the bed's sides and its tailgate
            new Section(0, 150, 72, 132, 110, 100),
            new Section(-130, -111, 10, 3, 151, 38), new Section(130, -111, 10, 3, 151, 38),
            new Section(0, -262, 10, 132, 3, 38)));
    /** A roadster's: open, low, a bonnet before the seat and a deck behind it. */
    public static final Bodywork ROADSTER = new Bodywork("roadster", 1.3, 1, List.of(
            // low, all of it, its sides as high as its deck
            new Section(0, 0, 15, 120, 202, 43)));
    /** A truck's: a cab over its front wheels, and a flatbed behind with low sides. */
    public static final Bodywork TRUCK_CAB = new Bodywork("truck", 1.5, 2, List.of(
            // the cab; the flatbed's sides and its tailgate
            new Section(0, 223, 87, 156, 129, 117),
            new Section(-154, -130, 5, 3, 224, 35), new Section(154, -130, 5, 3, 224, 35),
            new Section(0, -354, 5, 156, 3, 35)));
    /**
     * A van's: tall and boxy, a short low nose, a big windscreen sloping up to a high roof, the sides
     * glazed behind the driver's door, and a hatch at the back; on the car's chassis, its fixed gun
     * poking out through a port in the right half of the windscreen.
     */
    public static final Bodywork VAN = new Bodywork("van", 1.4, 2, List.of(
            // one tall box, nose to tail
            new Section(0, 0, 80, 118, 202, 108)));

    /** Every chassis there is, to know one by its shape. */
    public static final List<Chassis> CHASSES = List.of(CAR_CHASSIS, TRUCK_CHASSIS, PICKUP_CHASSIS);

    /**
     * The car's gun: a heavy Gatling gun, fixed to the right of the driver's seat on a short post,
     * its barrels reaching out over the front; it fires the way the car faces, and is fired by
     * whoever drives it.
     */
    public static final Mount CAR_GUN = new Mount(Gun.HEAVY_GATLING,
            new Section(CAR_CHASSIS.driverAcross() + 62, CAR_CHASSIS.driverAlong() + 10, 70, 0, 0, 0), 230, 0, 0, 0,
            0, false, false, CAR_CHASSIS.station(), CAR_CHASSIS.station(), false);

    /**
     * The pickup's gun: a heavy Gatling gun on a post at the back, high enough to fire over the
     * driver's head; all round, 10° down and 45° up, turning 100° a second at most, and holding its
     * aim on the world as the pickup turns under it. Whoever sits at the wheel drives and fires both,
     * as one: the keys drive, the mouse aims the gun.
     */
    public static final Mount REAR_GUN = new Mount(Gun.HEAVY_GATLING,
            new Section(0, -PICKUP_CHASSIS.length() / 2 + 80, 190, 0, 0, 0), 230, 180, 10, 45, 100, true, false,
            PICKUP_CHASSIS.door(), PICKUP_CHASSIS.station(), false);

    /**
     * A gun on a mount ({@link Turret}): which gun ({@link Gun}), where it turns about on what
     * carries it ({@code pivot}, in that's frame, as for a vehicle's parts), how far its muzzle is
     * out from there ({@code barrel}), how far it turns either way ({@code traverse}, degrees; 180
     * for all round), down ({@code depression}) and up ({@code elevation}), how fast it turns at
     * most ({@code slew}, degrees a second), whether it holds its aim on the world as what carries it
     * turns ({@code stabilized}), whether whoever mans it is inside, out of sight ({@code closed}),
     * where they stand to man it ({@code station}), where they are while they do ({@code seat}), and
     * whether, held down too long, it gets too hot to fire until it has cooled ({@code overheats}).
     */
    public record Mount(Gun gun, Section pivot, double barrel, double traverse, double depression, double elevation,
                        double slew, boolean stabilized, boolean closed, Section station, Section seat,
                        boolean overheats) {

        /**
         * Whether it is fixed: turning neither round nor up and down on what carries it, so it fires
         * the way that faces, tipped as that is; fired by whoever drives it, not manned on its own.
         */
        public boolean fixed() {
            return traverse == 0 && depression == 0 && elevation == 0;
        }
    }

    /**
     * A bunker: a concrete pillbox, 5.2 m across and 5 m deep, its walls 2.4 m high under a roof,
     * a slit along its front from 1 m to 2.1 m up for its gun to fire through, and its gunner to see out of, and a door in the
     * middle of its back. Each wall solid; the slit and the door open.
     */
    public static final List<Section> BUNKER_WALLS = List.of(
            new Section(0, 250, 50, 260, 20, 50),
            new Section(0, 250, 225, 260, 20, 15),
            new Section(-240, 0, 120, 20, 270, 120),
            new Section(240, 0, 120, 20, 270, 120),
            new Section(-150, -250, 120, 110, 20, 120),
            new Section(150, -250, 120, 110, 20, 120),
            new Section(0, 0, 260, 260, 270, 20));
    public static final Section BUNKER_BOX = new Section(0, 0, 140, 260, 270, 140);
    public static final Shape BUNKER = new Shape(BUNKER_BOX, BUNKER_WALLS);
    /**
     * The bunker's gun: a heavy Gatling gun on a pedestal behind the slit, firing out through it,
     * 55° either way, 8° down and 25° up, turning 70° a second at most. Manned from the door:
     * whoever mans it is inside, out of sight.
     */
    public static final Mount BUNKER_GUN = new Mount(Gun.HEAVY_GATLING, new Section(0, 170, 150, 0, 0, 0), 240,
            55, 8, 25, 70, false, true, new Section(0, -330, 0, 0, 0, 0), new Section(0, 60, 1, 0, 0, 0), false);

    /**
     * A gunship: a platform a little bigger than the carpet, a heavy Gatling gun on its console.
     * Whoever mans the gun flies it too ({@link #GUNSHIP_GUN}).
     */
    public static final Deck GUNSHIP = new Deck(360, 300);
    /**
     * The gunship's gun: on a post through its console, firing over its front, all round, 30° down
     * and 40° up, turning 90° a second at most; and holding its aim on the world as the gunship
     * turns under it. Manned from behind the console, where the gunner stands in plain sight.
     */
    public static final Mount GUNSHIP_GUN = new Mount(Gun.HEAVY_GATLING,
            new Section(0, GUNSHIP.console().along(), 135, 0, 0, 0), 240, 180, 30, 40, 90, true, false,
            GUNSHIP.station(), GUNSHIP.station(), false);

    /** A small platform for one, to fly about on like a magic carpet; and a broad one, a barge, room for a stall. */
    public static final Deck CARPET = new Deck(300, 240);
    public static final Deck BARGE = new Deck(1000, 600);
    /** Where a platform stands at the start, in open air: heading from the start, and how far; and its deck. */
    private record Berth(double heading, double out, Deck deck) {
    }

    private static final List<Berth> PLATFORMS = List.of(new Berth(60, 1800, CARPET), new Berth(10, 2200, BARGE));
    /** How low and high a platform's deck goes: just off the ground, and well over the airships. */
    public static final double LIFT_LOW = 60;
    public static final double LIFT_HIGH = 4500;
    /** How high a moored airship is: its gondola and lower fin just over the avatar's head, to walk in under and board. */
    public static final double MOORED = 600;
    /**
     * The melon stall's table, in the barge's frame, toward its back: 2.4 m long, 1 m across and
     * 70 cm high.
     */
    public static final Section TABLE = new Section(0, -120, 35, 120, 50, 35);

    /**
     * Someone put in the world at the start ({@link Person}): who, on which platform's deck (as
     * big as {@code deck}), where on it in its frame ({@code across}, {@code along}), which way they
     * face from the way it heads, and what they sit on ({@code seat}, put there with them; null for
     * standing).
     */
    public record Cast(Person.Role role, Deck deck, double across, double along, double facing, Furniture seat) {

        /** Whether they sit. */
        public boolean sitting() {
            return seat != null;
        }
    }

    /**
     * The people at the fruit stall on the barge: the stallholder on a stool behind the table,
     * facing the start; a customer before it; and another sat on a scooter parked to one side.
     */
    public static final List<Cast> CAST = List.of(
            new Cast(Person.Role.STALLHOLDER, BARGE, -30, TABLE.along() + TABLE.halfAlong() + 45, 180, Furniture.STOOL),
            new Cast(Person.Role.CUSTOMER, BARGE, 45, TABLE.along() - TABLE.halfAlong() - 55, 0, null),
            new Cast(Person.Role.RIDER, BARGE, -314, TABLE.along() - TABLE.halfAlong() - 82, 30, Furniture.SCOOTER));
    /**
     * The airships' routes: where each starts (heading from the start, and how far), how high it
     * flies, and how fast round, in units a second: slowly, a lap taking the best part of two
     * minutes, the middle one going round the other way.
     */
    private static final double[][] AIRSHIPS = {
            {70, 6500, 3100, 400},
            {190, 7500, 3300, -350},
            {300, 5500, 3500, 300},
    };

    /** The heading the avatar starts on: the staircase lies along it. */
    public static final double START_YAW = 30;
    public static final int STEPS = 5;
    public static final double STEP_RISE = 60;
    public static final double GATE_SPAN = 600;
    public static final double GATE_BAR = 36;
    /** The range of dummies: which way from the start, and how far. */
    public static final double RANGE_HEADING = -40;
    public static final double RANGE_OUT = 1100;
    /** A dummy: a board on legs, as wide, as thick and as tall as this; and how far apart in a row. */
    public static final double DUMMY_WIDTH = 90;
    public static final double DUMMY_DEPTH = 24;
    public static final double DUMMY_HEIGHT = 180;
    public static final double DUMMY_APART = 200;
    /** The drones: how many, how wide across, and how deep top to bottom. */
    public static final int DRONES = 8;
    public static final double DRONE_SIZE = 130;
    public static final double DRONE_HEIGHT = 40;

    private final List<Piece> pieces = new ArrayList<>();
    private final List<Gate> gates = new ArrayList<>();

    /** The playground: everything at once. */
    public Course() {
        this(Demo.PLAYGROUND);
    }

    /** The world of {@code demo}: the playground, or a simple one round a single vehicle. */
    public Course(final Demo demo) {
        switch (demo) {
            case PLAYGROUND -> playground();
            case PLATFORM -> platformDemo();
            case AIRSHIP -> airshipDemo();
            case FLYOVER -> flyoverDemo();
            case GUNS -> gunsDemo();
        }
    }

    /**
     * The flying platform's demo: open ground, a ring of pillars to fly between, a few gates high
     * up, and the small platform standing just ahead of the start.
     */
    private void platformDemo() {
        scenery(11);
        pieces.add(Piece.moving(CARPET.shape(), new Place(0, 500, LIFT_LOW, 0), Kind.PLATFORM, 0, null));
    }

    /**
     * The airship's demo: open ground, a ring of pillars, gates high up, and an airship moored
     * ahead of the start, broadside on, its gondola just over the robot's head: to walk in under and
     * board. Beyond it, a yard of crates to bomb.
     */
    private void airshipDemo() {
        scenery(13);
        pieces.add(Piece.moving(AIRSHIP, new Place(0, 2500, MOORED, 90), Kind.AIRSHIP, 0, null));
        final Random random = new Random(17);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                final double size = 150 + random.nextDouble() * 120;
                pieces.add(new Piece(Block.standing(-800 + column * 800, 3500 + row * 450, size, size,
                        100 + random.nextDouble() * 200, random.nextDouble() * 90), Kind.CRATE, random.nextInt(3)));
            }
        }
    }

    /** Where the flyover crosses the way ahead of the start, how high its deck is, and how wide its road. */
    public static final double FLYOVER_Z = 1500;
    public static final double FLYOVER_HEIGHT = 400;
    public static final double ROAD_WIDTH = 600;
    /** How long the flyover's deck is, and each of its ramps: one in four. */
    public static final double DECK_LENGTH = 1200;
    public static final double RAMP_LENGTH = 4 * FLYOVER_HEIGHT;
    /** How thick its deck is. */
    public static final double DECK_THICK = 40;

    /**
     * The flyover's demo: a road across the way ahead of the start, up a ramp, over a deck high
     * enough to run under, and down a ramp the other side; and further on, a ramp on its own, to
     * run up and leap off. Pillars about, as in the vehicles' demos.
     */
    /**
     * The guns' demo: a bunker just ahead of the start, its door toward it and its gun facing away,
     * and a gunship off to the right, a platform with a gun on its console;
     * over open ground with something to shoot at: rows of crates and of dummies out in front, and
     * drones hovering further out. Pillars about, as in the vehicles' demos.
     */
    private void gunsDemo() {
        scenery(23);
        pieces.add(Piece.moving(BUNKER, new Place(0, 700, 0, 0), Kind.BUNKER, 0, null));
        // and the gunship, off to the right of the start, at its lowest, heading the way the bunker faces
        pieces.add(Piece.moving(GUNSHIP.shape(), new Place(900, 250, LIFT_LOW, 0), Kind.PLATFORM, 0, null));
        final Random random = new Random(29);
        for (int i = 0; i < 14; i++) {
            final double heading = -45 + 90.0 * i / 13;
            final double out = 2200 + random.nextDouble() * 2000;
            final Vec3 at = Num.heading(heading).multiply(out).add(0, 0, 700);
            final double size = 140 + random.nextDouble() * 140;
            final Block crate = Block.standing(at.x(), at.z(), size, size, 80 + random.nextDouble() * 220,
                    random.nextDouble() * 90);
            final int tint = random.nextInt(3);
            if (pieces.stream().noneMatch(p -> p.block().overlaps(crate))) {
                pieces.add(new Piece(crate, Kind.CRATE, tint));
            }
        }
        for (int k = 0; k < 7; k++) {
            final double across = (k - 3) * 300;
            pieces.add(new Piece(Block.standing(across, 2000, DUMMY_WIDTH, DUMMY_DEPTH, DUMMY_HEIGHT, 0),
                    Kind.DUMMY, k % 3));
        }
        for (int i = 0; i < 6; i++) {
            final double heading = -40 + 16.0 * i;
            final Vec3 at = Num.heading(heading).multiply(3800 + 400 * (i % 2)).add(0, 0, 700);
            final double low = 500 + 150 * (i % 3);
            pieces.add(new Piece(new Block(at.x(), at.z(), DRONE_SIZE / 2, DRONE_SIZE / 2, 180 + heading,
                    low, low + DRONE_HEIGHT), Kind.DRONE, i % 3));
        }
    }

    private void flyoverDemo() {
        scenery(19, true);
        roads(flyover(0, FLYOVER_Z, 90));
        // and a car's chassis and a truck's, either side of the way ahead, to drive on them
        pieces.add(Piece.vehicle(CAR_CHASSIS, ROADSTER, new Place(-450, 300, CAR_CHASSIS.ride(), 0), 0));
        pieces.add(Piece.vehicle(TRUCK_CHASSIS, TRUCK_CAB, new Place(550, 250, TRUCK_CHASSIS.ride(), 0), 1));
        // and a van on the car's chassis, its gun and all, beyond the roadster
        pieces.add(Piece.vehicle(CAR_CHASSIS, VAN, new Place(-950, 250, CAR_CHASSIS.ride(), 0), 3));
        // and a pickup's, its gun on the back, behind the start
        pieces.add(Piece.vehicle(PICKUP_CHASSIS, PICKUP_BODY, new Place(0, -700, PICKUP_CHASSIS.ride(), 0), 2));
        // and crates to shoot, out on the open ground either side of the road
        final Random random = new Random(31);
        for (int i = 0; i < 16; i++) {
            final double side = i % 2 == 0 ? -1 : 1;
            final Vec3 at = new Vec3(side * (800 + random.nextDouble() * 1800), 0, -2600 - random.nextDouble() * 3200);
            final double size = 120 + random.nextDouble() * 140;
            final Block crate = Block.standing(at.x(), at.z(), size, size, 80 + random.nextDouble() * 180,
                    random.nextDouble() * 90);
            final int tint = random.nextInt(3);
            if (pieces.stream().noneMatch(p -> p.block().overlaps(crate))) {
                pieces.add(new Piece(crate, Kind.CRATE, tint));
            }
        }
        // a ramp to leap off: one in three, up to a metre and a half, open at its head
        pieces.add(new Piece(Block.ramp(0, 3000, 0, 0, 3450, 150, 500), Kind.ROAD, 0));
    }

    /**
     * A flyover, its middle at ({@code x}, {@code z}), its road heading {@code heading} (either
     * way): a ramp up, one in four, a deck {@link #FLYOVER_HEIGHT} up, high enough to run or drive
     * under, and a ramp down; each {@link #ROAD_WIDTH} wide.
     */
    private static List<Block> flyover(final double x, final double z, final double heading) {
        final Vec3 along = Num.heading(heading);
        final double end = DECK_LENGTH / 2;
        final double foot = end + RAMP_LENGTH;
        final List<Block> road = new ArrayList<>();
        for (final double side : new double[] {-1, 1}) {
            road.add(Block.ramp(x + along.x() * foot * side, z + along.z() * foot * side, 0,
                    x + along.x() * end * side, z + along.z() * end * side, FLYOVER_HEIGHT, ROAD_WIDTH));
        }
        road.add(1, new Block(x, z, ROAD_WIDTH / 2, end, heading, FLYOVER_HEIGHT - DECK_THICK, FLYOVER_HEIGHT));
        return road;
    }

    /** A road on the ground, from ({@code fromX}, {@code fromZ}) to ({@code toX}, {@code toZ}): a strip of tarmac, a step's fraction high. */
    private static Block street(final double fromX, final double fromZ, final double toX, final double toZ) {
        final double length = Math.hypot(toX - fromX, toZ - fromZ);
        final double yaw = Math.toDegrees(Math.atan2(toX - fromX, toZ - fromZ));
        return new Block((fromX + toX) / 2, (fromZ + toZ) / 2, ROAD_WIDTH / 2, length / 2, yaw, 0, STREET);
    }

    /** How high a road on the ground stands: hardly at all, to be walked onto without a step. */
    public static final double STREET = 3;

    /** Each of {@code road}'s blocks, laid as a piece of road: the ramps one shade, the decks and streets another. */
    private void roads(final List<Block> road) {
        for (final Block block : road) {
            pieces.add(new Piece(block, Kind.ROAD, block.sloped() ? 0 : 1));
        }
    }

    /**
     * The playground's roads: a flyover behind the start, carrying a road east and west over a
     * street that runs out from the start under it; and another, out to the right, carrying a road
     * north and south over a street running east under it.
     */
    private static List<Block> playgroundRoads() {
        final List<Block> road = new ArrayList<>(flyover(0, -3500, 90));
        road.add(street(0, -1600, 0, -8500));
        road.addAll(flyover(5200, 3800, 0));
        road.add(street(2900, 3800, 10500, 3800));
        return road;
    }

    /** Whether {@code block} stands on or near any of {@code road}: crates and pillars there are cleared away. */
    private static boolean inTheWay(final Block block, final List<Block> road) {
        for (final Block stretch : road) {
            final Block clear = new Block(stretch.x(), stretch.z(), stretch.halfWidth() + 150, stretch.halfDepth() + 150,
                    stretch.yaw(), 0, Double.MAX_VALUE);
            if (clear.overlaps(block)) {
                return true;
            }
        }
        return false;
    }

    /**
     * What the vehicles' demos have about them: pillars of all heights in a wide ring, and gates high
     * up, all clear of the way straight ahead of the start, where the vehicle stands.
     */
    private void scenery(final long seed) {
        scenery(seed, false);
    }

    /** As {@link #scenery(long)}, keeping clear of the flyover too, if {@code road}. */
    private void scenery(final long seed, final boolean road) {
        final Random random = new Random(seed);
        for (int i = 0; i < 28; i++) {
            final double angle = i * 2 * Math.PI / 28 + random.nextDouble() * 0.2;
            final double reach = 2500 + random.nextDouble() * 5000;
            final double size = 150 + random.nextDouble() * 200;
            final double height = 400 + Math.pow(random.nextDouble(), 1.5) * 3000;
            final double x = Math.sin(angle) * reach;
            final double z = Math.cos(angle) * reach;
            if (Math.abs(x) < 1200 && z > -800 && z < 4600 || road && Math.abs(x) < 3000 && Math.abs(z - FLYOVER_Z) < 900) {
                continue;  // keep the way ahead clear, and the road
            }
            pieces.add(new Piece(Block.standing(x, z, size, size, height, 0), Kind.PILLAR, random.nextInt(4)));
        }

        for (int i = 0; i < 4; i++) {
            final double angle = i * Math.PI / 2 + 0.7;
            gate(Math.sin(angle) * 4000, 900 + i * 400, Math.cos(angle) * 4000, random.nextDouble() * 180, i % 3);
        }
    }

    /** Everything: the course, the range, the airships, the platforms and the stall. */
    private void playground() {
        final Random random = new Random(7);

        final double c = Math.cos(Math.toRadians(START_YAW));
        final double s = Math.sin(Math.toRadians(START_YAW));
        for (int i = 0; i < STEPS; i++) {
            final double ahead = 500 + i * 170;
            pieces.add(new Piece(Block.standing(s * ahead, c * ahead, 170, 170, STEP_RISE * (i + 1), START_YAW),
                    Kind.CRATE, i % 3));
        }

        for (int i = 0; i < 90; i++) {
            final double x = spot(random);
            final double z = spot(random);
            if (Math.hypot(x, z) < 1400) {
                continue;  // keep the start, and the staircase, clear
            }
            final double width = 100 + random.nextDouble() * 160;
            final double depth = 100 + random.nextDouble() * 160;
            final double height = 40 + random.nextDouble() * 200;
            pieces.add(new Piece(Block.standing(x, z, width, depth, height, random.nextDouble() * 90),
                    Kind.CRATE, random.nextInt(3)));
        }

        for (int i = 0; i < 170; i++) {
            final double x = spot(random);
            final double z = spot(random);
            if (Math.hypot(x, z) < 1400) {
                continue;
            }
            final double size = 120 + random.nextDouble() * 200;
            final double height = 300 + Math.pow(random.nextDouble(), 2) * 2300;
            pieces.add(new Piece(Block.standing(x, z, size, size, height, 0), Kind.PILLAR, random.nextInt(4)));
        }

        // the roads have the ground they run over to themselves: what was scattered there is cleared
        final List<Block> roads = playgroundRoads();
        pieces.removeIf(p -> (p.kind() == Kind.CRATE || p.kind() == Kind.PILLAR) && inTheWay(p.block(), roads));

        for (int i = 0; i < 14; i++) {
            final double angle = i * 2 * Math.PI / 14 + 0.4;
            final double reach = 1800 + random.nextDouble() * 8000;
            gate(Math.sin(angle) * reach, 500 + random.nextDouble() * 1500, Math.cos(angle) * reach,
                    random.nextDouble() * 180, i % 3);
        }

        // the targets, last, in the open ring round the start: a range of dummies off to the
        // left, another few behind, and drones hovering all round
        range(RANGE_HEADING, RANGE_OUT, 5);
        range(START_YAW + 180, RANGE_OUT + 50, 3);
        for (int i = 0; i < DRONES; i++) {
            final double heading = 10 + 360.0 * i / DRONES;
            final Vec3 at = Num.heading(heading).multiply(i % 2 == 0 ? 800 : 1200);
            final double low = 280 + 110 * (i % 4);
            pieces.add(new Piece(new Block(at.x(), at.z(), DRONE_SIZE / 2, DRONE_SIZE / 2, heading,
                    low, low + DRONE_HEIGHT), Kind.DRONE, i % 3));
        }

        // and the airships, high over everything else, going slowly round far out
        for (int i = 0; i < AIRSHIPS.length; i++) {
            final double[] a = AIRSHIPS[i];
            final Route route = new Route(a[0], a[1], a[2], a[3]);
            pieces.add(Piece.moving(AIRSHIP, route.at(0), Kind.AIRSHIP, i % 2, route));
        }

        // and the platforms, standing at their lowest, each heading out from the start
        for (int i = 0; i < PLATFORMS.size(); i++) {
            final Berth berth = PLATFORMS.get(i);
            final Vec3 at = Num.heading(berth.heading()).multiply(berth.out());
            pieces.add(Piece.moving(berth.deck().shape(), new Place(at.x(), at.z(), LIFT_LOW, berth.heading()),
                    Kind.PLATFORM, i, null));
        }

        // and on the barge, a watermelon stall riding on it
        for (int i = 0; i < pieces.size(); i++) {
            if (pieces.get(i).kind() == Kind.PLATFORM && Deck.of(pieces.get(i).shape()).equals(BARGE)) {
                stall(i);
            }
        }

        // and the roads, last, so that nothing else is numbered differently for them
        roads(roads);
    }

    /**
     * The fruit stall, on platform {@code barge}, toward the back of it: a table ({@link #TABLE},
     * {@link Furniture#TABLE}), a layer of watermelons on it, a few more on the deck beside it,
     * ready to go up, and pineapples standing on the deck at the other end. Each is loose: it stands
     * on what is under it, and goes where that goes ({@link Prop}).
     */
    private void stall(final int barge) {
        final Place deck = pieces.get(barge).place();
        final Place table = on(deck, TABLE.across(), TABLE.along(), 0);
        pieces.add(Piece.loose(Furniture.TABLE, Kind.FURNITURE, table, 0));
        for (int row = 0; row < 3; row++) {
            for (int k = 0; k < 6; k++) {
                final double across = (k - 2.5) * (Fruit.WATERMELON.length() + 2);
                final double along = (row - 1) * (Fruit.WATERMELON.width() + 3);
                pieces.add(Piece.loose(Fruit.WATERMELON, Kind.FRUIT, on(table, across, along, Furniture.TABLE.height()),
                        (row * 6 + k) % 3));
            }
        }
        final double[][] heap = {{175, -150}, {175, -110}, {215, -130}, {215, -84}};
        for (int i = 0; i < heap.length; i++) {
            pieces.add(Piece.loose(Fruit.WATERMELON, Kind.FRUIT, on(deck, heap[i][0], heap[i][1], 0), i % 3));
        }
        final double[][] pineapples = {{-160, -90}, {-182, -114}, {-160, -138}};
        for (int i = 0; i < pineapples.length; i++) {
            pieces.add(Piece.loose(Fruit.PINEAPPLE, Kind.FRUIT, on(deck, pineapples[i][0], pineapples[i][1], 0), i));
        }
        // and what the people sit on, where they sit: a little back of its middle, facing their way
        for (final Cast cast : CAST) {
            if (cast.sitting() && cast.deck().equals(BARGE)) {
                final Vec3 ahead = Num.heading(cast.facing()).multiply(cast.seat().seat());
                final Place seat = on(deck, cast.across() + ahead.x(), cast.along() + ahead.z(), 0);
                pieces.add(Piece.loose(cast.seat(), Kind.FURNITURE,
                        new Place(seat.x(), seat.z(), seat.height(), Num.wrap(deck.yaw() + cast.facing())), 0));
            }
        }
    }

    /** A place {@code across}, {@code along} and {@code up} in the frame of {@code at}, heading as it heads. */
    private static Place on(final Place at, final double across, final double along, final double up) {
        final Vec3 spot = new Vec3(at.x(), 0, at.z()).add(Num.right(at.yaw()).multiply(across))
                .add(Num.heading(at.yaw()).multiply(along));
        return new Place(spot.x(), spot.z(), at.height() + up, at.yaw());
    }

    /** An airship's solid parts, at {@code place}: the hull, the fins, the gondola, the pylons and the engines. */
    public static List<Block> airship(final Place place) {
        return AIRSHIP.solid(place);
    }

    /** How high the airships' hulls run, each one's line through the middle of its hull: for the graphics. */
    public static double hullHeight(final Piece airship) {
        return airship.block().bottom() + AIRSHIP_BOX.halfUp() - AIRSHIP_BOX.up();
    }

    /**
     * A row of {@code count} dummies, {@code out} from the start along {@code heading}, each
     * facing back toward it, side by side.
     */
    private void range(final double heading, final double out, final int count) {
        final Vec3 middle = Num.heading(heading).multiply(out);
        final Vec3 across = Num.right(heading);
        for (int k = 0; k < count; k++) {
            final Vec3 at = middle.add(across.multiply((k - (count - 1) / 2.0) * DUMMY_APART));
            pieces.add(new Piece(Block.standing(at.x(), at.z(), DUMMY_WIDTH, DUMMY_DEPTH, DUMMY_HEIGHT, heading),
                    Kind.DUMMY, k % 2));
        }
    }

    public List<Piece> pieces() {
        return List.copyOf(pieces);
    }

    /** The gates' openings, to fly through. */
    public List<Gate> gates() {
        return List.copyOf(gates);
    }

    public Obstacles obstacles() {
        // each piece's solid blocks, numbered as the piece is
        final List<Block> blocks = new ArrayList<>();
        final List<Integer> numbers = new ArrayList<>();
        for (int i = 0; i < pieces.size(); i++) {
            for (final Block block : pieces.get(i).solid()) {
                blocks.add(block);
                numbers.add(i);
            }
        }
        return new Obstacles(blocks, gates, numbers);
    }

    /**
     * A square frame standing up in the air, centred at the given height: four bars, each a
     * block turned with the gate, round an open middle.
     */
    private void gate(final double x, final double height, final double z, final double yaw, final int tint) {
        final double half = GATE_SPAN / 2;
        final double bar = GATE_BAR / 2;
        final double c = Math.cos(Math.toRadians(yaw));
        final double s = Math.sin(Math.toRadians(yaw));
        // top and bottom run the full width; the sides stand between them
        pieces.add(new Piece(new Block(x, z, half + bar, bar, yaw, height + half - bar, height + half + bar),
                Kind.GATE, tint));
        pieces.add(new Piece(new Block(x, z, half + bar, bar, yaw, height - half - bar, height - half + bar),
                Kind.GATE, tint));
        for (final double side : new double[] {-half, half}) {
            // across the gate's own width, turned with it as Rotate(yaw, Y) turns (side, 0, 0)
            pieces.add(new Piece(new Block(x + side * c, z - side * s, bar, bar, yaw, height - half, height + half),
                    Kind.GATE, tint));
        }
        // the hole the bars stand round: each bar's inner edge is half a bar in from its middle
        gates.add(new Gate(x, z, yaw, height, GATE_SPAN - GATE_BAR));
    }

    private static double spot(final Random random) {
        return (random.nextDouble() * 2 - 1) * (Body.EDGE - 400);
    }
}
