package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Block;
import com.example.fxsuite.avatar.sim.Body;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Damage;
import com.example.fxsuite.avatar.sim.Demo;
import com.example.fxsuite.avatar.sim.Lift;
import com.example.fxsuite.avatar.sim.Place;
import com.example.fxsuite.avatar.sim.Play;
import com.example.fxsuite.avatar.sim.Turret;
import com.example.fxsuite.avatar.sim.Volume;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The town: every building on a site of its own, one piece each, solid where its parts are, clear of
 * the others and of the start; in its middle the temple, in its park, its ponds and pavilions; the
 * pyramids at its edge; every vehicle parked in the temple's courtyard; a gun on top of each tower's
 * every core.
 */
class TownTest {

    /** What stands in the town that is in someone's way: all of it but the park's lawn, its water and its paving. */
    private static List<Course.Piece> standing() {
        return Town.pieces().stream().filter(p -> p.kind() != Course.Kind.GARDEN).toList();
    }

    @Test
    void everyBuildingStandsOnASiteOfItsOwn() {
        final List<Course.Piece> pieces = Town.pieces();
        assertEquals(8, Town.SITES.size(), "the church, the apartments and six towers");
        for (int k = 0; k < Town.SITES.size(); k++) {
            final Course.Piece building = pieces.get(k);
            assertEquals(Course.Kind.BUILDING, building.kind());
            assertEquals(k, building.tint(), "its site's number, to draw it by");
            assertFalse(building.solid().isEmpty() && building.volumes().isEmpty(), "in the way");
            assertTrue(Double.isFinite(Damage.toughness(building)), "broken as a whole");
            assertTrue(building.block().top() + Body.TALL < Body.CEILING,
                    Town.SITES.get(k).name() + " can be flown over");
            // its solid blocks within its box
            for (final Block part : building.solid()) {
                assertTrue(part.top() <= building.block().top() + 1e-6, Town.SITES.get(k).name());
            }
            // and none where the temple stands, in the middle of the grid
            final Town.Site site = Town.SITES.get(k);
            assertFalse(site.column() == Town.TEMPLE_COLUMN && site.row() == Town.TEMPLE_ROW, site.name());
            for (int other = k + 1; other < Town.SITES.size(); other++) {
                final Town.Site there = Town.SITES.get(other);
                assertFalse(site.column() == there.column() && site.row() == there.row(), site.name() + " and " + there.name());
            }
        }
    }

    @Test
    void nothingStandsOnAnotherOrOnTheStart() {
        final List<Course.Piece> pieces = standing();
        for (int a = 0; a < pieces.size(); a++) {
            final Course.Piece one = pieces.get(a);
            for (int b = a + 1; b < pieces.size(); b++) {
                final Course.Piece other = pieces.get(b);
                // a wall's units meet each other and its gate, end to end
                final boolean walled = (one.kind() == Course.Kind.WALL_UNIT || one.tint() == TempleYard.KEYS + TempleYard.HALLS.size())
                        && (other.kind() == Course.Kind.WALL_UNIT || other.tint() == TempleYard.KEYS + TempleYard.HALLS.size());
                if (!walled) {
                    assertFalse(one.block().overlaps(other.block()), one.kind() + " " + one.tint() + " and " + other.kind()
                            + " " + other.tint());
                }
            }
            for (final Block part : one.solid()) {
                assertFalse(part.covers(0, 0, 100), one.kind() + " " + one.tint() + " on the start");
            }
            for (final Volume part : one.volumes()) {
                assertFalse(part.covers(0, 0, 100), one.kind() + " " + one.tint() + " on the start");
            }
        }
    }

