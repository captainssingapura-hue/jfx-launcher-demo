package com.example.fxsuite.launcher.ui;

import com.example.fxsuite.launcher.Install;
import com.example.fxsuite.launcher.ProtocolRegistrar;
import com.example.fxsuite.launcher.setup.Keys;
import com.example.fxsuite.launcher.setup.RegistryPlan;
import com.example.fxsuite.launcher.setup.RegistrySetup;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Settings for this environment — the former standalone setup app, now where users look
 * for it: under the menu bar.
 *
 * <ul>
 *   <li><b>Registration</b> — shows the exact registry changes before making them, and
 *       removal restores whatever handler was there before FxSuite took the scheme.</li>
 *   <li><b>Signing keys</b> — generate a pair into {@code <keys root>/<env>/}, install the
 *       public half beside this launcher, and check it matches the anchor in force.</li>
 * </ul>
 */
final class SettingsWindow {

    private SettingsWindow() {}

    static void open(Stage owner, String envId, Consumer<String> log) {
        show(owner, envId, log, 0);
    }

    static void openOnKeys(Stage owner, String envId, Consumer<String> log) {
        show(owner, envId, log, 1);
    }

    private static void show(Stage owner, String envId, Consumer<String> log, int tabIndex) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.NONE);
        stage.setTitle("Settings — " + envId);

        TabPane tabs = new TabPane();
        tabs.getTabs().addAll(registrationTab(envId, log), keysTab(envId, log));
        tabs.getSelectionModel().select(tabIndex);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        stage.setScene(new Scene(tabs, 820, 560));
        stage.show();
    }

    // ── Registration ─────────────────────────────────────────────────────

    private static Tab registrationTab(String envId, Consumer<String> log) {
        TextArea plan = new TextArea();
        plan.setEditable(false);
        plan.setFont(Font.font("Consolas", 12));
        VBox.setVgrow(plan, Priority.ALWAYS);

        Label status = new Label();
        Runnable refresh = () -> {
            String current = RegistrySetup.currentCommand(envId);
            status.setText(current == null
                    ? "fxsuite-" + envId + ":// is NOT registered"
                    : "fxsuite-" + envId + ":// is registered → " + current);
        };
        refresh.run();

        Button showInstall = new Button("Preview install");
        Button showRemove = new Button("Preview removal");
        Button apply = new Button("Apply shown plan");
        apply.setStyle("-fx-background-color:#2748c8; -fx-text-fill:white; -fx-font-weight:bold;");

        final RegistryPlan[] pending = new RegistryPlan[1];

        showInstall.setOnAction(e -> {
            try {
                pending[0] = RegistrySetup.installPlan(envId, ProtocolRegistrar.ownJarPath(), null);
                plan.setText("INSTALL  fxsuite-" + envId + "://\n\n" + pending[0].describe()
                        + "\nAll changes are under HKEY_CURRENT_USER — no administrator rights needed.");
            } catch (Exception ex) { plan.setText("Could not build a plan: " + ex.getMessage()); }
        });
        showRemove.setOnAction(e -> {
            pending[0] = RegistrySetup.uninstallPlan(envId);
            plan.setText("REMOVE  fxsuite-" + envId + "://\n\n" + pending[0].describe()
                    + "\nRemoval restores any handler that existed before FxSuite registered this scheme.");
        });
        apply.setOnAction(e -> {
            if (pending[0] == null) { plan.setText("Preview a change first."); return; }
            String result = RegistrySetup.apply(pending[0]);
            log.accept(result);
            plan.appendText("\n\n→ " + result);
            pending[0] = null;
            refresh.run();
        });

        HBox buttons = new HBox(10, showInstall, showRemove, apply);
        buttons.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(10,
                new Label("Web links only reach this launcher once its URL scheme is registered."),
                status, buttons, new Label("Exact registry changes:"), plan);
        box.setPadding(new Insets(16));

        Tab tab = new Tab("Registration", box);
        return tab;
    }

    // ── Signing keys ─────────────────────────────────────────────────────

    private static Tab keysTab(String envId, Consumer<String> log) {
        TextArea out = new TextArea();
        out.setEditable(false);
        out.setFont(Font.font("Consolas", 12));
        VBox.setVgrow(out, Priority.ALWAYS);

        TextField root = new TextField(defaultKeysRoot().toString());
        HBox.setHgrow(root, Priority.ALWAYS);
        Button browse = new Button("Browse…");
        browse.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Choose the keys root (a sub-folder is made per environment)");
            Path cur = Path.of(root.getText());
            if (Files.isDirectory(cur)) dc.setInitialDirectory(cur.toFile());
            File chosen = dc.showDialog(root.getScene().getWindow());
            if (chosen != null) root.setText(chosen.toPath().toString());
        });

        Runnable showAnchor = () -> {
            try {
                Path jar = ProtocolRegistrar.ownJarPath();
                Keys.Anchor a = Keys.installedAnchor(envId, jar == null ? null : jar.getParent(), jar);
                out.setText("Trust anchor in force for '" + envId + "': " + a.describe()
                        + "\nfingerprint: " + a.fingerprint() + "\n");
            } catch (Exception ex) { out.setText("Could not read the trust anchor: " + ex.getMessage()); }
        };
        showAnchor.run();

        Button gen = new Button("Generate keypair");
        gen.setOnAction(e -> {
            try {
                String fp = Keys.generate(Path.of(root.getText().trim()), envId);
                out.appendText("\ngenerated keypair for '" + envId + "' (fingerprint " + fp + ")"
                        + "\n  private half is the SERVER's — hand it to the token issuer, never to a workstation\n");
                log.accept("generated keypair for " + envId);
            } catch (Exception ex) { out.appendText("\ncould not generate: " + ex.getMessage() + "\n"); }
        });

        Button install = new Button("Install public key here");
        install.setOnAction(e -> {
            try {
                Path jar = ProtocolRegistrar.ownJarPath();
                Keys.installPublic(Path.of(root.getText().trim()), envId, jar.getParent());
                out.appendText("\ninstalled the public key beside this launcher\n");
                log.accept("installed public key for " + envId);
                showAnchor.run();
            } catch (Exception ex) { out.appendText("\ncould not install: " + ex.getMessage() + "\n"); }
        });

        Button check = new Button("Check match");
        check.setOnAction(e -> {
            Path jar = ProtocolRegistrar.ownJarPath();
            boolean ok = Keys.matches(Path.of(root.getText().trim()), envId,
                    jar == null ? null : jar.getParent(), jar);
            out.appendText(ok ? "\nprivate key MATCHES the anchor this launcher trusts ✓\n"
                              : "\nprivate key does NOT match the anchor this launcher trusts ✗\n");
        });

        HBox rootRow = new HBox(8, new Label("Keys root:"), root, browse);
        rootRow.setAlignment(Pos.CENTER_LEFT);
        HBox actions = new HBox(10, gen, install, check);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(10,
                new Label("Keys live in <keys root>/" + envId + "/ — the private half belongs to the server."),
                rootRow, actions, out);
        box.setPadding(new Insets(16));
        return new Tab("Signing keys", box);
    }

    private static Path defaultKeysRoot() {
        try {
            Path root = Install.root();
            Path parent = root.getParent();
            return (parent != null ? parent : root).resolve("keys");
        } catch (Exception e) {
            return Path.of(System.getProperty("user.dir"), "keys");
        }
    }
}
