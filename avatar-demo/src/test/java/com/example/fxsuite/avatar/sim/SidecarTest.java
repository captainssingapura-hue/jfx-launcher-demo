package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The boxer's sidecar chassis: a few steel tubes welded into one frame, clamped to the motorcycle's
 * own frame at four points, clear of it everywhere else, holding its wheel on the ground out to the
 * side, on the rear part of its frame, a little ahead of the motorcycle's back wheel; mounted on
 * either side, the same frame mirrored; kept as text.
 */
class SidecarTest {

    private static final Sidecar SIDECAR = Sidecar.BOXER;
    private static final Frame FRAME = SIDECAR.frame();

    /** How far {@code p} is from the rod from {@code a} to {@code b}. */
    static double fromRod(final Frame.Node p, final Frame.Node a, final Frame.Node b) {
        final double[] ab = {b.across() - a.across(), b.along() - a.along(), b.up() - a.up()};
        final double[] ap = {p.across() - a.across(), p.along() - a.along(), p.up() - a.up()};
        final double run = ab[0] * ab[0] + ab[1] * ab[1] + ab[2] * ab[2];
        final double t = Math.clamp((ap[0] * ab[0] + ap[1] * ab[1] + ap[2] * ab[2]) / run, 0, 1);
        return Math.sqrt(Math.pow(ap[0] - t * ab[0], 2) + Math.pow(ap[1] - t * ab[1], 2) + Math.pow(ap[2] - t * ab[2], 2));
    }

    @Test
    void itIsOneFrameOfTubesClampedToTheMotorcyclesOwnTubesAtFourPoints() {
        assertTrue(FRAME.problems().isEmpty(), FRAME.problems().toString());
        assertTrue(FRAME.rods().size() >= 8, "a few tubes: " + FRAME.rods().size());
        final List<Frame.Node> mounts = FRAME.nodes().stream().filter(Frame.Node::mount).toList();
        assertEquals(4, mounts.size(), "two low struts and two high, so it cannot twist");
        // each clamped round one of the motorcycle's own tubes, on its right
        final Frame bike = Frames.BOXER;
        for (final Frame.Node mount : mounts) {
            final double nearest = bike.rods().stream().mapToDouble(r -> fromRod(mount, bike.nodes().get(r.from()),
                    bike.nodes().get(r.to()))).min().orElseThrow();
            assertTrue(nearest < 1, mount.name() + " on a tube of the motorcycle's: " + nearest + " cm off");
            assertTrue(mount.across() > 0, mount.name() + " on its right");
        }
    }

    @Test
    void theRestOfItIsClearOfTheMotorcycleAndItsCylinders() {
        final double side = Course.BOXER.width() / 2;
        for (final Frame.Node node : FRAME.nodes()) {
            if (!node.mount()) {
                assertTrue(node.across() > side + FRAME.tube().radius(), node.name() + " out beside it");
            }
        }
        // no tube through the right cylinder, out to the side ahead of the rider's knees (as drawn)
        final double ride = Course.BOXER.ride();
        for (final Frame.Rod rod : FRAME.rods()) {
            final Frame.Node a = FRAME.nodes().get(rod.from());
            final Frame.Node b = FRAME.nodes().get(rod.to());
            for (int k = 0; k <= 50; k++) {
                final double t = k / 50.0;
                final double across = a.across() + (b.across() - a.across()) * t;
                final double along = a.along() + (b.along() - a.along()) * t;
                final double up = a.up() + (b.up() - a.up()) * t;
                assertFalse(across > 14 && across < 49 && along > 12 && along < 32 && up > ride - 17 && up < ride + 1,
                        a.name() + " to " + b.name() + " through the cylinder");
            }
        }
    }

    @Test
    void itsWheelStandsOnTheGroundOnTheRearPartALittleAheadOfTheBackWheel() {
        assertEquals(SIDECAR.radius(), SIDECAR.hub().up(), 1e-9, "on the ground");
        assertTrue(SIDECAR.track() > 100 && SIDECAR.track() < 150, "a metre and a third or so out: " + SIDECAR.track());
        final double lead = SIDECAR.lead(Course.BOXER.axles().getLast());
        assertTrue(lead > 5 && lead < 45, "a little ahead of the back wheel, as a sidecar's is set up: " + lead);
        assertTrue(SIDECAR.radius() < Course.BOXER.wheelRadius(), "a smaller wheel than the motorcycle's");
        final Frame.Node front = FRAME.nodes().get(SIDECAR.mounting().front());
        final Frame.Node back = FRAME.nodes().get(SIDECAR.mounting().back());
        assertTrue(SIDECAR.along() < (front.along() + back.along()) / 2, "on the rear part of its outer rail");
        assertTrue(FRAME.mass() > 10 && FRAME.mass() < 40, "a frame one can lift: " + FRAME.mass() + " kg");
    }

