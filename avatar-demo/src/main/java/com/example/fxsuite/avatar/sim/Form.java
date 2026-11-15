package com.example.fxsuite.avatar.sim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A 3D form as a graph, and nothing more: vertices, each a point with no size, and edges, each a
 * straight line between two of them with no thickness. What it is made of, how thick its parts are
 * and what it bears are for whatever uses it to say (how it is drawn, how it collides, how it
 * stands up), each about its parts by their names: so a building's shape is the same whether it is
 * drawn, walked through or loaded until it falls.
 *
 * <p>In its owner's frame, standing on the ground: {@code across} to its right, {@code along}
 * ahead, {@code up} from the ground, in centimetres; placed in the world as one thing.
 *
 * <ul>
 *   <li><b>Vertices</b> are named, each name its own: the name is what anything else holds it by.</li>
 *   <li><b>Edges</b> join two vertices: straight, or curving as a {@link Curve} says (an arc, a bow, a
 *       corner's sweep), its points worked out along it ({@link #points}). A form need not be one piece: a window's outline stands
 *       apart from the walls round it, until a face (to come) holds it as a hole.</li>
 *   <li><b>From these</b> come how big it is ({@link #bounds}), its edges' length all told, how
 *       many pieces it is in, and what is wrong with it ({@link #problems}): vertices so near they
 *       are the same point, an edge twice, a vertex nothing joins.</li>
 *   <li><b>Kept as text</b> ({@link #text}, {@link #read}): a line for each vertex, one for each
 *       edge by its vertices' names.</li>
 * </ul>
 *
 * @param name     what it is
 * @param vertices its points
 * @param edges    its lines
 */
public record Form(String name, List<Vertex> vertices, List<Edge> edges) {

    /** Points nearer than this, in centimetres (a millimetre), are the same point. */
    public static final double TOLERANCE = 0.1;

    /** A point, named. */
    public record Vertex(String name, double across, double along, double up) {

        public double distance(final Vertex other) {
            return Math.sqrt(Math.pow(across - other.across, 2) + Math.pow(along - other.along, 2)
                    + Math.pow(up - other.up, 2));
        }
    }

    /** A line from vertex {@code from} to vertex {@code to}: straight, or as {@code curve} curves it ({@link Curve}). */
    public record Edge(int from, int to, Curve curve) {

        /** Straight. */
        public Edge(final int from, final int to) {
            this(from, to, null);
        }

        public boolean curved() {
            return curve != null;
        }
    }

    /** A curved edge is drawn, and measured, in this many straight pieces at least. */
    public static final int PIECES = 12;

    /** Where vertex {@code v} is. */
    public Vec3 at(final int v) {
        final Vertex p = vertices.get(v);
        return new Vec3(p.across(), p.along(), p.up());
    }

    /**
     * {@code edge}'s points, end to end: its two ends, if it runs straight; if it curves, so many along
     * its curve that none of its straight pieces is longer than {@code longest} (and {@link #PIECES} at least).
     */
    public java.util.List<Vec3> points(final Edge edge, final double longest) {
        final Vec3 a = at(edge.from());
        final Vec3 b = at(edge.to());
        if (!edge.curved()) {
            return java.util.List.of(a, b);
        }
        double length = 0;
        Vec3 last = a;
        for (int k = 1; k <= 64; k++) {
            final Vec3 p = edge.curve().at(a, b, k / 64.0);
            length += p.distance(last);
            last = p;
        }
        final int pieces = Math.max(PIECES, (int) Math.ceil(length / longest));
        final java.util.List<Vec3> points = new ArrayList<>(pieces + 1);
        for (int k = 0; k <= pieces; k++) {
            points.add(k == 0 ? a : k == pieces ? b : edge.curve().at(a, b, (double) k / pieces));
        }
        return points;
    }

    /** The box it all fits in. */
    public record Bounds(double leftmost, double rightmost, double back, double front, double low, double high) {

        public double width() {
            return rightmost - leftmost;
        }

        public double depth() {
            return front - back;
        }

        public double height() {
            return high - low;
        }
    }

    public Form {
        vertices = List.copyOf(vertices);
        edges = List.copyOf(edges);
        for (final Edge edge : edges) {
            if (edge.from() < 0 || edge.to() < 0 || edge.from() >= vertices.size() || edge.to() >= vertices.size()) {
                throw new IllegalArgumentException("an edge to a vertex there is not: " + edge);
            }
            if (edge.from() == edge.to()) {
                throw new IllegalArgumentException("an edge from a vertex to itself: " + vertices.get(edge.from()).name());
            }
        }
    }

    /**
     * The same shape {@code k} times as big every way, about its origin (on the ground, so it still
     * stands there): every vertex as far again from it, every curve as deep again; named and joined
     * as it is.
     */
    public Form scaled(final double k) {
        if (!(k > 0) || Double.isInfinite(k)) {
            throw new IllegalArgumentException("scaled by " + k + ": a building is scaled by something more than nothing");
        }
        if (k == 1) {
            return this;
        }
        return new Form(name, vertices.stream().map(v -> new Vertex(v.name(), v.across() * k, v.along() * k, v.up() * k))
                .toList(), edges.stream().map(e -> new Edge(e.from(), e.to(), e.curve() == null ? null : e.curve().scaled(k)))
                .toList());
    }

    /** The vertex named {@code named}'s index; -1 if there is none. */
    public int vertex(final String named) {
        for (int v = 0; v < vertices.size(); v++) {
            if (vertices.get(v).name().equals(named)) {
                return v;
            }
        }
        return -1;
    }

    /** How long {@code edge} is: straight, end to end; curved, along its curve. */
    public double length(final Edge edge) {
        if (!edge.curved()) {
            return vertices.get(edge.from()).distance(vertices.get(edge.to()));
        }
        final java.util.List<Vec3> points = points(edge, 5);
        double length = 0;
        for (int k = 1; k < points.size(); k++) {
            length += points.get(k).distance(points.get(k - 1));
        }
        return length;
    }

    /** Its edges' length, all told. */
    public double length() {
        return edges.stream().mapToDouble(this::length).sum();
    }

    public Bounds bounds() {
        if (vertices.isEmpty()) {
            return new Bounds(0, 0, 0, 0, 0, 0);
        }
        double left = Double.POSITIVE_INFINITY;
        double right = Double.NEGATIVE_INFINITY;
        double back = Double.POSITIVE_INFINITY;
        double front = Double.NEGATIVE_INFINITY;
        double low = Double.POSITIVE_INFINITY;
        double high = Double.NEGATIVE_INFINITY;
        for (final Vertex v : vertices) {
            left = Math.min(left, v.across());
            right = Math.max(right, v.across());
            back = Math.min(back, v.along());
            front = Math.max(front, v.along());
            low = Math.min(low, v.up());
            high = Math.max(high, v.up());
        }
        for (final Edge edge : edges) {
            if (edge.curved()) {
                for (final Vec3 p : points(edge, 50)) {
                    left = Math.min(left, p.x());
                    right = Math.max(right, p.x());
                    back = Math.min(back, p.y());
                    front = Math.max(front, p.y());
                    low = Math.min(low, p.z());
                    high = Math.max(high, p.z());
                }
            }
        }
        return new Bounds(left, right, back, front, low, high);
    }

    /** How many pieces it is in: vertices joined by edges, each lot one piece. */
    public int pieces() {
        final int[] lot = new int[vertices.size()];
        for (int v = 0; v < lot.length; v++) {
            lot[v] = v;
        }
        for (final Edge edge : edges) {
            final int a = root(lot, edge.from());
            final int b = root(lot, edge.to());
            if (a != b) {
                lot[a] = b;
            }
        }
        int pieces = 0;
        for (int v = 0; v < lot.length; v++) {
            if (root(lot, v) == v) {
                pieces++;
            }
        }
        return pieces;
    }

    private static int root(final int[] lot, final int v) {
        int r = v;
        while (lot[r] != r) {
            lot[r] = lot[lot[r]];
            r = lot[r];
        }
        return r;
    }

    /**
     * What is wrong with it: two vertices of one name; two so near they are one point; an edge
     * given twice; a vertex no edge joins.
     */
    public List<String> problems() {
        final List<String> problems = new ArrayList<>();
        final Set<String> names = new HashSet<>();
        for (final Vertex v : vertices) {
            if (!names.add(v.name())) {
                problems.add("two vertices called " + v.name());
            }
        }
        // near points, by the cells of a grid as big as the tolerance, so it is quick however many
        final Map<List<Long>, List<Integer>> cells = new HashMap<>();
        for (int v = 0; v < vertices.size(); v++) {
            final Vertex at = vertices.get(v);
            final long x = (long) Math.floor(at.across() / TOLERANCE);
            final long y = (long) Math.floor(at.along() / TOLERANCE);
            final long z = (long) Math.floor(at.up() / TOLERANCE);
            for (long dx = -1; dx <= 1; dx++) {
                for (long dy = -1; dy <= 1; dy++) {
                    for (long dz = -1; dz <= 1; dz++) {
                        for (final int other : cells.getOrDefault(List.of(x + dx, y + dy, z + dz), List.of())) {
                            if (vertices.get(other).distance(at) < TOLERANCE) {
                                problems.add(vertices.get(other).name() + " and " + at.name() + " are one point");
                            }
                        }
                    }
                }
            }
            cells.computeIfAbsent(List.of(x, y, z), k -> new ArrayList<>()).add(v);
        }
        final Set<List<Integer>> joined = new HashSet<>();
        final boolean[] used = new boolean[vertices.size()];
        for (final Edge edge : edges) {
            if (!joined.add(List.of(Math.min(edge.from(), edge.to()), Math.max(edge.from(), edge.to())))) {
                problems.add("an edge twice, " + vertices.get(edge.from()).name() + " to " + vertices.get(edge.to()).name());
            }
            used[edge.from()] = true;
            used[edge.to()] = true;
        }
        for (int v = 0; v < used.length; v++) {
            if (!used[v]) {
                problems.add(vertices.get(v).name() + " joins nothing");
            }
        }
        return problems;
    }

    /** As text: its name, then a line for each vertex, then one for each edge, by its vertices' names. */
    public String text() {
        final StringBuilder text = new StringBuilder("form ").append(Frame.quoted(name)).append('\n');
        for (final Vertex v : vertices) {
            text.append("vertex ").append(Frame.quoted(v.name())).append(' ').append(Frame.number(v.across()))
                    .append(' ').append(Frame.number(v.along())).append(' ').append(Frame.number(v.up())).append('\n');
        }
        for (final Edge e : edges) {
            text.append("edge ").append(Frame.quoted(vertices.get(e.from()).name())).append(' ')
                    .append(Frame.quoted(vertices.get(e.to()).name()));
            if (e.curved()) {
                final Curve c = e.curve();
                text.append(' ').append(c.kind().name().toLowerCase(java.util.Locale.ROOT)).append(numbers(c.control()))
                        .append(' ').append(Frame.number(c.fullness()));
                if (c.from() != null) {
                    text.append(numbers(c.from())).append(numbers(c.to())).append(' ').append(c.t0()).append(' ')
                            .append(c.t1());
                }
            }
            text.append('\n');
        }
        return text.toString();
    }

    private static String numbers(final Vec3 p) {
        return " " + Frame.number(p.x()) + " " + Frame.number(p.y()) + " " + Frame.number(p.z());
    }

    private static Vec3 vec(final List<String> words, final int at) {
        return new Vec3(Double.parseDouble(words.get(at)), Double.parseDouble(words.get(at + 1)),
                Double.parseDouble(words.get(at + 2)));
    }

    /** A curve from {@code words}, from {@code at} on (its kind, its figures); null if there are none. */
    private static Curve curve(final List<String> words, final int at) {
        if (words.size() <= at) {
            return null;
        }
        final Curve.Kind kind = Curve.Kind.valueOf(words.get(at).toUpperCase(java.util.Locale.ROOT));
        final Vec3 control = vec(words, at + 1);
        final double fullness = Double.parseDouble(words.get(at + 4));
        return switch (kind) {
            case ARC -> Curve.arc(control);
            case POWER -> words.size() > at + 5
                    ? Curve.bow(control, fullness, vec(words, at + 5), vec(words, at + 8),
                            Double.parseDouble(words.get(at + 11)), Double.parseDouble(words.get(at + 12)))
                    : Curve.bow(control, fullness);
            case CORNER -> Curve.corner(control, fullness, vec(words, at + 5), vec(words, at + 8),
                    Double.parseDouble(words.get(at + 11)), Double.parseDouble(words.get(at + 12)));
        };
    }

    /** A form from its {@link #text}; lines it does not know (and blank ones, and # comments) are passed over. */
    public static Form read(final String text) {
        final Builder form = new Builder("unnamed");
        String name = "unnamed";
        for (final String line : text.split("\\R")) {
            final List<String> words = Frame.words(line);
            if (words.isEmpty() || words.getFirst().startsWith("#")) {
                continue;
            }
            switch (words.getFirst()) {
                case "form" -> name = words.get(1);
                case "vertex" -> form.vertex(words.get(1), Double.parseDouble(words.get(2)),
                        Double.parseDouble(words.get(3)), Double.parseDouble(words.get(4)));
                case "edge" -> form.edge(form.at(words.get(1)), form.at(words.get(2)), curve(words, 3));
                default -> {
                    // for whatever uses it
                }
            }
        }
        return new Form(name, form.vertices, form.edges);
    }

    /**
     * A form put together a vertex and an edge at a time, each vertex by a name of its own, each
     * edge between vertices by their names or numbers.
     */
    public static final class Builder {
        private final String name;
        private final List<Vertex> vertices = new ArrayList<>();
        private final List<Edge> edges = new ArrayList<>();
        private final Map<String, Integer> named = new HashMap<>();

        public Builder(final String name) {
            this.name = name;
        }

        /** A vertex, its number; one of the same name already there is a mistake. */
        public int vertex(final String called, final double across, final double along, final double up) {
            if (named.containsKey(called)) {
                throw new IllegalArgumentException("a vertex called " + called + " already");
            }
            named.put(called, vertices.size());
            vertices.add(new Vertex(called, across, along, up));
            return vertices.size() - 1;
        }

        /** The number of the vertex called {@code called}. */
        public int at(final String called) {
            final Integer v = named.get(called);
            if (v == null) {
                throw new IllegalArgumentException("no vertex called " + called);
            }
            return v;
        }

        public Builder edge(final int from, final int to) {
            edges.add(new Edge(from, to));
            return this;
        }

        /** An edge curving as {@code curve} says; straight, if it is null. */
        public Builder edge(final int from, final int to, final Curve curve) {
            edges.add(new Edge(from, to, curve));
            return this;
        }

        /** Where vertex {@code v} is, as it is put together. */
        public Vec3 position(final int v) {
            final Vertex p = vertices.get(v);
            return new Vec3(p.across(), p.along(), p.up());
        }

        public Builder edge(final String from, final String to) {
            return edge(at(from), at(to));
        }

        /** Edges along {@code points} in turn, and back from the last to the first. */
        public Builder loop(final int... points) {
            for (int k = 0; k < points.length; k++) {
                edge(points[k], points[(k + 1) % points.length]);
            }
            return this;
        }

        /** Edges along {@code points} in turn, not closed. */
        public Builder line(final int... points) {
            for (int k = 0; k + 1 < points.length; k++) {
                edge(points[k], points[k + 1]);
            }
            return this;
        }

        public Form build() {
            return new Form(name, vertices, edges);
        }
    }
}
