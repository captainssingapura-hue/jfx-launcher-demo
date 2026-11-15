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
 * grid by dashed beams); a 10 m scale and its size. With a tower's frame ({@link Structure}), that
 * over it: its cores, filled and hatched; its columns, as big as they stand there; and a void deck's
 * enclosed part, outlined; the plan's own columns then left out, the frame's being what stands.
 */
final class PlanView extends Pane {

    private final Canvas canvas = new Canvas();
    private Plan.Layout layout;
    private Structure structure;

    /**
     * What holds a section up, flat: its cores' outlines, its columns' footprints, and any part of it
     * walled in (a void deck's enclosed wing).
     */
    record Structure(List<List<Plan.Pt>> cores, List<List<Plan.Pt>> columns, List<List<Plan.Pt>> enclosed) {
    }

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
        show(shown, null);
    }

    /** Shows {@code shown}, and {@code frame} over it, if any. */
    void show(final Plan.Layout shown, final Structure frame) {
        layout = shown;
        structure = frame;
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
        // its beams, and its columns; under a frame, its grid faint, for where the frame's columns stand off it
        g.setStroke(Color.web(structure == null ? "#8a96a3" : "#c4cbd2"));
        g.setLineWidth(1);
        for (final Plan.Pt[] beam : layout.beams()) {
            final double[] a = to.at(beam[0].x(), beam[0].y());
            final double[] b = to.at(beam[1].x(), beam[1].y());
            g.strokeLine(a[0], a[1], b[0], b[1]);
        }
        // the beams tying its columns along its curves in to its grid, dashed; not the frame's, so not under it
        g.setLineDashes(4, 3);
        for (final Plan.Pt[] tie : structure == null ? layout.ties() : List.<Plan.Pt[]>of()) {
            final double[] a = to.at(tie[0].x(), tie[0].y());
            final double[] b = to.at(tie[1].x(), tie[1].y());
            g.strokeLine(a[0], a[1], b[0], b[1]);
        }
        g.setLineDashes();
        if (structure == null) {
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
        } else {
            frame(g, to);
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

    /** The frame: an enclosed part, faint and outlined; its cores, filled and hatched; its columns. */
    private void frame(final GraphicsContext g, final Transform to) {
        for (final List<Plan.Pt> part : structure.enclosed()) {
            g.setFill(Color.web("#cfd6dd"));
            g.beginPath();
            path(g, part, to);
            g.fill();
            g.setStroke(Color.web("#6d7781"));
            g.setLineWidth(1.5);
            g.setLineDashes(8, 4);
            g.stroke();
            g.setLineDashes();
        }
        for (final List<Plan.Pt> core : structure.cores()) {
            g.setFill(Color.web("#9aa1a8"));
            g.beginPath();
            path(g, core, to);
            g.fill();
            // hatched, as a section cut through concrete is
            g.save();
            g.beginPath();
            path(g, core, to);
            g.clip();
            g.setStroke(Color.web("#6f777f"));
            g.setLineWidth(1);
            final double[] box = box(core, to);
            for (double d = box[0] - (box[3] - box[1]); d < box[2]; d += 8) {
                g.strokeLine(d, box[3], d + (box[3] - box[1]), box[1]);
            }
            g.restore();
            g.setStroke(Color.web("#3d4249"));
            g.setLineWidth(2);
            g.beginPath();
            path(g, core, to);
            g.stroke();
        }
        g.setFill(Color.web("#2f4a66"));
        for (final List<Plan.Pt> column : structure.columns()) {
            g.beginPath();
            path(g, column, to);
            g.fill();
            // at least a dot, however small it is drawn
            final double[] box = box(column, to);
            if (box[2] - box[0] < 4) {
                g.fillOval((box[0] + box[2]) / 2 - 2, (box[1] + box[3]) / 2 - 2, 4, 4);
            }
        }
    }

    /** The box round {@code loop} as drawn: left, top, right, bottom. */
    private static double[] box(final List<Plan.Pt> loop, final Transform to) {
        final double[] box = {Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
        for (final Plan.Pt p : loop) {
            final double[] at = to.at(p.x(), p.y());
            box[0] = Math.min(box[0], at[0]);
            box[1] = Math.min(box[1], at[1]);
            box[2] = Math.max(box[2], at[0]);
            box[3] = Math.max(box[3], at[1]);
        }
        return box;
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
