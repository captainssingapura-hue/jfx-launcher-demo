package com.example.fxsuite.app.hello;

import java.util.List;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

/**
 * The Hello app's window. Runs in its own JVM, started by the launcher only
 * after the launch token has been verified — so this code trusts that it was
 * invoked legitimately and just presents its UI.
 *
 * <p>JavaFX classes here are compiled against {@code provided} JavaFX and
 * satisfied at runtime by the shared {@code fxsuite-javafx.jar} on the
 * classpath — nothing JavaFX is bundled in this app's jar.</p>
 */
public final class HelloApp extends Application {

    @Override
    public void start(Stage stage) {
        List<String> args = getParameters().getRaw();
        String requestedVer = args.size() > 1 ? args.get(1) : "?";
        String bundledVer = bundledVersion();      // baked into THIS jar's bytes
        String env = System.getProperty("fxsuite.env", "unknown");   // injected by the launcher

        // Environment must be unmistakable: with several environments open at once, the
        // dominant risk is acting in the wrong one.
        Label envBanner = new Label("  " + env.toUpperCase() + "  ");
        envBanner.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        envBanner.setTextFill(Color.WHITE);
        envBanner.setStyle("-fx-background-color:" + envColour(env) + "; -fx-background-radius:6;"
                + " -fx-padding:4 12 4 12;");

        Label heading = new Label("Hello, FxSuite  ·  v" + bundledVer);
        heading.setFont(Font.font("Segoe UI", FontWeight.BOLD, 26));
        heading.setTextFill(Color.web("#1b5e20"));

        Label blurb = new Label("A native JavaFX window, launched from a web link "
                + "and running in its own process. This exact version was downloaded "
                + "from the repository on demand and integrity-checked before launch.");
        blurb.setWrapText(true);
        blurb.setFont(Font.font("Segoe UI", 15));

        Label ver = new Label("requested v" + requestedVer + "  ·  bundled v" + bundledVer);
        ver.setFont(Font.font("Consolas", 13));
        ver.setTextFill(Color.web("#455a64"));

        Label proc = new Label("pid " + ProcessHandle.current().pid()
                + "  ·  JavaFX from shared fxsuite-javafx.jar");
        proc.setFont(Font.font("Consolas", 13));
        proc.setTextFill(Color.web("#455a64"));

        Label ok = new Label("✔ token verified by launcher → this process spawned");
        ok.setFont(Font.font("Consolas", 13));
        ok.setTextFill(Color.web("#2e7d32"));

        VBox root = new VBox(14, envBanner, heading, blurb, ver, proc, ok);
        root.setPadding(new Insets(28));
        root.setAlignment(Pos.CENTER_LEFT);
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #e8f5e9, #ffffff);"
                + " -fx-border-color:" + envColour(env) + "; -fx-border-width: 5 0 0 0;");

        stage.setTitle("[" + env.toUpperCase() + "] FxSuite — Hello v" + bundledVer);
        stage.setScene(new Scene(root, 580, 380));
        stage.show();
        stage.toFront();
    }

    /** Colour-code the environment: red = Production, amber = UAT, blue = dev, grey = unknown. */
    private static String envColour(String env) {
        if (env.equals("prod")) return "#c62828";
        if (env.equals("uat")) return "#ef6c00";
        if (env.startsWith("dev")) return "#1565c0";
        return "#607d8b";
    }

    /** Version baked into this jar (a resource added at publish time); "dev" if absent. */
    private static String bundledVersion() {
        try (var in = HelloApp.class.getResourceAsStream("/app-version")) {
            if (in == null) return "dev";
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            return "dev";
        }
    }
}
