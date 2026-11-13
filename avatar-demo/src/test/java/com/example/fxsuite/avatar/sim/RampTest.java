package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Ramps: blocks whose tops slope, stood on, run up and down, shot at; and the flyover built of them. */
class RampTest {

    /** Up a metre in four, from (0, 0) to (0, 1000), 4 m wide. */
    private static final Block RAMP = Block.ramp(0, 0, 0, 0, 1000, 250, 400);
    private static final Controls RUN = new Controls(1, 0, 0, 0, true, false, 0);

    @Test
    void itsTopSlopesEvenlyFromItsFootToItsHead() {
        assertTrue(RAMP.sloped());
        assertEquals(0, RAMP.yaw(), 1e-9);
        assertEquals(0, RAMP.topAt(0, 0, 0), 1e-9);
        assertEquals(125, RAMP.topAt(0, 500, 0), 1e-9);
        assertEquals(250, RAMP.topAt(0, 1000, 0), 1e-9);
        assertEquals(127.5, RAMP.topAt(0, 500, 10), 1e-9, "where a circle reaches highest on it");
        assertEquals(250, RAMP.topAt(0, 2000, 0), 1e-9, "past its head, its head");
        assertFalse(new Block(0, 0, 10, 10, 0, 0, 50).sloped());
        // turned any way, the same: up toward its head
        final Block east = Block.ramp(0, 0, 0, 1000, 0, 250, 400);
        assertEquals(125, east.topAt(500, 0, 0), 1e-9);
        assertEquals(250, east.topAt(1000, 0, 0), 1e-9);
    }

    @Test
    void whatIsShotAtItMeetsTheSlope() {
        // straight down onto the middle: the slope, half way up, its face tipped back toward the foot
        final Hit down = RAMP.ray(new Vec3(0, -1000, 500), new Vec3(0, 1, 0), 5000);
        assertNotNull(down);
        assertEquals(-125, down.at().y(), 1e-6);
        assertTrue(down.normal().y() < -0.9 && down.normal().z() < 0, "facing up, and back down the ramp");
        // level, half a metre up, toward the head: over the foot, until the slope comes up to meet it
        final Hit level = RAMP.ray(new Vec3(0, -50, -100), new Vec3(0, 0, 1), 5000);
        assertNotNull(level);
        assertEquals(200, level.at().z(), 1e-6);
        // level, well above its head: over it all
        assertNull(RAMP.ray(new Vec3(0, -300, -100), new Vec3(0, 0, 1), 5000));
    }

    @Test
    void itIsRunUpOnFootAndRunDownWithoutAHop() {
        final Obstacles world = new Obstacles(List.of(RAMP, new Block(0, 1500, 200, 500, 0, 0, 250)));
        Body body = Body.standing(new Vec3(0, 0, -200), 0);
        final List<Event> events = new ArrayList<>();
        double was = 0;
        for (int i = 0; i < 360; i++) {
            final Body.Moved moved = body.move(RUN, Clock.STEP, world);
            body = moved.body();
            events.addAll(moved.events());
            assertTrue(body.grounded(), "on its feet all the way up, at " + body.position());
            assertTrue(body.height() >= was - 1e-9, "never lower");
            was = body.height();
        }
        assertEquals(250, body.height(), 1e-9, "up on top");
        assertTrue(events.stream().noneMatch(e -> e instanceof Event.Landed), "never off its feet");
        // and down again, facing the foot
        body = Body.standing(new Vec3(0, -250, 1200), 180);
        for (int i = 0; i < 360; i++) {
            final Body.Moved moved = body.move(RUN, Clock.STEP, world);
            body = moved.body();
            events.addAll(moved.events());
            assertTrue(body.grounded(), "on its feet all the way down, at " + body.position());
        }
        assertEquals(0, body.height(), 1e-9);
        assertTrue(body.position().z() < -100, "down and off the foot");
        assertTrue(events.stream().noneMatch(e -> e instanceof Event.Landed), "not a hop");
    }

    @Test
    void itsHighSideIsAWall() {
        // beside it, near its head, on the ground: running into its side stops
        final Obstacles world = new Obstacles(List.of(RAMP));
        Body body = Body.standing(new Vec3(-400, 0, 900), 90);
        for (int i = 0; i < 240; i++) {
            body = body.move(RUN, Clock.STEP, world).body();
        }
        assertTrue(body.position().x() < -200 + 1e-6, "stopped at its side: " + body.position());
        assertEquals(0, body.height(), 1e-9);
    }

