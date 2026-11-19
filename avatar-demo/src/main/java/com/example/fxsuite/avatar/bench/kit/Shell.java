package com.example.fxsuite.avatar.bench.kit;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.List;

/**
 * A bench's window, laid out as every bench's is: a bar along the top (what is on show, what is shown of it, and
 * <i>Working…</i> while its workshop is at work); what it shows in the middle; a side panel to the right, what can be
 * changed over the figures and the problems. Its figures, problems and whether it is at work it hears of on its bus
 * ({@link Bus.Figures}, {@link Bus.Problems}, {@link Bus.Working}); saved as a picture and closed if the properties ask
 * ({@link Snapshot}).
 */
public final class Shell {

    private final String title;
    private final HBox bar = new HBox(8);
    private final Label working = new Label();
    private final Label figures = new Label();
    private final Label problems = new Label();
    private final BorderPane root = new BorderPane();

    /** A window titled {@code title}, hearing of its work on {@code bus}. */
    public Shell(final String title, final Bus bus) {
        this.title = title;
        working.setStyle("-fx-text-fill: #8a5a00; -fx-font-style: italic;");
        figures.setWrapText(true);
        problems.setWrapText(true);
        problems.setTextFill(Color.web("#b03a2e"));
        // taking no room while there is nothing to say
        for (final Label said : List.of(working, problems)) {
            said.managedProperty().bind(said.textProperty().isNotEmpty());
            said.visibleProperty().bind(said.textProperty().isNotEmpty());
        }
        bar.setPadding(new Insets(6, 8, 6, 8));
        bar.setAlignment(Pos.CENTER_LEFT);
        root.setTop(bar);
        root.setStyle("-fx-background-color: #f4f5f7;");
        bus.subscribe(Bus.Working.class, w -> working.setText(w.busy() ? "Working…" : ""), Bus.SCREEN);
        bus.subscribe(Bus.Problems.class, p -> problems.setText(p.text()), Bus.SCREEN);
        bus.subscribe(Bus.Figures.class, f -> figures.setText(f.text()), Bus.SCREEN);
    }

    /** Along the top bar, in order, before <i>Working…</i>. */
    public Shell bar(final List<? extends Node> items) {
        bar.getChildren().setAll(items);
        bar.getChildren().add(working);
        root.setTop(bar);
        return this;
    }

    /** No bar along the top: <i>Working…</i> shown where the bench puts {@link #working}. */
    public Shell barless() {
        root.setTop(null);
        return this;
    }

    /** Where <i>Working…</i> is shown, for a bench with no bar. */
    public Label working() {
        return working;
    }

    /** In the middle. */
    public Shell middle(final Node shown) {
        root.setCenter(shown);
        return this;
    }

    /** To the right, so wide: {@code heading} over {@code changed} (which takes what room there is), the figures and problems under. */
    public Shell side(final Node heading, final Node changed, final double width) {
        VBox.setVgrow(changed, Priority.ALWAYS);
        final VBox side = new VBox(6, heading, changed, figures, problems);
        side.setPadding(new Insets(8));
        side.setPrefWidth(width);
        root.setRight(side);
        return this;
    }

    /** To the left: what can be changed, as a bench lays it out itself (its figures where it puts {@link #figures}). */
    public Shell left(final Node changed) {
        root.setLeft(changed);
        return this;
    }

    /** Where its figures are shown, as it hears of them: for a bench that lays out its own side. */
    public Label figures() {
        return figures;
    }

    /** Where what is wrong is shown, as it hears of it. */
    public Label problems() {
        return problems;
    }

    /** The window shown on {@code stage}, so big; saved and closed if the properties ask. */
    public Scene show(final Stage stage, final double width, final double height) {
        final Scene scene = new Scene(root, width, height);
        stage.setTitle(title);
        stage.setScene(scene);
        stage.show();
        Snapshot.ifAsked(scene);
        return scene;
    }
}
