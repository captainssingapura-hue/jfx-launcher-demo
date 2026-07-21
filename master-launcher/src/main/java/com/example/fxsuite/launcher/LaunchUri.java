package com.example.fxsuite.launcher;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses and validates the {@code fxsuite-<env>://} URL the OS hands us.
 *
 * <p>Everything arriving here is <b>untrusted</b>: any web page, email or local file
 * can craft such a URL once the scheme is registered. So this parser is strict and
 * fail-closed:</p>
 * <ul>
 *   <li>the scheme must be {@code fxsuite-<env>} — the environment is part of the
 *       scheme, and the launcher only accepts <i>its own</i> environment (checked by
 *       the caller against {@link EnvConfig});</li>
 *   <li>the authority must be the single action we understand, {@code launch};</li>
 *   <li>the app id must match a tight character allow-list — no traversal, no
 *       separators.</li>
 * </ul>
 *
 * <p>Expected shape: {@code fxsuite-prod://launch/<appId>?tok=<signed-token>}.</p>
 */
public record LaunchUri(String env, String action, String appId, String token) {

    /** Tight allow-list for the app id: lowercase, digits, hyphen; 1–32 chars. */
    private static final Pattern APP_ID = Pattern.compile("[a-z0-9][a-z0-9-]{0,31}");
    /** Scheme carries the environment: fxsuite-prod, fxsuite-uat, fxsuite-dev3 … */
    private static final Pattern SCHEME = Pattern.compile("fxsuite-([a-z0-9][a-z0-9-]{0,31})");

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
        if (scheme == null) {
            throw new LaunchException("Missing scheme (expected fxsuite-<env>://…): " + raw);
        }
        Matcher m = SCHEME.matcher(scheme.toLowerCase());
        if (!m.matches()) {
            throw new LaunchException("Unsupported scheme (expected fxsuite-<env>://…): " + scheme);
        }
        String env = m.group(1);

        // fxsuite-prod://launch/hello  ->  host = "launch", path = "/hello"
        String action = uri.getHost();
        if (action == null || !action.equalsIgnoreCase(ACTION_LAUNCH)) {
            throw new LaunchException("Unknown action (expected fxsuite-<env>://launch/…): " + raw);
        }

        String path = uri.getPath() == null ? "" : uri.getPath();
        String appId = path.startsWith("/") ? path.substring(1) : path;
        if (appId.isEmpty()) {
            throw new LaunchException("No app id in URL (expected …/launch/<app>): " + raw);
        }
        if (!APP_ID.matcher(appId).matches()) {
            throw new LaunchException("Illegal app id: '" + appId + "'");
        }

        return new LaunchUri(env, ACTION_LAUNCH, appId, extractToken(uri.getRawQuery()));
    }

    /**
     * Pull {@code tok} out of the raw query. Read raw (undecoded) because a compact
     * JWS is already URL-safe, so decoding would only risk corrupting it.
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
