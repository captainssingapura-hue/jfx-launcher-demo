package com.example.fxsuite.avatar;

import com.example.fxsuite.avatar.sim.Demo;
import com.example.fxsuite.avatar.sim.Recording;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * The avatar demos: a choice of which to play ({@link Demo}), then that one ({@link Game}); Tab in
 * a demo comes back to the choice.
 *
 * <p>The choice is skipped for a demo asked for ({@code -Davatar.demo=AIRSHIP}), for a replay (the
 * demo it was made in), and for a run without a keyboard (a script, a snapshot: the playground,
 * unless asked otherwise).
 */
public final class AvatarDemo extends Application {

    private Stage stage;
    private Game game;

    @Override
    public void start(final Stage stage) {
        this.stage = stage;
        stage.setTitle("FxSuite · Avatar");
        final Demo asked = asked();
        if (asked != null) {
            play(asked);
        } else {
            choose();
        }
        stage.show();
    }

    @Override
    public void stop() {
        if (game != null) {
            game.stop();
        }
    }

    /** The demo asked for, or that a replay or an unattended run calls for; null to let the player choose. */
    private static Demo asked() {
        final String demo = System.getProperty("avatar.demo");
        if (demo != null) {
            return Demo.valueOf(demo.trim().toUpperCase(Locale.ROOT));
        }
        final String replay = System.getProperty("avatar.replay");
        if (replay != null) {
            try (Reader in = Files.newBufferedReader(Path.of(replay))) {
                return Recording.read(in).demo();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        if (System.getProperty("avatar.script") != null || System.getProperty("avatar.snapshot") != null) {
            return Demo.PLAYGROUND;
        }
        return null;
    }

    /** Back to the choice: the demo being played, if any, put away. */
    private void choose() {
        if (game != null) {
            game.stop();
            game = null;
        }
        stage.setTitle("FxSuite · Avatar");
        stage.setScene(choice());
    }

    private void play(final Demo demo) {
        if (game != null) {
            game.stop();
        }
        game = new Game(stage, demo, () -> Platform.runLater(this::choose),
                other -> Platform.runLater(() -> play(other)));
    }

    /** The choice: each demo, by name and what is in it, to click or to pick by its number. */
    private Scene choice() {
        final Label title = new Label("FxSuite · Avatar");
        title.setStyle("-fx-text-fill: #f4f6f8; -fx-font-size: 30px; -fx-font-weight: bold;");
        final Label hint = new Label("Choose a demo  ·  in a demo, Tab comes back here");
        hint.setStyle("-fx-text-fill: #aab4bf; -fx-font-size: 14px;");
        final VBox list = new VBox(14, title, hint);
        list.setAlignment(Pos.CENTER);
        list.setPadding(new Insets(40));
        final Demo[] demos = Demo.values();
        for (int i = 0; i < demos.length; i++) {
            final Demo demo = demos[i];
            final Button card = new Button((i + 1) + "   " + demo.title() + "\n" + demo.description());
            card.setStyle("-fx-font-size: 15px; -fx-text-fill: #f4f6f8; -fx-background-color: #2b3440;"
                    + " -fx-background-radius: 8; -fx-padding: 14 22 14 22; -fx-alignment: center-left;"
                    + " -fx-cursor: hand;");
            card.setPrefWidth(620);
            card.setOnAction(e -> play(demo));
            list.getChildren().add(card);
        }
        list.setStyle("-fx-background-color: linear-gradient(to bottom, #1b222b, #11161c);");
        final Scene scene = new Scene(list, 1280, 800);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            final int n = e.getCode().ordinal() - KeyCode.DIGIT1.ordinal();
            if (n >= 0 && n < demos.length && e.getCode().isDigitKey()) {
                play(demos[n]);
            }
        });
        return scene;
    }
}
