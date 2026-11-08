# Decoupling Infrastructure and Application Management

**Standard-VM fleets with a single integration contract**

Status: design white paper (extension) · Version 0.1 · 2026-07-25
Extends: [Controlled, Low-Touch Host Management for FxSuite](controlled-host-management-whitepaper.md)

---

## Abstract

The base paper describes *how* a host manages its applications: a signed control plane, a
reconcile loop, and privilege-separated agents. This companion paper is about *where the
boundary sits* — the deliberate separation of **infrastructure management** (the VMs, their OS,
their image) from **application management** (what runs on them, at which version, with which
config).

The thesis is simple: these are two independent planes with two independent lifecycles, and
they should meet at exactly **one** seam. That seam is a fixed, tiny contract — two long-running
agents that the host's init system starts and keeps alive. Nothing about application deployment
or execution requires touching the VM, its image, or its OS integration. A VM is therefore
*cattle*: standard, interchangeable, provisioned and re-provisioned freely, carrying no
app-specific state.

This decoupling is the point of the whole design. It lets a platform team ship a golden image
and patch an OS on one cadence while an application team deploys and rolls back services on a
completely different one — neither blocking the other, and neither needing to understand the
other's internals.

---

## 1. The thesis: two planes, one seam

```
        INFRASTRUCTURE PLANE                     APPLICATION PLANE
   (owns the VM and its OS)                  (owns what runs on it)

   image pipeline ─► golden image            control plane ─► signed desired state
        │                                          │
        ▼                                          ▼
   provision / replace VMs                    fetch · verify · run · upgrade · heal
        │                                          │
        └────────────────┐            ┌────────────┘
                         ▼            ▼
                 ┌──────────────────────────────┐
                 │      THE SEAM (one contract)  │
                 │  init system starts & keeps   │
                 │  alive two agents:            │
                 │    admin-agent  (as admin)    │
                 │    run-agent    (as run)      │
                 └──────────────────────────────┘
```

Everything above the seam on the left is standard infrastructure tooling (image builders, VM
orchestration, an OS patch process). Everything above it on the right is the machinery from the
base paper. The two never reach across; they only meet at the contract in the middle.

The contract is deliberately the smallest thing that could possibly work: **start two agents,
as two users, and keep them running.** That is the *entire* system-level integration an
application needs. There is no per-app OS artifact, no per-deploy privileged step, and nothing
about any application baked into the VM image.

---

## 2. Cattle, not pets

A *pet* server is unique, hand-tended, and irreplaceable: apps are installed onto it, its
config drifts over time, and losing it hurts. A *cattle* server is one of a herd of identical,
disposable units: you never fix one, you replace it.

This design makes FxSuite hosts cattle, because the VM image contains **no application state**:

- not the application binaries (staged by the agent at runtime, from the repository);
- not their versions (declared in the control plane, not the image);
- not their configuration (delivered through the application plane);
- not any per-app OS integration (no per-app unit, registry entry, or install step).

What the image *does* contain is only the seam: the two agents and the two units that start
them. Two images that are byte-identical will, pointed at the same control plane, converge to
running exactly the same applications — even though the image "knows" about none of them.

The practical consequences of cattle are large:

- **Reprovisioning is a valid repair.** A misbehaving VM is not diagnosed and patched; it is
  destroyed and replaced. The replacement boots the standard image and re-converges from the
  declared state.
- **No configuration drift.** Because nothing app-specific is ever written into the image or
  mutated on the VM outside the agent's managed tree, two hosts of the same image cannot drift.
- **Horizontal scale is a clone.** Adding capacity is cloning the image and booting it; the new
  host self-populates from the control plane.

---

## 3. The single integration contract

The seam is worth stating as a precise contract, because its smallness is the whole value.

**The image MUST provide:**

1. two OS identities — `admin` (deployer) and `run` (runtime), per the base paper's §6;
2. two enabled init-system units that start the agents as those identities and keep them alive
   (`Restart=always`), with the run-agent set **not** to take its app children down when it is
   itself restarted (`KillMode=process`, per §7 below);
3. a writable data directory the agents own (staged versions, intent/status, logs);
4. network reachability to the control plane and the artifact repository;
5. a means for the agent to establish trust in the control-plane key (baked-in pin, or
   first-boot provisioning).

**The image MUST NOT need to know:**

- which applications will run, or their versions, or their configuration;
- anything that changes when an application is deployed, upgraded, or removed.

