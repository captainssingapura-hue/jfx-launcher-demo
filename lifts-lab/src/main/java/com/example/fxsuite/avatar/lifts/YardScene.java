package com.example.fxsuite.avatar.lifts;

import com.example.fxsuite.avatar.bench.kit.Spot;
import com.example.fxsuite.avatar.build.Solid;
import com.example.fxsuite.avatar.sim.Place;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The lifts yard: a frame of four floors (the ground, and three over it on columns), open all round but for a rail at
 * each upper floor's edge, and one of each lift at work in it: a paternoster on its left, a shuttle lift on its right,
 * an escalator from the ground up to its first floor at its back. Each floor cut where a lift goes through it. Each
 * lift as it mostly is: nothing to set.
 */
final class YardScene implements LiftScene {

    /** Its floors' tops, from the ground up; how far its frame reaches across and along from its middle. */
    static final List<Double> FLOORS = List.of(0.0, 400.0, 800.0, 1200.0);
    static final double HALF_ACROSS = 900;
    static final double HALF_ALONG = 500;

    final Paternoster paternoster = Paternoster.about(-490, 0, -300, FLOORS.getLast() + 300, 302, 30,
            Paternoster.WIDE, Paternoster.DEEP, Paternoster.HIGH);
    final ShuttleLift shuttle = new ShuttleLift(260, 0, FLOORS, 160, 100);
    final Escalator escalator = new Escalator(0, -HALF_ALONG, FLOORS.get(1), 30, 50);

    @Override
    public String name() {
        return "Yard";
    }

    @Override
    public List<Knob> knobs() {
        return List.of();
    }

    @Override
    public Built built(final Map<String, Double> set) {
        final List<Solid> standing = new ArrayList<>();
        final List<double[]> holes = List.of(paternoster.hole(), shuttle.hole());
        final double a = HALF_ACROSS;
        final double l = HALF_ALONG;
        final double t = Frame.RAIL_THICK;
        for (int f = 1; f < FLOORS.size(); f++) {
            final double top = FLOORS.get(f);
            standing.addAll(Frame.floor("floor " + f, -a, a, -l, l, top, holes));
            standing.add(Frame.rail("rail, front", -a, a, l - t, l, top));
            standing.add(Frame.rail("rail, left", -a, -a + t, -l, l, top));
            standing.add(Frame.rail("rail, right", a - t, a, -l, l, top));
            if (f == 1) {
                final double gap = escalator.outside() / 2 + 10;
                standing.add(Frame.rail("rail, back, left", -a, escalator.x() - gap, -l, -l + t, top));
                standing.add(Frame.rail("rail, back, right", escalator.x() + gap, a, -l, -l + t, top));
            } else {
                standing.add(Frame.rail("rail, back", -a, a, -l, -l + t, top));
            }
        }
        for (final double x : new double[] {-a + 20, a - 20}) {
            for (final double z : new double[] {-l + 20, l - 20}) {
                standing.add(Frame.column(x, z, FLOORS.getLast()));
            }
        }
        standing.addAll(shuttle.shaft(Frame.SLAB));
        standing.addAll(paternoster.shaft(0));
        standing.addAll(escalator.balustrades());
        standing.addAll(escalator.combs());
        final List<Solid> drawn = new ArrayList<>(standing);
        drawn.addAll(escalator.truss());
        final List<Mover> movers = new ArrayList<>(paternoster.movers(SceneWorld.KEY + 1));
        movers.addAll(shuttle.movers(SceneWorld.KEY + 2, SceneWorld.KEY + 3, SceneWorld.KEY + 4));
        movers.addAll(escalator.movers(SceneWorld.KEY + 5));
        final Paternoster p = paternoster;
        final ShuttleLift s = shuttle;
        final Escalator e = escalator;
        final String figures = String.format(Locale.ROOT,
                "Paternoster: %d cabins round a loop %.1f m long, at %.2f m/s; round in %.0f s; a cabin past a landing"
                        + " every %.1f s each way%n"
                        + "Shuttle lift: %d floors, at %.1f m/s top speed; round its timetable in %.1f s, %.1f s at each"
                        + " stop%n"
                        + "Escalator: %.1f m up at %.0f°, %.1f m along; %d steps, each rising %.1f cm; %.2f m/s",
                p.cabins(), p.loop() / 100, p.speed() / 100, p.round(), p.round() / p.cabins(),
                s.floors().size(), s.speed() / 100, s.cycle(), s.dwell(),
                e.rise() / 100, e.slope(), e.length() / 100, e.steps(), e.riser(), e.speed() / 100);
        return new Built("Lifts yard", figures, drawn, standing, movers,
                List.of(drawn, solids(p.cabin()), solids(s.car()), solids(s.carLeaf()), solids(s.landingLeaf()),
                        solids(e.stepBoxes())),
                new Spot("before it", new Place(0, l + 400, 0, 180)),
                new Place(-a, l + 1_100, 0, 90), new Place(-300, l + 250, 0, 180),
                4_000, new double[] {0, 700, 0}, 2 * a, 145);
    }

    private static List<Solid> solids(final List<Box> boxes) {
        return boxes.stream().map(Box::solid).toList();
    }
}
