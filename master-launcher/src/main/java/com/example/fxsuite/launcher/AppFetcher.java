package com.example.fxsuite.launcher;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

/**
 * Resolves an app+version to a local jar, downloading from the pinned repo on a
 * cache miss and verifying integrity against the token's signed {@code sha256}.
 *
 * <p>This is what makes the app catalog <b>dynamic</b>: the launcher ships no app
 * jars. The server rolls a version forward simply by minting tokens for it; the
 * launcher fetches that version on first use and caches it for next time.</p>
 *
 * <p>Cache: {@code %LOCALAPPDATA%\fxsuite\cache\<app>\<ver>\app-<app>-<ver>.jar}.
 * Every launch re-hashes the resolved jar against the signed hash, so a cached
 * file that no longer matches is re-fetched rather than trusted.</p>
 */
public final class AppFetcher {

    private final EnvConfig env;

    public AppFetcher(EnvConfig env) {
        this.env = env;
    }

    /**
     * @return path to a verified local jar whose SHA-256 equals {@code expectedSha256}
     * @throws LaunchException on download failure or integrity mismatch
     */
    public Path fetch(String app, String ver, String expectedSha256) throws LaunchException {
        // Cache is keyed by environment: the same app+version in Prod and dev-2 are
        // separate files and can never be confused.
        Path cacheJar = Install.cacheRoot(env.envId())
                .resolve(app).resolve(ver).resolve("app-" + app + "-" + ver + ".jar");

        // Cache hit only if the bytes still match the signed hash.
        try {
            if (Files.isRegularFile(cacheJar) && Hashing.sha256Hex(cacheJar).equals(expectedSha256)) {
                DiagLog.log("[fetch] cache hit: " + app + " " + ver);
                return cacheJar;
            }
        } catch (IOException e) {
            DiagLog.log("[fetch] could not read cached jar, will re-download: " + e.getMessage());
        }

        URI src = env.artifactUri(app, ver);
        DiagLog.log("[fetch] downloading " + src);
        byte[] bytes = download(src);

        String actual = Hashing.sha256Hex(bytes);
        if (!actual.equals(expectedSha256)) {
            throw new LaunchException("Integrity check FAILED for " + app + " " + ver
                    + " — downloaded bytes do not match the signed hash. Refusing to launch.");
        }

        try {
            Files.createDirectories(cacheJar.getParent());
            Path part = cacheJar.resolveSibling(cacheJar.getFileName() + ".part");
            Files.write(part, bytes);
            Files.move(part, cacheJar,
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new LaunchException("Could not write app to cache: " + e.getMessage());
        }
        DiagLog.log("[fetch] verified + cached: " + cacheJar);
        return cacheJar;
    }

    private static byte[] download(URI src) throws LaunchException {
        // No redirect following — the repo host is pinned; a redirect could point
        // off-host. Fail closed on anything but a 200.
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        HttpRequest req = HttpRequest.newBuilder(src)
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        try {
            HttpResponse<byte[]> resp = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() != 200) {
                throw new LaunchException("Repository returned HTTP " + resp.statusCode()
                        + " for " + src);
            }
            return resp.body();
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new LaunchException("Could not download app from repository: " + e.getMessage());
        }
    }
}
