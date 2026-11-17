package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Body;
import com.example.fxsuite.avatar.sim.Box;
import com.example.fxsuite.avatar.sim.Clock;
import com.example.fxsuite.avatar.sim.Controls;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Demo;
import com.example.fxsuite.avatar.sim.Interior;
import com.example.fxsuite.avatar.sim.Obstacles;
import com.example.fxsuite.avatar.sim.Place;
import com.example.fxsuite.avatar.sim.Placed;
import com.example.fxsuite.avatar.sim.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The park on the sky hull's deck: a football pitch in its middle, a real one's proportions as wide as the deck
 * allows; a pool to its stern along its port side, a deck to lie by it; a basketball court to its bow; none over
 * another; all of it on the deck, clear of its edge; garden in what is left, a rail round all of it; and on the
 * towers, stood on, its water gone into.
 */
class SkyParkTest {

    private static final SkyHull HULL = SkyHull.standard();
    private static final SkyPark PARK = new SkyPark(HULL);
    private static final List<Solid> SOLIDS = PARK.solids();

    @Test
    void aPitchInTheMiddleAsARealOnesProportionsAsWideAsTheDeckAllows() {
        final Box pitch = PARK.pitch();
        final double wide = pitch.maxX() - pitch.minX();
        final double long_ = pitch.maxZ() - pitch.minZ();
        assertEquals(SkyPark.PITCH_LONG / SkyPark.PITCH_WIDE, long_ / wide, 1e-9, "a real pitch's proportions");
        assertEquals(0, (pitch.minZ() + pitch.maxZ()) / 2, 1e-9, "in the middle");
        assertEquals(HULL.size().beam(), wide + 2 * SkyPark.CLEAR + 2 * SkyPark.RUN_OFF, 1e-9, "as wide as it can be");
        assertTrue(wide > 3_000 && long_ > 4_600, "about 48 m by 31: " + long_ + " by " + wide);
        // its lines, as a real one's: two goals, two penalty areas, a centre circle
        assertEquals(2, SOLIDS.stream().filter(s -> s.name().matches("pitch \\w+ goal, crossbar")).count());
        assertTrue(SOLIDS.stream().anyMatch(s -> s.name().startsWith("pitch centre circle")));
    }

    @Test
    void aPoolToItsSternACourtToItsBowNoneOverAnother() {
        final Box pitch = PARK.grass();
        final Box pool = PARK.pool();
        final Box lounge = PARK.lounge();
        final Box court = PARK.court();
        assertTrue(pool.maxZ() < pitch.minZ(), "the pool to the stern of the pitch");
        assertTrue(court.minZ() > pitch.maxZ(), "the court to its bow");
        assertTrue(pool.minX() < 0 && pool.maxX() < lounge.minX(), "the pool on its port side, the lounge beside it");
        assertEquals(SkyPark.COURT_LONG, court.maxZ() - court.minZ(), 1e-9, "a real court");
        assertEquals(SkyPark.COURT_WIDE, court.maxX() - court.minX(), 1e-9);
        final List<Box> laid = List.of(pitch, pool, lounge, court);
        for (int a = 0; a < laid.size(); a++) {
            for (int b = a + 1; b < laid.size(); b++) {
                assertFalse(laid.get(a).meets(laid.get(b)), a + " and " + b + " apart");
            }
        }
        // each wholly on the deck where it is full width, clear of its edge
        for (final Box b : laid) {
            for (final double y : new double[] {b.minZ(), b.maxZ()}) {
                assertTrue(PARK.onDeck(b.minX(), y, SkyPark.CLEAR - 1) && PARK.onDeck(b.maxX(), y, SkyPark.CLEAR - 1),
                        "on the deck: " + b);
            }
        }
    }

    @Test
    void allOfItOnTheDeckGardenInWhatIsLeftARailRoundIt() {
        for (final Solid s : SOLIDS) {
            for (final Point p : s.corners()) {
                assertTrue(Math.abs(p.across()) <= HULL.half(p.along()) + 1e-6, "on the deck: " + s.name());
                final double lowest = s.name().startsWith("pool") ? PARK.pool().low() - SkyPark.FLOOR : PARK.deck();
                assertTrue(p.up() >= lowest - 1e-9, "on it, not in it, but the pool sunk in it: " + s.name());
            }
        }
        // trees and hedges only in the garden
        for (final Solid s : SOLIDS) {
            if (s.name().matches("(tree \\d+, trunk|hedge \\d+)")) {
                final Box b = Built.box(s);
                assertTrue(PARK.garden((b.minX() + b.maxX()) / 2, (b.minZ() + b.maxZ()) / 2, 0), s.name());
            }
        }
        final long trees = SOLIDS.stream().filter(s -> s.name().matches("tree \\d+, trunk")).count();
        assertTrue(trees > 20, "trees: " + trees);
        assertTrue(SOLIDS.stream().anyMatch(s -> s.name().matches("lawn .*")), "lawn");
        assertTrue(SOLIDS.stream().anyMatch(s -> s.name().matches("lawn \\d+, starboard")), "lawn beside the court");
        // the rail, at its edge each side, all the way to its bow's tip
        assertEquals(2L * HULL.size().spaces(), SOLIDS.stream().filter(s -> s.name().startsWith("rail ")).count());
        assertTrue(SOLIDS.size() < 4_000, "a park, not a city: " + SOLIDS.size());
    }

