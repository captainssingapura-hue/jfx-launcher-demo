package com.example.acme.ledger;

import com.example.acme.charts.Charts;
import com.example.acme.contract.Transfer;
import com.example.acme.contract.Wire;
import com.example.acme.core.Amount;
import com.example.acme.net.Net;
import com.example.acme.text.Text;
import com.example.acme.ui.Panels;
import com.fasterxml.jackson.databind.ObjectMapper;
import hue.captains.singapura.tao.ontology.Mutable;
import java.util.List;
import java.util.Map;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public final class LedgerApp extends Application implements Mutable {

    @Override
    public void start(final Stage stage) {
        final Screen screen = new Screen();
        final VBox body = new VBox(4);
        body.setPadding(new Insets(18));
        final Text text = new Text();
        final Net net = new Net();
        final Charts charts = new Charts();
        body.getChildren().addAll(
                screen.heading("Ledger 1.20261101.0"),
                screen.section("Running with"),
                screen.check("AcmeText", text::release),
                screen.check("AcmeNet", net::release),
                screen.check("AcmeCore", net::coreRelease),
                screen.check("AcmeUi", () -> new Panels().release()),
                screen.check("AcmeCharts", charts::release),
                screen.check("AcmeContract", () -> new Wire().release()),
                screen.check("Jackson", () -> ObjectMapper.class.getPackage().getImplementationVersion()),
                screen.section("Doing"),
                screen.check("Accounts", () -> text.list(List.of("cash", "bank", "card"))),
                screen.check("Text", text::origin),
                screen.check("Payment", () -> net.send(new Amount(1250))),
                screen.check("Negative amounts", () -> net.acceptsNegativeAmounts()
                        ? "ACCEPTED: this AcmeCore still has the flaw" : "rejected"),
                screen.check("Wire", () -> "compiled for version " + Wire.WIRE_VERSION
                        + ", running version " + new Wire().version()),
                screen.check("Transfer as JSON", () -> new ObjectMapper()
                        .writeValueAsString(new Transfer("alice", "bob", 1250))));
        Node chart;
        try {
            chart = charts.bars("Ledger", Map.of("Mon", 3, "Tue", 5, "Wed", 2, "Thu", 7));
        } catch (Exception | LinkageError e) {
            chart = screen.failed("Chart", e);
        }
        body.getChildren().add(chart);

        body.getChildren().addAll(screen.section("Classpath"), screen.classpath());

        stage.setTitle(System.getProperty("acme.release", "Ledger 1.20261101.0"));
        stage.setScene(new Scene(new ScrollPane(body), 700, 900));
        stage.show();
    }
}