    @Test
    void theTempleInTheMiddleInItsParkItsGateToTheStart() {
        final List<Course.Piece> pieces = Town.pieces();
        final Place middle = Town.middle();
        final List<Course.Piece> temple = pieces.stream().filter(p -> p.tint() >= TempleYard.KEYS
                && p.tint() < Pyramids.KEYS).toList();
        assertEquals(TempleYard.pieces().size(), temple.size(), "its halls, its wall and its gate");
        for (final Course.Piece part : temple) {
            final Block box = part.block();
            assertTrue(Math.hypot(box.x() - middle.x(), box.z() - middle.z()) + Math.hypot(box.halfWidth(), box.halfDepth())
                    < Town.PARK, "within its park");
        }
        // its gate on the town's middle line, before the temple, toward the start
        final Course.Piece gate = temple.getLast();
        assertEquals(middle.x(), gate.block().x(), 1e-6);
        assertTrue(gate.block().z() < middle.z());
        // the park: a lawn round it, a path to its gate, two ponds, each its island and bridge; every building clear of it
        final List<Course.Piece> garden = pieces.stream().filter(p -> p.kind() == Course.Kind.GARDEN).toList();
        assertEquals(Town.PARK, garden.getFirst().block().halfWidth(), 1e-6);
        assertEquals(Course.GARDEN_LAWN, garden.getFirst().tint());
        assertEquals(2, garden.stream().filter(p -> p.tint() == Course.GARDEN_WATER).count(), "two ponds");
        assertTrue(garden.stream().allMatch(p -> p.solid().isEmpty() && p.volumes().isEmpty()), "in no one's way");
        for (int k = 0; k < Town.SITES.size(); k++) {
            assertFalse(garden.getFirst().block().overlaps(pieces.get(k).block()), Town.SITES.get(k).name() + " in the park");
        }
    }

    @Test
    void whatLiesInTheParkLiesOverNothingElseOrWellOverIt() {
        final List<Course.Piece> garden = Town.park().stream().filter(p -> p.kind() == Course.Kind.GARDEN).toList();
        final Course.Piece lawn = garden.getFirst();
        for (int a = 0; a < garden.size(); a++) {
            for (int b = a + 1; b < garden.size(); b++) {
                final Block one = garden.get(a).block();
                final Block other = garden.get(b).block();
                // a rim round its water round its island: rings, drawn one round the next
                final boolean rings = garden.get(a).tint() != Course.GARDEN_PAVING && garden.get(b).tint() != Course.GARDEN_PAVING
                        && Math.hypot(one.x() - other.x(), one.z() - other.z()) < 1;
                // the lawn open where the paving at its middle is, and where each pond is
                final boolean holed = garden.get(a) == lawn && (other.covers(lawn.block().x(), lawn.block().z(), 0)
                        || garden.get(b).tint() != Course.GARDEN_PAVING);
                if (!rings && !holed && one.overlaps(other)) {
                    assertTrue(Math.abs(one.top() - other.top()) >= Town.LAID_APART, a + " over " + b + ": " + one.top()
                            + " and " + other.top());
                }
            }
        }
    }

    @Test
    void thePyramidsAtTheEdgeOfTown() {
        final List<Course.Piece> pyramids = Town.pieces().stream().filter(p -> p.tint() >= Pyramids.KEYS
                && p.tint() < Pavilions.KEYS).toList();
        assertEquals(3, pyramids.size());
        double east = Double.NEGATIVE_INFINITY;
        for (int k = 0; k < Town.SITES.size(); k++) {
            final Block box = Town.piece(k).block();
            east = Math.max(east, box.x() + Math.hypot(box.halfWidth(), box.halfDepth()));
        }
        for (final Course.Piece pyramid : pyramids) {
            final Block box = pyramid.block();
            assertTrue(box.x() - Math.hypot(box.halfWidth(), box.halfDepth()) > east, "east of the town's buildings");
        }
    }

    @Test
    void solidWhereItsPartsAreOpenBetween() {
        // the church: its nave, its roof, its apse, its tower, its spire each shaped as it is, not one box round it all
        final Course.Piece church = Town.piece(0);
        assertTrue(church.volumes().size() >= 6, church.volumes().size() + " parts");
        // a building that does not say how it is shaped: its parts' boxes
        assertTrue(Town.piece(1).solid().size() >= 2, "the apartments, in boxes");
        assertEquals("nave", Town.part("nave, left wall, slab 3"));
        assertEquals("column", Town.part("column 12"));
        assertEquals("roof", Town.part("roof, front slope, slab 40"));
    }

