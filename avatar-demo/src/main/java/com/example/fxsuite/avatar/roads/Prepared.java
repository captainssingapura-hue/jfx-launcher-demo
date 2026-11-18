package com.example.fxsuite.avatar.roads;

/**
 * The ground at a point as a road prepares it: so high ({@code height}, cm, up), and covered so; its {@code cover}
 * null where no road has touched it, as it was.
 */
public record Prepared(double height, Cover cover) {

    /** Whether a road has prepared it. */
    public boolean touched() {
        return cover != null;
    }
}
