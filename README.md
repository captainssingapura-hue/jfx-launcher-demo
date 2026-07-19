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

```
authorized page ─▶ GET /token?app=hello&ver=1.1.0 ─▶ backend looks up the jar in the repo,
       │                                              signs {app, ver, sha256} (RS256, private key)
       ▼                                              returns fxsuite://launch/hello?tok=<JWT>
   browser hands URL to OS ─▶ HKCU handler ─▶ javaw -jar master-launcher.jar "<url>"   (thin gatekeeper, no JavaFX)
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

**Install layout** — note it contains **no apps**:

```
fxsuite/
  master-launcher.jar        the registered handler target
  lib/fxsuite-javafx.jar     shared JavaFX runtime (once)
  launcher.properties        repo.base=http://…    (pinned, trusted config)
```

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
mvn -o -f fxsuite-javafx/pom.xml  clean package
mvn -o -f app-hello/pom.xml       clean package
mvn -o -f master-launcher/pom.xml clean package

# install (NO apps): launcher + shared JavaFX + pinned repo config
mkdir -p dist/fxsuite/lib
cp master-launcher/target/master-launcher.jar dist/fxsuite/master-launcher.jar
cp fxsuite-javafx/target/fxsuite-javafx.jar   dist/fxsuite/lib/fxsuite-javafx.jar
printf 'repo.base=http://localhost:8087\n' > dist/fxsuite/launcher.properties

# publish app-hello 1.0.0 and 1.1.0 to the repo (embed a version marker so the
# bytes — and the hash, and the displayed version — differ per version)
for v in 1.0.0 1.1.0; do
  d=dist/repo/apps/hello/$v; mkdir -p "$d"
  cp app-hello/target/app-hello.jar "$d/app-hello-$v.jar"
  t=$(mktemp -d); printf '%s' "$v" > "$t/app-version"
  jar uf "$d/app-hello-$v.jar" -C "$t" app-version; rm -rf "$t"
done
```

**Install the handler (one-time, per user — no admin):**

```bash
java -jar dist/fxsuite/master-launcher.jar --register
```

`--register` writes `HKCU\Software\Classes\fxsuite\shell\open\command` pointing at
the launcher jar in `dist/fxsuite`. Remove it with `--unregister`.

**2. Run the web side (one JVM, three ports):**

```bash
cd web-launcher
mvn -o exec:java -Dfxsuite.repo.dir=/abs/path/to/dist/repo
#   http://localhost:8085/   homing-studio dashboard (catalogue)
#   http://localhost:8086/   authorized launch page (pick a version → token → launch)
#   http://localhost:8086/copycat   decoy: same URL, no token → rejected
#   http://localhost:8087/   artifact repository (Nexus/Artifactory stand-in)
```

**3. Open `http://localhost:8086/` in a real browser and click a version.**
The page fetches a fresh signed token for that version; the launcher downloads it
(first time), verifies the bytes, caches it, and a native JavaFX window shows that
version. Click a different version to see a dynamic update; click again to see a
cache hit (no re-download). Open `/copycat` to see a tokenless URL rejected.

Simulate from a shell:

```bash
# authorized: pick a version → download + verify + launch
URL=$(curl -s "http://localhost:8086/token?app=hello&ver=1.1.0" | sed -E 's/.*"url":"([^"]*)".*/\1/')
java -jar dist/fxsuite/master-launcher.jar "$URL"

# copycat / bare URL → rejected (dialog), nothing spawned
cmd /c start "" "fxsuite://launch/hello"
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
