package com.example.fxsuite.avatar.lifts;

import com.example.fxsuite.avatar.bench.kit.Spot;
import com.example.fxsuite.avatar.build.Solid;
import com.example.fxsuite.avatar.sim.Place;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A shuttle lift on its own: its shaft from the ground up past its top floor to its cap, a landing's doorway in its
 * front at each floor; a landing in front of it at each floor over the ground, railed along its front and sides.
 * Its floors no nearer than its car is high; its doorway no wider than half its car, as its doors need.
 */
final class ShuttleScene implements LiftScene {

    /** How far its landings reach either side of its shaft, and out in front of it. */
    static final double SIDES = 250;
    static final double FRONT = 500;

    @Override
    public String name() {
        return "Shuttle lift";
    }

    @Override
    public List<Knob> knobs() {
        return List.of(
                new Knob("floors", "Floors", "", 2, 10, 1, 4),
                new Knob("storey", "Storey", "m", 3, 5, 0.1, 4),
                new Knob("width", "Car width", "m", 1.2, 2.6, 0.05, ShuttleLift.WIDE / 100),
                new Knob("depth", "Car depth", "m", 1.2, 2.6, 0.05, ShuttleLift.DEEP / 100),
                new Knob("height", "Car height", "m", 2.2, 3, 0.05, ShuttleLift.HIGH / 100),
                new Knob("doorway", "Doorway", "m", 0.6, 1.3, 0.05, ShuttleLift.DOORWAY / 100),
                new Knob("speed", "Top speed", "m/s", 0.5, 4, 0.1, 1.6),
                new Knob("accel", "Acceleration", "m/s²", 0.5, 1.5, 0.05, 1),
                new Knob("held", "Doors held open", "s", 2, 8, 0.5, ShuttleLift.HELD));
    }

    @Override
    public Built built(final Map<String, Double> set) {
        final List<Knob> k = knobs();
        final int floors = (int) Math.round(k.get(0).of(set));
        final double high = k.get(4).of(set) * 100;
        final double wide = k.get(2).of(set) * 100;
        // floors no nearer than its car is high; its doorway no wider than half its car
        final double storey = Math.max(k.get(1).of(set) * 100,
                ShuttleLift.FLOOR + high + ShuttleLift.FLOOR + Frame.SLAB + 5);
        final double doorway = Math.min(k.get(5).of(set) * 100, wide / 2);
        final List<Double> tops = new ArrayList<>();
        for (int f = 0; f < floors; f++) {
            tops.add(f * storey);
        }
        final ShuttleLift s = new ShuttleLift(0, 0, tops, k.get(6).of(set) * 100, k.get(7).of(set) * 100, wide,
                k.get(3).of(set) * 100, high, doorway, k.get(8).of(set));
        final double[] h = s.hole();
        final double w = ShuttleLift.SHAFT_WALL;
        final double x0 = h[0] - w - SIDES;
        final double x1 = h[1] + w + SIDES;
        final double z1 = h[3] + w + FRONT;
        final List<Solid> frame = new ArrayList<>(Frame.landings(tops, x0, x1, h[2] - w, z1, List.of(h)));
        frame.addAll(s.shaft(Frame.SLAB));
        final String figures = String.format(Locale.ROOT,
                "%d floors, %.1f m apart%s; a car %.2f × %.2f m, %.2f m high, its doorway %.2f m%s; %.1f m/s top speed,"
                        + " %.2f m/s²: a floor in %.1f s; %.1f s at each stop; round its timetable in %.1f s",
                floors, storey / 100, storey > k.get(1).of(set) * 100 + 1e-6 ? " (as near as its car lets them)" : "",
                wide / 100, s.deep() / 100, high / 100, doorway / 100,
                doorway < k.get(5).of(set) * 100 - 1e-6 ? " (half its car, as its doors need)" : "",
                s.speed() / 100, s.accel() / 100, s.travel(storey), s.dwell(), s.cycle());
        final double top = s.floors().getLast() + ShuttleLift.FLOOR + high + ShuttleLift.FLOOR + 50 + Frame.SLAB;
        final double size = Math.max(x1 - x0, top);
        return new Built("Shuttle lift", figures, frame, frame, s.movers(SceneWorld.KEY + 1, SceneWorld.KEY + 2,
                SceneWorld.KEY + 3),
                List.of(frame, solids(s.car()), solids(s.carLeaf()), solids(s.landingLeaf())),
                new Spot("before it", new Place(s.x(), h[3] + w + 300, 0, 180)),
                new Place(x0, z1 + 700, 0, 90), new Place(x0 + 100, z1 + 250, 0, 180),
                size + 3_000, new double[] {0, top / 2, 0}, size, 145);
    }

    private static List<Solid> solids(final List<Box> boxes) {
        return boxes.stream().map(Box::solid).toList();
    }
}
