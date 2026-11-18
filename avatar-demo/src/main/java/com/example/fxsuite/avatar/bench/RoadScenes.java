package com.example.fxsuite.avatar.bench;

import com.example.fxsuite.avatar.roads.Carrier;
import com.example.fxsuite.avatar.roads.Profile;
import com.example.fxsuite.avatar.roads.Roadway;
import com.example.fxsuite.avatar.roads.Roadworks;
import com.example.fxsuite.avatar.roads.Section;
import com.example.fxsuite.avatar.sim.Vec2;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Made-up places to lay roads in on the roads bench ({@link RoadBench}): each a profile, and the roads through it
 * made of whatever kind across is chosen, in the order they are laid: their lines, what carries them where that is
 * given, what they must clear; any still water; the kind they are shown with first, and where along the first
 * (cm) it is looked at first. Each runs about 500 m, from z = 0 northward.
 */
final class RoadScenes {

    /** A place and the roads through it. */
    record Scene(String name, String section, double at, Profile profile,
                 BiFunction<Section, Map<String, Double>, List<Roadworks.Plan>> roads, double water, List<Knob> knobs) {

        /** A scene with nothing to set: its roads made of the kind alone. */
        Scene(final String name, final String section, final double at, final Profile profile,
              final Function<Section, List<Roadworks.Plan>> roads, final double water) {
            this(name, section, at, profile, (kind, set) -> roads.apply(kind), water, List.of());
        }

        /** Its roads, of {@code kind}, its knobs set as {@code set} says, or as they are at first. */
        List<Roadworks.Plan> roads(final Section kind, final Map<String, Double> set) {
            final Map<String, Double> all = new java.util.HashMap<>();
            knobs.forEach(k -> all.put(k.name(), set.getOrDefault(k.name(), k.value())));
            return roads.apply(kind, all);
        }

        /** Whether it has still water, at {@link #water} high. */
        boolean watered() {
            return !Double.isNaN(water);
        }
    }

    /** Something of a scene to set on the bench: its name, as it is shown; the least, the most, the step, and as it is at first. */
    record Knob(String name, double least, double most, double step, double value) {
    }

    /** How long each road is. */
    static final double LENGTH = 50_000;

    private RoadScenes() {
    }

    /** The scenes, by name, in order. */
    static Map<String, Scene> all() {
        final Map<String, Scene> scenes = new LinkedHashMap<>();
        // flat: barely a lump, the ground falling a little across
        scenes.put("flat", one("flat", "town street", 25_000,
                (x, z, below) -> 0.004 * x + 15 * Math.sin(z / 4_000) * Math.cos(x / 3_000),
                straight(), List.of(), List.of(), Double.NaN));
        // a hill: the road cut through its top, banked up its sides
        scenes.put("hill", one("hill", "two-lane road", 25_000,
                (x, z, below) -> 1_300 * bump(z - 25_000, 9_000) + 0.01 * x,
                straight(), List.of(), List.of(), Double.NaN));
        // a valley too deep to bank across: a viaduct, chosen
        scenes.put("valley", one("valley", "two-lane road", 25_000,
                (x, z, below) -> -2_600 * bump(z - 25_000, 8_000) + 0.006 * x,
                straight(), List.of(), List.of(), Double.NaN));
        // a river crossed square: a bridge, given, held over the water
        final double water = -200;
        scenes.put("river", one("river", "two-lane road", 25_000,
                (x, z, below) -> 0.002 * z - 500 * channel(z - 25_000, 1_500, 600) + 40 * Math.sin(x / 2_500),
                straight(), List.of(new Roadway.Given(22_400, 27_600, Carrier.STRUCTURE)),
                List.of(new Roadway.Clearance(23_000, 27_000, water + 500 + Roadway.DECK)), water));
        // a ridge too high to cut through: a tunnel, chosen
        scenes.put("ridge", one("ridge", "two-lane road", 11_000,
                (x, z, below) -> 3_600 * bump(z - 25_000, 11_000) - 0.005 * x,
                straight(), List.of(), List.of(), Double.NaN));
        // rolling country, a lane winding through it
        scenes.put("rolling", one("rolling", "country lane", 25_000,
                (x, z, below) -> 300 * Math.sin(x / 7_000) + 450 * Math.sin(z / 9_000) + 180 * Math.cos((x + z) / 4_000),
                line(z -> 6_000 * Math.sin(z / LENGTH * Math.PI * 1.5)), List.of(), List.of(), Double.NaN));
        // a loop as a running track is, but as long as it is set, its bends as round, up on a viaduct all round over
        // level ground, as high as it is set: to run round and round on
        scenes.put("loop", new Scene("loop", "two-lane road", 2_000, Profile.level(0), RoadScenes::loop, Double.NaN,
                List.of(new Knob(LOOP_LENGTH, 300, 5_000, 50, 1_000), new Knob(LOOP_RADIUS, 20, 500, 5, 80),
                        new Knob(LOOP_HEIGHT, 2, 40, 0.5, 6), new Knob(LOOP_TILT, 0, 30, 0.5, 6))));
        // a divided road on gentle ground: two carriageways side by side, the median between them grass
        scenes.put("dual", new Scene("dual", "carriageway", 25_000,
                (x, z, below) -> 250 * Math.sin(z / 8_000) + 120 * Math.sin(x / 5_000 + z / 13_000),
                section -> divided("dual", section, line(z -> 2_500 * Math.sin(z / LENGTH * Math.PI)), z -> 0),
                Double.NaN));
        // a divided road across a steep hillside: its carriageways parting, the upper cut in, the lower built out,
        // a gully across both
        scenes.put("mountain", new Scene("mountain", "carriageway", 25_000,
                (x, z, below) -> -0.35 * x + 400 * Math.sin(z / 12_000) - 1_400 * bump(z - 31_000, 2_200),
                section -> divided("mountain", section, straight(), z -> 1_500 * bump(z - 25_000, 10_000)),
                Double.NaN));
        return scenes;
    }

