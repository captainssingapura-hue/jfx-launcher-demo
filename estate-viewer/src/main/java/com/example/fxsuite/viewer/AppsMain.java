package com.example.fxsuite.viewer;

/**
 * Entry point for the App Dependencies Viewer:
 * {@code java -cp estate-viewer.jar com.example.fxsuite.viewer.AppsMain}. The jar's own
 * entry point, {@link Main}, starts the whole-estate viewer.
 */
public final class AppsMain {

    public static void main(final String[] args) {
        javafx.application.Application.launch(AppDependenciesViewer.class, args);
    }
}
