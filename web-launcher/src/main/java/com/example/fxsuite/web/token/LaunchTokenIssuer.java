package com.example.fxsuite.web.token;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

/**
 * Mints short-lived, app- and version-bound, RS256-signed launch tokens.
 *
 * <p>This is the trust anchor: only this component (on the server) holds the
 * private key. The token carries the app id, the exact version, and the SHA-256
 * of that version's jar — so the launcher can download the version on demand and
 * verify the bytes. What apps/versions exist is decided by the repository, not
 * hardcoded here; this class just signs what it is handed.</p>
 */
public final class LaunchTokenIssuer {

    public static final String ISS = "fxsuite-web";
    public static final String AUD = "fxsuite-launcher";

    /** Deliberately short: a leaked token is useless within a minute or two. */
    public static final long DEFAULT_TTL_SECONDS = 120;

    private static final String PRIVATE_KEY_RESOURCE = "/fxsuite/launch-signing-key.pk8.b64";

    private final PrivateKey privateKey;

    public LaunchTokenIssuer() {
        this.privateKey = loadEmbeddedKey();
    }

    public long defaultExpiry(long nowSeconds) {
        return nowSeconds + DEFAULT_TTL_SECONDS;
    }

    /** Issue a compact-JWS token authorizing exactly {@code app}@{@code ver} with the given jar hash. */
    public String issue(String app, String ver, String sha256Hex, long nowSeconds) {
        long exp = nowSeconds + DEFAULT_TTL_SECONDS;
        String jti = UUID.randomUUID().toString();

        String header = b64url("{\"alg\":\"RS256\",\"typ\":\"JWT\"}");
        String payload = b64url(String.format(
                "{\"iss\":\"%s\",\"aud\":\"%s\",\"app\":\"%s\",\"ver\":\"%s\",\"sha256\":\"%s\","
                        + "\"iat\":%d,\"exp\":%d,\"jti\":\"%s\"}",
                ISS, AUD, app, ver, sha256Hex, nowSeconds, exp, jti));
        String signingInput = header + "." + payload;
        return signingInput + "." + sign(signingInput);
    }

    private String sign(String signingInput) {
        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(privateKey);
            sig.update(signingInput.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(sig.sign());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to sign launch token", e);
        }
    }

    private static String b64url(String s) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    private static PrivateKey loadEmbeddedKey() {
        try (InputStream in = LaunchTokenIssuer.class.getResourceAsStream(PRIVATE_KEY_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Missing signing key: " + PRIVATE_KEY_RESOURCE);
            }
            String b64 = new String(in.readAllBytes(), StandardCharsets.US_ASCII).trim();
            byte[] der = Base64.getDecoder().decode(b64);
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not load signing key", e);
        }
    }
}
