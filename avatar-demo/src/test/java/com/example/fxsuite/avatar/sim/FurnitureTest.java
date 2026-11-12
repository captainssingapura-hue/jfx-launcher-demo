package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Loose things: pushed about, falling, landing whole or not, and what stands on them going with them. */
class FurnitureTest {

    private static Play run(Play play, final Controls controls, final double seconds, final List<Event> events) {
        for (long step = Math.round(seconds / Clock.STEP); step > 0; step--) {
            final Play.Stepped stepped = play.step(controls, Clock.STEP);
            play = stepped.play();
            events.addAll(stepped.events());
        }
        return play;
    }

    private static int table(final Play play) {
        for (int k = 0; k < play.props().size(); k++) {
            if (play.props().get(k).thing() == Furniture.TABLE) {
                return k;
            }
        }
        throw new AssertionError("no table");
    }

    /** The props that stand on the table's top at the start. */
    private static List<Integer> onTheTable(final Play play) {
        final double top = play.props().get(table(play)).place().height() + Furniture.TABLE.height();
        final List<Integer> on = new ArrayList<>();
        for (int k = 0; k < play.props().size(); k++) {
            if (play.props().get(k).place().height() == top) {
                on.add(k);
            }
        }
        return on;
    }

    @Test
    void walkingIntoTheTablePushesItSlowlyAndWhatIsOnItGoesWithIt() {
        final Play start = Play.start(false);
        final Prop table = start.props().get(table(start));
        final Place at = table.place();
        // just before its front, a little to one side, facing it
        final Vec3 spot = new Vec3(at.x(), -at.height(), at.z())
                .add(Num.heading(at.yaw()).multiply(Furniture.TABLE.width() / 2 + Body.RADIUS + 4))
                .add(Num.right(at.yaw()).multiply(60));
        Play play = new Play(new Avatar(Body.standing(spot, Num.wrap(at.yaw() + 180)), false), start.damage(),
                start.sky(), start.lifts(), start.people(), start.props());
        play = run(play, new Controls(1, 0, 0, 0, false, false, 0), 0.8, new ArrayList<>());
        final Place moved = play.props().get(table(start)).place();
        final double went = new Vec3(moved.x() - at.x(), 0, moved.z() - at.z()).dotProduct(Num.heading(at.yaw()));
        assertTrue(went < -30, "pushed back the way the avatar walked: " + went);
        assertTrue(-went < Body.WALK * 0.8, "and slower than it walks: heavy");
        for (final int k : onTheTable(start)) {
            final Place was = start.props().get(k).place();
            final Place is = play.props().get(k).place();
            assertEquals(was.x() - at.x(), is.x() - moved.x(), 1e-6, "prop " + k + " went with the table");
            assertEquals(was.z() - at.z(), is.z() - moved.z(), 1e-6);
            assertEquals(was.height(), is.height(), 1e-9);
        }
        assertFalse(play.props().get(table(start)).box().covers(play.avatar().body().position().x(),
                play.avatar().body().position().z(), Body.RADIUS - 2), "never in the avatar");
    }

    @Test
    void itSlidesToAStopShortOfWhatIsInItsWay() {
        final Play start = Play.start(false);
        final int k = table(start);
        final List<Prop> props = new ArrayList<>(start.props());
        final Place at = props.get(k).place();
        // sent hard toward the barge's console, three metres off
        final Vec3 toward = Num.heading(at.yaw()).multiply(3000);
        props.set(k, props.get(k).pushed(toward.x(), toward.z()));
        Play play = new Play(start.avatar(), start.damage(), start.sky(), start.lifts(), start.people(), props);
        play = run(play, Controls.NONE, 2, new ArrayList<>());
        final Prop stopped = play.props().get(k);
        assertEquals(0, stopped.sliding(), "at a stop");
        final double went = new Vec3(stopped.place().x() - at.x(), 0, stopped.place().z() - at.z()).magnitude();
        assertTrue(went > 200, "having gone some way: " + went);
        final Block console = start.damage().piece(start.lifts().get(1).piece()).solid().get(1);
        assertFalse(stopped.box().overlaps(console), "short of the console, not in it");
    }

    @Test
    void theTableBrokenWhatWasOnItFallsToTheDeckWhole() {
        final Play start = Play.start(false);
        final Prop table = start.props().get(table(start));
        final List<Blow> blows = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            blows.add(new Blow(Vec3.ZERO, new Vec3(0, -1, 0), Surface.BLOCK, Weapon.GLAIVE, table.piece()));
        }
        final Damage broken = start.damage().took(blows).damage();
        assertTrue(broken.broken(table.piece()), "two strokes of the glaive");
        final List<Event> events = new ArrayList<>();
        final Play play = run(new Play(start.avatar(), broken, start.sky(), start.lifts(), start.people(),
                start.props()), Controls.NONE, 1, events);
        final double deck = start.lifts().get(1).place().height();
        for (final int k : onTheTable(start)) {
            assertEquals(deck, play.props().get(k).place().height(), 1e-9, "prop " + k + " down on the deck");
            assertFalse(play.damage().broken(play.props().get(k).piece()), "and whole: a short drop");
        }
    }

    /** The start, the first watermelon on its own in the open, {@code high} up. */
    private static Play dropping(final double high) {
        final Play start = Play.start(false);
        final List<Prop> props = new ArrayList<>(start.props());
        final int k = onTheTable(start).getFirst();
        props.set(k, new Prop(props.get(k).piece(), props.get(k).thing(), new Place(300, 300, high, 0)));
        return new Play(start.avatar(), start.damage(), start.sky(), start.lifts(), start.people(), props);
    }

    @Test
    void aMelonDroppedFromHighEnoughBursts() {
        final List<Event> events = new ArrayList<>();
        final Play low = run(dropping(100), Controls.NONE, 1, events);
        final Prop lowMelon = low.props().get(onTheTable(Play.start(false)).getFirst());
        assertEquals(0, lowMelon.place().height(), 1e-9, "a metre down: on the ground");
        assertFalse(low.damage().broken(lowMelon.piece()), "and whole");
        events.clear();
        final Play high = run(dropping(300), Controls.NONE, 1, events);
        assertTrue(high.damage().broken(lowMelon.piece()), "three metres down: burst");
        assertTrue(events.stream().anyMatch(e -> e instanceof Event.Broke b && b.by() == Weapon.FALL
                && b.fruit() == Fruit.WATERMELON));
    }
}
