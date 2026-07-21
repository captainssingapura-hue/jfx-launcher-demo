package com.example.fxsuite.web.action;

import com.example.fxsuite.web.repo.AppRepository;
import com.example.fxsuite.web.token.LaunchTokenIssuer;

import hue.captains.singapura.js.homing.server.EmptyParam;
import hue.captains.singapura.tao.http.action.GetAction;
import hue.captains.singapura.tao.http.action.Param;
import hue.captains.singapura.tao.http.action.ParamMarshaller;

import io.vertx.ext.web.RoutingContext;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * {@code GET /token?app=&env=&ver=} — mints a signed launch URL for one environment.
 *
 * <p>Registered through {@code Fixtures.harnessGetActions()}, so it is served by the
 * studio itself rather than a side-car HTTP server. Because it is same-origin with the
 * pages that call it, no CORS is involved — which matters for the one endpoint that
 * must not be callable by other sites.</p>
 *
 * <p>The response is a plain record: the framework serialises any non-{@code TypedContent}
 * result to JSON, so there is no hand-built JSON here.</p>
 */
public final class TokenGetAction
        implements GetAction<RoutingContext, TokenGetAction.Query, EmptyParam.NoHeaders, TokenGetAction.Response> {

    private static final Pattern APP = Pattern.compile("[a-z0-9][a-z0-9-]{0,31}");
    private static final Pattern VER = Pattern.compile("[0-9][0-9A-Za-z.\\-]{0,31}");
    private static final Pattern ENV = Pattern.compile("[a-z0-9][a-z0-9-]{0,31}");

    /** Typed view of the query string. */
    public record Query(String app, String env, String ver) implements Param._QueryString {}

    /** Serialised to JSON by the framework. */
    public record Response(String url, String env, String app, String ver,
                           String sha256, boolean dedicatedKey, long exp) {}

    private final AppRepository repo;
    private final LaunchTokenIssuer issuer = new LaunchTokenIssuer();

    public TokenGetAction(AppRepository repo) {
        this.repo = repo;
    }

    @Override
    public ParamMarshaller._QueryString<RoutingContext, Query> queryStrMarshaller() {
        return ctx -> new Query(ctx.request().getParam("app"),
                                ctx.request().getParam("env"),
                                ctx.request().getParam("ver"));
    }

    @Override
    public ParamMarshaller._Header<RoutingContext, EmptyParam.NoHeaders> headerMarshaller() {
        return ctx -> new EmptyParam.NoHeaders();
    }

    @Override
    public CompletableFuture<Response> execute(Query q, EmptyParam.NoHeaders headers) {
        try {
            String app = require(q.app(), APP, "app");
            String env = require(q.env(), ENV, "env");

            // Version: explicit request, else this environment's policy, else latest.
            String ver = (q.ver() == null || q.ver().isBlank()) ? pinnedVersionFor(env) : q.ver();
            if (ver == null) {
                ver = repo.latest(app).orElseThrow(
                        () -> new ApiException(404, "no published versions for " + app));
            }
            if (!VER.matcher(ver).matches()) throw new ApiException(400, "invalid version");
            if (!repo.has(app, ver)) throw new ApiException(404, app + " " + ver + " is not published");

            long now = System.currentTimeMillis() / 1000;
            String sha256 = repo.sha256(app, ver);            // hash of the exact published jar
            String token = issuer.issue(env, app, ver, sha256, now);
            // The scheme carries the environment, so the OS routes to that environment's launcher.
            String url = "fxsuite-" + env + "://launch/" + app + "?tok=" + token;

            return CompletableFuture.completedFuture(new Response(
                    url, env, app, ver, sha256, issuer.hasDedicatedKey(env), issuer.defaultExpiry(now)));
        } catch (ApiException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    /** Version policy per environment: Production pinned, UAT the RC, dev the newest. */
    private static String pinnedVersionFor(String env) {
        if (env.equals("prod")) return "1.0.0";
        if (env.equals("uat")) return "1.1.0";
        return null;   // dev[1..n] -> latest published
    }

    private static String require(String value, Pattern shape, String name) {
        return Optional.ofNullable(value)
                .filter(v -> shape.matcher(v).matches())
                .orElseThrow(() -> new ApiException(400, "missing or invalid " + name));
    }
}
