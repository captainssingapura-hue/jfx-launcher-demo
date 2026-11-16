package com.example.fxsuite.avatar.sim;

import java.util.ArrayList;
import java.util.List;

/**
 * What drives a hinge between shut and open ({@link #least} and {@link #most}, degrees), at a rate
 * that can be set ({@link #rate}, degrees a second), in stages, each change of stage an
 * {@link Event} for its sound. Pure: the same state and commands, the same steps; for a door on a
 * bench as in the world.
 *
 * <p>Driven hydraulically ({@link Drive#HYDRAULIC}), as a heavy door is on an airship:
 * <ul>
 *   <li><b>Opening</b> from shut: its pump starts and spools up; its lock pin is drawn; its valve
 *       opens and it moves, easing up to its rate; near its end it is cushioned, slowing; it stops
 *       against its end, held there; its pump runs down.</li>
 *   <li><b>Closing</b>: its pump starts; its valve opens the other way and it moves, cushioned as it
 *       nears shut; it stops, and its lock pin drops in, locking it; its pump runs down.</li>
 *   <li>Asked the other way while moving, its valve switches over and it slows and turns back;
 *       asked to shut while its lock is still being drawn, the pin drops straight back. Put part-way
 *       by hand ({@link #placed}), it is held there until asked, then goes whichever way it is.</li>
 * </ul>
 *
 * @param drive   how it is driven
 * @param least   shut, degrees
 * @param most    open
 * @param rate    how fast it moves at most, degrees a second
 * @param angle   where it is
 * @param open    whether it is asked to be open
 * @param stage   what it is doing
 * @param timer   how long it has been at its stage, seconds
 * @param speed   how fast it moves, degrees a second, opening when positive
 * @param spool   how fast its pump turns, 0 (stopped) to 1 (running)
 * @param cushion whether it is being cushioned toward its end
 */
