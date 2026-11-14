package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The fruit stall on the barge: its table and fruit, loose things standing where they were put, going where the barge goes. */
class StallTest {

    private final Course course = new Course();

    private List<Integer> of(final Fruit fruit) {
        final List<Integer> found = new ArrayList<>();
        for (int i = 0; i < course.pieces().size(); i++) {
            if (course.pieces().get(i).fruit() == fruit) {
                found.add(i);
            }
        }
        return found;
    }

    private List<Integer> melons() {
        return of(Fruit.WATERMELON);
    }

    private static Play run(Play play, final Controls controls, final double seconds) {
        for (long step = Math.round(seconds / Clock.STEP); step > 0; step--) {
            play = play.step(controls, Clock.STEP).play();
        }
        return play;
    }

    @Test
    void theTableStandsOnTheBargeItsFruitOnItAndOnTheDeck() {
        final Play start = Play.start(false);
        final double deck = start.lifts().get(1).place().height();
        // the stall's: not the crates, loose about the course
        final List<Prop> props = start.props().stream().filter(p -> !(p.thing() instanceof Crate)).toList();
        assertEquals(28, props.size(), "a table, a stool, a scooter, 22 watermelons and 3 pineapples");
        assertEquals(1, props.stream().filter(p -> p.thing() == Furniture.STOOL).count());
        assertEquals(1, props.stream().filter(p -> p.thing() == Furniture.SCOOTER).count());
        final Prop table = props.stream().filter(p -> p.thing() == Furniture.TABLE).findFirst().orElseThrow();
        assertEquals(deck, table.place().height(), 1e-9, "the table on the deck");
        assertEquals(22, melons().size());
        assertEquals(3, of(Fruit.PINEAPPLE).size());
        final double top = deck + Furniture.TABLE.height();
        final Block slab = start.damage().piece(start.lifts().get(1).piece()).solid().getFirst();
        for (final Prop prop : props) {
            assertTrue(course.pieces().get(prop.piece()).kind().loose());
            assertFalse(course.pieces().get(prop.piece()).rides(), "loose, riding nothing");
            if (prop != table) {
                final double on = prop.place().height();
                assertTrue(on == top || on == deck, "on the table or the deck: " + on);
                assertFalse(prop.box().overlaps(table.box()), "not in the table");
            }
            assertTrue(slab.covers(prop.box().x(), prop.box().z(), 0), "over the deck");
            for (final Prop other : props) {
                assertTrue(other == prop || !prop.box().overlaps(other.box()), "not in each other");
            }
        }
        assertTrue(Course.Kind.FRUIT.breakable() && Course.Kind.FURNITURE.breakable());
    }

    @Test
    void leftAloneItAllStaysJustWhereItIs() {
        final Play start = Play.start(false);
        final Play later = run(start, Controls.NONE, 2);
        for (int k = 0; k < start.props().size(); k++) {
            assertEquals(start.props().get(k).place(), later.props().get(k).place(), "prop " + k);
        }
        assertTrue(start.props().stream().noneMatch(p -> later.damage().broken(p.piece())), "and nothing broken");
    }

    @Test
    void flyingTheBargeTakesTheStallWithIt() {
        final Play start = Play.start(false);
        final Lift barge = start.lifts().get(1);
        Play play = new Play(new Avatar(Body.standing(barge.pilot(), barge.place().yaw()), false), start.damage(),
                start.sky(), start.lifts(), start.people(), start.props());
        play = play.step(Controls.doing(Action.TAKE), Clock.STEP).play();
        play = run(play, new Controls(0, 0, 0.3, 1, true, false, 0), 2);
        final Place now = play.lifts().get(1).place();
        assertTrue(now.height() > barge.place().height() + 300, "it climbed");
        for (int k = 0; k < start.props().size(); k++) {
            if (start.props().get(k).thing() instanceof Crate) {
                continue;  // the crates stand about the course, not on the barge
            }
            final Place was = start.props().get(k).place();
            final Vec3 expected = barge.place().carry(new Vec3(was.x(), -was.height(), was.z()), now);
            final Place is = play.props().get(k).place();
            assertEquals(expected.x(), is.x(), 1e-6, "prop " + k + " carried");
            assertEquals(expected.z(), is.z(), 1e-6);
            assertEquals(-expected.y(), is.height(), 1e-6);
            assertEquals(Num.wrap(was.yaw() + now.yaw() - barge.place().yaw()), is.yaw(), 1e-6, "and turned");
        }
        final Play flown = play;
        assertTrue(start.props().stream().noneMatch(p -> flown.damage().broken(p.piece())));
    }

    @Test
    void standingOnTheMelonsTheAvatarGoesWithTheBargeToo() {
        final Play start = Play.start(false);
        final int k = 1;
        final Lift drifting = new Lift(start.lifts().get(k).piece(), Course.BARGE, start.lifts().get(k).place(), 500, 0,
                0, false);
        final List<Lift> lifts = new ArrayList<>(start.lifts());
        lifts.set(k, drifting);
        // on top of the melons on the table, which stand on the table, which stands on the barge as it drifts on
        final Block melon = course.pieces().get(melons().get(8)).block();
        final Vec3 on = new Vec3(melon.x(), -melon.top(), melon.z());
        Play play = new Play(new Avatar(Body.standing(on, 0), false), start.damage(), start.sky(), lifts,
                start.people(), start.props());
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
    void aMelonBurstsWhereItIs() {
        final Damage damage = Damage.of(course);
        final int melon = melons().getFirst();
        final Course.Piece piece = course.pieces().get(melon);
        final Place far = new Place(piece.place().x() + 3000, piece.place().z() - 1000, 2000, 75);
        final Blow stroke = new Blow(Vec3.ZERO, new Vec3(0, -1, 0), Surface.BLOCK, Weapon.GLAIVE, melon);
        final Damage.Taken taken = damage.took(List.of(stroke), i -> i == melon ? piece.placed(far) : damage.piece(i));
        final Event.Broke broke = taken.events().stream().filter(e -> e instanceof Event.Broke)
                .map(e -> (Event.Broke) e).findFirst().orElseThrow();
        assertEquals(piece.placed(far).block(), broke.block(), "its pieces fly from where it is now");
        assertTrue(taken.damage().moving().stream().noneMatch(i -> i == melon), "and it is gone");
    }
}
