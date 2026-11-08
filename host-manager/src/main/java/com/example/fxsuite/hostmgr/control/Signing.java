package com.example.fxsuite.hostmgr.control;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * The trust link between the control plane and the host agent.
 *
 * <p>The control plane signs the desired-state manifest; the agent verifies it against a
 * pinned public key before acting on a single byte. The signature is <b>detached</b> — taken
 * over the exact manifest bytes and transmitted alongside them — so there is no canonical-JSON
 * problem: the agent verifies precisely what it will parse.</p>
 *
 * <p>This is the same RS256 scheme the launch/kill commands already use, turned toward a
 * declarative manifest instead of a one-shot command. It is what stops a rogue endpoint (or a
 * tampered manifest in flight) from telling a host to run something the operator never
 * declared.</p>
 */
public final class Signing {

    private Signing() {}

    public static String sign(byte[] payload, PrivateKey key) {
        try {
            Signature s = Signature.getInstance("SHA256withRSA");
            s.initSign(key);
            s.update(payload);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(s.sign());
        } catch (Exception e) {
            throw new IllegalStateException("could not sign manifest", e);
        }
    }

    public static boolean verify(byte[] payload, String sigB64Url, PublicKey key) {
        try {
            Signature s = Signature.getInstance("SHA256withRSA");
            s.initVerify(key);
            s.update(payload);
            return s.verify(Base64.getUrlDecoder().decode(sigB64Url));
        } catch (Exception e) {
            return false;
        }
    }

    public static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    public static PublicKey publicKey(String b64) {
        try {
            return KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(b64)));
        } catch (Exception e) {
            throw new IllegalArgumentException("bad public key", e);
        }
    }
}
