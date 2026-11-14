package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.sim.Form;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.paint.PhongMaterial;

import java.util.EnumMap;
import java.util.Map;

/**
 * A {@link Form}, drawn as its skeleton: each edge a thin square rod from vertex to vertex, all of
 * them that lean alike in one mesh (upright, level or sloped, each in a colour of its own, so a
 * building's columns, floors and roofs read apart at a glance); and, if asked, a small cube at each
 * vertex. A building of thousands of edges is so drawn as a handful of meshes, not thousands of
 * shapes. In its owner's frame, standing on the ground: across to the right is x, up is -y (y points
 * down), along ahead is z.
 */
public final class FormModel {

    /** How an edge leans: straight up, level, or sloped between. */
    public enum Lean { UPRIGHT, LEVEL, SLOPED }

    /** Within this of straight up, or of level, an edge counts as that: about a twentieth of a degree. */
    private static final double SQUARE = 1e-3;

    private FormModel() {
    }

    /** How {@code edge} of {@code form} leans. */
    public static Lean lean(final Form form, final Form.Edge edge) {
        final Form.Vertex a = form.vertices().get(edge.from());
        final Form.Vertex b = form.vertices().get(edge.to());
        final double rise = Math.abs(b.up() - a.up()) / a.distance(b);
        return rise > 1 - SQUARE ? Lean.UPRIGHT : rise < SQUARE ? Lean.LEVEL : Lean.SLOPED;
    }

    /**
     * Its edges, {@code thickness} across, each in the material for how it leans; and its vertices,
     * if {@code vertices}, as cubes twice as big, in {@code dots}.
     */
    public static Group of(final Form form, final double thickness, final Map<Lean, PhongMaterial> materials,
                           final boolean vertices, final PhongMaterial dots) {
        final Map<Lean, MeshBuilder> meshes = new EnumMap<>(Lean.class);
        for (final Lean lean : Lean.values()) {
            meshes.put(lean, new MeshBuilder());
        }
        for (final Form.Edge edge : form.edges()) {
            meshes.get(lean(form, edge)).rod(point(form.vertices().get(edge.from())),
                    point(form.vertices().get(edge.to())), thickness);
        }
        final Group drawn = new Group();
        for (final Lean lean : Lean.values()) {
            if (!meshes.get(lean).empty()) {
                drawn.getChildren().add(meshes.get(lean).view(materials.get(lean)));
            }
        }
        if (vertices && !form.vertices().isEmpty()) {
            final MeshBuilder cubes = new MeshBuilder();
            for (final Form.Vertex v : form.vertices()) {
                cubes.cube(point(v), thickness * 2.2);
            }
            drawn.getChildren().add(cubes.view(dots));
        }
        return drawn;
    }

    /** Where {@code vertex} is, in the model's terms. */
    public static Point3D point(final Form.Vertex vertex) {
        return new Point3D(vertex.across(), -vertex.up(), vertex.along());
    }
}
