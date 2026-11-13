package com.example.fxsuite.avatar.sim;

import java.util.ArrayList;
import java.util.List;

/**
 * What the avatar holds, what it does with it, and what that hits. Pure.
 *
 * <ul>
 *   <li><b>Rifles</b>, one in each hand, fire in turn while the trigger is held: left, right,
 *       left. Each fires {@link #RIFLE_RATE} rounds a minute, so together twice that.</li>
 *   <li><b>The Gatling gun</b> has to spin up first: holding the trigger spins its barrels up
 *       over {@link #SPIN_UP} seconds, and only at full speed does it fire, {@link #GATLING_RATE}
 *       rounds a minute, held dead steady: every round along the same line. Let go, and it stops
 *       firing at once, and spins down over {@link #SPIN_DOWN} seconds; squeeze again before it
 *       has stopped, and it is back up to speed the sooner.</li>
 *   <li><b>The glaive</b> swings in strokes of {@link #STROKE} seconds while the trigger is held:
 *       an overhead cleave, then a sweep across, then a cleave again. Each stroke winds up first;
 *       its blade comes through at {@link #STRIKE} of the way in, which is when it is heard, and
 *       when it strikes whatever is within its reach: a cleave comes down in front, onto the
 *       ground if nothing is nearer; a sweep goes across at shoulder height. Let go mid-stroke,
 *       and the stroke still finishes.</li>
 * </ul>
 *
 * <p>A round flies from its muzzle straight ahead of the body, wherever the head or the camera
 * is looking, tilted up or down only by the aim ({@link Controls#aim}), and spread a little as
 * rounds fired from the hip are. It strikes the first block, or the ground, in its way: a
 * {@link Blow}, for the world to take within the step.
 * {@link #sight} marks where that line meets the world, for the player to see where they are
 * shooting. The spread goes by the rounds' count, not by chance, so a run repeats exactly.
 *
 * @param held       what is in the hands
 * @param reload     seconds until the next round can fire
 * @param leftNext   whether the next round comes from the left rifle
 * @param sinceLeft  seconds since the left rifle last fired; likewise the right rifle, or the
 *                   Gatling gun
 * @param stroke     seconds into the glaive's stroke, or negative between strokes
 * @param strokes    how many strokes have been begun, the one under way among them
 * @param rounds     how many rounds have been fired since the weapon was drawn
 * @param pitch      how far up (down when negative) they shoot, in degrees: the aim, within
 *                   {@link #limit} of level
 * @param spin       how fast the Gatling gun's barrels are turning, 0 still to 1 full speed
 */
