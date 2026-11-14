package com.example.fxsuite.avatar.build;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * A wall's design, put together: a wall doing {@code role} (a wall, or a base), of what its palette
 * makes that of and as thick as it says (or {@code thickness}, if that is not 0), about the surface's
 * plane, cut by its building's lines into cells (a bay of a storey each); each cell filled
 * ({@link Infill}) as {@code pattern} says, the fill cutting its openings out of the wall and adding
 * what it adds (glass, a balcony); its parapet band solid; and {@code trims} over all of it (bands
 * at its floors, fins, a coping). Its wall is carried round its ends by half its thickness, to meet
 * the walls at its corners, if {@code wraps}. A base in a stone palette is rusticated: laid in
 * courses as high as the palette says, each standing proud of the joints between them. What is
 * doubtful about it ({@link #doubts}): an opening wider than its wall's material spans.
 *
 * @param title     what it is called
 * @param role      its wall's: {@link Role#WALL} or {@link Role#BASE}
 * @param thickness its wall's; 0 for its palette's
 * @param pattern   which fill each cell has
 * @param trims     laid over it all
 * @param wraps     whether it reaches round its corners
 */
public record Facade(String title, Role role, double thickness, Pattern pattern, List<Design> trims,
                     boolean wraps) implements Design {

    public Facade {
        trims = List.copyOf(trims);
    }

    /** A bay ({@code bay}) of a storey ({@code storey}): from {@code u0} to {@code u1}, {@code v0} to {@code v1}. */
    public record Cell(int bay, int storey, double u0, double u1, double v0, double v1) {

        public double width() {
            return u1 - u0;
        }

        public double height() {
            return v1 - v0;
        }

        public String name() {
            return "storey " + storey + ", bay " + bay;
        }
    }

    /** A rectangle on a surface, in u and v. */
    public record Rect(double u0, double u1, double v0, double v1) {

        public double area() {
            return Math.max(0, u1 - u0) * Math.max(0, v1 - v0);
        }
    }

    /** What a cell's fill does: the openings it cuts in the wall, and the solids it adds. */
    public record Fill(List<Rect> openings, List<Solid> solids) {

        public Fill {
            openings = List.copyOf(openings);
            solids = List.copyOf(solids);
        }

        public static final Fill NOTHING = new Fill(List.of(), List.of());
    }

    /** How a cell of a wall is filled: a window, a balcony, nothing. */
    @FunctionalInterface
    public interface Infill {
        Fill fill(Surface surface, Cell cell, double thickness, Palette palette);
    }

    /** Which fill each cell has. */
    @FunctionalInterface
    public interface Pattern {
        Infill at(Cell cell);

        /** The same in every cell. */
        static Pattern all(final Infill infill) {
            return cell -> infill;
        }

        /** {@code a} and {@code b} by turns, bay by bay and storey by storey, as a chessboard's squares. */
        static Pattern checker(final Infill a, final Infill b) {
            return cell -> (cell.bay() + cell.storey()) % 2 == 0 ? a : b;
        }
    }

    /** How thick its wall is, in {@code palette}. */
    public double thick(final Palette palette) {
        return thickness > 0 ? thickness : palette.thickness(role);
    }

    /** How high its courses are, in {@code palette}: a base's, if the palette rusticates it; else 0. */
    private double courses(final Palette palette) {
        return role == Role.BASE ? palette.rustication() : 0;
    }

    /** Each cell of {@code surface}, as it is filled in {@code palette}. */
    private void cells(final Surface surface, final Palette palette,
                       final java.util.function.BiConsumer<Cell, Fill> each) {
        for (int s = 0; s < surface.storeys(); s++) {
            for (int b = 0; b < surface.bays(); b++) {
                final Cell cell = new Cell(b, s, surface.columns()[b], surface.columns()[b + 1], surface.floors()[s],
                        surface.storeyTop(s));
                each.accept(cell, pattern.at(cell).fill(surface, cell, thick(palette), palette));
            }
        }
    }

    @Override
    public List<Solid> dress(final Surface surface, final Palette palette) {
        final List<Solid> solids = new ArrayList<>();
        final double thickness = thick(palette);
        final double half = thickness / 2;
        final double course = courses(palette);
        final double last = surface.bays() - 1;
        cells(surface, palette, (cell, fill) -> {
            final Rect wall = new Rect(cell.u0() - (wraps && cell.bay() == 0 ? half : 0),
                    cell.u1() + (wraps && cell.bay() == last ? half : 0), cell.v0(), cell.v1());
            int k = 0;
            for (final Rect piece : cut(wall, fill.openings())) {
                solids.add(surface.box(cell.name() + ", wall " + k, palette, role, piece.u0(), piece.u1(), piece.v0(),
                        piece.v1(), -half, half));
                if (course > 0) {
                    rusticate(surface, palette, cell.name() + ", wall " + k, piece, half, course, solids);
                }
                k++;
            }
            solids.addAll(fill.solids());
        });
        if (surface.parapet() > 0) {
            solids.add(surface.box("parapet", palette, role, wraps ? -half : 0, surface.width() + (wraps ? half : 0),
                    surface.height() - surface.parapet(), surface.height(), -half, half));
        }
        for (final Design trim : trims) {
            solids.addAll(trim.dress(surface, palette));
        }
        return solids;
    }

    /** Rustication's joints, this wide: each course stands this far proud of them. */
    private static final double PROUD = 2;
    private static final double JOINT = 3;

    /**
     * {@code piece} of a wall laid in courses {@code course} high, from the surface's foot: each a
     * slab standing proud of its face, the joints between them left back.
     */
    private void rusticate(final Surface surface, final Palette palette, final String name, final Rect piece,
                           final double half, final double course, final List<Solid> solids) {
        final int first = (int) Math.floor(piece.v0() / course);
        for (int c = first; c * course < piece.v1(); c++) {
            final double low = Math.max(piece.v0(), c * course + (c == 0 ? 0 : JOINT / 2));
            final double high = Math.min(piece.v1(), (c + 1) * course - JOINT / 2);
            if (high - low > JOINT) {
                solids.add(surface.box(name + ", course " + c, palette, role, piece.u0(), piece.u1(), low, high, half,
                        half + PROUD));
            }
        }
    }

    /**
     * What is doubtful about it on {@code surface} in {@code palette}: an opening wider than its
     * wall's material spans with nothing to carry what is over it.
     */
    @Override
    public List<String> doubts(final Surface surface, final Palette palette) {
        final Material material = palette.of(role);
        final double[] widest = {0};
        cells(surface, palette, (cell, fill) -> fill.openings()
                .forEach(o -> widest[0] = Math.max(widest[0], o.u1() - o.u0())));
        if (widest[0] <= material.span()) {
            return List.of();
        }
        return List.of(String.format(java.util.Locale.ROOT, "%s: %s in %s, openings %.1f m wide; %s spans %.1f m unsupported",
                surface.name(), title, material.title(), widest[0] / 100, material.title(), material.span() / 100));
    }

    /**
     * {@code whole}, less {@code holes}: as rectangles that cover what is left and nothing else. Cut
     * into strips at every hole's sides; each strip, less what holes span all of it, in rectangles up
     * it; strips side by side that come out alike, put back together.
     */
    public static List<Rect> cut(final Rect whole, final List<Rect> holes) {
        final TreeSet<Double> edges = new TreeSet<>(List.of(whole.u0(), whole.u1()));
        for (final Rect hole : holes) {
            for (final double u : new double[] {hole.u0(), hole.u1()}) {
                if (u > whole.u0() && u < whole.u1()) {
                    edges.add(u);
                }
            }
        }
        final List<Rect> pieces = new ArrayList<>();
        List<double[]> open = null;
        double from = whole.u0();
        Double previous = null;
        for (final double u : edges) {
            if (previous != null) {
                final double middle = (previous + u) / 2;
                final List<double[]> spans = new ArrayList<>();
                double v = whole.v0();
                final List<Rect> across = holes.stream().filter(h -> h.u0() < middle && h.u1() > middle)
                        .sorted((a, b) -> Double.compare(a.v0(), b.v0())).toList();
                for (final Rect hole : across) {
                    final double low = Math.max(whole.v0(), hole.v0());
                    if (low > v) {
                        spans.add(new double[] {v, low});
                    }
                    v = Math.max(v, Math.min(whole.v1(), hole.v1()));
                }
                if (v < whole.v1()) {
                    spans.add(new double[] {v, whole.v1()});
                }
                if (open != null && !same(open, spans)) {
                    for (final double[] span : open) {
                        pieces.add(new Rect(from, previous, span[0], span[1]));
                    }
                    from = previous;
                }
                if (open == null) {
                    from = previous;
                }
                open = spans;
            }
            previous = u;
        }
        if (open != null) {
            for (final double[] span : open) {
                pieces.add(new Rect(from, whole.u1(), span[0], span[1]));
            }
        }
        return pieces;
    }

    private static boolean same(final List<double[]> a, final List<double[]> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int k = 0; k < a.size(); k++) {
            if (a.get(k)[0] != b.get(k)[0] || a.get(k)[1] != b.get(k)[1]) {
                return false;
            }
        }
        return true;
    }
}
