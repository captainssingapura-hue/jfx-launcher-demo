package com.example.fxsuite.avatar.sim;

import java.util.List;

/**
 * The ground, as it rises and falls: the lie of the land, surveyed from its map ({@link Survey}), and laid
 * over it in turn its features ({@link Layer}), each worked out on its own and exactly, however fine: a
 * river's channel cut into it ({@link Channel}), and, in time, paths, ditches, the edges of level pads.
 * What everything that goes over the ground stands on ({@link Obstacles#footing}), and what a ray meets
 * low down; and what is drawn. Level, at 0, where there is none ({@link #LEVEL}). Pure, and the same every time.
 */
public final class Land {

    /** Level ground, at 0, everywhere. */
    public static final Land LEVEL = new Land(null, List.of());

    /**
     * A feature of the land, laid over what is under it: so it is worked out apart from the rest, and as
     * finely as it needs to be, wherever it is asked.
     */
    public interface Layer {

        /** The ground's height at ({@code x}, {@code z}), it laid over ground {@code under} high there. */
        double shape(double x, double z, double under);

        /** Whether it changes anything over the ground from ({@code minX}, {@code minZ}) to ({@code maxX}, {@code maxZ}). */
        boolean touches(double minX, double minZ, double maxX, double maxZ);

        /** How far below what is under it it takes the ground, at most. */
        double deepest();

        /** How far above what is under it it takes the ground, at most: none, but for what raises it. */
        default double tallest() {
            return 0;
        }

        /**
         * How much of {@code cover} it lays on the ground at ({@code x}, {@code z}): 1 all of it, 0 none, between,
         * thinning out into the grass. None does, but for what does.
         */
        default double cover(final Cover cover, final double x, final double z) {
            return 0;
        }
    }

    /**
     * What covers the ground, where a feature lays something on it rather than grass: bare earth (a channel's bed
     * and banks), gravel (a country road's top), asphalt (a road's).
     */
    public enum Cover { EARTH, GRAVEL, ASPHALT }

    /** How much of {@code cover} there is on the ground at ({@code x}, {@code z}), by whichever of its features lays most. */
    public double cover(final Cover cover, final double x, final double z) {
        double most = 0;
        final int[] near = index == null ? null : index.at(x, z);
        if (near != null) {
            for (final int k : near) {
                most = Math.max(most, layers.get(k).cover(cover, x, z));
            }
            return most;
        }
        for (final Layer layer : layers) {
            most = Math.max(most, layer.cover(cover, x, z));
        }
        return most;
    }

    /** The same land, {@code more} laid over it after its own features. */
    public Land with(final Layer more) {
        return with(List.of(more));
    }

    /** The same land, {@code more} laid over it in turn after its own features. */
    public Land with(final List<? extends Layer> more) {
        final List<Layer> all = new java.util.ArrayList<>(layers);
        all.addAll(more);
        if (index == null) {
            return new Land(base, all);
        }
        // its index as it was, each one more filed in the cells it touches
        Index grown = index;
        for (int k = layers.size(); k < all.size(); k++) {
            grown = grown.plus(k, all.get(k));
        }
        return new Land(base, List.copyOf(all), grown);
    }

    private final Survey base;
    private final List<Layer> layers;
    /** Which of its features touch each cell of its survey's ground, in order: null, unsurveyed. */
    private final Index index;

    /** The land surveyed as {@code base} (null for level ground, at 0), {@code layers} laid over it in turn. */
    public Land(final Survey base, final List<Layer> layers) {
        this(base, List.copyOf(layers), base == null ? null : Index.of(base, layers));
    }

    private Land(final Survey base, final List<Layer> layers, final Index index) {
        this.base = base;
        this.layers = layers;
        this.index = index;
    }

    /**
     * Which of its features touch each square of its ground, {@link #CELL} across, by their places among them, in
     * order: so what is asked of the land at a point is asked only of the features that may change it there, however
     * many it has (a city's roads). Its cells' lists shared from one land to the next laid over it, each changed only
     * where the one laid over touches.
     */
    private record Index(double x0, double z0, int across, int along, int[][] cells) {

        static final double CELL = 10_000;
        private static final int[] NONE = new int[0];

