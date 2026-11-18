package com.example.fxsuite.avatar.bench.kit;

import com.example.fxsuite.avatar.Game;
import com.example.fxsuite.avatar.sim.Body;
import com.example.fxsuite.avatar.sim.Place;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * How a bench's world is played ({@link Played}): who comes chosen on a panel of its own (which vehicles, in the order
 * listed; how many people; whether the avatar wears the jet-pack), then <i>Play</i>, the game in a window of its own,
 * the avatar where the world starts it; <i>Again from here</i>, the world as the bench now has it played again, the
 * avatar where it is in the game being played. Closing the game's window, or Tab in it, ends it.
 *
 * <p>Who comes may be chosen by the properties too, to play without a hand on it: {@code -Dbench.guests=truck,barge}
 * (the start of each one's name), {@code -Dbench.people=3}, {@code -Davatar.pack=true}.
 */
public final class Player {

    private final Supplier<PlayWorld> world;
    private final Map<Guest, CheckBox> chosen = new LinkedHashMap<>();
    private final Spinner<Integer> people = new Spinner<>(0, 40, 0);
    private final CheckBox pack = new CheckBox("Jet-pack");
    private Game game;
    private Stage window;

    /**
     * Plays what {@code world} gives when asked (nothing, while it gives null), with any guest there is, those of
     * {@code atFirst} chosen to start with.
     */
    public Player(final Supplier<PlayWorld> world, final List<Guest> atFirst) {
        this.world = world;
        // or as the properties say: -Dbench.guests=truck,barge (the start of each one's name), -Dbench.people=3
        final String asked = System.getProperty("bench.guests");
        for (final Guest guest : Guest.all()) {
            final CheckBox box = new CheckBox(guest.name());
            box.setSelected(asked == null ? atFirst.contains(guest) : java.util.Arrays.stream(asked.split(","))
                    .map(String::trim).anyMatch(name -> !name.isEmpty() && guest.name().startsWith(name)));
            chosen.put(guest, box);
        }
        people.getValueFactory().setValue(Integer.getInteger("bench.people", 0));
        pack.setSelected(Boolean.getBoolean("avatar.pack"));
    }

    /** The panel who comes is chosen on, and the buttons to play. */
    public Node panel() {
        final VBox boxes = new VBox(4);
        boxes.getChildren().addAll(chosen.values());
        final Button play = new Button("Play");
        play.setOnAction(e -> play());
        play.setStyle("-fx-font-weight: bold;");
        final Button again = new Button("Again from here");
        again.setOnAction(e -> again());
        final VBox panel = new VBox(8, new Label("Who comes"), boxes, new HBox(6, new Label("People"), people), pack,
                new HBox(6, play, again));
        panel.setPadding(new Insets(6, 0, 0, 0));
        return panel;
    }

    /** Who is chosen to come, the avatar on {@code from} (where the world starts it, if null). */
    public Played.Guests guests(final Spot from) {
        final List<Guest> vehicles = new ArrayList<>();
        chosen.forEach((guest, box) -> {
            if (box.isSelected()) {
                vehicles.add(guest);
            }
        });
        return new Played.Guests(vehicles, people.getValue(), pack.isSelected(), from);
    }

    /** The world played afresh, the avatar where it starts. */
    public void play() {
        play(null);
    }

    /** The world played again as the bench now has it, the avatar where it is now; afresh, if nothing is being played. */
    public void again() {
        if (game == null) {
            play(null);
            return;
        }
        final Body body = game.avatar();
        play(new Spot("here", new Place(body.position().x(), body.position().z(), body.height(), body.yaw())));
    }

    private void play(final Spot from) {
        final PlayWorld w = world.get();
        if (w == null) {
            return;
        }
        if (window != null) {
            window.close();
        }
        final Stage opened = new Stage();
        final Game started = new Game(opened, new Played(w, guests(from)), () -> Platform.runLater(opened::close),
                other -> { });
        opened.setOnHidden(e -> {
            started.stop();
            if (window == opened) {
                window = null;
                game = null;
            }
        });
        window = opened;
        game = started;
    }
}
