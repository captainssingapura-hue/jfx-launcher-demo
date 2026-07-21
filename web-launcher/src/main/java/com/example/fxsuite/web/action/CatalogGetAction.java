package com.example.fxsuite.web.action;

import com.example.fxsuite.web.repo.AppRepository;

import hue.captains.singapura.js.homing.server.EmptyParam;
import hue.captains.singapura.tao.http.action.GetAction;
import hue.captains.singapura.tao.http.action.Param;
import hue.captains.singapura.tao.http.action.ParamMarshaller;

import io.vertx.ext.web.RoutingContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * {@code GET /catalog} — everything published in the artifact repository.
 *
 * <p>Returns a plain record; the framework serialises it to JSON.</p>
 */
public final class CatalogGetAction
        implements GetAction<RoutingContext, CatalogGetAction.Query, EmptyParam.NoHeaders, CatalogGetAction.Response> {

    /** No query parameters — the catalogue is served whole. */
    public record Query() implements Param._QueryString {}

    public record Item(String app, String ver, String sha256) {}
    public record Response(List<Item> items) {}

    private final AppRepository repo;

    public CatalogGetAction(AppRepository repo) {
        this.repo = repo;
    }

    @Override
    public ParamMarshaller._QueryString<RoutingContext, Query> queryStrMarshaller() {
        return ctx -> new Query();
    }

    @Override
    public ParamMarshaller._Header<RoutingContext, EmptyParam.NoHeaders> headerMarshaller() {
        return ctx -> new EmptyParam.NoHeaders();
    }

    @Override
    public CompletableFuture<Response> execute(Query q, EmptyParam.NoHeaders headers) {
        List<Item> items = new ArrayList<>();
        for (String app : repo.apps()) {
            for (String ver : repo.versions(app)) {
                items.add(new Item(app, ver, repo.sha256(app, ver)));
            }
        }
        return CompletableFuture.completedFuture(new Response(items));
    }
}
