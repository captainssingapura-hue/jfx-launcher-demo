package com.example.fxsuite.avatar.bench;

import com.example.fxsuite.avatar.build.Plan;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.FillRule;
import javafx.scene.text.Font;

import java.util.List;

/**
 * A floor plan, drawn flat ({@link Plan.Layout}), north up, scaled to fit: its floor, inside its
 * outline as shaped (its courtyards left out); its outline square, faint and dashed, where its
 * corners were before they were shaped; its outline as shaped, dark, the points on its curves
 * marked; the beams between its columns, and its columns (round, along its curves, tied in to its
 * grid by dashed beams); a 10 m scale and its size.
 */
final class PlanView extends Pane {

    private final Canvas canvas = new Canvas();
    private Plan.Layout layout;

    PlanView() {
        getChildren().add(canvas);
        widthProperty().addListener((o, was, now) -> draw());
        heightProperty().addListener((o, was, now) -> draw());
        setMinSize(200, 200);
    }

    @Override
    protected void layoutChildren() {
        canvas.setWidth(getWidth());
        canvas.setHeight(getHeight());
        draw();
    }

    /** Shows {@code shown}; nothing, if null. */
    void show(final Plan.Layout shown) {
        layout = shown;
        draw();
    }

    private void draw() {
        final GraphicsContext g = canvas.getGraphicsContext2D();
        final double w = canvas.getWidth();
        final double h = canvas.getHeight();
        g.setFill(Color.web("#f7f8f6"));
        g.fillRect(0, 0, w, h);
        if (layout == null || layout.shaped().isEmpty() || w < 10 || h < 10) {
            return;
        }
        final double[] box = layout.bounds();
        final double margin = 50;
        final double scale = Math.min((w - 2 * margin) / Math.max(1, box[2] - box[0]),
                (h - 2 * margin) / Math.max(1, box[3] - box[1]));
        final double cx = w / 2 - scale * (box[0] + box[2]) / 2;
        final double cy = h / 2 + scale * (box[1] + box[3]) / 2;
        final Transform to = (x, y) -> new double[] {cx + x * scale, cy - y * scale};

        // its floor: inside its outlines, its courtyards left out
        g.setFill(Color.web("#e4e8ec"));
        g.setFillRule(FillRule.EVEN_ODD);
        g.beginPath();
        for (final List<Plan.Pt> loop : layout.shaped()) {
            path(g, loop, to);
        }
        g.fill();
        // its outline square, before its corners were shaped
        g.setStroke(Color.web("#a8b0b8"));
        g.setLineWidth(1);
        g.setLineDashes(6, 4);
        for (final List<Plan.Pt> loop : layout.square()) {
            g.beginPath();
            path(g, loop, to);
            g.stroke();
        }
        g.setLineDashes();
        // its beams, and its columns
        g.setStroke(Color.web("#8a96a3"));
        g.setLineWidth(1);
        for (final Plan.Pt[] beam : layout.beams()) {
            final double[] a = to.at(beam[0].x(), beam[0].y());
            final double[] b = to.at(beam[1].x(), beam[1].y());
            g.strokeLine(a[0], a[1], b[0], b[1]);
        }
        // the beams tying its columns along its curves in to its grid, dashed
        g.setLineDashes(4, 3);
        for (final Plan.Pt[] tie : layout.ties()) {
            final double[] a = to.at(tie[0].x(), tie[0].y());
            final double[] b = to.at(tie[1].x(), tie[1].y());
            g.strokeLine(a[0], a[1], b[0], b[1]);
        }
        g.setLineDashes();
        g.setFill(Color.web("#2f6fb3"));
        for (final Plan.Pt column : layout.columns().values()) {
            final double[] at = to.at(column.x(), column.y());
            g.fillRect(at[0] - 3.5, at[1] - 3.5, 7, 7);
        }
        g.setFill(Color.web("#5b8fd0"));
        for (final Plan.Pt column : layout.perimeterColumns().values()) {
            final double[] at = to.at(column.x(), column.y());
            g.fillOval(at[0] - 4, at[1] - 4, 8, 8);
        }
        // its outline as shaped, and the points on its curves
        g.setStroke(Color.web("#22262b"));
        g.setLineWidth(2.5);
        for (final List<Plan.Pt> loop : layout.shaped()) {
            g.beginPath();
            path(g, loop, to);
            g.stroke();
        }
        g.setFill(Color.web("#c26a1b"));
        final List<List<Plan.Pt>> loops = layout.shaped();
        final List<List<Boolean>> curved = layout.curved();
        for (int l = 0; l < loops.size(); l++) {
            for (int k = 0; k < loops.get(l).size(); k++) {
                if (curved.get(l).get(k)) {
                    final double[] at = to.at(loops.get(l).get(k).x(), loops.get(l).get(k).y());
                    g.fillOval(at[0] - 2.5, at[1] - 2.5, 5, 5);
                }
            }
        }
        // its size, and a scale of 10 m, and north
        g.setFill(Color.web("#3b3f45"));
        g.setFont(Font.font("System", 13));
        g.fillText(String.format(java.util.Locale.ROOT, "%.1f m by %.1f m", (box[2] - box[0]) / 100,
                (box[3] - box[1]) / 100), 14, 22);
        g.setStroke(Color.web("#3b3f45"));
        g.setLineWidth(2);
        g.strokeLine(14, h - 18, 14 + 1000 * scale, h - 18);
        g.fillText("10 m", 18 + 1000 * scale, h - 13);
        g.fillText("N", w - 26, 26);
        g.strokeLine(w - 22, 32, w - 22, 52);
        g.strokeLine(w - 22, 32, w - 26, 39);
        g.strokeLine(w - 22, 32, w - 18, 39);
    }

    @FunctionalInterface
    private interface Transform {
        double[] at(double x, double y);
    }

    private static void path(final GraphicsContext g, final List<Plan.Pt> loop, final Transform to) {
        for (int k = 0; k < loop.size(); k++) {
            final double[] at = to.at(loop.get(k).x(), loop.get(k).y());
            if (k == 0) {
                g.moveTo(at[0], at[1]);
            } else {
                g.lineTo(at[0], at[1]);
            }
        }
        g.closePath();
    }
}
