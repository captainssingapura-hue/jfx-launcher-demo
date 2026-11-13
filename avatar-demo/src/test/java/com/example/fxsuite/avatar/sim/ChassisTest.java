package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A bare chassis, driven: only its front wheels steer, so it turns about its back wheels; it
 * speeds up, brakes and reverses; it drives up and down ramps on its wheels, tipped as they slope,
 * leaps off a ramp's head, and stops dead against what stands in its way.
 */
class ChassisTest {

    private static final Controls TAKE = Controls.doing(Action.TAKE);
    private static final Controls GO = new Controls(1, 0, 0, 0, true, false, 0);

    private static Play run(Play play, final Controls controls, final double seconds, final List<Event> events) {
        for (long step = Math.round(seconds / Clock.STEP); step > 0; step--) {
            final Play.Stepped stepped = play.step(controls, Clock.STEP);
            play = stepped.play();
            events.addAll(stepped.events());
        }
        return play;
    }

    private static Lift car(final Play play) {
        return play.lifts().stream().filter(l -> l.craft() == Course.CAR_CHASSIS).findFirst().orElseThrow();
    }

    /** The roads demo, the car put at {@code place}, and the avatar at its wheel, driving. */
    private static Play driving(final Place place) {
        final Play start = Play.start(Demo.FLYOVER, false);
        final List<Lift> lifts = new ArrayList<>(start.lifts());
        final int k = lifts.indexOf(car(start));
        final Lift put = new Lift(lifts.get(k).piece(), Course.CAR_CHASSIS, place);
        lifts.set(k, put);
        final Play at = new Play(new Avatar(Body.standing(put.pilot(), place.yaw()), false), start.damage(),
                start.sky(), lifts, start.people(), start.props(), start.turrets());
        assertTrue(at.withinReach().isPresent(), "at its wheel");
        return run(at, TAKE, Clock.STEP, new ArrayList<>());
    }

    /** On open ground far out, beyond the pillars, its wheels on it, facing +z. */
    private static Place open() {
        return new Place(-9000, -6000, Course.CAR_CHASSIS.ride(), 0);
    }

    @Test
    void theRoadsDemoHasACarAndATruck() {
        final Play start = Play.start(Demo.FLYOVER, false);
        assertTrue(start.lifts().stream().anyMatch(l -> l.craft() == Course.CAR_CHASSIS));
        assertTrue(start.lifts().stream().anyMatch(l -> l.craft() == Course.TRUCK_CHASSIS));
        assertEquals(4, Course.CAR_CHASSIS.wheels().size());
        assertEquals(6, Course.TRUCK_CHASSIS.wheels().size(), "the back four on two axles together");
        assertEquals(240 + 215, Course.TRUCK_CHASSIS.wheelbase(), 1e-9, "to between its back axles");
        // standing on its wheels, its bed as high as it rides
        for (final Lift lift : start.lifts()) {
            if (lift.craft() instanceof Course.Chassis chassis) {
                assertEquals(chassis.ride(), lift.place().height(), 1e-9);
            }
        }
    }

    @Test
    void itSpeedsUpToItsTopSpeedCarryingItsDriver() {
        final List<Event> events = new ArrayList<>();
        Play play = driving(open());
        assertTrue(car(play).held());
        play = run(play, GO, 4, events);
        assertEquals(Course.CAR_CHASSIS.drive().top(), car(play).ahead(), 1e-9, "flat out");
        assertTrue(car(play).place().z() > -6000 + 4000, "a long way ahead");
        assertEquals(0, car(play).place().x() + 9000, 1e-6, "straight on, unsteered");
        assertEquals(0, play.avatar().body().position().distance(car(play).pilot()), 1, "its driver with it");
    }

    @Test
    void sBrakesThenReverses() {
        final List<Event> events = new ArrayList<>();
        Play play = driving(open());
        play = run(play, GO, 1, events);
        final Controls back = new Controls(-1, 0, 0, 0, true, false, 0);
        play = run(play, back, 0.3, events);
        assertTrue(car(play).ahead() > 0, "braking");
        play = run(play, back, 3, events);
        assertEquals(-Course.CAR_CHASSIS.drive().reverse(), car(play).ahead(), 1e-9, "then in reverse");
    }

