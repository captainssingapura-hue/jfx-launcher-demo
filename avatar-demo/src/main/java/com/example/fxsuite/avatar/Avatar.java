package com.example.fxsuite.avatar;

/**
 * The avatar as a whole: how it moves ({@link Body}) and what it is wearing. For now that is
 * just the jet-pack, and the one rule it brings: only with the pack on can it fly. Pure.
 *
 * <ul>
 *   <li>Without the pack, taking off does nothing.</li>
 *   <li>Taking the pack off in flight stops the flight, and it falls.</li>
 *   <li>Putting it on, or taking it off on the ground or mid-jump, changes nothing else.</li>
 * </ul>
 */
public record Avatar(Body body, boolean wearingPack) {

    public Avatar step(final Controls controls, final double seconds, final Obstacles world) {
        return new Avatar(body.step(controls, seconds, world), wearingPack);
    }

    public Avatar jumped() {
        return new Avatar(body.jumped(), wearingPack);
    }

    /** Takes off, if wearing the pack, or stops flying. */
    public Avatar toggledFlight() {
        return body.flying() || wearingPack ? new Avatar(body.toggledFlight(), wearingPack) : this;
    }

    /** Puts the pack on, or takes it off; off in flight, it falls. */
    public Avatar toggledPack() {
        return new Avatar(wearingPack && body.flying() ? body.toggledFlight() : body, !wearingPack);
    }
}
