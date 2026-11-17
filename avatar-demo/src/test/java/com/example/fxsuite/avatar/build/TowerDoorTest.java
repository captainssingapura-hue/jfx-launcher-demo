package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Action;
import com.example.fxsuite.avatar.sim.Avatar;
import com.example.fxsuite.avatar.sim.Blow;
import com.example.fxsuite.avatar.sim.Damage;
import com.example.fxsuite.avatar.sim.Hit;
import com.example.fxsuite.avatar.sim.Weapon;
import com.example.fxsuite.avatar.sim.Body;
import com.example.fxsuite.avatar.sim.Clock;
import com.example.fxsuite.avatar.sim.Controls;
import com.example.fxsuite.avatar.sim.Demo;
import com.example.fxsuite.avatar.sim.Interior;
import com.example.fxsuite.avatar.sim.Lean;
import com.example.fxsuite.avatar.sim.Play;
import com.example.fxsuite.avatar.sim.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The wall tower's doors, opened and shut: each of its balconies' doors one, in the way as built, its sliding leaves
 * as far open as it is; T at a shut door opening it wide, so the body walks in, and again shutting it, so it is kept
 * out; T with no door at hand doing nothing; and drawn, each door's leaves as far open as it is.
 */
class TowerDoorTest {

    private static final WallTower TOWER = WallTower.standard();
    private static final WallTower.Size SIZE = TOWER.size();
    private static final Controls T = new Controls(0, 0, 0, 0, false, false, 0, List.of(Action.TAKE));
    private static final Controls WALK = new Controls(1, 0, 0, 0, false, false, 0);
    private static Play start;

    @BeforeAll
    static void told() {
        TowerMap.tell();
        start = Play.start(Demo.WALL_TOWER, true);
    }

    private static Interior tower(final Play play) {
        return (Interior) play.damage().obstacles().volumes().get(0);
    }

    @Test
    void eachBalconysDoorOneItsLeavesInTheWayAsBuilt() {
        final List<WallTower.Opening> openings = TOWER.openings();
        final Interior interior = tower(start);
        assertEquals(openings.size(), interior.doors().size());
        int expected = 0;
        for (int k = 0; k < SIZE.storeys(); k++) {
            if (k != SIZE.merge() - 1 && k != SIZE.merge()) {
                expected += TOWER.legs(k * SIZE.storey(), (k + 1) * SIZE.storey()).size() * (SIZE.walls() - 1) * 2;
            }
        }
        assertEquals(expected, openings.size());
        // its leaves, as built, as far open as each is built: where its solids are, and as they are drawn
        final List<Solid> solids = TOWER.solids(WallTower.palette());
        for (int d = 0; d < openings.size(); d += 37) {
            final WallTower.Opening o = openings.get(d);
            assertEquals(WallTower.opening(Integer.parseInt(o.name().replaceAll(".*storey ", "")),
                    Integer.parseInt(o.name().replaceAll("^\\w+ door (\\d+),.*", "$1")), o.east()), interior.open(d));
            final List<Solid> leaves = o.solids(o.open(), WallTower.palette());
            assertEquals(TowerMap.door(d, o.open()), leaves);
            for (int k = 0; k < leaves.size(); k++) {
                final Solid leaf = leaves.get(k);
                assertTrue(solids.contains(leaf), "built as " + leaf.name());
                final Lean part = interior.parts().get(interior.doors().get(d).parts()[k]);
                assertEquals(WallTower.lean(leaf).box(), part.box(), leaf.name());
            }
        }
        // the rest of it drawn in one, without them
        assertEquals(solids.size() - 14 * openings.size(), TowerMap.solids(0).size());
        assertTrue(TowerMap.solids(0).stream().noneMatch(WallTower::doorPart));
    }

    /** A body on the balcony of storey {@code k}, bay {@code j}, before its west door, facing in. */
    private static Play onBalcony(final int k, final int j) {
        final double floor = k * SIZE.storey() + WallTower.FLOOR;
        final double along = (TOWER.wallAt(j) + TOWER.wallAt(j + 1)) / 2;
        final Body body = Body.standing(new Vec3(TOWER.westOuter(floor) + WallTower.BALCONY / 2, -floor, along), 90);
        return new Play(new Avatar(body, true), start.damage(), start.sky(), start.lifts(), start.people(),
                start.props(), start.turrets());
    }

    private static Play walked(Play play, final int steps) {
        for (int n = 0; n < steps; n++) {
            play = play.step(WALK, Clock.STEP).play();
        }
        return play;
    }

    /** The first shut west door from storey 40 up: its storey and bay. */
    private static int[] shutDoor() {
        int k = 40;
        int j = 1;
        while (WallTower.opening(k, j, false) != 0) {
            j++;
            if (j == SIZE.walls() - 1) {
                j = 0;
                k++;
            }
        }
        return new int[] {k, j};
    }

