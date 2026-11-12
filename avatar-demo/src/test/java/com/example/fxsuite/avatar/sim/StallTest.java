package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The melon stall on the barge: riding it wherever it flies, and its watermelons to be shot or cut. */
class StallTest {

    private final Course course = new Course();

    private int barge() {
        for (int i = 0; i < course.pieces().size(); i++) {
            final Course.Piece piece = course.pieces().get(i);
            if (piece.kind() == Course.Kind.PLATFORM && Course.Deck.of(piece.shape()).equals(Course.BARGE)) {
                return i;
            }
        }
        throw new AssertionError("no barge");
    }

    private List<Integer> melons() {
        return of(Fruit.WATERMELON);
    }

    private List<Integer> of(final Fruit fruit) {
        final List<Integer> melons = new ArrayList<>();
        for (int i = 0; i < course.pieces().size(); i++) {
            if (course.pieces().get(i).fruit() == fruit) {
                melons.add(i);
            }
        }
        return melons;
    }

    private static Play run(Play play, final Controls controls, final double seconds) {
        for (long step = Math.round(seconds / Clock.STEP); step > 0; step--) {
            play = play.step(controls, Clock.STEP).play();
        }
        return play;
    }

    @Test
    void theStallStandsOnTheBargeItsMelonsOnTheTableAndTheDeck() {
        final int barge = barge();
        final Course.Piece deck = course.pieces().get(barge);
        final List<Course.Piece> riding = course.pieces().stream().filter(p -> p.carrier() == barge).toList();
        assertEquals(1, riding.stream().filter(p -> p.kind() == Course.Kind.STALL).count(), "a table");
        assertEquals(22, riding.stream().filter(p -> p.fruit() == Fruit.WATERMELON).count(), "and 22 watermelons");
        assertEquals(3, riding.stream().filter(p -> p.fruit() == Fruit.PINEAPPLE).count(), "and 3 pineapples");
        assertEquals(25, riding.stream().filter(p -> p.kind() == Course.Kind.FRUIT).count(), "all fruit, of one kind");
        assertEquals(22, melons().size(), "all of them on it");
        assertEquals(3, of(Fruit.PINEAPPLE).size());
        final Block table = riding.stream().filter(p -> p.kind() == Course.Kind.STALL).findFirst().orElseThrow().block();
        final double floor = deck.place().height();
        assertEquals(floor, table.bottom(), 1e-9, "the table on the deck");
        final Block slab = deck.solid().getFirst();
        final List<Block> laid = riding.stream().filter(p -> p.kind() == Course.Kind.FRUIT).map(Course.Piece::block).toList();
        for (final Block melon : laid) {
            assertTrue(melon.bottom() == table.top() || melon.bottom() == floor, "on the table or the deck: " + melon);
            assertTrue(slab.covers(melon.x(), melon.z(), 0), "over the deck");
            assertFalse(melon.overlaps(table) && melon.bottom() < table.top(), "not in the table");
            for (final Block other : laid) {
                assertTrue(other == melon || !melon.overlaps(other), "not in each other");
            }
        }
        assertTrue(Course.Kind.FRUIT.breakable() && Course.Kind.FRUIT.target());
        assertFalse(Course.Kind.STALL.breakable(), "the table stands whatever is thrown at it");
    }

    @Test
    void flyingTheBargeTakesTheStallWithItAndItsOwnCargoDoesNotHoldItBack() {
        final Play start = Play.start(false);
        final int k = 1;
        final Lift lift = start.lifts().get(k);
        assertEquals(barge(), lift.piece());
        Play play = new Play(new Avatar(Body.standing(lift.pilot(), lift.place().yaw()), false), start.damage(),
                start.sky(), start.lifts());
        play = play.step(Controls.doing(Action.TAKE), Clock.STEP).play();
        play = run(play, new Controls(1, 0, 0.3, 1, true, false, 0), 2);
        final Place now = play.lifts().get(k).place();
        assertTrue(Math.hypot(now.x() - lift.place().x(), now.z() - lift.place().z()) > 500, "it flew");
        assertTrue(now.height() > Course.LIFT_LOW + 200, "and climbed");
        final Map<Integer, Block> moving = play.moving();
        for (final int melon : melons()) {
            final Block expected = course.pieces().get(melon).placed(now).block();
            assertEquals(expected, moving.get(melon), "melon " + melon + " where the barge has carried it");
        }
    }

