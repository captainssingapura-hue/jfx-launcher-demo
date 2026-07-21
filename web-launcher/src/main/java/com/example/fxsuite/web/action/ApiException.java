package com.example.fxsuite.web.action;

import hue.captains.singapura.tao.http.action.ExternalError;
import hue.captains.singapura.tao.http.action.HttpReturnableException;
import hue.captains.singapura.tao.http.action.InternalError;

/**
 * A failure that should surface as a specific HTTP status rather than a 500.
 *
 * <p>The framework's error handler reads {@link #statusCode()} and serialises
 * {@link #externalError()} as the response body, so callers get
 * {@code {"error":"…"}} with a sensible status.</p>
 */
public final class ApiException extends RuntimeException
        implements HttpReturnableException<ApiException.Problem, ApiException.Problem> {

    /** Both marker interfaces are empty, so one record can serve as either view. */
    public record Problem(String error) implements ExternalError, InternalError {}

    private final int statusCode;
    private final Problem problem;

    public ApiException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
        this.problem = new Problem(message);
    }

    @Override public int statusCode() { return statusCode; }
    @Override public Problem externalError() { return problem; }
    @Override public Problem internalError() { return problem; }
}
