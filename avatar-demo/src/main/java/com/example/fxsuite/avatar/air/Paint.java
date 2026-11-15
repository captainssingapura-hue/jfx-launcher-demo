package com.example.fxsuite.avatar.air;

/**
 * What a part of an aircraft is finished in, by what it does: a livery ({@link Livery}) says what
 * colour each is. So one airframe can be drawn as a patrol airship, a bomber, a rescue helicopter.
 */
public enum Paint {
    /** The main skin: an envelope, a fuselage, a drone's body. */
    SKIN,
    /** The frames and girders standing proud of a skin, bands round it. */
    FRAME,
    /** A second body hung on the first: a gondola, a car, an engine's housing. */
    CAR,
    /** Windows, a canopy. */
    GLASS,
    /** Engines, struts, landing gear: bare metal. */
    METAL,
    /** Propellers, rotor blades, tyres. */
    DARK,
    /** Wings, fins and tailplanes. */
    WING,
    /** A stripe, a band, an insignia: the livery's accent. */
    TRIM
}