    @Test
    void theFlyoverIsRunUnderStraightAheadOfTheStart() {
        Play play = Play.start(Demo.FLYOVER, false);
        for (int i = 0; i < 600 && play.avatar().body().position().z() < 2200; i++) {
            play = play.step(RUN, Clock.STEP).play();
            assertEquals(0, play.avatar().body().height(), 1e-9, "on the ground all the way");
        }
        assertTrue(play.avatar().body().position().z() > Course.FLYOVER_Z + Course.ROAD_WIDTH, "through under it");
    }

    @Test
    void theFlyoverIsRunUpAcrossAndDownTheOtherSide() {
        final Play start = Play.start(Demo.FLYOVER, false);
        final double foot = Course.DECK_LENGTH / 2 + Course.RAMP_LENGTH;
        Play play = new Play(new Avatar(Body.standing(new Vec3(-foot - 200, 0, Course.FLYOVER_Z), 90), false),
                start.damage(), start.sky(), start.lifts(), start.people(), start.props());
        double highest = 0;
        for (int i = 0; i < 120 * 8; i++) {
            play = play.step(RUN, Clock.STEP).play();
            assertTrue(play.avatar().body().grounded(), "on its feet, at " + play.avatar().body().position());
            highest = Math.max(highest, play.avatar().body().height());
        }
        assertEquals(Course.FLYOVER_HEIGHT, highest, 1e-9, "over the deck");
        assertTrue(play.avatar().body().position().x() > foot, "and down the other side");
        assertEquals(0, play.avatar().body().height(), 1e-9);
    }

    @Test
    void thePlaygroundsRoadsRunClearOfEverythingElse() {
        final Course course = new Course();
        final List<Course.Piece> roads = course.pieces().stream().filter(p -> p.kind() == Course.Kind.ROAD).toList();
        assertEquals(8, roads.size(), "two flyovers of three, and two streets");
        assertEquals(4, roads.stream().filter(p -> p.block().sloped()).count(), "four ramps");
        for (final Course.Piece road : roads) {
            for (final Course.Piece other : course.pieces()) {
                if (other.kind() == Course.Kind.ROAD) {
                    continue;
                }
                for (final Block block : other.solid()) {
                    assertFalse(road.block().overlaps(block), other.kind() + " " + block + " in the way of " + road);
                }
            }
        }
    }

    @Test
    void thePlaygroundsFlyoverBehindTheStartIsRunUpAndOver() {
        final Play start = Play.start(false);
        final double foot = Course.DECK_LENGTH / 2 + Course.RAMP_LENGTH;
        Play play = new Play(new Avatar(Body.standing(new Vec3(-foot - 200, 0, -3500), 90), false), start.damage(),
                start.sky(), start.lifts(), start.people(), start.props());
        double highest = 0;
        for (int i = 0; i < 120 * 8; i++) {
            play = play.step(RUN, Clock.STEP).play();
            assertTrue(play.avatar().body().grounded(), "on its feet, at " + play.avatar().body().position());
            highest = Math.max(highest, play.avatar().body().height());
        }
        assertEquals(Course.FLYOVER_HEIGHT, highest, 1e-9, "over the deck");
        assertTrue(play.avatar().body().position().x() > foot, "and down the other side");
        // and along the street out behind the start, under it
        play = new Play(new Avatar(Body.standing(new Vec3(0, -Course.STREET, -1800), 180), false), start.damage(),
                start.sky(), start.lifts(), start.people(), start.props());
        for (int i = 0; i < 120 * 4; i++) {
            play = play.step(RUN, Clock.STEP).play();
            assertEquals(Course.STREET, play.avatar().body().height(), 1e-9, "on the street, all the way under");
        }
        assertTrue(play.avatar().body().position().z() < -3500 - Course.ROAD_WIDTH, "through under the deck");
    }

    @Test
    void offTheHeadOfTheLoneRampItLeaps() {
        final Play start = Play.start(Demo.FLYOVER, false);
        Play play = new Play(new Avatar(Body.standing(new Vec3(0, 0, 2700), 0), false), start.damage(), start.sky(),
                start.lifts(), start.people(), start.props());
        boolean leapt = false;
        final List<Event> events = new ArrayList<>();
        for (int i = 0; i < 240; i++) {
            final Play.Stepped stepped = play.step(RUN, Clock.STEP);
            play = stepped.play();
            events.addAll(stepped.events());
            leapt |= play.avatar().body().inJump() && play.avatar().body().position().z() > 3450;
        }
        assertTrue(leapt, "off its feet past the head");
        assertTrue(events.stream().anyMatch(e -> e instanceof Event.Landed), "and down again");
        assertTrue(play.avatar().body().grounded());
    }
}
