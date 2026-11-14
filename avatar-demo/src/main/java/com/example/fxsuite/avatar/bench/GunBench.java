package com.example.fxsuite.avatar.bench;

import com.example.fxsuite.avatar.sound.Gatling;
import com.example.fxsuite.avatar.sound.GatlingVoice;
import com.example.fxsuite.avatar.sound.GunStyle;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

/**
 * A bench for working on the Gatling guns' sounds, apart from the game: pick a {@link Gatling} and
 * a {@link GunStyle}, and fire it. Its sound is made as it turns and fires ({@link GatlingVoice}),
 * not played from a recording, so whatever it does, it is heard doing it.
 *
 * <ul>
 *   <li><b>Fire:</b> hold Space (or the Fire button). Its barrels spin up first, as long as that
 *       gun's take, and it fires once they are up to speed; let go, it stops at once and they run
 *       down, as in the game.</li>
 *   <li><b>Spin:</b> hold S to spin it up without firing: the motor, the gears and the bolts
 *       clicking over dry.</li>
 *   <li><b>Height:</b> how far over the ground it is, which says how late the ground sends the
 *       blast back, and whether the empty cases are heard landing.</li>
 * </ul>
 *
 * Under it, the sound as it is made, a quarter of a second of it, each shot to be seen; and what
 * it is made of: its frequencies, with the shots' note marked (orange, how often it fires, and
 * its multiples), each shot's thump (red) and the gears' whine (blue).
 *
 * <p>{@code -Dgun.pick=GIANT} (or {@code HAND}, {@code HEAVY}) picks the gun to start with;
 * {@code -Dgun.style=realistic} the mix; {@code -Dgun.height=80} its height in metres;
 * {@code -Dgun.fire=true} holds the trigger from the start; {@code -Dbench.snapshot=out.png} saves
 * the window after {@code -Dbench.settle} ms and exits, playing nothing.
 */
public final class GunBench extends Application {

    private static final Color PAPER = Color.web("#1d2228");
    private static final Color RULE = Color.web("#39414b");
    private static final Color WAVE = Color.web("#d9822b");
    private static final Color SHOTS = Color.web("#ff8a3d");
    private static final Color THUMP = Color.web("#ff5a5a");
    private static final Color WHINE = Color.web("#5fa8e0");
    private static final int FFT = 8192;
    private static final double LOW = 20;
    private static final double HIGH = 16_000;
    private static final double RATE = 44_100;

    private GunOut out;
    private Gatling gatling = Gatling.HAND;
    private GunStyle style = GunStyle.CINEMATIC;
    /** The trigger held, by the key or the button; the barrels spun without firing; how fast they turn. */
    private boolean trigger;
    private boolean pressed;
    private boolean dry;
    private double spin;

    private final ComboBox<Gatling> guns = new ComboBox<>();
    private final RadioButton realistic = new RadioButton(GunStyle.REALISTIC.title());
    private final RadioButton cinematic = new RadioButton(GunStyle.CINEMATIC.title());
    private final Label about = new Label();
    private final Label styleAbout = new Label();
    private final Label facts = new Label();
    private final Slider height = EngineBench.slider(0, 150, 1.2);
    private final Slider volume = EngineBench.slider(0, 1, 0.8);
    private final Label readout = new Label();
    private final Label status = new Label();
    private final Canvas wave = new Canvas(660, 170);
    private final Canvas spectrum = new Canvas(660, 240);
    /** The latest sound, more than the quarter second drawn; its frequencies from the last {@link #FFT} of it. */
    private final float[] samples = new float[1 << 14];

