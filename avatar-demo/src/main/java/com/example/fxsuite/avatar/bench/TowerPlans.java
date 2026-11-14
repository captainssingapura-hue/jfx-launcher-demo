package com.example.fxsuite.avatar.bench;

import com.example.fxsuite.avatar.build.Tower;

import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.util.StringConverter;

import java.util.Locale;

/**
 * A tower's plans, flat, a section at a time ({@link Tower.Section}): which section picked, its plan
 * drawn ({@link PlanView}), and its figures read out: where it stands, how many floors how high, its
 * floor, its columns; and what is wrong with the tower.
 */
final class TowerPlans extends BorderPane {

    private final ComboBox<Tower.Section> sections = new ComboBox<>();
    private final PlanView view = new PlanView();
    private final Label figures = new Label();
    private final Tower tower;

    TowerPlans(final Tower tower) {
        this.tower = tower;
        sections.setConverter(new StringConverter<>() {
            @Override
            public String toString(final Tower.Section section) {
                return section == null ? "" : section.name();
            }

            @Override
            public Tower.Section fromString(final String name) {
                return null;
            }
        });
        sections.getItems().setAll(tower.sections());
        sections.setValue(tower.sections().getFirst());
        sections.setOnAction(e -> show());
        final HBox top = new HBox(8, new Label("Section"), sections);
        top.setPadding(new Insets(10));
        setTop(top);
        setCenter(view);
        figures.setFont(Font.font("Monospaced", 12));
        figures.setPadding(new Insets(8, 10, 10, 10));
        setBottom(figures);
        show();
    }

    private void show() {
        final Tower.Section section = sections.getValue();
        final int index = tower.sections().indexOf(section);
        final var layout = section.plan().layout();
        view.show(layout);
        final StringBuilder out = new StringBuilder(String.format(Locale.ROOT,
                "%s: from %.1f m up, %d floors of %.1f m; %.0f m² a floor; %d columns (%d round its curves), %d beams",
                section.name(), tower.base(index) / 100, section.floors(), section.height() / 100, layout.area() / 10_000,
                layout.allColumns().size(), layout.perimeterColumns().size(), layout.beams().size() + layout.ties().size()));
        out.append(String.format(Locale.ROOT, "%n%s: %d floors, %.1f m to its roof", tower.name(), tower.floors(),
                tower.height() / 100));
        out.append(tower.problems().isEmpty() ? "; nothing wrong with it" : "");
        tower.problems().forEach(p -> out.append("\n! ").append(p));
        figures.setText(out.toString());
    }
}
