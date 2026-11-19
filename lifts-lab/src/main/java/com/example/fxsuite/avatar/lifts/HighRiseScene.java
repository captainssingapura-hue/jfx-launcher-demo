package com.example.fxsuite.avatar.lifts;

import com.example.fxsuite.avatar.bench.kit.Spot;
import com.example.fxsuite.avatar.build.Solid;
import com.example.fxsuite.avatar.sim.Place;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A high-rise building's bank of lifts: its shafts from the lobby up past its top floor, a floor before them at every
 * storey (railed at its edges, cut for the shafts), a landing's doors at the lobby and at each floor of its zone, the
 * shafts' fronts blank at the floors it goes past. Its cars fast, easing into speed and out of it, each on the same
 * timetable a share of it apart: express up to its zone, its zone's floors up and down, express down.
 */
final class HighRiseScene implements LiftScene {

    /** How far its floors reach either side of its shafts, and out in front of them. */
    static final double SIDES = 500;
    static final double FRONT = 800;

    @Override
    public String name() {
        return "High-rise bank";
    }

    @Override
    public List<Knob> knobs() {
        return List.of(
                new Knob("floors", "Floors over the lobby", "", 10, 60, 1, 40),
                new Knob("storey", "Storey", "m", 3.6, 5, 0.05, 4.2),
                new Knob("from", "Zone from floor", "", 1, 59, 1, 30),
                new Knob("to", "Zone to floor", "", 1, 60, 1, 40),
                new Knob("cars", "Cars", "", 1, 8, 1, 4),
                new Knob("speed", "Top speed", "m/s", 2.5, 10, 0.1, 6),
                new Knob("accel", "Acceleration", "m/s²", 0.6, 1.5, 0.05, 1),
                new Knob("jerk", "Easing (jerk)", "m/s³", 1, 2.5, 0.1, 1.5),
                new Knob("width", "Car width", "m", 1.6, 2.6, 0.05, 2),
                new Knob("depth", "Car depth", "m", 1.4, 2.4, 0.05, 1.7),
                new Knob("height", "Car height", "m", 2.4, 3, 0.05, 2.6),
                new Knob("doorway", "Doorway", "m", 0.8, 1.3, 0.05, 1),
                new Knob("held", "Doors held open", "s", 2, 6, 0.5, 3.5));
    }

    @Override
    public Built built(final Map<String, Double> set) {
        final List<Knob> k = knobs();
        final int floors = (int) Math.round(k.get(0).of(set));
        final double high = k.get(10).of(set) * 100;
        final double storey = Math.max(k.get(1).of(set) * 100,
                ShuttleLift.FLOOR + high + ShuttleLift.FLOOR + Frame.SLAB + 5);
        final int to = Math.min(floors, (int) Math.round(k.get(3).of(set)));
        final int from = Math.min(to, (int) Math.round(k.get(2).of(set)));
        final double wide = k.get(8).of(set) * 100;
        final double doorway = Math.min(k.get(11).of(set) * 100, wide / 2);
        final List<Double> levels = new ArrayList<>();
        for (int f = 0; f <= floors; f++) {
            levels.add(f * storey);
        }
        final Profile profile = new Profile(k.get(5).of(set) * 100, k.get(6).of(set) * 100, k.get(7).of(set) * 100);
        final HighRiseBank bank = new HighRiseBank(0, 0, levels, from, to, (int) Math.round(k.get(4).of(set)), profile,
                wide, k.get(9).of(set) * 100, high, doorway, k.get(12).of(set));
        final double[] h = bank.hole();
        final double w = HighRiseBank.SHAFT_WALL;
        final double x0 = h[0] - w - SIDES;
        final double x1 = h[1] + w + SIDES;
        final double z1 = h[3] + w + FRONT;
        final List<Solid> frame = new ArrayList<>(Frame.landings(levels, x0, x1, h[2] - w, z1, List.of(h)));
        frame.addAll(bank.shafts(Frame.SLAB));
        final double express = levels.get(from) - levels.getFirst();
        final double ride = profile.time(express);
        final String figures = String.format(Locale.ROOT,
                "%d floors over the lobby, %.2f m apart, %.0f m to the top; %d cars serving the lobby and floors %d to %d,"
                        + " express past %d floors; %.1f m/s top speed, %.2f m/s², easing %.1f m/s³:"
                        + " the lobby to floor %d (%.0f m) in %.1f s; %.1f s at each stop; round in %.0f s, a car every"
                        + " %.0f s",
                floors, storey / 100, levels.getLast() / 100, bank.cars(), from, to, from - 1, profile.speed() / 100,
                profile.accel() / 100, profile.jerk() / 100, from, express / 100, ride, bank.dwell(), bank.cycle(),
                bank.interval());
        final double size = Math.max(x1 - x0, bank.over());
        return new Built("High-rise bank", figures, frame, frame,
                bank.movers(SceneWorld.KEY + 1, SceneWorld.KEY + 2, SceneWorld.KEY + 3),
                List.of(frame, solids(bank.car()), solids(bank.carLeaf()), solids(bank.landingLeaf())),
                new Spot("in the lobby", new Place(bank.x(), h[3] + w + 300, 0, 180)),
                new Place(x0, z1 + 700, 0, 90), new Place(x0 + 100, z1 + 250, 0, 180),
                size + 3_000, new double[] {0, levels.getLast() / 2, 0}, size, 145);
    }

    private static List<Solid> solids(final List<Box> boxes) {
        return boxes.stream().map(Box::solid).toList();
    }
}
