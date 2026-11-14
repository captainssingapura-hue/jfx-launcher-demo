package com.example.fxsuite.avatar.sim;

import java.util.ArrayList;
import java.util.List;

/**
 * A sidecar's chassis: a few steel tubes, welded together at their ends, holding a third wheel out
 * to one side of a motorcycle and bolted to the motorcycle's own frame. Pure figures, in the
 * motorcycle's frame: {@code out} from its middle toward the sidecar's side, {@code along} ahead,
 * {@code up} from the ground, in centimetres. Described as if on the motorcycle's right; mounted on
 * its {@code side}, right or left, it is that mirrored ({@link #placed}).
 *
 * <p>Its {@code joints} are where tubes meet, each named; its {@code bars} the tubes, each from one
 * joint to another; those joints marked {@link Joint#mount} are where it is bolted to the
 * motorcycle. Its wheel is where it is set: {@code along} and {@code out}, standing on the ground, on
 * a stub axle out from its outer rail; the joint on the rail where the stub axle starts, and the
 * stub axle's end, go wherever the wheel is set ({@link Mounting}), along the rail. From these come
 * what a sidecar outfit is set up by: how far out its wheel is ({@link #track}), how far ahead of
 * the motorcycle's back wheel ({@link #lead}), and how much steel it takes ({@link #steel},
 * {@link #mass}).
 *
 * @param side     which side of the motorcycle it is on
 * @param joints   where the tubes meet, as if on the right
 * @param bars     the tubes
 * @param mounting how its wheel is held: the outer rail's ends, and the joints its stub axle runs between
 * @param tube     the tubes' outside radius, in centimetres
 * @param wall     their wall's thickness
 * @param along    how far ahead its wheel's hub is
 * @param out      how far out its wheel's hub is, from the motorcycle's middle
 * @param radius   the wheel's radius
 * @param width    the wheel's width
 */
