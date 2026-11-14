package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The boxer with an ice cream cart for its sidecar: the same frame and wheel as its sidecar, a
 * freezer on it in place of the tub; heavier, slower, harder to tip toward it (the cart is ballast)
 * but still tipped held over toward it; an outfit of its own in the world.
 */
class IceCreamCartTest {

    private static final Course.Chassis CART = Course.BOXER_ICE_CREAM;
    private static final Course.Chassis TUB = Course.BOXER_SIDECAR;
    private static final Controls TAKE = Controls.doing(Action.TAKE);

    @Test
    void itIsAnOutfitOfItsOwnOnTheSidecarsFrame() {
        assertEquals(CART, Course.Chassis.of(CART.shape()), "told from the tub by its cart");
        assertEquals(TUB, Course.Chassis.of(TUB.shape()));
        assertEquals(Sidecar.Body.ICE_CREAM_CART, CART.sidecar().body());
        assertEquals(Sidecar.Body.TUB, TUB.sidecar().body());
        assertEquals(TUB.sidecar().frame(), CART.sidecar().frame(), "the same tubes");
        assertEquals(TUB.sidecar().hub(), CART.sidecar().hub(), "and wheel");
        assertFalse(CART.leans());
        assertTrue(CART.door().across() < 0, "got onto away from the cart");
        assertEquals(Frames.BOXER, Frames.of(CART));
    }

    @Test
    void itIsHeavierAndSlowerThanWithItsTub() {
        assertTrue(CART.drive().mass() > TUB.drive().mass());
        assertTrue(CART.drive().top() < TUB.drive().top());
        assertEquals(CART.drive().mass(), Outfit.of(CART).mass(), 1, "its drive as heavy as its outfit");
    }

    @Test
    void theCartIsBallastItTakesMoreToTipTowardIt() {
        // on the right: turning toward it, the pull is to the left
        final double cart = Outfit.of(CART).limit(-1);
        final double tub = Outfit.of(TUB).limit(-1);
        assertTrue(cart > tub, cart / Outfit.GRAVITY + " g against " + tub / Outfit.GRAVITY + " g");
        assertTrue(Outfit.of(CART).limit(1) < Outfit.of(TUB).limit(1), "and less, away from it");
        final double v = 40 * 100 / 3.6;
        assertTrue(Outfit.lookUp(Outfit.table(CART), v, true, 20) > Outfit.lookUp(Outfit.table(TUB), v, true, 20),
                "steered the sharper toward it at 40 km/h");
    }

    @Test
    void itsCartPersistsInItsText() {
        final Sidecar cart = CART.sidecar();
        assertEquals(cart, Sidecar.read(cart.text()));
        assertTrue(cart.text().contains("body ice_cream_cart"));
        assertEquals(Sidecar.Body.TUB, Sidecar.read(cart.text().replace("body ice_cream_cart\n", "")).body(),
                "a tub, when it says nothing");
    }

    @Test
    void itStandsInTheRoadsDemoThePlaygroundAndPitRoad() {
        for (final Demo demo : new Demo[] {Demo.FLYOVER, Demo.PLAYGROUND}) {
            assertTrue(Play.start(demo, false).lifts().stream().anyMatch(l -> l.craft() == CART), demo.name());
        }
        assertNotEquals(-1, Course.CHASSES.indexOf(CART));
    }

    private static Play run(Play play, final Controls controls, final double seconds) {
        for (long step = Math.round(seconds / Clock.STEP); step > 0; step--) {
            play = play.step(controls, Clock.STEP).play();
        }
        return play;
    }

    @Test
    void heldTowardItsCartAtSpeedItStillGoesOver() {
        final Play start = Play.start(Demo.FLYOVER, false);
        int k = -1;
        for (int i = 0; i < start.lifts().size(); i++) {
            if (start.lifts().get(i).craft() == CART) {
                k = i;
            }
        }
        final Vec3 door = start.lifts().get(k).at(CART.door());
        Play play = new Play(new Avatar(Body.standing(new Vec3(door.x(), 0, door.z()), 0), false), start.damage(),
                start.sky(), start.lifts(), start.people(), start.props(), start.turrets());
        play = run(play, TAKE, Clock.STEP);
        assertTrue(play.lifts().get(k).held(), "got on");
        final Lift was = play.lifts().get(k);
        final List<Lift> lifts = new ArrayList<>(play.lifts());
        lifts.set(k, new Lift(was.piece(), CART, was.place(), 6000 / 3.6, 0, 0, true, true, 0, 0, 0, 0));
        play = new Play(play.avatar(), play.damage(), play.sky(), lifts, play.people(), play.props(), play.turrets());
        // straight, it holds
        play = run(play, new Controls(1, 0, 0, 0, true), 1);
        assertFalse(play.lifts().get(k).over());
        // held toward the cart, over within three seconds
        for (int step = 0; step < 360 && !play.lifts().get(k).over(); step++) {
            play = run(play, new Controls(1, 1, 0, 0, true), Clock.STEP);
        }
        assertTrue(play.lifts().get(k).over());
        play = run(play, Controls.NONE, Clock.STEP);
        assertFalse(play.lifts().get(k).held(), "its rider thrown");
    }
}
