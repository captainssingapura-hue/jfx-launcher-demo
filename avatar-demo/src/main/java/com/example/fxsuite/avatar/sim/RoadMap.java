package com.example.fxsuite.avatar.sim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Roads as they are described (docs/road-model.md): each an ordered list of vertices, and between each two in
 * turn a segment, homogeneous in every attribute, its curvature among them; kinds, named sets of a segment's
 * attributes; nodes, named vertices roads share, where they meet. Only what is decided is here: the line,
 * the heights, the transitions, the junctions are worked out from it ({@link Alignment}, {@link Road}).
 *
 * <p>Written as text, in metres:
 * <pre>
 * kind lane width=4m surface=gravel grade=12% curve=1/15m slope=1:2 camber=0% transition=12m vertical=30m
 * node square at=-40,98
 * road "church lane" kind=lane
 *   from square
 *   to -62,131
 *   to -95,170 curve=+1/25m
 *   to -235,383 height=12m
 * </pre>
 * Each {@code to} closes a segment, which carries on the attributes of the one before unless it says otherwise.
 *
 * @param kinds the kinds of road, by name
 * @param nodes the vertices roads share, by name
 * @param ways  the roads, in the order they are laid
 */
public record RoadMap(List<Kind> kinds, List<Node> nodes, List<Way> ways) {

    /**
     * A kind of road: a segment's attributes, as a set. {@code width} across its carriageway; its {@code surface};
     * its steepest {@code grade}, rise over run; its {@code tightest} curvature (one over its least radius); whether
     * a {@code corner} (a kink at a vertex) is allowed; its side slopes {@code slope} across for each up or down;
     * its {@code camber}, rise over run either side of its crown; how long a {@code transition} at a vertex is; how
     * long its grade takes to change ({@code vertical}).
     */
    public record Kind(String name, double width, Land.Cover surface, double grade, double tightest, boolean corners,
                       double slope, double camber, double transition, double vertical) {

        public Kind {
            if (name == null || surface == null || !(width > 0) || !(grade > 0) || !(tightest > 0) || !(slope > 0)
                    || camber < 0 || transition < 0 || !(vertical >= 0)) {
                throw new IllegalArgumentException("a kind of road has a name, a width, a surface and its limits");
            }
        }
    }

    /** A vertex roads share, by its name. */
    public record Node(String name, Vec2 at) {
    }

    /** A vertex of a road: where it is; how high, if pinned ({@code NaN} if not); the node it is, if any (null). */
    public record Vertex(Vec2 at, double height, String node) {

        public Vertex(final Vec2 at) {
            this(at, Double.NaN, null);
        }

        public boolean pinned() {
            return !Double.isNaN(height);
        }
    }

    /** A segment: its kind, by name; its curvature, signed (+ bending left, going along the road); whether a bridge. */
    public record Segment(String kind, double curvature, boolean bridge) {
    }

    /** A road: its name, its vertices in order, the segments between them; closed back to its first, if a {@code loop}. */
    public record Way(String name, List<Vertex> vertices, List<Segment> segments, boolean loop) {

        public Way {
            vertices = List.copyOf(vertices);
            segments = List.copyOf(segments);
            if (vertices.size() < 2 || segments.size() != vertices.size() - (loop ? 0 : 1)) {
                throw new IllegalArgumentException("a road goes from vertex to vertex, a segment between each two");
            }
        }

        /** The vertex segment {@code k} ends at. */
        public Vertex end(final int k) {
            return vertices.get((k + 1) % vertices.size());
        }
    }

    public RoadMap {
        kinds = List.copyOf(kinds);
        nodes = List.copyOf(nodes);
        ways = List.copyOf(ways);
    }

    /** Kind {@code name}; null if there is none. */
    public Kind kind(final String name) {
        return kinds.stream().filter(k -> k.name().equals(name)).findFirst().orElse(null);
    }

    /** Node {@code name}; null if there is none. */
    public Node node(final String name) {
        return nodes.stream().filter(n -> n.name().equals(name)).findFirst().orElse(null);
    }

    /** Its roads' lines, worked out: straights, arcs, and the transitions between them, a point every {@code step}. */
    public List<Alignment> aligned(final double step) {
        return ways.stream().map(w -> Alignment.of(this, w, step)).toList();
    }