    @Test
    void itsWheelIsSetAlongAndOutAndItsStubAxleGoesWithItAlongTheRail() {
        final Sidecar set = SIDECAR.wheel(-20, 150);
        assertEquals(-20, set.along(), 1e-9);
        assertEquals(150, set.track(), 1e-9);
        final Frame.Node axle = set.frame().nodes().get(set.mounting().axle());
        final Frame.Node stub = set.frame().nodes().get(set.mounting().stub());
        final Frame.Node front = set.frame().nodes().get(set.mounting().front());
        assertEquals(-20, axle.along(), 1e-9, "on the rail, beside the wheel");
        assertEquals(front.across(), axle.across(), 1e-9, "the rail straight, so as far out as its ends");
        assertEquals(-20, stub.along(), 1e-9);
        assertTrue(stub.across() < 150 && stub.across() > 150 - set.width(), "the stub axle into the wheel's middle");
        assertEquals(set.radius(), stub.up(), 1e-9, "at the hub's height");
        assertTrue(set.frame().connected());
        // no further along than the rail reaches, either way
        assertEquals(front.along(), SIDECAR.wheel(500, 132).along(), 1e-9);
        assertEquals(set.frame().nodes().get(set.mounting().back()).along(), SIDECAR.wheel(-500, 132).along(), 1e-9);
        // and its frame reshaped, the wheel stays where it was set, the stub axle still on the rail
        final Sidecar reshaped = set.with(set.frame().moved(set.mounting().back(), 140, -110, 24));
        assertEquals(-20, reshaped.along(), 1e-9);
        final Frame.Node onRail = reshaped.frame().nodes().get(reshaped.mounting().axle());
        assertTrue(onRail.across() > front.across() && onRail.across() < 140, "between the rail's ends, as it now runs");
        // and bigger, still on the ground
        assertEquals(34, SIDECAR.wheeled(34).hub().up(), 1e-9);
    }

    @Test
    void mountedOnTheLeftItIsTheSameFrameMirrored() {
        final Sidecar left = SIDECAR.on(Sidecar.Side.LEFT);
        assertEquals(Sidecar.Side.RIGHT, SIDECAR.side());
        assertEquals(FRAME.steel(), left.placed().steel(), 1e-9, "the same tubes");
        for (int n = 0; n < FRAME.nodes().size(); n++) {
            final Frame.Node onRight = SIDECAR.placed().nodes().get(n);
            final Frame.Node onLeft = left.placed().nodes().get(n);
            assertEquals(-onRight.across(), onLeft.across(), 1e-9, onRight.name());
            assertEquals(onRight.along(), onLeft.along(), 1e-9);
            assertEquals(onRight.up(), onLeft.up(), 1e-9);
        }
        assertTrue(left.placed(left.hub()).across() < 0, "its wheel out on the left");
        assertTrue(SIDECAR.placed(SIDECAR.hub()).across() > 0, "and on the right, on the right");
    }

    @Test
    void keptAsTextItReadsBackTheSame() {
        final Sidecar shaped = SIDECAR.on(Sidecar.Side.LEFT).wheel(-60, 140).wheeled(30);
        final Sidecar read = Sidecar.read(shaped.text());
        assertEquals(shaped.side(), read.side());
        assertEquals(shaped.mounting(), read.mounting());
        assertEquals(shaped.along(), read.along(), 1e-9);
        assertEquals(shaped.out(), read.out(), 1e-9);
        assertEquals(shaped.radius(), read.radius(), 1e-9);
        assertEquals(shaped.frame().nodes().size(), read.frame().nodes().size());
        assertEquals(shaped.frame().rods(), read.frame().rods());
        assertTrue(shaped.text().contains("side left"));
    }

    @Test
    void aShowroomHasOnlyWhatIsPutInIt() {
        final Course.Piece boxer = Course.Piece.vehicle(Course.BOXER, null, new Place(0, 0, Course.BOXER.ride(), 0), 0);
        final Course showroom = Course.of(List.of(boxer));
        assertEquals(1, showroom.pieces().size());
        assertEquals(1, Lift.of(Damage.of(showroom)).size(), "the boxer, to be shown as the game shows it");
    }
}
