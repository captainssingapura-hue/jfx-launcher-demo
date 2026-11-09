package com.example.acme.atlas;

import com.example.acme.core.Amount;
import com.example.acme.net.Net;
import com.example.acme.text.Text;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import hue.captains.singapura.tao.ontology.Mutable;
import java.util.List;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.datatype.guava.GuavaModule;

public final class AtlasApp extends Application implements Mutable {

    @Override
    public void start(final Stage stage) {
        final Screen screen = new Screen();
        final VBox body = new VBox(4);
        body.setPadding(new Insets(18));
        final Net net = new Net();
        final JsonMapper json = JsonMapper.builder().addModule(new GuavaModule()).build();
        final Text text = new Text();
        body.getChildren().addAll(
                screen.heading("Atlas 1.20261121.0"),
                screen.section("Running with"),
                screen.check("AcmeNet", net::release),
                screen.check("AcmeCore", net::coreRelease),
                screen.check("AcmeText", text::release),
                screen.check("Jackson", () -> JsonMapper.class.getPackage().getImplementationVersion()),
                screen.check("Guava", () -> ImmutableList.class.getPackage().getImplementationVersion()),
                screen.section("Doing"),
                screen.check("Payment", () -> net.send(new Amount(990))),
                screen.check("Negative amounts", () -> net.acceptsNegativeAmounts()
                        ? "ACCEPTED: this AcmeCore still has the flaw" : "rejected"),
                screen.check("Regions", () -> text.list(List.of("north", "south", "west"))),
                screen.check("Text", text::origin),
                screen.check("Stores as JSON", () -> json.writeValueAsString(
                        ImmutableMap.of("region", "north", "stores", ImmutableList.of(3, 5, 8)))));

        body.getChildren().addAll(screen.section("Classpath"), screen.classpath());

        stage.setTitle(System.getProperty("acme.release", "Atlas 1.20261121.0"));
        stage.setScene(new Scene(new ScrollPane(body), 680, 760));
        stage.show();
    }
}
