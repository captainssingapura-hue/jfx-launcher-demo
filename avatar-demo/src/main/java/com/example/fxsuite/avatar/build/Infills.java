package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.build.Facade.Cell;
import com.example.fxsuite.avatar.build.Facade.Fill;
import com.example.fxsuite.avatar.build.Facade.Infill;
import com.example.fxsuite.avatar.build.Facade.Rect;

import java.util.ArrayList;
import java.util.List;

/**
 * Ways to fill a cell of a wall ({@link Facade}): nothing, a window, a ribbon of windows, glazing
 * from floor to ceiling, a balcony and its door, deep punched windows. Each in centimetres from the
 * cell's sides and its floor; its glass set into the wall, as far from its face as each says.
 */
public final class Infills {

    /** Glass, its mullions this far apart at most; as thick as it is. */
    private static final double MULLIONS = 150;
    private static final double PANE = 2;
    private static final double MULLION = 6;

    private Infills() {
    }

    /** Solid wall: no opening at all. */
    public static Infill blank() {
        return (surface, cell, thickness) -> Fill.NOTHING;
    }

    /**
     * Glass in {@code opening}, at {@code w} out from the wall's plane, mullions across it no
     * further apart than {@code MULLIONS}, and a transom across it at {@code transom} up it (none if 0).
     */
    static List<Solid> glazed(final Surface surface, final Cell cell, final String name, final Rect opening,
                              final double w, final double transom) {
        final List<Solid> solids = new ArrayList<>();
        solids.add(surface.box(cell.name() + ", " + name + ", glass", Material.GLASS, opening.u0(), opening.u1(),
                opening.v0(), opening.v1(), w - PANE / 2, w + PANE / 2));
        final int panes = Math.max(1, (int) Math.ceil((opening.u1() - opening.u0()) / MULLIONS));
        final double pane = (opening.u1() - opening.u0()) / panes;
        for (int k = 1; k < panes; k++) {
            final double u = opening.u0() + k * pane;
            solids.add(surface.box(cell.name() + ", " + name + ", mullion " + k, Material.METAL, u - MULLION / 2,
                    u + MULLION / 2, opening.v0(), opening.v1(), w - MULLION / 2, w + MULLION / 2));
        }
        if (transom > 0) {
            solids.add(surface.box(cell.name() + ", " + name + ", transom", Material.METAL, opening.u0(), opening.u1(),
                    opening.v0() + transom - MULLION / 2, opening.v0() + transom + MULLION / 2, w - MULLION / 2,
                    w + MULLION / 2));
        }
        return solids;
    }

    /** A window, {@code side} in from the cell's sides, from {@code sill} to {@code head} up it; its glass set back to the wall's middle. */
    public static Infill window(final double side, final double sill, final double head) {
        return (surface, cell, thickness) -> {
            final Rect opening = new Rect(cell.u0() + side, cell.u1() - side, cell.v0() + sill, cell.v0() + head);
            return new Fill(List.of(opening), glazed(surface, cell, "window", opening, 0, 0));
        };
    }

    /**
     * A ribbon window: from {@code sill} to {@code head}, the whole bay but for a pier
     * {@code pier} wide at each line of columns; so, bay after bay, a band of glass along the storey.
     */
    public static Infill ribbon(final double sill, final double head, final double pier) {
        return (surface, cell, thickness) -> {
            final Rect opening = new Rect(cell.u0() + pier / 2, cell.u1() - pier / 2, cell.v0() + sill,
                    cell.v0() + head);
            return new Fill(List.of(opening), glazed(surface, cell, "ribbon", opening, thickness / 2 - 8, 0));
        };
    }

    /**
     * Glazing from {@code bottom} up the storey (the slab's edge below it) to its top, the whole
     * bay; set back {@code recess} behind the wall's face, a transom at door height.
     */
    public static Infill glazing(final double bottom, final double recess) {
        return (surface, cell, thickness) -> {
            final Rect opening = new Rect(cell.u0(), cell.u1(), cell.v0() + bottom, cell.v1());
            return new Fill(List.of(opening), glazed(surface, cell, "glazing", opening, thickness / 2 - recess, 230));
        };
    }

    /**
     * A shopfront: glass the bay's width but for a pier {@code pier} wide at each line of columns,
     * on a riser {@code riser} high, up to a fascia {@code fascia} deep under the storey's top; a
     * transom across it at door height.
     */
    public static Infill shopfront(final double riser, final double fascia, final double pier) {
        return (surface, cell, thickness) -> {
            final Rect opening = new Rect(cell.u0() + pier / 2, cell.u1() - pier / 2, cell.v0() + riser,
                    cell.v1() - fascia);
            return new Fill(List.of(opening), glazed(surface, cell, "shopfront", opening, thickness / 2 - 10, 230 - riser));
        };
    }

