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
 * dark see-through blue-grey (drawn last, so what is behind it shows), metal near black, the roof's
 * gravel darker, brick a red brown, stone a warm pale limestone. In the building's frame, standing
 * on the ground: across to the right is x, up is -y, along ahead is z.
 */
public final class BuildingModel {

    private static final Map<Material, PhongMaterial> LOOKS = new EnumMap<>(Material.class);

    static {
        LOOKS.put(Material.CONCRETE, BoxRig.matte("#a9a59b"));
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
        final PhongMaterial glass = new PhongMaterial(Color.web("#4f6f86", 0.55));
        glass.setSpecularColor(Color.web("#d8e6f0"));
        glass.setSpecularPower(40);
        LOOKS.put(Material.GLASS, glass);
    }

    private BuildingModel() {
    }

    /** How {@code material} is drawn. */
    public static PhongMaterial look(final Material material) {
        return LOOKS.get(material);
    }

    /** {@code solids}, drawn: a mesh for each material, glass last. */
    public static Group of(final List<Solid> solids) {
        final Map<Material, MeshBuilder> meshes = new EnumMap<>(Material.class);
        for (final Solid solid : solids) {
            final Point3D[] corners = new Point3D[8];
            for (int k = 0; k < 8; k++) {
                final Point p = solid.corners().get(k);
                corners[k] = new Point3D(p.across(), -p.up(), p.along());
            }
            meshes.computeIfAbsent(solid.material(), m -> new MeshBuilder()).box(corners);
        }
        final Group drawn = new Group();
        for (final Material material : Material.values()) {
            if (material != Material.GLASS && meshes.containsKey(material)) {
                drawn.getChildren().add(meshes.get(material).view(LOOKS.get(material)));
            }
        }
        if (meshes.containsKey(Material.GLASS)) {
            drawn.getChildren().add(meshes.get(Material.GLASS).view(LOOKS.get(Material.GLASS)));
        }
        return drawn;
    }
}
