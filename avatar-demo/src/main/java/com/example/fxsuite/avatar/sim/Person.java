package com.example.fxsuite.avatar.sim;

import java.util.ArrayList;
import java.util.List;

/**
 * Someone in the world other than the avatar (a character): who they are ({@code role}, which is how they look
 * and sit), their {@code body}, how much more they can take ({@code health}), and whether they are
 * sitting. Pure state, stepped with the rest of the world ({@link Play}).
 *
 * <ul>
 *   <li><b>A body like the avatar's</b> ({@link Body}): an upright cylinder that falls, stands on
 *       whatever is under it, slides along what it runs into, and is carried by what it stands on
 *       when that moves: the barge, say.</li>
 *   <li><b>Pushed</b> by the avatar walking into them, they are shoved along at its pace and slide
 *       to a stop; shoved over an edge, they fall. They push each other out of the way. A push gets
 *       anyone sitting to their feet.</li>
 *   <li><b>Struck</b> by a round, the glaive or a bomb, they flinch back and their health runs
 *       down; at none left, they are knocked down, and lie there. Down, they can still be shoved
 *       about.</li>
 * </ul>
 *
 * <p>To be struck, each is a target with a number of its own ({@link #piece}), far past the
 * course's pieces, so a ray can say whom it met.
 *
 * @param id      which of them, from 0
 * @param role    who they are
 * @param body    where they are and how they are going
 * @param health  how much more they can take before they are knocked down
 * @param sitting whether they sit, as they were put, until something gets them up
 */
public record Person(int id, Role role, Body body, double health, boolean sitting) {

    /** Who someone is: how they look and how they sit, for the graphics. */
    public enum Role { STALLHOLDER, CUSTOMER, RIDER }

    /** How much anyone can take: six rifle rounds, or a stroke of the glaive. */
    public static final double TOUGHNESS = 60;
    /** Where their numbers as targets start, past any piece of the course. */
    public static final int BASE = 1_000_000;
    /** A shove sends them on a little faster than what shoved them, so they come clear. */
    public static final double SHOVE = 1.15;
    /** How hard a hit knocks them back, units a second. */
    public static final double FLINCH = 120;
    /** How high someone lying down is. */
    public static final double LYING = 40;

    /** The cast, put where {@link Course#CAST} says, on the platform it says, as that platform now is ({@code lifts}). */
    public static List<Person> of(final Damage damage, final List<Lift> lifts) {
        final List<Person> people = new ArrayList<>();
        for (final Course.Cast cast : Course.CAST) {
            for (final Lift lift : lifts) {
                if (lift.deck().equals(cast.deck())) {
                    final Place at = lift.place();
                    final Vec3 spot = new Vec3(at.x(), -at.height(), at.z())
                            .add(Num.right(at.yaw()).multiply(cast.across()))
                            .add(Num.heading(at.yaw()).multiply(cast.along()));
                    people.add(new Person(people.size(), cast.role(),
                            Body.standing(spot, Num.wrap(at.yaw() + cast.facing())), TOUGHNESS, cast.sitting()));
                }
            }
        }
        return people;
    }

    /** Whether they are knocked down. */
    public boolean down() {
        return health <= 0;
    }

    /** Their number as a target. */
    public int piece() {
        return BASE + id;
    }

    /** Whether target {@code piece} is someone, not a piece of the course. */
    public static boolean is(final int piece) {
        return piece >= BASE;
    }

    /** Which of them target {@code piece} is. */
    public static int which(final int piece) {
        return piece - BASE;
    }

    /** Their box, to be struck: up, as tall as a body; down, lying low along the way they faced. */
    public Block box() {
        final Vec3 at = body.position();
        final double floor = body.height();
        return down()
                ? new Block(at.x(), at.z(), Body.RADIUS, Body.TALL / 2, body.yaw(), floor, floor + LYING)
                : new Block(at.x(), at.z(), Body.RADIUS, Body.RADIUS, body.yaw(), floor, floor + Body.TALL);
    }

    /** {@code seconds} on, as they are going, among {@code world}: coming to a stop, or falling. */
    public Person moved(final Obstacles world, final double seconds) {
        return new Person(id, role, body.move(Controls.NONE, seconds, world).body(), health, sitting);
    }

    /** Carried by what they stand on, as it moved from {@code from} to {@code to}. */
    public Person carried(final Place from, final Place to) {
        return new Person(id, role, body.carried(from, to), health, sitting);
    }

    /** Shoved, across the ground, {@code vx} and {@code vz} faster: up on their feet, if sitting. */
    public Person shoved(final double vx, final double vz) {
        return new Person(id, role, body.shoved(vx, vz), health, false);
    }

    /** Moved {@code dx} and {@code dz} across the ground, pushed clear of something. */
    public Person shifted(final double dx, final double dz) {
        return new Person(id, role, body.shifted(dx, dz), health, sitting);
    }

    /** Struck, taking {@code blow}: up on their feet, if sitting, and down if that was the last of them. */
    public Person struck(final double blow) {
        return new Person(id, role, body, Math.max(0, health - blow), false);
    }
}
