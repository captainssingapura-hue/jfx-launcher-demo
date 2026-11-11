package com.example.fxsuite.avatar.sound;

import com.example.fxsuite.avatar.sim.Avatar;
import com.example.fxsuite.avatar.sim.Body;
import com.example.fxsuite.avatar.sim.Event;
import com.example.fxsuite.avatar.sim.Num;
import com.example.fxsuite.avatar.sim.Surface;
import com.example.fxsuite.avatar.sim.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * What to hear this frame, worked out from the avatar alone. Each of its events becomes a sound
 * to play once, from where it happened. Its thrust and speed set the loops: the jets at the
 * pack, the wind at the listener. Pure, and no device: an output plays what this says.
 *
 * <p>The wings have no event of their own; they follow flight, as the graphics draw them:
 * opening on take-off, folding as a flight ends, whether cut short or landed.
 */
public final class Cues {

    /** A fall at this speed lands as hard as a landing can sound: a drop from a tall pillar. */
    static final double HARDEST_LANDING = 2500;
    /** Feet sit this far either side of the body's middle. */
    static final double FOOT_SPREAD = 10;
    /** Where the pack sits: up the back, and behind it (as {@code JetPack} draws it). */
    static final double PACK_HEIGHT = 120;
    static final double PACK_BEHIND = 36;

    private Cues() {
    }

    /** One sound to play once: {@code at} where in the world, or null for at the listener. */
    public record Play(Sound sound, double amount, Vec3 at) {
    }

    /** How a loop should be playing now, and where; null for at the listener. */
    public record Hum(Sound loop, Level level, Vec3 at) {
    }

    /** All of it: what to play once, and how every loop should be playing. */
    public record Heard(List<Play> plays, List<Hum> hums) {
        public Heard {
            plays = List.copyOf(plays);
            hums = List.copyOf(hums);
        }
    }

    public static Heard of(final Avatar avatar, final Profile profile) {
        final Body body = avatar.body();
        final Vec3 feet = body.position();
        final Vec3 pack = pack(body);
        final List<Play> plays = new ArrayList<>();
        for (final Event event : avatar.events()) {
            switch (event) {
                case Event.Footfall f -> plays.add(new Play(
                        f.on() == Surface.BLOCK ? Sound.STEP_BLOCK : Sound.STEP_GROUND,
                        Num.clamp(body.groundSpeed() / Body.RUN, 0, 1),
                        feet.add(Num.right(body.yaw()).multiply(f.left() ? -FOOT_SPREAD : FOOT_SPREAD))));
                case Event.Jumped _ -> plays.add(new Play(Sound.JUMP, 1, feet));
                case Event.Landed l -> {
                    plays.add(new Play(l.on() == Surface.BLOCK ? Sound.LANDING_BLOCK : Sound.LANDING,
                            Num.clamp(l.speed() / HARDEST_LANDING, 0.05, 1), feet));
                    if (l.fromFlight()) {
                        plays.add(new Play(Sound.CUT, 1, pack));
                        plays.add(new Play(Sound.WINGS_FOLD, 1, pack));
                    }
                }
                case Event.Bumped b -> plays.add(new Play(Sound.BUMP, Num.clamp(b.speed() / Body.BOOST, 0, 1),
                        feet.add(0, -Body.TALL / 2, 0)));
                case Event.TookOff _ -> {
                    plays.add(new Play(Sound.IGNITION, 1, pack));
                    plays.add(new Play(Sound.WINGS_OPEN, 1, pack));
                }
                case Event.FlightCut _ -> {
                    plays.add(new Play(Sound.CUT, 1, pack));
                    plays.add(new Play(Sound.WINGS_FOLD, 1, pack));
                }
                case Event.PackOn _ -> plays.add(new Play(Sound.PACK_ON, 1, pack));
                case Event.PackOff _ -> plays.add(new Play(Sound.PACK_OFF, 1, pack));
                // a reward, for the player rather than the world: heard as if close by
                case Event.PassedGate g -> plays.add(new Play(Sound.GATE, Num.clamp(g.speed() / Body.BOOST, 0, 1), null));
            }
        }
        final double thrust = avatar.thrust();
        final double speed = Math.sqrt(body.speed() * body.speed() + body.drift() * body.drift()
                + body.climb() * body.climb());
        final List<Hum> hums = new ArrayList<>();
        for (final Sound loop : Sound.loops()) {
            hums.add(new Hum(loop, profile.level(loop, thrust, speed), loop == Sound.WIND ? null : pack));
        }
        return new Heard(plays, hums);
    }

    /** The middle of the jet-pack, on the avatar's back. */
    static Vec3 pack(final Body body) {
        return body.position().add(0, -PACK_HEIGHT, 0).subtract(Num.heading(body.yaw()).multiply(PACK_BEHIND));
    }
}