    /**
     * Its roads laid on {@code under}, in order, each over those before it: where a road's vertex is a node an
     * earlier road has, it is pinned as high as that road is there, so they meet.
     */
    public List<Road> laid(final Land under, final double step) {
        final List<Road> roads = new ArrayList<>();
        final Map<String, Road> at = new HashMap<>();
        Land land = under;
        for (final Way way : ways) {
            final Map<Integer, Double> pins = new HashMap<>();
            for (int v = 0; v < way.vertices().size(); v++) {
                final String node = way.vertices().get(v).node();
                if (node != null && at.containsKey(node)) {
                    final Vec2 p = way.vertices().get(v).at();
                    pins.put(v, at.get(node).level(p.x(), p.z()));
                }
            }
            final Road road = Road.of(this, way, land, pins, step);
            roads.add(road);
            for (final Vertex v : way.vertices()) {
                if (v.node() != null) {
                    at.putIfAbsent(v.node(), road);
                }
            }
            land = land.with(road);
        }
        return roads;
    }

    /** What is wrong with it, as described: what cannot be worked out, or should be looked at. */
    public List<String> problems() {
        final List<String> problems = new ArrayList<>();
        for (final Way way : ways) {
            for (final Segment s : way.segments()) {
                if (kind(s.kind()) == null) {
                    problems.add("\"" + way.name() + "\": no kind of road " + s.kind());
                }
            }
            for (final Vertex v : way.vertices()) {
                if (v.node() != null && (node(v.node()) == null || !node(v.node()).at().equals(v.at()))) {
                    problems.add("\"" + way.name() + "\": no node " + v.node() + " where it says");
                }
            }
        }
        if (!problems.isEmpty()) {
            return problems;
        }
        final List<Alignment> lines = aligned(Alignment.STEP);
        for (final Alignment line : lines) {
            problems.addAll(line.problems());
        }
        // roads crossing, but where they share a node, or on a bridge
        for (int a = 0; a < ways.size(); a++) {
            for (int b = a + 1; b < ways.size(); b++) {
                final Vec2 at = lines.get(a).crossing(lines.get(b));
                if (at != null && !shared(ways.get(a), ways.get(b), at)) {
                    problems.add("\"" + ways.get(a).name() + "\" and \"" + ways.get(b).name() + "\" cross at "
                            + metres(at.x()) + "," + metres(at.z()) + ", not at a node they share");
                }
            }
        }
        return problems;
    }

    /** How near a node two roads' crossing may be and be their meeting there. */
    private static final double MEETING = 2_000;

    private static boolean shared(final Way a, final Way b, final Vec2 at) {
        for (final Vertex v : a.vertices()) {
            if (v.node() != null && b.vertices().stream().anyMatch(w -> v.node().equals(w.node()))
                    && Math.hypot(v.at().x() - at.x(), v.at().z() - at.z()) < MEETING) {
                return true;
            }
        }
        return false;
    }

    // --- as text ---------------------------------------------------------------------------------------------

    private static final Pattern ROAD = Pattern.compile("road\\s+\"([^\"]*)\"\\s*(.*)");

    /** It written as text, in metres, that {@link #parse} reads back. */
    public String text() {
        final StringBuilder out = new StringBuilder();
        for (final Kind k : kinds) {
            out.append("kind ").append(k.name()).append(" width=").append(metres(k.width())).append("m surface=")
                    .append(k.surface().name().toLowerCase(Locale.ROOT)).append(" grade=").append(percent(k.grade()))
                    .append(" curve=").append(curve(k.tightest())).append(" slope=1:").append(plain(k.slope()))
                    .append(" camber=").append(percent(k.camber())).append(" transition=").append(metres(k.transition()))
                    .append("m vertical=").append(metres(k.vertical())).append('m').append(k.corners() ? " corners" : "")
                    .append('\n');
        }
        for (final Node n : nodes) {
            out.append("node ").append(n.name()).append(" at=").append(point(n.at())).append('\n');
        }
        for (final Way w : ways) {
            final String kind = w.segments().getFirst().kind();
            out.append("road \"").append(w.name()).append("\" kind=").append(kind).append(w.loop() ? " loop" : "").append('\n');
            out.append(vertex(w.vertices().getFirst(), w.vertices().getFirst().node() != null ? "  from " : "  at "))
                    .append('\n');
            Segment was = new Segment(kind, 0, false);
            for (int s = 0; s < w.segments().size(); s++) {
                final Segment seg = w.segments().get(s);
                final StringBuilder line = new StringBuilder(vertex(w.end(s), "  to "));
                if (!seg.kind().equals(was.kind())) {
                    line.append(" kind=").append(seg.kind());
                }
                if (seg.curvature() != was.curvature()) {
                    line.append(" curve=").append(seg.curvature() == 0 ? "0" : (seg.curvature() > 0 ? "+" : "-")
                            + curve(Math.abs(seg.curvature())));
                }
                if (seg.bridge() != was.bridge()) {
                    line.append(seg.bridge() ? " bridge" : " ground");
                }
                out.append(line).append('\n');
                was = seg;
            }
        }
        return out.toString();
    }

