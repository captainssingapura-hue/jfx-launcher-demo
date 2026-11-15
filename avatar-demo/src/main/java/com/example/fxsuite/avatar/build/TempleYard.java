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

    private static final Map<Integer, List<Solid>> DRESSED = new ConcurrentHashMap<>();

    /** Hall {@code k}, dressed: red lacquer, its roof's tiles as it says. Worked out once, when first wanted. */
    public static List<Solid> solids(final int k) {
        return DRESSED.computeIfAbsent(k, i -> {
            final Hall hall = HALLS.get(i);
            final Building building = Temple.of(hall.size()).building();
            return building.dressed(List.of(building.option("Roof tiles", hall.tiles())), Temple.LACQUERED);
        });
    }

    /** What stands in the courtyard: its halls, each one piece. */
    public static List<Course.Piece> pieces() {
        final List<Course.Piece> pieces = new ArrayList<>();
        for (int k = 0; k < HALLS.size(); k++) {
            pieces.add(Town.building(solids(k), HALLS.get(k).place(), KEYS + k));
        }
        return pieces;
    }

    /** What building {@code key} is dressed as: a hall of the courtyard's, or one of the town's. */
    public static List<Solid> drawn(final int key) {
        return key >= KEYS ? solids(key - KEYS) : Town.solids(key);
    }
}
