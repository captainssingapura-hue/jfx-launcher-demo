package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Place;
import com.example.fxsuite.avatar.sim.Volume;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A garden of pavilions ({@link Pavilion}), one of each kind, round a lawn, each facing in to its
 * middle, where the avatar starts: a square one to a point, a hexagonal and an octagonal one, a
 * round one, a long gallery under a hip roof, one under a gable, and a modern one with a flat roof
 * on steel posts. Each one piece in the world ({@link Course.Kind#BUILDING}), in the way only where
 * its platform, its pillars, its beams and its roof are: walked into and stood under.
 */
public final class Pavilions {

    private Pavilions() {
    }

    /** Their keys, to draw them by, start here: past the pyramids'. */
    public static final int KEYS = 400;
    /** How far round the lawn they stand from its middle. */
    public static final double ROUND = 3200;

    /** One of each kind. */
    public static final List<Pavilion.Spec> PRESETS = List.of(
            new Pavilion.Spec("square pavilion", Pavilion.Footprint.SQUARE, 500, 500, 1, Pavilion.Pillar.ROUND, 30, 320,
                    Pavilion.Roof.PYRAMID, 0.75, 110, 45, Material.GLAZED, Material.LACQUER, 60),
            new Pavilion.Spec("hexagonal pavilion", Pavilion.Footprint.HEXAGON, 600, 600, 1, Pavilion.Pillar.ROUND, 30, 330,
                    Pavilion.Roof.PYRAMID, 0.85, 110, 45, Material.GREEN_GLAZED, Material.LACQUER, 70),
            new Pavilion.Spec("octagonal pavilion", Pavilion.Footprint.OCTAGON, 700, 700, 1, Pavilion.Pillar.SQUARE, 34, 340,
                    Pavilion.Roof.PYRAMID, 0.7, 100, 60, Material.GREY_TILE, Material.STONE, 50),
            new Pavilion.Spec("round pavilion", Pavilion.Footprint.ROUND, 600, 600, 1, Pavilion.Pillar.ROUND, 28, 300,
                    Pavilion.Roof.PYRAMID, 0.9, 90, 30, Material.TIMBER, Material.TIMBER, 30),
            new Pavilion.Spec("long gallery", Pavilion.Footprint.RECTANGLE, 1250, 400, 5, Pavilion.Pillar.ROUND, 30, 320,
                    Pavilion.Roof.HIP, 0.7, 100, 45, Material.GREEN_GLAZED, Material.LACQUER, 70),
            new Pavilion.Spec("gable pavilion", Pavilion.Footprint.RECTANGLE, 900, 500, 3, Pavilion.Pillar.SQUARE, 30, 310,
                    Pavilion.Roof.GABLE, 0.6, 90, 30, Material.GREY_TILE, Material.TIMBER),
            new Pavilion.Spec("modern pavilion", Pavilion.Footprint.RECTANGLE, 900, 600, 3, Pavilion.Pillar.SQUARE, 20, 330,
                    Pavilion.Roof.FLAT, 0, 120, 15, Material.CONCRETE, Material.METAL));

    /** Where pavilion {@code k} stands: round the lawn's far side, facing in to its middle. */
    public static Place place(final int k) {
        final double heading = Math.toRadians(-90 + 180.0 * k / (PRESETS.size() - 1));
        final double x = ROUND * Math.sin(heading);
        final double z = ROUND * Math.cos(heading) + ROUND / 2;
        return new Place(x, z, 0, Math.toDegrees(Math.atan2(-x, -(z - ROUND / 2))));
    }

    private static final Map<Integer, List<Solid>> DRESSED = new ConcurrentHashMap<>();

    /** Pavilion {@code k}, dressed its own way: worked out once, when first wanted. */
    public static List<Solid> solids(final int k) {
        return DRESSED.computeIfAbsent(k, i -> {
            final Building building = Pavilion.of(PRESETS.get(i)).building();
            return building.dressed(building.own(), building.palettes().getFirst());
        });
    }

    /** What stands in the garden: each pavilion, one piece, in the way as it is shaped. */
    public static List<Course.Piece> pieces() {
        final List<Course.Piece> pieces = new ArrayList<>();
        for (int k = 0; k < PRESETS.size(); k++) {
            final List<Volume> shape = Pavilion.of(PRESETS.get(k)).shape();
            pieces.add(Town.building(solids(k), place(k), KEYS + k, shape, 1));
        }
        return pieces;
    }
}
