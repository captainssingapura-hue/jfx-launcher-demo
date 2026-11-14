package com.example.fxsuite.avatar.sim;

import java.util.ArrayList;
import java.util.List;

/**
 * A motorcycle with a sidecar, as it stands on its three wheels, and how hard it can be turned
 * before it tips: a simple rule, worked out once into a table, looked up as it is ridden.
 *
 * <p><b>Its weight</b> is a few masses ({@link #masses}): the motorcycle and its rider; the
 * sidecar's tubes, where its {@link Frame} has its mass; the sidecar's body and wheel, out over it;
 * and a passenger in it, if any. Together they have a centre ({@link #centre}), out toward the
 * sidecar and some way up.
 *
 * <p><b>It stands</b> on a triangle: the motorcycle's two wheels in line, and the sidecar's out to
 * the side. Going round, the turn pulls its weight outward, as hard as the speed squared over the
 * turn's radius; it tips when that pull, against gravity, carries its weight's line out past an
 * edge of the triangle: as far as its centre is from that edge, against how high it is, times
 * gravity ({@link #limit}). Turning toward the sidecar, the pull is away from it, over the
 * motorcycle's own wheels, and its centre is close to them: the sidecar lifts first, and soon
 * ("flying the chair"). Turning away from it, the pull is toward the sidecar, over the line from the
 * motorcycle's front wheel to the sidecar's, much further off: it is all but untippable that way.
 *
 * <p><b>The table</b> ({@link #table}): for each speed, the sharpest it may be steered either way
 * before it tips, a turn's radius being the wheelbase over the tangent of the steer. Ridden, the
 * steer is held against the table's ({@link #grip}): well within it, steady; past
 * {@link #LIFTS} of it, the sidecar's wheel (or the motorcycle's back one) lifting; past it, tipping.
 * A passenger in the sidecar, as weight out over it, lets it be turned toward it harder.
 *
 * @param bike      the motorcycle
 * @param sidecar   its sidecar
 * @param passenger the passenger in the sidecar, in kilograms; 0 for none
 */
public record Outfit(Course.Chassis bike, Sidecar sidecar, double passenger) {

    /** Gravity, in centimetres a second a second. */
    public static final double GRAVITY = 981;
    /** The sidecar's body, a tub, and its wheel, in kilograms: as the tub has it ({@link Sidecar.Body}). */
    public static final double CHAIR = Sidecar.Body.TUB.kg();
    /** Past this much of the limit, a wheel lifts. */
    public static final double LIFTS = 0.85;
    /**
     * How its bars are turned, gently, the more so the faster it goes: toward the sidecar, they take
     * {@code ROLL_IN} seconds to come round to the sharpest it may be steered at its speed, and go
     * no further than {@code LOCK} times that, so held over it still tips, but a touch does not;
     * away from it, no further than {@code AWAY} of its limit that way, as the tyres would slide
     * long before it tipped so; and they are let back {@code BACK_OFF} times as fast as they turn in.
     */
    public static final double ROLL_IN = 0.5;
    public static final double LOCK = 1.6;
    public static final double AWAY = 0.95;
    public static final double BACK_OFF = 3;
    /** Its bars never turn slower than this, in degrees a second. */
    public static final double SLOWEST = 1;
    /** The table's speeds: every 10 km/h, to the motorcycle's top. */
    public static final double STEP = 1000 / 3.6;

    /** A mass, in kilograms, and where it is: across (right), along, up. */
    public record Mass(String name, double kg, double across, double along, double up) {
    }

    /** At speed {@code speed} (cm/s), the sharpest it may be steered, in degrees, toward its sidecar and away. */
    public record Row(double speed, double toward, double away) {
    }

    /** How it is going round: steady; a wheel lifting; or tipping over. */
    public enum Grip { STEADY, LIFTING, TIPPING }

    /** Past its limit for this long, in seconds, it goes over. */
    public static final double TIPS = 0.25;

    /** The boxer, its sidecar as it is set up, no passenger. */
    public static final Outfit BOXER = new Outfit(Course.BOXER, Sidecar.BOXER, 0);

