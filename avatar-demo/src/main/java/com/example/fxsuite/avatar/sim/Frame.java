package com.example.fxsuite.avatar.sim;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Anything built of rigid rods joined at their ends, as a graph: a motorcycle's frame, a sidecar's
 * chassis, and, to come, a tower or a building's beams. Pure figures, in whatever it belongs to's
 * own frame: {@code across} to its right, {@code along} ahead, {@code up} from the ground, in
 * centimetres.
 *
 * <ul>
 *   <li><b>Nodes</b> are where rods meet, each named, and of a {@link Kind}: welded, bolted to what
 *       carries it, or a hinge (a pivot to come; for now held as a weld).</li>
 *   <li><b>Rods</b> each run straight from one node to another. Each is a {@link Tube}: its own,
 *       or the frame's.</li>
 *   <li><b>From these</b> come its length of tube, its mass and where its mass is, which rods meet at
 *       a node, whether it is one piece, and how much it leans on its welds to keep its shape
 *       ({@link #slack}).</li>
 *   <li><b>Shaped</b> one change at a time: a node moved, a rod split in two at a new node, a rod
 *       added between two nodes or taken away, a rod's tube changed. Each gives a new frame.</li>
 *   <li><b>Kept as text</b> ({@link #text}, {@link #read}): a line for its tube, one for each node
 *       and one for each rod, the rods by their nodes' names, to read and edit by hand.</li>
 * </ul>
 *
 * @param name  what it is
 * @param nodes where the rods meet
 * @param rods  the rods
 * @param tube  the tube its rods are, unless a rod has its own
 */
public record Frame(String name, List<Node> nodes, List<Rod> rods, Tube tube) {

    /** Steel, in kilograms a cubic centimetre. */
    public static final double STEEL = 0.00785;

    /** What a node is: rods welded together there; bolted to what carries it; or a pivot. */
    public enum Kind { WELD, MOUNT, HINGE }

    /** A point of the frame. */
    public record Node(String name, double across, double along, double up, Kind kind) {

        public Node(final String name, final double across, final double along, final double up) {
            this(name, across, along, up, Kind.WELD);
        }

        /** How far it is from {@code other}. */
        public double distance(final Node other) {
            return Math.sqrt(Math.pow(across - other.across, 2) + Math.pow(along - other.along, 2)
                    + Math.pow(up - other.up, 2));
        }

        /** The same, moved to {@code across}, {@code along}, {@code up}. */
        public Node at(final double across, final double along, final double up) {
            return new Node(name, across, along, up, kind);
        }

        /** Whether it is bolted to what carries the frame. */
        public boolean mount() {
            return kind == Kind.MOUNT;
        }
    }

    /** A round tube: its outside radius and its wall's thickness, in centimetres. */
    public record Tube(double radius, double wall) {

        public Tube {
            if (radius <= 0 || wall <= 0 || wall >= radius) {
                throw new IllegalArgumentException("a tube is thicker than its wall: " + radius + ", " + wall);
            }
        }

        /** How much steel a centimetre of it is, in square centimetres. */
        public double area() {
            final double inside = radius - wall;
            return Math.PI * (radius * radius - inside * inside);
        }
    }

    /** A rod from node {@code from} to node {@code to}, by their places among the nodes; its own tube, or null for the frame's. */
    public record Rod(int from, int to, Tube tube) {

        public Rod(final int from, final int to) {
            this(from, to, null);
        }
    }

    /** A point, as a node is, without a name: where its mass is, say. */
    public record Point(double across, double along, double up) {
    }

    public Frame {
        nodes = List.copyOf(nodes);
        rods = List.copyOf(rods);
        for (final Rod rod : rods) {
            if (rod.from() < 0 || rod.to() < 0 || rod.from() >= nodes.size() || rod.to() >= nodes.size()) {
                throw new IllegalArgumentException(name + ": a rod to a node there is not: " + rod);
            }
        }
    }

    /** The tube rod {@code rod} is: its own, or the frame's. */
    public Tube tube(final Rod rod) {
        return rod.tube() != null ? rod.tube() : tube;
    }

    /** How long rod {@code rod} is. */
    public double length(final Rod rod) {
        return nodes.get(rod.from()).distance(nodes.get(rod.to()));
    }

    /** How much tube it takes, all told, in centimetres. */
    public double steel() {
        return rods.stream().mapToDouble(this::length).sum();
    }

    /** How heavy its rods are, all told, in kilograms. */
    public double mass() {
        return rods.stream().mapToDouble(r -> length(r) * tube(r).area() * STEEL).sum();
    }

    /** Where its mass is: each rod's at its middle, as heavy as it is. */
    public Point centre() {
        double mass = 0;
        double across = 0;
        double along = 0;
        double up = 0;
        for (final Rod rod : rods) {
            final double m = length(rod) * tube(rod).area() * STEEL;
            final Node a = nodes.get(rod.from());
            final Node b = nodes.get(rod.to());
            mass += m;
            across += m * (a.across() + b.across()) / 2;
            along += m * (a.along() + b.along()) / 2;
            up += m * (a.up() + b.up()) / 2;
        }
        return mass == 0 ? new Point(0, 0, 0) : new Point(across / mass, along / mass, up / mass);
    }

    /** The rods that meet at node {@code node}, by their places among the rods. */
    public List<Integer> rodsAt(final int node) {
        final List<Integer> at = new ArrayList<>();
        for (int r = 0; r < rods.size(); r++) {
            if (rods.get(r).from() == node || rods.get(r).to() == node) {
                at.add(r);
            }
        }
        return at;
    }

    /** By its name, its place among the nodes; -1 if none is so named. */
    public int node(final String named) {
        for (int n = 0; n < nodes.size(); n++) {
            if (nodes.get(n).name().equals(named)) {
                return n;
            }
        }
        return -1;
    }

    /** Whether every node is joined to every other through the rods: one piece, not loose ones. */
    public boolean connected() {
        if (nodes.isEmpty()) {
            return true;
        }
        final boolean[] reached = new boolean[nodes.size()];
        final List<Integer> next = new ArrayList<>(List.of(0));
        reached[0] = true;
        while (!next.isEmpty()) {
            final int at = next.removeLast();
            for (final Rod rod : rods) {
                final int other = rod.from() == at ? rod.to() : rod.to() == at ? rod.from() : -1;
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

    /**
     * How many ways it could still move if every node were a pin rather than a weld, by Maxwell's
     * count: each node free to go three ways, but those bolted down, each rod taking one away, and,
     * if nothing holds it, the six ways it moves as a whole besides. Above 0, it keeps its shape only
     * because its welds hold, and that many braces would make it hold by its rods alone.
     */
    public int slack() {
        final long held = nodes.stream().filter(Node::mount).count();
        final long free = nodes.size() - held;
        final int whole = held == 0 ? 6 : 0;
        return (int) Math.max(0, 3 * free - rods.size() - whole);
    }

    /** What is wrong with it, if anything: a rod from a node to itself, two rods alike, a node no rod meets, two nodes one name, or in pieces. */
    public List<String> problems() {
        final List<String> wrong = new ArrayList<>();
        for (int r = 0; r < rods.size(); r++) {
            final Rod rod = rods.get(r);
            if (rod.from() == rod.to()) {
                wrong.add("a rod from " + nodes.get(rod.from()).name() + " to itself");
            }
            for (int s = r + 1; s < rods.size(); s++) {
                final Rod other = rods.get(s);
                if (Math.min(rod.from(), rod.to()) == Math.min(other.from(), other.to())
                        && Math.max(rod.from(), rod.to()) == Math.max(other.from(), other.to())) {
                    wrong.add("two rods between " + nodes.get(rod.from()).name() + " and " + nodes.get(rod.to()).name());
                }
            }
        }
        for (int n = 0; n < nodes.size(); n++) {
            if (rodsAt(n).isEmpty()) {
                wrong.add("no rod meets " + nodes.get(n).name());
            }
            for (int m = n + 1; m < nodes.size(); m++) {
                if (nodes.get(n).name().equals(nodes.get(m).name())) {
                    wrong.add("two nodes named " + nodes.get(n).name());
                }
            }
        }
        if (!connected()) {
            wrong.add("in pieces: not every node is joined to the rest");
        }
        return wrong;
    }

    /** The same, node {@code index} moved to {@code across}, {@code along}, {@code up}. */
    public Frame moved(final int index, final double across, final double along, final double up) {
        final List<Node> now = new ArrayList<>(nodes);
        now.set(index, nodes.get(index).at(across, along, up));
        return new Frame(name, now, rods, tube);
    }

    /** The same, rod {@code index} split in two at a new node, named {@code named}, in its middle. */
    public Frame split(final int index, final String named) {
        final Rod rod = rods.get(index);
        final Node a = nodes.get(rod.from());
        final Node b = nodes.get(rod.to());
        final List<Node> now = new ArrayList<>(nodes);
        now.add(new Node(named, (a.across() + b.across()) / 2, (a.along() + b.along()) / 2, (a.up() + b.up()) / 2));
        final int middle = now.size() - 1;
        final List<Rod> split = new ArrayList<>(rods);
        split.set(index, new Rod(rod.from(), middle, rod.tube()));
        split.add(index + 1, new Rod(middle, rod.to(), rod.tube()));
        return new Frame(name, now, split, tube);
    }

    /** The same, a rod added from node {@code from} to node {@code to}, of the frame's tube. */
    public Frame joined(final int from, final int to) {
        final List<Rod> now = new ArrayList<>(rods);
        now.add(new Rod(from, to));
        return new Frame(name, nodes, now, tube);
    }

    /** The same, rod {@code index} taken away. */
    public Frame removed(final int index) {
        final List<Rod> now = new ArrayList<>(rods);
        now.remove(index);
        return new Frame(name, nodes, now, tube);
    }

    /** The same, rod {@code index} of tube {@code made} (null: the frame's). */
    public Frame sized(final int index, final Tube made) {
        final List<Rod> now = new ArrayList<>(rods);
        final Rod rod = rods.get(index);
        now.set(index, new Rod(rod.from(), rod.to(), made));
        return new Frame(name, nodes, now, tube);
    }

    /** The same, its own tube, the one its rods are unless they have their own, {@code made}. */
    public Frame tubed(final Tube made) {
        return new Frame(name, nodes, rods, made);
    }

    /** The same, mirrored left for right: what is on the right of the middle now on the left. */
    public Frame mirrored() {
        return new Frame(name, nodes.stream().map(n -> n.at(-n.across(), n.along(), n.up())).toList(), rods, tube);
    }

    /**
     * As text: its name, its tube, a line for each node (its name, where it is, and its kind if not
     * a weld), and one for each rod (its two nodes' names, and its tube if not the frame's).
     * Names are in quotes; a line beginning {@code #} is a remark. {@link #read} reads it back.
     */
    public String text() {
        final StringBuilder out = new StringBuilder();
        out.append("frame ").append(quoted(name)).append('\n');
        out.append("tube ").append(number(tube.radius())).append(' ').append(number(tube.wall())).append('\n');
        for (final Node node : nodes) {
            out.append("node ").append(quoted(node.name())).append(' ').append(number(node.across())).append(' ')
                    .append(number(node.along())).append(' ').append(number(node.up()));
            if (node.kind() != Kind.WELD) {
                out.append(' ').append(node.kind().name().toLowerCase(Locale.ROOT));
            }
            out.append('\n');
        }
        for (final Rod rod : rods) {
            out.append("rod ").append(quoted(nodes.get(rod.from()).name())).append(' ')
                    .append(quoted(nodes.get(rod.to()).name()));
            if (rod.tube() != null) {
                out.append(" tube ").append(number(rod.tube().radius())).append(' ').append(number(rod.tube().wall()));
            }
            out.append('\n');
        }
        return out.toString();
    }

    /** A frame from its {@link #text}; any line not about the frame is left for whatever else reads it. */
    public static Frame read(final String text) {
        String name = "frame";
        Tube tube = null;
        final List<Node> nodes = new ArrayList<>();
        final List<String[]> rodLines = new ArrayList<>();
        int number = 0;
        for (final String raw : text.split("\\R")) {
            number++;
            final List<String> words = words(raw);
            if (words.isEmpty() || words.getFirst().startsWith("#")) {
                continue;
            }
            try {
                switch (words.getFirst()) {
                    case "frame" -> name = words.get(1);
                    case "tube" -> tube = new Tube(Double.parseDouble(words.get(1)), Double.parseDouble(words.get(2)));
                    case "node" -> nodes.add(new Node(words.get(1), Double.parseDouble(words.get(2)),
                            Double.parseDouble(words.get(3)), Double.parseDouble(words.get(4)),
                            words.size() > 5 ? Kind.valueOf(words.get(5).toUpperCase(Locale.ROOT)) : Kind.WELD));
                    case "rod" -> rodLines.add(words.toArray(String[]::new));
                    default -> {
                        // not the frame's: some other part of whatever it belongs to
                    }
                }
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("line " + number + ": " + raw, e);
            }
        }
        if (tube == null) {
            throw new IllegalArgumentException("no tube for its rods");
        }
        final Frame named = new Frame(name, nodes, List.of(), tube);
        final List<Rod> rods = new ArrayList<>();
        for (final String[] line : rodLines) {
            final int from = named.node(line[1]);
            final int to = named.node(line[2]);
            if (from < 0 || to < 0) {
                throw new IllegalArgumentException("a rod to a node there is not: " + String.join(" ", line));
            }
            rods.add(new Rod(from, to, line.length > 5 && line[3].equals("tube")
                    ? new Tube(Double.parseDouble(line[4]), Double.parseDouble(line[5])) : null));
        }
        return new Frame(name, nodes, rods, tube);
    }

    /** A line's words: split at spaces, a word in quotes kept whole. */
    static List<String> words(final String line) {
        final List<String> words = new ArrayList<>();
        int i = 0;
        while (i < line.length()) {
            final char c = line.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
            } else if (c == '"') {
                final int end = line.indexOf('"', i + 1);
                if (end < 0) {
                    throw new IllegalArgumentException("a quote not closed: " + line);
                }
                words.add(line.substring(i + 1, end));
                i = end + 1;
            } else {
                int end = i;
                while (end < line.length() && !Character.isWhitespace(line.charAt(end))) {
                    end++;
                }
                words.add(line.substring(i, end));
                i = end;
            }
        }
        return words;
    }

    static String quoted(final String name) {
        return '"' + name.replace("\"", "'") + '"';
    }

    static String number(final double value) {
        final double rounded = Math.round(value * 100) / 100.0;
        return rounded == Math.rint(rounded) ? String.valueOf((long) rounded) : String.valueOf(rounded);
    }
}
