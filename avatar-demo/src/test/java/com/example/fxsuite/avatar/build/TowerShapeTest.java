package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Body;
import com.example.fxsuite.avatar.sim.Controls;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Hit;
import com.example.fxsuite.avatar.sim.Obstacles;
import com.example.fxsuite.avatar.sim.Vec3;
import com.example.fxsuite.avatar.sim.Volume;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The towers in the way as they are shaped: each section its plan's outline stood up, curves and all;
 * a parapet round each roof nothing stands on. A round tower is round, an L is open in its corner, a
 * setback's terrace is stood on behind its parapet.
 */
class TowerShapeTest {

    private static Obstacles world(final List<Volume> shape) {
        return new Obstacles(List.of()).with(shape, shape.stream().map(v -> 0).toList());
    }

    @Test
    void everySectionIsItsOutlineStoodUpAndItsRoofHasAParapet() {
        for (final Tower tower : Towers.all()) {
            final Obstacles world = world(TowerBuildings.shape(tower));
            final Plan.Layout top = tower.sections().getLast().plan().layout();
            final double[] box = top.bounds();
            final Plan.Pt middle = new Plan.Pt((box[0] + box[2]) / 2, (box[1] + box[3]) / 2);
            if (!top.inside(middle)) {
                continue;
            }
            // on its roof, in its middle; its parapet a little higher round it
            assertEquals(tower.height(), world.floor(middle.x(), middle.y(), 16, tower.height() + 20), 1e-6, tower.name());
            assertEquals(tower.height() + TowerBuildings.PARAPET, world.floor(box[0] + 10, middle.y(), 5,
                    tower.height() + 200), 1e-6, tower.name() + "'s parapet");
        }
    }

    @Test
    void aRoundTowerIsRoundItsBoxsCornersOpen() {
        final Tower round = Towers.round();
        final Plan.Layout plan = round.sections().getFirst().plan().layout();
        final double[] box = plan.bounds();
        final Obstacles world = world(TowerBuildings.shape(round));
        // just inside its box's corner: open ground; a ray level at 10 m toward its middle meets its curve
        final double cx = (box[0] + box[2]) / 2;
        final double cz = (box[1] + box[3]) / 2;
        final double radius = (box[2] - box[0]) / 2;
        assertEquals(0, world.floor(box[0] + 100, box[1] + 100, 16, 50), 1e-9, "its box's corner, open");
        final Vec3 from = new Vec3(cx - 2 * radius, -1000, cz - 2 * radius);
        final Vec3 toward = new Vec3(1, 0, 1).normalize();
        final Hit hit = world.ray(from, toward, 10_000);
        assertNotNull(hit);
        assertEquals(radius, Math.hypot(hit.at().x() - cx, hit.at().z() - cz), radius * 0.01, "on its curve");
    }

    @Test
    void anLIsOpenInItsCorner() {
        final Tower l = Towers.lBlock();
        // its upper section: an L; somewhere in its box but outside its outline, open
        final Plan.Layout plan = l.sections().getLast().plan().layout();
        final double[] box = plan.bounds();
        final double base = l.base(l.sections().size() - 1);
        final Obstacles world = world(TowerBuildings.shape(l));
        boolean open = false;
        for (double x = box[0] + 200; x < box[2]; x += 400) {
            for (double z = box[1] + 200; z < box[3]; z += 400) {
                if (!plan.inside(new Plan.Pt(x, z))) {
                    open = true;
                    assertEquals(base, world.floor(x, z, 16, l.height() + 500), 1e-6, "on the podium, in the L's corner");
                }
            }
        }
        assertTrue(open, "some of its box outside its L");
    }

    @Test
    void aSetbacksTerraceIsStoodOnAndItsParapetStopsAWalkOffIt() {
        final Tower setback = Towers.setback();
        final Obstacles world = world(TowerBuildings.shape(setback));
        final Plan.Layout podium = setback.sections().getFirst().plan().layout();
        final Plan.Layout shaft = setback.sections().get(1).plan().layout();
        final double terrace = setback.base(1);
        final double[] outer = podium.bounds();
        final double[] inner = shaft.bounds();
        // between the shaft and the podium's edge: its terrace
        final double x = (outer[0] + inner[0]) / 2;
        final double z = (outer[1] + outer[3]) / 2;
        assertEquals(terrace, world.floor(x, z, 16, terrace + 20), 1e-6);
        // walked out across the terrace toward its edge: stopped by the parapet
        Body body = Body.standing(new Vec3(x, -terrace, z), -90);
        for (int i = 0; i < 240; i++) {
            body = body.step(new Controls(1, 0, 0, 0, false), 1.0 / 60, world);
        }
        assertTrue(body.position().x() > outer[0], "still on the terrace: " + body.position());
        assertEquals(terrace, body.height(), 1e-6);
    }

    @Test
    void theTownsTowersAreInTheWayAsTheyAreShaped() {
        Course.town(Town::pieces);
        for (int k = 3; k < Town.SITES.size(); k++) {
            final Course.Piece tower = Town.pieces().get(k);
            assertFalse(tower.volumes().isEmpty(), Town.SITES.get(k).name());
            assertTrue(tower.solid().isEmpty(), Town.SITES.get(k).name());
        }
    }
}
