package com.example.fxsuite.web.repo;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The published-artifact repository (a stand-in for Nexus/Artifactory), backed
 * by a directory:
 *
 * <pre>
 *   &lt;root&gt;/apps/&lt;app&gt;/&lt;ver&gt;/app-&lt;app&gt;-&lt;ver&gt;.jar
 * </pre>
 *
 * <p>The web backend uses this to discover versions and to compute the SHA-256
 * it signs into a launch token. The same directory is what {@link RepoHttpServer}
 * serves to the launcher — so the hash the server signs and the bytes the
 * launcher downloads are guaranteed to be the same file.</p>
 */
public record AppRepository(Path root) {

    /** Repo directory from {@code -Dfxsuite.repo.dir}, else {@code <cwd>/dist/repo}. */
    public static AppRepository fromSystemProperty() {
        String dir = System.getProperty("fxsuite.repo.dir");
        Path root = (dir != null && !dir.isBlank())
                ? Path.of(dir)
                : Path.of(System.getProperty("user.dir"), "dist", "repo");
        return new AppRepository(root.toAbsolutePath().normalize());
    }

    public Path jar(String app, String ver) {
        return root.resolve("apps").resolve(app).resolve(ver).resolve("app-" + app + "-" + ver + ".jar");
    }

    public boolean has(String app, String ver) {
        return Files.isRegularFile(jar(app, ver));
    }

    public String sha256(String app, String ver) {
        try {
            byte[] bytes = Files.readAllBytes(jar(app, ver));
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(bytes);
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) {
                sb.append(Character.forDigit((b >> 4) & 0xf, 16));
                sb.append(Character.forDigit(b & 0xf, 16));
            }
            return sb.toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** All published versions of an app, newest first. */
    public List<String> versions(String app) {
        Path appDir = root.resolve("apps").resolve(app);
        if (!Files.isDirectory(appDir)) return List.of();
        try (Stream<Path> s = Files.list(appDir)) {
            return s.filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .filter(v -> has(app, v))
                    .sorted(VERSION_DESC)
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Optional<String> latest(String app) {
        return versions(app).stream().findFirst();
    }

    /** Newest-first ordering by dotted numeric segments (1.10.0 &gt; 1.9.0). */
    private static final Comparator<String> VERSION_DESC = (a, b) -> compareVersions(b, a);

    private static int compareVersions(String a, String b) {
        String[] pa = a.split("[.\\-]");
        String[] pb = b.split("[.\\-]");
        int n = Math.max(pa.length, pb.length);
        for (int i = 0; i < n; i++) {
            long va = numericOrZero(i < pa.length ? pa[i] : "0");
            long vb = numericOrZero(i < pb.length ? pb[i] : "0");
            if (va != vb) return Long.compare(va, vb);
        }
        return a.compareTo(b);
    }

    private static long numericOrZero(String s) {
        try { return Long.parseLong(s); } catch (NumberFormatException e) { return 0; }
    }
}
