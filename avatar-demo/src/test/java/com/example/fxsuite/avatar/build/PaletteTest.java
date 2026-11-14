package com.example.fxsuite.avatar.build;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * One building, one way of dressing it, built in each palette: each part of what its role is made
 * of in it; its walls as thick as the palette says; a stone base rusticated; and what is doubtful
 * in it said: an opening wider than its wall's material spans.
 */
class PaletteTest {

    private static final Building FLATS = Apartments.building();
    private static final List<Building.Option> PUNCHED_ON_SOLID = List.of(
            FLATS.option(Apartments.FACADE, "Punched windows"), FLATS.option(Apartments.GROUND, "Solid, high windows"));

    @Test
    void eachPartIsMadeOfWhatItsRoleIsInThePalette() {
        for (final Palette palette : Palette.ALL) {
            for (final Solid solid : FLATS.dressed(PUNCHED_ON_SOLID, palette)) {
                assertEquals(palette.of(solid.role()), solid.material(), palette.name() + ": " + solid.name());
            }
        }
        final List<Solid> brick = FLATS.dressed(PUNCHED_ON_SOLID, Palette.BRICK);
        assertTrue(brick.stream().anyMatch(s -> s.name().startsWith("front, ") && s.material() == Material.BRICK));
        assertTrue(brick.stream().anyMatch(s -> s.name().startsWith("ground front, ") && s.material() == Material.STONE),
                "its base stone");
        assertTrue(brick.stream().anyMatch(s -> s.name().contains("surround") && s.material() == Material.STONE),
                "its dressings stone");
        assertTrue(brick.stream().filter(s -> s.name().startsWith("pilotis")).allMatch(s -> s.material() == Material.CONCRETE),
                "its structure concrete, whatever its walls");
        assertTrue(brick.stream().anyMatch(s -> s.material() == Material.GLASS));
        assertThrows(IllegalArgumentException.class, () -> new Palette("half", Map.of(Role.WALL, Material.BRICK), 30, 30, 0),
                "a palette says what every role is made of");
    }

    /** How thick the front's walls are, built in {@code palette}: how far its wall pieces reach along, in and out. */
    private static double frontWall(final Palette palette, final String surface) {
        final Solid piece = FLATS.dressed(PUNCHED_ON_SOLID, palette).stream()
                .filter(s -> s.name().startsWith(surface + ", storey 0, bay 0, wall ") && !s.name().contains("course"))
                .findFirst().orElseThrow();
        final double low = piece.corners().stream().mapToDouble(Point::along).min().orElseThrow();
        final double high = piece.corners().stream().mapToDouble(Point::along).max().orElseThrow();
        return high - low;
    }

    @Test
    void itsWallsAreAsThickAsThePaletteSaysTheBaseThickerInMasonry() {
        assertEquals(30, frontWall(Palette.CONCRETE, "front"), 1e-9);
        assertEquals(35, frontWall(Palette.BRICK, "front"), 1e-9);
        assertEquals(50, frontWall(Palette.STONE, "front"), 1e-9);
        assertEquals(50, frontWall(Palette.BRICK, "ground front"), 1e-9, "a plinth under the brick");
        assertEquals(60, frontWall(Palette.STONE, "ground front"), 1e-9);
        // the punched windows' glass set the deeper in the thicker wall
        assertTrue(glassDepth(Palette.STONE) > glassDepth(Palette.CONCRETE));
    }

    /** How far the front's first punched window's glass is set back from its wall's face. */
    private static double glassDepth(final Palette palette) {
        final Solid glass = FLATS.dressed(PUNCHED_ON_SOLID, palette).stream()
                .filter(s -> s.name().equals("front, storey 0, bay 0, window 0, glass")).findFirst().orElseThrow();
        final double face = 600 + palette.wall() / 2;
        return face - glass.corners().stream().mapToDouble(Point::along).max().orElseThrow();
    }

    @Test
    void aStoneBaseIsRusticatedInCoursesStandingProudConcreteIsNot() {
        final List<Solid> concrete = FLATS.dressed(PUNCHED_ON_SOLID, Palette.CONCRETE);
        final List<Solid> brick = FLATS.dressed(PUNCHED_ON_SOLID, Palette.BRICK);
        assertTrue(concrete.stream().noneMatch(s -> s.name().contains(", course ")));
        final List<Solid> courses = brick.stream().filter(s -> s.name().contains(", course ")).toList();
        assertTrue(courses.size() > 50, courses.size() + " courses");
        assertTrue(courses.stream().allMatch(s -> s.role() == Role.BASE && s.name().startsWith("ground ")),
                "its base only, not its brick walls");
        // each course two centimetres proud of the base's face, at most 40 high
        for (final Solid course : courses) {
            final double up = course.corners().stream().mapToDouble(Point::up).max().orElseThrow()
                    - course.corners().stream().mapToDouble(Point::up).min().orElseThrow();
            assertTrue(up <= 40 + 1e-9, course.name());
        }
        final Solid first = courses.stream().filter(s -> s.name().startsWith("ground front, ")).findFirst().orElseThrow();
        assertEquals(600 + 25 + 2, first.corners().stream().mapToDouble(Point::along).max().orElseThrow(), 1e-9);
    }

    @Test
    void whatIsDoubtfulIsSaidAnOpeningWiderThanItsWallsMaterialSpans() {
        // concrete spans anything: nothing doubtful, however it is dressed
        for (final Building.Option facade : FLATS.choices().get(Apartments.FACADE)) {
            for (final Building.Option ground : FLATS.choices().get(Apartments.GROUND)) {
                assertEquals(List.of(), FLATS.doubts(List.of(facade, ground), Palette.CONCRETE));
            }
        }
        // punched windows in brick on a stone base with pairs of high windows: nothing doubtful
        final List<String> punched = FLATS.doubts(PUNCHED_ON_SOLID, Palette.BRICK);
        assertEquals(List.of(), punched);
        // ribbon windows in brick: 5.6 m openings, more than brick spans
        final List<String> ribbons = FLATS.doubts(List.of(FLATS.option(Apartments.FACADE, "Ribbon windows"),
                FLATS.option(Apartments.GROUND, "Open, on pilotis")), Palette.BRICK);
        assertEquals(List.of("front: ribbon windows in brick, openings 5.6 m wide; brick spans 3.0 m unsupported",
                "back: ribbon windows in brick, openings 5.6 m wide; brick spans 3.0 m unsupported"), ribbons);
        // and shopfronts in a stone base
        assertTrue(FLATS.doubts(List.of(FLATS.option(Apartments.FACADE, "Punched windows"),
                FLATS.option(Apartments.GROUND, "Shopfronts")), Palette.STONE).stream()
                .anyMatch(d -> d.startsWith("ground front: shopfronts in stone")));
    }
}
