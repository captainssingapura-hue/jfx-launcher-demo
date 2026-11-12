package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What the weapons do to the world: wearing it down, and breaking it. */
class DamageTest {

    private static final double FRAME = 1.0 / 60;
    private final Course course = new Course();

    private int first(final Course.Kind kind) {
        final List<Course.Piece> pieces = course.pieces();
        for (int i = 0; i < pieces.size(); i++) {
            if (pieces.get(i).kind() == kind) {
                return i;
            }
        }
        throw new AssertionError("no " + kind);
    }

    private static Blow blow(final int piece, final Weapon by) {
        return new Blow(Vec3.ZERO, new Vec3(0, 0, -1), Surface.BLOCK, by, piece);
    }

    private static List<Blow> blows(final int piece, final Weapon by, final int count) {
        final List<Blow> all = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            all.add(blow(piece, by));
        }
        return all;
    }

    @Test
    void aRaySaysWhichPieceItMet() {
        final int dummy = first(Course.Kind.DUMMY);
        final Block board = course.pieces().get(dummy).block();
        final Vec3 from = new Vec3(0, -100, 0);
        final Vec3 at = new Vec3(board.x(), -100, board.z());
        final Hit hit = course.obstacles().ray(from, at.subtract(from).normalize(), 5000);
        assertEquals(dummy, hit.piece());
        assertEquals(-1, course.obstacles().ray(from, new Vec3(0, 1, 0), 5000).piece(), "the ground is no piece");
    }

    @Test
    void eachBlowWearsAPieceDownAndTheLastBreaksIt() {
        final int dummy = first(Course.Kind.DUMMY);
        final Damage fresh = Damage.of(course);
        assertEquals(1, fresh.left(dummy));
        final int needed = (int) Math.ceil(Damage.DUMMY / Weapon.RIFLES.blow());

        final Damage.Taken worn = fresh.took(blows(dummy, Weapon.RIFLES, needed - 1));
        assertEquals(1.0 / needed, worn.damage().left(dummy), 1e-9);
        assertFalse(worn.damage().broken(dummy));
        assertTrue(worn.events().stream().noneMatch(e -> e instanceof Event.Broke), "nothing broke yet");
        // each blow comes out struck, with what the dummy had left after it
        assertEquals(needed - 1, worn.events().size());
        final Event.Struck last = (Event.Struck) worn.events().getLast();
        assertEquals(dummy, last.piece());
        assertEquals(1.0 / needed, last.left(), 1e-9);
        assertEquals((needed - 1.0) / needed, ((Event.Struck) worn.events().getFirst()).left(), 1e-9);
        assertEquals(1, fresh.left(dummy), "a new damage for each blow; the old one unchanged");

        final Damage.Taken broken = worn.damage().took(List.of(blow(dummy, Weapon.RIFLES)));
        assertTrue(broken.damage().broken(dummy));
        assertEquals(0, broken.damage().left(dummy));
        assertEquals(0, ((Event.Struck) broken.events().getFirst()).left(), "struck, with nothing left");
        final Event.Broke broke = (Event.Broke) broken.events().get(1);
        assertEquals(dummy, broke.piece());
        assertEquals(Course.Kind.DUMMY, broke.kind());
        assertEquals(Weapon.RIFLES, broke.by());
        assertEquals(course.pieces().get(dummy).block(), broke.block());
    }

    @Test
    void theWeaponsHitAsHardAsTheyShould() {
        final int drone = first(Course.Kind.DRONE);
        // a drone falls to a dozen rifle rounds, fifteen of the Gatling gun's, or two strokes of the glaive
        assertTrue(Damage.of(course).took(blows(drone, Weapon.RIFLES, 12)).damage().broken(drone));
        assertFalse(Damage.of(course).took(blows(drone, Weapon.RIFLES, 11)).damage().broken(drone));
        assertTrue(Damage.of(course).took(blows(drone, Weapon.GATLING, 15)).damage().broken(drone));
        assertFalse(Damage.of(course).took(blows(drone, Weapon.GATLING, 14)).damage().broken(drone));
        assertTrue(Damage.of(course).took(blows(drone, Weapon.GLAIVE, 2)).damage().broken(drone));
    }

    @Test
    void biggerCratesAreTougherAndNoneTakesLong() {
        final List<Course.Piece> crates = course.pieces().stream()
                .filter(p -> p.kind() == Course.Kind.CRATE).toList();
        final Course.Piece small = crates.stream().min((a, b) -> Double.compare(volume(a), volume(b))).orElseThrow();
        final Course.Piece big = crates.stream().max((a, b) -> Double.compare(volume(a), volume(b))).orElseThrow();
        assertTrue(Damage.toughness(big) > 3 * Damage.toughness(small));
        // the Gatling gun, at 40 rounds a second, gets through the biggest in a few seconds
        final double seconds = Damage.toughness(big) / (Weapon.GATLING.blow() * Arms.GATLING_RATE / 60);
        assertTrue(seconds > 1 && seconds < 3, "the biggest crate takes " + seconds + " s of the Gatling gun");
    }

    private static double volume(final Course.Piece piece) {
        final Block b = piece.block();
        return b.halfWidth() * b.halfDepth() * (b.top() - b.bottom());
    }

    @Test
    void pillarsGatesAndTheGroundStandWhateverIsThrownAtThem() {
        final Damage fresh = Damage.of(course);
        for (final Course.Kind kind : new Course.Kind[] {Course.Kind.PILLAR, Course.Kind.GATE}) {
            final int piece = first(kind);
            final Damage.Taken taken = fresh.took(blows(piece, Weapon.GLAIVE, 1000));
            assertSame(fresh, taken.damage(), kind + " is untouched");
            assertEquals(1, taken.damage().left(piece));
        }
        final Damage.Taken ground = fresh.took(List.of(new Blow(Vec3.ZERO, new Vec3(0, -1, 0), Surface.GROUND,
                Weapon.GLAIVE)));
        assertSame(fresh, ground.damage(), "the ground");
        assertEquals(List.of(new Event.Struck(Vec3.ZERO, new Vec3(0, -1, 0), Surface.GROUND, Weapon.GLAIVE)),
                ground.events(), "struck all the same, and none the worse");
        assertSame(fresh, fresh.took(List.of()).damage(), "nothing struck");
    }

    @Test
    void whatIsBrokenStaysBrokenAndBreaksOnlyOnce() {
        final int drone = first(Course.Kind.DRONE);
        final Damage.Taken taken = Damage.of(course).took(blows(drone, Weapon.GLAIVE, 5));
        assertEquals(1, taken.events().stream().filter(e -> e instanceof Event.Broke).count(),
                "one break, however many blows land in the step");
        final Damage.Taken again = taken.damage().took(blows(drone, Weapon.GLAIVE, 5));
        assertTrue(again.events().stream().noneMatch(e -> e instanceof Event.Broke));
        assertTrue(again.events().stream().allMatch(e -> ((Event.Struck) e).left() == 0), "nothing left to take");
        assertTrue(again.damage().broken(drone));
    }

    @Test
    void whatIsBrokenIsGoneFromTheWorld() {
        final int dummy = first(Course.Kind.DUMMY);
        final Block board = course.pieces().get(dummy).block();
        final Vec3 from = new Vec3(0, -100, 0);
        final Vec3 toward = new Vec3(board.x(), -100, board.z()).subtract(from).normalize();
        final Damage broken = Damage.of(course).took(blows(dummy, Weapon.GLAIVE, 3)).damage();
        final Hit through = broken.obstacles().ray(from, toward, 5000);
        assertTrue(through == null || through.piece() != dummy, "rounds fly on where it stood");
        assertEquals(course.obstacles().blocks().size() - 1, broken.obstacles().blocks().size());
        assertEquals(course.obstacles().blocks().size(), Damage.of(course).obstacles().blocks().size());
    }

    @Test
    void whatStoodOnItFallsWhenItBreaks() {
        // standing on the first stair, which then breaks under it
        final Block stair = course.pieces().get(0).block();
        Body body = Body.standing(new Vec3(stair.x(), -stair.top(), stair.z()), Course.START_YAW);
        final Controls still = new Controls(0, 0, 0, 0, false);
        Damage damage = Damage.of(course);
        for (int i = 0; i < 30; i++) {
            body = body.step(still, FRAME, damage.obstacles());
        }
        assertEquals(stair.top(), body.height(), 1e-9, "standing on it");
        damage = damage.took(blows(0, Weapon.GLAIVE, 10)).damage();
        for (int i = 0; i < 60; i++) {
            body = body.step(still, FRAME, damage.obstacles());
        }
        assertEquals(0, body.height(), 1e-9, "down on the ground");
    }

    @Test
    void firingAtADummyBreaksItAndTheRoundsThenFlyOn() {
        final int middle = first(Course.Kind.DUMMY) + 2;
        final Block board = course.pieces().get(middle).block();
        Play play = new Play(new Avatar(Body.standing(Vec3.ZERO, Course.RANGE_HEADING), false)
                .drawn(Weapon.RIFLES), Damage.of(course));
        final Controls fire = new Controls(0, 0, 0, 0, false, true, 0);
        Event.Broke broke = null;
        int struck = 0;
        double took = 0;
        while (broke == null && took < 5) {
            final Play.Stepped stepped = play.step(fire, FRAME);
            play = stepped.play();
            for (final Event event : stepped.events()) {
                if (event instanceof Event.Struck s && s.piece() == middle) {
                    struck++;
                } else if (event instanceof Event.Broke b) {
                    broke = b;
                }
            }
            took += FRAME;
        }
        assertNotNull(broke, "broken within " + took + " s");
        assertEquals(middle, broke.piece());
        assertEquals((int) (Damage.DUMMY / Weapon.RIFLES.blow()), struck, "broken by the round that took the last of it");
        // 300 harm at 20 rounds of 10 a second, were every round to strike it; from the hip, a few go wide
        assertTrue(took >= 1.5 - FRAME && took < 2.2, "broken in " + took + " s");
        assertTrue(broke.struck().distance(new Vec3(board.x(), broke.struck().y(), board.z())) < 60,
                "the last blow struck it");
        // fire on: nothing more of it to strike
        for (final Event event : play.step(fire, 0.5).events()) {
            if (event instanceof Event.Struck s) {
                assertTrue(s.piece() != middle);
            }
        }
    }

    @Test
    void anAirshipIsManyBlocksAllOnePieceAndGoesAllAtOnce() {
        final int airship = first(Course.Kind.AIRSHIP);
        final Course.Piece piece = course.pieces().get(airship);
        final int parts = Course.HULL.size() + Course.FINS.size() + 1 + Course.PYLONS.size() + Course.ENGINES.size();
        assertEquals(parts, piece.solid().size());
        final Obstacles world = course.obstacles();
        assertEquals(parts, world.pieces().stream().filter(n -> n == airship).count(), "each block numbered as it");

        // a round up into the gondola, and one into a fin, both strike the airship
        final Block gondola = piece.solid().get(Course.HULL.size() + Course.FINS.size());
        final Block fin = piece.solid().get(Course.HULL.size());
        for (final Block part : List.of(gondola, fin)) {
            final Vec3 below = new Vec3(part.x(), -(part.bottom() - 500), part.z());
            assertEquals(airship, world.ray(below, new Vec3(0, -1, 0), 1000).piece());
        }

        final Damage down = Damage.of(course).took(blows(airship, Weapon.GLAIVE, 30)).damage();
        assertTrue(down.broken(airship));
        assertFalse(down.obstacles().pieces().contains(airship), "not a block of it left");
        assertEquals(world.blocks().size() - parts, down.obstacles().blocks().size());
    }

    @Test
    void anAirshipTakesMuchMoreThanAnythingElse() {
        final int airship = first(Course.Kind.AIRSHIP);
        final Course.Piece piece = course.pieces().get(airship);
        for (final Course.Piece other : course.pieces()) {
            if (other.kind() != Course.Kind.AIRSHIP && other.kind().breakable()) {
                assertTrue(Damage.toughness(piece) > 4 * Damage.toughness(other), other.toString());
            }
        }
        final int rounds = (int) Math.ceil(Damage.AIRSHIP / Weapon.GATLING.blow());
        assertFalse(Damage.of(course).took(blows(airship, Weapon.GATLING, rounds - 1)).damage().broken(airship));
        assertTrue(Damage.of(course).took(blows(airship, Weapon.GATLING, rounds)).damage().broken(airship));
        final double seconds = rounds / (Arms.GATLING_RATE / 60);
        assertTrue(seconds > 8 && seconds < 12, "most of ten seconds of the Gatling gun: " + seconds);
    }

    @Test
    void theAirshipsHangOverEverythingElseWithinReachOfTheJets() {
        final List<Course.Piece> airships = course.pieces().stream()
                .filter(p -> p.kind() == Course.Kind.AIRSHIP).toList();
        assertEquals(3, airships.size());
        final double highest = course.pieces().stream().filter(p -> p.kind() != Course.Kind.AIRSHIP)
                .mapToDouble(p -> p.block().top()).max().orElseThrow();
        for (final Course.Piece airship : airships) {
            assertTrue(airship.block().bottom() > highest + 100, "clear over the tallest pillar: " + airship);
            assertTrue(airship.block().top() < Body.CEILING, "and under the jets' ceiling");
            for (final Block part : airship.solid()) {
                assertTrue(Math.abs(part.x()) < Body.EDGE && Math.abs(part.z()) < Body.EDGE, "within the world");
            }
        }
    }

    @Test
    void theRobotCanLandOnAnAirshipsBack() {
        final Course.Piece airship = course.pieces().get(first(Course.Kind.AIRSHIP));
        final Block top = airship.solid().get(3);
        final Obstacles.Footing footing = course.obstacles().footing(top.x(), top.z(), Body.FOOTING, top.top() + 1);
        assertEquals(top.top(), footing.height(), 1e-9);
    }

    @Test
    void eachStepTakesItsOwnBlowsOnce() {
        // each step's blows wear the world down once, and come out of the step beside it
        final int middle = first(Course.Kind.DUMMY) + 2;
        Play play = new Play(new Avatar(Body.standing(Vec3.ZERO, Course.RANGE_HEADING), false)
                .drawn(Weapon.RIFLES), Damage.of(course));
        final Controls fire = new Controls(0, 0, 0, 0, false, true, 0);
        int struck = 0;
        for (int i = 0; i < 24; i++) {
            final Play.Stepped stepped = play.step(fire, Clock.STEP);
            play = stepped.play();
            struck += (int) stepped.events().stream()
                    .filter(e -> e instanceof Event.Struck s && s.piece() == middle).count();
        }
        assertTrue(struck > 0);
        assertEquals(Damage.DUMMY - struck * Weapon.RIFLES.blow(), play.damage().health(middle), 1e-9);
    }
}
