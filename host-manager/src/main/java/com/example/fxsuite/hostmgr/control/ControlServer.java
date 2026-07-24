package com.example.fxsuite.hostmgr.control;

import com.example.fxsuite.hostmgr.json.Json;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The centralized control plane.
 *
 * <p>Operators declare intent in a plain file ({@code desired.json}); the server reads it on
 * each request and hands agents a <b>signed</b> copy. Editing that file is the entire
 * deployment gesture — bump a version, and every host converges on its next tick. Agents
 * report their actual state to {@code /report}, and {@code /ops} shows the fleet: what each
 * host is running, at what version, and whether it has caught up to the current generation.</p>
 *
 * <p>Endpoints:</p>
 * <ul>
 *   <li>{@code GET /pubkey} — the key agents pin to verify manifests;</li>
 *   <li>{@code GET /desired?host=<id>} — the signed desired-state envelope;</li>
 *   <li>{@code POST /report} — an agent's actual state;</li>
 *   <li>{@code GET /ops} + {@code GET /ops/state} — the fleet dashboard and its data.</li>
 * </ul>
 *
 * <p>PoC simplifications (same as the relay's): the signing key is generated on start and
 * delivered over plain HTTP on localhost; production pins a key out of band and uses TLS.</p>
 */
public final class ControlServer {

    private final int port;
    private final Path desiredFile;
    private final KeyPair keys;
    private final String publicKeyB64;

    /** host id -> the last actual-state report it posted. */
    private final Map<String, String> reports = new ConcurrentHashMap<>();

    public ControlServer(int port, Path desiredFile) {
        this.port = port;
        this.desiredFile = desiredFile;
        try {
            KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
            g.initialize(2048);
            this.keys = g.generateKeyPair();
            this.publicKeyB64 = Base64.getEncoder().encodeToString(keys.getPublic().getEncoded());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void start() throws IOException {
        HttpServer http = HttpServer.create(new InetSocketAddress(port), 0);
        http.createContext("/pubkey", this::handlePubkey);
        http.createContext("/desired", this::handleDesired);
        http.createContext("/report", this::handleReport);
        http.createContext("/ops/state", this::handleOpsState);
        http.createContext("/ops", ex -> {
            if (!ex.getRequestURI().getPath().equals("/ops")) { respond(ex, 404, "text/plain", "Not found"); return; }
            serveResource(ex, "/hostmgr/ops.html");
        });
        http.createContext("/", ex -> respond(ex, 404, "text/plain", "Not found"));
        http.setExecutor(null);
        http.start();
        System.out.println("[control] http://localhost:" + port + "/ops   (desired: " + desiredFile + ")");
        System.out.println("[control]   GET /desired?host=<id>  ·  POST /report  ·  GET /pubkey");
    }

    private void handlePubkey(HttpExchange ex) throws IOException {
        respond(ex, 200, "application/json", "{\"pubkey\":" + Json.quote(publicKeyB64) + "}");
    }

    /** Read the declared state, sign the exact bytes, return the envelope. */
    private void handleDesired(HttpExchange ex) throws IOException {
        if (!Files.isRegularFile(desiredFile)) {
            respond(ex, 404, "application/json", "{\"error\":\"no desired.json\"}");
            return;
        }
        byte[] payload = Files.readAllBytes(desiredFile);
        String sig = Signing.sign(payload, keys.getPrivate());
        String envelope = "{\"payload\":" + Json.quote(Base64.getUrlEncoder().withoutPadding().encodeToString(payload))
                + ",\"sig\":" + Json.quote(sig) + ",\"alg\":\"SHA256withRSA\"}";
        respond(ex, 200, "application/json", envelope);
    }

    private void handleReport(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) { respond(ex, 405, "text/plain", "POST only"); return; }
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        try {
            String host = Json.str(Json.object(Json.parse(body)), "host");
            if (host != null) {
                reports.put(host, body);
                System.out.println("[control] report from " + host + ": " + body);
            }
            respond(ex, 200, "application/json", "{\"ok\":true}");
        } catch (Exception e) {
            respond(ex, 400, "application/json", "{\"error\":\"bad report\"}");
        }
    }

    private void handleOpsState(HttpExchange ex) throws IOException {
        long generation = 0;
        try {
            if (Files.isRegularFile(desiredFile)) {
                generation = Json.lng(Json.object(Json.parse(Files.readString(desiredFile))), "generation", 0);
            }
        } catch (Exception ignored) {}

        StringBuilder sb = new StringBuilder("{\"generation\":").append(generation).append(",\"hosts\":[");
        boolean first = true;
        for (String host : reports.keySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append(reports.get(host));      // each report is already a JSON object
        }
        respond(ex, 200, "application/json", sb.append("]}").toString());
    }

    // ── http helpers ─────────────────────────────────────────────────────

    private void serveResource(HttpExchange ex, String resource) throws IOException {
        try (InputStream in = ControlServer.class.getResourceAsStream(resource)) {
            if (in == null) { respond(ex, 500, "text/plain", "missing " + resource); return; }
            byte[] body = in.readAllBytes();
            ex.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            ex.sendResponseHeaders(200, body.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(body); }
        }
    }

    private static void respond(HttpExchange ex, int status, String type, String body) throws IOException {
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", type + "; charset=utf-8");
        ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(status, b.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(b); }
    }
}
