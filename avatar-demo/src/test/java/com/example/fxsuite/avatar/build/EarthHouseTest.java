package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Block;
import com.example.fxsuite.avatar.sim.Body;
import com.example.fxsuite.avatar.sim.Controls;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Obstacles;
import com.example.fxsuite.avatar.sim.Place;
import com.example.fxsuite.avatar.sim.Vec3;
import com.example.fxsuite.avatar.sim.Volume;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The earth house: made to a size it can stand at; open at its door and its window; walked into through
 * its door, stood up in under its roof, kept out by its walls elsewhere; built of what is chosen.
 */
class EarthHouseTest {

    private static final double FRAME = 1.0 / 60;
    private static final EarthHouse HOUSE = EarthHouse.standard();

    /** The house standing at the world's middle, its front to +z, alone. */
    private static Obstacles world(final EarthHouse house) {
        final List<Solid> solids = house.solids(EarthHouse.palette());
        return Course.of(List.of(Town.building(solids, new Place(0, 0, 0, 0), 900, house.shape(), 1))).obstacles();
    }

    private static Body walk(final Obstacles world, final Vec3 from, final double yaw, final double seconds) {
        Body body = Body.standing(from, yaw);
        for (int i = 0; i < seconds / FRAME; i++) {
            body = body.step(new Controls(1, 0, 0, 0, false), FRAME, world);
        }
        return body;
    }

    @Test
    void itIsMadeToASizeItCanStandAt() {
        assertThrows(IllegalArgumentException.class, () -> new EarthHouse.Size(200, 245, 50, 170, 35, 95, 205, 70, true),
                "too small");
        assertThrows(IllegalArgumentException.class, () -> new EarthHouse.Size(560, 245, 150, 170, 35, 95, 205, 70, true),
                "walls too thick");
        assertThrows(IllegalArgumentException.class, () -> new EarthHouse.Size(560, 245, 50, 170, 35, 95, 240, 70, true),
                "its door into its eave");
        assertThrows(IllegalArgumentException.class, () -> new EarthHouse.Size(560, 245, 50, 170, 35, 250, 205, 70, true),
                "its door wider than it can take");
        final EarthHouse wide = EarthHouse.of(new EarthHouse.Size(900, 300, 70, 250, 50, 110, 220, 0, false));
        assertEquals(450, wide.outside(), 1e-9);
        assertEquals(380, wide.inside(), 1e-9, "as thick as it is made");
        assertTrue(wide.top() > HOUSE.top(), "a bigger roof, higher");
    }

    @Test
    void itsRoofSitsOnItsWallsItsEaveDroopingOverThem() {
        // its roof's underside meets its walls' outside at their height, and rises inward over the room
        assertTrue(HOUSE.under(HOUSE.inside()) > HOUSE.size().walls());
        assertTrue(HOUSE.under(HOUSE.eave()) > Body.TALL, "its eave walked under");
        assertTrue(HOUSE.over(HOUSE.eave()) >= HOUSE.size().walls() - 1e-9, "no lower than its walls");
        assertEquals(HOUSE.over(0), HOUSE.top(), 1e-9);
    }

    @Test
    void itIsWalkedIntoThroughItsDoorAndKeptOutElsewhere() {
        final Obstacles world = world(HOUSE);
        // from before its door, walking in (its front +z: heading back, 180)
        final Body in = walk(world, new Vec3(0, 0, HOUSE.outside() + 300), 180, 4);
        final double out = Math.hypot(in.position().x(), in.position().z());
        assertTrue(out < HOUSE.inside(), "inside, " + out + " from its middle");
        assertTrue(in.height() <= EarthHouse.FLOOR + 1e-6, "on its floor");
        // from its side, walking at it: kept out by its wall
        final Body side = walk(world, new Vec3(-HOUSE.outside() - 300, 0, 0), 90, 4);
        assertTrue(Math.hypot(side.position().x(), side.position().z()) > HOUSE.outside(), "kept out");
    }

    @Test
    void insideThereIsRoomToStandUpUnderItsRoof() {
        final Obstacles world = world(HOUSE);
        final double ceiling = world.ceiling(0, 0, Body.RADIUS, EarthHouse.FLOOR + 1);
        assertTrue(ceiling > EarthHouse.FLOOR + Body.TALL, "its roof over its middle, " + ceiling);
        assertTrue(world.ceiling(HOUSE.inside() - Body.RADIUS - 5, 0, Body.RADIUS, EarthHouse.FLOOR + 1)
                > EarthHouse.FLOOR + Body.TALL, "and by its walls");
    }

    @Test
    void itsDoorwayAndWindowAreOpenItsWallsSolid() {
        final List<Volume> shape = HOUSE.shape();
        final double middle = (HOUSE.inside() + HOUSE.outside()) / 2;
        // in its doorway, from its floor up to under its door's head: open
        final Block doorway = new Block(0, middle, 20, 20, 0, EarthHouse.FLOOR + 1, HOUSE.size().doorHigh() - 1);
        assertTrue(shape.stream().noneMatch(v -> v.overlaps(doorway)), "its doorway open");
        // in its window, between its sill and its head: open
        final double t = Math.toRadians(EarthHouse.WINDOW_AT);
        final Block window = new Block(middle * Math.sin(t), middle * Math.cos(t), 10, 10, 0, EarthHouse.SILL + 1,
                HOUSE.windowTop() - 1);
        assertTrue(shape.stream().noneMatch(v -> v.overlaps(window)), "its window open");
        // its walls elsewhere solid
        final Block wall = new Block(-middle, 0, 10, 10, 0, 50, 150);
        assertTrue(shape.stream().anyMatch(v -> v.overlaps(wall)), "its walls solid");
    }

    @Test
    void itIsBuiltOfWhatIsChosen() {
        final Building building = HOUSE.building();
        final Palette palette = building.palettes().getFirst();
        // red earth walls, its chimney too; thatch, no tufts on it
        final List<Solid> red = building.dressed(List.of(building.option("Walls", "Red earth plaster"),
                building.option("Roof", "Thatch")), palette);
        assertTrue(red.stream().filter(s -> s.role() == Role.WALL).allMatch(s -> s.material() == Material.RED_EARTH));
        assertTrue(red.stream().filter(s -> s.role() == Role.SPIRE).allMatch(s -> s.material() == Material.RED_EARTH));
        assertTrue(red.stream().filter(s -> s.role() == Role.ROOF).allMatch(s -> s.material() == Material.THATCH));
        assertTrue(red.stream().noneMatch(s -> s.name().contains("tuft")), "no grass growing on thatch");
        // its own way: mud, turf, grass growing on it, its window shuttered
        final List<Solid> own = building.dressed(building.own(), palette);
        assertTrue(own.stream().anyMatch(s -> s.name().contains("tuft")), "grass growing on turf");
        assertTrue(own.stream().anyMatch(s -> s.name().startsWith("window, shutter") && s.material() == Material.TIMBER));
        // glazed
        final List<Solid> glazed = building.dressed(List.of(building.option("Window", "Glazed")), palette);
        assertTrue(glazed.stream().anyMatch(s -> s.material() == Material.GLASS));
        // no window, no chimney: none of either
        final EarthHouse plain = EarthHouse.of(new EarthHouse.Size(560, 245, 50, 170, 35, 95, 205, 0, false));
        final List<Solid> bare = plain.solids(EarthHouse.palette());
        assertTrue(bare.stream().noneMatch(s -> s.name().startsWith("window") || s.name().equals("chimney")));
        assertFalse(bare.isEmpty());
    }
}
