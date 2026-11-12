package com.example.fxsuite.avatar.sim;

import java.util.ArrayList;
import java.util.List;

/**
 * What the avatar holds, what it does with it, and what that hits. Pure.
 *
 * <ul>
 *   <li><b>Rifles</b>, one in each hand, fire in turn while the trigger is held: left, right,
 *       left. Each fires {@link #RIFLE_RATE} rounds a minute, so together twice that. A round
 *       flies from its muzzle straight ahead of the body, wherever the head or the camera is
 *       looking, tilted up or down only by the aim ({@link Controls#aim}), and spread a little as rounds fired from the hip are. It strikes the first
 *       block, or the ground, in its way. {@link #sight} marks where that line meets the world,
 *       for the player to see where they are shooting.</li>
 *   <li><b>The glaive</b> swings in strokes of {@link #STROKE} seconds while the trigger is held:
 *       an overhead cleave, then a sweep across, then a cleave again. Each stroke winds up first;
 *       its blade comes through at {@link #STRIKE} of the way in, which is when it is heard, and
 *       when it strikes whatever is within its reach: a cleave comes down in front, onto the
 *       ground if nothing is nearer; a sweep goes across at shoulder height. Let go mid-stroke,
 *       and the stroke still finishes.</li>
 * </ul>
 *
 * <p>The spread of the rounds goes by their count, not by chance, so a run repeats exactly.
 *
 * @param held       what is in the hands
 * @param reload     seconds until the next round can fire
 * @param leftNext   whether the next round comes from the left rifle
 * @param sinceLeft  seconds since the left rifle last fired; likewise the right
 * @param stroke     seconds into the glaive's stroke, or negative between strokes
 * @param strokes    how many strokes have been begun, the one under way among them
 * @param rounds     how many rounds have been fired since the rifles were drawn
 * @param pitch      how far up (down when negative) they shoot, in degrees: the aim, within
 *                   {@link #AIM_LIMIT}
 */
