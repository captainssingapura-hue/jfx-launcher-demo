package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lifts resting on lifts resting on lifts: each goes where the one under it goes, however deep the
 * stack, the one under it moved first.
 */
class StackTest {

    /** {@code lift}, its engine off, set down on whatever is under it at {@code x}, {@code z}. */
    private static Play setDown(final Play play, final int which, final double x, final double z) {
        final List<Lift> lifts = new ArrayList<>(play.lifts());
        final Lift lift = lifts.get(which);
        final Lift high = with(lift, new Place(x, z, Course.LIFT_HIGH + 2000, lift.place().yaw()));
        lifts.set(which, high);
        final Play over = new Play(play.avatar(), play.damage(), play.sky(), lifts, play.people(), play.props(),
                play.turrets());
        final Lift.Support under = over.under(high);
        lifts.set(which, with(lift, new Place(x, z, under.top() + high.below(), lift.place().yaw())));
        return new Play(play.avatar(), play.damage(), play.sky(), lifts, play.people(), play.props(), play.turrets());
    }

    private static Lift with(final Lift lift, final Place place) {
        return new Lift(lift.piece(), lift.craft(), place, 0, 0, 0, false, false, lift.bombs(), lift.ready(),
                lift.steer(), lift.tilt());
    }

    private static int index(final Play play, final Course.Deck deck) {
        for (int i = 0; i < play.lifts().size(); i++) {
            if (deck.equals(play.lifts().get(i).deck())) {
                return i;
            }
        }
        throw new AssertionError(deck);
    }

    @Test
    void aStackStaysStacked() {
        Play play = Play.start(false);
        final int barge = index(play, Course.BARGE);
        final int carpet = index(play, Course.CARPET);
        final Place b = play.lifts().get(barge).place();
        // the carpet down on a clear part of the barge, away from the stall
        Play stacked = null;
        for (final double[] off : new double[][] {{320, 0}, {-320, 0}, {0, 250}, {0, -250}}) {
            final Play tried = setDown(play, carpet, b.x() + off[0], b.z() + off[1]);
            if (liftUnder(tried, carpet) == barge) {
                stacked = tried;
                break;
            }
        }
        assertTrue(stacked != null, "somewhere clear on the barge");
        play = stacked;
        assertEquals(barge, liftUnder(play, carpet), "the carpet on the barge");
        final List<Place> was = play.lifts().stream().map(Lift::place).toList();
        for (int step = 0; step < 120; step++) {
            play = play.step(Controls.NONE, Clock.STEP).play();
        }
        // the barge sits where it is, and so does what is stacked on it
        for (final int i : List.of(barge, carpet)) {
            assertEquals(was.get(i).height(), play.lifts().get(i).place().height(), 1, "lift " + i);
        }
        assertEquals(barge, liftUnder(play, carpet), "still");
        assertTrue(play.lifts().get(carpet).resting(play.under(play.lifts().get(carpet))));
    }

    @Test
    void eachLiftMovesAfterTheOneItRestsOnHoweverDeep() {
        // 0 on 2, 2 on 3, 3 on 1, 1 on nothing
        assertEquals(List.of(1, 3, 2, 0), Play.underFirst(new int[] {2, -1, 3, 1}));
    }

    @Test
    void liftsRestingOnEachOtherInARingStillAllMove() {
        // 0 and 1 on each other, 2 on 0: the first of the ring moves on its own, then the rest
        final int[] onLift = {1, 0, 0};
        assertEquals(List.of(0, 1, 2), Play.underFirst(onLift));
        assertEquals(-1, onLift[0]);
    }

    /** Which lift {@code which} rests on, by its place in the lifts; -1 if none. */
    private static int liftUnder(final Play play, final int which) {
        final Lift.Support under = play.under(play.lifts().get(which));
        if (under.piece() < 0) {
            return -1;
        }
        final Course.Piece piece = play.damage().piece(under.piece());
        final int carrier = piece.rides() ? piece.carrier() : under.piece();
        for (int i = 0; i < play.lifts().size(); i++) {
            if (play.lifts().get(i).piece() == carrier) {
                return i;
            }
        }
        return -1;
    }
}
