package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Body;
import com.example.fxsuite.avatar.sim.Controls;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Obstacles;
import com.example.fxsuite.avatar.sim.Vec2;
import com.example.fxsuite.avatar.sim.Vec3;
import com.example.fxsuite.avatar.sim.Volume;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pavilions: open, hollow but for their platform, pillars, beams and roof, each in the way as it is
 * shaped; walked up into by their steps, stood in under their roofs, stopped by their pillars; made
 * to order, their roofs' tiles and pillars' material chosen.
 */
class PavilionTest {

    private static Obstacles world(final List<Volume> shape) {
        return new Obstacles(List.of()).with(shape, shape.stream().map(v -> 0).toList());
    }

    @Test
    void everyKindIsMadeDressedAndInTheWay() {
        for (final Pavilion.Spec spec : Pavilions.PRESETS) {
            final Pavilion pavilion = Pavilion.of(spec);
            final Building building = pavilion.building();
            final List<Solid> solids = building.dressed(building.own(), building.palettes().getFirst());
            assertFalse(solids.isEmpty(), spec.name());
            for (final Sheet sheet : building.sheets()) {
                assertTrue(solids.stream().anyMatch(s -> s.name().startsWith(sheet.name() + ", ")), sheet.name());
            }
            assertEquals(spec.tiles(), solids.stream().filter(s -> s.role() == Role.ROOF).findFirst().orElseThrow()
                    .material(), spec.name() + "'s own tiles");
            assertEquals(spec.columns(), solids.stream().filter(s -> s.name().startsWith("pillar")).findFirst()
                    .orElseThrow().material(), spec.name() + "'s own pillars");
            assertEquals(pavilion.pillars().size(), pavilion.skeleton().edges().stream()
                    .filter(e -> pavilion.skeleton().vertices().get(e.from()).name().endsWith(", foot")).count());
            // in its middle: its platform under, its roof's underside over, room to stand between
            final Obstacles world = world(pavilion.shape());
            assertEquals(pavilion.floor(), world.floor(0, 0, 16, pavilion.floor() + 20), 1e-6, spec.name());
            assertEquals(pavilion.eaves(), world.ceiling(0, 0, 32, pavilion.floor() + Body.TALL), 1e-6, spec.name());
            assertFalse(world.slide(0, 0, 100, 0, 32, pavilion.floor() + 20, pavilion.floor() + Body.TALL).blocked(),
                    spec.name() + ": room to move in it");
            // on its roof: as high as its ridge over its middle
            assertEquals(pavilion.ridge(), world.floor(0, 0, 1, pavilion.ridge() + 50), 2, spec.name() + "'s roof");
        }
    }

    @Test
    void walkedUpItsStepsAndInStoppedByItsPillars() {
        for (final Pavilion.Spec spec : Pavilions.PRESETS) {
            final Pavilion pavilion = Pavilion.of(spec);
            final Obstacles world = world(pavilion.shape());
            // from out before its front, walking in: up its steps, onto its platform, in under its roof
            Body body = Body.standing(new Vec3(0, 0, spec.depth() / 2 + 900), 180);
            for (int i = 0; i < 120 && body.position().z() > 0; i++) {
                body = body.step(new Controls(1, 0, 0, 0, false), 1.0 / 60, world);
            }
            for (int i = 0; i < 600 && body.position().z() > 0; i++) {
                body = body.step(new Controls(1, 0, 0, 0, false), 1.0 / 60, world);
            }
            assertTrue(body.position().z() <= 0, spec.name() + ": in to its middle: " + body.position());
            assertEquals(pavilion.floor(), body.height(), 1e-6, spec.name() + ": up on its platform");
            // straight at a pillar from within: stopped by it
            final Vec2 pillar = pavilion.pillars().getFirst();
            final double yaw = Math.toDegrees(Math.atan2(pillar.x(), pillar.z()));
            Body at = Body.standing(new Vec3(0, -pavilion.floor(), 0), yaw);
            for (int i = 0; i < 300; i++) {
                at = at.step(new Controls(1, 0, 0, 0, false), 1.0 / 60, world);
            }
            final double gap = Math.hypot(at.position().x() - pillar.x(), at.position().z() - pillar.z());
            assertTrue(gap >= Body.RADIUS + spec.thick() / 2 * 0.9 - 1, spec.name() + ": stopped by a pillar, " + gap);
        }
    }

    @Test
    void itsRoofsAreShapedAsTheyAre() {
        // the hexagonal one's roof: under its point at its middle, down to its eaves at a side's middle
        final Pavilion hexagon = Pavilion.of(Pavilions.PRESETS.get(1));
        final Obstacles world = world(hexagon.shape());
        final double apothem = hexagon.spec().width() / 2 * Math.cos(Math.PI / 6) + hexagon.spec().overhang();
        final double half = (hexagon.ridge() + hexagon.eaves()) / 2;
        assertEquals(half, world.floor(0, apothem / 2, 1, hexagon.ridge() + 50), 3, "half way down to its front");
        assertEquals(half, world.floor(apothem / 2 * Math.sin(Math.toRadians(60)),
                apothem / 2 * Math.cos(Math.toRadians(60)), 1, hexagon.ridge() + 50), 3, "and to the next side round");
        // the gable's: under its ridge along its length, its ends open beyond its eaves
        final Pavilion gable = Pavilion.of(Pavilions.PRESETS.get(5));
        final Obstacles g = world(gable.shape());
        assertEquals(gable.ridge(), g.floor(gable.spec().width() / 2, 0, 1, gable.ridge() + 50), 1);
        assertEquals(gable.ridge(), g.floor(-gable.spec().width() / 2, 0, 1, gable.ridge() + 50), 1);
    }

