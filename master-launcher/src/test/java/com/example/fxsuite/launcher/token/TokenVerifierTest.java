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
 * Exercises the full RS256 verification path with an ephemeral keypair, so no
 * dependency on the embedded resource key. A tiny in-test signer stands in for
 * the web backend's issuer.
 */
class TokenVerifierTest {

    private static KeyPair keys;
    private static final long NOW = 1_000_000L;   // fixed clock
    private static final String SHA = "a".repeat(64);   // a well-formed 64-hex hash

    @BeforeAll
    static void genKeys() throws Exception {
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        keys = g.generateKeyPair();
    }

    private TokenVerifier verifier() {
        return new TokenVerifier(keys.getPublic(), () -> NOW);
    }

    /** Build a claims object with the standard fields, overridable via the args. */
    private static String claims(String app, String ver, String sha, long iat, long exp) {
        return "{\"iss\":\"fxsuite-web\",\"aud\":\"fxsuite-launcher\","
                + "\"app\":\"" + app + "\",\"ver\":\"" + ver + "\",\"sha256\":\"" + sha + "\","
                + "\"iat\":" + iat + ",\"exp\":" + exp + ",\"jti\":\"abc123\"}";
    }

    @Test
    void acceptsValidToken() throws Exception {
        String tok = mint(claims("hello", "1.2.0", SHA, NOW, NOW + 60));
        LaunchToken t = verifier().verify(tok, "hello");
        assertEquals("hello", t.app());
        assertEquals("1.2.0", t.ver());
        assertEquals(SHA, t.sha256());
    }

    @Test
    void rejectsMissingToken() {
        assertThrows(LaunchException.class, () -> verifier().verify(null, "hello"));
        assertThrows(LaunchException.class, () -> verifier().verify("", "hello"));
    }

    @Test
    void rejectsAppMismatch() throws Exception {
        String tok = mint(claims("hello", "1.2.0", SHA, NOW, NOW + 60));
        assertThrows(LaunchException.class, () -> verifier().verify(tok, "payroll"));
    }

    @Test
    void rejectsExpired() throws Exception {
        String tok = mint(claims("hello", "1.2.0", SHA, NOW - 600, NOW - 300));
        assertThrows(LaunchException.class, () -> verifier().verify(tok, "hello"));
    }

    @Test
    void rejectsWrongIssuer() throws Exception {
        String tok = mint("{\"iss\":\"evil-site\",\"aud\":\"fxsuite-launcher\","
                + "\"app\":\"hello\",\"ver\":\"1.2.0\",\"sha256\":\"" + SHA + "\","
                + "\"iat\":" + NOW + ",\"exp\":" + (NOW + 60) + ",\"jti\":\"x\"}");
        assertThrows(LaunchException.class, () -> verifier().verify(tok, "hello"));
    }

    @Test
    void rejectsMissingVersionOrHash() throws Exception {
        String noVer = mint("{\"iss\":\"fxsuite-web\",\"aud\":\"fxsuite-launcher\","
                + "\"app\":\"hello\",\"sha256\":\"" + SHA + "\","
                + "\"iat\":" + NOW + ",\"exp\":" + (NOW + 60) + ",\"jti\":\"x\"}");
        assertThrows(LaunchException.class, () -> verifier().verify(noVer, "hello"));

        String noSha = mint("{\"iss\":\"fxsuite-web\",\"aud\":\"fxsuite-launcher\","
                + "\"app\":\"hello\",\"ver\":\"1.2.0\","
                + "\"iat\":" + NOW + ",\"exp\":" + (NOW + 60) + ",\"jti\":\"x\"}");
        assertThrows(LaunchException.class, () -> verifier().verify(noSha, "hello"));
    }

    @Test
    void rejectsMalformedHash() throws Exception {
        String tok = mint(claims("hello", "1.2.0", "not-a-hash", NOW, NOW + 60));
        assertThrows(LaunchException.class, () -> verifier().verify(tok, "hello"));
    }

    @Test
    void rejectsTamperedPayload() throws Exception {
        String tok = mint(claims("hello", "1.2.0", SHA, NOW, NOW + 60));
        String[] p = tok.split("\\.");
        String forged = b64url(claims("payroll", "9.9.9", SHA, NOW, NOW + 60));
        String tampered = p[0] + "." + forged + "." + p[2];
        assertThrows(LaunchException.class, () -> verifier().verify(tampered, "payroll"));
    }

    @Test
    void rejectsAlgNone() throws Exception {
        String header = b64url("{\"alg\":\"none\",\"typ\":\"JWT\"}");
        String payload = b64url(claims("hello", "1.2.0", SHA, NOW, NOW + 60));
        String tok = header + "." + payload + ".";
        assertThrows(LaunchException.class, () -> verifier().verify(tok, "hello"));
    }

    // --- tiny RS256 signer (stands in for the web backend) ---------------

    private static String mint(String payloadJson) throws Exception {
        String header = b64url("{\"alg\":\"RS256\",\"typ\":\"JWT\"}");
        String payload = b64url(payloadJson);
        String signingInput = header + "." + payload;
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign((PrivateKey) keys.getPrivate());
        sig.update(signingInput.getBytes(StandardCharsets.US_ASCII));
        String signature = Base64.getUrlEncoder().withoutPadding().encodeToString(sig.sign());
        return signingInput + "." + signature;
    }

    private static String b64url(String s) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }
}
