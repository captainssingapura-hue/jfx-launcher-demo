package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Block;
import com.example.fxsuite.avatar.sim.Body;
import com.example.fxsuite.avatar.sim.Controls;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Damage;
import com.example.fxsuite.avatar.sim.Furniture;
import com.example.fxsuite.avatar.sim.Obstacles;
import com.example.fxsuite.avatar.sim.Place;
import com.example.fxsuite.avatar.sim.Vec3;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The park, walked in: its floor at the ground; each pond's kerb stepped over, its water waded, its bridge
 * walked out to its island and its railings in the way; its trees, lamp posts, benches, bins and planters
 * standing where they should, each to be broken.
 */
class ParkTest {

    private static final double FRAME = 1.0 / 60;

    /** The park alone, laid out as it is in the town. */
    private static Obstacles world() {
        return Course.of(Town.park()).obstacles();
    }

    /** A body walking from {@code from} heading {@code yaw}, for {@code seconds}. */
    private static Body walk(final Obstacles world, final Vec3 from, final double yaw, final double seconds) {
        Body body = Body.standing(from, yaw);
        for (int i = 0; i < seconds / FRAME; i++) {
            body = body.step(new Controls(1, 0, 0, 0, false), FRAME, world);
        }
        return body;
    }

    /** Where the pond on side {@code side} (-1 west, 1 east) has its middle. */
    private static Vec3 pond(final double side) {
        return new Vec3(Town.middle().x() + side * Town.POND_OUT, 0, Town.middle().z());
    }

    @Test
    void itsFloorIsAtTheGroundItsPavingBesideItsLawn() {
        final List<Course.Piece> garden = Town.park().stream().filter(p -> p.kind() == Course.Kind.GARDEN).toList();
        final Course.Piece lawn = garden.getFirst();
        assertEquals(Course.GARDEN_LAWN, lawn.tint());
        assertEquals(0, lawn.block().top(), 1e-9, "the lawn at the ground");
        assertTrue(garden.stream().filter(p -> p.tint() == Course.GARDEN_PAVING && p.solid().isEmpty())
                .allMatch(p -> p.block().top() <= 1), "the courtyard and the path a hair over it, beside it");
    }

    @Test
    void aPondsKerbIsSteppedOverAndItsWaterWaded() {
        // from the lawn south of the east pond, walking north, in over its kerb: on the bottom, the water over the feet
        final Vec3 water = pond(1);
        final Body in = walk(world(), new Vec3(water.x() + 1000, 0, water.z() - Town.POND - Town.RIM - 300), 0, 3);
        final double out = Math.hypot(in.position().x() - water.x(), in.position().z() - water.z());
        assertTrue(out < Town.POND, "in the water, " + out + " from its middle");
        assertEquals(0, in.height(), 1e-6, "wading, on the bottom");
        assertTrue(Town.WATER_TOP > 0 && Town.WATER_TOP < Town.KERB_TOP, "the water over the feet, under the kerb");
    }

    @Test
    void itsBridgeIsWalkedOutToItsIslandAndItsRailingsAreInTheWay() {
        final Vec3 water = pond(1);
        // from the lawn east of the pond, walking west along the bridge: up onto it, out to the island
        final Body over = walk(world(), new Vec3(water.x() + Town.POND + Town.RIM + 300, 0, water.z()), -90, 6);
        final double out = Math.hypot(over.position().x() - water.x(), over.position().z() - water.z());
        assertTrue(out < Town.ISLAND, "on the island, " + out + " from its middle");
        assertTrue(over.height() >= Town.KERB_TOP - 1e-6, "up on it");
        // on the bridge, over the water, walking off its side: the railing stops it
        final Vec3 on = new Vec3(water.x() + (Town.ISLAND + Town.POND) / 2, -Town.KERB_TOP, water.z());
        final Body side = walk(world(), on, 0, 2);
        assertTrue(Math.abs(side.position().z() - water.z()) < Town.BRIDGE / 2, "kept on the bridge by its railing");
    }

    @Test
    void itsTreesLampsAndFurnitureStandClearOfItsPathItsPondsAndEachOtherEachToBeBroken() {
        final List<Course.Piece> park = Town.park();
        final List<Course.Piece> things = park.stream().filter(p -> p.kind() == Course.Kind.TREE
                || p.kind() == Course.Kind.LAMP || p.kind() == Course.Kind.FURNITURE).toList();
        assertTrue(things.stream().filter(p -> p.kind() == Course.Kind.TREE).count() >= 15, "trees round its edge");
        assertTrue(things.stream().filter(p -> p.kind() == Course.Kind.LAMP).count() >= 8, "lamps along the path");
        assertTrue(things.stream().anyMatch(p -> p.furniture() == Furniture.BENCH));
        assertTrue(things.stream().anyMatch(p -> p.furniture() == Furniture.BIN));
        assertTrue(things.stream().anyMatch(p -> p.furniture() == Furniture.PLANTER));
        final Course.Piece path = park.get(2);
        assertEquals(Course.GARDEN_PAVING, path.tint());
        for (int a = 0; a < things.size(); a++) {
            final Course.Piece one = things.get(a);
            assertTrue(Double.isFinite(Damage.toughness(one)), one.kind() + ": to be broken");
            assertFalse(path.block().overlaps(one.block()), one.kind() + " on the path");
            for (final double side : new double[] {-1, 1}) {
                final Vec3 water = pond(side);
                assertTrue(Math.hypot(one.block().x() - water.x(), one.block().z() - water.z()) > Town.POND + Town.RIM,
                        one.kind() + " in a pond");
            }
            for (int b = a + 1; b < things.size(); b++) {
                assertFalse(one.block().overlaps(things.get(b).block()), one.kind() + " and " + things.get(b).kind());
            }
        }
        // the furniture loose, standing on the ground
        for (final Course.Piece one : things) {
            if (one.kind() == Course.Kind.FURNITURE) {
                assertTrue(one.kind().loose());
                assertEquals(0, one.place().height(), 1e-9);
            }
        }
    }

    @Test
    void aPavilionOnAnIslandStandsOnIt() {
        final List<Course.Piece> pavilions = Town.park().stream().filter(p -> p.kind() == Course.Kind.BUILDING).toList();
        int onIslands = 0;
        for (final Course.Piece pavilion : pavilions) {
            final Place at = pavilion.place();
            if (Math.abs(Math.abs(at.x() - Town.middle().x()) - Town.POND_OUT) < 1) {
                onIslands++;
                assertEquals(Town.KERB_TOP, at.height(), 1e-9);
                final Block box = pavilion.block();
                assertEquals(Town.KERB_TOP, box.bottom(), 1e-9, "its box from the island's top");
            }
        }
        assertEquals(2, onIslands);
    }
}
