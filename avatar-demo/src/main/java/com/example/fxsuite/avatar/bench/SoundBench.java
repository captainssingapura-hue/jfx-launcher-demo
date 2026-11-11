package com.example.fxsuite.avatar.bench;

import com.example.fxsuite.avatar.audio.Outputs;
import com.example.fxsuite.avatar.audio.Speakers;
import com.example.fxsuite.avatar.sim.Body;
import com.example.fxsuite.avatar.sim.Num;
import com.example.fxsuite.avatar.sim.Vec3;
import com.example.fxsuite.avatar.sound.Clip;
import com.example.fxsuite.avatar.sound.Level;
import com.example.fxsuite.avatar.sound.Profile;
import com.example.fxsuite.avatar.sound.Profiles;
import com.example.fxsuite.avatar.sound.Sound;
import com.example.fxsuite.avatar.sound.Wav;

import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleFunction;

/**
 * A bench for working on the sounds, apart from the game: play each one, softer or harder,
 * take after take, from anywhere round the listener; run the loops and drive them as the
 * game will (the jets by thrust, the wind by speed); see the waveform; save it to a file.
 * Every sound comes in each {@link Profile}; switch profile, and everything, loops too,
 * changes character at once.
 * Any output will do, and it can be switched while running: OpenAL, Java Sound, or none,
 * when the bench is just for looking or saving.
 *
 * <p>The listener stands at the origin facing +z, as the avatar does at the start: ahead is
 * 0°, right 90°. Space or Enter plays the chosen sound.
 *
 * <p>{@code -Davatar.sounds=robot|arcade} picks the profile to start with;
 * {@code -Davatar.audio=openal|javasound|silent} the output;
 * {@code -Dbench.select=LANDING} the sound; {@code -Dbench.snapshot=out.png} saves the window
 * after {@code -Dbench.settle} ms (900 by default) and exits, playing nothing.
 */
public final class SoundBench extends Application {

    private static final Color WAVE = Color.web("#d9822b");
    private static final Color PAPER = Color.web("#1d2228");
    private static final Color RULE = Color.web("#39414b");

    private Speakers speakers;
    private boolean switching;
    private Profile profile = Profiles.fromSettings();

    private final ComboBox<Profile> profiles = new ComboBox<>();
    private final Label about = new Label();

    private final ComboBox<Speakers.Kind> output = new ComboBox<>();
    private final Label status = new Label();
    private final Slider volume = slider(0, 1, 0.8);

    private final ListView<Sound> sounds = new ListView<>();
    private final Slider amount = slider(0, 1, 0.6);
    private final Label amountName = new Label("Amount");
    private final Spinner<Integer> variant = new Spinner<>(0, 999, 0);
    private final CheckBox vary = new CheckBox("New take each play");
    private final Slider direction = slider(-180, 180, 0);
    private final Slider distance = slider(0, 5000, 0);
    private final Slider pitch = slider(0.5, 2, 1);

    private final Canvas wave = new Canvas(760, 150);
    private final Label stats = new Label();
    private Clip shown;

    private final CheckBox jets = new CheckBox("Jet engines");
    private final Slider thrust = slider(0, 1, 0.35);
    private final Label jetLevels = new Label();
    private final CheckBox wind = new CheckBox("Wind");
    private final Slider speed = slider(0, 2400, Body.FLY);
    private final Label windLevel = new Label();
    private final Map<Sound, Clip> loopClips = new EnumMap<>(Sound.class);
    private final Map<Sound, Speakers.Loop> playing = new EnumMap<>(Sound.class);

