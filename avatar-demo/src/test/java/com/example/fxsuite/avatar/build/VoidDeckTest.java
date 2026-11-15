package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Body;
import com.example.fxsuite.avatar.sim.Controls;
import com.example.fxsuite.avatar.sim.Obstacles;
import com.example.fxsuite.avatar.sim.Vec3;
import com.example.fxsuite.avatar.sim.Volume;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A tower with a void deck: a storey open but for its columns (the whole grid's) and a wing enclosed
 * at one end, between the solid sections under and over it; a railing round its open edge; on it, a
 * garden, its pool sunk into the section under it and plants (no bar or stall: the game's), all clear of the
 * columns and each other, and no taller than the deck.
 */
class VoidDeckTest {

    private static Obstacles world(final List<Volume> shape) {
        return new Obstacles(List.of()).with(shape, shape.stream().map(v -> 0).toList());
    }

    @Test
    void itStandsItsWallsOnItsSkeletonItsDeckAsHighAsAsked() {
        for (final Tower tower : List.of(Towers.voidDeck(), Towers.voidDeck(5, 380, 0), Towers.voidDeck(20, 1200, 4))) {
            assertEquals(List.of(), tower.problems(), tower.toString());
            assertEquals(List.of(), TowerBuildings.mismatches(TowerBuildings.of(tower), tower), tower.toString());
            final Tower.Section deck = tower.sections().get(1);
            assertTrue(deck.open());
            assertEquals(Towers.DECK_TOWER_FLOORS, tower.floors());
            assertEquals((Towers.DECK_TOWER_FLOORS - 1) * 380 + deck.height(), tower.height(), 1e-6);
        }
        assertThrows(IllegalArgumentException.class, () -> Towers.voidDeck(1, 760, 2), "too low");
        assertThrows(IllegalArgumentException.class, () -> Towers.voidDeck(15, 200, 2), "too low a deck");
        assertThrows(IllegalArgumentException.class, () -> Towers.voidDeck(15, 760, 6), "a wing past its end");
    }

    @Test
    void openButForItsColumnsItsWingAndItsRailing() {
        final Tower tower = Towers.voidDeck();
        final Tower.Section deck = tower.sections().get(1);
        final double floor = tower.base(1);
        final double top = floor + deck.tall();
        final Obstacles world = world(TowerBuildings.shape(tower));
        // in a bay of it, between columns: its floor under, the section over it overhead
        final double x = 1200;
        final double z = -400;
        assertEquals(floor, world.floor(x, z, 16, floor + 20), 1e-6);
        assertEquals(top, world.ceiling(x, z, 32, floor + Body.TALL), 1e-6);
        // its frame's columns: the whole grid's, but those of its core (on its middle line, in its walls) and inside the wing
        // (the wing's own perimeter columns set in behind its outer walls, so inside it: 3 + 5 + 2)
        assertEquals(7 * 5 - 3 - 10, TowerBuildings.deckColumns(tower, 1).size(), "35 less the core's 3 and the wing's 10");
        assertEquals(1, tower.structure().cores().size(), "a core in its middle");
        // as its plan shows them, those walled into the wing too: every one but the core's, each as big as the open ones
        final List<TowerBuildings.Standing> all = TowerBuildings.standing(tower, 1);
        assertEquals(7 * 5 - 3, all.size(), "35 less the core's 3, the wing's standing all the same");
        assertTrue(all.containsAll(TowerBuildings.deckColumns(tower, 1)), "the open ones among them");
        // walked east, toward its open edge: stopped by the railing, still on the deck
        Body body = Body.standing(new Vec3(1800, -floor, -400), 90);
        for (int i = 0; i < 300; i++) {
            body = body.step(new Controls(1, 0, 0, 0, false), 1.0 / 60, world);
        }
        assertEquals(floor, body.height(), 1e-6, "still on the deck");
        assertTrue(body.position().x() < 2400 - TowerBuildings.RAILING_IN, "behind the railing: " + body.position());
        // walked west, toward the wing: stopped at its wall
        Body west = Body.standing(new Vec3(400, -floor, -1200), 270);
        for (int i = 0; i < 300; i++) {
            west = west.step(new Controls(1, 0, 0, 0, false), 1.0 / 60, world);
        }
        assertTrue(west.position().x() > -2400 + 2 * Towers.BAY - 1, "stopped by the wing: " + west.position());
        // open all through: no wing, every column
        final Tower open = Towers.voidDeck(15, 760, 0);
        assertEquals(35 - 3, TowerBuildings.deckColumns(open, 1).size(), "every column, but the core's");
        assertEquals(4, TowerBuildings.openEdges(open.sections().get(1)).size(), "a railing along all four of its edges");
        assertEquals(3, TowerBuildings.openEdges(deck).size(), "but along the wing's end wall");
    }

    @Test
    void itsGardenStandsClearOfTheColumnsAndEachOtherNoTallerThanTheDeck() {
        for (final Tower tower : List.of(Towers.voidDeck(), Towers.voidDeck(12, 400, 0), Towers.voidDeck(18, 1100, 3))) {
            final DeckGarden garden = DeckGarden.of(tower);
            final Tower.Section deck = tower.sections().get(1);
            final List<TowerBuildings.Standing> columns = TowerBuildings.deckColumns(tower, 1);
            final RoofGarden.Pool pool = garden.pool();
            for (final List<Plan.Pt> core : tower.structure().cores()) {
                assertTrue(core.stream().allMatch(p -> pool.gap(p.x(), p.y()) > RoofGarden.RIM), "no core in the pool");
            }
            for (final TowerBuildings.Standing c : columns) {
                assertTrue(pool.gap(c.at().x(), c.at().y()) > RoofGarden.RIM + c.size() / 2, "no column in the pool");
                for (final RoofGarden.Plant plant : garden.plants()) {
                    assertTrue(Math.hypot(plant.x() - c.at().x(), plant.z() - c.at().y()) > plant.size() * 0.8,
                            "no plant on a column");
                }
            }
            // every plant's crown under the deck's soffit
            for (final RoofGarden.Plant plant : garden.plants()) {
                final double tall = plant.kind() == RoofGarden.Kind.TREE ? 2.9 : plant.kind() == RoofGarden.Kind.PINE ? 4 : 1.1;
                assertTrue(plant.size() * tall + 50 < deck.tall(), tower.toString() + ": " + plant);
            }
            // in the way: on the pool's floor, sunk into the section under the deck
            final List<Volume> shape = garden.on(TowerBuildings.shape(tower));
            final Obstacles world = world(shape);
            assertEquals(garden.poolFloor(), world.floor(pool.x(), pool.z(), 16, garden.floor() + 20), 1e-6);
            final Building dressed = garden.on(TowerBuildings.of(tower));
            final List<Solid> solids = dressed.dressed(dressed.own(), Palette.CONCRETE);
            assertFalse(solids.stream().anyMatch(s -> s.name().startsWith("bar, ") || s.name().startsWith("stall, ")),
                    "no bar or stall built into it: those are the game's");
            assertTrue(solids.stream().anyMatch(s -> s.material() == Material.WATER), "its pool's water");
            assertFalse(dressed.plates().stream().anyMatch(p -> p.name().equals("lower, roof")),
                    "the deck's floor paved round the pool, not drawn whole over it");
        }
    }
}
