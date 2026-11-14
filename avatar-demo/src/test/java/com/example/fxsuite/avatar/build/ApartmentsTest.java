package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Buildings;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The brutalist apartments, composed: one structure and one set of surfaces, dressed many ways;
 * each way dressing every surface it names, nothing far outside the building, the pilotis under it
 * whatever its facade.
 */
class ApartmentsTest {

    private static final Building FLATS = Apartments.building();

    @Test
    void itsSurfacesAreTheBuildingsAndLieOnItsStructure() {
        assertEquals(Buildings.apartments(), FLATS.skeleton());
        final Set<String> kinds = new HashSet<>();
        FLATS.surfaces().forEach(s -> kinds.add(s.kind()));
        assertEquals(Set.of("front", "back", "end", "roof", "underside", "lobby", "tower", "tower top"), kinds);
        final Surface front = FLATS.surfaces().stream().filter(s -> s.name().equals("front")).findFirst().orElseThrow();
        // the front on the front line of columns, over the upper storeys, its bays the column grid's
        assertEquals(600, front.at(0, 0, 0).along(), 1e-9);
        assertEquals(420, front.at(0, 0, 0).up(), 1e-9);
        assertEquals(6, front.bays());
        assertEquals(4, front.storeys());
        assertEquals(1720, front.at(0, front.height(), 0).up(), 1e-9, "up to the parapet's top");
    }

    @Test
    void eachOptionDressesItsWholeAndNothingFarOutsideIt() {
        final Set<String> names = new HashSet<>();
        for (final Building.Option option : FLATS.options()) {
            assertTrue(names.add(option.name()), "each its own name");
            final List<Solid> solids = FLATS.dressed(option);
            for (final Surface surface : FLATS.surfaces()) {
                assertTrue(solids.stream().anyMatch(s -> s.name().startsWith(surface.name() + ", ")),
                        option.name() + " dresses " + surface.name());
            }
            for (final Solid solid : solids) {
                for (final Point p : solid.corners()) {
                    assertTrue(Math.abs(p.across()) <= 1800 + 120 && Math.abs(p.along()) <= 600 + 200
                            && p.up() >= 0 && p.up() <= 1970 + 10, option.name() + ": " + solid.name() + " at " + p);
                }
            }
            assertTrue(solids.stream().anyMatch(s -> s.material() == Material.GLASS), option.name() + " glazed");
            // the open ground storey's columns, whatever the facade: three lines of seven
            assertEquals(21, solids.stream().filter(s -> s.name().startsWith("pilotis")).count());
        }
    }

    @Test
    void theSameBuildingDressedAnotherWayChangesOnlyItsFacades() {
        final Building.Option balconies = FLATS.option("Balconies front, ribbons back");
        final Building.Option fins = FLATS.option("Egg-crate fins");
        final List<Solid> a = FLATS.dressed(balconies);
        final List<Solid> b = FLATS.dressed(fins);
        assertNotEquals(a, b);
        // its ends, roof, lobby and tower the same either way
        for (final String part : List.of("left end, ", "right end, ", "roof, ", "lobby, ", "roof tower")) {
            assertEquals(a.stream().filter(s -> s.name().startsWith(part)).toList(),
                    b.stream().filter(s -> s.name().startsWith(part)).toList(), part);
        }
        // balconies: one in every other bay of the four storeys of the front, solid fronted
        assertEquals(12, a.stream().filter(s -> s.name().startsWith("front, ") && s.name().endsWith("balcony, slab"))
                .count());
        assertEquals(0, b.stream().filter(s -> s.name().contains("balcony")).count());
        // fins: one at each line of columns and two between, on each long side
        assertEquals(2 * (6 * 3 + 1), b.stream().filter(s -> s.name().contains(", fin ")).count());
        // and an option changed one surface at a time
        final Building.Option mixed = balconies.with("balconies, punched back", "back",
                new Facade("punched", Material.PRECAST, 40, Facade.Pattern.all(Infills.punched(2, 130, 12,
                        Material.CONCRETE)), List.of(), true));
        final List<Solid> c = FLATS.dressed(mixed);
        assertEquals(a.stream().filter(s -> s.name().startsWith("front, ")).toList(),
                c.stream().filter(s -> s.name().startsWith("front, ")).toList(), "its front untouched");
        assertTrue(c.stream().anyMatch(s -> s.name().startsWith("back, ") && s.material() == Material.PRECAST));
    }
}
