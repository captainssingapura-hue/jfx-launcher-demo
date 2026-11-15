package com.example.fxsuite.avatar.build;

import java.util.List;

/**
 * What each building in the world is drawn as, by its key ({@code Course.Piece.tint}): the town's
 * buildings from 0, the temple's halls from {@link TempleYard#KEYS}, its wall's units from
 * {@link TempleYard#WALL_KEYS}, the pyramids from {@link Pyramids#KEYS}.
 */
public final class Sites {

    private Sites() {
    }

    /** Building {@code key}'s solids, about the middle of its box. */
    public static List<Solid> drawn(final int key) {
        return key >= Pyramids.KEYS ? Pyramids.solids(key - Pyramids.KEYS) : TempleYard.drawn(key);
    }
}
