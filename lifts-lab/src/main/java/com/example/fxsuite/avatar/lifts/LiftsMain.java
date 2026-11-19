package com.example.fxsuite.avatar.lifts;

import com.example.fxsuite.avatar.Game;

/**
 * The lifts lab's jar, run: its bench ({@link LiftBench}). Started from a class that is not JavaFX's, so the jar runs
 * with JavaFX on its class path; the game's worlds told of the simulation first, as any game window needs.
 */
public final class LiftsMain {

    private LiftsMain() {
    }

    public static void main(final String[] args) {
        Game.tellWorlds();
        javafx.application.Application.launch(LiftBench.class, args);
    }
}
