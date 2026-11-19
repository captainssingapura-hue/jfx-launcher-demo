package com.example.fxsuite.avatar.lifts;

import com.example.fxsuite.avatar.build.Material;
import com.example.fxsuite.avatar.build.Role;
import com.example.fxsuite.avatar.build.Solid;
import com.example.fxsuite.avatar.sim.Place;

import java.util.ArrayList;
import java.util.List;

/**
 * A bank of a high-rise building's lifts: {@code cars} cars in shafts side by side, their doors facing along, serving
 * the lobby (the ground floor) and one zone of floors ({@code zoneFrom} to {@code zoneTo}), going past the floors
 * under the zone without stopping (expressing), fast and smooth ({@link Profile}).
 *
 * <p>Each car on the same timetable, its start {@code 1/cars} of the way round after the car before: from the lobby
 * express up to the zone, stopping at each of its floors going up, then at each coming down, then express down to the
 * lobby. At each stop its doors opening, held open, and shutting, and that landing's doors with them; a landing's
 * doors only where the bank stops, the shafts' fronts blank between. Where each car is, and how far open each door
 * is, is a function of time alone: a lift to ride, not yet one to call.
 *
 * @param x        across, the middle of the bank
 * @param z        along, the middle of its shafts
 * @param levels   each floor's top, from the lobby up, the building's every floor
 * @param zoneFrom the zone's lowest floor (1 the first over the lobby)
 * @param zoneTo   the zone's highest floor
 * @param cars     how many cars
 * @param profile  how its cars go: their speed, acceleration and how smoothly
 * @param wide     how wide a car is outside, across
 * @param deep     how deep a car is outside, along
 * @param high     how high a car is inside
 * @param doorway  how wide a car's doorway is (a landing's a little wider)
 * @param held     how long its doors are held open at a stop, seconds
 */
