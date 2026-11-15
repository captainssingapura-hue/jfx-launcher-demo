package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Place;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A temple's courtyard, as a Chinese temple is laid out round one: at its back, on its middle line,
 * the great hall, nine bays across on a high platform, facing down the courtyard to the start; before
 * it, a smaller hall each side, five bays across, facing in across the courtyard to each other, their
 * roofs green where the great hall's are yellow. Each is one piece in the world
 * ({@link Course.Kind#BUILDING}), as the town's buildings are ({@link Town#building}). Its gate, at
 * the courtyard's front, is still to come: a building to pass through.
 */
public final class TempleYard {

    private TempleYard() {
    }

    /** A hall: what it is called, how big it is built, its roof's tiles, where it stands and which way it faces. */
    public record Hall(String name, Temple.Size size, String tiles, Place place) {
    }

    /** Its halls' keys, to draw them by, start here: past the town's. */
    public static final int KEYS = 100;

    /** The great hall: nine bays across, its middle 7.2 m, five deep, its columns 6 m on a 2.4 m platform. */
    public static final Temple.Size GREAT = new Temple.Size(9, 720, 5, 600, 240);
    /** Each side hall: five bays across, its middle 5 m, three deep, its columns 4 m on a 0.9 m platform. */
    public static final Temple.Size SIDE = new Temple.Size(5, 500, 3, 400, 90);

    /** Its halls: the great hall at the back, facing the start; the side halls either side before it, facing in. */
    public static final List<Hall> HALLS = List.of(
            new Hall("great hall", GREAT, "Yellow glazed", new Place(0, 6500, 0, 180)),
            new Hall("west hall", SIDE, "Green glazed", new Place(-3300, 2800, 0, 90)),
            new Hall("east hall", SIDE, "Green glazed", new Place(3300, 2800, 0, -90)));

    /** Its wall's units' keys, to draw them by, start here: past its halls'. */
    public static final int WALL_KEYS = 200;
    /** How long, at most, each unit of its wall: what one blow can bring down. */
    public static final double UNIT = 600;
    /** Its wall round it: its front, its sides, its back; and the openings in it, front and back. */
    public static final double FRONT = -800;
    public static final double BACK = 9000;
    public static final double FLANK = 4700;
    /** The back's corners rounded so far in. */
    public static final double ROUNDED = 800;
    /** The main entrance, on its middle line at its front, so wide; and the back door, behind the great hall. */
    public static final double ENTRANCE = 900;
    public static final double BACK_DOOR = 180;

    /**
     * Its wall: a red wall on a stone plinth, its coping green-tiled, round the courtyard and its halls;
     * the front open on its middle line for the main entrance (its gate still to come), and a small
     * door in the back behind the great hall; the back's corners rounded. In units of 6 m at most.
     */
    public static final Wall WALL = new Wall("temple wall", Wall.Style.TEMPLE, enclosure());

    private static List<Wall.Unit> enclosure() {
        final List<Wall.Unit> units = new ArrayList<>();
        final double gate = ENTRANCE / 2;
        final double door = BACK_DOOR / 2;
        final double turn = BACK - ROUNDED;
        units.addAll(Wall.run(at(-FLANK, FRONT), at(-gate, FRONT), UNIT));
        units.addAll(Wall.run(at(gate, FRONT), at(FLANK, FRONT), UNIT));
        units.addAll(Wall.run(at(-FLANK, FRONT), at(-FLANK, turn), UNIT));
        units.addAll(Wall.arc(at(-FLANK + ROUNDED, turn), ROUNDED, 270, 360, UNIT));
        units.addAll(Wall.run(at(-FLANK + ROUNDED, BACK), at(-door, BACK), UNIT));
        units.addAll(Wall.run(at(door, BACK), at(FLANK - ROUNDED, BACK), UNIT));
        units.addAll(Wall.arc(at(FLANK - ROUNDED, turn), ROUNDED, 0, 90, UNIT));
        units.addAll(Wall.run(at(FLANK, turn), at(FLANK, FRONT), UNIT));
        return units;
    }

    private static Point at(final double x, final double z) {
        return new Point(x, z, 0);
    }

    /** Its wall's palette: red plaster on stone, its coping's tiles green, as the side halls' roofs are. */
    private static final Palette WALLED = Temple.LACQUERED.with(Map.of(Role.ROOF, Material.GREEN_GLAZED));

    private static final Map<Integer, List<Solid>> WALL_DRAWN = new ConcurrentHashMap<>();

    private static final Map<Integer, List<Solid>> DRESSED = new ConcurrentHashMap<>();

    /** Hall {@code k}, dressed: red lacquer, its roof's tiles as it says. Worked out once, when first wanted. */
    public static List<Solid> solids(final int k) {
        return DRESSED.computeIfAbsent(k, i -> {
            final Hall hall = HALLS.get(i);
            final Building building = Temple.of(hall.size()).building();
            return building.dressed(List.of(building.option("Roof tiles", hall.tiles())), Temple.LACQUERED);
        });
    }

    /** What stands in the courtyard: its halls, each one piece; and round it its wall, each unit a piece. */
    public static List<Course.Piece> pieces() {
        final List<Course.Piece> pieces = new ArrayList<>();
        for (int k = 0; k < HALLS.size(); k++) {
            pieces.add(Town.building(solids(k), HALLS.get(k).place(), KEYS + k));
        }
        for (int u = 0; u < WALL.units().size(); u++) {
            pieces.add(WALL.piece(u, WALL_KEYS + u));
        }
        return pieces;
    }

    /** What building {@code key} is dressed as: a unit of the courtyard's wall, a hall of it, or one of the town's. */
    public static List<Solid> drawn(final int key) {
        if (key >= WALL_KEYS) {
            return WALL_DRAWN.computeIfAbsent(key - WALL_KEYS, u -> WALL.drawn(u, WALLED));
        }
        return key >= KEYS ? solids(key - KEYS) : Town.solids(key);
    }
}
