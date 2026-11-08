# Controlled, Low-Touch Host Management for FxSuite

**A declarative control plane with privilege-separated on-host agents**

Status: design white paper · Version 0.1 · 2026-07-25
Companion: [Decoupling Infrastructure and Application Management](decoupling-infra-and-app-management-whitepaper.md)

---

## Abstract

FxSuite delivers native desktop applications from internal web links without central IT
involvement. This paper extends that model to the **server side**: the long-running
applications and services of the same suite, managed the same way — declaratively, from a
central control plane, but with the day-to-day resilience and the privilege discipline that
production servers demand.

The design has three moving ideas. First, **declarative desired state**: operators state which
apps a host should run and at which version; each host converges itself. Second, a
**pluggable supervision seam** so the same control plane drives hosts of very different
privilege postures. Third, **privilege separation between deployment and runtime** — the
identity that writes application binaries is never the identity that executes them — realised,
where wanted, as **two cooperating agents** on each host.

A working prototype implements the control plane, the reconcile loop, artifact integrity, and
one supervisor lane end to end. This paper records the full target architecture, including the
parts still on paper, so the security and operational reasoning is settled before more code is
written.

---

## 1. Context and motivation

The desktop side of FxSuite is deliberately low-touch: a per-user protocol handler launches
signed applications, needs no administrator rights, and keeps IT out of the day-to-day. That
property — *the operator declares intent; the machine makes it so, without a human in the loop*
— is worth carrying to the servers that run the suite's back-end and headless components.

The naïve path is to run those services under an init system (systemd, Windows Services) and
call it done. That gives supervision but not the two things this suite actually needs:

- **Auto-upgrade from a pinned, verified source.** An init system runs what is on disk; it
  does not fetch a new version named in a signed instruction, verify its bytes, swap it, and
  keep the old one for rollback.
- **A central control plane.** An init system is per-host. There is no single place that sees
  the fleet, declares intent across it, and records who changed what.

Those are exactly the capabilities the desktop side already has in its launch-token machinery.
This paper reuses that machinery — signed instructions, pinned keys, hash-pinned artifacts —
turned from *launch this now* into *converge to this declared state*, and adds the privilege
discipline appropriate to a shared server.

---

## 2. Scope and non-goals

**In scope.** Long-running server applications and headless services that are part of the
suite; their deployment, upgrade, health, and decommissioning; a fleet-wide control plane;
and the on-host privilege model.

**Non-goals.**

- This is not a general-purpose orchestrator. It does not schedule across hosts, bin-pack,
  or provide service discovery. Where those are needed, a real orchestrator (Nomad,
  Kubernetes) is the right tool; this design deliberately delegates process supervision to the
  host's own init system rather than reinventing it.
- It does not replace the desktop launcher. The desktop model (per-user, no admin, local
  trust) is unchanged and out of scope here.
- It does not attempt to defend a host against its own trusted deployer (see §4).

---

## 3. Design principles

1. **Declare, don't command.** Operators change a declared desired state; hosts reconcile to
   it continuously. A missed command, a rebooted host, or a crashed process is not a special
   case — it is just the next reconcile tick's work.
2. **Verify before acting.** Nothing is deployed that is not named in a signed manifest and
   whose bytes do not match a pinned hash. The trust decision precedes every side effect.
3. **Separate deciding from doing.** The code that decides *what* runs (the deployer) is a
   different identity from the code that *runs* it (the runtime). Neither can do the other's
   job.
4. **Spend privilege once.** Any privileged setup happens at provisioning, not per operation.
   After that, the steady state needs no elevated rights.
5. **Delegate supervision.** Keeping a process alive, capping its resources, and starting it at
   boot are solved problems; the host's init system owns them. The agent's novel work is the
   verified-upgrade and control-plane layer above that.
6. **The seam is the design.** Hosts differ in what they will permit. A single interface —
   the *supervisor* — isolates those differences so everything above it is identical
   everywhere.

---

## 4. Threat model and trust boundaries

The design distinguishes two adversaries and treats them very differently.