public record Sidecar(Side side, List<Joint> joints, List<Bar> bars, Mounting mounting, double tube, double wall,
                      double along, double out, double radius, double width) {

    /** Steel, in kilograms a cubic centimetre. */
    public static final double STEEL = 0.00785;

    /** Which side of the motorcycle: its right, as where traffic keeps right; or its left, as where it keeps left. */
    public enum Side {
        RIGHT(1), LEFT(-1);

        private final double sign;

        Side(final double sign) {
            this.sign = sign;
        }

        /** +1 on the right, -1 on the left: what {@code out} is multiplied by to be across. */
        public double sign() {
            return sign;
        }
    }

    /** A point of the frame: {@code out}, {@code along} and {@code up}; and whether it is bolted to the motorcycle there. */
    public record Joint(String name, double out, double along, double up, boolean mount) {

        public Joint(final String name, final double out, final double along, final double up) {
            this(name, out, along, up, false);
        }

        /** How far it is from {@code other}. */
        public double distance(final Joint other) {
            return Math.sqrt(Math.pow(out - other.out, 2) + Math.pow(along - other.along, 2)
                    + Math.pow(up - other.up, 2));
        }

        /** The same, moved to {@code out}, {@code along}, {@code up}. */
        public Joint at(final double out, final double along, final double up) {
            return new Joint(name, out, along, up, mount);
        }
    }

    /** A tube, from joint {@code from} to joint {@code to}, by their places in the joints. */
    public record Bar(int from, int to) {
    }

    /**
     * How the wheel is held: the outer rail runs from joint {@code front} to joint {@code back}; the
     * stub axle from joint {@code axle}, on that rail, to joint {@code stub}, beside the wheel. Those
     * two go wherever the wheel is set.
     */
    public record Mounting(int front, int back, int axle, int stub) {
    }

    /** How far inside the wheel's middle the stub axle ends: half the wheel, and a little. */
    private static final double STUB_GAP = 2;

    public Sidecar {
        joints = new ArrayList<>(joints);
        for (final Bar bar : bars) {
            if (bar.from() == bar.to() || bar.from() < 0 || bar.to() < 0 || bar.from() >= joints.size()
                    || bar.to() >= joints.size()) {
                throw new IllegalArgumentException("a tube from one joint to another: " + bar);
            }
        }
        if (tube <= wall || wall <= 0 || radius <= 0 || width <= 0) {
            throw new IllegalArgumentException("a tube is thicker than its wall, a wheel has a size");
        }
        // the wheel, along the outer rail no further than its ends; the stub axle held from the rail there
        final Joint front = joints.get(mounting.front());
        final Joint back = joints.get(mounting.back());
        final double rail = front.along() - back.along();
        final double t = rail == 0 ? 0 : Math.clamp((front.along() - along) / rail, 0, 1);
        along = front.along() - t * rail;
        joints.set(mounting.axle(), joints.get(mounting.axle()).at(front.out() + t * (back.out() - front.out()), along,
                front.up() + t * (back.up() - front.up())));
        joints.set(mounting.stub(), joints.get(mounting.stub()).at(out - width / 2 - STUB_GAP, along, radius));
        joints = List.copyOf(joints);
        bars = List.copyOf(bars);
    }

    // the joints of the boxer's sidecar frame, by name
    private static final int FRONT_LOW = 0;
    private static final int BACK_LOW = 1;
    private static final int FRONT_HIGH = 2;
    private static final int BACK_HIGH = 3;
    private static final int INNER_FRONT = 4;
    private static final int INNER_BACK = 5;
    private static final int OUTER_FRONT = 6;
    private static final int OUTER_BACK = 7;
    private static final int AXLE = 8;
    private static final int STUB = 9;

    /**
     * The boxer's sidecar chassis, on its right: a flat frame of four tubes, an inner rail and an
     * outer one with a cross tube at each end, braced across a diagonal, low beside the motorcycle
     * and clear of its cylinders; bolted to the motorcycle at four points, two low struts and two
     * high ones, so it cannot twist; and its wheel on a stub axle off the rear part of the outer rail,
     * about a metre and a third out from the motorcycle's middle and a little ahead of its back
     * wheel, as a sidecar's is set up, so the outfit runs straight. Tubes 38 mm across, 2.5 mm walls.
     */
    public static final Sidecar BOXER = new Sidecar(Side.RIGHT, List.of(
            new Joint("front, low, on the motorcycle", 14, 45, 26, true),
            new Joint("back, low, on the motorcycle", 14, -40, 26, true),
            new Joint("front, high, on the motorcycle", 12, 60, 70, true),
            new Joint("back, high, on the motorcycle", 12, -30, 72, true),
            new Joint("inner rail, front", 56, 75, 24),
            new Joint("inner rail, back", 56, -95, 24),
            new Joint("outer rail, front", 118, 62, 24),
            new Joint("outer rail, back", 118, -100, 24),
            new Joint("outer rail, at the axle", 118, -70, 24),
            new Joint("stub axle's end", 124, -70, 28)),
            List.of(
                    // the struts out from the motorcycle, low and high, to the inner rail's ends
                    new Bar(FRONT_LOW, INNER_FRONT), new Bar(BACK_LOW, INNER_BACK),
                    new Bar(FRONT_HIGH, INNER_FRONT), new Bar(BACK_HIGH, INNER_BACK),
                    // the frame: the inner rail, the outer one (in two, the axle between), the cross tubes
                    new Bar(INNER_FRONT, INNER_BACK), new Bar(OUTER_FRONT, AXLE), new Bar(AXLE, OUTER_BACK),
                    new Bar(INNER_FRONT, OUTER_FRONT), new Bar(INNER_BACK, OUTER_BACK),
                    // braced across, and the stub axle out to the wheel
                    new Bar(INNER_FRONT, OUTER_BACK), new Bar(AXLE, STUB)),
            new Mounting(OUTER_FRONT, OUTER_BACK, AXLE, STUB), 1.9, 0.25, -70, 132, 28, 12);

    /** Its wheel's hub, as if on the right: where it is set, standing on the ground. */
    public Joint hub() {
        return new Joint("the wheel's hub", out, along, radius);
    }

    /** Its joints where they are on the motorcycle: on its side, {@code out} becoming across, right or left. */
    public List<Joint> placed() {
        return joints.stream().map(this::placed).toList();
    }

    /** {@code joint} where it is on the motorcycle: across, right or left. */
    public Joint placed(final Joint joint) {
        return joint.at(joint.out() * side.sign(), joint.along(), joint.up());
    }

    /** How long tube {@code bar} is, in centimetres. */
    public double length(final Bar bar) {
        return joints.get(bar.from()).distance(joints.get(bar.to()));
    }

    /** How much tube it takes, all told, in centimetres. */
    public double steel() {
        return bars.stream().mapToDouble(this::length).sum();
    }

    /** How heavy its tubes are, all told, in kilograms. */
    public double mass() {
        final double inside = tube - wall;
        return steel() * Math.PI * (tube * tube - inside * inside) * STEEL;
    }

    /** How far out its wheel is from the motorcycle's middle, whichever side. */
    public double track() {
        return out;
    }

    /** How far ahead of the motorcycle's back wheel, at {@code back} along, its wheel is. */
    public double lead(final double back) {
        return along - back;
    }

    /** The joints bolted to the motorcycle. */
    public List<Joint> mounts() {
        return joints.stream().filter(Joint::mount).toList();
    }

    /** Whether every joint is joined to every other through the tubes: one frame, not loose pieces. */
    public boolean connected() {
        if (joints.isEmpty()) {
            return true;
        }
        final boolean[] reached = new boolean[joints.size()];
        final List<Integer> next = new ArrayList<>(List.of(0));
        reached[0] = true;
        while (!next.isEmpty()) {
            final int at = next.removeLast();
            for (final Bar bar : bars) {
                final int other = bar.from() == at ? bar.to() : bar.to() == at ? bar.from() : -1;
                if (other >= 0 && !reached[other]) {
                    reached[other] = true;
                    next.add(other);
                }
            }
        }
        for (final boolean r : reached) {
            if (!r) {
                return false;
            }
        }
        return true;
    }

    /** Whether joint {@code index} goes wherever the wheel is set, rather than being placed itself. */
    public boolean follows(final int index) {
        return index == mounting.axle() || index == mounting.stub();
    }

    /** The same, joint {@code index} moved to {@code moved}: what the vehicle bench shapes it by. The wheel stays set. */
    public Sidecar moved(final int index, final Joint moved) {
        final List<Joint> now = new ArrayList<>(joints);
        now.set(index, moved);
        return new Sidecar(side, now, bars, mounting, tube, wall, along, out, radius, width);
    }

    /** The same, its wheel set {@code along} and {@code out}: along the outer rail, no further than its ends. */
    public Sidecar wheel(final double along, final double out) {
        return new Sidecar(side, joints, bars, mounting, tube, wall, along, out, radius, width);
    }

    /** The same, mounted on {@code side} of the motorcycle. */
    public Sidecar on(final Side side) {
        return new Sidecar(side, joints, bars, mounting, tube, wall, along, out, radius, width);
    }

    /** The same, its tubes {@code tube} in radius and its wheel {@code radius}: its wall no thicker than a tube allows. */
    public Sidecar sized(final double tube, final double radius) {
        return new Sidecar(side, joints, bars, mounting, tube, Math.min(wall, tube / 3), along, out, radius, width);
    }

    /** As Java, to paste back as it now is, once it has been shaped on the bench. */
    public String code() {
        final StringBuilder out = new StringBuilder("new Sidecar(Side." + side + ", List.of(\n");
        for (int j = 0; j < joints.size(); j++) {
            final Joint joint = joints.get(j);
            out.append(String.format("        new Joint(\"%s\", %s, %s, %s%s)%s%n", joint.name(), number(joint.out()),
                    number(joint.along()), number(joint.up()), joint.mount() ? ", true" : "",
                    j < joints.size() - 1 ? "," : "),"));
        }
        out.append("        List.of(");
        for (int b = 0; b < bars.size(); b++) {
            out.append(String.format("new Bar(%d, %d)%s", bars.get(b).from(), bars.get(b).to(),
                    b < bars.size() - 1 ? ", " : "),\n"));
        }
        out.append(String.format("        new Mounting(%d, %d, %d, %d), %s, %s, %s, %s, %s, %s)", mounting.front(),
                mounting.back(), mounting.axle(), mounting.stub(), number(tube), number(wall), number(along),
                number(this.out), number(radius), number(width)));
        return out.toString();
    }

    private static String number(final double value) {
        final double rounded = Math.round(value * 10) / 10.0;
        return rounded == Math.rint(rounded) ? String.valueOf((long) rounded) : String.valueOf(rounded);
    }
}
