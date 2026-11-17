package com.example.fxsuite.avatar.sim;

/**
 * How far a land vehicle's front wheels may be turned, and how fast they get there, by its speed: a full turn
 * pulling across no more than a tyre holds ({@link #GRIP}), and the faster it goes the gentler, and faster so
 * ({@link #pull}); so far round as pulls that ({@link #lock}), its own lock at a crawl; turned in over a time,
 * the longer the faster it goes ({@link #turnIn}), let back twice as fast; never further than the lock as it
 * now is ({@link #turned}).
 */
public final class Steering {

    /** The world's own g, cm/s²: not the game's heavier one for falling ({@link Body#GRAVITY}). */
    public static final double G = 981;
    /** What a tyre holds across, cm/s²: 0.9 g. */
    public static final double GRIP = 0.9 * G;
    /** At its top speed, a full turn pulls this much less than {@link #GRIP}. */
    static final double CALM = 0.4;
    /** How long a key held down takes to turn the wheels to their lock, at a crawl; and how much longer at its top. */
    static final double IN = 0.15;
    static final double SLOWER = 0.45;
    /** Let go, they come back this much faster than they were turned in. */
    static final double BACK = 2;

    private Steering() {
    }

    /** How hard a full turn pulls across, cm/s², going {@code speed} on {@code chassis}: {@link #GRIP}, calmer the faster. */
    public static double pull(final Course.Chassis chassis, final double speed) {
        final double share = Math.min(1, Math.abs(speed) / chassis.drive().top());
        return GRIP * (1 - CALM * share * share);
    }

    /** How far its wheels may be turned, in degrees, going {@code speed}: its own lock, or as far as pulls {@link #pull}. */
    public static double lock(final Course.Chassis chassis, final double speed) {
        final double v = Math.abs(speed);
        final double most = chassis.drive().steer();
        if (v < 1) {
            return most;
        }
        return Math.min(most, Math.toDegrees(Math.atan(chassis.wheelbase() * pull(chassis, v) / (v * v))));
    }

    /** How long, in seconds, a key held down takes to turn its wheels from straight to {@link #lock}, going {@code speed}. */
    public static double turnIn(final Course.Chassis chassis, final double speed) {
        final double share = Math.min(1, Math.abs(speed) / chassis.drive().top());
        return IN + SLOWER * share * share;
    }

    /**
     * Its wheels turned {@code steer} degrees, {@code seconds} on, with {@code asked} (-1 to 1) asked of them, going
     * {@code speed}: toward that share of {@link #lock}, at that over {@link #turnIn} (back toward straight
     * {@link #BACK} as fast), no faster than they turn at all; and never further round than the lock is now.
     */
    public static double turned(final Course.Chassis chassis, final double steer, final double asked,
                                final double speed, final double seconds) {
        final double lock = lock(chassis, speed);
        final double wanted = Num.clamp(asked, -1, 1) * lock;
        final boolean back = Math.abs(wanted) < Math.abs(steer) || wanted * steer < 0;
        final double rate = Math.min(chassis.drive().steerRate(), lock / turnIn(chassis, speed)) * (back ? BACK : 1);
        final double now = steer + Num.clamp(wanted - steer, -rate * seconds, rate * seconds);
        return Num.clamp(now, -lock, lock);
    }
}