record HighRiseBank(double x, double z, List<Double> levels, int zoneFrom, int zoneTo, int cars, Profile profile,
                    double wide, double deep, double high, double doorway, double held) {

    /** Its shafts' walls so thick, so far out from a car. */
    static final double SHAFT_WALL = 15;
    static final double GAP = 10;

    HighRiseBank {
        levels = List.copyOf(levels);
        if (!(zoneFrom >= 1 && zoneFrom <= zoneTo && zoneTo < levels.size() && cars >= 1 && held >= 0)) {
            throw new IllegalArgumentException("a bank serving the lobby and a zone of its building's floors");
        }
        if (!(doorway > 0 && doorway <= wide / 2 && deep > 2 * ShuttleLift.WALL
                && high > ShuttleLift.DOOR_UNDER + 150)) {
            throw new IllegalArgumentException("a bank's cars with room for their doors either side of their doorways");
        }
    }

    /** How far apart its shafts' middles are. */
    double pitch() {
        return wide + 2 * GAP + SHAFT_WALL;
    }

    /** Car {@code k}'s middle, across. */
    double carX(final int k) {
        return x + (k - (cars - 1) / 2.0) * pitch();
    }

    /** The floors it stops at: the lobby, and its zone's. */
    List<Integer> served() {
        final List<Integer> served = new ArrayList<>(List.of(0));
        for (int f = zoneFrom; f <= zoneTo; f++) {
            served.add(f);
        }
        return served;
    }

    /** Its cars' stops round its timetable: the lobby, its zone up, its zone down; then back to the lobby. */
    List<Integer> stops() {
        final List<Integer> stops = new ArrayList<>(served());
        for (int f = zoneTo - 1; f >= zoneFrom; f--) {
            stops.add(f);
        }
        return stops;
    }

    /** How long a car stands at a stop. */
    double dwell() {
        return ShuttleLift.OPENING + held + ShuttleLift.SHUTTING;
    }

    /** Its timetable, worked out once: where each car is at any moment. */
    Timetable timetable() {
        return new Timetable(this);
    }

    /** How long a car takes round its timetable. */
    double cycle() {
        return timetable().cycle();
    }

    /** How long after one car another comes, at any floor it serves. */
    double interval() {
        return cycle() / cars;
    }

    private double level(final int floor) {
        return levels.get(floor);
    }

    /** Car {@code k}'s state, {@code seconds} in (the timetable worked out afresh: for a few questions, not many). */
    ShuttleLift.State at(final int k, final double seconds) {
        return timetable().at(k, seconds);
    }

    /**
     * A bank's timetable, worked out once: each stop round it, when it begins, and the run from it to the next, so
     * that where each car is, asked every step for every car, is a few sums.
     */
    static final class Timetable {

        private final int cars;
        private final double held;
        private final double dwell;
        private final int[] floors;
        private final double[] levels;
        private final double[] starts;
        private final Profile.Run[] runs;
        private final double cycle;

        private Timetable(final HighRiseBank bank) {
            this.cars = bank.cars();
            this.held = bank.held();
            this.dwell = bank.dwell();
            final List<Integer> stops = bank.stops();
            final int n = stops.size();
            floors = new int[n];
            levels = new double[n];
            starts = new double[n];
            runs = new Profile.Run[n];
            double t = 0;
            for (int s = 0; s < n; s++) {
                floors[s] = stops.get(s);
                levels[s] = bank.level(floors[s]);
                runs[s] = bank.profile().run(bank.level(stops.get((s + 1) % n)) - levels[s]);
                starts[s] = t;
                t += dwell + runs[s].time();
            }
            cycle = t;
        }

        /** How long a car takes round it. */
        double cycle() {
            return cycle;
        }

        /** Car {@code k}'s state, {@code seconds} in: its floor's top, the floor it stands at (-1 going), how far open. */
        ShuttleLift.State at(final int k, final double seconds) {
            final double t = (((seconds + k * cycle / cars) % cycle) + cycle) % cycle;
            int s = 0;
            while (s + 1 < starts.length && starts[s + 1] <= t) {
                s++;
            }
            final double in = t - starts[s];
            if (in < dwell) {
                final double open = in < ShuttleLift.OPENING ? in / ShuttleLift.OPENING
                        : in < ShuttleLift.OPENING + held ? 1 : 1 - (in - ShuttleLift.OPENING - held) / ShuttleLift.SHUTTING;
                return new ShuttleLift.State(levels[s], floors[s], Math.clamp(open, 0, 1));
            }
            return new ShuttleLift.State(levels[s] + runs[s].gone(in - dwell), -1, 0);
        }

        /** How far open car {@code k}'s landing doors at {@code floor} are: as the car's, while it stands there. */
        double landingOpen(final int k, final int floor, final double seconds) {
            final ShuttleLift.State s = at(k, seconds);
            return s.at() == floor ? s.open() : 0;
        }
    }

    /** How high its cars' doors are. */
    double doorHigh() {
        return high - ShuttleLift.DOOR_UNDER;
    }

    /** How wide a landing's doorway is. */
    double landing() {
        return doorway + ShuttleLift.LANDING_WIDER;
    }

    /** A car. */
    List<Box> car() {
        return ShuttleLift.car(wide, deep, high, doorway);
    }

    /** A leaf of a car's doors. */
    List<Box> carLeaf() {
        return ShuttleLift.leaf("car door", doorway / 2, doorHigh());
    }

    /** A leaf of a landing's doors. */
    List<Box> landingLeaf() {
        return ShuttleLift.leaf("landing door", landing() / 2, doorHigh() + 10);
    }

    /** Each car, its doors and its landings' doors, each a mover: drawn by {@code car}, {@code carDoor}, {@code landingDoor}. */
    List<Mover> movers(final int car, final int carDoor, final int landingDoor) {
        final List<Mover> movers = new ArrayList<>();
        final double front = z + deep / 2 + GAP + SHAFT_WALL + 1.5;
        // one timetable, worked out once, for every car's way and every door's
        final Timetable timetable = timetable();
        final double carDoorOut = doorway / 4;
        final double landingOut = landing() / 4;
        for (int c = 0; c < cars; c++) {
            final int k = c;
            final double cx = carX(k);
            movers.add(new Mover("car " + (k + 1), car(),
                    seconds -> new Place(cx, z, timetable.at(k, seconds).level() - ShuttleLift.FLOOR, 0), car));
            for (final int side : new int[] {-1, 1}) {
                movers.add(new Mover("car " + (k + 1) + ", door", carLeaf(), seconds -> {
                    final ShuttleLift.State s = timetable.at(k, seconds);
                    return new Place(cx + side * carDoorOut * (1 + 2 * s.open()), z + deep / 2 + 1.5, s.level(), 0);
                }, carDoor));
                for (final int floor : served()) {
                    final double at = level(floor);
                    movers.add(new Mover("car " + (k + 1) + ", landing door, floor " + floor, landingLeaf(),
                            seconds -> new Place(cx + side * landingOut
                                    * (1 + 2 * timetable.landingOpen(k, floor, seconds)), front, at, 0), landingDoor));
                }
            }
        }
        return movers;
    }

    /** The hole each floor is cut for its shafts, all together (min x, max x, min z, max z). */
    double[] hole() {
        final double half = cars * pitch() / 2 - SHAFT_WALL / 2;
        return new double[] {x - half, x + half, z - deep / 2 - GAP, z + deep / 2 + GAP};
    }

    /** How high its shafts go: past its top floor, a car and some, to its machine room's floor. */
    double over() {
        return levels.getLast() + ShuttleLift.FLOOR + high + ShuttleLift.FLOOR + 100;
    }

    /**
     * Its shafts, from the lobby up past its top floor: a back and sides all the way up, a wall between each two;
     * in front, storey by storey, each car's landing's doorway where it stops, blank where it expresses; capped over by
     * its machine room's floor ({@code slab} thick).
     */
    List<Solid> shafts(final double slab) {
        final List<Solid> walls = new ArrayList<>();
        final double[] h = hole();
        final double w = SHAFT_WALL;
        final double over = over();
        walls.add(wall("shafts, back", h[0] - w, h[1] + w, h[2] - w, h[2], 0, over));
        for (int k = 0; k <= cars; k++) {
            final double between = x + (k - cars / 2.0) * pitch();
            walls.add(wall("shafts, side " + k, between - w / 2, between + w / 2, h[2], h[3], 0, over));
        }
        final List<Integer> served = served();
        for (int f = 0; f < levels.size(); f++) {
            final double from = level(f);
            final double to = f + 1 < levels.size() ? level(f + 1) - slab : over;
            for (int k = 0; k < cars; k++) {
                final double cx = carX(k);
                final double left = cx - pitch() / 2 + w / 2;
                final double right = cx + pitch() / 2 - w / 2;
                if (served.contains(f)) {
                    final double door = landing() / 2;
                    walls.add(wall("shaft front", left, cx - door, h[3], h[3] + w, from, to));
                    walls.add(wall("shaft front", cx + door, right, h[3], h[3] + w, from, to));
                    if (from + doorHigh() + 10 < to) {
                        walls.add(wall("shaft front, over", cx - door, cx + door, h[3], h[3] + w,
                                from + doorHigh() + 10, to));
                    }
                } else {
                    walls.add(wall("shaft front", left, right, h[3], h[3] + w, from, to));
                }
            }
        }
        walls.add(wall("machine room, floor", h[0] - w, h[1] + w, h[2] - w, h[3] + w, over, over + slab));
        return walls;
    }

    private static Solid wall(final String name, final double a0, final double a1, final double l0, final double l1,
                              final double u0, final double u1) {
        return new Box(name, Role.WALL, Material.PRECAST, a0, a1, l0, l1, u0, u1).solid();
    }
}
