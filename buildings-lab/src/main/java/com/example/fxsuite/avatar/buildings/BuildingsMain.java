package com.example.fxsuite.avatar.buildings;

import com.example.fxsuite.avatar.Game;

/**
 * The buildings lab's jar, run: its bench ({@link TowerBench}). Started from a class that is not JavaFX's, so the jar
 * runs with JavaFX on its class path; the game's worlds told of the simulation first, as any game window needs.
 */
public final class BuildingsMain {

    private BuildingsMain() {
    }

    public static void main(final String[] args) {
        Game.tellWorlds();
        javafx.application.Application.launch(TowerBench.class, args);
    }
}
