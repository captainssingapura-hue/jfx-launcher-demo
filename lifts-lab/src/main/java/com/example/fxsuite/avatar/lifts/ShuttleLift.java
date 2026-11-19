package com.example.fxsuite.avatar.lifts;

import com.example.fxsuite.avatar.build.Material;
import com.example.fxsuite.avatar.build.Role;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Place;

import java.util.ArrayList;
import java.util.List;

/**
 * A shuttle lift: a car in a shaft, going floor to floor on a timetable, up to the top and down to the foot and up
 * again, stopping at each; at each stop its doors opening, held open, and shutting; and at each landing a pair of
 * doors of the landing's own, opening only while the car stands there, so no one walks into an empty shaft. Its
 * doors face along.
 *
 * <p>Where the car is, and how far open each door is, is a function of time alone: from one floor to the next it
 * gathers speed at {@code accel} to {@code speed}, and slows as it gathers it, to stop level with the floor.
 *
 * @param x      across, the middle of its shaft
 * @param z      along, the middle of its shaft
 * @param floors each landing's floor, from the foot up
 * @param speed  its top speed, cm/s
 * @param accel  how fast it gathers speed and loses it, cm/s²
 */
record ShuttleLift(double x, double z, List<Double> floors, double speed, double accel) {

    /** Its car: so wide and deep outside, so high inside; its floor so thick; its doorway so wide and so high. */
    static final double CAR = 200;
    static final double HIGH = 250;
    static final double FLOOR = 10;
    static final double DOORWAY = 100;
    static final double DOOR_HIGH = 210;
    /** A landing's doorway: so wide and so high; the shaft's walls so thick, so far out from the car. */
    static final double LANDING = 120;
    static final double LANDING_HIGH = 220;
    static final double SHAFT_WALL = 10;
    static final double GAP = 10;
    /** At each stop: its doors opening, held open, shutting, seconds. */
    static final double OPENING = 1.5;
    static final double HELD = 4;
    static final double SHUTTING = 1.5;

    ShuttleLift {
        floors = List.copyOf(floors);
        if (floors.size() < 2 || !(speed > 0 && accel > 0)) {
            throw new IllegalArgumentException("a shuttle lift between two floors at least, going");
        }
    }

    /** How long it stands at a stop. */
    static double dwell() {
        return OPENING + HELD + SHUTTING;
    }

    /** How long it takes to go {@code distance}, from standing to standing. */
    double travel(final double distance) {
        final double d = Math.abs(distance);
        final double ramp = speed * speed / accel;
        return d >= ramp ? d / speed + speed / accel : 2 * Math.sqrt(d / accel);
    }

    /** How far it has gone of {@code distance}, {@code t} after starting, from standing to standing. */
    double gone(final double distance, final double t) {
        final double d = Math.abs(distance);
        final double total = travel(d);
        final double clamped = Math.clamp(t, 0, total);
        final double top = Math.min(speed, Math.sqrt(d * accel));
        final double ramp = top / accel;
        final double done;
        if (clamped < ramp) {
            done = accel * clamped * clamped / 2;
        } else if (clamped > total - ramp) {
            final double left = total - clamped;
            done = d - accel * left * left / 2;
        } else {
            done = accel * ramp * ramp / 2 + top * (clamped - ramp);
        }
        return Math.signum(distance) * done;
    }

    /** Its stops, in order, round its timetable: the foot to the top and back down, the foot not twice. */
    List<Integer> stops() {
        final List<Integer> stops = new ArrayList<>();
        for (int f = 0; f < floors.size(); f++) {
            stops.add(f);
        }
        for (int f = floors.size() - 2; f > 0; f--) {
            stops.add(f);
        }
        return stops;
    }

    /** How long its timetable takes, round. */
    double cycle() {
        double total = 0;
        final List<Integer> stops = stops();
        for (int k = 0; k < stops.size(); k++) {
            total += dwell() + travel(floors.get(stops.get((k + 1) % stops.size())) - floors.get(stops.get(k)));
        }
        return total;
    }

    /** Where it is, {@code seconds} in: its floor's top, which floor it stands at (-1 going), how far open its doors. */
    record State(double level, int at, double open) {
    }

    /** Its state {@code seconds} after the start of its timetable, at the foot with its doors beginning to open. */
    State at(final double seconds) {
        final List<Integer> stops = stops();
        double t = ((seconds % cycle()) + cycle()) % cycle();
        for (int k = 0; ; k = (k + 1) % stops.size()) {
            final int here = stops.get(k);
            final double level = floors.get(here);
            if (t < dwell()) {
                final double open = t < OPENING ? t / OPENING
                        : t < OPENING + HELD ? 1 : 1 - (t - OPENING - HELD) / SHUTTING;
                return new State(level, here, Math.clamp(open, 0, 1));
            }
            t -= dwell();
            final double next = floors.get(stops.get((k + 1) % stops.size()));
            final double going = travel(next - level);
            if (t < going) {
                return new State(level + gone(next - level, t), -1, 0);
            }
            t -= going;
        }
    }

