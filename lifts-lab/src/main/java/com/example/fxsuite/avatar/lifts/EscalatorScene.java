package com.example.fxsuite.avatar.lifts;

import com.example.fxsuite.avatar.bench.kit.Spot;
import com.example.fxsuite.avatar.build.Solid;
import com.example.fxsuite.avatar.sim.Place;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * An escalator on its own: from the ground up to a deck on four columns, railed round but where the escalator comes up
 * to it; walked onto at its foot, stepped off onto the deck at its head.
 */
final class EscalatorScene implements LiftScene {

    /** How far the deck reaches either side of the escalator, and back from its head. */
    static final double SIDES = 400;
    static final double DEEP = 800;

    @Override
    public String name() {
        return "Escalator";
    }

    @Override
    public List<Knob> knobs() {
        return List.of(
                new Knob("rise", "Rise", "m", 2.5, 8, 0.1, 4),
                new Knob("slope", "Slope", "°", 27, 35, 0.5, 30),
                new Knob("width", "Step width", "m", 0.6, 1.2, 0.05, Escalator.WIDE / 100),
                new Knob("speed", "Speed", "m/s", 0.3, 0.75, 0.01, 0.5),
                new Knob("flat", "Flat at each end", "m", 0.8, 1.6, 0.05, Escalator.FLAT / 100));
    }

    @Override
    public Built built(final Map<String, Double> set) {
        final List<Knob> k = knobs();
        final Escalator e = new Escalator(0, 0, k.get(0).of(set) * 100, k.get(1).of(set), k.get(3).of(set) * 100,
                k.get(2).of(set) * 100, k.get(4).of(set) * 100);
        final double x0 = -SIDES;
        final double x1 = SIDES;
        final double z0 = e.head();
        final double z1 = e.head() + DEEP;
        final double rise = e.rise();
        final double t = Frame.RAIL_THICK;
        final double gap = e.outside() / 2 + 10;
        final List<Solid> frame = new ArrayList<>(Frame.landings(List.of(rise), x0, x1, z0, z1, List.of()));
        frame.add(Frame.rail("rail, back, left", x0, e.x() - gap, z0, z0 + t, rise));
        frame.add(Frame.rail("rail, back, right", e.x() + gap, x1, z0, z0 + t, rise));
        frame.add(Frame.column(x0 + Frame.COLUMN / 2, z0 + Frame.COLUMN / 2, rise));
        frame.add(Frame.column(x1 - Frame.COLUMN / 2, z0 + Frame.COLUMN / 2, rise));
        frame.addAll(e.balustrades());
        frame.addAll(e.combs());
        final List<Solid> drawn = new ArrayList<>(frame);
        drawn.addAll(e.truss());
        final String figures = String.format(Locale.ROOT,
                "%.1f m up at %.1f°, %.1f m along, %.2f m wide; %d steps, each %.1f cm deep, rising %.1f cm;"
                        + " %.2f m/s: up in %.0f s",
                rise / 100, e.slope(), e.length() / 100, e.width() / 100, e.steps(), e.tread(), e.riser(),
                e.speed() / 100, e.length() / e.speed());
        final double size = Math.max(e.length() + DEEP, rise + 200);
        return new Built("Escalator", figures, drawn, frame, e.movers(SceneWorld.KEY + 1),
                List.of(drawn, e.stepBoxes().stream().map(Box::solid).toList()),
                new Spot("at its foot", new Place(e.x(), e.foot() - 300, 0, 0)),
                new Place(x0, e.foot() - 900, 0, 90), new Place(e.x() + gap + 100, e.foot() - 100, 0, 0),
                size + 3_000, new double[] {0, rise / 2, (e.foot() + z1) / 2}, size, -35);
    }
}
