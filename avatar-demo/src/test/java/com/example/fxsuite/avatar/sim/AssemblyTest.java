package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A building as one thing of many parts: answering as its parts do where they are, nothing away from them; a pane of
 * it worn down and, worn through, gone, the rest standing; put where its building stands.
 */
class AssemblyTest {

    /** A floor 20 m square, 30 cm thick, 4 m up; a pane 10 cm thick, 6 m across, standing on the ground under it. */
    private static Assembly storey() {
        final Prism floor = new Prism(List.of(new Vec2(0, 0), new Vec2(2000, 0), new Vec2(2000, 2000),
                new Vec2(0, 2000)), 370, 400);
        final Prism pane = new Prism(List.of(new Vec2(700, 0), new Vec2(1300, 0), new Vec2(1300, 10),
                new Vec2(700, 10)), 0, 370);
        return new Assembly(List.of(floor), List.of(pane), 60, 5_000);
    }

    @Test
    void itAnswersAsItsPartsDoAndNothingAwayFromThem() {
        final Assembly a = storey();
        assertEquals(400, a.topAt(1000, 1000, 30, 1000), 1e-9, "its floor stood on");
        assertEquals(370, a.undersideAt(1000, 1000, 30, 0), 1e-9, "its underside over whoever is under it");
        assertNotNull(a.push(1000, -20, 30, 0, 180), "its pane in the way");
        assertNull(a.push(1000, 1000, 30, 0, 180), "nothing in the way under its floor, away from the pane");
        assertFalse(a.covers(5_000, 5_000, 30));
        assertEquals(Double.NEGATIVE_INFINITY, a.topAt(5_000, 5_000, 30, 1000), "nothing away from it");
        final Hit hit = a.ray(new Vec3(1000, -150, -500), new Vec3(0, 0, 1), 2_000);
        assertEquals(500, hit.distance(), 1e-6, "a round meets the pane");
    }

    @Test
    void aPaneWornThroughIsGoneTheRestStanding() {
        Assembly a = storey();
        final Hit hit = a.ray(new Vec3(1000, -150, -500), new Vec3(0, 0, 1), 2_000);
        final int pane = a.breakableAt(hit.at());
        assertEquals(0, pane);
        assertEquals(-1, a.breakableAt(new Vec3(1000, -400, 1000)), "its floor does not break so");
        a = a.struck(pane, 0.5);
        assertFalse(a.broken(pane));
        assertEquals(0.5, a.whole(pane), 1e-12);
        a = a.struck(pane, 0.5);
        assertTrue(a.broken(pane));
        assertNull(a.push(1000, -20, 30, 0, 180), "gone: in no one's way");
        final Hit through = a.ray(new Vec3(1000, -150, -500), new Vec3(0, 0, 1), 2_000);
        assertNull(through, "a round goes on in");
        assertEquals(400, a.topAt(1000, 1000, 30, 1000), 1e-9, "its floor standing");
        assertEquals(-1, a.breakableAt(hit.at()), "nothing there to break");
    }

    @Test
    void putWhereItsBuildingStandsAsBrokenAsItIs() {
        final Assembly a = storey().struck(0, 1);
        final Volume placed = Volume.placed(a, new Place(10_000, 0, 50, 90), 1);
        assertTrue(placed instanceof Assembly moved && moved.broken(0), "broken where it stands too");
        // turned a quarter, its floor's middle ( 1000, 1000 ) now east of where it stands, raised as it is
        assertEquals(450, placed.topAt(11_000, -1_000, 30, 2000), 1e-9);
        assertEquals(Double.NEGATIVE_INFINITY, placed.topAt(1000, 1000, 30, 2000), "nothing where it was");
    }
}
