package com.example.fxsuite.avatar.roads;

/**
 * What is there before a road: how high the surface is at a point, the highest under {@code below} (cm, up). The
 * land as it is, a deck already built, another road, a made-up hill: a road is designed against one, whatever it
 * comes from, and never asks the land itself.
 */
@FunctionalInterface
public interface Profile {

    /** As high as anything is at (x, z), under {@code below}. */
    double height(double x, double z, double below);

    /** As high as anything is at (x, z), however high. */
    default double height(final double x, final double z) {
        return height(x, z, Double.POSITIVE_INFINITY);
    }

    /** Level ground, so high. */
    static Profile level(final double height) {
        return (x, z, below) -> height;
    }
}