    @Override
    public void start(final Stage stage) {
        gatling = Gatling.called(System.getProperty("gun.pick"), Gatling.HAND);
        style = GunStyle.named(System.getProperty("gun.style"));
        final boolean snapshot = System.getProperty("bench.snapshot") != null;
        out = new GunOut(gatling, style, snapshot);
        trigger = Boolean.getBoolean("gun.fire");
        height.setValue(Double.parseDouble(System.getProperty("gun.height", "1.2")));

        guns.getItems().setAll(Gatling.ALL);
        guns.setValue(gatling);
        guns.setOnAction(e -> pick(guns.getValue()));
        final ToggleGroup mix = new ToggleGroup();
        realistic.setToggleGroup(mix);
        cinematic.setToggleGroup(mix);
        (style == GunStyle.REALISTIC ? realistic : cinematic).setSelected(true);
        mix.selectedToggleProperty().addListener((o, was, now) -> {
            style = now == realistic ? GunStyle.REALISTIC : GunStyle.CINEMATIC;
            out.style(style);
            describe();
        });
        about.setWrapText(true);
        styleAbout.setWrapText(true);
        facts.setWrapText(true);
        volume.valueProperty().addListener((o, was, now) -> out.volume(now.doubleValue()));
        status.setText("Sound: " + out.device());
        status.setWrapText(true);
        readout.setFont(Font.font("Monospaced", FontWeight.BOLD, 14));
        final Button fire = new Button("Fire (hold, or hold Space)");
        fire.setFocusTraversable(false);
        fire.setOnMousePressed(e -> pressed = true);
        fire.setOnMouseReleased(e -> pressed = false);

        final VBox controls = new VBox(10,
                EngineBench.heading("Gun"), guns, about, facts,
                EngineBench.heading("Mix"), new HBox(14, realistic, cinematic), styleAbout,
                EngineBench.heading("Firing"), fire,
                EngineBench.label("Hold S to spin it without firing."),
                EngineBench.row("Height (m)", height), EngineBench.row("Volume", volume),
                readout, status);
        controls.setPadding(new Insets(14));
        controls.setPrefWidth(330);
        controls.setMinWidth(330);

        final VBox shown = new VBox(10, EngineBench.label("The sound, the last quarter of a second"), wave,
                EngineBench.label("What it is made of: the shots' note (orange) and its multiples, each shot's"
                        + " thump (red), the gears' whine (blue)"),
                spectrum);
        shown.setPadding(new Insets(14));
        final BorderPane root = new BorderPane();
        root.setLeft(controls);
        root.setCenter(shown);
        root.setStyle("-fx-background-color: #f4f5f7;");
        final Scene scene = new Scene(new HBox(root), 1020, 640);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            switch (e.getCode()) {
                case SPACE, ENTER -> trigger = true;
                case S -> dry = true;
                default -> {
                    return;
                }
            }
            e.consume();
        });
        scene.addEventFilter(KeyEvent.KEY_RELEASED, e -> {
            switch (e.getCode()) {
                case SPACE, ENTER -> trigger = false;
                case S -> dry = false;
                default -> {
                    return;
                }
            }
            e.consume();
        });
        describe();

        new AnimationTimer() {
            private long last = -1;

            @Override
            public void handle(final long now) {
                final double seconds = last < 0 ? 0 : Math.min(0.1, (now - last) / 1e9);
                last = now;
                frame(seconds);
            }
        }.start();

        stage.setTitle("Gun bench");
        stage.setScene(scene);
        stage.setOnHidden(e -> out.close());
        stage.show();
        SoundBench.snapshotIfAsked(scene);
    }

    private void pick(final Gatling picked) {
        gatling = picked;
        spin = 0;
        out.gatling(picked);
        describe();
    }

    private void describe() {
        about.setText(gatling.about());
        styleAbout.setText(style.about());
        facts.setText(String.format("%.0f rounds a minute (%.0f a second), %d barrels, %.1f mm; spins up in %.1f s,"
                        + " down in %.1f s; each shot thumps at %.0f Hz, the gears whine at %.0f Hz%s.",
                gatling.rate(), gatling.perSecond(), gatling.barrels(), gatling.calibre(), gatling.gun().spinUp(),
                gatling.gun().spinDown(), gatling.thump(), gatling.whine(),
                gatling.hydraulic() ? ", the hydraulic drive hissing" : ""));
    }

    /** Spins it up or down as the game's gun does ({@code Gun}), and says so to the sound. */
    private void frame(final double seconds) {
        final boolean firing = trigger || pressed;
        final boolean turning = firing || dry;
        spin = turning ? Math.min(1, spin + seconds / gatling.gun().spinUp())
                : Math.max(0, spin - seconds / gatling.gun().spinDown());
        out.set(spin, firing && spin >= 1, height.getValue());
        readout.setText(String.format("%-14s %3.0f%%%n%-14s %d%n%-14s %.0f ms",
                firing && spin >= 1 ? "Firing" : turning ? "Spinning up" : spin > 0 ? "Running down" : "Still",
                100 * out.spin(), "Rounds", out.shots(), "Ground echo",
                1000 * GatlingVoice.echoDelay(height.getValue())));
        out.latest(samples);
        drawWave();
        drawSpectrum();
    }

    private void drawWave() {
        final GraphicsContext g = wave.getGraphicsContext2D();
        final double w = wave.getWidth();
        final double h = wave.getHeight();
        g.setFill(PAPER);
        g.fillRect(0, 0, w, h);
        g.setStroke(RULE);
        g.strokeLine(0, h / 2, w, h / 2);
        final int shown = (int) (0.25 * RATE);
        g.setStroke(WAVE);
        g.setLineWidth(1);
        // the loudest and quietest of each column, so no shot falls between them
        final double per = (double) shown / w;
        for (int px = 0; px < w; px++) {
            final int a = (int) (px * per);
            final int b = Math.max(a + 1, (int) ((px + 1) * per));
            double low = 0;
            double high = 0;
            for (int i = a; i < b; i++) {
                final double s = samples[samples.length - shown + i];
                low = Math.min(low, s);
                high = Math.max(high, s);
            }
            g.strokeLine(px + 0.5, h / 2 - high * h / 2, px + 0.5, h / 2 - low * h / 2);
        }
    }

    private void drawSpectrum() {
        final double[] re = new double[FFT];
        final double[] im = new double[FFT];
        for (int i = 0; i < FFT; i++) {
            re[i] = samples[samples.length - FFT + i] * (0.5 - 0.5 * Math.cos(2 * Math.PI * i / (FFT - 1)));
        }
        EngineBench.fft(re, im);
        final GraphicsContext g = spectrum.getGraphicsContext2D();
        final double w = spectrum.getWidth();
        final double h = spectrum.getHeight() - 16;
        g.setFill(PAPER);
        g.fillRect(0, 0, w, spectrum.getHeight());
        g.setFont(Font.font("System", 10));
        for (double f = 31.25; f <= HIGH; f *= 2) {
            final double x = x(f, w);
            g.setStroke(RULE);
            g.strokeLine(x, 0, x, h);
            g.setFill(Color.rgb(170, 180, 190));
            g.fillText(f < 1000 ? String.format("%.0f", f) : String.format("%.0fk", f / 1000), x + 2, h + 12);
        }
        g.setLineWidth(1.5);
        g.setStroke(SHOTS);
        for (int k = 1; k <= 6; k++) {
            g.setGlobalAlpha(k == 1 ? 1 : 0.35);
            final double f = gatling.perSecond() * k;
            g.strokeLine(x(f, w), 0, x(f, w), h);
        }
        g.setGlobalAlpha(1);
        g.setStroke(THUMP);
        g.strokeLine(x(gatling.thump(), w), 0, x(gatling.thump(), w), h);
        g.setStroke(WHINE);
        final double whine = gatling.whine() * out.spin();
        if (whine > LOW) {
            g.strokeLine(x(whine, w), 0, x(whine, w), h);
        }
        g.setStroke(Color.WHITE);
        g.setLineWidth(1.4);
        g.beginPath();
        boolean started = false;
        for (int px = 0; px < w; px++) {
            final double from = frequency(px, w);
            final double to = frequency(px + 1, w);
            final int a = Math.max(1, (int) Math.floor(from * FFT / RATE));
            final int b = Math.max(a, (int) Math.ceil(to * FFT / RATE));
            double most = 1e-12;
            for (int k = a; k <= b && k < FFT / 2; k++) {
                most = Math.max(most, Math.hypot(re[k], im[k]) / (FFT / 4.0));
            }
            final double db = 20 * Math.log10(most);
            final double y = h * Math.clamp(-db / 80, 0, 1);
            if (started) {
                g.lineTo(px, y);
            } else {
                g.moveTo(px, y);
                started = true;
            }
        }
        g.stroke();
    }

    private static double x(final double frequency, final double width) {
        return width * Math.log(frequency / LOW) / Math.log(HIGH / LOW);
    }

    private static double frequency(final double x, final double width) {
        return LOW * Math.pow(HIGH / LOW, x / width);
    }
}
