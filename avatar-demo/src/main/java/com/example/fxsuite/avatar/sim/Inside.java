package com.example.fxsuite.avatar.sim;

/**
 * An object's own level of the index: its inside, cut into cells, in its own frame (across, along, up; see
 * {@link Box}). Asked only through the world's level ({@link Places}, {@link Placed}): where a point falls, and which
 * cells a box reaches. A building whose structure is regular answers by arithmetic from its description; anything
 * else, by a plain grid ({@link GridInside}).
 */
public interface Inside {

    /** All of it, in its own frame: whatever is outside this is in none of its cells. */
    Box bounds();

    /** How many cells it has: they are numbered from 0 to one less. */
    int count();

    /** The cell a point falls in, or -1 where it is in none: outside. */
    int cell(double across, double along, double up);

    /** The cells a box reaches, each once, in order. */
    int[] cells(Box box);

    /** All a cell may take up: its box. */
    Box box(int cell);
}
