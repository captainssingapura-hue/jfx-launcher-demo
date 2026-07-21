# launcher-demo — FxSuite web launcher (proof-of-concept)

Launch native **JavaFX** desktop apps from links on an internal website, on
Windows, with **no admin rights and no IT involvement** — by registering a
custom `fxsuite://` protocol handler under the current user (`HKCU`).

This repo proves the plumbing end-to-end **and** the security gate: a launch only
runs if the URL carries a short-lived, **server-signed token** that names the app,
the exact **version**, and the **SHA-256** of that version's jar. A copycat site
reusing the URL has no valid token; a tampered jar fails the hash check. Apps are
**not** shipped with the launcher — they are pulled on demand from a repository
(Nexus/Artifactory) and cached, so the server can roll versions forward without
touching the install.

**Multiple environments** (Production, UAT, `dev[1..n]`) are supported: each has its own
URL scheme and its own launcher install — see
[docs/multi-env-simple-launcher.md](docs/multi-env-simple-launcher.md) and §*Environments* below.

```
authorized page ─▶ GET /token?app=hello&env=prod ─▶ backend looks up the jar in the repo,
       │                                            signs {env, app, ver, sha256} (RS256, per-env key)
       ▼                                            returns fxsuite-prod://launch/hello?tok=<JWT>
   browser hands URL to OS ─▶ HKCU handler ─▶ javaw -jar prod/master-launcher.jar --env=prod "<url>"
                                                     │
                     validate URL + VERIFY TOKEN (public key) → get app, ver, sha256
                                                     │
                     cache miss? download <repo>/apps/hello/1.1.0/… → check bytes == signed sha256
                                                     │
                     spawn in its OWN process, sharing one JavaFX jar:
                     javaw -cp <cache>/app-hello-1.1.0.jar;lib/fxsuite-javafx.jar HelloMain
                                                     │
                                            native JavaFX window
```

## Architecture

The launcher is a **thin, JavaFX-free gatekeeper** that ships **no app jars**. It
verifies the request, fetches the exact version named in the token from a **pinned
repository** (download + integrity-check + cache), then launches the app as a
**separate process** that puts the one shared `fxsuite-javafx.jar` on its classpath.
So JavaFX is stored once, apps are tiny, and versions update dynamically.

| Module | What it is | Size |
|--------|-----------|------|
| [`master-launcher`](master-launcher) | Gatekeeper: register handler, verify token, fetch+verify+cache the versioned app jar, spawn its process. No JavaFX. | ~30 KB |
| [`fxsuite-javafx`](fxsuite-javafx) | Shared JavaFX runtime — all classes + 54 native DLLs shaded into one jar. | ~9.5 MB (×1) |
| [`app-hello`](app-hello) | A single app. JavaFX is `provided` (not bundled); own code only. Published per version to the repo. | ~6 KB (×versions) |
| [`web-launcher`](web-launcher) | Dashboard + token origin + repo server, built on the **homing-studio** framework. | — |

Each is an independent Maven build (JDK 25). `web-launcher` depends on the released
homing-studio **0.5.4**, resolved from the remote Maven repo — no local framework
build required.

**Install layout** — one install *per environment*, and **no apps**:

```
fxsuite/
  lib/fxsuite-javafx.jar             shared JavaFX runtime (once, across environments)
  prod/  master-launcher.jar  launcher.properties (env=prod)  verify-key.x509.b64  ← Prod's own key
  uat/   master-launcher.jar  launcher.properties (env=uat)   verify-key.x509.b64
  dev/   master-launcher.jar  launcher.properties (repo.base) verify-key.x509.b64  ← shared by dev1..devN
```

Singleton environments (Prod, UAT) get a **dedicated install with their own trust anchor**;
all dev environments share the one `dev/` install and are distinguished by `--env=` on the
registered command.

## Environments

| Environment | Scheme | Binary | Args |
|---|---|---|---|
| Production | `fxsuite-prod://` | `prod/master-launcher.jar` | env from `launcher.properties` |
| UAT | `fxsuite-uat://` | `uat/master-launcher.jar` | env from `launcher.properties` |
| dev1 … devN | `fxsuite-dev1://` … | **same** `dev/master-launcher.jar` | `--env=devN [--base=…]` |

Three independent layers keep environments apart — all verified:

1. **Scheme** — the launcher refuses a URL addressed to another environment.
2. **Signature** — singleton environments have their own signing key, so a UAT token fails
   outright at Production.
3. **`env` claim** — multiplexed dev environments *share* a key, so the signed `env` claim is
   what stops a dev‑1 token being replayed at dev‑2.

State is keyed by environment (`%LOCALAPPDATA%\fxsuite\<env>\{cache,launch.log}`), so the same
app+version in Prod and dev are separate copies, and apps carry colour‑coded environment chrome
(🔴 Prod, 🟠 UAT, 🔵 dev) injected via `-Dfxsuite.env`.