    /**
     * An entrance: glazed doors {@code width} wide and {@code height} high in the middle of the bay,
     * from its floor, a transom over them; and over it all a canopy of {@code material}, standing
     * {@code canopy} out of the wall.
     */
    public static Infill entrance(final double width, final double height, final double canopy,
                                  final Material material) {
        return (surface, cell, thickness) -> {
            final double middle = (cell.u0() + cell.u1()) / 2;
            final Rect opening = new Rect(middle - width / 2, middle + width / 2, cell.v0(), cell.v0() + height);
            final List<Solid> solids = new ArrayList<>(glazed(surface, cell, "entrance", opening, 0, 220));
            solids.add(surface.box(cell.name() + ", entrance, canopy", material, opening.u0() - 40, opening.u1() + 40,
                    opening.v1() + 10, opening.v1() + 30, thickness / 2, thickness / 2 + canopy));
            return new Fill(List.of(opening), solids);
        };
    }

    /**
     * A balcony, {@code depth} deep out of the wall, {@code side} in from the cell's sides: a slab at
     * the floor, a solid front a metre high and solid sides, of {@code material}; and behind it, a
     * glazed door {@code door} wide.
     */
    public static Infill balcony(final double depth, final double side, final double door, final Material material) {
        return (surface, cell, thickness) -> {
            final double face = thickness / 2;
            final double u0 = cell.u0() + side;
            final double u1 = cell.u1() - side;
            final double floor = cell.v0();
            final double rail = floor + 110;
            final double wall = 15;
            final double middle = (cell.u0() + cell.u1()) / 2;
            final Rect opening = new Rect(middle - door / 2, middle + door / 2, floor + 15, floor + 235);
            final List<Solid> solids = new ArrayList<>(glazed(surface, cell, "balcony door", opening, 0, 0));
            final String name = "balcony";
            solids.add(surface.box(cell.name() + ", " + name + ", slab", material, u0, u1, floor - 10, floor + 15, face,
                    face + depth));
            solids.add(surface.box(cell.name() + ", " + name + ", front", material, u0, u1, floor + 15, rail,
                    face + depth - wall, face + depth));
            solids.add(surface.box(cell.name() + ", " + name + ", left side", material, u0, u0 + wall, floor + 15, rail,
                    face, face + depth - wall));
            solids.add(surface.box(cell.name() + ", " + name + ", right side", material, u1 - wall, u1, floor + 15,
                    rail, face, face + depth - wall));
            return new Fill(List.of(opening), solids);
        };
    }

    /**
     * Deep punched windows: {@code count} of them across the bay, each {@code size} square, its sill
     * a third of the way up the storey, the glass far back in the wall and a surround of
     * {@code material} standing {@code proud} out of its face round each.
     */
    public static Infill punched(final int count, final double size, final double proud, final Material material) {
        return (surface, cell, thickness) -> {
            final List<Rect> openings = new ArrayList<>();
            final List<Solid> solids = new ArrayList<>();
            final double face = thickness / 2;
            final double frame = 12;
            final double sill = cell.v0() + cell.height() / 3;
            for (int k = 0; k < count; k++) {
                final double middle = cell.u0() + cell.width() * (k + 0.5) / count;
                final Rect opening = new Rect(middle - size / 2, middle + size / 2, sill, sill + size);
                openings.add(opening);
                final String name = "window " + k;
                solids.addAll(glazed(surface, cell, name, opening, -face + 6, 0));
                // its surround: a frame round it, standing out of the wall
                final double u0 = opening.u0() - frame;
                final double u1 = opening.u1() + frame;
                final double v0 = opening.v0() - frame;
                final double v1 = opening.v1() + frame;
                solids.add(surface.box(cell.name() + ", " + name + ", surround top", material, u0, u1, opening.v1(), v1,
                        face, face + proud));
                solids.add(surface.box(cell.name() + ", " + name + ", surround bottom", material, u0, u1, v0,
                        opening.v0(), face, face + proud));
                solids.add(surface.box(cell.name() + ", " + name + ", surround left", material, u0, opening.u0(),
                        opening.v0(), opening.v1(), face, face + proud));
                solids.add(surface.box(cell.name() + ", " + name + ", surround right", material, opening.u1(), u1,
                        opening.v0(), opening.v1(), face, face + proud));
            }
            return new Fill(openings, solids);
        };
    }
}