**A compromised runtime.** An application process — or the data it handles — turns hostile.
The primary goal of privilege separation is to deny this adversary **persistence**: it must not
be able to rewrite its own binary, plant the next version, or edit the configuration or unit
that governs it. It may still misbehave within the rights it legitimately holds (the network it
may reach, the data it may read), but it cannot tamper with the managed software supply on the
host, and any write it attempts to the code tree is immediately anomalous and alertable.

**A compromised deployer.** The agent identity that writes binaries, or the control-plane
signing key. This adversary is **explicitly not defended against by the user split** — writing
binaries is the deployer's job by definition. It is constrained instead by the *other* half of
the design: the signed manifest and pinned key bound what the deployer will accept as
instructions, and an audit trail records every change to declared state. The two mechanisms are
complementary:

> **Signing constrains the deployer's inputs. The user split constrains the runtime's reach.**
> A robust posture needs both; neither substitutes for the other.

**Out of scope.** As on the desktop side, a host whose trusted deployer identity *and* signing
key are both compromised is out of scope — at that point the attacker is the legitimate
operator. Physical/root compromise of the host is likewise out of scope; this design raises the
bar for runtime compromise, it does not defeat an attacker who already owns the host.

### Trust chain

```
   signing key (control plane)
        │  signs
        ▼
   desired-state manifest ──verified by pinned key──▶ admin-agent (deployer)
                                                          │ writes
                                                          ▼
                                        code tree + local intent  (admin-owned, run-readable)
                                                          │ read-only
                                                          ▼
                                                     run-agent / init  (runtime, unprivileged)
```

Trust flows strictly downward. The runtime never holds the signing key and never contacts the
control plane; it obeys only the admin-owned intent on the read-only tree.

---

## 5. Architecture

### 5.1 Control plane

A central service holds the **declared desired state** for the fleet: for each host (or for all
hosts), the set of apps that should run, each pinned to an exact version and content hash, in a
named environment. Operators change this state; that change *is* the deployment.

The control plane:

- serves the desired state as a **signed** document — a detached RS256 signature over the exact
  manifest bytes, so an agent verifies precisely what it will parse;
- accepts **status reports** from hosts and presents the fleet: what each host runs, at what
  version, and whether it has caught up to the current declared *generation*;
- is the single point of audit for changes to intent.

The desired state carries a monotonic **generation** number. Hosts report the generation they
last converged, so "which hosts are behind" is answerable at a glance.

### 5.2 On-host agent(s)

Each host runs an agent that repeats one dull, idempotent loop:

```
pull signed desired state  →  verify  →  reconcile  →  report actual state  →  sleep
```

The reconcile step is a **pure function** of *(declared, actual)* — no I/O, no processes — which
is what makes the operational behaviour testable and predictable:

| Situation | Action |
|---|---|
| declared, not running | **install** |
| running the wrong version | **upgrade** |
| declared and correct version, but down (crashed, rebooted) | **self-heal** |
| running, no longer declared (or disabled) | **decommission** |
| declared, correct version, active | nothing — a steady host does no work |

For hosts that separate deployment from runtime, this single agent is split into two (§7).

### 5.3 The supervisor seam and permission lanes

Everything above the seam — control plane, signed manifest, reconciler, artifact handling — is
identical on every host. Only *how a process is actually started, stopped, and observed*
differs, behind one interface. Three lanes cover the realistic privilege postures:

| Lane | Mechanism | Privilege required | Deploy/run separation | Survives agent restart | Notes |
|---|---|---|---|---|---|
| **A** | `systemctl --user` (user units) | one-time `enable-linger`, else none | **No** — agent UID = app UID | Yes | Lowest touch; closest to the desktop "no central IT" ethos, but cannot separate deploy from runtime |
| **B** | system units, `User=` / `DynamicUser=` | one-time root/polkit policy | **Yes** | Yes | Gains per-app identity plus systemd's restart/health/resource limits; the production target when separation is wanted |
| **C** | in-process spawn (`ProcessBuilder`) | none | **No** | No | Any OS, no privilege; for locked-down hosts and as the cross-platform proving ground |

