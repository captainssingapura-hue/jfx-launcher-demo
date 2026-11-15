package com.example.fxsuite.avatar.build;

import java.util.List;

/**
 * One solid part of a building as dressed: a box, not necessarily square to the building, given by
 * its eight corners: corner {@code k} is at the low or high end of its first edge as bit 0 of k
 * says, of its second as bit 1, of its third as bit 2. Named, so whatever comes after (damage,
 * collision) can tell which it is; what it does ({@code role}), and so what it is made of
 * ({@code material}), as the palette it was dressed in says.
 */
public record Solid(String name, Role role, Material material, List<Point> corners) {

    public Solid {
        corners = List.copyOf(corners);
        if (corners.size() != 8) {
            throw new IllegalArgumentException("a box has eight corners, not " + corners.size());
        }
    }

    /** The same, {@code k} times as big every way, about the building's origin: each corner as far again from it. */
    public Solid scaled(final double k) {
        return new Solid(name, role, material, corners.stream().map(p -> p.times(k)).toList());
    }

    /**
     * {@code solids}, {@code k} times as big every way about the building's origin: a building scaled
     * whole, its doors and windows with it. Nothing more than nothing.
     */
    public static List<Solid> scaled(final List<Solid> solids, final double k) {
        if (!(k > 0) || Double.isInfinite(k)) {
            throw new IllegalArgumentException("scaled by " + k + ": a building is scaled by something more than nothing");
        }
        return k == 1 ? solids : solids.stream().map(s -> s.scaled(k)).toList();
    }

    /** How much room it takes, in cubic centimetres: its three edges' lengths multiplied (it is a box). */
    public double volume() {
        final Point o = corners.get(0);
        return corners.get(1).plus(o.times(-1)).length() * corners.get(2).plus(o.times(-1)).length()
                * corners.get(4).plus(o.times(-1)).length();
    }
}
