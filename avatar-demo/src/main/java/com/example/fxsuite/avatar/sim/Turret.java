package com.example.fxsuite.avatar.sim;

import java.util.ArrayList;
import java.util.List;

/**
 * A gun on a mount, aimed freely: the other way to aim a gun from the hands' ({@link Arms}), where
 * the gun goes wherever what holds it faces. Its {@link Course.Mount} is bolted to what carries it,
 * piece {@code host} of the course, whose frame is {@code base}; the gun turns on it by itself.
 * Pure state, stepped with the rest of the world ({@link Play}); the firing is every gun's
 * ({@link Gun#fire}).
 *
 * <ul>
 *   <li><b>Aimed:</b> whoever mans it turns it with the mouse, round ({@code yaw}, from straight
 *       ahead of what carries it) and up and down ({@code pitch}), no further than its mount lets
 *       it: {@code wantYaw} and {@code wantPitch} are where it is asked to point, within those
 *       limits. Heavy, it swings there no faster than its mount's slew: a hand-held gun snaps
 *       round, a turret takes a moment. Asked further than its limits, it stops at them.</li>
 *   <li><b>Fired</b> with the trigger, as every gun is, from its muzzle at the end of its barrel,
 *       along the way it points. Its rounds go through what carries it, as if it were not there.</li>
 *   <li><b>Heat:</b> each round heats its barrels ({@link #HEAT}), and they cool all the time
 *       ({@link #COOL}). Hot through ({@code heat} at 1), it will not fire again until it has
 *       cooled to {@link #RECOVERED}: {@code cooling}.</li>
 *   <li><b>Manned</b> ({@code held}) from its station, within {@link #REACH} of it, on foot; let go
 *       the same way. Whoever mans it is at its seat: inside, out of sight, for a closed mount.</li>
 * </ul>
 */
