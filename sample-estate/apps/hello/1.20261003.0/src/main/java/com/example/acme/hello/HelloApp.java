package com.example.acme.hello;

import hue.captains.singapura.tao.ontology.Mutable;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public final class HelloApp extends Application implements Mutable {

    @Override
    public void start(final Stage stage) {
        final Screen screen = new Screen();
        final VBox body = new VBox(4);
        body.setPadding(new Insets(18));
        body.getChildren().addAll(
                screen.heading("Hello 1.20261003.0"),
                screen.line("Hello from the Acme estate."),
                screen.section("Running with"),
                screen.check("Java", () -> System.getProperty("java.version")),
                screen.check("JavaFX", () -> System.getProperty("javafx.runtime.version")));

        body.getChildren().addAll(screen.section("Classpath"), screen.classpath());

        stage.setTitle(System.getProperty("acme.release", "Hello 1.20261003.0"));
        stage.setScene(new Scene(new ScrollPane(body), 680, 760));
        stage.show();
    }
}
