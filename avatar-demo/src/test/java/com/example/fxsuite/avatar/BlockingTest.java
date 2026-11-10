package com.example.fxsuite.avatar;

import javafx.geometry.Point3D;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** How the body moves among obstacles: blocked, sliding, standing on top, falling off, bumping its head. */
class BlockingTest {

    private static final double FRAME = 1.0 / 60;
    private static final Controls AHEAD = new Controls(1, 0, 0, 0, false);

    /** A wall across the +x direction, its face at x = 250. */
    private static final Obstacles WALL = world(Block.standing(300, 0, 100, 4000, 1000, 0));

    private static Obstacles world(final Block... blocks) {
        return new Obstacles(List.of(blocks));
    }

    private static Body run(Body body, final Controls controls, final double seconds, final Obstacles world) {
        for (double t = 0; t < seconds - 1e-9; t += FRAME) {
            body = body.step(controls, FRAME, world);
        }
        return body;
    }

    @Test
    void aWallStopsItAtItsSurface() {
        final Body after = run(Body.standing(Point3D.ZERO, 90), AHEAD, 3, WALL);
        assertEquals(250 - Body.RADIUS, after.position().getX(), 1e-6);
        assertEquals(0, after.speed(), 1, "the speed into the wall is gone, so the legs stop too");
        assertTrue(after.grounded());
    }

    @Test
    void runningIntoAWallAtASlantSlidesAlongIt() {
        final Body after = run(Body.standing(Point3D.ZERO, 60), AHEAD, 3, WALL);
        assertTrue(after.position().getX() <= 250 - Body.RADIUS + 1e-6);
        assertTrue(after.position().getZ() > 400, "slid along it: " + after.position());
    }

    @Test
    void jumpsOntoACrateItCanReach() {
        final Obstacles crate = world(Block.standing(150, 0, 100, 100, 100, 0));
        final Body pressed = run(Body.standing(Point3D.ZERO, 90), AHEAD, 1, crate);
        assertEquals(100 - Body.RADIUS, pressed.position().getX(), 1e-6, "against its side first");
        final Body on = run(pressed.jumped(), AHEAD, 0.5, crate);
        final Body stood = run(on, Controls.NONE, 1, crate);
        assertEquals(100, stood.height(), 1e-9);
        assertTrue(stood.grounded());
        assertTrue(stood.position().getX() > 100, "over the top: " + stood.position());
    }

    @Test
    void cannotJumpOntoACrateOutOfReach() {
        final double reach = Body.JUMP * Body.JUMP / (2 * Body.GRAVITY) + Body.STEP;
        final Obstacles tall = world(Block.standing(150, 0, 100, 100, reach + 10, 0));
        final Body pressed = run(Body.standing(Point3D.ZERO, 90), AHEAD, 1, tall);
        final Body after = run(run(pressed.jumped(), AHEAD, 1.5, tall), Controls.NONE, 0.5, tall);
        assertEquals(0, after.height(), 1e-9);
        assertEquals(100 - Body.RADIUS, after.position().getX(), 1e-6);
    }

    @Test
    void standsStillOnTopWithoutSinking() {
        final Obstacles crate = world(Block.standing(0, 0, 200, 200, 100, 0));
        final Body after = run(Body.standing(new Point3D(0, -100, 0), 0), Controls.NONE, 5, crate);
        assertEquals(100, after.height(), 1e-9);
        assertTrue(after.grounded());
    }

    @Test
    void walksOffAnEdgeAndFalls() {
        final Obstacles crate = world(Block.standing(0, 0, 200, 200, 100, 0));
        final Body after = run(Body.standing(new Point3D(0, -100, 0), 90), AHEAD, 1.5, crate);
        assertEquals(0, after.height(), 1e-9);
        assertTrue(after.grounded());
        assertTrue(after.position().getX() >= 100 + Body.RADIUS - 1e-6, "clear of its side: " + after.position());
    }

    @Test
    void flyingDownOntoABlockLandsOnIt() {
        final Obstacles pillar = world(Block.standing(0, 0, 300, 300, 300, 0));
        final Body flying = run(Body.standing(new Point3D(0, -600, 0), 0).toggledFlight(), Controls.NONE, 1, pillar);
        final Body down = run(flying, new Controls(0, 0, 0, -1, false), 3, pillar);
        assertFalse(down.flying());
        assertTrue(down.grounded());
        assertEquals(300, down.height(), 1e-9);
    }

    @Test
    void flyingLevelOverALowLedgeLiftsOverItWithoutLanding() {
        // it hovers at about 140, so a 150 ledge is within a step of its feet
        final Obstacles ledge = world(Block.standing(300, 0, 200, 200, 150, 0));
        final Body hovering = run(Body.standing(Point3D.ZERO, 90).toggledFlight(), Controls.NONE, 2, ledge);
        final Body over = run(hovering, AHEAD, 1, ledge);
        assertTrue(over.flying(), "still flying");
        assertTrue(over.height() >= 150, "lifted over it: " + over.height());
        assertTrue(over.position().getX() > 200);
    }

    @Test
    void theHeadStopsAtAnUnderside() {
        final Obstacles slab = world(new Block(0, 0, 300, 300, 0, 400, 450));
        final Body up = run(Body.standing(Point3D.ZERO, 0).toggledFlight(), new Controls(0, 0, 0, 1, false), 2, slab);
        assertEquals(400 - Body.TALL, up.height(), 1e-9);
        assertTrue(up.climb() <= 0);
    }

    @Test
    void aBoostAtASlowFrameRateStillCannotPassAThinBar() {
        final Obstacles bar = world(Block.standing(300, 0, 10, 4000, 1000, 0));
        Body body = new Body(new Point3D(0, -100, 0), 90, Body.BOOST, 0, 0, 0, true, false, 0, 0);
        for (int i = 0; i < 10; i++) {
            body = body.step(new Controls(1, 0, 0, 0, true), 0.1, bar);
        }
        assertEquals(295 - Body.RADIUS, body.position().getX(), 1e-6);
    }
}
