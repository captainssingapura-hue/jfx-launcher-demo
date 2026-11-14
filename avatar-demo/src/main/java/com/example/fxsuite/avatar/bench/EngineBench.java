package com.example.fxsuite.avatar.bench;

import com.example.fxsuite.avatar.Dashboard;
import com.example.fxsuite.avatar.sound.Charger;
import com.example.fxsuite.avatar.sound.Driving;
import com.example.fxsuite.avatar.sound.Engine;
import com.example.fxsuite.avatar.sound.Exhaust;
import com.example.fxsuite.avatar.sound.Revs;
import com.example.fxsuite.avatar.sound.Supercharger;
import com.example.fxsuite.avatar.sound.Turbo;
import com.example.fxsuite.avatar.sound.Vehicle;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.Locale;

/**
 * A bench for working on the engines' sounds, apart from the game: pick an {@link Engine}, and
 * rev it. Its sound is made as it runs, from its revs and throttle ({@code EngineVoice}), not
 * played from a recording, so whatever it does, it is heard doing it.
 *
 * <ul>
 *   <li><b>Rev free:</b> hold Space (or W) for full throttle, or set the throttle by its slider;
 *       it revs up as briskly as that engine does, off the throttle it falls back to its idle, and
 *       held flat out it bounces off the limiter ({@link Revs}).</li>
 *   <li><b>Hold the revs:</b> it turns at whatever the revs slider says, at whatever throttle, to
 *       listen to one place in its range.</li>
 *   <li><b>Drive:</b> the {@link Vehicle} the engine is in ({@link Driving}), on an open road: W
 *       or Space the throttle (or its slider), S the brake, E or Up to change up, Q or Down to
 *       change down; or the gearbox left to change by itself. It pulls away on a slipping clutch,
 *       the revs then held to the road speed by the gear, dropping at each change up, blipped at
 *       each change down; off the throttle it holds back, and pops. A scooter's CVT holds its revs
 *       as it gathers speed. The load is what the driving says.</li>
 * </ul>
 *
 * The load says how hard it is working: none revving free in neutral, all of it pulling a car up
 * a hill. The tachometer reads the revs, as the game's does; under it, the sound as it is made,
 * and what it is made of: its frequencies, with the firing note marked (it moves with the revs)
 * and the pipes' notes (they do not).
 *
 * <p>{@code -Dengine.pick=V8} (or another engine's {@link Engine#key}: {@code THUMPER}, {@code V_TWIN}, ...) picks the engine to
 * start with; {@code -Dengine.rpm=3000} holds it there; {@code -Dengine.throttle} sets the
 * throttle; {@code -Dengine.vehicle=pickup} drives that vehicle; {@code -Dengine.charger=turbo} (or {@code super}, {@code none}) charges a car's engine so;
 * {@code -Dengine.drive=true} drives instead; {@code -Dbench.snapshot=out.png} saves the window after {@code -Dbench.settle} ms
 * and exits, playing nothing.
 */
public final class EngineBench extends Application {

    private static final Color PAPER = Color.web("#1d2228");
    private static final Color RULE = Color.web("#39414b");
    private static final Color WAVE = Color.web("#d9822b");
    private static final Color FIRING = Color.web("#ff8a3d");
    private static final Color PIPE = Color.web("#5fa8e0");
    /** The frequencies are read off this many of the latest samples. */
    private static final int FFT = 4096;
    /** The lowest and highest frequencies drawn. */
    private static final double LOW = 20;
    private static final double HIGH = 8000;

    private EngineOut out;
    /** The engine picked, as it comes, uncharged; and as it is, charged as chosen. */
    private Engine base = Engine.V8;
    private Engine engine = Engine.V8;
    private Revs revs = Revs.idling(engine);
    private boolean held;
    private boolean braking;
    private int lever;
    private Vehicle vehicle = Vehicle.of(engine);
    private Driving driving = Driving.stopped(vehicle);

