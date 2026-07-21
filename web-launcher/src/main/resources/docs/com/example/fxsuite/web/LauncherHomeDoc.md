# FxSuite — Desktop App Launcher

> **Launches are gated by a server-signed token that names the environment.** Pick an
> environment on the *Launch by environment* page; the backend signs a token for that
> environment and version, and the desktop app opens. A different site reusing the URL has no
> valid token and is rejected.

## In this studio

- **Launch by environment** — Production, UAT and the dev environments, each with its own
  URL scheme and its own launcher install.
- **Published versions** — what is in the artifact repository, and which version each
  environment resolves to.

Both are proper MPAs (`StandardMPA` hosting one widget), reachable from the catalogue — not
hand-written HTML pages.

## One-time setup

Each environment registers its own handler once (no admin rights needed):

```
java -jar dist/fxsuite/prod/master-launcher.jar --register --env=prod
java -jar dist/fxsuite/dev/master-launcher.jar  --register --env=dev1
```

That registers `fxsuite-prod://` / `fxsuite-dev1://` under your user account
(`HKEY_CURRENT_USER`), so clicks route to a local `java -jar` — no IT approval, no UAC prompt.

## What happens on a launch

1. The page asks the backend for a token: `GET /token?app=hello&env=<env>`.
2. The backend signs a short-lived token — **environment, app, version and the artifact's
   SHA-256** — with that environment's private key.
3. The browser hands `fxsuite-<env>://launch/hello?tok=…` to the OS, which routes it to that
   environment's launcher install.
4. The launcher **verifies** the token, checks it is addressed to *its* environment, downloads
   the exact version, checks the hash, and launches it with environment-coloured chrome.

## Why another site can't launch anything

A protocol handler never learns which site invoked it, so we don't try to check the domain.
Trust rides on the **signature**: only the backend holds the signing keys. Three independent
layers keep environments apart — the scheme, the per-environment signing key, and the signed
`env` claim (which is what separates dev‑1 from dev‑2, since they share a key).

> See the [copycat page](http://localhost:8086/copycat) — deliberately served from a separate
> origin as ad-hoc HTML, because its whole point is to be a *foreign* site.
