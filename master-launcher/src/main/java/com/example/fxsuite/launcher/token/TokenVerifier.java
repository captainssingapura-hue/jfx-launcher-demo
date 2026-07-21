package com.example.fxsuite.launcher.token;

import com.example.fxsuite.launcher.DiagLog;
import com.example.fxsuite.launcher.Install;
import com.example.fxsuite.launcher.LaunchException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Verifies a compact-JWS launch token against this environment's public key.
 *
 * <p>This is the security gate. Because any web page can craft a {@code fxsuite-<env>://}
 * URL, a bare or copied URL is not enough: the launcher only proceeds if the token</p>
 * <ol>
 *   <li>is a well-formed RS256 JWS,</li>
 *   <li>verifies against <b>this environment's</b> public key,</li>
 *   <li>names the expected issuer and audience,</li>
 *   <li>is bound to <b>this environment</b> ({@code env} claim),</li>
 *   <li>is bound to the app being launched, carries a valid version + artifact hash, and</li>
 *   <li>has not expired.</li>
 * </ol>
 *
 * <p><b>Per-environment trust.</b> The key is read from {@code verify-key.x509.b64} in the
 * launcher's install directory when present, else from the embedded resource. Giving each
 * environment its own install (and key) means a Production launcher cannot be driven by a
 * token minted for any other environment — its signature simply will not verify.</p>
 */
public final class TokenVerifier {

    public static final String EXPECTED_ISS = "fxsuite-web";
    public static final String EXPECTED_AUD = "fxsuite-launcher";
    private static final String ALG = "RS256";

    /** Tolerated clock skew, seconds. */
    private static final long LEEWAY = 30;

    private static final Pattern VERSION = Pattern.compile("[0-9][0-9A-Za-z.\\-]{0,31}");
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");
    private static final Pattern ENV = Pattern.compile("[a-z0-9][a-z0-9-]{0,31}");

    private static final String EMBEDDED_KEY_RESOURCE = "/fxsuite/launch-verify-key.x509.b64";

    private final PublicKey publicKey;
    private final java.util.function.LongSupplier clockSeconds;

    public TokenVerifier() {
        this(loadKey(), () -> System.currentTimeMillis() / 1000);
    }

    /** For tests: inject a key and a fixed clock. */
    public TokenVerifier(PublicKey publicKey, java.util.function.LongSupplier clockSeconds) {
        this.publicKey = publicKey;
        this.clockSeconds = clockSeconds;
    }

    /**
     * @param compactJws  raw token from the URL's {@code tok} query param
     * @param expectedEnv this launcher's own environment id
     * @param expectedApp the app id parsed from the URL path
     * @throws LaunchException with a user-safe reason if verification fails
     */
    public LaunchToken verify(String compactJws, String expectedEnv, String expectedApp)
            throws LaunchException {
        if (compactJws == null || compactJws.isBlank()) {
            throw new LaunchException("This link is missing a launch token. "
                    + "Launches must come from the FxSuite site.");
        }

        String[] parts = compactJws.split("\\.");
        if (parts.length != 3) throw new LaunchException("Malformed launch token.");

        String headerJson = decodeToString(parts[0]);
        String payloadJson = decodeToString(parts[1]);

        // Reject anything that is not our exact algorithm — closes "alg:none" and
        // algorithm-substitution downgrades.
        String alg = extractString(headerJson, "alg");
        if (!ALG.equals(alg)) throw new LaunchException("Unsupported token algorithm: " + alg);

        if (!signatureValid(parts[0] + "." + parts[1], parts[2])) {
            throw new LaunchException("Launch token signature is invalid for environment '"
                    + expectedEnv + "' — it was not issued for this environment.");
        }

        String iss = extractString(payloadJson, "iss");
        String aud = extractString(payloadJson, "aud");
        String env = extractString(payloadJson, "env");
        String app = extractString(payloadJson, "app");
        String ver = extractString(payloadJson, "ver");
        String sha256 = extractString(payloadJson, "sha256");
        Long iat = extractLong(payloadJson, "iat");
        Long exp = extractLong(payloadJson, "exp");
        String jti = extractString(payloadJson, "jti");

        if (!EXPECTED_ISS.equals(iss)) throw new LaunchException("Untrusted token issuer.");
        if (!EXPECTED_AUD.equals(aud)) throw new LaunchException("Token not intended for this launcher.");
        if (env == null || app == null || ver == null || sha256 == null || iat == null || exp == null) {
            throw new LaunchException("Launch token is missing required claims.");
        }
        if (!ENV.matcher(env).matches()) throw new LaunchException("Illegal environment in token: '" + env + "'");

        // The decisive multi-environment check. Singleton environments are already
        // separated by key; multiplexed dev environments share a signer, so this claim
        // is the only thing stopping a dev-1 token being replayed at dev-2.
        if (!env.equals(expectedEnv)) {
            throw new LaunchException("Token is for environment '" + env
                    + "' but this launcher serves '" + expectedEnv + "'.");
        }
        if (!app.equals(expectedApp)) {
            throw new LaunchException("Token authorizes app '" + app
                    + "' but the link asked for '" + expectedApp + "'.");
        }
        if (!VERSION.matcher(ver).matches()) throw new LaunchException("Illegal version in token: '" + ver + "'");
        if (!SHA256.matcher(sha256).matches()) throw new LaunchException("Illegal artifact hash in token.");

        long now = clockSeconds.getAsLong();
        if (now > exp + LEEWAY) {
            throw new LaunchException("Launch token has expired. Reopen the app from the FxSuite site.");
        }
        if (now + LEEWAY < iat) {
            throw new LaunchException("Launch token is not yet valid (clock skew?).");
        }

        return new LaunchToken(iss, aud, env, app, ver, sha256, iat, exp, jti);
    }

    private boolean signatureValid(String signingInput, String signaturePart) throws LaunchException {
        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(publicKey);
            sig.update(signingInput.getBytes(StandardCharsets.US_ASCII));
            return sig.verify(base64Url(signaturePart));
        } catch (GeneralSecurityException e) {
            throw new LaunchException("Could not verify token signature.");
        }
    }

    // --- helpers ---------------------------------------------------------

    private static String decodeToString(String b64url) {
        return new String(base64Url(b64url), StandardCharsets.UTF_8);
    }

    private static byte[] base64Url(String s) {
        return Base64.getUrlDecoder().decode(s);
    }

    private static String extractString(String json, String key) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }

    private static Long extractLong(String json, String key) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(\\d+)").matcher(json);
        return m.find() ? Long.parseLong(m.group(1)) : null;
    }

    /** Per-environment key file next to the jar, else the embedded default. */
    private static PublicKey loadKey() {
        try {
            Path f = Install.verifyKeyFile();
            if (Files.isRegularFile(f)) {
                return fromBase64(Files.readString(f, StandardCharsets.US_ASCII));
            }
        } catch (LaunchException | IOException | GeneralSecurityException e) {
            DiagLog.log("[trust] could not read per-environment key, falling back: " + e.getMessage());
        }
        try (InputStream in = TokenVerifier.class.getResourceAsStream(EMBEDDED_KEY_RESOURCE)) {
            if (in == null) throw new IllegalStateException("Missing embedded public key");
            return fromBase64(new String(in.readAllBytes(), StandardCharsets.US_ASCII));
        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalStateException("Could not load launcher public key", e);
        }
    }

    private static PublicKey fromBase64(String b64) throws GeneralSecurityException {
        byte[] der = Base64.getDecoder().decode(b64.trim());
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
    }
}