    private final ComboBox<Engine> engines = new ComboBox<>();
    /** What blows the intake, for a car's engine: nothing, a turbocharger or a supercharger. */
    private static final String NONE = "Breathing on its own";
    private static final String TURBO = "Turbocharged";
    private static final String SUPER = "Supercharged";
    private final ComboBox<String> induction = new ComboBox<>();
    /** Its exhaust: the stock manifold or equal-length headers; and, with two banks, a crossover or none. */
    private static final String STOCK = "Stock manifold";
    private static final String EQUAL = "Equal-length headers";
    private static final String DUAL = "True dual, no crossover";
    private static final String H = "H-pipe";
    private static final String X = "X-pipe";
    private final ComboBox<String> headers = new ComboBox<>();
    private final ComboBox<String> crossing = new ComboBox<>();
    /** What it drives, of the vehicles with its engine: the bench's own, or the game's (its pickup, its truck). */
    private final ComboBox<Vehicle> vehicles = new ComboBox<>();
    private boolean choosing;
    private final Label about = new Label();
    private final Label facts = new Label();
    private final RadioButton free = new RadioButton("Rev free (hold Space or W for full throttle)");
    private final RadioButton hold = new RadioButton("Hold the revs");
    private final RadioButton drive = new RadioButton("Drive: W or Space the throttle, S the brake, E or Up / Q or Down the gears");
    private final CheckBox auto = new CheckBox("The gearbox changes gear by itself");
    private final Label ride = new Label();
    private final Slider throttle = slider(0, 1, 0);
    private final Slider load = slider(0, 1, 0);
    private final Slider rpm = slider(700, 6000, 2000);
    private final Slider volume = slider(0, 1, 0.8);
    private final Label readout = new Label();
    private final Label status = new Label();
    private final Dashboard dashboard = new Dashboard();
    private final Canvas wave = new Canvas(640, 150);
    private final Canvas spectrum = new Canvas(640, 220);
    private final float[] samples = new float[FFT];

