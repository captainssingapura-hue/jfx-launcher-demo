package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Outline;
import com.example.fxsuite.avatar.sim.Place;
import com.example.fxsuite.avatar.sim.Prism;
import com.example.fxsuite.avatar.sim.Vec2;
import com.example.fxsuite.avatar.sim.Vec3;
import com.example.fxsuite.avatar.sim.Volume;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * A tower and a garden on its roof, composed: one building put on another. On the tower's roof, a
 * lawn; in the middle of it, as near as there is most room, a pavilion ({@link Pavilion}) about half
 * as wide as the roof, facing the tower's front; round it, trees and shrubs, as many as the roof has
 * room for, clear of the pavilion, its steps, and the parapet.
 *
 * <p>Composed by placing: the pavilion's parts, its solids and its shape, put in the tower's own frame
 * on its roof, as the tower is put in the world; the plants likewise. So the tower's piece carries all
 * of it, placed and scaled together.
 */
public final class RoofGarden {

    /** A plant: a broad tree, a pine, or a shrub; where, and how big (its crown's radius). */
    public enum Kind { TREE, PINE, SHRUB }

    public record Plant(Kind kind, double x, double z, double size) {
    }

    /** How thick the lawn is; how far in from the roof's edge anything stands (the parapet, and room by it). */
    public static final double LAWN = 5;
    public static final double EDGE = 120;

    private final Tower tower;
    private final Pavilion pavilion;
    private final Place at;
    private final List<Plant> plants;
    private final List<Vec2> roof;

    private RoofGarden(final Tower tower, final Pavilion pavilion, final Place at, final List<Plant> plants,
                       final List<Vec2> roof) {
        this.tower = tower;
        this.pavilion = pavilion;
        this.at = at;
        this.plants = List.copyOf(plants);
        this.roof = List.copyOf(roof);
    }

    /**
     * A garden on {@code tower}'s roof, its pavilion of {@code kind} under a roof of {@code roofing},
     * tiled in {@code tiles} on pillars of {@code columns}: about half as wide as the roof, as big as
     * there is room for where there is most.
     */
    public static RoofGarden of(final Tower tower, final Pavilion.Footprint kind, final Pavilion.Roof roofing,
                                final Material tiles, final Material columns) {
        final List<Vec2> roof = tower.sections().getLast().plan().layout().shaped().getFirst().stream()
                .map(p -> new Vec2(p.x(), p.y())).toList();
        final double[] box = bounds(roof);
        // where there is most room: the point furthest from the roof's edge (the middle, if it ties)
        Vec2 best = null;
        double room = -1;
        final double cx = (box[0] + box[2]) / 2;
        final double cz = (box[1] + box[3]) / 2;
        for (double x = box[0]; x <= box[2]; x += 100) {
            for (double z = box[1]; z <= box[3]; z += 100) {
                if (!Outline.inside(roof, x, z)) {
                    continue;
                }
                final Vec2 edge = Outline.nearest(roof, x, z);
                final double clear = Math.hypot(edge.x() - x, edge.z() - z) - Math.hypot(x - cx, z - cz) * 1e-3;
                if (clear > room) {
                    room = clear;
                    best = new Vec2(x, z);
                }
            }
        }
        // about half the roof's width; smaller if it would not fit there
        double width = Math.min(box[2] - box[0], box[3] - box[1]) / 2;
        Pavilion pavilion = Pavilion.of(spec(tower.name(), kind, roofing, tiles, columns, width));
        while (pavilion.reach() > room - EDGE && width > 300) {
            width *= 0.92;
            pavilion = Pavilion.of(spec(tower.name(), kind, roofing, tiles, columns, width));
        }
        final Place at = new Place(best.x(), best.z(), tower.height() + LAWN, 0);
        return new RoofGarden(tower, pavilion, at, plant(tower, roof, best, pavilion.reach()), roof);
    }

