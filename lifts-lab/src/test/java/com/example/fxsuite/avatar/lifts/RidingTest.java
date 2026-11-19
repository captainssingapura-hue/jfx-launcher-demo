package com.example.fxsuite.avatar.lifts;

import com.example.fxsuite.avatar.bench.kit.Played;
import com.example.fxsuite.avatar.bench.kit.Spot;
import com.example.fxsuite.avatar.sim.Clock;
import com.example.fxsuite.avatar.sim.Controls;
import com.example.fxsuite.avatar.sim.Play;
import com.example.fxsuite.avatar.sim.Place;
import com.example.fxsuite.avatar.sim.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The lifts ridden, in the simulation: the avatar stood in a paternoster's cabin goes up with it; stood in the shuttle
 * lift's car, it is at the next floor when the car is; stood on an escalator's step, it goes up and along with it.
 */
class RidingTest {

    private final YardScene yard = new YardScene();

    /** The avatar stood on {@code spot} in the yard, the world stepped {@code seconds}: where it is then. */
    private Vec3 ridden(final Spot spot, final double seconds) {
        Play play = Play.start(new Played(new SceneWorld(yard.built()), new Played.Guests(List.of(), 0, false, spot)),
                false);
        for (int n = 0; n < seconds / Clock.STEP; n++) {
            play = play.step(Controls.NONE, Clock.STEP).play();
        }
        return play.avatar().body().position();
    }

    @Test
    void stoodInAPaternostersCabinItGoesUpWithIt() {
        final Paternoster p = yard.paternoster;
        // the second cabin, its floor just over the ground on its way up
        final Place cabin = p.cabin(1).at(0);
        final double floor = cabin.height() + Paternoster.FLOOR;
        final Vec3 at = ridden(new Spot("in a cabin", new Place(cabin.x(), cabin.z(), floor, 0)), 10);
        assertEquals(floor + p.speed() * 10, -at.y(), 3, "ten seconds up");
        assertEquals(cabin.x(), at.x(), 3, "in its cabin still");
    }

    @Test
    void stoodInTheShuttleLiftsCarItIsAtTheNextFloorWhenTheCarIs() {
        final ShuttleLift s = yard.shuttle;
        // the car at the foot, its doors opening; the next stop, the first floor, once it has stood and gone a floor
        final double seconds = s.dwell() + s.travel(400) + 2;
        final Vec3 at = ridden(new Spot("in the car", new Place(s.x(), s.z(), 0, 180)), seconds);
        assertEquals(1, s.at(seconds).at(), "the car standing at the first floor");
        assertEquals(400, -at.y(), 3, "on its floor, at the first floor");
    }

    @Test
    void stoodOnAnEscalatorsStepItGoesUpAndAlongWithIt() {
        final Escalator e = yard.escalator;
        // a step on its incline, a third of the way up
        final double s = e.flat() + e.run() / 3;
        final int k = (int) Math.round(s / (2 * e.length() / e.steps()));
        final Place step = e.step(k).at(0);
        final double top = step.height() + e.riser();
        final Vec3 at = ridden(new Spot("on a step", new Place(step.x(), step.z(), top, 0)), 3);
        final Place then = e.step(k).at(3);
        assertEquals(then.height() + e.riser(), -at.y(), 6, "up with its step");
        assertEquals(then.z(), at.z(), 20, "along with its step");
    }
}
