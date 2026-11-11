package com.example.fxsuite.avatar.audio;

import com.example.fxsuite.avatar.sim.Num;
import com.example.fxsuite.avatar.sim.Vec3;

/**
 * How a sound's place in the world changes what the listener hears: it fades with distance,
 * and it leans to the ear it is nearer. One model, so every output sounds the same: OpenAL is
 * set up to this, and the software {@link Mix} works it out itself.
 *
 * <p>The fade is OpenAL's "inverse distance, clamped": full within {@link #NEAR}, then falling
 * as {@code NEAR / distance}, and no further beyond {@link #FAR}.
 */
public final class Space {

    /** Within 3 m, full loudness. */
    public static final double NEAR = 300;
    /** Past 200 m, no quieter. */
    public static final double FAR = 20_000;
    public static final double ROLLOFF = 1;

    private Space() {
    }

    /** How loud, 0 to 1, a sound is heard from {@code distance} away. */
    public static double fade(final double distance) {
        final double d = Num.clamp(distance, NEAR, FAR);
        return NEAR / (NEAR + ROLLOFF * (d - NEAR));
    }

    /**
     * Which side a sound is on, for a listener at {@code ear} facing {@code yaw}: -1 hard left,
     * 0 ahead, behind or right on top of the listener, 1 hard right. Height does not count.
     */
    public static double side(final Vec3 ear, final double yaw, final Vec3 at) {
        final Vec3 to = new Vec3(at.x() - ear.x(), 0, at.z() - ear.z());
        final double length = to.magnitude();
        return length < 1e-6 ? 0 : Num.clamp(to.dotProduct(Num.right(yaw)) / length, -1, 1);
    }
}
