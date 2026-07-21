package com.example.fxsuite.web.token;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * A deliberately <b>separate origin</b> hosting the copycat decoy.
 *
 * <p>The token and catalogue APIs used to live here; they now belong to the studio
 * itself (contributed via {@code Fixtures.harnessGetActions()}), so the UI and its API
 * share one origin and no CORS is needed.</p>
 *
 * <p>What remains is only the decoy page — and it stays on its own port <i>on purpose</i>:
 * its whole point is to be a different website emitting the same {@code fxsuite-<env>://}
 * URL without a valid token. Serving it from the studio would undercut what it
 * demonstrates.</p>
 */
public final class TokenHttpServer {

    private final int port;

    public TokenHttpServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/copycat", ex -> serveResource(ex, "/site/copycat.html"));
        server.createContext("/", this::handleRoot);
        server.setExecutor(null);
        server.start();
        System.out.println("[decoy-origin] http://localhost:" + port + "/copycat"
                + "   (a different site — no token, should be rejected)");
    }

    private void handleRoot(HttpExchange ex) throws IOException {
        // The "/" context matches every unmatched path, so anything that is not the
        // root — including the retired /token and /catalog — must 404 rather than
        // silently serve this page with a 200.
        if (!ex.getRequestURI().getPath().equals("/")) {
            respond(ex, 404, "text/plain", "Not found — this origin only hosts /copycat.");
            return;
        }
        respond(ex, 200, "text/html",
                "<!doctype html><meta charset=utf-8><title>Another website</title>"
                + "<body style=\"font:16px system-ui,sans-serif;margin:6vh auto;max-width:640px\">"
                + "<h2>A different origin</h2><p>This host exists only to prove that a site which "
                + "is not FxSuite cannot launch anything. See <a href=\"/copycat\">/copycat</a>.</p>"
                + "<p>The real launcher UI and its API live in the studio: "
                + "<a href=\"http://localhost:8085/\">localhost:8085</a>.</p>");
    }

    private void serveResource(HttpExchange ex, String resource) throws IOException {
        try (InputStream in = TokenHttpServer.class.getResourceAsStream(resource)) {
            if (in == null) { respond(ex, 500, "text/plain", "missing " + resource); return; }
            byte[] body = in.readAllBytes();
            ex.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            ex.sendResponseHeaders(200, body.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(body); }
        }
    }

    private static void respond(HttpExchange ex, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", contentType + "; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }
}
