package com.example.fxsuite.avatar.air;

import com.example.fxsuite.avatar.build.Point;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Aircraft, made of two kinds of body (a fuselage lofted through its sections, a box with its edges
 * rounded) and the solid parts fixed to them: each body closed, as big as it says, its surface where
 * its skin is; parts mounted where and as they are turned; every aircraft made to any size it may be,
 * whole, finite, and the same either side.
 */
class AircraftTest {

    private static void near(final Point expected, final Point actual, final double within, final String what) {
        assertEquals(expected.across(), actual.across(), within, what + ": across");
        assertEquals(expected.along(), actual.along(), within, what + ": along");
        assertEquals(expected.up(), actual.up(), within, what + ": up");
    }

    @Test
    void aMountRollsThenPitchesThenYawsThenMoves() {
        final Point ahead = new Point(0, 1, 0);
        near(new Point(1, 0, 0), new Mount(Point.ZERO, 90, 0, 0).place(ahead), 1e-9, "yawed right, its nose to the right");
        near(new Point(0, 0, 1), new Mount(Point.ZERO, 0, 90, 0).place(ahead), 1e-9, "pitched up, its nose up");
        near(new Point(0, 0, 1), new Mount(Point.ZERO, 0, 0, -90).place(new Point(1, 0, 0)), 1e-9,
                "rolled -90, its right side up: a fin's span");
        near(new Point(0, 1, 0), new Mount(Point.ZERO, 0, -90, 0).place(new Point(0, 0, 1)), 1e-9,
                "pitched -90, its up ahead: a propeller's axis");
        near(new Point(5, 7, 9), Mount.at(5, 7, 9).place(Point.ZERO), 1e-9, "moved");
    }

    @Test
    void aFuselageIsClosedAtBothEndsAsBigAsItSaysAndTheSameEitherSide() {
        final Fuselage envelope = Fuselage.envelope("envelope", Paint.SKIN, 2400, 520);
        final Skin skin = envelope.skin();
        assertTrue(Skin.apex(skin.rings().getFirst()), "its tail to a point");
        assertTrue(Skin.apex(skin.rings().getLast()), "its nose to a point");
        final Extent e = envelope.extent();
        assertEquals(2400, e.length(), 1e-6);
        assertEquals(520, e.width(), 1);
        assertEquals(520, e.height(), 1);
        assertEquals(-e.minAcross(), e.maxAcross(), 1e-6, "as wide either side");
        // its surface where its skin is: its middle's section a circle 260 round
        near(new Point(260, 0, 0), envelope.surface(0, 0), 1e-6, "its right side");
        near(new Point(0, 0, 260), envelope.surface(0, 90), 1e-6, "its top");
        near(new Point(0, 0, -260), envelope.surface(0, 270), 1e-6, "its belly");
        // every one of the shapes it is made to, closed and whole
        for (final Fuselage f : List.of(Fuselage.aircraft("a", Paint.SKIN, 800, 120),
                Fuselage.pod("p", Paint.SKIN, 1200, 240, 250), Fuselage.nacelle("n", Paint.METAL, 200, 60),
                Fuselage.spinner("s", Paint.TRIM, 40, 50))) {
            assertTrue(Skin.apex(f.skin().rings().getFirst()) && Skin.apex(f.skin().rings().getLast()), f.name());
            assertTrue(f.skin().points().stream().allMatch(p -> Double.isFinite(p.across() + p.along() + p.up())), f.name());
        }
        assertThrows(IllegalArgumentException.class, () -> new Fuselage("x", Paint.SKIN,
                List.of(Fuselage.Station.round(10, 5), Fuselage.Station.round(0, 5)), 12), "its sections tail to nose");
    }

