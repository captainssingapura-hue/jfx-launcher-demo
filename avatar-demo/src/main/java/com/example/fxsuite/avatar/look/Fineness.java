package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.sim.Num;
import com.example.fxsuite.avatar.sim.Vec3;

/**
 * How finely the land is drawn: a fineness from 1, the finest, to -1, the roughest, by how far it is from the
 * camera and how fast the camera moves ({@link #of}). At rest, falling evenly with the logarithm of distance
 * ({@link #rest}); less the faster the camera goes, as the square of its speed ({@link #penalty}), and the
 * faster so; speed counting for nothing right round the camera, and less and less far off ({@link #weight}).
 *
 * <p>The land is built at a few levels, fineness rounded ({@link #level}), going finer again only once it is
 * past a level's edge by {@link #HOLD}. The camera's speed is smoothed ({@link Pace}).
 */
public final class Fineness {

    /** At rest: this fine at {@link #NEAR}, and as rough again at {@link #FAR}. */
    static final double EDGE = 0.75;
    static final double NEAR = 30_000;
    static final double FAR = 450_000;
    /** Below this speed, walking and running, no penalty; this much faster, a penalty of 1. */
    static final double FREE = 500;
    static final double SCALE = 5_000;
    /** Within this, speed counts for nothing; from it to {@link #FULL}, more and more; full out to {@link #WANES}. */
    static final double FLOOR = 6_000;
    static final double FULL = 15_000;
    /** Beyond this, speed counts less and less, as how fast the land moves on screen does. */
    static final double WANES = 80_000;
    /** How many levels the land is built at, and the edges of their bands, finest first. */
    static final int LEVELS = 5;
    private static final double[] EDGES = {0.75, 0.25, -0.25, -0.75};
    /** How far past a band's edge fineness must be for a tile to go finer again. */
    static final double HOLD = 0.1;

    private Fineness() {
    }

    /**
     * How finely land {@code distance} across the ground from the camera is drawn, the camera moving at
     * {@code speed}: and past {@link #WANES}, where speed counts less and less, never finer than there, so the
     * land is never drawn finer for being further off.
     */
    public static double of(final double distance, final double speed) {
        final double penalty = penalty(speed);
        double fine = rest(distance) - weight(distance) * penalty;
        if (distance > WANES) {
            fine = Math.min(fine, rest(WANES) - penalty);
        }
        return Num.clamp(fine, -1, 1);
    }

    /** At rest: {@link #EDGE} at {@link #NEAR}, as much below 0 at {@link #FAR}, evenly in the logarithm between. */
    static double rest(final double distance) {
        if (distance <= 0) {
            return 1;
        }
        return Num.clamp(EDGE - 2 * EDGE * Math.log(distance / NEAR) / Math.log(FAR / NEAR), -1, 1);
    }

    /** What speed {@code speed} costs: none below {@link #FREE}, and then as the square of how much faster. */
    static double penalty(final double speed) {
        final double over = Math.max(0, speed - FREE) / SCALE;
        return over * over;
    }

    /** How much speed counts {@code distance} off: none within {@link #FLOOR}, all from {@link #FULL}, waning past {@link #WANES}. */
    static double weight(final double distance) {
        if (distance <= FLOOR) {
            return 0;
        }
        if (distance < FULL) {
            final double t = (distance - FLOOR) / (FULL - FLOOR);
            return t * t * (3 - 2 * t);
        }
        return distance <= WANES ? 1 : WANES / distance;
    }

    /**
     * The level fineness {@code fine} rounds to, 0 the finest, having been at {@code was} (-1 if not yet): a
     * finer one again only once past its edge by {@link #HOLD}.
     */
    static int level(final double fine, final int was) {
        int level = 0;
        while (level < EDGES.length && fine < EDGES[level] + (level < was ? HOLD : 0)) {
            level++;
        }
        return level;
    }

    /**
     * The camera's speed across the ground, smoothed: taken in quickly as it rises ({@link #RISE}), let go slowly
     * as it falls ({@link #FALL}), so a hard stop does not ask for all the fine land at once; a jump of more than
     * {@link #CUT} in a frame no speed at all.
     */
    public static final class Pace {
        static final double RISE = 2;
        static final double FALL = 0.5;
        static final double CUT = 5_000;
        private Vec3 last;
        private double speed;

        /** The camera now at {@code eye}, {@code seconds} after it was last: its speed, smoothed. */
        public double at(final Vec3 eye, final double seconds) {
            if (last != null && seconds > 0) {
                final double moved = Math.hypot(eye.x() - last.x(), eye.z() - last.z());
                if (moved <= CUT) {
                    final double now = moved / seconds;
                    speed = Num.ease(speed, now, now > speed ? RISE : FALL, seconds);
                }
            }
            last = eye;
            return speed;
        }

        public double speed() {
            return speed;
        }
    }
}
