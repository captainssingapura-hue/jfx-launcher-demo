package com.example.fxsuite.viewer;

import com.example.fxsuite.deployment.BundleRelease;
import com.example.fxsuite.deployment.Exposure;
import com.example.fxsuite.deployment.ExternalArtifact;
import com.example.fxsuite.deployment.Version;
import com.example.fxsuite.deployment.VersionText;
import com.example.fxsuite.deployment.Violation;
import com.example.fxsuite.deployment.Warning;
import com.example.fxsuite.viewer.EstateLayout.Placed;
import com.example.fxsuite.viewer.Highlighter.Highlight;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;

/**
 * The parts of a viewer window both apps share: the side panel's pieces, the details of a
 * selected release, the stylesheet, and saving a picture for runs without a mouse.
 */
public final class ViewerChrome {

    private final VersionText text = new VersionText();

    /** A window's scene with the viewer stylesheet. */
    public Scene scene(final Parent root) {
        final Scene scene = new Scene(root, 1440, 880);
        scene.getStylesheets().add(ViewerChrome.class.getResource("/viewer.css").toExternalForm());
        return scene;
    }

    /** The side panel: the given rows, scrollable. */
    public ScrollPane sidePanel(final Node... rows) {
        final VBox panel = new VBox(14, rows);
        panel.getStyleClass().add("side");
        final ScrollPane scroll = new ScrollPane(panel);
        scroll.setFitToWidth(true);
        scroll.setPrefWidth(380);
        scroll.getStyleClass().add("side-scroll");
        return scroll;
    }

    public Label title(final String title) {
        return styled(title, "title");
    }

    public Label counts(final EstateLayout layout) {
        return muted(countsText(layout));
    }

    public String countsText(final EstateLayout layout) {
        return layout.placed().size() + " releases · " + layout.edges().size() + " dependencies · "
                + (layout.placed().isEmpty() ? 0 : layout.maxDepth() + 1) + " layers";
    }

    public VBox legend() {
        return new VBox(6,
                swatch("bundle", "Bundle of external libraries"),
                swatch("library", "Internal library"),
                swatch("application", "Application"),
                swatch("virtual-sample", "Virtual release: same binary, newer dependencies"),
                swatch("vulnerable-sample", "⚠ CVE: a bundle bringing a jar with a known vulnerability"),
                swatch("insecure-sample", "⚠ insecure: an app release that runs with one"));
    }

    public Label help(final String extra) {
        return muted("Drag to orbit · right-drag to pan · scroll to zoom · R resets the view\n" + extra);
    }

    /** Fills {@code into} with what the highlight says about its release, and its known vulnerabilities. */
    public void details(final VBox into, final Highlight highlight, final Placed placed, final SecurityMarks security) {
        final String kind = switch (placed.kind()) {
            case BUNDLE -> "Bundle";
            case LIBRARY -> "Library";
            case APPLICATION -> "Application";
        };
        into.getChildren().setAll(styled(text.named(highlight.root()), "selection"),
                muted(kind + " · depth " + placed.depth() + (placed.isVirtual() ? " · virtual release" : "")));

        security(into, highlight.root(), placed, security);

        into.getChildren().add(section("Declares"));
        final List<Version<?>> declared = sorted(highlight.root().dependencies().versions());
        if (declared.isEmpty()) {
            into.getChildren().add(muted("No internal dependencies."));
        }
        declared.forEach(v -> into.getChildren().add(new Label(text.named(v))));

        if (highlight.root() instanceof BundleRelease bundle) {
            final List<String> jars = bundle.members().artifacts().stream()
                    .map(ExternalArtifact::coordinates).sorted().toList();
            into.getChildren().add(section("Brings " + jars.size() + " external jars"));
            jars.forEach(jar -> into.getChildren().add(styled(jar, "jar")));
        }

        final List<Version<?>> resolved = sorted(highlight.resolution().resolved().versions());
        into.getChildren().add(section("Runs with " + resolved.size() + " releases"));
        resolved.forEach(v -> into.getChildren().add(new Label(text.named(v))));

        for (final Warning warning : highlight.resolution().warnings().items()) {
            into.getChildren().addAll(section("Warning"), styled(warning.message(), "warning"));
        }
        for (final Violation conflict : highlight.resolution().conflicts().items()) {
            into.getChildren().addAll(section("Conflict"), styled(conflict.message(), "conflict"));
        }
    }

    private void security(final VBox into, final Version<?> release, final Placed placed, final SecurityMarks security) {
        final List<Exposure> exposures = security.exposures(release);
        final boolean bundle = placed.kind() == EstateLayout.Kind.BUNDLE;
        if (exposures.isEmpty()) {
            into.getChildren().addAll(section("Security"), styled("No known vulnerabilities in "
                    + (bundle ? "the jars it brings" : "anything it runs with")
                    + " (advisories from OSV, " + security.asOf() + ").", "secure"));
            return;
        }
        final long advisories = exposures.stream().map(e -> e.advisory().id()).distinct().count();
        into.getChildren().addAll(section("Security"), styled(switch (placed.kind()) {
            case BUNDLE -> "⚠ Brings jars with " + advisories + " known vulnerabilities";
            case APPLICATION -> "⚠ Insecure: runs with " + advisories + " known vulnerabilities";
            case LIBRARY -> "⚠ Runs with " + advisories + " known vulnerabilities";
        }, "insecure-text"));
        for (final Exposure exposure : exposures) {
            final ExternalArtifact jar = exposure.jar();
            into.getChildren().addAll(
                    styled(exposure.advisory().name() + "  " + exposure.advisory().severity(), "advisory"),
                    muted(exposure.advisory().summary()),
                    styled(jar.artifact() + " " + jar.version()
                            + (bundle ? "" : " via " + text.named(exposure.bundle()))
                            + " · fixed in " + exposure.fixedIn(), "jar"));
        }
    }

    public Label section(final String title) {
        return styled(title.toUpperCase(), "section");
    }

    public Label muted(final String message) {
        return styled(message, "muted");
    }

    public Label styled(final String message, final String styleClass) {
        final Label label = new Label(message);
        label.getStyleClass().add(styleClass);
        label.setWrapText(true);
        return label;
    }

    /**
     * When {@code -Destate.snapshot=<png>} is set, saves the window once it has settled and
     * exits. For runs without a mouse.
     */
    public void snapshotIfAsked(final Scene scene) {
        final String path = System.getProperty("estate.snapshot");
        if (path == null) {
            return;
        }
        final PauseTransition settle = new PauseTransition(Duration.millis(600));
        settle.setOnFinished(e -> {
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
        settle.play();
    }

    private List<Version<?>> sorted(final java.util.Set<? extends Version<?>> versions) {
        return versions.stream().<Version<?>>map(v -> v).sorted(Comparator.comparing(text::named)).toList();
    }

    private HBox swatch(final String kind, final String meaning) {
        final Region box = new Region();
        box.getStyleClass().addAll("swatch", kind);
        final Label label = new Label(meaning);
        label.setWrapText(true);
        return new HBox(10, box, label);
    }
}