    private static String vertex(final Vertex v, final String lead) {
        final StringBuilder out = new StringBuilder(lead).append(v.node() != null ? v.node() : point(v.at()));
        if (v.pinned()) {
            out.append(" height=").append(metres(v.height())).append('m');
        }
        return out.toString();
    }

    /** Reads roads from their {@link #text}; lines may be blank, and {@code #} starts a remark. */
    public static RoadMap parse(final String text) {
        final List<Kind> kinds = new ArrayList<>();
        final List<Node> nodes = new ArrayList<>();
        final List<Way> ways = new ArrayList<>();
        String name = null;
        String kind = null;
        boolean loop = false;
        List<Vertex> vertices = null;
        List<Segment> segments = null;
        Segment was = null;
        final String[] lines = text.split("\\R");
        for (int number = 1; number <= lines.length + 1; number++) {
            final String line = number <= lines.length ? lines[number - 1].replaceFirst("#.*", "").strip() : "";
            try {
                final boolean closes = number > lines.length || line.startsWith("road ") || line.startsWith("kind ")
                        || line.startsWith("node ");
                if (closes && name != null) {
                    ways.add(new Way(name, vertices, closed(vertices, segments, loop), loop));
                    name = null;
                }
                if (line.isEmpty()) {
                    continue;
                }
                final String[] words = line.split("\\s+");
                switch (words[0]) {
                    case "kind" -> kinds.add(kind(words));
                    case "node" -> {
                        if (words.length != 3 || !key(words[2]).equals("at")) {
                            throw new IllegalArgumentException("a node is written node name at=x,z");
                        }
                        nodes.add(new Node(words[1], point(value(words[2]))));
                    }
                    case "road" -> {
                        final Matcher head = ROAD.matcher(line);
                        if (!head.matches()) {
                            throw new IllegalArgumentException("a road is written road \"its name\" kind=...");
                        }
                        name = head.group(1);
                        kind = null;
                        loop = false;
                        for (final String word : head.group(2).isBlank() ? new String[0] : head.group(2).split("\\s+")) {
                            if (word.equals("loop")) {
                                loop = true;
                            } else if (key(word).equals("kind")) {
                                kind = value(word);
                            } else {
                                throw new IllegalArgumentException("a road has no " + word);
                            }
                        }
                        if (kind == null) {
                            throw new IllegalArgumentException("a road is of a kind");
                        }
                        vertices = new ArrayList<>();
                        segments = new ArrayList<>();
                        was = new Segment(kind, 0, false);
                    }
                    case "at", "from", "to" -> {
                        if (name == null) {
                            throw new IllegalArgumentException("a vertex is of a road");
                        }
                        if (words[0].equals("to") == vertices.isEmpty()) {
                            throw new IllegalArgumentException("a road starts at or from a vertex, then goes to others");
                        }
                        final String node = words[0].equals("at") ? null : nodeOrNull(nodes, words[1]);
                        final Vec2 at = node != null ? nodes.stream().filter(n -> n.name().equals(node)).findFirst()
                                .orElseThrow().at() : point(words[1]);
                        double height = Double.NaN;
                        String segKind = was.kind();
                        double curvature = was.curvature();
                        boolean bridge = was.bridge();
                        for (int w = 2; w < words.length; w++) {
                            switch (words[w]) {
                                case "bridge" -> bridge = true;
                                case "ground" -> bridge = false;
                                default -> {
                                    final String v = value(words[w]);
                                    switch (key(words[w])) {
                                        case "height" -> height = centimetres(v);
                                        case "kind" -> segKind = v;
                                        case "curve" -> curvature = curvature(v);
                                        default -> throw new IllegalArgumentException("a vertex has no " + key(words[w]));
                                    }
                                }
                            }
                        }
                        vertices.add(new Vertex(at, height, node));
                        if (words[0].equals("to")) {
                            was = new Segment(segKind, curvature, bridge);
                            segments.add(was);
                        }
                    }
                    default -> throw new IllegalArgumentException("no " + words[0] + " among roads");
                }
            } catch (final IllegalArgumentException e) {
                throw new IllegalArgumentException("line " + Math.min(number, lines.length) + ": " + e.getMessage(), e);
            }
        }
        return new RoadMap(kinds, nodes, ways);
    }

