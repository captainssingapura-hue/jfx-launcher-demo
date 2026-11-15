package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Body;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Obstacles;
import com.example.fxsuite.avatar.sim.Outline;
import com.example.fxsuite.avatar.sim.Vec2;
import com.example.fxsuite.avatar.sim.Volume;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A tower with a garden on its roof, composed: a pavilion about half as wide as the roof, where there
 * is most room, on a lawn; trees and shrubs round it, clear of it and of the roof's edge; all of it in
 * the way as it is shaped, in the tower's frame, and carried by the tower's piece in the town.
 */
class RoofGardenTest {

    /** Each of the town's towers, with its garden as the town has it. */
    private static List<RoofGarden> gardens() {
        return List.of(
                RoofGarden.of(Towers.setback(), Pavilion.Footprint.SQUARE, Pavilion.Roof.PYRAMID, Material.GLAZED,
                        Material.LACQUER),
                RoofGarden.of(Towers.round(), Pavilion.Footprint.ROUND, Pavilion.Roof.PYRAMID, Material.GREEN_GLAZED,
                        Material.LACQUER),
                RoofGarden.of(Towers.oval(), Pavilion.Footprint.OCTAGON, Pavilion.Roof.PYRAMID, Material.GREY_TILE,
                        Material.STONE),
                RoofGarden.of(Towers.rounded(), Pavilion.Footprint.RECTANGLE, Pavilion.Roof.HIP, Material.GREEN_GLAZED,
                        Material.LACQUER),
                RoofGarden.of(Towers.lBlock(), Pavilion.Footprint.HEXAGON, Pavilion.Roof.PYRAMID, Material.GLAZED,
                        Material.LACQUER),
                RoofGarden.of(Towers.bowFront(), Pavilion.Footprint.RECTANGLE, Pavilion.Roof.FLAT, Material.CONCRETE,
                        Material.METAL));
    }

    private static List<Vec2> roof(final Tower tower) {
        return tower.sections().getLast().plan().layout().shaped().getFirst().stream()
                .map(p -> new Vec2(p.x(), p.y())).toList();
    }

    private static double gap(final List<Vec2> roof, final double x, final double z) {
        final Vec2 edge = Outline.nearest(roof, x, z);
        return Math.hypot(edge.x() - x, edge.z() - z);
    }

    @Test
    void aPavilionAboutHalfTheRoofsWidthWhereThereIsMostRoom() {
        for (final RoofGarden garden : gardens()) {
            final List<Vec2> roof = roof(garden.tower());
            final double[] box = {roof.stream().mapToDouble(Vec2::x).min().orElseThrow(),
                    roof.stream().mapToDouble(Vec2::z).min().orElseThrow(),
                    roof.stream().mapToDouble(Vec2::x).max().orElseThrow(),
                    roof.stream().mapToDouble(Vec2::z).max().orElseThrow()};
            final double narrowest = Math.min(box[2] - box[0], box[3] - box[1]);
            final double width = garden.pavilion().spec().width();
            final String name = garden.tower().name();
            assertTrue(width <= narrowest / 2 + 1e-6, name + ": no more than half: " + width);
            // no less than a quarter; a sixth, where a core stands up through the roof's middle and it goes beside it
            final double least = garden.tower().structure().cores().isEmpty() ? narrowest / 4 : narrowest / 6;
            assertTrue(width >= least, name + ": no less than " + least + ": " + width + " of " + narrowest);
            // all of it on the roof, clear of its edge
            final double x = garden.place().x();
            final double z = garden.place().z();
            assertTrue(Outline.inside(roof, x, z), name);
            assertTrue(gap(roof, x, z) >= garden.pavilion().reach() + RoofGarden.EDGE - 1e-6, name + ": clear of its edge");
            assertEquals(garden.tower().height() + RoofGarden.LAWN, garden.place().height(), 1e-9, name + ": on its lawn");
        }
    }

    @Test
    void plantsRoundItClearOfItAndOfTheEdgeTheSameEveryTime() {
        final List<RoofGarden> again = gardens();
        int k = 0;
        for (final RoofGarden garden : gardens()) {
            final List<Vec2> roof = roof(garden.tower());
            final String name = garden.tower().name();
            assertFalse(garden.plants().isEmpty(), name + ": some plants");
            for (final RoofGarden.Plant plant : garden.plants()) {
                assertTrue(Outline.inside(roof, plant.x(), plant.z()), name);
                assertTrue(gap(roof, plant.x(), plant.z()) >= RoofGarden.EDGE, name + ": clear of the edge");
                assertTrue(Math.hypot(plant.x() - garden.place().x(), plant.z() - garden.place().z())
                        > garden.pavilion().reach() + 150, name + ": clear of the pavilion and its steps");
            }
            assertEquals(garden.plants(), again.get(k++).plants(), name + ": the same every time");
        }
    }

