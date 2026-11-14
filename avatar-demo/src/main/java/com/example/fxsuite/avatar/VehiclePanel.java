package com.example.fxsuite.avatar;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

/**
 * A small panel of a vehicle's controls, low on the left: one button for each thing that can be
 * done at the controls of what the avatar has taken, each with the key that does the same, so that
 * no one has to remember them. Its buttons are clicked with the mouse freed (Esc); held in the
 * world, the mouse aims and fires as ever. Seen only at a vehicle's controls, or a gun's.
 *
 * <p>It does nothing itself: a button pressed is handed to whoever shows it, as the
 * {@link Control} it is, to do just as its key would.
 */
public final class VehiclePanel {

    /** What a button does. */
    public enum Control {
        /** Take control of the vehicle's gun, the mouse aiming it and the keys driving, or give it up. */
        GUN,
        /** Let one bomb go. */
        BOMB,
        /** The view from the driver's eyes, or from behind again. */
        FIRST_PERSON,
        /** The camera put back behind. */
        BEHIND,
        /** Let go of the controls: get out, step away, or step out through the hatch. */
        LEAVE
    }

    private static final String PANEL_STYLE = "-fx-background-color: rgba(20, 24, 30, 0.62); -fx-background-radius: 6;";
    private static final String TITLE_STYLE = "-fx-text-fill: #f4f6f8; -fx-font-size: 13px; -fx-font-weight: bold;";
    private static final String BUTTON_STYLE = "-fx-background-color: rgba(255, 255, 255, 0.12);"
            + " -fx-text-fill: #f4f6f8; -fx-font-size: 12px; -fx-background-radius: 4; -fx-padding: 5 10 5 10;"
            + " -fx-cursor: hand;";
    private static final String KEY_STYLE = "-fx-background-color: rgba(255, 255, 255, 0.85); -fx-text-fill: #14181e;"
            + " -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 3; -fx-padding: 0 5 0 5;";
    private static final String HINT_STYLE = "-fx-text-fill: #b8c0c8; -fx-font-size: 11px;";

    private final VBox root = new VBox(6);
    private final Consumer<Control> pressed;
    /** What it shows now, to build it again only when that changes. */
    private String showing = "";

    /** A panel handing each button pressed to {@code pressed}. */
    public VehiclePanel(final Consumer<Control> pressed) {
        this.pressed = pressed;
        root.setPadding(new Insets(10, 12, 10, 12));
        root.setStyle(PANEL_STYLE);
        root.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        root.setPickOnBounds(false);
        root.setVisible(false);
    }

    public Node node() {
        return root;
    }

    /** Not seen: nothing taken. */
    public void hide() {
        root.setVisible(false);
    }

    /**
     * Seen, for {@code title}, with a button for each of {@code controls}; their words as things
     * are, the vehicle's gun under control ({@code gun}) or not, from the driver's eyes ({@code firstPerson})
     * or not; and whether the mouse is free to click them ({@code free}).
     */
    public void show(final String title, final List<Control> controls, final boolean gun, final boolean firstPerson,
                     final boolean free) {
        root.setVisible(true);
        // held in the world, the mouse is the aim's, and a click there the trigger's
        root.setMouseTransparent(!free);
        final String now = title + controls + gun + firstPerson + free;
        if (now.equals(showing)) {
            return;
        }
        showing = now;
        final Label heading = new Label(title);
        heading.setStyle(TITLE_STYLE);
        root.getChildren().setAll(heading);
        for (final Control control : controls) {
            root.getChildren().add(button(control, gun, firstPerson));
        }
        final Label hint = new Label(free ? "Click a button, or Esc to take the mouse back" : "Esc frees the mouse to click these");
        hint.setStyle(HINT_STYLE);
        root.getChildren().add(hint);
    }

    private Button button(final Control control, final boolean gun, final boolean firstPerson) {
        final String words = switch (control) {
            case GUN -> gun ? "Give up the gun" : "Take the gun";
            case BOMB -> "Drop a bomb";
            case FIRST_PERSON -> firstPerson ? "View from behind" : "First person";
            case BEHIND -> "Camera behind";
            case LEAVE -> "Get out";
        };
        final String key = switch (control) {
            case GUN -> "G";
            case BOMB -> "Right-click";
            case FIRST_PERSON -> "V";
            case BEHIND -> "C";
            case LEAVE -> "T";
        };
        final Label what = new Label(words);
        what.setStyle("-fx-text-fill: #f4f6f8;");
        final Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        final Label shortcut = new Label(key);
        shortcut.setStyle(KEY_STYLE);
        final HBox face = new HBox(12, what, gap, shortcut);
        face.setAlignment(Pos.CENTER_LEFT);
        face.setMinWidth(180);
        final Button button = new Button();
        button.setGraphic(face);
        button.setStyle(BUTTON_STYLE);
        button.setMaxWidth(Double.MAX_VALUE);
        // never the keyboard's: Space and the rest stay the vehicle's
        button.setFocusTraversable(false);
        button.setOnAction(e -> pressed.accept(control));
        return button;
    }
}
