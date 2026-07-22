package com.example.fxsuite.env.dev;

import com.example.fxsuite.app.hello.HelloMain;
import com.example.fxsuite.launcher.env.BundledApp;
import com.example.fxsuite.launcher.env.MultiplexedEnv;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Development — a multiplexed environment: this one build serves dev1 … dev99.
 *
 * <p>Developer environments are created and discarded far too often to justify a build each,
 * so the jar is installed once and registered once per environment, each registration
 * passing {@code --env=devN}. Membership is enforced, so this build can never be registered
 * as {@code fxsuite-prod://}.</p>
 */
public final class DevEnv extends MultiplexedEnv {

    public DevEnv() {
        // membership is printed to the Windows console too, so keep it ASCII
        super("dev", "Development", Pattern.compile("dev[1-9][0-9]?"), "dev1..dev99");
    }

    @Override
    public String repoBase() {
        return "http://localhost:8087";
    }

    @Override
    public String accentColour() {
        return "#1565c0";
    }

    @Override
    public List<BundledApp> bundledApps() {
        return List.of(BundledApp.of("hello", HelloMain.class, "1.0.0"));
    }
}
