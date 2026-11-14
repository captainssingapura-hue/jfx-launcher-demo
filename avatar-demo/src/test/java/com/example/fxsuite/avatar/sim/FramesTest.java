package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The boxer's frame, as rods, as a real one is built: one welded piece, the same on both sides, its
 * head tube on the forks' line, clear of the ground, of both wheels and of the engine, under the tank
 * and the seat, and about as heavy as such a frame is.
 */
class FramesTest {

    private static final Frame FRAME = Frames.BOXER;
    private static final Course.Chassis BIKE = Course.BOXER;

    /** Each point along every rod, {@code steps} to a rod, as across, along, up. */
    private static java.util.List<double[]> along(final Frame frame, final int steps) {
        final java.util.List<double[]> points = new java.util.ArrayList<>();
        for (final Frame.Rod rod : frame.rods()) {
            final Frame.Node a = frame.nodes().get(rod.from());
            final Frame.Node b = frame.nodes().get(rod.to());
            for (int k = 0; k <= steps; k++) {
                final double t = (double) k / steps;
                points.add(new double[] {a.across() + (b.across() - a.across()) * t,
                        a.along() + (b.along() - a.along()) * t, a.up() + (b.up() - a.up()) * t,
                        frame.tube(rod).radius()});
            }
        }
        return points;
    }

    @Test
    void itIsOneWeldedPieceTheSameOnBothSides() {
        assertTrue(FRAME.problems().isEmpty(), FRAME.problems().toString());
        assertEquals(FRAME, Frames.of(BIKE));
        assertEquals(null, Frames.of(Course.CRUISER), "not every motorcycle is built of rods yet");
        // every node on one side has its twin on the other, or is on the middle
        for (final Frame.Node node : FRAME.nodes()) {
            if (Math.abs(node.across()) < 1e-9) {
                continue;
            }
            assertTrue(FRAME.nodes().stream().anyMatch(o -> o.across() == -node.across() && o.along() == node.along()
                    && o.up() == node.up()), node.name() + " has its twin");
        }
        // its mass in the middle, but a little to the right: its right swing arm is the shaft's housing
        assertTrue(FRAME.centre().across() > 0 && FRAME.centre().across() < 2, "across " + FRAME.centre().across());
    }

    @Test
    void itsHeadTubeLiesOnTheForksLine() {
        // the forks run from the handlebars, over the column, down to the front axle
        final double topAlong = BIKE.column().along();
        final double topUp = BIKE.ride() + BIKE.bars();
        final double axleAlong = BIKE.axles().getFirst();
        final double axleUp = BIKE.wheelRadius();
        for (final String end : new String[] {"head tube, top", "head tube, foot"}) {
            final Frame.Node node = FRAME.nodes().get(FRAME.node(end));
            // how far off the line from the bars to the axle, along and up
            final double t = (node.along() - topAlong) / (axleAlong - topAlong);
            final double onLine = topUp + t * (axleUp - topUp);
            assertEquals(onLine, node.up(), 3, end + " on the forks' line");
        }
    }

    @Test
    void itIsClearOfTheGroundTheWheelsAndTheEngineAndUnderTheTankAndSeat() {
        final double front = BIKE.axles().getFirst();
        final double back = BIKE.axles().getLast();
        final double r = BIKE.wheelRadius();
        final double half = BIKE.wheelWidth() / 2;
        for (final double[] p : along(FRAME, 40)) {
            final String at = String.format("(%.0f, %.0f, %.0f)", p[0], p[1], p[2]);
            assertTrue(p[2] - p[3] > 6, "clear of the ground at " + at);
            // not through either wheel: inside a wheel's width, within its tyre
            for (final double axle : new double[] {front, back}) {
                final boolean inWheel = Math.abs(p[0]) < half + p[3]
                        && Math.hypot(p[1] - axle, p[2] - r) < r - 2;
                assertFalse(inWheel, "through a wheel at " + at);
            }
            // not through the engine block (as drawn: across 14 either way, along -16 to 36, 12 to 40 up)
            assertFalse(Math.abs(p[0]) < 14 && p[1] > -16 && p[1] < 36 && p[2] > 12 + p[3] && p[2] < 40 - p[3],
                    "through the engine at " + at);
            // no higher than the seat and the tank, about 80 up
            assertTrue(p[2] + p[3] < 84, "under the tank and seat at " + at);
        }
    }

    @Test
    void itIsAboutAsHeavyAsSuchAFrameIs() {
        assertTrue(FRAME.mass() > 10 && FRAME.mass() < 35, FRAME.mass() + " kg");
        assertTrue(FRAME.steel() > 800, "some 8 m of tube and more: " + FRAME.steel());
        assertTrue(FRAME.slack() > 0, "held by its welds, as such a frame is");
        assertTrue(FRAME.nodes().stream().filter(n -> n.kind() == Frame.Kind.HINGE).count() == 2,
                "the swing arm's pivot, either side");
    }

    @Test
    void keptAsTextItReadsBackTheSame() {
        assertEquals(FRAME, Frame.read(FRAME.text()));
    }
}
