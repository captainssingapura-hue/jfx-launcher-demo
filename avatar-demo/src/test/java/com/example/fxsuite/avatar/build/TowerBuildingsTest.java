package com.example.fxsuite.avatar.build;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Towers assembled into buildings: their surfaces made from their stacks apart from their skeletons,
 * and the two agreeing; a wall to each straight run of an outline and each piece of a curve, a
 * parapet where nothing stands on it; floors and roofs of any shape; a design for each section, so
 * different designs stack.
 */
class TowerBuildingsTest {

    @Test
    void eachPresetsSurfacesAgreeWithItsSkeletonAndAreAllDressed() {
        for (final Tower tower : Towers.all()) {
            final Building building = TowerBuildings.of(tower);
            assertEquals(List.of(), TowerBuildings.mismatches(building, tower), tower.name());
            final List<Solid> solids = building.dressed(building.own(), Palette.CONCRETE);
            for (final Surface wall : building.surfaces()) {
                assertTrue(solids.stream().anyMatch(s -> s.name().startsWith(wall.name() + ", ")),
                        tower.name() + ": " + wall.name() + " dressed");
            }
            for (final Plate plate : building.plates()) {
                assertTrue(solids.stream().anyMatch(s -> s.name().startsWith(plate.name() + ", ")),
                        tower.name() + ": " + plate.name() + " laid");
            }
            // nothing far beyond its plans: fins and parapets aside, within a metre
            final double[] reach = new double[4];
            for (final Tower.Section section : tower.sections()) {
                final double[] box = section.plan().layout().bounds();
                for (int k = 0; k < 4; k++) {
                    reach[k] = k < 2 ? Math.min(reach[k], box[k]) : Math.max(reach[k], box[k]);
                }
            }
            for (final Solid solid : solids) {
                for (final Point p : solid.corners()) {
                    assertTrue(p.across() >= reach[0] - 100 && p.across() <= reach[2] + 100
                            && p.along() >= reach[1] - 100 && p.along() <= reach[3] + 100
                            && p.up() >= -40 && p.up() <= tower.height()
                                    + Math.max(TowerBuildings.PARAPET + 20, TowerBuildings.OVERRUN + 1),
                            tower.name() + ": " + solid.name() + " at " + p);
                }
            }
        }
    }

    @Test
    void aWallToEachStraightRunAndToEachPieceOfACurve() {
        final Tower setback = Towers.setback();
        final Building building = TowerBuildings.of(setback);
        assertEquals(3 * 4, building.surfaces().size(), "four walls to each of its three sections");
        // the shaft's front: 40 m, its columns every 8 m along it, over its 28 floors, a parapet over them
        final Surface front = building.surfaces().stream().filter(s -> s.kind().equals("shaft walls"))
                .filter(s -> s.out().along() > 0.9).findFirst().orElseThrow();
        assertEquals(4000, front.width(), 1e-6);
        assertEquals(List.of(0.0, 800.0, 1600.0, 2400.0, 3200.0, 4000.0),
                java.util.Arrays.stream(front.columns()).boxed().toList());
        assertEquals(28, front.storeys());
        assertEquals(TowerBuildings.PARAPET, front.parapet(), 1e-9, "the top stands back from it: its roof a terrace");
        assertEquals(1000, front.at(0, 0, 0).up(), 1e-9, "from the podium's roof up");
        // round: a wall to each piece of the curve, each no wider than a panel, facing out
        final Building round = TowerBuildings.of(Towers.round());
        assertEquals(round.surfaces().size(), Towers.round().sections().getFirst().plan().layout().shaped().getFirst().size());
        for (final Surface wall : round.surfaces()) {
            assertTrue(wall.width() <= Corner.PANEL + 1e-6);
            final Point middle = wall.at(wall.width() / 2, 0, 0);
            assertTrue(wall.out().across() * middle.across() + wall.out().along() * middle.along() > 0, "facing out");
        }
    }

