package com.example.fxsuite.avatar.sim;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.example.fxsuite.avatar.sim.Frame.Kind;
import com.example.fxsuite.avatar.sim.Frame.Node;
import com.example.fxsuite.avatar.sim.Frame.Rod;
import com.example.fxsuite.avatar.sim.Frame.Tube;

/**
 * A sidecar's chassis: a few steel tubes welded together ({@link Frame}), holding a third wheel out
 * to one side of a motorcycle and bolted to the motorcycle's own frame where its {@link Kind#MOUNT}
 * nodes are. Described as if on the motorcycle's right, {@code across} being how far out from its
 * middle; mounted on its {@code side}, right or left, it is that mirrored ({@link #placed}).
 *
 * <p>Its wheel is where it is set: {@code along} and {@code out}, standing on the ground, on a stub
 * axle out from its outer rail; the node on the rail where the stub axle starts, and the stub
 * axle's end, go wherever the wheel is set ({@link Mounting}), along the rail. From these come what a
 * sidecar outfit is set up by: how far out its wheel is ({@link #track}), and how far ahead of the
 * motorcycle's back wheel ({@link #lead}).
 *
 * @param side     which side of the motorcycle it is on
 * @param frame    its tubes, as if on the right
 * @param mounting how its wheel is held: the outer rail's ends, and the nodes its stub axle runs between
 * @param along    how far ahead its wheel's hub is
 * @param out      how far out its wheel's hub is, from the motorcycle's middle
 * @param radius   the wheel's radius
 * @param width    the wheel's width
 */
