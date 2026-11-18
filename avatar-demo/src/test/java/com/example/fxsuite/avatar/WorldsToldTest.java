package com.example.fxsuite.avatar;

import com.example.fxsuite.avatar.worlds.Bay;
import com.example.fxsuite.avatar.sim.Demos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The worlds designed outside the simulation told of it when asked, as the launcher asks before it looks any demo up
 * by name or offers them: Marina Bay then found by its name, and offered.
 */
class WorldsToldTest {

    @Test
    void marinaBayFoundByItsNameAndOfferedOnceTheWorldsAreTold() {
        Game.tellWorlds();
        Game.tellWorlds();
        assertSame(Bay.DEMO, Demos.named("MARINA_BAY"));
        assertTrue(Demos.playable().contains(Bay.DEMO));
        assertTrue(Demos.playable().stream().filter(d -> d == Bay.DEMO).count() == 1, "once");
    }
}
