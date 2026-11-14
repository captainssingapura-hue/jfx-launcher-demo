package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The boxer's sidecar chassis: a few steel tubes welded into one frame, bolted to the motorcycle at
 * four points, clear of it everywhere else, holding its wheel on the ground out to the right, a
 * little ahead of the motorcycle's back wheel.
 */
class SidecarTest {

    private static final Sidecar SIDECAR = Sidecar.BOXER;

    @Test
    void itIsOneFrameOfTubesBoltedToTheMotorcycleAtFourPoints() {
        assertTrue(SIDECAR.connected(), "every joint joined to every other through the tubes");
        assertTrue(SIDECAR.bars().size() >= 8, "a few tubes: " + SIDECAR.bars().size());
        assertEquals(4, SIDECAR.mounts().size(), "two low struts and two high, so it cannot twist");
        // bolted to the motorcycle where the motorcycle is: within its own width
        for (final Sidecar.Joint mount : SIDECAR.mounts()) {
            assertTrue(Math.abs(mount.out()) <= Course.BOXER.width() / 2, mount.name());
        }
        // every mount has a tube out from it
        for (int j = 0; j < SIDECAR.joints().size(); j++) {
            final int joint = j;
            assertTrue(SIDECAR.bars().stream().anyMatch(b -> b.from() == joint || b.to() == joint),
                    SIDECAR.joints().get(j).name());
        }
    }

    @Test
    void theRestOfItIsClearOfTheMotorcycleAndItsCylinders() {
        final double side = Course.BOXER.width() / 2;
        for (final Sidecar.Joint joint : SIDECAR.joints()) {
            if (!joint.mount()) {
                assertTrue(joint.out() > side + SIDECAR.tube(), joint.name() + " out beside it");
            }
        }
        // no tube through the right cylinder, out to the side ahead of the rider's knees (as drawn)
        final double ride = Course.BOXER.ride();
        for (final Sidecar.Bar bar : SIDECAR.bars()) {
            final Sidecar.Joint a = SIDECAR.joints().get(bar.from());
            final Sidecar.Joint b = SIDECAR.joints().get(bar.to());
            for (int k = 0; k <= 50; k++) {
                final double t = k / 50.0;
                final double across = a.out() + (b.out() - a.out()) * t;
                final double along = a.along() + (b.along() - a.along()) * t;
                final double up = a.up() + (b.up() - a.up()) * t;
                final boolean inCylinder = across > 14 && across < 49 && along > 12 && along < 32
                        && up > ride - 17 && up < ride + 1;
                assertFalse(inCylinder, a.name() + " to " + b.name() + " through the cylinder");
            }
        }
    }

    @Test
    void itsWheelStandsOnTheGroundOutToTheRightALittleAheadOfTheBackWheel() {
        assertEquals(SIDECAR.radius(), SIDECAR.hub().up(), 1e-9, "on the ground");
        assertTrue(SIDECAR.track() > 100 && SIDECAR.track() < 150, "a metre and a quarter or so out: " + SIDECAR.track());
        final double lead = SIDECAR.lead(Course.BOXER.axles().getLast());
        assertTrue(lead > 5 && lead < 45, "a little ahead of the back wheel, as a sidecar's is set up: " + lead);
        assertTrue(SIDECAR.radius() < Course.BOXER.wheelRadius(), "a smaller wheel than the motorcycle's");
        // on the rear part of the frame, as a sidecar's wheel typically is
        final Sidecar.Joint front = SIDECAR.joints().get(SIDECAR.mounting().front());
        final Sidecar.Joint back = SIDECAR.joints().get(SIDECAR.mounting().back());
        assertTrue(SIDECAR.along() < (front.along() + back.along()) / 2, "behind the middle of its outer rail");
    }