    @Test
    void everyDoorUprightABalconyInFromWhereItsFaceComesFurthestIn() {
        for (final WallTower.Opening o : TOWER.openings()) {
            for (final Solid s : o.solids(o.open(), WallTower.palette())) {
                final List<Point> c = s.corners();
                assertEquals(c.get(0).across(), c.get(4).across(), 1e-9, "upright: " + s.name());
                assertEquals(c.get(1).across(), c.get(5).across(), 1e-9, "upright: " + s.name());
            }
            // its sill a balcony in from where its face comes furthest in, at the storey's floor or at its ceiling
            final List<Point> sill = o.solids(0, WallTower.palette()).get(0).corners();
            final double in = o.east() ? Math.min(o.face()[0], o.face()[1]) - WallTower.BALCONY
                    : Math.max(o.face()[0], o.face()[1]) + WallTower.BALCONY;
            assertEquals(in, o.east() ? sill.get(1).across() : sill.get(0).across(), 1e-9, o.name());
        }
        // low on the east leg, where its face leans most, the balcony deeper at its floor than at its ceiling
        final WallTower.Opening low = TOWER.openings().stream().filter(o -> o.east() && o.name().endsWith("storey 0"))
                .max(java.util.Comparator.comparingDouble(o -> o.face()[0])).orElseThrow();
        assertTrue(low.face()[0] - low.middle() > WallTower.BALCONY + 100, "deeper at its floor");
        assertEquals(WallTower.BALCONY, low.face()[1] - low.middle(), 1e-9);
    }

    @Test
    void aDoorShotThroughGoneTheTowerStanding() {
        final int[] at = shutDoor();
        final double floor = at[0] * SIZE.storey() + WallTower.FLOOR;
        final double face = TOWER.westOuter(floor);
        final double along = (TOWER.wallAt(at[1]) + TOWER.wallAt(at[1] + 1)) / 2;
        final Play play = onBalcony(at[0], at[1]);
        final int door = play.doorAtHand().orElseThrow().door();
        // a rifle round at a time, from the balcony, at its glass
        final Vec3 from = new Vec3(face + 40, -(floor + 120), along + 100);
        final Vec3 east = new Vec3(1, 0, 0);
        Damage damage = play.damage();
        int rounds = 0;
        while (!tower(damage).broken(door)) {
            final Hit hit = damage.obstacles().ray(from, east, 1_000);
            assertEquals(0, hit.piece(), "the tower's");
            assertEquals(WallTower.BALCONY - 40, hit.distance(), 1, "at its glass");
            damage = damage.took(List.of(new Blow(hit.at(), hit.normal(), hit.on(), Weapon.RIFLES, hit.piece()))).damage();
            rounds++;
            assertTrue(rounds <= 12);
        }
        assertEquals(Damage.DOOR / Weapon.RIFLES.blow(), rounds, 1e-9, "as tough as a drone");
        assertEquals(1, damage.left(0), 1e-9, "the tower none the worse");
        // gone: the round goes on into the room; T finds no door; walked through where it was
        final Hit through = damage.obstacles().ray(from, east, 1_000);
        assertTrue(through == null || through.distance() > WallTower.BALCONY + 100, "into the room");
        final Play after = new Play(play.avatar(), damage, play.sky(), play.lifts(), play.people(), play.props(),
                play.turrets());
        assertFalse(after.doorAtHand().isPresent());
        assertTrue(walked(after, 360).avatar().body().position().x() > face + WallTower.BALCONY + 300, "walked in");
        // its neighbours whole and in the way still
        assertFalse(tower(damage).broken(door + 1));
    }

    private static Interior tower(final Damage damage) {
        return (Interior) damage.obstacles().volumes().get(0);
    }

    @Test
    void tAtAShutDoorOpensItAndInAgainShutsIt() {
        final int[] at = shutDoor();
        final int k = at[0];
        final int j = at[1];
        final double face = TOWER.westOuter(k * SIZE.storey() + WallTower.FLOOR);
        // shut: kept out
        Play play = onBalcony(k, j);
        assertTrue(play.doorAtHand().isPresent() && !play.doorAtHand().get().open());
        assertTrue(walked(play, 360).avatar().body().position().x() < face + WallTower.BALCONY, "kept out");
        // T: wide open; in
        play = play.step(T, Clock.STEP).play();
        final int door = play.doorAtHand().orElseThrow().door();
        assertEquals(1, tower(play).open(door));
        assertTrue(play.doorAtHand().get().open());
        final Play in = walked(play, 360);
        assertTrue(in.avatar().body().position().x() > face + WallTower.BALCONY + 300, "walked in");
        // T again, back on the balcony: shut, kept out again
        play = play.step(T, Clock.STEP).play();
        assertEquals(0, tower(play).open(door));
        assertTrue(walked(play, 360).avatar().body().position().x() < face + WallTower.BALCONY, "kept out again");
        // and the same T, the same world: one door changed, no other
        final Interior before = tower(start);
        final Interior after = tower(onBalcony(k, j).step(T, Clock.STEP).play());
        for (int d = 0; d < before.doors().size(); d++) {
            assertEquals(d == door ? 1 : before.open(d), after.open(d), "door " + d);
        }
    }

    @Test
    void tWithNoDoorAtHandDoesNothing() {
        final Play out = new Play(new Avatar(Body.standing(TowerMap.START, 90), true), start.damage(), start.sky(),
                start.lifts(), start.people(), start.props(), start.turrets());
        assertFalse(out.doorAtHand().isPresent());
        assertSame(tower(out), tower(out.step(T, Clock.STEP).play()));
    }
}
