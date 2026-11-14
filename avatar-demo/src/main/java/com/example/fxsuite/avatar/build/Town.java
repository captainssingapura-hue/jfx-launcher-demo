package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Block;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Num;
import com.example.fxsuite.avatar.sim.Place;
import com.example.fxsuite.avatar.sim.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The town: every building put up on a site of its own along streets, a grid of three by three
 * 120 m apart, ahead of the start, each facing back down the way to it; and the boxer motorcycle by
 * the start to ride between them. Each building in the world is one piece ({@link Course.Kind#BUILDING}),
 * dressed in its own way and its first palette; its solid blocks its parts' boxes (its nave, its
 * tower, its roof; its podium, its shaft), so it is solid where it stands and open between;
 * broken as a whole.
 */
public final class Town {

    private Town() {
    }

    /** A site: which building, where, and how it is dressed. */
    public record Site(String name, Supplier<Building> building, Palette palette, Place place) {
    }

    /** Its streets' grid: so far apart, the first row so far ahead. */
    private static final double APART = 12_000;
    private static final double AHEAD = 8_000;

    /** Its sites, row by row from the start. */
    public static final List<Site> SITES = sites();

    private static List<Site> sites() {
        final List<Site> sites = new ArrayList<>();
        final List<Object[]> buildings = List.of(
                new Object[] {"town church", (Supplier<Building>) Church::building, Palette.STONE},
                new Object[] {"temple hall", (Supplier<Building>) Temple::building, Temple.LACQUERED},
                new Object[] {"brutalist apartments", (Supplier<Building>) Apartments::building, Palette.CONCRETE},
                new Object[] {"setback tower", (Supplier<Building>) () -> TowerBuildings.of(Towers.setback()), Palette.STONE},
                new Object[] {"round tower", (Supplier<Building>) () -> TowerBuildings.of(Towers.round()), Palette.CONCRETE},
                new Object[] {"oval tower", (Supplier<Building>) () -> TowerBuildings.of(Towers.oval()), Palette.CONCRETE},
                new Object[] {"rounded tower", (Supplier<Building>) () -> TowerBuildings.of(Towers.rounded()), Palette.PRECAST},
                new Object[] {"L block on a podium", (Supplier<Building>) () -> TowerBuildings.of(Towers.lBlock()),
                        Palette.CONCRETE},
                new Object[] {"bow-front tower", (Supplier<Building>) () -> TowerBuildings.of(Towers.bowFront()),
                        Palette.CONCRETE});
        for (int k = 0; k < buildings.size(); k++) {
            final Object[] b = buildings.get(k);
            @SuppressWarnings("unchecked")
            final Supplier<Building> make = (Supplier<Building>) b[1];
            final double x = (k % 3 - 1) * APART;
            final double z = AHEAD + (k / 3) * APART;
            // each turned to face back toward the start, its front to the street
            sites.add(new Site((String) b[0], make, (Palette) b[2], new Place(x, z, 0, 180)));
        }
        return List.copyOf(sites);
    }

    /** Each site's building, dressed: worked out once, when first wanted. */
    private static final Map<Integer, List<Solid>> DRESSED = new java.util.concurrent.ConcurrentHashMap<>();

    /** Site {@code k}'s building, dressed as it is: its own way, in its palette. */
    public static List<Solid> solids(final int k) {
        return DRESSED.computeIfAbsent(k, i -> {
            final Site site = SITES.get(i);
            final Building building = site.building().get();
            return building.dressed(building.own(), site.palette());
        });
    }

    /** What stands in the town: each building, one piece, and the boxer motorcycle by the start. */
    public static List<Course.Piece> pieces() {
        final List<Course.Piece> pieces = new ArrayList<>();
        for (int k = 0; k < SITES.size(); k++) {
            pieces.add(piece(k));
        }
        pieces.add(Course.Piece.vehicle(Course.BOXER, null, new Place(300, 600, Course.BOXER.ride(), 0), 0));
        return pieces;
    }

    /**
     * Site {@code k}'s building, one piece: its box about its middle, as high as it stands; its solid
     * blocks a box round each of its parts, as its solids name them (its nave, its tower, its roof, its
     * columns, all of them together each), turned and placed as it is.
     */
    static Course.Piece piece(final int k) {
        final Site site = SITES.get(k);
        final Place at = site.place();
        final Map<String, double[]> parts = new LinkedHashMap<>();
        final double[] whole = {0, 0, 0};
        for (final Solid solid : solids(k)) {
            final double[] box = parts.computeIfAbsent(part(solid.name()), n -> new double[] {
                    Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                    Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY});
            for (final Point p : solid.corners()) {
                box[0] = Math.min(box[0], p.across());
                box[1] = Math.max(box[1], p.across());
                box[2] = Math.min(box[2], p.along());
                box[3] = Math.max(box[3], p.along());
                box[4] = Math.min(box[4], p.up());
                box[5] = Math.max(box[5], p.up());
                whole[0] = Math.max(whole[0], Math.abs(p.across()));
                whole[1] = Math.max(whole[1], Math.abs(p.along()));
                whole[2] = Math.max(whole[2], p.up());
            }
        }
        final List<Block> solid = new ArrayList<>();
        for (final double[] box : parts.values()) {
            final Vec3 middle = place(at, (box[0] + box[1]) / 2, (box[2] + box[3]) / 2);
            solid.add(new Block(middle.x(), middle.z(), Math.max(1, (box[1] - box[0]) / 2),
                    Math.max(1, (box[3] - box[2]) / 2), at.yaw(), Math.max(0, box[4]), Math.max(box[4] + 1, box[5])));
        }
        final Block block = new Block(at.x(), at.z(), whole[0], whole[1], at.yaw(), 0, whole[2]);
        return new Course.Piece(block, Course.Kind.BUILDING, k, solid, null, at, null, -1, null);
    }

    /** Which part a solid is of, by its name: up to its first comma, any number at its end left off. */
    static String part(final String name) {
        final int comma = name.indexOf(", ");
        return (comma < 0 ? name : name.substring(0, comma)).replaceAll(" \\d+$", "");
    }

    /** Where {@code across}, {@code along} in a building at {@code at} is in the world (x, 0, z). */
    private static Vec3 place(final Place at, final double across, final double along) {
        final Vec3 right = Num.right(at.yaw());
        final Vec3 ahead = Num.heading(at.yaw());
        return new Vec3(at.x() + right.x() * across + ahead.x() * along, 0, at.z() + right.z() * across + ahead.z() * along);
    }
}
