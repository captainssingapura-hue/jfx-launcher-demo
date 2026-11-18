package com.example.fxsuite.avatar.roads;

import com.example.fxsuite.avatar.build.MarinaBay;
import com.example.fxsuite.avatar.build.Sites;
import com.example.fxsuite.avatar.build.Solid;
import com.example.fxsuite.avatar.sim.Body;
import com.example.fxsuite.avatar.sim.Clock;
import com.example.fxsuite.avatar.sim.Controls;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Demo;
import com.example.fxsuite.avatar.sim.Land;
import com.example.fxsuite.avatar.sim.Obstacles;
import com.example.fxsuite.avatar.sim.Road;
import com.example.fxsuite.avatar.sim.Vec2;
import com.example.fxsuite.avatar.sim.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bridges built on the stretches of roads that are bridges, on Marina Bay's: one on each stretch; its deck's top the
 * road's own level, stood and ridden on over the water; its parapets in the way at its sides; its piers down to what
 * is under it, where that is low enough to need them; each one piece, drawn by a key of its own.
 */
class BridgesTest {

    private static List<Bridges.Bridge> bridges;
    private static Obstacles world;
    private static Land land;

    @BeforeAll
    static void built() {
        MarinaBay.tell();
        bridges = MarinaBay.bridges();
        world = new Course(MarinaBay.DEMO).obstacles();
        land = MarinaBay.land();
    }

    /** The road a bridge carries. */
    private static Road road(final Bridges.Bridge b) {
        return MarinaBay.roads().stream().filter(r -> r.name().equals(b.road()) && r.bridged(b.from())).findFirst()
                .orElseThrow();
    }

    @Test
    void oneOnEachStretchOfARoadThatIsABridge() {
        int stretches = 0;
        for (final Road r : MarinaBay.roads()) {
            for (int i = 0; i < r.line().size(); i++) {
                if (r.bridged(i) && (i == 0 || !r.bridged(i - 1))) {
                    stretches++;
                }
            }
        }
        assertEquals(stretches, bridges.size(), "one on each");
        assertTrue(bridges.size() >= 30, "the bay's bridges: " + bridges.size());
        for (final Bridges.Bridge b : bridges) {
            assertTrue(b.to() > b.from());
            assertFalse(b.solids().isEmpty());
            assertFalse(b.parts().isEmpty());
        }
    }

    /** A bridge over the water, its middle: the road, which point, where. */
    private static Bridges.Bridge overWater() {
        for (final Bridges.Bridge b : bridges) {
            final Road r = road(b);
            final Vec2 mid = r.line().get((b.from() + b.to()) / 2);
            if (land.height(mid.x(), mid.z()) < MarinaBay.SEA && b.to() - b.from() > 10) {
                return b;
            }
        }
        throw new AssertionError("no bridge over the water");
    }

    @Test
    void itsDecksTopTheRoadsOwnLevelStoodOnOverTheWater() {
        final Bridges.Bridge b = overWater();
        final Road r = road(b);
        final int mid = (b.from() + b.to()) / 2;
        final Vec2 at = r.line().get(mid);
        final double level = r.level(mid);
        assertEquals(level, world.footing(at.x(), at.z(), 32, level + 20).height(), 3, "on its deck, at its road's level");
        assertTrue(land.height(at.x(), at.z()) < MarinaBay.SEA, "the water under it");
        // stood on it a second: not fallen in
        Body body = Body.standing(new Vec3(at.x(), -level, at.z()), 0);
        for (int n = 0; n < 120; n++) {
            body = body.step(Controls.NONE, Clock.STEP, world);
        }
        assertEquals(level, body.height(), 3, "standing on it still");
    }

    @Test
    void itsParapetsInTheWayAtItsSides() {
        final Bridges.Bridge b = overWater();
        final Road r = road(b);
        final int mid = (b.from() + b.to()) / 2;
        final Vec2 a = r.line().get(mid);
        final Vec2 c = r.line().get(mid + 1);
        final double len = Math.hypot(c.x() - a.x(), c.z() - a.z());
        // to its side: square to the way it goes
        final double sx = (c.z() - a.z()) / len;
        final double sz = -(c.x() - a.x()) / len;
        final double edge = r.wide(mid) / 2 + Bridges.PARAPET_THICK / 2;
        final double level = r.level(mid);
        final Obstacles.Contact against = world.slide(a.x() + sx * edge, a.z() + sz * edge, 0, 0, 32, level + 20,
                level + 200);
        assertTrue(against.blocked(), "a parapet there");
    }

    @Test
    void itsPiersDownToWhatIsUnderItWhereThatIsLowEnoughToNeedThem() {
        int piers = 0;
        for (final Bridges.Bridge b : bridges) {
            for (final Solid s : b.solids()) {
                if (s.name().contains(", column ")) {
                    piers++;
                    final double foot = s.corners().stream().mapToDouble(p -> p.up()).min().orElseThrow();
                    final double x = s.corners().stream().mapToDouble(p -> p.across()).average().orElseThrow();
                    final double z = s.corners().stream().mapToDouble(p -> p.along()).average().orElseThrow();
                    assertEquals(land.height(x, z) - Bridges.FOOTING, foot, 30, "into what is under it: " + s.name());
                }
            }
        }
        assertTrue(piers > 10, "piers where there is room under: " + piers);
    }

    @Test
    void eachOnePieceDrawnByAKeyOfItsOwn() {
        final Course course = new Course(MarinaBay.DEMO);
        final List<Course.Piece> pieces = course.pieces().stream()
                .filter(p -> p.kind() == Course.Kind.BUILDING && p.tint() >= 700 && p.tint() < 700 + bridges.size())
                .toList();
        assertEquals(bridges.size(), pieces.size());
        final List<Solid> drawn = Sites.drawn(pieces.getFirst().tint());
        assertNotNull(drawn);
        assertEquals(bridges.getFirst().solids().size(), drawn.size(), "its solids");
        // about the middle of its box, up from its foot
        assertTrue(drawn.stream().flatMap(s -> s.corners().stream()).allMatch(p -> p.up() >= -1e-6));
    }
}
