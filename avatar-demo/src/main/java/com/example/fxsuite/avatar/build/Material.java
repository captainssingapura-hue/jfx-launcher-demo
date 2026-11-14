package com.example.fxsuite.avatar.build;

/** What a building's parts are made of: how they are drawn is the graphics' to say. */
public enum Material {
    /** Concrete cast where it stands, its boards' marks on it. */
    CONCRETE("board-marked concrete"),
    /** Concrete cast elsewhere, smoother and paler. */
    PRECAST("precast concrete"),
    GLASS("glass"),
    /** Window frames, mullions, rails. */
    METAL("dark metal"),
    /** A flat roof's covering. */
    GRAVEL("gravel");

    private final String title;

    Material(final String title) {
        this.title = title;
    }

    public String title() {
        return title;
    }
}
