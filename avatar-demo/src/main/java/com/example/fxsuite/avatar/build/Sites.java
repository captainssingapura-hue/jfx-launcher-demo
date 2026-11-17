package com.example.fxsuite.avatar.build;

import java.util.List;

/**
 * What each building in the world is drawn as, by its key ({@code Course.Piece.tint}): the town's
 * buildings from 0, the temple's halls from {@link TempleYard#KEYS}, its wall's units from
 * {@link TempleYard#WALL_KEYS}, the pyramids from {@link Pyramids#KEYS}, the pavilions from
 * {@link Pavilions#KEYS}, the village's from {@link Village#KEYS}, the wall tower from {@link TowerMap#KEYS}.
 */
public final class Sites {

    private Sites() {
    }

    /**
     * Building {@code key}'s door {@code door}'s leaves that slide, {@code open} as far open, about the middle of its
     * box as its solids are: drawn apart from them, as the door is opened and shut. None for a building without doors.
     */
    public static List<Solid> leaves(final int key, final int door, final double open) {
        return key >= TowerMap.KEYS ? TowerMap.leaves(door, open) : List.of();
    }

    /** Building {@code key}'s solids, about the middle of its box. */
    public static List<Solid> drawn(final int key) {
        if (key >= TowerMap.KEYS) {
            return TowerMap.solids(key - TowerMap.KEYS);
        }
        if (key >= Village.KEYS) {
            return Village.solids(key - Village.KEYS);
        }
        if (key >= Pavilions.KEYS) {
            return Pavilions.solids(key - Pavilions.KEYS);
        }
        return key >= Pyramids.KEYS ? Pyramids.solids(key - Pyramids.KEYS) : TempleYard.drawn(key);
    }
}
