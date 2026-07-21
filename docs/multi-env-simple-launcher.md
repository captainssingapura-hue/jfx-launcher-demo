# Multi‑environment support — simple launcher (no control plane)

**Status:** draft for review · **Scope:** running the suite against Production, UAT and
`dev[1..n]` using the **stateless launcher** (protocol handler + signed token), *without* a
standing agent.

**Relationship to [`multi-environment-design.md`](multi-environment-design.md):** that document
covers the managed‑agent architecture. The environment *taxonomy* (§2), *version policy /
promotion* (§8) and *trust principles* (§9) carry over unchanged. Everything about sessions,
heartbeats, readiness probes, reconciliation and ops consoles **does not apply here**.

---

## 1. Goals / non‑goals

**Goals**
- One environment per launch URL, unambiguously.
- Strong isolation between environments, especially Production.
- Cheap, self‑service onboarding for short‑lived dev environments.
- Minimum moving parts — the launcher only ever **launches**.

**Explicit non‑goals** (the point of this variant)
- No standing connection, no session model, no heartbeat, no readiness probe.
- No live inventory, no remote kill, no desired‑state reconciliation, no ops console.
- The launcher does not track what it started; it exits after spawning.

---

## 2. Model: one registered URL scheme per environment

> **Decision.** Every environment gets its **own launch URL scheme**. Singleton environments
> map to **dedicated binaries**; multiplexed dev environments share **one binary distinguished
> by arguments**.

| Environment | Scheme | Binary | Args |
|---|---|---|---|
| Production | `fxsuite-prod://` | `prod/master-launcher.jar` *(embeds Prod public key)* | — |
| UAT | `fxsuite-uat://` | `uat/master-launcher.jar` *(embeds UAT public key)* | — |
| dev‑1 | `fxsuite-dev1://` | `dev/master-launcher.jar` *(embeds dev public key)* | `--env=dev1 --base=…` |
| dev‑n | `fxsuite-devN://` | **same** dev binary | `--env=devN --base=…` |

### Why this shape

- **Environment is decided by the registry entry, not by the URL payload.** Which scheme was
  invoked determines which binary runs with which arguments. Nothing a web page puts *inside*
  the URL can change the environment — a genuinely useful property.
- **Dedicated binaries give Prod its own trust root for free.** The Prod launcher embeds only
  the Prod public key, so a token minted anywhere else simply fails signature verification.
- **Dev stays cheap.** One installed binary, one embedded dev key, N registrations that differ
  only by arguments — matching dev's ephemeral, high‑churn nature.

*Alternative considered:* a single `fxsuite://` scheme with the environment in the path,
handled by one binary. Rejected — it collapses all environments into one binary and one trust
anchor, which is exactly the isolation we want for Production.

---

## 3. Registration

> Full details of every key/value, the `.reg` escaping rules, verification and troubleshooting:
> [`windows-registry.md`](windows-registry.md).

Per environment, under `HKEY_CURRENT_USER` (no admin, as today):

```
HKCU\Software\Classes\fxsuite-prod\shell\open\command
  = "…\javaw.exe" -jar "…\fxsuite\prod\master-launcher.jar" "%1"

HKCU\Software\Classes\fxsuite-dev3\shell\open\command
  = "…\javaw.exe" -jar "…\fxsuite\dev\master-launcher.jar" --env=dev3 --base=https://dev3.internal "%1"
```

- Registration is **per environment and independent**: `--register --env=<id> [--base=<url>]`,
  `--unregister --env=<id>`.
- **Dev onboarding is self‑service:** the dev environment's own web page offers "enable this
  environment", which runs the dev binary's register mode with that env's id and base URL.
- **Dev registrations expire.** Because dev envs churn, registrations accumulate — provide a
  `--prune` that removes registrations whose environment is gone.
- **Not‑yet‑registered environment:** clicking its link does nothing (unknown scheme). The web
  page should detect this case and show the "enable this environment" flow instead.

---

## 4. Install layout

```
fxsuite/
  prod/   master-launcher.jar   launcher.properties   (Prod public key embedded)
  uat/    master-launcher.jar   launcher.properties   (UAT public key embedded)
  dev/    master-launcher.jar   launcher.properties   (dev public key embedded)
  lib/    fxsuite-javafx.jar    ← shared runtime (see decision below)
```