    @Test
    void aParapetOnlyWhereNothingStandsOnTheWall() {
        // two sections of one plan: the lower's walls go on up into the upper's, a parapet on the upper's only
        final Plan plan = Plans.of(Plans.Shape.RECTANGLE, new Plans.Size(3200, 2400, 800, 0));
        final Tower twice = new Tower("twice", List.of(new Tower.Section("lower", plan, 10, 400),
                new Tower.Section("upper", plan, 10, 400)));
        final Building building = TowerBuildings.of(twice);
        assertTrue(building.surfaces().stream().filter(s -> s.kind().equals("lower walls")).allMatch(s -> s.parapet() == 0));
        assertTrue(building.surfaces().stream().filter(s -> s.kind().equals("upper walls"))
                .allMatch(s -> s.parapet() == TowerBuildings.PARAPET));
        assertEquals(List.of(), TowerBuildings.mismatches(building, twice));
    }

    @Test
    void floorsAndRoofsOfAnyShapeLaidInTriangles() {
        final Tower round = Towers.round();
        final Building building = TowerBuildings.of(round);
        assertEquals(44, building.plates().stream().filter(p -> p.kind().equals("round tower floors")).count());
        assertEquals(1, building.plates().stream().filter(p -> p.kind().equals("round tower roof")).count());
        final Plate roof = building.plates().getLast();
        final double area = round.sections().getFirst().plan().layout().area();
        assertEquals(area, roof.area(), 1e-6);
        final double triangles = roof.triangles().stream().mapToDouble(t -> Plan.Layout.signedArea(List.of(t))).sum();
        assertEquals(area, triangles, 1e-3, "its triangles cover it, each anticlockwise");
        // an L, not convex: its triangles still exactly it
        final Plate l = TowerBuildings.of(Towers.lBlock()).plates().getLast();
        assertEquals(l.area(), l.triangles().stream().mapToDouble(t -> Plan.Layout.signedArea(List.of(t))).sum(), 1e-3);
        assertTrue(l.triangles().stream().allMatch(t -> Plan.Layout.signedArea(List.of(t)) > 0));
    }

    @Test
    void differentDesignsStackOnePerSection() {
        final Building building = TowerBuildings.of(Towers.setback());
        assertEquals(List.of("podium", "shaft", "top"), List.copyOf(building.choices().keySet()));
        assertEquals("Shopfront base", building.own().getFirst().name(), "a podium's own, its base");
        assertEquals("Curtain wall", building.own().get(1).name());
        final List<Solid> solids = building.dressed(List.of(building.option("podium", "Shopfront base"),
                building.option("shaft", "Finned curtain wall"), building.option("top", "Ribbon windows")), Palette.STONE);
        assertTrue(solids.stream().anyMatch(s -> s.name().startsWith("shaft, ") && s.name().contains(", fin ")));
        assertTrue(solids.stream().noneMatch(s -> s.name().startsWith("podium, ") && s.name().contains(", fin ")));
        assertTrue(solids.stream().anyMatch(s -> s.name().startsWith("podium, ") && s.material() == Material.STONE),
                "its base stone, in the stone palette");
        assertTrue(solids.stream().anyMatch(s -> s.name().startsWith("top, ") && s.name().contains("ribbon")));
    }

    @Test
    void whereSurfacesAndSkeletonDisagreeItIsSaid() {
        final Tower tower = Towers.setback();
        final Building building = TowerBuildings.of(tower);
        // one of its walls moved a metre along, made apart from its skeleton as it might be by mistake
        final List<Surface> walls = new ArrayList<>(building.surfaces());
        final Surface wall = walls.get(4);
        walls.set(4, new Surface(wall.name(), wall.kind(), wall.origin().plus(wall.u().times(100)), wall.u(), wall.v(),
                wall.out(), wall.width(), wall.height(), wall.columns(), wall.floors(), wall.parapet()));
        final Building wrong = new Building(building.name(), building.skeleton(), walls, building.plates(),
                building.structure(), building.choices(), building.palettes());
        final List<String> mismatches = TowerBuildings.mismatches(wrong, tower);
        assertFalse(mismatches.isEmpty());
        assertTrue(mismatches.stream().allMatch(m -> m.startsWith(wall.name() + ": ")), mismatches.toString());
    }
}
