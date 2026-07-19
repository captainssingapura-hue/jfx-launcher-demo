# FxSuite — Desktop App Launcher

> **Launches are gated by a server-signed token.** The dashboard link below carries
> no token on its own — it points at the *authorized launch page*, which mints a
> fresh signed token and then opens the native app. A different site that merely
> reuses the `fxsuite://` URL has no valid token and is rejected.

## Launch

- **[▶ Open the authorized launch page](http://localhost:8086/)** — mint a token and
  launch “Hello, FxSuite”.
- **[⚠ Open the copycat page](http://localhost:8086/copycat)** — same URL, no token;
  the launcher refuses it.

## One-time setup

If nothing happens, install the protocol handler once (no admin rights needed):

```
java -jar master-launcher.jar --register
```

That registers `fxsuite://` under your user account (`HKEY_CURRENT_USER`), so clicks
route to a local `java -jar` — no IT approval, no UAC prompt.

## What happens on an authorized click

1. The launch page calls the backend for a fresh token: `GET /token?app=hello`.
2. The backend signs a short-lived token (RS256, private key server-side only) and
   returns `fxsuite://launch/hello?tok=<JWT>`.
3. The browser hands that URL to the OS protocol handler → `javaw -jar master-launcher.jar …`.
4. The launcher **verifies** the token with its embedded public key: signature,
   issuer/audience, app binding, and expiry — then opens the window.

## Why a copycat is rejected

A protocol handler never learns which site invoked it, so we don't try to check the
domain. Instead trust rides on the **signature**: only the FxSuite backend holds the
private key, so only it can mint a token the launcher accepts. A copied URL carries no
valid token → refused.

> **Next:** one-time-use enforcement (burn the token's `jti` nonce server-side) to
> also defeat replay of a captured, still-fresh token.
