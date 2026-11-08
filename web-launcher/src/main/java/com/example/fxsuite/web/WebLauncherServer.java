package com.example.fxsuite.web;

import com.example.fxsuite.web.repo.AppRepository;
import com.example.fxsuite.web.repo.RepoHttpServer;
import com.example.fxsuite.web.token.TokenHttpServer;

import hue.captains.singapura.tao.http.config.HostConfig;
import hue.captains.singapura.tao.http.vertx.VertxActionHost;

import java.io.IOException;

/**
 * Standalone server for the FxSuite launcher demo. Runs three things in one JVM:
 *
 * <ul>
 *   <li>the <b>dashboard</b> on port 8085: the pages and the token/catalogue API
 *       ({@link LauncherRoutes}), on one origin;</li>
 *   <li>a <b>decoy origin</b> ({@link TokenHttpServer}) on 8086, a different website that
 *       emits the same link without a valid token;</li>
 *   <li>the <b>artifact repository</b> ({@link RepoHttpServer}) on 8087, a
 *       Nexus/Artifactory stand-in the launcher downloads app versions from.</li>
 * </ul>
 *
 * <p>Repository directory comes from {@code -Dfxsuite.repo.dir} (default
 * {@code <cwd>/dist/repo}).</p>
 *
 * <pre>{@code
 * mvn exec:java -Dfxsuite.repo.dir=/abs/path/to/dist/repo
 * }</pre>
 */
public final class WebLauncherServer {

    static final int DASHBOARD_PORT = 8085;
    static final int TOKEN_PORT = 8086;
    static final int REPO_PORT = 8087;

    /**
     * The dashboard mints launch tokens, so it listens on this machine only. A shared
     * deployment would put it behind authentication rather than widen this.
     */
    static final String DASHBOARD_HOST = "127.0.0.1";

    private WebLauncherServer() {}

    public static void main(String[] args) throws IOException {
        AppRepository repo = AppRepository.fromSystemProperty();

        // Artifact repository (the launcher downloads versions from here).
        new RepoHttpServer(REPO_PORT, repo.root()).start();

        // A separate origin hosting only the copycat decoy.
        new TokenHttpServer(TOKEN_PORT).start();

        new VertxActionHost(new LauncherRoutes(repo), new HostConfig(DASHBOARD_HOST, DASHBOARD_PORT, null))
                .start()
                .onSuccess(server -> System.out.println(
                        "[dashboard] http://localhost:" + server.actualPort() + "/"))
                .onFailure(e -> {
                    System.err.println("[dashboard] could not start on port " + DASHBOARD_PORT
                            + ": " + e.getMessage());
                    System.exit(1);
                });
    }
}
