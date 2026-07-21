package com.example.fxsuite.launcher.token;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.fxsuite.launcher.LaunchException;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Exercises the full RS256 verification path with ephemeral keypairs, including the
 * multi-environment checks: a token must be signed by <i>this</i> environment's key
 * <b>and</b> carry a matching {@code env} claim.
 */
class TokenVerifierTest {

    private static KeyPair envKeys;      // this launcher's environment
    private static KeyPair otherKeys;    // a different environment (e.g. Prod vs UAT)
    private static final long NOW = 1_000_000L;
    private static final String SHA = "a".repeat(64);

    @BeforeAll
    static void genKeys() throws Exception {
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        envKeys = g.generateKeyPair();
        otherKeys = g.generateKeyPair();
    }

    private TokenVerifier verifier() {
        return new TokenVerifier(envKeys.getPublic(), () -> NOW);
    }

    private static String claims(String env, String app, String ver, String sha, long iat, long exp) {
        return "{\"iss\":\"fxsuite-web\",\"aud\":\"fxsuite-launcher\",\"env\":\"" + env + "\","
                + "\"app\":\"" + app + "\",\"ver\":\"" + ver + "\",\"sha256\":\"" + sha + "\","
                + "\"iat\":" + iat + ",\"exp\":" + exp + ",\"jti\":\"abc123\"}";
    }

    @Test
    void acceptsValidToken() throws Exception {
        String tok = mint(envKeys, claims("uat", "hello", "1.2.0", SHA, NOW, NOW + 60));
        LaunchToken t = verifier().verify(tok, "uat", "hello");
        assertEquals("uat", t.env());
        assertEquals("hello", t.app());
        assertEquals("1.2.0", t.ver());
        assertEquals(SHA, t.sha256());
    }

    /** Multiplexed dev environments share a signer — the env claim is what separates them. */
    @Test
    void rejectsTokenForAnotherEnvironmentSignedBySameKey() throws Exception {
        String tok = mint(envKeys, claims("dev1", "hello", "1.2.0", SHA, NOW, NOW + 60));
        assertThrows(LaunchException.class, () -> verifier().verify(tok, "dev2", "hello"));
    }

    /** Singleton environments have their own key — a foreign token fails on signature. */
    @Test
    void rejectsTokenSignedByAnotherEnvironmentsKey() throws Exception {
        String tok = mint(otherKeys, claims("uat", "hello", "1.2.0", SHA, NOW, NOW + 60));
        assertThrows(LaunchException.class, () -> verifier().verify(tok, "uat", "hello"));
    }

    @Test
    void rejectsMissingEnvClaim() throws Exception {
        String tok = mint(envKeys, "{\"iss\":\"fxsuite-web\",\"aud\":\"fxsuite-launcher\","
                + "\"app\":\"hello\",\"ver\":\"1.2.0\",\"sha256\":\"" + SHA + "\","
                + "\"iat\":" + NOW + ",\"exp\":" + (NOW + 60) + ",\"jti\":\"x\"}");
        assertThrows(LaunchException.class, () -> verifier().verify(tok, "uat", "hello"));
    }

    @Test
    void rejectsMissingToken() {
        assertThrows(LaunchException.class, () -> verifier().verify(null, "uat", "hello"));
        assertThrows(LaunchException.class, () -> verifier().verify("", "uat", "hello"));
    }

    @Test
    void rejectsAppMismatch() throws Exception {
        String tok = mint(envKeys, claims("uat", "hello", "1.2.0", SHA, NOW, NOW + 60));
        assertThrows(LaunchException.class, () -> verifier().verify(tok, "uat", "payroll"));
    }

    @Test
    void rejectsExpired() throws Exception {
        String tok = mint(envKeys, claims("uat", "hello", "1.2.0", SHA, NOW - 600, NOW - 300));
        assertThrows(LaunchException.class, () -> verifier().verify(tok, "uat", "hello"));
    }

    @Test
    void rejectsWrongIssuer() throws Exception {
        String tok = mint(envKeys, "{\"iss\":\"evil-site\",\"aud\":\"fxsuite-launcher\",\"env\":\"uat\","
                + "\"app\":\"hello\",\"ver\":\"1.2.0\",\"sha256\":\"" + SHA + "\","
                + "\"iat\":" + NOW + ",\"exp\":" + (NOW + 60) + ",\"jti\":\"x\"}");
        assertThrows(LaunchException.class, () -> verifier().verify(tok, "uat", "hello"));
    }

    @Test
    void rejectsMalformedHash() throws Exception {
        String tok = mint(envKeys, claims("uat", "hello", "1.2.0", "not-a-hash", NOW, NOW + 60));
        assertThrows(LaunchException.class, () -> verifier().verify(tok, "uat", "hello"));
    }

    @Test
    void rejectsTamperedPayload() throws Exception {
        String tok = mint(envKeys, claims("uat", "hello", "1.2.0", SHA, NOW, NOW + 60));
        String[] p = tok.split("\\.");
        String forged = b64url(claims("prod", "payroll", "9.9.9", SHA, NOW, NOW + 60));
        assertThrows(LaunchException.class,
                () -> verifier().verify(p[0] + "." + forged + "." + p[2], "prod", "payroll"));
    }

    @Test
    void rejectsAlgNone() throws Exception {
        String header = b64url("{\"alg\":\"none\",\"typ\":\"JWT\"}");
        String payload = b64url(claims("uat", "hello", "1.2.0", SHA, NOW, NOW + 60));
        assertThrows(LaunchException.class, () -> verifier().verify(header + "." + payload + ".", "uat", "hello"));
    }

    // --- tiny RS256 signer (stands in for the web backend) ---------------

    private static String mint(KeyPair keys, String payloadJson) throws Exception {
        String header = b64url("{\"alg\":\"RS256\",\"typ\":\"JWT\"}");
        String payload = b64url(payloadJson);
        String signingInput = header + "." + payload;
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign((PrivateKey) keys.getPrivate());
        sig.update(signingInput.getBytes(StandardCharsets.US_ASCII));
        return signingInput + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(sig.sign());
    }

    private static String b64url(String s) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }
}
