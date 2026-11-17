package com.example.fxsuite.avatar;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The views at a land vehicle's controls, as racing games have them: C goes through all five in turn, and round again. */
class DriveViewTest {

    @Test
    void allFiveInTurnAndRoundAgain() {
        final List<Game.DriveView> seen = new ArrayList<>();
        Game.DriveView view = Game.DriveView.CHASE_FAR;
        for (int k = 0; k < 5; k++) {
            seen.add(view);
            view = view.next();
        }
        assertEquals(List.of(Game.DriveView.values()), seen);
        assertEquals(Game.DriveView.CHASE_FAR, view, "and round again");
        assertFalse(Game.DriveView.CHASE_FAR.onIt());
        assertFalse(Game.DriveView.CHASE_NEAR.onIt());
        assertTrue(Game.DriveView.HOOD.onIt() && Game.DriveView.COCKPIT.onIt() && Game.DriveView.BUMPER.onIt(),
                "from a point on the vehicle");
    }
}
