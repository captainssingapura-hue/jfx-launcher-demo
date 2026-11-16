package com.example.fxsuite.avatar.air;

import com.example.fxsuite.avatar.build.Point;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
    void linesGiveASkeletonAVertexWhereEachLongeronCrossesEachSectionAndAPanelBetweenEachTwo() {
        final Lines lines = Gunship.fuselage(1600);
        final List<Lines.Section> sections = lines.refined();
        // its sections given, and two between each two
        final int count = lines.sections().size();
        assertEquals(count + (count - 1) * 2, sections.size());
        final int round = 2 * Gunship.LONGERONS.size() - 2;
        final com.example.fxsuite.avatar.sim.Form skeleton = lines.skeleton();
        // a vertex round each section but its two ends, each drawn to one point
        assertEquals((sections.size() - 2) * round + 2, skeleton.vertices().size());
        assertTrue(skeleton.problems().isEmpty(), skeleton.problems().toString());
        assertEquals(1, skeleton.pieces(), "all one piece");
        // a panel between each two longerons and each two sections
        final List<Panel> panels = lines.panels((band, along) -> Paint.SKIN);
        assertEquals((sections.size() - 1) * round, panels.size());
        // the same either side
        final Extent e = lines.extent();
        assertEquals(-e.minAcross(), e.maxAcross(), 1e-6);
        // and where a longeron crosses a given section, where it was given
        final Lines.Section given = lines.sections().get(5);
        near(new Point(given.points().get(2)[0], given.along(), given.points().get(2)[1]), lines.on("side", given.along()),
                1e-6, "its widest line, where it was given");
        // its canopy glazed over its cockpit, and not ahead of it
        final Craft body = Gunship.body(1600);
        assertTrue(body.panels().stream().anyMatch(p -> p.paint() == Paint.GLASS), "a glazed canopy");
        assertTrue(body.panels().stream().filter(p -> p.paint() == Paint.GLASS)
                .allMatch(p -> p.corners().stream().allMatch(c -> c.along() > 0.15 * 1600 && c.along() < 0.5 * 1600)),
                "over its cockpit only");
    }

    @Test
    void aBodysStyleWidensItSwellsItsHaunchAndDrawsInItsCabinItsEndsStillClosed() {
        final double l = 1600;
        final Lines plain = Gunship.fuselage(l, BodyStyle.PLAIN);
        final Lines wide = Gunship.fuselage(l, new BodyStyle(1.2, 1, 0, 0.56, 0, 0, 0));
        assertEquals(plain.extent().width() * 1.2, wide.extent().width(), 1e-6, "a fifth wider");
        assertEquals(plain.extent().height(), wide.extent().height(), 1e-6, "no taller");
        // its haunch: its beltline out where it is, not at its nose
        final Lines hipped = Gunship.fuselage(l, new BodyStyle(1, 1, 0.03, 0.76, 0, 0, 0));
        // (at a section it is drawn through: its bay, straight-walled, is not swelled between its walls)
        final double at = (0.76 - 0.5) * l;
        assertEquals(plain.on("side", at).across() + 0.03 * l, hipped.on("side", at).across(), 1e-6, "swelled there");
        final double nose = (0.955 - 0.5) * l;
        assertEquals(plain.on("side", nose).across(), hipped.on("side", nose).across(), 1e-6, "not at its nose");
        // its cabin drawn in over its beltline: its rail in by the share, its beltline as it was
        final Lines drawnIn = Gunship.fuselage(l, new BodyStyle(1, 1, 0, 0.56, 0.3, 0, 0));
        assertEquals(plain.on("rail", at).across() * 0.7, drawnIn.on("rail", at).across(), 1e-6);
        assertEquals(plain.on("side", at).across(), drawnIn.on("side", at).across(), 1e-6);
        // and its ends, however styled, still drawn to a point
        final Lines styled = Gunship.fuselage(l, new BodyStyle(1.3, 1.2, 0.04, 0.4, 0.4, 0.02, 0.5));
        assertTrue(styled.sections().getFirst().point() && styled.sections().getLast().point());
        assertTrue(styled.skeleton().problems().isEmpty());
    }

    @Test
    void aGunshipsBodyIsTheSameHoweverItIsLiftedEachLiftOnItsHardpoints() {
        final double l = 1600;
        final Gunship.Hardpoints at = Gunship.hardpoints(l);
        final Extent body = Gunship.body(l).extent();
        // its bay over its centre of gravity: along it either side of it, its floor above it, open above
        final Gunship.Bay bay = at.bay();
        final double centre = (Gunship.CENTRE_AT - 0.5) * l;
        assertTrue(bay.floor().along() - bay.length() / 2 < centre && centre < bay.floor().along() + bay.length() / 2,
                "its bay either side of its centre of gravity");
        assertEquals(0, bay.floor().across(), 1e-9);
        assertTrue(bay.floor().up() > 0, "its floor above its centre of gravity");
        assertEquals(0.196 * l, bay.length(), 0.01 * l);
        assertTrue(bay.depth() > 0.07 * l && bay.depth() < 0.1 * l, "as deep as its cabin's roof: " + bay.depth() / l);
        assertTrue(bay.width() > 0.05 * l && bay.width() < body.width(), "across its back");
        assertTrue(bay.floor().up() + bay.depth() < body.maxUp() + 1, "nothing over its top line");
        assertTrue(at.tail().at().along() < -0.4 * l && at.nose().at().along() > 0.3 * l, "tail behind, nose ahead");
        assertTrue(at.ground() < body.minUp() + 1, "its wheels on the ground");
        // each lift the same body, and its own: its body's every point among the lifted one's
        final java.util.Set<Point> bare = new java.util.HashSet<>(Gunship.body(l).points());
        for (final Gunship.Lift lift : Gunship.Lift.values()) {
            if (lift == Gunship.Lift.NONE) {
                continue;
            }
            final Craft lifted = Gunship.of(l, lift);
            assertTrue(new java.util.HashSet<>(lifted.points()).containsAll(bare), lift + ": the same body");
            assertTrue(lifted.points().size() > bare.size(), lift + ": and its lift");
        }
        // the ducts tilt together, from hovering to flying
        final java.util.Map<String, double[]> controls = Hinge.controls(Gunship.of(l, Gunship.Lift.TWIN_PROPELLERS));
        assertEquals(List.of("tilt"), List.copyOf(controls.keySet()));
        assertEquals(0, controls.get("tilt")[0], 1e-9);
        assertEquals(90, controls.get("tilt")[1], 1e-9);
    }

    @Test
    void theBombersHullIsAFrameOfGirdersSixteenSidedItsBellyFlatterThanItsBack() {
        final double l = 7000;
        final Lines hull = Bomber.hull(l);
        assertEquals(Bomber.FRAMES.length, hull.sections().size(), "a section at each main frame");
        final int round = 2 * Bomber.LONGERONS.size() - 2;
        assertEquals(16, round);
        final com.example.fxsuite.avatar.sim.Form skeleton = hull.skeleton();
        assertTrue(skeleton.problems().isEmpty(), skeleton.problems().toString());
        assertEquals(1, skeleton.pieces(), "all one frame");
        // a vertex where each girder crosses each section, and round each main frame, between its girders
        assertEquals((hull.refined().size() - 2) * round + (Bomber.FRAMES.length - 2) * round * Bomber.AROUND + 2,
                skeleton.vertices().size(), "drawn to a point at each end");
        final Extent e = hull.extent();
        assertEquals(l, e.length(), 1e-6);
        assertEquals(Bomber.DIAMETER * l, e.width(), 0.01 * l, "as wide as it is round");
        assertEquals(e.maxUp() * Bomber.BELLY, -e.minUp(), 0.01 * l, "its belly flatter than its back");
        // its widest girder level with its axis, all along its middle
        assertEquals(Bomber.DIAMETER * l / 2, hull.on("beam", 0).across(), 1e-6);
        assertEquals(0, hull.on("beam", 0).up(), 1e-6);
        assertEquals(hull.on("beam", 0).across(), hull.on("beam", 0.15 * l).across(), 1, "a parallel middle");
        // its frames round: above its beam, every point round a frame as far out as its girders, part of a circle
        final Lines.Section frame = hull.sections().get(10);
        final List<Point> ring = hull.ring(frame);
        assertEquals(round * (Bomber.AROUND + 1), ring.size());
        final double out = Bomber.round(Bomber.FRAMES[10]) * Bomber.DIAMETER * l / 2;
        for (final Point p : ring) {
            if (p.up() >= 0) {
                assertEquals(out, Math.hypot(p.across(), p.up()), 1e-6, "on the circle: " + p);
            } else {
                assertTrue(Math.hypot(p.across(), p.up()) <= out + 1e-6, "its belly flatter: " + p);
            }
        }
        // its skin a strip between each two points round
        assertEquals((hull.refined().size() - 1) * round * (Bomber.AROUND + 1),
                hull.panels((band, along) -> Paint.SKIN).size());
        // its tail tapering to its hub, slim, and closing round behind
        assertEquals(Bomber.HUB * Bomber.DIAMETER * l / 2, hull.on("beam", (Bomber.HUB_ENDS - 0.5) * l).across(), 1e-6);
    }

    @Test
    void theBombersShroudRingsItsHubBehindItsTaperHeldByFourVanes() {
        final double l = 7000;
        final Lines outside = Bomber.shroud(l, true);
        final Lines inside = Bomber.shroud(l, false);
        // closed: its two faces meet at its leading and trailing edges
        for (final int end : new int[] {0, outside.sections().size() - 1}) {
            for (int i = 0; i < Bomber.LONGERONS.size(); i++) {
                assertArrayEquals(outside.sections().get(end).points().get(i), inside.sections().get(end).points().get(i),
                        1e-6);
            }
        }
        // big and shallow: most of the hull's width across, a third of that deep
        final Extent ring = outside.extent();
        assertTrue(ring.width() > 0.8 * Bomber.DIAMETER * l, "big: " + ring.width());
        assertTrue(ring.length() < 0.4 * ring.width(), "shallow: " + ring.length());
        assertTrue(inside.on("beam", ring.minAlong() + ring.length() / 2).across()
                < outside.on("beam", ring.minAlong() + ring.length() / 2).across(), "a ring with some thickness");
        // behind the taper, round the hub
        assertTrue(ring.maxAlong() <= (Bomber.HUB_ENDS + 0.011 - 0.5) * l, "from the hull's rear");
        assertTrue(Bomber.hull(l).extent().minAlong() < ring.minAlong(), "its hub running on through it");
        // its body: the hull, the shroud's two faces and four vanes, each by its lines, its skeleton
        final Craft body = Bomber.body(l);
        assertEquals(1 + 2 + 4, body.skeletons().size(), "the hull, the shroud's two faces, four vanes");
        assertTrue(body.panels().stream().anyMatch(p -> p.paint() == Paint.CAR), "its keel band");
        assertEquals(0, body.spinners().size(), "nothing turning in it yet");
    }

    @Test
    void aPropellerIsMadeToAnySizeItsBladesByItsLinesTwistedForASteadyPitch() {
        // more blades the bigger, turning no faster than can be seen
        assertEquals(2, Propeller.of(100).blades());
        assertEquals(3, Propeller.of(300).blades());
        assertEquals(4, Propeller.of(600).blades());
        assertEquals(5, Propeller.of(1400).blades());
        assertEquals(Propeller.MOST_RATE, Propeller.of(300).rate(), 1e-9);
        assertEquals(Propeller.TIPS / (Math.PI * 1400), Propeller.of(1400).rate(), 1e-9, "a giant's, by its tips");
        for (final double d : new double[] {50, 300, 1400}) {
            final Propeller propeller = Propeller.of(d);
            final Lines blade = propeller.blade();
            assertEquals(d / 2, blade.extent().maxAlong(), 1e-6, "out to its tip");
            // (a model's, half a metre across, is thinner than a millimetre just short of its tip: one point, to
            // a graph made for buildings; its lines are whole all the same)
            if (d >= 300) {
                assertTrue(blade.skeleton().problems().isEmpty(), blade.skeleton().problems().toString());
            }
            // twisted: its chord steep at its root, flatter at its tip; at three quarters out, set for its pitch
            assertTrue(chordAngle(blade, 0) > 60 && chordAngle(blade, 5) < 20, chordAngle(blade, 0) + " to " + chordAngle(blade, 5));
            final double r = 0.82 * d / 2;
            assertEquals(Math.toDegrees(Math.atan(Propeller.PITCH * d / (2 * Math.PI * r))), chordAngle(blade, 4), 1e-6);
            // whole: all of it turning, about +along, its spinner and each blade by its lines
            final Craft whole = propeller.build();
            assertEquals(1, whole.spinners().size());
            final Spinner turning = whole.spinners().getFirst();
            assertEquals(new Point(0, 1, 0), turning.axis());
            assertTrue(turning.rate() > 0);
            assertEquals(1 + propeller.blades(), turning.skeletons().size(), "its spinner and its blades");
            assertTrue(whole.panels().isEmpty() && whole.pieces().isEmpty(), "nothing still");
            final Extent e = whole.extent();
            assertEquals(d / 2, e.maxUp(), 0.01 * d, "its first blade straight up");
            assertTrue(e.length() < 0.25 * d, "thin, along its axis");
        }
        // a coaxial pair: one behind the other, turning opposite ways, its blades the other hand
        final Craft pair = Propeller.of(1400).coaxial(0.16);
        assertEquals(2, pair.spinners().size());
        final Spinner ahead = pair.spinners().get(0);
        final Spinner behind = pair.spinners().get(1);
        assertEquals(0.16 * 1400, ahead.hub().along() - behind.hub().along(), 1e-6);
        assertEquals(-ahead.rate(), behind.rate(), 1e-9);
    }

    /** How steeply blade {@code blade}'s {@code k}th section's chord is set to its turning, degrees: leading edge to trailing. */
    private static double chordAngle(final Lines blade, final int k) {
        final double[] lead = blade.sections().get(k).points().get(0);
        final double[] trail = blade.sections().get(k).points().get(3);
        // across the way it turns, and up back against its pull: so the leading edge is ahead and pulled forward
        return Math.toDegrees(Math.atan2(-(lead[1] - trail[1]), lead[0] - trail[0]));
    }

    @Test
    void theGunshipFlownInTheWorldIsAsBigAsItIsDrawnItsWheelsWhereTheyAre() {
        final Extent drawn = Gunship.of(1600, Gunship.Lift.TWIN_PROPELLERS).extent();
        final com.example.fxsuite.avatar.sim.Course.Section box = com.example.fxsuite.avatar.sim.Course.GUNSHIP.room();
        assertEquals(drawn.maxAcross(), box.across() + box.halfAcross(), 2, "out to its ducts");
        assertEquals(drawn.minAlong(), box.along() - box.halfAlong(), 2, "its tail");
        assertEquals(drawn.maxAlong(), box.along() + box.halfAlong(), 2, "its nose");
        assertEquals(drawn.minUp(), box.up() - box.halfUp(), 2, "its wheels");
        assertTrue(box.up() + box.halfUp() >= drawn.maxUp(), "over its top, and its ducts tilted");
        assertEquals(-Gunship.hardpoints(1600).ground(), com.example.fxsuite.avatar.sim.Course.GUNSHIP.standing(), 1,
                "standing on its wheels");
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
                craft.panels().forEach(p -> fixed.addAll(p.corners()));
                final Extent still = Extent.of(fixed);
                // (a main rotor's tail fan is on one side only, against its turning, as it would be)
                if (kind != Crafts.Kind.GUNSHIP_ROTOR) {
                    assertEquals(-still.minAcross(), still.maxAcross(), 1e-6 * size * 100, what + ": the same either side");
                }
                // as long as it was made, give or take what stands out of it (a drone, as wide, its rotors in it; a
                // helicopter, its rotors out of it)
                // an aircraft or an engine as long (or as wide) as it was made; a weapon or a body, any size it is
                if (kind.category() == Crafts.Category.AIRCRAFT || kind.engine()) {
                    // (a propeller by its disc: its blades' reach, up or across, as they stand)
                    final double made = kind == Crafts.Kind.PROPELLER || kind == Crafts.Kind.COAXIAL_PROPELLER
                            ? Math.max(e.width(), e.height())
                            : kind == Crafts.Kind.QUADCOPTER || kind == Crafts.Kind.DUCTED_FAN
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
