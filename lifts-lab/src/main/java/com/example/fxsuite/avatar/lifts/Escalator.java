package com.example.fxsuite.avatar.lifts;

import com.example.fxsuite.avatar.build.Material;
import com.example.fxsuite.avatar.build.Point;
import com.example.fxsuite.avatar.build.Role;
import com.example.fxsuite.avatar.build.Solid;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Place;

import java.util.ArrayList;
import java.util.List;

/**
 * An escalator going up along: its steps coming up flat out of the floor at its foot, a stair up its incline, flat
 * again at its head, and dipping under the landing there, back down under the stair, out of sight, to come up at its
 * foot again. Its head at {@code head} along, {@code rise} up; its foot as far back along as its incline at
 * {@code slope} and its flats need.
 *
 * <p>Where each step is is a function of time alone, at {@code speed} along. A step's rise is under what a body steps
 * up of itself, so it can be walked up as well as ridden.
 *
 * @param x     across, its middle
 * @param head  along, where its steps go under the landing at its head
 * @param rise  how high its head's landing is over its foot's
 * @param slope how steep its incline, degrees
 * @param speed how fast its steps go, cm/s
 */
record Escalator(double x, double head, double rise, double slope, double speed) {

    /** Its steps: so wide; each so deep along; its flats at foot and head so long; how far a step dips at either end. */
    static final double WIDE = 100;
    static final double TREAD = 34;
    static final double FLAT = 100;
    static final double DIP = 30;
    static final double DIPPING = 40;
    /** How far under its stair its steps go back down. */
    static final double UNDER = 60;

    Escalator {
        if (!(rise > 0 && slope > 0 && slope < 45 && speed > 0)) {
            throw new IllegalArgumentException("an escalator going up, its incline under 45 degrees, its steps going");
        }
    }

    /** How far along its incline goes. */
    double run() {
        return rise / Math.tan(Math.toRadians(slope));
    }

    /** How far along it goes, foot to head. */
    double length() {
        return FLAT + run() + FLAT;
    }

    /** Where along its foot is. */
    double foot() {
        return head - length();
    }

    /** How high each step is, its rise: as a step is deep along its incline, so much up. */
    double riser() {
        return TREAD * Math.tan(Math.toRadians(slope));
    }

    /** How many steps it has, all round. */
    int steps() {
        return (int) Math.round(2 * length() / TREAD);
    }

    /** How high a step's top is, {@code u} along from its foot, on its way up: flat, the stair, flat; dipping at either end. */
    double top(final double u) {
        final double stair = u < FLAT ? 0 : u < FLAT + run() ? (u - FLAT) / run() * rise : rise;
        if (u < DIPPING) {
            return stair - DIP * (1 - u / DIPPING);
        }
        if (u > length() - DIPPING) {
            return stair - DIP * (u - (length() - DIPPING)) / DIPPING;
        }
        return stair;
    }

    /** Step {@code k}'s way: up from its foot to its head, then back under. */
    Course.Route step(final int k) {
        final double round = 2 * length();
        final double start = k * round / steps();
        return seconds -> {
            final double s = (((start + speed * seconds) % round) + round) % round;
            if (s < length()) {
                return new Place(x, foot() + s, top(s) - riser(), 0);
            }
            final double u = length() - (s - length());
            final double stair = Math.max(0, Math.min(rise, (u - FLAT) / run() * rise));
            return new Place(x, foot() + u, stair - UNDER - riser(), 0);
        };
    }

    /** A step: its tread on top, its riser's height deep, so the steps on its incline make a stair with no gaps. */
    List<Box> stepBoxes() {
        return List.of(new Box("step", Role.BASE, Material.METAL, -WIDE / 2, WIDE / 2, -TREAD / 2, TREAD / 2, 0,
                riser()));
    }

    /** Its steps, each a mover drawn by {@code key}. */
    List<Mover> movers(final int key) {
        final List<Mover> movers = new ArrayList<>();
        for (int k = 0; k < steps(); k++) {
            movers.add(new Mover("escalator step " + (k + 1), stepBoxes(), step(k), key));
        }
        return movers;
    }

    /** Its balustrades, either side, a metre over its steps, glass: in the way. */
    List<Solid> balustrades() {
        final List<Solid> sides = new ArrayList<>();
        for (final int side : new int[] {-1, 1}) {
            final double a0 = x + side * (WIDE / 2 + 5);
            final double a1 = a0 + side * 3;
            sides.addAll(stretches("balustrade, " + (side < 0 ? "left" : "right"), Role.GLAZING, Material.GLASS,
                    Math.min(a0, a1), Math.max(a0, a1), 0, 100));
        }
        return sides;
    }

    /**
     * Its comb plates, at its foot and its head, as long as its steps dip: the floor carried on over them, so whoever
     * rides a step off its end stays at the landing's level as the step goes down under the plate. In the way.
     */
    List<Solid> combs() {
        final double a = WIDE / 2;
        return List.of(
                new Box("comb plate, foot", Role.BASE, Material.METAL, x - a, x + a, foot(), foot() + DIPPING, -2, 0)
                        .solid(),
                new Box("comb plate, head", Role.BASE, Material.METAL, x - a, x + a, head - DIPPING, head, rise - 2,
                        rise).solid());
    }

    /** Its truss under its stair, from the floor at its foot to the landing at its head: drawn, not in the way. */
    List<Solid> truss() {
        return stretches("truss", Role.STRUCTURE, Material.METAL, x - WIDE / 2 - 8, x + WIDE / 2 + 8, -UNDER - 40, -2);
    }

    /**
     * Solids along it, foot to head, one for each stretch (its foot's flat, its incline, its head's flat): from
     * {@code a0} to {@code a1} across, {@code below} to {@code above} over the line of its steps' tops.
     */
    List<Solid> stretches(final String name, final Role role, final Material material, final double a0,
                          final double a1, final double below, final double above) {
        final List<Solid> parts = new ArrayList<>();
        final double[] along = {foot(), foot() + FLAT, foot() + FLAT + run(), head};
        final double[] up = {0, 0, rise, rise};
        for (int k = 0; k + 1 < along.length; k++) {
            final List<Point> corners = new ArrayList<>();
            for (final double a : new double[] {a0, a1}) {
                corners.add(new Point(a, along[k], up[k] + below));
                corners.add(new Point(a, along[k + 1], up[k + 1] + below));
                corners.add(new Point(a, along[k + 1], up[k + 1] + above));
                corners.add(new Point(a, along[k], up[k] + above));
            }
            parts.add(new Solid(name + ", " + (k == 1 ? "incline" : k == 0 ? "foot" : "head"), role, material,
                    corners));
        }
        return parts;
    }
}
