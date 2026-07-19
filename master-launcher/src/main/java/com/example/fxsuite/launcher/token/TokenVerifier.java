package com.example.fxsuite.launcher.token;

import com.example.fxsuite.launcher.LaunchException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Verifies a compact-JWS launch token against the launcher's embedded public key.
 *
 * <p>This is the security gate. Because <i>any</i> web page, email, or local file
 * can craft an {@code fxsuite://} URL, a bare or copied URL is not enough: the
 * launcher only proceeds if the URL carries a token that</p>
 * <ol>
 *   <li>is a well-formed RS256 JWS,</li>
 *   <li>has a signature that verifies against our public key — i.e. it was minted
 *       by the FxSuite backend, which alone holds the private key,</li>
 *   <li>names the expected issuer and audience,</li>
 *   <li>is bound to the exact app id being launched, and</li>
 *   <li>has not expired.</li>
 * </ol>
 *
 * <p>A copycat site that hardcodes {@code fxsuite://launch/hello} cannot produce a
 * valid signature, so its launch is refused.</p>
 */
public final class TokenVerifier {

    /** Must match the issuer's constants. */
    public static final String EXPECTED_ISS = "fxsuite-web";
    public static final String EXPECTED_AUD = "fxsuite-launcher";
    private static final String ALG = "RS256";

    /** Tolerated clock skew, seconds. */
    private static final long LEEWAY = 30;

    /** Accepted version strings (digits, dot, hyphen — e.g. 1.2.0, 1.2.0-rc1). */
    private static final Pattern VERSION = Pattern.compile("[0-9][0-9A-Za-z.\\-]{0,31}");
    /** Lowercase hex SHA-256. */
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");

    private static final String PUBLIC_KEY_RESOURCE = "/fxsuite/launch-verify-key.x509.b64";

    private final PublicKey publicKey;
    private final java.util.function.LongSupplier clockSeconds;

    public TokenVerifier() {
        this(loadEmbeddedKey(), () -> System.currentTimeMillis() / 1000);
    }

    /** For tests: inject a key and a fixed clock. */
    public TokenVerifier(PublicKey publicKey, java.util.function.LongSupplier clockSeconds) {
        this.publicKey = publicKey;
        this.clockSeconds = clockSeconds;
    }

    /**
     * @param compactJws the raw token from the URL's {@code tok} query param
     * @param expectedApp the app id parsed from the URL path
     * @return the validated claims
     * @throws LaunchException with a user-safe reason if verification fails
     */
    public LaunchToken verify(String compactJws, String expectedApp) throws LaunchException {
        if (compactJws == null || compactJws.isBlank()) {
            throw new LaunchException("This link is missing a launch token. "
                    + "Launches must come from the FxSuite site.");
        }

        String[] parts = compactJws.split("\\.");
        if (parts.length != 3) {
            throw new LaunchException("Malformed launch token.");
        }
        String headerJson = decodeToString(parts[0]);
        String payloadJson = decodeToString(parts[1]);

        // Reject anything that is not our exact algorithm — closes the "alg:none"
        // and algorithm-substitution downgrade attacks.
        String alg = extractString(headerJson, "alg");
        if (!ALG.equals(alg)) {
            throw new LaunchException("Unsupported token algorithm: " + alg);
        }

        // Signature covers the ASCII of "<header>.<payload>".
        String signingInput = parts[0] + "." + parts[1];
        if (!signatureValid(signingInput, parts[2])) {
            throw new LaunchException("Launch token signature is invalid — "
                    + "this link was not issued by FxSuite.");
        }

        String iss = extractString(payloadJson, "iss");
        String aud = extractString(payloadJson, "aud");
        String app = extractString(payloadJson, "app");
        String ver = extractString(payloadJson, "ver");
        String sha256 = extractString(payloadJson, "sha256");
        Long iat = extractLong(payloadJson, "iat");
        Long exp = extractLong(payloadJson, "exp");
        String jti = extractString(payloadJson, "jti");

        if (!EXPECTED_ISS.equals(iss)) throw new LaunchException("Untrusted token issuer.");
        if (!EXPECTED_AUD.equals(aud)) throw new LaunchException("Token not intended for this launcher.");
        if (app == null || ver == null || sha256 == null || iat == null || exp == null) {
            throw new LaunchException("Launch token is missing required claims.");
        }
        if (!app.equals(expectedApp)) {
            throw new LaunchException("Token authorizes app '" + app
                    + "' but the link asked for '" + expectedApp + "'.");
        }
        if (!VERSION.matcher(ver).matches()) {
            throw new LaunchException("Illegal version in token: '" + ver + "'");
        }
        if (!SHA256.matcher(sha256).matches()) {
            throw new LaunchException("Illegal artifact hash in token.");
        }

        long now = clockSeconds.getAsLong();
        if (now > exp + LEEWAY) {
            throw new LaunchException("Launch token has expired. Reopen the app from the FxSuite site.");
        }
        if (now + LEEWAY < iat) {
            throw new LaunchException("Launch token is not yet valid (clock skew?).");
        }

        return new LaunchToken(iss, aud, app, ver, sha256, iat, exp, jti);
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

    // The payload is a flat object of constrained string/number values (no nested
    // objects, no quotes/backslashes inside values), so targeted extraction is
    // safe and keeps the launcher dependency-free.
    private static String extractString(String json, String key) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }

    private static Long extractLong(String json, String key) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(\\d+)").matcher(json);
        return m.find() ? Long.parseLong(m.group(1)) : null;
    }

    private static PublicKey loadEmbeddedKey() {
        try (InputStream in = TokenVerifier.class.getResourceAsStream(PUBLIC_KEY_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Missing embedded public key: " + PUBLIC_KEY_RESOURCE);
            }
            String b64 = new String(in.readAllBytes(), StandardCharsets.US_ASCII).trim();
            byte[] der = Base64.getDecoder().decode(b64);
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalStateException("Could not load launcher public key", e);
        }
    }
}
