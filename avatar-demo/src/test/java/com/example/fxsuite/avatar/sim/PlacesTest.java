package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The world's level: what is near a box exactly the things whose boxes meet it, each once, in the order they were
 * added; still so as things move, are taken away, or reach over very many cells.
 */
class PlacesTest {

    /** A thing with a name, so two alike are still two. */
    private record Thing(int id) {
    }

    private static Box scatter(final int k, final int salt, final double size) {
        final double x = 100_000 * (Num.hash(k, salt, 1) - 0.5);
        final double z = 100_000 * (Num.hash(k, salt, 2) - 0.5);
        final double u = 3_000 * Num.hash(k, salt, 3);
        return new Box(x, x + size * Num.hash(k, salt, 4), z, z + size * Num.hash(k, salt, 5), u,
                u + 1_000 * Num.hash(k, salt, 6));
    }

    private static List<Thing> meeting(final List<Thing> things, final List<Box> boxes, final Box box) {
        final List<Thing> met = new ArrayList<>();
        for (int k = 0; k < things.size(); k++) {
            if (boxes.get(k) != null && boxes.get(k).meets(box)) {
                met.add(things.get(k));
            }
        }
        return met;
    }

    @Test
    void whatIsNearABoxTheThingsWhoseBoxesMeetItInTheOrderTheyWereAdded() {
        final Places<Thing> places = new Places<>();
        final List<Thing> things = new ArrayList<>();
        final List<Box> boxes = new ArrayList<>();
        for (int k = 0; k < 3_000; k++) {
            // most a few metres, some a block or two, a few over a whole district: listed as everywhere
            final double size = k % 100 == 0 ? 60_000 : k % 10 == 0 ? 6_000 : 600;
            final Thing t = new Thing(k);
            final Box b = scatter(k, 0, size);
            places.add(t, b);
            things.add(t);
            boxes.add(b);
        }
        for (int q = 0; q < 300; q++) {
            final Box box = scatter(q, 7, 4_000);
            assertEquals(meeting(things, boxes, box), places.near(box), "near " + box);
        }
        assertEquals(3_000, places.size());
    }

    @Test
    void stillSoAsThingsMoveAndAreTakenAway() {
        final Places<Thing> places = new Places<>();
        final List<Thing> things = new ArrayList<>();
        final List<Box> boxes = new ArrayList<>();
        for (int k = 0; k < 1_000; k++) {
            final Thing t = new Thing(k);
            final Box b = scatter(k, 0, k % 50 == 0 ? 80_000 : 800);
            places.add(t, b);
            things.add(t);
            boxes.add(b);
        }
        for (int step = 1; step <= 5; step++) {
            for (int k = 0; k < things.size(); k++) {
                if (boxes.get(k) == null) {
                    continue;
                }
                if (Num.hash(k, step, 9) < 0.05) {
                    places.remove(things.get(k));
                    boxes.set(k, null);
                    continue;
                }
                // some a little (the same cells, mostly), some far
                final Box was = boxes.get(k);
                final Box b = Num.hash(k, step, 8) < 0.5
                        ? new Box(was.minX() + 30, was.maxX() + 30, was.minZ(), was.maxZ(), was.low(), was.high())
                        : scatter(k, step, k % 50 == 0 ? 80_000 : 800);
                places.move(things.get(k), b);
                boxes.set(k, b);
            }
            for (int q = 0; q < 100; q++) {
                final Box box = scatter(q, 100 + step, 5_000);
                assertEquals(meeting(things, boxes, box), places.near(box), "step " + step + ", near " + box);
            }
        }
        assertEquals(boxes.stream().filter(b -> b != null).count(), places.size());
    }

    @Test
    void eachThingOnceTwoAlikeStillTwo() {
        final Places<Thing> places = new Places<>();
        final Thing one = new Thing(1);
        final Thing same = new Thing(1);
        places.add(one, new Box(0, 10, 0, 10, 0, 10));
        places.add(same, new Box(0, 10, 0, 10, 0, 10));
        assertEquals(2, places.near(new Box(-5, 5, -5, 5, 0, 1)).size());
        assertThrows(IllegalArgumentException.class, () -> places.add(one, new Box(0, 1, 0, 1, 0, 1)));
        places.remove(one);
        assertFalse(places.has(one));
        assertTrue(places.has(same));
        assertThrows(IllegalArgumentException.class, () -> places.move(one, new Box(0, 1, 0, 1, 0, 1)));
        // over a cell's edge it is in both, and given once
        places.add(one, new Box(Places.CELL - 10, Places.CELL + 10, 0, 10, 0, 10));
        assertEquals(List.of(same, one), places.near(new Box(0, 3 * Places.CELL, 0, 10, 0, 10)));
    }
}
