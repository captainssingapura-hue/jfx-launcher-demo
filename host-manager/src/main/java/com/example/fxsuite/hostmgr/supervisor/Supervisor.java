package com.example.fxsuite.hostmgr.supervisor;

import com.example.fxsuite.hostmgr.model.LaunchSpec;
import com.example.fxsuite.hostmgr.model.UnitId;
import com.example.fxsuite.hostmgr.model.UnitState;

import java.util.List;

/**
 * How this host actually starts, stops and observes processes — the seam where the permission
 * lane plugs in.
 *
 * <p>Everything above this interface (the control plane, the signed manifest, the reconciler)
 * is identical whichever lane a host uses. Only the mechanics of "keep this running" differ:</p>
 *
 * <ul>
 *   <li>{@link SystemdUserSupervisor} — lane A. Drives {@code systemctl --user}; needs no root
 *       beyond a one-time {@code loginctl enable-linger}. Survives host reboots and gets
 *       restart/health/resource limits from systemd itself.</li>
 *   <li>{@link DirectSupervisor} — lane C. Spawns and tracks processes in-process; needs no
 *       privilege at all and runs anywhere, at the cost of not surviving an agent restart.</li>
 * </ul>
 */
public interface Supervisor {

    /** A short name for logs and the ops report ({@code "systemd-user"} / {@code "direct"}). */
    String kind();

    /** The current state of every unit this supervisor manages. */
    List<UnitState> list() throws Exception;

    /**
     * Ensure the unit is running the given (already-verified) artifact. Idempotent: if it is
     * already running the right version this is a no-op; otherwise it starts, or replaces, or
     * restarts it. The reconciler calls this for installs, upgrades and self-heals alike.
     */
    void apply(LaunchSpec spec) throws Exception;

    /** Stop the unit and forget it — a decommission. */
    void stop(UnitId id) throws Exception;
}
