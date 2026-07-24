package com.example.fxsuite.hostmgr;

import com.example.fxsuite.hostmgr.control.ControlClient;
import com.example.fxsuite.hostmgr.model.AppSpec;
import com.example.fxsuite.hostmgr.model.DesiredState;
import com.example.fxsuite.hostmgr.model.LaunchSpec;
import com.example.fxsuite.hostmgr.model.UnitId;
import com.example.fxsuite.hostmgr.model.UnitState;
import com.example.fxsuite.hostmgr.supervisor.Supervisor;

import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * The on-host agent: one reconcile tick, repeated.
 *
 * <p>Each tick pulls the signed desired state, stages any artifacts an install or upgrade
 * needs, asks the {@link Reconciler} what to change, drives the {@link Supervisor} to make it
 * so, then reports actual state upward. Nothing here knows <i>how</i> processes are supervised
 * — that is the supervisor's job — so the same loop runs over systemd or direct supervision
 * unchanged.</p>
 *
 * <p>The loop is deliberately dull and idempotent: a steady host converges to "no changes" and
 * simply keeps reporting, and any drift — a crash, a reboot, a new declared version — is just
 * the next tick's plan. That is what "low touch" means here: the operator changes the declared
 * state, and hosts make themselves match.</p>
 */
public final class HostAgent {

    private final String hostId;
    private final ControlClient control;
    private final Artifacts artifacts;
    private final Supervisor supervisor;
    private final long intervalMs;

    private long lastGeneration = -1;

    public HostAgent(String hostId, ControlClient control, Artifacts artifacts,
                     Supervisor supervisor, long intervalMs) {
        this.hostId = hostId;
        this.control = control;
        this.artifacts = artifacts;
        this.supervisor = supervisor;
        this.intervalMs = intervalMs;
    }

    public void run() throws InterruptedException {
        log("agent up: host=" + hostId + " supervisor=" + supervisor.kind() + " interval=" + intervalMs + "ms");
        while (true) {
            try {
                tick();
            } catch (Exception e) {
                log("tick error: " + e.getMessage());
            }
            Thread.sleep(intervalMs);
        }
    }

    /** One convergence pass. Package-visible so a test or the demo can drive a single tick. */
    public void tick() throws Exception {
        DesiredState desired = control.pullDesired(hostId);
        if (!desired.appliesTo(hostId)) {
            log("desired gen " + desired.generation() + " does not target this host");
            return;
        }
        if (desired.generation() != lastGeneration) {
            log("desired generation " + desired.generation() + " (" + desired.enabledApps().size() + " app(s) declared)");
            lastGeneration = desired.generation();
        }

        List<UnitState> actual = supervisor.list();
        Reconciler.Plan plan = Reconciler.plan(desired, actual);

        for (Reconciler.Apply a : plan.toApply()) {
            AppSpec spec = a.spec();
            try {
                Path jar = artifacts.stage(spec);          // download + verify only if needed
                supervisor.apply(new LaunchSpec(spec.id(), spec.version(), jar,
                        List.of("--env=" + spec.env(), "--version=" + spec.version()), spec.memoryMax()));
                log(a.reason().name().toLowerCase() + " " + spec.id() + " -> " + spec.version());
            } catch (Exception e) {
                log("FAILED " + a.reason().name().toLowerCase() + " " + spec.id() + ": " + e.getMessage());
            }
        }
        for (UnitId id : plan.toStop()) {
            try {
                supervisor.stop(id);
                log("decommission " + id);
            } catch (Exception e) {
                log("FAILED decommission " + id + ": " + e.getMessage());
            }
        }
        if (plan.isEmpty()) log("in sync (gen " + desired.generation() + ")");

        report(desired.generation());
    }

    private void report(long generation) throws Exception {
        List<UnitState> now = supervisor.list();
        StringBuilder units = new StringBuilder();
        for (int i = 0; i < now.size(); i++) {
            if (i > 0) units.append(',');
            units.append(now.get(i).toJson());
        }
        String json = "{\"host\":\"" + hostId + "\",\"kind\":\"" + supervisor.kind()
                + "\",\"generation\":" + generation + ",\"units\":[" + units + "]}";
        control.report(json);
    }

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("HH:mm:ss");

    private void log(String line) {
        System.out.println("[agent " + TS.format(LocalTime.now()) + "] " + line);
    }
}
