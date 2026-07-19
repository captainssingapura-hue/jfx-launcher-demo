package com.example.fxsuite.app.hello;

import javafx.application.Application;

/**
 * Process entry point for the Hello app.
 *
 * <p>Like the launcher, this deliberately does not extend {@link Application}:
 * a plain {@code main} that calls {@link Application#launch} is what lets JavaFX
 * start from the classpath (here the shared {@code fxsuite-javafx.jar} the
 * launcher puts on {@code -cp}), without any module-path flags.</p>
 *
 * <p>Args (all optional): {@code args[0]} = window title.</p>
 */
public final class HelloMain {
    private HelloMain() {}

    public static void main(String[] args) {
        Application.launch(HelloApp.class, args);
    }
}