    @Test
    void aBoxBodyIsAsBigAsItSaysItsEdgesRoundedItsFacesFlat() {
        final BoxBody box = BoxBody.of("box", Paint.SKIN, 400, 200, 100, 20);
        final Skin skin = box.skin();
        assertTrue(Skin.apex(skin.rings().getFirst()) && Skin.apex(skin.rings().getLast()), "closed at both ends");
        final Extent e = box.extent();
        assertEquals(400, e.length(), 1e-6);
        assertEquals(200, e.width(), 1e-6);
        assertEquals(100, e.height(), 1e-6);
        // its faces flat, out to where its edges round
        near(new Point(100, 0, 0), box.surface(0, 0), 1e-6, "its right face");
        near(new Point(0, 0, 50), box.surface(0, 90), 1e-6, "its top");
        near(new Point(100, 0, 30), box.surface(0, Math.toDegrees(Math.atan2(30, 100))), 1e-6, "flat as far as its rounding");
        // and round its corner (straight out through the corner's middle): as far from that middle as it is rounded
        final Point corner = box.surface(0, Math.toDegrees(Math.atan2(30, 80)));
        assertEquals(20, Math.hypot(corner.across() - 80, corner.up() - 30), 1e-6, "on its rounded edge");
        // over its rounded ends, smaller as the rounding goes
        assertTrue(box.surface(195, 0).across() < 100 && box.surface(195, 0).across() > 80, "its end's rounded edge");
        // drawn in toward its nose
        final BoxBody tapered = new BoxBody("t", Paint.SKIN, 400, 200, 100, 10, 0.4, 0);
        // 190 out of 200 to its nose: 1 - 0.4 * 190 / 200 of its size, 62
        assertEquals(62, tapered.surface(190, 0).across(), 1e-6, "its nose 0.4 smaller, drawn in straight to it");
        assertThrows(IllegalArgumentException.class, () -> BoxBody.of("x", Paint.SKIN, 400, 200, 100, 60),
                "rounded more than half its depth");
    }

    @Test
    void aWingSweepsBackAndRisesToItsTipAndAPatchLiesOnTheSkin() {
        final List<Piece> wing = Parts.wing("w", Paint.WING, 500, 200, 100, 30, 10, 0.12);
        final Extent e = Extent.of(wing.stream().flatMap(p -> p.corners().stream()).toList());
        assertEquals(500, e.maxAcross(), 1e-6, "its span");
        assertEquals(0, e.maxAlong(), 1e-6, "its leading edge at the root ahead of all of it");
        assertEquals(-500 * Math.tan(Math.toRadians(30)) - 100, e.minAlong(), 1e-6, "its tip's trailing edge, swept back");
        assertTrue(e.maxUp() > 500 * Math.tan(Math.toRadians(10)), "risen to its tip");
        // a window on a box: just under the skin to its proud face
        final BoxBody box = BoxBody.of("box", Paint.SKIN, 400, 200, 100, 20);
        for (final Piece piece : Parts.patch("window", Paint.GLASS, box, -50, 50, -10, 10, 2, 2, 2)) {
            for (final Point p : piece.corners()) {
                assertTrue(p.across() > 99.4 && p.across() < 102 + 1e-6, "on its right face: " + p);
            }
        }
    }

    @Test
    void aHingeTurnsItsPartAboutItsAxisAndItsMirrorTurnsAlike() {
        // a part a metre out to the right, hinged about the along axis through the origin
        final Craft part = Craft.named("part").part(Piece.box("p", Paint.METAL, new Point(100, 0, 0), 10, 10, 10)).build();
        final Hinge hinge = part.hinged("tilt", Point.ZERO, new Point(0, 1, 0), -90, 90).hinges().getFirst();
        // the right-hand way about +along: the right side goes down, as a roll to the right does
        near(new Point(0, 0, -100), hinge.turned(new Point(100, 0, 0), 90), 1e-9, "turned a quarter");
        near(new Point(100, 0, 0), hinge.turned(new Point(100, 0, 0), 0), 1e-9, "at rest");
        // its mirror image, on the left, turns its side down too for the same control: either side alike
        final Hinge mirror = hinge.mirrored();
        near(new Point(0, 0, -100), mirror.turned(new Point(-100, 0, 0), 90), 1e-9, "the left side down, alike");
        // mounted: its pivot put, its axis turned with it
        final Hinge mounted = hinge.placed(new Mount(new Point(0, 0, 500), 90, 0, 0));
        near(new Point(0, 0, 500), mounted.pivot(), 1e-9, "its pivot put");
        near(new Point(1, 0, 0), mounted.axis(), 1e-9, "its axis yawed");
        // a turret: hinges on hinges, each its control and its range
        final java.util.Map<String, double[]> controls = Hinge.controls(Weapons.chinTurret(100));
        assertEquals(List.of("turret traverse", "turret elevation"), List.copyOf(controls.keySet()));
        assertEquals(-60, controls.get("turret elevation")[0], 1e-9);
        assertEquals(120, controls.get("turret traverse")[1], 1e-9);
    }

