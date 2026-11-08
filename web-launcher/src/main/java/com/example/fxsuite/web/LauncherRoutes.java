package com.example.fxsuite.web;

import com.example.fxsuite.web.action.CatalogGetAction;
import com.example.fxsuite.web.action.TokenGetAction;
import com.example.fxsuite.web.repo.AppRepository;

import hue.captains.singapura.tao.http.action.ActionRegistry;
import hue.captains.singapura.tao.http.action.GetAction;
import hue.captains.singapura.tao.http.action.PostAction;

import io.vertx.ext.web.RoutingContext;

import java.util.Map;
import java.util.Objects;

/**
 * Every route of the dashboard: its pages and the API they call.
 *
 * <p>The pages and the API share one origin, so the pages call {@code /token} without CORS,
 * and other sites cannot read what it returns. That matters for the one endpoint that must
 * not be usable by other sites.</p>
 */
public record LauncherRoutes(AppRepository repo) implements ActionRegistry<RoutingContext> {

    public LauncherRoutes {
        Objects.requireNonNull(repo);
    }

    @Override
    public Map<String, GetAction<RoutingContext, ?, ?, ?>> getActions() {
        return Map.of(
                "/", Page.html("index.html"),
                "/launch", Page.html("launch.html"),
                "/published", Page.html("published.html"),
                "/site.css", Page.css("site.css"),
                "/launch.js", Page.js("launch.js"),
                "/published.js", Page.js("published.js"),
                "/token", new TokenGetAction(repo),
                "/catalog", new CatalogGetAction(repo));
    }

    @Override
    public Map<String, PostAction<RoutingContext, ?, ?, ?>> postActions() {
        return Map.of();
    }
}
