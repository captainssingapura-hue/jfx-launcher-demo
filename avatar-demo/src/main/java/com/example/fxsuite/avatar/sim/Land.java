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
        for (final Layer layer : layers) {
            most = Math.max(most, layer.cover(cover, x, z));
        }
        return most;
    }

    /** The same land, {@code more} laid over it after its own features. */
    public Land with(final Layer more) {
        final List<Layer> all = new java.util.ArrayList<>(layers);
        all.add(more);
        return new Land(base, all);
    }

    /** The same land, {@code more} laid over it in turn after its own features. */
    public Land with(final List<? extends Layer> more) {
        final List<Layer> all = new java.util.ArrayList<>(layers);
        all.addAll(more);
        return new Land(base, all);
    }

    private final Survey base;
    private final List<Layer> layers;

    /** The land surveyed as {@code base} (null for level ground, at 0), {@code layers} laid over it in turn. */
    public Land(final Survey base, final List<Layer> layers) {
        this.base = base;
        this.layers = List.copyOf(layers);
    }

    /** Whether it is level ground, at 0, everywhere. */
    public boolean level() {
        return base == null && layers.isEmpty();
    }

    /** Its lie, as surveyed; null if level under its features. */
    public Survey base() {
        return base;
    }

    public List<Layer> layers() {
        return layers;
    }

    /** The ground's height at ({@code x}, {@code z}). */
    public double height(final double x, final double z) {
        double height = base == null ? 0 : base.height(x, z);
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
