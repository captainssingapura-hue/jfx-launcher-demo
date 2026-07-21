package com.example.fxsuite.setup;

import com.example.fxsuite.launcher.EnvConfig;
import com.example.fxsuite.launcher.setup.RegistryPlan;
import com.example.fxsuite.launcher.setup.RegistrySetup;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

/**
 * The FxSuite setup app.
 *
 * <p>Deliberately shows its work: pick environments, see the <b>exact</b> registry
 * changes that will be made, then apply them. Every action has an inverse — removing
 * an environment restores whatever handler was registered before FxSuite took over
 * the scheme, or deletes the key if there was none.</p>
 */
public final class SetupApp extends javafx.application.Application {

    /** One installable/removable environment. */
    public static final class EnvRow {
        final BooleanProperty selected = new SimpleBooleanProperty(false);
        final String envId;
        final Path launcherJar;     // null when the install is gone but a registration remains
        final String base;
        final String origin;
        String status = "";

        EnvRow(String envId, Path launcherJar, String base, String origin) {
            this.envId = envId; this.launcherJar = launcherJar; this.base = base; this.origin = origin;
        }
        public BooleanProperty selectedProperty() { return selected; }
    }

    private final ObservableList<EnvRow> rows = FXCollections.observableArrayList();
    private final TextArea planView = new TextArea();
    private final TextArea logView = new TextArea();
    private final ToggleGroup action = new ToggleGroup();
    private RadioButton installMode;
    private Path installRoot;
    private Path devInstall;        // shared dev launcher, if present

