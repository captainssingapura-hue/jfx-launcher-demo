package com.example.fxsuite.setup;

import javafx.application.Application;

/**
 * Entry point for the setup app.
 *
 * <p>As elsewhere, a plain {@code main} that calls {@link Application#launch} — that is
 * what allows a bare {@code java -jar fxsuite-setup.jar} to work with JavaFX on the
 * classpath, with no module-path flags and no prior installation.</p>
 */
public final class SetupMain {
    private SetupMain() {}

    public static void main(String[] args) {
        Application.launch(SetupApp.class, args);
    }
}
