import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Turns an OpenStreetMap extract (Overpass XML, {@code out geom}) round Marina Bay into the avatar demo's terrain
 * text ({@code Terrain.parse}): the land from the coastline, the bay, its channel and the river's mouth as one
 * basin, and the larger lakes; in metres from the middle hotel tower, turned so its long side runs along z.
 *
 * <pre>
 * java tools/OsmTerrain.java marina.osm [x0 x1 z0 z1 in metres]
 * </pre>
 *
 * Prints the terrain text, then what it found: the towers' places and bearing, each lake's size. Map data
 * (c) OpenStreetMap contributors, ODbL: what it prints is derived from it, and is to be credited so.
 */
public final class OsmTerrain {

    /** Metres per degree, north and east at the equator; how near two points must be to be one. */
    private static final double NORTH = 110_574;
    private static final double EAST = 111_320;
    private static final double SIMPLIFY = 2;

    private static double lat0;
    private static double lon0;
    private static double turn;

    record P(double x, double z) {
        String key() {
            return String.format(Locale.ROOT, "%.3f,%.3f", x, z);
        }
    }

    record Way(String id, Map<String, String> tags, List<P> points) {
    }

    public static void main(final String[] args) throws Exception {
        final Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(args[0]));
        final double x0 = args.length > 1 ? Double.parseDouble(args[1]) : -2_400;
        final double x1 = args.length > 2 ? Double.parseDouble(args[2]) : 2_400;
        final double z0 = args.length > 3 ? Double.parseDouble(args[3]) : -2_000;
        final double z1 = args.length > 4 ? Double.parseDouble(args[4]) : 1_800;
        // the raw ways and relations, their points as latitude and longitude
        final List<Element> ways = elements(doc, "way");
        final List<Element> relations = elements(doc, "relation");
        // the origin: the middle tower; its bearing: its footprint's long axis
        final Element tower = ways.stream().filter(w -> "Marina Bay Sands Tower 2".equals(tags(w).get("name"))).findFirst()
                .orElseThrow();
        final double[][] t2 = latLons(tower);
        lat0 = mean(t2, 0);
        lon0 = mean(t2, 1);
        turn = 0;
        turn = Math.PI / 2 - axis(project(t2));
        final StringBuilder notes = new StringBuilder();
        notes.append(String.format(Locale.ROOT, "# origin %.7f,%.7f; turned %.2f degrees%n", lat0, lon0,
                Math.toDegrees(turn)));
        for (final Element w : ways) {
            final String name = tags(w).get("name");
            if (name != null && name.startsWith("Marina Bay Sands Tower")) {
                final List<P> ps = project(latLons(w));
                final P mid = centre(ps);
                notes.append(String.format(Locale.ROOT, "# %s at %.1f,%.1f, its long side at %.2f degrees from z%n",
                        name, mid.x(), mid.z(), 90 - Math.toDegrees(axis(ps))));
            }
        }
        // the land: the coastline's chains, clipped to the world, closed round its edge, the land on their left
        final List<List<P>> chains = chains(ways.stream().filter(w -> "coastline".equals(tags(w).get("natural")))
                .map(w -> project(latLons(w))).toList());
        final List<List<P>> land = land(chains, x0, x1, z0, z1);
        // the basin: the reservoir's and the river's shores, each stretch they share left out of both
        final List<List<P>> parts = new ArrayList<>();
        for (final Element r : relations) {
            final String name = tags(r).get("name");
            if ("Marina Reservoir".equals(name) || "Singapore River".equals(name)) {
                final NodeList members = r.getElementsByTagName("member");
                for (int k = 0; k < members.getLength(); k++) {
                    final Element m = (Element) members.item(k);
                    if (m.getAttribute("type").equals("way")) {
                        parts.add(project(latLons(m)));
                    }
                }
            }
        }
        final List<List<P>> basin = unshared(parts);
        // the lakes: closed water ways, the larger ones, named
        final List<String> lakes = new ArrayList<>();
        final List<List<P>> lakeRings = new ArrayList<>();
        for (final Element w : ways) {
            final Map<String, String> tags = tags(w);
            if (!"water".equals(tags.get("natural")) || "canal".equals(tags.get("water"))) {
                continue;
            }
            final List<P> ring = project(latLons(w));
            final double area = Math.abs(area(ring));
            final P mid = centre(ring);
            notes.append(String.format(Locale.ROOT, "# water %s %s: %.0f m2 at %.0f,%.0f%n", w.getAttribute("id"),
                    tags.getOrDefault("name", "-"), area, mid.x(), mid.z()));
            if (area > 3_000 && mid.x() > x0 && mid.x() < x1 && mid.z() > z0 && mid.z() < z1) {
                lakes.add(tags.getOrDefault("name", "a lake"));
                lakeRings.add(ring);
            }
        }
        for (final Element r : relations) {
            if ("Dragonfly Lake".equals(tags(r).get("name"))) {
                for (final List<P> ring : rings(r)) {
                    lakes.add("Dragonfly Lake");
                    lakeRings.add(ring);
                }
            }
        }
        // the lines: the land's shores and its bounds at the world's edge; the basin's shores; the lakes', closed
        final StringBuilder out = new StringBuilder();
        out.append(String.format(Locale.ROOT, "world %.0f %.0f %.0f %.0f%n", x0, x1, z0, z1));
        for (final List<P> ring : land) {
            final int n = ring.size();
            // round the ring a side at a time, a run of shore or of bound at a time
            int start = 0;
            while (start < n && !bound(ring.get(start), ring.get((start + n - 1) % n), x0, x1, z0, z1)) {
                start++;
            }
            if (start == n) {
                start = 0;
            }
            List<P> run = new ArrayList<>(List.of(ring.get(start)));
            boolean along = bound(ring.get(start), ring.get((start + 1) % n), x0, x1, z0, z1);
            for (int k = 1; k <= n; k++) {
                final P a = ring.get((start + k - 1) % n);
                final P b = ring.get((start + k) % n);
                final boolean edge = bound(a, b, x0, x1, z0, z1);
                if (edge != along) {
                    out.append(along ? "bound " : "land ").append(line(along ? run : simplifiedLine(run))).append('\n');
                    run = new ArrayList<>(List.of(a));
                    along = edge;
                }
                run.add(b);
            }
            out.append(along ? "bound " : "land ").append(line(along ? run : simplifiedLine(run))).append('\n');
        }
        for (final List<P> line : basin) {
            // closed, so in and out are told right: where its ends do not meet (a river cut off past the world's
            // edge), joined there
            final List<P> closed = new ArrayList<>(simplifiedLine(line));
            if (!closed.getFirst().key().equals(closed.getLast().key())) {
                closed.add(closed.getFirst());
            }
            out.append("basin ").append(line(closed)).append('\n');
        }
        for (int k = 0; k < lakeRings.size(); k++) {
            final List<P> ring = new ArrayList<>(simplified(lakeRings.get(k)));
            ring.add(ring.getFirst());
            out.append("# ").append(lakes.get(k)).append('\n');
            out.append("lake ").append(line(ring)).append('\n');
        }
        System.out.print(out);
        System.out.print(notes);
    }

    // --- reading -----------------------------------------------------------------------------------------------

    private static List<Element> elements(final Document doc, final String tag) {
        final List<Element> all = new ArrayList<>();
        final NodeList list = doc.getDocumentElement().getElementsByTagName(tag);
        for (int k = 0; k < list.getLength(); k++) {
            if (list.item(k).getParentNode() == doc.getDocumentElement()) {
                all.add((Element) list.item(k));
            }
        }
        return all;
    }

    private static Map<String, String> tags(final Element e) {
        final Map<String, String> tags = new HashMap<>();
        final NodeList list = e.getElementsByTagName("tag");
        for (int k = 0; k < list.getLength(); k++) {
            final Element t = (Element) list.item(k);
            if (t.getParentNode() == e) {
                tags.put(t.getAttribute("k"), t.getAttribute("v"));
            }
        }
        return tags;
    }

    /** The points directly in {@code e}, each {latitude, longitude}. */
    private static double[][] latLons(final Element e) {
        final List<double[]> points = new ArrayList<>();
        final NodeList list = e.getChildNodes();
        for (int k = 0; k < list.getLength(); k++) {
            if (list.item(k) instanceof Element nd && nd.getTagName().equals("nd") && nd.hasAttribute("lat")) {
                points.add(new double[] {Double.parseDouble(nd.getAttribute("lat")), Double.parseDouble(nd.getAttribute("lon"))});
            }
        }
        return points.toArray(new double[0][]);
    }

    /** A relation's rings, its outer members' ways joined end to end. */
    private static List<List<P>> rings(final Element relation) {
        final List<List<P>> parts = new ArrayList<>();
        final NodeList members = relation.getElementsByTagName("member");
        for (int k = 0; k < members.getLength(); k++) {
            final Element m = (Element) members.item(k);
            if (m.getAttribute("type").equals("way") && !m.getAttribute("role").equals("inner")) {
                parts.add(project(latLons(m)));
            }
        }
        return chains(parts, true);
    }

    // --- where ---------------------------------------------------------------------------------------------------

    private static List<P> project(final double[][] latLons) {
        final List<P> points = new ArrayList<>();
        final double c = Math.cos(Math.toRadians(lat0));
        for (final double[] ll : latLons) {
            final double east = (ll[1] - lon0) * c * EAST;
            final double north = (ll[0] - lat0) * NORTH;
            // turned so the towers' long side runs along z
            points.add(new P(east * Math.cos(turn) - north * Math.sin(turn), east * Math.sin(turn) + north * Math.cos(turn)));
        }
        return points;
    }

    private static double mean(final double[][] values, final int k) {
        double sum = 0;
        for (final double[] v : values) {
            sum += v[k];
        }
        return sum / values.length;
    }

    private static P centre(final List<P> ps) {
        double x = 0;
        double z = 0;
        for (final P p : ps) {
            x += p.x() / ps.size();
            z += p.z() / ps.size();
        }
        return new P(x, z);
    }

    /** The angle from x of a set of points' long axis, by their spread. */
    private static double axis(final List<P> ps) {
        final P c = centre(ps);
        double sxx = 0;
        double szz = 0;
        double sxz = 0;
        for (final P p : ps) {
            sxx += (p.x() - c.x()) * (p.x() - c.x());
            szz += (p.z() - c.z()) * (p.z() - c.z());
            sxz += (p.x() - c.x()) * (p.z() - c.z());
        }
        return 0.5 * Math.atan2(2 * sxz, sxx - szz);
    }

    private static double area(final List<P> ring) {
        double a = 0;
        for (int k = 0; k < ring.size(); k++) {
            final P p = ring.get(k);
            final P q = ring.get((k + 1) % ring.size());
            a += p.x() * q.z() - q.x() * p.z();
        }
        return a / 2;
    }

    // --- joining -------------------------------------------------------------------------------------------------

    private static List<List<P>> chains(final List<List<P>> parts) {
        return chains(parts, false);
    }

    /** Lines joined where one ends and another begins (or, {@code either}, ends too, turned round). */
    private static List<List<P>> chains(final List<List<P>> parts, final boolean either) {
        final List<List<P>> left = new ArrayList<>(parts.stream().map(ArrayList::new).toList());
        final List<List<P>> joined = new ArrayList<>();
        while (!left.isEmpty()) {
            final List<P> chain = new ArrayList<>(left.removeFirst());
            boolean grew = true;
            while (grew && !chain.getFirst().key().equals(chain.getLast().key())) {
                grew = false;
                for (int k = 0; k < left.size(); k++) {
                    final List<P> part = left.get(k);
                    if (part.getFirst().key().equals(chain.getLast().key())) {
                        chain.addAll(part.subList(1, part.size()));
                    } else if (part.getLast().key().equals(chain.getFirst().key())) {
                        chain.addAll(0, part.subList(0, part.size() - 1));
                    } else if (either && part.getLast().key().equals(chain.getLast().key())) {
                        chain.addAll(part.reversed().subList(1, part.size()));
                    } else if (either && part.getFirst().key().equals(chain.getFirst().key())) {
                        chain.addAll(0, part.reversed().subList(0, part.size() - 1));
                    } else {
                        continue;
                    }
                    left.remove(k);
                    grew = true;
                    break;
                }
            }
            if (chain.getFirst().key().equals(chain.getLast().key())) {
                chain.removeLast();
            }
            joined.add(chain);
        }
        return joined;
    }

    /** Rings joined where they share a stretch of edge: each edge both have, gone; what is left, chained. */
    private static List<List<P>> merged(final List<List<P>> rings) {
        final Map<String, int[]> count = new HashMap<>();
        final List<P[]> edges = new ArrayList<>();
        for (final List<P> ring : rings) {
            for (int k = 0; k < ring.size(); k++) {
                final P a = ring.get(k);
                final P b = ring.get((k + 1) % ring.size());
                edges.add(new P[] {a, b});
                count.computeIfAbsent(undirected(a, b), x -> new int[1])[0]++;
            }
        }
        final List<List<P>> kept = new ArrayList<>();
        for (final P[] e : edges) {
            if (count.get(undirected(e[0], e[1]))[0] == 1) {
                kept.add(List.of(e[0], e[1]));
            }
        }
        return chains(kept, true).stream().filter(r -> r.size() >= 3).toList();
    }

    private static String undirected(final P a, final P b) {
        return a.key().compareTo(b.key()) < 0 ? a.key() + "|" + b.key() : b.key() + "|" + a.key();
    }

    /** Lines, each stretch two of them share left out of both; what is left joined end to end where it meets. */
    private static List<List<P>> unshared(final List<List<P>> lines) {
        final Map<String, int[]> count = new HashMap<>();
        for (final List<P> line : lines) {
            for (int k = 0; k + 1 < line.size(); k++) {
                count.computeIfAbsent(undirected(line.get(k), line.get(k + 1)), x -> new int[1])[0]++;
            }
        }
        final List<List<P>> kept = new ArrayList<>();
        for (final List<P> line : lines) {
            List<P> run = null;
            for (int k = 0; k + 1 < line.size(); k++) {
                if (count.get(undirected(line.get(k), line.get(k + 1)))[0] == 1) {
                    if (run == null) {
                        run = new ArrayList<>(List.of(line.get(k)));
                    }
                    run.add(line.get(k + 1));
                } else if (run != null) {
                    kept.add(run);
                    run = null;
                }
            }
            if (run != null) {
                kept.add(run);
            }
        }
        return chains(kept, true).stream().map(c -> {
            // a chain that closed lost its last point, the same as its first: put it back, so it is a line round
            final List<P> l = new ArrayList<>(c);
            return l;
        }).toList();
    }

    /** Whether a line from {@code a} to {@code b} runs along one of the world's edges: a bound, not a shore. */
    private static boolean bound(final P a, final P b, final double x0, final double x1, final double z0, final double z1) {
        final double e = 1e-6;
        return Math.abs(a.x() - x0) < e && Math.abs(b.x() - x0) < e || Math.abs(a.x() - x1) < e && Math.abs(b.x() - x1) < e
                || Math.abs(a.z() - z0) < e && Math.abs(b.z() - z0) < e || Math.abs(a.z() - z1) < e && Math.abs(b.z() - z1) < e;
    }

    /** A line with only the points that matter, its ends kept (Douglas and Peucker). */
    private static List<P> simplifiedLine(final List<P> line) {
        if (line.size() < 3) {
            return line;
        }
        final boolean[] keep = new boolean[line.size()];
        keep[0] = true;
        keep[line.size() - 1] = true;
        simplify(line, 0, line.size() - 1, keep);
        final List<P> kept = new ArrayList<>();
        for (int k = 0; k < line.size(); k++) {
            if (keep[k]) {
                kept.add(line.get(k));
            }
        }
        return kept;
    }

    private static String line(final List<P> points) {
        return loop(points);
    }

    // --- the land, closed round the world's edge --------------------------------------------------------------

    /** Where a point on the rectangle's edge is, going round it anticlockwise from its south-west corner. */
    private static double round(final P p, final double x0, final double x1, final double z0, final double z1) {
        final double w = x1 - x0;
        final double h = z1 - z0;
        if (Math.abs(p.z() - z0) < 1e-6) {
            return p.x() - x0;
        }
        if (Math.abs(p.x() - x1) < 1e-6) {
            return w + p.z() - z0;
        }
        if (Math.abs(p.z() - z1) < 1e-6) {
            return w + h + (x1 - p.x());
        }
        return 2 * w + h + (z1 - p.z());
    }

    private static boolean in(final P p, final double x0, final double x1, final double z0, final double z1) {
        return p.x() > x0 && p.x() < x1 && p.z() > z0 && p.z() < z1;
    }

    /** Where the line from {@code a} (inside) to {@code b} (outside) crosses the rectangle's edge. */
    private static P crossing(final P a, final P b, final double x0, final double x1, final double z0, final double z1) {
        double t = 1;
        if (b.x() < x0) {
            t = Math.min(t, (x0 - a.x()) / (b.x() - a.x()));
        }
        if (b.x() > x1) {
            t = Math.min(t, (x1 - a.x()) / (b.x() - a.x()));
        }
        if (b.z() < z0) {
            t = Math.min(t, (z0 - a.z()) / (b.z() - a.z()));
        }
        if (b.z() > z1) {
            t = Math.min(t, (z1 - a.z()) / (b.z() - a.z()));
        }
        final P c = new P(a.x() + (b.x() - a.x()) * t, a.z() + (b.z() - a.z()) * t);
        // onto the edge exactly
        return new P(Math.abs(c.x() - x0) < 1e-6 ? x0 : Math.abs(c.x() - x1) < 1e-6 ? x1 : c.x(),
                Math.abs(c.z() - z0) < 1e-6 ? z0 : Math.abs(c.z() - z1) < 1e-6 ? z1 : c.z());
    }

    /**
     * The land within the rectangle: each run of coastline through it, the land on its left; from where one leaves
     * it, round its edge anticlockwise to where the next comes in, its corners on the way; closed rings within it as
     * they are (islands).
     */
    private static List<List<P>> land(final List<List<P>> chains, final double x0, final double x1, final double z0,
                                      final double z1) {
        final List<List<P>> runs = new ArrayList<>();
        final List<List<P>> islands = new ArrayList<>();
        for (final List<P> chain : chains) {
            final boolean closed = chain.size() > 2 && chain.stream().allMatch(p -> in(p, x0, x1, z0, z1));
            if (closed) {
                islands.add(chain);
                continue;
            }
            List<P> run = null;
            for (int k = 0; k < chain.size(); k++) {
                final P p = chain.get(k);
                final boolean inside = in(p, x0, x1, z0, z1);
                if (inside && run == null) {
                    run = new ArrayList<>();
                    if (k > 0) {
                        run.add(crossing(p, chain.get(k - 1), x0, x1, z0, z1));
                    }
                }
                if (inside) {
                    run.add(p);
                } else if (run != null) {
                    run.add(crossing(chain.get(k - 1), p, x0, x1, z0, z1));
                    runs.add(run);
                    run = null;
                }
            }
            if (run != null) {
                System.err.println("# a coastline ends inside the world: " + run.getLast());
            }
        }
        final List<List<P>> rings = new ArrayList<>(islands);
        final double perimeter = 2 * (x1 - x0) + 2 * (z1 - z0);
        final P[] corners = {new P(x0, z0), new P(x1, z0), new P(x1, z1), new P(x0, z1)};
        final boolean[] used = new boolean[runs.size()];
        for (int start = 0; start < runs.size(); start++) {
            if (used[start]) {
                continue;
            }
            final List<P> ring = new ArrayList<>();
            int at = start;
            while (!used[at]) {
                used[at] = true;
                final List<P> run = runs.get(at);
                ring.addAll(run);
                // round the edge anticlockwise from where it leaves to the nearest place one comes in
                final double from = round(run.getLast(), x0, x1, z0, z1);
                int next = -1;
                double best = Double.POSITIVE_INFINITY;
                for (int k = 0; k < runs.size(); k++) {
                    final double ahead = (round(runs.get(k).getFirst(), x0, x1, z0, z1) - from + perimeter) % perimeter;
                    if (ahead < best) {
                        best = ahead;
                        next = k;
                    }
                }
                for (int c = 0; c < 8; c++) {
                    final P corner = corners[c % 4];
                    final double ahead = (round(corner, x0, x1, z0, z1) - from + perimeter) % perimeter;
                    if (ahead > 1e-6 && ahead < best) {
                        // in order: the nearer first
                        ring.add(corner);
                    }
                }
                sortTail(ring, run.size(), from, perimeter, x0, x1, z0, z1);
                at = next;
            }
            rings.add(ring);
        }
        return rings;
    }

    /** The corners added after the run's last point, in the order they are come to round the edge. */
    private static void sortTail(final List<P> ring, final int runSize, final double from, final double perimeter,
                                 final double x0, final double x1, final double z0, final double z1) {
        int start = ring.size();
        while (start > 0 && isCorner(ring.get(start - 1), x0, x1, z0, z1)) {
            start--;
        }
        final Map<String, P> once = new LinkedHashMap<>();
        for (final P p : ring.subList(Math.max(start, 0), ring.size())) {
            once.put(p.key(), p);
        }
        final List<P> tail = new ArrayList<>(once.values());
        tail.sort((a, b) -> Double.compare((round(a, x0, x1, z0, z1) - from + perimeter) % perimeter,
                (round(b, x0, x1, z0, z1) - from + perimeter) % perimeter));
        ring.subList(Math.max(start, 0), ring.size()).clear();
        ring.addAll(tail);
    }

    private static boolean isCorner(final P p, final double x0, final double x1, final double z0, final double z1) {
        return (p.x() == x0 || p.x() == x1) && (p.z() == z0 || p.z() == z1);
    }

    // --- clipping, simplifying, writing -----------------------------------------------------------------------

    /** A ring cut to the rectangle, one side at a time (Sutherland and Hodgman). */
    private static List<P> clip(final List<P> ring, final double x0, final double x1, final double z0, final double z1) {
        List<P> out = ring;
        for (int side = 0; side < 4; side++) {
            final List<P> in = out;
            out = new ArrayList<>();
            for (int k = 0; k < in.size(); k++) {
                final P a = in.get(k);
                final P b = in.get((k + 1) % in.size());
                final boolean ai = inside(a, side, x0, x1, z0, z1);
                final boolean bi = inside(b, side, x0, x1, z0, z1);
                if (ai) {
                    out.add(a);
                }
                if (ai != bi) {
                    final double t = switch (side) {
                        case 0 -> (x0 - a.x()) / (b.x() - a.x());
                        case 1 -> (x1 - a.x()) / (b.x() - a.x());
                        case 2 -> (z0 - a.z()) / (b.z() - a.z());
                        default -> (z1 - a.z()) / (b.z() - a.z());
                    };
                    out.add(new P(a.x() + (b.x() - a.x()) * t, a.z() + (b.z() - a.z()) * t));
                }
            }
        }
        return out;
    }

    private static boolean inside(final P p, final int side, final double x0, final double x1, final double z0,
                                  final double z1) {
        return switch (side) {
            case 0 -> p.x() >= x0;
            case 1 -> p.x() <= x1;
            case 2 -> p.z() >= z0;
            default -> p.z() <= z1;
        };
    }

    /** A ring with only the points that matter, none further than {@link #SIMPLIFY} from what is kept (Douglas and Peucker). */
    private static List<P> simplified(final List<P> ring) {
        if (ring.size() < 8) {
            return ring;
        }
        final List<P> closed = new ArrayList<>(ring);
        closed.add(ring.getFirst());
        final boolean[] keep = new boolean[closed.size()];
        keep[0] = true;
        keep[closed.size() - 1] = true;
        // split at the point farthest from the first, so the ring is two lines
        int far = 0;
        for (int k = 1; k < ring.size(); k++) {
            if (Math.hypot(ring.get(k).x() - ring.getFirst().x(), ring.get(k).z() - ring.getFirst().z())
                    > Math.hypot(ring.get(far).x() - ring.getFirst().x(), ring.get(far).z() - ring.getFirst().z())) {
                far = k;
            }
        }
        keep[far] = true;
        simplify(closed, 0, far, keep);
        simplify(closed, far, closed.size() - 1, keep);
        final List<P> kept = new ArrayList<>();
        for (int k = 0; k < ring.size(); k++) {
            if (keep[k]) {
                kept.add(ring.get(k));
            }
        }
        return kept;
    }

    private static void simplify(final List<P> line, final int a, final int b, final boolean[] keep) {
        double most = 0;
        int at = -1;
        final P p = line.get(a);
        final P q = line.get(b);
        final double len = Math.hypot(q.x() - p.x(), q.z() - p.z());
        for (int k = a + 1; k < b; k++) {
            final P r = line.get(k);
            final double d = len < 1e-9 ? Math.hypot(r.x() - p.x(), r.z() - p.z())
                    : Math.abs((q.x() - p.x()) * (p.z() - r.z()) - (p.x() - r.x()) * (q.z() - p.z())) / len;
            if (d > most) {
                most = d;
                at = k;
            }
        }
        if (at >= 0 && most > SIMPLIFY) {
            keep[at] = true;
            simplify(line, a, at, keep);
            simplify(line, at, b, keep);
        }
    }

    private static String loop(final List<P> ring) {
        final StringBuilder s = new StringBuilder();
        for (final P p : ring) {
            if (!s.isEmpty()) {
                s.append(' ');
            }
            s.append(String.format(Locale.ROOT, "%.1f,%.1f", p.x(), p.z()));
        }
        return s.toString();
    }
}
