package com.example.fxsuite.avatar.bench.kit;

import com.example.fxsuite.avatar.FrameLog;
import com.example.fxsuite.avatar.SlowFrames;

import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
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
 *
 * <p>At the right of its bar, whether slow frames are logged and how slow is slow ({@link FrameLog}): one setting for
 * the bench and every game played from it, changed as they run. While it is on, the bench's own view's slow frames
 * are logged too ({@link SlowFrames}).
 */
public final class Shell {

    private final String title;
    private final HBox bar = new HBox(8);
    private final Label working = new Label();
    private final Label figures = new Label();
    private final Label problems = new Label();
    private final BorderPane root = new BorderPane();
    /** Whether slow frames are logged, and how slow is slow: at the right of the bar. */
    private final HBox logging = loggingBox();
    private final SlowFrames slowFrames;

    /** A window titled {@code title}, hearing of its work on {@code bus}. */
    public Shell(final String title, final Bus bus) {
        this.title = title;
        this.slowFrames = new SlowFrames("bench " + title);
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
        final Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        bar.getChildren().addAll(gap, logging);
        root.setTop(bar);
        return this;
    }

    /** Whether slow frames are logged and how slow is slow, for a bench with no bar to put where it will. */
    public Node logging() {
        return logging;
    }

    /**
     * A box to log slow frames, on or off, and a threshold in milliseconds: the one setting ({@link FrameLog}),
     * followed by the bench's view and by every game played from it, as they run.
     */
    private static HBox loggingBox() {
        final CheckBox on = new CheckBox("Log frames slower than");
        on.setSelected(FrameLog.ON.get());
        on.selectedProperty().addListener((o, was, now) -> FrameLog.ON.set(now));
        FrameLog.ON.addListener((o, was, now) -> on.setSelected(now));
        final Spinner<Double> slow = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(1, 1_000,
                FrameLog.SLOW.get(), 5));
        slow.setEditable(true);
        slow.setPrefWidth(80);
        slow.valueProperty().addListener((o, was, now) -> {
            if (now != null) {
                FrameLog.SLOW.set(now);
            }
        });
        FrameLog.SLOW.addListener((o, was, now) -> slow.getValueFactory().setValue(now.doubleValue()));
        final Label ms = new Label("ms");
        final HBox box = new HBox(6, on, slow, ms);
        box.setAlignment(Pos.CENTER_RIGHT);
        Tooltip.install(box, new Tooltip("Each frame slower than this, the bench's and any game's played from it,"
                + " written to the console and to " + FrameLog.FILE));
        return box;
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
        // its own view's slow frames, while they are logged
        final AnimationTimer frames = new AnimationTimer() {
            @Override
            public void handle(final long now) {
                slowFrames.follow();
                if (slowFrames.on()) {
                    final String slow = slowFrames.began(now, root);
                    if (slow != null) {
                        slowFrames.write(slow);
                    }
                }
            }
        };
        frames.start();
        stage.setOnHidden(e -> {
            frames.stop();
            slowFrames.on(false);
        });
        Snapshot.ifAsked(scene);
        return scene;
    }
}
