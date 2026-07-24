package com.example.fxsuite.hostmgr.model;

/**
 * Identifies one managed app on a host: an application in a particular environment.
 *
 * <p>The name deliberately matches a systemd template instance —
 * {@code fxsuite-<app>@<env>} → {@code fxsuite-hello@prod} — so the same identity is the unit
 * name under the systemd supervisor and a map key under the direct one. One template, one
 * instance per environment, mirroring the launcher's multiplexed {@code --env}.</p>
 */
public record UnitId(String appId, String env) {

    public String unitName() {
        return "fxsuite-" + appId + "@" + env;
    }

    @Override
    public String toString() {
        return unitName();
    }
}
