package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The platforms, flown like magic carpets from their consoles: taking the controls, flying, letting go. */
class LiftTest {

    private static final Controls TAKE = Controls.doing(Action.TAKE);
    private static final Controls AHEAD = new Controls(1, 0, 0, 0, true, false, 0);
    private static final Controls UP = new Controls(0, 0, 0, 1, true, false, 0);
    private static final Controls DOWN = new Controls(0, 0, 0, -1, true, false, 0);

    /** {@code play}, with the avatar standing where platform {@code k}'s pilot stands, facing the way it heads. */
    private static Play piloting(final Play play, final int k) {
        final Lift lift = play.lifts().get(k);
        final Body body = Body.standing(lift.pilot(), lift.place().yaw());
        return new Play(new Avatar(body, false), play.damage(), play.sky(), play.lifts());
    }

    /** The start, with the avatar at the small platform's controls, taken. */
    private static Play flying(final List<Event> events) {
        return once(piloting(Play.start(false), 0), TAKE, events);
    }

    private static Play run(Play play, final Controls controls, final double seconds, final List<Event> events) {
        for (long step = Math.round(seconds / Clock.STEP); step > 0; step--) {
            final Play.Stepped stepped = play.step(controls, Clock.STEP);
            play = stepped.play();
            events.addAll(stepped.events());
        }
        return play;
    }

    private static Play once(final Play play, final Controls controls, final List<Event> events) {
        final Play.Stepped stepped = play.step(controls, Clock.STEP);
        events.addAll(stepped.events());
        return stepped.play();
    }

    private static Place where(final Play play) {
        return play.lifts().getFirst().place();
    }

    /** Where the avatar stands on the small platform, in the platform's own terms: across and along its deck. */
    private static Vec3 onDeck(final Play play) {
        final Place at = where(play);
        final Block frame = new Block(at.x(), at.z(), 1, 1, at.yaw(), at.height(), at.height());
        return frame.toLocal(play.avatar().body().position());
    }

    @Test
    void aDeckIsAsBigAsItIsMadeWithItsConsoleAtTheFront() {
        final Course.Deck deck = new Course.Deck(500, 320);
        assertEquals(deck, Course.Deck.of(deck.shape()));
        assertEquals(250, deck.slab().halfAcross());
        assertEquals(160, deck.slab().halfAlong());
        assertTrue(deck.console().along() > 0, "the console at the front");
        assertTrue(deck.pilot() < deck.console().along() - deck.console().halfAlong() - Body.RADIUS,
                "the pilot behind it, clear of it");
        assertTrue(deck.pilot() > -deck.depth() / 2 + Body.RADIUS, "and on the deck");
        assertThrows(IllegalArgumentException.class, () -> new Course.Deck(100, 300), "too narrow for the console");
        assertThrows(IllegalArgumentException.class, () -> new Course.Deck(300, 150), "too shallow for the pilot");
    }

    @Test
    void thereAreTwoPlatformsTheirOwnSizesEachInOpenAir() {
        final Play start = Play.start(false);
        final List<Lift> decks = start.lifts().stream().filter(l -> l.deck() != null).toList();
        assertEquals(List.of(Course.CARPET, Course.BARGE), decks.stream().map(Lift::deck).toList());
        for (final Lift lift : decks) {
            assertEquals(Course.LIFT_LOW, lift.place().height());
            assertFalse(lift.held());
            // what it carries: what rides on it, and the loose things on its deck (all of them are, on the barge)
            final List<Integer> aboard = new ArrayList<>(start.damage().riding(lift.piece()));
            if (lift.deck().equals(Course.BARGE)) {
                start.props().forEach(p -> aboard.add(p.piece()));
            }
            assertTrue(lift.clear(lift.place(), start.obstacles(), aboard), "nothing in the way where it stands");
            // and nothing over it all the way up, so it can go straight up from the start
            final Block column = new Block(lift.place().x(), lift.place().z(), lift.deck().width() / 2,
                    lift.deck().depth() / 2, lift.place().yaw(), 0, Course.LIFT_HIGH + Body.TALL);
            for (int i = 0; i < start.obstacles().blocks().size(); i++) {
                final int other = start.obstacles().pieces().get(i);
                if (other != lift.piece() && !aboard.contains(other)) {
                    assertFalse(column.overlaps(start.obstacles().blocks().get(i)), "in its column: " + i);
                }
            }
        }
    }

