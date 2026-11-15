package com.example.fxsuite.avatar.build;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Corner stones and a cornice, each a choice of its own, added to the walls whatever their facade:
 * the stones long and short by turns, meeting long to short round each corner, kept off the windows;
 * the cornice stepping out, meeting round the corners; both in the palette's dressings.
 */
class DressingsTest {

    private static final Building FLATS = Apartments.building();

    /** Dressed with {@code facade}, a solid ground floor, and {@code corners} and {@code top}, in {@code palette}. */
    private static List<Solid> dressed(final String facade, final String corners, final String top, final Palette palette) {
        return FLATS.dressed(List.of(FLATS.option(Apartments.FACADE, facade),
                FLATS.option(Apartments.GROUND, "Solid, high windows"), FLATS.option(Apartments.CORNERS, corners),
                FLATS.option(Apartments.TOP, top)), palette);
    }

    private static double extent(final Solid solid, final java.util.function.ToDoubleFunction<Point> axis) {
        return solid.corners().stream().mapToDouble(axis).max().orElseThrow()
                - solid.corners().stream().mapToDouble(axis).min().orElseThrow();
    }

    private static Solid named(final List<Solid> solids, final String name) {
        return solids.stream().filter(s -> s.name().equals(name)).findFirst().orElseThrow(() -> new AssertionError(name));
    }

    @Test
    void theyAreChoicesOfTheirOwnThePlainOnesAddingNothing() {
        assertEquals(List.of(Apartments.FACADE, Apartments.GROUND, Apartments.CORNERS, Apartments.TOP),
                List.copyOf(FLATS.choices().keySet()));
        final List<Solid> plain = dressed("Punched windows", "Plain", "Coping", Palette.BRICK);
        assertTrue(plain.stream().noneMatch(s -> s.name().contains("corner stone") || s.name().contains("cornice")));
        // added, they add to what the facade made, and change nothing of it
        final List<Solid> dressedUp = dressed("Punched windows", "Corner stones", "Cornice", Palette.BRICK);
        final List<Solid> rest = new ArrayList<>(dressedUp);
        rest.removeIf(s -> s.name().contains("corner stone") || s.name().contains("cornice"));
        assertEquals(plain, rest);
    }

    @Test
    void cornerStonesLongAndShortByTurnsMeetLongToShortRoundEachCorner() {
        final Palette palette = Palette.BRICK;
        final List<Solid> solids = dressed("Punched windows", "Corner stones", "Coping", palette);
        final double reach = 1800 + palette.wall() / 2 + 3;
        // the front's left-hand corner (seen from outside, at across +1800) is the right end's right-hand one
        int courses = 0;
        for (int c = 0; ; c++) {
            final String front = "front, corner stone, left, course " + c;
            if (solids.stream().noneMatch(s -> s.name().equals(front))) {
                break;
            }
            courses++;
            final Solid a = named(solids, front);
            final Solid b = named(solids, "right end, corner stone, right, course " + c);
            final double along = extent(a, Point::across);
            final double round = extent(b, Point::along);
            assertTrue((along > round) == (c % 2 == 0), "course " + c + ": " + along + " and " + round);
            assertEquals(60 + 3, Math.max(along, round), 1e-9, "a long stone, 60 along its wall and 3 round the corner");
            // both wrapping the corner to the same edge
            assertEquals(reach, a.corners().stream().mapToDouble(Point::across).max().orElseThrow(), 1e-9);
            assertEquals(reach, b.corners().stream().mapToDouble(Point::across).max().orElseThrow(), 1e-9);
            assertEquals(Material.STONE, a.material(), "the palette's dressings");
        }
        assertEquals(43, courses, "30 cm courses up the 13 m wall, to its top");
    }

    @Test
    void cornerStonesKeepOffTheWindows() {
        final List<Solid> ribbons = dressed("Ribbon windows", "Corner stones", "Coping", Palette.CONCRETE);
        final List<Solid> punched = dressed("Punched windows", "Corner stones", "Coping", Palette.CONCRETE);
        final long ribbonStones = ribbons.stream().filter(s -> s.name().startsWith("front, corner stone")).count();
        final long punchedStones = punched.stream().filter(s -> s.name().startsWith("front, corner stone")).count();
        assertTrue(ribbonStones < punchedStones, ribbonStones + " against " + punchedStones);
        // and no stone over the front's glass
        final Surface front = FLATS.surfaces().stream().filter(s -> s.name().equals("front")).findFirst().orElseThrow();
        final List<Facade.Rect> glass = Dressings.openings(front, ribbons);
        for (final Solid stone : ribbons.stream().filter(s -> s.name().startsWith("front, corner stone")).toList()) {
            final Facade.Rect at = Dressings.openings(front, List.of(new Solid(front.name() + ", probe", Role.GLAZING,
                    Material.GLASS, stone.corners()))).getFirst();
            for (final Facade.Rect window : glass) {
                assertFalse(at.u0() + 6 < window.u1() - 6 && window.u0() + 6 < at.u1() - 6
                        && at.v0() + 6 < window.v1() - 6 && window.v0() + 6 < at.v1() - 6, stone.name());
            }
        }
    }

    @Test
    void aCorniceStepsOutAlongTheTopAndMeetsRoundTheCorners() {
        final Palette palette = Palette.STONE;
        final List<Solid> solids = dressed("Punched windows", "Plain", "Cornice", palette);
        final List<Solid> cornice = solids.stream().filter(s -> s.name().contains(", cornice, ")).toList();
        assertEquals(4 * 4, cornice.size(), "four steps on each of four walls");
        assertTrue(cornice.stream().allMatch(s -> s.material() == Material.STONE && s.role() == Role.DRESSING));
        final Solid corona = named(solids, "front, cornice, corona");
        final double out = palette.wall() / 2 + 34;
        assertEquals(1800 + out, corona.corners().stream().mapToDouble(Point::across).max().orElseThrow(), 1e-9,
                "round the corner as far as it stands out");
        assertEquals(600 + out, corona.corners().stream().mapToDouble(Point::along).max().orElseThrow(), 1e-9);
        // over the coping, and the steps further out the higher, to the corona
        final Solid bed = named(solids, "front, cornice, bed");
        assertTrue(bed.corners().stream().mapToDouble(Point::along).max().orElseThrow()
                < corona.corners().stream().mapToDouble(Point::along).max().orElseThrow());
        final double coping = named(solids, "front, coping").corners().stream().mapToDouble(Point::up).max().orElseThrow();
        assertTrue(corona.corners().stream().mapToDouble(Point::up).max().orElseThrow() > coping);
    }
}
