package com.example.fxsuite.viewer;

import com.example.fxsuite.estate.DemoEstate;
import com.example.fxsuite.estate.KnownAdvisories;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Optional;

/**
 * The whole demo estate in 3D: every release of every unit. Click a release to see what it
 * resolves to.
 *
 * <p>For an unattended picture: {@code -Destate.snapshot=out.png} saves the window and exits,
 * and {@code -Destate.select="Ledger 1.20261101.0"} selects a release first.
 */
public final class EstateViewer extends Application {

    @Override
    public void start(final Stage stage) {
        final ViewerChrome chrome = new ViewerChrome();
        final Releases releases = new Releases(new DemoEstate().units());
        final KnownAdvisories advisories = new KnownAdvisories();
        final SecurityMarks security = new SecurityMarks(advisories.all(), advisories.asOf());
        final SizeProvider sizes = new SimulatedSizes();
        final DependencyView view = new DependencyView(security, sizes);
        final VBox details = new VBox(10);

        view.setOnSelect(highlight -> chrome.details(details, highlight,
                view.layout().of(highlight.root()).orElseThrow(), security, sizes));
        view.setOnClear(() -> details.getChildren().setAll(chrome.muted("Nothing selected.")));
        view.show(new EstateLayout(releases.all()));

        final BorderPane root = new BorderPane(view.node());
        root.setRight(chrome.sidePanel(
                chrome.title("Demo estate"),
                chrome.counts(view.layout()),
                chrome.legend(sizes),
                chrome.help("Click a release to see what it resolves to · Esc clears"),
                new Region(),
                details));
        final Scene scene = chrome.scene(root);
        view.installKeys(scene);

        stage.setTitle("FxSuite · Estate viewer");
        stage.setScene(scene);
        stage.show();

        Optional.ofNullable(System.getProperty("estate.select")).flatMap(view::find).ifPresent(view::select);
        chrome.snapshotIfAsked(scene);
    }
}