    @Test
    void itsEavesTurnUpAtTheirCornersTheirMiddlesWhereStraightOnesAre() {
        for (final Pavilion.Spec spec : Pavilions.PRESETS) {
            final Pavilion pavilion = Pavilion.of(spec);
            final List<Sheet> roof = pavilion.sheets().stream().filter(s -> s.name().startsWith("roof")).toList();
            if (roof.isEmpty()) {
                continue;
            }
            final double lowest = roof.stream().flatMap(s -> s.points().stream()).mapToDouble(Vec3::z).min().orElseThrow();
            final double cornerHigh = pavilion.corners().getFirst().z();
            assertEquals(pavilion.eaves() + spec.upturn(), cornerHigh, 1e-9, spec.name() + "'s corners");
            if (spec.upturn() > 0 && spec.footprint() != Pavilion.Footprint.ROUND) {
                // the eaves' middles down where a straight eave's are, their corners up
                // (filled in between bowed edges, a face may dip a few centimetres under them)
                assertEquals(pavilion.eaves(), lowest, 6, spec.name() + "'s eaves' middles");
                assertTrue(pavilion.skeleton().edges().stream().anyMatch(e -> e.curved()), spec.name() + " curved");
            } else {
                assertEquals(cornerHigh, lowest, 1, spec.name() + ": its rim all as high");
            }
        }
        assertThrows(IllegalArgumentException.class, () -> new Pavilion.Spec("x", Pavilion.Footprint.RECTANGLE, 900, 500, 3,
                Pavilion.Pillar.ROUND, 30, 300, Pavilion.Roof.GABLE, 0.6, 90, 30, Material.GREY_TILE, Material.TIMBER, 50),
                "no corners on a gable to turn up");
    }

    @Test
    void madeOnlyToOrdersThatStand() {
        assertThrows(IllegalArgumentException.class, () -> new Pavilion.Spec("x", Pavilion.Footprint.HEXAGON, 600, 600, 1,
                Pavilion.Pillar.ROUND, 30, 300, Pavilion.Roof.HIP, 0.7, 100, 30, Material.GLAZED, Material.LACQUER),
                "a hip roof over a hexagon");
        assertThrows(IllegalArgumentException.class, () -> new Pavilion.Spec("x", Pavilion.Footprint.RECTANGLE, 600, 900, 2,
                Pavilion.Pillar.ROUND, 30, 300, Pavilion.Roof.HIP, 0.7, 100, 30, Material.GLAZED, Material.LACQUER),
                "a hip roof's ridge across its shorter side");
        assertThrows(IllegalArgumentException.class, () -> new Pavilion.Spec("x", Pavilion.Footprint.SQUARE, 500, 500, 1,
                Pavilion.Pillar.ROUND, 30, 150, Pavilion.Roof.PYRAMID, 0.7, 100, 30, Material.GLAZED, Material.LACQUER),
                "too low to stand under");
    }

    @Test
    void oneOfEachKindRoundTheTownsParkFacingTheTemple() {
        final List<Course.Piece> pieces = Town.park().stream().filter(p -> p.kind() == Course.Kind.BUILDING).toList();
        assertEquals(Pavilions.PRESETS.size(), pieces.size());
        assertEquals(Pavilions.PRESETS.size(), pieces.stream().map(Course.Piece::tint).distinct().count(), "one of each");
        final com.example.fxsuite.avatar.sim.Place middle = Town.middle();
        final List<Course.Piece> temple = TempleYard.pieces().stream().map(p -> p.within(Town.temple())).toList();
        int onIslands = 0;
        for (int a = 0; a < pieces.size(); a++) {
            final Course.Piece pavilion = pieces.get(a);
            assertFalse(pavilion.volumes().isEmpty());
            assertTrue(pavilion.solid().isEmpty(), "in the way only as it is shaped");
            assertFalse(Sites.drawn(pavilion.tint()).isEmpty());
            final double out = Math.hypot(pavilion.place().x() - middle.x(), pavilion.place().z() - middle.z());
            assertTrue(out + Math.hypot(pavilion.block().halfWidth(), pavilion.block().halfDepth()) < Town.PARK,
                    "in the park");
            // facing the temple
            final Vec3 facing = com.example.fxsuite.avatar.sim.Num.heading(pavilion.place().yaw());
            assertTrue((facing.x() * (middle.x() - pavilion.place().x()) + facing.z() * (middle.z() - pavilion.place().z()))
                    / out > 0.99, "facing the temple");
            if (Math.abs(out - Town.POND_OUT) < 1 && Math.abs(pavilion.place().z() - middle.z()) < 1) {
                onIslands++;
                assertTrue(Math.max(pavilion.block().halfWidth(), pavilion.block().halfDepth()) < Town.ISLAND,
                        "on its island");
            }
            for (final Course.Piece part : temple) {
                assertFalse(pavilion.block().overlaps(part.block()), "clear of the temple");
            }
            for (int b = a + 1; b < pieces.size(); b++) {
                assertFalse(pavilion.block().overlaps(pieces.get(b).block()), a + " and " + b);
            }
        }
        assertEquals(2, onIslands, "one in the middle of each pond");
    }
}