public record Actuator(Drive drive, double least, double most, double rate, double angle, boolean open, Stage stage,
                       double timer, double speed, double spool, boolean cushion) {

    /** How a hinge is driven: so long to draw its lock, to get up to speed, the share of its travel it is cushioned over. */
    public enum Drive {
        /** By a pump and a ram: its lock drawn in a third of a second, up to speed in a quarter, cushioned over its last eighth. */
        HYDRAULIC(0.35, 0.25, 0.125, 0.15, 0.5, 0.8);

        private final double unlock;
        private final double accel;
        private final double cushion;
        private final double slowest;
        private final double spoolUp;
        private final double spoolDown;

        /**
         * @param unlock    how long its lock pin takes to draw, seconds, its pump spooling up meanwhile
         * @param accel     how long it takes to get up to its rate, seconds
         * @param cushion   the share of its travel, at its end, it is cushioned over
         * @param slowest   how slow it goes at the very end, cushioned, a share of its rate
         * @param spoolUp   how long its pump takes to spool up, seconds
         * @param spoolDown and to run down
         */
        Drive(final double unlock, final double accel, final double cushion, final double slowest,
              final double spoolUp, final double spoolDown) {
            this.unlock = unlock;
            this.accel = accel;
            this.cushion = cushion;
            this.slowest = slowest;
            this.spoolUp = spoolUp;
            this.spoolDown = spoolDown;
        }

        public double unlock() {
            return unlock;
        }

        public double cushion() {
            return cushion;
        }
    }

    /** What it is doing. */
    public enum Stage {
        /** Shut, its lock in. */
        SHUT,
        /** Its lock being drawn, its pump spooling up, about to open. */
        UNLOCKING,
        OPENING,
        /** Open, held there. */
        OPEN,
        CLOSING,
        /** Stopped part-way, put there by hand: going nowhere until asked, then whichever way it is asked. */
        HELD,
        /** Asked to go, from part-way: its pump to start and its valve to open, this step. */
        STARTING
    }

    /** A change, heard: its pump started or stopped; its lock drawn or dropped in; its valve opened or switched; cushioned; stopped. */
    public enum Event {
        PUMP_START, UNLOCK, VALVE, CUSHION, STOP, LOCK, PUMP_STOP
    }

    /** It, a step on, and what changed meanwhile, in order. */
    public record Stepped(Actuator actuator, List<Event> events) {
        public Stepped {
            events = List.copyOf(events);
        }
    }

    public Actuator {
        if (!(least < most) || !(rate > 0)) {
            throw new IllegalArgumentException("a hinge from " + least + " to " + most + " at " + rate);
        }
    }

    /** Shut and locked, still, from {@code least} to {@code most}, at {@code rate}. */
    public static Actuator shut(final Drive drive, final double least, final double most, final double rate) {
        return new Actuator(drive, least, most, rate, least, false, Stage.SHUT, 0, 0, 0, false);
    }

    /** The same, asked to be open, or shut. */
    public Actuator asked(final boolean opened) {
        // from part-way, set going whichever way it is asked
        final Stage going = stage == Stage.HELD ? Stage.STARTING : stage;
        return new Actuator(drive, least, most, rate, angle, opened, going, timer, speed, spool, cushion);
    }

    /**
     * The same, put at {@code degrees} by hand and left there, still: shut and locked at its least,
     * open (held, asked to be) anywhere else, its pump running down.
     */
    public Actuator placed(final double degrees) {
        final double at = Math.clamp(degrees, least, most);
        final Stage still = at <= least ? Stage.SHUT : at >= most ? Stage.OPEN : Stage.HELD;
        return new Actuator(drive, least, most, rate, at, still == Stage.OPEN, still, 0, 0, spool, false);
    }

    /** The same, to move at {@code degrees} a second at most from now on. */
    public Actuator rate(final double degrees) {
        return new Actuator(drive, least, most, degrees, angle, open, stage, timer, speed, spool, cushion);
    }

    /** How hard its fluid flows, 0 to 1: how fast it moves, a share of its rate. */
    public double flow() {
        return Math.min(1, Math.abs(speed) / rate);
    }

    /** Whether it is on the move, or about to be. */
    public boolean busy() {
        return stage == Stage.UNLOCKING || stage == Stage.OPENING || stage == Stage.CLOSING || stage == Stage.STARTING;
    }

    /** It, {@code seconds} on, and what changed meanwhile. */
    public Stepped step(final double seconds) {
        final List<Event> events = new ArrayList<>();
        Stage now = stage;
        double time = timer + seconds;
        double v = speed;
        double at = angle;
        boolean cushioned = cushion;
        // what it is asked, against what it is doing: starting, turning back, or giving up drawing its lock
        if (now == Stage.STARTING) {
            events.add(Event.PUMP_START);
            events.add(Event.VALVE);
            now = open ? Stage.OPENING : Stage.CLOSING;
            time = seconds;
        } else         if (open && (now == Stage.SHUT)) {
            events.add(Event.PUMP_START);
            now = Stage.UNLOCKING;
            time = seconds;
        } else if (!open && now == Stage.UNLOCKING) {
            events.add(Event.LOCK);
            now = Stage.SHUT;
            time = seconds;
        } else if (open && now == Stage.CLOSING || !open && now == Stage.OPENING) {
            events.add(Event.VALVE);
            now = open ? Stage.OPENING : Stage.CLOSING;
            time = seconds;
            cushioned = false;
        } else if (!open && now == Stage.OPEN) {
            events.add(Event.PUMP_START);
            events.add(Event.VALVE);
            now = Stage.CLOSING;
            time = seconds;
            cushioned = false;
        }
        if (now == Stage.UNLOCKING && time >= drive.unlock) {
            events.add(Event.UNLOCK);
            events.add(Event.VALVE);
            now = Stage.OPENING;
            time = time - drive.unlock;
        }
        if (now == Stage.OPENING || now == Stage.CLOSING) {
            final double target = now == Stage.OPENING ? most : least;
            final double way = Math.signum(target - at);
            final double left = Math.abs(target - at);
            final double span = drive.cushion * (most - least);
            // flow-limited: its rate, but cushioned near its end, slower and slower
            final double share = left < span ? Math.max(drive.slowest, left / span) : 1;
            if (left < span && !cushioned) {
                events.add(Event.CUSHION);
                cushioned = true;
            }
            final double wanted = way * rate * share;
            final double change = rate / drive.accel * seconds;
            v = v + Math.clamp(wanted - v, -change, change);
            at = at + v * seconds;
            if (way == 0 || Math.signum(target - at) != way) {
                // there: stopped against its end; shut, its lock dropped in
                at = target;
                v = 0;
                events.add(Event.STOP);
                if (now == Stage.CLOSING) {
                    events.add(Event.LOCK);
                }
                events.add(Event.PUMP_STOP);
                now = now == Stage.OPENING ? Stage.OPEN : Stage.SHUT;
                time = 0;
                cushioned = false;
            }
        }
        // its pump: spooling up while it has work, running down when it has done
        final boolean working = now == Stage.UNLOCKING || now == Stage.OPENING || now == Stage.CLOSING;
        final double pump = working ? Math.min(1, spool + seconds / drive.spoolUp)
                : Math.max(0, spool - seconds / drive.spoolDown);
        return new Stepped(new Actuator(drive, least, most, rate, at, open, now, time, v, pump, cushioned), events);
    }
}
