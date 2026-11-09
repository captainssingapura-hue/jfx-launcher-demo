package com.example.fxsuite.viewer;

import com.example.fxsuite.deployment.Version;
import com.example.fxsuite.launch.AppLauncher;
import com.example.fxsuite.launch.Installer;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Launches the chosen release with {@link AppLauncher}: the same resolution, installation
 * into the release's own directory, and JVM of its own as the command line. Shows whether
 * the release is installed or running, and what happened.
 */
final class LaunchPanel {

    private final ViewerChrome chrome;
    private final AppLauncher launcher;
    /** Releases started from this window, by installation directory. */
    private final Map<Path, Process> running = new HashMap<>();

    private final Button launch = new Button("Launch");
    private final Button reinstall = new Button("Reinstall and launch");
    private final CheckBox nearestWins = new CheckBox("Nearest-wins classpath, to see what goes wrong");
    private final VBox status = new VBox(4);
    private final VBox node;
    private Version<?> release;
    private boolean busy;

    LaunchPanel(final ViewerChrome chrome, final AppLauncher launcher) {
        this.chrome = chrome;
        this.launcher = launcher;
        launch.getStyleClass().add("launch");
        launch.setOnAction(e -> launch(false));
        reinstall.setOnAction(e -> launch(true));
        nearestWins.setOnAction(e -> refresh());
        node = new VBox(8, new HBox(8, launch, reinstall), nearestWins, status);
        show(null);
    }

    Node node() {
        return node;
    }

    /** The release the panel is for; null for none. */
    void show(final Version<?> release) {
        this.release = release;
        refresh();
    }

    private void refresh() {
        reinstall.setVisible(false);
        reinstall.setManaged(false);
        if (release == null) {
            launch.setDisable(true);
            status.getChildren().setAll(chrome.muted("Pick an application and a version."));
            return;
        }
        final AppLauncher.Prepared prepared = launcher.prepare(release, nearestWins.isSelected());
        launch.setDisable(busy || !prepared.canLaunch());
        if (!prepared.canLaunch()) {
            status.getChildren().setAll(chrome.styled("Cannot launch: its dependencies do not resolve.", "conflict"));
            return;
        }
        final Process process = running.get(prepared.dir());
        if (process != null && process.isAlive()) {
            status.getChildren().setAll(chrome.styled("Running, pid " + process.pid(), "running"),
                    chrome.muted("Output: " + prepared.log()));
        } else if (prepared.isInstalled()) {
            status.getChildren().setAll(chrome.muted("Installed in " + prepared.dir()));
        } else {
            status.getChildren().setAll(chrome.muted("Not installed yet. Launching copies its "
                    + prepared.jars().size() + " jars into " + prepared.dir()));
        }
        if (prepared.nearestWins()) {
            status.getChildren().add(chrome.styled("Uses the classpath a nearest-wins build tool would make,"
                    + " not the model's resolution.", "warning"));
        }
    }

    /** Launches the current release, as the Launch button does. */
    void launchNow() {
        if (release != null && !launch.isDisabled()) {
            launch(false);
        }
    }

    private void launch(final boolean reinstalling) {
        final AppLauncher.Prepared prepared = launcher.prepare(release, nearestWins.isSelected());
        busy = true;
        launch.setDisable(true);
        reinstall.setVisible(false);
        reinstall.setManaged(false);
        status.getChildren().setAll(chrome.muted((reinstalling ? "Reinstalling" : "Installing") + " and starting "
                + prepared.label() + " ..."));

        final Thread worker = new Thread(() -> {
            try {
                final Installer.Installation installation = launcher.install(prepared, reinstalling);
                final Process process = launcher.start(prepared, installation);
                Platform.runLater(() -> {
                    running.put(prepared.dir(), process);
                    process.onExit().thenRun(() -> Platform.runLater(this::refresh));
                    done();
                    status.getChildren().add(0, chrome.muted(installation.outcome()));
                });
            } catch (Installer.ReleaseChanged e) {
                Platform.runLater(() -> {
                    done();
                    status.getChildren().setAll(chrome.styled(e.getMessage(), "conflict"),
                            chrome.muted("A real fix publishes a new version. Reinstalling takes the new bytes anyway."));
                    reinstall.setVisible(true);
                    reinstall.setManaged(true);
                });
            } catch (Installer.MissingJars e) {
                failed(e.getMessage() + "\nPublish the sample estate first, from the repository root:"
                        + " mvn -f sample-estate/pom.xml deploy");
            } catch (IOException | RuntimeException e) {
                failed(e.getMessage());
            }
        }, "launch " + prepared.label());
        worker.setDaemon(true);
        worker.start();
    }

    private void done() {
        busy = false;
        refresh();
    }

    private void failed(final String message) {
        Platform.runLater(() -> {
            done();
            status.getChildren().setAll(chrome.styled(message, "conflict"));
        });
    }
}