        static Index of(final Survey base, final List<Layer> layers) {
            final double x0 = base.x(0);
            final double z0 = base.z(0);
            final int across = Math.max(1, (int) Math.ceil((base.x(base.across() - 1) - x0) / CELL));
            final int along = Math.max(1, (int) Math.ceil((base.z(base.along() - 1) - z0) / CELL));
            final int[][] cells = new int[across * along][];
            java.util.Arrays.fill(cells, NONE);
            Index index = new Index(x0, z0, across, along, cells);
            for (int k = 0; k < layers.size(); k++) {
                index = index.plus(k, layers.get(k));
            }
            return index;
        }

        /** The same, feature {@code k}, {@code layer}, filed in each cell it touches, after those there. */
        Index plus(final int k, final Layer layer) {
            final int[][] grown = cells.clone();
            for (int j = 0; j < along; j++) {
                for (int i = 0; i < across; i++) {
                    if (layer.touches(x0 + i * CELL, z0 + j * CELL, x0 + (i + 1) * CELL, z0 + (j + 1) * CELL)) {
                        final int[] was = grown[j * across + i];
                        final int[] now = java.util.Arrays.copyOf(was, was.length + 1);
                        now[was.length] = k;
                        grown[j * across + i] = now;
                    }
                }
            }
            return new Index(x0, z0, across, along, grown);
        }

        /** The features that may change the ground at ({@code x}, {@code z}), in order; null off its ground. */
        int[] at(final double x, final double z) {
            final double fi = (x - x0) / CELL;
            final double fj = (z - z0) / CELL;
            if (fi < 0 || fj < 0 || fi >= across || fj >= along) {
                return null;
            }
            return cells[(int) fj * across + (int) fi];
        }
    }

    /** Whether it is level ground, at 0, everywhere. */
    public boolean level() {
        return base == null && layers.isEmpty();
    }

    /** Its lie, as surveyed; null if level under its features. */
    public Survey base() {
        return base;
    }

    /**
     * {@code x} held within the world's edges west and east: its survey's, if it is surveyed; else
     * {@link Body#EDGE} either way.
     */
    public double insideX(final double x) {
        return base == null ? Num.clamp(x, -Body.EDGE, Body.EDGE) : Num.clamp(x, base.x(0), base.x(base.across() - 1));
    }

    /** {@code z} held within the world's edges south and north, as {@link #insideX} is. */
    public double insideZ(final double z) {
        return base == null ? Num.clamp(z, -Body.EDGE, Body.EDGE) : Num.clamp(z, base.z(0), base.z(base.along() - 1));
    }

    public List<Layer> layers() {
        return layers;
    }

    /** The ground's height at ({@code x}, {@code z}). */
    public double height(final double x, final double z) {
        double height = base == null ? 0 : base.height(x, z);
        final int[] near = index == null ? null : index.at(x, z);
        if (near != null) {
            for (final int k : near) {
                height = layers.get(k).shape(x, z, height);
            }
            return height;
        }
        for (final Layer layer : layers) {
            height = layer.shape(x, z, height);
        }
        return height;
    }

    /** Which way and how fast the ground rises at ({@code x}, {@code z}), rise over run, across and along, over {@code apart} each way. */
    public Vec2 rise(final double x, final double z, final double apart) {
        return new Vec2((height(x + apart, z) - height(x - apart, z)) / (2 * apart),
                (height(x, z + apart) - height(x, z - apart)) / (2 * apart));
    }

    /** Whether any of its features changes anything over the ground from ({@code minX}, {@code minZ}) to ({@code maxX}, {@code maxZ}). */
    public boolean featured(final double minX, final double minZ, final double maxX, final double maxZ) {
        for (final Layer layer : layers) {
            if (layer.touches(minX, minZ, maxX, maxZ)) {
                return true;
            }
        }
        return false;
    }

    /** As high as it goes, at most. */
    public double highest() {
        double tallest = 0;
        for (final Layer layer : layers) {
            tallest += layer.tallest();
        }
        return (base == null ? 0 : base.highest()) + tallest;
    }

    /** As low as it goes, at least. */
    public double lowest() {
        double deepest = 0;
        for (final Layer layer : layers) {
            deepest += layer.deepest();
        }
        return (base == null ? 0 : base.lowest()) - deepest;
    }
}