    @Override
    public void start(Stage stage) {
        installRoot = installRoot();

        Label title = new Label("FxSuite Setup");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        title.setTextFill(Color.web("#1a3a8f"));
        Label where = new Label("Install location: " + installRoot);
        where.setFont(Font.font("Consolas", 12));
        where.setTextFill(Color.web("#5b6478"));

        // --- environments table ---
        TableView<EnvRow> table = new TableView<>(rows);
        table.setEditable(true);
        table.setPrefHeight(190);

        TableColumn<EnvRow, Boolean> pick = new TableColumn<>("");
        pick.setCellValueFactory(c -> c.getValue().selected);
        pick.setCellFactory(CheckBoxTableCell.forTableColumn(pick));
        pick.setEditable(true);
        pick.setPrefWidth(34);

        TableColumn<EnvRow, String> env = new TableColumn<>("Environment");
        env.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().envId));
        env.setPrefWidth(110);

        TableColumn<EnvRow, String> scheme = new TableColumn<>("URL scheme");
        scheme.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                EnvConfig.scheme(c.getValue().envId) + "://"));
        scheme.setPrefWidth(150);

        TableColumn<EnvRow, String> status = new TableColumn<>("Current status");
        status.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().status));
        status.setPrefWidth(210);

        TableColumn<EnvRow, String> target = new TableColumn<>("Launcher");
        target.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().launcherJar == null ? "(install missing)"
                        : installRoot.relativize(c.getValue().launcherJar).toString()));
        target.setPrefWidth(230);

        table.getColumns().addAll(List.of(pick, env, scheme, status, target));

        // --- add a dev environment ---
        TextField devId = new TextField();   devId.setPromptText("dev3");           devId.setPrefWidth(90);
        TextField devBase = new TextField(); devBase.setPromptText("https://dev3.internal (optional)");
        HBox.setHgrow(devBase, Priority.ALWAYS);
        Button addDev = new Button("Add dev environment");
        addDev.setOnAction(e -> {
            String id = devId.getText().trim();
            if (id.isEmpty() || !EnvConfig.ENV_ID.matcher(id).matches()) { log("invalid environment id: '" + id + "'"); return; }
            if (devInstall == null) { log("no shared dev install found under " + installRoot); return; }
            if (rows.stream().anyMatch(r -> r.envId.equals(id))) { log(id + " is already listed"); return; }
            EnvRow r = new EnvRow(id, devInstall, devBase.getText().trim(), "dev");
            r.selected.set(true);
            rows.add(r);
            refreshStatuses();
            log("added " + id + " (uses the shared dev launcher)");
        });
        HBox devBox = new HBox(8, new Label("dev id:"), devId, devBase, addDev);
        devBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // --- action + plan ---
        installMode = new RadioButton("Install / update");
        RadioButton removeMode = new RadioButton("Remove (reversible)");
        installMode.setToggleGroup(action); removeMode.setToggleGroup(action);
        installMode.setSelected(true);
        action.selectedToggleProperty().addListener((o, a, b) -> showPlan());

        Button refresh = new Button("Refresh");
        refresh.setOnAction(e -> { discover(); showPlan(); });
        Button apply = new Button("Apply changes");
        apply.setStyle("-fx-background-color:#2748c8; -fx-text-fill:white; -fx-font-weight:bold;");
        apply.setOnAction(e -> applySelected());
        CheckBox all = new CheckBox("select all");
        all.setOnAction(e -> { rows.forEach(r -> r.selected.set(all.isSelected())); showPlan(); });

        HBox actions = new HBox(14, installMode, removeMode, all, refresh, apply);
        actions.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        planView.setEditable(false);
        planView.setFont(Font.font("Consolas", 12));
        planView.setPrefRowCount(9);
        logView.setEditable(false);
        logView.setFont(Font.font("Consolas", 12));
        logView.setPrefRowCount(6);

        VBox root = new VBox(10, title, where, table, devBox, actions,
                new Label("Exact registry changes that will be made:"), planView,
                new Label("Activity:"), logView);
        root.setPadding(new Insets(18));
        root.setStyle("-fx-background-color: linear-gradient(to bottom right,#eef2ff,#ffffff);");
        VBox.setVgrow(planView, Priority.ALWAYS);

        stage.setTitle("FxSuite Setup");
        stage.setScene(new Scene(root, 940, 720));
        stage.show();

        discover();
        // Recompute the plan whenever a tick box changes.
        rows.forEach(r -> r.selected.addListener((o, a, b) -> showPlan()));
        showPlan();
    }

    // --- discovery -------------------------------------------------------

    private void discover() {
        rows.clear();
        devInstall = null;
        List<EnvRow> found = new ArrayList<>();

        if (Files.isDirectory(installRoot)) {
            try (Stream<Path> dirs = Files.list(installRoot)) {
                for (Path d : dirs.filter(Files::isDirectory).sorted().toList()) {
                    Path jar = d.resolve("master-launcher.jar");
                    if (!Files.isRegularFile(jar)) continue;
                    Properties p = props(d.resolve("launcher.properties"));
                    String env = p.getProperty("env");
                    String base = p.getProperty("repo.base");
                    if (env != null && !env.isBlank()) {
                        found.add(new EnvRow(env.trim(), jar, base, "singleton install"));
                    } else {
                        devInstall = jar;    // shared dev install: env supplied per registration
                    }
                }
            } catch (Exception e) {
                log("could not scan " + installRoot + ": " + e.getMessage());
            }
        }

        // Registrations with no matching install still deserve a row — otherwise they
        // could never be removed from here.
        for (String e : RegistrySetup.registeredEnvs()) {
            if (found.stream().noneMatch(r -> r.envId.equals(e))) {
                Path jar = devInstall;   // best guess; only needed for the install direction
                found.add(new EnvRow(e, jar, null, "already registered"));
            }
        }

        rows.setAll(found);
        rows.forEach(r -> r.selected.addListener((o, a, b) -> showPlan()));
        refreshStatuses();
        log("found " + rows.size() + " environment(s)"
                + (devInstall == null ? "" : "; shared dev launcher present"));
    }

    private void refreshStatuses() {
        for (EnvRow r : rows) {
            String current = RegistrySetup.currentCommand(r.envId);
            if (current == null) {
                r.status = "not registered";
            } else if (r.launcherJar != null
                    && current.equals(RegistrySetup.commandFor(r.envId, r.launcherJar, r.base))) {
                r.status = "registered — this install";
            } else {
                r.status = "registered — different target";
            }
        }
        rows.sort((a, b) -> a.envId.compareTo(b.envId));
    }

    // --- plan + apply ----------------------------------------------------

    private List<RegistryPlan> plansForSelection() {
        boolean install = installMode.isSelected();
        List<RegistryPlan> plans = new ArrayList<>();
        for (EnvRow r : rows) {
            if (!r.selected.get()) continue;
            plans.add(install ? RegistrySetup.installPlan(r.envId, r.launcherJar, r.base)
                              : RegistrySetup.uninstallPlan(r.envId));
        }
        return plans;
    }

    private void showPlan() {
        List<RegistryPlan> plans = plansForSelection();
        if (plans.isEmpty()) {
            planView.setText("Tick one or more environments above to see exactly what will change.");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (RegistryPlan p : plans) {
            sb.append(p.action() == RegistryPlan.Action.INSTALL ? "INSTALL  " : "REMOVE   ")
              .append(p.scheme()).append("://   (environment '").append(p.envId()).append("')")
              .append(System.lineSeparator())
              .append(p.describe())
              .append(System.lineSeparator());
        }
        sb.append("All changes are under HKEY_CURRENT_USER — no administrator rights required.");
        planView.setText(sb.toString());
    }

    private void applySelected() {
        List<RegistryPlan> plans = plansForSelection();
        if (plans.isEmpty()) { log("nothing selected"); return; }
        for (RegistryPlan p : plans) {
            log(RegistrySetup.apply(p));
        }
        refreshStatuses();
        rows.sort((a, b) -> a.envId.compareTo(b.envId));
        // force the table to repaint the status column
        ObservableList<EnvRow> copy = FXCollections.observableArrayList(rows);
        rows.setAll(copy);
        rows.forEach(r -> r.selected.addListener((o, a, b) -> showPlan()));
        showPlan();
    }

    // --- helpers ---------------------------------------------------------

    /** Where this setup jar lives — the install root that holds the per-environment folders. */
    private static Path installRoot() {
        try {
            var src = SetupApp.class.getProtectionDomain().getCodeSource();
            if (src != null && src.getLocation() != null) {
                Path p = Path.of(src.getLocation().toURI()).toAbsolutePath();
                if (p.toString().toLowerCase().endsWith(".jar") && p.getParent() != null) return p.getParent();
            }
        } catch (Exception ignored) {}
        return Path.of(System.getProperty("user.dir")).toAbsolutePath();
    }

    private static Properties props(Path f) {
        Properties p = new Properties();
        if (Files.isRegularFile(f)) {
            try (InputStream in = Files.newInputStream(f)) { p.load(in); } catch (Exception ignored) {}
        }
        return p;
    }

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("HH:mm:ss");

    private void log(String line) {
        logView.appendText(TS.format(LocalTime.now()) + "  " + line + System.lineSeparator());
    }
}
