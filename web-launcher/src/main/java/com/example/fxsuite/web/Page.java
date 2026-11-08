package com.example.fxsuite.web;

import com.example.fxsuite.web.action.NoParams;

import hue.captains.singapura.tao.http.action.GetAction;
import hue.captains.singapura.tao.http.action.ParamMarshaller;
import hue.captains.singapura.tao.http.action.TypedContent;

import io.vertx.ext.web.RoutingContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * One page or asset of the dashboard, served from {@code src/main/resources/web/}.
 *
 * <p>It is read once, when the routes are built, so a missing file stops the server
 * starting instead of failing on the first visit.</p>
 */
public final class Page implements GetAction<RoutingContext, NoParams.Query, NoParams.Headers, Page.Content> {

    /** Returned as-is: ja-http writes {@link TypedContent} without turning it into JSON. */
    public record Content(String contentType, String body) implements TypedContent {}

    private final Content content;

    private Page(String name, String contentType) {
        String resource = "/web/" + name;
        try (InputStream in = Page.class.getResourceAsStream(resource)) {
            if (in == null) throw new IllegalStateException("missing page resource " + resource);
            this.content = new Content(contentType, new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // The charset is explicit: the pages contain non-ASCII text such as "·" and "…".
    public static Page html(String name) { return new Page(name, "text/html; charset=utf-8"); }
    public static Page css(String name)  { return new Page(name, "text/css; charset=utf-8"); }
    public static Page js(String name)   { return new Page(name, "text/javascript; charset=utf-8"); }

    @Override
    public ParamMarshaller._QueryString<RoutingContext, NoParams.Query> queryStrMarshaller() {
        return NoParams.query();
    }

    @Override
    public ParamMarshaller._Header<RoutingContext, NoParams.Headers> headerMarshaller() {
        return NoParams.headers();
    }

    @Override
    public CompletableFuture<Content> execute(NoParams.Query q, NoParams.Headers h) {
        return CompletableFuture.completedFuture(content);
    }
}
