package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.build.Material;
import com.example.fxsuite.avatar.build.Point;
import com.example.fxsuite.avatar.build.Solid;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A building as dressed ({@code build.Building#dressed}), drawn: its solids, all those of a material
 * in one mesh, in that material's colour: board-marked concrete warm grey, precast paler, glass a
 * sky-tinted blue-grey, whole by day (drawn last, to show what is behind it come night), metal near black, the roof's
 * gravel darker, brick a red brown, stone a warm pale limestone. In the building's frame, standing
 * on the ground: across to the right is x, up is -y, along ahead is z.
 */
public final class BuildingModel {

    private static final Map<Material, PhongMaterial> LOOKS = new EnumMap<>(Material.class);

    static {
        LOOKS.put(Material.CONCRETE, BoxRig.matte("#a9a59b"));
        LOOKS.put(Material.SANDSTONE, BoxRig.matte("#d2b07a"));
        LOOKS.put(Material.LIMESTONE, BoxRig.matte("#e6dfcc"));
        LOOKS.put(Material.GRASS, BoxRig.matte("#6f9a4e"));
        LOOKS.put(Material.LEAVES, BoxRig.matte("#3d7a3c"));
        LOOKS.put(Material.BARK, BoxRig.matte("#5b4330"));
        // earth: mud plaster a warm grey, red earth, a pale clay, sod darker, rammed earth an ochre; turf, thatch, reed
        LOOKS.put(Material.MUD, BoxRig.matte("#a89c86"));
        LOOKS.put(Material.RED_EARTH, BoxRig.matte("#a3654b"));
        LOOKS.put(Material.PALE_CLAY, BoxRig.matte("#d6c7a6"));
        LOOKS.put(Material.SOD, BoxRig.matte("#6d604c"));
        LOOKS.put(Material.RAMMED_EARTH, BoxRig.matte("#b48a5a"));
        LOOKS.put(Material.TURF, BoxRig.matte("#7a7650"));
        LOOKS.put(Material.THATCH, BoxRig.matte("#b59a5f"));
        LOOKS.put(Material.REED, BoxRig.matte("#9a8d68"));
        LOOKS.put(Material.EARTH, BoxRig.matte("#8a7557"));
        // as the roads' own surface is drawn on the land ({@link TerrainModel#ASPHALT})
        LOOKS.put(Material.ASPHALT, BoxRig.matte("#5f6566"));
        LOOKS.put(Material.PRECAST, BoxRig.matte("#cfcac0"));
        LOOKS.put(Material.METAL, BoxRig.matte("#33373c"));
        LOOKS.put(Material.GRAVEL, BoxRig.matte("#7f7c75"));
        LOOKS.put(Material.BRICK, BoxRig.matte("#9a5844"));
        LOOKS.put(Material.STONE, BoxRig.matte("#cbbd9b"));
        LOOKS.put(Material.SLATE, BoxRig.matte("#4a5058"));
        LOOKS.put(Material.TILE, BoxRig.matte("#a4553a"));
        LOOKS.put(Material.COPPER, BoxRig.matte("#5e9e86"));
        LOOKS.put(Material.GLAZED, BoxRig.matte("#d9a520"));
        LOOKS.put(Material.GREEN_GLAZED, BoxRig.matte("#3f8a5a"));
        LOOKS.put(Material.GREY_TILE, BoxRig.matte("#5d6166"));
        LOOKS.put(Material.LACQUER, BoxRig.matte("#a3241f"));
        LOOKS.put(Material.PAINTED, BoxRig.matte("#2f7d78"));
        LOOKS.put(Material.TIMBER, BoxRig.matte("#4a3326"));
        LOOKS.put(Material.PLASTER, BoxRig.matte("#b2453a"));
        // glass by day: the sky it gives back, not what is behind it; till there is night, when what is lit inside
        // shows through, it is drawn so, whole
        final PhongMaterial glass = new PhongMaterial(Color.web("#5f819b"));
        glass.setSpecularColor(Color.web("#e2ecf4"));
        glass.setSpecularPower(32);
        LOOKS.put(Material.GLASS, glass);
        // water as the rivers' and the ponds' is: the same look, lit as they are
        LOOKS.put(Material.WATER, WaterModel.look(Horizon.lighting(World.AMBIENT, World.SUNLIGHT, World.SUN)));
        LOOKS.put(Material.POOL_TILE, BoxRig.matte("#8fd0dc"));
    }

    private BuildingModel() {
    }

    /** How {@code material} is drawn. */
    public static PhongMaterial look(final Material material) {
        return LOOKS.get(material);
    }

    /** {@code solids}, drawn, all of them: a mesh for each material, glass last. */
    public static Group of(final List<Solid> solids) {
        return levels(solids).node();
    }

    /**
     * A building drawn in its levels ({@link Detail}): {@code node}, all of it; in it, a group for each
     * level, to be shown or hidden by how far off it is seen; and, last, the glass and the water of its
     * body, always shown, drawn after all else so what is behind them shows.
     */
    public record Leveled(Group node, Map<Detail.Level, Group> levels) {
    }

    /** {@code solids}, drawn in their levels: in each, a mesh for each material, glass last. */
    public static Leveled levels(final List<Solid> solids) {
        final Map<Detail.Level, List<Solid>> split = Detail.split(solids);
        final Map<Detail.Level, Group> levels = new EnumMap<>(Detail.Level.class);
        final Group node = new Group();
        Group seeThrough = null;
        for (final Detail.Level level : Detail.Level.values()) {
            final Group[] drawn = drawn(split.get(level));
            levels.put(level, drawn[0]);
            node.getChildren().add(drawn[0]);
            if (level == Detail.Level.BODY) {
                seeThrough = drawn[1];
            } else {
                drawn[0].getChildren().add(drawn[1]);
            }
        }
        node.getChildren().add(seeThrough);
        return new Leveled(node, levels);
    }

    /** {@code solids}, drawn: a mesh for each material, apart from what is see-through (glass, water), apart. */
    private static Group[] drawn(final List<Solid> solids) {
        final Map<Material, MeshBuilder> meshes = new EnumMap<>(Material.class);
        for (final Solid solid : solids) {
            final Point3D[] corners = new Point3D[8];
            for (int k = 0; k < 8; k++) {
                final Point p = solid.corners().get(k);
                corners[k] = new Point3D(p.across(), -p.up(), p.along());
            }
            meshes.computeIfAbsent(solid.material(), m -> new MeshBuilder()).box(corners);
        }
        final Group solid = new Group();
        final Group seeThrough = new Group();
        for (final Material material : Material.values()) {
            if (material != Material.GLASS && material != Material.WATER && meshes.containsKey(material)) {
                solid.getChildren().add(meshes.get(material).view(LOOKS.get(material)));
            }
        }
        for (final Material material : new Material[] {Material.GLASS, Material.WATER}) {
            if (meshes.containsKey(material)) {
                seeThrough.getChildren().add(meshes.get(material).view(LOOKS.get(material)));
            }
        }
        return new Group[] {solid, seeThrough};
    }
}
