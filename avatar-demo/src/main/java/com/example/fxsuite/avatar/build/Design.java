package com.example.fxsuite.avatar.build;

import java.util.ArrayList;
import java.util.List;

/**
 * How a surface is dressed: from the surface (its size, its lines of columns, its storeys) and the
 * palette it is built in ({@link Palette}), the solids that make it up, each doing its role, of what
 * the palette makes that of. And what about it is doubtful in that palette ({@link #doubts}): an
 * opening wider than its wall's material spans, say. One design can dress any surface it suits, in
 * any palette; designs are put together ({@link #and}) from smaller ones.
 */
@FunctionalInterface
public interface Design {

    List<Solid> dress(Surface surface, Palette palette);

    /** What is doubtful about it on {@code surface}, built in {@code palette}: nothing, unless it says. */
    default List<String> doubts(final Surface surface, final Palette palette) {
        return List.of();
    }

    /** This, and {@code more} on the same surface. */
    default Design and(final Design more) {
        final Design first = this;
        return new Design() {
            @Override
            public List<Solid> dress(final Surface surface, final Palette palette) {
                final List<Solid> both = new ArrayList<>(first.dress(surface, palette));
                both.addAll(more.dress(surface, palette));
                return both;
            }

            @Override
            public List<String> doubts(final Surface surface, final Palette palette) {
                final List<String> both = new ArrayList<>(first.doubts(surface, palette));
                both.addAll(more.doubts(surface, palette));
                return both;
            }
        };
    }

    /** Nothing: a surface left open. */
    static Design none() {
        return (surface, palette) -> List.of();
    }

    /** A plain slab doing {@code role} over all of it, from {@code w0} to {@code w1} out. */
    static Design slab(final Role role, final double w0, final double w1) {
        return (surface, palette) -> List.of(surface.box("slab", palette, role, 0, surface.width(), 0, surface.height(),
                w0, w1));
    }
}
