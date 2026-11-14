package com.example.fxsuite.avatar.build;

/**
 * What a building's parts are made of, and the one thing about each that a design must heed: how
 * wide an opening a wall of it spans with nothing to carry what is over it (concrete, as wide as
 * may be; brick, three metres, on its lintels; stone less). How each is drawn is the graphics' to
 * say.
 */
public enum Material {
    /** Concrete cast where it stands, its boards' marks on it. */
    CONCRETE("board-marked concrete", Double.POSITIVE_INFINITY),
    /** Concrete cast elsewhere, smoother and paler. */
    PRECAST("precast concrete", Double.POSITIVE_INFINITY),
    BRICK("brick", 300),
    /** Dressed stone, laid in courses. */
    STONE("stone", 250),
    GLASS("glass", Double.POSITIVE_INFINITY),
    /** Window frames, mullions, rails. */
    METAL("dark metal", Double.POSITIVE_INFINITY),
    /** A flat roof's covering. */
    GRAVEL("gravel", Double.POSITIVE_INFINITY);

    private final String title;
    private final double span;

    Material(final String title, final double span) {
        this.title = title;
        this.span = span;
    }

    public String title() {
        return title;
    }

    /** How wide an opening, in centimetres, a wall of it spans with nothing to carry it. */
    public double span() {
        return span;
    }
}
