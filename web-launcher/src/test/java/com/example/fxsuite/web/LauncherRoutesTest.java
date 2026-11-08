package com.example.fxsuite.web;

import com.example.fxsuite.web.repo.AppRepository;

import hue.captains.singapura.tao.http.config.HostConfig;
import hue.captains.singapura.tao.http.vertx.VertxActionHost;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The dashboard as a browser sees it, against a real server on a free port. */
class LauncherRoutesTest {

    @TempDir static Path repoDir;

    private static VertxActionHost host;
    private static int port;
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @BeforeAll
    static void start() throws Exception {
        Path jar = repoDir.resolve("apps/hello/1.2.0/app-hello-1.2.0.jar");
        Files.createDirectories(jar.getParent());
        Files.writeString(jar, "stand-in jar");

        host = new VertxActionHost(new LauncherRoutes(new AppRepository(repoDir)),
                new HostConfig("127.0.0.1", 0, null));
        port = host.start().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS).actualPort();
    }

    @AfterAll
    static void stop() throws Exception {
        host.stop().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
    }

    private static HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static String contentType(HttpResponse<?> response) {
        return response.headers().firstValue("Content-Type").orElse("");
    }

    @Test
    void servesEachPageAsUtf8Html() throws Exception {
        for (String path : new String[] {"/", "/launch", "/published"}) {
            HttpResponse<String> page = get(path);
            assertEquals(200, page.statusCode(), path);
            assertEquals("text/html; charset=utf-8", contentType(page), path);
            assertTrue(page.body().contains("FxSuite · Launcher"), path + " decoded as UTF-8");
        }
    }

    @Test
    void servesTheStylesheetAndScripts() throws Exception {
        assertEquals("text/css; charset=utf-8", contentType(get("/site.css")));
        assertEquals("text/javascript; charset=utf-8", contentType(get("/launch.js")));
        assertEquals("text/javascript; charset=utf-8", contentType(get("/published.js")));
    }

    @Test
    void catalogListsWhatIsPublished() throws Exception {
        HttpResponse<String> catalog = get("/catalog");
        assertEquals(200, catalog.statusCode());
        assertTrue(contentType(catalog).startsWith("application/json"));
        assertTrue(catalog.body().contains("\"app\":\"hello\""), catalog.body());
        assertTrue(catalog.body().contains("\"ver\":\"1.2.0\""), catalog.body());
    }

    @Test
    void tokenNeedsAValidEnvironment() throws Exception {
        HttpResponse<String> token = get("/token?app=hello");
        assertEquals(400, token.statusCode());
        assertTrue(token.body().contains("missing or invalid env"), token.body());
    }

    @Test
    void tokenRefusesAVersionThatIsNotPublished() throws Exception {
        HttpResponse<String> token = get("/token?app=hello&env=dev1&ver=9.9.9");
        assertEquals(404, token.statusCode());
        assertTrue(token.body().contains("is not published"), token.body());
    }

    @Test
    void theRetiredStudioAddressesAreGone() throws Exception {
        assertEquals(404, get("/app?app=env-launch").statusCode());
    }
}