    @Test
    void onlyItsFrontWheelsSteerSoItTurnsAboutItsBackWheels() {
        final List<Event> events = new ArrayList<>();
        Play play = driving(open());
        play = run(play, new Controls(0.3, 0, 0, 0, true, false, 0), 1, events);
        // D turns it right, its yaw growing; its back wheels going only the way it faces, never sideways
        final Controls right = new Controls(0.3, 1, 0, 0, true, false, 0);
        double turned = 0;
        for (int i = 0; i < 240; i++) {
            final Lift before = car(play);
            play = run(play, right, Clock.STEP, events);
            final Lift after = car(play);
            turned += Num.wrap(after.place().yaw() - before.place().yaw());
            final Vec3 rearBefore = before.at(new Course.Section(0, Course.CAR_CHASSIS.rear(), 0, 0, 0, 0));
            final Vec3 rearAfter = after.at(new Course.Section(0, Course.CAR_CHASSIS.rear(), 0, 0, 0, 0));
            final Vec3 slid = rearAfter.subtract(rearBefore);
            final double sideways = slid.x() * Num.right(after.place().yaw()).x()
                    + slid.z() * Num.right(after.place().yaw()).z();
            assertEquals(0, sideways, 0.05, "the back wheels never slide sideways");
        }
        assertTrue(turned > 30, "turned right: " + turned);
        assertTrue(car(play).steer() > 0);
        // let go of D, its wheels come straight again
        play = run(play, new Controls(0.3, 0, 0, 0, true, false, 0), 1, events);
        assertEquals(0, car(play).steer(), 1e-9);
    }

    @Test
    void theMouseDoesNotTurnIt() {
        final List<Event> events = new ArrayList<>();
        Play play = driving(open());
        play = run(play, new Controls(1, 0, 3, 0, true, false, 0), 1, events);
        assertEquals(0, car(play).place().yaw(), 1e-9);
    }

    @Test
    void itDrivesUpOverAndDownTheFlyoverTippedAsItSlopes() {
        final double foot = Course.DECK_LENGTH / 2 + Course.RAMP_LENGTH;
        final List<Event> events = new ArrayList<>();
        Play play = driving(new Place(-foot - 300, Course.FLYOVER_Z, Course.CAR_CHASSIS.ride(), 90));
        double highest = 0;
        double steepest = 0;
        for (int i = 0; i < 120 * 12 && car(play).place().x() < foot + 600; i++) {
            play = run(play, new Controls(1, 0, 0, 0, false, false, 0), Clock.STEP, events);
            highest = Math.max(highest, car(play).place().height());
            steepest = Math.max(steepest, car(play).tilt());
        }
        assertEquals(Course.FLYOVER_HEIGHT + Course.CAR_CHASSIS.ride(), highest, 1, "over the deck");
        assertEquals(Math.toDegrees(Math.atan(0.25)), steepest, 0.5, "tipped up as the ramp is, one in four");
        assertTrue(car(play).place().x() > foot + 600, "and down the other side");
        assertEquals(Course.CAR_CHASSIS.ride(), car(play).place().height(), 1);
        assertEquals(0, car(play).tilt(), 0.5);
    }

    @Test
    void offTheHeadOfTheLoneRampItLeaps() {
        final List<Event> events = new ArrayList<>();
        Play play = driving(new Place(0, 2200, Course.CAR_CHASSIS.ride(), 0));
        boolean airborne = false;
        for (int i = 0; i < 120 * 4; i++) {
            play = run(play, GO, Clock.STEP, events);
            final Lift car = car(play);
            airborne |= car.place().z() > 3450 + 300 && car.place().height() > Course.CAR_CHASSIS.ride() + 50;
        }
        assertTrue(airborne, "flying past the ramp's head");
        assertEquals(Course.CAR_CHASSIS.ride(), car(play).place().height(), 1e-6, "and down again on its wheels");
    }

    @Test
    void itStopsDeadAgainstARampsSide() {
        final List<Event> events = new ArrayList<>();
        // beside the flyover's west ramp, near its head, heading straight at its side
        final double x = -Course.DECK_LENGTH / 2 - 300;
        Play play = driving(new Place(x, Course.FLYOVER_Z - 900, Course.CAR_CHASSIS.ride(), 0));
        play = run(play, GO, 3, events);
        assertTrue(car(play).place().z() < Course.FLYOVER_Z - Course.ROAD_WIDTH / 2, "stopped short of it");
        assertEquals(0, car(play).ahead(), 1e-9);
        assertEquals(Course.CAR_CHASSIS.ride(), car(play).place().height(), 1e-6);
    }

    @Test
    void letGoItRollsToAStop() {
        final List<Event> events = new ArrayList<>();
        Play play = driving(open());
        play = run(play, GO, 2, events);
        play = run(play, TAKE, Clock.STEP, events);
        assertFalse(car(play).held());
        play = run(play, Controls.NONE, 8, events);
        assertEquals(0, car(play).ahead(), 1e-9);
    }

