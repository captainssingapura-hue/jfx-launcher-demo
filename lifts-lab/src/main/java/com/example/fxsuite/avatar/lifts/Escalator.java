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
 * again at its head, and dipping under the comb plate there, back down under the stair, out of sight, to come up at
 * its foot again. Its head at {@code head} along, {@code rise} up; its foot as far back along as its incline at
 * {@code slope} and its flats ({@code flat} each) need.
 *
 * <p>Where each step is is a function of time alone, at {@code speed} along. Each step is as deep as its slope lets
 * its rise be a little under what a body steps up of itself ({@link #RISER}), so it can be walked as well as ridden.
 *
 * @param x     across, its middle
 * @param head  along, where its steps go under the comb plate at its head
 * @param rise  how high its head's landing is over its foot's
 * @param slope how steep its incline, degrees
 * @param speed how fast its steps go, cm/s
 * @param width how wide its steps are
 * @param flat  how long its flats are, at foot and head
 */
record Escalator(double x, double head, double rise, double slope, double speed, double width, double flat) {

    /** Steps as they mostly are: so wide; flats so long. */
    static final double WIDE = 100;
    static final double FLAT = 100;
    /** How high each step rises: a little under what a body steps up of itself. */
    static final double RISER = 19.5;
    /** How far a step dips at either end, over how long; how far under its stair its steps go back down. */
    static final double DIP = 30;
    static final double DIPPING = 40;
    static final double UNDER = 60;

    Escalator {
        if (!(rise > 0 && slope >= 20 && slope <= 40 && speed > 0 && width >= 40 && flat >= DIPPING + 20)) {
            throw new IllegalArgumentException("an escalator going up, its incline from 20 to 40 degrees, its steps going");
        }
    }

    /** One with steps as they mostly are. */
    Escalator(final double x, final double head, final double rise, final double slope, final double speed) {
        this(x, head, rise, slope, speed, WIDE, FLAT);
    }

    /** How far along its incline goes. */
    double run() {
        return rise / Math.tan(Math.toRadians(slope));
    }

    /** How far along it goes, foot to head. */
    double length() {
        return flat + run() + flat;
    }

    /** Where along its foot is. */
    double foot() {
        return head - length();
    }

    /** How deep each step is along: as deep as its slope lets it rise {@link #RISER}. */
    double tread() {
        return RISER / Math.tan(Math.toRadians(slope));
    }

    /** How high each step is, its rise. */
    double riser() {
        return RISER;
    }

    /** How many steps it has, all round. */
    int steps() {
        return (int) Math.round(2 * length() / tread());
    }

    /** How high a step's top is, {@code u} along from its foot, on its way up: flat, the stair, flat; dipping at either end. */
    double top(final double u) {
        final double stair = u < flat ? 0 : u < flat + run() ? (u - flat) / run() * rise : rise;
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
            final double stair = Math.max(0, Math.min(rise, (u - flat) / run() * rise));
            return new Place(x, foot() + u, stair - UNDER - riser(), 0);
        };
    }

    /** A step: its tread on top, its riser's height deep, so the steps on its incline make a stair with no gaps. */
    List<Box> stepBoxes() {
        return List.of(new Box("step", Role.BASE, Material.METAL, -width / 2, width / 2, -tread() / 2, tread() / 2, 0,
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

    /**
     * Its comb plates, at its foot and its head, as long as its steps dip: the floor carried on over them, so whoever
     * rides a step off its end stays at the landing's level as the step goes down under the plate. In the way.
     */
    List<Solid> combs() {
        final double a = width / 2;
        return List.of(
                new Box("comb plate, foot", Role.BASE, Material.METAL, x - a, x + a, foot(), foot() + DIPPING, -2, 0)
                        .solid(),
                new Box("comb plate, head", Role.BASE, Material.METAL, x - a, x + a, head - DIPPING, head, rise - 2,
                        rise).solid());
    }

    /** Its balustrades, either side, a metre over its steps, glass: in the way. */
    List<Solid> balustrades() {
        final List<Solid> sides = new ArrayList<>();
        for (final int side : new int[] {-1, 1}) {
            final double a0 = x + side * (width / 2 + 5);
            final double a1 = a0 + side * 3;
            sides.addAll(stretches("balustrade, " + (side < 0 ? "left" : "right"), Role.GLAZING, Material.GLASS,
                    Math.min(a0, a1), Math.max(a0, a1), 0, 100));
        }
        return sides;
    }

    /** Its truss under its stair, from the floor at its foot to the landing at its head: drawn, not in the way. */
    List<Solid> truss() {
        return stretches("truss", Role.STRUCTURE, Material.METAL, x - width / 2 - 8, x + width / 2 + 8, -UNDER - 40,
                -2);
    }

    /** How wide it is all told, balustrades and all. */
    double outside() {
        return width + 2 * 8;
    }

    /**
     * Solids along it, foot to head, one for each stretch (its foot's flat, its incline, its head's flat): from
     * {@code a0} to {@code a1} across, {@code below} to {@code above} over the line of its steps' tops.
     */
    List<Solid> stretches(final String name, final Role role, final Material material, final double a0,
                          final double a1, final double below, final double above) {
        final List<Solid> parts = new ArrayList<>();
        final double[] along = {foot(), foot() + flat, foot() + flat + run(), head};
        final double[] up = {0, 0, rise, rise};
        for (int k = 0; k + 1 < along.length; k++) {
            // corner c across as bit 0 says, along as bit 1, under or over as bit 2, as a solid's corners are drawn
            final List<Point> corners = new ArrayList<>();
            for (int c = 0; c < 8; c++) {
                final int end = (c & 2) == 0 ? k : k + 1;
                corners.add(new Point((c & 1) == 0 ? a0 : a1, along[end], up[end] + ((c & 4) == 0 ? below : above)));
            }
            parts.add(new Solid(name + ", " + (k == 1 ? "incline" : k == 0 ? "foot" : "head"), role, material,
                    corners));
        }
        return parts;
    }
}
