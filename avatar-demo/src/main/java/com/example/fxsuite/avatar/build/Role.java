package com.example.fxsuite.avatar.build;

/**
 * What a part of a building does, whatever it is made of: a design says what each part's role is,
 * and a {@link Palette} what each role is made of; so one design is built in brick or concrete as
 * its palette says.
 */
public enum Role {
    /** The walls above the ground storey. */
    WALL,
    /** The ground storey's walls: its base, or plinth. */
    BASE,
    /** What dresses the walls: surrounds, sills, bands, copings, fins, balconies, canopies. */
    DRESSING,
    /** The structure that shows: columns, slabs, the undersides of floors. */
    STRUCTURE,
    /** A roof's covering: a flat roof's gravel, a pitched roof's slates or tiles. */
    ROOF,
    /** Glass, in windows, doors and shopfronts. */
    GLAZING,
    /** Window frames, mullions, transoms; doors and their lattices. */
    FRAME,
    /** A spire's covering, and what crowns it. */
    SPIRE
}
