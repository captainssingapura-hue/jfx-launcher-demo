package com.example.fxsuite.alt.relay;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The relay/backend for the alternative architecture.
 *
 * <p>Agents hold a standing, authenticated WebSocket connection (port 8091). The
 * web page calls {@code POST /launch} on the HTTP trigger (port 8090); the relay
 * validates it, RSA-signs a launch command, and PUSHES it to that user's agent
 * over the socket. The browser never contacts the agent — so a copycat site has
 * no local endpoint to forge against.</p>
 *
 * <p>PoC simplifications (production notes): agent auth is a shared token (→ real
 * auth / mTLS); the transport is plain {@code ws://} on localhost (→ {@code wss://}
 * on 443); the server key is ephemeral and delivered on connect / TOFU (→ pinned
 * cert). The signed-command design is the real, keepable part.</p>
 */
public final class RelayServer {

    static final int WEB_PORT = 8090;
    static final int WS_PORT = 8091;
    static final String SHARED_TOKEN = "poc-secret";   // PoC agent auth
    static final long COMMAND_TTL_SECONDS = 60;

    private static final Pattern SAFE = Pattern.compile("[a-z0-9][a-z0-9.\\-]{0,31}");

    /** user -> their connected agent (last one wins for the PoC). */
    private final ConcurrentHashMap<String, WsConnection> agents = new ConcurrentHashMap<>();
    /** user -> last heartbeat JSON reported by their agent (live inventory). */
    private final ConcurrentHashMap<String, String> heartbeats = new ConcurrentHashMap<>();
    private final KeyPair keys;
    private final String publicKeyB64;

    /** Published-artifact repo dir, shared with the top-level demo's dist/repo. */
    private final Path repoDir = Path.of(System.getProperty("fxsuite.repo.dir", "dist/repo"))
            .toAbsolutePath().normalize();

    private RelayServer() throws GeneralSecurityException {
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        this.keys = g.generateKeyPair();
        this.publicKeyB64 = Base64.getEncoder().encodeToString(keys.getPublic().getEncoded());
    }

    public static void main(String[] args) throws Exception {
        new RelayServer().start();
        Thread.currentThread().join();   // keep the JVM alive
    }

    private void start() throws IOException {
        new WsServer(WS_PORT, new AgentHandler()).start();
        startWebTrigger();
        System.out.println("[relay] http on http://localhost:" + WEB_PORT + "/   (repo: " + repoDir + ")");
        System.out.println("[relay]   variant 1 (push):  GET /   +   POST /launch?user=&app=&ver=");
        System.out.println("[relay]   variant 2 (pull):  GET /catalog   +   GET /repo/apps/<app>/<ver>/<jar>");
    }

    // --- agent WebSocket side -------------------------------------------

    private final class AgentHandler implements WsServer.Handler {
        @Override public void onConnect(WsConnection c) throws IOException {
            String user = c.query.get("user");
            String token = c.query.get("token");
            if (user == null || !SHARED_TOKEN.equals(token)) {
                c.sendText("{\"type\":\"denied\",\"reason\":\"bad auth\"}");
                c.close();
                return;
            }
            agents.put(user, c);
            // Hand the agent our public key so it can verify pushed commands.
            c.sendText("{\"type\":\"welcome\",\"user\":\"" + user + "\",\"pubkey\":\"" + publicKeyB64 + "\"}");
            System.out.println("[relay] agent connected: user=" + user + " (total " + agents.size() + ")");
        }

        @Override public void onMessage(WsConnection c, String message) {
            String user = c.query.get("user");
            if (user != null && message.contains("\"type\":\"heartbeat\"")) {
                heartbeats.put(user, message);   // live inventory of what this agent is running
            } else {
                System.out.println("[relay] <- agent: " + message);   // acks / status
            }
        }

        @Override public void onClose(WsConnection c) {
            String user = c.query.get("user");
            agents.values().remove(c);
            if (user != null) heartbeats.remove(user);
            System.out.println("[relay] agent disconnected (total " + agents.size() + ")");
        }
    }

    // --- web trigger side -----------------------------------------------

