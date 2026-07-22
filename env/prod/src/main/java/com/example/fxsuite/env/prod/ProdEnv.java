package com.example.fxsuite.env.prod;

import com.example.fxsuite.app.hello.HelloMain;
import com.example.fxsuite.launcher.env.BundledApp;
import com.example.fxsuite.launcher.env.SingletonEnv;

import java.util.List;

/**
 * Production — a singleton environment: this build serves prod and nothing else.
 *
 * <p>Because it is a {@link SingletonEnv}, its registered command needs no {@code --env},
 * and a {@code --env} naming any other environment is refused outright.</p>
 */
public final class ProdEnv extends SingletonEnv {

    public ProdEnv() {
        super("prod", "Production");
    }

    @Override
    public String repoBase() {
        return "http://localhost:8087";
    }

    @Override
    public String accentColour() {
        return "#c62828";
    }

    @Override
    public List<BundledApp> bundledApps() {
        return List.of(BundledApp.of("hello", HelloMain.class, "1.0.0"));
    }
}
