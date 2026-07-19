package com.example.fxsuite.launcher.token;

/**
 * The validated claims carried by a launch token.
 *
 * <ul>
 *   <li>{@code iss} — issuer; must be the FxSuite web backend.</li>
 *   <li>{@code aud} — audience; must be the launcher.</li>
 *   <li>{@code app} — the app id this token authorizes.</li>
 *   <li>{@code ver} — the exact app version to run (the server chooses it, so it
 *       can roll everyone forward just by minting tokens for a new version).</li>
 *   <li>{@code sha256} — hex SHA-256 of the app jar the server wants run. The
 *       launcher downloads by app+version from a <i>pinned</i> repo and refuses
 *       to launch unless the bytes hash to this value — so the signed token
 *       authorizes the exact <b>bytes</b>, not just the app id.</li>
 *   <li>{@code iat} / {@code exp} — issued-at / expiry, epoch seconds.</li>
 *   <li>{@code jti} — unique token id (nonce); reserved for one-time-use.</li>
 * </ul>
 */
public record LaunchToken(String iss, String aud, String app, String ver,
                          String sha256, long iat, long exp, String jti) {}