public record Arms(Weapon held, double reload, boolean leftNext, double sinceLeft, double sinceRight,
                   double stroke, int strokes, int rounds, double pitch, double spin) {

    /** Rounds a minute from each rifle: as an assault rifle fires. */
    public static final double RIFLE_RATE = 600;
    /** Between rounds, the two rifles taking turns. */
    public static final double RIFLE_INTERVAL = 60 / RIFLE_RATE / 2;
    /** Rounds a minute from the Gatling gun, at full speed; and between them. */
    public static final double GATLING_RATE = 2400;
    public static final double GATLING_INTERVAL = 60 / GATLING_RATE;
    /** Seconds for the Gatling gun's barrels to spin up from still, and down from full speed. */
    public static final double SPIN_UP = 0.6;
    public static final double SPIN_DOWN = 1.5;
    /** How far a round can fly. */
    public static final double RANGE = 30_000;
    /**
     * How far off true a round may go, in degrees either way: a rifle's, fired from the hip; and
     * the Gatling gun's, none: the robot is strong enough to hold it dead steady, every round
     * along the same line.
     */
    public static final double SPREAD = 1.2;
    public static final double GATLING_SPREAD = 0;
    /** The sight marks the line ahead no further off than this: 30 m. */
    public static final double SIGHT = 3000;
    /** The furthest up or down the guns can be aimed from the hip, in degrees: the rifles, and the heavier Gatling gun. */
    public static final double AIM_LIMIT = 50;
    public static final double GATLING_AIM_LIMIT = 30;
    /** One stroke of the glaive, wind-up to recovery, in seconds. */
    public static final double STROKE = 0.75;
    /** How far into a stroke the blade comes through, as a share of it. */
    public static final double STRIKE = 0.42;
    /** From the shoulder to the blade's tip: the arm, and the glaive beyond the hand. */
    public static final double REACH = 255;
    /** A cleave comes down in front at this many degrees below level; a sweep goes level across. */
    public static final double CLEAVE_DOWN = 55;
    /** The strokes, in the order they come. */
    public static final int CLEAVE = 0;
    public static final int SWEEP = 1;

    /** The rifles held from the hip: how high the muzzles, how far out to the sides, how far ahead. */
    public static final double MUZZLE_HEIGHT = 130;
    public static final double MUZZLE_OUT = 32;
    public static final double MUZZLE_AHEAD = 93;
    /** The Gatling gun's muzzle, held at the right hip: how high, how far out to the right, how far ahead. */
    public static final double GATLING_HEIGHT = 123;
    public static final double GATLING_OUT = 16;
    public static final double GATLING_AHEAD = 121;
    /** The right shoulder, where the glaive swings from: how high, and how far out. */
    public static final double SHOULDER_HEIGHT = 144;
    public static final double SHOULDER_OUT = 41;

    private static final double LONG_AGO = 1e9;

    /** Empty hands. */
    public static final Arms EMPTY = holding(Weapon.NONE);

    /** Freshly drawn, nothing fired or swung, still. */
    public static Arms holding(final Weapon weapon) {
        return new Arms(weapon, 0, true, LONG_AGO, LONG_AGO, -1, 0, 0, 0, 0);
    }

    /**
     * What changed, what happened (rounds fired, strokes swung), and the {@link Blow}s they dealt:
     * intents, for the world to take within the same step, never outputs themselves.
     */
    public record Stepped(Arms arms, List<Event> events, List<Blow> blows) {

        public Stepped {
            events = List.copyOf(events);
            blows = List.copyOf(blows);
        }
    }

    /** What a step is gathering as it goes: what happened, and the blows dealt. */
    private record Out(List<Event> events, List<Blow> blows) {

        Stepped stepped(final Arms arms) {
            return new Stepped(arms, events, blows);
        }
    }

    /** As {@link #step(boolean, double, Body, Obstacles, double)}, standing at the origin on open ground, level. */
    public Stepped step(final boolean trigger, final double seconds) {
        return step(trigger, seconds, Body.standing(Vec3.ZERO, 0), Obstacles.NONE, 0);
    }

    /**
     * Fires or swings for {@code seconds}, the trigger held or not, from {@code body} among
     * {@code world}, aimed {@code aim} degrees up (down when negative).
     */
    public Stepped step(final boolean trigger, final double seconds, final Body body, final Obstacles world,
                        final double aim) {
        final Out out = new Out(new ArrayList<>(), new ArrayList<>());
        final Arms aimed = new Arms(held, reload, leftNext, sinceLeft, sinceRight, stroke, strokes, rounds,
                Num.clamp(aim, -limit(held), limit(held)), spin);
        return switch (held) {
            // no one holds a bomb, or a fall
            case NONE, BOMB, FALL, HEAVY_GATLING -> out.stepped(aimed);
            case RIFLES -> aimed.fire(Gun.RIFLES, trigger, seconds, body, world);
            case GATLING -> aimed.fire(Gun.GATLING, trigger, seconds, body, world);
            case GLAIVE -> aimed.swing(trigger, seconds, body, world, out);
        };
    }

    /** How far up or down a weapon can be aimed. */
    public static double limit(final Weapon weapon) {
        return weapon == Weapon.GATLING ? GATLING_AIM_LIMIT : AIM_LIMIT;
    }

    /** How far through the stroke under way, 0 to 1; negative between strokes. */
    public double progress() {
        return stroke < 0 ? -1 : stroke / STROKE;
    }

    /** Which stroke is under way ({@link #CLEAVE} or {@link #SWEEP}), or the last one. */
    public int strokeKind() {
        return Math.max(0, strokes - 1) % 2;
    }

    /** Whether the Gatling gun is firing: up to speed, and a round gone in the last moment. */
    public boolean firing() {
        return held == Weapon.GATLING && spin >= 1 && sinceRight < 2 * GATLING_INTERVAL;
    }

    /** A rifle's muzzle, held from the hip: the left one, or the right. */
    public static Vec3 muzzle(final Body body, final boolean left) {
        return ahead(body, MUZZLE_HEIGHT, left ? -MUZZLE_OUT : MUZZLE_OUT, MUZZLE_AHEAD);
    }

    /** The Gatling gun's muzzle, at the right hip: the middle of its barrels' ends. */
    public static Vec3 gatlingMuzzle(final Body body) {
        return ahead(body, GATLING_HEIGHT, GATLING_OUT, GATLING_AHEAD);
    }

    /**
     * The sight: where the line the rounds fly along first meets a block or the ground, just short
     * of it; or {@link #SIGHT} ahead, when nothing is nearer. The line goes straight ahead of the
     * body, tilted by the aim: from the Gatling gun's muzzle, or for the rifles from between
     * theirs, at their height. Only a mark for the player; the rifles' rounds fly from the muzzles
     * either side of it.
     */
    public static Vec3 sight(final Body body, final Obstacles world, final double pitch, final Weapon weapon) {
        final Vec3 ahead = line(body, pitch);
        final Vec3 from = weapon == Weapon.GATLING ? gatlingMuzzle(body) : ahead(body, MUZZLE_HEIGHT, 0, MUZZLE_AHEAD);
        final Hit hit = world.ray(from, ahead, SIGHT);
        return hit == null ? from.add(ahead.multiply(SIGHT)) : hit.at().subtract(ahead.multiply(2));
    }

    /** The rifles' sight. */
    public static Vec3 sight(final Body body, final Obstacles world, final double pitch) {
        return sight(body, world, pitch, Weapon.RIFLES);
    }

    /** Straight ahead of the body, tilted {@code pitch} degrees up (down when negative): a unit vector. */
    public static Vec3 line(final Body body, final double pitch) {
        final double p = Math.toRadians(pitch);
        // y points down: up is -y
        return Num.heading(body.yaw()).multiply(Math.cos(p)).add(0, -Math.sin(p), 0);
    }

    /** The right shoulder, where the glaive swings from. */
    public static Vec3 shoulder(final Body body) {
        return body.position().add(0, -SHOULDER_HEIGHT, 0).add(Num.right(body.yaw()).multiply(SHOULDER_OUT));
    }

    /** A point {@code height} up, {@code out} to the right (left when negative) and {@code forward} ahead of the body. */
    private static Vec3 ahead(final Body body, final double height, final double out, final double forward) {
        return body.position().add(0, -height, 0)
                .add(Num.right(body.yaw()).multiply(out))
                .add(Num.heading(body.yaw()).multiply(forward));
    }

    /**
     * The rifles, or the Gatling gun: fired as every gun is ({@link Gun#fire}), held as the hands
     * hold them. Each round leaves from its muzzle at the hip and flies straight ahead of the body,
     * tilted by the aim: the gun goes with the body that holds it.
     */
    private Stepped fire(final Gun gun, final boolean trigger, final double seconds, final Body body,
                         final Obstacles world) {
        final Vec3 line = line(body, pitch);
        final Vec3 across = Num.right(body.yaw());
        final Gun.Muzzles muzzles = gun.pair()
                ? left -> new Gun.Bore(muzzle(body, left), line, across)
                : left -> new Gun.Bore(gatlingMuzzle(body), line, across);
        final Gun.Fired fired = gun.fire(gun(), trigger, seconds, muzzles, world);
        return new Stepped(with(fired.state()), fired.events(), fired.blows());
    }

    /** How the gun in the hands is getting on, as the firing of every gun goes ({@link Gun.State}). */
    private Gun.State gun() {
        return new Gun.State(reload, leftNext, sinceLeft, sinceRight, rounds, spin);
    }

    /** The same, the gun in the hands now getting on as {@code gun} says. */
    private Arms with(final Gun.State gun) {
        return new Arms(held, gun.reload(), gun.leftNext(), gun.sinceLeft(), gun.sinceRight(), stroke, strokes,
                gun.rounds(), pitch, gun.spin());
    }

    private Stepped swing(final boolean trigger, final double seconds, final Body body, final Obstacles world,
                          final Out out) {
        double at = stroke;
        int count = strokes;
        if (at < 0 && trigger) {
            at = 0;
            count++;
        }
        if (at < 0) {
            return out.stepped(this);
        }
        final double was = at;
        at += seconds;
        if (was < STRIKE * STROKE && at >= STRIKE * STROKE) {
            strike((count - 1) % 2, body, world, out);
        }
        if (at >= STROKE) {
            if (trigger) {
                // straight into the next stroke
                at -= STROKE;
                count++;
                if (at >= STRIKE * STROKE) {
                    strike((count - 1) % 2, body, world, out);
                }
            } else {
                at = -1;
            }
        }
        return out.stepped(new Arms(held, reload, leftNext, sinceLeft, sinceRight, at, count, rounds, pitch, spin));
    }

    /** The blade comes through: heard, and striking the nearest thing within its reach, if any. */
    private static void strike(final int kind, final Body body, final Obstacles world, final Out out) {
        out.events().add(new Event.Swung(kind));
        final Vec3 from = shoulder(body);
        final Vec3 ahead = Num.heading(body.yaw());
        final List<Vec3> paths = new ArrayList<>();
        if (kind == CLEAVE) {
            final double down = Math.toRadians(CLEAVE_DOWN);
            paths.add(ahead.multiply(Math.cos(down)).add(0, Math.sin(down), 0));
        } else {
            // the sweep, from out on the right, through ahead, across to the left
            for (final double turn : new double[] {60, 30, 0, -30, -60}) {
                paths.add(Num.heading(body.yaw() + turn));
            }
        }
        Hit nearest = null;
        for (final Vec3 path : paths) {
            final Hit hit = world.ray(from, path, REACH);
            if (hit != null && (nearest == null || hit.distance() < nearest.distance())) {
                nearest = hit;
            }
        }
        if (nearest != null) {
            out.blows().add(new Blow(nearest.at(), nearest.normal(), nearest.on(), Weapon.GLAIVE, nearest.piece()));
        }
    }
}
