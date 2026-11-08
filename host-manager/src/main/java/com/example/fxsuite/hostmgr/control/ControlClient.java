package com.example.fxsuite.hostmgr.control;

import com.example.fxsuite.hostmgr.json.Json;
import com.example.fxsuite.hostmgr.model.DesiredState;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Map;

/**
 * The agent's view of the control plane: fetch the pinned public key once, then pull the
 * signed desired-state and verify it before returning anything.
 *
 * <p>The manifest arrives as an envelope {@code {"payload":"<base64url manifest>","sig":"…"}}.
 * The signature is checked over the exact payload bytes; only then is the payload decoded and
 * parsed. If verification fails, {@link #pullDesired} throws and the agent keeps running the
 * last good state rather than acting on an unverified manifest.</p>
 */
public final class ControlClient {

    private final String base;
    private final HttpClient http = HttpClient.newHttpClient();
    private volatile PublicKey serverKey;

    public ControlClient(String base) {
        this.base = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    /** Trust-on-first-use: pin the control plane's key. In production this is delivered out of band. */
    public void pinKey() throws IOException, InterruptedException {
        HttpResponse<String> r = http.send(HttpRequest.newBuilder(URI.create(base + "/pubkey")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        if (r.statusCode() != 200) throw new IOException("HTTP " + r.statusCode() + " fetching /pubkey");
        String b64 = Json.str(Json.object(Json.parse(r.body())), "pubkey");
        this.serverKey = Signing.publicKey(b64);
    }

    public DesiredState pullDesired(String hostId) throws IOException, InterruptedException {
        if (serverKey == null) pinKey();
        HttpResponse<String> r = http.send(
                HttpRequest.newBuilder(URI.create(base + "/desired?host=" + hostId)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        if (r.statusCode() != 200) throw new IOException("HTTP " + r.statusCode() + " fetching /desired");

        Map<String, Object> env = Json.object(Json.parse(r.body()));
        String payloadB64 = Json.str(env, "payload");
        String sig = Json.str(env, "sig");
        if (payloadB64 == null || sig == null) throw new IOException("manifest envelope missing payload/sig");

        byte[] payload = Base64.getUrlDecoder().decode(payloadB64);
        if (!Signing.verify(payload, sig, serverKey)) {
            throw new IOException("desired-state signature INVALID — refusing to act on it");
        }
        return DesiredState.parse(new String(payload, StandardCharsets.UTF_8));
    }

    /** Report actual state upward. Best-effort: a failed report never blocks reconciliation. */
    public void report(String json) {
        try {
            http.send(HttpRequest.newBuilder(URI.create(base + "/report"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(json)).build(),
                    HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {
            // ops simply shows this host as stale until the next successful report
        }
    }
}
