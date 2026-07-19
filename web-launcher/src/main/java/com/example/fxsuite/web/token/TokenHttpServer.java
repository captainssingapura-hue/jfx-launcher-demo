package com.example.fxsuite.web.token;

import com.example.fxsuite.web.repo.AppRepository;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The <b>authorized FxSuite origin</b>. Holds the issuer (and thus the private
 * key) and serves:
 *
 * <ul>
 *   <li>{@code GET /}                 — the authorized launch page;</li>
 *   <li>{@code GET /token?app=&ver=}  — a fresh signed launch URL as JSON; {@code ver}
 *       is optional and defaults to the latest published version;</li>
 *   <li>{@code GET /copycat}          — a decoy page whose link has NO token.</li>
 * </ul>
 *
 * <p>What may be launched is decided by the {@link AppRepository}: the server
 * only issues a token for an app+version that actually exists there, and signs
 * that jar's real SHA-256 into the token.</p>
 */
public final class TokenHttpServer {

    private static final Pattern APP = Pattern.compile("[a-z0-9][a-z0-9-]{0,31}");
    private static final Pattern VER = Pattern.compile("[0-9][0-9A-Za-z.\\-]{0,31}");

    private final int port;
    private final AppRepository repo;
    private final LaunchTokenIssuer issuer = new LaunchTokenIssuer();

    public TokenHttpServer(int port, AppRepository repo) {
        this.port = port;
        this.repo = repo;
    }

    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/token", this::handleToken);
        server.createContext("/copycat", ex -> serveResource(ex, "/site/copycat.html", "text/html"));
        server.createContext("/", this::handleRoot);
        server.setExecutor(null);
        server.start();
        System.out.println("[token-server] authorized origin on http://localhost:" + port + "/");
        System.out.println("[token-server]   /             authorized launch page");
        System.out.println("[token-server]   /token?app=&ver=   signed launch URL (JSON); ver optional → latest");
        System.out.println("[token-server]   /copycat      tokenless decoy page (should be rejected)");
    }

    private void handleRoot(HttpExchange ex) throws IOException {
        if (!ex.getRequestURI().getPath().equals("/")) {
            respond(ex, 404, "text/plain", "Not found");
            return;
        }
        serveResource(ex, "/site/authorized.html", "text/html");
    }

    private void handleToken(HttpExchange ex) throws IOException {
        String app = queryParam(ex, "app");
        String ver = queryParam(ex, "ver");

        if (app == null || !APP.matcher(app).matches()) {
            respond(ex, 400, "application/json", "{\"error\":\"missing or invalid app\"}");
            return;
        }
        if (ver == null || ver.isBlank()) {
            Optional<String> latest = repo.latest(app);
            if (latest.isEmpty()) {
                respond(ex, 404, "application/json", "{\"error\":\"no published versions for " + app + "\"}");
                return;
            }
            ver = latest.get();
        }
        if (!VER.matcher(ver).matches()) {
            respond(ex, 400, "application/json", "{\"error\":\"invalid version\"}");
            return;
        }
        if (!repo.has(app, ver)) {
            respond(ex, 404, "application/json",
                    "{\"error\":\"" + app + " " + ver + " is not published\"}");
            return;
        }

        long now = System.currentTimeMillis() / 1000;
        String sha256 = repo.sha256(app, ver);          // hash of the exact published jar
        String token = issuer.issue(app, ver, sha256, now);
        String launchUrl = "fxsuite://launch/" + app + "?tok=" + token;

        ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        String json = "{\"url\":\"" + launchUrl + "\",\"app\":\"" + app + "\",\"ver\":\"" + ver
                + "\",\"sha256\":\"" + sha256 + "\",\"exp\":" + issuer.defaultExpiry(now) + "}";
        respond(ex, 200, "application/json", json);
    }

    // --- helpers ---------------------------------------------------------

    private static String queryParam(HttpExchange ex, String key) {
        String q = ex.getRequestURI().getRawQuery();
        if (q == null) return null;
        for (String pair : q.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && pair.substring(0, eq).equals(key)) {
                return pair.substring(eq + 1);
            }
        }
        return null;
    }

    private void serveResource(HttpExchange ex, String resource, String contentType) throws IOException {
        try (InputStream in = TokenHttpServer.class.getResourceAsStream(resource)) {
            if (in == null) {
                respond(ex, 500, "text/plain", "Missing resource: " + resource);
                return;
            }
            byte[] body = in.readAllBytes();
            ex.getResponseHeaders().add("Content-Type", contentType + "; charset=utf-8");
            ex.sendResponseHeaders(200, body.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(body);
            }
        }
    }

    private static void respond(HttpExchange ex, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", contentType + "; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}
