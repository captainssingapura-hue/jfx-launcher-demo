package com.example.fxsuite.avatar.air;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The colours an aircraft is painted in: one for each {@link Paint}, as {@code #rrggbb}.
 *
 * @param name    what it is called
 * @param colours each paint's colour
 */
public record Livery(String name, Map<Paint, String> colours) {

    public Livery {
        colours = Map.copyOf(colours);
        for (final Paint paint : Paint.values()) {
            if (!colours.containsKey(paint)) {
                throw new IllegalArgumentException(name + ": no colour for " + paint);
            }
        }
    }

    /** The colour of {@code paint}. */
    public String colour(final Paint paint) {
        return colours.get(paint);
    }

    private static Livery of(final String name, final String... colours) {
        final Map<Paint, String> each = new EnumMap<>(Paint.class);
        for (int k = 0; k < Paint.values().length; k++) {
            each.put(Paint.values()[k], colours[k]);
        }
        return new Livery(name, each);
    }

    /** Silver doped fabric, grey frames, a dark car, gold bands: a patrol airship's. */
    public static final Livery PATROL = of("Patrol", "#c9ccd1", "#8e949c", "#3b4148", "#2e4a63", "#555b63",
            "#202428", "#b9bdc4", "#c9a227");
    /** Olive drab, its frames darker, a gunmetal car, red bands: a heavy bomber's. */
    public static final Livery BOMBER = of("Bomber", "#565b4c", "#3c4036", "#2b2e30", "#1d2b33", "#3d4146",
            "#17191b", "#4a4f42", "#8e1f1c");
    /** White, red bands and wings: a rescue craft's, to be seen. */
    public static final Livery RESCUE = of("Rescue", "#eeeeea", "#c8c8c2", "#d22d2d", "#24425c", "#6a7078",
            "#1c1f22", "#d22d2d", "#f0b323");
    /** Matt grey all over, black rotors: a drone's. */
    public static final Livery GREY = of("Grey", "#80868d", "#5d6369", "#4a5056", "#141a20", "#3e4349",
            "#121417", "#6c7278", "#e07b22");

    /** Olive drab, darker panels and frames, yellow markings: a military machine's. */
    public static final Livery OLIVE = of("Olive drab", "#4b5235", "#3a3f2a", "#3f4430", "#1b2a30", "#4a4d4f",
            "#16181a", "#454b31", "#c9b04a");
    /** Sand and tan, dark red markings: a desert machine's. */
    public static final Livery DESERT = of("Desert", "#b59a6a", "#9a8156", "#a48b5e", "#1d2a33", "#5a5650",
            "#1b1b1a", "#a8905f", "#6b2f22");
    /** Dark grey metal all over, orange markings. */
    public static final Livery GUNMETAL = of("Gunmetal", "#4d545b", "#3a4046", "#3f454b", "#1a2630", "#5a6067",
            "#141618", "#474e55", "#d48a2a");

    /** All of them. */
    public static List<Livery> all() {
        return List.of(PATROL, BOMBER, RESCUE, GREY, OLIVE, DESERT, GUNMETAL);
    }
}
