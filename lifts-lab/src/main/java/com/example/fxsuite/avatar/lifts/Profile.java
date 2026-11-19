package com.example.fxsuite.avatar.lifts;

/**
 * How a fast lift goes from standing to standing: its acceleration never more than {@code accel}, its speed never more
 * than {@code speed}, and its acceleration itself changing no faster than {@code jerk}, so it eases into gathering
 * speed and out of it, as a comfortable lift does. Over any distance: easing in, gathering speed, easing out of it,
 * going at its speed if it reaches it, and the same backwards to stop; a short distance never reaching its speed, a
 * very short one never its acceleration.
 *
 * <p>Seven stretches, each of a constant jerk (+, 0, −, 0, −, 0, +), worked out exactly: where it is at any moment
 * is the sum of the stretches before, integrated.
 *
 * @param speed its top speed, cm/s
 * @param accel its greatest acceleration, cm/s²
 * @param jerk  how fast its acceleration changes at most, cm/s³
 */
public record Profile(double speed, double accel, double jerk) {

    public Profile {
        if (!(speed > 0 && accel > 0 && jerk > 0)) {
            throw new IllegalArgumentException("a lift's profile going, gathering speed, easing");
        }
    }

    /** Its seven stretches over {@code distance}: how long each, at what jerk. */
    private double[][] stretches(final double distance) {
        final double d = Math.abs(distance);
        if (d == 0) {
            return new double[0][];
        }
        // the greatest speed it reaches: its own if the distance lets it, else as much as gathering and losing it allow
        double peak = speed;
        if (2 * gathering(peak) > d) {
            double lo = 0;
            double hi = speed;
            for (int k = 0; k < 80; k++) {
                final double mid = (lo + hi) / 2;
                if (2 * gathering(mid) > d) {
                    hi = mid;
                } else {
                    lo = mid;
                }
            }
            peak = lo;
        }
        final double a = Math.min(accel, Math.sqrt(peak * jerk));
        final double easing = a / jerk;
        final double steady = Math.max(0, (peak - a * easing) / a);
        final double cruise = Math.max(0, (d - 2 * gathering(peak)) / Math.max(peak, 1e-9));
        return new double[][] {
            {easing, jerk}, {steady, 0}, {easing, -jerk}, {cruise, 0}, {easing, -jerk}, {steady, 0}, {easing, jerk}};
    }

    /** How far it goes gathering speed from standing to {@code v}. */
    private double gathering(final double v) {
        if (v <= 0) {
            return 0;
        }
        final double a = Math.min(accel, Math.sqrt(v * jerk));
        final double easing = a / jerk;
        final double steady = Math.max(0, (v - a * easing) / a);
        // time to gather it, and the speed's average over it being half of it
        return v * (2 * easing + steady) / 2;
    }

    /** Its run over {@code distance}, worked out once: to be asked where it is as often as wanted. */
    Run run(final double distance) {
        return new Run(distance, stretches(distance));
    }

    /** How long it takes to go {@code distance}, from standing to standing. */
    public double time(final double distance) {
        return run(distance).time();
    }

    /** How far it has gone of {@code distance}, {@code t} after starting, from standing to standing. */
    public double gone(final double distance, final double t) {
        return run(distance).gone(t);
    }

    /** How fast it is going, {@code t} after starting over {@code distance}. */
    public double speedAt(final double distance, final double t) {
        return run(distance).speedAt(t);
    }

    /** A run over {@code distance}, its stretches worked out: how long it takes, where it is and how fast at a moment. */
    public static final class Run {

        private final double distance;
        private final double[][] stretches;
        private final double time;

        private Run(final double distance, final double[][] stretches) {
            this.distance = distance;
            this.stretches = stretches;
            double total = 0;
            for (final double[] s : stretches) {
                total += s[0];
            }
            this.time = total;
        }

        /** How long it takes, from standing to standing. */
        public double time() {
            return time;
        }

        /** How far it has gone, {@code t} after starting. */
        public double gone(final double t) {
            if (t >= time) {
                return distance;
            }
            double x = 0;
            double v = 0;
            double a = 0;
            double left = Math.max(0, t);
            for (final double[] s : stretches) {
                final double dt = Math.min(left, s[0]);
                final double j = s[1];
                x += v * dt + a * dt * dt / 2 + j * dt * dt * dt / 6;
                v += a * dt + j * dt * dt / 2;
                a += j * dt;
                left -= dt;
                if (left <= 0) {
                    break;
                }
            }
            return Math.signum(distance) * Math.min(x, Math.abs(distance));
        }

        /** How fast it is going, {@code t} after starting. */
        public double speedAt(final double t) {
            double v = 0;
            double a = 0;
            double left = Math.max(0, t);
            for (final double[] s : stretches) {
                final double dt = Math.min(left, s[0]);
                v += a * dt + s[1] * dt * dt / 2;
                a += s[1] * dt;
                left -= dt;
                if (left <= 0) {
                    break;
                }
            }
            return Math.max(0, v);
        }
    }
}