    @Test
    void everyVehicleParkedInTheTemplesCourtyardClearOfEverything() {
        Course.town(Town::pieces, Town::things, Town::people);
        final Play play = Play.start(Demo.TOWN, false);
        final Place yard = Town.temple();
        final List<Lift> vehicles = play.lifts().stream()
                .filter(l -> l.craft() instanceof Course.Chassis || l.craft() == Course.GUNSHIP).toList();
        assertEquals(11, vehicles.size(), "the cars, the truck, the pickup, the six motorcycles and the gunship");
        for (final Lift vehicle : vehicles) {
            final double across = vehicle.place().x() - yard.x();
            final double along = vehicle.place().z() - yard.z();
            assertTrue(Math.abs(across) < TempleYard.FLANK && along > TempleYard.FRONT && along < TempleYard.BACK,
                    vehicle.craft() + " in the courtyard");
            assertTrue(vehicle.clear(vehicle.place(), play.obstacles(), List.of()), vehicle.craft() + " parked clear");
        }
        // clear of the monks, and of each other
        for (int a = 0; a < vehicles.size(); a++) {
            final Block one = play.damage().piece(vehicles.get(a).piece()).block();
            for (final Course.Cast monk : Course.MONKS) {
                final Course.Cast there = monk.within(yard);
                assertFalse(one.covers(there.across(), there.along(), Body.RADIUS + 100), vehicles.get(a).craft() + " on a monk");
            }
            for (int b = a + 1; b < vehicles.size(); b++) {
                assertFalse(one.overlaps(play.damage().piece(vehicles.get(b).piece()).block()), a + " and " + b);
            }
        }
    }

    @Test
    void aGatlingGunOnTopOfEveryTowersEveryCore() {
        Course.town(Town::pieces, Town::things, Town::people);
        Course.guns(Town::guns);
        final Play play = Play.start(Demo.TOWN, false);
        final List<Turret> guns = play.turrets().stream().filter(t -> t.mount() == Course.MOUNTED_GUN).toList();
        int cores = 0;
        for (int k = Town.FIRST_TOWER; k < Town.SITES.size(); k++) {
            cores += Town.tower(k).structure().cores().size();
        }
        assertEquals(cores, guns.size(), "one on each core");
        for (final Turret gun : guns) {
            final Course.Piece tower = play.damage().piece(gun.host());
            assertEquals(Course.Kind.BUILDING, tower.kind());
            assertTrue(tower.tint() >= Town.FIRST_TOWER, "on a tower");
            assertEquals((Town.tower(tower.tint()).height() + TowerBuildings.OVERRUN) * Town.scale(), gun.base().height(), 1e-6,
                    "on top of its core");
            // nothing over it, where it turns and whoever mans it sits
            final Block over = new Block(gun.base().x(), gun.base().z(), 60, 60, 0, gun.base().height() + 5,
                    gun.base().height() + 250);
            assertTrue(tower.volumes().stream().noneMatch(v -> v.overlaps(over)), "clear over it");
        }
        // and none in the playground's buildings, nor on the temple's halls
        assertTrue(Course.guns(TempleYard.pieces().getFirst()).isEmpty());
    }

    @Test
    void theTownDemoAlsoHasTheStallAndTheStandUpBeforeIt() {
        Course.town(Town::pieces, Town::things, Town::people);
        final Play play = Play.start(Demo.TOWN, false);
        final List<Course.Piece> pieces = new Course(Demo.TOWN).pieces();
        assertEquals(Town.pieces(), pieces.subList(0, Town.pieces().size()), "what stands in the town first");
        assertTrue(play.lifts().stream().anyMatch(l -> l.craft().equals(Course.BARGE)), "the barge");
        assertTrue(play.lifts().stream().anyMatch(l -> l.craft().equals(Course.MEETING)), "the meeting platform");
        assertTrue(pieces.stream().anyMatch(p -> p.kind() == Course.Kind.FRUIT), "melons on the stall");
        assertTrue(pieces.stream().anyMatch(p -> p.kind() == Course.Kind.FURNITURE), "the table, the stool, the scooter, the scrum board");
        // the platforms and their stall clear of everything standing in the town
        final List<Course.Piece> standing = standing();
        for (final Course.Piece piece : pieces.subList(Town.pieces().size(), pieces.size())) {
            if (piece.kind() == Course.Kind.FRUIT || piece.kind() == Course.Kind.FURNITURE) {
                continue;
            }
            for (final Course.Piece other : standing) {
                assertFalse(other.block().overlaps(piece.block()), piece.kind() + " on " + other.kind() + " " + other.tint());
            }
        }
    }
}
