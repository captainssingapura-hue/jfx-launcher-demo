package com.example.fxsuite.avatar.sim;

import java.util.List;

/**
 * Still water: a pond, its top {@code level} high, filling the land wherever that is lower within
 * {@code outline}, a loop round it where the land is higher than its top all the way round (as a
 * contour above it is). Where water lies, for what is drawn of it and, in time, for what wades in it.
 */
public record Pond(List<Vec2> outline, double level) {

    public Pond {
        outline = List.copyOf(outline);
        if (outline.size() < 3) {
            throw new IllegalArgumentException("a pond lies within a loop");
        }
    }

    /** Whether ({@code x}, {@code z}) is in its water, on {@code land}: within its loop, and the land under its top. */
    public boolean wet(final Land land, final double x, final double z) {
        return Outline.inside(outline, x, z) && land.height(x, z) < level;
    }
}
