package com.example.fxsuite.avatar.air;

import com.example.fxsuite.avatar.build.Point;

/**
 * The body layer of an aircraft: what its other parts (wings, engines, fins, struts) are fixed to.
 * Made in a frame of its own, its middle at the origin, its nose to +along; mounted on the aircraft
 * ({@link Mount}). Two kinds, as yet: a {@link Fuselage}, lofted through sections, as versatile as
 * its sections are (an airship's envelope, an airliner's fuselage, a helicopter's pod and boom); and
 * a {@link BoxBody}, a box with its edges rounded (a drone's body, a boxy helicopter, a gondola).
 */
public sealed interface Body permits Fuselage, BoxBody {

    String name();

    Paint paint();

    /** Its skin, in its own frame. */
    Skin skin();

    /**
     * The point on its skin {@code along} its length, {@code around} its section in degrees: 0 on
     * its right side, 90 on top, 180 on its left, 270 under it. Where a part is fixed to it.
     */
    Point surface(double along, double around);

    /** How far it reaches each way. */
    default Extent extent() {
        return Extent.of(skin().points());
    }
}
