package com.example.fxsuite.web.repo;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A minimal static file server standing in for Nexus/Artifactory. Serves the
 * repository directory read-only, e.g.
 * {@code GET /apps/hello/1.0.0/app-hello-1.0.0.jar}.
 *
 * <p>Path traversal is refused: the resolved file must stay inside the repo root.</p>
 */
public final class RepoHttpServer {

    private final int port;
    private final Path root;

    public RepoHttpServer(int port, Path root) {
        this.port = port;
        this.root = root.toAbsolutePath().normalize();
    }

    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", this::serve);
        server.setExecutor(null);
        server.start();
        System.out.println("[repo-server] artifact repository on http://localhost:" + port
                + "/  (root: " + root + ")");
    }

    private void serve(HttpExchange ex) throws IOException {
        String rawPath = ex.getRequestURI().getPath();
        // Normalize and confine to the repo root — defeats ../ traversal.
        Path target = root.resolve("." + rawPath).normalize();
        if (!target.startsWith(root) || !Files.isRegularFile(target)) {
            byte[] body = "Not found".getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(404, body.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(body); }
            return;
        }
        byte[] bytes = Files.readAllBytes(target);
        ex.getResponseHeaders().add("Content-Type", "application/java-archive");
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }
}
