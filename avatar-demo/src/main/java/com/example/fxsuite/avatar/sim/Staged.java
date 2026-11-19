package com.example.fxsuite.avatar.sim;

import java.util.List;

/**
 * A building as finely as what is happening at it needs ({@code staged-fineness}): its coarse shape, its outline stood
 * up a stretch of storeys at a time, nothing inside it; its fine model, every part of it, some breaking on their own
 * ({@link Assembly}); and which it is at ({@link Stage}), as the world's focus has it ({@link Play}). To the world one
 * volume, whose bounds are both's and never change, so the world's index of what is in the way stays as it is however
 * its stage changes.
 *
 * <p>It answers from its coarse shape unless it is in focus and fine physics is on ({@link #FINE}); which it is not yet:
 * its stage is worked out and kept, its fine model drawn, its coarse shape in the way.
 *
 * @param coarse its coarse shape, its parts' answers together
 * @param fine   its fine model
 * @param stage  how finely the focus wants it
 */
public record Staged(Assembly coarse, Assembly fine, Stage stage) implements Volume {

    /** Whether a building in focus is in the way part by part, as its fine model is. Not yet. */
    public static final boolean FINE = false;

    /** How finely a building is wanted: far from the focus, near it, met by it. */
    public enum Stage { FAR, NEAR, FOCUS }

    /** Its coarse shape's volumes and its fine model, far off. */
    public Staged(final List<Volume> coarse, final Assembly fine) {
        this(new Assembly(coarse, List.of(), 0, 0), fine, Stage.FAR);
    }

    /** The same, at {@code now}: itself, if it is there already. */
    public Staged at(final Stage now) {
        return now == stage ? this : new Staged(coarse, fine, now);
    }

    /** What answers for it: its fine model, in focus with fine physics on; else its coarse shape. */
    public Assembly answering() {
        return FINE && stage == Stage.FOCUS ? fine : coarse;
    }

    /** Both put where its building stands, {@code scale}d, at its stage. */
    public Staged placed(final Place at, final double scale) {
        return new Staged(coarse.placed(at, scale), fine.placed(at, scale), stage);
    }

    /** Its bounds: both its shapes', so they never change with its stage. */
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
        return answering().covers(x, z, radius);
    }

    @Override
    public boolean steep(final double x, final double z, final double radius, final double top) {
        return answering().steep(x, z, radius, top);
    }

    @Override
    public double topAt(final double x, final double z, final double radius, final double below) {
        return answering().topAt(x, z, radius, below);
    }

    @Override
    public double undersideAt(final double x, final double z, final double radius, final double above) {
        return answering().undersideAt(x, z, radius, above);
    }

    @Override
    public Vec2 push(final double x, final double z, final double radius, final double feet, final double head) {
        return answering().push(x, z, radius, feet, head);
    }

    @Override
    public Vec2 push(final double ax, final double az, final double bx, final double bz, final double radius,
                     final double feet, final double head) {
        return answering().push(ax, az, bx, bz, radius, feet, head);
    }

    @Override
    public Hit ray(final Vec3 from, final Vec3 direction, final double reach) {
        return answering().ray(from, direction, reach);
    }

    @Override
    public boolean overlaps(final Block block) {
        return answering().overlaps(block);
    }
}
