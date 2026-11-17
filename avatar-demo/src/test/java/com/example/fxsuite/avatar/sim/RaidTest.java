package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A raid's way: up to its cruise, straight on to its target's circle, slowing to a crawl, then round
 * it for good, sinking to its hover; never jumping, and turning only as it goes.
 */
class RaidTest {

    /** From 400 m out to the west, 20 m up, to circle 10 m round the origin at 60 m, after 200 m up, at 10 m/s, from 5 s on. */
    private static final Raid RAID = new Raid(5, new Place(-40_000, 3_000, 2_000, 90), 20_000, 0, 0, 6_000, 1_000, 1_000);

    @Test
    void itGoesOnSmoothlyFromWhereItStartsRoundItsTargetForGood() {
        Place was = RAID.at(0);
        assertEquals(-40_000, was.x(), 1e-9);
        assertEquals(2_000, was.height(), 1e-9, "where it starts, until it is time");
        double highest = 0;
        for (double t = 0; t < 400; t += 0.05) {
            final Place at = RAID.at(t);
            // no faster than it goes, or climbs: no jump
            final double moved = Math.hypot(at.x() - was.x(), at.z() - was.z());
            assertTrue(moved <= RAID.speed() * 0.05 + 1e-6, "at " + t + " moved " + moved);
            assertTrue(Math.abs(at.height() - was.height()) <= Raid.CLIMB * 0.05 + 1e-6, "at " + t);
            // turning only a little at a time, once it is under way
            if (t > 5 + 18_000 / Raid.CLIMB + 0.1) {
                assertTrue(Math.abs(Num.wrap(at.yaw() - was.yaw())) < 2, "at " + t + " turned from " + was.yaw() + " to "
                        + at.yaw());
            }
            highest = Math.max(highest, at.height());
            was = at;
        }
        assertEquals(20_000, highest, 1e-6, "up to its cruise");
        // long after: round its target, at its hover, at a crawl
        final Place late = RAID.at(400);
        assertEquals(1_000, Math.hypot(late.x(), late.z()), 1e-6, "round its circle");
        assertEquals(6_000, late.height(), 1e-6, "sunk to its hover");
        final Place later = RAID.at(401);
        assertEquals(Raid.CRAWL, Math.hypot(later.x() - late.x(), later.z() - late.z()), 1, "at a crawl");
    }

    @Test
    void itCirclesSomething() {
        assertThrows(IllegalArgumentException.class, () -> new Raid(0, new Place(0, 0, 0, 0), 0, 0, 0, 0, 0, 1_000));
        assertThrows(IllegalArgumentException.class, () -> new Raid(0, new Place(0, 0, 0, 0), 0, 0, 0, 0, 100, 10));
    }
}
