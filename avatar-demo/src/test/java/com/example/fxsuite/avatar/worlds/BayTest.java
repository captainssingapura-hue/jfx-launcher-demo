package com.example.fxsuite.avatar.worlds;

import com.example.fxsuite.avatar.build.MarinaBay;
import com.example.fxsuite.avatar.build.TowerMap;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Demos;
import com.example.fxsuite.avatar.sim.Play;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Marina Bay as a demo: its land as it lies, the towers, their hull and its park standing on it, the motorcycles
 * parked, its bridges built; its pavements stood on; each motorcycle to ride.
 */
class BayTest {

    @Test
    void aDemoTheTowersTheMotorcyclesAndItsBridgesStandingOnItItsPavementsStoodOn() {
        Demos.register(Bay.DEMO);
        final Course course = new Course(Bay.DEMO);
        assertFalse(course.land().level(), "its land as it lies");
        assertFalse(course.grid());
        assertTrue(course.pieces().size() > TowerMap.buildings().size() + Course.BIKES.size(),
                "the towers and what is on them, the motorcycles, and its bridges: " + course.pieces().size());
        assertEquals(MarinaBay.START, course.start());
        assertEquals(TowerMap.people().size(), course.ground().size());
        assertTrue(course.ponds().size() > 5);
    }

    @Test
    void eachMotorcycleToRide() {
        Demos.register(Bay.DEMO);
        final Play play = Play.start(Bay.DEMO, true);
        assertEquals(Course.BIKES.size(), play.lifts().stream().filter(l -> l.craft() instanceof Course.Chassis c
                && c.twoWheels()).count(), "each one to ride");
    }
}
