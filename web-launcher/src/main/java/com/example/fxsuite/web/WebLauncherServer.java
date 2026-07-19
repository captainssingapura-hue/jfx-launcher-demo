package com.example.fxsuite.web;

import com.example.fxsuite.web.repo.AppRepository;
import com.example.fxsuite.web.repo.RepoHttpServer;
import com.example.fxsuite.web.token.TokenHttpServer;

import hue.captains.singapura.js.homing.studio.base.Bootstrap;
import hue.captains.singapura.js.homing.studio.base.DefaultRuntimeParams;
import hue.captains.singapura.js.homing.studio.base.Studio;
import hue.captains.singapura.js.homing.studio.base.Umbrella;

import java.io.IOException;

/**
 * Standalone server for the FxSuite launcher demo. Runs three things in one JVM:
 *
 * <ul>
 *   <li>the homing-studio <b>dashboard</b> on port 8085 (catalogue);</li>
 *   <li>the <b>authorized token origin</b> ({@link TokenHttpServer}) on 8086, which
 *       mints version-bound signed tokens and serves the authorized/copycat pages;</li>
 *   <li>the <b>artifact repository</b> ({@link RepoHttpServer}) on 8087 — a
 *       Nexus/Artifactory stand-in the launcher downloads app versions from.</li>
 * </ul>
 *
 * <p>Repository directory comes from {@code -Dfxsuite.repo.dir} (default
 * {@code <cwd>/dist/repo}).</p>
 *
 * <pre>{@code
 * mvn -o exec:java -Dfxsuite.repo.dir=/abs/path/to/dist/repo
 * }</pre>
 */
public final class WebLauncherServer {

    static final int DASHBOARD_PORT = 8085;
    static final int TOKEN_PORT = 8086;
    static final int REPO_PORT = 8087;

    private WebLauncherServer() {}

    public static void main(String[] args) throws IOException {
        AppRepository repo = AppRepository.fromSystemProperty();

        // Artifact repository (the launcher downloads versions from here).
        new RepoHttpServer(REPO_PORT, repo.root()).start();

        // Authorized origin: version-bound signed tokens + launch/copycat pages.
        new TokenHttpServer(TOKEN_PORT, repo).start();

        // Homing-studio dashboard (blocks).
        Umbrella<Studio<?>> umbrella = new Umbrella.Solo<>(LauncherStudio.INSTANCE);
        new Bootstrap<>(
                new LauncherFixtures<>(umbrella),
                new DefaultRuntimeParams(DASHBOARD_PORT)
        ).start();
    }
}
