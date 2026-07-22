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
                     spawn in its OWN process; the launcher jar provides JavaFX:
                     javaw -cp <cache>/app-hello-1.1.0.jar;master-launcher.jar HelloMain
                                                     │
                                            native JavaFX window
```

## Architecture

The launcher's own code uses only the JDK. Each **environment is one self-contained jar**
that bundles JavaFX **and its managed apps** and bakes in its own environment id. A launch
resolves the app from what the launcher carries and runs it **from the launcher jar's own
classpath — no download**. Anything *not* bundled falls back to fetching the exact version
from a **pinned repository** (download + integrity-check + cache). Either way the app runs as
a **separate process**, and the token still gates every launch.

So the common case is: the web link opens the env's launcher, which already has the app. The
trade-off is that bundling pins one version per app per env jar — updating the managed set means
shipping a new env jar (the "curated suite, versioned as a unit" model). Registration reflects
the env-baked design: a singleton command is just `javaw -jar master-launcher.jar "%1"` (no
`--env`); the shared dev jar takes `--env=devN`.

| Module | What it is | Size |
|--------|-----------|------|
| [`master-launcher`](master-launcher) | The launcher **core library**: launcher UI, protocol-handler launch, CLI, Settings (registration + keys), token verification. Bundles nothing. | ~90 KB |
| [`env/prod`, `env/uat`, `env/dev`](env) | One module per environment — a regular fat jar (`FxSuite-<env>.jar`) of core + JavaFX + that environment's apps. Its identity is its resources: which environment it is, and which apps it carries. | ~9.6 MB each |
| [`app-hello`](app-hello) | A single app. JavaFX is `provided` (not bundled); own code only. Published per version to the repo. | ~6 KB (×versions) |
| [`web-launcher`](web-launcher) | Dashboard (homing MPAs) + token/catalogue API + repo server. | — |
| [`fxsuite-javafx`](fxsuite-javafx) | Shared JavaFX runtime jar — now used **only by the `alt/` PoCs**; the main launcher is self-contained. | ~9.5 MB |

Each is an independent Maven build (JDK 25). `web-launcher` depends on the released
homing-studio **0.5.4**, resolved from the remote Maven repo — no local framework
build required.

**Install layout** — one **dedicated, double-clickable build per environment**:

```
fxsuite/
  FxSuite-prod.jar              env baked in; carries its apps + JavaFX
  FxSuite-uat.jar               env baked in
  FxSuite-dev.jar               no baked env — takes --env=devN (shared by dev1..devN)
  verify-key-prod.x509.b64      Prod's own trust anchor (env-specific, so builds can share a folder)
```

Each jar is the **whole environment app**. Double-click it and you get the launcher UI —
the apps it carries, one click away — with registration and signing keys under **Settings**
in the menu bar. There is no separate installer program.

Each `master-launcher.jar` is **self-contained** — it bundles JavaFX (classes + Windows
natives) and, when it spawns an app, puts *itself* on the app's classpath to provide it. So
there is no shared `lib/` directory and no relative-path lookup to get wrong; an environment
is one jar plus its `launcher.properties`. (The build produces this as `master-launcher-app.jar`
alongside a thin `master-launcher.jar` used only as a compile dependency.)

Singleton environments (Prod, UAT) get a **dedicated install with their own trust anchor**;
all dev environments share the one `dev/` install and are distinguished by `--env=` on the
registered command. Apps themselves stay thin — JavaFX is `provided`, never bundled — so the
only copies of JavaFX are the handful of launcher installs.

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

**1. Generate the signing keys** (nothing is committed, so a fresh clone has none):

```bash
java tools/KeyGen.java     # shared pair: private -> web-launcher, public -> master-launcher
```

That one pair is enough to run everything. Per-environment keys — so a token minted
for one environment fails at another on the **signature** alone — are easiest to manage
in the setup app (step 3): pick a **keys root**, and it creates one folder per
environment holding that environment's pair, shows each launcher's **trust anchor and
fingerprint**, installs the public half, and can confirm the private key matches the key
a launcher actually trusts.

```
<keys root>/prod/signing-key.pk8.b64    private — the SERVER's; give it to the issuer
<keys root>/prod/verify-key.x509.b64    public  — installed beside prod's launcher
```

Keep the default shared pair and everything still works; environments simply share one
trust anchor instead of having their own.

**2. Build the jars, assemble the install, and publish two app versions:**

```bash
mvn clean package          # root aggregator builds all modules (JDK 25 required)

# `mvn clean package` above already produced one fat jar per environment —
# deploying is just copying them:
mkdir -p dist/fxsuite
cp env/prod/target/FxSuite-prod.jar dist/fxsuite/
cp env/uat/target/FxSuite-uat.jar   dist/fxsuite/
cp env/dev/target/FxSuite-dev.jar   dist/fxsuite/

# A launcher prefers verify-key-<env>.x509.b64 beside it, then a shared
# verify-key.x509.b64, then the key baked into its jar — so nothing more is needed for
# the shared-key setup. Per-environment anchors are installed from Settings ▸ Signing keys.

# publish app-hello 1.0.0 and 1.1.0 to the repo (embed a version marker so the
# bytes — and the hash, and the displayed version — differ per version)
for v in 1.0.0 1.1.0; do
  d=dist/repo/apps/hello/$v; mkdir -p "$d"
  cp app-hello/target/app-hello.jar "$d/app-hello-$v.jar"
  t=$(mktemp -d); printf '%s' "$v" > "$t/app-version"
  jar uf "$d/app-hello-$v.jar" -C "$t" app-version; rm -rf "$t"
done
```

**3. Register the environments** (one-time, per user — no admin). Either run the setup app:

```bash
cp fxsuite-setup/target/fxsuite-setup.jar dist/fxsuite/
java -jar dist/fxsuite/fxsuite-setup.jar
```

A self-contained JavaFX installer (it bundles JavaFX, since it runs before anything is
installed). It lists the environments it finds, shows their current status, and displays
**the exact registry changes** before applying them. Removal is a true inverse: it restores
any handler that was registered before FxSuite took over the scheme, and only deletes the
key when there was none.

It also manages the **launch-token keys**: choose a keys root, and per environment it can
generate a pair into `<keys root>/<env>/`, install the public half beside that launcher,
and check that the private key matches the anchor the launcher trusts. Each row shows the
anchor's source (install folder vs the jar's built-in default) and its fingerprint, so a
Production launcher silently falling back to the shared key is visible at a glance.
Generating keys is an *operator* action — the private half belongs to the token issuer and
must never be distributed to workstations.

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

**4. Run the web side (one JVM, three ports):**

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

**5. Open `http://localhost:8085/` in a real browser and pick an environment.**
The studio's *Launch by environment* page asks the backend for a signed token; the
launcher downloads that version (first time), verifies the bytes, caches it, and a
native JavaFX window opens with environment-coloured chrome. Launch a different
environment to see them side by side; launch the same one twice to see a cache hit.
Open `http://localhost:8086/copycat` — a different origin — to see a tokenless URL
rejected.

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
