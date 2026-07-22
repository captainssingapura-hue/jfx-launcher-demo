package com.example.fxsuite.launcher.setup;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Launch-token keys, as the setup app manages them.
 *
 * <p>Keys live under a root the operator chooses, one folder per environment:</p>
 * <pre>
 *   &lt;keysRoot&gt;/prod/signing-key.pk8.b64   private — belongs on the SERVER only
 *   &lt;keysRoot&gt;/prod/verify-key.x509.b64   public  — installed next to that environment's launcher
 * </pre>
 *
 * <p><b>The halves belong on different machines.</b> The launcher only ever needs the
 * public half; the private half is the token issuer's and must never be distributed to
 * workstations. Generating a pair is therefore an operator action, not part of a normal
 * install.</p>
 */
public final class Keys {

    public static final String PRIVATE_FILE = "signing-key.pk8.b64";
    public static final String PUBLIC_FILE = "verify-key.x509.b64";
    /** Installed beside the launcher as verify-key-<env>.x509.b64 so per-env jars can share a folder. */
    public static String installedName(String envId) { return "verify-key-" + envId + ".x509.b64"; }
    /** Where the launcher jar carries its built-in fallback anchor. */
    private static final String EMBEDDED_ENTRY = "fxsuite/launch-verify-key.x509.b64";

    private Keys() {}

    /** Which key a launcher will actually trust, and its fingerprint. */
    public record Anchor(Source source, String fingerprint) {
        enum Source { INSTALL_FOLDER, EMBEDDED_IN_JAR, NONE }

        public String describe() {
            return switch (source) {
                case INSTALL_FOLDER -> "install folder";
                case EMBEDDED_IN_JAR -> "embedded (shared default)";
                case NONE -> "none";
            };
        }
    }

    // --- generation -------------------------------------------------------

    /** Generate a fresh pair into {@code <keysRoot>/<env>/}. Returns the public fingerprint. */
    public static String generate(Path keysRoot, String envId) throws Exception {
        Path dir = keysRoot.resolve(envId);
        Files.createDirectories(dir);

        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        KeyPair kp = g.generateKeyPair();

        Files.writeString(dir.resolve(PRIVATE_FILE), b64(kp.getPrivate().getEncoded()));
        Files.writeString(dir.resolve(PUBLIC_FILE), b64(kp.getPublic().getEncoded()));
        return fingerprintOfB64(b64(kp.getPublic().getEncoded()));
    }

    /** Copy the public half into the environment's launcher install folder. */
    public static void installPublic(Path keysRoot, String envId, Path envInstallDir) throws IOException {
        Path from = keysRoot.resolve(envId).resolve(PUBLIC_FILE);
        if (!Files.isRegularFile(from)) throw new IOException("no public key at " + from);
        Files.createDirectories(envInstallDir);
        Files.copy(from, envInstallDir.resolve(installedName(envId)),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    // --- inspection -------------------------------------------------------

    /**
     * What this environment's launcher will trust: the key file beside it if present,
     * otherwise the one baked into the launcher jar — mirroring TokenVerifier's own order.
     */
    public static Anchor installedAnchor(String envId, Path envInstallDir, Path launcherJar) {
        try {
            Path f = keyBeside(envId, envInstallDir);
            if (f != null) {
                return new Anchor(Anchor.Source.INSTALL_FOLDER,
                        fingerprintOfB64(Files.readString(f, StandardCharsets.US_ASCII)));
            }
            if (launcherJar != null && Files.isRegularFile(launcherJar)) {
                try (JarFile jf = new JarFile(launcherJar.toFile())) {
                    JarEntry e = jf.getJarEntry(EMBEDDED_ENTRY);
                    if (e != null) {
                        try (InputStream in = jf.getInputStream(e)) {
                            String b64 = new String(in.readAllBytes(), StandardCharsets.US_ASCII);
                            return new Anchor(Anchor.Source.EMBEDDED_IN_JAR, fingerprintOfB64(b64));
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // fall through — report as none rather than failing the whole table
        }
        return new Anchor(Anchor.Source.NONE, "—");
    }

    /** Does the generated private key correspond to the key the launcher will trust? */
    public static boolean matches(Path keysRoot, String envId, Path envInstallDir, Path launcherJar) {
        try {
            Path priv = keysRoot.resolve(envId).resolve(PRIVATE_FILE);
            if (!Files.isRegularFile(priv)) return false;

            PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(
                    new PKCS8EncodedKeySpec(Base64.getDecoder()
                            .decode(Files.readString(priv, StandardCharsets.US_ASCII).trim())));
            PublicKey publicKey = trustedKey(envId, envInstallDir, launcherJar);
            if (publicKey == null) return false;

            byte[] nonce = new byte[32];
            new SecureRandom().nextBytes(nonce);
            Signature s = Signature.getInstance("SHA256withRSA");
            s.initSign(privateKey); s.update(nonce);
            byte[] sig = s.sign();

            Signature v = Signature.getInstance("SHA256withRSA");
            v.initVerify(publicKey); v.update(nonce);
            return v.verify(sig);
        } catch (Exception e) {
            return false;
        }
    }

    private static PublicKey trustedKey(String envId, Path envInstallDir, Path launcherJar) throws Exception {
        Path f = keyBeside(envId, envInstallDir);
        String b64 = null;
        if (f != null) {
            b64 = Files.readString(f, StandardCharsets.US_ASCII);
        } else if (launcherJar != null && Files.isRegularFile(launcherJar)) {
            try (JarFile jf = new JarFile(launcherJar.toFile())) {
                JarEntry e = jf.getJarEntry(EMBEDDED_ENTRY);
                if (e != null) try (InputStream in = jf.getInputStream(e)) {
                    b64 = new String(in.readAllBytes(), StandardCharsets.US_ASCII);
                }
            }
        }
        if (b64 == null) return null;
        return KeyFactory.getInstance("RSA").generatePublic(
                new X509EncodedKeySpec(Base64.getDecoder().decode(b64.trim())));
    }

    /** The key file beside the launcher for this env: env-specific first, then shared. */
    private static Path keyBeside(String envId, Path dir) {
        if (dir == null) return null;
        Path envSpecific = dir.resolve(installedName(envId));
        if (Files.isRegularFile(envSpecific)) return envSpecific;
        Path shared = dir.resolve(PUBLIC_FILE);
        return Files.isRegularFile(shared) ? shared : null;
    }

    /** Short SHA-256 over the key's DER bytes — enough to compare two anchors by eye. */
    public static String fingerprintOfB64(String b64) {
        try {
            byte[] der = Base64.getDecoder().decode(b64.trim());
            byte[] d = MessageDigest.getInstance("SHA-256").digest(der);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) sb.append(String.format("%02x", d[i]));
            return sb.toString();
        } catch (Exception e) {
            return "unreadable";
        }
    }

    private static String b64(byte[] der) {
        return Base64.getEncoder().encodeToString(der);
    }
}