The lane is a per-host choice; the control plane and every host above the seam are unaware of
it beyond a label in the status report.

### 5.4 Artifact flow

Artifacts (application jars) live in a repository (Nexus/Artifactory or equivalent). On an
install or upgrade the agent:

1. reads the pinned version and **sha256** from the verified manifest;
2. reuses the artifact if already present and matching — so an upgrade downloads once and a
   rollback re-downloads nothing;
3. otherwise downloads to a temporary path, **verifies the hash before the bytes are ever
   placed at their final location**, then atomically moves it into a per-version directory.

Versions are retained side by side:

```
<appsRoot>/<app>/versions/<ver>/app.jar
<appsRoot>/<app>/current -> versions/<ver>          (the running selection)
```

An **upgrade is a symlink repoint plus a restart**; a **rollback is the same repoint in
reverse** — instant, no download. A running process keeps its old jar open through the versioned
path, so swapping `current` never pulls the file out from under it.

---

## 6. Privilege separation: the `admin` and `run` identities

Two OS identities, mapped to the two jobs:

- **`admin`** — the **deployer**. Owns the code and config tree; may write binaries, stage
  versions, repoint `current`, and author units. This is the identity the deployment side runs
  as.
- **`run`** — the **runtime**. Executes the applications with read + execute on the code, and
  write access to nothing in that tree. It cannot alter what it runs.

The filesystem is split to match the identities, so the separation is real and not merely
nominal:

```
admin-owned, run reads (read-only):   code tree · configs · current · intent
run-owned, run writes:                a separate state tree · logs · scratch
```

If the runtime could write where it executes, the separation would collapse; the two trees are
therefore distinct, with one-directional access.

**Why Java forces this to be an OS concern.** A JVM cannot launch a child as a different user —
`ProcessBuilder` always spawns as the caller's identity, and switching identity is inherently
privileged. So the runtime identity cannot be conjured by the deployer at spawn time; it must be
established by a privileged subsystem. Two paths exist:

- **Delegate the identity switch to the init system.** The agent (as `admin`) asks systemd or
  the Windows SCM to start the app *as* `run` (`User=run`, a service logon account, or
  `DynamicUser`). The privileged component does the setuid; the agent never holds that power.
- **Establish each identity once at boot.** Start the runtime side already as `run`. Then no
  switch is ever needed — plain `ProcessBuilder` inherits `run`. This is the two-agent topology.

Prefer **passwordless** runtime identities — `DynamicUser` on Linux, virtual service accounts or
gMSA on Windows — so no runtime credential is ever stored by the agent.

---

## 7. Two-agent topology

Because a process cannot change its own user, the cleanest way to run apps as `run` while
deploying as `admin` — without any per-spawn privilege — is to run **two agents**, each started
once as its own identity:

```
   signed desired state
          │
   ┌──────▼────────────────┐  writes    ┌── code tree (admin-owned, run reads) ──┐
   │  admin-agent  (admin)  │───────────▶│  versions/<v>/app.jar   current -> <v>  │
   │  pull · verify · stage │            │  intent.json                            │
   │  swap current · report │            └──────────────────────┬──────────────────┘
   └──────▲────────────────┘                         reads       │ (read-only to run)
          │ reads status                                         ▼
          │                                     ┌──────────────────────────────────┐
          └──────────────────────────────────── │   run-agent  (run)               │
                    status.json (run-owned)      │  reconcile processes as run:     │
                                                 │  start / stop / heal             │
                                                 └──────────────────────────────────┘
```

