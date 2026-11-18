package com.example.fxsuite.avatar.bench;

import com.example.fxsuite.avatar.roads.Carrier;
import com.example.fxsuite.avatar.roads.Profile;
import com.example.fxsuite.avatar.roads.Roadway;
import com.example.fxsuite.avatar.sim.Vec2;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;

/**
 * Made-up places to lay a road in on the roads bench ({@link RoadBench}): each a profile, a line across it, what
 * carries it where that is given, what it must clear, any still water; the kind of road it is shown with first, and
 * where along it (cm) it is looked at first.
 * Each road runs 500 m, from z = 0 northward.
 */
final class RoadScenes {

    /** A place and a road through it. */
    record Scene(String name, String section, double at, Profile profile, List<Vec2> line, List<Roadway.Given> given,
                 List<Roadway.Clearance> clearances, double water) {

        /** Whether it has still water, at {@link #water} high. */
        boolean watered() {
            return !Double.isNaN(water);
        }
    }

    /** How long each road is. */
    static final double LENGTH = 50_000;

    private RoadScenes() {
    }

    /** The scenes, by name, in order. */
    static Map<String, Scene> all() {
        final Map<String, Scene> scenes = new LinkedHashMap<>();
        // flat: barely a lump, the ground falling a little across
        scenes.put("flat", new Scene("flat", "town street", 25_000,
                (x, z, below) -> 0.004 * x + 15 * Math.sin(z / 4_000) * Math.cos(x / 3_000),
                straight(), List.of(), List.of(), Double.NaN));
        // a hill: the road cut through its top, banked up its sides
        scenes.put("hill", new Scene("hill", "two-lane road", 25_000,
                (x, z, below) -> 1_300 * bump(z - 25_000, 9_000) + 0.01 * x,
                straight(), List.of(), List.of(), Double.NaN));
        // a valley too deep to bank across: a viaduct, chosen
        scenes.put("valley", new Scene("valley", "two-lane road", 25_000,
                (x, z, below) -> -2_600 * bump(z - 25_000, 8_000) + 0.006 * x,
                straight(), List.of(), List.of(), Double.NaN));
        // a river crossed square: a bridge, given, held over the water
        final double water = -200;
        scenes.put("river", new Scene("river", "two-lane road", 25_000,
                (x, z, below) -> 0.002 * z - 500 * channel(z - 25_000, 1_500, 600) + 40 * Math.sin(x / 2_500),
                straight(), List.of(new Roadway.Given(22_400, 27_600, Carrier.STRUCTURE)),
                List.of(new Roadway.Clearance(23_000, 27_000, water + 500 + Roadway.DECK)), water));
        // a ridge too high to cut through: a tunnel, chosen
        scenes.put("ridge", new Scene("ridge", "two-lane road", 11_000,
                (x, z, below) -> 3_600 * bump(z - 25_000, 11_000) - 0.005 * x,
                straight(), List.of(), List.of(), Double.NaN));
        // rolling country, a lane winding through it
        scenes.put("rolling", new Scene("rolling", "country lane", 25_000,
                (x, z, below) -> 300 * Math.sin(x / 7_000) + 450 * Math.sin(z / 9_000) + 180 * Math.cos((x + z) / 4_000),
                line(z -> 6_000 * Math.sin(z / LENGTH * Math.PI * 1.5)), List.of(), List.of(), Double.NaN));
        return scenes;
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
