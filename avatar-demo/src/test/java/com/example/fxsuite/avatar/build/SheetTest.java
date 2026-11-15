package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Vec3;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Faces of any shape: a flat one with holes cut into triangles that cover it less its holes; a
 * curved one filled in from its edges, through its corners and along its edges, facing out; and either
 * laid as a slab along its normals.
 */
class SheetTest {

    private static List<Vec3> square(final double x0, final double z0, final double size) {
        return List.of(new Vec3(x0, 0, z0), new Vec3(x0 + size, 0, z0), new Vec3(x0 + size, 0, z0 + size),
                new Vec3(x0, 0, z0 + size));
    }

    @Test
    void aFlatFaceWithHolesIsCutIntoTrianglesCoveringItLessItsHoles() {
        // a wall 10 m by 4 m, two windows cut out of it
        final List<Vec3> wall = List.of(new Vec3(0, 0, 0), new Vec3(1000, 0, 0), new Vec3(1000, 0, 400),
                new Vec3(0, 0, 400));
        final Sheet sheet = Sheet.flat("wall", "walls", wall, List.of(square(200, 100, 150), square(600, 100, 150)),
                new Vec3(0, -1, 0));
        assertEquals(1000 * 400 - 2 * 150 * 150, sheet.area(), 1e-6);
        // every triangle facing out, the way the face does
        for (final int[] t : sheet.triangles()) {
            final Vec3 n = Sheet.cross(sheet.points().get(t[1]).subtract(sheet.points().get(t[0])),
                    sheet.points().get(t[2]).subtract(sheet.points().get(t[0])));
            assertTrue(n.y() < 0, "anticlockwise seen from outside");
        }
        // a doorway cut into its foot, as part of its outline: a concave outline
        final List<Vec3> notched = List.of(new Vec3(0, 0, 0), new Vec3(400, 0, 0), new Vec3(400, 0, 250),
                new Vec3(600, 0, 250), new Vec3(600, 0, 0), new Vec3(1000, 0, 0), new Vec3(1000, 0, 400), new Vec3(0, 0, 400));
        assertEquals(1000 * 400 - 200 * 250, Sheet.flat("front", "walls", notched, List.of(), new Vec3(0, -1, 0)).area(), 1e-6);
    }

    @Test
    void aCurvedFaceFilledInFromItsEdgesRunsThroughThemAndFacesOut() {
        // a quarter of a cylinder's side: curved foot and head, straight sides
        final List<Vec3> foot = new ArrayList<>();
        final List<Vec3> head = new ArrayList<>();
        for (int k = 0; k <= 16; k++) {
            final double a = Math.PI / 2 * k / 16;
            foot.add(new Vec3(500 * Math.cos(a), 500 * Math.sin(a), 0));
            head.add(new Vec3(500 * Math.cos(a), 500 * Math.sin(a), 300));
        }
        final Sheet wall = Sheet.patch("curved", "walls", foot, List.of(foot.getLast(), head.getLast()), head,
                List.of(foot.getFirst(), head.getFirst()), 16, 3, new Vec3(0, 0, 150));
        for (int p = 0; p < wall.points().size(); p++) {
            final Vec3 at = wall.points().get(p);
            assertEquals(500, Math.hypot(at.x(), at.y()), 2, "on the cylinder");
            final Vec3 out = wall.normals().get(p);
            assertTrue(out.x() * at.x() + out.y() * at.y() > 0, "facing out");
        }
        assertEquals(Math.PI / 2 * 500 * 300, wall.area(), Math.PI / 2 * 500 * 300 * 0.01);
        // three-sided: a cone's quarter, up to its apex
        final Vec3 apex = new Vec3(0, 0, 400);
        final Sheet cone = Sheet.patch("cone", "roof", foot, List.of(foot.getLast(), apex), List.of(apex),
                List.of(foot.getFirst(), apex), 16, 6, new Vec3(0, 0, -100));
        assertTrue(cone.points().stream().anyMatch(p -> p.distance(apex) < 1e-9), "up to its apex");
        assertTrue(cone.normals().stream().allMatch(n -> n.z() > 0), "facing up and out");
    }

    @Test
    void laidAsASlabAlongItsNormals() {
        final Sheet sheet = Sheet.flat("floor", "floor", square(0, 0, 100).stream()
                .map(p -> new Vec3(p.x(), p.z(), 0)).toList(), List.of(), new Vec3(0, 0, 1));
        final List<Solid> slab = sheet.slab("slab", Palette.CONCRETE, Role.STRUCTURE, -20, 0);
        assertEquals(sheet.triangles().size(), slab.size());
        for (final Solid solid : slab) {
            assertTrue(solid.corners().stream().allMatch(p -> p.up() >= -20 - 1e-9 && p.up() <= 1e-9));
        }
    }
}
