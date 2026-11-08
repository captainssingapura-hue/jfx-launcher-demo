package com.example.fxsuite.web.action;

import hue.captains.singapura.tao.http.action.Param;
import hue.captains.singapura.tao.http.action.ParamMarshaller;

import io.vertx.ext.web.RoutingContext;

/** For a route that reads nothing from the query string or the headers. */
public final class NoParams {

    private NoParams() {}

    public record Query() implements Param._QueryString {}

    public record Headers() implements Param._Header {}

    public static ParamMarshaller._QueryString<RoutingContext, Query> query() {
        return ctx -> new Query();
    }

    public static ParamMarshaller._Header<RoutingContext, Headers> headers() {
        return ctx -> new Headers();
    }
}
