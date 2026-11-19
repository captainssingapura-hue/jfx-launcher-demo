package com.example.fxsuite.avatar.lifts;

import com.example.fxsuite.avatar.build.Material;
import com.example.fxsuite.avatar.build.Role;
import com.example.fxsuite.avatar.build.Solid;
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
 * @param x       across, the middle of its shaft
 * @param z       along, the middle of its shaft
 * @param floors  each landing's floor, from the foot up
 * @param speed   its top speed, cm/s
 * @param accel   how fast it gathers speed and loses it, cm/s²
 * @param wide    how wide its car is outside, across
 * @param deep    how deep its car is outside, along
 * @param high    how high its car is inside
 * @param doorway how wide its car's doorway is (its landings' a little wider)
 * @param held    how long its doors are held open at a stop, seconds
 */
record ShuttleLift(double x, double z, List<Double> floors, double speed, double accel, double wide, double deep,
                   double high, double doorway, double held) {

    /** A car as they mostly are: so wide and deep outside, so high inside, its doorway so wide; held open so long. */
    static final double WIDE = 200;
    static final double DEEP = 200;
    static final double HIGH = 250;
    static final double DOORWAY = 100;
    static final double HELD = 4;
    /** Its car's floor so thick, its walls; its doors this much under its ceiling; a landing's this much wider. */
    static final double FLOOR = 10;
    static final double WALL = 4;
    static final double DOOR_UNDER = 40;
    static final double LANDING_WIDER = 20;
    /** The shaft's walls so thick, so far out from the car. */
    static final double SHAFT_WALL = 10;
    static final double GAP = 10;
    /** At each stop: its doors opening, and shutting, seconds. */
    static final double OPENING = 1.5;
    static final double SHUTTING = 1.5;

    ShuttleLift {
        floors = List.copyOf(floors);
        if (floors.size() < 2 || !(speed > 0 && accel > 0 && held >= 0)) {
            throw new IllegalArgumentException("a shuttle lift between two floors at least, going");
        }
        // its doors' leaves, slid open, lie over its front either side of its doorway: no wider than half its width
        if (!(doorway > 0 && doorway <= wide / 2 && deep > 2 * WALL && high > DOOR_UNDER + 150)) {
            throw new IllegalArgumentException("a shuttle lift's car with room for its doors either side of its doorway");
        }
        for (int f = 1; f < floors.size(); f++) {
            if (floors.get(f) - floors.get(f - 1) < FLOOR + high + FLOOR + 25) {
                throw new IllegalArgumentException("a shuttle lift's floors further apart than its car is high");
            }
        }
    }

    /** One with a car as they mostly are, its doors held open as long. */
    ShuttleLift(final double x, final double z, final List<Double> floors, final double speed, final double accel) {
        this(x, z, floors, speed, accel, WIDE, DEEP, HIGH, DOORWAY, HELD);
    }

    /** How high its car's doors are, and its landings'. */
    double doorHigh() {
        return high - DOOR_UNDER;
    }

    /** How wide its landings' doorways are. */
    double landing() {
        return doorway + LANDING_WIDER;
    }

    /** How long it stands at a stop. */
    double dwell() {
        return OPENING + held + SHUTTING;
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
                        : t < OPENING + held ? 1 : 1 - (t - OPENING - held) / SHUTTING;
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
    List<Box> car() {
        return car(wide, deep, high, doorway);
    }

    /** A car so wide, deep and high inside, its doorway so wide, its doors {@link #DOOR_UNDER} under its ceiling. */
    static List<Box> car(final double wide, final double deep, final double high, final double doorway) {
        final double a = wide / 2;
        final double l = deep / 2;
        final double w = WALL;
        final double door = doorway / 2;
        final double up = FLOOR + high;
        final double doorHigh = high - DOOR_UNDER;
        return List.of(
                new Box("car, floor", Role.BASE, Material.METAL, -a, a, -l, l, 0, FLOOR),
                new Box("car, back", Role.WALL, Material.METAL, -a, a, -l, -l + w, FLOOR, up),
                new Box("car, left side", Role.WALL, Material.METAL, -a, -a + w, -l + w, l - w, FLOOR, up),
                new Box("car, right side", Role.WALL, Material.METAL, a - w, a, -l + w, l - w, FLOOR, up),
                new Box("car, front, left", Role.WALL, Material.METAL, -a, -door, l - w, l, FLOOR, up),
                new Box("car, front, right", Role.WALL, Material.METAL, door, a, l - w, l, FLOOR, up),
                new Box("car, front, over", Role.WALL, Material.METAL, -door, door, l - w, l, FLOOR + doorHigh, up),
                new Box("car, roof", Role.ROOF, Material.METAL, -a, a, -l, l, up, up + FLOOR));
    }

    /** A door's leaf, {@code wide} wide, {@code high} high, 2 cm thick. */
    static List<Box> leaf(final String name, final double wide, final double high) {
        return List.of(new Box(name, Role.FRAME, Material.METAL, -wide / 2, wide / 2, -1, 1, 0, high));
    }

    /** A leaf of its car's doors. */
    List<Box> carLeaf() {
        return leaf("car door", doorway / 2, doorHigh());
    }

    /** A leaf of a landing's doors. */
    List<Box> landingLeaf() {
        return leaf("landing door", landing() / 2, doorHigh() + 10);
    }

    /** Its car, its car's doors and each landing's doors, each a mover: drawn by {@code car}, {@code carDoor}, {@code landingDoor}. */
    List<Mover> movers(final int car, final int carDoor, final int landingDoor) {
        final List<Mover> movers = new ArrayList<>();
        movers.add(new Mover("shuttle car", car(), seconds -> new Place(x, z, at(seconds).level() - FLOOR, 0), car));
        // the car's doors, just outside its front, sliding apart over its front's sides
        for (final int side : new int[] {-1, 1}) {
            movers.add(new Mover("car door, " + (side < 0 ? "left" : "right"), carLeaf(), seconds -> {
                final State s = at(seconds);
                return new Place(x + side * doorway / 4 * (1 + 2 * s.open()), z + deep / 2 + 1.5, s.level(), 0);
            }, carDoor));
        }
        // each landing's doors, on the shaft's front, sliding apart over it while the car stands there
        final double front = z + deep / 2 + GAP + SHAFT_WALL + 1.5;
        for (int f = 0; f < floors.size(); f++) {
            final int floor = f;
            for (final int side : new int[] {-1, 1}) {
                movers.add(new Mover("landing door, floor " + f + ", " + (side < 0 ? "left" : "right"), landingLeaf(),
                        seconds -> new Place(x + side * landing() / 4 * (1 + 2 * landingOpen(floor, seconds)), front,
                                floors.get(floor), 0), landingDoor));
            }
        }
        return movers;
    }

    /** The hole each floor is cut for its shaft: the car and the gap round it (min x, max x, min z, max z). */
    double[] hole() {
        return new double[] {x - wide / 2 - GAP, x + wide / 2 + GAP, z - deep / 2 - GAP, z + deep / 2 + GAP};
    }

    /**
     * Its shaft, storey by storey from its foot, each landing's doorway in its front; each storey up to the floor over
     * it ({@code slab} thick), the top one up past its car to a cap.
     */
    List<Solid> shaft(final double slab) {
        final List<Solid> walls = new ArrayList<>();
        for (int f = 0; f < floors.size(); f++) {
            final double from = floors.get(f);
            final double to = f + 1 < floors.size() ? floors.get(f + 1) - slab : from + FLOOR + high + FLOOR + 50;
            storey(from, to).forEach(b -> walls.add(b.solid()));
        }
        final double[] h = hole();
        final double over = floors.getLast() + FLOOR + high + FLOOR + 50;
        walls.add(new Box("shaft, cap", Role.ROOF, Material.PRECAST, h[0] - SHAFT_WALL, h[1] + SHAFT_WALL,
                h[2] - SHAFT_WALL, h[3] + SHAFT_WALL, over, over + slab).solid());
        return walls;
    }

    /** Its shaft's walls from {@code from} up to {@code to}: behind, either side, and in front round a landing's doorway. */
    private List<Box> storey(final double from, final double to) {
        final double[] h = hole();
        final double w = SHAFT_WALL;
        final double door = landing() / 2;
        final double lintel = from + doorHigh() + 10;
        final List<Box> walls = new ArrayList<>(List.of(
                new Box("shaft, back", Role.WALL, Material.PRECAST, h[0] - w, h[1] + w, h[2] - w, h[2], from, to),
                new Box("shaft, left", Role.WALL, Material.PRECAST, h[0] - w, h[0], h[2], h[3], from, to),
                new Box("shaft, right", Role.WALL, Material.PRECAST, h[1], h[1] + w, h[2], h[3], from, to),
                new Box("shaft, front, left", Role.WALL, Material.PRECAST, h[0] - w, x - door, h[3], h[3] + w, from,
                        to),
                new Box("shaft, front, right", Role.WALL, Material.PRECAST, x + door, h[1] + w, h[3], h[3] + w, from,
                        to)));
        if (lintel < to) {
            walls.add(new Box("shaft, front, over", Role.WALL, Material.PRECAST, x - door, x + door, h[3], h[3] + w,
                    lintel, to));
        }
        return walls;
    }
}
