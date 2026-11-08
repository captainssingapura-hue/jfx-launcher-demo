# host-manager — low-touch declarative host management

A control plane declares the set of apps a host should run; an on-host agent makes reality
match and keeps it there. Editing the declared state **is** the deployment — bump a version,
and every host converges on its next tick. The same agent handles day-to-day operations:
crashes are healed, reboots recover, retired apps are stopped, and actual state is reported
back to one fleet view.

This is the "fully controlled" variant grown up: the standing agent + signed-command +
inventory ideas from [`alt/`](../alt) turned from *push me a launch* into *converge to this
declared state*.

```
   operator edits desired.json          ┌── GET /pubkey   (pin once)
            │                            │
   ┌────────▼─────────┐  signed manifest │   ┌───────────────┐   reconcile   ┌────────────┐
   │  control plane   │◀─────────────────┼──▶│   host agent  │──────────────▶│ Supervisor │
   │  (this jar,      │  GET /desired    │   │  (this jar,   │   pure diff   │  systemd / │
   │   control mode)  │◀── POST /report ─┘   │  agent mode)  │◀──────────────│   direct   │
   │  GET /ops        │                      └───────┬───────┘   list()      └─────┬──────┘
   └──────────────────┘                              │ stage+verify (sha256)       │ spawn
                                                     ▼                             ▼
                                          Nexus/repo  GET /repo/apps/…      long-running apps
```

## Run it

One jar, two modes.

```bash
# 1) artifact repo (any static server; the alt relay already serves dist/repo on :8090)
java -Dfxsuite.repo.dir="$PWD/dist/repo" -cp alt/relay/target/classes \
     com.example.fxsuite.alt.relay.RelayServer

# 2) control plane — serves the SIGNED desired state + the ops board
java -jar host-manager/target/fxsuite-host-manager.jar \
     control --port=9099 --desired=desired.json

# 3) on-host agent — pull → reconcile → report, every few seconds
java -jar host-manager/target/fxsuite-host-manager.jar \
     agent --control=http://localhost:9099 --repo=http://localhost:8090 \
           --host=app-server-07 --supervisor=direct --interval=5
```

`desired.json`:

```json
{ "generation": 1, "host": "*", "apps": [
  { "appId": "svc", "version": "1.1.0", "sha256": "03ff…", "env": "prod",
    "memoryMax": "256M", "enabled": true }
] }
```

Open **http://localhost:9099/ops** for the fleet: every host, what it runs, at what version,
and whether it has caught up to the declared generation.

## What the agent does each tick

[`Reconciler.plan`](src/main/java/com/example/fxsuite/hostmgr/Reconciler.java) is a pure
function of *(declared, actual)* — the whole operations story, unit-tested:

| Situation | Action |
|---|---|
| declared, not running | **install** |
| running the wrong version | **upgrade** (stage new, swap, restart) |
| declared + right version, but down (crashed / rebooted) | **self-heal** |
| running, no longer declared (or `enabled:false`) | **decommission** |
| declared + right version + active | nothing — a steady host does no work |

Artifacts are staged by [`Artifacts`](src/main/java/com/example/fxsuite/hostmgr/Artifacts.java):
downloaded once, **verified against the pinned `sha256`** before they are ever placed at their
final path, and kept per-version so an upgrade fetches once and a rollback re-downloads
nothing.

## The Supervisor seam — where the permission lane plugs in

Everything above is identical whichever way a host is allowed to run processes. Only
[`Supervisor`](src/main/java/com/example/fxsuite/hostmgr/supervisor/Supervisor.java) differs:

- **`SystemdUserSupervisor`** (lane A, Linux) — drives `systemctl --user` / `systemd-run
  --user`. No root beyond a one-time `loginctl enable-linger`; survives reboots; gets
  restart/health/resource limits from systemd itself. Upgrades are a `current -> versions/<ver>`
  symlink swap. Uses templated units `fxsuite-<app>@<env>` — the server-side twin of the
  launcher's `--env`.
- **`DirectSupervisor`** (lane C, any OS) — spawns and tracks processes in-process. No
  privilege at all; its trade-off is that it does not survive an agent restart (systemd does).

## Trust

The control plane signs `desired.json`; the agent pins the control key and **verifies the exact
manifest bytes before acting on one of them** (detached RS256, same scheme as the launch/kill
commands). A tampered manifest, or one from a rogue endpoint, is refused and the host keeps its
last good state. See `SigningTest`.

## Verified end to end (direct supervisor, on Windows)

`install → upgrade → self-heal → decommission`, driven only by editing `desired.json`:

```
[agent] install fxsuite-svc@prod -> 1.0.0          # declared gen 1
[agent] upgrade fxsuite-svc@prod -> 1.1.0          # bumped to gen 2 (new bytes, new pid)
[agent] heal    fxsuite-svc@prod -> 1.0.0          # process killed → restarted next tick
[agent] decommission fxsuite-svc@prod              # removed from desired → stopped
```

The systemd supervisor is written to the same interface and compiles, but is exercised only
where systemd exists; on the Windows build box the direct supervisor proves the loop.

## PoC simplifications → production

- Control key is generated on start and served over plain HTTP (TOFU) → pin out of band, use
  TLS/mTLS, and add RBAC + an audit trail of who changed the declared state.
- `desired.json` is a flat file → back it with a real store and a change history.
- The direct supervisor is for demos and locked-down hosts; production Linux uses lane A so
  apps survive agent restarts and get systemd's supervision for free.
