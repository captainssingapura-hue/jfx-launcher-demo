package com.example.fxsuite.avatar.build;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The wall tower: its section, its west leg all but straight, its east leg splayed at its foot and curving in to meet
 * it over the atrium, one shaft above; its walls, all the same shape, along it; a storey of it, each leg its walls,
 * floors and glass; the storey where its legs meet shut; made only to a size it can stand at.
 */
class WallTowerTest {

    private static final WallTower TOWER = WallTower.standard();
    private static final WallTower.Size SIZE = TOWER.size();

    @Test
    void itsWestLegAllButStraightItsEastLegCurvingInToMeetItOneShaftAbove() {
        // the west face, upright at its foot, curving out as it rises, more toward the top: by its curve at the top
        assertEquals(-SIZE.shaft() / 2, TOWER.westOuter(0), 1e-9);
        assertEquals(-SIZE.shaft() / 2 - SIZE.curve() / 4, TOWER.westOuter(SIZE.high() / 2), 1e-9);
        assertEquals(-SIZE.shaft() / 2 - SIZE.curve(), TOWER.westOuter(SIZE.high()), 1e-9);
        // the east face, splayed out at its foot, the shaft's where the legs meet, and above
        assertEquals(SIZE.shaft() / 2 + SIZE.splay(), TOWER.eastOuter(0), 1e-9);
        assertEquals(SIZE.shaft() / 2, TOWER.eastOuter(SIZE.meet()), 1e-9);
        assertEquals(SIZE.shaft() / 2, TOWER.eastOuter(SIZE.high()), 1e-9);
        // curving in all the way up, ever less steeply out: standing straight where they meet
        for (double up = 100; up < SIZE.meet(); up += 100) {
            assertTrue(TOWER.eastOuter(up) < TOWER.eastOuter(up - 100), "in, " + up);
        }
        // apart at the foot, with the atrium between; one shaft from where they meet
        assertTrue(TOWER.apart(0));
        assertTrue(TOWER.eastInner(0) - TOWER.westInner(0) > 2_000, "an atrium over 20 m across at its foot");
        assertFalse(TOWER.apart(SIZE.meet() + 1));
        assertTrue(TOWER.atriumTop() > SIZE.meet() / 2 && TOWER.atriumTop() <= SIZE.meet(), "the atrium closing under where they meet");
        assertEquals(2, TOWER.legs(0, SIZE.storey()).size());
        assertEquals(1, TOWER.legs(SIZE.high() - SIZE.storey(), SIZE.high()).size());
    }

    @Test
    void itsWallsAllTheSameShapeAlongItItsEndsThicker() {
        assertEquals(-SIZE.length() / 2 + SIZE.end() / 2, TOWER.wallAt(0), 1e-9);
        assertEquals(SIZE.length() / 2 - SIZE.end() / 2, TOWER.wallAt(SIZE.walls() - 1), 1e-9);
        assertEquals(SIZE.end(), TOWER.thick(0));
        assertEquals(SIZE.wall(), TOWER.thick(1));
        // so many solids: each leg of each storey, its walls; and between each two, its floor and on each of its two
        // faces its rooms' glass and its balcony's rail; or, where it is shut, a wall
        int expected = 1;
        for (int k = 0; k < SIZE.storeys(); k++) {
            final int legs = TOWER.legs(k * SIZE.storey(), (k + 1) * SIZE.storey()).size();
            final boolean shut = k == SIZE.merge() - 1 || k == SIZE.merge();
            expected += legs * (SIZE.walls() + (SIZE.walls() - 1) * (1 + 2 * (shut ? 1 : 2)));
        }
        assertEquals(expected, TOWER.solids(WallTower.palette()).size(), "and its roof");
    }

    @Test
    void whereItsLegsMeetShutNoGlass() {
        final long glassAtTheMeeting = TOWER.solids(WallTower.palette()).stream()
                .filter(s -> s.role() == Role.GLAZING && s.name().endsWith("storey " + SIZE.merge()))
                .count();
        assertEquals(0, glassAtTheMeeting);
        final long glassAbove = TOWER.solids(WallTower.palette()).stream()
                .filter(s -> s.role() == Role.GLAZING && s.name().endsWith("storey " + (SIZE.merge() + 2)))
                .count();
        assertEquals(4L * (SIZE.walls() - 1), glassAbove, "glass and a rail on both its faces above");
        assertTrue(TOWER.skeleton().problems().isEmpty(), "its skeleton: " + TOWER.skeleton().problems());
    }

    @Test
    void itsFloorsRunOutAsBalconiesTheRoomsGlassSetBackARailAtTheEdge() {
        final int k = SIZE.storeys() - 3;
        final double h0 = k * SIZE.storey();
        final double west = TOWER.westOuter(h0 + WallTower.FLOOR);
        final Solid pane = named("west pane 3, storey " + k);
        final Solid rail = named("west rail 3, storey " + k);
        final Solid floor = named("floor 3, storey " + k);
        // the floor out to the face; the rail at it; the rooms' glass set back as deep as a balcony
        assertEquals(TOWER.westOuter(h0), floor.corners().get(0).across(), 1e-6);
        assertEquals(west, rail.corners().get(0).across(), 0.1, "each storey's faces straight between its floors");
        assertEquals(west + WallTower.BALCONY, pane.corners().get(0).across(), 0.1);
        assertEquals(WallTower.RAIL, rail.corners().get(4).up() - rail.corners().get(0).up(), 1e-9, "a rail waist high");
    }

    private static Solid named(final String name) {
        return TOWER.solids(WallTower.palette()).stream().filter(s -> s.name().equals(name)).findFirst().orElseThrow();
    }

    @Test
    void madeOnlyToASizeItCanStandAt() {
        assertThrows(IllegalArgumentException.class, () -> new WallTower.Size(55, 360, 0, 6_000, 9, 60, 80, 1_100, 100,
                2_400, 1_200, 3_400), "its legs meeting above the ground");
        assertThrows(IllegalArgumentException.class, () -> new WallTower.Size(55, 360, 23, 6_000, 9, 60, 80, 2_600, 100,
                2_400, 1_200, 3_400), "its west leg narrower than its shaft");
        assertThrows(IllegalArgumentException.class, () -> new WallTower.Size(55, 360, 23, 1_000, 9, 60, 80, 1_100, 100,
                2_400, 1_200, 3_400), "room along it for its walls");
    }
}