    private void startWebTrigger() throws IOException {
        HttpServer http = HttpServer.create(new InetSocketAddress(WEB_PORT), 0);
        http.createContext("/launch", this::handleLaunch);       // variant 1 (push)
        http.createContext("/catalog", this::handleCatalog);     // variant 2 (pull)
        http.createContext("/repo", this::handleRepo);           // variant 2 (pull) — artifacts
        http.createContext("/ops/agents", this::handleOpsAgents);// ops: live inventory (monitor)
        http.createContext("/ops/kill", this::handleOpsKill);    // ops: remote kill (control)
        http.createContext("/ops", ex -> {                        // ops: dashboard
            if (!ex.getRequestURI().getPath().equals("/ops")) { respond(ex, 404, "text/plain", "Not found"); return; }
            serveResource(ex, "/altsite/ops.html");
        });
        http.createContext("/", ex -> {
            if (!ex.getRequestURI().getPath().equals("/")) { respond(ex, 404, "text/plain", "Not found"); return; }
            serveResource(ex, "/altsite/index.html");
        });
        http.setExecutor(null);
        http.start();
    }

    private void handleLaunch(HttpExchange ex) throws IOException {
        String user = param(ex, "user");
        String app = param(ex, "app");
        String ver = param(ex, "ver");
        if (user == null || app == null || ver == null
                || !SAFE.matcher(app).matches() || !SAFE.matcher(ver).matches()) {
            respond(ex, 400, "application/json", "{\"error\":\"missing/invalid user, app or ver\"}");
            return;
        }
        WsConnection agent = agents.get(user);
        if (agent == null) {
            respond(ex, 409, "application/json", "{\"error\":\"no agent connected for " + user + "\"}");
            return;
        }
        Path jar = repoDir.resolve("apps").resolve(app).resolve(ver).resolve("app-" + app + "-" + ver + ".jar");
        if (!Files.isRegularFile(jar)) {
            respond(ex, 404, "application/json", "{\"error\":\"" + app + " " + ver + " not published\"}");
            return;
        }
        try {
            String command = signedLaunchCommand(app, ver, sha256Hex(jar));
            agent.sendText(command);
            System.out.println("[relay] -> pushed launch to " + user + ": " + app + " " + ver);
            respond(ex, 200, "application/json", "{\"pushed\":true,\"app\":\"" + app + "\",\"ver\":\"" + ver + "\"}");
        } catch (IOException e) {
            respond(ex, 502, "application/json", "{\"error\":\"agent send failed\"}");
        }
    }

    /** Build a launch command JSON with an RSA signature over its canonical form. */
    private String signedLaunchCommand(String app, String ver, String sha256) {
        long iat = System.currentTimeMillis() / 1000;
        long exp = iat + COMMAND_TTL_SECONDS;
        String nonce = UUID.randomUUID().toString();
        String canonical = "launch|" + app + "|" + ver + "|" + sha256 + "|" + nonce + "|" + iat + "|" + exp;
        String sig = sign(canonical);
        return "{\"type\":\"launch\",\"app\":\"" + app + "\",\"ver\":\"" + ver + "\",\"sha256\":\"" + sha256 + "\","
                + "\"nonce\":\"" + nonce + "\",\"iat\":" + iat + ",\"exp\":" + exp + ","
                + "\"sig\":\"" + sig + "\"}";
    }

    /** Build a signed control command (e.g. remote kill of a running app). */
    private String signedKillCommand(String launchId) {
        long iat = System.currentTimeMillis() / 1000;
        long exp = iat + COMMAND_TTL_SECONDS;
        String nonce = UUID.randomUUID().toString();
        String canonical = "kill|" + launchId + "|" + nonce + "|" + iat + "|" + exp;
        String sig = sign(canonical);
        return "{\"type\":\"kill\",\"launchId\":\"" + launchId + "\","
                + "\"nonce\":\"" + nonce + "\",\"iat\":" + iat + ",\"exp\":" + exp + ","
                + "\"sig\":\"" + sig + "\"}";
    }