    @Override
    public void start(final Stage stage) {
        final String picked = System.getProperty("engine.pick");
        engine = Engine.called(picked, Engine.V8);
        final boolean snapshot = System.getProperty("bench.snapshot") != null;
        out = new EngineOut(engine, snapshot);

        engines.getItems().setAll(Engine.ALL);
        engines.setValue(engine);
        engines.setOnAction(e -> pick(engines.getValue()));
        induction.getItems().setAll(NONE, TURBO, SUPER);
        induction.valueProperty().addListener((o, was, now) -> {
            if (!choosing && now != null) {
                fit(charger(now));
            }
        });
        headers.getItems().setAll(STOCK, EQUAL);
        crossing.getItems().setAll(DUAL, H, X);
        for (final ComboBox<String> piping : java.util.List.of(headers, crossing)) {
            piping.valueProperty().addListener((o, was, now) -> {
                if (!choosing && now != null) {
                    fit(charger(induction.getValue()));
                }
            });
        }
        about.setWrapText(true);
        facts.setWrapText(true);
        final ToggleGroup mode = new ToggleGroup();
        free.setToggleGroup(mode);
        hold.setToggleGroup(mode);
        drive.setToggleGroup(mode);
        drive.setWrapText(true);
        free.setSelected(true);
        rpm.disableProperty().bind(hold.selectedProperty().not());
        load.disableProperty().bind(drive.selectedProperty());
        auto.disableProperty().bind(drive.selectedProperty().not());
        auto.setSelected(true);
        ride.setWrapText(true);
        mode.selectedToggleProperty().addListener((o, was, now) -> {
            // into the driving seat, stopped, in first; out of it, the engine as it was
            if (now == drive) {
                driving = Driving.stopped(vehicle);
            } else if (was == drive) {
                revs = new Revs(driving.rpm(), 0, 0);
            }
        });
        volume.valueProperty().addListener((o, was, now) -> out.volume(now.doubleValue()));
        status.setText("Sound: " + out.device());
        status.setWrapText(true);
        readout.setFont(Font.font("Monospaced", FontWeight.BOLD, 14));
        if (System.getProperty("engine.rpm") != null) {
            hold.setSelected(true);
            rpm.setValue(Double.parseDouble(System.getProperty("engine.rpm")));
        }
        if (Boolean.getBoolean("engine.drive")) {
            drive.setSelected(true);
        }
        throttle.setValue(Double.parseDouble(System.getProperty("engine.throttle", "0")));

        final VBox controls = new VBox(10,
                heading("Engine"), engines, row("Induction", induction), row("Exhaust", headers),
                row("Crossover", crossing), about,
                facts,
                heading("Running"), free, hold, drive, auto, row("Vehicle", vehicles), ride,
                row("Throttle", throttle), row("Load", load), row("Revs", rpm), row("Volume", volume),
                readout, status);
        controls.setPadding(new Insets(14));
        controls.setPrefWidth(340);
        controls.setMinWidth(340);

        final StackPane dial = new StackPane(dashboard.node());
        dial.setAlignment(Pos.CENTER);
        final VBox shown = new VBox(10, dial, label("The sound, the last 60 ms"), wave,
                label("What it is made of: the firing note (orange) moves with the revs, the pipes' notes (blue) stay"),
                spectrum);
        shown.setPadding(new Insets(14));
        final BorderPane root = new BorderPane();
        root.setLeft(controls);
        root.setCenter(shown);
        root.setStyle("-fx-background-color: #f4f5f7;");
        final Scene scene = new Scene(new HBox(root), 1010, 760);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            switch (e.getCode()) {
                case SPACE, W -> held = true;
                case S -> braking = true;
                case E, UP -> lever = 1;
                case Q, DOWN -> lever = -1;
                default -> {
                    return;
                }
            }
            e.consume();
        });
        scene.addEventFilter(KeyEvent.KEY_RELEASED, e -> {
            switch (e.getCode()) {
                case SPACE, W -> held = false;
                case S -> braking = false;
                case E, UP, Q, DOWN -> { }
                default -> {
                    return;
                }
            }
            e.consume();
        });
        pick(engine);
        final String charging = System.getProperty("engine.charger");
        vehicles.valueProperty().addListener((o, was, now) -> {
            if (!choosing && now != null) {
                fit(charger(induction.getValue()));
                driving = Driving.stopped(vehicle);
            }
        });
        final String riding = System.getProperty("engine.vehicle");
        if (riding != null) {
            final String wanted = riding.toLowerCase(Locale.ROOT);
            vehicles.getItems().stream().filter(v -> v.name().toLowerCase(Locale.ROOT).startsWith(wanted))
                    .findFirst().ifPresent(vehicles::setValue);
        }
        if (charging != null && !induction.isDisabled()) {
            induction.setValue(charging.startsWith("s") ? SUPER : charging.startsWith("t") ? TURBO : NONE);
        }

        new AnimationTimer() {
            private long last = -1;

            @Override
            public void handle(final long now) {
                final double seconds = last < 0 ? 0 : Math.min(0.1, (now - last) / 1e9);
                last = now;
                frame(seconds);
            }
        }.start();

        stage.setTitle("Engine bench");
        stage.setScene(scene);
        stage.setOnHidden(e -> out.close());
        stage.show();
        SoundBench.snapshotIfAsked(scene);
    }

    /** The engine picked, charged as its vehicle comes, if a car's; the choice of charger, for a car's only. */
    private void pick(final Engine picked) {
        base = picked;
        final Charger fitted = Vehicle.of(picked).engine().charger();
        induction.setDisable(!picked.car());
        choosing = true;
        induction.setValue(fitted instanceof Turbo ? TURBO : fitted instanceof Supercharger ? SUPER : NONE);
        headers.setValue(STOCK);
        crossing.setValue(DUAL);
        crossing.setDisable(!picked.twoBanks());
        vehicles.getItems().setAll(Vehicle.ALL.stream().filter(v -> v.engine().key().equals(picked.key())).toList());
        vehicles.setValue(Vehicle.of(picked));
        choosing = false;
        fit(fitted);
        revs = Revs.idling(engine);
        driving = Driving.stopped(vehicle);
    }

    /** The engine picked, charged by {@code fitted} (or nothing), in its vehicle: as it was going, if it was. */
    private void fit(final Charger fitted) {
        Exhaust piping = base.exhaust();
        if (EQUAL.equals(headers.getValue())) {
            piping = piping.equalLength();
        }
        final String crossed = crossing.getValue();
        piping = piping.crossed(X.equals(crossed) ? Exhaust.X_PIPE : H.equals(crossed) ? Exhaust.H_PIPE : Exhaust.NONE);
        final Engine picked = base.with(fitted).piped(piping);
        engine = picked;
        vehicle = (vehicles.getValue() != null ? vehicles.getValue() : Vehicle.of(picked)).with(picked);
        ride.setText(vehicle.cvt() > 0
                ? String.format(Locale.ROOT, "Drives a %s: %.0f kg, %.0f N·m, a CVT holding %.0f rpm flat out",
                        vehicle.name().toLowerCase(Locale.ROOT), vehicle.mass(), vehicle.torque(), vehicle.cvt())
                : String.format(Locale.ROOT, "Drives a %s: %.0f kg, %.0f N·m, %d gears, %.2f s a change",
                        vehicle.name().toLowerCase(Locale.ROOT), vehicle.mass(), vehicle.torque(), vehicle.count(), vehicle.shift()));
        out.engine(picked);
        about.setText(picked.about() + (picked.charged() ? " " + charged(picked.charger()) : ""));
        final double[] pipes = picked.pipes();
        final StringBuilder notes = new StringBuilder();
        for (final double p : pipes) {
            notes.append(notes.isEmpty() ? "" : ", ").append(Math.round(p));
        }
        facts.setText(String.format(Locale.ROOT, "%d cylinders%s, firing %s · idle %.0f · red line %.0f rpm · pipes ring at %s Hz",
                picked.cylinders(), picked.charged() ? ", " + picked.charger().kind() : "", picked.exhaust().written(),
                picked.idle(), picked.redline(), notes));
        rpm.setMin(picked.idle());
        rpm.setMax(picked.redline());
    }

    private void frame(final double seconds) {
        final double open = held ? 1 : throttle.getValue();
        final double turning;
        final double firing;
        final boolean cut;
        if (drive.isSelected()) {
            driving = driving.step(vehicle, open, braking ? 1 : 0, lever, auto.isSelected(), seconds);
            lever = 0;
            out.set(driving.rpm(), driving.firing(), driving.load());
            dashboard.show(driving.rpm(), engine.redline(), driving.shown(), driving.kmh(), seconds);
            readout.setText(String.format(Locale.ROOT,
                    "%5.0f rpm · firing %4.0f Hz%n%5.1f km/h · gear %s · %s%nthrottle %3.0f%% · load %3.0f%%%s%s%s",
                    driving.rpm(), engine.firingRate(driving.rpm()), driving.kmh(), driving.shown(),
                    driving.clutch().name().toLowerCase(Locale.ROOT), open * 100, driving.load() * 100, boosting(),
                    braking ? " · BRAKE" : "", driving.cut() > 0 ? " · LIMITER" : ""));
            out.latest(samples);
            drawWave();
            drawSpectrum(driving.rpm());
            return;
        }
        lever = 0;
        if (free.isSelected()) {
            revs = revs.step(engine, open, seconds);
            turning = revs.rpm();
            firing = revs.firing();
            cut = revs.cut() > 0;
        } else {
            turning = rpm.getValue();
            firing = open;
            cut = false;
            revs = new Revs(turning, 0, open);
        }
        out.set(turning, firing, load.getValue());
        dashboard.show(turning, engine.redline(), "N", 0, seconds);
        readout.setText(String.format(Locale.ROOT, "%5.0f rpm · firing %4.0f Hz%nthrottle %3.0f%% · load %3.0f%%%s%s",
                turning, engine.firingRate(turning), open * 100, load.getValue() * 100, boosting(), cut ? " · LIMITER" : ""));
        out.latest(samples);
        drawWave();
        drawSpectrum(turning);
    }

    /** What blows the intake, as chosen. */
    private static Charger charger(final String chosen) {
        return TURBO.equals(chosen) ? Turbo.STANDARD : SUPER.equals(chosen) ? Supercharger.STANDARD : null;
    }

    /** A line on how {@code charger} sounds. */
    private static String charged(final Charger charger) {
        return charger instanceof Turbo
                ? "Turbocharged: boost late and under load, a whistle rising as it spins, a hiss lifting off on boost."
                : "Supercharged: boost at once, and a whine geared to the revs; off the throttle, no hiss.";
    }

    /** The boost, for the readout, if anything blows the engine's intake. */
    private String boosting() {
        return engine.charged() ? String.format(Locale.ROOT, "%n boost %3.0f%%", out.boost() * 100) : "";
    }

    private void drawWave() {
        final GraphicsContext g = wave.getGraphicsContext2D();
        final double w = wave.getWidth();
        final double h = wave.getHeight();
        g.setFill(PAPER);
        g.fillRect(0, 0, w, h);
        g.setStroke(RULE);
        g.strokeLine(0, h / 2, w, h / 2);
        final int shown = (int) (0.06 * 44_100);
        g.setStroke(WAVE);
        g.setLineWidth(1.2);
        g.beginPath();
        for (int i = 0; i < shown; i++) {
            final double x = w * i / shown;
            final double y = h / 2 - samples[FFT - shown + i] * h / 2;
            if (i == 0) {
                g.moveTo(x, y);
            } else {
                g.lineTo(x, y);
            }
        }
        g.stroke();
    }

    private void drawSpectrum(final double turning) {
        final double[] re = new double[FFT];
        final double[] im = new double[FFT];
        for (int i = 0; i < FFT; i++) {
            re[i] = samples[i] * (0.5 - 0.5 * Math.cos(2 * Math.PI * i / (FFT - 1)));
        }
        fft(re, im);
        final GraphicsContext g = spectrum.getGraphicsContext2D();
        final double w = spectrum.getWidth();
        final double h = spectrum.getHeight() - 16;
        g.setFill(PAPER);
        g.fillRect(0, 0, w, spectrum.getHeight());
        // the octaves, marked
        g.setFont(Font.font("System", 10));
        for (double f = 31.25; f <= HIGH; f *= 2) {
            final double x = x(f, w);
            g.setStroke(RULE);
            g.strokeLine(x, 0, x, h);
            g.setFill(Color.rgb(170, 180, 190));
            g.fillText(f < 1000 ? String.format("%.0f", f) : String.format("%.0fk", f / 1000), x + 2, h + 12);
        }
        // the pipes' notes, where they stay; the firing note and its first few multiples, where the revs put them
        g.setLineWidth(1.5);
        g.setStroke(PIPE);
        for (final double p : engine.pipes()) {
            g.strokeLine(x(p, w), 0, x(p, w), h);
        }
        g.setStroke(FIRING);
        final double note = engine.firingRate(turning);
        for (int k = 1; k <= 4; k++) {
            g.setGlobalAlpha(k == 1 ? 1 : 0.35);
            g.strokeLine(x(note * k, w), 0, x(note * k, w), h);
        }
        g.setGlobalAlpha(1);
        // the sound's frequencies, in decibels, the loudest of each column
        g.setStroke(Color.WHITE);
        g.setLineWidth(1.4);
        g.beginPath();
        boolean started = false;
        for (int px = 0; px < w; px++) {
            final double from = frequency(px, w);
            final double to = frequency(px + 1, w);
            final int a = Math.max(1, (int) Math.floor(from * FFT / 44_100.0));
            final int b = Math.max(a, (int) Math.ceil(to * FFT / 44_100.0));
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

    /** In place, radix two: {@code re} and {@code im} a power of two long. */
    static void fft(final double[] re, final double[] im) {
        final int n = re.length;
        for (int i = 1, j = 0; i < n; i++) {
            int bit = n >> 1;
            for (; (j & bit) != 0; bit >>= 1) {
                j ^= bit;
            }
            j ^= bit;
            if (i < j) {
                double t = re[i];
                re[i] = re[j];
                re[j] = t;
                t = im[i];
                im[i] = im[j];
                im[j] = t;
            }
        }
        for (int len = 2; len <= n; len <<= 1) {
            final double angle = -2 * Math.PI / len;
            final double wr = Math.cos(angle);
            final double wi = Math.sin(angle);
            for (int i = 0; i < n; i += len) {
                double cr = 1;
                double ci = 0;
                for (int k = 0; k < len / 2; k++) {
                    final int a = i + k;
                    final int b = a + len / 2;
                    final double tr = re[b] * cr - im[b] * ci;
                    final double ti = re[b] * ci + im[b] * cr;
                    re[b] = re[a] - tr;
                    im[b] = im[a] - ti;
                    re[a] += tr;
                    im[a] += ti;
                    final double next = cr * wr - ci * wi;
                    ci = cr * wi + ci * wr;
                    cr = next;
                }
            }
        }
    }

    static Slider slider(final double min, final double max, final double value) {
        final Slider slider = new Slider(min, max, value);
        slider.setPrefWidth(200);
        return slider;
    }

    static HBox row(final String name, final javafx.scene.Node control) {
        final Label label = new Label(name);
        label.setMinWidth(70);
        final HBox row = new HBox(8, label, control);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    static Label heading(final String text) {
        final Label label = new Label(text);
        label.setFont(Font.font("System", FontWeight.BOLD, 14));
        return label;
    }

    static Label label(final String text) {
        final Label label = new Label(text);
        label.setWrapText(true);
        return label;
    }
}