    /** How far open landing {@code floor}'s doors are, {@code seconds} in: as the car's, while it stands there. */
    double landingOpen(final int floor, final double seconds) {
        final State s = at(seconds);
        return s.at() == floor ? s.open() : 0;
    }

    /** The car: its floor, its back and sides, its front either side of its doorway and over it, its roof. */
    static List<Box> car() {
        final double h = CAR / 2;
        final double w = 4;
        final double door = DOORWAY / 2;
        final double up = FLOOR + HIGH;
        return List.of(
                new Box("car, floor", Role.BASE, Material.METAL, -h, h, -h, h, 0, FLOOR),
                new Box("car, back", Role.WALL, Material.METAL, -h, h, -h, -h + w, FLOOR, up),
                new Box("car, left side", Role.WALL, Material.METAL, -h, -h + w, -h + w, h - w, FLOOR, up),
                new Box("car, right side", Role.WALL, Material.METAL, h - w, h, -h + w, h - w, FLOOR, up),
                new Box("car, front, left", Role.WALL, Material.METAL, -h, -door, h - w, h, FLOOR, up),
                new Box("car, front, right", Role.WALL, Material.METAL, door, h, h - w, h, FLOOR, up),
                new Box("car, front, over", Role.WALL, Material.METAL, -door, door, h - w, h, FLOOR + DOOR_HIGH, up),
                new Box("car, roof", Role.ROOF, Material.METAL, -h, h, -h, h, up, up + FLOOR));
    }

    /** A door's leaf, {@code wide} wide, {@code high} high, 2 cm thick. */
    static List<Box> leaf(final String name, final double wide, final double high) {
        return List.of(new Box(name, Role.FRAME, Material.METAL, -wide / 2, wide / 2, -1, 1, 0, high));
    }

    /** Its car, its car's doors and each landing's doors, each a mover: drawn by {@code car}, {@code carDoor}, {@code landingDoor}. */
    List<Mover> movers(final int car, final int carDoor, final int landingDoor) {
        final List<Mover> movers = new ArrayList<>();
        movers.add(new Mover("shuttle car", car(), seconds -> new Place(x, z, at(seconds).level() - FLOOR, 0), car));
        // the car's doors, just outside its front, sliding apart over its front's sides
        for (final int side : new int[] {-1, 1}) {
            movers.add(new Mover("car door, " + (side < 0 ? "left" : "right"), leaf("car door", DOORWAY / 2, DOOR_HIGH),
                    seconds -> {
                        final State s = at(seconds);
                        return new Place(x + side * DOORWAY / 4 * (1 + 2 * s.open()), z + CAR / 2 + 1.5, s.level(), 0);
                    }, carDoor));
        }
        // each landing's doors, on the shaft's front, sliding apart over it while the car stands there
        final double front = z + CAR / 2 + GAP + SHAFT_WALL + 1.5;
        for (int f = 0; f < floors.size(); f++) {
            final int floor = f;
            for (final int side : new int[] {-1, 1}) {
                movers.add(new Mover("landing door, floor " + f + ", " + (side < 0 ? "left" : "right"),
                        leaf("landing door", LANDING / 2, LANDING_HIGH),
                        seconds -> new Place(x + side * LANDING / 4 * (1 + 2 * landingOpen(floor, seconds)), front,
                                floors.get(floor), 0), landingDoor));
            }
        }
        return movers;
    }

    /** The hole each floor is cut for its shaft: the car and the gap round it. */
    double[] hole() {
        final double h = CAR / 2 + GAP;
        return new double[] {x - h, x + h, z - h, z + h};
    }

    /**
     * Its shaft's walls from {@code from} up to {@code to}: behind it, either side, and in front either side of the
     * landing's doorway and over it ({@code lintel} up from {@code from}); its foot at {@code from}.
     */
    List<Box> shaft(final double from, final double to, final boolean doorway) {
        final double h = CAR / 2 + GAP;
        final double w = SHAFT_WALL;
        final double door = LANDING / 2;
        final List<Box> walls = new ArrayList<>(List.of(
                new Box("shaft, back", Role.WALL, Material.CONCRETE, x - h - w, x + h + w, z - h - w, z - h, from, to),
                new Box("shaft, left", Role.WALL, Material.CONCRETE, x - h - w, x - h, z - h, z + h, from, to),
                new Box("shaft, right", Role.WALL, Material.CONCRETE, x + h, x + h + w, z - h, z + h, from, to)));
        if (doorway) {
            walls.add(new Box("shaft, front, left", Role.WALL, Material.CONCRETE, x - h - w, x - door, z + h, z + h + w,
                    from, to));
            walls.add(new Box("shaft, front, right", Role.WALL, Material.CONCRETE, x + door, x + h + w, z + h,
                    z + h + w, from, to));
            if (from + LANDING_HIGH < to) {
                walls.add(new Box("shaft, front, over", Role.WALL, Material.CONCRETE, x - door, x + door, z + h,
                        z + h + w, from + LANDING_HIGH, to));
            }
        } else {
            walls.add(new Box("shaft, front", Role.WALL, Material.CONCRETE, x - h - w, x + h + w, z + h, z + h + w,
                    from, to));
        }
        return walls;
    }
}