If a proposed feature would require adding something app-specific to the image or a privileged
step to a deploy, it violates the contract and belongs on the application plane instead. This is
the test that keeps the seam from eroding.

---

## 4. Two independent upgrade tracks

Decoupling is most visible at upgrade time, because the two planes upgrade *completely
differently* and on their own schedules:

| | Infrastructure plane | Application plane |
|---|---|---|
| **Unit of change** | the VM image | a declared version in the manifest |
| **Owner** | platform / infra team | application / Production Support team |
| **Mechanism** | build image → roll VMs (replace) | edit desired state → agents converge |
| **Trigger** | OS patch, CVE, base-image update | app release |
| **Typical cadence** | weekly / monthly, batched | per release, on demand |
| **Rollback** | roll back to the previous image | set the version back (instant repoint) |
| **Touches the other plane?** | no — new VMs re-converge apps automatically | no — no VM change, no image rebuild |
| **Blast radius** | the VMs being replaced | the app being changed |

An OS security patch is a pure infrastructure event: build a new golden image, roll the fleet by
replacing VMs, and each replacement re-converges its applications from the unchanged declared
state. The application team need not be involved and need not even be aware.

An application release is a pure application event: change one version in the control plane and
the fleet converges. No image is rebuilt, no VM is mutated, no infra ticket is filed.

Neither track can block the other, and each can roll back without disturbing the other. That
independence is the deliverable.

---

## 5. Organisational decoupling

The technical seam lets the *organisation* split along the same line — Conway's law working in
your favour rather than against you.

- The **platform team** owns the image and the VM lifecycle. Their contract to everyone else is
  "a standard VM with the two agents running." They never need to know what applications exist.
- The **application team** (and Production Support) owns the declared state and the app
  lifecycle. Their contract is the control plane. They never need image-build rights, VM login,
  or OS-level privilege on a host.

Because the seam is a hard boundary, neither team can accidentally depend on the other's
internals: an application cannot demand a bespoke OS package in the image without visibly
breaking the contract, and an image change cannot alter application behaviour because the image
carries no application. The boundary is self-policing.

---

## 6. Lifecycle flows

Every lifecycle event resolves to one of the two planes, and the agents bridge them.

- **Provision a VM.** Clone the golden image → boot → the two agents start → admin-agent pulls
  and verifies the declared state → apps are staged and run. No app-specific provisioning step.
- **Upgrade the OS.** Build a new image → replace VMs with it (rolling) → each new VM
  re-converges its apps. The apps are never "migrated"; they are simply re-declared reality on
  fresh hosts.
- **Deploy or upgrade an app.** Edit the declared version and generation → hosts converge on
  their next tick. No VM touched.
- **Remediate a sick host.** Replace the VM (cattle). Faster and more reliable than diagnosing a
  drifted pet, and it cannot leave residue.
- **Scale out / in.** Clone more image instances, or destroy some. New hosts self-populate;
  destroyed hosts are simply forgotten by the control plane after they stop reporting.
- **Disaster recovery.** The declared state *is* the source of truth for what should run. Any
  VM, or the whole fleet, is rebuildable from image + control plane; nothing irreplaceable lives
  on a host.

---

## 7. Where the seam holds — and where it leaks

Honesty about the boundary is what keeps it trustworthy. Three places deserve explicit
treatment.

### 7.1 The agent's own upgrade straddles the seam

The two agents are the one thing that is *both* infrastructure (they ship in the image, started
by the init system) *and* the mechanism of application management. So "how is the agent itself
upgraded?" is the one question that crosses the boundary.

Two defensible answers, and the design supports either:

- **Agent-as-infrastructure (default).** The agent version is part of the image; upgrading it is
  an image roll like any OS change. This keeps the seam perfectly clean and guarantees a freshly
  provisioned VM has a known-good agent even if the control plane is unreachable at boot — the
  agent is the bootstrap, so it must be in the image, never fetched to start.
- **Signed agent self-update (escape hatch).** For a small agent fix without a full image roll,
  the admin-agent may fetch a signed new agent build and swap itself. This deliberately crosses
  into the application plane and must be treated with the same signing discipline as any
  artifact. The run-agent's `KillMode=process` plus pidfile re-adoption (below) means swapping
  the run-agent does **not** disturb the applications it supervises.

Recommendation: ship the agent in the image by default; treat self-update as a managed, signed
exception, not the norm.

### 7.2 Agent restart must not take applications down

