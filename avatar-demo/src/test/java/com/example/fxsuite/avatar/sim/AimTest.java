package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Rays into the world, and what the weapons strike with them. */
class AimTest {

    private static final double FRAME = 1.0 / 60;
    private static final Vec3 AHEAD = new Vec3(0, 0, 1);

    /** A wall 1 m thick across the way ahead, its near face {@code at} ahead, 4 m tall. */
    private static Obstacles wall(final double at) {
        return new Obstacles(List.of(Block.standing(0, at + 50, 4000, 100, 400, 0)));
    }

    @Test
    void aRayMeetsTheNearFaceOfABlockFacingBack() {
        final Hit hit = wall(500).ray(new Vec3(0, -100, 0), AHEAD, 10_000);
        assertEquals(Surface.BLOCK, hit.on());
        assertEquals(500, hit.distance(), 1e-9);
        assertEquals(new Vec3(0, -100, 500), hit.at());
        assertEquals(new Vec3(0, 0, -1), hit.normal(), "facing back toward the shooter");
    }

    @Test
    void aTurnedBlockIsMetOnItsTurnedFace() {
        // a block turned 45°, seen square on to one of its faces
        final Block turned = new Block(0, 1000, 100, 100, 45, 0, 400);
        final Vec3 along = Num.heading(45 + 180);
        final Hit hit = turned.ray(new Vec3(0, -100, 1000).subtract(along.multiply(1000)), along, 10_000);
        assertEquals(900, hit.distance(), 1e-6);
        assertEquals(1, -hit.normal().dotProduct(along), 1e-9, "met square on");
    }

    @Test
    void theGroundIsMetFromAboveAndTheSkyIsNot() {
        final Vec3 down = new Vec3(0, 1, 1).normalize();
        final Hit ground = Obstacles.NONE.ray(new Vec3(0, -100, 0), down, 10_000);
        assertEquals(Surface.GROUND, ground.on());
        assertEquals(new Vec3(0, 0, 100), ground.at());
        assertEquals(new Vec3(0, -1, 0), ground.normal(), "facing up");
        assertNull(Obstacles.NONE.ray(new Vec3(0, -100, 0), new Vec3(0, -1, 1).normalize(), 10_000), "open sky");
        assertNull(wall(500).ray(new Vec3(0, -100, 0), AHEAD, 400), "out of reach");
        assertNull(wall(500).ray(new Vec3(0, -100, 0), new Vec3(0, 0, -1), 10_000), "behind");
    }

    @Test
    void theNearestThingIsMetFirst() {
        final Obstacles two = new Obstacles(List.of(Block.standing(0, 2050, 4000, 100, 400, 0),
                Block.standing(0, 850, 4000, 100, 400, 0)));
        assertEquals(800, two.ray(new Vec3(0, -100, 0), AHEAD, 10_000).distance(), 1e-9);
        // a wall above the ray's height is passed under, to the ground beyond
        final Obstacles high = new Obstacles(List.of(new Block(0, 300, 4000, 100, 0, 500, 900)));
        assertEquals(Surface.GROUND, high.ray(new Vec3(0, -100, 0), new Vec3(0, 1, 5).normalize(), 10_000).on());
    }

    /** Fires (or swings) for {@code seconds}, standing at the origin facing {@code yaw}, gathering what happened. */
    private static List<Event> use(final Weapon weapon, final Obstacles world, final double yaw, final double seconds) {
        Arms arms = Arms.holding(weapon);
        final Body body = Body.standing(Vec3.ZERO, yaw);
        final List<Event> all = new ArrayList<>();
        for (double t = 0; t < seconds - 1e-9; t += FRAME) {
            final Arms.Stepped stepped = arms.step(true, FRAME, body, world, 0);
            all.addAll(stepped.events());
            arms = stepped.arms();
        }
        return all;
    }

    @Test
    void roundsFlyStraightAheadOfTheBodyAndStrikeTheWall() {
        final List<Event> events = use(Weapon.RIFLES, wall(1000), 0, 1);
        final List<Event.Struck> struck = events.stream()
                .filter(e -> e instanceof Event.Struck).map(e -> (Event.Struck) e).toList();
        final long fired = events.stream().filter(e -> e instanceof Event.Fired).count();
        assertEquals(fired, struck.size(), "every round strikes the wall");
        for (final Event.Struck s : struck) {
            assertEquals(Weapon.RIFLES, s.by());
            assertEquals(1000, s.at().z(), 1e-6, "on its face");
            // straight ahead of one muzzle or the other, off by no more than the spread
            final double off = (1000 - Arms.MUZZLE_AHEAD) * Math.tan(Math.toRadians(Arms.SPREAD)) * 1.5;
            assertTrue(Math.abs(Math.abs(s.at().x()) - Arms.MUZZLE_OUT) < off, "ahead of a muzzle: " + s.at());
            assertEquals(Arms.MUZZLE_HEIGHT, -s.at().y(), off, "at its height");
        }
        assertTrue(struck.stream().map(Event.Struck::at).distinct().count() > struck.size() / 2,
                "spread about, not all in one hole");
        assertEquals(events, use(Weapon.RIFLES, wall(1000), 0, 1), "and the same every run");
    }

    @Test
    void roundsGoWhereTheBodyFacesWhateverElse() {
        final double cone = Math.cos(Math.toRadians(Arms.SPREAD * 1.5));
        for (final double yaw : new double[] {0, 30, -120}) {
            for (final Event e : use(Weapon.RIFLES, Obstacles.NONE, yaw, 0.5)) {
                if (e instanceof Event.Fired f) {
                    assertTrue(f.to().subtract(f.from()).normalize().dotProduct(Num.heading(yaw)) > cone,
                            "along the facing, " + yaw + "°");
                }
            }
        }
    }

