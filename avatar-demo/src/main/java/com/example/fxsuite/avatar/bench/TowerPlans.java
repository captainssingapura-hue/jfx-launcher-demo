package com.example.fxsuite.avatar.bench;

import com.example.fxsuite.avatar.build.Frame;
import com.example.fxsuite.avatar.build.Tower;
import com.example.fxsuite.avatar.build.TowerBuildings;

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
 * floor, its columns; its frame ({@link Frame}) over it, its cores and the columns standing in it,
 * as big as they are there; and what is wrong with the tower.
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
        // -Dbuilding.section: the start of the name of the section to show first
        final String asked = System.getProperty("building.section", "");
        sections.setValue(tower.sections().stream()
                .filter(s -> !asked.isEmpty() && s.name().toLowerCase(Locale.ROOT).startsWith(asked.toLowerCase(Locale.ROOT)))
                .findFirst().orElse(tower.sections().getFirst()));
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
        final Frame frame = tower.structure();
        final java.util.List<TowerBuildings.Standing> standing = TowerBuildings.standing(tower, index);
        view.show(layout, new PlanView.Structure(frame.cores(),
                standing.stream().map(TowerBuildings.Standing::footprint).toList(),
                section.enclosed() == null ? java.util.List.of() : section.enclosed().layout().shaped()));
        final StringBuilder out = new StringBuilder(String.format(Locale.ROOT,
                "%s: from %.1f m up, %d floors of %.1f m; %.0f m² a floor; %d columns (%d round its curves), %d beams",
                section.name(), tower.base(index) / 100, section.floors(), section.height() / 100, layout.area() / 10_000,
                layout.allColumns().size(), layout.perimeterColumns().size(), layout.beams().size() + layout.ties().size()));
        final double least = standing.stream().mapToDouble(TowerBuildings.Standing::size).min().orElse(0);
        final double most = standing.stream().mapToDouble(TowerBuildings.Standing::size).max().orElse(0);
        final long round = standing.stream().filter(c -> c.section() == Frame.Section.ROUND).count();
        out.append(String.format(Locale.ROOT, "%nits frame: %d core%s (%.0f m²); %d columns here, %s cm across%s%s",
                frame.cores().size(), frame.cores().size() == 1 ? "" : "s",
                frame.cores().stream().mapToDouble(TowerPlans::area).sum() / 10_000, standing.size(),
                Math.round(least) == Math.round(most) ? String.format(Locale.ROOT, "%.0f", most)
                        : String.format(Locale.ROOT, "%.0f to %.0f", least, most),
                round == 0 ? "" : round == standing.size() ? ", round" : ", " + round + " round",
                section.open() ? "; " + TowerBuildings.columns(tower, index).size() + " in the open" : ""));
        out.append(String.format(Locale.ROOT, "%n%s: %d floors, %.1f m to its roof", tower.name(), tower.floors(),
                tower.height() / 100));
        out.append(tower.problems().isEmpty() ? "; nothing wrong with it" : "");
        tower.problems().forEach(p -> out.append("\n! ").append(p));
        figures.setText(out.toString());
    }

    /** The area inside {@code loop}. */
    private static double area(final java.util.List<com.example.fxsuite.avatar.build.Plan.Pt> loop) {
        double twice = 0;
        for (int k = 0; k < loop.size(); k++) {
            final var a = loop.get(k);
            final var b = loop.get((k + 1) % loop.size());
            twice += a.x() * b.y() - b.x() * a.y();
        }
        return Math.abs(twice) / 2;
    }
}
