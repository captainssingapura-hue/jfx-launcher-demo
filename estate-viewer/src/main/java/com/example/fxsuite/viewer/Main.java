package com.example.fxsuite.viewer;

/**
 * Plain entry point. Launching the JavaFX application class directly from the classpath
 * fails with "JavaFX runtime components are missing", so the jar starts here.
 */
public final class Main {

    public static void main(final String[] args) {
        javafx.application.Application.launch(EstateViewer.class, args);
    }
}