**Repository layout** (Nexus/Artifactory stand-in) and **local cache**:

```
<repo>/apps/<app>/<ver>/app-<app>-<ver>.jar          published artifacts (many versions)
%LOCALAPPDATA%/fxsuite/cache/<app>/<ver>/…jar         downloaded on first use, re-hashed each launch
```

The launcher resolves the install root from its own jar location. The repo base is
read from `launcher.properties` — never from the URL/token — and the artifact path
is a fixed pattern built from the (charset-validated) app + version, so a token can
choose *which version* but never redirect the download to another host.

> **Still no shell in the flow.** The app process is a direct `CreateProcess` of
> `javaw.exe` with an argument vector (via `ProcessBuilder`) — no `cmd.exe`, no
> script. The only other subprocess anywhere is `reg.exe` during registration.

## Try it

**1. Build the jars, assemble the install, and publish two app versions:**

```bash
mvn clean package          # root aggregator builds all modules (JDK 25 required)

# shared JavaFX runtime (once)
mkdir -p dist/fxsuite/lib
cp fxsuite-javafx/target/fxsuite-javafx.jar dist/fxsuite/lib/fxsuite-javafx.jar

# one install per environment (singletons get their own trust anchor)
for E in prod uat; do
  mkdir -p dist/fxsuite/$E
  cp master-launcher/target/master-launcher.jar dist/fxsuite/$E/master-launcher.jar
  printf "env=$E\nrepo.base=http://localhost:8087\n" > dist/fxsuite/$E/launcher.properties
done
# one shared install for all dev environments (no env= — supplied per registration)
mkdir -p dist/fxsuite/dev
cp master-launcher/target/master-launcher.jar dist/fxsuite/dev/master-launcher.jar
printf 'repo.base=http://localhost:8087\n' > dist/fxsuite/dev/launcher.properties

# publish app-hello 1.0.0 and 1.1.0 to the repo (embed a version marker so the
# bytes — and the hash, and the displayed version — differ per version)
for v in 1.0.0 1.1.0; do
  d=dist/repo/apps/hello/$v; mkdir -p "$d"
  cp app-hello/target/app-hello.jar "$d/app-hello-$v.jar"
  t=$(mktemp -d); printf '%s' "$v" > "$t/app-version"
  jar uf "$d/app-hello-$v.jar" -C "$t" app-version; rm -rf "$t"
done
```

**Register the environments (one-time, per user — no admin).** Either run the setup app:

```bash
cp fxsuite-setup/target/fxsuite-setup.jar dist/fxsuite/
java -jar dist/fxsuite/fxsuite-setup.jar
```

A self-contained JavaFX installer (it bundles JavaFX, since it runs before anything is
installed). It lists the environments it finds, shows their current status, and displays
**the exact registry changes** before applying them. Removal is a true inverse: it restores
any handler that was registered before FxSuite took over the scheme, and only deletes the
key when there was none.

…or use the command line:

```bash
java -jar dist/fxsuite/prod/master-launcher.jar --register --env=prod
java -jar dist/fxsuite/uat/master-launcher.jar  --register --env=uat
java -jar dist/fxsuite/dev/master-launcher.jar  --register --env=dev1   # same binary…
java -jar dist/fxsuite/dev/master-launcher.jar  --register --env=dev2   # …different arg

java -jar dist/fxsuite/dev/master-launcher.jar  --list     # show registered environments
java -jar dist/fxsuite/dev/master-launcher.jar  --prune    # drop registrations whose jar is gone
```

Each writes `HKCU\Software\Classes\fxsuite-<env>\shell\open\command`. Remove one with
`--unregister --env=<id>`. Every key/value it touches — plus verification and
troubleshooting — is documented in [docs/windows-registry.md](docs/windows-registry.md).

**2. Run the web side (one JVM, three ports):**

```bash
cd web-launcher
mvn exec:java -Dfxsuite.repo.dir=/abs/path/to/dist/repo
#   http://localhost:8085/                       homing-studio catalogue (the UI)
#   http://localhost:8085/app?app=env-launch     launch by environment   (MPA)
#   http://localhost:8085/app?app=published-apps published versions      (MPA)
#   http://localhost:8085/token?app=&env=        signed-token API   (same origin)
#   http://localhost:8085/catalog                published artifacts (same origin)
#   http://localhost:8086/copycat                decoy: a DIFFERENT site, no token → rejected
#   http://localhost:8087/                       artifact repository (Nexus stand-in)
```

The UI pages are **homing MPAs** (`StandardMPA` hosting one widget each), registered in
`LauncherStudio.apps()` and organised as catalogue entries — not hand-written HTML.

The APIs are **served by the studio itself**, contributed through
`Fixtures.harnessGetActions()` — homing's standard way to add routes. They return plain Java
records, which the framework serialises to JSON (only non-JSON responses need `TypedContent`).
Because the UI and its API share one origin, **no CORS is involved** — which matters for the
one endpoint that must not be callable by other sites.

