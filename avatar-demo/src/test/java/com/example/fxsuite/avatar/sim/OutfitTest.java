package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The boxer with its sidecar, going round: its weight out toward the sidecar, it tips far sooner
 * turning toward the sidecar than away; a table of the sharpest it may be steered at each speed,
 * worked out once and looked up as it goes; steady, lifting, tipping; and a passenger, as weight
 * out over the sidecar, letting it be turned harder.
 */
class OutfitTest {

    private static final Outfit SOLO = Outfit.BOXER;
    /** A km/h, in cm/s. */
    private static final double KMH = 100 / 3.6;

    @Test
    void itsWeightIsItsMassesAndLiesOutTowardTheSidecar() {
        final double kg = SOLO.masses().stream().mapToDouble(Outfit.Mass::kg).sum();
        assertEquals(kg, SOLO.mass(), 1e-9);
        assertEquals(Course.BOXER.drive().mass() + Sidecar.BOXER.frame().mass() + Outfit.CHAIR, kg, 1e-9);
        final Outfit.Mass centre = SOLO.centre();
        assertTrue(centre.across() > 5 && centre.across() < 30, "out toward the sidecar, on the right: " + centre.across());
        assertTrue(centre.up() > 40 && centre.up() < 70, "as high as a big cruiser's, rider aboard: " + centre.up());
        assertTrue(centre.along() < Course.BOXER.axles().getFirst() && centre.along() > Course.BOXER.axles().getLast(),
                "between its wheels");
    }

    @Test
    void itTipsFarSoonerTurningTowardTheSidecarThanAway() {
        final Outfit.Mass centre = SOLO.centre();
        // turning toward the sidecar (right), the pull is left, over the motorcycle's own wheels
        final double toward = SOLO.limit(-1);
        assertEquals(Outfit.GRAVITY * centre.across() / centre.up(), toward, 1e-6, "as far out as its weight, over how high");
        final double away = SOLO.limit(1);
        assertTrue(away > 3 * toward, "away from it, all but untippable: " + away / 981 + " g, against " + toward / 981);
        assertTrue(toward / Outfit.GRAVITY > 0.15 && toward / Outfit.GRAVITY < 0.35,
                "the chair lifts at a fifth of a g or so, solo: " + toward / Outfit.GRAVITY);
    }

    @Test
    void aPassengerInTheSidecarLetsItBeTurnedTowardItHarder() {
        final Outfit carrying = SOLO.carrying(80);
        assertEquals(SOLO.mass() + 80, carrying.mass(), 1e-9);
        assertTrue(carrying.centre().across() > SOLO.centre().across());
        assertTrue(carrying.limit(-1) > 1.5 * SOLO.limit(-1), "ballast in the chair: " + carrying.limit(-1) / 981);
        assertTrue(carrying.table().get(3).toward() > SOLO.table().get(3).toward());
    }

    @Test
    void itsTableGoesFromTenToItsTopTheSharpestNarrowingWithSpeed() {
        final List<Outfit.Row> table = SOLO.table();
        assertEquals(10 * KMH, table.getFirst().speed(), 1e-9);
        assertTrue(table.getLast().speed() >= Course.BOXER.drive().top() - 5 * KMH, "to its top");
        final double most = Course.BOXER.drive().steer();
        for (int r = 0; r < table.size(); r++) {
            final Outfit.Row row = table.get(r);
            assertTrue(row.toward() <= most && row.away() <= most, "no sharper than it steers at all");
            assertTrue(row.toward() <= row.away(), "toward the sidecar, never sharper than away");
            if (r > 0) {
                assertTrue(row.toward() <= table.get(r - 1).toward(), "narrowing with speed");
            }
            // as the rule has it: the turn's pull, at that steer, just its limit
            if (row.toward() < most) {
                final double radius = Course.BOXER.wheelbase() / Math.tan(Math.toRadians(row.toward()));
                assertEquals(SOLO.limit(-1), row.speed() * row.speed() / radius, 1e-6);
            }
        }
        assertEquals(1.76, table.get(3).toward(), 0.05, "at 40 km/h, under two degrees toward it");
    }

    @Test
    void lookedUpBetweenItsRowsAndHeldAgainstItSteadyLiftingOrTipping() {
        final List<Outfit.Row> table = SOLO.table();
        final double most = Course.BOXER.drive().steer();
        // on a row, the row; between two, between them
        assertEquals(table.get(3).toward(), Outfit.lookUp(table, 40 * KMH, true, most), 1e-9);
        final double between = Outfit.lookUp(table, 45 * KMH, true, most);
        assertTrue(between < table.get(3).toward() && between > table.get(4).toward());
        assertEquals(most, Outfit.lookUp(table, 0, true, most), 1e-9, "at a stop, as sharp as it steers");
        // at 40 km/h, toward the sidecar (right): steady well within, lifting near, tipping past
        final double sharpest = table.get(3).toward();
        assertEquals(Outfit.Grip.STEADY, SOLO.grip(table, 40 * KMH, 0.5 * sharpest));
        assertEquals(Outfit.Grip.LIFTING, SOLO.grip(table, 40 * KMH, 0.95 * sharpest));
        assertEquals(Outfit.Grip.TIPPING, SOLO.grip(table, 40 * KMH, 1.1 * sharpest));
        // the same steer away from it: steady
        assertEquals(Outfit.Grip.STEADY, SOLO.grip(table, 40 * KMH, -1.1 * sharpest));
        assertEquals(Outfit.Grip.STEADY, SOLO.grip(table, 40 * KMH, 0), "straight on");
        assertEquals(1.1, SOLO.share(table, 40 * KMH, 1.1 * sharpest), 1e-9);
    }

    @Test
    void onTheLeftItTipsTurningLeftAsOnTheRightItDoesTurningRight() {
        final Outfit left = SOLO.with(Sidecar.BOXER.on(Sidecar.Side.LEFT));
        assertEquals(-SOLO.centre().across(), left.centre().across(), 1e-9);
        assertEquals(SOLO.limit(-1), left.limit(1), 1e-6, "mirrored");
        assertTrue(left.towardSidecar(-2));
        assertFalse(left.towardSidecar(2));
        final List<Outfit.Row> table = left.table();
        assertEquals(Outfit.Grip.TIPPING, left.grip(table, 40 * KMH, -1.1 * table.get(3).toward()));
        assertEquals(Outfit.Grip.STEADY, left.grip(table, 40 * KMH, 1.1 * table.get(3).toward()));
    }
}
