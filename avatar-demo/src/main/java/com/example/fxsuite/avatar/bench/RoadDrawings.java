package com.example.fxsuite.avatar.bench;

import com.example.fxsuite.avatar.look.RoadworkModel;
import com.example.fxsuite.avatar.roads.Carrier;
import com.example.fxsuite.avatar.roads.Course;
import com.example.fxsuite.avatar.roads.Cover;
import com.example.fxsuite.avatar.roads.Ground;
import com.example.fxsuite.avatar.roads.Prepared;
import com.example.fxsuite.avatar.roads.Profile;
import com.example.fxsuite.avatar.roads.Roadway;
import com.example.fxsuite.avatar.roads.Roadworks;
import com.example.fxsuite.avatar.roads.Section;
import com.example.fxsuite.avatar.sim.Vec2;

import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A road drawn flat, as a road is drawn on paper:
 *
 * <ul>
 *   <li><b>its long section</b>: along it, flattened, heights drawn larger. The ground under its line, its design
 *       line, its formation; each stretch coloured by what carries it; its structures, tunnels and clearances; the
 *       point it is looked at;</li>
 *   <li><b>its cross-section</b> at a point: across it, square to it. The ground as it was and as the road leaves
 *       it, every strip's every course, the structure under it, its markings; heights drawn larger as asked.</li>
 * </ul>
 */
final class RoadDrawings {

    static final Map<Carrier, Color> CARRIERS = Map.of(
            Carrier.GROUND, Color.web("#6f9a4e"), Carrier.STRUCTURE, Color.web("#7d8590"),
            Carrier.TUNNEL, Color.web("#3d3d3d"), Carrier.ON, Color.web("#c9a227"));
    private static final Color PAPER = Color.web("#fbfaf7");
    private static final Color GROUND = Color.web("#d9c9ab");
    private static final Color GROUND_LINE = Color.web("#7a5f40");
    private static final Color WATER = Color.web("#7fb0cc");
    private static final Color LINE = Color.web("#1d1f22");
    private static final Color FORMATION = Color.web("#c0392b");
    private static final Color OTHER = Color.web("#55595e");
    private static final Map<Cover, Color> COVERS = Map.of(
            Cover.GRASS, Color.web("#5f9a3a"), Cover.EARTH, Color.web("#8a6a45"), Cover.GRAVEL, Color.web("#9a958a"));
    private static final Color CONCRETE = Color.web("#9fa7b0");
    private static final Color CURSOR = Color.web("#e67e22");
    private static final Color INK = Color.web("#4a4f55");
    private static final Font SMALL = Font.font("System", 10);

    private RoadDrawings() {
    }

    // ---- the long section