    @Test
    void onTheTowersStoodOnItsWaterGoneInto() {
        TowerMap.tell();
        final Obstacles world = new Course(Demo.WALL_TOWER).obstacles();
        final Interior hull = (Interior) world.volumes().get(TowerMap.TOWERS);
        final Placed placed = hull.placed();
        final Place at = TowerMap.hullAt();
        final double deck = at.height() + PARK.deck();
        // the pitch's middle, its grass; the pool's, its floor, its water no footing
        final Box pitch = PARK.pitch();
        final Point middle = HULL.bent(new Point(700, 1_200, 0));  // clear of every line
        assertEquals(deck + 3, world.footing(placed.x(middle.across(), middle.along()),
                placed.z(middle.across(), middle.along()), 32, deck + 20).height(), 1e-6, "on the pitch");
        final Box pool = PARK.pool();
        final Point swim = HULL.bent(new Point((pool.minX() + pool.maxX()) / 2, (pool.minZ() + pool.maxZ()) / 2, 0));
        assertEquals(deck - SkyPark.POOL_DEEP, world.footing(placed.x(swim.across(), swim.along()),
                placed.z(swim.across(), swim.along()), 32, deck + 20).height(), 1e-6, "into the pool, to its floor");
        // and kept from the edge by the rail
        final Point edge = HULL.bent(new Point(HULL.size().beam() / 2 - 20, -6_000, 0));  // against its rail
        final Obstacles.Contact c = world.slide(placed.x(edge.across(), edge.along()),
                placed.z(edge.across(), edge.along()), 0, 0, 32, deck + 20, deck + 200);
        assertTrue(c.blocked(), "the rail in the way");
    }

    @Test
    void jumpedIntoFromTheDeckWalkedOutUpItsSteps() {
        TowerMap.tell();
        final Obstacles world = new Course(Demo.WALL_TOWER).obstacles();
        final Placed placed = ((Interior) world.volumes().get(TowerMap.TOWERS)).placed();
        final double deck = TowerMap.hullAt().height() + PARK.deck();
        final Box pool = PARK.pool();
        // which way is ahead, toward its bow, on the deck where the pool is
        final double along = (pool.minZ() + pool.maxZ()) / 2;
        final Point a = HULL.bent(new Point(0, along, 0));
        final Point b = HULL.bent(new Point(0, along + 100, 0));
        final double bow = Math.toDegrees(Math.atan2(placed.x(b.across(), b.along()) - placed.x(a.across(), a.along()),
                placed.z(b.across(), b.along()) - placed.z(a.across(), a.along())));
        final double west = bow - 90;
        // stood on the deck beside it, walking toward it: over its rim, and down to its floor
        final double x0 = pool.minX() + 400;
        final Point beside = HULL.bent(new Point(x0, pool.minZ() - 150, 0));
        Body body = Body.standing(new Vec3(placed.x(beside.across(), beside.along()), -deck,
                placed.z(beside.across(), beside.along())), bow);
        final Controls walk = new Controls(1, 0, 0, 0, false, false, 0);
        for (int n = 0; n < 240; n++) {
            body = body.step(walk, Clock.STEP, world);
        }
        assertEquals(deck - SkyPark.POOL_DEEP, body.height(), 1e-6, "in the pool, on its floor");
        // from its floor, at the foot of its steps, walking up them toward its bow: out, on the deck
        final double x1 = pool.maxX() - SkyPark.POOL_WALL - SkyPark.STEPS_WIDE / 2;
        final Point foot = HULL.bent(new Point(x1, pool.maxZ() - SkyPark.POOL_WALL - 400, 0));
        body = Body.standing(new Vec3(placed.x(foot.across(), foot.along()), -(deck - SkyPark.POOL_DEEP),
                placed.z(foot.across(), foot.along())), bow);
        for (int n = 0; n < 600; n++) {
            body = body.step(walk, Clock.STEP, world);
        }
        assertTrue(body.height() >= deck && body.height() <= deck + 10, "up its steps, out on the deck, or what is on it: "
                + (body.height() - deck));
        assertTrue(Double.isFinite(west));
    }

    @Test
    void itsWaterStillWaterAsAPondsIsALittleUnderItsRimWithinItsWalls() {
        TowerMap.tell();
        final List<com.example.fxsuite.avatar.sim.Pond> ponds = new Course(Demo.WALL_TOWER).ponds();
        assertEquals(1, ponds.size(), "the pool's water, and no other");
        final com.example.fxsuite.avatar.sim.Pond pond = ponds.getFirst();
        final Place at = TowerMap.hullAt();
        assertEquals(at.height() + PARK.deck() - SkyPark.SURFACE, pond.level(), 1e-9, "a little under its rim");
        assertTrue(SOLIDS.stream().noneMatch(s -> s.material() == Material.WATER), "not one of its solids");
        // each point of its outline within the pool, in under its walls: as far in from its rim as half a wall
        final Interior hull = (Interior) new Course(Demo.WALL_TOWER).obstacles().volumes().get(TowerMap.TOWERS);
        final Placed placed = hull.placed();
        final Box pool = PARK.pool();
        for (final com.example.fxsuite.avatar.sim.Vec2 p : pond.outline()) {
            // back into the hull's frame, and unbent: across out from the curve's middle, along round it
            final double a = placed.across(p.x(), p.z());
            final double l = placed.along(p.x(), p.z());
            final double r = HULL.size().curve();
            final double across = Math.hypot(a + r, l) - r;
            final double along = r * Math.atan2(l, a + r);
            assertTrue(across > pool.minX() && across < pool.maxX(), "within its walls across: " + across);
            assertTrue(along > pool.minZ() && along < pool.maxZ(), "within its walls along: " + along);
            assertTrue(across < pool.minX() + SkyPark.POOL_WALL || across > pool.maxX() - SkyPark.POOL_WALL,
                    "in under a wall: " + across);
        }
    }
}