    /** A pavilion of {@code kind}, {@code width} across, proportioned to it: its pillars, its roof, its eaves. */
    private static Pavilion.Spec spec(final String tower, final Pavilion.Footprint kind, final Pavilion.Roof roofing,
                                      final Material tiles, final Material columns, final double width) {
        final boolean rectangle = kind == Pavilion.Footprint.RECTANGLE;
        final double depth = rectangle ? width * 0.55 : width;
        final int bays = rectangle ? (width > 1600 ? 5 : 3) : 1;
        final double thick = Math.clamp(width / 24, 25, 60);
        final double tall = Math.clamp(width * 0.28, 280, 650);
        final double pitch = roofing == Pavilion.Roof.FLAT ? 0 : 0.75;
        final double upturn = roofing == Pavilion.Roof.PYRAMID || roofing == Pavilion.Roof.HIP ? width * 0.06 : 0;
        return new Pavilion.Spec(tower + "'s roof pavilion", kind, width, depth, bays, Pavilion.Pillar.ROUND, thick, tall,
                roofing, pitch, width * 0.14, 30, tiles, columns, upturn);
    }

    /**
     * Plants round it, as the roof has room for: tried at points a few metres apart, shaken a little,
     * each kept if it is on the roof clear of its edge, of the pavilion, and of the plants before it.
     * The same for the same tower, every time.
     */
    private static List<Plant> plant(final Tower tower, final List<Vec2> roof, final Vec2 middle, final double reach) {
        final Random random = new Random(tower.name().hashCode());
        final double[] box = bounds(roof);
        final List<Plant> plants = new ArrayList<>();
        for (double x = box[0] + 200; x < box[2]; x += 350) {
            for (double z = box[1] + 200; z < box[3]; z += 350) {
                final double px = x + (random.nextDouble() - 0.5) * 200;
                final double pz = z + (random.nextDouble() - 0.5) * 200;
                final double pick = random.nextDouble();
                final Kind kind = pick < 0.45 ? Kind.TREE : pick < 0.7 ? Kind.PINE : Kind.SHRUB;
                final double size = switch (kind) {
                    case TREE -> 110 + random.nextDouble() * 60;
                    case PINE -> 80 + random.nextDouble() * 30;
                    case SHRUB -> 50 + random.nextDouble() * 30;
                };
                if (!Outline.inside(roof, px, pz)) {
                    continue;
                }
                final Vec2 edge = Outline.nearest(roof, px, pz);
                if (Math.hypot(edge.x() - px, edge.z() - pz) < EDGE + size * 0.6) {
                    continue;
                }
                if (Math.hypot(px - middle.x(), pz - middle.z()) < reach + 150 + size) {
                    continue;
                }
                if (plants.stream().anyMatch(p -> Math.hypot(p.x() - px, p.z() - pz) < (p.size() + size) * 1.1)) {
                    continue;
                }
                plants.add(new Plant(kind, px, pz, size));
            }
        }
        return plants;
    }

    private static double[] bounds(final List<Vec2> outline) {
        return new double[] {outline.stream().mapToDouble(Vec2::x).min().orElseThrow(),
                outline.stream().mapToDouble(Vec2::z).min().orElseThrow(),
                outline.stream().mapToDouble(Vec2::x).max().orElseThrow(),
                outline.stream().mapToDouble(Vec2::z).max().orElseThrow()};
    }

    public Tower tower() {
        return tower;
    }

    public Pavilion pavilion() {
        return pavilion;
    }

    /** Where the pavilion stands, in the tower's frame: on the lawn on its roof. */
    public Place place() {
        return at;
    }

    public List<Plant> plants() {
        return plants;
    }

    /** The roof's top: the lawn's. */
    public double lawn() {
        return tower.height() + LAWN;
    }

    // ---------------------------------------------------------------- composed

    /** The tower, dressed as {@code tower} dresses it, with the garden on its roof. */
    public Building on(final Building tower) {
        final List<Building.Structure> structure = new ArrayList<>(tower.structure());
        structure.add((skeleton, palette) -> solids());
        return new Building(tower.name(), tower.skeleton(), tower.surfaces(), tower.plates(), tower.sheets(), structure,
                tower.choices(), tower.palettes());
    }

    /** The tower in the way as it is shaped, with the garden on its roof in the way as it is. */
    public List<Volume> on(final List<Volume> tower) {
        final List<Volume> all = new ArrayList<>(tower);
        all.addAll(shape());
        return all;
    }

