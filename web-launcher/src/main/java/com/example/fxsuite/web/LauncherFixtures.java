package com.example.fxsuite.web;

import hue.captains.singapura.js.homing.core.AppModule;
import hue.captains.singapura.js.homing.studio.base.DefaultFixtures;
import hue.captains.singapura.js.homing.studio.base.Fixtures;
import hue.captains.singapura.js.homing.studio.base.Studio;
import hue.captains.singapura.js.homing.studio.base.Umbrella;

import java.util.List;
import java.util.Objects;

/**
 * Plain delegation to {@link DefaultFixtures} — the launcher has no custom
 * harness apps or chrome, so the framework defaults are all it needs.
 */
public record LauncherFixtures<S extends Studio<?>>(Umbrella<S> umbrella)
        implements Fixtures<S> {

    public LauncherFixtures {
        Objects.requireNonNull(umbrella);
    }

    @Override public List<AppModule<?, ?>> harnessApps() {
        return new DefaultFixtures<>(umbrella).harnessApps();
    }

    @Override public NodeChrome chromeFor(Umbrella<S> node) {
        return new DefaultFixtures<>(umbrella).chromeFor(node);
    }
}
