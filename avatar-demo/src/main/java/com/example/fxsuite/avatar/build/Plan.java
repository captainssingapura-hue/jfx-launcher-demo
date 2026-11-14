package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Form;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * A floor plan, composed: rectangles of structural grid ({@link Grid}), each added to it or cut out
 * of it, in turn; so a square, a rectangle, an L, a cross, a courtyard. In centimetres, x across (to
 * the east), y along (to the north).
 *
 * <p>From that, its {@link Layout}: its lines (every grid's, together), its cells (filled, or not),
 * its outline traced round the filled cells, square; its outline shaped where its corners say
 * ({@link Corner}: cut, rounded, an ellipse), in straight pieces; its columns, where its grids'
 * lines cross within it (a grid's lines within {@link #SNAP} of a line of a grid before it moved
 * onto it, so the grids put together share their lines, and their columns); and, as a graph ({@link Form}, level), its columns, the beams along its
 * lines between them, and its outline.
 *
 * @param name  what it is
 * @param parts its grids, each added or cut, in turn
 */
public record Plan(String name, List<Part> parts) {

    /** A grid, added to the plan or cut out of it. */
    public record Part(Grid grid, boolean cut) {
    }

    /** A point on the plan. */
    public record Pt(double x, double y) {

        public double distance(final Pt other) {
            return Math.hypot(x - other.x, y - other.y);
        }
    }

    /** Points nearer than this, in centimetres, are one. */
    public static final double TOLERANCE = 0.5;
    /** A grid's lines this near one of the grids before it are moved onto it: they share one set of lines. */
    public static final double SNAP = 150;

    public Plan {
        parts = List.copyOf(parts);
        if (parts.isEmpty() || parts.getFirst().cut()) {
            throw new IllegalArgumentException(name + " starts with a grid added");
        }
    }

    /** A plan of {@code grids}, all added. */
    public static Plan of(final String name, final Grid... grids) {
        return new Plan(name, java.util.Arrays.stream(grids).map(g -> new Part(g, false)).toList());
    }

    /** The same, {@code grid} added to it. */
    public Plan add(final Grid grid) {
        final List<Part> more = new ArrayList<>(parts);
        more.add(new Part(grid, false));
        return new Plan(name, more);
    }

    /** The same, {@code grid} cut out of it. */
    public Plan cut(final Grid grid) {
        final List<Part> more = new ArrayList<>(parts);
        more.add(new Part(grid, true));
        return new Plan(name, more);
    }

    /** All that comes of it, worked out. */
    public Layout layout() {
        return new Layout(this);
    }

    /**
     * A plan worked out: its lines, its cells, its outline (square, and shaped), its columns, what is
     * wrong with it.
     */
    public static final class Layout {
        private final Plan plan;
        /** Its grids, as put together: each after the first moved onto the lines before it, where near them. */
        private final List<Part> parts = new ArrayList<>();
        private final double[] xs;
        private final double[] ys;
        private final boolean[][] filled;
        private final List<List<Pt>> square = new ArrayList<>();
        private final List<List<Pt>> shaped = new ArrayList<>();
        /** Each shaped loop's points, whether each lies on a curve (a corner shaped). */
        private final List<List<Boolean>> curved = new ArrayList<>();
        private final Map<String, Pt> columns = new LinkedHashMap<>();
        /** The columns along its curves, by name: where its grid stands back from its outline. */
        private final Map<String, Pt> perimeter = new LinkedHashMap<>();
        /** The beams tying each of those to the nearest column of its grid. */
        private final List<Pt[]> ties = new ArrayList<>();
        private final List<String> problems = new ArrayList<>();

        Layout(final Plan plan) {
            this.plan = plan;
            // its grids, each after the first with its lines moved onto those before it, if near them
            final TreeSet<Double> xLines = new TreeSet<>();
            final TreeSet<Double> yLines = new TreeSet<>();
            for (final Part part : plan.parts()) {
                final Grid grid = xLines.isEmpty() ? part.grid() : part.grid().snapped(xLines, yLines, SNAP);
                parts.add(new Part(grid, part.cut()));
                for (final double x : grid.xs()) {
                    xLines.add(snap(x));
                }
                for (final double y : grid.ys()) {
                    yLines.add(snap(y));
                }
            }
            xs = xLines.stream().mapToDouble(Double::doubleValue).toArray();
            ys = yLines.stream().mapToDouble(Double::doubleValue).toArray();
            filled = new boolean[xs.length - 1][ys.length - 1];
            for (int i = 0; i + 1 < xs.length; i++) {
                for (int j = 0; j + 1 < ys.length; j++) {
                    final double cx = (xs[i] + xs[i + 1]) / 2;
                    final double cy = (ys[j] + ys[j + 1]) / 2;
                    for (final Part part : parts) {
                        if (part.grid().covers(cx, cy)) {
                            filled[i][j] = !part.cut();
                        }
                    }
                }
            }
            trace();
            shape();
            placeColumns();
            placePerimeter();
            if (square.isEmpty()) {
                problems.add("nothing left of it");
            } else if (square.stream().filter(loop -> signedArea(loop) > 0).count() > 1) {
                problems.add("it is in pieces");
            }
            if (!columns.isEmpty() && structures() > 1) {
                problems.add("its structure is in " + structures() + " pieces: its grids share no line to tie them");
            }
        }

        private static double snap(final double v) {
            return Math.round(v * 1000) / 1000.0;
        }

        public Plan plan() {
            return plan;
        }

        public double[] xs() {
            return xs.clone();
        }

        public double[] ys() {
            return ys.clone();
        }

        /** Whether the cell between lines {@code i} and {@code i + 1} across, {@code j} and {@code j + 1} along, is in it. */
        public boolean filled(final int i, final int j) {
            return i >= 0 && j >= 0 && i < filled.length && j < filled[0].length && filled[i][j];
        }

        /** Its outlines, square: its outside first (anticlockwise), then any courtyards (clockwise). */
        public List<List<Pt>> square() {
            return square.stream().map(List::copyOf).toList();
        }

        /** Its outlines with their corners shaped, in straight pieces. */
        public List<List<Pt>> shaped() {
            return shaped.stream().map(List::copyOf).toList();
        }

        /** For each of {@link #shaped}'s points, whether it is on a shaped corner's curve. */
        public List<List<Boolean>> curved() {
            return curved.stream().map(List::copyOf).toList();
        }

        /** Its columns, by name: where its grids' lines cross, within its outline as shaped. */
        public Map<String, Pt> columns() {
            return Map.copyOf(columns);
        }

        /** In order, as found. */
        public List<String> columnNames() {
            return List.copyOf(columns.keySet());
        }

        public List<String> problems() {
            return List.copyOf(problems);
        }

        /** How much floor it has, in square centimetres: inside its shaped outline, less its courtyards. */
        public double area() {
            return shaped.stream().mapToDouble(Layout::signedArea).sum();
        }

        /** How long its shaped outline is, all of it. */
        public double perimeter() {
            double length = 0;
            for (final List<Pt> loop : shaped) {
                for (int k = 0; k < loop.size(); k++) {
                    length += loop.get(k).distance(loop.get((k + 1) % loop.size()));
                }
            }
            return length;
        }

        /** The box it fits in: west, south, east, north. */
        public double[] bounds() {
            double w = Double.POSITIVE_INFINITY;
            double s = Double.POSITIVE_INFINITY;
            double e = Double.NEGATIVE_INFINITY;
            double n = Double.NEGATIVE_INFINITY;
            for (final List<Pt> loop : shaped) {
                for (final Pt p : loop) {
                    w = Math.min(w, p.x());
                    s = Math.min(s, p.y());
                    e = Math.max(e, p.x());
                    n = Math.max(n, p.y());
                }
            }
            return new double[] {w, s, e, n};
        }

        static double signedArea(final List<Pt> loop) {
            double twice = 0;
            for (int k = 0; k < loop.size(); k++) {
                final Pt a = loop.get(k);
                final Pt b = loop.get((k + 1) % loop.size());
                twice += a.x() * b.y() - b.x() * a.y();
            }
            return twice / 2;
        }

        /**
         * Its outlines, traced round its filled cells, each cell's edge that has no filled cell beyond
         * it an edge of the outline, run so the cells are on its left: its outside anticlockwise, a
         * courtyard clockwise. Edges running on straight are joined into one.
         */
        private void trace() {
            // each edge by where it starts, as grid indices
            final Map<Long, List<int[]>> from = new HashMap<>();
            final int nx = filled.length;
            final int ny = filled[0].length;
            for (int i = 0; i < nx; i++) {
                for (int j = 0; j < ny; j++) {
                    if (!filled[i][j]) {
                        continue;
                    }
                    if (!filled(i, j - 1)) {
                        edge(from, i, j, i + 1, j);
                    }
                    if (!filled(i + 1, j)) {
                        edge(from, i + 1, j, i + 1, j + 1);
                    }
                    if (!filled(i, j + 1)) {
                        edge(from, i + 1, j + 1, i, j + 1);
                    }
                    if (!filled(i - 1, j)) {
                        edge(from, i, j + 1, i, j);
                    }
                }
            }
            while (!from.isEmpty()) {
                final long startKey = from.keySet().stream().min(Long::compare).orElseThrow();
                final List<int[]> path = new ArrayList<>();
                int[] edge = take(from, startKey, null);
                final int si = edge[0];
                final int sj = edge[1];
                while (true) {
                    path.add(edge);
                    if (edge[2] == si && edge[3] == sj) {
                        break;
                    }
                    final long next = key(edge[2], edge[3]);
                    if (!from.containsKey(next)) {
                        problems.add("its outline does not close");
                        break;
                    }
                    if (from.get(next).size() > 1) {
                        problems.add("two of its parts touch only at a corner, at " + (xs[edge[2]] / 100) + " m, "
                                + (ys[edge[3]] / 100) + " m");
                    }
                    edge = take(from, next, edge);
                }
                // its corners: where the way it runs turns
                final List<Pt> loop = new ArrayList<>();
                for (int k = 0; k < path.size(); k++) {
                    final int[] in = path.get((k + path.size() - 1) % path.size());
                    final int[] out = path.get(k);
                    if (Integer.signum(in[2] - in[0]) != Integer.signum(out[2] - out[0])
                            || Integer.signum(in[3] - in[1]) != Integer.signum(out[3] - out[1])) {
                        loop.add(new Pt(xs[out[0]], ys[out[1]]));
                    }
                }
                square.add(loop);
            }
            // its outside first
            square.sort((a, b) -> Double.compare(signedArea(b), signedArea(a)));
        }

        private static long key(final int i, final int j) {
            return ((long) i << 32) | (j & 0xffffffffL);
        }

        private static void edge(final Map<Long, List<int[]>> from, final int i0, final int j0, final int i1,
                                 final int j1) {
            from.computeIfAbsent(key(i0, j0), k -> new ArrayList<>()).add(new int[] {i0, j0, i1, j1});
        }

        /** The edge from {@code at} to take next, after {@code came}: where there are two, the one turning left. */
        private static int[] take(final Map<Long, List<int[]>> from, final long at, final int[] came) {
            final List<int[]> out = from.get(at);
            int pick = 0;
            if (came != null && out.size() > 1) {
                final int dx = came[2] - came[0];
                final int dy = came[3] - came[1];
                for (int k = 0; k < out.size(); k++) {
                    final int ex = out.get(k)[2] - out.get(k)[0];
                    final int ey = out.get(k)[3] - out.get(k)[1];
                    if (dx * ey - dy * ex > 0) {
                        pick = k;
                    }
                }
            }
            final int[] edge = out.remove(pick);
            if (out.isEmpty()) {
                from.remove(at);
            }
            return edge;
        }

        /** How its corner at {@code at} is shaped: as the last grid whose corner is there says; square if none does. */
        private Corner cornerAt(final Pt at) {
            Corner corner = Corner.SQUARE;
            for (final Part part : parts) {
                for (final Grid.Quarter quarter : Grid.Quarter.values()) {
                    final double[] c = part.grid().at(quarter);
                    if (Math.abs(c[0] - at.x()) < TOLERANCE && Math.abs(c[1] - at.y()) < TOLERANCE
                            && !part.grid().corner(quarter).square()) {
                        corner = part.grid().corner(quarter);
                    }
                }
            }
            return corner;
        }

        /**
         * Its outlines shaped: at each corner its grids shape, the corner cut back along the edges
         * either side of it and the cut drawn as its corner's curve, in straight pieces. A shaped
         * corner must be an outside one, and the two cuts along an edge must leave some of it.
         */
        private void shape() {
            for (final List<Pt> loop : square) {
                final int n = loop.size();
                final Corner[] corners = new Corner[n];
                final double[] back = new double[n];
                final double[] on = new double[n];
                for (int k = 0; k < n; k++) {
                    final Pt prev = loop.get((k + n - 1) % n);
                    final Pt at = loop.get(k);
                    final Pt next = loop.get((k + 1) % n);
                    corners[k] = cornerAt(at);
                    if (corners[k].square()) {
                        continue;
                    }
                    final double cross = (at.x() - prev.x()) * (next.y() - at.y()) - (at.y() - prev.y()) * (next.x() - at.x());
                    if (cross <= 0) {
                        problems.add(String.format(java.util.Locale.ROOT,
                                "its inside corner at %.1f m, %.1f m is not shaped: only outside corners are, as yet",
                                at.x() / 100, at.y() / 100));
                        corners[k] = Corner.SQUARE;
                        continue;
                    }
                    // the cut along the edge coming in, and the edge going out: across or along as each runs
                    final boolean inAcross = Math.abs(at.y() - prev.y()) < TOLERANCE;
                    back[k] = inAcross ? corners[k].across() : corners[k].along();
                    on[k] = inAcross ? corners[k].along() : corners[k].across();
                }
                for (int k = 0; k < n; k++) {
                    final Pt at = loop.get(k);
                    final Pt next = loop.get((k + 1) % n);
                    if (on[k] + back[(k + 1) % n] > at.distance(next) + TOLERANCE) {
                        problems.add(String.format(java.util.Locale.ROOT,
                                "its corners' cuts overrun the edge from %.1f m, %.1f m to %.1f m, %.1f m",
                                at.x() / 100, at.y() / 100, next.x() / 100, next.y() / 100));
                    }
                }
                final List<Pt> out = new ArrayList<>();
                final List<Boolean> onCurve = new ArrayList<>();
                for (int k = 0; k < n; k++) {
                    final Pt prev = loop.get((k + n - 1) % n);
                    final Pt at = loop.get(k);
                    final Pt next = loop.get((k + 1) % n);
                    if (corners[k].square()) {
                        add(out, onCurve, at, false);
                        continue;
                    }
                    final double inLength = at.distance(prev);
                    final double outLength = at.distance(next);
                    final double ix = (at.x() - prev.x()) / inLength;
                    final double iy = (at.y() - prev.y()) / inLength;
                    final double ox = (next.x() - at.x()) / outLength;
                    final double oy = (next.y() - at.y()) / outLength;
                    final int pieces = corners[k].pieces(back[k], on[k]);
                    for (int p = 0; p <= pieces; p++) {
                        final double[] f = corners[k].at((double) p / pieces);
                        add(out, onCurve, new Pt(at.x() - ix * back[k] * f[0] + ox * on[k] * f[1],
                                at.y() - iy * back[k] * f[0] + oy * on[k] * f[1]), true);
                    }
                }
                shaped.add(out);
                curved.add(onCurve);
            }
        }

        /** {@code p} onto the outline, unless it is the point just before it (or the first, ending it). */
        private static void add(final List<Pt> out, final List<Boolean> onCurve, final Pt p, final boolean curve) {
            if (!out.isEmpty() && out.getLast().distance(p) < TOLERANCE) {
                onCurve.set(onCurve.size() - 1, onCurve.getLast() || curve);
                return;
            }
            if (out.size() > 1 && out.getFirst().distance(p) < TOLERANCE) {
                onCurve.set(0, onCurve.getFirst() || curve);
                return;
            }
            out.add(p);
            onCurve.add(curve);
        }

        /** Its columns: where each grid added to it has its lines cross, within its outline as shaped (or on it). */
        private void placeColumns() {
            for (final Part part : parts) {
                if (part.cut()) {
                    continue;
                }
                for (final double x : part.grid().xs()) {
                    for (final double y : part.grid().ys()) {
                        final Pt at = new Pt(snap(x), snap(y));
                        final String name = "column x" + index(xs, at.x()) + " y" + index(ys, at.y());
                        if (!columns.containsKey(name) && inside(at)) {
                            columns.put(name, at);
                        }
                    }
                }
            }
        }

        private static int index(final double[] lines, final double v) {
            for (int k = 0; k < lines.length; k++) {
                if (Math.abs(lines[k] - v) < TOLERANCE) {
                    return k;
                }
            }
            return -1;
        }

        /** Whether {@code p} is within its shaped outline, or on it: in its outside, in none of its courtyards. */
        public boolean inside(final Pt p) {
            for (final List<Pt> loop : shaped) {
                for (int k = 0; k < loop.size(); k++) {
                    if (toSegment(p, loop.get(k), loop.get((k + 1) % loop.size())) < TOLERANCE) {
                        return true;
                    }
                }
            }
            boolean in = false;
            for (final List<Pt> loop : shaped) {
                for (int k = 0, m = loop.size() - 1; k < loop.size(); m = k++) {
                    final Pt a = loop.get(k);
                    final Pt b = loop.get(m);
                    if ((a.y() > p.y()) != (b.y() > p.y())
                            && p.x() < (b.x() - a.x()) * (p.y() - a.y()) / (b.y() - a.y()) + a.x()) {
                        in = !in;
                    }
                }
            }
            return in;
        }

        private static double toSegment(final Pt p, final Pt a, final Pt b) {
            final double dx = b.x() - a.x();
            final double dy = b.y() - a.y();
            final double length = dx * dx + dy * dy;
            final double t = length == 0 ? 0 : Math.clamp(((p.x() - a.x()) * dx + (p.y() - a.y()) * dy) / length, 0, 1);
            return Math.hypot(p.x() - (a.x() + t * dx), p.y() - (a.y() + t * dy));
        }

        /** Its beams: along each line across, then along, between columns next to each other on it, where it runs within it. */
        public List<Pt[]> beams() {
            final List<Pt[]> beams = new ArrayList<>();
            for (final boolean across : new boolean[] {true, false}) {
                final Map<Double, List<Pt>> lines = new java.util.TreeMap<>();
                columns.values().forEach(p -> lines.computeIfAbsent(across ? p.y() : p.x(), k -> new ArrayList<>()).add(p));
                for (final List<Pt> line : lines.values()) {
                    line.sort((a, b) -> Double.compare(across ? a.x() : a.y(), across ? b.x() : b.y()));
                    for (int k = 0; k + 1 < line.size(); k++) {
                        final Pt a = line.get(k);
                        final Pt b = line.get(k + 1);
                        if (inside(new Pt((a.x() + b.x()) / 2, (a.y() + b.y()) / 2))) {
                            beams.add(new Pt[] {a, b});
                        }
                    }
                }
            }
            return beams;
        }

        /** Its columns along its curves, by name. */
        public Map<String, Pt> perimeterColumns() {
            return Map.copyOf(perimeter);
        }

        /** The beams tying its columns along its curves each to the nearest column of its grid. */
        public List<Pt[]> ties() {
            return List.copyOf(ties);
        }

        /** All its columns, by name: its grid's, and those along its curves. */
        public Map<String, Pt> allColumns() {
            final Map<String, Pt> all = new LinkedHashMap<>(columns);
            all.putAll(perimeter);
            return all;
        }

        /** A column along its curves no further than this from the next. */
        public static final double PERIMETER = 600;

        /**
         * Columns along each curve of its outline, where its grid stands back from it: at each end of
         * the curve (where it leaves a straight edge) and evenly between, no more than
         * {@link #PERIMETER} apart round it; a whole loop curved (a circle) evenly all round. Each tied
         * to the nearest column of its grid by a beam.
         */
        private void placePerimeter() {
            for (int l = 0; l < shaped.size(); l++) {
                final List<Pt> loop = shaped.get(l);
                final List<Boolean> onCurve = curved.get(l);
                final int n = loop.size();
                final boolean whole = onCurve.stream().allMatch(c -> c);
                // each run of points on a curve, in turn round the loop: where it starts and how many points it has
                final List<int[]> runs = new ArrayList<>();
                if (whole) {
                    runs.add(new int[] {0, n + 1});
                } else {
                    int start = 0;
                    while (onCurve.get(start)) {
                        start++;
                    }
                    for (int k = 1; k <= n; k++) {
                        final int at = (start + k) % n;
                        if (onCurve.get(at) && !onCurve.get((at + n - 1) % n)) {
                            int length = 0;
                            while (onCurve.get((at + length) % n) && length < n) {
                                length++;
                            }
                            runs.add(new int[] {at, length});
                        }
                    }
                }
                for (final int[] run : runs) {
                    final double[] along = new double[run[1]];
                    for (int k = 1; k < run[1]; k++) {
                        along[k] = along[k - 1] + loop.get((run[0] + k - 1) % n).distance(loop.get((run[0] + k) % n));
                    }
                    final double length = along[run[1] - 1];
                    final int pieces = Math.max(1, (int) Math.ceil(length / PERIMETER));
                    for (int p = 0; p < pieces + (whole ? 0 : 1); p++) {
                        final double want = length * p / pieces;
                        int best = 0;
                        for (int k = 1; k < run[1]; k++) {
                            if (Math.abs(along[k] - want) < Math.abs(along[best] - want)) {
                                best = k;
                            }
                        }
                        final int index = (run[0] + best) % n;
                        final Pt at = loop.get(index);
                        if (columns.values().stream().noneMatch(c -> c.distance(at) < TOLERANCE)) {
                            perimeter.putIfAbsent("perimeter " + l + ", " + index, at);
                        }
                    }
                }
            }
            for (final Pt at : perimeter.values()) {
                columns.values().stream().min((a, b) -> Double.compare(a.distance(at), b.distance(at)))
                        .filter(nearest -> inside(new Pt((nearest.x() + at.x()) / 2, (nearest.y() + at.y()) / 2)))
                        .ifPresent(nearest -> ties.add(new Pt[] {at, nearest}));
            }
        }

        /** Its columns and beams, how many pieces: one, unless its grids share no line to tie them. */
        public int structures() {
            final Map<String, String> lot = new HashMap<>();
            columns.values().forEach(p -> lot.put(key(p), key(p)));
            perimeter.values().forEach(p -> lot.put(key(p), key(p)));
            for (final Pt[] tie : ties) {
                lot.put(root(lot, key(tie[0])), root(lot, key(tie[1])));
            }
            for (final Pt[] beam : beams()) {
                final String a = root(lot, key(beam[0]));
                final String b = root(lot, key(beam[1]));
                lot.put(a, b);
            }
            return (int) lot.keySet().stream().filter(k -> root(lot, k).equals(k)).count();
        }

        private static String root(final Map<String, String> lot, final String k) {
            String r = k;
            while (!lot.get(r).equals(r)) {
                r = lot.get(r);
            }
            return r;
        }

        /**
         * As a graph, level ({@link Form}: across x, along y, up 0): its columns; a beam between each
         * two next to each other along a line, where it runs within it; its columns along its curves,
         * each tied to its grid; and its outline, shaped, each point of it a column where one stands there.
         */
        public Form graph() {
            final Form.Builder form = new Form.Builder(plan.name());
            final Map<String, Integer> at = new HashMap<>();
            columns.forEach((name, p) -> at.put(key(p), form.vertex(name, p.x(), p.y(), 0)));
            perimeter.forEach((name, p) -> at.put(key(p), form.vertex(name, p.x(), p.y(), 0)));
            for (final Pt[] beam : beams()) {
                form.edge(at.get(key(beam[0])), at.get(key(beam[1])));
            }
            for (final Pt[] tie : ties) {
                form.edge(at.get(key(tie[0])), at.get(key(tie[1])));
            }
            for (int l = 0; l < shaped.size(); l++) {
                // its points, and the columns standing on it between them, in turn
                final List<Pt> loop = new ArrayList<>();
                final List<Pt> corners = shaped.get(l);
                for (int k = 0; k < corners.size(); k++) {
                    final Pt a = corners.get(k);
                    final Pt b = corners.get((k + 1) % corners.size());
                    loop.add(a);
                    columns.values().stream()
                            .filter(c -> c.distance(a) > TOLERANCE && c.distance(b) > TOLERANCE && toSegment(c, a, b) < TOLERANCE)
                            .sorted((c, d) -> Double.compare(c.distance(a), d.distance(a))).forEach(loop::add);
                }
                final int[] points = new int[loop.size()];
                for (int k = 0; k < loop.size(); k++) {
                    final Pt p = loop.get(k);
                    final String key = key(p);
                    if (!at.containsKey(key)) {
                        at.put(key, form.vertex("outline " + l + ", " + k, p.x(), p.y(), 0));
                    }
                    points[k] = at.get(key);
                }
                // its edges, but for those a beam already runs along
                final Form built = form.build();
                for (int k = 0; k < points.length; k++) {
                    final int a = points[k];
                    final int b = points[(k + 1) % points.length];
                    final boolean beam = built.edges().stream().anyMatch(e -> e.from() == a && e.to() == b
                            || e.from() == b && e.to() == a);
                    if (!beam) {
                        form.edge(a, b);
                    }
                }
            }
            return form.build();
        }

        private static String key(final Pt p) {
            return Math.round(p.x() * 10) + "," + Math.round(p.y() * 10);
        }
    }
}
