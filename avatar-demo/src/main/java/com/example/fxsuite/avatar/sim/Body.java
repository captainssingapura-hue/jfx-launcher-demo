package com.example.fxsuite.avatar.sim;

import java.util.ArrayList;
import java.util.List;

/**
 * Where the avatar is and how it is moving. Pure maths: no scene graph.
 *
 * <p>JavaFX's y axis points down, so the height above the ground is {@code -y}. The avatar
 * faces along {@code (sin yaw, 0, cos yaw)}, so a positive turn goes right, matching
 * {@code Rotate(yaw, Y_AXIS)} on its node; its right is {@code (cos yaw, 0, -sin yaw)}.
 *
 * <p>It moves ahead and sideways (strafing) without turning; turning is separate, by however
 * much the controls say, and {@code turnRate} only records how fast that has been, for
 * banking into turns. Speeds ease toward what the controls ask for, exponentially, so the
 * result barely depends on how the time is split into frames. On its feet it grips and speed
 * follows quickly; in the air momentum carries.
 *
 * <p>Solid {@link Obstacles} stop it: to the avatar they are an upright cylinder,
 * {@link #RADIUS} round and {@link #TALL} high. It stands on whatever is under its feet, the
 * ground or the top of a block; steps up onto a ledge its feet come within {@link #STEP} of,
 * so a jump onto a crate lands on it; falls when it walks off an edge; stops climbing when its
 * head meets an underside; and slides along whatever it runs into, losing the speed that went
 * into it. Each step moves in sub-steps of at most {@link #SUBSTEP}, so not even a boost at a
 * slow frame rate carries it through a thin bar.
 *
 * <p>A jump is a state of its own, from leaving its feet to landing, whether by jumping, walking
 * off an edge or dropping out of flight. Its momentum then keeps to the heading it left on,
 * {@code travel}, whatever way it is turned to face: turning in mid-air turns only the body,
 * never the path. The keys can still nudge the path, gently and reckoned along that same
 * heading, and letting go of them keeps the momentum rather than braking it. On landing,
 * walking goes back to following the facing.
 *
 * @param travel   the heading that {@code speed} and {@code drift} are reckoned along: the
 *                 facing, except in a jump, when it is the heading it left its feet on
 * @param speed    world units per second along {@code travel} (negative backs up)
 * @param drift    world units per second to the right of {@code travel} (negative to the left)
 * @param turnRate degrees per second, right when positive, smoothed
 * @param climb    world units per second upward
 * @param onGround standing on something: the ground or a block
 * @param phase    the walk cycle, in radians: advances with distance covered on its feet
 * @param time     seconds since the start, for idle and hover motion
 */
