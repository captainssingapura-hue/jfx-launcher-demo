package com.example.fxsuite.avatar.sim;

/**
 * An airship's way to a target, and round over it ({@link Course.Route}), from {@code from} seconds
 * after the start, where it then is, {@code start}:
 *
 * <ol>
 *   <li>up (or down) where it is to {@code cruise}, high enough to clear all that stands between;</li>
 *   <li>straight on to the circle {@code orbit} round the target's middle ({@code x}, {@code z}),
 *       meeting it square on, so it goes on round without a turn: at {@code speed}, slowing as it
 *       comes near to a crawl ({@link #CRAWL});</li>
 *   <li>and round it at a crawl, for good, sinking as it goes to {@code hover}, its bombs' height.</li>
 * </ol>
 *
 * <p>Nose first along its way. Pure: where it is is worked out afresh for any moment.
 */
public record Raid(double from, Place start, double cruise, double x, double z, double hover, double orbit,
                   double speed) implements Course.Route {

    /** How fast it climbs or sinks, units a second. */
    public static final double CLIMB = 400;
    /** How fast it slows, coming near, units a second each second. */
    public static final double BRAKE = 40;
    /** How fast it goes round over its target: a crawl, its bombs falling near where they are let go. */
    public static final double CRAWL = 150;

    public Raid {
        if (!(orbit > 0) || !(speed >= CRAWL)) {
            throw new IllegalArgumentException("round " + orbit + " at " + speed + ": round something, no slower than a crawl");
        }
    }

    /** Where along it, {@code seconds} after the start. */
    @Override
    public Place at(final double seconds) {
        double t = Math.max(0, seconds - from);
        // up or down to its cruise, where it is
        final double rising = Math.abs(cruise - start.height()) / CLIMB;
        final Leg leg = leg();
        if (t < rising) {
            return new Place(start.x(), start.z(), start.height() + Math.signum(cruise - start.height()) * CLIMB * t,
                    leg.yaw());
        }
        t -= rising;
        // on toward the circle, at speed, then slowing to a crawl
        final double braking = Math.min(leg.length(), (speed * speed - CRAWL * CRAWL) / (2 * BRAKE));
        final double fast = speed;
        final double atSpeed = (leg.length() - braking) / fast;
        // the speed it starts slowing at: its own, unless it has not that far to go
        final double slowing = Math.sqrt(CRAWL * CRAWL + 2 * BRAKE * braking);
        final double slowFor = (slowing - CRAWL) / BRAKE;
        if (t < atSpeed + slowFor) {
            final double along = t < atSpeed ? fast * t
                    : leg.length() - braking + slowing * (t - atSpeed) - BRAKE * (t - atSpeed) * (t - atSpeed) / 2;
            final double share = leg.length() > 0 ? along / leg.length() : 1;
            return new Place(start.x() + (leg.toX() - start.x()) * share, start.z() + (leg.toZ() - start.z()) * share,
                    cruise, leg.yaw());
        }
        t -= atSpeed + slowFor;
        // and round, at a crawl, sinking to its hover
        final double angle = leg.angle() + leg.sense() * CRAWL * t / orbit;
        final double height = cruise + Math.signum(hover - cruise) * Math.min(Math.abs(hover - cruise), CLIMB * t);
        final double px = x + orbit * Math.cos(angle);
        final double pz = z + orbit * Math.sin(angle);
        // its way round: square to the line out from the middle, the way it goes
        final double wx = -Math.sin(angle) * leg.sense();
        final double wz = Math.cos(angle) * leg.sense();
        return new Place(px, pz, height, Math.toDegrees(Math.atan2(wx, wz)));
    }

    /** Where its run in ends, on the circle; which way it heads; where on the circle that is, and which way round it goes on. */
    private record Leg(double toX, double toZ, double length, double yaw, double angle, double sense) {
    }

    /**
     * Its run in: from where it starts to where a line from there meets the circle square on (on the
     * circle's right-hand side, seen from there); straight out to the circle, if it starts within it.
     */
    private Leg leg() {
        final double dx = start.x() - x;
        final double dz = start.z() - z;
        final double d = Math.hypot(dx, dz);
        final double out = Math.atan2(dz, dx);
        final double meet;
        if (d > orbit) {
            meet = out + Math.acos(orbit / d);
        } else {
            meet = d > 0 ? out : 0;
        }
        final double toX = x + orbit * Math.cos(meet);
        final double toZ = z + orbit * Math.sin(meet);
        final double length = Math.hypot(toX - start.x(), toZ - start.z());
        final double wx = length > 0 ? (toX - start.x()) / length : -Math.sin(meet);
        final double wz = length > 0 ? (toZ - start.z()) / length : Math.cos(meet);
        // round the way it was going when it met the circle
        final double sense = wx * -Math.sin(meet) + wz * Math.cos(meet) >= 0 ? 1 : -1;
        return new Leg(toX, toZ, length, Math.toDegrees(Math.atan2(wx, wz)), meet, sense);
    }
}