    @Test
    void inTheWayAsItIsWalkedOnUnderAndRound() {
        for (final RoofGarden garden : gardens()) {
            final List<Volume> shape = garden.on(TowerBuildings.shape(garden.tower()));
            final Obstacles world = new Obstacles(List.of()).with(shape, shape.stream().map(v -> 0).toList());
            final String name = garden.tower().name();
            final Pavilion pavilion = garden.pavilion();
            final double lawn = garden.lawn();
            // in the pavilion's middle: on its platform, its roof's underside over it
            final double x = garden.place().x();
            final double z = garden.place().z();
            assertEquals(lawn + pavilion.floor(), world.floor(x, z, 16, lawn + pavilion.floor() + 20), 1e-6, name);
            assertEquals(lawn + pavilion.eaves(), world.ceiling(x, z, 32, lawn + pavilion.floor() + Body.TALL), 1e-6, name);
            // out on the lawn, between the plants: the lawn under it
            final RoofGarden.Plant plant = garden.plants().getFirst();
            assertEquals(lawn, world.floor(plant.x() + plant.size() + 60, plant.z(), 16, lawn + 20), 1e-6,
                    name + ": on the lawn beside a plant");
            // a plant's trunk, or a shrub, in the way
            final double trunk = plant.kind() == RoofGarden.Kind.SHRUB ? plant.size() * 0.7 : plant.size() * 0.12;
            assertTrue(world.slide(plant.x() - trunk - 20, plant.z(), 100, 0, 32, lawn + 20, lawn + Body.TALL).blocked()
, name + ": its trunk in the way");
        }
    }

    @Test
    void aPoolSunkInTheRoofBesideThePavilionClearOfItAndOfTheEdge() {
        for (final RoofGarden garden : gardens()) {
            final List<Vec2> roof = roof(garden.tower());
            final RoofGarden.Pool pool = garden.pool();
            final String name = garden.tower().name();
            for (final Vec2 c : pool.outline(RoofGarden.RIM)) {
                assertTrue(Outline.inside(roof, c.x(), c.z()), name + ": its rim on the roof");
                assertTrue(gap(roof, c.x(), c.z()) >= RoofGarden.EDGE - 1e-6, name + ": clear of the edge");
            }
            assertTrue(pool.gap(garden.place().x(), garden.place().z())
                    >= garden.pavilion().reach() + RoofGarden.RIM, name + ": clear of the pavilion and its steps");
            for (final RoofGarden.Plant plant : garden.plants()) {
                assertTrue(pool.gap(plant.x(), plant.z()) > RoofGarden.RIM, name + ": no plant in it");
            }
            final List<Volume> shape = garden.on(TowerBuildings.shape(garden.tower()));
            final Obstacles world = new Obstacles(List.of()).with(shape, shape.stream().map(v -> 0).toList());
            // in it: its floor under, down in the roof; beside it, its rim, and the lawn beyond
            assertEquals(garden.poolFloor(), world.floor(pool.x(), pool.z(), 16, garden.lawn() + 20), 1e-6,
                    name + ": on the pool's floor");
            assertEquals(garden.lawn() + RoofGarden.RIM_HIGH,
                    world.floor(pool.x() + pool.halfAcross() + RoofGarden.RIM / 2, pool.z(), 5, garden.lawn() + 30), 1e-6,
                    name + ": on its rim");
            // a round down into it: through the water, onto its floor
            final com.example.fxsuite.avatar.sim.Hit hit = world.ray(new com.example.fxsuite.avatar.sim.Vec3(pool.x(),
                    -(garden.lawn() + 1000), pool.z()), new com.example.fxsuite.avatar.sim.Vec3(0, 1, 0), 5000);
            assertEquals(garden.poolFloor(), -hit.at().y(), 1e-6, name + ": down to its floor");
        }
    }

    @Test
    void theTownsTowersCarryTheirGardens() {
        Course.town(Town::pieces);
        final List<RoofGarden> gardens = gardens();
        for (int k = 3; k < Town.SITES.size(); k++) {
            final Course.Piece tower = Town.pieces().get(k);
            final RoofGarden garden = gardens.get(k - 3);
            assertEquals(garden.on(TowerBuildings.shape(garden.tower())).size(), tower.volumes().size(),
                    Town.SITES.get(k).name() + ": in the way as the tower and its garden");
            assertTrue(Town.solids(k).stream().anyMatch(s -> s.name().startsWith("pavilion, ")),
                    Town.SITES.get(k).name() + ": its pavilion drawn");
            assertTrue(Town.solids(k).stream().anyMatch(s -> s.material() == Material.LEAVES),
                    Town.SITES.get(k).name() + ": its plants drawn");
        }
    }
}