Because the run-agent supervises apps in-process, a naïve init unit would kill every application
whenever the run-agent restarts (systemd's default `KillMode=control-group`). The contract
therefore mandates `KillMode=process` on the run-agent, and the run-agent persists a pidfile per
application into its state tree, re-adopting the still-live processes on startup (via
`ProcessHandle.of(pid)` — sufficient for liveness and control). A run-agent restart or
self-update is then transparent to running applications; a full VM reboot recovers everything
through the ordinary reconcile loop.

### 7.3 The VM is the isolation boundary

Minimal integration has a price, accepted deliberately here in the spirit of the cattle model:
because the run-agent runs all applications as the single `run` identity (giving each app its
own OS identity would require the per-app OS integration this design exists to avoid),
**isolation between applications is provided at VM granularity, not process granularity.** Apps
that share a host share a trust domain. Where stronger isolation is required, the cattle answer
applies: give that application its own VM (its own head of cattle). Likewise, hard resource caps
(cgroups) are a per-unit systemd feature the in-process model forgoes; the agent applies soft,
in-process limits instead (e.g. JVM heap flags from the manifest), with the VM as the real
ceiling.

These are not accidents of implementation; they are the conscious cost of keeping the
integration surface to two agents. See the base paper §8 for the security implications of a
shared runtime identity.

---

## 8. Consequences

Pulling the benefits together, decoupling the two planes yields:

- **Independent cadence.** OS patching and application releases proceed on their own schedules,
  neither gated on the other.
- **Immutable infrastructure.** Images are built, not mutated; drift is structurally impossible;
  reprovisioning is the primary repair.
- **Stable, auditable images.** A golden image with no application content is small, rarely
  changing, and testable once for the whole fleet.
- **Deployment without infrastructure.** Shipping an application needs no image rebuild, no VM
  access, and no OS privilege — only a change to declared state.
- **Recoverability.** With the image reproducible and the declared state authoritative, any host
  is disposable and any loss is rebuildable.

---

## 9. Assumptions and accepted trades

Stated plainly so they can be revisited:

1. **The VM is the isolation and resource boundary.** One shared `run` identity per host; apps
   needing stronger separation get their own VM. (§7.3)
2. **No hard per-app resource caps** without reintroducing per-app OS units; soft in-process
   limits only. (§7.3)
3. **Applications are re-convergeable, not stateful on the host.** Anything a host must not lose
   lives off-host (a datastore, the control plane); a VM can always be replaced. This is what
   makes cattle safe.
4. **The agent belongs to the image.** Its presence is an infrastructure guarantee; its update
   is infrastructure by default. (§7.1)

If assumption 1 or 3 does not hold for a particular workload, that workload is not a fit for the
standard-VM fleet and should be treated as a pet — explicitly, and by exception.

---

## 10. Relationship to the base paper

This paper does not change the base design; it fixes *which* of the base paper's options the
standard-VM fleet selects, and why:

- The base paper's three supervision **lanes** (§5.3) collapse for this fleet to **one shape**:
  in-process supervision, with deploy/run separation supplied by starting two agents as two
  users at boot. Per-app systemd units (lane B) are explicitly **rejected here**, because making
  each application a unit is precisely the "deeper integration" this decoupling exists to avoid.
- The **two-agent topology** (base §7) is therefore not one option among several but *the*
  model, because it is the only one that delivers deploy/run separation with no per-app OS
  integration.
- The base paper's security properties and trust chain (§4, §8) are unchanged and assumed.

Together the two papers answer two different questions: the base paper, "how does a host safely
run what it is told to run?"; this paper, "why does telling it require nothing of the host's
infrastructure but two running agents?"

---

## Appendix — The contract as a checklist

A golden image is compliant if, and only if:

- [ ] users `admin` and `run` exist, with the code/config tree owned by `admin` and readable —
      not writable — by `run`, and a separate `run`-writable state tree;
- [ ] two init units are enabled: `fxsuite-admin` (as `admin`) and `fxsuite-run` (as `run`),
      both `Restart=always`, the run unit `KillMode=process`;
- [ ] a known-good agent build is present in the image (the agent is the bootstrap; it is not
      fetched to start);
- [ ] the control-plane key trust is establishable (pinned in image or provisioned on first
      boot);
- [ ] network egress to the control plane and artifact repository is permitted;
- [ ] **nothing** application-specific is present: no app binaries, versions, configs, or units.

The last line is the one that matters most. If it ever fails, the planes have re-coupled.
