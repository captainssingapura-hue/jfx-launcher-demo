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
        assertEquals(Set.of("front", "back", "end", "roof", "underside", "lobby", "tower", "tower top", "ground front",
                "ground back", "ground end"), kinds);
        assertEquals(List.of(Apartments.FACADE, Apartments.GROUND, Apartments.CORNERS, Apartments.TOP),
                List.copyOf(FLATS.choices().keySet()));
        final Surface front = FLATS.surfaces().stream().filter(s -> s.name().equals("front")).findFirst().orElseThrow();
        // the front on the front line of columns, over the upper storeys, its bays the column grid's
        assertEquals(600, front.at(0, 0, 0).along(), 1e-9);
        assertEquals(420, front.at(0, 0, 0).up(), 1e-9);
        assertEquals(6, front.bays());
        assertEquals(4, front.storeys());
        assertEquals(1720, front.at(0, front.height(), 0).up(), 1e-9, "up to the parapet's top");
    }

    @Test
    void eachFacadeWithEachGroundFloorDressesWhatTheyNameAndNothingFarOutsideIt() {
        for (final List<Building.Option> choice : FLATS.choices().values()) {
            assertEquals(choice.size(), choice.stream().map(Building.Option::name).distinct().count(), "each its own name");
        }
        for (final Building.Option facade : FLATS.choices().get(Apartments.FACADE)) {
            for (final Building.Option ground : FLATS.choices().get(Apartments.GROUND)) {
                final Building.Option option = facade.and(ground);
                checkDressed(option, FLATS.dressed(List.of(facade, ground), Palette.CONCRETE));
            }
        }
    }

    private static void checkDressed(final Building.Option option, final List<Solid> solids) {
        for (final Surface surface : FLATS.surfaces()) {
            assertEquals(option.designs().containsKey(surface.kind()),
                    solids.stream().anyMatch(s -> s.name().startsWith(surface.name() + ", ")),
                    option.name() + " dresses " + surface.name() + " if it names its kind");
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

    @Test
    void itsGroundFloorOpenOnItsPilotisOrClosedIn() {
        final Building.Option facade = FLATS.own().getFirst();
        final List<Solid> open = FLATS.dressed(List.of(facade, FLATS.option(Apartments.GROUND, "Open, on pilotis")), Palette.CONCRETE);
        final List<Solid> shops = FLATS.dressed(List.of(facade, FLATS.option(Apartments.GROUND, "Shopfronts")), Palette.CONCRETE);
        final List<Solid> solid = FLATS.dressed(List.of(facade, FLATS.option(Apartments.GROUND, "Solid, high windows")), Palette.CONCRETE);
        assertEquals(FLATS.own().subList(0, 2), List.of(facade, FLATS.option(Apartments.GROUND, "Open, on pilotis")),
                "open, its own");
        // open: nothing round the ground storey but the lobby's glass among the pilotis
        assertTrue(open.stream().noneMatch(s -> s.name().startsWith("ground ")));
        assertTrue(open.stream().anyMatch(s -> s.name().startsWith("lobby, ") && s.material() == Material.GLASS));
        // closed in: all round, the lobby gone in; its entrance in the middle two bays of its front, under canopies
        for (final List<Solid> closed : List.of(shops, solid)) {
            for (final String side : List.of("ground front, ", "ground back, ", "ground left end, ", "ground right end, ")) {
                assertTrue(closed.stream().anyMatch(s -> s.name().startsWith(side)), side);
            }
            assertTrue(closed.stream().noneMatch(s -> s.name().startsWith("lobby, ")));
            assertEquals(List.of("ground front, storey 0, bay 2, entrance, canopy", "ground front, storey 0, bay 3, entrance, canopy"),
                    closed.stream().map(Solid::name).filter(n -> n.endsWith("canopy")).toList());
            assertEquals(21, closed.stream().filter(s -> s.name().startsWith("pilotis")).count(), "its columns still there");
        }
        // shopfronts glazed along it; solid, a band of high windows only
        final double shopGlass = glass(shops, "ground back, ");
        final double highGlass = glass(solid, "ground back, ");
        assertTrue(shopGlass > 2.5 * highGlass, shopGlass + " against " + highGlass);
        // and the facade above the same whatever is below
        assertEquals(open.stream().filter(s -> s.name().startsWith("front, ")).toList(),
                shops.stream().filter(s -> s.name().startsWith("front, ")).toList());
    }

    private static double glass(final List<Solid> solids, final String on) {
        return solids.stream().filter(s -> s.name().startsWith(on) && s.material() == Material.GLASS)
                .mapToDouble(Solid::volume).sum();
    }

    @Test
    void theSameBuildingDressedAnotherWayChangesOnlyItsFacades() {
        final Building.Option balconies = FLATS.option(Apartments.FACADE, "Balconies front, ribbons back");
        final Building.Option fins = FLATS.option(Apartments.FACADE, "Egg-crate fins");
        final List<Solid> a = FLATS.dressed(balconies, Palette.CONCRETE);
        final List<Solid> b = FLATS.dressed(fins, Palette.CONCRETE);
        assertNotEquals(a, b);
        // its ends, roof, lobby and tower the same either way
        for (final String part : List.of("left end, ", "right end, ", "roof, ", "roof tower")) {
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
                new Facade("punched", Role.WALL, 0, Facade.Pattern.all(Infills.punched(2, 130, 12,
                        Role.DRESSING)), List.of(), true));
        final List<Solid> c = FLATS.dressed(mixed, Palette.CONCRETE);
        assertEquals(a.stream().filter(s -> s.name().startsWith("front, ")).toList(),
                c.stream().filter(s -> s.name().startsWith("front, ")).toList(), "its front untouched");
        assertTrue(c.stream().anyMatch(s -> s.name().startsWith("back, ") && s.name().contains("surround")));
    }
}
