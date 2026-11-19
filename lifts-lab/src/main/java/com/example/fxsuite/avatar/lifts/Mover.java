package com.example.fxsuite.avatar.lifts;

import com.example.fxsuite.avatar.build.Solid;
import com.example.fxsuite.avatar.sim.Course;

import java.util.List;

/**
 * One moving part of a lift: a cabin, a car, a door's leaf, a step. Its boxes in its own frame, square about its middle
 * across and along, its foot at nothing up; where it is at each moment ({@code route}, a function of time alone); and
 * the key it is drawn by, as buildings are. To the simulation, a piece that goes its own way: whoever stands on it is
 * carried with it, and its boxes are in the way wherever it is.
 *
 * @param name  what it is
 * @param boxes what it is made of, in its own frame
 * @param route where it is at each moment: its foot's height, its middle across and along
 * @param key   the key it is drawn by
 */
record Mover(String name, List<Box> boxes, Course.Route route, int key) {

    Mover {
        boxes = List.copyOf(boxes);
        for (final Box b : boxes) {
            if (b.u0() < -1e-9) {
                throw new IllegalArgumentException(name + ": its foot at nothing up, not " + b.u0());
            }
        }
    }

    /** What it is made of to the simulation: its box all round (square about its middle), and each box solid. */
    Course.Shape shape() {
        double across = 0;
        double along = 0;
        double up = 0;
        for (final Box b : boxes) {
            across = Math.max(across, Math.max(Math.abs(b.a0()), Math.abs(b.a1())));
            along = Math.max(along, Math.max(Math.abs(b.l0()), Math.abs(b.l1())));
            up = Math.max(up, b.u1());
        }
        return new Course.Shape(new Course.Section(0, 0, up / 2, across, along, up / 2),
                boxes.stream().map(Box::section).toList());
    }

    /** What is drawn of it. */
    List<Solid> solids() {
        return boxes.stream().map(Box::solid).toList();
    }

    /** It, as a piece of the world: where its route has it at the start, going its way, drawn as a building is. */
    Course.Piece piece() {
        return Course.Piece.moving(shape(), route.at(0), Course.Kind.BUILDING, key, route);
    }
}
