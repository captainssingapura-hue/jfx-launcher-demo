package com.example.fxsuite.avatar.bench;

import com.example.fxsuite.avatar.sim.Contour;
import com.example.fxsuite.avatar.sim.Survey;
import com.example.fxsuite.avatar.sim.Terrain;
import com.example.fxsuite.avatar.sim.Vec2;

import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * A terrain's map, drawn flat, north (z) up: its land shaded in relief and tinted by height, from its
 * {@link Survey}; contour lines drawn back out of that, every so far up, each fifth bold; and over it
 * the map itself, its loops, each its height, the flat ones dashed and lightly filled, and its spot
 * heights. A scale, north, and where the pointer is: how high and how steep the land is there.
 *
 * <p>Worked on with the mouse: a loop's point or a spot height dragged moves it, to the metre; a
 * double click on a loop's line puts a point in it; a right click on a point takes it out (a loop
 * keeps three); dragging anywhere else moves the map, and the wheel zooms it about the pointer. Each
 * change is handed on as the terrain changed ({@link #onEdit}), to be surveyed again.
 */
final class TerrainMap extends Pane {

    /** How near the pointer, in pixels, a point or line is taken to be under it. */
    private static final double NEAR = 7;
    /** What a moved point snaps to, in centimetres: the metre. */
    private static final double SNAP = 100;

    private final Canvas canvas = new Canvas();
    private final Label where = new Label();
    private Terrain terrain;
    private Survey survey;
    private Image relief;
    private List<List<Vec2>> thin = List.of();
    private List<List<Vec2>> bold = List.of();
    /** Pixels to the centimetre; none yet, until it is fitted. */
    private double scale = -1;
    private double midX;
    private double midZ;
    private int selected = -1;
    private Grab grab;
    private boolean moved;
    private Consumer<Terrain> edited = t -> { };
    private IntConsumer picked = i -> { };

    /** What the mouse took hold of: a loop's point, a spot height, or (neither) the map itself. */
    private record Grab(int loop, int point, int spot, double x, double y, double midX, double midZ) {
    }

    TerrainMap() {
        getChildren().addAll(canvas, where);
        where.setFont(Font.font("Monospaced", 12));
        where.setPadding(new Insets(4, 8, 4, 8));
        where.setStyle("-fx-background-color: rgba(255,255,255,0.8);");
        where.setVisible(false);
        setOnMouseExited(e -> where.setVisible(false));
        setMinSize(300, 300);
        setOnMousePressed(this::pressed);
        setOnMouseDragged(this::dragged);
        setOnMouseReleased(this::released);
        setOnMouseClicked(this::clicked);
        setOnMouseMoved(e -> point(e.getX(), e.getY()));
        setOnScroll(this::zoomed);
    }

    /** Runs {@code then} with the terrain each time it is changed on the map. */
    void onEdit(final Consumer<Terrain> then) {
        edited = then;
    }

    /** Runs {@code then} with the loop picked on the map. */
    void onPick(final IntConsumer then) {
        picked = then;
    }

    @Override
    protected void layoutChildren() {
        canvas.setWidth(getWidth());
        canvas.setHeight(getHeight());
        where.autosize();
        where.relocate(8, getHeight() - where.getHeight() - 8);
        draw();
    }

    /** Shows {@code shown}'s map, over whatever survey there is so far; fitted to the view if it is new. */
    void show(final Terrain shown) {
        if (terrain == null || !terrain.sameGround(shown)) {
            scale = -1;
        }
        terrain = shown;
        if (selected >= shown.contours().size()) {
            selected = -1;
        }
        draw();
    }

    /** Shows the land surveyed from the map: its relief, and its contour lines, thin and bold. */
    void surveyed(final Survey land, final Image shaded, final List<List<Vec2>> lines, final List<List<Vec2>> boldLines) {
        survey = land;
        relief = shaded;
        thin = lines;
        bold = boldLines;
        draw();
    }

    /** Fits the whole map in view again. */
    void fit() {
        scale = -1;
        draw();
    }

    // --- worked out apart, off the screen's thread -------------------------------------------------------

    /** The land shaded in relief, lit from the north-west, and tinted by height: low green to high pale stone. */
    static Image relief(final Survey land) {
        final int n = land.across();
        final int m = land.along();
        final double low = land.lowest();
        final double span = Math.max(1, land.highest() - low);
        final int[] pixels = new int[n * m];
        final double lx = -0.5;
        final double ly = 0.7071;
        final double lz = 0.5;
        for (int j = 0; j < m; j++) {
            for (int i = 0; i < n; i++) {
                final double dx = (land.at(Math.min(n - 1, i + 1), j) - land.at(Math.max(0, i - 1), j)) / (2 * land.every());
                final double dz = (land.at(i, Math.min(m - 1, j + 1)) - land.at(i, Math.max(0, j - 1))) / (2 * land.every());
                // the slopes made more of, as a relief map does, or gentle land shows nothing
                final double ex = -3 * dx;
                final double ez = -3 * dz;
                final double length = Math.sqrt(ex * ex + 1 + ez * ez);
                final double lit = Math.max(0, (ex * lx + ly + ez * lz) / length);
                final double shade = 0.45 + 0.75 * lit;
                final int[] tint = tint((land.at(i, j) - low) / span);
                final int r = (int) Math.min(255, tint[0] * shade);
                final int g = (int) Math.min(255, tint[1] * shade);
                final int b = (int) Math.min(255, tint[2] * shade);
                pixels[(m - 1 - j) * n + i] = 0xff000000 | r << 16 | g << 8 | b;
            }
        }
        final WritableImage image = new WritableImage(n, m);
        image.getPixelWriter().setPixels(0, 0, n, m, PixelFormat.getIntArgbInstance(), pixels, 0, n);
        return image;
    }

    private static final int[][] TINTS = {{120, 158, 92}, {168, 184, 112}, {200, 182, 128}, {170, 140, 104}, {226, 222, 212}};

    private static int[] tint(final double t) {
        final double at = Math.max(0, Math.min(1, t)) * (TINTS.length - 1);
        final int k = Math.min(TINTS.length - 2, (int) at);
        final double f = at - k;
        final int[] c = new int[3];
        for (int m = 0; m < 3; m++) {
            c[m] = (int) Math.round(TINTS[k][m] + (TINTS[k + 1][m] - TINTS[k][m]) * f);
        }
        return c;
    }

    /** Its contour lines every {@code apart}, those every fifth one apart left out (they are drawn bold). */
    static List<List<Vec2>> lines(final Survey land, final double apart, final boolean fifths) {
        final List<List<Vec2>> lines = new ArrayList<>();
        for (long k = (long) Math.ceil(land.lowest() / apart); k * apart <= land.highest(); k++) {
            if ((k % 5 == 0) == fifths) {
                lines.addAll(land.lines(k * apart));
            }
        }
        return lines;
    }

    // --- drawn --------------------------------------------------------------------------------------------

    private double sx(final double x) {
        return getWidth() / 2 + (x - midX) * scale;
    }

    private double sy(final double z) {
        return getHeight() / 2 - (z - midZ) * scale;
    }

    private double wx(final double px) {
        return midX + (px - getWidth() / 2) / scale;
    }

    private double wz(final double py) {
        return midZ - (py - getHeight() / 2) / scale;
    }

    private void draw() {
        final GraphicsContext g = canvas.getGraphicsContext2D();
        final double w = getWidth();
        final double h = getHeight();
        g.setFill(Color.web("#eef0ec"));
        g.fillRect(0, 0, w, h);
        if (terrain == null || w < 10 || h < 10) {
            return;
        }
        if (scale < 0) {
            scale = Math.min((w - 40) / (terrain.x1() - terrain.x0()), (h - 40) / (terrain.z1() - terrain.z0()));
            midX = (terrain.x0() + terrain.x1()) / 2;
            midZ = (terrain.z0() + terrain.z1()) / 2;
        }
        // the land
        if (relief != null && survey != null && survey.terrain().sameGround(terrain)) {
            g.setImageSmoothing(true);
            final double half = survey.every() / 2;
            g.drawImage(relief, sx(terrain.x0() - half), sy(terrain.z1() + half),
                    (terrain.x1() - terrain.x0() + 2 * half) * scale, (terrain.z1() - terrain.z0() + 2 * half) * scale);
        }
        g.setStroke(Color.web("#5a4630", 0.35));
        g.setLineWidth(0.8);
        lines(g, thin);
        g.setStroke(Color.web("#5a4630", 0.7));
        g.setLineWidth(1.5);
        lines(g, bold);
        // the world's edge
        g.setStroke(Color.web("#3b3f45"));
        g.setLineWidth(1);
        g.strokeRect(sx(terrain.x0()), sy(terrain.z1()), (terrain.x1() - terrain.x0()) * scale, (terrain.z1() - terrain.z0()) * scale);
        // the map: its loops
        final List<Contour> loops = terrain.contours();
        g.setFont(Font.font("System", 12));
        for (int k = 0; k < loops.size(); k++) {
            final Contour loop = loops.get(k);
            final boolean picked = k == selected;
            g.beginPath();
            path(g, loop.loop());
            if (loop.flat()) {
                g.setFill(Color.web("#f2e7c9", 0.55));
                g.fill();
                g.setLineDashes(8, 5);
            }
            g.setStroke(picked ? Color.web("#c2410c") : Color.web("#1f3b5c"));
            g.setLineWidth(picked ? 3 : 2);
            g.stroke();
            g.setLineDashes();
            final double size = picked ? 7 : 5;
            g.setFill(picked ? Color.web("#c2410c") : Color.web("#1f3b5c"));
            Vec2 top = loop.loop().getFirst();
            for (final Vec2 p : loop.loop()) {
                g.fillRect(sx(p.x()) - size / 2, sy(p.z()) - size / 2, size, size);
                if (p.z() > top.z()) {
                    top = p;
                }
            }
            label(g, metres(loop.height()) + (loop.flat() ? " flat" : ""), sx(top.x()), sy(top.z()) - 8, picked);
        }
        // its spot heights
        for (final Terrain.Spot spot : terrain.spots()) {
            final double x = sx(spot.at().x());
            final double y = sy(spot.at().z());
            g.setFill(Color.web("#7a1f1f"));
            g.fillPolygon(new double[] {x, x - 6, x + 6}, new double[] {y - 7, y + 5, y + 5}, 3);
            label(g, metres(spot.height()), x + 9, y + 4, false);
        }
        // a scale, and north
        final double[] steps = {1_000, 2_000, 5_000, 10_000, 20_000, 50_000, 100_000, 200_000, 500_000};
        double bar = steps[steps.length - 1];
        for (final double step : steps) {
            if (step * scale >= 80) {
                bar = step;
                break;
            }
        }
        g.setStroke(Color.web("#22262b"));
        g.setFill(Color.web("#22262b"));
        g.setLineWidth(2);
        g.strokeLine(w - 30 - bar * scale, h - 20, w - 30, h - 20);
        g.fillText(metres(bar), w - 30 - bar * scale, h - 26);
        g.fillText("N", w - 26, 22);
        g.strokeLine(w - 22, 28, w - 22, 48);
        g.strokeLine(w - 22, 28, w - 26, 35);
        g.strokeLine(w - 22, 28, w - 18, 35);
    }

    private void lines(final GraphicsContext g, final List<List<Vec2>> lines) {
        for (final List<Vec2> line : lines) {
            g.beginPath();
            for (int k = 0; k < line.size(); k++) {
                if (k == 0) {
                    g.moveTo(sx(line.get(k).x()), sy(line.get(k).z()));
                } else {
                    g.lineTo(sx(line.get(k).x()), sy(line.get(k).z()));
                }
            }
            g.stroke();
        }
    }

    private void path(final GraphicsContext g, final List<Vec2> loop) {
        for (int k = 0; k < loop.size(); k++) {
            if (k == 0) {
                g.moveTo(sx(loop.get(k).x()), sy(loop.get(k).z()));
            } else {
                g.lineTo(sx(loop.get(k).x()), sy(loop.get(k).z()));
            }
        }
        g.closePath();
    }

    private static void label(final GraphicsContext g, final String text, final double x, final double y, final boolean picked) {
        g.setFill(Color.web("#ffffff", 0.75));
        g.fillRect(x - 2, y - 11, text.length() * 7 + 4, 15);
        g.setFill(picked ? Color.web("#c2410c") : Color.web("#1f3b5c"));
        g.fillText(text, x, y);
    }

    private static String metres(final double centimetres) {
        final double m = centimetres / 100;
        return (m == Math.rint(m) ? String.format(Locale.ROOT, "%.0f", m) : String.format(Locale.ROOT, "%.2f", m)
                .replaceAll("0$", "")) + " m";
    }

    /** Where the pointer is, how high the land is there, and how steep. */
    private void point(final double px, final double py) {
        if (terrain == null || scale < 0) {
            return;
        }
        final double x = wx(px);
        final double z = wz(py);
        final StringBuilder text = new StringBuilder(String.format(Locale.ROOT, "%.0f, %.0f m", x / 100, z / 100));
        if (survey != null && x >= terrain.x0() && x <= terrain.x1() && z >= terrain.z0() && z <= terrain.z1()) {
            text.append(String.format(Locale.ROOT, "   %.2f m high, %.1f° steep", survey.height(x, z) / 100,
                    Math.toDegrees(Math.atan(survey.slope(x, z)))));
        }
        where.setText(text.toString());
        where.setVisible(true);
        where.autosize();
        where.relocate(8, getHeight() - where.getHeight() - 8);
    }

    // --- worked on ----------------------------------------------------------------------------------------

    private void pressed(final MouseEvent e) {
        if (terrain == null) {
            return;
        }
        moved = false;
        final int[] point = pointAt(e.getX(), e.getY());
        if (e.getButton() == MouseButton.SECONDARY) {
            if (point != null) {
                final List<Vec2> loop = new ArrayList<>(terrain.contours().get(point[0]).loop());
                if (loop.size() > 3) {
                    loop.remove(point[1]);
                    replace(point[0], loop);
                    edited.accept(terrain);
                }
            }
            return;
        }
        if (e.getButton() != MouseButton.PRIMARY) {
            return;
        }
        if (point != null) {
            pick(point[0]);
            grab = new Grab(point[0], point[1], -1, e.getX(), e.getY(), midX, midZ);
            return;
        }
        final int spot = spotAt(e.getX(), e.getY());
        grab = new Grab(-1, -1, spot, e.getX(), e.getY(), midX, midZ);
    }

    private void dragged(final MouseEvent e) {
        if (grab == null) {
            return;
        }
        final Vec2 to = new Vec2(snap(wx(e.getX())), snap(wz(e.getY())));
        if (grab.loop() >= 0) {
            final List<Vec2> loop = new ArrayList<>(terrain.contours().get(grab.loop()).loop());
            if (!loop.get(grab.point()).equals(to)) {
                loop.set(grab.point(), to);
                moved |= replace(grab.loop(), loop);
            }
        } else if (grab.spot() >= 0) {
            final List<Terrain.Spot> spots = new ArrayList<>(terrain.spots());
            spots.set(grab.spot(), new Terrain.Spot(to, spots.get(grab.spot()).height()));
            terrain = terrain.with(terrain.contours(), spots);
            moved = true;
        } else {
            midX = grab.midX() - (e.getX() - grab.x()) / scale;
            midZ = grab.midZ() + (e.getY() - grab.y()) / scale;
        }
        draw();
        point(e.getX(), e.getY());
    }

    private void released(final MouseEvent e) {
        if (grab != null && moved && (grab.loop() >= 0 || grab.spot() >= 0)) {
            edited.accept(terrain);
        }
        grab = null;
    }

    private void clicked(final MouseEvent e) {
        if (terrain == null || e.getButton() != MouseButton.PRIMARY || e.getClickCount() != 2) {
            return;
        }
        // a point put in the line nearest the pointer
        double best = NEAR;
        int bestLoop = -1;
        int bestSide = -1;
        for (int k = 0; k < terrain.contours().size(); k++) {
            final List<Vec2> loop = terrain.contours().get(k).loop();
            for (int s = 0; s < loop.size(); s++) {
                final Vec2 a = loop.get(s);
                final Vec2 b = loop.get((s + 1) % loop.size());
                final double d = toSegment(e.getX(), e.getY(), sx(a.x()), sy(a.z()), sx(b.x()), sy(b.z()));
                if (d < best) {
                    best = d;
                    bestLoop = k;
                    bestSide = s;
                }
            }
        }
        if (bestLoop >= 0) {
            final List<Vec2> loop = new ArrayList<>(terrain.contours().get(bestLoop).loop());
            loop.add(bestSide + 1, new Vec2(snap(wx(e.getX())), snap(wz(e.getY()))));
            pick(bestLoop);
            if (replace(bestLoop, loop)) {
                edited.accept(terrain);
            }
        }
    }

    private void zoomed(final ScrollEvent e) {
        if (terrain == null || scale < 0) {
            return;
        }
        final double x = wx(e.getX());
        final double z = wz(e.getY());
        scale *= Math.pow(1.15, e.getDeltaY() / 40);
        midX = x - (e.getX() - getWidth() / 2) / scale;
        midZ = z + (e.getY() - getHeight() / 2) / scale;
        draw();
    }

    private void pick(final int loop) {
        selected = loop;
        picked.accept(loop);
        draw();
    }

    /** Loop {@code k} as {@code loop} instead; whether it could be (a loop goes round something). */
    private boolean replace(final int k, final List<Vec2> loop) {
        final List<Contour> loops = new ArrayList<>(terrain.contours());
        try {
            loops.set(k, new Contour(loops.get(k).height(), loop, loops.get(k).flat()));
        } catch (final IllegalArgumentException ignored) {
            return false;
        }
        terrain = terrain.with(loops, terrain.spots());
        return true;
    }

    /** The loop and point nearest the pointer, the picked loop's first; null if none is near. */
    private int[] pointAt(final double px, final double py) {
        int[] found = null;
        double best = NEAR;
        for (int k = 0; k < terrain.contours().size(); k++) {
            final List<Vec2> loop = terrain.contours().get(k).loop();
            for (int p = 0; p < loop.size(); p++) {
                final double d = Math.hypot(sx(loop.get(p).x()) - px, sy(loop.get(p).z()) - py) - (k == selected ? 2 : 0);
                if (d < best) {
                    best = d;
                    found = new int[] {k, p};
                }
            }
        }
        return found;
    }

    private int spotAt(final double px, final double py) {
        for (int s = 0; s < terrain.spots().size(); s++) {
            final Vec2 at = terrain.spots().get(s).at();
            if (Math.hypot(sx(at.x()) - px, sy(at.z()) - py) < NEAR + 2) {
                return s;
            }
        }
        return -1;
    }

    private static double snap(final double v) {
        return Math.round(v / SNAP) * SNAP;
    }

    private static double toSegment(final double px, final double py, final double ax, final double ay, final double bx,
                                    final double by) {
        final double ex = bx - ax;
        final double ey = by - ay;
        final double length = ex * ex + ey * ey;
        final double t = length == 0 ? 0 : Math.max(0, Math.min(1, ((px - ax) * ex + (py - ay) * ey) / length));
        return Math.hypot(px - ax - t * ex, py - ay - t * ey);
    }
}