    /** The garden's solids, in the tower's frame: its lawn, its pavilion dressed its own way, its plants. */
    public List<Solid> solids() {
        final List<Solid> solids = new ArrayList<>();
        final Palette lawn = Palette.STONE.with(Map.of(Role.DRESSING, Material.GRASS));
        solids.addAll(Sheet.flat("lawn", "lawn", roof.stream().map(p -> new Vec3(p.x(), p.z(), tower.height())).toList(),
                List.of(), new Vec3(0, 0, 1)).slab("lawn", lawn, Role.DRESSING, 0, LAWN));
        final Building building = pavilion.building();
        for (final Solid s : building.dressed(building.own(), building.palettes().getFirst())) {
            solids.add(new Solid("pavilion, " + s.name(), s.role(), s.material(), s.corners().stream()
                    .map(p -> new Point(p.across() + at.x(), p.along() + at.z(), p.up() + at.height())).toList()));
        }
        int k = 0;
        for (final Plant plant : plants) {
            solids.addAll(plant(plant, "plant " + k++));
        }
        return solids;
    }

    /** A plant drawn: its trunk, and its crown of leaves (a ball for a tree, a cone for a pine, a mound for a shrub). */
    private List<Solid> plant(final Plant plant, final String name) {
        final double base = lawn();
        final double r = plant.size();
        final List<Solid> solids = new ArrayList<>();
        switch (plant.kind()) {
            case TREE -> {
                final double trunk = r * 1.4;
                solids.addAll(frustum(name + ", trunk", Material.BARK, plant, r * 0.12, base, r * 0.1, base + trunk));
                solids.addAll(frustum(name + ", crown", Material.LEAVES, plant, r * 0.55, base + trunk - r * 0.2, r,
                        base + trunk + r * 0.5));
                solids.addAll(frustum(name + ", crown top", Material.LEAVES, plant, r, base + trunk + r * 0.5, r * 0.35,
                        base + trunk + r * 1.5));
            }
            case PINE -> {
                final double trunk = r * 0.8;
                solids.addAll(frustum(name + ", trunk", Material.BARK, plant, r * 0.12, base, r * 0.1, base + trunk));
                solids.addAll(frustum(name + ", crown", Material.LEAVES, plant, r, base + trunk, 2, base + trunk + r * 3.2));
            }
            case SHRUB -> solids.addAll(frustum(name + ", leaves", Material.LEAVES, plant, r * 0.8, base, r * 0.3,
                    base + r * 1.1));
        }
        return solids;
    }

    /** A frustum round the plant, from radius {@code r0} at {@code low} to {@code r1} at {@code high}: wedges from its middle. */
    private static List<Solid> frustum(final String name, final Material material, final Plant plant, final double r0,
                                       final double low, final double r1, final double high) {
        final List<Solid> solids = new ArrayList<>();
        final int sides = 8;
        for (int k = 0; k < sides; k++) {
            final double a = 2 * Math.PI * k / sides;
            final double b = 2 * Math.PI * (k + 1) / sides;
            final List<Point> corners = new ArrayList<>();
            for (int c = 0; c < 8; c++) {
                final boolean top = (c & 4) != 0;
                final double radius = top ? r1 : r0;
                final double angle = (c & 1) == 0 ? a : b;
                final double out = (c & 2) == 0 ? 0 : radius;
                corners.add(new Point(plant.x() + out * Math.cos(angle), plant.z() + out * Math.sin(angle),
                        top ? high : low));
            }
            solids.add(new Solid(name + " " + k, Role.DRESSING, material, corners));
        }
        return solids;
    }

    /**
     * The garden in the way as it is shaped, in the tower's frame: its lawn; its pavilion's parts on it;
     * each tree's and pine's trunk, each shrub. The crowns are not: flown through.
     */
    public List<Volume> shape() {
        final List<Volume> shape = new ArrayList<>();
        shape.add(new Prism(roof, tower.height(), lawn()));
        pavilion.shape().forEach(v -> shape.add(Volume.placed(v, at, 1)));
        for (final Plant plant : plants) {
            final double r = plant.kind() == Kind.SHRUB ? plant.size() * 0.7 : plant.size() * 0.12;
            final double high = plant.kind() == Kind.SHRUB ? plant.size() * 1.1
                    : plant.kind() == Kind.TREE ? plant.size() * 1.4 : plant.size() * 0.8;
            final List<Vec2> round = new ArrayList<>();
            for (int k = 0; k < 8; k++) {
                final double a = 2 * Math.PI * k / 8;
                round.add(new Vec2(plant.x() + r * Math.cos(a), plant.z() + r * Math.sin(a)));
            }
            shape.add(new Prism(round, lawn(), lawn() + high));
        }
        return shape;
    }
}
