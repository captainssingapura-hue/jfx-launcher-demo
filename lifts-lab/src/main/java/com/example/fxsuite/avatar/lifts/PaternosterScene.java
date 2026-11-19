package com.example.fxsuite.avatar.lifts;

import com.example.fxsuite.avatar.bench.kit.Spot;
import com.example.fxsuite.avatar.build.Solid;
import com.example.fxsuite.avatar.sim.Place;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A paternoster on its own: its two shafts, open in front, from a pit under the ground up past its top floor; a landing
 * in front of them at each floor over the ground, railed along its front and sides. Its cabins go a cabin's height and
 * more past its top and bottom floors before turning, as a paternoster's do, so each floor is passed by a cabin's whole
 * floor.
 */
final class PaternosterScene implements LiftScene {

    /** How far its landings reach either side of its shafts, and out in front of them. */
    static final double SIDES = 200;
    static final double FRONT = 500;

    @Override
    public String name() {
        return "Paternoster";
    }

    @Override
    public List<Knob> knobs() {
        return List.of(
                new Knob("floors", "Floors", "", 2, 8, 1, 4),
                new Knob("storey", "Storey", "m", 3, 5, 0.1, 4),
                new Knob("width", "Cabin width", "m", 1.2, 2.2, 0.05, Paternoster.WIDE / 100),
                new Knob("depth", "Cabin depth", "m", 0.9, 1.6, 0.05, Paternoster.DEEP / 100),
                new Knob("height", "Cabin height", "m", 1.9, 2.6, 0.05, Paternoster.HIGH / 100),
                new Knob("spacing", "Cabins apart", "m", 2.5, 6, 0.1, 3),
                new Knob("speed", "Speed", "m/s", 0.15, 0.45, 0.01, 0.3));
    }

    @Override
    public Built built(final Map<String, Double> set) {
        final List<Knob> k = knobs();
        final int floors = (int) Math.round(k.get(0).of(set));
        final double storey = k.get(1).of(set) * 100;
        final double wide = k.get(2).of(set) * 100;
        final double deep = k.get(3).of(set) * 100;
        final double high = k.get(4).of(set) * 100;
        final double spacing = k.get(5).of(set) * 100;
        final double speed = k.get(6).of(set) * 100;
        final List<Double> tops = new ArrayList<>();
        for (int f = 0; f < floors; f++) {
            tops.add(f * storey);
        }
        final double cabin = Paternoster.FLOOR + high + Paternoster.FLOOR;
        final double topFloor = tops.getLast();
        final Paternoster p = Paternoster.about(0, 0, -cabin - 40, topFloor + cabin + 40, spacing, speed, wide, deep,
                high);
        final double[] h = p.hole();
        final double w = Paternoster.SHAFT_WALL;
        final double x0 = h[0] - w - SIDES;
        final double x1 = h[1] + w + SIDES;
        final double z1 = h[3] + FRONT;
        final List<Solid> frame = new ArrayList<>(Frame.landings(tops, x0, x1, h[2] - w, z1, List.of(h)));
        frame.addAll(p.shaft(p.bottom() - p.radius() - 30));
        final String figures = String.format(Locale.ROOT,
                "%d floors, %.1f m apart; %d cabins, %.2f × %.2f m, %.2f m high, round a loop %.1f m long at %.2f m/s;"
                        + " round in %.0f s; a cabin past a landing every %.1f s each way",
                floors, storey / 100, p.cabins(), wide / 100, deep / 100, high / 100, p.loop() / 100, speed / 100,
                p.round(), p.round() / p.cabins());
        final double size = Math.max(x1 - x0, p.over());
        return new Built("Paternoster", figures, frame, frame, p.movers(SceneWorld.KEY + 1),
                List.of(frame, p.cabin().stream().map(Box::solid).toList()),
                new Spot("before it", new Place(p.upX(), h[3] + 300, 0, 180)),
                new Place(x0, z1 + 700, 0, 90), new Place(x0 + 100, z1 + 250, 0, 180),
                size + 3_000, new double[] {0, topFloor / 2, 0}, size, 145);
    }
}
