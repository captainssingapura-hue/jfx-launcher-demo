package com.example.fxsuite.launcher.ui;

import com.example.fxsuite.launcher.DiagLog;
import com.example.fxsuite.launcher.EnvConfig;
import com.example.fxsuite.launcher.AppSpawner;
import com.example.fxsuite.launcher.env.BundledApp;
import com.example.fxsuite.launcher.env.EnvSpec;
import com.example.fxsuite.launcher.setup.RegistrySetup;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

/**
 * What you get when you double-click an environment's launcher jar.
 *
 * <p>The default view is the <b>app launcher</b>: the apps this environment's build carries,
 * each one click away. Setup — registering the {@code fxsuite-<env>://} handler and managing
 * signing keys — is not a separate program any more; it lives under <b>Settings</b> in the
 * menu bar, where a user expects it.</p>
 *
 * <p>The window follows the build's {@link EnvSpec}: a singleton build shows its environment
 * fixed and unchangeable, a multiplexed one lets you pick which of its family you mean and
 * refuses anything outside it.</p>
 *
 * <p>Launching from here is a <i>local</i> action by the person who installed the jar, so it
 * needs no launch token. The token exists to stop <i>websites</i> triggering launches; it
 * still gates every {@code fxsuite-<env>://} URL.</p>
 */
public final class LauncherUiApp extends Application {

    private EnvConfig env;
    private EnvSpec spec;
    private TextField envField;      // editable only for a multiplexed build
    private Label envNote;
    private TextArea logView;
    private final List<Button> launchButtons = new ArrayList<>();

