package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.sim.Land;

import org.junit.jupiter.api.Test;

import javafx.scene.paint.Color;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The land drawn: each tile as fine as it is near, coming back finer a little nearer than it went
 * coarser; and its colour, the base lit and its
 * picture together, its tint near, the far ground's fading as it hazes, the haze itself at the last.
 */
class TerrainModelTest {

    private static final double[] LIT = Horizon.lighting(World.AMBIENT, World.SUNLIGHT, World.SUN);

    @Test
    void eachTileAsFineAsItIsNearComingBackALittleNearer() {
        assertEquals(0, TerrainModel.level(0, -1));
        assertEquals(1, TerrainModel.level(150_000, -1));
        assertEquals(TerrainModel.LEVELS - 1, TerrainModel.level(900_000, -1));
        // gone coarser past 1 km, it stays so till well inside it
        assertEquals(1, TerrainModel.level(95_000, 1));
        assertEquals(0, TerrainModel.level(85_000, 1));
        assertEquals(1, TerrainModel.level(105_000, 0));
    }

    @Test
    void itsColourItsTintNearTheHazeAtTheLast() {
        for (int c = 0; c < 3; c++) {
            final double base = channel(TerrainModel.BASE, c);
            for (double t = 0; t <= 1; t += 0.1) {
                final double tint = channel(TerrainModel.tint(t), c);
                // near, as level ground is lit: its tint
                assertEquals(tint * LIT[c], base * LIT[c] + TerrainModel.glow(tint, base, 0, LIT[c], 0), 1e-9);
                // all hazed: the haze
                final double hazy = channel(Horizon.HAZY, c);
                assertEquals(hazy, base * LIT[c] + TerrainModel.glow(tint, base, hazy, LIT[c], 1), 1e-9);
                for (double haze = 0; haze <= 1; haze += 0.1) {
                    assertTrue(TerrainModel.glow(tint, base, hazy, LIT[c], haze) > 0, "its base no more than its picture asks");
                }
            }
        }
        // low down, the meadow's grass: as the far ground is
        assertEquals(World.GRASS, TerrainModel.tint(0));
        final int covers = Land.Cover.values().length;
        assertEquals(World.GRASS, TerrainModel.colour((int) TerrainModel.column(new double[covers], 0)));
        // and each cover, all of it: its own colour, no less than the base in any channel either
        final Color[] colours = {TerrainModel.EARTH, TerrainModel.GRAVEL, TerrainModel.ASPHALT};
        for (final Land.Cover cover : Land.Cover.values()) {
            final double[] all = new double[covers];
            all[cover.ordinal()] = 1;
            assertEquals(colours[cover.ordinal()], TerrainModel.colour((int) TerrainModel.column(all, 0.7)), cover.name());
            for (int c = 0; c < 3; c++) {
                assertTrue(channel(colours[cover.ordinal()], c) >= channel(TerrainModel.BASE, c));
            }
        }
        // a road over bare earth: the road's
        final double[] both = new double[covers];
        both[Land.Cover.EARTH.ordinal()] = 1;
        both[Land.Cover.ASPHALT.ordinal()] = 1;
        assertEquals(TerrainModel.ASPHALT, TerrainModel.colour((int) TerrainModel.column(both, 0.7)));
    }

    private static double channel(final Color colour, final int c) {
        return c == 0 ? colour.getRed() : c == 1 ? colour.getGreen() : colour.getBlue();
    }
}
