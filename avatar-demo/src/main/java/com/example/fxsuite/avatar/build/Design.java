package com.example.fxsuite.avatar.build;

import java.util.ArrayList;
import java.util.List;

/**
 * How a surface is dressed: from the surface (its size, its lines of columns, its storeys) and the
 * palette it is built in ({@link Palette}), the solids that make it up, each doing its role, of what
 * the palette makes that of. And what about it is doubtful in that palette ({@link #doubts}): an
 * opening wider than its wall's material spans, say. One design can dress any surface it suits, in
 * any palette; designs are put together ({@link #and}) from smaller ones. A design that lays slabs
 * dresses level surfaces of any shape too ({@link #plate}).
 */
@FunctionalInterface
public interface Design {

    List<Solid> dress(Surface surface, Palette palette);

    /** {@code plate}, a level surface of any shape, dressed: nothing, unless it lays slabs. */
    default List<Solid> plate(final Plate plate, final Palette palette) {
        return List.of();
    }

    /** {@code sheet}, a face of any shape, flat or curved, dressed: nothing, unless it lays slabs. */
    default List<Solid> sheet(final Sheet sheet, final Palette palette) {
        return List.of();
    }

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
            public List<Solid> plate(final Plate plate, final Palette palette) {
                final List<Solid> both = new ArrayList<>(first.plate(plate, palette));
                both.addAll(more.plate(plate, palette));
                return both;
            }

            @Override
            public List<Solid> sheet(final Sheet sheet, final Palette palette) {
                final List<Solid> both = new ArrayList<>(first.sheet(sheet, palette));
                both.addAll(more.sheet(sheet, palette));
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

    /**
     * A plain slab doing {@code role} over all of it, from {@code w0} to {@code w1} out: over a
     * rectangle, one box; over a level surface of any shape, or a face of any shape ({@link Sheet}), a
     * prism on each of its triangles.
     */
    static Design slab(final Role role, final double w0, final double w1) {
        return new Design() {
            @Override
            public List<Solid> dress(final Surface surface, final Palette palette) {
                return List.of(surface.box("slab", palette, role, 0, surface.width(), 0, surface.height(), w0, w1));
            }

            @Override
            public List<Solid> plate(final Plate plate, final Palette palette) {
                return plate.slab("slab", palette, role, w0, w1);
            }

            @Override
            public List<Solid> sheet(final Sheet sheet, final Palette palette) {
                return sheet.slab("slab", palette, role, w0, w1);
            }
        };
    }
}
