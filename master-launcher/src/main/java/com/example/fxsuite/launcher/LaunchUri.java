package com.example.fxsuite.launcher;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

/**
 * Parses and validates the {@code fxsuite://} URL the OS hands us.
 *
 * <p>Everything arriving here is <b>untrusted</b>: any web page (not just ours),
 * an email, or a local file can craft an {@code fxsuite://} URL once the handler
 * is registered. So this parser is strict and fail-closed:</p>
 * <ul>
 *   <li>scheme must be exactly {@code fxsuite};</li>
 *   <li>the authority must be the single action we understand, {@code launch};</li>
 *   <li>the app id must match a tight character allow-list — no path traversal,
 *       no separators, no surprises — before it is ever looked up in
 *       {@link com.example.fxsuite.launcher.app.AppRegistry}.</li>
 * </ul>
 *
 * <p>Expected shape: {@code fxsuite://launch/<appId>?tok=<signed-token>}. The
 * signed token in the {@code tok} query param is extracted verbatim here and
 * verified by {@link com.example.fxsuite.launcher.token.TokenVerifier}; a
 * missing token yields {@code null}, which the verifier rejects.</p>
 */
public record LaunchUri(String action, String appId, String token) {

    /** Tight allow-list for the app id: lowercase, digits, hyphen; 1–32 chars. */
    private static final Pattern APP_ID = Pattern.compile("[a-z0-9][a-z0-9-]{0,31}");

    private static final String SCHEME = "fxsuite";
    private static final String ACTION_LAUNCH = "launch";

    /**
     * @throws LaunchException with a human-readable reason if anything about the
     *         URL is malformed or outside the allow-list.
     */
    public static LaunchUri parse(String raw) throws LaunchException {
        if (raw == null || raw.isBlank()) {
            throw new LaunchException("No launch URL was provided.");
        }

        final URI uri;
        try {
            uri = new URI(raw.trim());
        } catch (URISyntaxException e) {
            throw new LaunchException("Not a valid URL: " + raw);
        }

        String scheme = uri.getScheme();
        if (scheme == null || !scheme.equalsIgnoreCase(SCHEME)) {
            throw new LaunchException("Unsupported scheme (expected fxsuite://): " + raw);
        }

        // fxsuite://launch/hello  ->  host = "launch", path = "/hello"
        String action = uri.getHost();
        if (action == null || !action.equalsIgnoreCase(ACTION_LAUNCH)) {
            throw new LaunchException("Unknown action (expected fxsuite://launch/…): " + raw);
        }

        String path = uri.getPath() == null ? "" : uri.getPath();
        // Strip the single leading slash; reject anything with further structure
        // so nested paths like /a/b or /../x can never reach the registry lookup.
        String appId = path.startsWith("/") ? path.substring(1) : path;
        if (appId.isEmpty()) {
            throw new LaunchException("No app id in URL (expected fxsuite://launch/<app>): " + raw);
        }
        if (!APP_ID.matcher(appId).matches()) {
            throw new LaunchException("Illegal app id: '" + appId + "'");
        }

        return new LaunchUri(ACTION_LAUNCH, appId, extractToken(uri.getRawQuery()));
    }

    /**
     * Pull {@code tok} out of the raw query. Read raw (undecoded) because a
     * compact JWS is already URL-safe (base64url + '.'), so decoding would only
     * risk corrupting it. Returns {@code null} when absent.
     */
    private static String extractToken(String rawQuery) {
        if (rawQuery == null || rawQuery.isEmpty()) return null;
        for (String pair : rawQuery.split("&")) {
            int eq = pair.indexOf('=');
            String key = eq >= 0 ? pair.substring(0, eq) : pair;
            if (key.equals("tok")) {
                String val = eq >= 0 ? pair.substring(eq + 1) : "";
                return val.isEmpty() ? null : val;
            }
        }
        return null;
    }
}