    @Test
    void standingOnTheMelonsTheAvatarGoesWithTheBargeToo() {
        final Play start = Play.start(false);
        final int k = 1;
        final Lift drifting = new Lift(barge(), Course.BARGE, start.lifts().get(k).place(), 500, 0, 0, false);
        final List<Lift> lifts = new ArrayList<>(start.lifts());
        lifts.set(k, drifting);
        // on top of the melons on the table, which ride on the barge as it drifts on
        final Block melon = course.pieces().get(melons().get(8)).block();
        final Vec3 on = new Vec3(melon.x(), -melon.top(), melon.z());
        Play play = new Play(new Avatar(Body.standing(on, 0), false), start.damage(), start.sky(), lifts);
        play = run(play, Controls.NONE, 0.5);
        final Place moved = play.lifts().get(k).place();
        final Vec3 expected = drifting.place().carry(on, moved);
        assertTrue(Math.hypot(moved.x() - drifting.place().x(), moved.z() - drifting.place().z()) > 100, "drifting on");
        assertEquals(expected.x(), play.avatar().body().position().x(), 1e-6, "and the avatar with it");
        assertEquals(expected.z(), play.avatar().body().position().z(), 1e-6);
        assertEquals(melon.top(), play.avatar().body().height(), 1e-6, "still on the melons");
    }

    @Test
    void aMelonTakesThreeRoundsOrOneStroke() {
        final Damage damage = Damage.of(course);
        final int melon = melons().getFirst();
        final Vec3 at = new Vec3(0, -100, 0);
        final Blow round = new Blow(at, new Vec3(0, -1, 0), Surface.BLOCK, Weapon.RIFLES, melon);
        Damage.Taken taken = damage.took(List.of(round, round));
        assertFalse(taken.damage().broken(melon), "two rounds, and it holds");
        taken = taken.damage().took(List.of(round));
        assertTrue(taken.damage().broken(melon), "the third bursts it");
        assertTrue(taken.events().stream().anyMatch(e -> e instanceof Event.Broke b && b.kind() == Course.Kind.FRUIT
                && b.fruit() == Fruit.WATERMELON));
        final Blow stroke = new Blow(at, new Vec3(0, -1, 0), Surface.BLOCK, Weapon.GLAIVE, melon);
        assertTrue(damage.took(List.of(stroke)).damage().broken(melon), "one stroke of the glaive");
    }

    @Test
    void aPineappleTakesTwoRoundsAndBurstsAsAPineapple() {
        final Damage damage = Damage.of(course);
        final int pineapple = of(Fruit.PINEAPPLE).getFirst();
        final Blow round = new Blow(new Vec3(0, -100, 0), new Vec3(0, -1, 0), Surface.BLOCK, Weapon.RIFLES, pineapple);
        Damage.Taken taken = damage.took(List.of(round));
        assertFalse(taken.damage().broken(pineapple), "one round, and it holds");
        taken = taken.damage().took(List.of(round));
        assertTrue(taken.damage().broken(pineapple), "the second bursts it");
        assertTrue(taken.events().stream().anyMatch(e -> e instanceof Event.Broke b && b.fruit() == Fruit.PINEAPPLE));
        assertTrue(Fruit.PINEAPPLE.heft() < Fruit.WATERMELON.heft(), "and it bursts higher than a watermelon");
    }

    @Test
    void aMelonBurstsWhereItIsWhereverTheBargeHasTakenIt() {
        final Damage damage = Damage.of(course);
        final int melon = melons().getFirst();
        final Course.Piece piece = course.pieces().get(melon);
        final Place far = new Place(piece.place().x() + 3000, piece.place().z() - 1000, 2000, 75);
        final Blow stroke = new Blow(Vec3.ZERO, new Vec3(0, -1, 0), Surface.BLOCK, Weapon.GLAIVE, melon);
        final Damage.Taken taken = damage.took(List.of(stroke), i -> damage.piece(i).moves()
                ? damage.piece(i).placed(far) : damage.piece(i));
        final Event.Broke broke = taken.events().stream().filter(e -> e instanceof Event.Broke)
                .map(e -> (Event.Broke) e).findFirst().orElseThrow();
        assertEquals(piece.placed(far).block(), broke.block(), "its pieces fly from where it is now");
        assertTrue(taken.damage().riding(barge()).stream().noneMatch(i -> i == melon), "and it rides no more");
    }
}
