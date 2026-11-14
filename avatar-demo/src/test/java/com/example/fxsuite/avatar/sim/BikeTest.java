package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two-wheelers: a scooter, a sport bike and a cruiser, each on two wheels in line under a
 * narrow frame; got onto from beside them, ridden as a car is driven, and got off again; and
 * leaning into their turns, the harder the faster they go round, upright going straight.
 */
class BikeTest {

    private static final Controls TAKE = Controls.doing(Action.TAKE);

    private static Play run(Play play, final Controls controls, final double seconds) {
        for (long step = Math.round(seconds / Clock.STEP); step > 0; step--) {
            play = play.step(controls, Clock.STEP).play();
        }
        return play;
    }

    private static Lift bike(final Play play, final Course.Chassis chassis) {
        return play.lifts().stream().filter(l -> l.craft() == chassis).findFirst().orElseThrow();
    }

    @Test
    void eachRidesOnTwoWheelsInLineUnderItsHandlebars() {
        for (final Course.Chassis bike : Course.BIKES) {
            assertTrue(bike.twoWheels());
            final List<Course.Section> wheels = bike.wheels();
            assertEquals(2, wheels.size(), bike.name());
            for (final Course.Section wheel : wheels) {
                assertEquals(0, wheel.across(), 1e-9, "in line");
            }
            assertEquals(bike.width() / 2, bike.half(), 1e-9, "as wide as its handlebars");
            assertTrue(bike.bars() < Course.Chassis.WHEEL_HEIGHT, "its bars lower than a steering wheel");
            assertEquals(bike, Course.Chassis.of(bike.shape()), "known by its frame");
        }
        assertThrows(IllegalArgumentException.class, () -> new Course.Chassis("trike", 70, 200, 40, 20, 10,
                List.of(70.0, 0.0, -70.0), 0, -20, Course.SCOOTER.drive(), true), "two wheels, no more");
        assertFalse(Course.CAR_CHASSIS.twoWheels());
    }

    @Test
    void gotOnFromBesideItRiddenOffAndGotOffAgain() {
        for (final Course.Chassis chassis : Course.BIKES) {
            final Play start = Play.start(Demo.FLYOVER, false);
            final Lift parked = bike(start, chassis);
            final Vec3 beside = parked.at(chassis.door());
            Play play = new Play(new Avatar(Body.standing(beside, 0), false), start.damage(), start.sky(),
                    start.lifts(), start.people(), start.props(), start.turrets());
            assertTrue(play.withinReach().isPresent(), chassis.name() + ": got onto from beside it");
            play = run(play, TAKE, Clock.STEP);
            assertTrue(bike(play, chassis).held(), chassis.name() + " taken");
            play = run(play, new Controls(1, 0, 0, 0, true, false, 0), 1.5);
            final Lift riding = bike(play, chassis);
            assertTrue(riding.place().z() - parked.place().z() > 100, chassis.name() + " ridden off: " + riding.place());
            play = run(play, Controls.NONE, 4);
            play = run(play, TAKE, Clock.STEP);
            assertFalse(bike(play, chassis).held(), chassis.name() + " got off");
            assertFalse(play.hidden());
        }
    }

    @Test
    void itLeansIntoItsTurnsTheHarderTheFasterItGoesRoundUprightGoingStraight() {
        final Course.Chassis chassis = Course.SPORT_BIKE;
        final Lift parked = bike(Play.start(Demo.FLYOVER, false), chassis);
        assertEquals(0, parked.lean(), 1e-12, "upright, stopped");
        final double slow = going(parked, 500, 10).lean();
        final double fast = going(parked, 1500, 10).lean();
        assertTrue(slow > 0 && fast > slow, "into a right turn, the more the faster: " + slow + ", " + fast);
        assertTrue(going(parked, 1500, -10).lean() < 0, "and into a left one, to the left");
        assertEquals(0, going(parked, 1500, 0).lean(), 1e-12, "upright, going straight");
        assertTrue(going(parked, 2800, 20).lean() <= Lift.MOST_LEAN, "never past its most");
        // and a car never leans
        final Lift car = Play.start(Demo.FLYOVER, false).lifts().stream()
                .filter(l -> l.craft() == Course.CAR_CHASSIS).findFirst().orElseThrow();
        assertEquals(0, going(car, 1500, 10).lean(), 1e-12);
    }

    /** {@code lift} going {@code speed} ahead, its front wheel turned {@code steer}. */
    private static Lift going(final Lift lift, final double speed, final double steer) {
        return new Lift(lift.piece(), lift.craft(), lift.place(), speed, 0, 0, true, true, 0, 0, steer, 0);
    }

    @Test
    void theRoadsAndThePlaygroundHaveAllThree() {
        for (final Demo demo : new Demo[] {Demo.FLYOVER, Demo.PLAYGROUND}) {
            final List<Course.Chassis> found = new ArrayList<>();
            for (final Lift lift : Play.start(demo, false).lifts()) {
                if (lift.craft() instanceof Course.Chassis chassis && chassis.leans()) {
                    found.add(chassis);
                }
            }
            assertEquals(Course.BIKES, found, demo.name());
        }
    }
}
