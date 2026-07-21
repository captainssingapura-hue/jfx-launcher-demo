package com.example.fxsuite.alt.local;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

/**
 * Variant 2 — the local launcher does everything locally.
 *
 * <p>On start it pulls the catalogue from the backend and lists apps/versions in
 * its own JavaFX UI. Clicking Launch downloads that version, verifies its SHA-256
 * against the catalogue, caches it, and spawns it as a separate process sharing
 * the one JavaFX jar. There is no browser and no server-push in the loop.</p>
 */
public final class LocalLauncherApp extends Application {

    private static final Pattern ITEM = Pattern.compile(
            "\\{\"app\":\"([^\"]+)\",\"ver\":\"([^\"]+)\",\"sha256\":\"([0-9a-f]{64})\"}");

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    private String repoBase = System.getProperty("fxsuite.repo.base", "http://localhost:8090");
    private Path sharedJar = Path.of(System.getProperty(
            "fxsuite.shared.jar", "fxsuite-javafx/target/fxsuite-javafx.jar")).toAbsolutePath();

    private Label status;
    private VBox catalogBox;
    private TextArea logView;

    private record Item(String app, String ver, String sha256) {}

    @Override
    public void start(Stage stage) {
        Label title = new Label("FxSuite Launcher");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        title.setTextFill(Color.web("#1a3a8f"));

        status = new Label("Loading catalogue…");
        status.setFont(Font.font("Segoe UI", 14));

        catalogBox = new VBox(8);
        catalogBox.setPadding(new Insets(6, 0, 6, 0));

        Button refresh = new Button("Refresh");
        refresh.setOnAction(e -> fetchCatalog());

        logView = new TextArea();
        logView.setEditable(false);
        logView.setFont(Font.font("Consolas", 12));
        logView.setPrefRowCount(8);

        VBox root = new VBox(12, title,
                new Label("backend: " + repoBase),
                status, catalogBox, refresh, new Label("Activity:"), logView);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: linear-gradient(to bottom right,#eef2ff,#ffffff);");

        stage.setTitle("FxSuite Launcher (local)");
        stage.setScene(new Scene(root, 560, 520));
        stage.show();

        if (!Files.isRegularFile(sharedJar)) {
            log("WARNING: shared JavaFX jar not found at " + sharedJar
                    + " — set -Dfxsuite.shared.jar");
        }
        fetchCatalog();
    }

    // --- catalogue -------------------------------------------------------

    private void fetchCatalog() {
        setStatus("Loading catalogue from " + repoBase + "…", "#1a3a8f");
        new Thread(() -> {
            try {
                String json = httpGetString(repoBase + "/catalog");
                List<Item> items = parseItems(json);
                Platform.runLater(() -> showCatalogue(items));
            } catch (Exception e) {
                setStatus("Could not load catalogue: " + e.getMessage(), "#b71c1c");
                log("catalogue error: " + e);
            }
        }, "catalogue-fetch").start();
    }

    private void showCatalogue(List<Item> items) {
        catalogBox.getChildren().clear();
        if (items.isEmpty()) {
            setStatus("Catalogue is empty (no apps published).", "#b71c1c");
            return;
        }
        setStatus("Choose an app to launch:", "#2e7d32");

        // Optional deep-launch / test hook: -Dfxsuite.autolaunch=app:ver
        String auto = System.getProperty("fxsuite.autolaunch");
        if (auto != null && auto.contains(":")) {
            String app = auto.substring(0, auto.indexOf(':'));
            String ver = auto.substring(auto.indexOf(':') + 1);
            items.stream().filter(i -> i.app().equals(app) && i.ver().equals(ver)).findFirst()
                    .ifPresent(i -> new Thread(() -> launch(i), "autolaunch").start());
        }

        for (Item it : items) {
            Label name = new Label(it.app() + "   v" + it.ver());
            name.setFont(Font.font("Segoe UI", 15));
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Button launch = new Button("Launch");
            launch.setStyle("-fx-background-color:#2748c8; -fx-text-fill:white; -fx-font-weight:bold;");
            launch.setOnAction(e -> new Thread(() -> launch(it), "launch-" + it.app()).start());
            HBox row = new HBox(10, name, spacer, launch);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(6, 10, 6, 10));
            row.setStyle("-fx-background-color:#ffffff; -fx-border-color:#cdd7f0; -fx-background-radius:8; -fx-border-radius:8;");
            catalogBox.getChildren().add(row);
        }
    }