public record Sidecar(Side side, Frame frame, Mounting mounting, double along, double out, double radius,
                      double width) {

    /** Which side of the motorcycle: its right, as where traffic keeps right; or its left, as where it keeps left. */
    public enum Side {
        RIGHT(1), LEFT(-1);

        private final double sign;

        Side(final double sign) {
            this.sign = sign;
        }

        /** +1 on the right, -1 on the left: what out from the middle is multiplied by to be across. */
        public double sign() {
            return sign;
        }
    }

    /**
     * How the wheel is held: the outer rail runs from node {@code front} to node {@code back}; the
     * stub axle from node {@code axle}, on that rail, to node {@code stub}, beside the wheel. Those
     * two go wherever the wheel is set.
     */
    public record Mounting(int front, int back, int axle, int stub) {
    }

    /** How far inside the wheel's middle the stub axle ends. */
    private static final double STUB_GAP = 2;

    public Sidecar {
        if (radius <= 0 || width <= 0) {
            throw new IllegalArgumentException("a wheel has a size");
        }
        // the wheel, along the outer rail no further than its ends; the stub axle held from the rail there
        final Node front = frame.nodes().get(mounting.front());
        final Node back = frame.nodes().get(mounting.back());
        final double rail = front.along() - back.along();
        final double t = rail == 0 ? 0 : Math.clamp((front.along() - along) / rail, 0, 1);
        along = front.along() - t * rail;
        frame = frame.moved(mounting.axle(), front.across() + t * (back.across() - front.across()), along,
                        front.up() + t * (back.up() - front.up()))
                .moved(mounting.stub(), out - width / 2 - STUB_GAP, along, radius);
    }

    // the nodes of the boxer's sidecar frame, by name
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
     * and clear of its cylinders; clamped to the motorcycle's own frame ({@link Frames#BOXER}) at
     * four points, so it cannot twist: low, at the foot of the right down tube and at the back of the
     * cradle; high, on the right down tube and on the right seat rail. Its wheel on a stub axle off
     * the rear part of the outer rail, about a metre and a third out from the motorcycle's middle and
     * a little ahead of its back wheel, as a sidecar's is set up, so the outfit runs straight. Tubes
     * 38 mm across, 2.5 mm walls.
     */
    public static final Sidecar BOXER = new Sidecar(Side.RIGHT, new Frame("the boxer's sidecar", List.of(
            new Node("front, low, on the motorcycle", 12, 46, 34, Kind.MOUNT),
            new Node("back, low, on the motorcycle", 12, -18, 10, Kind.MOUNT),
            new Node("front, high, on the motorcycle", 3.6, 40.4, 59.2, Kind.MOUNT),
            new Node("back, high, on the motorcycle", 10, -46, 66.2, Kind.MOUNT),
            new Node("inner rail, front", 56, 75, 24),
            new Node("inner rail, back", 56, -95, 24),
            new Node("outer rail, front", 118, 62, 24),
            new Node("outer rail, back", 118, -100, 24),
            new Node("outer rail, at the axle", 118, -70, 24),
            new Node("stub axle's end", 124, -70, 28)),
            List.of(
                    // the struts out from the motorcycle, low and high, to the inner rail's ends
                    new Rod(FRONT_LOW, INNER_FRONT), new Rod(BACK_LOW, INNER_BACK),
                    new Rod(FRONT_HIGH, INNER_FRONT), new Rod(BACK_HIGH, INNER_BACK),
                    // the frame: the inner rail, the outer one (in two, the axle between), the cross tubes
                    new Rod(INNER_FRONT, INNER_BACK), new Rod(OUTER_FRONT, AXLE), new Rod(AXLE, OUTER_BACK),
                    new Rod(INNER_FRONT, OUTER_FRONT), new Rod(INNER_BACK, OUTER_BACK),
                    // braced across, and the stub axle out to the wheel
                    new Rod(INNER_FRONT, OUTER_BACK), new Rod(AXLE, STUB)),
            new Tube(1.9, 0.25)),
            new Mounting(OUTER_FRONT, OUTER_BACK, AXLE, STUB), -70, 132, 28, 12);

    /** Its wheel's hub, as if on the right: where it is set, standing on the ground. */
    public Node hub() {
        return new Node("the wheel's hub", out, along, radius);
    }

    /** Its frame where it is on the motorcycle: on its side, right or left. */
    public Frame placed() {
        return side == Side.RIGHT ? frame : frame.mirrored();
    }

    /** {@code node} where it is on the motorcycle: across, right or left. */
    public Node placed(final Node node) {
        return node.at(node.across() * side.sign(), node.along(), node.up());
    }

    /** How far out its wheel is from the motorcycle's middle, whichever side. */
    public double track() {
        return out;
    }

    /** How far ahead of the motorcycle's back wheel, at {@code back} along, its wheel is. */
    public double lead(final double back) {
        return along - back;
    }

    /** Whether node {@code index} goes wherever the wheel is set, rather than being placed itself. */
    public boolean follows(final int index) {
        return index == mounting.axle() || index == mounting.stub();
    }

    /** The same, its frame {@code reshaped}: the wheel stays set, the stub axle still on the rail. */
    public Sidecar with(final Frame reshaped) {
        return new Sidecar(side, reshaped, mounting, along, out, radius, width);
    }

    /** The same, its wheel set {@code along} and {@code out}: along the outer rail, no further than its ends. */
    public Sidecar wheel(final double along, final double out) {
        return new Sidecar(side, frame, mounting, along, out, radius, width);
    }

    /** The same, mounted on {@code side} of the motorcycle. */
    public Sidecar on(final Side side) {
        return new Sidecar(side, frame, mounting, along, out, radius, width);
    }

    /** The same, its wheel {@code radius}, standing on the ground. */
    public Sidecar wheeled(final double radius) {
        return new Sidecar(side, frame, mounting, along, out, radius, width);
    }

    /** As text: its frame's ({@link Frame#text}), and lines for its side, its wheel and how it is held. */
    public String text() {
        final List<Node> nodes = frame.nodes();
        return frame.text() + "side " + side.name().toLowerCase(Locale.ROOT) + '\n'
                + "wheel " + Frame.number(along) + ' ' + Frame.number(out) + ' ' + Frame.number(radius) + ' '
                + Frame.number(width) + '\n'
                + "mounting " + Frame.quoted(nodes.get(mounting.front()).name()) + ' '
                + Frame.quoted(nodes.get(mounting.back()).name()) + ' ' + Frame.quoted(nodes.get(mounting.axle()).name())
                + ' ' + Frame.quoted(nodes.get(mounting.stub()).name()) + '\n';
    }

    /** A sidecar from its {@link #text}. */
    public static Sidecar read(final String text) {
        final Frame frame = Frame.read(text);
        Side side = Side.RIGHT;
        double[] wheel = null;
        Mounting mounting = null;
        for (final String line : text.split("\\R")) {
            final List<String> words = Frame.words(line);
            if (words.isEmpty()) {
                continue;
            }
            switch (words.getFirst()) {
                case "side" -> side = Side.valueOf(words.get(1).toUpperCase(Locale.ROOT));
                case "wheel" -> wheel = new double[] {Double.parseDouble(words.get(1)), Double.parseDouble(words.get(2)),
                        Double.parseDouble(words.get(3)), Double.parseDouble(words.get(4))};
                case "mounting" -> {
                    final List<Integer> at = new ArrayList<>();
                    for (int w = 1; w <= 4; w++) {
                        final int node = frame.node(words.get(w));
                        if (node < 0) {
                            throw new IllegalArgumentException("its wheel held at a node there is not: " + words.get(w));
                        }
                        at.add(node);
                    }
                    mounting = new Mounting(at.get(0), at.get(1), at.get(2), at.get(3));
                }
                default -> {
                    // the frame's
                }
            }
        }
        if (wheel == null || mounting == null) {
            throw new IllegalArgumentException("a sidecar has a wheel, and holds it");
        }
        return new Sidecar(side, frame, mounting, wheel[0], wheel[1], wheel[2], wheel[3]);
    }
}
