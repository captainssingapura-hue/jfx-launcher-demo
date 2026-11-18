package com.example.fxsuite.avatar.bench.kit;

import com.example.fxsuite.avatar.look.DrawnDemo;
import com.example.fxsuite.avatar.look.Scenery;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Person;
import com.example.fxsuite.avatar.sim.Place;
import com.example.fxsuite.avatar.sim.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * A bench's world played ({@link PlayWorld}) with whoever is asked to come ({@link Guests}): a demo made on the fly,
 * never offered, never looked up by name, its runs not recorded. Its world as the bench built it; its vehicles parked
 * where the world parks them, in the order chosen; its people stood where it stands them; the avatar on the spot asked,
 * or where the world starts it, with the jet-pack or without.
 */
public final class Played implements DrawnDemo {

    /**
     * Who comes: {@code vehicles}, in order; {@code people} people; the avatar on {@code from} (where the world starts
     * it, if null), wearing the jet-pack if {@code pack}.
     */
    public record Guests(List<Guest> vehicles, int people, boolean pack, Spot from) {

        public Guests {
            vehicles = List.copyOf(vehicles);
        }

        /** {@code vehicles}, no one else, the avatar where the world starts it, without the pack. */
        public static Guests of(final List<Guest> vehicles) {
            return new Guests(vehicles, 0, false, null);
        }

        /** The same, the avatar on {@code spot}. */
        public Guests from(final Spot spot) {
            return new Guests(vehicles, people, pack, spot);
        }
    }

    private final PlayWorld world;
    private final Guests guests;

    public Played(final PlayWorld world, final Guests guests) {
        this.world = world;
        this.guests = guests;
    }

    /** What is played. */
    public PlayWorld world() {
        return world;
    }

    /** Who comes. */
    public Guests guests() {
        return guests;
    }

    @Override
    public String name() {
        return "BENCH_PLAY";
    }

    @Override
    public String title() {
        return world.title();
    }

    @Override
    public String description() {
        return world.description();
    }

    @Override
    public double startYaw() {
        return start().place().yaw();
    }

    @Override
    public boolean packed() {
        return guests.pack();
    }

    @Override
    public boolean offered() {
        return false;
    }

    @Override
    public void lay(final Course.Layout layout) {
        world.lay(layout);
        layout.pieces(vehicles());
        final List<Course.Cast> people = new ArrayList<>();
        for (final Spot spot : guests.people() > 0 ? world.standing(guests.people()) : List.<Spot>of()) {
            final Place p = spot.place();
            people.add(Course.Cast.standing(Person.Role.GUEST, p.x(), p.z(), p.height(), p.yaw(), null));
        }
        layout.people(people);
        final Place s = start().place();
        layout.start(new Vec3(s.x(), -s.height(), s.z()));
    }

    @Override
    public Scenery scenery() {
        return world.scenery();
    }

    /** Where the avatar starts. */
    private Spot start() {
        return guests.from() != null ? guests.from() : world.start();
    }

    /** The vehicles, each on its spot, in order. */
    List<Course.Piece> vehicles() {
        final List<Spot> spots = world.parked(guests.vehicles().stream().map(Guest::length).toList());
        final List<Course.Piece> pieces = new ArrayList<>();
        for (int k = 0; k < guests.vehicles().size(); k++) {
            pieces.add(guests.vehicles().get(k).at(spots.get(k), k % 4));
        }
        return pieces;
    }
}
