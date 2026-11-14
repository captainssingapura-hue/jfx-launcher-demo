package com.example.fxsuite.avatar.build;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * What a building is made of: for each role a part can have ({@link Role}), its material; for its
 * walls and its base, how thick they are; and, if its base is stone, the height of its courses, each
 * laid proud of the joints between them (rusticated). Glass, frames, the structure that shows and
 * the roof's covering are the same in every palette here: concrete, glass, dark metal and gravel.
 *
 * @param name        what it is called
 * @param materials   by role
 * @param wall        how thick its walls are, in centimetres
 * @param base        how thick its base is
 * @param rustication how high its base's courses are (0 for none)
 */
public record Palette(String name, Map<Role, Material> materials, double wall, double base, double rustication) {

    public Palette {
        final EnumMap<Role, Material> all = new EnumMap<>(Role.class);
        all.put(Role.STRUCTURE, Material.CONCRETE);
        all.put(Role.ROOF, Material.GRAVEL);
        all.put(Role.GLAZING, Material.GLASS);
        all.put(Role.FRAME, Material.METAL);
        all.putAll(materials);
        for (final Role role : Role.values()) {
            if (!all.containsKey(role)) {
                throw new IllegalArgumentException(name + " says nothing of what its " + role + " is made of");
            }
        }
        materials = Map.copyOf(all);
    }

    /** What {@code role} is made of. */
    public Material of(final Role role) {
        return materials.get(role);
    }

    /** How thick a wall of {@code role} is: its base's, or its walls'. */
    public double thickness(final Role role) {
        return role == Role.BASE ? base : wall;
    }

    private static Map<Role, Material> walls(final Material wall, final Material base, final Material dressing) {
        return Map.of(Role.WALL, wall, Role.BASE, base, Role.DRESSING, dressing);
    }

    /** Brutalism's own: concrete cast in place, its boards' marks on it, all of it. */
    public static final Palette CONCRETE = new Palette("Board-marked concrete",
            walls(Material.CONCRETE, Material.CONCRETE, Material.CONCRETE), 30, 30, 0);
    /** Precast concrete panels, paler and smoother, thicker; on a cast concrete base. */
    public static final Palette PRECAST = new Palette("Precast panels",
            walls(Material.PRECAST, Material.CONCRETE, Material.PRECAST), 40, 30, 0);
    /** Brick walls, dressed in stone, on a rusticated stone base. */
    public static final Palette BRICK = new Palette("Brick, stone dressings, stone base",
            walls(Material.BRICK, Material.STONE, Material.STONE), 35, 50, 40);
    /** Ashlar stone, thick, on a deeper rusticated base of it. */
    public static final Palette STONE = new Palette("Ashlar stone",
            walls(Material.STONE, Material.STONE, Material.STONE), 50, 60, 45);

    /** All of them, concrete first. */
    public static final List<Palette> ALL = List.of(CONCRETE, PRECAST, BRICK, STONE);
}
