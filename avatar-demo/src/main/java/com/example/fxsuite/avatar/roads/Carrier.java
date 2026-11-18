package com.example.fxsuite.avatar.roads;

/**
 * What holds a stretch of a road's formation true: the one requirement each meets in its own way, none knowing
 * how the others do.
 */
public enum Carrier {
    /** The ground, cut and filled to it, its side slopes out to the ground as it was. */
    GROUND,
    /** A structure built for it: a bridge, a viaduct. The land under it left as it is. */
    STRUCTURE,
    /** A bore through what is above it, its floor the formation. The land above left as it is. */
    TUNNEL,
    /** A structure already there, its top the profile: nothing built, nothing shaped. */
    ON
}
