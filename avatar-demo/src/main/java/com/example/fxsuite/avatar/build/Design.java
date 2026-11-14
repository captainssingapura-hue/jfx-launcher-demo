package com.example.fxsuite.avatar.build;

import java.util.ArrayList;
import java.util.List;

/**
 * How a surface is dressed: from the surface (its size, its lines of columns, its storeys), the
 * solids that make it up. One design can dress any surface it suits; designs are put together
 * ({@link #and}) from smaller ones.
 */
@FunctionalInterface
public interface Design {

    List<Solid> dress(Surface surface);

    /** This, and {@code more} on the same surface. */
    default Design and(final Design more) {
        return surface -> {
            final List<Solid> both = new ArrayList<>(dress(surface));
            both.addAll(more.dress(surface));
            return both;
        };
    }

    /** Nothing: a surface left open. */
    static Design none() {
        return surface -> List.of();
    }

    /** A plain slab of {@code material} over all of it, from {@code w0} to {@code w1} out. */
    static Design slab(final Material material, final double w0, final double w1) {
        return surface -> List.of(surface.box("slab", material, 0, surface.width(), 0, surface.height(), w0, w1));
    }
}
