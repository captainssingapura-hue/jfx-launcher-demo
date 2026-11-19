package com.example.fxsuite.avatar.bench;

import com.example.fxsuite.avatar.bench.kit.PlayWorld;
import com.example.fxsuite.avatar.bench.kit.Spot;
import com.example.fxsuite.avatar.build.Point;
import com.example.fxsuite.avatar.build.Sites;
import com.example.fxsuite.avatar.build.Solid;
import com.example.fxsuite.avatar.build.Town;
import com.example.fxsuite.avatar.look.Scenery;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Land;
import com.example.fxsuite.avatar.sim.Outline;
import com.example.fxsuite.avatar.sim.Place;
import com.example.fxsuite.avatar.sim.Prism;
import com.example.fxsuite.avatar.sim.Terrain;
import com.example.fxsuite.avatar.sim.Vec2;
import com.example.fxsuite.avatar.sim.Volume;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;

import java.util.ArrayList;
import java.util.List;

/**
 * A building of the buildings bench as a world to play ({@link PlayWorld}): the building as it is dressed on the bench,
 * stood in the middle of level ground ruled every 10 m, a street along its front; to walk round, and into, with whoever
 * the bench brings.
 *
 * <ul>
 *   <li><b>the building</b>: drawn as it is dressed; in the way as it is built, each of its solids what it covers seen
 *       from above, stood straight up from its lowest to its highest ({@link Prism}): its walls, floors, glass and
 *       roof in the way, a doorway a way in, its floors stood on. A sloped solid, a roof, as solid as its highest;</li>
 *   <li><b>the street</b>: {@link #STREET} wide, {@link #GAP} in front of it, running past it on either side;</li>
 *   <li><b>its spots</b>: the avatar on the street before its middle, facing it; vehicles parked along the street's
 *       far side, nose to tail, facing along it; people along its front, facing the street.</li>
 * </ul>
 *
 * <p>Its front is its side furthest along (the way the bench's person stands before it).
 */
final class BuildingSite implements PlayWorld {

    /** The key the building is drawn by, as buildings are. */
    static final int KEY = 950_000;
    /** How far in front of its front the street's near edge is, how wide the street is, how thick its paving. */
    static final double GAP = 600;
    static final double STREET = 800;
    static final double PAVING = 2;
    /** How far the street and the ground run past it either side. */
    static final double PAST = 6_000;
    /** How far apart people stand along its front, and how far before it. */
    static final double PEOPLE_APART = 150;
    static final double BEFORE = 200;
    /** The gap between parked vehicles. */
    static final double ROOM = 200;

    private final String name;
    /** Its solids, about the middle of its footprint, its ground at nothing. */
    private final List<Solid> solids;
    /** How far it reaches across and along from its middle, at most. */
    private final double halfAcross;
    private final double halfAlong;

    /** {@code dressed}, the building called {@code name}, stood in the middle of the site. */
    BuildingSite(final String name, final List<Solid> dressed) {
        this.name = name;
        double minA = Double.POSITIVE_INFINITY;
        double maxA = Double.NEGATIVE_INFINITY;
        double minL = Double.POSITIVE_INFINITY;
        double maxL = Double.NEGATIVE_INFINITY;
        for (final Solid s : dressed) {
            for (final Point p : s.corners()) {
                minA = Math.min(minA, p.across());
                maxA = Math.max(maxA, p.across());
                minL = Math.min(minL, p.along());
                maxL = Math.max(maxL, p.along());
            }
        }
        final double midA = (minA + maxA) / 2;
        final double midL = (minL + maxL) / 2;
        this.solids = dressed.stream().map(s -> new Solid(s.name(), s.role(), s.material(), s.corners().stream()
                .map(p -> new Point(p.across() - midA, p.along() - midL, p.up())).toList())).toList();
        this.halfAcross = (maxA - minA) / 2;
        this.halfAlong = (maxL - minL) / 2;
    }

