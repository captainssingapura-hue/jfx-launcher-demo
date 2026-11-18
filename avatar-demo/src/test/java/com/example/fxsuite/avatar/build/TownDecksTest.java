package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Controls;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Demo;
import com.example.fxsuite.avatar.sim.Demos;
import com.example.fxsuite.avatar.sim.Furniture;
import com.example.fxsuite.avatar.sim.Person;
import com.example.fxsuite.avatar.sim.Play;
import com.example.fxsuite.avatar.sim.Prop;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The town's towers' void decks: every tower has one, three open all round and three with a wing
 * enclosed; on each, a bar and a melon stall set out, loose, and the people at them; and in the
 * game, all of it stays on the deck: the things rest on its floor, the people stand and sit there.
 */
class TownDecksTest {

    @AfterEach
    void townAsItWas() {
        Course.town(Town::pieces);
    }

    @Test
    void everyTowerHasAVoidDeckThreeFullThreePartialNothingWrong() {
        int full = 0;
        int partial = 0;
        for (int k = Town.FIRST_TOWER; k < Town.SITES.size(); k++) {
            final Tower tower = Town.tower(k);
            final String name = Town.SITES.get(k).name();
            final List<Tower.Section> decks = tower.sections().stream().filter(Tower.Section::open).toList();
            assertEquals(1, decks.size(), name + ": a void deck");
            if (decks.getFirst().enclosed() == null) {
                full++;
            } else {
                partial++;
            }
            assertTrue(tower.problems().isEmpty(), name + ": " + tower.problems());
            final DeckGarden garden = Town.deckGarden(k);
            assertTrue(garden.bar().isPresent(), name + ": room for a bar");
            assertTrue(garden.stall().isPresent(), name + ": room for a stall");
        }
        assertEquals(3, full);
        assertEquals(3, partial);
    }

    @Test
    void onEachDeckABarAndAStallAndThePeopleAtThem() {
        final List<Course.Piece> things = Town.things();
        final int towers = Town.SITES.size() - Town.FIRST_TOWER;
        assertEquals(towers, things.stream().filter(p -> p.furniture() == Furniture.BAR).count(), "a bar on each");
        assertEquals(towers, things.stream().filter(p -> p.furniture() == Furniture.TABLE).count(), "a stall on each");
        assertEquals(3L * towers, things.stream().filter(p -> p.furniture() == Furniture.BAR_STOOL).count());
        final List<Course.Cast> people = Town.onDecks();
        assertEquals(towers, people.stream().filter(c -> c.role() == Person.Role.BARTENDER).count());
        assertEquals(2L * towers, people.stream().filter(c -> c.role() == Person.Role.DRINKER).count());
        assertEquals(towers, people.stream().filter(c -> c.role() == Person.Role.STALLHOLDER).count());
        assertEquals(towers, people.stream().filter(c -> c.role() == Person.Role.CUSTOMER).count());
        // each up on its tower's deck
        for (final Course.Cast cast : people) {
            assertTrue(cast.deck() == null && cast.up() > 1000, cast + ": up on a deck");
        }
    }

    @Test
    void inTheGameItAllStaysOnTheDeck() {
        Town.tell();
        Play play = Play.start(Demos.TOWN, false);
        final List<Course.Piece> pieces = new Course(Demos.TOWN).pieces();
        final List<Prop> before = play.props().stream().filter(p -> p.place().height() > 1000).toList();
        final List<Person> people = play.people().stream().filter(p -> p.body().height() > 1000).toList();
        assertEquals(Town.things().size(), before.size(), "every thing on a deck");
        assertEquals(Town.onDecks().size(), people.size(), "everyone on a deck");
        for (int i = 0; i < 120; i++) {
            play = play.step(Controls.NONE, 1.0 / 60).play();
        }
        for (final Prop was : before) {
            final Prop now = play.props().stream().filter(p -> p.piece() == was.piece()).findFirst().orElseThrow();
            assertEquals(was.place().height(), now.place().height(), 1, pieces.get(was.piece()) + ": still where it was put");
            assertEquals(was.place().x(), now.place().x(), 1);
            assertEquals(was.place().z(), now.place().z(), 1);
        }
        for (final Person was : people) {
            final Person now = play.people().get(was.id());
            assertEquals(was.body().height(), now.body().height(), 2, was.role() + ": still on the deck");
            assertEquals(was.sitting(), now.sitting(), was.role() + ": sat as they were");
            assertTrue(!now.down(), was.role() + ": unhurt");
        }
    }
}