    private String sign(String canonical) {
        try {
            Signature s = Signature.getInstance("SHA256withRSA");
            s.initSign(keys.getPrivate());
            s.update(canonical.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(s.sign());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    // --- variant 2 (pull): catalog + artifact serving -------------------

    /** List apps + versions + each jar's SHA-256, scanned from the repo dir. */
    private void handleCatalog(HttpExchange ex) throws IOException {
        respond(ex, 200, "application/json", catalogJson());
    }

    private String catalogJson() throws IOException {
        // Flat item list, easy for the launcher to parse:
        // {"items":[{"app":"hello","ver":"1.0.0","sha256":"…"}, …]}
        Path appsDir = repoDir.resolve("apps");
        StringBuilder sb = new StringBuilder("{\"items\":[");
        boolean first = true;
        if (Files.isDirectory(appsDir)) {
            for (Path appDir : sortedDirs(appsDir)) {
                String app = appDir.getFileName().toString();
                for (Path verDir : sortedDirs(appDir)) {
                    String ver = verDir.getFileName().toString();
                    Path jar = verDir.resolve("app-" + app + "-" + ver + ".jar");
                    if (!Files.isRegularFile(jar)) continue;
                    if (!first) sb.append(',');
                    first = false;
                    sb.append("{\"app\":\"").append(app).append("\",\"ver\":\"").append(ver)
                      .append("\",\"sha256\":\"").append(sha256Hex(jar)).append("\"}");
                }
            }
        }
        return sb.append("]}").toString();
    }

    /** Serve an artifact from the repo dir: GET /repo/apps/<app>/<ver>/<jar>. */
    private void handleRepo(HttpExchange ex) throws IOException {
        String rel = ex.getRequestURI().getPath().substring("/repo".length());  // /apps/...
        Path target = repoDir.resolve("." + rel).normalize();
        if (!target.startsWith(repoDir) || !Files.isRegularFile(target)) {
            respond(ex, 404, "text/plain", "Not found");
            return;
        }
        byte[] bytes = Files.readAllBytes(target);
        ex.getResponseHeaders().add("Content-Type", "application/java-archive");
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private static List<Path> sortedDirs(Path dir) throws IOException {
        List<Path> out = new ArrayList<>();
        try (Stream<Path> s = Files.list(dir)) {
            s.filter(Files::isDirectory).sorted().forEach(out::add);
        }
        return out;
    }

    private static String sha256Hex(Path file) throws IOException {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file));
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) { sb.append(Character.forDigit((b >> 4) & 0xf, 16)); sb.append(Character.forDigit(b & 0xf, 16)); }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    // --- variant 1 ops plane: monitor + control -------------------------

    /** Live inventory: every connected agent and what it reported running. */
    private void handleOpsAgents(HttpExchange ex) throws IOException {
        StringBuilder sb = new StringBuilder("{\"agents\":[");
        boolean first = true;
        for (String user : agents.keySet()) {
            if (!first) sb.append(',');
            first = false;
            String hb = heartbeats.get(user);
            if (hb != null) {
                sb.append(hb);                          // heartbeat already carries user + apps
            } else {
                sb.append("{\"user\":\"").append(user).append("\",\"apps\":[]}");   // connected, no heartbeat yet
            }
        }
        respond(ex, 200, "application/json", sb.append("]}").toString());
    }

    /** Remote kill: push a signed kill command to the user's agent. */
    private void handleOpsKill(HttpExchange ex) throws IOException {
        String user = param(ex, "user");
        String launchId = param(ex, "launchId");
        if (user == null || launchId == null) {
            respond(ex, 400, "application/json", "{\"error\":\"need user and launchId\"}");
            return;
        }
        WsConnection agent = agents.get(user);
        if (agent == null) {
            respond(ex, 409, "application/json", "{\"error\":\"no agent connected for " + user + "\"}");
            return;
        }
        try {
            agent.sendText(signedKillCommand(launchId));
            System.out.println("[relay] -> pushed KILL to " + user + ": launchId=" + launchId);
            respond(ex, 200, "application/json", "{\"killed\":\"" + launchId + "\"}");
        } catch (IOException e) {
            respond(ex, 502, "application/json", "{\"error\":\"agent send failed\"}");
        }
    }

    // --- http helpers ----------------------------------------------------

    private static String param(HttpExchange ex, String key) {
        String q = ex.getRequestURI().getRawQuery();
        if (q == null) return null;
        for (String pair : q.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && pair.substring(0, eq).equals(key)) return pair.substring(eq + 1);
        }
        return null;
    }

    private void serveResource(HttpExchange ex, String resource) throws IOException {
        try (InputStream in = RelayServer.class.getResourceAsStream(resource)) {
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
