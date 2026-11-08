package com.example.fxsuite.hostmgr;

import com.example.fxsuite.hostmgr.model.AppSpec;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.regex.Pattern;

/**
 * Turns a declared {@link AppSpec} into a concrete, verified jar on disk.
 *
 * <p>Layout mirrors the systemd upgrade model even under the direct supervisor, so the two
 * lanes stage identically:</p>
 * <pre>
 *   &lt;appsRoot&gt;/&lt;app&gt;/versions/&lt;ver&gt;/app.jar
 * </pre>
 *
 * <p>A version already present and matching its pinned hash is reused untouched — so an
 * upgrade downloads once and a rollback re-downloads nothing. The hash is checked before the
 * bytes are ever placed at their final path (download to {@code .part}, verify, atomic move),
 * so a half-written or substituted artifact can never be launched.</p>
 */
public final class Artifacts {

    private static final Pattern SAFE = Pattern.compile("[a-z0-9][a-z0-9.\\-]{0,64}");
    private static final Pattern SHA = Pattern.compile("[0-9a-f]{64}");

    private final Path appsRoot;
    private final String repoBase;
    private final HttpClient http = HttpClient.newHttpClient();

    public Artifacts(Path appsRoot, String repoBase) {
        this.appsRoot = appsRoot;
        this.repoBase = repoBase.endsWith("/") ? repoBase.substring(0, repoBase.length() - 1) : repoBase;
    }

    /** The versioned path an app's jar lives at once staged. */
    public Path versionedJar(String app, String ver) {
        return appsRoot.resolve(app).resolve("versions").resolve(ver).resolve("app.jar");
    }

    /**
     * Ensure the spec's exact artifact is present and hash-verified, returning its path.
     *
     * @throws IOException if the download fails or the bytes do not match the pinned hash
     */
    public Path stage(AppSpec spec) throws IOException, InterruptedException {
        if (!SAFE.matcher(spec.appId()).matches() || !SAFE.matcher(spec.version()).matches()
                || spec.sha256() == null || !SHA.matcher(spec.sha256()).matches()) {
            throw new IOException("refusing to stage malformed spec: " + spec.appId() + " " + spec.version());
        }
        Path jar = versionedJar(spec.appId(), spec.version());
        if (Files.isRegularFile(jar) && sha256Hex(Files.readAllBytes(jar)).equals(spec.sha256())) {
            return jar;                                  // already have this exact build
        }

        String url = repoBase + "/repo/apps/" + spec.appId() + "/" + spec.version()
                + "/app-" + spec.appId() + "-" + spec.version() + ".jar";
        HttpResponse<byte[]> r = http.send(HttpRequest.newBuilder(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());
        if (r.statusCode() != 200) throw new IOException("HTTP " + r.statusCode() + " fetching " + url);

        String got = sha256Hex(r.body());
        if (!got.equals(spec.sha256())) {
            throw new IOException("hash mismatch for " + spec.appId() + " " + spec.version()
                    + ": pinned " + spec.sha256() + " but downloaded " + got);
        }

        Files.createDirectories(jar.getParent());
        Path part = jar.resolveSibling("app.jar.part");
        Files.write(part, r.body());
        Files.move(part, jar, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        return jar;
    }

    static String sha256Hex(byte[] data) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(data);
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) {
                sb.append(Character.forDigit((b >> 4) & 0xf, 16));
                sb.append(Character.forDigit(b & 0xf, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
