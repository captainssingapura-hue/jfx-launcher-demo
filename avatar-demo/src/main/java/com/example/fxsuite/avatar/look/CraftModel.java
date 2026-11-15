package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.air.Craft;
import com.example.fxsuite.avatar.air.Hinge;
import com.example.fxsuite.avatar.air.Livery;
import com.example.fxsuite.avatar.air.Paint;
import com.example.fxsuite.avatar.air.Piece;
import com.example.fxsuite.avatar.air.Skin;
import com.example.fxsuite.avatar.air.Spinner;
import com.example.fxsuite.avatar.build.Point;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.transform.Rotate;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * An aircraft ({@link Craft}) as drawn, in a livery ({@link Livery}): its skins smooth, its
 * pieces flat face by face, each paint one mesh; what turns ({@link Spinner}) apart, each turning
 * about its own axis ({@link #turn}). Its frame as the aircraft's: across to x, up to -y, along to z.
 */
public final class CraftModel {

    private final Group root = new Group();
    private final List<Rotate> turns = new ArrayList<>();
    private final List<Double> rates = new ArrayList<>();
    private int triangles;

    /** Each control's hinges: how each is turned, and which way. */
    private final Map<String, List<Rotate>> hinges = new java.util.LinkedHashMap<>();
    private final Map<Rotate, Double> senses = new java.util.HashMap<>();

    /** Whether it is drawn dressed (its skin, its parts); its lines' skeletons, and how thick they are drawn. */
    private final boolean dressed;
    private final boolean lines;
    private final double thickness;

    public CraftModel(final Craft craft, final Livery livery) {
        this(craft, livery, true, false, 0);
    }

    /**
     * {@code craft} in {@code livery}: dressed, if {@code dressed}; and its lines' skeletons, if
     * {@code skeleton}, each edge a rod {@code thickness} across, in a colour for how it leans (as a
     * building's skeleton is drawn).
     */
    public CraftModel(final Craft craft, final Livery livery, final boolean dressed, final boolean skeleton,
                      final double thickness) {
        this.dressed = dressed;
        this.lines = skeleton;
        this.thickness = thickness;
        root.getChildren().add(drawn(craft, livery));
    }

    /** How a skeleton's edges are coloured, by how they lean: as a building's. */
    private static final Map<FormModel.Lean, PhongMaterial> LEANS = Map.of(
            FormModel.Lean.UPRIGHT, BoxRig.matte("#2f6fb3"), FormModel.Lean.LEVEL, BoxRig.matte("#3b3f45"),
            FormModel.Lean.SLOPED, BoxRig.matte("#c26a1b"));

    /** {@code craft} drawn: its skins and pieces; what turns, each about its axis; what is hinged, each on its hinge, and so on. */
    private Group drawn(final Craft craft, final Livery livery) {
        final Group group = new Group();
        if (dressed) {
            group.getChildren().add(meshes(craft.skins(), craft.pieces(), craft.panels(), livery));
        }
        if (lines) {
            for (final com.example.fxsuite.avatar.sim.Form skeleton : craft.skeletons()) {
                group.getChildren().add(FormModel.of(skeleton, thickness, LEANS, false, BoxRig.matte("#d8323a")));
            }
        }
        for (final Spinner spinner : craft.spinners()) {
            final Group turning = dressed ? meshes(spinner.skins(), spinner.pieces(), List.of(), livery) : new Group();
            final Rotate turn = about(spinner.hub(), spinner.axis());
            turning.getTransforms().add(turn);
            turns.add(turn);
            rates.add(spinner.rate());
            group.getChildren().add(turning);
        }
        for (final Hinge hinge : craft.hinges()) {
            final Group hinged = drawn(hinge.part(), livery);
            final Rotate turn = about(hinge.pivot(), hinge.axis());
            hinged.getTransforms().add(turn);
            hinges.computeIfAbsent(hinge.control(), c -> new ArrayList<>()).add(turn);
            senses.put(turn, hinge.sense());
            group.getChildren().add(hinged);
        }
        return group;
    }

    /** A turn about {@code axis} through {@code point}, in the drawing's frame: the right-hand way, as the aircraft's. */
    private static Rotate about(final Point point, final Point axis) {
        final Point3D at = fx(point);
        return new Rotate(0, at.getX(), at.getY(), at.getZ(), new Point3D(axis.across(), -axis.up(), axis.along()));
    }

    /** The controls its hinges are turned by. */
    public java.util.Set<String> controls() {
        return hinges.keySet();
    }

    /** Turns the hinges of {@code control} to {@code degrees}. */
    public void set(final String control, final double degrees) {
        for (final Rotate turn : hinges.getOrDefault(control, List.of())) {
            turn.setAngle(degrees * senses.get(turn));
        }
    }

    public Node node() {
        return root;
    }

    /** How many triangles it is drawn in. */
    public int triangles() {
        return triangles;
    }

    /** Turns what turns {@code seconds} on, at {@code power} (0 to 1) of its full rate. */
    public void turn(final double seconds, final double power) {
        for (int k = 0; k < turns.size(); k++) {
            final Rotate turn = turns.get(k);
            turn.setAngle((turn.getAngle() + rates.get(k) * 360 * seconds * power) % 360);
        }
    }

    private Group meshes(final List<Skin> skins, final List<Piece> pieces,
                         final List<com.example.fxsuite.avatar.air.Panel> panels, final Livery livery) {
        final Map<Paint, MeshBuilder> meshes = new EnumMap<>(Paint.class);
        for (final Skin skin : skins) {
            meshes.computeIfAbsent(skin.paint(), p -> new MeshBuilder())
                    .skin(skin.rings().stream().map(r -> r.stream().map(CraftModel::fx).toArray(Point3D[]::new)).toList(),
                            skin.faceted());
        }
        for (final Piece piece : pieces) {
            meshes.computeIfAbsent(piece.paint(), p -> new MeshBuilder())
                    .box(piece.corners().stream().map(CraftModel::fx).toArray(Point3D[]::new));
        }
        for (final com.example.fxsuite.avatar.air.Panel panel : panels) {
            meshes.computeIfAbsent(panel.paint(), p -> new MeshBuilder())
                    .face(panel.corners().stream().map(CraftModel::fx).toArray(Point3D[]::new));
        }
        final Group drawn = new Group();
        // glass last, so it is drawn over what is behind it
        for (final Paint paint : Paint.values()) {
            if (paint != Paint.GLASS && meshes.containsKey(paint)) {
                add(drawn, meshes.get(paint), paint, livery);
            }
        }
        if (meshes.containsKey(Paint.GLASS)) {
            add(drawn, meshes.get(Paint.GLASS), Paint.GLASS, livery);
        }
        return drawn;
    }

    private void add(final Group drawn, final MeshBuilder mesh, final Paint paint, final Livery livery) {
        final javafx.scene.shape.MeshView view = mesh.view(material(paint, livery.colour(paint)));
        triangles += ((javafx.scene.shape.TriangleMesh) view.getMesh()).getFaces().size() / 6;
        drawn.getChildren().add(view);
    }

    /** A paint's colour, with a sheen: glass and metal shine, fabric and paint less so. */
    private static PhongMaterial material(final Paint paint, final String colour) {
        final PhongMaterial material = new PhongMaterial(Color.web(colour));
        switch (paint) {
            case GLASS -> {
                material.setSpecularColor(Color.web("#e8f2ff"));
                material.setSpecularPower(60);
            }
            case METAL, TRIM -> {
                material.setSpecularColor(Color.web("#9aa3ad"));
                material.setSpecularPower(28);
            }
            default -> {
                material.setSpecularColor(Color.web("#4a4f56"));
                material.setSpecularPower(18);
            }
        }
        return material;
    }

    private static Point3D fx(final Point p) {
        return new Point3D(p.across(), -p.up(), p.along());
    }
}