- **admin-agent** does everything that writes: pull and verify the manifest, stage and
  hash-check artifacts, repoint `current`, and write a small **intent file** ("svc@prod should
  be version X"). It holds the control-plane connection and the pinned key. It never touches a
  process.
- **run-agent** does everything runtime: read the intent and the read-only binaries, keep the
  right processes alive as `run`, and write a small **status file**. It is **local-only** — no
  control-plane connection, no key, no outbound channel to compromise. It never touches a
  binary.

The **filesystem is the contract**, with two strictly one-way channels: admin writes / run reads
for *intent*; run writes / admin reads for *status*. There is never a shared writable directory.

### What the split buys, beyond running apps as `run`

- **The runtime-facing logic is itself unprivileged.** All code that touches application
  surfaces — parsing health responses, tailing logs, deciding restarts — runs as `run`, not
  `admin`. A defect there is contained to the runtime identity.
- **The network-facing, key-holding logic is isolated from the apps.** Only the admin-agent
  speaks to the control plane; the run-agent is airgapped from it.

### The one-agent alternative

If the host is content to let **systemd supervise the applications directly** (lane B, system
units with `User=run`/`DynamicUser`), a **single** admin-agent suffices: it deploys and asks
systemd to run each app as `run`. This is simpler — one process, no intent/status protocol — and
gives *more*: per-app identity and systemd's restart/health/resource limits for free. The
two-agent topology earns its extra moving part only when the runtime is supervised by *our own*
agent rather than the init system (for in-process or cross-platform supervision, or richer
app-level orchestration than units express).

The decision reduces to one question:

> **Who supervises the applications — the init system, or our agent?**
> Init system → one agent, delegate the identity switch. Our agent → two agents, split by user.
> Both achieve deploy/run separation; they differ only in who owns the process lifecycle.

---

## 8. Security properties

| Property | Mechanism | Limit |
|---|---|---|
| Only declared software runs | signed manifest, pinned key, verified before any action | assumes key secrecy |
| Exact bytes, no substitution | sha256 pinned in the manifest, checked before launch | assumes hash in a trusted manifest |
| Runtime cannot persist | `run` has no write to the code tree; identity established by a privileged subsystem | not vs. a compromised deployer |
| Runtime cannot self-reconfigure | configs are `admin`-owned, `run`-read | — |
| Runtime has no control channel | run-agent is local-only; only admin-agent holds the key/connection | — |
| Blast radius bounded | per-app runtime identity (`DynamicUser` / per-app account) | a *shared* `run` under-delivers; per-app is required for real isolation |
| Change is attributable | control plane is the single point of audit for intent | requires the audit trail to be honoured |

Two cautions the design must honour:

- **A shared `run` is the weak form.** One runtime account across many apps means one
  compromise reaches every other app's readable data. The isolation pays off only with a
  per-app runtime identity.
- **`admin` reading `run`-written status is a high-reads-low boundary.** A compromised runtime
  could try to attack the deployer through the status file. That parse must be minimal and
  schema-validated, exactly as the deployer's reading of application logs must be.

---

## 9. Operations model

Day-to-day operations are all expressions of one gesture — *edit the declared state* — plus the
agent's continuous self-correction.

- **Deploy / upgrade.** Change the version in the manifest and bump the generation. Hosts stage
  the new artifact, repoint `current`, and restart on their next tick. The fleet view shows each
  host converging from the old generation to the new.
- **Rollback.** Set the version back. Because prior versions are retained, this is a repoint and
  restart with no download — as fast as forward.
- **Self-heal.** A crashed or reboot-lost process is simply "declared but not active" on the next
  tick, and is started again. Under lane B, systemd also restarts it immediately; the reconcile
  loop is the backstop and the cross-lane guarantee.
- **Decommission.** Remove the app from the manifest (or mark it disabled). The host stops it and
  reports it gone.
- **Observability.** Every host reports actual unit state and its converged generation to the
  control plane's fleet board.

"Low touch" is the sum of these: the operator states intent once, and hosts absorb drift —
crashes, reboots, version skew — without a human in the loop.

---

## 10. Trade-offs and decision guide

Two decisions shape a deployment.

**Which permission lane?** (§5.3)

- Choose **A** for the lightest footprint when deploy/run separation is not required — it keeps
  the "no ongoing central involvement" property but cannot separate the two identities.
- Choose **B** when you want deploy/run separation, per-app identity, and init-system
  supervision — accepting a one-time root/polkit grant at provisioning.
- Choose **C** for hosts too locked down for even a one-time grant, or as the cross-platform
  proving ground — accepting no separation and no survival across agent restart.

**One agent or two?** (§7) — decided by who supervises the apps. There is no free lunch: even
two agents require a one-time privileged step to place them as two identities at boot. What the
separation *never* costs is per-operation privilege; what it *always* costs is that one-time
setup.

A blunt summary: **separation is worth it for anything Production Support must trust, and it is
not free — it moves the controlled variant from lane A to lane B (or `DynamicUser`).**

---

## 11. Prototype status

To keep the paper honest, what exists in code versus what is described here:

**Built and verified end to end** (on Windows, via the direct/lane-C supervisor):

- the control plane: signed desired state, fleet status board, generation tracking;
- the single agent loop: pull → verify → reconcile → report;
- the reconciler as a pure function, with install / upgrade / self-heal / decommission unit
  tested;
- artifact staging with sha256 verification and per-version retention;
- manifest signing and verification, including rejection of a tampered or wrongly-signed
  manifest;
- a demonstrated sequence: install `1.0.0` → declaratively upgrade to `1.1.0` (new bytes, new
  pid) → kill the process and observe self-heal → remove from desired and observe decommission.

**Written, compile-verified, not yet runtime-exercised:**

- the systemd (lane A) supervisor binding — it targets `systemctl --user` and runs only where
  systemd exists.

**Described here, not yet built:**

- the two-agent topology (`admin-agent` + `run-agent`) and the intent/status filesystem
  contract;
- the `admin`/`run` OS user separation and the split code/state trees;
- lane B: system units with `User=` / `DynamicUser`, and per-app runtime identities;
- control-plane hardening: out-of-band key distribution, TLS/mTLS, RBAC, and a durable audit
  trail for changes to declared state.

---

## 12. Open questions and roadmap

1. **Key distribution and rotation.** The prototype pins the control-plane key trust-on-first-use
   over localhost. Production needs out-of-band delivery and a rotation story.
2. **Manifest store and audit.** A flat file is enough to prove reconciliation; a durable store
   with change history and per-change attribution is required for the "single point of audit"
   claim.
3. **Per-app identity mechanism.** `DynamicUser` vs. standing per-app accounts vs. Windows
   virtual/gMSA accounts — pick per platform, and confirm the read-only-tree ownership holds
   under each.
4. **Windows parity.** The lane-B analog (Windows Services with distinct logon accounts,
   passwordless where possible) mirrors the Linux design but wants its own validation pass.
5. **Agent supervision.** Whichever topology, the agents themselves must be kept alive by the
   init system; the bootstrap and self-update of the agents is its own small problem.

---

## Appendix A — Glossary

- **Control plane** — the central service holding and signing the declared desired state and
  presenting the fleet view.
- **Desired state / manifest** — the signed declaration of which apps a host should run, pinned
  to version and content hash.
- **Generation** — a monotonic counter on the desired state; hosts report the generation they
  last converged.
- **Reconcile** — the pure comparison of declared vs. actual state that yields the change set.
- **Supervisor** — the interface abstracting how processes are started/stopped/observed; its
  implementations are the permission *lanes*.
- **Lane A / B / C** — user-unit systemd / system-unit systemd / in-process spawn.
- **admin / run** — the deployer identity (writes binaries) and the runtime identity (executes
  them).
- **admin-agent / run-agent** — the two cooperating agents in the split topology.
- **Intent file / status file** — the admin-writes/run-reads and run-writes/admin-reads
  filesystem channels between the two agents.

## Appendix B — Example desired state

```json
{
  "generation": 7,
  "host": "*",
  "apps": [
    { "appId": "svc", "version": "1.1.0", "sha256": "03ff…", "env": "prod",
      "memoryMax": "256M", "enabled": true }
  ]
}
```

The control plane serves this wrapped in a signed envelope
(`{ "payload": "<base64url manifest>", "sig": "<base64url signature>", "alg": "SHA256withRSA" }`);
the agent verifies the signature over the exact payload before parsing it.
