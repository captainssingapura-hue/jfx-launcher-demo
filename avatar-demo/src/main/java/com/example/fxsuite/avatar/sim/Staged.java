package com.example.fxsuite.avatar.sim;

import java.util.ArrayList;
import java.util.List;

/**
 * A building as finely as what is happening at it needs ({@code staged-fineness}): its coarse shape, its outline stood
 * up a stretch of storeys at a time, nothing inside it; its fine model, every part of it, some breaking on their own
 * ({@link Assembly}), as broken as they are whatever its stage, which is what has happened to it kept; its storeys, its
 * cells, by the heights between them; and the stage it is at ({@link Stage}), as the world's focus has it
 * ({@link Play}). To the world one volume, whose box is both shapes' and never changes, so the world's index of what is
 * in the way stays as it is however its stage changes.
 *
 * <p>In focus, with fine physics on ({@link #FINE}), the storeys the focus meets are fine: its fine model's parts in
 * them answer, its glass breaks, its doorways are open; above and below them, its coarse shape cut away where they are.
 * Anywhere else, at any other stage, its coarse shape answers.
 */
public final class Staged implements Volume {

    /** Whether a building in focus is in the way part by part where the focus is, as its fine model is. */
    public static final boolean FINE = true;

    /** How finely a building is wanted: far from the focus, near it, met by it. */
    public enum Stage { FAR, NEAR, FOCUS }

    private final List<Volume> coarseParts;
    private final Assembly coarse;
    private final Assembly fine;
    /** Its storeys' floors, the lowest first, and its roof: its cells, by the heights between them. */
    private final double[] levels;
    private final Stage stage;
    /** The band of storeys fine, from and to up; none ({@code NaN}) but in focus. */
    private final double from;
    private final double to;
    /** Its coarse shape cut away where the band is (null, if none is left); its fine model answering in the band. */
    private final Assembly cut;
    private final Assembly banded;

    /** Its coarse shape's volumes, its fine model, its storeys by their floors' heights and its roof's: far off. */
    public Staged(final List<Volume> coarse, final Assembly fine, final double[] levels) {
        this(coarse, new Assembly(coarse, List.of(), 0, 0), fine, levels.clone(), Stage.FAR, Double.NaN, Double.NaN);
    }

    private Staged(final List<Volume> coarseParts, final Assembly coarse, final Assembly fine, final double[] levels,
                   final Stage stage, final double from, final double to) {
        this.coarseParts = List.copyOf(coarseParts);
        this.coarse = coarse;
        this.fine = fine;
        this.levels = levels;
        this.stage = stage;
        this.from = from;
        this.to = to;
        if (Double.isNaN(from)) {
            this.cut = null;
            this.banded = null;
        } else {
            final List<Volume> left = new ArrayList<>();
            for (final Volume v : coarseParts) {
                if (v instanceof Prism p) {
                    if (Math.min(p.top(), from) > p.bottom()) {
                        left.add(new Prism(p.outline(), p.holes(), p.bottom(), Math.min(p.top(), from)));
                    }
                    if (p.top() > Math.max(p.bottom(), to)) {
                        left.add(new Prism(p.outline(), p.holes(), Math.max(p.bottom(), to), p.top()));
                    }
                } else {
                    left.add(v);
                }
            }
            this.cut = left.isEmpty() ? null : new Assembly(left, List.of(), 0, 0);
            this.banded = fine.banded(from, to);
        }
    }

    public Stage stage() {
        return stage;
    }

    /** Its fine model, as broken as it is: what is drawn of it, and what happened to it. */
    public Assembly fine() {
        return fine;
    }

    /** Its coarse shape. */
    public Assembly coarse() {
        return coarse;
    }

    /** Where the band of fine storeys begins and ends, up; {@code NaN} if none is fine. */
    public double from() {
        return from;
    }

    public double to() {
        return to;
    }

    /** Whether its fine model answers where the focus is: in focus, fine physics on, some of its storeys met by it. */
    public boolean fineAnswering() {
        return FINE && stage == Stage.FOCUS && !Double.isNaN(from);
    }

    /**
     * The same at {@code now}, the focus reaching from {@code low} to {@code high} up: in focus, its storeys the focus
     * meets the band fine, from the floor of the lowest to the floor over the highest; itself, if that is as it is.
     */
    public Staged focused(final Stage now, final double low, final double high) {
        double bandFrom = Double.NaN;
        double bandTo = Double.NaN;
        if (now == Stage.FOCUS) {
            for (int s = 0; s + 1 < levels.length; s++) {
                if (levels[s + 1] > low && levels[s] < high) {
                    bandFrom = Double.isNaN(bandFrom) ? levels[s] : bandFrom;
                    bandTo = levels[s + 1];
                }
            }
        }
        if (now == stage && same(bandFrom, from) && same(bandTo, to)) {
            return this;
        }
        return new Staged(coarseParts, coarse, fine, levels, now, bandFrom, bandTo);
    }

