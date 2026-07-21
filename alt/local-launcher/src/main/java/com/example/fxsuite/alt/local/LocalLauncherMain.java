package com.example.fxsuite.alt.local;

import javafx.application.Application;

/**
 * Entry point for the local launcher (variant 2). Plain {@code main} calling
 * {@link Application#launch} so JavaFX starts from the classpath (shared
 * {@code fxsuite-javafx.jar}).
 *
 * <p>Config via system properties:</p>
 * <ul>
 *   <li>{@code -Dfxsuite.repo.base} — backend base URL (default http://localhost:8090)</li>
 *   <li>{@code -Dfxsuite.shared.jar} — path to fxsuite-javafx.jar (default
 *       fxsuite-javafx/target/fxsuite-javafx.jar)</li>
 * </ul>
 */
public final class LocalLauncherMain {
    private LocalLauncherMain() {}

    public static void main(String[] args) {
        Application.launch(LocalLauncherApp.class, args);
    }
}
