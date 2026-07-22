package com.example.fxsuite.env.uat;

import com.example.fxsuite.app.hello.HelloMain;
import com.example.fxsuite.launcher.env.BundledApp;
import com.example.fxsuite.launcher.env.SingletonEnv;

import java.util.List;

/**
 * UAT — a singleton environment, like Production but with its own trust root, its own
 * repository and its own {@code fxsuite-uat://} scheme.
 */
public final class UatEnv extends SingletonEnv {

    public UatEnv() {
        super("uat", "UAT");
    }

    @Override
    public String repoBase() {
        return "http://localhost:8087";
    }

    @Override
    public String accentColour() {
        return "#ef6c00";
    }

    @Override
    public List<BundledApp> bundledApps() {
        return List.of(BundledApp.of("hello", HelloMain.class, "1.0.0"));
    }
}
