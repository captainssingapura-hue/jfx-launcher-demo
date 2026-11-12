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
        return fade(distance, NEAR);
    }

    /**
     * How loud, 0 to 1, a sound that carries further is heard: full within {@code near}, then
     * falling as {@code near / distance}. A blast is still loud a long way off, where a footstep
     * would be lost.
     */
    public static double fade(final double distance, final double near) {
        final double d = Num.clamp(distance, near, Math.max(near, FAR));
        return near / (near + ROLLOFF * (d - near));
    }

    /**
     * Which side a sound is on, for a listener at {@code ear} facing {@code yaw}: -1 hard left,
     * 0 ahead, behind, above or below, 1 hard right. Worked out from the whole direction, height
     * and all, as OpenAL does: a foot just to the side but well below the ear is nearly in the
     * middle, not in one ear.
     */
    public static double side(final Vec3 ear, final double yaw, final Vec3 at) {
        final Vec3 to = at.subtract(ear);
        final double length = to.magnitude();
        return length < 1e-6 ? 0 : Num.clamp(to.dotProduct(Num.right(yaw)) / length, -1, 1);
    }
}
