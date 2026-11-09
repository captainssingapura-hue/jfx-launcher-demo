package com.example.acme.hello;

import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;
import javafx.application.Application;

/**
 * Entry point. Launching the JavaFX application class directly from a classpath fails, so
 * the app starts here.
 */
public final class Main implements StatelessFunctionalObject {

    public static void main(final String[] args) {
        Application.launch(HelloApp.class, args);
    }
}