    @Override
    public void start(final Stage stage) {
        output.getItems().setAll(Speakers.Kind.devices());
        output.setOnAction(e -> {
            if (!switching) {
                openOutput(output.getValue());
            }
        });
        volume.valueProperty().addListener((o, was, now) -> {
            if (speakers != null) {
                speakers.volume(now.doubleValue());
            }
        });
        profiles.getItems().setAll(Profiles.all());
        profiles.setConverter(new StringConverter<>() {
            @Override
            public String toString(final Profile p) {
                return p == null ? "" : p.title();
            }

            @Override
            public Profile fromString(final String title) {
                return null;
            }
        });
        profiles.setValue(profile);
        profiles.setOnAction(e -> switchProfile(profiles.getValue()));
        about.setText(profile.about());
        final HBox voice = new HBox(10, bold("Profile"), profiles, about);
        voice.setAlignment(Pos.CENTER_LEFT);
        status.setWrapText(true);
        final Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        final HBox top = new HBox(10, bold("Output"), output, gap, new Label("Volume"), volume);
        top.setAlignment(Pos.CENTER_LEFT);
        final VBox header = new VBox(8, voice, top, status);

        final BorderPane root = new BorderPane();
        root.setPadding(new Insets(12));
        root.setTop(header);
        BorderPane.setMargin(header, new Insets(0, 0, 12, 0));
        root.setCenter(new HBox(16, oneShots(), loops()));
        final VBox bottom = new VBox(6, wave, stats);
        BorderPane.setMargin(bottom, new Insets(12, 0, 0, 0));
        root.setBottom(bottom);

        final Scene scene = new Scene(root);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.SPACE || e.getCode() == KeyCode.ENTER) {
                play();
                e.consume();
            }
        });
        stage.setTitle("Sound bench");
        stage.setScene(scene);
        stage.setOnHidden(e -> closeOutput());

        openOutput(Speakers.Kind.fromSettings());
        final String chosen = System.getProperty("bench.select");
        sounds.getSelectionModel().select(chosen == null ? Sound.STEP_GROUND : Sound.valueOf(chosen));
        stage.show();
        snapshotIfAsked(scene);
    }

    @Override
    public void stop() {
        closeOutput();
    }

    // ---- the parts of the window

    private VBox oneShots() {
        sounds.getItems().setAll(Sound.oneShots());
        sounds.setPrefSize(220, 370);
        sounds.getSelectionModel().selectedItemProperty().addListener((o, was, now) -> {
            amount.setDisable(now == null || now.amount() == null);
            amountName.setText(now == null || now.amount() == null ? "Amount" : capitalised(now.amount()));
            showPreview();
        });
        sounds.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                play();
            }
        });
        amount.valueProperty().addListener((o, was, now) -> showPreview());
        variant.valueProperty().addListener((o, was, now) -> showPreview());
        variant.setEditable(true);
        variant.setPrefWidth(90);
        vary.setSelected(true);

        final GridPane controls = grid();
        int row = 0;
        controls.addRow(row++, amountName, amount, readout(amount, v -> String.format("%.2f", v)));
        controls.addRow(row++, new Label("Take"), variant, vary);
        controls.addRow(row++, new Label("Direction"), direction,
                readout(direction, v -> String.format("%+.0f°", v)));
        controls.addRow(row++, new Label("Distance"), distance,
                readout(distance, v -> v < 1 ? "at the ear" : String.format("%.1f m", v / 100)));
        controls.addRow(row, new Label("Pitch"), pitch, readout(pitch, v -> String.format("×%.2f", v)));

        final Button play = new Button("Play");
        play.setDefaultButton(false);
        play.setOnAction(e -> play());
        final Button walk = new Button("Play 8 in a row");
        walk.setOnAction(e -> playInARow(8));
        final Button save = new Button("Save WAV…");
        save.setOnAction(e -> saveShown(save));
        final Button all = new Button("Render all…");
        all.setOnAction(e -> renderAll(all));
        final HBox buttons = new HBox(8, play, walk, save, all);

        final VBox box = new VBox(10, bold("One-shots"), new HBox(12, sounds, controls), buttons);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private VBox loops() {
        renderLoops();
        jets.setOnAction(e -> {
            runLoops(jets.isSelected(), Sound.JET_RUMBLE, Sound.JET_HISS, Sound.JET_WHINE);
            steerLoops();
        });
        wind.setOnAction(e -> {
            runLoops(wind.isSelected(), Sound.WIND);
            steerLoops();
        });
        thrust.valueProperty().addListener((o, was, now) -> steerLoops());
        speed.valueProperty().addListener((o, was, now) -> steerLoops());
        jetLevels.setFont(Font.font("Consolas", 12));
        windLevel.setFont(Font.font("Consolas", 12));

        final GridPane controls = grid();
        controls.addRow(0, jets);
        controls.addRow(1, new Label("Thrust"), thrust, readout(thrust, v -> String.format("%.2f", v)));
        controls.add(jetLevels, 0, 2, 3, 1);
        controls.addRow(3, wind);
        controls.addRow(4, new Label("Speed"), speed, readout(speed, v -> String.format("%.0f u/s", v)));
        controls.add(windLevel, 0, 5, 3, 1);
        final Label note = new Label("Hover ≈ 0.35 thrust, full boost 1.0; "
                + "walk " + (int) Body.WALK + ", fly " + (int) Body.FLY + ", boost " + (int) Body.BOOST + " u/s.");
        note.setWrapText(true);
        note.setMaxWidth(320);
        steerLoops();
        return new VBox(10, bold("Loops, driven as in the game"), controls, note);
    }

    // ---- what the controls do

    /** The chosen sound, as the controls say, made now. */
    private Clip current() {
        final Sound sound = sounds.getSelectionModel().getSelectedItem();
        return sound == null ? null : profile.render(sound, amount.getValue(), variant.getValue());
    }

    private void showPreview() {
        final Clip clip = current();
        if (clip != null) {
            show(clip);
        }
    }

    private void play() {
        final Clip clip = current();
        if (clip == null) {
            return;
        }
        show(clip);
        final Vec3 at = place();
        if (at == null) {
            speakers.play(clip, 1, pitch.getValue());
        } else {
            speakers.play(clip, 1, pitch.getValue(), at);
        }
        if (vary.isSelected()) {
            variant.getValueFactory().setValue((variant.getValue() + 1) % 1000);
        }
    }

    private void playInARow(final int times) {
        final Timeline row = new Timeline();
        for (int i = 0; i < times; i++) {
            row.getKeyFrames().add(new KeyFrame(Duration.seconds(0.36 * i), e -> play()));
        }
        row.play();
    }

    /** Where the one-shots sound from: null at the ear, else round the listener. */
    private Vec3 place() {
        final double d = distance.getValue();
        return d < 1 ? null : Num.heading(direction.getValue()).multiply(d);
    }

    private void runLoops(final boolean on, final Sound... loops) {
        for (final Sound loop : loops) {
            final Speakers.Loop running = playing.remove(loop);
            if (running != null) {
                running.stop();
            }
            if (on) {
                playing.put(loop, speakers.loop(loopClips.get(loop)));
            }
        }
    }

    /** Another profile: the loops made again and, if running, restarted in it; the preview redrawn. */
    private void switchProfile(final Profile chosen) {
        if (chosen == null || chosen == profile) {
            return;
        }
        profile = chosen;
        about.setText(profile.about());
        renderLoops();
        runLoops(jets.isSelected(), Sound.JET_RUMBLE, Sound.JET_HISS, Sound.JET_WHINE);
        runLoops(wind.isSelected(), Sound.WIND);
        steerLoops();
        showPreview();
    }

    private void renderLoops() {
        for (final Sound loop : Sound.loops()) {
            loopClips.put(loop, profile.render(loop, 1, 0));
        }
    }

    /** The loops' levels from thrust and speed, by the same rules the game will use. */
    private void steerLoops() {
        final double t = thrust.getValue();
        final Map<Sound, Level> levels = new EnumMap<>(Sound.class);
        for (final Sound loop : Sound.loops()) {
            levels.put(loop, profile.level(loop, t, speed.getValue()));
        }
        playing.forEach((loop, voice) -> voice.set(levels.get(loop).gain(), levels.get(loop).pitch()));
        jetLevels.setText(String.format("rumble %s   hiss %s   whine %s",
                level(levels.get(Sound.JET_RUMBLE)), level(levels.get(Sound.JET_HISS)),
                level(levels.get(Sound.JET_WHINE))));
        windLevel.setText("wind " + level(levels.get(Sound.WIND)));
    }

    private static String level(final Level level) {
        return String.format("%.2f ×%.2f", level.gain(), level.pitch());
    }

    // ---- the output

    private void openOutput(final Speakers.Kind kind) {
        closeOutput();
        final Outputs.Opened opened = Outputs.open(kind);
        speakers = opened.speakers();
        speakers.volume(volume.getValue());
        speakers.listen(Vec3.ZERO, 0);
        switching = true;
        output.setValue(speakers.kind());
        switching = false;
        final StringBuilder said = new StringBuilder(speakers.kind() == Speakers.Kind.SILENT
                ? "Silent: nothing plays, but everything can be seen and saved."
                : "Playing through " + speakers.kind().title() + " on " + speakers.device() + ".");
        opened.notes().forEach(n -> said.append("\n").append(n));
        status.setText(said.toString());
        runLoops(jets.isSelected(), Sound.JET_RUMBLE, Sound.JET_HISS, Sound.JET_WHINE);
        runLoops(wind.isSelected(), Sound.WIND);
        steerLoops();
    }

    private void closeOutput() {
        if (speakers != null) {
            playing.values().forEach(Speakers.Loop::stop);
            playing.clear();
            speakers.close();
            speakers = null;
        }
    }

    // ---- looking and saving

    /** The clip's waveform, its loudest and quietest in each column, over a grid of 50 ms. */
    private void show(final Clip clip) {
        shown = clip;
        final GraphicsContext g = wave.getGraphicsContext2D();
        final double w = wave.getWidth();
        final double h = wave.getHeight();
        final double mid = h / 2;
        g.setFill(PAPER);
        g.fillRect(0, 0, w, h);
        final double perPixel = Math.max(1, (double) clip.length() / w);
        g.setStroke(RULE);
        g.setLineWidth(1);
        final double tick = 0.05 * Clip.RATE / perPixel;
        for (double x = tick; x < w; x += tick) {
            g.strokeLine(Math.round(x) + 0.5, 0, Math.round(x) + 0.5, h);
        }
        g.strokeLine(0, mid + 0.5, w, mid + 0.5);
        g.setStroke(WAVE);
        for (int x = 0; x < w; x++) {
            final int from = (int) (x * perPixel);
            final int to = Math.min(clip.length(), (int) ((x + 1) * perPixel));
            if (from >= to) {
                break;
            }
            float low = 0;
            float high = 0;
            for (int i = from; i < to; i++) {
                low = Math.min(low, clip.sample(i));
                high = Math.max(high, clip.sample(i));
            }
            g.strokeLine(x + 0.5, mid - high * mid, x + 0.5, mid - low * mid + 1);
        }
        stats.setText(String.format("%s   %.0f ms   peak %.1f dBFS   RMS %.1f dBFS   (grid: 50 ms)",
                clip.name(), clip.seconds() * 1000, decibels(clip.peak()), decibels(clip.rms())));
    }

    private static double decibels(final double level) {
        return level <= 0 ? Double.NEGATIVE_INFINITY : 20 * Math.log10(level);
    }

    private void saveShown(final Button from) {
        if (shown == null) {
            return;
        }
        final FileChooser chooser = new FileChooser();
        chooser.setInitialFileName(shown.name() + ".wav");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("WAV sound", "*.wav"));
        final File file = chooser.showSaveDialog(from.getScene().getWindow());
        if (file != null) {
            try {
                Wav.write(file.toPath(), shown);
                stats.setText("saved " + file);
            } catch (IOException ex) {
                stats.setText("could not save: " + ex.getMessage());
            }
        }
    }

    private void renderAll(final Button from) {
        final DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Render every sound into…");
        final File folder = chooser.showDialog(from.getScene().getWindow());
        if (folder != null) {
            try {
                final List<Path> written = Render.all(folder.toPath());
                stats.setText("wrote " + written.size() + " files to " + folder);
            } catch (IOException ex) {
                stats.setText("could not render: " + ex.getMessage());
            }
        }
    }

    // ---- small helpers

    private static Slider slider(final double min, final double max, final double value) {
        final Slider slider = new Slider(min, max, value);
        slider.setPrefWidth(200);
        return slider;
    }

    private static Label readout(final Slider slider, final DoubleFunction<String> format) {
        final Label label = new Label(format.apply(slider.getValue()));
        label.setMinWidth(70);
        slider.valueProperty().addListener((o, was, now) -> label.setText(format.apply(now.doubleValue())));
        return label;
    }

    private static String capitalised(final String word) {
        return Character.toUpperCase(word.charAt(0)) + word.substring(1);
    }

    private static Label bold(final String text) {
        final Label label = new Label(text);
        label.setFont(Font.font(null, FontWeight.BOLD, 13));
        return label;
    }

    private static GridPane grid() {
        final GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        return grid;
    }

    /** Saves the window to {@code -Dbench.snapshot}, if set, and exits. */
    private static void snapshotIfAsked(final Scene scene) {
        final String path = System.getProperty("bench.snapshot");
        if (path == null) {
            return;
        }
        final PauseTransition wait = new PauseTransition(Duration.millis(Long.getLong("bench.settle", 900)));
        wait.setOnFinished(e -> {
            try {
                final WritableImage image = scene.snapshot(null);
                final PixelReader pixels = image.getPixelReader();
                final BufferedImage out = new BufferedImage((int) image.getWidth(), (int) image.getHeight(),
                        BufferedImage.TYPE_INT_ARGB);
                for (int y = 0; y < out.getHeight(); y++) {
                    for (int x = 0; x < out.getWidth(); x++) {
                        out.setRGB(x, y, pixels.getArgb(x, y));
                    }
                }
                ImageIO.write(out, "png", new File(path));
                System.out.println("saved " + path);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            Platform.exit();
        });
        wait.play();
    }
}
