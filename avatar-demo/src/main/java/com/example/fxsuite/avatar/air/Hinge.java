package com.example.fxsuite.avatar.air;

import com.example.fxsuite.avatar.build.Point;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Part of an aircraft that turns on a hinge, to an angle set by a control ({@code control}), not
 * round and round as a {@link Spinner} does: a duct tilted from hovering to flying, a turret traversed
 * and its gun raised, a rudder, a door. Its {@code part}, in the aircraft's frame as it is at rest
 * (at 0°), turns about {@code axis} through {@code pivot} by the control's angle times
 * {@code sense} (the right-hand way, the thumb along the axis), within {@code least} to {@code most}.
 * What is on it (its spinners, the hinges on it in turn: a gun on a turret) goes with it.
 *
 * @param control which control turns it: hinges sharing a name turn together
 * @param pivot   a point on its axis
 * @param axis    its axis, a unit
 * @param sense   which way it turns for the control: 1, or -1 for its mirror image, so either side
 *                turns alike
 * @param least   how far the control goes one way, in degrees
 * @param most    and the other
 * @param part    what turns, at rest
 */
public record Hinge(String control, Point pivot, Point axis, double sense, double least, double most, Craft part) {

    public Hinge {
        if (Math.abs(axis.length() - 1) > 1e-6 || Math.abs(Math.abs(sense) - 1) > 1e-9 || least > most) {
            throw new IllegalArgumentException(control + ": an axis " + axis + ", sense " + sense + ", " + least
                    + " to " + most);
        }
    }

    /** The same, mounted ({@link Mount}): its pivot put, its axis turned. */
    public Hinge placed(final Mount mount) {
        return new Hinge(control, mount.place(pivot), mount.turn(axis), sense, least, most, part.placed(mount));
    }

    /**
     * Its mirror image, on the other side: its pivot and axis mirrored, and its sense turned about,
     * so it turns as its original does for the same control (a mirror turns a turn the other way).
     */
    public Hinge mirrored() {
        return new Hinge(control, Skin.mirror(pivot), Skin.mirror(axis), -sense, least, most, part.mirrored());
    }

    /** Where {@code p}, on its part at rest, is with the control at {@code degrees}. */
    public Point turned(final Point p, final double degrees) {
        final double t = Math.toRadians(degrees * sense);
        final Point v = p.plus(pivot.times(-1));
        // Rodrigues: v cos t + (k x v) sin t + k (k . v)(1 - cos t)
        final Point k = axis;
        return pivot.plus(v.times(Math.cos(t)).plus(Piece.cross(k, v).times(Math.sin(t)))
                .plus(k.times(k.dot(v) * (1 - Math.cos(t)))));
    }

    /** The controls of {@code craft}'s hinges, and of those on them, each its range: least, most. */
    public static Map<String, double[]> controls(final Craft craft) {
        final Map<String, double[]> controls = new LinkedHashMap<>();
        for (final Hinge hinge : craft.hinges()) {
            controls.putIfAbsent(hinge.control(), new double[] {hinge.least(), hinge.most()});
            controls(hinge.part()).forEach(controls::putIfAbsent);
        }
        return controls;
    }
}