    @Test
    void theControlsAreTakenAtTheConsoleAndNowhereElse() {
        final List<Event> events = new ArrayList<>();
        final Play away = once(Play.start(false), TAKE, events);
        assertTrue(away.atControls().isEmpty());
        assertTrue(events.isEmpty(), "far from any console, nothing: " + events);
        final Play at = piloting(Play.start(false), 1);
        assertTrue(at.withinReach().isPresent(), "at the barge's console, its controls are in reach");
        final Play taken = once(at, TAKE, events);
        assertEquals(List.of(new Event.TookControls(at.lifts().get(1).piece())), events);
        assertTrue(taken.atControls().isPresent());
        assertTrue(taken.withinReach().isEmpty(), "and with them taken, nothing more to take");
    }

    @Test
    void aheadItFliesTheWayItHeadsCarryingThePilot() {
        final List<Event> events = new ArrayList<>();
        Play play = flying(events);
        final Place from = where(play);
        final Vec3 stood = onDeck(play);
        play = run(play, AHEAD, 3, events);
        final Place to = where(play);
        final Vec3 went = new Vec3(to.x() - from.x(), 0, to.z() - from.z());
        assertTrue(went.magnitude() > 1000, "some way: " + went.magnitude());
        assertEquals(1, went.normalize().dotProduct(Num.heading(from.yaw())), 1e-9, "straight ahead");
        assertEquals(from.height(), to.height(), "at the same height");
        final Vec3 stands = onDeck(play);
        assertEquals(stood.x(), stands.x(), 1e-6, "the pilot where it stood on the deck");
        assertEquals(stood.z(), stands.z(), 1e-6);
        assertEquals(to.height(), play.avatar().body().height(), 1e-6);
        assertTrue(play.atControls().isPresent());
    }

    @Test
    void theMouseTurnsItAndThePilotTurnsWithIt() {
        final List<Event> events = new ArrayList<>();
        Play play = flying(events);
        final double yaw = where(play).yaw();
        final Vec3 stood = onDeck(play);
        play = run(play, new Controls(0, 0, 0.5, 0, true, false, 0), 1.5, events);
        assertEquals(Num.wrap(yaw + 90), where(play).yaw(), 1e-9, "a quarter turn");
        assertEquals(Num.wrap(yaw + 90), play.avatar().body().yaw(), 1e-9, "the pilot facing the way it heads still");
        final Vec3 stands = onDeck(play);
        assertEquals(stood.x(), stands.x(), 1e-6, "and where it stood on the deck");
        assertEquals(stood.z(), stands.z(), 1e-6);
    }

    @Test
    void itClimbsAndDescendsBetweenItsLowestAndHighest() {
        final List<Event> events = new ArrayList<>();
        Play play = flying(events);
        play = run(play, DOWN, 1, events);
        assertEquals(Course.LIFT_LOW, where(play).height(), "no lower than its lowest");
        play = run(play, UP, 3, events);
        assertTrue(where(play).height() > Course.LIFT_LOW + 700, "up: " + where(play).height());
        assertEquals(where(play).height(), play.avatar().body().height(), 1e-6, "the pilot up with it");
        assertTrue(events.stream().noneMatch(e -> e instanceof Event.Jumped), "space flew it, not the avatar");
        play = run(play, UP, 20, events);
        assertEquals(Course.LIFT_HIGH, where(play).height(), "and no higher than its highest");
        assertEquals(0, play.lifts().getFirst().climb());
    }

