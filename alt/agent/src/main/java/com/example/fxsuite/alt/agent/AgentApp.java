package com.example.fxsuite.alt.agent;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

/**
 * The managed agent (variant 1). Holds a WebSocket to the relay and turns it into
 * a control + observability channel for Production Support:
 *
 * <ul>
 *   <li>launches are pushed as RSA-signed commands; the agent verifies them, then
 *       fetches + hash-verifies + spawns the app as a real process;</li>
 *   <li>every few seconds it heartbeats its <b>live inventory</b> (what it is
 *       running, versions, pids, uptime) to the relay;</li>
 *   <li>it obeys signed <b>control</b> commands — e.g. remote kill of a running app.</li>
 * </ul>
 *
 * <p>So even though the apps run locally, ops can see and control them centrally.</p>
 */
public final class AgentApp extends Application {

    private static final long CLOCK_LEEWAY = 60;
    private static final Pattern SAFE = Pattern.compile("[a-z0-9][a-z0-9.\\-]{0,31}");
    private static final Pattern SHA = Pattern.compile("[0-9a-f]{64}");

    private volatile PublicKey serverKey;
    private volatile WebSocket ws;
    private String user = "alice";
    private String wsBase = "ws://localhost:8091";
    private String httpBase = "http://localhost:8090";
    private String host = "unknown";