The only other origins are things that genuinely *are* separate: the copycat decoy (it must
look foreign) and the artifact repository (a Nexus stand-in).

**3. Open `http://localhost:8086/` in a real browser and click a version.**
The page fetches a fresh signed token for that version; the launcher downloads it
(first time), verifies the bytes, caches it, and a native JavaFX window shows that
version. Click a different version to see a dynamic update; click again to see a
cache hit (no re-download). Open `/copycat` to see a tokenless URL rejected.

Simulate from a shell:

```bash
# authorized: pick an environment → token → download + verify + launch
URL=$(curl -s "http://localhost:8086/token?app=hello&env=prod" | sed -E 's/.*"url":"([^"]*)".*/\1/')
java -jar dist/fxsuite/prod/master-launcher.jar "$URL"

# copycat / bare URL → rejected (dialog), nothing spawned
cmd /c start "" "fxsuite-prod://launch/hello"

# cross-environment replay → also rejected
TOK=$(curl -s "http://localhost:8086/token?app=hello&env=dev1" | sed -E 's/.*tok=([^"]*)".*/\1/')
java -jar dist/fxsuite/dev/master-launcher.jar --env=dev2 "fxsuite-dev2://launch/hello?tok=$TOK"
```

Diagnostics (every launch is logged, since the handler runs windowless
`javaw`): `%LOCALAPPDATA%\fxsuite\launch.log`. Downloaded apps cache under
`%LOCALAPPDATA%\fxsuite\cache`.

## How the launcher stays safe

The incoming URL is **untrusted** — any site, email, or file on the machine can
invoke `fxsuite://` once it's registered. So the launcher, in order:

1. **parses strictly** ([`LaunchUri`](master-launcher/src/main/java/com/example/fxsuite/launcher/LaunchUri.java)):
   scheme must be `fxsuite`, action must be `launch`, app id must match a tight
   character allow-list (no path traversal, no separators);
2. **verifies a server-signed token** ([`TokenVerifier`](master-launcher/src/main/java/com/example/fxsuite/launcher/token/TokenVerifier.java)):
   RS256 signature against an embedded public key, correct issuer/audience, the
   token's `app` claim must equal the URL's app id, valid `ver` + `sha256` claims,
   and not expired. The signed token — not a local list — is what authorizes an app,
   which is what makes the catalog dynamic;
3. **downloads only from the pinned repo** ([`RepoConfig`](master-launcher/src/main/java/com/example/fxsuite/launcher/RepoConfig.java) + [`AppFetcher`](master-launcher/src/main/java/com/example/fxsuite/launcher/AppFetcher.java)):
   the repo base is trusted local config; the artifact path is a fixed pattern from
   the validated app + version. A token can pick the version but can't redirect the
   download (no SSRF), and redirects are refused;
4. **verifies the bytes** ([`AppFetcher`](master-launcher/src/main/java/com/example/fxsuite/launcher/AppFetcher.java)):
   the downloaded jar must hash to the token's signed `sha256` or it is refused and
   never cached — so the signed token authorizes the exact **bytes**, not just the id.

### Why domain filtering isn't the answer

A protocol handler **cannot** be restricted by originating domain — the browser
never passes the origin to the handler. So trust rides on the **signature**
instead. The backend ([`LaunchTokenIssuer`](web-launcher/src/main/java/com/example/fxsuite/web/token/LaunchTokenIssuer.java))
mints a short-lived RS256 JWT with a private key that never leaves the server;
the launcher ships only the **public** key. Only the FxSuite origin can produce a
token the launcher accepts, so a copied URL from any other site is refused.

Keys are **generated locally**, never committed. Run once before building:

```bash
java tools/KeyGen.java
```

It writes the private key to `web-launcher/.../fxsuite/launch-signing-key.pk8.b64`
(the issuer) and the public key to `master-launcher/.../fxsuite/launch-verify-key.x509.b64`
(baked into the launcher jar). Both are git-ignored; re-run any time to rotate.
*(A real deployment would keep the private key in a secret store, not a resource.)*

## Roadmap (next)

- **One-time use:** burn the token's `jti` nonce server-side (a launcher→backend
  callback) so a captured, still-fresh token can't be replayed. The `jti` claim is
  already issued and carried through verification.
- **Per-user binding + real auth:** issue tokens only to an authenticated session
  and bind a user id into the token.
- **Repo hardening for production:** HTTPS + repo auth (Nexus/Artifactory
  credentials), a signed catalog/version-manifest so the launcher can discover
  “latest” without the token server, and cache eviction/pinning policy.
- **Offline/again-later:** the cache already lets a previously-run version relaunch
  without the repo; add an explicit offline mode + integrity re-check on load.
