package com.example.fxsuite.avatar.sim;

/**
 * A door that slides, in a building ({@link Interior}): where its middle is at its foot, in the building's own frame
 * (across, along, up); which of the building's parts slide with it, and how far along each goes, fully open, from
 * where it is shut.
 */
public record Door(double across, double along, double up, int[] parts, double[] travel) {

    public Door {
        if (parts.length != travel.length) {
            throw new IllegalArgumentException(parts.length + " parts sliding but " + travel.length + " ways");
        }
        parts = parts.clone();
        travel = travel.clone();
    }
}
