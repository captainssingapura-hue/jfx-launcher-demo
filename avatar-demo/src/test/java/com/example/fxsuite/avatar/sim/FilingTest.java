package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What is in an inside's cells: each thing in the cell its middle is in; filed again only crossing into another; taken
 * out leaving them all, for the world's level to take, and back in coming in; what is near found as far round as the
 * largest thing reaches. In a building set in a world: a vase thrown off a balcony leaves it for the world; the
 * jet-pack, flying in, leaves the world for it.
 */
class FilingTest {

    /** A thing with a name, so two alike are still two. */
    private record Thing(String name) {
    }

    /** Three bays of 7.5 m along, two rooms across, three storeys. */
    private static final GridInside BUILDING = new GridInside(new Box(-1_000, 1_000, 0, 2_250, 0, 1_080), 1_000,
            750, 360);

    @Test
    void eachThingInTheCellItsMiddleIsInFiledAgainOnlyCrossingIntoAnother() {
        final Filing<Thing> filing = new Filing<>(BUILDING);
        final Thing chair = new Thing("chair");
        final Thing table = new Thing("table");
        final int room = filing.file(chair, -500, 300, 40, 40);
        assertEquals(BUILDING.cell(-500, 300, 40), room);
        assertEquals(room, filing.file(table, -400, 350, 40, 90));
        assertEquals(List.of(chair, table), filing.in(room), "in the order they came in");
        // moved about the room, the same cell
        assertEquals(room, filing.move(chair, -300, 600, 40));
        assertEquals(List.of(chair, table), filing.in(room));
        // through the door into the next bay
        final int next = filing.move(chair, -300, 900, 40);
        assertEquals(BUILDING.cell(-300, 900, 40), next);
        assertEquals(List.of(table), filing.in(room));
        assertEquals(List.of(chair), filing.in(next));
        assertEquals(next, filing.cell(chair));
        assertThrows(IllegalArgumentException.class, () -> filing.file(chair, 0, 0, 0, 1), "moved, not filed again");
    }

    @Test
    void whatIsNearFoundAsFarRoundAsTheLargestThingReaches() {
        final Filing<Thing> filing = new Filing<>(BUILDING);
        final Thing cup = new Thing("cup");
        filing.file(cup, -100, 740, 100, 5);
        // a body just over the bay's wall from it: the cup's cell not reached by its box, the cup not near
        final Box body = Box.around(-100, 790, 30, 32, 180);
        assertTrue(filing.near(body).isEmpty());
        // a sofa three metres long filed somewhere: now all is looked for as far round as it reaches
        filing.file(new Thing("sofa"), 500, 2_000, 30, 150);
        assertEquals(150, filing.reach());
        assertTrue(filing.near(body).contains(cup));
        // nor is it near a body two storeys over it
        assertFalse(filing.near(Box.around(-100, 740, 900, 32, 100)).contains(cup));
    }

    @Test
    void aVaseThrownOffABalconyLeavesTheBuildingForTheWorldTheJetPackFlyingIn() {
        final Placed tower = new Placed(new Place(10_000, 4_000, 0, 30), BUILDING);
        final Places<Object> world = new Places<>();
        world.add(tower, tower.world());
        final Filing<Thing> inside = new Filing<>(BUILDING);
        // a vase on the balcony of the second storey, at the building's edge
        final Thing vase = new Thing("vase");
        double a = 980;
        double l = 1_100;
        double u = 400;
        assertTrue(inside.file(vase, a, l, u, 15) >= 0);
        // thrown out and falling: each step, still filed in the building, until it is not, and then in the world
        boolean out = false;
        for (int step = 0; step < 20 && !out; step++) {
            a += 20;
            u -= 30;
            if (inside.move(vase, a, l, u) < 0) {
                out = true;
                final double x = tower.x(a, l);
                final double z = tower.z(a, l);
                world.add(vase, Box.around(x, z, tower.height(u), 15, 30));
            }
        }
        assertTrue(out, "gone over the edge");
        assertEquals(0, inside.size());
        assertTrue(world.has(vase));
        // and the jet-pack, flying in from the world: the world says the tower is near, the tower says which cell
        final Thing pack = new Thing("jet-pack");
        final double x = tower.x(-1_300, 1_500);
        final double z = tower.z(-1_300, 1_500);
        world.add(pack, Box.around(x, z, 750, 32, 180));
        assertEquals(List.of(tower, pack), world.near(world.box(pack)), "the tower near it, and itself");
        assertEquals(-1, tower.cell(x, z, 750), "not in it yet");
        final double xIn = tower.x(-900, 1_500);
        final double zIn = tower.z(-900, 1_500);
        assertTrue(tower.cell(xIn, zIn, 750) >= 0);
        world.remove(pack);
        assertEquals(tower.cell(xIn, zIn, 750), inside.file(pack, tower.across(xIn, zIn), tower.along(xIn, zIn),
                tower.up(750), 32));
        assertFalse(world.has(pack));
    }
}
