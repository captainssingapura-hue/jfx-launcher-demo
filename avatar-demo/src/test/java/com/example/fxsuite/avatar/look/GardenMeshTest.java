package com.example.fxsuite.avatar.look;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The ground swept round a lawn: out from the lawn's middle to the square's edge, whichever way. */
class GardenMeshTest {

    @Test
    void aRayGoesIntoABoxAndOutOfItOrMissesIt() {
        // a path 6 m wide, from 49 m to 100 m south of the middle
        final com.example.fxsuite.avatar.sim.Block path = new com.example.fxsuite.avatar.sim.Block(0, -7_450, 300, 2_550, 0, 0, 1);
        final double[] south = GardenMesh.through(path, 0, -1);
        assertEquals(4_900, south[0], 1e-6);
        assertEquals(10_000, south[1], 1e-6);
        // a little off south: in at its near end, out through its side
        final double a = Math.toRadians(2);
        final double[] off = GardenMesh.through(path, Math.sin(a), -Math.cos(a));
        assertEquals(4_900 / Math.cos(a), off[0], 1e-6);
        assertEquals(300 / Math.sin(a), off[1], 1e-6);
        org.junit.jupiter.api.Assertions.assertNull(GardenMesh.through(path, 0, 1), "north, it misses it");
    }

    @Test
    void outToTheSquaresEdgeWhicheverWay() {
        // from the middle, straight out each way, and to a corner
        assertEquals(1_000, GardenMesh.edge(1_000, 0, 0, 1, 0), 1e-9);
        assertEquals(1_000, GardenMesh.edge(1_000, 0, 0, 0, -1), 1e-9);
        assertEquals(1_000 * Math.sqrt(2), GardenMesh.edge(1_000, 0, 0, Math.sqrt(0.5), Math.sqrt(0.5)), 1e-6);
        // from off the middle, to the nearer edge that way
        assertEquals(400, GardenMesh.edge(1_000, 600, 0, 1, 0), 1e-9);
        assertEquals(1_600, GardenMesh.edge(1_000, 600, 0, -1, 0), 1e-9);
        assertEquals(400 * Math.sqrt(2), GardenMesh.edge(1_000, 600, 0, Math.sqrt(0.5), Math.sqrt(0.5)), 1e-6,
                "the side it reaches first");
    }
}
