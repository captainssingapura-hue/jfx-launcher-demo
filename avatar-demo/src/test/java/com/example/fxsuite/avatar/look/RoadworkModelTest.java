package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.roads.Carrier;
import com.example.fxsuite.avatar.roads.Profile;
import com.example.fxsuite.avatar.roads.Roadway;
import com.example.fxsuite.avatar.roads.Section;
import com.example.fxsuite.avatar.roads.Sections;
import com.example.fxsuite.avatar.sim.Vec2;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A road's pavement drawn as it can be seen: its strips' tops, its sides, its ends; nothing under its top or between its
 * strips; each face from its outside only, lit as it faces. So few triangles that a wide road round a kilometre's loop is
 * no burden: an eighth of what a box for every course of every strip would be, and far fewer the wider it is.
 */
class RoadworkModelTest {

    private static final Section ROAD = Sections.standard().get("two-lane road");

    /** A kilometre's loop, a circle, up on a structure all round: its pavement and its markings over every carrier alike. */
    private static Roadway loop(final Section section) {
        final double radius = 100_000 / (2 * Math.PI);
        final List<Vec2> line = new ArrayList<>();
        for (int k = 0; k < 1_000; k++) {
            final double t = 2 * Math.PI * k / 1_000;
            line.add(new Vec2(radius * Math.cos(t), radius * Math.sin(t)));
        }
        line.add(line.getFirst());
        return Roadway.design("loop", line, section, Profile.level(0),
                List.of(new Roadway.Given(0, Double.MAX_VALUE, Carrier.STRUCTURE)),
                List.of(new Roadway.Clearance(0, Double.MAX_VALUE, 600)));
    }

    /** How many triangles {@code node} draws, all told, and each mesh drawn from its outside only. */
    private static int triangles(final Node node) {
        if (node instanceof MeshView view) {
            assertEquals(CullFace.BACK, view.getCullFace(), "drawn from its outside only");
            final TriangleMesh mesh = (TriangleMesh) view.getMesh();
            return mesh.getFaces().size() / mesh.getFaceElementSize();
        }
        int count = 0;
        for (final Node child : ((Group) node).getChildren()) {
            count += triangles(child);
        }
        return count;
    }

    /** As many as a box for every course of every paved strip would be, on every segment. */
    private static int boxes(final Roadway road) {
        int courses = 0;
        for (final Section.Laid l : road.section().paved()) {
            courses += l.strip().courses().size();
        }
        return (road.size() - 1) * courses * 12;
    }

    @Test
    void anEighthOfABoxForEveryCourseFarLessTheWiderTheRoad() {
        final Roadway narrow = loop(ROAD);
        final Roadway wide = loop(ROAD.withLanes(6));
        final int few = triangles(RoadworkModel.pavement(narrow, Double.POSITIVE_INFINITY));
        final int more = triangles(RoadworkModel.pavement(wide, Double.POSITIVE_INFINITY));
        assertTrue(few * 8 <= boxes(narrow), few + " against " + boxes(narrow));
        assertTrue(more * 15 <= boxes(wide), more + " against " + boxes(wide));
        // a strip more adds only its top: two triangles a segment
        assertEquals(2 * 2 * 5 * (wide.size() - 1), more - few, "ten lanes more, their tops alone");
    }

    @Test
    void itsCoursesEndOnOnlyWhereItIsCutAway() {
        final Roadway road = loop(ROAD);
        final int whole = triangles(RoadworkModel.pavement(road, Double.POSITIVE_INFINITY));
        final int cut = triangles(RoadworkModel.pavement(road, 50_000));
        // half of it, and its end on there: each paved strip's courses
        int courses = 0;
        for (final Section.Laid l : ROAD.paved()) {
            courses += l.strip().courses().size();
        }
        assertEquals(whole / 2 + 2 * courses, cut, 2 * 4 * 2 + 2 * courses);
    }

    @Test
    void itsTopFacingUpItsSidesOutward() {
        final Group drawn = RoadworkModel.pavement(loop(ROAD), Double.POSITIVE_INFINITY);
        int up = 0;
        int level = 0;
        for (final Node n : drawn.getChildren()) {
            final TriangleMesh mesh = (TriangleMesh) ((MeshView) n).getMesh();
            for (int k = 0; k < mesh.getNormals().size(); k += 3) {
                final Point3D normal = new Point3D(mesh.getNormals().get(k), mesh.getNormals().get(k + 1),
                        mesh.getNormals().get(k + 2));
                if (normal.getY() < -0.99) {
                    up++;
                } else if (Math.abs(normal.getY()) < 1e-6) {
                    level++;
                } else {
                    throw new AssertionError("neither up nor across: " + normal);
                }
            }
        }
        assertTrue(up > 0 && level > 0, up + " up, " + level + " across");
    }
}