    private final HttpClient http = HttpClient.newHttpClient();
    private final ScheduledExecutorService beat = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "heartbeat"); t.setDaemon(true); return t;
    });

    /** launchId (command nonce) -> the running process we spawned. */
    private final ConcurrentHashMap<String, Proc> running = new ConcurrentHashMap<>();
    private record Proc(Process process, String app, String ver, long startMs) {}

    private Label status;
    private Label runningLabel;
    private TextArea logView;
    private final StringBuilder rx = new StringBuilder();

    @Override
    public void start(Stage stage) {
        List<String> args = getParameters().getRaw();
        if (args.size() > 0) user = args.get(0);
        if (args.size() > 1) wsBase = args.get(1);
        if (args.size() > 2) httpBase = args.get(2);
        try { host = InetAddress.getLocalHost().getHostName(); } catch (Exception ignored) {}

        Label title = new Label("FxSuite Agent (managed)");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        title.setTextFill(Color.web("#1a3a8f"));
        status = new Label("Connecting to relay…");
        status.setFont(Font.font("Segoe UI", 14));
        runningLabel = new Label("running apps: 0");
        runningLabel.setFont(Font.font("Consolas", 13));
        logView = new TextArea(); logView.setEditable(false);
        logView.setFont(Font.font("Consolas", 12)); logView.setPrefRowCount(12);

        VBox root = new VBox(10, title, new Label("user: " + user + " @ " + host),
                status, runningLabel, logView);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: linear-gradient(to bottom right,#e7edff,#ffffff);");
        stage.setTitle("FxSuite Agent — " + user);
        stage.setScene(new Scene(root, 640, 420));
        stage.show();

        connect();
        beat.scheduleAtFixedRate(this::heartbeat, 2, 4, TimeUnit.SECONDS);
    }

    private void connect() {
        URI uri = URI.create(wsBase + "/agent?user=" + user + "&token=poc-secret");
        log("connecting: " + uri);
        http.newWebSocketBuilder().buildAsync(uri, new RelayListener())
            .whenComplete((w, err) -> {
                if (err != null) { setStatus("Connection failed: " + err.getMessage(), "#b71c1c"); log("connect error: " + err); }
            });
    }

    private final class RelayListener implements WebSocket.Listener {
        @Override public void onOpen(WebSocket w) {
            ws = w; w.request(1);
            setStatus("Connected — awaiting server key…", "#1a3a8f"); log("websocket open");
        }
        @Override public CompletionStage<?> onText(WebSocket w, CharSequence data, boolean last) {
            rx.append(data);
            if (last) { String m = rx.toString(); rx.setLength(0); handle(m); }
            w.request(1); return null;
        }
        @Override public CompletionStage<?> onClose(WebSocket w, int code, String reason) {
            ws = null; setStatus("Disconnected (" + code + ")", "#b71c1c"); return null;
        }
        @Override public void onError(WebSocket w, Throwable t) {
            ws = null; setStatus("Error: " + t.getMessage(), "#b71c1c"); log("error: " + t);
        }
    }

    private void handle(String msg) {
        switch (String.valueOf(str(msg, "type"))) {
            case "welcome" -> {
                try {
                    serverKey = KeyFactory.getInstance("RSA").generatePublic(
                            new X509EncodedKeySpec(Base64.getDecoder().decode(str(msg, "pubkey"))));
                    setStatus("Connected — server key pinned. Managed by ops.", "#2e7d32");
                    log("welcome: server public key pinned");
                } catch (Exception e) { setStatus("Bad server key", "#b71c1c"); }
            }
            case "launch" -> verifyAndLaunch(msg);
            case "kill" -> verifyAndKill(msg);
            case "denied" -> setStatus("Relay denied the connection", "#b71c1c");
            default -> log("unknown message: " + msg);
        }
    }

    // --- launch (fetch → verify → spawn → track) ------------------------

    private void verifyAndLaunch(String msg) {
        String app = str(msg, "app"), ver = str(msg, "ver"), sha = str(msg, "sha256");
        String nonce = str(msg, "nonce"); Long iat = num(msg, "iat"), exp = num(msg, "exp");
        String sig = str(msg, "sig");
        if (serverKey == null) { log("REJECT launch: no server key yet"); return; }
        if (app == null || ver == null || sha == null || nonce == null || iat == null || exp == null || sig == null
                || !SAFE.matcher(app).matches() || !SAFE.matcher(ver).matches() || !SHA.matcher(sha).matches()) {
            log("REJECT launch: malformed"); return;
        }
        long now = System.currentTimeMillis() / 1000;
        if (now > exp + CLOCK_LEEWAY) { log("REJECT launch: expired"); return; }
        if (!sigValid("launch|" + app + "|" + ver + "|" + sha + "|" + nonce + "|" + iat + "|" + exp, sig)) {
            log("REJECT launch: BAD SIGNATURE — " + app + " " + ver); return;
        }
        new Thread(() -> spawn(app, ver, sha, nonce), "launch-" + app).start();
    }

    private void spawn(String app, String ver, String expectedSha, String launchId) {
        try {
            String url = httpBase + "/repo/apps/" + app + "/" + ver + "/app-" + app + "-" + ver + ".jar";
            log("downloading " + app + " " + ver + " …");
            byte[] bytes = httpGetBytes(url);
            if (!sha256Hex(bytes).equals(expectedSha)) { log("REJECT " + app + " " + ver + ": hash mismatch"); return; }

            Path jar = cacheFile(app, ver);
            Files.createDirectories(jar.getParent());
            Path part = jar.resolveSibling(jar.getFileName() + ".part");
            Files.write(part, bytes);
            Files.move(part, jar, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);

            String mainClass = mainClassOf(jar);
            String javaw = Path.of(System.getProperty("java.home"), "bin", "javaw.exe").toString();
            String cp = jar + File.pathSeparator + sharedJar();
            ProcessBuilder pb = new ProcessBuilder(javaw, "--enable-native-access=ALL-UNNAMED",
                    "-cp", cp, mainClass, app, ver);
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile().toFile()));
            Process p = pb.start();
            running.put(launchId, new Proc(p, app, ver, System.currentTimeMillis()));
            log("spawned " + app + " " + ver + " (pid " + p.pid() + ", launchId " + launchId.substring(0, 8) + "…)");
            updateRunning();
            heartbeat();   // report immediately so ops sees it
        } catch (Exception e) {
            log("launch failed for " + app + " " + ver + ": " + e.getMessage());
        }
    }

    // --- control (verify → kill) ----------------------------------------

    private void verifyAndKill(String msg) {
        String launchId = str(msg, "launchId"), nonce = str(msg, "nonce"), sig = str(msg, "sig");
        Long iat = num(msg, "iat"), exp = num(msg, "exp");
        if (serverKey == null || launchId == null || nonce == null || iat == null || exp == null || sig == null) {
            log("REJECT kill: malformed"); return;
        }
        long now = System.currentTimeMillis() / 1000;
        if (now > exp + CLOCK_LEEWAY) { log("REJECT kill: expired"); return; }
        if (!sigValid("kill|" + launchId + "|" + nonce + "|" + iat + "|" + exp, sig)) {
            log("REJECT kill: BAD SIGNATURE"); return;
        }
        Proc proc = running.remove(launchId);
        if (proc == null) { log("kill: no running app with launchId " + launchId); return; }
        proc.process().destroy();
        log("KILLED (ops command): " + proc.app() + " " + proc.ver() + " pid " + proc.process().pid());
        updateRunning();
        heartbeat();
    }

    // --- heartbeat (live inventory) -------------------------------------

    private void heartbeat() {
        WebSocket w = ws;
        if (w == null) return;
        running.values().removeIf(pr -> !pr.process().isAlive());   // prune exited apps
        updateRunning();

        StringBuilder apps = new StringBuilder();
        boolean first = true;
        long now = System.currentTimeMillis();
        for (var e : running.entrySet()) {
            Proc pr = e.getValue();
            if (!first) apps.append(',');
            first = false;
            apps.append("{\"launchId\":\"").append(e.getKey()).append("\",\"app\":\"").append(pr.app())
                .append("\",\"ver\":\"").append(pr.ver()).append("\",\"pid\":").append(pr.process().pid())
                .append(",\"uptimeS\":").append((now - pr.startMs()) / 1000).append('}');
        }
        String hb = "{\"type\":\"heartbeat\",\"user\":\"" + user + "\",\"host\":\"" + host + "\",\"apps\":["
                + apps + "]}";
        try { w.sendText(hb, true); } catch (Exception ignored) {}
    }

    private void updateRunning() {
        int n = running.size();
        Platform.runLater(() -> { if (runningLabel != null) runningLabel.setText("running apps: " + n); });
    }

    // --- helpers ---------------------------------------------------------

    private Path sharedJar() {
        return Path.of(System.getProperty("fxsuite.shared.jar",
                "fxsuite-javafx/target/fxsuite-javafx.jar")).toAbsolutePath();
    }

    private Path cacheFile(String app, String ver) {
        String base = System.getenv().getOrDefault("LOCALAPPDATA", System.getProperty("java.io.tmpdir"));
        return Path.of(base, "fxsuite", "altagentcache", app, ver, "app-" + app + "-" + ver + ".jar");
    }

    private boolean sigValid(String canonical, String sigB64Url) {
        try {
            Signature s = Signature.getInstance("SHA256withRSA");
            s.initVerify(serverKey);
            s.update(canonical.getBytes(StandardCharsets.UTF_8));
            return s.verify(Base64.getUrlDecoder().decode(sigB64Url));
        } catch (Exception e) { return false; }
    }

    private byte[] httpGetBytes(String url) throws IOException, InterruptedException {
        HttpResponse<byte[]> r = http.send(HttpRequest.newBuilder(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());
        if (r.statusCode() != 200) throw new IOException("HTTP " + r.statusCode() + " for " + url);
        return r.body();
    }

    private static String mainClassOf(Path jar) throws IOException {
        try (JarFile jf = new JarFile(jar.toFile())) {
            Manifest mf = jf.getManifest();
            String m = mf == null ? null : mf.getMainAttributes().getValue("Main-Class");
            if (m == null || m.isBlank()) throw new IOException("no Main-Class");
            return m.trim();
        }
    }

    private static String sha256Hex(byte[] data) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(data);
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) { sb.append(Character.forDigit((b >> 4) & 0xf, 16)); sb.append(Character.forDigit(b & 0xf, 16)); }
            return sb.toString();
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    private static Path logFile() {
        String base = System.getenv().getOrDefault("LOCALAPPDATA", System.getProperty("java.io.tmpdir"));
        return Path.of(base, "fxsuite", "agent.log");
    }

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private void setStatus(String text, String color) {
        Platform.runLater(() -> { if (status != null) { status.setText(text); status.setTextFill(Color.web(color)); } });
    }

    private void log(String line) {
        String entry = TS.format(LocalTime.now()) + "  " + line;
        System.out.println(entry);
        try {
            Path f = logFile(); Files.createDirectories(f.getParent());
            Files.writeString(f, entry + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {}
        Platform.runLater(() -> { if (logView != null) logView.appendText(entry + "\n"); });
    }

    private static String str(String json, String key) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }
    private static Long num(String json, String key) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(\\d+)").matcher(json);
        return m.find() ? Long.parseLong(m.group(1)) : null;
    }
}
