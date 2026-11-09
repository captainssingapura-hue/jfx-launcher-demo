package com.example.fxsuite.viewer;

import com.example.fxsuite.deployment.DeploymentUnit;
import com.example.fxsuite.deployment.Version;
import com.example.fxsuite.deployment.VersionText;
import com.example.fxsuite.deployment.VirtualVersion;
import com.example.fxsuite.estate.DemoEstate;
import com.example.fxsuite.launch.AppLauncher;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * One application at a time: pick an app, then one of its versions, and see only the
 * releases that version depends on, directly or through other releases. The chosen version
 * is highlighted with what it resolves to; clicking another release shows that release's
 * own resolution, and Esc or a click on empty space goes back to the chosen version.
 *
 * <p>Uses the same rendering engine as {@link EstateViewer}.
 *
 * <p>The chosen version can be launched from here, through the same {@link AppLauncher} as
 * the command line: its own directory of jars, its own JVM. Jars come from
 * {@code dist/acme-repo} under the current directory, or {@code -Destate.repo=<dir>} first.
 *
 * <p>For an unattended picture: {@code -Destate.snapshot=out.png} saves the window and exits,
 * and {@code -Destate.select="Ledger 1.20261101.0"} picks the app and version first;
 * {@code -Destate.launch=true} then launches it.
 */
public final class AppDependenciesViewer extends Application {

    private final ViewerChrome chrome = new ViewerChrome();
    private final VersionText text = new VersionText();
    private final Releases releases = new Releases(new DemoEstate().units());
    private final DependencyView view = new DependencyView();
    private final VBox details = new VBox(10);
    private final Label counts = chrome.muted("");
    private final LaunchPanel launchPanel = new LaunchPanel(chrome, new AppLauncher(AppLauncher.Settings.defaults(
            Optional.ofNullable(System.getProperty("estate.repo")).map(Path::of), Optional.empty())));
    private Version<?> chosen;

    @Override
    public void start(final Stage stage) {
        final ComboBox<Class<? extends DeploymentUnit>> apps =
                new ComboBox<>(FXCollections.observableArrayList(releases.applications()));
        apps.setConverter(new StringConverter<>() {
            @Override public String toString(final Class<? extends DeploymentUnit> unit) {
                return unit == null ? "" : unit.getSimpleName();
            }
            @Override public Class<? extends DeploymentUnit> fromString(final String name) {
                return null;
            }
        });
        apps.setMaxWidth(Double.MAX_VALUE);

        final ComboBox<Version<?>> versions = new ComboBox<>();
        versions.setConverter(new StringConverter<>() {
            @Override public String toString(final Version<?> version) {
                return version == null ? ""
                        : text.number(version) + (version instanceof VirtualVersion<?, ?> ? "  · virtual" : "");
            }
            @Override public Version<?> fromString(final String number) {
                return null;
            }
        });
        versions.setMaxWidth(Double.MAX_VALUE);

        // newest version first, and chosen straight away
        apps.valueProperty().addListener((obs, old, app) -> {
            final List<Version<?>> newestFirst = new ArrayList<>(releases.of(app));
            Collections.reverse(newestFirst);
            versions.setItems(FXCollections.observableArrayList(newestFirst));
            versions.getSelectionModel().selectFirst();
        });
        versions.valueProperty().addListener((obs, old, version) -> {
            if (version != null) {
                show(version);
            }
        });

        view.setOnSelect(highlight -> chrome.details(details, highlight,
                view.layout().of(highlight.root()).orElseThrow()));
        view.setOnClear(() -> details.getChildren().setAll(chrome.muted("Pick an application and a version.")));
        view.setOnBackground(() -> {
            if (chosen != null) {
                view.select(chosen);
            } else {
                view.clear();
            }
        });

        final BorderPane root = new BorderPane(view.node());
        root.setRight(chrome.sidePanel(
                chrome.title("App dependencies"),
                chrome.section("Application"), apps,
                chrome.section("Version"), versions,
                chrome.section("Launch"), launchPanel.node(),
                counts,
                chrome.legend(),
                chrome.help("Click a release to see its own resolution · Esc goes back to the app"),
                new Region(),
                details));
        final Scene scene = chrome.scene(root);
        view.installKeys(scene);

        stage.setTitle("FxSuite · App Dependencies Viewer");
        stage.setScene(scene);
        stage.show();

        final String requested = System.getProperty("estate.select");
        final Version<?> initial = requested == null ? null : releases.all().stream()
                .filter(v -> text.named(v).equals(requested)).findFirst().orElse(null);
        if (initial != null) {
            apps.getSelectionModel().select(releases.applications().stream()
                    .filter(app -> releases.of(app).contains(initial)).findFirst().orElse(null));
            versions.getSelectionModel().select(initial);
            if (Boolean.getBoolean("estate.launch")) {
                launchPanel.launchNow();
            }
        } else if (!releases.applications().isEmpty()) {
            apps.getSelectionModel().selectFirst();
        }
        chrome.snapshotIfAsked(scene);
    }

    /** Draws only what the version reaches, and highlights what it resolves to. */
    private void show(final Version<?> version) {
        chosen = version;
        launchPanel.show(version);
        view.show(new EstateLayout(releases.closure(version)));
        counts.setText(chrome.countsText(view.layout()));
        view.select(version);
    }
}
