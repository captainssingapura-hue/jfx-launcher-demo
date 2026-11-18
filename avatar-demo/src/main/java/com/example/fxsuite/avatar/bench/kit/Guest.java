package com.example.fxsuite.avatar.bench.kit;

import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Place;

import java.util.ArrayList;
import java.util.List;

/**
 * What can be brought into a world played from a bench, besides the avatar: any of the game's vehicles, put on a spot
 * ({@link Spot}), facing as it does, standing on what is stood on there: a land vehicle on its wheels, a platform just
 * over it, an aircraft on its footing.
 */
public sealed interface Guest permits Guest.Vehicle, Guest.Platform, Guest.Plane {

    /** What it is called, to choose it by. */
    String name();

    /** What it is made of, in its own frame. */
    Course.Shape shape();

    /** How long it is, nose to tail. */
    default double length() {
        return 2 * shape().box().halfAlong();
    }

    /** It, as a piece of the world, put on {@code spot}, tinted the {@code tint}th way. */
    Course.Piece at(Spot spot, int tint);

    /** A land vehicle: a car, the pickup, the truck, a motorcycle; on its wheels. */
    record Vehicle(Course.Chassis chassis) implements Guest {

        @Override
        public String name() {
            return chassis.name();
        }

        @Override
        public Course.Shape shape() {
            return chassis.shape();
        }

        @Override
        public Course.Piece at(final Spot spot, final int tint) {
            final Place p = spot.place();
            return Course.Piece.vehicle(chassis, null, new Place(p.x(), p.z(), p.height() + chassis.ride(), p.yaw()),
                    tint);
        }
    }

    /** A platform: a deck flown from its console; hanging just over the ground, as it is left. */
    record Platform(String name, Course.Deck deck) implements Guest {

        @Override
        public Course.Shape shape() {
            return deck.shape();
        }

        @Override
        public Course.Piece at(final Spot spot, final int tint) {
            final Place p = spot.place();
            return Course.Piece.moving(deck.shape(), new Place(p.x(), p.z(), p.height() + Course.LIFT_LOW, p.yaw()),
                    Course.Kind.PLATFORM, tint, null);
        }
    }

    /** An aircraft, standing on its footing. */
    record Plane(Course.Aircraft aircraft) implements Guest {

        @Override
        public String name() {
            return aircraft.name();
        }

        @Override
        public Course.Shape shape() {
            return aircraft.shape();
        }

        @Override
        public Course.Piece at(final Spot spot, final int tint) {
            final Place p = spot.place();
            return Course.Piece.moving(aircraft.shape(), new Place(p.x(), p.z(), p.height() + aircraft.standing(),
                    p.yaw()), Course.Kind.AIRCRAFT, tint, null);
        }
    }

    /** The land vehicles, one of each, in the order they are usually parked. */
    static List<Guest> landVehicles() {
        return List.of(new Vehicle(Course.CAR_CHASSIS), new Vehicle(Course.PICKUP_CHASSIS),
                new Vehicle(Course.TRUCK_CHASSIS), new Vehicle(Course.SCOOTER), new Vehicle(Course.SPORT_BIKE),
                new Vehicle(Course.CRUISER), new Vehicle(Course.BOXER), new Vehicle(Course.BOXER_SIDECAR));
    }

    /** Everything that can be brought: the land vehicles, the ice cream cart, the platforms, the gunship. */
    static List<Guest> all() {
        final List<Guest> all = new ArrayList<>(landVehicles());
        all.add(new Vehicle(Course.BOXER_ICE_CREAM));
        all.add(new Platform("carpet", Course.CARPET));
        all.add(new Platform("meeting room", Course.MEETING));
        all.add(new Platform("barge", Course.BARGE));
        all.add(new Plane(Course.GUNSHIP));
        return all;
    }
}