    /** Each outfit's table, by its chassis: worked out once, the first time it is wanted. */
    private static final java.util.Map<Course.Chassis, List<Row>> TABLES =
            new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * The outfit a sidecar outfit's chassis is: the motorcycle it is built on (whose weight is its
     * own and its rider's) with its sidecar, no passenger. The boxer's, for the boxer with its sidecar.
     */
    public static Outfit of(final Course.Chassis outfit) {
        if (outfit.sidecar() == null) {
            throw new IllegalArgumentException(outfit.name() + " has no sidecar");
        }
        return outfit.sidecar() == Sidecar.BOXER ? BOXER : new Outfit(Course.BOXER, outfit.sidecar(), 0);
    }

    /** The table of chassis {@code outfit}'s outfit ({@link #of}): worked out once, then looked up. */
    public static List<Row> table(final Course.Chassis outfit) {
        return TABLES.computeIfAbsent(outfit, c -> of(c).table());
    }

    /** Its masses: the motorcycle with its rider, the sidecar's tubes, its body and wheel, and any passenger. */
    public List<Mass> masses() {
        final double side = sidecar.side().sign();
        final List<Mass> all = new ArrayList<>();
        // the motorcycle, its engine low, its rider high: about where a big cruiser's centre is, rider aboard
        all.add(new Mass("the motorcycle and its rider", bike.drive().mass(), 0, -15, 58));
        final Frame tubes = sidecar.placed();
        final Frame.Point centre = tubes.centre();
        all.add(new Mass("the sidecar's tubes", tubes.mass(), centre.across(), centre.along(), centre.up()));
        all.add(new Mass("the sidecar's " + sidecar.body().title() + " and wheel", sidecar.body().kg(),
                side * sidecar.track() * 0.7, sidecar.along() + 35, sidecar.body().up()));
        if (passenger > 0) {
            all.add(new Mass("its passenger", passenger, side * sidecar.track() * 0.68, sidecar.along() + 20, 65));
        }
        return all;
    }

    /** How heavy it is, all told, in kilograms. */
    public double mass() {
        return masses().stream().mapToDouble(Mass::kg).sum();
    }

    /** Where its weight is: the centre of all its masses. */
    public Mass centre() {
        double kg = 0;
        double across = 0;
        double along = 0;
        double up = 0;
        for (final Mass m : masses()) {
            kg += m.kg();
            across += m.kg() * m.across();
            along += m.kg() * m.along();
            up += m.kg() * m.up();
        }
        return new Mass("its weight", kg, across / kg, along / kg, up / kg);
    }

    /** Where its three wheels touch the ground, across and along: the motorcycle's front, its back, the sidecar's. */
    public double[][] wheels() {
        return new double[][] {{0, bike.axles().getFirst()}, {0, bike.axles().getLast()},
                {sidecar.side().sign() * sidecar.track(), sidecar.along()}};
    }

    /**
     * The hardest it can be pulled sideways, in cm/s², toward the side {@code toward} (+1 its right,
     * -1 its left) before it tips: how far its centre, carried that way, goes before it leaves the
     * triangle its wheels stand on, against how high it is, times gravity.
     */
    public double limit(final double toward) {
        final Mass centre = centre();
        final double[][] w = wheels();
        double reach = Double.POSITIVE_INFINITY;
        for (int e = 0; e < 3; e++) {
            final double[] a = w[e];
            final double[] b = w[(e + 1) % 3];
            // where a line across from the centre crosses this edge, if it does, and how far out
            final double run = b[1] - a[1];
            if (Math.abs(run) < 1e-9) {
                continue;
            }
            final double t = (centre.along() - a[1]) / run;
            if (t < 0 || t > 1) {
                continue;
            }
            final double across = a[0] + t * (b[0] - a[0]);
            final double out = (across - centre.across()) * toward;
            if (out >= 0) {
                reach = Math.min(reach, out);
            }
        }
        return GRAVITY * reach / centre.up();
    }

    /** Which way the pull goes, turning with {@code steer} (degrees, right when positive): outward, the other way. */
    private static double pull(final double steer) {
        return -Math.signum(steer);
    }

    /** Whether steering {@code steer} turns toward its sidecar. */
    public boolean towardSidecar(final double steer) {
        return Math.signum(steer) == sidecar.side().sign();
    }

    /** The sharpest it may be steered at {@code speed} (cm/s), toward the side {@code steer}'s sign says, in degrees. */
    public double sharpest(final double speed, final double toward) {
        final double most = bike.drive().steer();
        if (speed < 1) {
            return most;
        }
        final double limit = limit(pull(toward));
        return Math.min(most, Math.toDegrees(Math.atan(bike.wheelbase() * limit / (speed * speed))));
    }

