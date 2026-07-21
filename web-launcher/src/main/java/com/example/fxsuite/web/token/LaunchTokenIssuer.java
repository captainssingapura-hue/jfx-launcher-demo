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
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mints short-lived, environment- app- and version-bound RS256 launch tokens.
 *
 * <p><b>Per-environment signing.</b> A key is looked up as
 * {@code /fxsuite/launch-signing-key-<env>.pk8.b64}, falling back to the shared
 * {@code /fxsuite/launch-signing-key.pk8.b64}. That gives singleton environments
 * (Production, UAT) their own trust root — a token minted for one cannot verify at
 * another — while multiplexed dev environments share a signer and are separated by
 * the {@code env} claim instead.</p>
 */
public final class LaunchTokenIssuer {

    public static final String ISS = "fxsuite-web";
    public static final String AUD = "fxsuite-launcher";
    public static final long DEFAULT_TTL_SECONDS = 120;

    private static final String SHARED_KEY = "/fxsuite/launch-signing-key.pk8.b64";
    private static final String ENV_KEY = "/fxsuite/launch-signing-key-%s.pk8.b64";

    private final ConcurrentHashMap<String, PrivateKey> keys = new ConcurrentHashMap<>();

    public long defaultExpiry(long nowSeconds) {
        return nowSeconds + DEFAULT_TTL_SECONDS;
    }

    /** True if this environment has its own dedicated signing key (vs the shared one). */
    public boolean hasDedicatedKey(String env) {
        return LaunchTokenIssuer.class.getResource(String.format(ENV_KEY, env)) != null;
    }

    /** Issue a token authorizing exactly {@code app}@{@code ver} in {@code env}. */
    public String issue(String env, String app, String ver, String sha256Hex, long nowSeconds) {
        long exp = nowSeconds + DEFAULT_TTL_SECONDS;
        String jti = UUID.randomUUID().toString();

        String header = b64url("{\"alg\":\"RS256\",\"typ\":\"JWT\"}");
        String payload = b64url(String.format(
                "{\"iss\":\"%s\",\"aud\":\"%s\",\"env\":\"%s\",\"app\":\"%s\",\"ver\":\"%s\","
                        + "\"sha256\":\"%s\",\"iat\":%d,\"exp\":%d,\"jti\":\"%s\"}",
                ISS, AUD, env, app, ver, sha256Hex, nowSeconds, exp, jti));
        String signingInput = header + "." + payload;
        return signingInput + "." + sign(env, signingInput);
    }

    private String sign(String env, String signingInput) {
        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(keyFor(env));
            sig.update(signingInput.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(sig.sign());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to sign launch token", e);
        }
    }

    private PrivateKey keyFor(String env) {
        return keys.computeIfAbsent(env, e -> {
            PrivateKey k = load(String.format(ENV_KEY, e));
            return k != null ? k : load(SHARED_KEY);
        });
    }

    private static PrivateKey load(String resource) {
        try (InputStream in = LaunchTokenIssuer.class.getResourceAsStream(resource)) {
            if (in == null) return null;
            byte[] der = Base64.getDecoder()
                    .decode(new String(in.readAllBytes(), StandardCharsets.US_ASCII).trim());
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not load signing key " + resource, e);
        }
    }

    private static String b64url(String s) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }
}