- Singleton environments are **separately installed and separately upgradable**.
- All dev environments share the single `dev/` install.
- **Open:** whether the shared JavaFX runtime jar is genuinely shared across environments
  (saves ~9.5 MB per env) or duplicated per environment for strict Production isolation. It is
  environment‑agnostic code, so sharing is defensible if its integrity is verified.

---

## 5. Trust and the environment claim

Token claims stay as today plus `env`: **`{env, app, ver, sha256, iss, aud, iat, exp, jti}`**.

| Environment class | Signing key | What enforces environment separation |
|---|---|---|
| Singleton (Prod, UAT) | its own key | **key separation** — primary; `env` claim is belt‑and‑braces |
| Multiplexed (dev) | one shared dev key | **the `env` claim** — the *only* thing distinguishing dev1 from dev2 |

> **Mandatory check:** the launcher must verify `token.env == its own configured environment`
> (from the binary for singletons, from `--env` for dev). Without it, a dev‑1 token replayed at
> the `fxsuite-dev2://` URL would be accepted, because dev environments share a signer.

Everything else is unchanged: signature, expiry, audience, and the `sha256` pin over the
downloaded artifact.

---

## 6. Launch flow

1. The web page for environment *E* emits `fxsuite-<E>://launch/<app>?tok=<signed token>`.
2. Windows resolves the scheme to *E*'s command → the right binary, with the right args.
3. The launcher parses the URL and **verifies the token with its own embedded public key**.
4. It checks **`token.env` matches its own environment** (§5).
5. It resolves `app` + `ver` + `sha256`, then fetches from **that environment's** repo base
   (its `launcher.properties`, or `--base` for dev), using **that environment's** cache.
6. It verifies the bytes against `sha256`, then spawns the app with `-Dfxsuite.env=<E>` plus
   the environment's config, sharing the JavaFX runtime jar.
7. It exits. Nothing is tracked.

---

## 7. Workstation state partitioning

```
%LOCALAPPDATA%\fxsuite\<env>\cache\<app>\<ver>\app-<app>-<ver>.jar
%LOCALAPPDATA%\fxsuite\<env>\launch.log
```

- Cache and logs are **keyed by environment**, so the same app+version in Prod and dev‑2 are
  separate copies and can never be confused.
- Apps from different environments run **simultaneously** as independent processes.

---

## 8. Safety: environment must be unmistakable

The launcher injects `-Dfxsuite.env`, so each app can render its environment in the window —
colour‑coded chrome/banner (🔴 Prod, 🟠 UAT, 🔵 dev) and the env in its title. With several
environments open at once, the dominant operational risk is acting in the wrong one; this is
the cheapest possible mitigation and should not be skipped.

---

## 9. What you give up (versus the managed agent)

Stated plainly so the trade is deliberate:

| Capability | Simple launcher |
|---|---|
| Live inventory of what's running locally | ✗ |
| Remote kill / force‑update / version pin at runtime | ✗ |
| Desired‑ vs actual‑state reconciliation | ✗ |
| Presence / health of workstations | ✗ |
| **Audit of launches** | ✓ — **token issuance is the audit point** |

That last row matters: even with no agent, the server signs every launch, so it can log **who
launched which app, at which version, in which environment, when**. You lose visibility of
*running state*, not of *launch intent* — which covers a large share of what Production
Support usually asks for.

---

## 10. Edge cases & open questions

| Case | Handling / open question |
|---|---|
| Scheme proliferation from dev churn | `--prune`; consider capping registrations per user |
| Per‑scheme browser consent | Each new scheme prompts "allow this site to open…" once — mild friction, unavoidable |
| Dev env deleted while its app runs | App keeps running against a dead backend; it must fail gracefully |
| Same app, two environments, at once | Supported — separate caches, separate processes, distinct window chrome |
| Shared vs per‑env JavaFX runtime | **Open** (§4) |
| Environment not registered | Web page detects and offers the enable flow |
| Upgrading a singleton launcher | Independent per environment; Prod upgrades on its own schedule |

---

## 11. Implementation steps (small)

1. `env` as a signed token claim + the `token.env == own env` check.
2. `--env` / `--base` arguments and per‑env `launcher.properties`.
3. Per‑environment registration/unregistration (`fxsuite-<env>` schemes) + `--prune`.
4. Environment‑keyed cache and log paths.
5. Per‑environment build/packaging of the singleton binaries (embedding their own public key).
6. `-Dfxsuite.env` injection + environment chrome in the app.
