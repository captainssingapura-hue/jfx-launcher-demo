package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.roads.Roadway;
import com.example.fxsuite.avatar.roads.Roadworks;
import com.example.fxsuite.avatar.sim.Vec3;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

import java.util.ArrayList;
import java.util.List;

/**
 * Roads as laid ({@link Roadworks}), drawn: each road's pavement and markings ({@link RoadworkModel}), each drawn once
 * where one lies over another, in stretches of about {@link #STRETCH} along it; each stretch drawn only when in view
 * ({@link View}), and faded into the haze as the land is ({@link Haze}), worked out again for those in view where the
 * camera has moved far enough for it to show, a few a frame. The walls its roads want, as they are.
 */
public final class RoadScenery implements Scenery {

    /** How long along its road a stretch is, about. */
    static final double STRETCH = 20_000;

    /** A stretch as drawn: its meshes, the box round them, and where their haze was worked out from. */
    private static final class Drawn {
        final Group group;
        final List<TriangleMesh> meshes;
        final double[] box;
        final int size;
        boolean hazed;
        Vec3 hazedFrom;
        double hazedReach;

        Drawn(final Group group, final List<TriangleMesh> meshes, final double[] box, final int size) {
            this.group = group;
            this.meshes = meshes;
            this.box = box;
            this.size = size;
        }
    }

    private final Group root = new Group();
    private final List<Drawn> stretches = new ArrayList<>();
    private int next;

    private RoadScenery() {
    }

    /** {@code works}' roads drawn in stretches; the walls they want. Seen from nowhere yet: all of it drawn, clear. */
    public static RoadScenery of(final Roadworks works) {
        final RoadScenery scenery = new RoadScenery();
        for (int k = 0; k < works.roads().size(); k++) {
            // where a road before it has its pavement at its level, that drawn, not this; over it or under it, both
            final int before = k;
            final Roadway road = works.roads().get(k);
            final RoadworkModel.Covered covered = (x, z) -> works.pavedBefore(before, x, z, road.surface(x, z));
            for (final int[] stretch : stretches(road)) {
                scenery.add(new Group(RoadworkModel.pavement(road, stretch[0], stretch[1], covered),
                        RoadworkModel.markings(road, stretch[0], stretch[1], covered)));
            }
        }
        scenery.root.getChildren().add(RoadworkModel.walls(works));
        return scenery;
    }

    /** {@code road} in stretches, each from one of its points to another, about {@link #STRETCH} apart. */
    static List<int[]> stretches(final Roadway road) {
        final List<int[]> stretches = new ArrayList<>();
        int from = 0;
        while (from + 1 < road.size()) {
            int to = from + 1;
            while (to + 1 < road.size() && road.along(to) - road.along(from) < STRETCH) {
                to++;
            }
            stretches.add(new int[] {from, to});
            from = to;
        }
        return stretches;
    }

    private void add(final Group group) {
        final List<TriangleMesh> meshes = new ArrayList<>();
        meshes(group, meshes);
        final double[] box = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
            Double.NEGATIVE_INFINITY};
        int size = 0;
        for (final TriangleMesh mesh : meshes) {
            final float[] points = mesh.getPoints().toArray(null);
            for (int v = 0; v < points.length / 3; v++) {
                box[0] = Math.min(box[0], points[3 * v]);
                box[1] = Math.min(box[1], points[3 * v + 2]);
                box[2] = Math.max(box[2], points[3 * v]);
                box[3] = Math.max(box[3], points[3 * v + 2]);
            }
            size += points.length / 3;
        }
        if (size == 0) {
            return;
        }
        root.getChildren().add(group);
        stretches.add(new Drawn(group, meshes, box, size));
    }

    private static void meshes(final Node node, final List<TriangleMesh> into) {
        if (node instanceof MeshView view && view.getMesh() instanceof TriangleMesh mesh) {
            into.add(mesh);
        } else if (node instanceof Parent parent) {
            parent.getChildrenUnmodifiable().forEach(child -> meshes(child, into));
        }
    }

    @Override
    public Node node() {
        return root;
    }

    /** How many stretches it is drawn in. */
    int stretches() {
        return stretches.size();
    }

    /** How many of them are drawn now. */
    int shown() {
        return (int) stretches.stream().filter(d -> d.group.isVisible()).count();
    }

    /**
     * Each stretch drawn if {@code view} sees it; and the haze of those in view worked out again where the camera has
     * moved far enough for it to show, round them in turn, no more than {@link TerrainModel#BUDGET} points' worth a
     * frame.
     */
    @Override
    public void seenFrom(final View view) {
        final Vec3 eye = view.eye();
        for (final Drawn d : stretches) {
            d.group.setVisible(view.draws(Haze.near(d.box, eye)) && view.sees(d.box[0], d.box[1], d.box[2], d.box[3]));
        }
        int left = TerrainModel.BUDGET;
        int done = 0;
        for (int k = 0; k < stretches.size() && (left > 0 || done == 0); k++) {
            next = (next + 1) % stretches.size();
            final Drawn d = stretches.get(next);
            if (d.group.isVisible() && Haze.stale(d.box, d.hazed, d.hazedFrom, d.hazedReach, view)) {
                if (d.size > left && done > 0) {
                    break;
                }
                boolean any = false;
                for (final TriangleMesh mesh : d.meshes) {
                    any |= Haze.haze(mesh, view);
                }
                d.hazed = any;
                d.hazedFrom = eye;
                d.hazedReach = view.reach();
                left -= d.size;
                done++;
            }
        }
    }
}