    /** For each speed from 10 km/h to its top, every 10 km/h, the sharpest it may be steered either way. */
    public List<Row> table() {
        final List<Row> rows = new ArrayList<>();
        final double side = sidecar.side().sign();
        for (double speed = STEP; speed <= bike.drive().top() + STEP / 2; speed += STEP) {
            rows.add(new Row(speed, sharpest(speed, side), sharpest(speed, -side)));
        }
        return List.copyOf(rows);
    }

    /** The sharpest it may be steered at {@code speed}, toward its sidecar or away, from {@code table}, between its rows. */
    public static double lookUp(final List<Row> table, final double speed, final boolean toward, final double most) {
        if (speed <= table.getFirst().speed()) {
            // slower than the table goes: no sharper than at its first row, eased up to the steering's most at a stop
            final Row first = table.getFirst();
            final double at = toward ? first.toward() : first.away();
            final double t = Math.max(0, speed) / first.speed();
            return most + (at - most) * t;
        }
        for (int r = 1; r < table.size(); r++) {
            final Row above = table.get(r);
            if (speed <= above.speed()) {
                final Row below = table.get(r - 1);
                final double t = (speed - below.speed()) / (above.speed() - below.speed());
                final double from = toward ? below.toward() : below.away();
                final double to = toward ? above.toward() : above.away();
                return from + (to - from) * t;
            }
        }
        final Row last = table.getLast();
        return toward ? last.toward() : last.away();
    }

    /**
     * How it is going round at {@code speed} (cm/s), steered {@code steer} degrees (right when
     * positive), held against {@code table}: steady; lifting, past {@link #LIFTS} of the sharpest it
     * may be steered that way; tipping, past it.
     */
    public Grip grip(final List<Row> table, final double speed, final double steer) {
        if (Math.abs(steer) < 1e-9) {
            return Grip.STEADY;
        }
        final double sharpest = lookUp(table, Math.abs(speed), towardSidecar(steer), bike.drive().steer());
        final double share = Math.abs(steer) / sharpest;
        return share > 1 ? Grip.TIPPING : share > LIFTS ? Grip.LIFTING : Grip.STEADY;
    }

    /** How far toward its limit, 0 to past 1, going round at {@code speed} steered {@code steer}: what the bench shows it lifting by. */
    public double share(final List<Row> table, final double speed, final double steer) {
        if (Math.abs(steer) < 1e-9) {
            return 0;
        }
        return Math.abs(steer) / lookUp(table, Math.abs(speed), towardSidecar(steer), bike.drive().steer());
    }

    /**
     * The furthest its bars may be turned at {@code speed} (cm/s), toward the side {@code steer}'s
     * sign says, from {@code table}: no further than {@code most}, as any land vehicle's at that
     * speed; toward its sidecar, no further than {@link #LOCK} times the sharpest it may be steered
     * that way; away, no further than {@link #AWAY} of it.
     */
    public double lock(final List<Row> table, final double speed, final double steer, final double most) {
        final boolean toward = towardSidecar(steer);
        final double sharpest = lookUp(table, Math.abs(speed), toward, bike.drive().steer());
        return Math.min(most, (toward ? LOCK : AWAY) * sharpest);
    }

    /**
     * How fast its bars may be turned at {@code speed} (cm/s), toward the side {@code steer}'s sign
     * says, in degrees a second, from {@code table}: round to the sharpest it may be steered that way
     * in {@link #ROLL_IN} seconds, the slower the faster it goes; no faster than {@code fastest},
     * nor slower than {@link #SLOWEST}.
     */
    public double rate(final List<Row> table, final double speed, final double steer, final double fastest) {
        final double sharpest = lookUp(table, Math.abs(speed), towardSidecar(steer), bike.drive().steer());
        return Math.clamp(sharpest / ROLL_IN, SLOWEST, fastest);
    }

    /** The same, with {@code kg} in the sidecar. */
    public Outfit carrying(final double kg) {
        return new Outfit(bike, sidecar, Math.max(0, kg));
    }

    /** The same, its sidecar {@code changed}: shaped, set up, on the other side. */
    public Outfit with(final Sidecar changed) {
        return new Outfit(bike, changed, passenger);
    }
}
