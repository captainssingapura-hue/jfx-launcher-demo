package com.example.fxsuite.avatar.build;

import java.util.ArrayList;
import java.util.List;

/**
 * What can be laid over the whole of a wall ({@link Facade#trims}), along its building's lines:
 * bands at its floors, fins at its columns and between, a coping along its top. Each measured out
 * from the wall's plane, so it stands out of the wall's face by as much as it is deeper than half
 * the wall.
 */
public final class Trims {

    private Trims() {
    }

    /** A band along each floor, {@code deep} up from it, standing {@code out} from the wall's plane: its slab's edge, shown. */
    public static Design floorBands(final Material material, final double deep, final double out) {
        return surface -> {
            final List<Solid> bands = new ArrayList<>();
            for (int s = 0; s < surface.storeys(); s++) {
                final double floor = surface.floors()[s];
                bands.add(surface.box("floor band " + s, material, 0, surface.width(), floor - deep / 2, floor + deep / 2,
                        0, out));
            }
            return bands;
        };
    }

    /**
     * Fins, upright, {@code thick} across: one at each line of columns, and {@code between} more
     * evenly between each two; from the ground storey's floor to the parapet, standing {@code out}.
     */
    public static Design fins(final Material material, final int between, final double thick, final double out) {
        return surface -> {
            final List<Solid> fins = new ArrayList<>();
            final double top = surface.height() - surface.parapet();
            int k = 0;
            for (int b = 0; b < surface.bays(); b++) {
                final double u0 = surface.columns()[b];
                final double u1 = surface.columns()[b + 1];
                for (int f = 0; f <= between + (b == surface.bays() - 1 ? 1 : 0); f++) {
                    final double u = u0 + (u1 - u0) * f / (between + 1);
                    final double low = Math.max(0, u - thick / 2);
                    final double high = Math.min(surface.width(), u + thick / 2);
                    fins.add(surface.box("fin " + k++, material, low, high, 0, top, 0, out));
                }
            }
            return fins;
        };
    }

    /** A coping along the top: {@code deep} down from it, reaching {@code out} beyond the wall's plane each way. */
    public static Design coping(final Material material, final double deep, final double out) {
        return surface -> List.of(surface.box("coping", material, 0, surface.width(), surface.height(),
                surface.height() + deep, -out, out));
    }
}
