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
    void itsShapeIsStillItsChassis() {
        final Damage damage = Damage.of(new Course(Demo.FLYOVER));
        final Course.Piece pickup = damage.piece(vehicle(damage, Course.PICKUP_BODY));
        assertEquals(Course.PICKUP_CHASSIS.shape(), pickup.shape());
        assertEquals(Course.PICKUP_CHASSIS, Course.Chassis.of(pickup.shape()));
    }
}
