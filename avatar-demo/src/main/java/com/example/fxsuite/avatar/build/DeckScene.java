package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Fruit;
import com.example.fxsuite.avatar.sim.Furniture;
import com.example.fxsuite.avatar.sim.Num;
import com.example.fxsuite.avatar.sim.Person;
import com.example.fxsuite.avatar.sim.Place;
import com.example.fxsuite.avatar.sim.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * What is set out on a tower's void deck in the game, on the spots its garden keeps open
 * ({@link DeckGarden#bar()}, {@link DeckGarden#stall()}): game objects, loose ({@link Course.Piece#loose}),
 * standing on the deck's floor, to be pushed about and broken; and the people at them.
 *
 * <ul>
 *   <li><b>A bar</b>: its counter ({@link Furniture#BAR}), its back shelf with it; the bartender
 *       behind it; three tall stools before it, two of them taken, those sat there leaning on it.</li>
 *   <li><b>A melon stall</b>, as the one on the barge: its table under a striped awning, a layer of
 *       watermelons on it, a few more and two pineapples on the floor at its ends; the stallholder sat
 *       on a stool behind it, a customer before it.</li>
 * </ul>
 *
 * <p>Each spot's frame is the spot's: its middle, turned its way, its front (its -z) toward the
 * deck. They are life size, whatever size the tower is built; so, on a tower built smaller than
 * life, there is none ({@link #at}).
 */
public final class DeckScene {

    private DeckScene() {
    }

    /** Where the bar's counter is in its spot, along; where its bartender stands behind it; its stools before it. */
    static final double COUNTER = -20;
    static final double BARTENDER = COUNTER + 75;
    static final double STOOLS = COUNTER - 65;
    /** Where the stall's table is in its spot, along; its stallholder sat behind it, its customer before it. */
    static final double TABLE = -15;
    static final double STALLHOLDER = TABLE + 95;
    static final double CUSTOMER = TABLE - 105;

    /**
     * What is set out on {@code garden}'s deck, and who is there, with its tower standing at
     * {@code at}, built {@code scale} times as big: nothing, smaller than life.
     */
    public static Scene at(final DeckGarden garden, final Place at, final double scale) {
        final List<Course.Piece> things = new ArrayList<>();
        final List<Course.Cast> people = new ArrayList<>();
        if (scale < 1) {
            return new Scene(things, people);
        }
        final double floor = garden.floor() * scale;
        garden.bar().ifPresent(spot -> bar(base(spot, at, scale, floor), things, people));
        garden.stall().ifPresent(spot -> stall(base(spot, at, scale, floor), things, people));
        return new Scene(things, people);
    }

    /** What is set out, loose; and who is there. */
    public record Scene(List<Course.Piece> things, List<Course.Cast> people) {

        public Scene {
            things = List.copyOf(things);
            people = List.copyOf(people);
        }
    }

    /** The spot's frame in the world: its middle on the deck's floor, {@code floor} up, turned its way and the tower's. */
    private static Place base(final DeckGarden.Spot spot, final Place at, final double scale, final double floor) {
        final Vec3 right = Num.right(at.yaw());
        final Vec3 ahead = Num.heading(at.yaw());
        return new Place(at.x() + (right.x() * spot.x() + ahead.x() * spot.z()) * scale,
                at.z() + (right.z() * spot.x() + ahead.z() * spot.z()) * scale, floor, Num.wrap(at.yaw() + spot.yaw()));
    }

    /** A place {@code across}, {@code along} and {@code up} in the frame of {@code base}, turned {@code turn} more. */
    private static Place on(final Place base, final double across, final double along, final double up, final double turn) {
        final Vec3 spot = new Vec3(base.x(), 0, base.z()).add(Num.right(base.yaw()).multiply(across))
                .add(Num.heading(base.yaw()).multiply(along));
        return new Place(spot.x(), spot.z(), base.height() + up, Num.wrap(base.yaw() + turn));
    }

    private static void bar(final Place base, final List<Course.Piece> things, final List<Course.Cast> people) {
        things.add(Course.Piece.loose(Furniture.BAR, Course.Kind.FURNITURE, on(base, 0, COUNTER, 0, 0), 0));
        person(people, Person.Role.BARTENDER, on(base, 20, BARTENDER, 0, 180), null);
        for (int k = 0; k < 3; k++) {
            final Place stool = on(base, (k - 1) * 80, STOOLS, 0, 0);
            things.add(Course.Piece.loose(Furniture.BAR_STOOL, Course.Kind.FURNITURE, stool, 0));
            if (k != 1) {
                person(people, Person.Role.DRINKER, stool, Furniture.BAR_STOOL);
            }
        }
    }

    private static void stall(final Place base, final List<Course.Piece> things, final List<Course.Cast> people) {
        final Place table = on(base, 0, TABLE, 0, 0);
        things.add(Course.Piece.loose(Furniture.TABLE, Course.Kind.FURNITURE, table, 0));
        for (int row = 0; row < 2; row++) {
            for (int k = 0; k < 6; k++) {
                final double across = (k - 2.5) * (Fruit.WATERMELON.length() + 2);
                final double along = (row - 0.5) * (Fruit.WATERMELON.width() + 3);
                things.add(Course.Piece.loose(Fruit.WATERMELON, Course.Kind.FRUIT,
                        on(table, across, along, Furniture.TABLE.height(), 0), (row * 6 + k) % 3));
            }
        }
        final double[][] heap = {{148, TABLE - 20}, {148, TABLE + 15}};
        for (int i = 0; i < heap.length; i++) {
            things.add(Course.Piece.loose(Fruit.WATERMELON, Course.Kind.FRUIT, on(base, heap[i][0], heap[i][1], 0, 0), i));
        }
        final double[][] pineapples = {{-145, TABLE - 15}, {-145, TABLE + 12}};
        for (int i = 0; i < pineapples.length; i++) {
            things.add(Course.Piece.loose(Fruit.PINEAPPLE, Course.Kind.FRUIT,
                    on(base, pineapples[i][0], pineapples[i][1], 0, 0), i));
        }
        final Place seat = on(base, -30, STALLHOLDER, 0, 180);
        things.add(Course.Piece.loose(Furniture.STOOL, Course.Kind.FURNITURE, seat, 0));
        person(people, Person.Role.STALLHOLDER, seat, Furniture.STOOL);
        person(people, Person.Role.CUSTOMER, on(base, 45, CUSTOMER, 0, 0), null);
    }

    /** Someone at {@code at}, facing its way: sat on {@code seat} there (a little back of its middle), or standing. */
    private static void person(final List<Course.Cast> people, final Person.Role role, final Place at,
                               final Furniture seat) {
        final Vec3 back = seat == null ? Vec3.ZERO : Num.heading(at.yaw()).multiply(-seat.seat());
        people.add(Course.Cast.standing(role, at.x() + back.x(), at.z() + back.z(), at.height(), at.yaw(), seat));
    }
}
