package com.example.fxsuite.avatar;

import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * A land vehicle's instruments, low on the screen as racing games have them: a round tachometer,
 * its needle sweeping up the dial as the engine revs, the last of it red; the gear it is in, big,
 * in the middle of the dial (R going back); and how fast it is going, in kilometres an hour, read
 * out under it. Seen only while a land vehicle is driven, and on the engine bench.
 */
public final class Dashboard {

    /** How big it is, and how far round the dial goes: from low on the left, over the top, to low on the right. */
    private static final double SIZE = 250;
    private static final double SWEEP = 270;
    private static final double START = 225;
    /** The dial reads up to this, in thousands of turns a minute; or a thousand past the red line, if higher. */
    private static final int THOUSANDS = 8;
    /** How briskly the needle follows the engine. */
    private static final double FOLLOW = 18;

    private final Canvas canvas = new Canvas(SIZE, SIZE);
    private double needle = -1;

    public Dashboard() {
        canvas.setMouseTransparent(true);
        canvas.setVisible(false);
    }

    public Node node() {
        return canvas;
    }

    /** Not seen: no land vehicle driven. */
    public void hide() {
        canvas.setVisible(false);
        needle = -1;
    }

    /**
     * Seen, {@code seconds} after the last frame: the engine turning {@code rpm} times a minute, its
     * red line at {@code redline}; in gear {@code gear} ("R", "1", ...); going {@code kmh}.
     */
    public void show(final double rpm, final double redline, final String gear, final double kmh, final double seconds) {
        canvas.setVisible(true);
        needle = needle < 0 ? rpm : needle + (rpm - needle) * (1 - Math.exp(-FOLLOW * seconds));
        final GraphicsContext g = canvas.getGraphicsContext2D();
        final double middle = SIZE / 2;
        final double radius = SIZE / 2 - 14;
        g.clearRect(0, 0, SIZE, SIZE);
        // the face
        g.setFill(Color.rgb(16, 20, 26, 0.72));
        g.fillOval(4, 4, SIZE - 8, SIZE - 8);
        g.setStroke(Color.rgb(200, 210, 220, 0.35));
        g.setLineWidth(2);
        g.strokeOval(4, 4, SIZE - 8, SIZE - 8);
        // the red zone, from the red line up
        final int thousands = Math.max(THOUSANDS, (int) Math.ceil(redline / 1000) + 1);
        final double top = thousands * 1000;
        g.setStroke(Color.web("#e0342b"));
        g.setLineWidth(9);
        g.setLineCap(StrokeLineCap.BUTT);
        final double red = angle(redline / top);
        g.strokeArc(middle - radius + 6, middle - radius + 6, 2 * (radius - 6), 2 * (radius - 6), START - SWEEP,
                red - (START - SWEEP), ArcType.OPEN);
        // the marks: one each five hundred, numbered each thousand
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);
        g.setFont(Font.font("System", FontWeight.BOLD, 15));
        for (int half = 0; half <= thousands * 2; half++) {
            final double a = Math.toRadians(angle(half / (thousands * 2.0)));
            final boolean major = half % 2 == 0;
            final double from = radius - (major ? 16 : 9);
            final boolean hot = half * 500 >= redline;
            g.setStroke(hot ? Color.web("#ff6b5e") : Color.rgb(235, 240, 245));
            g.setLineWidth(major ? 3 : 1.5);
            g.strokeLine(middle + Math.cos(a) * from, middle - Math.sin(a) * from,
                    middle + Math.cos(a) * radius, middle - Math.sin(a) * radius);
            if (major) {
                g.setFill(hot ? Color.web("#ff6b5e") : Color.rgb(235, 240, 245));
                g.fillText(Integer.toString(half / 2), middle + Math.cos(a) * (radius - 30),
                        middle - Math.sin(a) * (radius - 30));
            }
        }
        g.setFont(Font.font("System", 10));
        g.setFill(Color.rgb(200, 210, 220, 0.7));
        g.fillText("x1000 rpm", middle, middle - 46);
        // the gear, big, in the middle; past the red line, it glows red
        g.setFont(Font.font("System", FontWeight.BOLD, 54));
        g.setFill(needle >= redline ? Color.web("#ff6b5e") : Color.WHITE);
        g.fillText(gear, middle, middle - 4);
        // the speed, under it
        g.setFont(Font.font("Monospaced", FontWeight.BOLD, 26));
        g.setFill(Color.WHITE);
        g.fillText(String.format("%3.0f", kmh), middle, middle + 44);
        g.setFont(Font.font("System", 11));
        g.setFill(Color.rgb(200, 210, 220, 0.8));
        g.fillText("km/h", middle, middle + 66);
        // the needle, from just clear of the gear out to the marks, so the gear is always read
        final double a = Math.toRadians(angle(Math.min(1.02, needle / top)));
        g.setStroke(Color.web("#ff8a3d"));
        g.setLineWidth(4);
        g.setLineCap(StrokeLineCap.ROUND);
        g.strokeLine(middle + Math.cos(a) * 46, middle - Math.sin(a) * 46,
                middle + Math.cos(a) * (radius - 10), middle - Math.sin(a) * (radius - 10));
    }

    /** Where on the dial {@code share} of the way round it is, in degrees anticlockwise from the right. */
    private static double angle(final double share) {
        return START - SWEEP * share;
    }
}
