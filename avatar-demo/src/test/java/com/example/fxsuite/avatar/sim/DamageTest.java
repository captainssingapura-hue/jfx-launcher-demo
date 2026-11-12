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

    private static Event.Struck blow(final int piece, final Weapon by) {
        return new Event.Struck(Vec3.ZERO, new Vec3(0, 0, -1), Surface.BLOCK, by, piece);
    }

    private static List<Event> blows(final int piece, final Weapon by, final int count) {
        final List<Event> all = new ArrayList<>();
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
        assertTrue(worn.events().isEmpty(), "nothing broke yet");
        assertEquals(1, fresh.left(dummy), "a new damage for each blow; the old one unchanged");

        final Damage.Taken broken = worn.damage().took(List.of(blow(dummy, Weapon.RIFLES)));
        assertTrue(broken.damage().broken(dummy));
        assertEquals(0, broken.damage().left(dummy));
        final Event.Broke broke = (Event.Broke) broken.events().getFirst();
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
        assertSame(fresh, fresh.took(List.of(new Event.Struck(Vec3.ZERO, new Vec3(0, -1, 0), Surface.GROUND,
                Weapon.GLAIVE))).damage(), "the ground");
        assertSame(fresh, fresh.took(List.of(new Event.Jumped())).damage(), "nothing struck");
    }

    @Test
    void whatIsBrokenStaysBrokenAndBreaksOnlyOnce() {
        final int drone = first(Course.Kind.DRONE);
        final Damage.Taken taken = Damage.of(course).took(blows(drone, Weapon.GLAIVE, 5));
        assertEquals(1, taken.events().size(), "one break, however many blows land in the frame");
        final Damage.Taken again = taken.damage().took(blows(drone, Weapon.GLAIVE, 5));
        assertTrue(again.events().isEmpty());
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
        assertEquals(course.pieces().size() - 1, broken.obstacles().blocks().size());
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
        Avatar avatar = new Avatar(Body.standing(Vec3.ZERO, Course.RANGE_HEADING), false).drawn(Weapon.RIFLES)
                .delivered();
        Damage damage = Damage.of(course);
        final Controls fire = new Controls(0, 0, 0, 0, false, true, 0);
        Event.Broke broke = null;
        int struck = 0;
        double took = 0;
        while (broke == null && took < 5) {
            avatar = avatar.step(fire, FRAME, damage.obstacles());
            final Damage.Taken taken = damage.took(avatar.events());
            damage = taken.damage();
            avatar = avatar.reporting(taken.events());
            for (final Event event : avatar.events()) {
                if (event instanceof Event.Struck s && s.piece() == middle) {
                    struck++;
                } else if (event instanceof Event.Broke b) {
                    broke = b;
                }
            }
            avatar = avatar.delivered();
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
        avatar = avatar.step(fire, 0.5, damage.obstacles());
        for (final Event event : avatar.events()) {
            if (event instanceof Event.Struck s) {
                assertTrue(s.piece() != middle);
            }
        }
    }
}