    @Test
    void theCarHasAGunFixedBesideTheSeatAndTheTruckNone() {
        final Play start = Play.start(Demo.FLYOVER, false);
        assertEquals(1, start.turrets().size());
        final Turret gun = start.turrets().getFirst();
        assertEquals(car(start).piece(), gun.host());
        assertTrue(gun.mount().fixed());
        assertEquals(Weapon.HEAVY_GATLING, gun.mount().gun().by());
        assertTrue(gun.mount().pivot().across() > Course.CAR_CHASSIS.driverAcross(), "to the right of the seat");
        assertFalse(gun.reaches(Body.standing(car(start).pilot(), 0)), "not manned on its own");
    }

    @Test
    void theDriverFiresItTheWayTheCarFacesAndItTurnsWithTheCar() {
        final List<Event> events = new ArrayList<>();
        Play play = driving(open());
        // turned a little to the right, still going
        play = run(play, new Controls(0.4, 1, 0, 0, true, false, 0), 1, events);
        play = run(play, new Controls(0.4, 0, 0, 0, true, false, 0), 1, events);
        // and stopped, on the handbrake
        play = run(play, new Controls(0, 0, 0, 1, true, false, 0), 2, events);
        assertEquals(0, car(play).ahead(), 1e-9);
        final double yaw = car(play).place().yaw();
        assertTrue(yaw > 10, "turned: " + yaw);
        events.clear();
        play = run(play, new Controls(0, 0, 0, 0, true, true, 0), 2, events);
        final List<Event.Fired> fired = events.stream().filter(e -> e instanceof Event.Fired)
                .map(e -> (Event.Fired) e).toList();
        assertFalse(fired.isEmpty(), "fired once spun up");
        final Vec3 heading = Num.heading(car(play).place().yaw());
        for (final Event.Fired f : fired) {
            assertEquals(Weapon.HEAVY_GATLING, f.by());
            final Vec3 way = f.to().subtract(f.from()).normalize();
            assertTrue(way.x() * heading.x() + way.z() * heading.z() > 0.999, "along the way the car faces");
            final Vec3 off = f.from().subtract(car(play).at(new Course.Section(0, 0, 0, 0, 0, 0)));
            assertTrue(off.x() * heading.x() + off.z() * heading.z() > Course.CAR_CHASSIS.length() / 2,
                    "from out over its front");
        }
        assertEquals(1, play.damage().left(car(play).piece()), 1e-12, "never into itself");
    }

    @Test
    void theHandsDoNotFireWhileTheCarsGunIsThere() {
        final List<Event> events = new ArrayList<>();
        Play play = driving(open());
        play = run(play, Controls.doing(new Action.Draw(Weapon.RIFLES)), Clock.STEP, events);
        assertEquals(Weapon.RIFLES, play.avatar().holding());
        events.clear();
        play = run(play, new Controls(0, 0, 0, 0, true, true, 0), 2, events);
        assertTrue(events.stream().noneMatch(e -> e instanceof Event.Fired f && f.by() == Weapon.RIFLES));
        assertTrue(events.stream().anyMatch(e -> e instanceof Event.Fired f && f.by() == Weapon.HEAVY_GATLING));
    }

    @Test
    void upARampItsGunIsTippedWithIt() {
        final double foot = Course.DECK_LENGTH / 2 + Course.RAMP_LENGTH;
        final List<Event> events = new ArrayList<>();
        Play play = driving(new Place(-foot - 300, Course.FLYOVER_Z, Course.CAR_CHASSIS.ride(), 90));
        for (int i = 0; i < 120 * 4 && car(play).tilt() < 13; i++) {
            play = run(play, new Controls(1, 0, 0, 0, false, false, 0), Clock.STEP, events);
        }
        assertTrue(car(play).tilt() > 13);
        play = run(play, new Controls(0.2, 0, 0, 0, false, false, 0), Clock.STEP, events);
        assertEquals(car(play).tilt(), play.turrets().getFirst().pitch(), 1e-9);
    }

    @Test
    void noOneAtTheWheelNoOneFiresIt() {
        final List<Event> events = new ArrayList<>();
        final Play start = Play.start(Demo.FLYOVER, false);
        run(start, new Controls(0, 0, 0, 0, true, true, 0), 2, events);
        assertTrue(events.stream().noneMatch(e -> e instanceof Event.Fired));
    }
}
