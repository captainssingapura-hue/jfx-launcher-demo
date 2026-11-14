package com.example.fxsuite.avatar.build;

import java.util.List;

/**
 * One solid part of a building as dressed: a box, not necessarily square to the building, given by
 * its eight corners: corner {@code k} is at the low or high end of its first edge as bit 0 of k
 * says, of its second as bit 1, of its third as bit 2. Named, so whatever comes after (damage,
 * collision) can tell which it is; made of {@code material}.
 */
public record Solid(String name, Material material, List<Point> corners) {

    public Solid {
        corners = List.copyOf(corners);
        if (corners.size() != 8) {
            throw new IllegalArgumentException("a box has eight corners, not " + corners.size());
        }
    }

    /** How much room it takes, in cubic centimetres: its three edges' lengths multiplied (it is a box). */
    public double volume() {
        final Point o = corners.get(0);
        return corners.get(1).plus(o.times(-1)).length() * corners.get(2).plus(o.times(-1)).length()
                * corners.get(4).plus(o.times(-1)).length();
    }
}