    @Test
    void itStopsShortOfWhatIsInItsWay() {
        // a tall pillar, and the small platform set down a few metres before it, heading straight at it
        final Play start = Play.start(false);
        final Course.Piece pillar = new Course().pieces().stream()
                .filter(p -> p.kind() == Course.Kind.PILLAR && p.block().top() > 1500).findFirst().orElseThrow();
        final Block p = pillar.block();
        final Vec3 off = Num.heading(37).multiply(p.halfWidth() + Course.CARPET.depth() / 2 + 600);
        final Place facing = new Place(p.x() + off.x(), p.z() + off.z(), 400, Num.wrap(37 + 180));
        final List<Lift> lifts = new ArrayList<>(start.lifts());
        lifts.set(0, new Lift(lifts.getFirst().piece(), Course.CARPET, facing));
        final List<Event> events = new ArrayList<>();
        Play play = once(piloting(new Play(start.avatar(), start.damage(), start.sky(), lifts), 0), TAKE, events);
        assertTrue(play.atControls().isPresent());
        play = run(play, AHEAD, 4, events);
        final Lift lift = play.lifts().getFirst();
        final Block room = lift.deck().room().placed(lift.place());
        assertFalse(room.overlaps(p), "it never goes into the pillar");
        final double was = Math.hypot(facing.x() - p.x(), facing.z() - p.z());
        final double is = Math.hypot(lift.place().x() - p.x(), lift.place().z() - p.z());
        assertTrue(is < was - 400, "but it came right up to it: " + was + " to " + is);
        assertEquals(0, lift.ahead(), "and stopped there");
        assertEquals(lift.place().height(), play.avatar().body().height(), 1e-6, "the pilot still aboard");
    }

    @Test
    void atTheControlsThePilotCanStillShoot() {
        final List<Event> events = new ArrayList<>();
        Play play = flying(events);
        play = once(play, Controls.doing(new Action.Draw(Weapon.RIFLES)), events);
        events.clear();
        run(play, new Controls(1, 0, 0, 0, true, true, 0), 0.5, events);
        assertTrue(events.stream().anyMatch(e -> e instanceof Event.Fired), "firing as it flies");
    }

    @Test
    void letGoItComesToAStopAndHangsThereAndTheKeysAreTheAvatarsAgain() {
        final List<Event> events = new ArrayList<>();
        Play play = flying(events);
        play = run(play, UP, 1.5, events);
        play = run(play, AHEAD, 1, events);
        events.clear();
        play = once(play, TAKE, events);
        assertEquals(List.of(new Event.LetGo(play.lifts().getFirst().piece())), events);
        play = run(play, Controls.NONE, 4, events);
        final Place hanging = where(play);
        play = run(play, Controls.NONE, 2, events);
        assertEquals(hanging, where(play), "stopped, and hanging there");
        assertEquals(hanging.height(), play.avatar().body().height(), 1e-6);
        events.clear();
        once(play, Controls.doing(Action.JUMP), events);
        assertTrue(events.stream().anyMatch(e -> e instanceof Event.Jumped), "and space jumps again");
    }

    @Test
    void noLongerStandingAtTheConsoleTheControlsAreLetGo() {
        final List<Event> events = new ArrayList<>();
        final Play taken = flying(events);
        final Play elsewhere = new Play(new Avatar(Body.standing(Vec3.ZERO, 0), false), taken.damage(), taken.sky(),
                taken.lifts());
        events.clear();
        final Play after = once(elsewhere, AHEAD, events);
        assertEquals(List.of(new Event.LetGo(taken.lifts().getFirst().piece())), events);
        assertTrue(after.atControls().isEmpty());
        assertEquals(where(taken), where(after), "and with no one at them, it does not go");
    }

    @Test
    void twoTurnedBoxesOverlapOnlyWhereTheyDo() {
        final Block a = new Block(0, 0, 100, 100, 0, 0, 100);
        // turned a quarter round on edge, its corner toward a: 141 out from its middle
        assertTrue(a.overlaps(new Block(230, 0, 100, 100, 45, 0, 100)), "a turned corner reaching in");
        assertFalse(a.overlaps(new Block(250, 0, 100, 100, 45, 0, 100)), "and just short");
        assertFalse(a.overlaps(new Block(150, 0, 100, 100, 45, 100, 200)), "over it, not in it");
        assertTrue(a.overlaps(new Block(0, 0, 10, 10, 30, 40, 60)), "inside it");
    }

    @Test
    void whatMovesIsDrawnPartWayBetweenTwoSteps() {
        final Block before = new Block(0, 0, 10, 10, 170, 100, 200);
        final Block after = new Block(40, -20, 10, 10, -170, 300, 400);
        final Block half = after.between(before, 0.5);
        assertEquals(20, half.x(), 1e-9);
        assertEquals(-10, half.z(), 1e-9);
        assertEquals(200, half.bottom(), 1e-9);
        assertEquals(300, half.top(), 1e-9);
        assertEquals(180, Math.abs(half.yaw()), 1e-9, "the short way round");
        assertEquals(after, after.between(before, 1));
    }
}
