package com.example.fxsuite.viewer;

import com.example.fxsuite.deployment.Version;
import com.example.fxsuite.viewer.EstateLayout.Placed;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * A walk through what a release runs with, one slate at a time: the release itself first,
 * then back through the layers to the base, left to right within a layer. The release is
 * always first, being one layer in front of everything it depends on. Pure: no JavaFX.
 */
public record Tour(List<Version<?>> stops) {

    public Tour {
        stops = List.copyOf(stops);
    }

    /** A tour of the given releases, as placed in the layout; those not placed are left out. */
    public static Tour of(final EstateLayout layout, final Collection<? extends Version<?>> releases) {
        return new Tour(releases.stream()
                .map(layout::of).flatMap(java.util.Optional::stream)
                .sorted(Comparator.comparingInt(Placed::depth).reversed()
                        .thenComparingDouble(Placed::lane)
                        .thenComparingInt(Placed::row))
                .<Version<?>>map(Placed::version).toList());
    }

    /**
     * The stop {@code by} steps on from {@code from}, going round from the last to the first
     * and back; from a release not on the tour, counted from the first stop.
     */
    public Version<?> step(final Version<?> from, final int by) {
        final int at = Math.max(0, stops.indexOf(from));
        return stops.get(Math.floorMod(at + by, stops.size()));
    }

    /** Which stop this is, counting from 1, or 0 if it is not on the tour. */
    public int number(final Version<?> stop) {
        return stops.indexOf(stop) + 1;
    }

    public int size() {
        return stops.size();
    }
}
