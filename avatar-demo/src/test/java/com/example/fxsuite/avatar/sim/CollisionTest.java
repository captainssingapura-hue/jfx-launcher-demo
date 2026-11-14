package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vehicles running into what gives: a crate is shoved along, slowly without harm, fast to
 * pieces; one vehicle into another shares their momentum by their masses, a car stopped by a
 * truck it hardly moves, a truck sending a car flying; a car hit side-on slides, and its tyres
 * scrub that off; and the avatar cannot shove a crate by hand.
 */
class CollisionTest {

    private static Play run(Play play, final Controls controls, final double seconds) {
        for (long step = Math.round(seconds / Clock.STEP); step > 0; step--) {
            play = play.step(controls, Clock.STEP).play();
        }
        return play;
    }

    private static Lift lift(final Play play, final int piece) {
        return play.lifts().stream().filter(l -> l.piece() == piece).findFirst().orElseThrow();
    }

    private static Prop prop(final Play play, final int piece) {
        return play.props().stream().filter(p -> p.piece() == piece).findFirst().orElseThrow();
    }

    /** {@code play} with the land vehicle {@code chassis} moved to {@code at}, going {@code ahead}, its wheels straight. */
    private static Play moved(final Play play, final Course.Chassis chassis, final Place at, final double ahead) {
        // the first of them: the roadster, not the van on the same chassis
        final List<Lift> lifts = new ArrayList<>();
        boolean done = false;
        for (final Lift lift : play.lifts()) {
            final boolean it = !done && lift.craft() == chassis;
            lifts.add(it ? new Lift(lift.piece(), chassis, at, ahead, 0, 0, lift.held(), true, 0, 0, 0, 0) : lift);
            done |= it;
        }
        return new Play(play.avatar(), play.damage(), play.sky(), lifts, play.people(), play.props(), play.turrets());
    }

    /** The Roads demo, its crate nearest the road, and {@code chassis} put {@code back} from it, heading at it, going {@code ahead}. */
    private record Setup(Play play, int crate, int vehicle) {
    }

    private static Setup atACrate(final Course.Chassis chassis, final double back, final double ahead) {
        final Play start = Play.start(Demo.FLYOVER, false);
        final Prop crate = start.props().stream().filter(p -> p.thing() instanceof Crate)
                .min(Comparator.comparingDouble(p -> Math.abs(p.place().x()))).orElseThrow();
        final double side = Math.signum(crate.place().x());
        final double reach = ((Crate) crate.thing()).length() / 2 + chassis.length() / 2 + back;
        final Place at = new Place(crate.place().x() - side * reach, crate.place().z(), chassis.ride(), side * 90);
        final Play placed = moved(start, chassis, at, ahead);
        final int vehicle = placed.lifts().stream().filter(l -> l.craft() == chassis).findFirst().orElseThrow().piece();
        return new Setup(placed, crate.piece(), vehicle);
    }

    @Test
    void aVehicleShovesACrateAlongAndSlowlyDoesItNoHarm() {
        final Setup setup = atACrate(Course.PICKUP_CHASSIS, 100, 300);
        final Place was = prop(setup.play(), setup.crate()).place();
        final Play after = run(setup.play(), Controls.NONE, 3);
        final Place now = prop(after, setup.crate()).place();
        final double shoved = Math.abs(now.x() - was.x());
        assertTrue(shoved > 30, "shoved along: " + shoved);
        assertEquals(was.z(), now.z(), 1, "straight on, the way it was hit");
        assertEquals(1, after.damage().left(setup.crate()), 1e-12, "a push: no harm");
        assertEquals(1, after.damage().left(setup.vehicle()), 1e-12);
    }

    @Test
    void hitFastACrateBreaksAndTheVehicleTakesSomeHarm() {
        final Setup setup = atACrate(Course.PICKUP_CHASSIS, 50, 1400);  // 50 km/h
        final Play after = run(setup.play(), Controls.NONE, 0.5);
        assertTrue(after.damage().broken(setup.crate()), "the crate in pieces");
        assertTrue(after.damage().left(setup.vehicle()) < 1, "the pickup dented");
        assertFalse(after.damage().broken(setup.vehicle()), "but not broken");
    }

