package com.example.fxsuite.avatar.buildings;

import com.example.fxsuite.avatar.Game;

/**
 * The buildings lab's jar, run: its tower bench ({@link TowerBench}), or with {@code bay} its Marina Bay bench
 * ({@link BayBench}). Started from a class that is not JavaFX's, so the jar runs with JavaFX on its class path; the
 * game's worlds told of the simulation first, as any game window needs.
 */
public final class BuildingsMain {

    private BuildingsMain() {
    }

    public static void main(final String[] args) {
        Game.tellWorlds();
        final boolean bay = args.length > 0 && args[0].equalsIgnoreCase("bay");
        javafx.application.Application.launch(bay ? BayBench.class : TowerBench.class, args);
    }
}
