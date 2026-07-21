package com.example.fxsuite.web;

import com.example.fxsuite.web.action.CatalogGetAction;
import com.example.fxsuite.web.action.TokenGetAction;
import com.example.fxsuite.web.repo.AppRepository;

import hue.captains.singapura.js.homing.core.AppModule;
import hue.captains.singapura.js.homing.studio.base.DefaultFixtures;
import hue.captains.singapura.js.homing.studio.base.Fixtures;
import hue.captains.singapura.js.homing.studio.base.Studio;
import hue.captains.singapura.js.homing.studio.base.Umbrella;
import hue.captains.singapura.tao.http.action.GetAction;

import io.vertx.ext.web.RoutingContext;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Studio fixtures. Chrome and harness apps delegate to {@link DefaultFixtures}; the
 * studio's own HTTP endpoints are contributed through {@link #harnessGetActions()} —
 * homing's standard extension point for adding routes to the studio server.
 *
 * <p>That is why the token and catalogue APIs are <b>same-origin</b> with the pages
 * that call them: no side-car HTTP server, and no CORS on the one endpoint that must
 * not be callable by other sites.</p>
 */
public record LauncherFixtures<S extends Studio<?>>(Umbrella<S> umbrella, AppRepository repo)
        implements Fixtures<S> {

    public LauncherFixtures {
        Objects.requireNonNull(umbrella);
        Objects.requireNonNull(repo);
    }

    @Override public List<AppModule<?, ?>> harnessApps() {
        return new DefaultFixtures<>(umbrella).harnessApps();
    }

    @Override public NodeChrome chromeFor(Umbrella<S> node) {
        return new DefaultFixtures<>(umbrella).chromeFor(node);
    }

    @Override
    public Map<String, GetAction<RoutingContext, ?, ?, ?>> harnessGetActions() {
        return Map.of(
                "/token", new TokenGetAction(repo),
                "/catalog", new CatalogGetAction(repo));
    }
}
