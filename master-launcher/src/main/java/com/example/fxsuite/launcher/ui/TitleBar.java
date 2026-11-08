package com.example.fxsuite.launcher.ui;

import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.HeaderBar;
import javafx.scene.layout.HeaderDragType;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * The launcher's window chrome: the environment, unmistakably, in the title bar itself.
 *
 * <p>JavaFX 25 can extend a window's content into its title-bar area
 * ({@link StageStyle#EXTENDED} + {@link HeaderBar}) while the OS keeps doing what it is good
 * at — resizing, dragging, snapping, and the min/max/close buttons. That lets the title bar
 * carry the environment colour, so a Production window cannot be mistaken for a dev one even
 * when it is small or half covered.</p>
 *
 * <p>Both are <b>preview</b> features in JavaFX 25, gated by {@code -Djavafx.enablePreview}.
 * This class is the only place that touches them, so when the API is finalised (or changes)
 * the update is here alone. Wherever the preview is unavailable — opted out with
 * {@code -Dfxsuite.titlebar=native}, or a platform without {@code EXTENDED_WINDOW} — the same
 * content is shown as a coloured strip under the ordinary Windows title bar instead.</p>
 */
public final class TitleBar {

    private static final String ENABLE_PREVIEW = "javafx.enablePreview";
    private static final String SUPPRESS_WARNING = "javafx.suppressPreviewWarning";
    /** Escape hatch: {@code -Dfxsuite.titlebar=native} keeps the stock title bar. */
    private static final String MODE = "fxsuite.titlebar";

    /** Height of the bar; the caption buttons are sized to match. */
    static final double HEIGHT = 40;

    private TitleBar() {}

    /**
     * Opts this JVM into the JavaFX preview API unless an operator chose otherwise.
     *
     * <p>Must run before JavaFX first touches a preview feature: it reads the flag once, when
     * its {@code PreviewFeature} class initialises. Setting it here, from {@code main()}, is what
     * keeps a plain double-click working — that path has no command line to put {@code -D} on.
     * An explicit value from the command line always wins.</p>
     */
    public static void optIn() {
        if (nativeRequested()) return;
        if (System.getProperty(ENABLE_PREVIEW) == null) System.setProperty(ENABLE_PREVIEW, "true");
        if (System.getProperty(SUPPRESS_WARNING) == null) System.setProperty(SUPPRESS_WARNING, "true");
    }

    /** Whether windows get the extended title bar (as opposed to the native one plus a strip). */
    static boolean extended() {
        return !nativeRequested()
                && Boolean.getBoolean(ENABLE_PREVIEW)
                && Platform.isSupported(ConditionalFeature.EXTENDED_WINDOW);
    }

    /**
     * Builds the window's scene with the chrome in place. Call before the stage is shown — a
     * stage's style is fixed from then on.
     *
     * @param leading left of the bar (logo, menus): clickable, never a drag handle
     * @param center  middle of the bar (title, environment): a drag handle, like title text
     * @param body    the window content below the bar
     */
    static Scene scene(Stage stage, String accent, Node leading, Node center, Parent body,
                       double width, double height) {
        Region bar;
        if (extended()) {
            stage.initStyle(StageStyle.EXTENDED);
            HeaderBar header = new HeaderBar();
            header.setLeading(leading);
            header.setCenter(center);
            HeaderBar.setAlignment(leading, Pos.CENTER_LEFT);
            // The bar's empty background already drags the window. Children are clickable by
            // default, so the title and pill are marked as a drag handle explicitly.
            HeaderBar.setDragType(center, HeaderDragType.DRAGGABLE_SUBTREE);
            HeaderBar.setPrefButtonHeight(stage, HEIGHT);
            bar = header;
        } else {
            Region left = new Region();
            Region right = new Region();
            HBox.setHgrow(left, Priority.ALWAYS);
            HBox.setHgrow(right, Priority.ALWAYS);
            bar = new HBox(leading, left, center, right);
        }
        bar.getStyleClass().add("fxsuite-title-bar");
        bar.setStyle("-fxsuite-accent: " + accent + ";");   // one colour drives the whole bar
        bar.setMinHeight(HEIGHT);
        bar.setPrefHeight(HEIGHT);

        VBox root = new VBox(bar, body);
        VBox.setVgrow(body, Priority.ALWAYS);

        Scene scene = new Scene(root, width, height);
        scene.getStylesheets().add(TitleBar.class.getResource("titlebar.css").toExternalForm());
        // JavaFX colours the caption-button glyphs from the brightness of the *scene* fill, not
        // the bar: below 0.5 they turn white. The fill never shows (the root covers it), so it
        // is set to a deep shade of the accent — white glyphs for every environment, including
        // UAT's orange, which on its own would have been judged light and got grey glyphs.
        scene.setFill(Color.web(accent).deriveColor(0, 1, 0.35, 1));
        return scene;
    }

    private static boolean nativeRequested() {
        return "native".equalsIgnoreCase(System.getProperty(MODE));
    }
}
