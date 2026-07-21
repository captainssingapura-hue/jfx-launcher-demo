# Alternative architecture PoC

Two alternatives to the top-level demo (which is left untouched). Both make the
launcher a **standing JavaFX app** and shrink or remove the browser's role, cutting
copycat-website risk. One backend ([`relay`](relay), pure JDK) serves both.

> 📊 **Visual overview:** open [`architecture.html`](architecture.html) in a browser —
> hand-drawn SVG diagrams of both flows, side by side.

```
Variant 1 (push):  browser ─POST /launch─▶ relay ─signed cmd over WebSocket─▶ agent
                     (only talks to the server)                         verify → launch

Variant 2 (pull):  local launcher ─GET /catalog, GET /repo/…─▶ relay
                     (its own JavaFX UI; browser not involved at all)   verify sha256 → spawn
```

## Modules

| Module | Variant | What it is |
|--------|---------|-----------|
| [`relay`](relay) | backend | Pure-JDK. WebSocket server (`:8091`) + HTTP (`:8090`): `POST /launch` (v1 push, RSA-signed), `GET /catalog` + `GET /repo/*` (v2 pull), one-button page. |
| [`agent`](agent) | 1 | JavaFX app holding a WebSocket to the relay; verifies server-signed launch commands pushed to it. |
| [`local-launcher`](local-launcher) | 2 | JavaFX app that pulls the catalogue, lists apps in its own UI, and downloads → verifies → spawns locally. |

---

## Variant 1 — server-mediated (push)

The web page only calls the **server**; the server validates, **RSA-signs a launch
command, and pushes it** down the agent's connection. The browser never touches the
local launch path, so a copycat site has no local endpoint to forge against.

```bash
mvn -f alt/relay/pom.xml clean compile
mvn -f alt/agent/pom.xml clean package
mvn -f fxsuite-javafx/pom.xml package        # shared JavaFX jar (if not built)

java -Dfxsuite.repo.dir="$PWD/dist/repo" -cp alt/relay/target/classes \
     com.example.fxsuite.alt.relay.RelayServer
java -cp "alt/agent/target/alt-agent.jar;fxsuite-javafx/target/fxsuite-javafx.jar" \
     com.example.fxsuite.alt.agent.AgentMain alice
# open http://localhost:8090/ and click a version
```

Verified: `409 no agent connected` when the agent is down; after it connects, a
browser click yields `VERIFIED launch command … → spawned (pid …)`. Agent log:
`%LOCALAPPDATA%\fxsuite\agent.log`.

### Ops plane — control & monitoring (why variant 1 exists)

The standing connection is a **management & observability channel** for Production
Support: locally‑running apps stay centrally visible and controllable.

- The agent spawns each launch as a **real process**, tracks it, and **heartbeats a
  live inventory** (app, version, pid, uptime) to the relay every few seconds.
- Ops console: open **`http://localhost:8090/ops`** — a table of every connected
  agent and what it's running, with a **Kill** button per app.
- Endpoints: `GET /ops/agents` (live inventory JSON) · `POST /ops/kill?user=&launchId=`
  (pushes an **RSA‑signed** kill command the agent verifies before terminating).

Verified end‑to‑end: a pushed launch appears in `/ops/agents` as a running pid; a
remote kill terminates that OS process and the inventory clears — *controlled and
monitored even when running locally.* Future ops actions (same channel): force‑update,
version pin/rollback, revoke, richer telemetry/audit.

---

## Variant 2 — launcher-native (pull) · *the local launcher handles everything locally*

No browser, no protocol handler, no push channel — **so no copycat surface at all.**
The launcher pulls the catalogue, lists apps + versions in its own JavaFX UI, and on
Launch downloads the jar, verifies its SHA-256 against the catalogue, caches it, and
spawns it as a separate process sharing the one JavaFX jar.

```bash
mvn -f alt/relay/pom.xml clean compile
mvn -f alt/local-launcher/pom.xml clean package
mvn -f fxsuite-javafx/pom.xml package        # shared JavaFX jar (if not built)

# backend (serves the catalogue + artifacts from dist/repo)
java -Dfxsuite.repo.dir="$PWD/dist/repo" -cp alt/relay/target/classes \
     com.example.fxsuite.alt.relay.RelayServer

# the local launcher (run from the repo root so the shared jar resolves)
java -cp "alt/local-launcher/target/alt-local-launcher.jar;fxsuite-javafx/target/fxsuite-javafx.jar" \
     com.example.fxsuite.alt.local.LocalLauncherMain
#   optional deep-launch / test hook:  add  -Dfxsuite.autolaunch=hello:1.1.0
#   config: -Dfxsuite.repo.base=http://localhost:8090  -Dfxsuite.shared.jar=<path>
```

Verified end-to-end: the launcher lists `hello 1.0.0 / 1.1.0`; launching yields
`downloading … → verified + cached → spawned (pid …, shared JavaFX jar)`, and the app
opens in its own process. Launcher log: `%LOCALAPPDATA%\fxsuite\local-launcher.log`;
cache under `%LOCALAPPDATA%\fxsuite\altcache`.

### What variant 2 validates
- The local launcher is **self-contained**: catalogue discovery + download +
  **integrity check (SHA-256)** + spawn, all driven from its own UI.
- **Zero web attack surface** — there is no local endpoint and no per-app URL for any
  website to target.

### PoC simplifications → production
- Catalogue is trusted over plain HTTP on localhost → production uses **`wss`/HTTPS**
  and a **signed catalogue** so the launcher can trust the versions + hashes.
- The launcher spawns the real app (Shape B) — same shared-jar model as the top-level
  `master-launcher`; that fetch/verify/spawn logic could be shared instead of mirrored.
- Add auth (who may see/launch which apps) on the catalogue + artifact endpoints.