public record Arms(Weapon held, double reload, boolean leftNext, double sinceLeft, double sinceRight,
                   double stroke, int strokes, int rounds, double pitch) {

    /** Rounds a minute from each rifle: as an assault rifle fires. */
    public static final double RIFLE_RATE = 600;
    /** Between rounds, the two rifles taking turns. */
    public static final double RIFLE_INTERVAL = 60 / RIFLE_RATE / 2;
    /** How far a round can fly, and how far off true it may go, in degrees either way. */
    public static final double RANGE = 30_000;
    /** The sight marks the line ahead no further off than this: 30 m. */
    public static final double SIGHT = 3000;
    /** The furthest up or down the rifles can be aimed from the hip, in degrees. */
    public static final double AIM_LIMIT = 50;
    public static final double SPREAD = 1.2;
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
    /** The right shoulder, where the glaive swings from: how high, and how far out. */
    public static final double SHOULDER_HEIGHT = 144;
    public static final double SHOULDER_OUT = 41;

    private static final double LONG_AGO = 1e9;

    /** Empty hands. */
    public static final Arms EMPTY = holding(Weapon.NONE);

    /** Freshly drawn, nothing fired or swung. */
    public static Arms holding(final Weapon weapon) {
        return new Arms(weapon, 0, true, LONG_AGO, LONG_AGO, -1, 0, 0, 0);
    }

    /** What changed, and what happened. */
    public record Stepped(Arms arms, List<Event> events) {
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
        final List<Event> events = new ArrayList<>();
        final Arms aimed = new Arms(held, reload, leftNext, sinceLeft, sinceRight, stroke, strokes, rounds,
                Num.clamp(aim, -AIM_LIMIT, AIM_LIMIT));
        return switch (held) {
            case NONE -> new Stepped(aimed, events);
            case RIFLES -> aimed.fire(trigger, seconds, body, world, events);
            case GLAIVE -> aimed.swing(trigger, seconds, body, world, events);
        };
    }

    /** How far through the stroke under way, 0 to 1; negative between strokes. */
    public double progress() {
        return stroke < 0 ? -1 : stroke / STROKE;
    }

    /** Which stroke is under way ({@link #CLEAVE} or {@link #SWEEP}), or the last one. */
    public int strokeKind() {
        return Math.max(0, strokes - 1) % 2;
    }

    /** A rifle's muzzle, held from the hip: the left one, or the right. */
    public static Vec3 muzzle(final Body body, final boolean left) {
        return body.position().add(0, -MUZZLE_HEIGHT, 0)
                .add(Num.right(body.yaw()).multiply(left ? -MUZZLE_OUT : MUZZLE_OUT))
                .add(Num.heading(body.yaw()).multiply(MUZZLE_AHEAD));
    }

    /**
     * The sight: where the line straight ahead of the body, tilted by the aim, between the rifles
     * at the height of their muzzles, first meets a block or the ground, just short of it. Or {@link #SIGHT} ahead,
     * when nothing is nearer. Only a mark for the player; the rounds fly from the muzzles either
     * side of it.
     */
    public static Vec3 sight(final Body body, final Obstacles world, final double pitch) {
        final Vec3 ahead = line(body, pitch);
        final Vec3 from = body.position().add(0, -MUZZLE_HEIGHT, 0).add(Num.heading(body.yaw()).multiply(MUZZLE_AHEAD));
        final Hit hit = world.ray(from, ahead, SIGHT);
        return hit == null ? from.add(ahead.multiply(SIGHT)) : hit.at().subtract(ahead.multiply(2));
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

    private Stepped fire(final boolean trigger, final double seconds, final Body body, final Obstacles world,
                         final List<Event> events) {
        double left = sinceLeft + seconds;
        double right = sinceRight + seconds;
        double wait = reload - seconds;
        boolean nextLeft = leftNext;
        int count = rounds;
        while (trigger && wait <= 0) {
            // how long ago, within this frame, the round went
            final double ago = -wait;
            if (nextLeft) {
                left = ago;
            } else {
                right = ago;
            }
            count++;
            shoot(nextLeft, count, body, world, pitch, events);
            nextLeft = !nextLeft;
            wait += RIFLE_INTERVAL;
        }
        // let go, and the next squeeze fires at once, once the action has cycled
        final double next = trigger ? wait : Math.max(0, wait);
        return new Stepped(new Arms(held, next, nextLeft, left, right, stroke, strokes, count, pitch), events);
    }

    /** One round: from its muzzle straight ahead of the body, tilted by the aim, a little off true, to whatever it strikes. */
    private static void shoot(final boolean left, final int round, final Body body, final Obstacles world,
                              final double pitch, final List<Event> events) {
        final Vec3 from = muzzle(body, left);
        // off true by up to SPREAD degrees each way, by the round's count
        final Vec3 across = Num.right(body.yaw());
        final double tan = Math.tan(Math.toRadians(SPREAD));
        final Vec3 direction = line(body, pitch).add(across.multiply(tan * scatter(round, 1)))
                .add(0, tan * scatter(round, 2), 0).normalize();
        final Hit hit = world.ray(from, direction, RANGE);
        events.add(new Event.Fired(left, from, hit == null ? from.add(direction.multiply(RANGE)) : hit.at()));
        if (hit != null) {
            events.add(new Event.Struck(hit.at(), hit.normal(), hit.on(), Weapon.RIFLES));
        }
    }

    /** -1 to 1, the same for the same round and salt: a spread that repeats. */
    private static double scatter(final int round, final int salt) {
        final long mixed = (round * 0x9E3779B97F4A7C15L + salt * 0xC2B2AE3D27D4EB4FL) * 0x2545F4914F6CDD1DL;
        return ((mixed >>> 11) * 0x1.0p-53) * 2 - 1;
    }

    private Stepped swing(final boolean trigger, final double seconds, final Body body, final Obstacles world,
                          final List<Event> events) {
        double at = stroke;
        int count = strokes;
        if (at < 0 && trigger) {
            at = 0;
            count++;
        }
        if (at < 0) {
            return new Stepped(this, events);
        }
        final double was = at;
        at += seconds;
        if (was < STRIKE * STROKE && at >= STRIKE * STROKE) {
            strike((count - 1) % 2, body, world, events);
        }
        if (at >= STROKE) {
            if (trigger) {
                // straight into the next stroke
                at -= STROKE;
                count++;
                if (at >= STRIKE * STROKE) {
                    strike((count - 1) % 2, body, world, events);
                }
            } else {
                at = -1;
            }
        }
        return new Stepped(new Arms(held, reload, leftNext, sinceLeft, sinceRight, at, count, rounds, pitch), events);
    }

    /** The blade comes through: heard, and striking the nearest thing within its reach, if any. */
    private static void strike(final int kind, final Body body, final Obstacles world, final List<Event> events) {
        events.add(new Event.Swung(kind));
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
            events.add(new Event.Struck(nearest.at(), nearest.normal(), nearest.on(), Weapon.GLAIVE));
        }
    }
}