    private static boolean same(final double a, final double b) {
        return Double.isNaN(a) ? Double.isNaN(b) : a == b;
    }

    /** The pane of it a point is on (a round's, a blade's, where it met it), where it is fine; -1 if none. */
    public int paneAt(final Vec3 at) {
        return fineAnswering() ? banded.breakableAt(at) : -1;
    }

    /** The same, its fine model {@code now} (a pane of it broken), at its stage, its band as it was. */
    public Staged withFine(final Assembly now) {
        return new Staged(coarseParts, coarse, now.unbanded(), levels, stage, from, to);
    }

    /** It put where its building stands, {@code scale}d: far off, its fine model as broken as it is. */
    public Staged placed(final Place at, final double scale) {
        final List<Volume> moved = coarseParts.stream().map(v -> Volume.placed(v, at, scale)).toList();
        final double[] up = new double[levels.length];
        for (int k = 0; k < levels.length; k++) {
            up[k] = levels[k] * scale + at.height();
        }
        return new Staged(moved, new Assembly(moved, List.of(), 0, 0), fine.placed(at, scale), up, Stage.FAR,
                Double.NaN, Double.NaN);
    }

    /** Its box: both its shapes', so it never changes with its stage. */
    @Override
    public Bounds bounds() {
        final Bounds a = coarse.bounds();
        final Bounds b = fine.bounds();
        return new Bounds(Math.min(a.minX(), b.minX()), Math.max(a.maxX(), b.maxX()), Math.min(a.minZ(), b.minZ()),
                Math.max(a.maxZ(), b.maxZ()));
    }

    @Override
    public double low() {
        return Math.min(coarse.low(), fine.low());
    }

    @Override
    public double high() {
        return Math.max(coarse.high(), fine.high());
    }

    @Override
    public boolean covers(final double x, final double z, final double radius) {
        if (!fineAnswering()) {
            return coarse.covers(x, z, radius);
        }
        return cut != null && cut.covers(x, z, radius) || banded.covers(x, z, radius);
    }

    @Override
    public boolean steep(final double x, final double z, final double radius, final double top) {
        if (!fineAnswering()) {
            return coarse.steep(x, z, radius, top);
        }
        return cut != null && cut.steep(x, z, radius, top) || banded.steep(x, z, radius, top);
    }

    @Override
    public double topAt(final double x, final double z, final double radius, final double below) {
        if (!fineAnswering()) {
            return coarse.topAt(x, z, radius, below);
        }
        final double inBand = banded.topAt(x, z, radius, below);
        return cut == null ? inBand : Math.max(inBand, cut.topAt(x, z, radius, below));
    }

    @Override
    public double undersideAt(final double x, final double z, final double radius, final double above) {
        if (!fineAnswering()) {
            return coarse.undersideAt(x, z, radius, above);
        }
        final double inBand = banded.undersideAt(x, z, radius, above);
        return cut == null ? inBand : Math.min(inBand, cut.undersideAt(x, z, radius, above));
    }

    @Override
    public Vec2 push(final double x, final double z, final double radius, final double feet, final double head) {
        return push(x, z, x, z, radius, feet, head);
    }

    /** Clear of its coarse shape where it is coarse, then of its fine parts where they are fine, from there. */
    @Override
    public Vec2 push(final double ax, final double az, final double bx, final double bz, final double radius,
                     final double feet, final double head) {
        if (!fineAnswering()) {
            return coarse.push(ax, az, bx, bz, radius, feet, head);
        }
        final boolean point = ax == bx && az == bz;
        final Vec2 first = cut == null ? null : point ? cut.push(ax, az, radius, feet, head)
                : cut.push(ax, az, bx, bz, radius, feet, head);
        final double dx = first == null ? 0 : first.x();
        final double dz = first == null ? 0 : first.z();
        final Vec2 then = point ? banded.push(ax + dx, az + dz, radius, feet, head)
                : banded.push(ax + dx, az + dz, bx + dx, bz + dz, radius, feet, head);
        if (first == null) {
            return then;
        }
        return then == null ? first : new Vec2(dx + then.x(), dz + then.z());
    }

    @Override
    public Hit ray(final Vec3 from, final Vec3 direction, final double reach) {
        if (!fineAnswering()) {
            return coarse.ray(from, direction, reach);
        }
        final Hit inBand = banded.ray(from, direction, reach);
        final Hit outside = cut == null ? null : cut.ray(from, direction, inBand == null ? reach : inBand.distance());
        return outside != null ? outside : inBand;
    }

    @Override
    public boolean overlaps(final Block block) {
        if (!fineAnswering()) {
            return coarse.overlaps(block);
        }
        return cut != null && cut.overlaps(block) || banded.overlaps(block);
    }
}