    @Test
    void itsWheelIsSetAlongAndOutAndItsStubAxleGoesWithItAlongTheRail() {
        final Sidecar set = SIDECAR.wheel(-20, 150);
        assertEquals(-20, set.along(), 1e-9);
        assertEquals(150, set.track(), 1e-9);
        final Sidecar.Joint axle = set.joints().get(set.mounting().axle());
        final Sidecar.Joint stub = set.joints().get(set.mounting().stub());
        final Sidecar.Joint front = set.joints().get(set.mounting().front());
        assertEquals(-20, axle.along(), 1e-9, "on the rail, beside the wheel");
        assertEquals(front.out(), axle.out(), 1e-9, "the rail straight, so as far out as its ends");
        assertEquals(-20, stub.along(), 1e-9);
        assertTrue(stub.out() < 150 && stub.out() > 150 - set.width(), "the stub axle into the wheel's middle");
        assertEquals(set.radius(), stub.up(), 1e-9, "at the hub's height");
        assertTrue(set.connected());
        // no further along than the rail reaches, either way
        assertEquals(front.along(), SIDECAR.wheel(500, 132).along(), 1e-9);
        assertEquals(set.joints().get(set.mounting().back()).along(), SIDECAR.wheel(-500, 132).along(), 1e-9);
        // and a joint of the frame moved, the wheel stays where it was set, the stub axle still on the rail
        final Sidecar reshaped = set.moved(set.mounting().back(), set.joints().get(set.mounting().back()).at(140, -110, 24));
        assertEquals(-20, reshaped.along(), 1e-9);
        final Sidecar.Joint onRail = reshaped.joints().get(reshaped.mounting().axle());
        assertTrue(onRail.out() > front.out() && onRail.out() < 140, "between the rail's ends, as it now runs");
    }

    @Test
    void mountedOnTheLeftItIsTheSameFrameMirrored() {
        final Sidecar left = SIDECAR.on(Sidecar.Side.LEFT);
        assertEquals(Sidecar.Side.RIGHT, SIDECAR.side());
        assertEquals(SIDECAR.steel(), left.steel(), 1e-9, "the same tubes");
        assertEquals(SIDECAR.track(), left.track(), 1e-9);
        for (int j = 0; j < SIDECAR.joints().size(); j++) {
            final Sidecar.Joint onRight = SIDECAR.placed().get(j);
            final Sidecar.Joint onLeft = left.placed().get(j);
            assertEquals(-onRight.out(), onLeft.out(), 1e-9, onRight.name());
            assertEquals(onRight.along(), onLeft.along(), 1e-9);
            assertEquals(onRight.up(), onLeft.up(), 1e-9);
        }
        assertTrue(left.placed(left.hub()).out() < 0, "its wheel out on the left");
        assertTrue(SIDECAR.placed(SIDECAR.hub()).out() > 0, "and on the right, on the right");
        assertTrue(left.code().startsWith("new Sidecar(Side.LEFT, List.of("));
    }

    @Test
    void itsSteelIsAsLongAndAsHeavyAsItsTubesMake() {
        double steel = 0;
        for (final Sidecar.Bar bar : SIDECAR.bars()) {
            steel += SIDECAR.length(bar);
        }
        assertEquals(steel, SIDECAR.steel(), 1e-9);
        final double area = Math.PI * (Math.pow(SIDECAR.tube(), 2) - Math.pow(SIDECAR.tube() - SIDECAR.wall(), 2));
        assertEquals(steel * area * Sidecar.STEEL, SIDECAR.mass(), 1e-9);
        assertTrue(SIDECAR.mass() > 10 && SIDECAR.mass() < 40, "a frame one can lift: " + SIDECAR.mass() + " kg");
    }

    @Test
    void shapedOnTheBenchItIsTheSameButWhereItWasMovedAndPrintsAsCode() {
        final Sidecar.Joint was = SIDECAR.joints().get(4);
        final Sidecar moved = SIDECAR.moved(4, was.at(was.out() + 10, was.along(), was.up()));
        assertEquals(was.out() + 10, moved.joints().get(4).out(), 1e-9);
        assertEquals(was.name(), moved.joints().get(4).name());
        for (int j = 0; j < SIDECAR.joints().size(); j++) {
            if (j != 4) {
                assertEquals(SIDECAR.joints().get(j), moved.joints().get(j));
            }
        }
        assertEquals(SIDECAR.bars(), moved.bars());
        final String code = SIDECAR.code();
        assertTrue(code.startsWith("new Sidecar(Side.RIGHT, List.of("));
        for (final Sidecar.Joint joint : SIDECAR.joints()) {
            assertTrue(code.contains("\"" + joint.name() + "\""), joint.name());
        }
        assertThrows(IllegalArgumentException.class, () -> new Sidecar(Sidecar.Side.RIGHT, SIDECAR.joints(),
                List.of(new Sidecar.Bar(0, 0)), SIDECAR.mounting(), 1.9, 0.25, -70, 132, 28, 12),
                "a tube from a joint to itself");
    }

    @Test
    void aShowroomHasOnlyWhatIsPutInIt() {
        final Course.Piece boxer = Course.Piece.vehicle(Course.BOXER, null, new Place(0, 0, Course.BOXER.ride(), 0), 0);
        final Course showroom = Course.of(List.of(boxer));
        assertEquals(1, showroom.pieces().size());
        assertEquals(1, Lift.of(Damage.of(showroom)).size(), "the boxer, to be shown as the game shows it");
    }
}
