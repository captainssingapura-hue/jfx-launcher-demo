package com.example.fxsuite.launcher.token;

/**
 * The validated claims carried by a launch token.
 *
 * <ul>
 *   <li>{@code iss} / {@code aud} — issuer and audience.</li>
 *   <li>{@code env} — the environment this token authorizes. The launcher refuses a
 *       token whose {@code env} is not its own. For singleton environments the
 *       separate signing key is the primary control; for multiplexed dev
 *       environments (which share a signer) this claim is the <b>only</b> thing
 *       separating dev‑1 from dev‑2.</li>
 *   <li>{@code app} — the app id.</li>
 *   <li>{@code ver} — the exact version to run.</li>
 *   <li>{@code sha256} — hex SHA-256 of the app jar, so the signed token authorizes
 *       the exact bytes.</li>
 *   <li>{@code iat} / {@code exp} — issued-at / expiry, epoch seconds.</li>
 *   <li>{@code jti} — unique token id (nonce); reserved for one-time-use.</li>
 * </ul>
 */
public record LaunchToken(String iss, String aud, String env, String app, String ver,
                          String sha256, long iat, long exp, String jti) {}