    // --- fetch → verify → spawn -----------------------------------------

    private void launch(Item it) {
        try {
            log("downloading " + it.app() + " " + it.ver() + " …");
            String url = repoBase + "/repo/apps/" + it.app() + "/" + it.ver()
                    + "/app-" + it.app() + "-" + it.ver() + ".jar";
            byte[] bytes = httpGetBytes(url);

            String actual = sha256Hex(bytes);
            if (!actual.equals(it.sha256())) {
                log("REJECT " + it.app() + " " + it.ver() + ": SHA-256 mismatch — not launching.");
                setStatus("Integrity check failed for " + it.app() + " " + it.ver(), "#b71c1c");
                return;
            }

            Path jar = cacheFile(it);
            Files.createDirectories(jar.getParent());
            Path part = jar.resolveSibling(jar.getFileName() + ".part");
            Files.write(part, bytes);
            Files.move(part, jar, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            log("verified + cached: " + jar.getFileName());

            String mainClass = mainClassOf(jar);
            spawn(jar, mainClass, it);
        } catch (Exception e) {
            log("launch failed for " + it.app() + " " + it.ver() + ": " + e.getMessage());
            setStatus("Launch failed: " + e.getMessage(), "#b71c1c");
        }
    }

    private void spawn(Path appJar, String mainClass, Item it) throws IOException {
        String javaw = Path.of(System.getProperty("java.home"), "bin", "javaw.exe").toString();
        String cp = appJar + java.io.File.pathSeparator + sharedJar;
        List<String> cmd = List.of(javaw, "--enable-native-access=ALL-UNNAMED",
                "-cp", cp, mainClass, it.app(), it.ver());
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile().toFile()));
        Process p = pb.start();
        log("spawned " + it.app() + " " + it.ver() + " (pid " + p.pid() + ", shared JavaFX jar)");
        setStatus("Launched " + it.app() + " v" + it.ver() + " ✓", "#2e7d32");
    }

    private Path cacheFile(Item it) {
        String base = System.getenv().getOrDefault("LOCALAPPDATA", System.getProperty("java.io.tmpdir"));
        return Path.of(base, "fxsuite", "altcache", it.app(), it.ver(),
                "app-" + it.app() + "-" + it.ver() + ".jar");
    }

    // --- helpers ---------------------------------------------------------

    private static List<Item> parseItems(String json) {
        List<Item> out = new ArrayList<>();
        Matcher m = ITEM.matcher(json);
        while (m.find()) out.add(new Item(m.group(1), m.group(2), m.group(3)));
        return out;
    }

    private String httpGetString(String url) throws IOException, InterruptedException {
        HttpResponse<String> r = http.send(HttpRequest.newBuilder(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        if (r.statusCode() != 200) throw new IOException("HTTP " + r.statusCode() + " for " + url);
        return r.body();
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
            String main = mf == null ? null : mf.getMainAttributes().getValue("Main-Class");
            if (main == null || main.isBlank()) throw new IOException("no Main-Class in " + jar.getFileName());
            return main.trim();
        }
    }

    private static String sha256Hex(byte[] data) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(data);
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) { sb.append(Character.forDigit((b >> 4) & 0xf, 16)); sb.append(Character.forDigit(b & 0xf, 16)); }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Path logFile() {
        String base = System.getenv().getOrDefault("LOCALAPPDATA", System.getProperty("java.io.tmpdir"));
        return Path.of(base, "fxsuite", "local-launcher.log");
    }

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private void setStatus(String text, String color) {
        Platform.runLater(() -> { if (status != null) { status.setText(text); status.setTextFill(Color.web(color)); } });
    }

    private void log(String line) {
        String entry = TS.format(LocalTime.now()) + "  " + line;
        System.out.println(entry);
        try {
            Path f = logFile();
            Files.createDirectories(f.getParent());
            Files.writeString(f, entry + System.lineSeparator(), StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException ignored) {}
        Platform.runLater(() -> { if (logView != null) logView.appendText(entry + "\n"); });
    }
}