    @Test
    void theAimTiltsTheRoundsUpOrDownWithinItsLimit() {
        final Body body = Body.standing(Vec3.ZERO, 30);
        for (final double aim : new double[] {20, -15, 80, -80}) {
            Arms arms = Arms.holding(Weapon.RIFLES);
            double sum = 0;
            int rounds = 0;
            for (int i = 0; i < 30; i++) {
                final Arms.Stepped stepped = arms.step(true, FRAME, body, Obstacles.NONE, aim);
                arms = stepped.arms();
                for (final Event e : stepped.events()) {
                    if (e instanceof Event.Fired f) {
                        final Vec3 way = f.to().subtract(f.from()).normalize();
                        sum += Math.toDegrees(Math.asin(-way.y()));
                        rounds++;
                        // still along the body's heading, seen from above
                        final Vec3 flat = new Vec3(way.x(), 0, way.z()).normalize();
                        assertTrue(flat.dotProduct(Num.heading(30)) > Math.cos(Math.toRadians(Arms.SPREAD * 2)));
                    }
                }
            }
            final double expected = Num.clamp(aim, -Arms.AIM_LIMIT, Arms.AIM_LIMIT);
            assertEquals(expected, arms.pitch(), 1e-9, "held at " + expected);
            assertEquals(expected, sum / rounds, Arms.SPREAD, "the rounds tilt by the aim, on average");
        }
    }

    @Test
    void theSightTiltsWithTheAim() {
        // aimed down, the line meets the ground ahead, where the rounds will
        final Vec3 down = Arms.sight(Body.standing(Vec3.ZERO, 0), Obstacles.NONE, -20);
        assertEquals(0, down.y(), 2, "on the ground, just short of it");
        assertEquals(Arms.MUZZLE_AHEAD + Arms.MUZZLE_HEIGHT / Math.tan(Math.toRadians(20)), down.z(), 3);
        final Vec3 up = Arms.sight(Body.standing(Vec3.ZERO, 0), Obstacles.NONE, 20);
        assertTrue(-up.y() > Arms.MUZZLE_HEIGHT + 900, "aimed up, it rises: " + up);
    }

    @Test
    void theSightMarksTheLineAheadWhereItMeetsTheWorld() {
        final Body facing = Body.standing(Vec3.ZERO, 0);
        final Vec3 onWall = Arms.sight(facing, wall(1000), 0);
        assertEquals(new Vec3(0, -Arms.MUZZLE_HEIGHT, 998), onWall, "just short of the wall, between the rifles");
        final Body turned = Body.standing(Vec3.ZERO, 30);
        final Vec3 open = Arms.sight(turned, Obstacles.NONE, 0);
        final Vec3 from = new Vec3(0, -Arms.MUZZLE_HEIGHT, 0).add(Num.heading(30).multiply(Arms.MUZZLE_AHEAD));
        assertEquals(Arms.SIGHT, open.distance(from), 1e-6, "nothing ahead: as far as the sight goes");
        assertEquals(1, open.subtract(from).normalize().dotProduct(Num.heading(30)), 1e-9, "where the body faces");
    }

    @Test
    void eachRoundFliesFromItsOwnMuzzle() {
        final List<Event.Fired> fired = use(Weapon.RIFLES, Obstacles.NONE, 0, 0.2).stream()
                .filter(e -> e instanceof Event.Fired).map(e -> (Event.Fired) e).toList();
        final Body body = Body.standing(Vec3.ZERO, 0);
        assertEquals(Arms.muzzle(body, true), fired.get(0).from());
        assertEquals(Arms.muzzle(body, false), fired.get(1).from());
        assertEquals(Arms.RANGE, fired.get(0).to().distance(fired.get(0).from()), 1e-6, "into the distance");
    }

    @Test
    void aCleaveComesDownOnTheGroundInFrontAndASweepFindsWhatIsBeside() {
        final List<Event.Struck> cleave = use(Weapon.GLAIVE, Obstacles.NONE, 0, Arms.STROKE * 0.9).stream()
                .filter(e -> e instanceof Event.Struck).map(e -> (Event.Struck) e).toList();
        assertEquals(1, cleave.size());
        assertEquals(Surface.GROUND, cleave.get(0).on());
        assertEquals(Weapon.GLAIVE, cleave.get(0).by());
        assertTrue(cleave.get(0).at().z() > 50 && cleave.get(0).at().z() < Arms.REACH, "in front, within reach");

        // a pillar out to the right, at shoulder height, within the sweep's reach
        final Obstacles pillar = new Obstacles(List.of(Block.standing(180, 80, 40, 40, 400, 0)));
        final List<Event.Struck> swept = use(Weapon.GLAIVE, pillar, 0, Arms.STROKE * 1.9).stream()
                .filter(e -> e instanceof Event.Struck).map(e -> (Event.Struck) e).toList();
        assertEquals(Surface.BLOCK, swept.get(swept.size() - 1).on(), "the sweep meets the pillar");

        final Obstacles far = new Obstacles(List.of(Block.standing(0, 600, 400, 40, 400, 0)));
        final List<Event> beyond = use(Weapon.GLAIVE, far, 0, Arms.STROKE * 1.9);
        assertTrue(beyond.stream().noneMatch(e -> e instanceof Event.Struck s && s.on() == Surface.BLOCK),
                "nothing out of reach is struck");
    }
}