public record Body(Vec3 position, double yaw, double travel, double speed, double drift, double turnRate,
                   double climb, boolean flying, boolean onGround, double phase, double time) {

    /** World units per second. 100 units is a metre. */
    public static final double WALK = 320;
    public static final double RUN = 760;
    public static final double FLY = 1100;
    public static final double BOOST = 2000;
    /** Sideways is slower than ahead, by this much. */
    public static final double SIDESTEP = 0.75;
    /** A brisk turn, degrees per second: the most the body banks or leans into. */
    public static final double TURN = 140;
    /** Climb or descent while flying, units per second. */
    public static final double LIFT = 800;
    public static final double GRAVITY = 2600;
    public static final double JUMP = 950;
    /** The world is a square this far from the centre each way, and this tall. */
    public static final double EDGE = 12000;
    public static final double CEILING = 5000;

    /** The avatar's size against obstacles: an upright cylinder. */
    public static final double RADIUS = 32;
    public static final double TALL = 200;
    /** A ledge no higher than this above the feet is stepped up onto. */
    public static final double STEP = 20;
    /** How far the feet must reach onto a top to stand on it: less than the full radius, so it slips off edges. */
    public static final double FOOTING = 16;
    /** The most it moves in one sub-step, in any direction. */
    public static final double SUBSTEP = 20;
    /** How quickly the keys can nudge a jump's path: gently, next to {@code 10} on its feet. */
    public static final double AIR_GRIP = 3;
    /** Losing more speed than this against something is a bump; less is just leaning on it. */
    public static final double BUMP = 150;
    /** Moving slower than this across the ground, the feet shuffle without a footfall. */
    public static final double SHUFFLE = 20;

    public static Body standing(final Vec3 at, final double yaw) {
        return new Body(at, yaw, yaw, 0, 0, 0, 0, false, true, 0, 0);
    }

    public double height() {
        return -position.y();
    }

    /**
     * Where this body is {@code share} of the way on from {@code before} (0 there, 1 here): for
     * drawing between two steps of the {@link Clock}. Turns go the short way round, and the walk
     * cycle across its wrap; what is or is not (flying, on its feet) is as it is here.
     */
    public Body between(final Body before, final double share) {
        final double s = Num.clamp(share, 0, 1);
        if (s >= 1) {
            return this;
        }
        final Vec3 at = before.position.add(position.subtract(before.position).multiply(s));
        final double cycle = 2 * Math.PI;
        double walked = phase - before.phase;
        walked -= cycle * Math.round(walked / cycle);
        return new Body(at, Num.wrap(before.yaw + Num.wrap(yaw - before.yaw) * s),
                Num.wrap(before.travel + Num.wrap(travel - before.travel) * s),
                before.speed + (speed - before.speed) * s, before.drift + (drift - before.drift) * s,
                before.turnRate + (turnRate - before.turnRate) * s, before.climb + (climb - before.climb) * s,
                flying, onGround, ((before.phase + walked * s) % cycle + cycle) % cycle,
                before.time + (time - before.time) * s);
    }

    /** On its feet: not flying, and standing on something. */
    public boolean grounded() {
        return !flying && onGround;
    }

    /** In a jump: off its feet and not flying, so its path is its own, whichever way it faces. */
    public boolean inJump() {
        return !flying && !onGround;
    }

    /** How fast it is going across the ground, ahead and sideways together. */
    public double groundSpeed() {
        return Math.hypot(speed, drift);
    }

    public Vec3 heading() {
        return Num.heading(yaw);
    }

    /** How hard the jet is firing, 0 to 1: nothing unless flying, and harder to go fast or climb. */
    public double thrust() {
        if (!flying) {
            return 0;
        }
        final double pace = Num.clamp(speed / BOOST, -0.3, 1);
        final double up = Num.clamp(climb / LIFT, -1, 1);
        return Num.clamp(0.35 + 0.65 * Math.max(Math.max(pace, up), Math.abs(drift) / BOOST), 0.2, 1);
    }

    /** One step's outcome: where the body is now, and what happened on the way. */
    public record Moved(Body body, List<Event> events) {

        public Moved {
            events = List.copyOf(events);
        }
    }

    /** After {@code seconds} of the given controls, on open ground. */
    public Body step(final Controls controls, final double seconds) {
        return move(controls, seconds, Obstacles.NONE).body();
    }

    /** After {@code seconds} of the given controls, among the given obstacles. */
    public Body step(final Controls controls, final double seconds, final Obstacles world) {
        return move(controls, seconds, world).body();
    }

    /**
     * After {@code seconds} of the given controls, among the given obstacles, with what happened
     * on the way: each foot that came down, a landing and how hard, a bump and how hard.
     */
    public Moved move(final Controls controls, final double seconds, final Obstacles world) {
        return move(controls, seconds, world, null);
    }

    /**
     * As {@link #move(Controls, double, Obstacles)}, with {@code wings} spread from the back
     * (null for none): they stop against what they meet, as the body does, so the whole body is
     * pushed clear of whatever a wing runs into.
     */
    public Moved move(final Controls controls, final double seconds, final Obstacles world, final Wings wings) {
        final List<Event> events = new ArrayList<>();
        double bump = 0;
        final boolean leaping = inJump();
        // out of a jump, the momentum is reckoned along the facing again
        final double[] own = leaping ? new double[] {speed, drift} : reframed(speed, drift, travel, yaw);
        final double newYaw = Num.wrap(yaw + controls.turn());
        final double newTurn = seconds > 0 ? Num.ease(turnRate, controls.turn() / seconds, 8, seconds) : turnRate;
        // in a jump the path keeps to the heading it left on; otherwise it follows the facing
        final double frame = leaping ? travel : newYaw;

        final double top = flying ? (controls.run() ? BOOST : FLY) : (controls.run() ? RUN : WALK);
        // backing up is slower than going ahead
        final double wanted = controls.forward() * top * (controls.forward() < 0 ? 0.5 : 1);
        final double wantedDrift = controls.sideways() * top * SIDESTEP;
        final double newSpeed;
        final double newDrift;
        if (leaping) {
            newSpeed = nudged(own[0], wanted, seconds);
            newDrift = nudged(own[1], wantedDrift, seconds);
        } else {
            // flying, it holds its place as a drone does: it brakes briskly once let go
            final double grip = flying ? 4 : 10;
            newSpeed = Num.ease(own[0], wanted, grip, seconds);
            newDrift = Num.ease(own[1], wantedDrift, grip, seconds);
        }
        double newClimb = flying
                ? Num.ease(climb, controls.lift() * LIFT * (controls.run() ? 1.6 : 1), 4, seconds)
                : climb - GRAVITY * seconds;

        final Vec3 ahead = Num.heading(frame);
        final Vec3 right = Num.right(frame);
        double vx = ahead.x() * newSpeed + right.x() * newDrift;
        double vz = ahead.z() * newSpeed + right.z() * newDrift;
        // the average climb over the frame: exact under constant gravity, so a jump reaches the
        // same height whatever the frame rate
        double rise = (climb + newClimb) / 2;

        final double reach = Math.max(Math.hypot(vx, vz), Math.abs(rise)) * seconds;
        // with wings out, finer steps: a wing is thin, and must not get into a bar between them
        final int substeps = Math.max(1, (int) Math.ceil(reach / (wings == null ? SUBSTEP : Wings.STEP)));
        final double dt = seconds / substeps;
        double x = position.x();
        double z = position.z();
        double height = height();
        boolean stillFlying = flying;
        boolean footing = false;
        Surface underfoot = Surface.GROUND;
        for (int i = 0; i < substeps; i++) {
            final double before = height;
            x = Num.clamp(x + vx * dt, -EDGE, EDGE);
            z = Num.clamp(z + vz * dt, -EDGE, EDGE);
            height += rise * dt;

            // the head meets an underside it was below
            final double ceiling = Math.min(CEILING + TALL,
                    world.ceiling(x, z, RADIUS, before + TALL - 1e-6));
            if (height + TALL > ceiling) {
                height = ceiling - TALL;
                bump = Math.max(bump, rise);
                rise = Math.min(0, rise);
                newClimb = Math.min(0, newClimb);
            }
            // the feet meet a top they were above, or within a step of
            final Obstacles.Footing under = world.footing(x, z, FOOTING, before + STEP);
            final double floor = under.height();
            if (height <= floor) {
                height = floor;
                // a flight ends on touching down; flying level over a low ledge just lifts over it
                if (!stillFlying || rise < 0) {
                    if (!onGround && !footing) {
                        events.add(new Event.Landed(Math.max(0, -rise), stillFlying, under.surface()));
                    }
                    stillFlying = false;
                    footing = true;
                    underfoot = under.surface();
                    rise = 0;
                    newClimb = 0;
                }
            }
            // and everything else in the way is slid along; a ledge within a step is left alone,
            // to be stepped up onto once the feet are over it
            final Obstacles.Contact contact = world.slide(x, z, vx, vz, RADIUS, height + STEP, height + TALL);
            bump = Math.max(bump, Math.hypot(vx - contact.vx(), vz - contact.vz()));
            x = contact.x();
            z = contact.z();
            vx = contact.vx();
            vz = contact.vz();
            // and each wing, a stroke behind the shoulders: where one is pushed clear of
            // something, the whole body goes with it
            if (wings != null) {
                final Vec3 side = Num.right(newYaw);
                final Vec3 back = Num.heading(newYaw).multiply(-1);
                for (final Wings.Span span : wings.spans()) {
                    final double bx = x + back.x() * span.behind();
                    final double bz = z + back.z() * span.behind();
                    final Obstacles.Shift wing = world.slide(
                            bx + side.x() * span.from(), bz + side.z() * span.from(),
                            bx + side.x() * span.to(), bz + side.z() * span.to(),
                            vx, vz, Wings.REACH, height + span.bottom(), height + span.top());
                    if (wing.blocked()) {
                        bump = Math.max(bump, Math.hypot(vx - wing.vx(), vz - wing.vz()));
                        x += wing.dx();
                        z += wing.dz();
                        vx = wing.vx();
                        vz = wing.vz();
                    }
                }
            }
        }

        // what is left of the velocity, reckoned along the frame it moved in; still in a jump, it
        // keeps that frame, and otherwise it goes back to the facing
        final double keptSpeed = vx * ahead.x() + vz * ahead.z();
        final double keptDrift = vx * right.x() + vz * right.z();
        final boolean stillLeaping = !stillFlying && !footing;
        final double newTravel = stillLeaping ? frame : newYaw;
        final double[] kept = reframed(keptSpeed, keptDrift, frame, newTravel);
        final double ground = Math.hypot(kept[0], kept[1]);
        final double walked = footing ? phase + ground * seconds / stride(ground) * 2 * Math.PI : phase;
        if (footing && ground > SHUFFLE) {
            // a foot comes down where its leg swings furthest forward: the left at a quarter of
            // the cycle, the right at three quarters
            if (passes(phase, walked, Math.PI / 2)) {
                events.add(new Event.Footfall(true, underfoot));
            }
            if (passes(phase, walked, 3 * Math.PI / 2)) {
                events.add(new Event.Footfall(false, underfoot));
            }
        }
        if (bump > BUMP) {
            events.add(new Event.Bumped(bump));
        }
        final Body after = new Body(new Vec3(x, -height, z), newYaw, newTravel, kept[0], kept[1], newTurn, newClimb,
                stillFlying, footing, walked % (2 * Math.PI), time + seconds);
        return new Moved(after, events);
    }

    /** Whether the walk cycle, going from {@code from} on to {@code to}, passes the point {@code mark} of a turn. */
    private static boolean passes(final double from, final double to, final double mark) {
        return (from < mark && to >= mark) || (from < mark + 2 * Math.PI && to >= mark + 2 * Math.PI);
    }

    /** Up off whatever it stands on, if standing; otherwise unchanged. The jump keeps to its heading now. */
    public Body jumped() {
        return grounded()
                ? new Body(position, yaw, yaw, speed, drift, turnRate, JUMP, false, false, phase, time)
                : this;
    }

    /** Takes off with a little lift, or stops flying and falls, keeping its momentum. */
    public Body toggledFlight() {
        return flying
                ? new Body(position, yaw, travel, speed, drift, turnRate, climb, false, false, phase, time)
                : new Body(position, yaw, travel, speed, drift, turnRate, Math.max(climb, LIFT * 0.7), true, false,
                        phase, time);
    }

    /**
     * A jump's velocity along one axis after the keys have had their say: they can push it toward
     * what they ask for, but letting go never brakes it, and nor does asking for less than it has.
     */
    static double nudged(final double value, final double wanted, final double seconds) {
        if (wanted == 0 || (Math.signum(wanted) == Math.signum(value) && Math.abs(value) >= Math.abs(wanted))) {
            return value;
        }
        return Num.ease(value, wanted, AIR_GRIP, seconds);
    }

    /** A velocity of {@code speed} ahead and {@code drift} right along heading {@code from}, as seen along {@code to}. */
    static double[] reframed(final double speed, final double drift, final double from, final double to) {
        if (from == to) {
            return new double[] {speed, drift};
        }
        final Vec3 v = Num.heading(from).multiply(speed).add(Num.right(from).multiply(drift));
        return new double[] {v.dotProduct(Num.heading(to)), v.dotProduct(Num.right(to))};
    }

    /** One full walk cycle, two steps, in world units: longer strides when running. */
    static double stride(final double groundSpeed) {
        return 150 + 110 * Math.min(1, Math.abs(groundSpeed) / RUN);
    }
}
