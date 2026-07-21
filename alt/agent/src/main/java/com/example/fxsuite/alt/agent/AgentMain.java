package com.example.fxsuite.alt.agent;

import javafx.application.Application;

/**
 * Entry point for the agent. Like the other JavaFX pieces, a plain {@code main}
 * that calls {@link Application#launch} so JavaFX starts from the classpath (the
 * shared {@code fxsuite-javafx.jar}) without module-path flags.
 *
 * <p>Optional args: {@code args[0]} = user (default "alice"),
 * {@code args[1]} = relay ws URL base (default ws://localhost:8091).</p>
 */
public final class AgentMain {
    private AgentMain() {}

    public static void main(String[] args) {
        Application.launch(AgentApp.class, args);
    }
}