    @Test
    void facetedAndChamferedBodiesKeepTheirSizeTheirFacesFlat() {
        final Fuselage faceted = Fuselage.aircraft("f", Paint.SKIN, 800, 120).faceted(8);
        assertTrue(faceted.skin().faceted(), "drawn flat");
        assertEquals(8, faceted.skin().rings().getFirst().size(), "eight sides round");
        final BoxBody box = BoxBody.of("box", Paint.SKIN, 400, 200, 100, 20).withChamfers();
        assertTrue(box.skin().faceted());
        final Extent e = box.extent();
        assertEquals(400, e.length(), 1e-6);
        assertEquals(200, e.width(), 1e-6);
        assertEquals(100, e.height(), 1e-6);
        // its corner cut straight, from (100, 30) to (80, 50): out through the cut's middle, a point on it
        final Point cut = box.surface(0, Math.toDegrees(Math.atan2(40, 90)));
        assertEquals(130, cut.across() + cut.up(), 1e-6, "on the cut across the corner");
        near(new Point(100, 0, 0), box.surface(0, 0), 1e-6, "its right face, as it was");
    }

    @Test
    void aCoaxialDuctTurnsItsTwoRotorsOppositeWays() {
        final Craft duct = Engine.coaxialDuct(300).build();
        assertEquals(2, duct.spinners().size());
        assertEquals(-duct.spinners().get(0).rate(), duct.spinners().get(1).rate(), 1e-9, "opposite ways");
        // both about its axis, ahead
        duct.spinners().forEach(s -> near(new Point(0, 1, 0), s.axis(), 1e-9, s.name()));
    }

    @Test
    void everyAircraftAtAnySizeIsWholeFiniteAndTheSameEitherSide() {
        for (final Crafts.Kind kind : Crafts.Kind.values()) {
            for (final double size : new double[] {kind.least(), kind.usual(), kind.most()}) {
                final Craft craft = kind.build(size);
                final String what = kind.title() + " " + size + " m";
                assertFalse(craft.points().isEmpty(), what + ": something to it (a turret's all on its hinges)");
                assertTrue(craft.points().stream().allMatch(p -> Double.isFinite(p.across() + p.along() + p.up())), what);
                final Extent e = craft.extent();
                // the same either side, but for what turns: a three-bladed rotor is not, at any one moment
                final java.util.List<Point> fixed = new java.util.ArrayList<>();
                craft.skins().forEach(s -> fixed.addAll(s.points()));
                craft.pieces().forEach(p -> fixed.addAll(p.corners()));
                final Extent still = Extent.of(fixed);
                assertEquals(-still.minAcross(), still.maxAcross(), 1e-6 * size * 100, what + ": the same either side");
                // as long as it was made, give or take what stands out of it (a drone, as wide, its rotors in it; a
                // helicopter, its rotors out of it)
                // an aircraft or an engine as long (or as wide) as it was made; a weapon or a body, any size it is
                if (kind.category() == Crafts.Category.AIRCRAFT || kind.engine()) {
                    final double made = kind == Crafts.Kind.QUADCOPTER || kind == Crafts.Kind.DUCTED_FAN
                            || kind == Crafts.Kind.COAXIAL_DUCT ? e.width() : still.length();
                    final double within = kind == Crafts.Kind.COAXIAL_DUCT ? 0.3 : kind.engine() ? 0.4 : 0.25;
                    assertEquals(size * 100, made, size * 100 * within, what + ": its size");
                }
                for (final Spinner spinner : craft.spinners()) {
                    assertEquals(1, spinner.axis().length(), 1e-9, what + ": " + spinner.name() + "'s axis a unit");
                }
            }
            assertThrows(IllegalArgumentException.class, () -> kind.build(kind.most() * 2), kind.title() + ": too big");
        }
    }
}
