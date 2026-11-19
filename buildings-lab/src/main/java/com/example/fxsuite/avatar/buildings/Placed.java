package com.example.fxsuite.avatar.buildings;

import com.example.fxsuite.avatar.build.Point;
import com.example.fxsuite.avatar.build.Solid;
import com.example.fxsuite.avatar.lifts.HighRiseBank;
import com.example.fxsuite.avatar.lifts.Mover;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Place;

import java.util.List;

/**
 * A bank of lifts, made about its own middle facing along ({@link HighRiseBank}), set in a building: its middle at
 * ({@code x}, {@code z}), turned {@code yaw} (a quarter turn or more: 90 faces it east, -90 west, 180 back). Its
 * shafts' walls turned and moved with it; each of its moving parts going its way turned and moved; the hole it cuts
 * in each floor turned and moved, square still.
 *
 * @param bank the bank, about its own middle, facing along
 * @param x    across, where its middle is
 * @param z    along, where its middle is
 * @param yaw  how it is turned, degrees, a whole number of quarter turns
 */
record Placed(HighRiseBank bank, double x, double z, double yaw) {

    Placed {
        if (Math.abs(yaw / 90 - Math.round(yaw / 90)) > 1e-9) {
            throw new IllegalArgumentException("a bank turned a whole number of quarter turns, not " + yaw);
        }
    }

    /** Where ({@code across}, {@code along}) in the bank's frame is in the building's: across, along. */
    double[] at(final double across, final double along) {
        final double r = Math.toRadians(yaw);
        // turned as the game turns a thing: its right (cos, -sin), its ahead (sin, cos)
        final double c = Math.cos(r);
        final double s = Math.sin(r);
        return new double[] {x + c * across + s * along, z - s * across + c * along};
    }

    /** Its shafts' walls, {@code slab} the floors' thickness, in the building. */
    List<Solid> shafts(final double slab) {
        return bank.shafts(slab).stream().map(this::placed).toList();
    }

    /** Its landings' doors standing shut, in the building: its lifts not at work. */
    List<Solid> landingsShut() {
        return bank.landingsShut().stream().map(this::placed).toList();
    }

    private Solid placed(final Solid solid) {
        return new Solid(solid.name(), solid.role(), solid.material(), solid.corners().stream().map(p -> {
            final double[] w = at(p.across(), p.along());
            return new Point(w[0], w[1], p.up());
        }).toList());
    }

    /** Its moving parts, drawn by the keys given, each going its way in the building, turned with it. */
    List<Mover> movers(final int car, final int carDoor, final int landingDoor) {
        return bank.movers(car, carDoor, landingDoor).stream().map(m -> {
            final Course.Route was = m.route();
            final Course.Route turned = seconds -> {
                final Place p = was.at(seconds);
                final double[] w = at(p.x(), p.z());
                return new Place(w[0], w[1], p.height(), p.yaw() + yaw);
            };
            return new Mover(m.name(), m.boxes(), turned, m.key());
        }).toList();
    }

    /** The hole it cuts in each floor it goes through, in the building: least and most across, least and most along. */
    double[] hole() {
        final double[] h = bank.hole();
        final double[] a = at(h[0], h[2]);
        final double[] b = at(h[1], h[3]);
        return new double[] {Math.min(a[0], b[0]), Math.max(a[0], b[0]), Math.min(a[1], b[1]), Math.max(a[1], b[1])};
    }

    /** How deep it is, back to front: the room it takes across a core when it faces across. */
    double depth() {
        return bank.deep() + 2 * HighRiseBank.GAP + 2 * HighRiseBank.SHAFT_WALL;
    }

    /** How long it is, side to side: the room it takes along a core when it faces across. */
    double length() {
        return bank.cars() * bank.pitch() + HighRiseBank.SHAFT_WALL;
    }
}