    @Override
    public void start(Stage stage) {
        // Same arguments the CLI honours: a multiplexed build can be opened straight onto one
        // of its environments (java -jar FxSuite-dev.jar --env=dev3), which is how a developer
        // runs it from a command prompt.
        env = EnvConfig.load(argValue("--env="), argValue("--base="));
        spec = env.spec();
        String envId = env.envId() != null ? env.envId()
                : (spec != null ? spec.suggestedId() : "dev");
        String accent = spec != null ? spec.accentColour() : "#607d8b";

        // ── header ────────────────────────────────────────────────────────
        Label title = new Label("FxSuite Launcher");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));

        Label pill = new Label("  " + envId.toUpperCase() + "  ");
        pill.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        pill.setTextFill(Color.WHITE);
        pill.setStyle("-fx-background-color:" + accent + "; -fx-background-radius:6;"
                + " -fx-padding:4 12 4 12;");

        boolean pickable = spec == null || spec.multiplexed();
        envField = new TextField(envId);
        envField.setPrefWidth(110);
        envField.setEditable(pickable);
        envField.setDisable(!pickable);

        Label envLabel = new Label(pickable ? "environment:" : "environment (fixed by this build):");
        envNote = new Label(spec == null ? ""
                : pickable ? "one build, several environments: " + spec.membership()
                           : "dedicated build — " + spec.displayName());
        envNote.setTextFill(Color.web("#5b6478"));
        envNote.setFont(Font.font("Segoe UI", 12));

        if (pickable) {
            envField.textProperty().addListener((o, was, now) -> validateEnv(now, pill));
        }

        HBox header = new HBox(12, title, pill);
        header.setAlignment(Pos.CENTER_LEFT);
        HBox envRow = new HBox(8, envLabel, envField, envNote);
        envRow.setAlignment(Pos.CENTER_LEFT);

        // ── the apps this build carries ───────────────────────────────────
        VBox apps = new VBox(8);
        if (env.bundledApps().isEmpty()) {
            Label none = new Label("This launcher carries no apps directly; apps are fetched "
                    + "from the repository when launched from the web.");
            none.setWrapText(true);
            none.setTextFill(Color.web("#5b6478"));
            apps.getChildren().add(none);
        } else {
            for (BundledApp b : env.bundledApps()) {
                apps.getChildren().add(appRow(b));
            }
        }

        // ── menu bar: Settings lives here ─────────────────────────────────
        MenuBar menuBar = new MenuBar();
        Menu settings = new Menu("Settings");
        MenuItem registration = new MenuItem("Registration…");
        registration.setOnAction(e -> SettingsWindow.open(stage, currentEnvId(), this::log));
        MenuItem keys = new MenuItem("Signing keys…");
        keys.setOnAction(e -> SettingsWindow.openOnKeys(stage, currentEnvId(), this::log));
        settings.getItems().addAll(registration, keys);

        Menu file = new Menu("File");
        MenuItem quit = new MenuItem("Exit");
        quit.setOnAction(e -> Platform.exit());
        file.getItems().add(quit);

        menuBar.getMenus().addAll(file, settings);

        logView = new TextArea();
        logView.setEditable(false);
        logView.setFont(Font.font("Consolas", 12));
        logView.setPrefRowCount(7);

        VBox body = new VBox(12, header, envRow, new Label("Apps in this environment:"), apps,
                new Label("Activity:"), logView);
        body.setPadding(new Insets(18));
        body.setStyle("-fx-background-color: linear-gradient(to bottom right,#eef2ff,#ffffff);"
                + " -fx-border-color:" + accent + "; -fx-border-width: 4 0 0 0;");
        VBox.setVgrow(logView, Priority.ALWAYS);

        VBox root = new VBox(menuBar, body);
        VBox.setVgrow(body, Priority.ALWAYS);

        stage.setTitle("FxSuite Launcher — "
                + (spec != null ? spec.displayName() + " (" + envId + ")" : envId.toUpperCase()));
        stage.setScene(new Scene(root, 700, 540));
        stage.show();

        // A multiplexed build has no environment until one is chosen — but here that is the
        // picker's job, not an error, so say it the way the window works.
        if (env.problem() != null) {
            log(spec != null && spec.multiplexed()
                    ? "this build serves " + spec.membership()
                            + " — choose the environment above (showing " + envId + ")"
                    : env.problem());
        }
        String status = RegistrySetup.currentCommand(currentEnvId()) == null
                ? "not registered — use Settings ▸ Registration to enable web links"
                : "registered: fxsuite-" + currentEnvId() + ":// links will open this launcher";
        log(status);
    }

    private String argValue(String prefix) {
        if (getParameters() == null) return null;
        for (String a : getParameters().getRaw()) {
            String s = a.trim();
            if (s.startsWith(prefix)) return s.substring(prefix.length());
        }
        return null;
    }

    /** A multiplexed build serves a family; anything outside it is refused here, not at launch. */
    private void validateEnv(String typed, Label pill) {
        String id = typed == null ? "" : typed.trim();
        boolean ok = spec == null ? EnvConfig.ENV_ID.matcher(id).matches() : spec.accepts(id);
        envField.setStyle(ok ? "" : "-fx-border-color:#c62828; -fx-border-width:2; -fx-border-radius:3;");
        envNote.setText(ok ? (spec == null ? "" : "one build, several environments: " + spec.membership())
                           : "not an environment this build serves (" + spec.membership() + ")");
        envNote.setTextFill(ok ? Color.web("#5b6478") : Color.web("#c62828"));
        if (ok && !id.isEmpty()) pill.setText("  " + id.toUpperCase() + "  ");
        launchButtons.forEach(b -> b.setDisable(!ok));
    }

    private HBox appRow(BundledApp b) {
        Label name = new Label(b.appId());
        name.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        Label ver = new Label("v" + b.version() + "   ·   carried by this launcher");
        ver.setTextFill(Color.web("#5b6478"));
        ver.setFont(Font.font("Segoe UI", 13));
        VBox text = new VBox(2, name, ver);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button launch = new Button("Launch");
        launch.setStyle("-fx-background-color:#2748c8; -fx-text-fill:white; -fx-font-weight:bold;");
        launch.setOnAction(e -> launchBundled(b));
        launchButtons.add(launch);

        HBox row = new HBox(12, text, spacer, launch);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 14, 10, 14));
        row.setStyle("-fx-background-color:#ffffff; -fx-border-color:#d3dae8;"
                + " -fx-background-radius:8; -fx-border-radius:8;");
        return row;
    }

    private void launchBundled(BundledApp b) {
        String envId = currentEnvId();
        new Thread(() -> {
            try {
                AppSpawner.spawnBundled(envId, b.mainClassName(), List.of(b.appId(), b.version()));
                log("launched " + b.appId() + " v" + b.version() + " in " + envId);
            } catch (Exception ex) {
                log("could not launch " + b.appId() + ": " + ex.getMessage());
            }
        }, "launch-" + b.appId()).start();
    }

    private String currentEnvId() {
        if (spec != null && !spec.multiplexed()) return spec.family();
        String typed = envField == null ? null : envField.getText().trim();
        if (typed != null && !typed.isEmpty()) return typed;
        return spec != null ? spec.suggestedId() : "dev";
    }

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("HH:mm:ss");

    private void log(String line) {
        DiagLog.log("[ui] " + line);
        Platform.runLater(() -> {
            if (logView != null) logView.appendText(TS.format(LocalTime.now()) + "  " + line + "\n");
        });
    }
}
