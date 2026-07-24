package com.example.fxsuite.hostmgr;

import com.example.fxsuite.hostmgr.control.Signing;

import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The trust gate: a manifest the agent acts on must carry a valid signature from the pinned
 * control-plane key, and nothing else.
 */
class SigningTest {

    private static KeyPair rsa() throws Exception {
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        return g.generateKeyPair();
    }

    @Test
    void intactManifestVerifies() throws Exception {
        KeyPair k = rsa();
        byte[] manifest = Signing.bytes("{\"generation\":4,\"apps\":[]}");
        String sig = Signing.sign(manifest, k.getPrivate());
        assertTrue(Signing.verify(manifest, sig, k.getPublic()));
    }

    @Test
    void tamperedManifestIsRejected() throws Exception {
        KeyPair k = rsa();
        byte[] manifest = Signing.bytes("{\"generation\":4,\"version\":\"1.0.0\"}");
        String sig = Signing.sign(manifest, k.getPrivate());

        byte[] tampered = manifest.clone();
        tampered[20] ^= 0x01;                    // flip one bit of the declared bytes
        assertFalse(Signing.verify(tampered, sig, k.getPublic()));
    }

    @Test
    void signatureFromAnotherKeyIsRejected() throws Exception {
        KeyPair real = rsa();
        KeyPair rogue = rsa();
        byte[] manifest = Signing.bytes("{\"generation\":4,\"apps\":[]}");
        String rogueSig = Signing.sign(manifest, rogue.getPrivate());
        // A rogue endpoint signing its own manifest cannot pass the pinned real key.
        assertFalse(Signing.verify(manifest, rogueSig, real.getPublic()));
    }
}
