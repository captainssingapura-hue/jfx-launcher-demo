package com.example.acme.atlas;

import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * How every Acme app shows itself: which release it is, what it runs with, the result of
 * each thing it tries, and the exact classpath it was started with.
 */
final class Screen implements StatelessFunctionalObject {

    /** One thing the app tries. Any error, including a missing method, is shown instead of thrown. */
    interface Check extends StatelessFunctionalObject {
        String run() throws Exception;
    }

    /**
     * The release, as the launcher names it. A virtual release runs the binary of its physical
     * base, which is what {@code binary} names.
     */
    Label heading(final String binary) {
        final String release = System.getProperty("acme.release", binary);
        final Label label = new Label(release.equals(binary) ? release : release + "   (binary " + binary + ")");
        label.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
        return label;
    }

    Label section(final String title) {
        final Label label = new Label(title.toUpperCase());
        label.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #666; -fx-padding: 10 0 0 0;");
        return label;
    }

    Label check(final String what, final Check check) {
        try {
            return line(what + ":  " + check.run(), "#1d2b1f");
        } catch (Exception | LinkageError e) {
            return line(what + ":  FAILED  " + e, "#b3261e");
        }
    }

    Label failed(final String what, final Throwable error) {
        return line(what + ":  FAILED  " + error, "#b3261e");
    }

    Label line(final String text) {
        return line(text, "#1d2b1f");
    }

    /** Each jar on the classpath, by file name, in classpath order. */
    VBox classpath() {
        final VBox jars = new VBox(2);
        Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator))
                .map(entry -> Path.of(entry).getFileName().toString())
                .forEach(name -> jars.getChildren().add(line(name, "#444")));
        return jars;
    }

    private Label line(final String text, final String colour) {
        final Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 12.5px; -fx-text-fill: " + colour + ";");
        return label;
    }
}
