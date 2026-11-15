package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Vec3;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The church and the temple hall dressed: every face laid; their roofs' coverings chosen apart from
 * their palettes; their faces on their skeletons' curves.
 */
class FacesTest {

    @Test
    void theChurchsFacesAreAllLaidItsRoofsAndSpireAsChosen() {
        final Building church = Church.standard().building();
        final List<Solid> solids = church.dressed(church.own(), Palette.STONE);
        for (final Sheet sheet : church.sheets()) {
            assertTrue(solids.stream().anyMatch(s -> s.name().startsWith(sheet.name() + ", ")), sheet.name());
        }
        // walls stone, as the palette says; slate on the roofs and copper on the spire, as chosen
        assertTrue(solids.stream().filter(s -> s.name().startsWith("nave, left wall")).allMatch(s -> s.material() == Material.STONE));
        assertTrue(solids.stream().filter(s -> s.name().startsWith("nave roof")).allMatch(s -> s.material() == Material.SLATE));
        assertTrue(solids.stream().filter(s -> s.name().startsWith("spire")).allMatch(s -> s.material() == Material.COPPER));
        // another roof covering, the walls brick: each chosen apart
        final List<Solid> other = church.dressed(List.of(church.option("Roofs", "Clay tiles"),
                church.option("Spire", "Slate")), Palette.BRICK);
        assertTrue(other.stream().filter(s -> s.name().startsWith("nave roof")).allMatch(s -> s.material() == Material.TILE));
        assertTrue(other.stream().filter(s -> s.name().startsWith("nave, left wall")).allMatch(s -> s.material() == Material.BRICK));
        // the side walls with their windows cut out: less than whole
        final Sheet left = church.sheets().stream().filter(s -> s.name().equals("nave, left wall")).findFirst().orElseThrow();
        assertTrue(left.area() < 2400 * 900 - 5 * 120 * 400, left.area() + " less six windows");
    }

    @Test
    void theSpiresFacesAreOnItsFlaredEdgesTheApseOnItsCurve() {
        final Building church = Church.standard().building();
        for (final Sheet face : church.sheets().stream().filter(s -> s.kind().equals("spire")).toList()) {
            // each face's points within the tower's square, up to the tip, narrowing as it rises
            for (final Vec3 p : face.points()) {
                assertTrue(Math.abs(p.x()) <= 250 + 1e-6 && Math.abs(p.y() - 1450) <= 250 + 1e-6);
            }
            assertTrue(face.points().stream().anyMatch(p -> p.distance(new Vec3(0, 1450, 3700)) < 1e-6));
        }
        final Sheet apse = church.sheets().stream().filter(s -> s.name().equals("apse, wall")).findFirst().orElseThrow();
        for (final Vec3 p : apse.points()) {
            assertEquals(500, Math.hypot(p.x(), p.y() + 1200), 2, "on the apse's half circle");
        }
    }

    @Test
    void theTemplesRoofSweepsUpAtItsCornersInItsChosenTiles() {
        final Building temple = Temple.standard().building();
        final List<Solid> solids = temple.dressed(temple.own(), Temple.LACQUERED);
        for (final Sheet sheet : temple.sheets()) {
            assertTrue(solids.stream().anyMatch(s -> s.name().startsWith(sheet.name() + ", ")), sheet.name());
        }
        assertTrue(solids.stream().filter(s -> s.name().startsWith("roof, ")).allMatch(s -> s.material() == Material.GLAZED));
        assertTrue(solids.stream().filter(s -> s.name().startsWith("column")).allMatch(s -> s.material() == Material.LACQUER));
        assertTrue(solids.stream().filter(s -> s.name().startsWith("bracket")).allMatch(s -> s.material() == Material.PAINTED));
        final List<Solid> green = temple.dressed(List.of(temple.option("Roof tiles", "Green glazed")), Temple.LACQUERED);
        assertTrue(green.stream().filter(s -> s.name().startsWith("roof, ")).allMatch(s -> s.material() == Material.GREEN_GLAZED));
        // the front slope through its eave's corners, up and out of its middle
        final Sheet front = temple.sheets().stream().filter(s -> s.name().equals("roof, front slope")).findFirst().orElseThrow();
        assertTrue(front.points().stream().anyMatch(p -> p.distance(new Vec3(1640, 960, 670)) < 1e-6), "through the corner");
        final double middle = front.points().stream().filter(p -> Math.abs(p.x()) < 1e-6 && Math.abs(p.y() - 910) < 1)
                .mapToDouble(Vec3::z).min().orElseThrow();
        assertEquals(600, middle, 1e-6, "the eave's middle 70 below its corners");
        // and up to the ridge
        assertTrue(front.points().stream().anyMatch(p -> p.distance(new Vec3(-900, 0, 1375)) < 1e-6));
        // as many columns as the hall has, each from the platform to its head
        assertEquals(24, solids.stream().filter(s -> s.name().startsWith("column")).count());
    }
}