    @Test
    void oneVehicleIntoAnotherSharesTheirMomentumByTheirMasses() {
        // the roadster at speed into the truck, parked: it stops, and the truck hardly moves; and the other way about
        final double light = Course.CAR_CHASSIS.drive().mass();
        final double heavy = Course.TRUCK_CHASSIS.drive().mass();
        for (final boolean carHits : new boolean[] {true, false}) {
            final Play start = Play.start(Demo.FLYOVER, false);
            final Course.Chassis mover = carHits ? Course.CAR_CHASSIS : Course.TRUCK_CHASSIS;
            final Course.Chassis parked = carHits ? Course.TRUCK_CHASSIS : Course.CAR_CHASSIS;
            // the parked one out in the open, behind the start, side-on; the mover heading straight at its side
            final Place standing = new Place(0, -2000, parked.ride(), 90);
            final double gap = mover.length() / 2 + parked.half() + 20;
            Play play = moved(moved(start, parked, standing, 0), mover, new Place(0, -2000 - gap, mover.ride(), 0), 1000);
            final int moverPiece = play.lifts().stream().filter(l -> l.craft() == mover).findFirst().orElseThrow().piece();
            final int parkedPiece = play.lifts().stream().filter(l -> l.craft() == parked).findFirst().orElseThrow().piece();
            final double before = mover.drive().mass() * 1000;
            Play after = play;
            for (int i = 0; i < 60 && lift(after, parkedPiece).across().magnitude() < 1; i++) {
                after = run(after, Controls.NONE, Clock.STEP);
            }
            final Vec3 struck = lift(after, parkedPiece).across();
            final double going = lift(after, moverPiece).across().z();
            final double momentum = mover.drive().mass() * going + parked.drive().mass() * struck.z();
            assertEquals(before, momentum, 0.03 * before, "momentum kept, as near as a step: " + mover.name());
            if (carHits) {
                assertTrue(struck.magnitude() < 300, "the truck hardly moved: " + struck);
                assertTrue(going < 300, "the car stopped short: " + going);
            } else {
                assertTrue(struck.magnitude() > 900, "the car sent off as fast as the truck came: " + struck);
                assertTrue(going > 600, "the truck hardly slowed: " + going);
            }
            assertTrue(light < heavy);
        }
    }

    @Test
    void hitSideOnACarSlidesAndItsTyresScrubThatOff() {
        final Play start = Play.start(Demo.FLYOVER, false);
        final Place standing = new Place(0, -2000, Course.CAR_CHASSIS.ride(), 90);
        final double gap = Course.PICKUP_CHASSIS.length() / 2 + Course.CAR_CHASSIS.half() + 20;
        Play play = moved(moved(start, Course.CAR_CHASSIS, standing, 0), Course.PICKUP_CHASSIS,
                new Place(0, -2000 - gap, Course.PICKUP_CHASSIS.ride(), 0), 800);
        final int car = play.lifts().stream().filter(l -> l.craft() == Course.CAR_CHASSIS).findFirst().orElseThrow().piece();
        double most = 0;
        for (int i = 0; i < 3 / Clock.STEP; i++) {
            play = run(play, Controls.NONE, Clock.STEP);
            most = Math.max(most, Math.abs(lift(play, car).aside()));
        }
        assertTrue(most > 300, "slid sideways: " + most);
        assertEquals(0, lift(play, car).aside(), 1e-9, "and its tyres stopped it");
        assertTrue(lift(play, car).place().z() > -2000 + 30, "shoved off where it stood: " + lift(play, car).place());
    }

    @Test
    void theAvatarCannotShoveACrateByHand() {
        final Play start = Play.start(Demo.FLYOVER, false);
        final Prop crate = start.props().stream().filter(p -> p.thing() instanceof Crate).findFirst().orElseThrow();
        final Crate box = (Crate) crate.thing();
        // up against its side, walking into it
        final Vec3 against = new Vec3(crate.place().x(), 0, crate.place().z() - box.width() / 2 - Body.RADIUS - 2);
        Play play = new Play(new Avatar(Body.standing(against, 0), false), start.damage(), start.sky(), start.lifts(),
                start.people(), start.props(), start.turrets());
        play = run(play, new Controls(1, 0, 0, 0, false, false, 0), 2);
        assertEquals(crate.place(), prop(play, crate.piece()).place(), "it stays where it stands");
        assertTrue(box.mass() > 100 && box.give() == 0);
    }

    @Test
    void drivenOnItPushesTheCrateAlongAheadOfIt() {
        final Setup setup = atACrate(Course.PICKUP_CHASSIS, 150, 0);
        // a step for its gun to be carried to where it now is; then at its wheel, taking it, and on the throttle
        final Play settled = run(setup.play(), Controls.NONE, Clock.STEP);
        final Lift pickup = lift(settled, setup.vehicle());
        Play play = new Play(new Avatar(Body.standing(pickup.pilot(), 0), false), settled.damage(), settled.sky(),
                settled.lifts(), settled.people(), settled.props(), settled.turrets());
        play = run(play, Controls.doing(Action.TAKE), Clock.STEP);
        assertTrue(lift(play, setup.vehicle()).held());
        final double was = prop(play, setup.crate()).place().x();
        play = run(play, new Controls(0.5, 0, 0, 0, true, false, 0), 4);
        final double gone = Math.abs(prop(play, setup.crate()).place().x() - was);
        assertTrue(gone > 200, "pushed along: " + gone);
        final double gap = Math.abs(prop(play, setup.crate()).place().x() - lift(play, setup.vehicle()).place().x());
        assertTrue(gap < Course.PICKUP_CHASSIS.length() / 2 + 200, "still ahead of it, being pushed: " + gap);
        assertFalse(play.damage().broken(setup.crate()), "pushed, not smashed");
    }
}