public record Turret(int host, Course.Mount mount, Place base, double yaw, double pitch, double wantYaw,
                     double wantPitch, Gun.State gun, double heat, boolean cooling, boolean held) {

    /**
     * How much each round heats the barrels, and how fast they cool, every second: firing, they
     * heat faster than they cool, and are hot through after five seconds or so held down; and
     * cool from hot through to firing again in four.
     */
    public static final double HEAT = 0.012;
    public static final double COOL = 0.15;
    /** Hot through, they must cool to this before it fires again. */
    public static final double RECOVERED = 0.4;
    /** How near its station whoever mans it must be. */
    public static final double REACH = 130;

    /** Mounted on piece {@code host}, whose frame is {@code base}: straight ahead, level, cold, and unmanned. */
    public Turret(final int host, final Course.Mount mount, final Place base) {
        this(host, mount, base, 0, 0, 0, 0, Gun.State.READY, 0, false, false);
    }

    /** The guns mounted among {@code damage}'s pieces: one in each bunker, one on each gunship, and one on each car and pickup. */
    public static List<Turret> of(final Damage damage) {
        final List<Turret> turrets = new ArrayList<>();
        for (int piece = 0; piece < damage.size(); piece++) {
            final Course.Piece what = damage.piece(piece);
            if (what.kind() == Course.Kind.BUNKER) {
                turrets.add(new Turret(piece, Course.BUNKER_GUN, what.place()));
            } else if (what.kind() == Course.Kind.PLATFORM && Course.Deck.of(what.shape()).equals(Course.GUNSHIP)) {
                turrets.add(new Turret(piece, Course.GUNSHIP_GUN, what.place()));
            } else if (what.kind() == Course.Kind.CAR && Course.Chassis.of(what.shape()) == Course.CAR_CHASSIS) {
                turrets.add(new Turret(piece, Course.CAR_GUN, what.place()));
            } else if (what.kind() == Course.Kind.CAR && Course.Chassis.of(what.shape()) == Course.PICKUP_CHASSIS) {
                turrets.add(new Turret(piece, Course.REAR_GUN, what.place()));
            }
        }
        return turrets;
    }

    /** A turret a step on, what happened (its rounds, each {@link Event.Fired}), and the blows its rounds dealt. */
    public record Stepped(Turret turret, List<Event> events, List<Blow> blows) {

        public Stepped {
            events = List.copyOf(events);
            blows = List.copyOf(blows);
        }
    }

    /** Where {@code spot} of what carries it is, in the world. */
    public Vec3 at(final Course.Section spot) {
        return new Vec3(base.x(), -(base.height() + spot.up()), base.z())
                .add(Num.right(base.yaw()).multiply(spot.across()))
                .add(Num.heading(base.yaw()).multiply(spot.along()));
    }

    /** Where it turns about, in the world. */
    public Vec3 pivot() {
        return at(mount.pivot());
    }

    /** Which way it points round, in the world's terms. */
    public double heading() {
        return Num.wrap(base.yaw() + yaw);
    }

    /** The way it points: a unit vector. */
    public Vec3 line() {
        final double p = Math.toRadians(pitch);
        // y points down: up is -y
        return Num.heading(heading()).multiply(Math.cos(p)).add(0, -Math.sin(p), 0);
    }

    /** The end of its barrel, where its rounds leave from. */
    public Vec3 muzzle() {
        return pivot().add(line().multiply(mount.barrel()));
    }

    /** Where its rounds leave from and along which line, as every gun is told. */
    public Gun.Bore bore() {
        return new Gun.Bore(muzzle(), line(), Num.right(heading()));
    }

    /** Whether it is firing: up to speed, a round gone in the last moment. */
    public boolean firing() {
        return gun.spin() >= 1 && gun.sinceRight() < 2 * mount.gun().interval();
    }

    /** Whether {@code body} can man it where it stands: on its feet, at its station. */
    public boolean reaches(final Body body) {
        if (mount.fixed()) {
            return false;  // fired by whoever drives what carries it, not manned on its own
        }
        if (!body.grounded()) {
            return false;
        }
        // at its station, or at the seat it is manned from, if that is elsewhere
        final Vec3 feet = body.position();
        for (final Vec3 spot : List.of(at(mount.station()), at(mount.seat()))) {
            if (Math.hypot(feet.x() - spot.x(), feet.z() - spot.z()) <= REACH && Math.abs(feet.y() - spot.y()) <= Body.STEP * 3) {
                return true;
            }
        }
        return false;
    }

    /** The same, manned or let go. */
    public Turret held(final boolean manned) {
        return new Turret(host, mount, base, yaw, pitch, wantYaw, wantPitch, gun, heat, cooling, manned);
    }

    /**
     * The same, what carries it having moved to {@code to}: it goes with it. Stabilized, it keeps
     * pointing the way it did in the world, as far as it turns, as what carries it turns under it;
     * not, it turns with it.
     */
    public Turret carried(final Place to) {
        final double turn = mount.stabilized() ? Num.wrap(to.yaw() - base.yaw()) : 0;
        return new Turret(host, mount, to, traversed(yaw - turn), pitch, traversed(wantYaw - turn), wantPitch, gun,
                heat, cooling, held);
    }

    /** As {@link #carried(Place)}, what carries it tipped {@code tilt} degrees nose up: a fixed gun is tipped with it. */
    public Turret carried(final Place to, final double tilt) {
        final Turret carried = carried(to);
        return !mount.fixed() ? carried : new Turret(host, mount, to, 0, tilt, 0, tilt, gun, heat, cooling, held);
    }

    /** {@code wanted} round, no further than it turns either way; all round, any way. */
    private double traversed(final double wanted) {
        return mount.traverse() >= 180 ? Num.wrap(wanted) : Num.clamp(wanted, -mount.traverse(), mount.traverse());
    }

    /**
     * {@code seconds} on: aimed by {@code controls} if manned (the mouse's turn round, its aim up
     * and down), swung that way no faster than its slew, and fired by the trigger into
     * {@code world} (what its rounds can strike: not what carries it), unless it is too hot.
     */
    public Stepped step(final Controls controls, final double seconds, final Obstacles world) {
        return step(controls, seconds, world, held);
    }

    /**
     * As {@link #step(Controls, double, Obstacles)}, fired and aimed by {@code controls} if
     * {@code firer}: whoever mans it, or, for a fixed gun, whoever drives what carries it. A fixed
     * gun is not aimed at all: it points the way it was carried to.
     */
    public Stepped step(final Controls controls, final double seconds, final Obstacles world, final boolean firer) {
        final boolean aimed = firer && !mount.fixed();
        final double wantedYaw = aimed ? traversed(wantYaw + controls.turn()) : wantYaw;
        final double wantedPitch = aimed ? Num.clamp(controls.aim(), -mount.depression(), mount.elevation())
                : mount.fixed() ? pitch : wantPitch;
        final double most = mount.slew() * seconds;
        final double nowYaw = mount.fixed() ? 0 : traversed(yaw + Num.clamp(Num.wrap(wantedYaw - yaw), -most, most));
        final double nowPitch = mount.fixed() ? pitch : pitch + Num.clamp(wantedPitch - pitch, -most, most);
        final Turret swung = new Turret(host, mount, base, nowYaw, nowPitch, wantedYaw, wantedPitch, gun, heat,
                cooling, held);
        final boolean trigger = firer && controls.trigger() && !cooling;
        final Gun.Fired fired = mount.gun().fire(gun, trigger, seconds, left -> swung.bore(), world);
        final int rounds = fired.state().rounds() - gun.rounds();
        // a gun that does not overheat stays cold
        double hot = mount.overheats() ? Math.max(0, heat + rounds * HEAT - COOL * seconds) : 0;
        boolean cool = cooling;
        if (hot >= 1) {
            hot = 1;
            cool = true;
        } else if (cool && hot <= RECOVERED) {
            cool = false;
        }
        return new Stepped(new Turret(host, mount, base, nowYaw, nowPitch, wantedYaw, wantedPitch, fired.state(), hot,
                cool, held), fired.events(), fired.blows());
    }
}
