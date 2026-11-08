package com.example.fxsuite.hostmgr.model;

import com.example.fxsuite.hostmgr.json.Json;

/**
 * The <i>actual</i> state of one managed unit, as the supervisor currently reports it — what
 * the reconciler compares against the desired state, and what the agent reports upward.
 *
 * @param version the version currently running (null if the supervisor cannot tell)
 * @param active  whether it is running right now
 * @param pid     the OS process id, or 0 if not running / unknown
 * @param uptimeS seconds since it started, best-effort
 */
public record UnitState(UnitId id, String version, boolean active, long pid, long uptimeS) {

    public static UnitState down(UnitId id) {
        return new UnitState(id, null, false, 0, 0);
    }

    public String toJson() {
        return "{\"unit\":" + Json.quote(id.unitName())
                + ",\"app\":" + Json.quote(id.appId())
                + ",\"env\":" + Json.quote(id.env())
                + ",\"version\":" + Json.quote(version)
                + ",\"active\":" + active
                + ",\"pid\":" + pid
                + ",\"uptimeS\":" + uptimeS + "}";
    }
}
