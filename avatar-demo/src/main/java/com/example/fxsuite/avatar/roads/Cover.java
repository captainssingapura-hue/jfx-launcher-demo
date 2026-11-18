package com.example.fxsuite.avatar.roads;

/**
 * What the ground's surface is where a road has prepared it: a ground strip's (a verge's grass), a side slope's,
 * a ditch's. Not built, as a pavement's courses are: the ground itself, graded, topsoiled and seeded, or left bare.
 */
public enum Cover {
    /** Topsoiled and seeded: verges, medians, side slopes. */
    GRASS,
    /** Bare: a ditch, freshly dug ground, the formation before it is paved. */
    EARTH,
    /** Loose stone, spread on the ground. */
    GRAVEL;

    /** As it is written: lower case. */
    public String written() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