    @Override
    public String title() {
        return "Building · " + name;
    }

    @Override
    public String description() {
        return "The buildings bench's " + name + ", on level ground, a street along its front.";
    }

    /** What is in its way: each of its solids, as it covers the ground, from its lowest to its highest. */
    static List<Volume> shape(final List<Solid> solids) {
        final List<Volume> shape = new ArrayList<>();
        for (final Solid s : solids) {
            double low = Double.POSITIVE_INFINITY;
            double high = Double.NEGATIVE_INFINITY;
            final List<Vec2> seen = new ArrayList<>();
            for (final Point p : s.corners()) {
                seen.add(new Vec2(p.across(), p.along()));
                low = Math.min(low, p.up());
                high = Math.max(high, p.up());
            }
            final List<Vec2> outline = Outline.hull(seen);
            if (outline.size() >= 3 && Math.abs(Outline.twiceArea(outline)) > 1 && high > low) {
                shape.add(new Prism(outline, low, high));
            }
        }
        return shape;
    }

    @Override
    public void lay(final Course.Layout world) {
        Sites.register(KEY, KEY + 1, k -> solids);
        final double reach = Math.ceil((Math.max(halfAcross, halfAlong + GAP + STREET) + PAST) / 1_000) * 1_000;
        final Terrain flat = new Terrain("building site", -reach, -reach, reach, reach, 1_000, 0, 0, Terrain.Noise.NONE,
                List.of(), List.of());
        world.land(new Land(flat.survey(), List.of())).ruled(true)
                .pieces(List.of(Town.building(solids, new Place(0, 0, 0, 0), KEY, shape(solids), 1)))
                .ways(List.of(street()));
    }

    /** The street's paving, stood on: along its front, past it either side. */
    private Prism street() {
        final double near = halfAlong + GAP;
        final double far = near + STREET;
        final double end = halfAcross + PAST;
        return new Prism(List.of(new Vec2(-end, near), new Vec2(end, near), new Vec2(end, far), new Vec2(-end, far)), 0,
                PAVING);
    }

    @Override
    public Scenery scenery() {
        final double length = 2 * (halfAcross + PAST);
        final Box paving = new Box(length, PAVING, STREET);
        paving.setTranslateY(-PAVING / 2);
        paving.setTranslateZ(halfAlong + GAP + STREET / 2);
        final PhongMaterial asphalt = new PhongMaterial(Color.web("#3a3d42"));
        asphalt.setSpecularColor(Color.web("#202226"));
        paving.setMaterial(asphalt);
        return Scenery.still(new Group(paving));
    }

    /** On the street's near side, before its middle, facing it. */
    @Override
    public Spot start() {
        return new Spot("before it", new Place(0, halfAlong + GAP + STREET / 4, PAVING, 180));
    }

    /** Along the street's far side, nose to tail with room between, about its middle, facing along it. */
    @Override
    public List<Spot> parked(final List<Double> lengths) {
        double total = 0;
        for (final double l : lengths) {
            total += l + ROOM;
        }
        double at = -total / 2;
        final List<Spot> spots = new ArrayList<>();
        for (int k = 0; k < lengths.size(); k++) {
            at += lengths.get(k) / 2;
            spots.add(new Spot("parked " + (k + 1), new Place(at, halfAlong + GAP + STREET * 3 / 4, PAVING, 90)));
            at += lengths.get(k) / 2 + ROOM;
        }
        return spots;
    }

    /** Along its front, a little before it, facing the street. */
    @Override
    public List<Spot> standing(final int count) {
        final List<Spot> spots = new ArrayList<>();
        for (int k = 0; k < count; k++) {
            final double across = (k - (count - 1) / 2.0) * PEOPLE_APART;
            spots.add(new Spot("standing " + (k + 1), new Place(across, halfAlong + BEFORE, 0, 0)));
        }
        return spots;
    }
}