    /** The long section, {@code w} by {@code h}, its point looked at {@code at} along it. */
    static void longSection(final GraphicsContext g, final double w, final double h, final Roadway road,
                            final List<Roadway.Clearance> clearances, final RoadScenes.Scene scene, final double at) {
        g.setFill(PAPER);
        g.fillRect(0, 0, w, h);
        final int n = road.size();
        final double left = 46;
        final double right = 12;
        final double top = 22;
        final double bottom = 24;
        double low = Double.POSITIVE_INFINITY;
        double high = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < n; i++) {
            low = Math.min(low, Math.min(road.ground(i), road.level(i) - Roadway.DECK));
            high = Math.max(high, Math.max(road.ground(i), road.level(i)));
            if (road.carrier(Math.min(i, n - 2)) == Carrier.TUNNEL) {
                high = Math.max(high, road.level(i) + Roadway.HEADROOM);
            }
        }
        if (scene.watered()) {
            low = Math.min(low, scene.water());
        }
        for (final Roadway.Clearance c : clearances) {
            high = Math.max(high, c.over());
        }
        final double pad = Math.max(200, (high - low) * 0.12);
        low -= pad;
        high += pad;
        final double length = road.length();
        final double sx = (w - left - right) / length;
        final double sy = (h - top - bottom) / (high - low);
        final Scale s = new Scale(left, top, sx, sy, low, h - bottom);
        // the ground under its line
        g.setFill(GROUND);
        g.beginPath();
        g.moveTo(s.x(0), s.y(low));
        for (int i = 0; i < n; i++) {
            g.lineTo(s.x(road.along(i)), s.y(road.ground(i)));
        }
        g.lineTo(s.x(length), s.y(low));
        g.closePath();
        g.fill();
        if (scene.watered()) {
            g.setFill(WATER);
            for (int i = 0; i + 1 < n; i++) {
                if (road.ground(i) < scene.water() || road.ground(i + 1) < scene.water()) {
                    final double x0 = s.x(road.along(i));
                    final double x1 = s.x(road.along(i + 1));
                    g.fillPolygon(new double[] {x0, x1, x1, x0}, new double[] {s.y(scene.water()), s.y(scene.water()),
                        s.y(Math.min(scene.water(), road.ground(i + 1))), s.y(Math.min(scene.water(), road.ground(i)))}, 4);
                }
            }
        }
        g.setStroke(GROUND_LINE);
        g.setLineWidth(1.2);
        g.beginPath();
        for (int i = 0; i < n; i++) {
            if (i == 0) {
                g.moveTo(s.x(road.along(i)), s.y(road.ground(i)));
            } else {
                g.lineTo(s.x(road.along(i)), s.y(road.ground(i)));
            }
        }
        g.stroke();
        // structures and tunnels
        for (final Roadway.Structure st : road.structures()) {
            final Roadway.Stretch str = st.stretch();
            if (str.carrier() == Carrier.STRUCTURE) {
                g.setFill(CONCRETE);
                for (int i = str.from(); i < str.to(); i++) {
                    final double x0 = s.x(road.along(i));
                    final double x1 = s.x(road.along(i + 1));
                    g.fillPolygon(new double[] {x0, x1, x1, x0}, new double[] {s.y(road.level(i)), s.y(road.level(i + 1)),
                        s.y(road.level(i + 1) - Roadway.DECK), s.y(road.level(i) - Roadway.DECK)}, 4);
                }
                g.setStroke(INK);
                g.setLineWidth(3);
                for (final int p : st.piers()) {
                    g.strokeLine(s.x(road.along(p)), s.y(road.level(p) - Roadway.DECK), s.x(road.along(p)),
                            s.y(road.ground(p)));
                }
            } else if (str.carrier() == Carrier.TUNNEL) {
                g.setFill(Color.web("#3d3d3d", 0.35));
                for (int i = str.from(); i < str.to(); i++) {
                    final double x0 = s.x(road.along(i));
                    final double x1 = s.x(road.along(i + 1));
                    g.fillPolygon(new double[] {x0, x1, x1, x0}, new double[] {
                        s.y(road.level(i) + Roadway.HEADROOM), s.y(road.level(i + 1) + Roadway.HEADROOM),
                        s.y(road.level(i + 1)), s.y(road.level(i))}, 4);
                }
            }
        }
        // clearances
        g.setStroke(FORMATION);
        g.setLineWidth(1);
        g.setLineDashes(6, 4);
        for (final Roadway.Clearance c : clearances) {
            g.strokeLine(s.x(c.from()), s.y(c.over()), s.x(c.to()), s.y(c.over()));
        }
        // the formation under the line
        g.setLineDashes(3, 3);
        g.beginPath();
        for (int i = 0; i < n; i++) {
            final int j = Math.min(i, n - 2);
            final double f = road.level(i) + road.section(j).formation(road.section(j).pavedSide());
            if (i == 0) {
                g.moveTo(s.x(road.along(i)), s.y(f));
            } else {
                g.lineTo(s.x(road.along(i)), s.y(f));
            }
        }
        g.stroke();
        g.setLineDashes();
        // the design line
        g.setStroke(LINE);
        g.setLineWidth(1.8);
        g.beginPath();
        for (int i = 0; i < n; i++) {
            if (i == 0) {
                g.moveTo(s.x(road.along(i)), s.y(road.level(i)));
            } else {
                g.lineTo(s.x(road.along(i)), s.y(road.level(i)));
            }
        }
        g.stroke();
        // what carries each stretch, along the top
        for (final Roadway.Stretch str : road.stretches()) {
            g.setFill(CARRIERS.get(str.carrier()));
            g.fillRect(s.x(road.along(str.from())), 4, Math.max(1, s.x(road.along(str.to())) - s.x(road.along(str.from()))), 8);
        }
        // the axes
        g.setFill(INK);
        g.setFont(SMALL);
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.TOP);
        g.setStroke(Color.web("#c8c6c0"));
        g.setLineWidth(0.6);
        for (double a = 0; a <= length + 1; a += 5_000) {
            g.strokeLine(s.x(a), h - bottom, s.x(a), h - bottom + 4);
            g.fillText(metres(a, 0), s.x(a), h - bottom + 5);
        }
        g.setTextAlign(TextAlignment.RIGHT);
        g.setTextBaseline(VPos.CENTER);
        final double every = nice((high - low) / 5);
        for (double y = Math.ceil(low / every) * every; y <= high; y += every) {
            g.strokeLine(left - 4, s.y(y), w - right, s.y(y));
            g.fillText(metres(y, 0), left - 6, s.y(y));
        }
        // the point looked at
        final int i = road.nearestPoint(at);
        g.setStroke(CURSOR);
        g.setLineWidth(1.5);
        g.strokeLine(s.x(at), top, s.x(at), h - bottom);
        g.setTextAlign(TextAlignment.LEFT);
        g.setTextBaseline(VPos.TOP);
        g.setFill(LINE);
        final double grade = i + 1 < n ? (road.level(i + 1) - road.level(i)) / (road.along(i + 1) - road.along(i)) : 0;
        g.fillText(String.format(Locale.ROOT, "%s m along: line %s m, ground %s m, grade %.1f%%  ·  heights ×%.0f",
                metres(at, 0), metres(road.level(i), 2), metres(road.ground(i), 2), grade * 100, sy / sx), left, 14);
    }

    // ---- the cross-section

    /**
     * The cross-section at the {@code i}th point of the {@code which}th of {@code works}' roads, {@code w} by
     * {@code h}, heights drawn {@code taller} times: the ground as they all leave it, its cover along its top; as it
     * was, dashed; any other road's pavement the section crosses, dark.
     */
    static void crossSection(final GraphicsContext g, final double w, final double h, final Roadworks works,
                             final int which, final RoadScenes.Scene scene, final int i, final double taller) {
        g.setFill(PAPER);
        g.fillRect(0, 0, w, h);
        final Roadway road = works.roads().get(which);
        final int j = Math.min(i, road.size() - 2);
        final Section section = road.section(j);
        final double level = road.level(i);
        final Profile was = works.raw();
        final Ground now = works.ground();
        // as far across as the roads change anything, and some
        double reach = Math.max(section.edge(-1), section.edge(1)) + 300;
        for (double a = -road.reach(); a <= road.reach(); a += 25) {
            final Vec2 p = road.at(i, a);
            if (Math.abs(now.at(p.x(), p.z()).height() - was.height(p.x(), p.z())) > 1) {
                reach = Math.max(reach, Math.abs(a) + 300);
            }
        }
        final double sx = (w - 20) / (2 * reach);
        final double sy = sx * taller;
        // heights about the road's
        final double mid = level - (h * 0.42) / sy;
        final Scale across = new Scale(w / 2, 0, sx, sy, mid, h * 0.92);
        final Scale s = across;
        // the ground: as the roads leave it, filled, its cover along its top; as it was, dashed
        final int samples = (int) (w / 2);
        final double[] xs = new double[samples + 2];
        final double[] ys = new double[samples + 2];
        final double[] wasY = new double[samples];
        final Cover[] covers = new Cover[samples];
        final Vec2[] at = new Vec2[samples];
        for (int k = 0; k < samples; k++) {
            final double a = -reach + 2 * reach * k / (samples - 1);
            final Vec2 p = road.at(i, a);
            final Prepared prepared = now.at(p.x(), p.z());
            at[k] = p;
            xs[k] = across.xAcross(a);
            ys[k] = s.y(prepared.height());
            covers[k] = prepared.cover();
            wasY[k] = s.y(was.height(p.x(), p.z()));
        }
        xs[samples] = xs[samples - 1];
        ys[samples] = h;
        xs[samples + 1] = xs[0];
        ys[samples + 1] = h;
        if (scene.watered()) {
            g.setFill(WATER);
            g.fillRect(0, s.y(scene.water()), w, h - s.y(scene.water()));
        }
        g.setFill(GROUND);
        g.fillPolygon(xs, ys, samples + 2);
        g.setLineWidth(3);
        for (int k = 0; k + 1 < samples; k++) {
            g.setStroke(covers[k] == null ? GROUND_LINE : COVERS.get(covers[k]));
            g.strokeLine(xs[k], ys[k], xs[k + 1], ys[k + 1]);
        }
        g.setStroke(GROUND_LINE);
        g.setLineDashes(4, 3);
        g.setLineWidth(1);
        g.strokePolyline(xs, wasY, samples);
        g.setLineDashes();
        // the other roads' pavements the section crosses
        g.setFill(OTHER);
        for (int r = 0; r < works.roads().size(); r++) {
            if (r == which) {
                continue;
            }
            final Roadway other = works.roads().get(r);
            for (int k = 0; k + 1 < samples; k++) {
                final double t0 = other.surface(at[k].x(), at[k].z());
                final double t1 = other.surface(at[k + 1].x(), at[k + 1].z());
                if (!Double.isNaN(t0) && !Double.isNaN(t1)) {
                    final double f0 = other.formation(at[k].x(), at[k].z());
                    final double f1 = other.formation(at[k + 1].x(), at[k + 1].z());
                    g.fillPolygon(new double[] {xs[k], xs[k + 1], xs[k + 1], xs[k]},
                            new double[] {s.y(t0), s.y(t1), s.y(f1), s.y(f0)}, 4);
                }
            }
        }
        // a structure or a tunnel under and round its pavement
        final Carrier carrier = road.carrier(j);
        if (carrier == Carrier.STRUCTURE) {
            g.setFill(CONCRETE);
            g.setStroke(Color.web("#00000040"));
            g.setLineWidth(0.5);
            // its deck under its lowest edge, tilted or not
            final double under = section.paved().stream().mapToDouble(l -> Math.min(l.innerTop(), l.outerTop()))
                    .min().orElse(0) - Roadway.DECK;
            for (final Section.Laid l : section.paved()) {
                final double d = l.strip().depth();
                quad(g, across, level, l.innerAcross(), l.outerAcross(), under, l.innerTop() - d,
                        under, l.outerTop() - d);
            }
            for (final int side : new int[] {-1, 1}) {
                final double edge = side * section.pavedEdge(side);
                final double t = section.top(edge) + Roadway.PARAPET;
                quad(g, across, level, edge, edge + side * Roadway.PARAPET_THICK, under, t, under, t);
            }
        } else if (carrier == Carrier.TUNNEL) {
            g.setFill(CONCRETE);
            final double l = -section.pavedEdge(-1) - 50;
            final double r = section.pavedEdge(1) + 50;
            final double low = section.formation(section.pavedSide()) - 100;
            quad(g, across, level, l - Roadway.LINING, l, low, Roadway.HEADROOM, low, Roadway.HEADROOM);
            quad(g, across, level, r, r + Roadway.LINING, low, Roadway.HEADROOM, low, Roadway.HEADROOM);
            quad(g, across, level, l - Roadway.LINING, r + Roadway.LINING, Roadway.HEADROOM,
                    Roadway.HEADROOM + Roadway.LINING, Roadway.HEADROOM, Roadway.HEADROOM + Roadway.LINING);
        }
        // every paved strip's every course
        g.setLineWidth(0.5);
        for (final Section.Laid l : section.paved()) {
            double down = 0;
            for (final Course c : l.strip().courses()) {
                g.setFill(RoadworkModel.colour(c.stuff()));
                g.setStroke(Color.web("#00000040"));
                quad(g, across, level, l.innerAcross(), l.outerAcross(), l.innerTop() - down - c.thick(),
                        l.innerTop() - down, l.outerTop() - down - c.thick(), l.outerTop() - down);
                down += c.thick();
            }
        }
        // the formation under the pavement
        g.setStroke(FORMATION);
        g.setLineWidth(1.2);
        g.setLineDashes(4, 3);
        for (final Section.Laid l : section.paved()) {
            final double d = l.strip().depth();
            g.strokeLine(across.xAcross(l.innerAcross()), s.y(level + l.innerTop() - d),
                    across.xAcross(l.outerAcross()), s.y(level + l.outerTop() - d));
        }
        g.setLineDashes();
        // the markings
        g.setFill(Color.WHITE);
        g.setStroke(LINE);
        g.setLineWidth(0.6);
        for (final Section.Marking m : section.markings()) {
            final double x0 = across.xAcross(m.across() - m.width() / 2);
            final double x1 = across.xAcross(m.across() + m.width() / 2);
            final double y = s.y(level + section.top(m.across()));
            g.fillRect(x0, y - 3, Math.max(2, x1 - x0), 3);
            g.strokeRect(x0, y - 3, Math.max(2, x1 - x0), 3);
        }
        // each strip named over it
        g.setFill(INK);
        g.setFont(SMALL);
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.BOTTOM);
        int row = 0;
        for (final Section.Laid l : section.laid()) {
            final double x = across.xAcross((l.innerAcross() + l.outerAcross()) / 2);
            final double y = s.y(level + Math.max(l.innerTop(), l.outerTop())) - 14 - 12 * (row++ % 2);
            g.fillText(l.strip().use().written() + " " + metres(l.strip().width(), 2), x, y);
        }
        // the line, and what it says
        g.setStroke(CURSOR);
        g.setLineWidth(1);
        g.strokeLine(across.xAcross(0), s.y(level) - 50, across.xAcross(0), s.y(level) + 12);
        g.setTextAlign(TextAlignment.LEFT);
        g.setTextBaseline(VPos.TOP);
        g.setFill(LINE);
        final double paved = section.pavedEdge(-1) + section.pavedEdge(1);
        final double deepest = section.paved().stream().mapToDouble(l -> l.strip().depth()).max().orElse(0);
        g.fillText(String.format(Locale.ROOT, "%s, %s m along: %s, %s; %s m wide, %s m paved, %s m deep  ·  heights ×%.0f",
                road.name(), metres(road.along(i), 0), section.name(), carrier.name().toLowerCase(Locale.ROOT),
                metres(section.width(), 2), metres(paved, 2), metres(deepest, 2), taller), 8, 6);
        // a metre, to measure by
        g.setStroke(LINE);
        g.setLineWidth(2);
        g.strokeLine(10, h - 10, 10 + 100 * sx, h - 10);
        g.setTextBaseline(VPos.BOTTOM);
        g.fillText("1 m", 14 + 100 * sx, h - 4);
    }

    private static void quad(final GraphicsContext g, final Scale s, final double level, final double a, final double b,
                             final double aLow, final double aHigh, final double bLow, final double bHigh) {
        final double[] xs = {s.xAcross(a), s.xAcross(b), s.xAcross(b), s.xAcross(a)};
        final double[] ys = {s.y(level + aHigh), s.y(level + bHigh), s.y(level + bLow), s.y(level + aLow)};
        g.fillPolygon(xs, ys, 4);
        g.strokePolygon(xs, ys, 4);
    }

    /** From cm to the canvas: {@code x0} where along (or across) is nothing, {@code base} where height is {@code low}. */
    private record Scale(double x0, double top, double sx, double sy, double low, double base) {

        double x(final double along) {
            return x0 + along * sx;
        }

        double xAcross(final double across) {
            return x0 + across * sx;
        }

        double y(final double height) {
            return base - (height - low) * sy;
        }
    }

    /** A round step near {@code rough}: 1, 2 or 5 of a power of ten. */
    private static double nice(final double rough) {
        final double p = Math.pow(10, Math.floor(Math.log10(Math.max(rough, 1e-9))));
        final double f = rough / p;
        return (f < 1.5 ? 1 : f < 3.5 ? 2 : f < 7.5 ? 5 : 10) * p;
    }

    static String metres(final double cm, final int places) {
        return String.format(Locale.ROOT, "%." + places + "f", cm / 100);
    }
}