    /** A loop's vertices, its last back at its first not kept twice; its segments as they are. */
    private static List<Segment> closed(final List<Vertex> vertices, final List<Segment> segments, final boolean loop) {
        if (loop && vertices.size() > 2 && vertices.getLast().at().equals(vertices.getFirst().at())) {
            vertices.removeLast();
        } else if (loop) {
            throw new IllegalArgumentException("a loop comes back to where it starts");
        }
        return segments;
    }

    private static String nodeOrNull(final List<Node> nodes, final String word) {
        return nodes.stream().anyMatch(n -> n.name().equals(word)) ? word : null;
    }

    private static Kind kind(final String[] words) {
        if (words.length < 2) {
            throw new IllegalArgumentException("a kind has a name");
        }
        double width = 0;
        Land.Cover surface = Land.Cover.GRAVEL;
        double grade = 0;
        double tightest = 0;
        boolean corners = false;
        double slope = 2;
        double camber = 0;
        double transition = 0;
        double vertical = 0;
        for (int w = 2; w < words.length; w++) {
            if (words[w].equals("corners")) {
                corners = true;
                continue;
            }
            final String v = value(words[w]);
            switch (key(words[w])) {
                case "width" -> width = centimetres(v);
                case "surface" -> surface = Land.Cover.valueOf(v.toUpperCase(Locale.ROOT));
                case "grade" -> grade = fraction(v);
                case "curve" -> tightest = curvature(v);
                case "slope" -> slope = Double.parseDouble(v.replaceFirst("^1:", ""));
                case "camber" -> camber = fraction(v);
                case "transition" -> transition = centimetres(v);
                case "vertical" -> vertical = centimetres(v);
                default -> throw new IllegalArgumentException("a kind has no " + key(words[w]));
            }
        }
        return new Kind(words[1], width, surface, grade, tightest, corners, slope, camber, transition, vertical);
    }

    private static String key(final String word) {
        final int at = word.indexOf('=');
        if (at < 0) {
            throw new IllegalArgumentException("expected name=value, not " + word);
        }
        return word.substring(0, at).toLowerCase(Locale.ROOT);
    }

    private static String value(final String word) {
        key(word);
        return word.substring(word.indexOf('=') + 1);
    }

    private static double centimetres(final String metres) {
        final String number = metres.endsWith("m") ? metres.substring(0, metres.length() - 1) : metres;
        try {
            return new BigDecimal(number).movePointRight(2).doubleValue();
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException("not a measure in metres: " + metres, e);
        }
    }

    private static double fraction(final String percent) {
        try {
            return new BigDecimal(percent.replace("%", "")).movePointLeft(2).doubleValue();
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException("not a percentage: " + percent, e);
        }
    }

    /** {@code "0"}, or {@code "+1/25m"}: one over a radius, signed, per centimetre. */
    private static double curvature(final String text) {
        if (text.equals("0")) {
            return 0;
        }
        final Matcher m = Pattern.compile("([+-]?)1/(.+)").matcher(text);
        if (!m.matches()) {
            throw new IllegalArgumentException("a curvature is 0, or 1/radius, signed: not " + text);
        }
        return (m.group(1).equals("-") ? -1 : 1) / centimetres(m.group(2));
    }

    private static Vec2 point(final String word) {
        final String[] xz = word.split(",");
        if (xz.length != 2) {
            throw new IllegalArgumentException("a point is x,z in metres, not " + word);
        }
        return new Vec2(centimetres(xz[0]), centimetres(xz[1]));
    }

    private static String point(final Vec2 p) {
        return metres(p.x()) + "," + metres(p.z());
    }

    /** One over a curvature, in metres, as written: {@code "1/25m"}. */
    private static String curve(final double curvature) {
        return "1/" + metres(1 / curvature) + "m";
    }

    private static String percent(final double fraction) {
        return plain(fraction * 100) + "%";
    }

    private static String plain(final double v) {
        final BigDecimal d = BigDecimal.valueOf(v).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros();
        return d.signum() == 0 ? "0" : d.toPlainString();
    }

    static String metres(final double centimetres) {
        return plain(centimetres / 100);
    }
}
