package com.example.fxsuite.avatar.bench.kit;

import com.example.fxsuite.avatar.build.Solid;
import com.example.fxsuite.avatar.look.BuildingModel;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;

import java.util.List;

/**
 * A bench's model view: a model, still, on level ground, to be looked at from any side. What it shows is only ever
 * {@link Model}s, each drawn once from its solids and never touched again: nothing in it moves, nothing in it is
 * redrawn but by {@link #show} putting other models in its place, and nothing about it changes from frame to frame but
 * where it is seen from. A bench that would see something move plays it ({@link Player}) in a window of its own.
 *
 * <p>Seen through an {@link Orbit}: dragged, it turns round what it looks at; right-dragged, moves across; scrolled,
 * nearer or further.
 */
public final class ModelView {

    /**
     * A model drawn, still: made from its solids anywhere (on a workshop's worker, so the screen waits on nothing),
     * shown by a view; nothing of it to reach but to show it.
     */
    public static final class Model {

        private final Node drawn;

        private Model(final Node drawn) {
            this.drawn = drawn;
        }

        /** {@code solids} drawn, each material's in one mesh. */
        public static Model of(final List<Solid> solids) {
            final Group drawn = BuildingModel.of(solids);
            // looked at, never picked: the view's dragging is all that answers the mouse
            drawn.setMouseTransparent(true);
            return new Model(drawn);
        }
    }

    private final Orbit orbit;
    private final Group ground = new Group();
    private final Group shown = new Group();

    /** A view as the roads bench has had it ({@link Orbit#standard}). */
    public ModelView() {
        this(Orbit.standard());
    }

    /** A view seen through {@code orbit}, its sky and lights; what it shows is only ever this view's. */
    public ModelView(final Orbit orbit) {
        this.orbit = orbit;
        ground.setMouseTransparent(true);
        orbit.add(ground, shown);
        ground(0, 0, 0, 20_000);
    }

    /** What to put in the window: the view, filling what holds it. */
    public Pane node() {
        return orbit.node();
    }

    /** {@code models} shown in place of whatever was. */
    public void show(final Model... models) {
        show(List.of(models));
    }

    /** {@code models} shown in place of whatever was. */
    public void show(final List<Model> models) {
        shown.getChildren().setAll(models.stream().map(m -> m.drawn).toList());
    }

    /** Level ground, grass, {@code size} cm square, its middle at ({@code x}, {@code z}), its top at {@code y} up. */
    public void ground(final double x, final double y, final double z, final double size) {
        final Box grass = new Box(size, 2, size);
        grass.setTranslateX(x);
        grass.setTranslateY(-y + 1.5);
        grass.setTranslateZ(z);
        final PhongMaterial look = new PhongMaterial(Color.web("#9fae8f"));
        look.setSpecularColor(Color.web("#3a3f46"));
        grass.setMaterial(look);
        ground.getChildren().setAll(grass);
    }

    /** Turned round ({@code x}, {@code y}, {@code z}), in the game's frame (y up). */
    public void lookAt(final double x, final double y, final double z) {
        orbit.lookAt(x, -y, z);
    }

    /** Seen from as {@code prefix}'s properties say ({@link Orbit#from}), or else as given: degrees, degrees down, m. */
    public void from(final String prefix, final double yawDegrees, final double pitchDegrees, final double metres) {
        orbit.from(prefix, yawDegrees, pitchDegrees, metres);
    }

    /** Seen from {@code yawDegrees} round, {@code pitchDegrees} down. */
    public void turned(final double yawDegrees, final double pitchDegrees) {
        orbit.turned(yawDegrees, pitchDegrees);
    }

    /** Seen from {@code cm} off. */
    public void distance(final double cm) {
        orbit.distance(cm);
    }

    /** Scrolled no nearer than {@code nearestCm}, no further than {@code furthestCm}. */
    public void zoom(final double nearestCm, final double furthestCm) {
        orbit.zoom(nearestCm, furthestCm);
    }
}
