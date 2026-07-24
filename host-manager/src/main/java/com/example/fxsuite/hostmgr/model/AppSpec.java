package com.example.fxsuite.hostmgr.model;

import com.example.fxsuite.hostmgr.json.Json;

import java.util.Map;

/**
 * One line of the declared desired state: run this app, at this exact version, in this
 * environment.
 *
 * <p>{@code sha256} pins the exact bytes — the agent verifies the artifact it downloads
 * against it, so the manifest chooses <i>which build</i> and no repository swap can substitute
 * another. {@code enabled=false} keeps a line in the manifest while asking the host to stop
 * running it — a decommission that is still declarative.</p>
 */
public record AppSpec(String appId, String version, String sha256, String env,
                      String memoryMax, boolean enabled) {

    public UnitId id() {
        return new UnitId(appId, env);
    }

    public static AppSpec from(Map<String, Object> o) {
        return new AppSpec(
                Json.str(o, "appId"),
                Json.str(o, "version"),
                Json.str(o, "sha256"),
                Json.str(o, "env"),
                Json.str(o, "memoryMax"),
                Json.bool(o, "enabled", true));
    }
}
