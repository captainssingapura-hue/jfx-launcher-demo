package com.example.fxsuite.avatar.sound;

import com.example.fxsuite.avatar.sim.Arms;
import com.example.fxsuite.avatar.sim.Avatar;
import com.example.fxsuite.avatar.sim.Block;
import com.example.fxsuite.avatar.sim.Body;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Event;
import com.example.fxsuite.avatar.sim.Num;
import com.example.fxsuite.avatar.sim.Person;
import com.example.fxsuite.avatar.sim.Surface;
import com.example.fxsuite.avatar.sim.Vec3;
import com.example.fxsuite.avatar.sim.Weapon;

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
    /** Where the hands are, in front of the chest. */
    static final double HANDS_HEIGHT = 130;
    /** The glaive's blade as it comes through, ahead of the chest. */
    static final double BLADE_AHEAD = 120;
    /** The biggest crate there is, in cubic units: it breaks as loud and deep as a break can sound. */
    static final double BIGGEST_CRATE = 260 * 260 * 240;

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

    /** As {@link #of(Avatar, Profile, List)}, with no airships about. */
    public static Heard of(final Avatar avatar, final List<Event> events, final Profile profile) {
        return of(avatar, events, profile, List.of());
    }

    /**
     * What to hear this frame. {@code airships} are where the airships still in the sky are; the
     * nearest is heard droning, from where it hangs, and none when none is left.
     */
    public static Heard of(final Avatar avatar, final List<Event> events, final Profile profile,
                           final List<Vec3> airships) {
        final Body body = avatar.body();
        final Vec3 feet = body.position();
        final Vec3 pack = pack(body);
        final List<Play> plays = new ArrayList<>();
        for (final Event event : events) {
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
                case Event.Drew d -> {
                    switch (d.weapon()) {
                        case RIFLES -> plays.add(new Play(Sound.RIFLES_DRAWN, 1, hands(body, 0)));
                        case GATLING -> plays.add(new Play(Sound.GATLING_DRAWN, 1, hands(body, 0)));
                        case GLAIVE -> plays.add(new Play(Sound.GLAIVE_DRAWN, 1, hands(body, 0)));
                        case NONE, BOMB, FALL -> {
                        }
                    }
                }
                // the Gatling gun's rounds come too fast to be heard one by one: its roar is a loop
                case Event.Fired f -> {
                    if (avatar.holding() != Weapon.GATLING) {
                        plays.add(new Play(Sound.RIFLE_SHOT, 1, f.from()));
                    }
                }
                // a bomb's blast is heard once, going off, not on each thing it reaches
                case Event.Struck s -> {
                    if (Person.is(s.piece())) {
                        // someone hit: a dull thump, not a ring off metal or wood
                        plays.add(new Play(Sound.BUMP, 0.5, s.at()));
                    } else if (s.by() != Weapon.BOMB && s.by() != Weapon.FALL) {
                        plays.add(new Play(struck(s), 1, s.at()));
                    }
                }
                // someone shoved stumbles off; falling, they hit the ground; destroyed, they go off with a bang
                case Event.Shoved s -> plays.add(new Play(Sound.BUMP, Num.clamp(s.speed() / Body.RUN, 0.2, 1), s.at()));
                case Event.Fell f -> plays.add(new Play(Sound.LANDING, Num.clamp(f.speed() / HARDEST_LANDING, 0.3, 1),
                        f.at()));
                case Event.Destroyed d -> plays.add(new Play(Sound.BREAK_DRONE, 1, d.at().add(0, -Body.TALL / 2, 0)));
                case Event.Exploded x -> plays.add(new Play(Sound.BOMB, 1, x.at()));
                case Event.Dropped _ -> {
                }
                // a latch, taking hold of the controls and letting go
                case Event.TookControls _ -> plays.add(new Play(Sound.PACK_ON, 1, hands(body, 0)));
                case Event.LetGo _ -> plays.add(new Play(Sound.PACK_OFF, 1, hands(body, 0)));
                // a platform's engine starting and cutting out, as the jet-pack's do; and its coming down
                case Event.EngineStarted _ -> plays.add(new Play(Sound.IGNITION, 1, hands(body, 0)));
                case Event.EngineCut _ -> plays.add(new Play(Sound.CUT, 1, hands(body, 0)));
                case Event.Settled s -> plays.add(new Play(Sound.LANDING_BLOCK,
                        Num.clamp(s.speed() / HARDEST_LANDING, 0.2, 1), s.at()));
                case Event.Broke b -> plays.add(switch (b.kind()) {
                    case DRONE -> new Play(Sound.BREAK_DRONE, 1, b.middle());
                    case AIRSHIP -> new Play(Sound.BREAK_AIRSHIP, 1, b.middle());
                    case FRUIT -> new Play(Sound.FRUIT, b.fruit().heft(), b.middle());
                    default -> new Play(Sound.BREAK_WOOD, size(b.block()), b.middle());
                });
                case Event.Swung s -> plays.add(new Play(s.stroke() == Arms.CLEAVE ? Sound.GLAIVE_CLEAVE
                        : Sound.GLAIVE_SWEEP, 1, hands(body, BLADE_AHEAD)));
            }
        }
        final double thrust = avatar.thrust();
        final Arms arms = avatar.arms();
        final double spin = arms.held() == Weapon.GATLING ? arms.spin() : 0;
        final double speed = Math.sqrt(body.speed() * body.speed() + body.drift() * body.drift()
                + body.climb() * body.climb());
        final Profile.Drive drive = new Profile.Drive(thrust, speed, spin, arms.firing());
        final List<Hum> hums = new ArrayList<>();
        final Vec3 airship = nearest(airships, feet);
        for (final Sound loop : Sound.loops()) {
            if (loop == Sound.AIRSHIP_ENGINES) {
                hums.add(airship == null ? new Hum(loop, Level.OFF, null)
                        : new Hum(loop, profile.level(loop, drive), airship));
                continue;
            }
            final Vec3 at = switch (loop) {
                case WIND -> null;
                case GATLING_SPIN, GATLING_FIRE -> Arms.gatlingMuzzle(body);
                default -> pack;
            };
            hums.add(new Hum(loop, profile.level(loop, drive), at));
        }
        return new Heard(plays, hums);
    }

    /** The one of {@code points} nearest {@code to}, or null if there are none. */
    private static Vec3 nearest(final List<Vec3> points, final Vec3 to) {
        Vec3 nearest = null;
        for (final Vec3 point : points) {
            if (nearest == null || point.distance(to) < nearest.distance(to)) {
                nearest = point;
            }
        }
        return nearest;
    }

    /** The middle of the jet-pack, on the avatar's back. */
    static Vec3 pack(final Body body) {
        return body.position().add(0, -PACK_HEIGHT, 0).subtract(Num.heading(body.yaw()).multiply(PACK_BEHIND));
    }

    /** What something struck sounds like: a round or the blade, on a block or the ground. */
    static Sound struck(final Event.Struck struck) {
        final boolean block = struck.on() == Surface.BLOCK;
        return struck.by() == Weapon.GLAIVE
                ? (block ? Sound.BLADE_ON_BLOCK : Sound.BLADE_ON_GROUND)
                : (block ? Sound.ROUND_ON_BLOCK : Sound.ROUND_ON_GROUND);
    }

    /**
     * How big something broken was, 0 to 1, for how it sounds: by its volume, 0 for a dummy's,
     * 1 for the biggest crate's.
     */
    static double size(final Block block) {
        final double volume = 4 * block.halfWidth() * block.halfDepth() * (block.top() - block.bottom());
        final double dummy = Course.DUMMY_WIDTH * Course.DUMMY_DEPTH * Course.DUMMY_HEIGHT;
        return Num.clamp(Math.log(volume / dummy) / Math.log(BIGGEST_CRATE / dummy), 0, 1);
    }

    /** In front of the chest, {@code ahead} further out: the hands, or where a stroke goes through. */
    static Vec3 hands(final Body body, final double ahead) {
        return body.position().add(0, -HANDS_HEIGHT, 0).add(Num.heading(body.yaw()).multiply(20 + ahead));
    }
}
