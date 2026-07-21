# Multi‑environment design — FxSuite launcher

**Status:** draft for review · **Scope:** how the suite runs against Production, UAT and
`dev[1..n]` · **Applies to:** the managed‑agent architecture (control plane + local agent)

> For the **stateless launcher** variant (protocol handler + signed token, no agent, no control
> plane) see [`multi-env-simple-launcher.md`](multi-env-simple-launcher.md). The environment
> taxonomy (§2), version policy (§8) and trust principles (§9) below are shared by both.

---

## 1. Goals / non‑goals

**Goals**
- Run the same suite against several environments, some simultaneously on one workstation.
- Give Production Support an exact, authoritative view of what is running locally, per environment.
- Keep environments isolated: a lower environment must never be able to drive a higher one.
- Support many short‑lived developer environments without a heavyweight install per environment.

**Non‑goals (for now)**
- Cross‑environment data movement or promotion of *data*.
- Per‑environment app source branching (that's release management; see §8 for artifact promotion only).

---

## 2. Environment taxonomy

Two classes of environment, treated deliberately differently.

| Class | Members | Lifetime | Count | Stakes |
|---|---|---|---|---|
| **Singleton** | Production, UAT | long‑lived, change‑controlled | exactly one each | high |
| **Multiplexed** | `dev[1..n]` | ephemeral (per branch / per developer) | many, churning | low |

**Rationale.** Prod and UAT are stable, audited, and few — they can afford (and deserve)
a dedicated agent and dedicated console each. Dev environments are numerous and
disposable; one agent and one console for all of them is far cheaper to operate.

---

## 3. Topology and the 1:1 invariant

> **Decision.** Each environment has a **dedicated server endpoint** bound to a
> **single local agent**, in a strict one‑to‑one mapping.

```
WORKSTATION                                  SERVER SIDE                 CONSOLES
┌────────────────────────────┐
│ Prod Agent   (singleton)   │◄──── 1:1 ────► Prod endpoint    ────────► Prod console
├────────────────────────────┤
│ UAT Agent    (singleton)   │◄──── 1:1 ────► UAT endpoint     ────────► UAT console
├────────────────────────────┤
│ Dev Agent   (multiplexed)  │                                        ┌─► Central
│   ├── session ─────────────┼──── 1:1 ────► dev‑1 endpoint  ────────┤   Dev
│   ├── session ─────────────┼──── 1:1 ────► dev‑2 endpoint  ────────┤   console
│   └── session ─────────────┼──── 1:1 ────► dev‑n endpoint  ────────┘
└────────────────────────────┘
```

### Invariants

1. **An agent channel never spans environments.** One channel ⇒ exactly one environment.
2. **Every agent invocation is its own isolated session.** Sessions never overlap; the
   authoritative view of a workstation is the **union of live sessions** (§6). Duplicates are
   allowed but detected and alerted — not rejected or displaced.
3. **Singleton envs get their own agent *process*.** Prod and UAT are separately installed,
   separately trusted, separately upgradable.
4. **Dev multiplexes at the process level only.** One dev agent process hosts *N* logical
   sessions, each still 1:1 with one dev endpoint, each with its own identity and state
   partition. The logical invariant is preserved; only the packaging is shared.

> Note the asymmetry is intentional: multiplexing trades process isolation for convenience.
> That trade is acceptable for dev and **not** acceptable for Prod/UAT.

*(A server endpoint of course serves many workstations — the 1:1 is per agent, i.e. scoped
to (environment × workstation × user), not globally.)*

---

## 4. Why 1:1 matters — exact state

The one‑to‑one mapping isn't just tidiness; it makes the server's knowledge of the local
side **complete and unambiguous**:

- **Authoritative inventory.** Everything an agent reports belongs to that environment — no
  filtering, no "which env is this process?" ambiguity, no partial views. With multiple
  sessions (§6) exactness is **per session**; the workstation view is their union.
- **Unambiguous control targeting.** "Kill this app for this user in UAT" resolves to exactly
  one channel. No fan‑out, no risk of hitting the wrong environment.
- **Desired‑ vs actual‑state reconciliation.** Because the agent's report is *complete* for
  its environment, the server can hold desired state (allowed versions, what should be
  running) and compute an exact diff — the control plane becomes a proper reconciler rather
  than a fire‑and‑forget command pusher.
- **Clean reconnect semantics.** On reconnect the agent re‑reports its full inventory and the
  server replaces (not merges) its view — no stale‑state reconciliation puzzles.
- **Blast radius.** A compromised or misbehaving environment's channel can't touch another.

**Cost of the invariant:** the singleton constraint must be *enforced* (§6), and Prod/UAT
each cost a real install and a real connection per workstation.

---

## 5. Console topology

> **Decision.** The console is **decoupled** from the per‑environment server endpoints.

| Environment class | Console |
|---|---|
| Singleton (Prod, UAT) | **one dedicated console per environment** |
| Multiplexed (`dev[1..n]`) | **one central console** across all dev environments |

- Consoles are read/act **clients** of the env endpoints' control API — they are not embedded
  in the endpoints. This lets consoles be deployed, secured and upgraded independently.
- The **Prod console stands alone** with its own RBAC and audit trail; it can be hosted in a
  restricted zone and never needs to reach dev.
- The **central dev console** discovers dev endpoints from the environment registry (§7) and
  fans out. If *n* grows large enough that fan‑out hurts, insert a dev aggregator later —
  the decoupling makes that a console‑side change only.

---

## 6. Agent requirement, sessions and duplicates

> **Decision.** Duplicates are **allowed, isolated, detected and alerted** — never rejected
> or displaced. Rationale: *displacing* an incumbent orphans the processes it owns;
> *rejecting* a newcomer blocks recovery whenever the server's liveness view is stale.
> Allow‑and‑detect degrades gracefully instead of failing hard.

### When is an agent required?

> **Rule.** The suite is **web by default**. A local agent is required *only* at the moment a
> user starts something needing a local component — never as a precondition for logging in or
> using the web application.

| App archetype | Local agent | Notes |
|---|---|---|
| Pure web app | not required | the majority case; zero install |
| Native JavaFX app | required | agent fetches, verifies, launches, supervises |
| **Web UI + local acceleration** | required | UI stays in the browser; the agent supplies local compute / native library / hardware access |

Consequences:
- **Zero adoption friction** — nobody installs an agent to use the web suite.
- The catalogue must carry **per‑app capability requirements** (`needs‑agent`,
  `needs‑local‑acceleration`, specific native/hardware needs) so the web knows when this flow
  applies at all.
- The third archetype turns the agent into a **local capability provider**, not just a process
  launcher. **Open (§11):** how the page consumes that capability — routed *via the server*
  (no local attack surface, higher latency) or via a **localhost endpoint** on the agent (fast,
  but reintroduces a local endpoint needing strict origin checks + a session‑bound token).

### Session model
- **Every invocation of the agent creates a new isolated session**, identified by
  `(env, workstation, user, session‑id)` where the session‑id is minted at start‑up.
- Sessions **never overlap**: inventory, control targeting and lifecycle are per session.
  A command for session A can never touch session B's processes.
- The **agent always tries to connect** and reconnects with backoff — it never refuses to
  start because another session may exist. Connection is therefore *not* the enforcement
  point; enforcement is advisory (UX) plus detection.

### What is isolated vs shared on the workstation
| Isolated per session | Safely shared across sessions |
|---|---|
| control channel + session id | artifact cache (immutable, hash‑addressed, atomic writes) |
| process inventory / tracking | per‑environment config bundle (read‑only) |
| log file | |
| any localhost port the agent binds | |

> Sharing the cache is deliberate — duplicating multi‑MB artifacts per session is waste, and
> immutability + atomic writes make concurrent use safe.
> **Not solved by session isolation:** apps that are themselves singletons (lock file, fixed
> port, shared user‑data dir) can still collide when launched from two sessions.

### Just‑in‑time readiness (the launch gate)

> **Decision.** Do **not** gate on the server's cached presence view. At the moment an action
> needs the agent, the server **actively probes the running session** to confirm the action
> can be honoured. No response ⇒ assume the session is broken and offer to start a new agent.

Why: passive presence is stale by up to a heartbeat interval, and — more importantly — a
**half‑dead agent** can keep heartbeating while its launch path is wedged. An action‑level
probe tests the thing that actually matters.

- **Pre‑flight, not ping.** The probe asks *"can you honour this?"* and the reply carries a
  reason on refusal: launch path healthy, env config present, repo reachable / version cached,
  required capability available (e.g. local acceleration), no policy block (version not
  allowed, app already running as a singleton). The web can then say something specific.
- **Short timeout** (~2–3 s) with a "checking…" state, then fall through to the start‑agent
  flow. The probe must be cheap — no downloads inside it.
- **Targeting.** If a live session answers, the web pairs to *that* session, so the subsequent
  launch has an unambiguous destination (§ duplicates). If several answer, pick the paired /
  most recent one.
- **Composes with the duplicate policy.** A probe that times out on a merely‑slow agent will
  sometimes start a second one. Under *allow + isolate + detect* that degrades to a harmless
  duplicate — which is precisely why reject‑or‑displace would have been the wrong partner
  policy here.
- **Probe ≠ guarantee.** The agent can still die between probe and launch, so the **launch
  itself must be acknowledged** by the agent (own timeout). The web reports "launched" on the
  agent's ack, not on the server's push.

### Detection and alerting
- **Duplicate := more than one *live* session for the same `(env, user, workstation)`.**
  Workstation is part of the key — the same user on a laptop *and* a VDI is legitimate.
- Alert severity follows the taxonomy (§2): in **Prod** a duplicate is an anomaly → alert;
  in **dev** it is often normal → informational.
- The console lists all live sessions and lets ops act on each independently.

### Enrolment
- Each session enrols with its environment endpoint; enrolment is per environment — a Prod
  enrolment grants nothing anywhere else.

---

## 7. Environment registry (needed by `dev[1..n]`)

Because dev environments come and go, the *list of environments* is itself dynamic — one
level above the app catalogue:

1. **Environment registry** → available environments + their endpoints + trust info.
2. **Per‑environment app catalogue** → apps + versions available *in that environment*.

Consumers: the dev agent (which dev envs to attach to), the central dev console (what to
show), and onboarding UX. Singleton envs are static entries; dev entries register and expire.

---

## 8. Artifacts, versions and configuration

- **Catalogue is per environment.** The same app id resolves to different versions per env.
- **Version policy per class:**
  | Env | Policy |
  |---|---|
  | Production | exact pinned versions, change‑controlled |
  | UAT | release candidates |
  | dev[1..n] | snapshot / latest |
- **Promotion** = publishing a version into the next environment's catalogue. Artifacts are
  immutable and identified by hash; promotion never rebuilds.
- **Config is a signed per‑environment bundle** (backend URLs, feature flags) fetched by the
  agent and injected into the launched app. App code stays environment‑agnostic — it reads
  injected config, which also suits shared server/client Java code.

---

## 9. Trust model

- **`env` is a signed claim** on every launch/control command — never a client‑chosen
  parameter. Tampering `env` would point an app at the wrong backend/data.
- **Per‑environment signing keys.** Production is its **own trust root**. UAT has its own.
  `dev[1..n]` may share a dev signer.
- An agent **pins only its own environment's key**, so it is structurally incapable of
  obeying another environment's server. This falls straight out of the 1:1 invariant.
- Artifact integrity (hash pinned in the signed command) is unchanged and applies per env.

---

## 10. Workstation state isolation & safety

- **State partitioned by environment**, e.g. `…/fxsuite/<env>/{cache,config,logs}`. No shared
  artifact cache across environments — same app+version in Prod and dev are separate copies.
- Apps from different environments **run simultaneously** as independent processes.
- **Environment must be visually unmistakable** — colour‑coded window chrome/banner
  (🔴 Prod, 🟠 UAT, 🔵 dev), env shown in every log line and console row. The dominant
  operational risk with several environments open is acting in the wrong one; optionally add
  a confirmation step for Prod launches.

---

## 11. Failure modes & open questions

| Case | Handling / open question |
|---|---|
| Agent offline | Console shows env as *no live session*; commands 409 rather than queue (confirm: queue‑and‑deliver later?) |
| Duplicate sessions | **Decided (§6):** allowed + isolated + detected; alert severity by env class |
| **Orphans from a dead session** | Its processes survive, and — because sessions are isolated — no surviving session can see or kill them. **Primary gap introduced by §6.** Preferred fix: Windows **Job Object with kill‑on‑close** so a session's processes die with it. Fallbacks: adopt tagged suite processes at start‑up, or retain the dead session's last inventory and flag those apps **unmanaged** in the console |
| Ambiguous launch target | Two live sessions ⇒ which receives a new launch? Resolved by pairing the web session to a specific agent session (§6) |
| Stale presence view | No longer gates anything — the launch gate is an **active probe** at action time (§6) |
| Half‑dead agent (heartbeats but wedged) | Caught by the action‑level probe; passive presence would have missed it |
| Probe OK, then agent dies before launch | Launch must be **acked** by the agent with its own timeout; "launched" is reported on ack, not on push |
| Probe times out on a merely‑slow agent | Starts a second agent ⇒ a duplicate, absorbed safely by the session model |
| Several sessions answer the probe | Pair to the answering / most recent session; that pairing targets the launch |
| Env endpoint down | Agent retries with backoff; locally cached apps still launchable? (**open** — offline policy per env class) |
| Dev env torn down while its apps run | Registry entry expires; agent session ends. Kill running apps or leave them? (**open**) |
| Agent restart | In‑memory inventory lost; see *orphans* above |
| App‑level singleton collision | Two sessions launching an app that assumes one instance (lock file / fixed port / shared data dir) — not solved by session isolation; must be handled per app |
| Version drift | Reconciler flags actual ≠ desired; ops can force‑update or pin |

**Decisions still to make**
1. Orphan containment mechanism — Job Object kill‑on‑close vs start‑up adoption vs flag‑only.
2. Probe timeout value, and how a **web UI consumes local acceleration** — routed via the
   server vs a localhost endpoint on the agent (origin checks + session‑bound token).
3. Offline behaviour per environment class (may Prod launch from cache when the endpoint is unreachable?).
4. Whether the central dev console fans out directly or via an aggregator.
5. Whether UAT shares Prod's console deployment model exactly, or is lighter.

---

## 12. Suggested phasing

1. **`env` as a signed dimension** end‑to‑end (claim, catalogue scoping, state paths, agent
   reporting) — smallest change that makes everything else expressible.
2. **Per‑env trust keys** + agent pinning; Prod as separate trust root.
3. **Session model + readiness probe** — per‑invocation session ids, enrolment, duplicate
   detection/alerting, per‑app capability tags in the catalogue, and the just‑in‑time probe
   that gates (and targets) a launch.
4. **Environment registry** + multiplexed dev agent sessions.
5. **Console split** — dedicated Prod/UAT consoles, central dev console.
6. **Reconciler** — desired vs actual state per environment, exploiting the 1:1 guarantee.
