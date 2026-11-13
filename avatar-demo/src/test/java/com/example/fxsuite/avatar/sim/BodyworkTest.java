package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A vehicle's body, to the world: nothing but how it takes harm. Its armour makes it the tougher,
 * and its plating takes something off every blow; its shape is still its chassis's.
 */
class BodyworkTest {

    private static int vehicle(final Damage damage, final Course.Bodywork body) {
        for (int i = 0; i < damage.size(); i++) {
            if (damage.piece(i).kind() == Course.Kind.CAR && damage.piece(i).body() == body) {
                return i;
            }
        }
        throw new AssertionError("no vehicle wearing " + body);
    }

    private static List<Blow> blows(final int piece, final Weapon by, final int count) {
        final List<Blow> all = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            all.add(new Blow(Vec3.ZERO, new Vec3(0, 0, -1), Surface.BLOCK, by, piece));
        }
        return all;
    }

    @Test
    void theRoadsVehiclesEachWearABody() {
        final Damage damage = Damage.of(new Course(Demo.FLYOVER));
        vehicle(damage, Course.ROADSTER);
        vehicle(damage, Course.TRUCK_CAB);
        vehicle(damage, Course.PICKUP_BODY);
        vehicle(damage, Course.VAN);
    }

    @Test
    void itsArmourMakesItTougher() {
        final Damage damage = Damage.of(new Course(Demo.FLYOVER));
        final int pickup = vehicle(damage, Course.PICKUP_BODY);
        assertEquals(Damage.CAR * Course.PICKUP_BODY.armour(), Damage.toughness(damage.piece(pickup)), 1e-9);
        // the same chassis, bare, takes no more than a chassis does
        final Course.Piece bare = Course.Piece.vehicle(Course.PICKUP_CHASSIS, null, damage.piece(pickup).place(), 0);
        assertEquals(Damage.CAR, Damage.toughness(bare), 1e-9);
    }

    @Test
    void itsPlatingTakesSomethingOffEveryBlow() {
        final Damage damage = Damage.of(new Course(Demo.FLYOVER));
        final int pickup = vehicle(damage, Course.PICKUP_BODY);
        final double toughness = Damage.toughness(damage.piece(pickup));
        final Damage struck = damage.took(blows(pickup, Weapon.GATLING, 10)).damage();
        final double each = Weapon.GATLING.blow() - Course.PICKUP_BODY.plating();
        assertEquals(1 - 10 * each / toughness, struck.left(pickup), 1e-9, "each round less its plating");
        assertTrue(each < Weapon.GATLING.blow());
    }

    @Test
    void theVanIsOnTheCarsChassisItsFixedGunAndAll() {
        final Play start = Play.start(Demo.FLYOVER, false);
        final int van = vehicle(start.damage(), Course.VAN);
        assertEquals(Course.CAR_CHASSIS, Course.Chassis.of(start.damage().piece(van).shape()));
        assertTrue(start.turrets().stream().anyMatch(t -> t.host() == van && t.mount() == Course.CAR_GUN));
    }

    @Test
    void itsShapeIsItsChassisAndItsBodysBoxes() {
        final Damage damage = Damage.of(new Course(Demo.FLYOVER));
        final Course.Piece pickup = damage.piece(vehicle(damage, Course.PICKUP_BODY));
        final List<Course.Section> parts = pickup.shape().parts();
        final List<Course.Section> chassis = Course.PICKUP_CHASSIS.shape().parts();
        assertEquals(chassis, parts.subList(0, chassis.size()), "the chassis first");
        assertEquals(Course.PICKUP_BODY.solid(), parts.subList(chassis.size(), parts.size()), "then the body");
        assertEquals(Course.PICKUP_CHASSIS, Course.Chassis.of(pickup.shape()), "known by its bed still");
        // as big as all of it: up to the cab's roof
        final Course.Section box = pickup.shape().box();
        assertEquals(172, box.up() + box.halfUp(), 1e-9);
    }

    @Test
    void roundsStrikeItsBodyWhereItsChassisAloneIsNot() {
        final Damage damage = Damage.of(new Course(Demo.FLYOVER));
        final int van = vehicle(damage, Course.VAN);
        final Course.Piece piece = damage.piece(van);
        // level with the van's windows, well over its bare chassis: a round meets its side
        final double up = piece.place().height() + 140;
        final Vec3 from = new Vec3(piece.place().x() - 1000, -up, piece.place().z());
        final Hit hit = damage.obstacles().ray(from, new Vec3(1, 0, 0), 2000);
        assertTrue(hit != null && hit.piece() == van, "struck its side");
        assertEquals(piece.place().x() - 118, hit.at().x(), 1e-6);
    }

    private static Play run(Play play, final Controls controls, final double seconds) {
        for (long step = Math.round(seconds / Clock.STEP); step > 0; step--) {
            play = play.step(controls, Clock.STEP).play();
        }
        return play;
    }

    @Test
    void theVanIsGotIntoFromBesideItsDoorAndLetOutThere() {
        final Play start = Play.start(Demo.FLYOVER, false);
        final int piece = vehicle(start.damage(), Course.VAN);
        final Lift van = start.lifts().stream().filter(l -> l.piece() == piece).findFirst().orElseThrow();
        final Vec3 door = van.at(van.craft().door());
        Play play = new Play(new Avatar(Body.standing(door, 0), false), start.damage(), start.sky(), start.lifts(),
                start.people(), start.props(), start.turrets());
        assertTrue(play.withinReach().isPresent(), "from the ground beside its door");
        play = run(play, Controls.doing(Action.TAKE), Clock.STEP);
        assertTrue(play.atControls().isPresent());
        assertEquals(0, play.avatar().body().position().distance(van.pilot()), 1e-6, "in its seat");
        // driven, its driver sits still in it, carried
        play = run(play, new Controls(1, 0, 0, 0, true, false, 0), 1.5);
        final Lift driven = play.atControls().orElseThrow();
        assertEquals(0, play.avatar().body().position().distance(driven.pilot()), 1e-6);
        play = run(play, new Controls(0, 0, 0, 1, true, false, 0), 2);
        // let go, out on the ground beside the door
        play = run(play, Controls.doing(Action.TAKE), Clock.STEP);
        assertTrue(play.atControls().isEmpty());
        play = run(play, Controls.NONE, 0.5);
        final Lift parked = play.lifts().stream().filter(l -> l.piece() == piece).findFirst().orElseThrow();
        final Vec3 out = parked.at(parked.craft().door());
        assertTrue(play.avatar().body().grounded());
        assertEquals(0, play.avatar().body().height(), 1e-6);
        assertEquals(0, Math.hypot(play.avatar().body().position().x() - out.x(),
                play.avatar().body().position().z() - out.z()), 1e-6, "beside the door");
    }

    @Test
    void theRobotBumpsIntoTheVansSide() {
        final Play start = Play.start(Demo.FLYOVER, false);
        final Course.Piece van = start.damage().piece(vehicle(start.damage(), Course.VAN));
        // beside it, walking square at its side, level with its middle
        Body body = Body.standing(new Vec3(van.place().x() + 500, 0, van.place().z()), -90);
        for (int i = 0; i < 240; i++) {
            body = body.move(new Controls(1, 0, 0, 0, false, false, 0), Clock.STEP, start.obstacles()).body();
        }
        assertEquals(van.place().x() + 118 + Body.RADIUS, body.position().x(), 1, "stopped at its side");
    }
}