    /** The loop's knobs: how long round, how round its bends, how high its viaduct, in metres; how much its bends tilt. */
    static final String LOOP_LENGTH = "Loop length (m)";
    static final String LOOP_RADIUS = "Bend radius (m)";
    static final String LOOP_HEIGHT = "Viaduct height (m)";
    static final String LOOP_TILT = "Bend tilt (°)";

    /**
     * The loop: two straights side by side, a half-circle bend at each end joining them, so long round as set, its bends
     * as round as set (no rounder than a circle as long round), going north up the east straight from its middle; a
     * structure all round, its surface as high as set over the level ground; tilted into its bends as much as set, run in
     * and out on either side as the road works out.
     */
    static List<Roadworks.Plan> loop(final Section section, final Map<String, Double> set) {
        final double length = set.get(LOOP_LENGTH) * 100;
        final double radius = Math.min(set.get(LOOP_RADIUS) * 100, length / (2 * Math.PI));
        final double half = (length - 2 * Math.PI * radius) / 4;
        final List<Vec2> line = new ArrayList<>();
        final double step = 100;
        for (double z = 0; z < half; z += step) {
            line.add(new Vec2(radius, z));
        }
        final int arc = Math.max(8, (int) Math.ceil(Math.PI * radius / step));
        for (int k = 0; k < arc; k++) {
            final double t = Math.PI * k / arc;
            line.add(new Vec2(radius * Math.cos(t), half + radius * Math.sin(t)));
        }
        for (double z = half; z > -half; z -= step) {
            line.add(new Vec2(-radius, z));
        }
        for (int k = 0; k < arc; k++) {
            final double t = Math.PI + Math.PI * k / arc;
            line.add(new Vec2(radius * Math.cos(t), -half + radius * Math.sin(t)));
        }
        for (double z = -half; z < 0; z += step) {
            line.add(new Vec2(radius, z));
        }
        line.add(line.getFirst());
        final double up = set.get(LOOP_HEIGHT) * 100;
        return List.of(new Roadworks.Plan("loop road", line, section,
                List.of(new Roadway.Given(0, Double.MAX_VALUE, Carrier.STRUCTURE)),
                List.of(new Roadway.Clearance(0, Double.MAX_VALUE, up)),
                Math.tan(Math.toRadians(set.getOrDefault(LOOP_TILT, 0.0)))));
    }

    /** One road, {@code name}, of whatever kind, along {@code line}. */
    private static Scene one(final String name, final String section, final double at, final Profile profile,
                             final List<Vec2> line, final List<Roadway.Given> given,
                             final List<Roadway.Clearance> clearances, final double water) {
        return new Scene(name, section, at, profile,
                kind -> List.of(new Roadworks.Plan(name + " road", line, kind, given, clearances)), water);
    }

    /**
     * A divided road along {@code centre}, of one-way kind {@code section}: two carriageways, side by side, parted
     * further by {@code apart} of z each. A kind paved both sides: one road, along the centre.
     */
    private static List<Roadworks.Plan> divided(final String name, final Section section, final List<Vec2> centre,
                                                final DoubleUnaryOperator apart) {
        if (section.pavedSide() == 0) {
            return List.of(new Roadworks.Plan(name + " road", centre, section, List.of(), List.of()));
        }
        final List<Roadworks.Plan> pair = Roadworks.carriageways(name, centre, section, List.of(), List.of());
        final int side = section.pavedSide();
        final List<Roadworks.Plan> parted = new ArrayList<>();
        for (int k = 0; k < 2; k++) {
            final Roadworks.Plan p = pair.get(k);
            final int out = k == 0 ? side : -side;
            final List<Vec2> moved = new ArrayList<>();
            for (final Vec2 v : p.line()) {
                moved.add(new Vec2(v.x() + out * apart.applyAsDouble(v.z()), v.z()));
            }
            parted.add(new Roadworks.Plan(p.name(), moved, section, p.given(), p.clearances()));
        }
        return parted;
    }

    /** Along z from 0, straight. */
    private static List<Vec2> straight() {
        return line(z -> 0);
    }

    /** Along z from 0, {@code x} of z across. */
    private static List<Vec2> line(final DoubleUnaryOperator x) {
        final List<Vec2> points = new ArrayList<>();
        for (double z = 0; z <= LENGTH + 1e-6; z += Roadway.STEP / 2) {
            points.add(new Vec2(x.applyAsDouble(z), z));
        }
        return points;
    }

    /** A smooth lump, 1 at its middle, about {@code wide} either side. */
    private static double bump(final double d, final double wide) {
        return Math.exp(-(d / wide) * (d / wide));
    }

    /** A channel's depth across it, 1 within {@code half} of its middle, rising to 0 over {@code bank} more. */
    private static double channel(final double d, final double half, final double bank) {
        final double off = Math.abs(d);
        if (off <= half) {
            return 1;
        }
        if (off >= half + bank) {
            return 0;
        }
        final double t = (off - half) / bank;
        return 1 - t * t * (3 - 2 * t);
    }
}
