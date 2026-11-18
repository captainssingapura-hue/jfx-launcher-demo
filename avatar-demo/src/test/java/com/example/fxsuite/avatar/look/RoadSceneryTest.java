package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.roads.Profile;
import com.example.fxsuite.avatar.roads.Roadway;
import com.example.fxsuite.avatar.roads.Roadworks;
import com.example.fxsuite.avatar.roads.Sections;
import com.example.fxsuite.avatar.sim.Vec2;
import com.example.fxsuite.avatar.sim.Vec3;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Roads drawn in stretches: each road from end to end, stretch after stretch; only those in view drawn; those far off
 * faded into the haze, those near clear.
 */
class RoadSceneryTest {

    /** A road 20 km long, due north from the origin. */
    private static Roadworks longRoad() {
        final List<Vec2> line = new ArrayList<>();
        for (double z = 0; z <= 2_000_000; z += 1_000) {
            line.add(new Vec2(0, z));
        }
        return Roadworks.design(Profile.level(0), List.of(new Roadworks.Plan("long", line,
                Sections.standard().get("two-lane road"), List.of(), List.of())));
    }

    @Test
    void eachRoadFromEndToEndStretchAfterStretch() {
        final Roadway road = longRoad().roads().getFirst();
        final List<int[]> stretches = RoadScenery.stretches(road);
        assertEquals(0, stretches.getFirst()[0]);
        assertEquals(road.size() - 1, stretches.getLast()[1]);
        for (int k = 0; k + 1 < stretches.size(); k++) {
            assertEquals(stretches.get(k)[1], stretches.get(k + 1)[0], "each where the last ends");
            final double long_ = road.along(stretches.get(k)[1]) - road.along(stretches.get(k)[0]);
            assertTrue(long_ >= RoadScenery.STRETCH && long_ < RoadScenery.STRETCH + 2 * Roadway.STEP, "" + long_);
        }
    }

    @Test
    void onlyThoseInViewDrawnThoseFarOffHazed() {
        final RoadScenery scenery = RoadScenery.of(longRoad());
        assertTrue(scenery.stretches() > 50);
        assertEquals(scenery.stretches(), scenery.shown(), "seen from nowhere yet: all of it");
        // at its start looking north, along it: all of it ahead in view as far as it sees; looking south, only near it
        final Vec3 eye = new Vec3(0, -200, 0);
        scenery.seenFrom(new View(eye, 0, 35, Sight.reach(0), 0));
        final int ahead = scenery.shown();
        assertTrue(ahead > 0 && ahead < scenery.stretches(), "as far as it sees: " + ahead);
        scenery.seenFrom(new View(eye, 180, 35, Sight.reach(0), 0));
        assertTrue(scenery.shown() < ahead, "behind it, only what is round it: " + scenery.shown());
        // looking north again, a frame after another: the far stretches hazed, the near clear
        for (int frame = 0; frame < 200; frame++) {
            scenery.seenFrom(new View(eye, 0, 35, Sight.reach(0), 0));
        }
        final Group root = (Group) scenery.node();
        double nearest = 1;
        double farthest = 0;
        for (final Node stretch : root.getChildren()) {
            if (!stretch.isVisible()) {
                continue;
            }
            for (final TriangleMesh mesh : meshes(stretch)) {
                final float[] points = mesh.getPoints().toArray(null);
                final float[] coords = mesh.getTexCoords().toArray(null);
                for (int v = 0; v < points.length / 3; v++) {
                    if (points[3 * v + 2] < 20_000) {
                        nearest = Math.min(nearest, coords[2 * v + 1]);
                    } else if (points[3 * v + 2] > Sight.BUILT * Sight.reach(0) * 0.9) {
                        farthest = Math.max(farthest, coords[2 * v + 1]);
                    }
                }
            }
        }
        assertEquals(Haze.CLEAR, nearest, 1e-6, "clear near");
        assertTrue(farthest > 0.5, "hazed far off: " + farthest);
    }

    private static List<TriangleMesh> meshes(final Node node) {
        final List<TriangleMesh> found = new ArrayList<>();
        if (node instanceof MeshView view) {
            found.add((TriangleMesh) view.getMesh());
        } else if (node instanceof javafx.scene.Parent parent) {
            parent.getChildrenUnmodifiable().forEach(child -> found.addAll(meshes(child)));
        }
        return found;
    }
}
