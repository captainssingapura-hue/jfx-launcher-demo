package com.example.fxsuite.launcher.ui;

import com.example.fxsuite.launcher.BundledApps;
import com.example.fxsuite.launcher.DiagLog;
import com.example.fxsuite.launcher.EnvConfig;
import com.example.fxsuite.launcher.AppSpawner;
import com.example.fxsuite.launcher.setup.RegistrySetup;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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
 * <p>The default view is the <b>app launcher</b>: the apps this environment's jar carries,
 * each one click away. Setup — registering the {@code fxsuite-<env>://} handler and managing
 * signing keys — is not a separate program any more; it lives under <b>Settings</b> in the
 * menu bar, where a user expects it.</p>
 *
 * <p>Launching from here is a <i>local</i> action by the person who installed the jar, so it
 * needs no launch token. The token exists to stop <i>websites</i> triggering launches; it
 * still gates every {@code fxsuite-<env>://} URL.</p>
 */
public final class LauncherUiApp extends Application {

    private EnvConfig env;
    private TextField envField;      // editable only when no environment is baked in
    private TextArea logView;

    @Override
    public void start(Stage stage) {
        env = EnvConfig.load(null, null);
        String baked = EnvConfig.bakedEnv();
        String envId = env.envId() == null ? "dev" : env.envId();

        // ── header ────────────────────────────────────────────────────────
        Label title = new Label("FxSuite Launcher");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));

        Label pill = new Label("  " + envId.toUpperCase() + "  ");
        pill.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        pill.setTextFill(Color.WHITE);
        pill.setStyle("-fx-background-color:" + envColour(envId) + "; -fx-background-radius:6;"
                + " -fx-padding:4 12 4 12;");

        envField = new TextField(envId);
        envField.setPrefWidth(110);
        envField.setEditable(baked == null);          // baked env is not user-editable
        envField.setDisable(baked != null);
        Label envLabel = new Label(baked != null ? "environment (baked in):" : "environment:");

        HBox header = new HBox(12, title, pill);
        header.setAlignment(Pos.CENTER_LEFT);
        HBox envRow = new HBox(8, envLabel, envField);
        envRow.setAlignment(Pos.CENTER_LEFT);

        // ── the apps this launcher carries ────────────────────────────────
        VBox apps = new VBox(8);
        if (BundledApps.all().isEmpty()) {
            Label none = new Label("This launcher carries no apps directly; apps are fetched "
                    + "from the repository when launched from the web.");
            none.setWrapText(true);
            none.setTextFill(Color.web("#5b6478"));
            apps.getChildren().add(none);
        } else {
            for (BundledApps.Bundled b : BundledApps.all()) {
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
                + " -fx-border-color:" + envColour(envId) + "; -fx-border-width: 4 0 0 0;");
        VBox.setVgrow(logView, Priority.ALWAYS);

        VBox root = new VBox(menuBar, body);
        VBox.setVgrow(body, Priority.ALWAYS);

        stage.setTitle("FxSuite Launcher — " + envId.toUpperCase());
        stage.setScene(new Scene(root, 660, 520));
        stage.show();

        String status = RegistrySetup.currentCommand(currentEnvId()) == null
                ? "not registered — use Settings ▸ Registration to enable web links"
                : "registered: fxsuite-" + currentEnvId() + ":// links will open this launcher";
        log(status);
    }

    private HBox appRow(BundledApps.Bundled b) {
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

        HBox row = new HBox(12, text, spacer, launch);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 14, 10, 14));
        row.setStyle("-fx-background-color:#ffffff; -fx-border-color:#d3dae8;"
                + " -fx-background-radius:8; -fx-border-radius:8;");
        return row;
    }

    private void launchBundled(BundledApps.Bundled b) {
        String envId = currentEnvId();
        new Thread(() -> {
            try {
                AppSpawner.spawnBundled(envId, b.mainClass(), List.of(b.appId(), b.version()));
                log("launched " + b.appId() + " v" + b.version() + " in " + envId);
            } catch (Exception ex) {
                log("could not launch " + b.appId() + ": " + ex.getMessage());
            }
        }, "launch-" + b.appId()).start();
    }

    private String currentEnvId() {
        String baked = EnvConfig.bakedEnv();
        if (baked != null) return baked;
        String typed = envField == null ? null : envField.getText().trim();
        return (typed == null || typed.isEmpty()) ? "dev" : typed;
    }

    private static String envColour(String env) {
        if (env.equals("prod")) return "#c62828";
        if (env.equals("uat")) return "#ef6c00";
        if (env.startsWith("dev")) return "#1565c0";
        return "#607d8b";
    }

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("HH:mm:ss");

    private void log(String line) {
        DiagLog.log("[ui] " + line);
        Platform.runLater(() -> {
            if (logView != null) logView.appendText(TS.format(LocalTime.now()) + "  " + line + "\n");
        });
    }
}
