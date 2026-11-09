package com.example.acme.ui;

import com.example.acme.text.Text;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;
import javafx.scene.control.Label;
import java.util.List;

/**
 * AcmeUi 1.20261021.0: the controls Acme apps share.
 */
public final class Panels implements StatelessFunctionalObject {

    public String release() {
        return "AcmeUi 1.20261021.0";
    }

    public Label banner(final String title) {
        final Label label = new Label(new Text().banner(title));
        label.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        return label;
    }

    /** Needs AcmeText's {@code list}, added in 3.20261020.0. */
    public Label items(final List<String> items) {
        return new Label(new Text().list(items));
    }
}
