# FxSuite demo — Quick Start

Run the whole thing on a Windows machine in ~5 minutes. Commands are for **Git Bash**
(run from the repo root `Q:\repos\javafx\launcher-demo`); PowerShell notes are added
where the syntax differs.

---

## 0. Prerequisites

- **Windows** (the shared JavaFX jar bundles Windows natives).
- **JDK 25** on your `PATH` — check with `java -version`.
- **Maven 3.9+** — check with `mvn -v`.
- **Internet access on first build** — `web-launcher` uses the released
  **homing-studio 0.5.4** (fetched from the remote Maven repo); no local framework
  build is needed. Later builds work offline once dependencies are cached.

Everything runs on **localhost**; nothing needs admin rights or IT.

---

## 1. Generate the signing keys, then build the jars (one-time)

Keys aren't in the repo — generate a matched pair first (run from the repo root):

```bash
java tools/KeyGen.java
```

This writes the private key into `web-launcher` (the token issuer) and the public
key into `master-launcher` (the verifier). Both are git-ignored; re-run to rotate.

Then build:

```bash
mvn -f fxsuite-javafx/pom.xml  clean package
mvn -f app-hello/pom.xml       clean package
mvn -f master-launcher/pom.xml clean package
```

You should get three jars:
- `fxsuite-javafx/target/fxsuite-javafx.jar` (~9.5 MB — the shared JavaFX runtime)
- `app-hello/target/app-hello.jar` (~6 KB — a thin app)
- `master-launcher/target/master-launcher.jar` (~30 KB — the gatekeeper)

---

## 2. Assemble the install + publish two app versions

The **install** deliberately contains *no apps* — apps are pulled from the repo on demand.

```bash
# install: launcher + shared JavaFX + pinned repo config
mkdir -p dist/fxsuite/lib
cp master-launcher/target/master-launcher.jar dist/fxsuite/master-launcher.jar
cp fxsuite-javafx/target/fxsuite-javafx.jar   dist/fxsuite/lib/fxsuite-javafx.jar
printf 'repo.base=http://localhost:8087\n' > dist/fxsuite/launcher.properties

# publish app-hello 1.0.0 and 1.1.0 to the repo (embed a version marker so the
# two versions have different bytes / hash / on-screen version)
for v in 1.0.0 1.1.0; do
  d=dist/repo/apps/hello/$v; mkdir -p "$d"
  cp app-hello/target/app-hello.jar "$d/app-hello-$v.jar"
  t=$(mktemp -d); printf '%s' "$v" > "$t/app-version"
  jar uf "$d/app-hello-$v.jar" -C "$t" app-version; rm -rf "$t"
done
```

<details>
<summary>PowerShell version of the publish loop</summary>

```powershell
foreach ($v in '1.0.0','1.1.0') {
  $d = "dist/repo/apps/hello/$v"; New-Item -ItemType Directory -Force $d | Out-Null
  Copy-Item app-hello/target/app-hello.jar "$d/app-hello-$v.jar"
  $t = New-Item -ItemType Directory -Force (Join-Path $env:TEMP "av-$v")
  Set-Content "$t/app-version" $v -NoNewline
  jar uf "$d/app-hello-$v.jar" -C $t app-version
}
```
</details>

---

## 3. Register the protocol handler (one-time, per user — no admin)

```bash
java -jar dist/fxsuite/master-launcher.jar --register
```

This writes `HKEY_CURRENT_USER\Software\Classes\fxsuite\shell\open\command` pointing at
the launcher in `dist/fxsuite`. No UAC prompt.

---

## 4. Start the web side (leave it running)

From a **second terminal**, in the `web-launcher` folder, pass the absolute path to the repo:

```bash
cd web-launcher
mvn exec:java -Dfxsuite.repo.dir="$(cd .. && pwd)/dist/repo"
```

> PowerShell: `mvn exec:java "-Dfxsuite.repo.dir=$((Resolve-Path ..\dist\repo).Path)"`

It serves three ports:

| URL | What |
|-----|------|
| `http://localhost:8085/` | homing-studio dashboard (catalogue) |
| `http://localhost:8086/` | **authorized launch page** — pick a version, get a token, launch |
| `http://localhost:8086/copycat` | decoy page (same URL, **no token**) |
| `http://localhost:8087/` | artifact repository (Nexus/Artifactory stand-in) |

---

## 5. Use it

Open **`http://localhost:8086/`** in a real browser (Edge/Chrome) and:

1. Click **Launch v1.0.0** → the launcher downloads it, verifies the hash, caches it,
   and a native window shows **v1.0.0**.
2. Click **Launch v1.1.0** → a different version downloads and launches (**dynamic update**).
3. Click **v1.0.0** again → **cache hit**, no re-download.
4. Open **`/copycat`** and click its link → a **“missing launch token”** dialog; nothing launches.

The browser will ask "Open FxSuite master launcher?" the first time — that's the OS
protocol prompt; allow it.

---

## What you should see

- Native JavaFX windows titled **FxSuite — Hello v1.0.0 / v1.1.0**, each in its own process.
- The copycat and any tampered/forged attempt produce a red rejection dialog, not a window.

---

## Diagnostics

Every launch is logged (the handler runs windowless, so there's no console):

```
%LOCALAPPDATA%\fxsuite\launch.log        every launch: token result, download/cache, spawn
%LOCALAPPDATA%\fxsuite\cache\             downloaded app jars, per app/version
```

Tail the log in Git Bash:

```bash
tail -f "$LOCALAPPDATA/fxsuite/launch.log"
```

---

## Command-line shortcuts (no browser)

```bash
# authorized: fetch a token for a version, then launch it
URL=$(curl -s "http://localhost:8086/token?app=hello&ver=1.1.0" | sed -E 's/.*"url":"([^"]*)".*/\1/')
java -jar dist/fxsuite/master-launcher.jar "$URL"

# copycat / bare URL → rejected, nothing spawned
cmd /c start "" "fxsuite://launch/hello"
```

---

## Cleanup / uninstall

```bash
# remove the protocol handler
java -jar dist/fxsuite/master-launcher.jar --unregister

# stop the web server: Ctrl-C in its terminal

# clear the downloaded-app cache (optional)
rm -rf "$LOCALAPPDATA/fxsuite/cache"
```

---

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| Clicking a link does nothing | Run step 3 (`--register`); confirm the browser's "Open FxSuite…" prompt. |
| “missing launch token” on the authorized page | The token server (8086) isn't running — start step 4. |
| “App … is not published” | You skipped the publish step (2), or `-Dfxsuite.repo.dir` doesn't point at `dist/repo`. |
| “Shared JavaFX runtime is missing” | `dist/fxsuite/lib/fxsuite-javafx.jar` isn't there — redo step 2. |
| Window doesn't appear but log shows `spawned …` | Check `launch.log` for a JavaFX error; ensure JDK **25**. |
| Port already in use | An old server is still running; stop it (or reboot the terminal). |

For the full architecture and security model, see [README.md](README.md).
